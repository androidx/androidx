/*
 * Copyright (C) 2017 The Android Open Source Project
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

package android.arch.background.workmanager.worker;

import android.arch.background.workmanager.Worker;
import android.util.Log;

/**
 * Test Worker that loops until Thread is interrupted.
 */

public class InfiniteTestWorker extends Worker {
    private static final String TAG = "InfiniteTestWorker";

    @Override
    public @WorkerResult int doWork() {
        while (true) {
            if (isInterrupted()) {
                Log.e(TAG, "Interrupted");
                return WORKER_RESULT_RETRY;
            }
        }
    }
}
