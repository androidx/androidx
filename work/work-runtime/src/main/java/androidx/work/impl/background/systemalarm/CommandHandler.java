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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.WorkerThread;
import androidx.work.Clock;
import androidx.work.Logger;
import androidx.work.impl.ExecutionListener;
import androidx.work.impl.StartStopToken;
import androidx.work.impl.StartStopTokens;
import androidx.work.impl.WorkDatabase;
import androidx.work.impl.WorkManagerImpl;
import androidx.work.impl.model.WorkGenerationalId;
import androidx.work.impl.model.WorkSpec;
import androidx.work.impl.model.WorkSpecDao;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The command handler used by {@link SystemAlarmDispatcher}.
 *
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class CommandHandler implements ExecutionListener {

    private static final String TAG = Logger.tagWithPrefix("CommandHandler");

    // actions
    static final String ACTION_SCHEDULE_WORK = "ACTION_SCHEDULE_WORK";
    static final String ACTION_DELAY_MET = "ACTION_DELAY_MET";
    static final String ACTION_STOP_WORK = "ACTION_STOP_WORK";
    static final String ACTION_CONSTRAINTS_CHANGED = "ACTION_CONSTRAINTS_CHANGED";
    static final String ACTION_RESCHEDULE = "ACTION_RESCHEDULE";
    static final String ACTION_EXECUTION_COMPLETED = "ACTION_EXECUTION_COMPLETED";

    // keys
    private static final String KEY_WORKSPEC_ID = "KEY_WORKSPEC_ID";
    private static final String KEY_WORKSPEC_GENERATION = "KEY_WORKSPEC_GENERATION";
    private static final String KEY_NEEDS_RESCHEDULE = "KEY_NEEDS_RESCHEDULE";

    // constants
    static final long WORK_PROCESSING_TIME_IN_MS = 10 * 60 * 1000L;

    // utilities
    static Intent createScheduleWorkIntent(@NonNull Context context,
            @NonNull WorkGenerationalId id) {
        Intent intent = new Intent(context, SystemAlarmService.class);
        intent.setAction(ACTION_SCHEDULE_WORK);
        return writeWorkGenerationalId(intent, id);
    }

    private static Intent writeWorkGenerationalId(@NonNull Intent intent,
            @NonNull WorkGenerationalId id) {
        intent.putExtra(KEY_WORKSPEC_ID, id.getWorkSpecId());
        intent.putExtra(KEY_WORKSPEC_GENERATION, id.getGeneration());
        return intent;
    }

    static WorkGenerationalId readWorkGenerationalId(@NonNull Intent intent) {
        return new WorkGenerationalId(intent.getStringExtra(KEY_WORKSPEC_ID),
                intent.getIntExtra(KEY_WORKSPEC_GENERATION, 0));
    }

    static Intent createDelayMetIntent(@NonNull Context context, @NonNull WorkGenerationalId id) {
        Intent intent = new Intent(context, SystemAlarmService.class);
        intent.setAction(ACTION_DELAY_MET);
        return writeWorkGenerationalId(intent, id);
    }

    static Intent createStopWorkIntent(@NonNull Context context, @NonNull String workSpecId) {
        Intent intent = new Intent(context, SystemAlarmService.class);
        intent.setAction(ACTION_STOP_WORK);
        intent.putExtra(KEY_WORKSPEC_ID, workSpecId);
        return intent;
    }
    static Intent createStopWorkIntent(@NonNull Context context, @NonNull WorkGenerationalId id) {
        Intent intent = new Intent(context, SystemAlarmService.class);
        intent.setAction(ACTION_STOP_WORK);
        return writeWorkGenerationalId(intent, id);
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
            @NonNull WorkGenerationalId id,
            boolean needsReschedule) {
        Intent intent = new Intent(context, SystemAlarmService.class);
        intent.setAction(ACTION_EXECUTION_COMPLETED);
        intent.putExtra(KEY_NEEDS_RESCHEDULE, needsReschedule);
        return writeWorkGenerationalId(intent, id);
    }

    // members
    private final Context mContext;
    private final Map<WorkGenerationalId, DelayMetCommandHandler> mPendingDelayMet;
    private final Object mLock;
    private final Clock mClock;
    private final StartStopTokens mStartStopTokens;

    CommandHandler(@NonNull Context context, Clock clock,
            @NonNull StartStopTokens startStopTokens) {
        mContext = context;
        mClock = clock;
        mStartStopTokens = startStopTokens;
        mPendingDelayMet = new HashMap<>();
        mLock = new Object();
    }

    @Override
    public void onExecuted(@NonNull WorkGenerationalId id, boolean needsReschedule) {
        synchronized (mLock) {
            // This listener is only necessary for knowing when a pending work is complete.
            // Delegate to the underlying execution listener itself.
            DelayMetCommandHandler listener = mPendingDelayMet.remove(id);
            mStartStopTokens.remove(id);
            if (listener != null) {
                listener.onExecuted(needsReschedule);
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
                Logger.get().error(TAG,
                        "Invalid request for " + action + " , requires " + KEY_WORKSPEC_ID + " .");
            } else {
                if (ACTION_SCHEDULE_WORK.equals(action)) {
                    handleScheduleWorkIntent(intent, startId, dispatcher);
                } else if (ACTION_DELAY_MET.equals(action)) {
                    handleDelayMet(intent, startId, dispatcher);
                } else if (ACTION_STOP_WORK.equals(action)) {
                    handleStopWork(intent, dispatcher);
                } else if (ACTION_EXECUTION_COMPLETED.equals(action)) {
                    handleExecutionCompleted(intent, startId);
                } else {
                    Logger.get().warning(TAG, "Ignoring intent " + intent);
                }
            }
        }
    }

    private void handleScheduleWorkIntent(
            @NonNull Intent intent,
            int startId,
            @NonNull SystemAlarmDispatcher dispatcher) {

        WorkGenerationalId id = readWorkGenerationalId(intent);
        Logger.get().debug(TAG, "Handling schedule work for " + id);

        WorkManagerImpl workManager = dispatcher.getWorkManager();
        WorkDatabase workDatabase = workManager.getWorkDatabase();
        workDatabase.beginTransaction();

        try {
            WorkSpecDao workSpecDao = workDatabase.workSpecDao();
            WorkSpec workSpec = workSpecDao.getWorkSpec(id.getWorkSpecId());

            // It is possible that this WorkSpec got cancelled/pruned since this isn't part of
            // the same database transaction as marking it enqueued (for example, if we using
            // any of the synchronous operations).  For now, handle this gracefully by exiting
            // the loop.  When we plumb ListenableFutures all the way through, we can remove the
            // *sync methods and return ListenableFutures, which will block on an operation on
            // the background task thread so all database operations happen on the same thread.
            // See b/114705286.
            if (workSpec == null) {
                Logger.get().warning(TAG,
                        "Skipping scheduling " + id + " because it's no longer in "
                        + "the DB");
                return;
            } else if (workSpec.state.isFinished()) {
                // We need to schedule the Alarms, even when the Worker is RUNNING. This is because
                // if the process gets killed, the Alarm is necessary to pick up the execution of
                // Work.
                Logger.get().warning(TAG,
                        "Skipping scheduling " + id + "because it is finished.");
                return;
            }

            // Note: The first instance of PeriodicWorker getting scheduled will set an alarm in the
            // past. This is because periodStartTime = 0.
            long triggerAt = workSpec.calculateNextRunTime();

            if (!workSpec.hasConstraints()) {
                Logger.get().debug(TAG,
                        "Setting up Alarms for " + id + "at " + triggerAt);
                Alarms.setAlarm(mContext, workDatabase, id, triggerAt);
            } else {
                // Schedule an alarm irrespective of whether all constraints matched.
                Logger.get().debug(TAG,
                        "Opportunistically setting an alarm for " + id + "at " + triggerAt);
                Alarms.setAlarm(
                        mContext,
                        workDatabase,
                        id,
                        triggerAt);

                // Schedule an update for constraint proxies
                // This in turn sets enables us to track changes in constraints
                Intent constraintsUpdate = CommandHandler.createConstraintsChangedIntent(mContext);
                dispatcher.getTaskExecutor().getMainThreadExecutor().execute(
                        new SystemAlarmDispatcher.AddRunnable(
                                dispatcher,
                                constraintsUpdate,
                                startId));
            }

            workDatabase.setTransactionSuccessful();
        } finally {
            workDatabase.endTransaction();
        }
    }

    private void handleDelayMet(
            @NonNull Intent intent,
            int startId,
            @NonNull SystemAlarmDispatcher dispatcher) {

        synchronized (mLock) {
            WorkGenerationalId id = readWorkGenerationalId(intent);
            Logger.get().debug(TAG, "Handing delay met for " + id);

            // Check to see if we are already handling an ACTION_DELAY_MET for the WorkSpec.
            // If we are, then there is nothing for us to do.
            if (!mPendingDelayMet.containsKey(id)) {
                DelayMetCommandHandler delayMetCommandHandler =
                        new DelayMetCommandHandler(mContext, startId,
                                dispatcher, mStartStopTokens.tokenFor(id));
                mPendingDelayMet.put(id, delayMetCommandHandler);
                delayMetCommandHandler.handleProcessWork();
            } else {
                Logger.get().debug(TAG, "WorkSpec " + id
                        + " is is already being handled for ACTION_DELAY_MET");
            }
        }
    }

    private void handleStopWork(
            @NonNull Intent intent,
            @NonNull SystemAlarmDispatcher dispatcher) {

        Bundle extras = intent.getExtras();
        String workSpecId = extras.getString(KEY_WORKSPEC_ID);
        List<StartStopToken> tokens;
        if (extras.containsKey(KEY_WORKSPEC_GENERATION)) {
            int generation = extras.getInt(KEY_WORKSPEC_GENERATION);
            tokens = new ArrayList<>(1);
            StartStopToken id = mStartStopTokens.remove(
                    new WorkGenerationalId(workSpecId, generation));
            if (id != null) {
                tokens.add(id);
            }
        } else {
            tokens = mStartStopTokens.remove(workSpecId);
        }
        for (StartStopToken token: tokens) {
            Logger.get().debug(TAG, "Handing stopWork work for " + workSpecId);
            dispatcher.getWorkerLauncher().stopWork(token);
            Alarms.cancelAlarm(mContext,
                    dispatcher.getWorkManager().getWorkDatabase(), token.getId());

            // Notify dispatcher, so it can clean up.
            dispatcher.onExecuted(token.getId(), false /* never reschedule */);
        }
    }

    private void handleConstraintsChanged(
            @NonNull Intent intent, int startId,
            @NonNull SystemAlarmDispatcher dispatcher) {

        Logger.get().debug(TAG, "Handling constraints changed " + intent);
        // Constraints changed command handler is synchronous. No cleanup
        // is necessary.
        ConstraintsCommandHandler changedCommandHandler =
                new ConstraintsCommandHandler(mContext, mClock, startId, dispatcher);
        changedCommandHandler.handleConstraintsChanged();
    }

    private void handleReschedule(
            @NonNull Intent intent,
            int startId,
            @NonNull SystemAlarmDispatcher dispatcher) {

        Logger.get().debug(TAG, "Handling reschedule " + intent + ", " + startId);
        dispatcher.getWorkManager().rescheduleEligibleWork();
    }

    private void handleExecutionCompleted(
            @NonNull Intent intent,
            int startId) {
        WorkGenerationalId id = readWorkGenerationalId(intent);
        boolean needsReschedule = intent.getExtras().getBoolean(KEY_NEEDS_RESCHEDULE);
        Logger.get().debug(
                TAG,
                "Handling onExecutionCompleted " + intent + ", " + startId);
        // Delegate onExecuted() to the command handler.
        onExecuted(id, needsReschedule);
    }

    @SuppressWarnings("deprecation")
    private static boolean hasKeys(@Nullable Bundle bundle, @NonNull String... keys) {
        if (bundle == null || bundle.isEmpty()) {
            return false;
        } else {
            for (String key : keys) {
                if (bundle.get(key) == null) {
                    return false;
                }
            }
            return true;
        }
    }
}
