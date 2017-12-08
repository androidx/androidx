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

package android.arch.background.workmanager.impl.background.systemalarm;

import android.arch.background.workmanager.impl.ExecutionListener;
import android.arch.background.workmanager.impl.WorkDatabase;
import android.arch.background.workmanager.impl.WorkManagerImpl;
import android.arch.background.workmanager.impl.background.BackgroundProcessor;
import android.arch.lifecycle.LifecycleService;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.util.Log;

/**
 * Service invoked by {@link android.app.AlarmManager} to run work tasks.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class SystemAlarmService extends LifecycleService implements ExecutionListener {
    private static final String EXTRA_WORK_ID = "EXTRA_WORK_ID";
    private static final String ACTION_CONSTRAINT_CHANGED = "CONSTRAINT_CHANGED";
    private static final String ACTION_DELAY_MET = "DELAY_MET";
    private static final String ACTION_CANCEL_WORK = "CANCEL_WORK";
    private static final String TAG = "SystemAlarmService";
    private BackgroundProcessor mProcessor;

    @Override
    public void onCreate() {
        super.onCreate();
        Context context = getApplicationContext();
        WorkManagerImpl workManagerImpl = WorkManagerImpl.getInstance();
        WorkDatabase database = workManagerImpl.getWorkDatabase();
        mProcessor =
                new BackgroundProcessor(context, database, workManagerImpl.getScheduler(), this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // TODO(janclarin): Keep wakelock.
        super.onStartCommand(intent, flags, startId);
        String action = intent.getAction();
        String workSpecId = intent.getStringExtra(EXTRA_WORK_ID);

        if (action == null) {
            Log.e(TAG, "No action provided!");
            return START_NOT_STICKY;
        }

        switch (action) {
            case ACTION_DELAY_MET:
                // TODO(xbhatnag): Don't process immediately. Update ConstraintsTracker and Proxies.
                Log.d(TAG, "Processing work " + workSpecId);
                mProcessor.process(workSpecId);
                break;
            case ACTION_CANCEL_WORK:
                Log.d(TAG, "Cancelling work " + workSpecId);
                mProcessor.cancel(workSpecId, true);
                break;
            case ACTION_CONSTRAINT_CHANGED:
                Log.d(TAG, "Constraint Changed. Service woken");
                break;
            default:
                Log.e(TAG, "Unknown action");
        }
        return START_NOT_STICKY;
    }

    @Override
    public void onExecuted(@NonNull String workSpecId, boolean needsReschedule) {
        Log.d(TAG, workSpecId + " executed on AlarmManager");
        // TODO(janclarin): Handle rescheduling if needed or if periodic.
        // TODO(xbhatnag): Update ConstraintsTracker and Proxies.

        if (!mProcessor.hasWork()) {
            // TODO(janclarin): Release wakelock.
            Log.d(TAG, "Stopping self");
            stopSelf();
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        super.onBind(intent);
        return null;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "Destroyed");
        super.onDestroy();
    }

    static Intent createConstraintChangedIntent(Context context) {
        Intent intent = new Intent(context, SystemAlarmService.class);
        intent.setAction(SystemAlarmService.ACTION_CONSTRAINT_CHANGED);
        return intent;
    }

    static Intent createDelayMetIntent(Context context, String workSpecId) {
        Intent intent = new Intent(context, SystemAlarmService.class);
        intent.setAction(SystemAlarmService.ACTION_DELAY_MET);
        intent.putExtra(EXTRA_WORK_ID, workSpecId);
        return intent;
    }

    static Intent createCancelWorkIntent(Context context, String workSpecId) {
        Intent intent = new Intent(context, SystemAlarmService.class);
        intent.setAction(SystemAlarmService.ACTION_CANCEL_WORK);
        intent.putExtra(EXTRA_WORK_ID, workSpecId);
        return intent;
    }
}
