# Vest-SDK

这是一个可以用于控制游戏跳转的三方依赖库，请按以下步骤集成：  

1. 把`sdk/vest-sdk-GooglePlaySHF-release.aar`复制到您的工程`app/libs`目录

2. 添加以下依赖项到工程`app/build.gradle`中  
    ```
    implementation 'androidx.multidex:multidex:2.0.1'  
    implementation 'com.android.installreferrer:installreferrer:2.2'  
    implementation 'com.google.android.gms:play-services-ads-identifier:18.0.1'  
    implementation 'com.google.code.gson:gson:2.9.0'  
    //OkHttp网络框架(不要随便变更版本)  
    implementation 'com.squareup.okhttp3:okhttp:3.12.2'  
    implementation 'com.squareup.okhttp3:logging-interceptor:3.12.2'  
    implementation 'androidx.annotation:annotation:1.5.0'  
    ```
3. 工程主Application继承`code.core.MainApplication`
  - 重写方法`getConfigAsset`返回配置文件名，该配置文件放在assets目录，配置文件来源将在第5点说明
    ```
    public class AppTestApplication extends MainApplication {  
          @Override  
          public String getConfigAsset() {  
              return "config";  
          }  
    }
    ```
4. 游戏跳转实现有两种实现方式：  
  - (1)启动入口Activity继承`code.core.MainActivity`，参照例子`com.example.app.test.AppTestMainActivity`
    - 重写方法`getLayoutResource`可自定义布局  
    - 重写方法`onShowVestGame`跳转到马甲游戏  
    - 方法`onShowOfficialGame`仅仅是一个跳转到正式游戏的回调，不需要实现  

  - (2)在自己的Activity中实现`VestSDK.getInstance().inspect()`方法，参照例子`com.example.app.test.AppTestSDKActivity`  
    ```
    VestSDK.getInstance().inspect(this, new VestInspectCallback() {  
                     
            @Override  
            public void onShowVestGame() {  
                Log.d(TAG, "show vest game");  
            }  
    
            @Override  
            public void onShowOfficialGame() {  
                Log.d(TAG, "show official game");  
            }  
        });  
    ```

5. 请使用Vest-SDK厂商提供的配置文件`config`，放到工程的assets目录。  
为避免出包之间文件关联，可以更改`config`文件名，并注意修改`code.core.MainApplication`的重载方法`getConfigAsset`返回值。
6. 至此Vest-SDK集成完毕。
