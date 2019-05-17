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

import java.util.concurrent.Executor;

/**
 * Helper class for retrieving an {@link Executor} which will post to the main thread.
 */
final class MainThreadExecutor {
    private static volatile Executor sExecutor;

    private MainThreadExecutor() {}

    static Executor getInstance() {
        if (sExecutor != null) {
            return sExecutor;
        }
        synchronized (MainThreadExecutor.class) {
            if (sExecutor == null) {
                sExecutor = new HandlerAdapterExecutor(new Handler(Looper.getMainLooper()));
            }
        }

        return sExecutor;
    }
}
