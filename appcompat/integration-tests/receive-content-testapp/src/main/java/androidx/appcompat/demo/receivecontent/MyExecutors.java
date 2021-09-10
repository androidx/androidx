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

package androidx.appcompat.demo.receivecontent;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

final class MyExecutors {
    private MyExecutors() {}

    private static final ListeningScheduledExecutorService BG =
            MoreExecutors.listeningDecorator(Executors.newSingleThreadScheduledExecutor());

    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());
    private static final Executor MAIN_EXECUTOR =
            runnable -> {
                if (!MAIN_HANDLER.post(runnable)) {
                    Log.e(Logcat.TAG, "Failed to post runnable on main thread");
                }
            };

    @NonNull
    public static ListeningScheduledExecutorService bg() {
        return BG;
    }

    @NonNull
    public static Executor main() {
        return MAIN_EXECUTOR;
    }
}
