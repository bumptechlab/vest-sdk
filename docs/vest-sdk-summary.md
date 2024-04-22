# Vest-SDK
### 一. 前言
本文档主要用于介绍SDK的使用以及讲解工程结构，最后给出了过包方案。   
SDK最新版本：1.2.6

### 二．SDK简介
Vest-SDK由4个依赖库组成，分别是：
1. vest-core核心库，主要是一些工具类方法和Adjust、Thinking Data数据记录模块。
2. vest-shf实现了SHF的A/B面切换开关，该开关配置在服务器端，需要联网才能获得。
3. vest-firebase实现了Firebase的A/B面切换开关，该开关配置在Firebase，需要联网才能获得。
4. vest-sdk实现游戏展示框架，只支持显示H5游戏。

注意： vest-firebase和vest-shf两种开关方式只能二选一，根据过包情况自由选择。Firebase在控制台可按照国家进行配置。

### 三．SDK使用
1. 把加密后的配置放到assets目录，并自行修改配置文件名
2. Application中初始化SDK，传入配置文件名
   ``` kotlin
   //传入配置文件
   VestSDK.init(getBaseContext(), "config");
   //设置包的发布模式：马甲包和渠道包
   VestSDK.setReleaseMode(VestReleaseMode.MODE_VEST)
   ```
3. 其中config文件的原始内容在使用Firebase控制和SHF控制的时候各有不同：
- Firebase控制：
   ``` json
    {
        "channel": "website",
        "brand": "test",
        "adjust_app_id": "5f4qg9uhutts",
        "adjust_event_start": "fq5h6s",
        "adjust_event_greeting": "2zmcn8",
        "adjust_event_access": "iuj12u",
        "adjust_event_updated": "oc5lmj"
    }
   ```
- SHF控制：
   ``` json
    {
        "channel": "website",
        "brand": "test",
        "shf_base_domain": "https://shf.test.baowengame.com",
        "shf_spare_domains": [
            "https://www.ozt4axm9.com",
            "https://www.6r4hx6e2.com",
            "https://www.cictnjac.com"
        ],
        "shf_dispatcher": "/4dbcdda313/e74a32918b/df14abf6ce87",
        "adjust_app_id": "5f4qg9uhutts",
        "adjust_event_start": "fq5h6s",
        "adjust_event_greeting": "2zmcn8",
        "adjust_event_access": "iuj12u",
        "adjust_event_updated": "oc5lmj"
    }
   ```
数据分为三大块   
（1）A/B面开关参数，每个品牌需要更换

| 字段                | 说明                                                      |
|--------------------|---------------------------------------------------------|
| channel            | 渠道号，功能上没有用到，但是也要按照厂商给的填写正确                              |
| brand              | 品牌号，开关按照品牌独立返回                                          |
| shf_base_domain    | 开关服务器主域名，每个品牌配置一个                                       |
| shf_spare_domains  | 备用服务器域名，当主域名无法访问时，轮询访问备用域名，备用域名每个品牌都是一样，主要是为了减少域名的购买成本。 |
| shf_dispatcher     | 开关请求API路径，是一个动态加密路径，由厂商后台提供                             |


（2）Adjust统计的有关参数，每个品牌需要更换

| 字段                   | 说明                     |
|-----------------------|:------------------------|
| adjust_app_id         | 用于初始化Adjust SDK      |
| adjust_event_start    | 记录程序首次启动次数        |
| adjust_event_greeting | 记录A/B开关请求成功事件     |
| adjust_event_access   | 记录进入B面游戏事件         |
| adjust_event_updated  | 没有用                    |


4. 实现A/B面切换，开关在厂商后台控制。   
   打开开关表示跳转到B面，回调方法onShowBSide   
   关闭开关表示跳转到A面，回调方法onShowASide   
   为了在审核期间不暴露请求API，还可以设置请求发起的延迟时间
