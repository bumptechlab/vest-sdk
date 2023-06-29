# Vest-SDK
这是一个可以用于控制游戏跳转的三方依赖库

## 集成步骤

1. 添加依赖(maven依赖或者本地依赖)  
   a.maven依赖方式  
    1)在project根目录build.gradle或者setting.gradle中添加仓库
    ```
    repositories {
        mavenCentral()
        google()
        maven { url 'https://raw.githubusercontent.com/martinloren/AabResGuard/mvn-repo' }
    }
    ```
    2)添加依赖到工程`app/build.gradle`   
    ```
    implementation 'io.github.bumptechlab:vest-sdk:0.9.4'
    ```
   b.本地直接依赖方式   
    1)拷贝sdk目录下的aar文件到app/libs文件夹，然后在app/build.gradle添加如下配置：
    ```
    android {
      repositories {
          flatDir {
              dirs 'libs'
          }
      }
    }
   
    dependencies {
        implementation (name:'vest-sdk-GooglePlaySHF-v0.9.4-release',ext:'aar')
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
    }
    ```
   
2. 工程主Application继承`code.core.MainApplication`
- (1)重写方法`getConfigAsset`返回配置文件名，该配置文件放在assets目录，配置文件来源将在第4点说明
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
- (1)启动入口Activity继承`code.core.MainActivity`，参照例子`com.example.app.test.AppTestMainActivity`
  - 重写方法`getLayoutResource`可自定义布局  
  - 重写方法`onShowVestGame`跳转到马甲内容  
  - 方法`onShowOfficialGame`仅仅是一个跳转到正式游戏的回调，不需要实现  

- (2)在自己的Activity中实现`VestSDK.getInstance().inspect()`方法，参照例子`com.example.app.test.AppTestSDKActivity`  
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
- (3)请在Activity生命周期方法onDestroy()中调用VestSDK.getInstance().onDestroy()方法。

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
