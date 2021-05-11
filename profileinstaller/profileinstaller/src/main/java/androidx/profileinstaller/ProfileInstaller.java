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
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.os.Build;
import android.util.Log;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.WorkerThread;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

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

    private static final String PROFILE_BASE_DIR = "/data/misc/profiles/cur/0";
    private static final String PROFILE_REF_BASE_DIR = "/data/misc/profiles/ref";
    private static final String PROFILE_FILE = "primary.prof";
    private static final String PROFILE_SOURCE_LOCATION = "dexopt/baseline.prof";

    /**
     * ART may generate an empty profile automatically, and so we use this number to determine a
     * minimum length/size that is indicative of the profile being non-empty. This is a number of
     * bytes.
     */
    private static final int MIN_MEANINGFUL_LENGTH = 10;

    /**
     * An object which can be passed to the ProfileInstaller which will receive information
     * during the installation process which can be used for logging and telemetry.
     */
    public interface Diagnostics {
        /**
         * The diagnostic method will get called 0 to many times during the installation process,
         * and is passed a [code] and optionally [data] which provides some information around
         * the install process.
         * @param code An int specifying which diagnostic situation has occurred.
         * @param data Optional data passed in that relates to the code passed.
         */
        void diagnostic(@DiagnosticCode int code, @Nullable Object data);

        /**
         * The result method will get called exactly once per installation, with a [code]
         * indicating what the result of the installation was.
         *
         * @param code An int specifying which result situation has occurred.
         * @param data Optional data passed in that relates to the code that was passed.
         */
        void result(@ResultCode int code, @Nullable Object data);
    }

    private static final Diagnostics EMPTY_DIAGNOSTICS = new Diagnostics() {
        @Override
        public void diagnostic(int code, @Nullable Object data) {
            // do nothing
        }

        @Override
        public void result(int code, @Nullable Object data) {
            // do nothing
        }
    };

    @SuppressWarnings("unused")
    @NonNull
    public static final Diagnostics LOG_DIAGNOSTICS = new Diagnostics() {
        static final String TAG = "ProfileInstaller";
        @Override
        public void diagnostic(int code, @Nullable Object data) {
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
            }
            Log.d(TAG, msg);
        }

        @Override
        public void result(int code, @Nullable Object data) {
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
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({
            DIAGNOSTIC_CURRENT_PROFILE_EXISTS,
            DIAGNOSTIC_CURRENT_PROFILE_DOES_NOT_EXIST,
            DIAGNOSTIC_REF_PROFILE_EXISTS,
            DIAGNOSTIC_REF_PROFILE_DOES_NOT_EXIST
    })
    public @interface DiagnosticCode {}

    /**
     * Indicates that when tryInstallSync was run, an existing profile was found in the "cur"
     * directory. The associated [data] passed in for this call will be the size, in bytes, of
     * the profile that was found.
     */
    @DiagnosticCode public static final int DIAGNOSTIC_CURRENT_PROFILE_EXISTS = 0;

    /**
     * Indicates that when tryInstallSync was run, no existing profile was found in the "cur"
     * directory.
     */
    @DiagnosticCode public static final int DIAGNOSTIC_CURRENT_PROFILE_DOES_NOT_EXIST = 1;

    /**
     * Indicates that when tryInstallSync was run, an existing profile was found in the "cur"
     * directory. The associated [data] passed in for this call will be the size, in bytes, of
     * the profile that was found.
     */
    @DiagnosticCode public static final int DIAGNOSTIC_REF_PROFILE_EXISTS = 2;

    /**
     * Indicates that when tryInstallSync was run, no existing profile was found in the "cur"
     * directory.
     */
    @DiagnosticCode public static final int DIAGNOSTIC_REF_PROFILE_DOES_NOT_EXIST = 3;

    /**
     * @hide
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
            RESULT_PARSE_EXCEPTION
    })
    public @interface ResultCode {}

    /**
     * Indicates that the profile got installed and written to disk successfully.
     *
     * Note that this should happen but is not the only condition that indicates "nothing went
     * wrong". Several result codes are indicative of expected behavior.
     */
    @ResultCode public static final int RESULT_INSTALL_SUCCESS = 0;

    /**
     * Indicates that no installation occurred because it was determined that the baseline
     * profile had already been installed previously.
     */
    @ResultCode public static final int RESULT_ALREADY_INSTALLED = 1;

    /**
     * Indicates that the current SDK level is such that installing a profile is not supported by
     * ART.
     */
    @ResultCode public static final int RESULT_UNSUPPORTED_ART_VERSION = 2;

    /**
     * Indicates that the installation was aborted because the app was found to not have adequate
     * permissions to write the profile to disk.
     */
    @ResultCode public static final int RESULT_NOT_WRITABLE = 3;

    /**
     * Indicates that the format required by this SDK version is not supported by this version of
     * the ProfileInstaller library.
     */
    @ResultCode public static final int RESULT_DESIRED_FORMAT_UNSUPPORTED = 4;

    /**
     * Indicates that no baseline profile was bundled with the APK, and as a result, no
     * installation could take place.
     */
    @ResultCode public static final int RESULT_BASELINE_PROFILE_NOT_FOUND = 5;

    /**
     * Indicates that an IO Exception took place during install. The associated [data] with this
     * result is the exception.
     */
    @ResultCode public static final int RESULT_IO_EXCEPTION = 6;

    /**
     * Indicates that a parsing exception occurred during install. The associated [data] with
     * this result is the exception.
     */
    @ResultCode public static final int RESULT_PARSE_EXCEPTION = 7;

    private static boolean shouldSkipInstall(
            @NonNull Diagnostics diagnostics,
            long baselineLength,
            boolean curExists,
            long curLength,
            boolean refExists,
            long refLength
    ) {
        if (curExists && curLength > MIN_MEANINGFUL_LENGTH) {
            // There's a non-empty profile sitting in this directory
            diagnostics.diagnostic(DIAGNOSTIC_CURRENT_PROFILE_EXISTS, null);
        } else {
            diagnostics.diagnostic(DIAGNOSTIC_CURRENT_PROFILE_DOES_NOT_EXIST, null);
        }

        if (refExists && refLength > MIN_MEANINGFUL_LENGTH) {
            diagnostics.diagnostic(DIAGNOSTIC_REF_PROFILE_EXISTS, null);
        } else {
            diagnostics.diagnostic(DIAGNOSTIC_REF_PROFILE_DOES_NOT_EXIST, null);
        }

        if (baselineLength > 0 && baselineLength == curLength) {
            // If the profiles are exactly the same size, we make the assumption that
            // they are in fact the same profile. In this case, there is no work for
            // us to do and we can exit early.
            diagnostics.result(RESULT_ALREADY_INSTALLED, null);
            return true;
        }

        if (baselineLength > 0 && baselineLength == refLength) {
            // If the profiles are exactly the same size, we make the assumption that
            // they are in fact the same profile. In this case, there is no work for
            // us to do and we can exit early.
            diagnostics.result(RESULT_ALREADY_INSTALLED, null);
            return true;
        }

        if (
                baselineLength > 0 &&
                        (baselineLength < curLength || baselineLength < refLength)
        ) {
            // if the baseline profile is smaller than the current profile or
            // reference profile, then we assume that it already has the baseline
            // profile in it. We avoid doing anything in this case as we don't want
            // to introduce unnecessary work on the app or ART every time the app runs.
            // TODO: we could do something a bit smarter here to indicate that we've
            //  already written the profile. For instance, we could save a file marking the
            //  install and look at that.
            diagnostics.result(RESULT_ALREADY_INSTALLED, null);
            return true;
        }
        return false;
    }

    /**
     * Transcode the source file to an appropriate destination format for this OS version, and
     * write it to the ART aot directory.
     *
     * @param assets the asset manager to read source file from dexopt/baseline.prof
     * @param packageName package name of the current apk
     * @param diagnostics The diagnostics object to pass diagnostics to
     */
    private static void transcodeAndWrite(
            @NonNull AssetManager assets,
            @NonNull String packageName,
            @NonNull Diagnostics diagnostics
    ) {
        byte[] version = desiredVersion();
        if (version == null) {
            diagnostics.result(RESULT_UNSUPPORTED_ART_VERSION, null);
            return;
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            diagnostics.result(RESULT_UNSUPPORTED_ART_VERSION, null);
            return;
        }
        File curProfile = new File(new File(PROFILE_BASE_DIR, packageName), PROFILE_FILE);

        boolean canWrite = curProfile.canWrite();
        if (!canWrite) {
            // It's possible that some OEMs might not allow writing to this directory. If this is
            // the case, there's not really anything we can do, so we should quit before doing
            // any unnecessary work.
            diagnostics.result(RESULT_NOT_WRITABLE, null);
            return;
        }

        File refProfile = new File(new File(PROFILE_REF_BASE_DIR, packageName), PROFILE_FILE);
        try (AssetFileDescriptor fd = assets.openFd(PROFILE_SOURCE_LOCATION)) {
            long baselineLength = fd.getLength();
            long curLength = curProfile.length();
            long refLength = refProfile.length();
            boolean curExists = curProfile.exists();
            boolean refExists = refProfile.exists();

            try (InputStream is = fd.createInputStream()) {
                byte[] baselineVersion = ProfileTranscoder.readHeader(is);
                // TODO: this is assuming that the baseline version is the P format. We should
                //  consider whether or not we want to also check for "future" formats, and
                //  assume that if a future format ended up in this file location, that the
                //  platform probably supports it and go ahead and move it to the cur profile
                //  location without parsing anything. For now, a "future" format will just fail
                //  below in the readProfile step.
                boolean transcodingNeeded = !Arrays.equals(baselineVersion, version);

                // NOTE: If transcoding is needed, then it isn't meaningful to compare the
                // lengths of the baseline profile with the cur/ref profiles. As a result, we
                // split logic here.
                if (!transcodingNeeded) {
                    if (shouldSkipInstall(diagnostics,
                            baselineLength,
                            curExists,
                            curLength,
                            refExists,
                            refLength)) {
                        return;
                    }

                    try (OutputStream os = new FileOutputStream(curProfile)) {
                        ProfileTranscoder.writeHeader(os, version);
                        Encoding.writeAll(is, os);
                    }
                } else {
                    // If transcoding into a different format, we first parse the baseline
                    // profile and then transcode it into a byte array so we can get the
                    // resulting length of the profile we want to write to disk. Then, based on
                    // that size, we determine if we want to actually "install" it or not.
                    Map<String, DexProfileData> profile =
                            ProfileTranscoder.readProfile(is, baselineVersion);
                    byte[] result;
                    try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
                        ProfileTranscoder.writeHeader(os, version);
                        boolean success = ProfileTranscoder.transcodeAndWriteBody(
                                os,
                                version,
                                profile
                        );

                        if (!success) {
                            diagnostics.result(RESULT_DESIRED_FORMAT_UNSUPPORTED, null);
                            return;
                        }

                        result = os.toByteArray();
                    }

                    long transcodedLength = result.length;

                    if (shouldSkipInstall(diagnostics,
                            transcodedLength,
                            curExists,
                            curLength,
                            refExists,
                            refLength)) {
                        return;
                    }

                    try (
                            InputStream bis = new ByteArrayInputStream(result);
                            OutputStream os = new FileOutputStream(curProfile)
                    ) {
                        // result already has the header in it, so we don't write the header
                        // here like we did above
                        Encoding.writeAll(bis, os);
                    }
                }
                diagnostics.result(RESULT_INSTALL_SUCCESS, null);
            }
        } catch (FileNotFoundException e) {
            diagnostics.result(RESULT_BASELINE_PROFILE_NOT_FOUND, e);
        } catch (IOException e) {
            diagnostics.result(RESULT_IO_EXCEPTION, e);
        } catch (IllegalStateException e) {
            diagnostics.result(RESULT_PARSE_EXCEPTION, e);
        }
    }

    private static @Nullable byte[] desiredVersion() {
        // If SDK is pre-N, we don't want to do anything, so return null.
        if (Build.VERSION.SDK_INT < ProfileVersion.MIN_SUPPORTED_SDK) {
            return null;
        }

        switch (Build.VERSION.SDK_INT) {
            case Build.VERSION_CODES.N_MR1:
            case Build.VERSION_CODES.N:
                return ProfileVersion.V001_N;

            case Build.VERSION_CODES.O_MR1:
            case Build.VERSION_CODES.O:
                return ProfileVersion.V005_O;
        }

        // we default back to P+, assuming that this will work for future releases
        return ProfileVersion.V010_P;
    }

    /**
     * Try to install the profile from assets into the ART aot profile directory.
     *
     * This should always be called after the first screen is shown to the user, to avoid
     * delaying application startup to install AOT profiles.
     *
     * @param context context to read assets from
     */
    @WorkerThread
    public static void tryInstallSync(@NonNull Context context) {
        tryInstallSync(context, EMPTY_DIAGNOSTICS);
    }

    /**
     * Try to install the profile from assets into the ART aot profile directory.
     *
     * This should always be called after the first screen is shown to the user, to avoid
     * delaying application startup to install AOT profiles.
     *
     * @param context context to read assets from
     * @param diagnostics an object which will receive diagnostic information about the installation
     */
    @WorkerThread
    public static void tryInstallSync(@NonNull Context context, @NonNull Diagnostics diagnostics) {
        Context appContext = context.getApplicationContext();
        String packageName = appContext.getPackageName();
        AssetManager assetManager = appContext.getAssets();
        transcodeAndWrite(assetManager, packageName, diagnostics);
    }

    /**
     * Creates a new thread and calls {@link ProfileInstaller#tryInstallSync(Context)} on it.
     *
     * Thread will be destroyed after the call completes.
     *
     * Warning: *Never* call this during app initialization as it will create a thread and
     * start disk read/write immediately.
     */
    static void tryInstallInBackground(@NonNull Context context) {
        Executor executor = new ThreadPoolExecutor(
                /* corePoolSize = */0,
                /* maximumPoolSize = */1,
                /* keepAliveTime = */0,
                /* unit = */TimeUnit.MILLISECONDS,
                /* workQueue = */new LinkedBlockingQueue<>()
        );
        executor.execute(() -> tryInstallSync(context));
    }
}
