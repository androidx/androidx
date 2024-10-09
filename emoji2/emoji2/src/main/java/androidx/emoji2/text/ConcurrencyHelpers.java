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

package androidx.emoji2.text;

import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;

import androidx.annotation.RequiresApi;

import org.jspecify.annotations.NonNull;

import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Various (internal) helpers for interacting with threads and event loops
 */
class ConcurrencyHelpers {

    // It is expected that all callers of this internal API call shutdown on completion, allow 15
    // seconds for retry delay before spinning down the thread.
    private static final int FONT_LOAD_TIMEOUT_SECONDS = 15 /* seconds */;

    private ConcurrencyHelpers() { /* can't instantiate */ }

    /**
     * Background thread worker with an explicit thread name.
     *
     * It is expected that callers explicitly shut down the returned ThreadPoolExecutor as soon
     * as they have completed font loading.
     *
     * @param name name of thread
     * @return ThreadPoolExecutor limited to one thread with a timeout of 15 seconds.
     */
    @SuppressWarnings("ThreadPriorityCheck")
    static ThreadPoolExecutor createBackgroundPriorityExecutor(@NonNull String name) {
        ThreadFactory threadFactory = runnable -> {
            Thread t = new Thread(runnable, name);
            t.setPriority(Process.THREAD_PRIORITY_BACKGROUND);
            return t;
        };
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                0 /* corePoolSize */,
                1 /* maximumPoolSize */,
                FONT_LOAD_TIMEOUT_SECONDS /* keepAliveTime */,
                TimeUnit.SECONDS /* keepAliveTime TimeUnit */,
                new LinkedBlockingDeque<>() /* unbounded queue*/,
                threadFactory
        );
        executor.allowCoreThreadTimeOut(true);
        return executor;
    }

    /**
     * @return Main thread handler, with createAsync if API level supports it.
     */
    static Handler mainHandlerAsync() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            return Handler28Impl.createAsync(Looper.getMainLooper());
        } else {
            return new Handler(Looper.getMainLooper());
        }
    }

    static Executor mainThreadExecutor() {
        return convertHandlerToExecutor(mainHandlerAsync());
    }

    /**
     * Convert a handler to an executor.
     *
     * We need to do this in places where Context is not available, and cannot use ContextCompat.
     *
     * @param handler a background thread handler
     * @return an executor that posts all work to that handler
     */
    static @NonNull Executor convertHandlerToExecutor(@NonNull Handler handler) {
        return handler::post;
    }

    @RequiresApi(28)
    static class Handler28Impl {
        private Handler28Impl() {
            // Non-instantiable.
        }

        public static Handler createAsync(Looper looper) {
            return Handler.createAsync(looper);
        }
    }
}
