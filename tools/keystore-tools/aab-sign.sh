
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
aabFile=$1

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

#签名aab
VERBOSE "签名aab"
branch=$(git rev-parse --abbrev-ref HEAD)
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
#  sh ${keystoreToolDir}/gen_jks.sh
  exit
fi

aabOutputFileName="$(basename ${aabFile} .aab)_signed.aab"
aabOutputFile=${aabOutputDir}/${aabOutputFileName}
#注意：不能使用 apksigner 为 aab 签名。签名aab的时候不需要使用v2签名，使用JDK的普通签名就行。
jarsigner -digestalg SHA1 -sigalg SHA1withRSA \
-keystore ${storeFile} \
-storepass ${storePassword} \
-keypass ${keyPassword} \
-signedjar ${aabOutputFile} \
${aabFile} \
${keyAlias}

#echo加密后的aab输出路径，供gradle或者其他脚本调用
echo "outputAab:${aabOutputFile}"
