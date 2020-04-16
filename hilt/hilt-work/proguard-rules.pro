# Keep class names of Hilt injected Workers since their name are used as a multibinding map key.
-keepclasseswithmembernames class * extends androidx.work.Worker {
    @androidx.hilt.work.WorkerInject
    <init>(...);
}