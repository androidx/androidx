## These rules enable microbenchmarking from a minified test, as an experimental feature

# Custom JNI registration, so entire class must be kept
-keepclasseswithmembers class androidx.benchmark.BlackHole { *; }
-keepclasseswithmembers class androidx.benchmark.CpuCounterJni { *; }

# basic protection against junit/androidx.test reflection, shouldn't affect library/test code
-keepclasseswithmembers class androidx.test.** { *; }
-keepclasseswithmembers class org.junit.** { *; }
-keepclasseswithmembers class junit.** { *; }
-dontwarn com.google.errorprone.annotations.MustBeClosed
-dontwarn com.google.errorprone.annotations.CanIgnoreReturnValue

## keep test classes
-keepclasseswithmembers @org.junit.runner.RunWith class * { *; }

## needed for org.junit.Test annotation to be discoverable by reflection
-keepattributes AnnotationDefault,
                RuntimeVisibleAnnotations,
                RuntimeVisibleParameterAnnotations,
                RuntimeVisibleTypeAnnotations

## needed for listeners instantiated by reflection (e.g. InstrumentationResultsRunListener)
-keepclasseswithmembers class * extends androidx.test.internal.runner.listener.InstrumentationRunListener { *; }
-keepclasseswithmembers class * extends org.junit.runner.notification.RunListener { *; }

## Needed due to b/328649293 - shouldn't be needed since they're ref'd by manifest
## May need to leave these in place long term to account for old gradle versions
-keepclasseswithmembers class androidx.benchmark.junit4.AndroidBenchmarkRunner { *; }
-keepclasseswithmembers class androidx.benchmark.IsolationActivity { *; }

## Needed due to b/339085669 - shouldn't be needed as they're referenced by code
-keepclasseswithmembers class androidx.benchmark.json.BenchmarkData$TestResult$ProfilerOutput$Type { *; }