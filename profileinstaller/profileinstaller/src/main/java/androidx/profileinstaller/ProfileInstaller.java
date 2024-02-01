/*
 * Copyright 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.profileinstaller;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.util.Log;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.WorkerThread;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.concurrent.Executor;

/**
 * Install ahead of time tracing profiles to configure ART to precompile bundled libraries.
 *
 * This will automatically be called by {@link ProfileInstallerInitializer} and you should never
 * call this unless you have disabled the initializer in your manifest.
 *
 * This reads profiles from the assets directory, where they must be embedded during the build
 * process. This will have no effect if there is not a profile embedded in the current APK.
 */
public class ProfileInstaller {
    // cannot construct
    private ProfileInstaller() {}

    static final String PROFILE_SOURCE_LOCATION = "dexopt/baseline.prof";

    private static final String TAG = "ProfileInstaller";

    private static final String PROFILE_BASE_DIR = "/data/misc/profiles/cur/0";
    private static final String PROFILE_FILE = "primary.prof";
    private static final String PROFILE_META_LOCATION = "dexopt/baseline.profm";
    private static final String PROFILE_INSTALLER_SKIP_FILE_NAME =
            "profileinstaller_profileWrittenFor_lastUpdateTime.dat";

    /**
     * An object which can be passed to the ProfileInstaller which will receive information
     * during the installation process which can be used for logging and telemetry.
     */
    public interface DiagnosticsCallback {
        /**
         * The diagnostic method will get called 0 to many times during the installation process,
         * and is passed a [code] and optionally [data] which provides some information around
         * the install process.
         * @param code An int specifying which diagnostic situation has occurred.
         * @param data Optional data passed in that relates to the code passed.
         */
        void onDiagnosticReceived(@DiagnosticCode int code, @Nullable Object data);

        /**
         * The result method will get called exactly once per installation, with a [code]
         * indicating what the result of the installation was.
         *
         * @param code An int specifying which result situation has occurred.
         * @param data Optional data passed in that relates to the code that was passed.
         */
        void onResultReceived(@ResultCode int code, @Nullable Object data);
    }

    @SuppressWarnings("SameParameterValue")
    static void result(
            @NonNull Executor executor,
            @NonNull DiagnosticsCallback diagnostics,
            @ResultCode int code,
            @Nullable Object data
    ) {
        executor.execute(() -> diagnostics.onResultReceived(code, data));
    }

    @SuppressWarnings("SameParameterValue")
    static void diagnostic(
            @NonNull Executor executor,
            @NonNull DiagnosticsCallback diagnostics,
            @DiagnosticCode int code,
            @Nullable Object data
    ) {
        executor.execute(() -> diagnostics.onDiagnosticReceived(code, data));
    }

    private static final DiagnosticsCallback EMPTY_DIAGNOSTICS = new DiagnosticsCallback() {
        @Override
        public void onDiagnosticReceived(int code, @Nullable Object data) {
            // do nothing
        }

        @Override
        public void onResultReceived(int code, @Nullable Object data) {
            // do nothing
        }
    };

