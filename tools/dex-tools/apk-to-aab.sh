#!/bin/bash
INFO(){
  echo "\033[0;32m$* \033[0m"
}
ERROR(){
  echo "\033[0;31m$* \033[0m"
}
WARNING(){
  echo "\033[0;33m$* \033[0m"
}
VERBOSE(){
  echo "\033[35m$* \033[0m"
}

apkFile=$1
INFO "开始执行：apk转aab"
INFO "apk文件：${apkFile}"
if [[ ! -f "${apkFile}" ]]
then
	ERROR "apk文件不存在：${apkFile}"
	exit
fi
#工作环境路径
rootDir=$(cd "$(dirname "$0")";pwd)
projRoot=$(cd ${rootDir}/../..;pwd)
keystoreDir=${projRoot}/app/keystore
keystoreToolDir=${projRoot}/tools/keystore-tools
#构建以及输出目录
toolsDir=${projRoot}/tools
buildDir=${toolsDir}/build
aabDir=${buildDir}/aab
aabOutputDir=${projRoot}/outputs/aab
#工具路径
buildToolsDir=${toolsDir}/build-tools
bundleToolDir=${toolsDir}/bundle-tool
platformDir=${toolsDir}/platform
#临时文件路径
appDir=${aabDir}/app
baseDir=${aabDir}/base
baseApk=${aabDir}/base.apk
baseZip=${aabDir}/base.zip
baseAab=${aabDir}/base.aab
publicTxt=${aabDir}/public.txt
compiledResources=${aabDir}/compiled_resources.zip

#清理历史构建文件
rm -rf ${aabDir}

#用apktool解压apk
VERBOSE "步骤1：解压apk"
java -jar ${buildToolsDir}/apktool.jar d ${apkFile} -s -o ${appDir}

#从config.gradle解析apk基本信息(无法脱离工程)
#packageName=$(echo $(echo $(cat ${projRoot}/config.gradle | grep applicationId) | grep -E -o "'(.*)'") | sed "s/'//g")
#minSdkVersion=$(echo $(cat ${projRoot}/config.gradle | grep minSdkVersion) | grep -E -o "[0-9]+")
#targetSdkVersion=$(echo $(cat ${projRoot}/config.gradle | grep targetSdkVersion) | grep -E -o "[0-9]+")
#versionCode=$(echo $(cat ${projRoot}/config.gradle | grep versionCode) | grep -E -o "[0-9]+")
#versionName=$(echo $(echo $(cat ${projRoot}/config.gradle | grep versionName) | grep -E -o "'(.*)'") | sed "s/'//g")

#从apktool.yml和AndroidManifest.xml解析apk基本信息
packageName=$(cat ${appDir}/AndroidManifest.xml | grep "manifest" | awk -F ' ' '{print $9}' | awk -F '"' '{print $2}')
minSdkVersion=$(cat ${appDir}/apktool.yml | grep minSdkVersion | awk -F "'" '{print $2}')
targetSdkVersion=$(cat ${appDir}/apktool.yml | grep targetSdkVersion | awk -F "'" '{print $2}')
versionCode=$(cat ${appDir}/apktool.yml | grep versionCode | awk -F "'" '{print $2}')
versionName=$(cat ${appDir}/apktool.yml | grep versionName | awk -F ": " '{print $2}')

echo "apk基本信息： "
echo "packageName=${packageName}"
echo "minSdkVersion=${minSdkVersion}"
echo "targetSdkVersion=${targetSdkVersion}"
echo "versionCode=${versionCode}"
echo "versionName=${versionName}"

#编译资源使用aapt2编译生成 *.flat文件集合
VERBOSE "步骤2：编译res资源"
${buildToolsDir}/aapt2 compile --legacy --dir ${appDir}/res -o ${compiledResources}
if [[ ! -f "${compiledResources}" ]]
then
  ERROR "compiled_resources.zip不存在，终止构建"
  exit
fi

#读取public.xml设置aapt2 link --stable-ids
rm -rf ${publicTxt}
while read rows
do
  if [[ $(echo $rows | grep "public") != "" ]]
  then
    type=$(echo "${rows}" | awk -F " " '{print $2}' | awk -F '"' '{print $2}')
    name=$(echo "${rows}" | awk -F " " '{print $3}' | awk -F '"' '{print $2}')
    id=$(echo "${rows}" | awk -F " " '{print $4}' | awk -F '"' '{print $2}')
    echo "${packageName}:${type}/${name} = ${id}" >> ${publicTxt}
  fi
