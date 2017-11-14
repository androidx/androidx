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
package android.arch.background.workmanager.background.firebase;

import android.arch.background.workmanager.WorkDatabase;
import android.arch.background.workmanager.WorkManager;
import android.arch.background.workmanager.model.WorkSpec;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.RestrictTo;
import android.util.Log;

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
        // TODO(xbhatnag): Avoid using getWorkDatabase() from WorkManager
        final PendingResult pendingResult = goAsync();
        final String workSpecId = intent.getStringExtra(WORKSPEC_ID_KEY);
        final WorkManager workManager = WorkManager.getInstance(context);
        final FirebaseJobScheduler scheduler = (FirebaseJobScheduler) workManager.getScheduler();
        final WorkDatabase database = workManager.getWorkDatabase();
        new Thread(new Runnable() {
            @Override
            public void run() {
                WorkSpec workSpec = database.workSpecDao().getWorkSpec(workSpecId);
                if (workSpec != null) {
                    scheduler.scheduleNow(workSpec);
                } else {
                    Log.e(TAG, "WorkSpec not found! Cannot schedule!");
                }
                pendingResult.finish();
            }
        }).start();
    }
}
