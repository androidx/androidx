/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.pdf.util;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.RestrictTo;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/** Thread-related utilities. */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public final class ThreadUtils {

    private static final Thread UI_THREAD = Looper.getMainLooper().getThread();

    public static final Handler UI_THREAD_HANDLER = new Handler(Looper.getMainLooper());

    /** The executor for background tasks. Makes it easier to test if it's sequential. */
    private static final Executor BACKGROUND_EXECUTOR =
            Executors.newSingleThreadExecutor(
                    new ThreadFactoryBuilder().setNameFormat("PdfViewerThreadUtils-%d").build());

    /**
     * Checks if the running thread is the UI thread.
     *
     * @return true if this is the UI thread otherwise false.
     */
    public static boolean isUiThread() {
        return Thread.currentThread().equals(UI_THREAD);
    }

    /**
     * Runs the given {@link Runnable} on the UI thread: <br>
     * Run immediately if this is the UI thread, post it on the UI thread otherwise.
     */
    public static void runOnUiThread(Runnable r) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            r.run();
        } else {
            UI_THREAD_HANDLER.post(r);
        }
    }

    /** Runs the given {@link Runnable} on a background thread. */
    public static void runInBackground(Runnable r) {
        BACKGROUND_EXECUTOR.execute(r);
    }

    /**
     * Posts the given runnable on the UI thread, to be started after the given delay (milliseconds)
     */
    public static void postOnUiThreadDelayed(Runnable r, long delay) {
        UI_THREAD_HANDLER.postDelayed(r, delay);
    }

    /** Removes the runnable from the UI thread, if it exists. */
    public static void removeCallbackOnUiThread(Runnable r) {
        UI_THREAD_HANDLER.removeCallbacks(r);
    }

    /** Posts the given runnable on the UI thread. */
    public static void postOnUiThread(Runnable r) {
        UI_THREAD_HANDLER.post(r);
    }

    private ThreadUtils() {
        // Static utility.
    }
}
