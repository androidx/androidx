# Keep all classes in the scenecore package. They can be used externally.
-keep class androidx.xr.scenecore.** { *; }
-keep class androidx.xr.scenecore.**$* { *; }
-keep class androidx.xr.scenecore.impl.** { *; }
-keep class androidx.xr.scenecore.impl.**$* { *; }

-keep class * extends androidx.xr.scenecore.** { *; }
-keep class * extends androidx.xr.scenecore.**$* { *; }
-keep class * extends androidx.xr.scenecore.impl.** { *; }
-keep class * extends androidx.xr.scenecore.impl.**$* { *; }
