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
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;

import androidx.work.Logger;
import androidx.work.impl.Scheduler;
import androidx.work.impl.WorkDatabase;
import androidx.work.impl.WorkManagerImpl;
import androidx.work.impl.model.WorkSpec;

import java.util.List;

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
        if (workManagerImpl == null) {
            Logger.error(TAG, "WorkManager is not initialized properly.  The most "
                    + "likely cause is that you disabled WorkManagerInitializer in your manifest "
                    + "but forgot to call WorkManager#initialize in your Application#onCreate or a "
                    + "ContentProvider.");
            return;
        }
        final WorkDatabase database = workManagerImpl.getWorkDatabase();
        // TODO (rahulrav@) Use WorkManager's task executor here instead.
        new Thread(new Runnable() {
            @Override
            public void run() {
                WorkSpec workSpec = database.workSpecDao().getWorkSpec(workSpecId);
                if (workSpec != null) {
                    /*
                     * FirebaseJobScheduler creates alarms for Workers that have an initial delay
                     * and routes them through this receiver. Here rather than call
                     * Schedulers#schedule(), we directly call FirebaseJobScheduler#scheduleNow()
                     * because Schedulers#schedule() will consider the Worker no longer eligible.
                     */
                    FirebaseJobScheduler scheduler = getFirebaseJobScheduler(workManagerImpl);
                    if (scheduler != null) {
                        Logger.debug(TAG, String.format("Scheduling WorkSpec %s", workSpecId));
                        scheduler.scheduleNow(workSpec);
                    } else {
                        Logger.error(TAG, "FirebaseJobScheduler not found! Cannot schedule!");
                    }
                } else {
                    Logger.error(TAG, "WorkSpec not found! Cannot schedule!");
                }
                pendingResult.finish();
            }
        }).start();
    }

    @Nullable
    static FirebaseJobScheduler getFirebaseJobScheduler(
            @NonNull WorkManagerImpl workManager) {

        List<Scheduler> schedulers = workManager.getSchedulers();
        if (schedulers.isEmpty()) {
            return null;
        }
        for (int i = 0; i < schedulers.size(); i++) {
            Scheduler scheduler = schedulers.get(i);
            if (FirebaseJobScheduler.class.isAssignableFrom(scheduler.getClass())) {
                return (FirebaseJobScheduler) scheduler;
            }
        }
        return null;
    }
}
