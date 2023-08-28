#!/bin/bash
apkFile=$1
echo "执行安装apk: ${apkFile}"
if [[ ! -f "${apkFile}" ]]
then
	echo "apk文件不存在：${apkFile}"
	exit
fi

#使用aapt查看apk的包名
packageName=$(aapt dump badging ${apkFile} | awk -F" " '/package/ {print $2}' | awk -F "'" '/name=/{print $2}')
launchActivity=$(aapt dump badging ${apkFile} | awk -F" " '/launchable-activity/ {print $2}' | awk -F "'" '/name=/{print $2}')

devices=$(adb devices | sed "s/List of devices attached//g" | sed "s/device//g")
deviceArr=($devices)
for device in ${deviceArr[*]}
do
	echo "安装apk到手机【${device}】\n包名：${packageName}\n启动Activity：${launchActivity}"
	apkTmpFile=/data/local/tmp/${packageName}.apk
	adb -s ${device} push ${apkFile} ${apkTmpFile}
	result=$(adb -s ${device} shell "
	pm install -r ${apkTmpFile}
	rm -rf ${apkTmpFile}
	")
	#这里不能直接用adb install
	#adb install ‎⁨-r ${apkfile}
	echo "手机【${device}】上安装结果：${result}"

	#===== 步骤6: 启动游戏 =====
	compare=$(echo $result | grep "Success")
	if [[ "$compare" != "" ]]
	then
	    echo "手机【${device}】上启动游戏"
		adb -s ${device} shell "
		am kill ${packageName}
		am start -n ${packageName}/${launchActivity}
		"
	else
		echo "手机【${device}】上安装失败，退出"
	    exit 0
	fi
done