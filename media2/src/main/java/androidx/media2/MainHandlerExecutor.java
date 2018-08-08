/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.media2;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import android.content.Context;
import android.os.Handler;

import androidx.annotation.RestrictTo;

import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;

/**
 * @hide
 */
@RestrictTo(LIBRARY_GROUP)
public class MainHandlerExecutor implements Executor {
    private final Handler mHandler;

    MainHandlerExecutor(Context context) {
        mHandler = new Handler(context.getMainLooper());
    }

    /**
     * Executes the {@link Runnable} on the main thread.
     *
     * @param runnable a runnable to execute
     */
    @Override
    public void execute(Runnable runnable) {
        if (!mHandler.post(runnable)) {
            throw new RejectedExecutionException(mHandler + " is shutting down");
        }
    }

    /**
     * Clears any pending {@link Runnable}.
     */
    public void clear() {
        mHandler.removeCallbacksAndMessages(null);
    }
}