done < ${appDir}/res/values/public.xml

#关联资源，生成base.apk
VERBOSE "步骤3：关联资源，生成base.apk"

${buildToolsDir}/aapt2 link --proto-format -o ${baseApk} -I ${platformDir}/android-31.jar \
--min-sdk-version ${minSdkVersion} \
--target-sdk-version ${targetSdkVersion} \
--version-code ${versionCode} \
--version-name ${versionName} \
--manifest ${appDir}/AndroidManifest.xml \
--auto-add-overlay \
-R ${compiledResources} \
--stable-ids ${publicTxt} \
--warn-manifest-validation


if [[ ! -f "${baseApk}" ]]
then
  ERROR "base.apk不存在，终止构建"
  exit
fi

#解压base.apk
VERBOSE "步骤4：解压base.apk"
unzip -q -o ${baseApk} -d ${baseDir}

#拷贝资源到base
VERBOSE "步骤5：拷贝app资源到base"
mkdir -p ${baseDir}/manifest
mkdir -p ${baseDir}/dex
mkdir -p ${baseDir}/root
mkdir -p ${baseDir}/root/META-INF

echo "moving ${baseDir}/AndroidManifest.xml > ${baseDir}/manifest/AndroidManifest.xml"
mv ${baseDir}/AndroidManifest.xml ${baseDir}/manifest

echo "copying ${appDir}/assets > ${baseDir}/assets"
cp -r ${appDir}/assets ${baseDir}

echo "copying ${appDir}/lib > ${baseDir}/lib"
cp -r ${appDir}/lib ${baseDir}

echo "copying ${appDir}/unknown > ${baseDir}/root/unknown"
cp -r ${appDir}/unknown ${baseDir}/root

echo "copying ${appDir}/kotlin > ${baseDir}/root/kotlin"
cp -r ${appDir}/kotlin ${baseDir}/root

echo "copying ${appDir}/*.dex > ${baseDir}/dex/"
cp ${appDir}/*.dex ${baseDir}/dex

#压缩base资源
VERBOSE "步骤6：压缩base生成base.zip"
cd ${baseDir}
zip -q -r ${baseZip} ./ -x "*.DS_Store" -x "__MACOSX"
if [[ ! -f "${baseZip}" ]]
then
  ERROR "base.zip不存在，终止构建"
  exit
fi

#编译aab
VERBOSE "步骤7：编译aab"
java -jar ${bundleToolDir}/bundletool.jar build-bundle --modules=${baseZip} --output=${baseAab} --config=${rootDir}/BundleConfig.json
if [[ ! -f "${baseAab}" ]]
then
  ERROR "base.aab不存在，终止构建"
  exit
fi

#签名aab
VERBOSE "步骤8：签名aab"
branch=$(git rev-parse --abbrev-ref HEAD)
storeFile=""
storePassword=""
keyPassword=""
keyAlias=""
if [[ ${branch} == "main" || ${branch} == "dev" || ${branch} == "dev-test" ]]
then
  storeFile=${keystoreDir}/ussjohnfkennedy.jks
  storePassword="ussjohnfkennedy"
  keyPassword="ussjohnfkennedy"
  keyAlias="ussjohnfkennedy"
else
  storeFile=${keystoreDir}/${branch}.jks
  storePassword=${branch}
  keyPassword=${branch}
  keyAlias=${branch}
fi
echo "storeFile: ${storeFile}"
echo "storePassword: ${storePassword}"
echo "keyPassword: ${keyPassword}"
echo "keyAlias: ${keyAlias}"

if [[ ! -f "${storeFile}" ]]
then
  ERROR "keystore文件不存在：${storeFile}"
  exit
fi

#注意：不能使用 apksigner 为 aab 签名。签名aab的时候不需要使用v2签名，使用JDK的普通签名就行。
jarsigner -digestalg SHA1 -sigalg SHA1withRSA \
-keystore ${storeFile} \
-storepass ${storePassword} \
-keypass ${keyPassword} \
${baseAab} \
${keyAlias}

#拷贝到outputs目录
VERBOSE "步骤9：拷贝aab ${baseAab}"
mkdir -p ${aabOutputDir}
aabOutputFileName="$(basename ${apkFile} .apk).aab"
aabOutputFile=${aabOutputDir}/${aabOutputFileName}
cp ${baseAab} ${aabOutputFile}

#echo加密后的aab输出路径，供gradle或者其他脚本调用
echo "outputAab:${aabOutputFile}"
echo "${aabOutputFile}" > ${buildDir}/output_aab.txt


