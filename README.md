# Vest-SDK

这是一个可以用于控制游戏跳转的三方依赖库，请按以下步骤集成：  

1. 添加依赖到工程`app/build.gradle`中  
    ```
    implementation 'io.github.bumptechlab:vest-sdk:0.9.1'
    ```
2. 工程主Application继承`code.core.MainApplication`
  - 重写方法`getConfigAsset`返回配置文件名，该配置文件放在assets目录，配置文件来源将在第5点说明
    ```
    public class AppTestApplication extends MainApplication {  
          @Override  
          public String getConfigAsset() {  
              return "config";  
          }  
    }
    ```
3. 游戏跳转实现有两种实现方式：  
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

4. 请使用Vest-SDK厂商提供的配置文件`config`，放到工程的assets目录。  
为避免出包之间文件关联，可以更改`config`文件名，并注意修改`code.core.MainApplication`的重载方法`getConfigAsset`返回值。
5. 至此Vest-SDK集成完毕。
