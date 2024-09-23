# Keep ListenableWorker / Worker if not removed during shrinking
-keepnames class * extends androidx.work.ListenableWorker
# Keep all constructors on ListenableWorker, Worker if class is kept
-keepclassmembers public class * extends androidx.work.ListenableWorker {
    public <init>(...);
}

# We need to keep WorkerParameters for the ListenableWorker constructor that is used by the
# default instantiation strategy in WorkerFactory.
-keep class androidx.work.WorkerParameters

# Keep InputMerger if not removed during shrinking
-keepnames class * extends androidx.work.InputMerger
# Keep constructor on InputMerger if class is kept
-keepclassmembers class * extends androidx.work.InputMerger { void <init>(); }
