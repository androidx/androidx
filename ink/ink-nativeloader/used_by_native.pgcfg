# Keep the attributes that contain annotations.
-keepattributes RuntimeVisible*Annotation*

# @UsedByNative should be used to annotate things referenced from name by JNI.
# This includes external methods in the Kotlin code and classes whose type
# is referenced by name in JNI C++ code, as well as any method that is looked
# up by name.
-if class androidx.ink.nativeloader.NativeLoader
-keep class androidx.ink.nativeloader.UsedByNative

# Keep annotated class names.
-if class androidx.ink.nativeloader.NativeLoader
-keepnames @androidx.ink.nativeloader.UsedByNative class * {
  <init>();
}

# Keep annotated class members if the class is kept. This is preserved not only
# from renaming but also from pruning, since some of the annotated methods may
# only be used as callbacks from native code.
-if class androidx.ink.nativeloader.NativeLoader
-keepclassmembers class * {
    @androidx.ink.nativeloader.UsedByNative *;
}
