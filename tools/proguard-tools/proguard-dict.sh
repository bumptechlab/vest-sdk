#工作环境路径
rootDir=$(cd "$(dirname "$0")";pwd)
projRoot=$(cd ${rootDir}/../..;pwd)
#构建以及输出目录
libsJar=${rootDir}/libs/lib-proguard-dict.jar

#使用java -jar lib-proguard-dict.jar -h 命令可以查看支持的字符集
java -jar ${libsJar} -h

#字典行数
lines=5000
#字典每行的字数，-1表示字数随机
characters=2
#使用日文平假名+日式标点符号，字符集索引请查看lib-proguard-dict.jar help信息（请自由组合字符集或者使用单一字符集）
charsets="7,8"

#字典输出路径
appProguardDictClass=${projRoot}/app/prog-dict-class.txt
appProguardDictPackage=${projRoot}/app/prog-dict-package.txt
appProguardDictMethod=${projRoot}/app/prog-dict-field-method.txt

#生成app下的字典
if [[ ! -f "${appProguardDictClass}" ]]
then
  echo "proguard dictionary $(basename ${appProguardDictClass} .txt) not exist, create a new one"
  java -jar ${libsJar} -o ${appProguardDictClass} -c ${characters} -l ${lines} -ch ${charsets}
fi

if [[ ! -f "${appProguardDictPackage}" ]]
then
  echo "proguard dictionary $(basename ${appProguardDictPackage} .txt) not exist, create a new one"
  java -jar ${libsJar} -o ${appProguardDictPackage} -c ${characters} -l ${lines} -ch ${charsets}
fi

if [[ ! -f "${appProguardDictMethod}" ]]
then
  echo "proguard dictionary $(basename ${appProguardDictMethod} .txt) not exist, create a new one"
  java -jar ${libsJar} -o ${appProguardDictMethod} -c ${characters} -l ${lines} -ch ${charsets}
fi

