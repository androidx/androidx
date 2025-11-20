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

package androidx.work.impl.background.systemalarm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.work.Logger;
import androidx.work.impl.WorkManagerImpl;

/**
 * Reschedules alarms on BOOT_COMPLETED.
 */
public class RescheduleReceiver extends BroadcastReceiver {

    private static final String TAG = Logger.tagWithPrefix("RescheduleReceiver");

    @Override
    public void onReceive(Context context, Intent intent) {
        Logger.get().debug(TAG, "Received intent " + intent);
        try {
            WorkManagerImpl workManager = WorkManagerImpl.getInstance(context);
            final PendingResult pendingResult = goAsync();
            workManager.setReschedulePendingResult(pendingResult);
        } catch (IllegalStateException e) {
            // WorkManager has not already been initialized.
            Logger.get().error(TAG,
                    "Cannot reschedule jobs. WorkManager needs to be initialized via a "
                            + "ContentProvider#onCreate() or an Application#onCreate().", e);
        }
    }
}
