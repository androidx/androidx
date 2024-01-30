# Keep class names of kotlin classes refered to by JNI code.
# Each class requires 2 proguard rules, one to match on the class and
# the other to match on each method referenced by native code.
-if class androidx.graphics.surface.SurfaceControlCompat
-keep @androidx.graphics.utils.JniVisible public class *

-if class androidx.graphics.surface.SurfaceControlCompat
-keepclasseswithmembers class * {
    @androidx.graphics.utils.JniVisible *;
}

-if class androidx.hardware.SyncFenceCompat
-keep @androidx.graphics.utils.JniVisible public class *

-if class androidx.hardware.SyncFenceCompat
-keepclasseswithmembers class * {
    @androidx.graphics.utils.JniVisible *;
}

-if class androidx.graphics.lowlatency.CanvasFrontBufferedRenderer
-keep @androidx.graphics.utils.JniVisible public class *

-if class androidx.graphics.lowlatency.CanvasFrontBufferedRenderer
-keepclasseswithmembers class * {
    @androidx.graphics.utils.JniVisible *;
}

-if class androidx.graphics.lowlatency.GLFrontBufferedRenderer
-keep @androidx.graphics.utils.JniVisible public class *

-if class androidx.graphics.lowlatency.GLFrontBufferedRenderer
-keepclasseswithmembers class * {
    @androidx.graphics.utils.JniVisible *;
}

-if class androidx.opengl.EGLExt
-keep @androidx.graphics.utils.JniVisible public class *

-if class androidx.opengl.EGLExt
-keepclasseswithmembers class * {
    @androidx.graphics.utils.JniVisible *;
}
