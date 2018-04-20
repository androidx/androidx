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

package androidx.work.test;

import android.support.annotation.NonNull;
import android.util.Log;

import androidx.work.impl.Scheduler;
import androidx.work.impl.model.WorkSpec;

import java.util.Arrays;

/**
 * An implementation of a Scheduler which just logs requests to schedule and cancel.
 */
public class NoOpScheduler implements Scheduler {
    private static final String TAG = "NoOpScheduler";

    @Override
    public void schedule(WorkSpec... workSpecs) {
        Log.i(TAG, String.format("Scheduling Request for %s ", Arrays.toString(workSpecs)));
    }

    @Override
    public void cancel(@NonNull String workSpecId) {
        Log.i(TAG, String.format("Cancel request for %s", workSpecId));
    }
}
