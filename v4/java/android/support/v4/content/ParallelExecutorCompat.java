/*
 * Copyright (C) 2015 The Android Open Source Project
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

package android.support.v4.content;

import android.os.Build;

import java.util.concurrent.Executor;

/**
 * Helper for accessing a shared parallel Executor instance
 * introduced after API level 4 in a backwards compatible fashion.
 */
public class ParallelExecutorCompat {
    public static Executor getParallelExecutor() {
        if (Build.VERSION.SDK_INT >= 11) {
            // From API 11 onwards, return AsyncTask.THREAD_POOL_EXECUTOR
            return ExecutorCompatHoneycomb.getParallelExecutor();
        } else {
            return ModernAsyncTask.THREAD_POOL_EXECUTOR;
        }
    }
}
