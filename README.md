# Vest-SDK
这是一个可以用于控制游戏跳转的三方依赖库

## 集成步骤

1. 添加依赖到工程`app/build.gradle`中  
    ```
    implementation 'io.github.bumptechlab:vest-sdk:0.9.2'
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
- (3)请在Activity生命周期方法onDestroy()中调用VestSDK.getInstance().onDestroy()方法。

5. 请使用Vest-SDK厂商提供的配置文件`config`，放到工程的assets目录。  
为避免出包之间文件关联，可以更改`config`文件名，并注意修改`code.core.MainApplication`的重载方法`getConfigAsset`返回值。
6. 至此Vest-SDK集成完毕。

## 版本说明
### 0.9.1
- 初始版本
### 0.9.2
- 增加SDK日志开关：VestSDK.setLoggable()
- 增加生命周期方法：VestSDK.getInstance().onDestroy()
- 修复：跳转马甲游戏之前没有结束闪屏界面
- 合并最新代码
