/*
 * Copyright 2017 The Android Open Source Project
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
package androidx.work.impl.background.firebase;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.RestrictTo;

import androidx.work.impl.Scheduler;
import androidx.work.impl.WorkDatabase;
import androidx.work.impl.WorkManagerImpl;
import androidx.work.impl.logger.Logger;
import androidx.work.impl.model.WorkSpec;

/**
 * Schedules a {@link WorkSpec} after an initial delay with {@link FirebaseJobScheduler}
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class FirebaseDelayedJobAlarmReceiver extends BroadcastReceiver {
    private static final String TAG = "FirebaseAlarmReceiver";
    static final String WORKSPEC_ID_KEY = "WORKSPEC_ID";

    @Override
    public void onReceive(final Context context, Intent intent) {
        final PendingResult pendingResult = goAsync();
        final String workSpecId = intent.getStringExtra(WORKSPEC_ID_KEY);
        final WorkManagerImpl workManagerImpl = WorkManagerImpl.getInstance();
        final WorkDatabase database = workManagerImpl.getWorkDatabase();
        new Thread(new Runnable() {
            @Override
            public void run() {
                WorkSpec workSpec = database.workSpecDao().getWorkSpec(workSpecId);
                if (workSpec != null) {
                    for (Scheduler scheduler : workManagerImpl.getSchedulers()) {
                        scheduler.schedule(workSpec);
                    }
                } else {
                    Logger.error(TAG, "WorkSpec not found! Cannot schedule!");
                }
                pendingResult.finish();
            }
        }).start();
    }
}
