# Vest-SDK
最新版本：0.10.14   
这是一个可以用于控制游戏跳转的三方依赖库，工程提供开源代码，可自行修改。   

SDK总共三个依赖库：  
vest-core: 项目运行所必须的核心库（必须引入）  
vest-sdk: 运行B面游戏的平台  
vest-shf: 用于切换A/B面的远程开关   

## 开发环境
- JdkVersion:  11
- GradleVersion: 7.4
- GradlePluginVersion: 7.3.0
- minSdkVersion    : 24
- targetSdkVersion : 34
- compileSdkVersion: 34

## 工程说明
- app-core是app-sdk、app-shf的核心库
- app-sdk用于构建游戏运行的平台
- app-shf用于构建审核服开关功能，用于切换A/B面
- app是用于测试sdk的测试工程
- 开源sdk使用者可以使用以下构建命令构建出aar，再自行导入自己的工程。（总共三个aar文件，分别输出到sdk目录和app/libs目录）
    ```
    ./gradlew clean app-core:assembleRelease app-sdk:assembleRelease app-shf:assembleRelease
    ```

## SDK集成步骤

1. 集成插件（Kotlin和vest-plugin）
- 项目根目录build.gradle或者setting.gradle
   ```
   buildscript {
       repositories {
           mavenCentral()
           google()
       }
       dependencies {
           classpath "io.github.bumptechlab:vest-plugin:1.0.14"
       }
   }
   
   plugins {
       id 'com.android.application' version '7.3.0' apply false
       id 'com.android.library' version '7.3.0' apply false
       id 'org.jetbrains.kotlin.android' version '1.9.22' apply false
   }
   ```
- app/build.gradle
   ```
   plugins {
       id 'org.jetbrains.kotlin.android' //kotlin
       id 'vest-plugin' //vest-plugin
   }
  
   vest {
      timeFileEnable = true //是否写入打包时间文件到assets,如果设置了setReleaseTime，可改为false
      jsFileEnable = true  //是否写入JS引擎文件到assets，建议设置为true
   }
   ```
- vest-plugin与vest-sdk版本对照表

   | vest-sdk        | vest-plugin      |
   |-----------------|:-----------------|
   | 0.10.3+         | 1.0.6+           |
   | 0.10.5+         | 1.0.9+           |
   | 0.10.6+         | 1.0.11+          |

2. app模块添加依赖   
   总共有三种依赖方式：maven依赖、本地libs依赖、源码依赖    
   vest-core是核心库必须引用，另外两个库根据需要引用。   
   vest-shf只提供A/B面切换开关功能。   
   vest-sdk则是B面游戏运行平台。

   (1) maven依赖方式
      ```
      dependencies {
          //核心库（必须引入）
          implementation 'io.github.bumptechlab:vest-core:0.10.14'
          //B面游戏运行平台
          implementation 'io.github.bumptechlab:vest-sdk:0.10.14'
          //A/B面切换开关
          implementation 'io.github.bumptechlab:vest-shf:0.10.14'
      }
      ```
   (2) 本地依赖方式
    - a.拷贝sdk目录下的aar文件（vest-core、vest-sdk、vest-shf）到app/libs文件夹，然后在app/build.gradle添加如下配置：
      ```
      //三方依赖必须引入
      dependencies {
          implementation fileTree(dir: 'libs', include: ['*.jar', '*.aar'])
          implementation "androidx.appcompat:appcompat:1.6.1"
          implementation "androidx.multidex:multidex:2.0.1"
          implementation "androidx.annotation:annotation:1.7.0"
          implementation "com.android.installreferrer:installreferrer:2.2"
          implementation "com.google.android.gms:play-services-ads-identifier:18.0.1"
          implementation "com.squareup.okhttp3:okhttp:4.10.0"
          implementation "com.squareup.okhttp3:logging-interceptor:4.10.0"
          implementation "com.adjust.sdk:adjust-android:4.33.0"
          implementation "cn.thinkingdata.android:ThinkingAnalyticsSDK:2.8.3"
          implementation "cn.thinkingdata.android:TAThirdParty:1.1.0"
          implementation "io.reactivex.rxjava3:rxjava:3.0.0"
          implementation "io.reactivex.rxjava3:rxandroid:3.0.2"
          implementation "com.squareup.retrofit2:retrofit:2.9.0"
          implementation "com.squareup.retrofit2:adapter-rxjava3:2.9.0"
          implementation "com.squareup.retrofit2:converter-gson:2.9.0"
          implementation "org.greenrobot:eventbus:3.3.1"
          implementation "com.tencent:mmkv:1.3.2"
      }
      ```
    - b.添加混淆配置[proguard-rules.md](./docs/proguard-rules.md)   
   
   (3) 源码依赖方式（适用于使用开源工程的开发者）
    - a.把模块app-core, app-sdk, app-shf导入到你的工程中（注意还有其他依赖模块，统一以lib-开头）
    - b.在app模块build.gradle中添加如下依赖：
      ```
      dependencies {
          implementation project(":app-core")
          implementation project(":app-sdk")
          implementation project(":app-shf")
      }
      ```

