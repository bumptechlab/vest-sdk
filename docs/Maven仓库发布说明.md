# 1.准备工作
根据官网介绍，sonatype对安全性有要求，所以需要我们注册GPG密钥才可以发布AAR，注册过程请查看备注中的教程。
所以请先在maven.properties中配置sonatype账号信息和GPG密钥信息，格式如下：   

  ```
  #创建GPG密钥后，pub字段中获取的秘钥后8位
  signing.keyId=
  #私钥密码：创建GPG密钥时，输入的密码
  signing.password=
  #secring.gpg文件的路径
  signing.secretKeyRingFile=
    
  #sonatype账号名
  ossrhUsername=
  #sonatype用户名称
  ossrhName=
  #sonatype Email
  ossrhEmail=
  #sonatype密码
  ossrhPassword=
  ```
# 2.发布AAR步骤

- 步骤1：打包AAR，使用以下命令构建app-core, app-sdk, app-shf三个模块，AAR最终输出到sdk目录。   
  ```
  ./gradlew clean app-core:assembleRelease app-sdk:assembleRelease app-shf:assembleRelease
  ```
- 步骤2：发布AAR，执行以下命令发布app-core, app-sdk, app-shf三个模块的AAR。(开源sdk使用者，不需要执行此步骤)
  ```
  ./gradlew publishReleasePublicationToMavenCentralRepository
  ```

## 备注：

- (1)生成和查看GPG密钥：   
生成GPG密钥：gpg --full-generate-key   
查看GPG密钥：gpg --list-keys   

- (2)有关Maven仓库的知识请参考以下博文：   
https://zhuanlan.zhihu.com/p/22351830   
https://mp.weixin.qq.com/s/FVR6_zMp5DxO5N4ptVuA6g   







