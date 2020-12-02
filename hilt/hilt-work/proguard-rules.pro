# Keep class names of Hilt injected Workers since their name are used as a multibinding map key.
-keepclasseswithmembernames class * extends androidx.work.ListenableWorker {
    @androidx.hilt.work.WorkerInject
    <init>(...);
}