# Prevent the OpenXR classes from being obfuscated as they are created from native code.
-keep class androidx.xr.runtime.openxr.** { *; }
-keep class androidx.xr.runtime.openxr.**$* { *; }
-keep class * extends androidx.xr.runtime.openxr.** { *; }
-keep class * extends androidx.xr.runtime.openxr.**$* { *; }
