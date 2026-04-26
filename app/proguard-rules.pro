# Keep RootEncoder classes
-keep class com.pedro.** { *; }

# Keep Retrofit + Gson model classes
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.padelcamera.app.api.model.** { *; }
-keep class com.padelcamera.app.config.AppConfig { *; }
