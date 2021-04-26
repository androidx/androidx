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

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;

import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Background thread manager for profile handler.
 */
class BackgroundProfileInstaller {
    private final Runnable mRunnable;

    BackgroundProfileInstaller(@NonNull Context context) {
        Context appContext = context.getApplicationContext();
        mRunnable = () -> ProfileInstaller.tryInstallProfile(appContext);
    }


    /**
     * Creates a new thread and calls {@link ProfileInstaller#tryInstallProfile(Context)} on it.
     *
     * Thread will be destroyed after the call completes.
     *
     * Warning: *Never* call this during app initialization as it will create a thread and
     * start disk read/write immediately.
     */
    @MainThread
    void start() {
        Executor executor = new ThreadPoolExecutor(0, 1, 0,
                TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>());
        executor.execute(mRunnable);
    }
}
