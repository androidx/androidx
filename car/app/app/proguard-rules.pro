# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

-keep class androidx.car.app.serialization.Bundleable
-keepclassmembers class * extends androidx.car.app.CarAppService
-keepclassmembers class androidx.car.app.FailureResponse
-keepclassmembers class androidx.car.app.SurfaceContainer
-keepclassmembers class androidx.car.app.CarContext {
    androidx.car.app.HostDispatcher mHostDispatcher;
}
-keepclassmembers class * extends androidx.car.app.Screen {
    androidx.car.app.CarContext mCarContext;
}