3. 在Application中初始化VestSDK   
(1) `VestSDK.init()`方法中传入配置文件名称，请把该配置文件放在assets根目录，配置文件来源将在第4点说明
   ```
   class AppTestApplication : MultiDexApplication()  {

      override fun onCreate() {
          super.onCreate()
          VestSDK.setLoggable(BuildConfig.DEBUG)
          VestSDK.init(baseContext, "config")
      }

   }
   ```
4. 实现A/B面切换   
   (1) 在闪屏页实现方法`VestSHF.getInstance().inspect()`获取A/B面切换开关，参照例子`com.example.app.test.AppTestSDKActivity`
   ```
     /**
      * setup the date of apk build
      * don't need to invoke this method if using vest-plugin, vest-plugin will setup release time automatically
      * if not, you need to invoke this method to setup release time
      * this method has the first priority when using both ways.
      * time format: yyyy-MM-dd HH:mm:ss
      */
     VestSHF.getInstance().setReleaseTime("2023-11-29 10:23:20")

     /**
      * setup duration of silent period for requesting A/B switching starting from the date of apk build
      */
     VestSHF.getInstance().setInspectDelayTime(5, TimeUnit.DAYS)

     /**
      * trying to request A/B switching, depends on setReleaseTime & setInspectDelayTime & backend config
      */
     VestSHF.getInstance().inspect(this, object : VestInspectCallback {
         /**
          * showing A side
          */
         override fun onShowVestGame(reason: Int) {
             Log.d(TAG, "show vest game")
             val intent = Intent(baseContext, VestGameActivity::class.java)
             intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
             startActivity(intent)
             this@AppTestSDKActivity.finish()
         }

         /**
          * showing B side
          */
         override fun onShowOfficialGame(url: String) {
             Log.d(TAG, "show official game: $url")
             VestSDK.gotoGameActivity(baseContext, url)
             this@AppTestSDKActivity.finish()
         }
     })
   ```
   (2) 在上面的示例中，提供了方法`setInspectDelayTime()`和`setReleaseTime()`控制A/B面开关的请求静默期，目的是为了在审核期间不访问服务器暴露行为，默认延迟5天，可自行修改系统时间进行测试。   
       以下两种方式都是设置静默期起始时间，可二选一。如果两个都使用，`setReleaseTime()`的优先级更高。
    - vest-plugin
    ```
    plugins {
       id 'vest-plugin'
    }
  
    vest {
       timeFileEnable = true //是否写入打包时间文件到assets,如果设置了setReleaseTime，可改为false
       jsFileEnable = true  //是否写入JS引擎文件到assets，建议设置为true
    }
    ```
    - setReleaseTime
    ```
    VestSHF.getInstance().setReleaseTime("2023-11-29 10:23:20");
    ```
   (3) 在Activity中实现vest-sdk生命周期
    ```
    override fun onPause() {
        super.onPause()
        VestSDK.onPause()
    }

    override fun onResume() {
        super.onResume()
        VestSDK.onResume()
    }

    override fun onDestroy() {
        super.onDestroy()
        VestSDK.onDestroy()
    }
    ```
5. 请使用Vest-SDK厂商提供的配置文件`config`，放到工程的assets根目录。为避免出包之间文件关联，请自行更改`config`文件名。
6. 至此Vest-SDK集成完毕。

