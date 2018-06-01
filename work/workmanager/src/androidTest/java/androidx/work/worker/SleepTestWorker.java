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

package androidx.work.worker;

import android.support.annotation.NonNull;
import android.util.Log;

import androidx.work.Worker;

/**
 * Worker that sleeps for 5 seconds before returning.
 */

public class SleepTestWorker extends Worker {
    public static final long SLEEP_DURATION = 5000;

    @Override
    public @NonNull Result doWork() {
        try {
            Log.d("SleepTestWorker", "Sleeping : " + SLEEP_DURATION);
            Thread.sleep(SLEEP_DURATION);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return Result.SUCCESS;
    }
}
