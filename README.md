# Vest-SDK
这是一个可以用于控制游戏跳转的三方依赖库，最新版本：0.9.10   
SDK总共三个依赖库：  
vest-core: 项目运行所必须的核心库（必须引入）  
vest-sdk: 运行B面游戏的平台  
vest-shf: 用于切换A/B面的远程开关

## 开发环境
- JdkVersion:  11
- GradleVersion: 7.3.3
- GradlePluginVersion: 4.2.2
- minSdkVersion    : 19  
- targetSdkVersion : 33  
- compileSdkVersion: 33  

## SDK集成步骤

1. 添加依赖(maven依赖或者本地依赖)。   
   vest-core是核心库必须引用，另外两个库根据需要引用。   
   vest-shf只提供A/B面切换开关功能，vest-sdk则是B面游戏运行平台。   

   (1) maven依赖方式   
   - a.在project根目录build.gradle或者setting.gradle中添加仓库
     ```
     repositories {
       mavenCentral()
       google()
     }
     ```
   - b.添加依赖到工程`app/build.gradle`   
     ```
     //核心库（必须引入）
     implementation 'io.github.bumptechlab:vest-core:0.9.10'
     //B面游戏运行平台
     implementation 'io.github.bumptechlab:vest-sdk:0.9.10'
     //A/B面切换开关
     implementation 'io.github.bumptechlab:vest-shf:0.9.10'
     ```
   (2) 本地依赖方式   
     - a.拷贝sdk目录下的aar文件（vest-core、vest-sdk、vest-shf）到app/libs文件夹，然后在app/build.gradle添加如下配置：
       ```
       //根据gradle版本决定是否需要指定libs目录（一般不需要）
       android {
         repositories {
           flatDir {
             dirs 'libs'
           }
         }
       }
       ```
       ```
       //三方依赖必须引入
       dependencies {
           implementation fileTree(dir: 'libs', include: ['*.jar', '*.aar'])
           implementation 'com.google.android.material:material:1.5.0'
           implementation 'androidx.multidex:multidex:2.0.1'
           implementation 'androidx.annotation:annotation:1.5.0'
           implementation 'com.android.installreferrer:installreferrer:2.2'
           implementation 'com.google.android.gms:play-services-ads-identifier:18.0.1'
           implementation 'com.squareup.okhttp3:okhttp:3.12.2'
           implementation 'com.squareup.okhttp3:logging-interceptor:3.12.2'
           implementation 'com.google.code.gson:gson:2.9.0'
           implementation 'com.adjust.sdk:adjust-android:4.33.0'
           implementation 'cn.thinkingdata.android:ThinkingAnalyticsSDK:2.8.3'
           implementation 'cn.thinkingdata.android:TAThirdParty:1.1.0'
           implementation 'com.onesignal:OneSignal:4.8.6'
           implementation 'io.github.dnspod:httpdns-sdk:4.4.0-intl'
           implementation 'androidx.room:room-rxjava2:2.1.0'
       }
       ```
   - b.添加混淆配置   
     ```
     # --------------------------------------------基本指令区--------------------------------------------#
     -optimizationpasses 5                               # 指定代码的压缩级别(在0~7之间，默认为5)
     -dontusemixedcaseclassnames                         # 是否使用大小写混合(windows大小写不敏感，建议加入)
     -verbose                                            # 混淆时是否记录日志(混淆后会生成映射文件)
   
     # --------------------------------------------可定制化区--------------------------------------------#
     # 基于annotation的keep(而非目录)
     -keepattributes Annotation
     -keep @androidx.annotation.Keep class ** {
       @androidx.annotation.Keep <fields>;
       @androidx.annotation.Keep <methods>;
     }
     -keepclassmembers class * {
       @android.webkit.JavascriptInterface <methods>;
     }
   
     -keep class * implements android.os.Parcelable {
       public static final android.os.Parcelable$Creator *;
     }
   
     # Proguard Android Webivew for release
     -keep public class android.net.http.SslError
     -keep public class android.webkit.WebViewClient
     -dontwarn android.net.http.SslError
     -dontwarn android.webkit.WebView
     -dontwarn android.webkit.WebViewClient
   
     # webview needs to choose photo from gallery (for 5.0 below)
     -keepclassmembers class * extends android.webkit.WebChromeClient {
       public void openFileChooser(...);
     }
   
     # adjust SDK
     -keep class com.adjust.sdk.**{ *; }
     -keep class com.google.android.gms.common.ConnectionResult {
       int SUCCESS;
     }
     -keep class com.google.android.gms.ads.identifier.AdvertisingIdClient {
       com.google.android.gms.ads.identifier.AdvertisingIdClient$Info getAdvertisingIdInfo(android.content.Context);
     }
     -keep class com.google.android.gms.ads.identifier.AdvertisingIdClient$Info {
       java.lang.String getId();
       boolean isLimitAdTrackingEnabled();
     }
     -keep public class com.android.installreferrer.**{ *; }
   
     -keepclassmembers class * implements java.io.Serializable {
       private static final java.io.ObjectStreamField[] serialPersistentFields;
       private void writeObject(java.io.ObjectOutputStream);
       private void readObject(java.io.ObjectInputStream);
       java.lang.Object writeReplace();
       java.lang.Object readResolve();
     }

     ```
   
2. 在Application中初始化VestSDK   
   (1) `VestSDK.init()`方法中传入配置文件名称，请把该配置文件放在assets根目录，配置文件来源将在第4点说明   
   ```
   public class AppTestApplication extends MultiDexApplication {

      @Override
      public void onCreate() {
          super.onCreate();
          VestSDK.init(getBaseContext(), "config-test");
          VestSDK.setLoggable(BuildConfig.DEBUG);
      }

   }
   ```
3. 实现A/B面切换   
   (1) 在闪屏页实现方法`VestSDK.getInstance().inspect()`获取A/B面切换开关，参照例子`com.example.app.test.AppTestSDKActivity`  
   ```
   VestSDK.getInstance().inspect(new VestInspectCallback() {
      //这里跳转到A面，A面请自行实现
      @Override
      public void onShowVestGame() {
          Log.d(TAG, "show vest game");
          Intent intent = new Intent(getBaseContext(), VestGameActivity.class);
          intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
          startActivity(intent);
      }

      //这里跳转到B面，B面由SDK提供，使用VestSDK.gotoGameActivity()方法跳转
      @Override
      public void onShowOfficialGame(String url) {
          Log.d(TAG, "show official game: " + url);
          VestSDK.gotoGameActivity(getBaseContext(), url);
          AppTestSDKActivity.this.finish();
      }
   }); 
   ```
   (2) 请在Activity生命周期方法onDestroy()中调用VestSDK.getInstance().onDestroy()方法。

4. 请使用Vest-SDK厂商提供的配置文件`config`，放到工程的assets根目录。为避免出包之间文件关联，请自行更改`config`文件名。
5. 至此Vest-SDK集成完毕。

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