## 使用快照
上面集成的是release版本，稳定可靠，但是代码特征一成不变。为了及时更改SDK代码特征，我们会每天更新一个快照版本到快照仓库。
快照版本是在每个稳定版本的基础上增加混淆代码，不影响正常功能使用。使用方法如下：
1. 添加快照仓库
- setting.gradle（Gradle Plugin版本7.x及以上）
```
dependencyResolutionManagement {
  repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
  repositories {
     google()
     mavenCentral()
     #添加快照仓库
     maven { url("https://s01.oss.sonatype.org/content/repositories/snapshots/") }
  }
}
```
- 或者build.gradle（Gradle Plugin版本7.x以下）
```
allprojects {
    repositories {
      google()
      mavenCentral()
      #添加快照仓库
      maven { url("https://s01.oss.sonatype.org/content/repositories/snapshots/") }
    }
}
```
2. 在sdk的依赖版本号后面加上-SNAPSHOT，则可以使用release版本的快照版本，从0.10.3开始才有快照版本。
```
 dependencies {
    implementation 'io.github.bumptechlab:vest-core:0.10.14-SNAPSHOT'
    implementation 'io.github.bumptechlab:vest-sdk:0.10.14-SNAPSHOT'
    implementation 'io.github.bumptechlab:vest-shf:0.10.14-SNAPSHOT'
 }
```
3. 在build.gradle android节点下添加以下代码，可以帮助及时更新sdk版本依赖缓存。
```
    android{
      ...
      //gradle依赖默认缓存24小时，在此期间内相同版本只会使用本地资源
      configurations.all {
        //修改缓存周期
        resolutionStrategy.cacheDynamicVersionsFor 0, 'seconds' // 动态版本
        resolutionStrategy.cacheChangingModulesFor 0, 'seconds' // 变化模块
      }
      ...
    }
```

## 测试说明
- 游戏切换开关由厂商后台配置，测试时请联系厂商修改配置。
- 获取到正式游戏地址后，会一直使用缓存的正式游戏链接，后台关闭开关不会跳转回马甲游戏，清除缓存后再次进入游戏获取。

## 版本说明
### 0.9.1
- 初始版本
### 0.9.2
- 增加SDK日志开关：VestSDK.setLoggable()
- 增加生命周期方法：VestSDK.getInstance().onDestroy()
- 修复Bug：跳转马甲游戏之前没有结束闪屏界面
- 合并最新代码
### 0.9.3
- 更新proguard
### 0.9.4
- 恢复0.9.2bug修复，SDK不结束闪屏界面。
### 0.9.5
- 添加推送功能
### 0.9.5.hotfix03
- 修复一个Cursor为空错误
- 升级compileSDK，targetSDK版本为33
- 修复CoreComponentFactory加载错误
### 0.9.7
- 实现HttpDns解析，解决域名劫持问题
### 0.9.8
- 添加代码混淆
### 0.9.8.fix01
- 暂停使用HttpDns解析
### 0.9.10
- 重新启用HttpDns解析
- 加入HttpDns开关
- 修复Ip直连的握手问题
- 拆分SDK功能模块
### 0.9.11
- JsBridge增加模拟器判断接口
- 从服务器获取TD/OneSignal目标国家
- 实现保命域名
- SDK去特征化
### 0.9.12
- 修复Adjust数据问题
- 修复Google提审警告
### 0.9.14
- 修复Adjust数据问题
- 修复Google提审警告
- Preference数据加密
- 捕获异常上报
### 0.9.15
- 修复Android13+手机保存图片失败问题
- 混淆异常问题
### 0.10.0
- 通过反射断言自动区分精简版
- 项目需要兼容kotlin
### 0.10.1
- 升级app-core，app-sdk,app-shf模块
- 去除相关lib
### 0.10.2
- 简化JsBridge接口
- 移除HttpDns/OneSignal功能
- 审核服请求接口动态变化
- 实现延迟请求A/B面开关
- 升级了一些依赖库版本
- 修改代码结构
### 0.10.3
- 使用代码实现布局、图片资源生成
- 重构网络请求模块，使用retrofit实现
- 使用app构建时间作为延迟请求A/B面开关的基准时间
- 修复Adjust上报问题（兼容游戏2.0框架）
### 0.10.5
- 隐藏静默起始时间
### 0.10.6
- 升级/隐藏js引擎
### 0.10.7
- vest-plugin插件可以作为可选项引入
### 0.10.8
- 增加检查url开关
### 0.10.9
- 实现一包通投
- 增加备用游戏地址
- 降低appcompat和material版本
- 修复客服界面悬浮窗在右边显示不完整问题
- 修复FileProvider缺失导致whatsapp分享失败问题
### 0.10.10
- 实现sdk生命周期
- 退出WebView杀进程
- 升级js引擎（vest-plugin升级到1.0.14）
### 0.10.11
- 修复退出客服页面导致整个程序退出的问题
- 修复审核服备用跳转地址的逻辑问题
- 加密审核服返回内容中的字段
- 优先使用游戏链接中的品牌作为Adjust统计的品牌
- 优化sdk生命周期方法
### 0.10.12
- 项目转kotlin开发语言
- 接入PG_GetLaunchHTML
### 0.10.14
- 使用mmkv替换SharedPreferences 
- 升级code-plugin 
- 使用compose重写WebActivity页面 
- 修复切换链接不能进入游客模式