    @NonNull
    static final DiagnosticsCallback LOG_DIAGNOSTICS = new DiagnosticsCallback() {
        static final String TAG = "ProfileInstaller";
        @Override
        public void onDiagnosticReceived(int code, @Nullable Object data) {
            String msg = "";
            switch (code) {
                case DIAGNOSTIC_CURRENT_PROFILE_EXISTS:
                    msg = "DIAGNOSTIC_CURRENT_PROFILE_EXISTS";
                    break;
                case DIAGNOSTIC_CURRENT_PROFILE_DOES_NOT_EXIST:
                    msg = "DIAGNOSTIC_CURRENT_PROFILE_DOES_NOT_EXIST";
                    break;
                case DIAGNOSTIC_REF_PROFILE_EXISTS:
                    msg = "DIAGNOSTIC_REF_PROFILE_EXISTS";
                    break;
                case DIAGNOSTIC_REF_PROFILE_DOES_NOT_EXIST:
                    msg = "DIAGNOSTIC_REF_PROFILE_DOES_NOT_EXIST";
                    break;
                case DIAGNOSTIC_PROFILE_IS_COMPRESSED:
                    msg = "DIAGNOSTIC_PROFILE_IS_COMPRESSED";
                    break;
            }
            Log.d(TAG, msg);
        }

        @Override
        public void onResultReceived(int code, @Nullable Object data) {
            String msg = "";
            switch (code) {
                case RESULT_INSTALL_SUCCESS: msg = "RESULT_INSTALL_SUCCESS";
                    break;
                case RESULT_ALREADY_INSTALLED: msg = "RESULT_ALREADY_INSTALLED";
                    break;
                case RESULT_UNSUPPORTED_ART_VERSION: msg = "RESULT_UNSUPPORTED_ART_VERSION";
                    break;
                case RESULT_NOT_WRITABLE: msg = "RESULT_NOT_WRITABLE";
                    break;
                case RESULT_DESIRED_FORMAT_UNSUPPORTED: msg = "RESULT_DESIRED_FORMAT_UNSUPPORTED";
                    break;
                case RESULT_BASELINE_PROFILE_NOT_FOUND: msg = "RESULT_BASELINE_PROFILE_NOT_FOUND";
                    break;
                case RESULT_IO_EXCEPTION: msg = "RESULT_IO_EXCEPTION";
                    break;
                case RESULT_PARSE_EXCEPTION: msg = "RESULT_PARSE_EXCEPTION";
                    break;
                case RESULT_INSTALL_SKIP_FILE_SUCCESS: msg = "RESULT_INSTALL_SKIP_FILE_SUCCESS";
                    break;
                case RESULT_DELETE_SKIP_FILE_SUCCESS: msg = "RESULT_DELETE_SKIP_FILE_SUCCESS";
                    break;
            }

            switch (code) {
                case RESULT_BASELINE_PROFILE_NOT_FOUND:
                case RESULT_IO_EXCEPTION:
                case RESULT_PARSE_EXCEPTION:
                    Log.e(TAG, msg, (Throwable) data);
                    break;
                default:
                    Log.d(TAG, msg);
                    break;
            }
        }
    };

    /**
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({
            DIAGNOSTIC_CURRENT_PROFILE_EXISTS,
            DIAGNOSTIC_CURRENT_PROFILE_DOES_NOT_EXIST,
            DIAGNOSTIC_REF_PROFILE_EXISTS,
            DIAGNOSTIC_REF_PROFILE_DOES_NOT_EXIST,
            DIAGNOSTIC_PROFILE_IS_COMPRESSED
    })
    public @interface DiagnosticCode {}

    /**
     * Indicates that when tryInstallSync was run, an existing profile was found in the "cur"
     * directory. The associated [data] passed in for this call will be the size, in bytes, of
     * the profile that was found.
     */
    @DiagnosticCode public static final int DIAGNOSTIC_CURRENT_PROFILE_EXISTS = 1;

    /**
     * Indicates that when tryInstallSync was run, no existing profile was found in the "cur"
     * directory.
     */
    @DiagnosticCode public static final int DIAGNOSTIC_CURRENT_PROFILE_DOES_NOT_EXIST = 2;

    /**
     * Indicates that when tryInstallSync was run, an existing profile was found in the "cur"
     * directory. The associated [data] passed in for this call will be the size, in bytes, of
     * the profile that was found.
     */
    @DiagnosticCode public static final int DIAGNOSTIC_REF_PROFILE_EXISTS = 3;

    /**
     * Indicates that when tryInstallSync was run, no existing profile was found in the "cur"
     * directory.
     */
    @DiagnosticCode public static final int DIAGNOSTIC_REF_PROFILE_DOES_NOT_EXIST = 4;

    /**
     * Indicates that the profile is compressed and a version of bundletool newer than 1.13.2
     * needs to be used to build the app.
     */
    @DiagnosticCode public static final int DIAGNOSTIC_PROFILE_IS_COMPRESSED = 5;

    /**
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({
            RESULT_INSTALL_SUCCESS,
            RESULT_ALREADY_INSTALLED,
            RESULT_UNSUPPORTED_ART_VERSION,
            RESULT_NOT_WRITABLE,
            RESULT_DESIRED_FORMAT_UNSUPPORTED,
            RESULT_BASELINE_PROFILE_NOT_FOUND,
            RESULT_IO_EXCEPTION,
            RESULT_PARSE_EXCEPTION,
            RESULT_META_FILE_REQUIRED_BUT_NOT_FOUND,
            RESULT_INSTALL_SKIP_FILE_SUCCESS,
            RESULT_DELETE_SKIP_FILE_SUCCESS,
            RESULT_SAVE_PROFILE_SIGNALLED,
            RESULT_SAVE_PROFILE_SKIPPED,
            RESULT_BENCHMARK_OPERATION_SUCCESS,
            RESULT_BENCHMARK_OPERATION_FAILURE,
            RESULT_BENCHMARK_OPERATION_UNKNOWN
    })
    public @interface ResultCode {}

    /**
     * Indicates that the profile got installed and written to disk successfully.
     *
     * Note that this should happen but is not the only condition that indicates "nothing went
     * wrong". Several result codes are indicative of expected behavior.
     */
    @ResultCode public static final int RESULT_INSTALL_SUCCESS = 1;

