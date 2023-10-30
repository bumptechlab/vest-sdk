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

aabFile=$1
INFO "开始执行：aab加密"
INFO "aab文件：${aabFile}"
if [[ ! -f "${aabFile}" ]]; then
  ERROR "aab文件不存在：${aabFile}"
  exit
fi
#工作环境路径
rootDir=$(cd "$(dirname "$0")";pwd)
projRoot=$(cd ${rootDir}/../..;pwd)
keystoreDir=${projRoot}/app/keystore
#输出目录
outputsDir=${projRoot}/outputs
aabOutputDir=${outputsDir}/aab
aarOutputDir=${outputsDir}/aar
#构建目录
toolsDir=${projRoot}/tools
buildDir=${toolsDir}/build
aarDir=${buildDir}/aar
aarTemp=${aarDir}/temp
aarJarFile=${aarTemp}/classes.jar
aarJarTempFile=${aarTemp}/classes-temp.jar
aarDexFile=${aarTemp}/classes.dex
aabDir=${buildDir}/aab
aabTempDir=${aabDir}/temp
baseDir=${aabTempDir}/base
aabDexDir=${baseDir}/dex
aabDexTemp=${baseDir}/dex_temp
baseZip=${aabDir}/base.zip
baseAab=${aabDir}/base.aab
#assets
aabAssetsDir=${baseDir}/assets
aabAssetsPluginJar=${aabAssetsDir}/plugin
aabAssetsPluginTempJar=${aabAssetsDir}/plugin-temp
assetsToolDir=${toolsDir}/assets-tools
#res
aabResDir=${baseDir}/res
#dex in assets
aabDexZip=${aabAssetsDir}/classes.zip
aabDexData=${aabAssetsDir}/classes.data

#工具路径
buildToolsDir=${toolsDir}/build-tools
bundleToolDir=${toolsDir}/bundle-tool
platformDir=${toolsDir}/platform
keystoreToolDir=${toolsDir}/keystore-tools

#进入工作区
cd ${projRoot}

#清理历史构建文件
rm -rf ${aabDir}
mkdir -p ${aabDir}
rm -rf ${aarDir}
mkdir -p ${aarDir}

VERBOSE "步骤1：构建aar"
#构建Dex解密核心库aar
${projRoot}/gradlew lib-dex-decrypt:clean
${projRoot}/gradlew lib-dex-decrypt:makeAAR

#寻找output文件夹下的aar文件
aarFile=""
for oneFile in $(ls ${aarOutputDir}); do
  if echo "${oneFile}" | grep -q -E '\.aar$'; then
    aarFile=${aarOutputDir}/${oneFile}
  fi
done
echo "aar文件：${aarFile}"

if [[ ! -f "${aarFile}" ]]; then
  ERROR "aar文件不存在：${aarFile}"
  exit
fi

#解压aar
VERBOSE "步骤2：解压aar"
unzip -q -o ${aarFile} -d ${aarTemp}

VERBOSE "步骤3：classes.jar转classes.dex"
#class文件脱糖
#脱糖含义：lamdal表达式在打包构建期间被转换成内部类的形式，这个过程叫脱糖
${buildToolsDir}/d8 --lib ${platformDir}/android-31.jar --output ${aarJarTempFile} ${aarJarFile}
#jar转dex
${buildToolsDir}/dx --dex --output ${aarDexFile} ${aarJarTempFile}
if [[ ! -f "${aarDexFile}" ]]; then
  ERROR "classes.jar转classes.dex失败"
  exit
fi

#解压aab
VERBOSE "步骤4：解压aab"
unzip -q -o ${aabFile} -d ${aabTempDir}

VERBOSE "步骤5：加密Assets资源"
manifestFile=${baseDir}/manifest/AndroidManifest.xml
sh ${assetsToolDir}/assets-compress.sh $manifestFile $aabAssetsDir 0

VERBOSE "步骤6：修改res资源MD5"
changeMd5=$(find "$aabResDir" -iname "*.png" -exec echo {} \; -exec magick {} {} \;)
echo "$changeMd5"

VERBOSE "步骤7：压缩加密Dex"
mkdir -p ${aabDexTemp}
aabDexIndex=2
aabDexFiles=$(
  cd ${aabDexDir}
  ls *.dex
)
for dexFile in ${aabDexFiles}; do
  echo "加密Dex: ${dexFile}"
  aabDexInputFile=${aabDexDir}/${dexFile}
  aabDexOutputFile=${aabDexTemp}/classes${aabDexIndex}.dex
  cp ${aabDexInputFile} ${aabDexOutputFile}
  rm -rf ${aabDexInputFile}
  let aabDexIndex++
done

zip -rjq ${aabDexZip} ${aabDexTemp}
java -jar ${rootDir}/libs/lib-dex-encrypt.jar -e -i ${aabDexZip} -o ${aabDexData}

#取8位md5放入文件名
aabDexDataMd5=$(md5 ${aabDexData} | awk -F ' = ' '{print $2}')
echo "md5[classes.data]: ${aabDexDataMd5}"
aabDexDataShortMd5=$(echo ${aabDexDataMd5:0:8})
aabDexDataFileName=sz${aabDexDataShortMd5}.data
mv ${aabDexData} ${aabAssetsDir}/${aabDexDataFileName}
rm -rf ${aabDexZip}
rm -rf ${aabDexTemp}

#把aar的classes.dex拷贝到base/dex
cp ${aarDexFile} ${aabDexDir}

VERBOSE "步骤8：压缩base.zip"
cd ${baseDir}
zip -q -r --no-dir-entries ${baseZip} ./ -x "*.DS_Store" -x "__MACOSX"

VERBOSE "步骤9：构建base.aab"
java -jar ${bundleToolDir}/bundletool.jar build-bundle \
  --modules ${baseZip} \
  --output ${baseAab} \
  --config=${rootDir}/BundleConfig.json

#VERBOSE "步骤6：检验base.aab"
#java -jar ${bundleToolDir}/bundletool.jar validate --bundle ${baseAab}

#签名aab
VERBOSE "步骤10：签名base.aab"
branch=$(git rev-parse --abbrev-ref HEAD)

#签名aab
sf=$(cat $projRoot/config.gradle| awk '/storeFile/{print $3}' | tr -d '\",')
sp=$(cat $projRoot/config.gradle| awk '/storePassword/{print $2}' | tr -d '\",')
kp=$(cat $projRoot/config.gradle| awk '/keyPassword/{print $3}' | tr -d '\",')
ka=$(cat $projRoot/config.gradle| awk '/keyAlias/{print $3}' | tr -d '\",')

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
  ERROR "keystore文件不存在：${storeFile}"
  #  sh ${keystoreToolDir}/gen_jks.sh
  exit
fi

aabOutputFileName="$(basename ${aabFile} .aab)_encrypted.aab"
aabOutputFile=${aabOutputDir}/${aabOutputFileName}
#注意：不能使用 apksigner 为 aab 签名。签名aab的时候不需要使用v2签名，使用JDK的普通签名就行。
jarsigner -digestalg SHA1 -sigalg SHA1withRSA \
  -keystore ${storeFile} \
  -storepass ${storePassword} \
  -keypass ${keyPassword} \
  -signedjar ${aabOutputFile} \
  ${baseAab} \
  ${keyAlias}

#echo加密后的aab输出路径，供gradle或者其他脚本调用
echo "outputAab:${aabOutputFile}"
echo "${aabOutputFile}" >${buildDir}/output_aab.txt

#删除apk、aar
rm -rf ${aabFile}
rm -rf ${aarOutputDir}