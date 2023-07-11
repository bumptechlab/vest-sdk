# Vest-SDK
这是一个可以用于控制游戏跳转的三方依赖库，最新版本：0.9.5

## 集成步骤

1. 添加依赖(maven依赖或者本地依赖)   
   (1) maven依赖方式   
   - a.在project根目录build.gradle或者setting.gradle中添加仓库
     ```
     repositories {
       mavenCentral()
       google()
       maven { url 'https://raw.githubusercontent.com/martinloren/AabResGuard/mvn-repo' }
     }
     ```
   - b.添加依赖到工程`app/build.gradle`   
     ```
     implementation 'io.github.bumptechlab:vest-sdk:0.9.5'
     ```
   (2) 本地依赖方式   
   - a.拷贝sdk目录下的aar文件到app/libs文件夹，然后在app/build.gradle添加如下配置：
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
         implementation (name:'vest-sdk-GooglePlaySHF-v0.9.5-release',ext:'aar')
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
   
     # antifake SDK
     -keep class com.snail.antifake.jni.* {
       native <methods>;
     }
   
     -keepclassmembers class * implements java.io.Serializable {
       private static final java.io.ObjectStreamField[] serialPersistentFields;
       private void writeObject(java.io.ObjectOutputStream);
       private void readObject(java.io.ObjectInputStream);
       java.lang.Object writeReplace();
       java.lang.Object readResolve();
     }
   
     -keepnames class com.facebook.FacebookActivity
     -keepnames class com.facebook.CustomTabActivity
     -keep class com.facebook.login.Login
     ```
   
2. 工程主Application继承`code.core.MainApplication`   
   (1) 重写方法`getConfigAsset`返回配置文件名，该配置文件放在assets目录，配置文件来源将在第4点说明   
   ```
   public class AppTestApplication extends MainApplication {
    
        @Override
        public void onCreate() {
            super.onCreate();
            //输出sdk日志开关，release模式请关闭
            VestSDK.setLoggable(BuildConfig.DEBUG);
        }
    
        @Override
        public String getConfigAsset() {
            return "config";
        }
   }
   ```
3. 游戏跳转实现有两种实现方式：  
   (1) 启动入口Activity继承`code.core.MainActivity`，参照例子`com.example.app.test.AppTestMainActivity`   
   - 重写方法`getLayoutResource`可自定义布局  
   - 重写方法`onShowVestGame`跳转到马甲内容  
   - 方法`onShowOfficialGame`仅仅是一个跳转到正式游戏的回调，不需要实现  

   (2) 在自己的Activity中实现`VestSDK.getInstance().inspect()`方法，参照例子`com.example.app.test.AppTestSDKActivity`  
   ```
   VestSDK.getInstance().inspect(this, new VestInspectCallback() {  
                     
         @Override  
         public void onShowVestGame() {  
             Log.d(TAG, "show vest game");
             Intent intent = new Intent(getBaseContext(), VestGameActivity.class);
             intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
             startActivity(intent);  
         }  
    
         @Override  
         public void onShowOfficialGame() {  
             Log.d(TAG, "show official game");  
         }  
   });  
   ```
   (3) 请在Activity生命周期方法onDestroy()中调用VestSDK.getInstance().onDestroy()方法。

4. 请使用Vest-SDK厂商提供的配置文件`config`，放到工程的assets目录。为避免出包之间文件关联，可以更改`config`文件名，并注意修改`code.core.MainApplication`的重载方法`getConfigAsset`返回值。
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
