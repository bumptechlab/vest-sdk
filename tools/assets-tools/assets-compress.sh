#!/bin/bash

#工作环境路径
rootDir=$(
  cd "$(dirname "$0")"
  pwd
)
projRoot=$(
  cd ${rootDir}/../..
  pwd
)

toolsDir=${projRoot}/tools
dexToolsDir=${toolsDir}/dex-tools
assetsCompressToolsDir=${toolsDir}/assets-tools
buildDir=${toolsDir}/build
apkDir=${buildDir}/apk
apkTemp=${apkDir}/temp
#aab
#aabDir=${buildDir}/aab
#aabTempDir=${aabDir}/temp
#baseDir=${aabTempDir}/base
#manifestFile=${baseDir}/manifest/AndroidManifest.xml
#aabAssetsDir=${baseDir}/assets
#assetsDir=$aabAssetsDir
#apk
#manifestFile=${apkTemp}/AndroidManifest.xml
#assetsDir=${apkTemp}/assets
manifestFile=$1
assetsDir=$2
caseSensitive=$3

#包名
#读aab manifest
tempManifest=$(dirname "$manifestFile")/manifest-temp.xml
LC_CTYPE=C tr -cd "[:print:]\n" <$manifestFile >$tempManifest
apkPkgId=$(cat $tempManifest | grep -a 'package.*\"R' | sed 's/.*package\([^"]*\)"R/\1/1' | base64)
if [ ! $apkPkgId ]; then
  apkMani=$manifestFile
  if [[ $caseSensitive == 1 ]]; then
    java -jar $assetsCompressToolsDir/AXMLPrinter2.jar $manifestFile >$tempManifest
    apkMani=$tempManifest
  fi
  #读 apk manifest
  apkPkgId=$(cat $apkMani | grep 'package="[^"]*"' | sed 's/.*package="\([^"]*\)".*/\1/' | base64)
fi
echo "$apkPkgId"
rm -rf $tempManifest
apkAssetsZipName="assets.apk"
apkAssetsGuideJson=${apkPkgId:2:9}
#随机文件名后缀
suffix=("zip" "wmx" "mp3" "txt" "mp4" "avi" "tar" "so" "dta" "gif" "jpg" "png" "psd" "bmp" "wmv" "asf" "rm" "rmvb" "mov")
suffixLength=${#suffix[@]}

##白名单 不需要assets加密的文件写到下面
customWhiteList=$(cat ${assetsCompressToolsDir}/white-list.txt)
whiteList=($apkAssetsGuideJson $apkAssetsZipName $customWhiteList)

function splitAndMv() {
  #拆分随机大小片8-20
  randomSize=$(expr $RANDOM % 10 + 7)
  split -b ${randomSize}M $apkAssetsZipName "$apkPkgId-"

  tempFiles=$(find . -iname "$apkPkgId-*")
  echo $tempFiles
  #排序
  sortedTmpFiles=$(
    for el in "${tempFiles[@]}"; do
      echo "$el"
    done | sort
  )
  jsonBody=""
  #随机加密位置
  tempFileArray=(${sortedTmpFiles// /})
  encryptedIndex=$(expr $RANDOM % ${#tempFileArray[@]})

  declare -i index=0
  encryptBody=",\"encryptedIndex\":$encryptedIndex"

  mvRandomFileName "$sortedTmpFiles" $encryptedIndex

  #移除最后一个逗号
  jsonBody=${jsonBody%?}
  echo "{\"data\":[${jsonBody}]$encryptBody}" >${apkAssetsGuideJson}
  #加密guide.json
  java -jar "$dexToolsDir/libs/lib-dex-encrypt.jar" -e -i "$apkAssetsGuideJson" -o "$apkAssetsGuideJson"
}

function mvRandomFileName() {
  for anyFile in $1; do
    #生成随机文件名
    randomFileLength=$(expr $RANDOM % $suffixLength + 1)
    random=$(openssl rand -base64 128 | md5 | cut -c1-$randomFileLength)
    fakeName="${random}.${suffix[$(expr $randomFileLength % $suffixLength)]}"
    mv $anyFile "$fakeName"

    jsonBody="$jsonBody\"$fakeName\","

    #随机加密一个片
    if [ $index -eq $2 ]; then
      echo "encrypt :$index"
      #加密
      java -jar "$dexToolsDir/libs/lib-dex-encrypt.jar" -e -i "$fakeName" -o "$fakeName"
    fi
    #  删除切片文件
    rm -rf "$anyFile"
    ((index++))
  done
}

echo "归档文件："
cd "$assetsDir/../"
apkAssetsFiles=$(ls assets)
#echo $apkAssetsFiles
zipExclude=""
# shellcheck disable=SC2068
for anyFile in ${whiteList[@]}; do
  zipExclude="${zipExclude} -x "${anyFile}" "
done
java -jar "$assetsCompressToolsDir/lib-zip.jar" -i $assetsDir -o $(pwd)/$apkAssetsZipName $zipExclude
mv $(pwd)/$apkAssetsZipName $assetsDir/$apkAssetsZipName

cd "$assetsDir"
#删除非白名单文件
rmCmd="find $(ls) -maxdepth 1"
#rm `ls | grep -v"$xx"`
# shellcheck disable=SC2068
for anyFile in ${whiteList[@]}; do
  rmCmd="${rmCmd} -not -name ${anyFile}"
done
$($rmCmd -delete -exec rm -rf {} \;)

# shellcheck disable=SC2181
if [ $? -eq 0 ]; then
  splitAndMv
  #生成垃圾文件
  echo "生成垃圾文件："
  sh ${assetsCompressToolsDir}/random-files.sh 5 20 K

  rm -rf $apkAssetsZipName
else
  echo "归档失败"
  echo
fi

cd "$projRoot"
