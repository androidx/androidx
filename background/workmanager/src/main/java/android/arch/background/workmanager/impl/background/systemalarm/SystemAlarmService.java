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

import static android.arch.background.workmanager.impl.background.systemalarm
        .SystemAlarmServiceImpl.EXTRA_WORK_ID;

import android.arch.background.workmanager.impl.ExecutionListener;
import android.arch.background.workmanager.impl.Scheduler;
import android.arch.background.workmanager.impl.WorkDatabase;
import android.arch.background.workmanager.impl.WorkManagerImpl;
import android.arch.background.workmanager.impl.background.BackgroundProcessor;
import android.arch.background.workmanager.impl.constraints.ConstraintsMetCallback;
import android.arch.background.workmanager.impl.constraints.ConstraintsTracker;
import android.arch.background.workmanager.impl.model.WorkSpec;
import android.arch.background.workmanager.impl.utils.LiveDataUtils;
import android.arch.lifecycle.LifecycleService;
import android.arch.lifecycle.Observer;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.util.Log;

import java.util.List;

/**
 * Service invoked by {@link android.app.AlarmManager} to run work tasks.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class SystemAlarmService extends LifecycleService implements ExecutionListener,
        Observer<List<WorkSpec>>, ConstraintsMetCallback,
        SystemAlarmServiceImpl.AllWorkExecutedCallback {

    private static final String TAG = "SystemAlarmService";
    private SystemAlarmServiceImpl mSystemAlarmServiceImpl;

    @Override
    public void onCreate() {
        super.onCreate();
        WorkManagerImpl workManagerImpl = WorkManagerImpl.getInstance();
        WorkDatabase database = workManagerImpl.getWorkDatabase();
        Scheduler scheduler = workManagerImpl.getBackgroundScheduler();
        Context context = getApplicationContext();

        BackgroundProcessor processor = new BackgroundProcessor(
                context,
                database,
                scheduler,
                workManagerImpl.getBackgroundExecutorService(),
                this);
        ConstraintsTracker constraintsTracker = new ConstraintsTracker(context, this);
        mSystemAlarmServiceImpl =
                new SystemAlarmServiceImpl(context, processor, constraintsTracker, this);

        LiveDataUtils
                .dedupedLiveDataFor(database.workSpecDao().getSystemAlarmEligibleWorkSpecs())
                .observe(this, this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        return mSystemAlarmServiceImpl.onStartCommand(intent);
    }

    @Override
    public void onChanged(@Nullable List<WorkSpec> workSpecs) {
        if (workSpecs == null) {
            return;
        }
        Log.d(TAG, "onChanged. Number of WorkSpecs: " + workSpecs.size());
        mSystemAlarmServiceImpl.onEligibleWorkChanged(workSpecs);
    }

    @Override
    public void onAllConstraintsMet(@NonNull List<String> workSpecIds) {
        mSystemAlarmServiceImpl.onAllConstraintsMet(workSpecIds);
    }

    @Override
    public void onAllConstraintsNotMet(@NonNull List<String> workSpecIds) {
        mSystemAlarmServiceImpl.onAllConstraintsNotMet(workSpecIds);
    }

    @Override
    public void onExecuted(@NonNull String workSpecId, boolean needsReschedule) {
        Log.d(TAG, workSpecId + " executed on AlarmManager");
        mSystemAlarmServiceImpl.onExecuted(workSpecId, needsReschedule);
    }

    @Override
    public void onAllWorkExecuted() {
        Log.d(TAG, "All work executed. Stopping self");
        stopSelf();
    }

    static Intent createConstraintChangedIntent(Context context) {
        Intent intent = new Intent(context, SystemAlarmService.class);
        intent.setAction(SystemAlarmServiceImpl.ACTION_CONSTRAINT_CHANGED);
        return intent;
    }

    static Intent createDelayMetIntent(Context context, String workSpecId) {
        Intent intent = new Intent(context, SystemAlarmService.class);
        intent.setAction(SystemAlarmServiceImpl.ACTION_DELAY_MET);
        intent.putExtra(EXTRA_WORK_ID, workSpecId);
        return intent;
    }

    static Intent createCancelWorkIntent(Context context, String workSpecId) {
        Intent intent = new Intent(context, SystemAlarmService.class);
        intent.setAction(SystemAlarmServiceImpl.ACTION_CANCEL_WORK);
        intent.putExtra(EXTRA_WORK_ID, workSpecId);
        return intent;
    }
}
