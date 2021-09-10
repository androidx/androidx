/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.core.os;

import android.os.Handler;

import androidx.annotation.NonNull;
import androidx.core.util.Preconditions;

import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;

/**
 * Helper for accessing features in {@link Executor}.
 */
public final class ExecutorCompat {

    /** Creates an adapter {@link Executor} that posts all executed tasks onto the given
     *  {@link Handler}.
     */
    public static @NonNull Executor create(@NonNull Handler handler) {
        return new HandlerExecutor(handler);
    }

    private ExecutorCompat() {
    }

    private static class HandlerExecutor implements Executor {
        private final Handler mHandler;

        HandlerExecutor(@NonNull Handler handler) {
            mHandler = Preconditions.checkNotNull(handler);
        }

        @Override
        public void execute(@NonNull Runnable command) {
            if (!mHandler.post(Preconditions.checkNotNull(command))) {
                throw new RejectedExecutionException(mHandler + " is shutting down");
            }
        }
    }
}
