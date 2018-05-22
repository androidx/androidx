-assumenosideeffects class androidx.work.impl.logger.InternalLogger {
 public void verbose(...);
 public void debug(...);
 public void info(...);
}

-keep class * extends androidx.work.Worker
-keep class * extends androidx.work.InputMerger
# Worker#internalInit is marked as @Keep
# We need to keep Data and Extras for the method descriptor of internalInit.
-keep class androidx.work.Data
-keep class androidx.work.impl.Extras
# We reflectively try and instantiate FirebaseJobScheduler when we find a Firebase dependency
# on the classpath.
-keep class androidx.work.impl.background.firebase.FirebaseJobScheduler
