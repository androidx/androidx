Android for Cars App Library Samples
===========================================
This directory contains sample apps that use Android for Cars App Library.
Please find the library documentation at https://developer.android.com/training/cars/navigation.

Build and Run From Android Studio
-----------------------
In order to build app APKs,

1. Open the [AndroidX project][5] in Android Studio.

2. Select a sample app directory (e.g. `helloworld-mobile`) in the `Project Structure` UI on the top
 left. Each sample app has two build targets, one for the mobile platform and the other for the
  automotive platform.

3. Go to `Build -> Make Module 'androidx.car.app.app-samples.<sample_app>-<platform>'`. The apks
 will be generated in `<repo root>/out/androidx/car/app/app-samples/<sample_app>-<platform>/build
 /outputs/apk`.

In order to install and run the apps,

1. Open the [AndroidX project][5] in Android Studio.

2. Go to `Run -> Edit Configurations`, select a sample app target (e.g. `car.app.app-samples
.helloworld-mobile`). If you are building for the `mobile` platform, also select `Launch: Nothing
` in `General -> Launch Options`. Click `OK` to close the dialog.

3. Select `Run -> Run <sample_app>`, to run the app, which will just install it in the selected
 device.

Run From the Command Line
---------------------
Run `./gradlew :car:app:app-samples:<sample_app>-<platform>:assemble` to assemble the APKs. E.g.

```bash
./gradlew :car:app:app-samples:helloworld-mobile:assemble
 ./gradlew :car:app:app-samples:helloworld-automotive:assemble
```

The APKs will be generated in `<repo root>/out/androidx/car/app/app-samples/<sample_app>-<platform>/build/outputs/apk`.

Install the APK with ADB:

```bash
adb install <path_to_sample_app_apk>
```

In order to install to a device directly, run the `installDebug` or `installRelease` gradle task
. E.g.

```bash
./gradlew :car:app:app-samples:helloworld-mobile:installDebug
./gradlew :car:app:app-samples:helloworld-automotive:installRelease
```

Run the Mobile App in the Desktop Head Unit (DHU)
-------------------------------------------
Follow the instructions in [Test Android apps for cars][1] to run the sample apps in the DHU.

In short:

1. [Enable the Android Developer Settings][2]
2. [Enable Unknown Sources in Android Auto][3]
3. [Run the DHU][4]

**Note**: In Android Q, there is no Android Auto app in the launcher. The way to get to the settings in that case is through Settings -> Apps & Notifications -> See all apps -> Android Auto -> Advanced -> Additional settings in the app.

**Important**: The host component required to support car apps in the Automotive OS emulator has not yet been released, and hence these samples will not work in the Automotive OS emulator.

[1]: https://developer.android.com/training/cars/testing
[2]: https://developer.android.com/studio/debug/dev-options
[3]: https://developer.android.com/training/cars/testing#step1
[4]: https://developer.android.com/training/cars/testing#running-dhu
[5]: https://android.googlesource.com/platform/frameworks/support/+/ac0c1d466abcfdcab2babb2e10eca574247e3c92/README.md#using-android-studio