# androidx.xr.runtime.DisplayBlendMode is referenced by native code.
-keep class androidx.xr.runtime.DisplayBlendMode { *; }
-keep class androidx.xr.runtime.DisplayBlendMode$Companion { *; }
# androidx.xr.runtime.FieldOfView is referenced by native code.
-keep class androidx.xr.runtime.FieldOfView { *; }
# androidx.xr.runtime.Log is referenced by native code.
-keep class androidx.xr.runtime.Log { *; }
# androidx.xr.runtime.TrackingState is referenced by native code.
-keep class androidx.xr.runtime.TrackingState { *; }
-keep class androidx.xr.runtime.TrackingState$Companion { *; }
# androidx.xr.runtime.math.FloatSize2d is referenced by native code.
-keep class androidx.xr.runtime.math.FloatSize2d { *; }
# androidx.xr.runtime.math.FloatSize3d is referenced by native code.
-keep class androidx.xr.runtime.math.FloatSize3d { *; }
# androidx.xr.runtime.math.GeospatialPose is referenced by native code.
-keep class androidx.xr.runtime.math.GeospatialPose { *; }
# androidx.xr.runtime.math.IntSize2d is referenced by native code.
-keep class androidx.xr.runtime.math.IntSize2d { *; }
# androidx.xr.runtime.math.Pose is referenced by native code.
-keep class androidx.xr.runtime.math.Pose { *; }
# androidx.xr.runtime.math.Quaternion is referenced by native code.
-keep class androidx.xr.runtime.math.Quaternion { *; }
# androidx.xr.runtime.math.Vector2 is referenced by native code.
-keep class androidx.xr.runtime.math.Vector2 { *; }
# androidx.xr.runtime.math.Vector2 is referenced by native code.
-keep class androidx.xr.runtime.math.Vector3 { *; }

# Preserve implementations of the various factory interfaces, as these are
# instantiated via reflection and not directly.
-keep class androidx.xr.runtime.internal.PerceptionRuntimeFactory { *; }
-keep class * implements androidx.xr.runtime.internal.PerceptionRuntimeFactory { *; }
-keep class androidx.xr.runtime.internal.RenderingRuntimeFactory { *; }
-keep class * implements androidx.xr.runtime.internal.RenderingRuntimeFactory { *; }
-keep class androidx.xr.runtime.internal.SceneRuntimeFactory { *; }
-keep class * implements androidx.xr.runtime.internal.SceneRuntimeFactory { *; }

# Preserve StateExtender and its implementations, as they're looked up via
# a service locator.
-keep class androidx.xr.runtime.StateExtender { *; }
-keep class * implements androidx.xr.runtime.StateExtender { *; }