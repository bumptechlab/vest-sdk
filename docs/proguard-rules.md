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

-dontwarn com.google.errorprone.annotations.Immutable
-keep class com.google.errorprone.annotations.Immutable
```