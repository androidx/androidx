## Below are internal workarounds for b/328649293
## These can't be in benchmark/, since they shouldn't ship to public consumers, but these should all
## be discoverable / kept from the test manifest
-keepclasseswithmembers class androidx.startup.InitializationProvider { *; }
-keepclasseswithmembers class androidx.activity.ComponentActivity { *; }
