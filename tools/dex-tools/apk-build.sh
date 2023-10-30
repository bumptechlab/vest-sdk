#!/bin/bash
INFO() {
  echo "\033[0;32m$* \033[0m"
}
ERROR() {
  echo "\033[0;31m$* \033[0m"
}
WARNING() {
  echo "\033[0;33m$* \033[0m"
}
VERBOSE() {
  echo "\033[35m$* \033[0m"
}

apkFile=$1
INFO "开始执行：apk-dex加密"
INFO "apk文件:${apkFile}"
if [[ ! -f "${apkFile}" ]]; then
  ERROR "apk文件不存在：${apkFile}"
  exit
fi

#工作环境路径
rootDir=$(
  cd "$(dirname "$0")"
  pwd
)
projRoot=$(
  cd ${rootDir}/../..
  pwd
)
keystoreDir=${projRoot}/app/keystore
keystoreToolDir=${projRoot}/tools/keystore-tools
#输出目录
outputsDir=${projRoot}/outputs
outputsApkDir=${outputsDir}/apk
outputsAarDir=${outputsDir}/aar
#构建目录
toolsDir=${projRoot}/tools
buildDir=${toolsDir}/build
apkDir=${buildDir}/apk
aarDir=${buildDir}/aar
aarTemp=${aarDir}/temp
aarDexFile=${aarTemp}/classes.dex
aarJarFile=${aarTemp}/classes.jar
aarJarTempFile=${aarTemp}/classes-temp.jar
apkTemp=${apkDir}/temp
unsignedApk=${apkDir}/app-unsigned.apk
alignedApk=${apkDir}/app-unsigned-aligned.apk
signedApk=${apkDir}/app-signed.apk
apkDexTemp=${apkTemp}/dex_temp
#assets
apkAssetsDir=${apkTemp}/assets
assetsToolDir=${toolsDir}/assets-tools
#res
apkResDir=${apkTemp}/res
#dex in assets
apkDexZip=${apkAssetsDir}/classes.zip
apkDexData=${apkAssetsDir}/classes.data

#工具路径
buildToolsDir=${toolsDir}/build-tools
platformDir=${toolsDir}/platform

#先清理历史构建文件
rm -rf ${apkDir}
rm -rf ${aarDir}

#检查文件系统是否对文件名大小写敏感
testDir=${buildDir}/test
mkdir -p ${testDir}
touch ${testDir}/a.txt ${testDir}/A.txt
testFiles=0
for oneFile in $(ls ${testDir}); do
  let testFiles++
done

if [[ ${testFiles} == 2 ]]; then
  caseSensitive=1
  INFO "系统检查：当前系统对文件名大小写敏感，一切正常"
else
  caseSensitive=0
  WARNING "系统检查：当前系统对文件名大小写不敏感，将会导致最后出的release包资源不会被混淆。建议脚本运行在大小写敏感的卷宗：https://zhuanlan.zhihu.com/p/35908178"
fi
rm -rf ${testDir}

#构建Dex解密核心库aar
cd ${projRoot}
${projRoot}/gradlew lib-dex-decrypt:clean
${projRoot}/gradlew lib-dex-decrypt:makeAAR

#寻找output文件夹下的aar文件
aarFile=""
for oneFile in $(ls ${outputsAarDir}); do
  if echo "${oneFile}" | grep -q -E '\.aar$'; then
    aarFile=${outputsAarDir}/${oneFile}
  fi
done
echo "aar文件：${aarFile}"

if [[ ! -f "${aarFile}" ]]; then
  ERROR "aar文件不存在：${aarFile}"
  exit
fi

#解压apk、aar并加密里面的classes.dex
VERBOSE "步骤1：解压aar"
mkdir -p ${aarTemp}
unzip -q -o ${aarFile} -d ${aarTemp}

VERBOSE "步骤2：classes.jar转classes.dex"
#class文件脱糖
#脱糖含义：lamdal表达式在打包构建期间被转换成内部类的形式，这个过程叫脱糖
${buildToolsDir}/d8 --lib ${platformDir}/android-31.jar --output ${aarJarTempFile} ${aarJarFile}
#jar转dex
${buildToolsDir}/dx --dex --output ${aarDexFile} ${aarJarTempFile}
if [[ ! -f "${aarDexFile}" ]]; then
  ERROR "classes.jar转classes.dex失败"
  exit
fi

VERBOSE "步骤3：解压apk"
#-s表示不反编译dex文件, -r表示不反编译资源
if [[ ${caseSensitive} == 1 ]]; then
  INFO "文件系统大小写敏感，不需要反编译资源"
  java -jar ${buildToolsDir}/apktool.jar d ${apkFile} -s -r -o ${apkTemp}
else
  INFO "文件系统大小写不敏感，需要反编译资源"
  java -jar ${buildToolsDir}/apktool.jar d ${apkFile} -s -o ${apkTemp}
fi

VERBOSE "步骤4：加密Assets资源"
manifestFile=${apkTemp}/AndroidManifest.xml
sh ${assetsToolDir}/assets-compress.sh $manifestFile $apkAssetsDir $caseSensitive

