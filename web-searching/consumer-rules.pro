# Keep the plugin entry class and its constructor
-keep class com.mp.web_searching.WebSearchPlugin {
    public <init>(android.content.Context);
}

# Keep *all* constructors just in case (safer)
-keepclassmembers class com.mp.web_searching.WebSearchPlugin {
    public <init>(...);
}
-keep class com.dark.plugins.api.** { *; }


# Preserve the entire ComposePlugin interface and its implementations (including content())
-keep interface com.dark.plugins.api.ComposePlugin { *; }
-keepclassmembers class * implements com.dark.plugins.api.ComposePlugin {
    * content(...);
}
-keepclassmembers class * implements com.dark.plugins.api.ComposePlugin {
    public *** content(...);
}

-keep @androidx.annotation.Keep class * { *; }

# In case PluginApi is referenced directly
-keep class com.dark.plugins.api.PluginApi { *; }

# Preserve Kotlin runtime as before
-dontwarn kotlin.jvm.internal.**
-dontwarn kotlin.**