    /**
     * Indicates that no installation occurred because it was determined that the baseline
     * profile had already been installed previously.
     */
    @ResultCode public static final int RESULT_ALREADY_INSTALLED = 2;

    /**
     * Indicates that the current SDK level is such that installing a profile is not supported by
     * ART.
     */
    @ResultCode public static final int RESULT_UNSUPPORTED_ART_VERSION = 3;

    /**
     * Indicates that the installation was aborted because the app was found to not have adequate
     * permissions to write the profile to disk.
     */
    @ResultCode public static final int RESULT_NOT_WRITABLE = 4;

    /**
     * Indicates that the format required by this SDK version is not supported by this version of
     * the ProfileInstaller library.
     */
    @ResultCode public static final int RESULT_DESIRED_FORMAT_UNSUPPORTED = 5;

    /**
     * Indicates that no baseline profile was bundled with the APK, and as a result, no
     * installation could take place.
     */
    @ResultCode public static final int RESULT_BASELINE_PROFILE_NOT_FOUND = 6;

    /**
     * Indicates that an IO Exception took place during install. The associated [data] with this
     * result is the exception.
     */
    @ResultCode public static final int RESULT_IO_EXCEPTION = 7;

    /**
     * Indicates that a parsing exception occurred during install. The associated [data] with
     * this result is the exception.
     */
    @ResultCode public static final int RESULT_PARSE_EXCEPTION = 8;

    /**
     * Indicates that the device requires a metadata file in order to install the profile
     * successfully, but there was not one included in the APK.
     *
     * The correct metadata files are produced when using Android Gradle Plugin `7.1.0-alpha05` or
     * newer.
     */
    @ResultCode public static final int RESULT_META_FILE_REQUIRED_BUT_NOT_FOUND = 9;

    /**
     * Indicates that a skip file was successfully written and profile installation will be skipped.
     */
    @ResultCode public static final int RESULT_INSTALL_SKIP_FILE_SUCCESS = 10;

    /**
     * Indicates that a skip file was successfully deleted and profile installation will resume.
     */
    @ResultCode public static final int RESULT_DELETE_SKIP_FILE_SUCCESS = 11;

    /**
     * Indicates that this process was signalled to save it's profile information
     */
    @ResultCode public static final int RESULT_SAVE_PROFILE_SIGNALLED = 12;

    /**
     * Indicates that this process was not able to signal itself to save profile information
     */
    @ResultCode public static final int RESULT_SAVE_PROFILE_SKIPPED = 13;

    /**
     * Indicates that the benchmark operation was successful
     */
    @ResultCode public static final int RESULT_BENCHMARK_OPERATION_SUCCESS = 14;

    /**
     * Indicates that the benchmark operation failed
     */
    @ResultCode public static final int RESULT_BENCHMARK_OPERATION_FAILURE = 15;

    /**
     * Indicates that the benchmark operation was unknown, likely meaning profileinstaller needs
     * to update to support the operation
     */
    @ResultCode public static final int RESULT_BENCHMARK_OPERATION_UNKNOWN = 16;

