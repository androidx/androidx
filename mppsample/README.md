# An Example project for Kotlin/MPP

From frameworks/support, try the following commands:
```
> ./gradlew :mppsample-library:jsTest
> ./gradlew :mppsample-library:jvmTest
> ./gradlew :mppsample-library:linuxTest         # Succeeds after running zero tests on Mac/Windows
> ./gradlew :mppsample-library:testDebugUnitTest # Android host-side test

// On Linux hosts only
> ./gradlew :mppsample-executable:runDebugExecutableNative
```

