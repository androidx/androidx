# Web Test App

This is a simple integration test application for `androidx.web`.

## Running and Installing

To build and install the test application onto a connected device or emulator, run the following from the root of the `frameworks/support` tree:

```bash
./gradlew :web:integration-tests:testapp:installDebug
```

To launch the default `MainActivity` (which uses Compose to host the WebContentView) on your device, run:

```bash
adb shell am start -n androidx.web.testapp/.MainActivity
```

To launch the Java View + ViewModel sample activity (`JavaWebActivity`), run:

```bash
adb shell am start -n androidx.web.testapp/.JavaWebActivity
```

## Running Tests

To run the instrumentation tests (which are located in the library itself), run the following command:

```bash
ALLOW_PUBLIC_REPOS=true PROJECT_PREFIX=:web: ./gradlew :web:web:connectedAndroidTest
```
