#!/bin/bash
rootDir=$(cd "$(dirname "$0")";pwd)
keystoreDir=$(cd ${rootDir}/../../app/keystore;pwd)
echo "keystoreDir=${keystoreDir}"

branch=`git rev-parse --abbrev-ref HEAD`
echo "branch: $branch"

keystoreFile=${keystoreDir}/${branch}.jks
keyGoogleFile=${keystoreDir}/${branch}_google.zip
storePassword=${branch}
keyPassword=${branch}
keyAlias=${branch}

# genkey
keytool -genkey -keystore ${keystoreFile} -alias ${keyAlias} -storepass ${storePassword} -keypass ${keyPassword} -keyalg RSA -validity 36500 -dname CN=$branch,OU=$branch,O=$branch,L=$branch,ST=$branch,C=$branch

# key_google.zip
java -jar ${rootDir}/pepk.jar --keystore=${keystoreFile} --alias=${keyAlias} --keystore-pass=${storePassword} --key-pass=${keyPassword} --output=${keyGoogleFile} --include-cert --encryptionkey=eb10fe8f7c7c9df715022017b00c6471f8ba8170b13049a11e6c09ffe3056a104a3bbe4ac5a955f4ba4fe93fc8cef27558a3eb9d2a529a2092761fb833b656cd48b9de6a

echo "generate successful"