VERBOSE "步骤5：修改res资源MD5"
changeMd5=$(find "$apkResDir" -iname "*.png" -exec echo {} \; -exec magick {} {} \;)
echo $changeMd5

VERBOSE "步骤6：压缩加密Dex"
mkdir -p ${apkDexTemp}
apkDexIndex=2
apkDexFiles=$(
  cd ${apkTemp}
  ls *.dex
)
for dexFile in ${apkDexFiles}; do
  echo "压缩加密Dex: ${dexFile}"
  apkDexInputFile=${apkTemp}/${dexFile}
  apkDexOutputFile=${apkDexTemp}/classes${apkDexIndex}.dex
  cp ${apkDexInputFile} ${apkDexOutputFile}
  rm -rf ${apkDexInputFile}
  let apkDexIndex++
done

zip -rjq ${apkDexZip} ${apkDexTemp}
java -jar ${rootDir}/libs/lib-dex-encrypt.jar -e -i ${apkDexZip} -o ${apkDexData}

#取8位md5放入文件名
apkDexDataMd5=$(md5 ${apkDexData} | awk -F ' = ' '{print $2}')
echo "md5[classes.data]: ${apkDexDataMd5}"
apkDexDataShortMd5=$(echo ${apkDexDataMd5:0:8})
apkDexDataFileName=sz${apkDexDataShortMd5}.data
mv ${apkDexData} ${apkAssetsDir}/${apkDexDataFileName}
rm -rf ${apkDexZip}
rm -rf ${apkDexTemp}

#把aar的classes.dex拷贝到temp
cp ${aarDexFile} ${apkTemp}

#压缩apk包，resources.arsc不能进行压缩
VERBOSE "步骤7：生成未签名apk（Apktool回编）"
java -jar ${buildToolsDir}/apktool.jar b ${apkTemp} -o ${unsignedApk}

#4字节对齐
VERBOSE "步骤8：对apk进行4字节对齐"
#-p 使未压缩的 .so 文件对齐页面
#-f 覆盖现有输出文件
#-v 输出详细信息
${buildToolsDir}/zipalign -f -p 4 ${unsignedApk} ${alignedApk}

#签名
VERBOSE "步骤9：对apk进行签名"

#签名aab
sf=$(cat $projRoot/config.gradle| awk '/storeFile/{print $3}' | tr -d '\",')
sp=$(cat $projRoot/config.gradle| awk '/storePassword/{print $2}' | tr -d '\",')
kp=$(cat $projRoot/config.gradle| awk '/keyPassword/{print $3}' | tr -d '\",')
ka=$(cat $projRoot/config.gradle| awk '/keyAlias/{print $3}' | tr -d '\",')

branch=$(git rev-parse --abbrev-ref HEAD)
storeFile=""
storePassword=""
keyPassword=""
keyAlias=""
if [[ ${branch} == "main" || ${branch} == "dev" || ${branch} == "dev-test" ]]; then
  storeFile=${keystoreDir}/ussjohnfkennedy.jks
  storePassword="ussjohnfkennedy"
  keyPassword="ussjohnfkennedy"
  keyAlias="ussjohnfkennedy"
else
  storeFile=${keystoreDir}/${sf}
  storePassword=${sp}
  keyPassword=${kp}
  keyAlias=${ka}
fi
echo "storeFile: ${storeFile}"
echo "storePassword: ${storePassword}"
echo "keyPassword: ${keyPassword}"
echo "keyAlias: ${keyAlias}"

if [[ ! -f "${storeFile}" ]]; then
  echo "keystore文件不存在：${storeFile}"
  #  sh ${keystoreToolDir}/gen_jks.sh
  exit
fi

#apksigner支持v1,v2签名
echo "apksigner version: $(${buildToolsDir}/apksigner --version)"
${buildToolsDir}/apksigner sign \
  --ks ${storeFile} \
  --ks-key-alias ${keyAlias} \
  --ks-pass pass:${storePassword} \
  --key-pass pass:${keyPassword} \
  --out ${signedApk} \
  ${alignedApk}

#jarsigner只支持v1签名
#jarsigner -verbose \
#-keystore ${keystoreFile} \
#-storepass ${keystorePwd} \
#-keypass ${keystorePwd} \
#-signedjar ${signedApk} \
#${alignedApk} \
#${keyAlias}

#设置遇到错误退出
set -o errexit
#使用aapt检查apk是否合法（比如: android:icon使用了color，存在错误无法提审）
aaptDumpInfo=$(${buildToolsDir}/aapt dump badging ${signedApk})

#拷贝加密后的apk到输出目录
mkdir -p ${outputsApkDir}
outputApkFileName="$(basename ${apkFile} .apk)_encrypted.apk"
outputApk=${outputsApkDir}/${outputApkFileName}
cp ${signedApk} ${outputApk}

#echo加密后的apk输出路径，供gradle或者其他脚本调用
echo "outputApk:${outputApk}"
echo "${outputApk}" >${buildDir}/output_apk.txt

#删除APK、AAR
rm -rf ${apkFile}
rm -rf ${outputsAarDir}
