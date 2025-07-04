# Keep all classes in the extensions and scenecore packages since they can be used externally
-keep public class androidx.xr.extensions.** { *; }
-keep public interface androidx.xr.extensions.** { *; }
-keep @interface androidx.xr.extensions.**
# Prevent internal implementations of com.android.xr.extensions.function.Consumer.accept from being
# stripped when being referenced as a synthetic lambda.
-keepclassmembers class androidx.xr.extensions.** {
  public void accept(***);
}

-keep class androidx.xr.scenecore.** { *; }
-keep class androidx.xr.scenecore.**$* { *; }
-keep class androidx.xr.scenecore.impl.** { *; }
-keep class androidx.xr.scenecore.impl.**$* { *; }

-keep class * extends androidx.xr.scenecore.** { *; }
-keep class * extends androidx.xr.scenecore.**$* { *; }
-keep class * extends androidx.xr.scenecore.impl.** { *; }
-keep class * extends androidx.xr.scenecore.impl.**$* { *; }
