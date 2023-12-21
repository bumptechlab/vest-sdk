```
-optimizationpasses 5
-dontusemixedcaseclassnames
-verbose

# @Keep rules
-keepattributes Annotation
-keep @androidx.annotation.Keep class ** {
    @androidx.annotation.Keep <fields>;
    @androidx.annotation.Keep <methods>;
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

-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# Proguard Apache HTTP for release
-keep class org.apache.http.** { *; }
-dontwarn org.apache.http.**

# Proguard okhttp for release
-keep class okhttp3.** { *; }
-dontwarn okhttp3.**

-keep class okio.** { *; }
-dontwarn okio.**

# Proguard rxJava for release
-keep class io.reactivex.rxjava3.** { *; }
-dontwarn io.reactivex.rxjava3.**

# adjust sdk start
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

# adjust sdk end

-dontwarn com.google.errorprone.annotations.Immutable
-keep class com.google.errorprone.annotations.Immutable

-dontwarn com.google.errorprone.annotations.CanIgnoreReturnValue
-dontwarn com.google.errorprone.annotations.CheckReturnValue
-dontwarn com.google.errorprone.annotations.RestrictedApi
```