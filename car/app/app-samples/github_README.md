Android for Cars App Library Samples
===========================================
This directory contains sample apps that use Android for Cars App Library.
Please find the library documentation at https://developer.android.com/training/cars/navigation.

Prerequisites
--------------
- [Android 6.0 SDK (API level 23)](https://developer.android.com/studio/releases/platforms#6.0) or newer.

Build and Run From Android Studio
-----------------------
In order to build app APKs,

1. Open the project in Android Studio: File -> Open -> Select this directory and click OK.

2. Select a sample app directory (e.g. `showcase`) in the `Project Structure` UI on the top left. Each sample app has two build targets, `showcase/mobile` is for the `projected` platform and `showcase/automotive` is for the `embedded` platform.

3. Go to `Build -> Make Module 'androidx.car.app.app-samples.<sample_app>.<platform>'`. The apks
 will be generated in `<sample_app>/<platform>/build/outputs/apk`.

In order to install and run the apps,

1. Open the AndroidX project in Android Studio.

2. Go to `Run -> Edit Configurations`, select a sample app target (e.g. `car.app.app-samples
.showcase-mobile`). If you are building for the `mobile` platform, also select `Launch: Nothing
` in `General -> Launch Options`. Click `OK` to close the dialog.

3. Select `Run -> Run <sample_app>`, to run the app, which will just install it in the selected
 device.

Run From the Command Line
---------------------
1. Open the project in Android Studio to download the gradle wrapper files.

2. Run `./gradlew :<sample_app>:<platform>:assemble` to assemble the APKs. E.g.

```bash
./gradlew :showcase:mobile:assemble
./gradlew :showcase:automotive:assemble
```

The APK should be generated under the sample app’s build directory, e.g. `<sample_app>/<platform>/build/outputs/apk/debug`.

If you see this error during the build:

    ```shell
    > SDK location not found. Define location with an ANDROID_SDK_ROOT environment variable or by
     setting the sdk.dir path in your projects local properties file at <project_path
     >/local.properties
    ```

Create `local.properties` file under the project directory, and copy the following line:

    ```
    sdk.dir = <your android sdk directory path>
    ```

3. Install the APK with ADB:

```bash
adb install <path_to_sample_app_apk>
```

Run the Mobile Apps in the Desktop Head Unit (DHU)
-------------------------------------------
Follow the instructions in [Test Android apps for cars][1] to run the sample apps in the DHU.

In short, do the following:

1. [Enable the Android Developer Settings][2]
2. [Enable Unknown Sources in Android Auto][3]
3. [Run the DHU][4]

**Note**: In Android Q, there is no Android Auto app in the launcher. The way to get to the settings in that case is through Settings -> Apps & Notifications -> See all apps -> Android Auto -> Advanced -> Additional settings in the app.

Run the Automotive Apps in the Automotive OS Emulator
-------------------------------------------
In order to use the Automotive OS emulator, download the Android Studio 4.2 or higher (currently
only available in the Beta and Canary tracks), and do the following:

1. Follow instructions to [run an Automotive OS emulator][5].
2. Follow instructions to [update the Template Host app][6].

[1]: https://developer.android.com/training/cars/testing
[2]: https://developer.android.com/studio/debug/dev-options
[3]: https://developer.android.com/training/cars/testing#step1
[4]: https://developer.android.com/training/cars/testing#running-dhu
[5]: https://developer.android.com/training/cars/testing#system-images
[6]: https://developer.android.com/training/cars/apps/automotive-os
