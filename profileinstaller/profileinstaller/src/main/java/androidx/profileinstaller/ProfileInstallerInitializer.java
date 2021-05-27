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
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.DoNotInline;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.startup.Initializer;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Startup library initializer that installs an AOT profile several seconds after launch.
 *
 * During application startup this will schedule background profile installation several seconds
 * later. At the scheduled time, a background thread will be created to install the profile.
 *
 * You can disable this initializer and call {@link ProfileInstaller#writeProfile(Context)}
 * yourself to control the threading behavior.
 *
 * To disable this initializer add the following to your manifest:
 *
 * <pre>
 *     <provider
 *         android:name="androidx.startup.InitializationProvider"
 *         android:authorities="${applicationId}.androidx-startup"
 *         android:exported="false"
 *         tools:node="merge">
 *         <meta-data android:name="androidx.profileinstaller.ProfileInstallerInitializer"
 *                   tools:node="remove" />
 *     </provider>
 * </pre>
 *
 * If you disable the initializer, ensure that {@link ProfileInstaller#writeProfile(Context)}
 * is called within a few (5-10) seconds of your app starting up.
 */
public class ProfileInstallerInitializer
        implements Initializer<ProfileInstallerInitializer.Result> {
    private static final int DELAY_MS = 5_000;

    /**
     *
     *
     * @return Result immediately.
     */
    @NonNull
    @Override
    public Result create(@NonNull Context context) {
        if (Build.VERSION.SDK_INT < ProfileVersion.MIN_SUPPORTED_SDK) {
            // If we are below the supported SDK, there is nothing for us to do, so return early.
            return new Result();
        }

        // If we made it this far, we are going to try and install the profile in the background.
        Context appContext = context.getApplicationContext();
        Handler handler;
        if (Build.VERSION.SDK_INT >= 28) {
            // avoid aligning with vsync when available using createAsync API
            handler = Handler28Impl.createAsync(Looper.getMainLooper());
        } else {
            handler = new Handler(Looper.getMainLooper());
        }

        handler.postDelayed(() -> writeInBackground(appContext), DELAY_MS);
        return new Result();
    }

    /**
     * Initializer has no dependencies.
     */
    @NonNull
    @Override
    public List<Class<? extends Initializer<?>>> dependencies() {
        return Collections.emptyList();
    }

    /**
     * Creates a new thread and calls {@link ProfileInstaller#writeProfile(Context)} on it.
     *
     * Thread will be destroyed after the call completes.
     *
     * Warning: *Never* call this during app initialization as it will create a thread and
     * start disk read/write immediately.
     */
    private static void writeInBackground(@NonNull Context context) {
        Executor executor = new ThreadPoolExecutor(
                /* corePoolSize = */0,
                /* maximumPoolSize = */1,
                /* keepAliveTime = */0,
                /* unit = */TimeUnit.MILLISECONDS,
                /* workQueue = */new LinkedBlockingQueue<>()
        );
        executor.execute(() -> ProfileInstaller.writeProfile(context));
    }

    /**
     * Empty result class for ProfileInstaller.
     */
    public static class Result { }

    @RequiresApi(28)
    private static class Handler28Impl {
        private Handler28Impl() {
            // Non-instantiable.
        }

        // avoid aligning with vsync when available (API 28+)
        @DoNotInline
        public static Handler createAsync(Looper looper) {
            return Handler.createAsync(looper);
        }
    }
}