- SHF控制示例代码：
    ```kotlin
    VestSHF.getInstance().apply {
        //设置APK构建时间
        setReleaseTime("2023-11-29 10:23:20")

        //设置延迟发起A/B请求的时间
        setInspectDelayTime(5, TimeUnit.DAYS)

        //设置是否检查SHF返回的URL合法性
        setCheckUrl(true)

        //开始请求A/B开关
    }.inspect(this, object : VestInspectCallback {
        //显示A面
        override fun onShowASide(reason: Int) {
            Log.d(TAG, "show A-side activity")
            gotoASide()
            finish()
        }

        //SDK内部执行了B面跳转，跳转结果通过launchResult给出，如果跳转不成功需要展示A面
        override fun onShowBSide(url: String, launchResult: Boolean) {
            Log.d(TAG, "show B-side activity: $url, result: $launchResult")
            if (!launchResult) {
                gotoASide()
            }
            finish()
        }

    })
    ```
- Firebase控制示例代码：
    ```kotlin
    VestFirebase.getInstance().apply {
        //设置APK构建时间
        setReleaseTime("2023-11-29 10:23:20")

        //设置延迟发起A/B请求的时间
        setInspectDelayTime(0, TimeUnit.DAYS)

    }.inspect(this, object : VestInspectCallback {

        //显示A面
        override fun onShowASide(reason: Int) {
            Log.d(TAG, "show A-side activity")
            gotoASide()
            finish()
        }

        //显示B面
        override fun onShowBSide(url: String, launchResult: Boolean) {
            Log.d(TAG, "show B-side activity: $url, result: $launchResult")
            if (!launchResult) {
                gotoASide()
            }
            finish()
        }

    })   
   ```

### 四．SDK功能模块说明
#### 1. Adjust统计，实现类book.sdk.core.manager.AdjustManager
在vest-core中，主要用于统计有关事件。

#### 2. BridgeInterface只保留了15个基本接口（为了消除恶意软件提醒），实现类book.sdk.bridge.JsBridgeImpl
实现B面游戏在WebView中与Android原生环境的互相调用。

#### 3. WebView，实现类book.sdk.ui.WebActivity
用于展示B面游戏的UI实现

#### 4. 配置存储中心，实现类code.sdk.core.util.ConfigPreference
用来存储从assets读取到的配置，也就是VestSDK.init(getBaseContext(), "config")传入的配置。
之所以要存储起来是为了让在vest-sdk和vest-shf中都能读取到配置，因为vest-sdk和vest-shf作为独立的sdk，无法与vest-core共享内存，只能用Preference作为中介实现配置共享。

### 五．关于过包技巧
#### 1. SDK代码混淆
SDK本身不提供代码混淆，要是审核遇到问题，可以尝试修改工程代码，业务代码不建议修改。可以自己添加类或者方法，并确保被业务代码引用，可以到github找一些看起来写得不怎么样的代码。

最好有50%的业务方法能引用到垃圾代码，这都是要手动加的，这样就能在代码审查中看到比较复杂的类引用关系。引用垃圾代码时，要保证那些垃圾代码不会被执行。可以用一个if判断永假来实现，如下代码：

- JunkCode.java（垃圾代码的永假实现示例）

   ``` java
   public class JunkCode extends Activity {
       public static final String TAG = "CzlfbdjoActivity";
   
       public static void inject() {
           if (TAG.compareTo("hxpdfskevuwqmatbnyjl") == 0) {
               //引用的垃圾代码，这里永远不执行
               ImtavxoActivity instanceImtavxoActivity = new ImtavxoActivity();
               instanceImtavxoActivity.kosjbfp();
           } else {
               // else的情况要实现，要不然proguard混淆阶段会因为if的永假条件，代码会被删除掉
               // 最好是不涉及功能的，打印日志，setProperty都可以
               System.setProperty("property1", "1");
           }
       }
   }
   ```

- FileUtil.java（SDK的业务代码，引用了永假条件的垃圾代码）

   ``` java
   public final class FileUtil {
       public static void ensureFile(File file) {
           //这里引用垃圾代码
           JunkCode.inject();
           if (file != null && !file.exists()) {
               ensureDirectory(file.getParentFile());
               try {
                   file.createNewFile();
               } catch (IOException e) {
                   e.printStackTrace();
               }
           }
       }
       ...
   }
   ```




### 六．SDK发布详见文档：[maven-publish.md](maven-publish.md)
