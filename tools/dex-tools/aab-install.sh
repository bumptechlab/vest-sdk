#!/bin/bash
aabFile=$1
echo "执行安装aab: ${aabFile}"
if [[ ! -f "${aabFile}" ]]
then
	echo "aab文件不存在：${aabFile}"
	exit
fi
#工作环境路径
rootDir=$(cd "$(dirname "$0")";pwd)
projRoot=$(cd ${rootDir}/../..;pwd)
keystoreDir=${projRoot}/app/keystore
keystoreToolDir=${projRoot}/tools/keystore-tools
#工具目录
toolsDir=$(cd "${rootDir}/..";pwd)
bundleToolDir=${toolsDir}/bundle-tool
#临时文件
apksDir=$(dirname ${aabFile})
apksFileName=$(basename ${aabFile} .aab).apks
apksFile=${apksDir}/${apksFileName}

#清理历史文件
rm -f ${apksDir}/*.apks

#Build Apks
echo -e "\033[35m步骤1：生成apks\033[0m"
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
  echo "keystore文件不存在：${storeFile}，生成一个"
  sh ${keystoreToolDir}/gen_jks.sh
fi

java -jar ${bundleToolDir}/bundletool.jar build-apks \
--bundle=${aabFile} \
--output=${apksFile} \
--ks=${storeFile} \
--ks-pass=pass:${storePassword} \
--key-pass=pass:${keyPassword} \
--ks-key-alias=${keyAlias} \
--mode=universal

if [[ ! -f "${apksFile}" ]]
then
	echo "apks文件不存在：${apksFile}"
	exit
fi

#Install Apks
echo -e "\033[35m步骤2：安装apks\033[0m"
java -jar ${bundleToolDir}/bundletool.jar install-apks --apks=${apksFile}

#Remove Apks
rm -rf ${apksFile}