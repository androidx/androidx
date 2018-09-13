-keep class * extends androidx.work.Worker
-keep class * extends androidx.work.InputMerger
# Worker#internalInit is marked as @Keep
# We need to keep WorkParameters for the method descriptor of internalInit.
-keep class androidx.work.WorkParameters
# We reflectively try and instantiate FirebaseJobScheduler when we find a Firebase dependency
# on the classpath.
-keep class androidx.work.impl.background.firebase.FirebaseJobScheduler
