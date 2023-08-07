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