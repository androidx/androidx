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

package android.arch.background.workmanager.firebase;

import android.arch.background.workmanager.Scheduler;
import android.arch.background.workmanager.model.WorkSpec;
import android.content.Context;
import android.support.annotation.RestrictTo;
import android.util.Log;

import com.firebase.jobdispatcher.FirebaseJobDispatcher;
import com.firebase.jobdispatcher.GooglePlayDriver;
import com.firebase.jobdispatcher.Job;

/**
 * A class that schedules work using {@link FirebaseJobDispatcher}.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class FirebaseJobScheduler implements Scheduler {
    private static final String TAG = "FirebaseJobScheduler";
    private FirebaseJobDispatcher mDispatcher;
    private FirebaseJobConverter mJobConverter;

    public FirebaseJobScheduler(Context context) {
        mDispatcher = new FirebaseJobDispatcher(new GooglePlayDriver(context));
        mJobConverter = new FirebaseJobConverter(mDispatcher);
    }

    @Override
    public void schedule(WorkSpec... workSpecs) {
        for (WorkSpec workSpec : workSpecs) {
            Job job = mJobConverter.convert(workSpec);
            Log.d(TAG, "Scheduling work, ID: " + workSpec.getId());
            int result = mDispatcher.schedule(job);
            if (result != FirebaseJobDispatcher.SCHEDULE_RESULT_SUCCESS) {
                Log.e(TAG, "Schedule failed. Result = " + result);
            }
        }
    }
}
