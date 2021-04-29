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

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.startup.Initializer;

import java.util.Collections;
import java.util.List;

/**
 * Startup library initializer that installs an AOT profile several seconds after launch.
 *
 * During application startup this will schedule background profile installation several seconds
 * later. At the scheduled time, a background thread will be created to install the profile.
 *
 * You can disable this initializer and call {@link ProfileInstaller#tryInstallProfile(Context)}
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
 * If you disable the initializer, ensure that {@link ProfileInstaller#tryInstallProfile(Context)}
 * is called within a few (5-10) seconds of your app starting up.
 */
public class ProfileInstallerInitializer
        implements Initializer<ProfileInstallerInitializer.Result> {
    private static final int DELAY_MS = 5_000;

    void doDelayedInit(Context appContext) {
        new BackgroundProfileInstaller(appContext).start();
    }

    /**
     *
     *
     * @return Result immediately.
     */
    @NonNull
    @Override
    public Result create(@NonNull Context context) {
        Context appContext = context.getApplicationContext();
        Handler handler;
        if (Build.VERSION.SDK_INT >= 28) {
            handler = Handler28Impl.createAsync(Looper.getMainLooper());
        } else {
            handler = new Handler(Looper.getMainLooper());
        }
        handler.postDelayed(() -> doDelayedInit(appContext), DELAY_MS);
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
     * Empty result class for ProfileInstaller.
     */
    public static class Result { }

    @RequiresApi(28)
    private static class Handler28Impl {
        private Handler28Impl() {
            // Non-instantiable.
        }

        // avoid aligning with vsync when available (API 28+)
        public static Handler createAsync(Looper looper) {
            return Handler.createAsync(looper);
        }

    }
}
