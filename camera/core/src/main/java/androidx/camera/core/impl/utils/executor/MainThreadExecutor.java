/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.camera.core.impl.utils.executor;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;

/**
 * An Executor which will post to the main thread.
 */
final class MainThreadExecutor implements Executor {
    private static volatile Executor sExecutor;
    private final Handler mMainThreadHandler;

    private MainThreadExecutor() {
        mMainThreadHandler = new Handler(Looper.getMainLooper());
    }

    static Executor getInstance() {
        if (sExecutor != null) {
            return sExecutor;
        }
        synchronized (MainThreadExecutor.class) {
            if (sExecutor == null) {
                sExecutor = new MainThreadExecutor();
            }
        }

        return sExecutor;
    }

    @Override
    public void execute(@NonNull Runnable command) {
        if (!mMainThreadHandler.post(command)) {
            throw new RejectedExecutionException(mMainThreadHandler + " is shutting down");
        }
    }
}
