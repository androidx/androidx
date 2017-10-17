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

import android.arch.background.workmanager.model.WorkSpec;

import com.firebase.jobdispatcher.FirebaseJobDispatcher;
import com.firebase.jobdispatcher.Job;
import com.firebase.jobdispatcher.Lifetime;
import com.firebase.jobdispatcher.Trigger;

/**
 * Converts a {@link WorkSpec} into a {@link Job}.
 */

class FirebaseJobConverter {
    private FirebaseJobDispatcher mDispatcher;

    FirebaseJobConverter(FirebaseJobDispatcher dispatcher) {
        mDispatcher = dispatcher;
    }

    // TODO(xbhatnag): Add constraints, initial delay, retry logic and periodic
    Job convert(WorkSpec workSpec) {
        return mDispatcher.newJobBuilder()
                .setService(FirebaseJobService.class)
                .setTag(workSpec.getId())
                .setLifetime(Lifetime.FOREVER)
                .setReplaceCurrent(true)
                .setTrigger(Trigger.NOW)
                .build();
    }
}
