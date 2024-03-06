## These rules enable microbenchmarking from a minified test, as an experimental feature

# basic protection against junit/androidx.test reflection, shouldn't affect library/test code
-keepclasseswithmembers class androidx.test.** { *; }
-keepclasseswithmembers class org.junit.** { *; }
-keepclasseswithmembers class junit.** { *; }
-dontwarn com.google.errorprone.annotations.MustBeClosed

## keep test classes
-keepclasseswithmembers @org.junit.runner.RunWith class * { *; }

## needed for org.junit.Test annotation to be discoverable by reflection
-keepattributes *Annotation*

## Needed due to b/328649293 - shouldn't be needed since they're ref'd by manifest
## May need to leave these in place long term to account for old gradle versions
-keepclasseswithmembers class androidx.benchmark.junit4.AndroidBenchmarkRunner { *; }
-keepclasseswithmembers class androidx.benchmark.IsolationActivity { *; }
