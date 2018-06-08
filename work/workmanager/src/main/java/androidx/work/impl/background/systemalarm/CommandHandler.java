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

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.support.annotation.WorkerThread;
import android.util.Log;

import androidx.work.impl.ExecutionListener;
import androidx.work.impl.WorkDatabase;
import androidx.work.impl.WorkManagerImpl;
import androidx.work.impl.model.WorkSpec;
import androidx.work.impl.model.WorkSpecDao;

import java.util.HashMap;
import java.util.Map;

/**
 * The command handler used by {@link SystemAlarmDispatcher}.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class CommandHandler implements ExecutionListener {

    private static final String TAG = "CommandHandler";

    // actions
    static final String ACTION_SCHEDULE_WORK = "ACTION_SCHEDULE_WORK";
    static final String ACTION_DELAY_MET = "ACTION_DELAY_MET";
    static final String ACTION_STOP_WORK = "ACTION_STOP_WORK";
    static final String ACTION_CONSTRAINTS_CHANGED = "ACTION_CONSTRAINTS_CHANGED";
    static final String ACTION_RESCHEDULE = "ACTION_RESCHEDULE";
    static final String ACTION_EXECUTION_COMPLETED = "ACTION_EXECUTION_COMPLETED";

    // keys
    private static final String KEY_WORKSPEC_ID = "KEY_WORKSPEC_ID";
    private static final String KEY_IS_SUCCESSFUL = "KEY_IS_SUCCESSFUL";
    private static final String KEY_NEEDS_RESCHEDULE = "KEY_NEEDS_RESCHEDULE";

    // constants
    static final long WORK_PROCESSING_TIME_IN_MS = 10 * 60 * 1000L;

    // utilities
    static Intent createScheduleWorkIntent(@NonNull Context context, @NonNull String workSpecId) {
        Intent intent = new Intent(context, SystemAlarmService.class);
        intent.setAction(ACTION_SCHEDULE_WORK);
        intent.putExtra(KEY_WORKSPEC_ID, workSpecId);
        return intent;
    }

    static Intent createDelayMetIntent(@NonNull Context context, @NonNull String workSpecId) {
        Intent intent = new Intent(context, SystemAlarmService.class);
        intent.setAction(ACTION_DELAY_MET);
        intent.putExtra(KEY_WORKSPEC_ID, workSpecId);
        return intent;
    }

    static Intent createStopWorkIntent(@NonNull Context context, @NonNull String workSpecId) {
        Intent intent = new Intent(context, SystemAlarmService.class);
        intent.setAction(ACTION_STOP_WORK);
        intent.putExtra(KEY_WORKSPEC_ID, workSpecId);
        return intent;
    }

    static Intent createConstraintsChangedIntent(@NonNull Context context) {
        Intent intent = new Intent(context, SystemAlarmService.class);
        intent.setAction(ACTION_CONSTRAINTS_CHANGED);
        return intent;
    }

    static Intent createRescheduleIntent(@NonNull Context context) {
        Intent intent = new Intent(context, SystemAlarmService.class);
        intent.setAction(ACTION_RESCHEDULE);
        return intent;
    }

    static Intent createExecutionCompletedIntent(
            @NonNull Context context,
            @NonNull String workSpecId,
            boolean isSuccessful,
            boolean needsReschedule) {

        Intent intent = new Intent(context, SystemAlarmService.class);
        intent.setAction(ACTION_EXECUTION_COMPLETED);
        intent.putExtra(KEY_WORKSPEC_ID, workSpecId);
        intent.putExtra(KEY_IS_SUCCESSFUL, isSuccessful);
        intent.putExtra(KEY_NEEDS_RESCHEDULE, needsReschedule);
        return intent;
    }

    // members
    private final Context mContext;
    private final Map<String, ExecutionListener> mPendingDelayMet;
    private final Object mLock;

    CommandHandler(@NonNull Context context) {
        mContext = context;
        mPendingDelayMet = new HashMap<>();
        mLock = new Object();
    }

    @Override
    public void onExecuted(
            @NonNull String workSpecId,
            boolean isSuccessful,
            boolean needsReschedule) {

        synchronized (mLock) {
            // This listener is only necessary for knowing when a pending work is complete.
            // Delegate to the underlying execution listener itself.
            ExecutionListener listener = mPendingDelayMet.remove(workSpecId);
            if (listener != null) {
                listener.onExecuted(workSpecId, isSuccessful, needsReschedule);
            }
        }
    }

    /**
     * @return <code>true</code> if there is work pending.
     */
    boolean hasPendingCommands() {
        // Needs to be synchronized as this could be checked from
        // both the command processing thread, as well as the
        // onExecuted callback.
        synchronized (mLock) {
            // If we have pending work being executed on the background
            // processor - we are not done yet.
            return !mPendingDelayMet.isEmpty();
        }
    }

    /**
     * The actual command handler.
     */
    @WorkerThread
    void onHandleIntent(
            @NonNull Intent intent,
            int startId,
            @NonNull SystemAlarmDispatcher dispatcher) {

        String action = intent.getAction();

        if (ACTION_CONSTRAINTS_CHANGED.equals(action)) {
            handleConstraintsChanged(intent, startId, dispatcher);
        } else if (ACTION_RESCHEDULE.equals(action)) {
            handleReschedule(intent, startId, dispatcher);
        } else {
            Bundle extras = intent.getExtras();
            if (!hasKeys(extras, KEY_WORKSPEC_ID)) {
                Log.e(TAG, String.format(
                        "Invalid request for %s, requires %s.", action, KEY_WORKSPEC_ID));
            } else {
                if (ACTION_SCHEDULE_WORK.equals(action)) {
                    handleScheduleWorkIntent(intent, startId, dispatcher);
                } else if (ACTION_DELAY_MET.equals(action)) {
                    handleDelayMet(intent, startId, dispatcher);
                } else if (ACTION_STOP_WORK.equals(action)) {
                    handleStopWork(intent, startId, dispatcher);
                } else if (ACTION_EXECUTION_COMPLETED.equals(action)) {
                    handleExecutionCompleted(intent, startId, dispatcher);
                } else {
                    Log.w(TAG, String.format("Ignoring intent %s", intent));
                }
            }
        }
    }

    private void handleScheduleWorkIntent(
            @NonNull Intent intent,
            int startId,
            @NonNull SystemAlarmDispatcher dispatcher) {

        Bundle extras = intent.getExtras();
        String workSpecId = extras.getString(KEY_WORKSPEC_ID);
        Log.d(TAG, String.format("Handling schedule work for %s", workSpecId));

        WorkManagerImpl workManager = dispatcher.getWorkManager();
        WorkDatabase workDatabase = workManager.getWorkDatabase();
        WorkSpecDao workSpecDao = workDatabase.workSpecDao();

        WorkSpec workSpec = workSpecDao.getWorkSpec(workSpecId);
        long triggerAt = workSpec.calculateNextRunTime();

        if (!workSpec.hasConstraints()) {
            Log.d(TAG, String.format("Setting up Alarms for %s", workSpecId));
            Alarms.setAlarm(mContext, dispatcher.getWorkManager(), workSpecId, triggerAt);
        } else {
            // Schedule an alarm irrespective of whether all constraints matched.
            Log.d(TAG, String.format("Opportunistically setting an alarm for %s", workSpecId));
            Alarms.setAlarm(
                    mContext,
                    dispatcher.getWorkManager(),
                    workSpecId,
                    triggerAt);

            // Schedule an update for constraint proxies
            // This in turn sets enables us to track changes in constraints
            Intent constraintsUpdate = CommandHandler.createConstraintsChangedIntent(mContext);
            dispatcher.postOnMainThread(
                    new SystemAlarmDispatcher.AddRunnable(
                            dispatcher,
                            constraintsUpdate,
                            startId));
        }
    }

    private void handleDelayMet(
            @NonNull Intent intent,
            int startId,
            @NonNull SystemAlarmDispatcher dispatcher) {

        Bundle extras = intent.getExtras();
        synchronized (mLock) {
            String workSpecId = extras.getString(KEY_WORKSPEC_ID);
            Log.d(TAG, String.format("Handing delay met for %s", workSpecId));
            DelayMetCommandHandler delayMetCommandHandler =
                    new DelayMetCommandHandler(mContext, startId, workSpecId, dispatcher);
            mPendingDelayMet.put(workSpecId, delayMetCommandHandler);
            delayMetCommandHandler.handleProcessWork();
        }
    }

    private void handleStopWork(
            @NonNull Intent intent, int startId,
            @NonNull SystemAlarmDispatcher dispatcher) {

        Bundle extras = intent.getExtras();
        String workSpecId = extras.getString(KEY_WORKSPEC_ID);
        Log.d(TAG, String.format("Handing stopWork work for %s", workSpecId));

        dispatcher.getWorkManager().stopWork(workSpecId);
        Alarms.cancelAlarm(mContext, dispatcher.getWorkManager(), workSpecId);

        // Notify dispatcher, so it can clean up.
        dispatcher.onExecuted(workSpecId, false, false /* never reschedule */);
    }

    private void handleConstraintsChanged(
            @NonNull Intent intent, int startId,
            @NonNull SystemAlarmDispatcher dispatcher) {

        Log.d(TAG, String.format("Handling constraints changed %s", intent));
        // Constraints changed command handler is synchronous. No cleanup
        // is necessary.
        ConstraintsCommandHandler changedCommandHandler =
                new ConstraintsCommandHandler(mContext, startId, dispatcher);
        changedCommandHandler.handleConstraintsChanged();
    }

    private void handleReschedule(
            @NonNull Intent intent,
            int startId,
            @NonNull SystemAlarmDispatcher dispatcher) {

        Log.d(TAG, String.format("Handling reschedule %s, %s", intent, startId));
        dispatcher.getWorkManager().rescheduleEligibleWork();
    }

    private void handleExecutionCompleted(
            @NonNull Intent intent,
            int startId,
            @NonNull SystemAlarmDispatcher dispatcher) {

        Bundle extras = intent.getExtras();
        String workSpecId = extras.getString(KEY_WORKSPEC_ID);
        boolean isSuccessful = extras.getBoolean(KEY_IS_SUCCESSFUL);
        boolean needsReschedule = extras.getBoolean(KEY_NEEDS_RESCHEDULE);
        Log.d(TAG, String.format("Handling onExecutionCompleted %s, %s", intent, startId));
        // Delegate onExecuted() to the command handler.
        onExecuted(workSpecId, isSuccessful, needsReschedule);
        // Check if we need to stop service.
        dispatcher.postOnMainThread(
                new SystemAlarmDispatcher.CheckForCompletionRunnable(dispatcher));
    }

    private static boolean hasKeys(@Nullable Bundle bundle, @NonNull String... keys) {
        if (bundle == null || bundle.isEmpty()) {
            return false;
        } else {
            for (String key : keys) {
                if (!bundle.containsKey(key) || bundle.get(key) == null) {
                    return false;
                }
            }
            return true;
        }
    }
}
