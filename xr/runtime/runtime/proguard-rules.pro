# Prevent the Runtime, Internal, and Math classes from being obfuscated as they
# are created from native code.
-keep class androidx.xr.runtime.** { *; }
-keep class androidx.xr.runtime.**$* { *; }
-keep class androidx.xr.runtime.math.** { *; }
-keep class androidx.xr.runtime.math.**$* { *; }
-keep class * extends androidx.xr.runtime.** { *; }
-keep class * extends androidx.xr.runtime.**$* { *; }
-keep class * extends androidx.xr.runtime.math.** { *; }
-keep class * extends androidx.xr.runtime.math.**$* { *; }
