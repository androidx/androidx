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
import android.content.res.AssetManager;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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

    private static final String TAG = ProfileInstaller.class.getSimpleName();
    private static final String PROFILE_BASE_DIR = "/data/misc/profiles/cur/0";
    private static final String PROFILE_FILE = "primary.prof";
    private static final String PROFILE_SOURCE_LOCATION = "dexopt/baseline.prof";

    /**
     * Transcode the source file to an appropriate destination format for this OS version, and
     * write it to the ART aot directory.
     *
     * @param assets the asset manager to read source file from dexopt/baseline.prof
     * @param packageName package name of the current apk
     */
    private static void transcodeAndWrite(AssetManager assets, String packageName) {
        byte[] version = desiredVersion();
        if (version == null) return;
        File profileDir = new File(PROFILE_BASE_DIR, packageName);
        File destFile =  new File(profileDir, PROFILE_FILE);
        try (InputStream is = assets.open(PROFILE_SOURCE_LOCATION, AssetManager.ACCESS_BUFFER)) {
            try (OutputStream os = new FileOutputStream(destFile)) {
                // this check isn't actually needed. It's just to make the linter happy.
                if (Build.VERSION.SDK_INT >= ProfileVersion.MIN_SUPPORTED_SDK) {
                    ProfileTranscoder.transcode(
                            is,
                            os,
                            version
                    );
                }
            }
        } catch (FileNotFoundException e) {
            Log.e(TAG, "baseline.prof file not found", e);
        } catch (IOException e) {
            Log.e(TAG, "IOException encountered during Profile transcoding", e);
        } catch (IllegalStateException e) {
            Log.e(TAG, "IOException encountered during Profile transcoding", e);
        }
        Log.i(TAG, "transcodeAndWrite completed successfully");
    }

    private static @Nullable byte[] desiredVersion() {
        // If DSK is pre-N, we don't want to do anything, so return null.
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
        Context appContext = context.getApplicationContext();
        String packageName = appContext.getPackageName();
        AssetManager assetManager = appContext.getAssets();
        transcodeAndWrite(assetManager, packageName);
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
