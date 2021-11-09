# Keep class names of Hilt injected Workers since their name are used as a multibinding map key.
-keepnames @androidx.hilt.work.HiltWorker class * extends androidx.work.ListenableWorker