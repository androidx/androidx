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

import android.arch.background.workmanager.BaseWork;
import android.arch.background.workmanager.Constraints;
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
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.util.Log;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Service invoked by {@link android.app.AlarmManager} to run work tasks.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class SystemAlarmService extends LifecycleService implements ExecutionListener,
        Observer<List<WorkSpec>>, ConstraintsMetCallback {
    private static final String EXTRA_WORK_ID = "EXTRA_WORK_ID";
    private static final String ACTION_CONSTRAINT_CHANGED = "CONSTRAINT_CHANGED";
    private static final String ACTION_DELAY_MET = "DELAY_MET";
    private static final String ACTION_CANCEL_WORK = "CANCEL_WORK";
    private static final String TAG = "SystemAlarmService";
    private final List<WorkSpec> mObservedWorkSpecs = new LinkedList<>();
    private final List<WorkSpec> mDelayNotMetWorkSpecs = new LinkedList<>();
    private final Object mLock = new Object();
    private BackgroundProcessor mProcessor;
    private ConstraintsTracker mConstraintsTracker;

    @Override
    public void onCreate() {
        super.onCreate();
        Context context = getApplicationContext();
        WorkManagerImpl workManagerImpl = WorkManagerImpl.getInstance();
        WorkDatabase database = workManagerImpl.getWorkDatabase();
        Scheduler scheduler = workManagerImpl.getScheduler();
        mProcessor = new BackgroundProcessor(context, database, scheduler, this);
        mConstraintsTracker = new ConstraintsTracker(context, this);
        LiveDataUtils.dedupedLiveDataFor(database.workSpecDao().getSystemAlarmEligibleWorkSpecs())
                .observe(this, this);
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
                onDelayMet(workSpecId);
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

    /**
     * This method is invoked via Intent when the delay of a WorkSpec is met.
     * This will find the WorkSpec in mDelayNotMetWorkSpecs and either observe constraints
     * or process immediately.
     * This method may be invoked before the LiveData is ready and mDelayNotMetWorkSpecs would be
     * empty. When LiveData is ready, the delays of all WorkSpecs will be checked against
     * current time.
     *
     * @param workSpecId ID of {@link WorkSpec} whose delay has been met
     */
    private void onDelayMet(String workSpecId) {
        Log.d(TAG, "Delay Met intent received for " + workSpecId);
        synchronized (mLock) {
            WorkSpec workSpec = removeWorkSpecWithId(workSpecId, mDelayNotMetWorkSpecs);
            if (workSpec == null) {
                Log.d(TAG, "Could not find " + workSpecId + " in Delay Not Met list. "
                        + "Is LiveData initialized right now?");
            } else if (hasConstraints(workSpec)) {
                Log.d(TAG, "Observing constraints for " + workSpec);
                mObservedWorkSpecs.add(workSpec);
                updateConstraintsTrackerAndProxy();
            } else if (isEnqueued(workSpec)) {
                Log.d(TAG, "Processing " + workSpec + " immediately");
                mProcessor.process(workSpec.getId());
            } else {
                Log.d(TAG, workSpec + " is unconstrained and currently running");
            }
        }
    }

    @Override
    public void onExecuted(@NonNull String workSpecId, boolean needsReschedule) {
        Log.d(TAG, workSpecId + " executed on AlarmManager");
        // TODO(janclarin): Handle rescheduling if needed or if periodic.

        synchronized (mLock) {
            WorkSpec executedWorkSpec = removeWorkSpecWithId(workSpecId, mObservedWorkSpecs);
            if (executedWorkSpec != null) {
                updateConstraintsTrackerAndProxy();
            } else {
                Log.d(TAG, workSpecId + " not in Observed WorkSpecs list");
            }
        }

        if (!mProcessor.hasWork()) {
            // TODO(janclarin): Release wakelock.
            Log.d(TAG, "Stopping self");
            stopSelf();
        }
    }

    /**
     * This method is invoked when the list of {@link WorkSpec}s is updated.
     * The scheduled time is checked with respect to current time.
     * As time goes on and delays are met, {@link #onDelayMet(String)} will be invoked
     * and the {@link WorkSpec} will be handled correctly.
     *
     * @param workSpecs updated {@link WorkSpec}s
     */
    @Override
    public void onChanged(@Nullable List<WorkSpec> workSpecs) {
        if (workSpecs == null) {
            return;
        }
        Log.d(TAG, "onChanged; # workspecs = " + workSpecs.size());

        synchronized (mLock) {
            mObservedWorkSpecs.clear();
            mDelayNotMetWorkSpecs.clear();

            for (WorkSpec workSpec : workSpecs) {
                if (!isDelayMet(workSpec)) {
                    Log.d(TAG, "Delay not met for " + workSpec);
                    mDelayNotMetWorkSpecs.add(workSpec);
                } else if (hasConstraints(workSpec)) {
                    Log.d(TAG, "Observing constraints for " + workSpec);
                    mObservedWorkSpecs.add(workSpec);
                } else if (isEnqueued(workSpec)) {
                    Log.d(TAG, "Processing " + workSpec + " immediately");
                    mProcessor.process(workSpec.getId());
                } else {
                    Log.d(TAG, workSpec + " is unconstrained and currently running");
                }
            }

            updateConstraintsTrackerAndProxy();
        }
    }

    @Override
    public void onAllConstraintsMet(@NonNull List<String> workSpecIds) {
        for (String workSpecId : workSpecIds) {
            mProcessor.process(workSpecId);
        }
    }

    @Override
    public void onAllConstraintsNotMet(@NonNull List<String> workSpecIds) {
        for (String workSpecId : workSpecIds) {
            mProcessor.cancel(workSpecId, true);
        }
    }

    private void updateConstraintsTrackerAndProxy() {
        mConstraintsTracker.replace(mObservedWorkSpecs);
        ConstraintProxy.updateAll(getApplicationContext(), mObservedWorkSpecs);
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

    private static boolean isEnqueued(@NonNull WorkSpec workSpec) {
        return workSpec.getStatus() == BaseWork.STATUS_ENQUEUED;
    }

    private static boolean hasConstraints(@NonNull WorkSpec workSpec) {
        return !Constraints.NONE.equals(workSpec.getConstraints());
    }

    private static boolean isDelayMet(@NonNull WorkSpec workSpec) {
        return workSpec.calculateNextRunTime() <= System.currentTimeMillis();
    }

    private static WorkSpec removeWorkSpecWithId(String workSpecId, List<WorkSpec> workSpecs) {
        Iterator<WorkSpec> iterator = workSpecs.iterator();
        while (iterator.hasNext()) {
            WorkSpec workSpec = iterator.next();
            if (workSpec.getId().equals(workSpecId)) {
                iterator.remove();
                return workSpec;
            }
        }
        return null;
    }
}