    /**
     * Check if we've already installed a profile for this app installation.
     *
     *
     * @param packageInfo used to lookup the last install time for this apk
     * @param appFilesDir directory to store a file to note prior installation
     * @param diagnostics for noting IO errors
     * @return true every time the APK is installed or upgraded until markProfileWritten is called.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @WorkerThread
    static boolean hasAlreadyWrittenProfileForThisInstall(PackageInfo packageInfo,
            File appFilesDir,
            DiagnosticsCallback diagnostics) {
        File skipFile = new File(appFilesDir, PROFILE_INSTALLER_SKIP_FILE_NAME);
        if (!skipFile.exists()) {
            /* We've never saved a skip file, fastest path */
            return false;
        }

        long lastProfileWritePackageUpdateTime;
        try (DataInputStream dataInputStream = new DataInputStream(new FileInputStream(skipFile))) {
            lastProfileWritePackageUpdateTime = dataInputStream.readLong();
        } catch (IOException e) {
            /* Consider the file as not a valid match */
            return false;
        }

        // check if the last write package update time matches the current install
        boolean result = lastProfileWritePackageUpdateTime == packageInfo.lastUpdateTime;
        if (result) {
            diagnostics.onResultReceived(RESULT_ALREADY_INSTALLED, null);
        }
        return result;
    }

    /**
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    static void noteProfileWrittenFor(@NonNull PackageInfo packageInfo, @NonNull File appFilesDir) {
        File skipFile = new File(appFilesDir, PROFILE_INSTALLER_SKIP_FILE_NAME);
        try (DataOutputStream os = new DataOutputStream(new FileOutputStream(skipFile))) {
            os.writeLong(packageInfo.lastUpdateTime);
        } catch (IOException e) {
            /* nothing */
        }
    }

    /**
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    static boolean deleteProfileWrittenFor(@NonNull File appFilesDir) {
        File skipFile = new File(appFilesDir, PROFILE_INSTALLER_SKIP_FILE_NAME);
        return skipFile.delete();
    }

    /**
     * Transcode the source file to an appropriate destination format for this OS version, and
     * write it to the ART aot directory.
     * @param assets the asset manager to read source file from dexopt/baseline.prof
     * @param packageName package name of the current apk
     * @param packageInfo for noting successful installation
     * @param filesDir for noting successful installation
     * @param apkName The apk file name the profile is targeting
     * @param diagnostics The diagnostics callback to pass diagnostics to
     * @return True whether the operation was successful, false otherwise
     */
    private static boolean transcodeAndWrite(
            @NonNull AssetManager assets,
            @NonNull String packageName,
            @NonNull PackageInfo packageInfo,
            @NonNull File filesDir,
            @NonNull String apkName,
            @NonNull Executor executor,
            @NonNull DiagnosticsCallback diagnostics
    ) {
        File curProfile = new File(new File(PROFILE_BASE_DIR, packageName), PROFILE_FILE);

        DeviceProfileWriter deviceProfileWriter = new DeviceProfileWriter(assets, executor,
                diagnostics, apkName, PROFILE_SOURCE_LOCATION, PROFILE_META_LOCATION, curProfile);

        if (!deviceProfileWriter.deviceAllowsProfileInstallerAotWrites()) {
            return false; /* nothing else to do here */
        }

        boolean success = deviceProfileWriter.read()
                .transcodeIfNeeded()
                .write();

        if (success) {
            noteProfileWrittenFor(packageInfo, filesDir);
        }
        return success;
    }

    /**
     * Try to write the profile from assets into the ART aot profile directory.
     *
     * You do not need to call this method if {@link ProfileInstallerInitializer} is enabled for
     * your application.
     *
     * If you disable the initializer, you should <b>call this method within 5-10 seconds</b> of
     * app launch, to ensure that art can use the generated profile.
     *
     * This should always be called after the first screen is shown to the user, to avoid
     * delaying application startup to install AOT profiles.
     *
     * It is encouraged that you call this method during <b>every</b> app startup to ensure
     * profiles are written correctly after app upgrades, or if the profile failed to write on the
     * previous launch.
     *
     * Profiles will be correctly formatted based on the current API level of the device, and only
     * installed if profileinstaller can determine that it is safe to do so.
     *
     * If the profile is not written, no action needs to be taken.
     *
     * @param context context to read assets from
     */
    @WorkerThread
    public static void writeProfile(@NonNull Context context) {
        writeProfile(context, Runnable::run, EMPTY_DIAGNOSTICS);
    }

    /**
     * Try to write the profile from assets into the ART aot profile directory.
     *
     * You do not need to call this method if {@link ProfileInstallerInitializer} is enabled for
     * your application.
     *
     * If you disable the initializer, you should call this method within 5-10 seconds of app
     * launch, to ensure that art can use the generated profile.
     *
     * This should always be called after the first screen is shown to the user, to avoid
     * delaying application startup to install AOT profiles.
     *
     * It is encouraged that you call this method during <b>every</b> app startup to ensure
     * profiles are written correctly after app upgrades, or if the profile failed to write on the
     * previous launch.
     *
     * Profiles will be correctly formatted based on the current API level of the device, and only
     * installed if profileinstaller can determine that it is safe to do so.
     *
     * If the profile is not written, no action needs to be taken.

     *
     * @param context context to read assets from
     * @param diagnostics an object which will receive diagnostic information about the
     * installation
     * @param executor the executor to run the diagnostic events through
     */
    @WorkerThread
    public static void writeProfile(
            @NonNull Context context,
            @NonNull Executor executor,
            @NonNull DiagnosticsCallback diagnostics
    ) {
        writeProfile(context, executor, diagnostics, false);
    }

    /**
     * Try to write the profile from assets into the ART aot profile directory.
     *
     * You do not need to call this method if {@link ProfileInstallerInitializer} is enabled for
     * your application.
     *
     * If you disable the initializer, you should call this method within 5-10 seconds of app
     * launch, to ensure that art can use the generated profile.
     *
     * This should always be called after the first screen is shown to the user, to avoid
     * delaying application startup to install AOT profiles.
     *
     * It is encouraged that you call this method during <b>every</b> app startup to ensure
     * profiles are written correctly after app upgrades, or if the profile failed to write on the
     * previous launch.
     *
     * Profiles will be correctly formatted based on the current API level of the device, and only
     * installed if profileinstaller can determine that it is safe to do so.
     *
     * If the profile is not written, no action needs to be taken unlesss {@code
     * forceWriteProfile} is {@code true}.
     *
     * @param context context to read assets from
     * @param executor the executor to run the diagnostic events through
     * @param diagnostics an object which will receive diagnostic information about the installation
     * @param forceWriteProfile an override to always install the profile
     *
     */
    @WorkerThread
    @SuppressWarnings("deprecation")
    static void writeProfile(
            @NonNull Context context,
            @NonNull Executor executor,
            @NonNull DiagnosticsCallback diagnostics,
            boolean forceWriteProfile
    ) {
        Context appContext = context.getApplicationContext();
        String packageName = appContext.getPackageName();
        ApplicationInfo appInfo = appContext.getApplicationInfo();
        AssetManager assetManager = appContext.getAssets();
        String apkName = new File(appInfo.sourceDir).getName();
        PackageManager packageManager = context.getPackageManager();
        PackageInfo packageInfo;
        try {
            packageInfo = packageManager.getPackageInfo(packageName, 0);
        } catch (PackageManager.NameNotFoundException e) {
            diagnostics.onResultReceived(RESULT_IO_EXCEPTION, e);

            // Calls the verification. Note that in this case since the force install failed we
            // don't need to report it to the ProfileVerifier.
            ProfileVerifier.writeProfileVerification(context, false);
            return;
        }
        File filesDir = context.getFilesDir();
        if (forceWriteProfile
                || !hasAlreadyWrittenProfileForThisInstall(packageInfo, filesDir, diagnostics)) {
            Log.d(TAG, "Installing profile for " + context.getPackageName());
            boolean profileWritten = transcodeAndWrite(assetManager, packageName, packageInfo,
                    filesDir, apkName, executor, diagnostics);
            ProfileVerifier.writeProfileVerification(
                    context, profileWritten && forceWriteProfile);
        } else {
            Log.d(TAG, "Skipping profile installation for " + context.getPackageName());
            ProfileVerifier.writeProfileVerification(context, false);
        }
    }

    /**
     * Writes a profile installation skip file, which makes {@link  ProfileInstaller} skip profile
     * installation. This is being done so that Macrobenchmarks can request a skip file for
     * `CompilationMode.None()`, and avoid any interference from {@link  ProfileInstaller}.
     *
     * @param context     context to read assets from
     * @param diagnostics an object which will receive diagnostic information
     * @param executor    the executor to run the diagnostic events through
     */
    @WorkerThread
    @SuppressWarnings("deprecation")
    static void writeSkipFile(
            @NonNull Context context,
            @NonNull Executor executor,
            @NonNull DiagnosticsCallback diagnostics
    ) {
        Context appContext = context.getApplicationContext();
        String packageName = appContext.getPackageName();
        PackageManager packageManager = context.getPackageManager();
        PackageInfo packageInfo;
        try {
            packageInfo = packageManager.getPackageInfo(packageName, 0);
        } catch (PackageManager.NameNotFoundException e) {
            result(executor, diagnostics, RESULT_IO_EXCEPTION, e);
            return;
        }
        File filesDir = context.getFilesDir();
        ProfileInstaller.noteProfileWrittenFor(packageInfo, filesDir);
        result(executor, diagnostics, RESULT_INSTALL_SKIP_FILE_SUCCESS, null);
    }

    /**
     * Deletes a profile installation skip so profile installation can continue after
     * CompilationMode.None()`.
     *
     * @param context     context to read assets from
     * @param diagnostics an object which will receive diagnostic information
     * @param executor    the executor to run the diagnostic events through
     */
    @WorkerThread
    static void deleteSkipFile(
            @NonNull Context context,
            @NonNull Executor executor,
            @NonNull DiagnosticsCallback diagnostics
    ) {
        File filesDir = context.getFilesDir();
        ProfileInstaller.deleteProfileWrittenFor(filesDir);
        result(executor, diagnostics, RESULT_DELETE_SKIP_FILE_SUCCESS, null);
    }
}
