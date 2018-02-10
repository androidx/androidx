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

package android.arch.background.workmanager.impl.background.systemalarm;

import android.arch.background.workmanager.impl.ExecutionListener;
import android.arch.background.workmanager.impl.Processor;
import android.arch.background.workmanager.impl.WorkManagerImpl;
import android.arch.background.workmanager.impl.logger.Logger;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.support.annotation.VisibleForTesting;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * The dispatcher used by the background processor which is based on
 * {@link android.app.AlarmManager}.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class SystemAlarmDispatcher implements ExecutionListener {

    private static final String TAG = "SystemAlarmDispatcher";
    private static final String PROCESS_COMMAND_TAG = "ProcessCommand";
    private static final String KEY_START_ID = "KEY_START_ID";

    private final WorkTimer mWorkTimer;
    private final Processor mProcessor;
    private final WorkManagerImpl mWorkManager;
    private final PowerManager mPowerManager;
    private final CommandHandler mCommandHandler;
    private final Handler mMainHandler;
    private final List<Intent> mIntents;
    // The executor service responsible for dispatching all the commands.
    private final ExecutorService mCommandExecutorService;

    @Nullable private CommandsCompletedListener mCompletedListener;

    SystemAlarmDispatcher(@NonNull Context context) {
        this(context, null, null);
    }

    @VisibleForTesting
    SystemAlarmDispatcher(
            @NonNull Context context,
            @Nullable Processor processor,
            @Nullable WorkManagerImpl workManager) {

        Context appContext = context.getApplicationContext();
        mPowerManager = (PowerManager) appContext.getSystemService(Context.POWER_SERVICE);
        mCommandHandler = new CommandHandler(appContext);
        mWorkTimer = new WorkTimer();
        mWorkManager = workManager != null ? workManager : WorkManagerImpl.getInstance();
        mProcessor = processor != null ? processor : mWorkManager.getProcessor();
        mProcessor.addExecutionListener(this);
        // a list of pending intents which need to be processed
        mIntents = new ArrayList<>();
        mMainHandler = new Handler(Looper.getMainLooper());
        // Use a single thread executor for handling the actual
        // execution of the commands themselves
        mCommandExecutorService = Executors.newSingleThreadExecutor();
    }

    void onDestroy() {
        mProcessor.removeExecutionListener(this);
    }

    @Override
    public void onExecuted(
            @NonNull String workSpecId,
            boolean isSuccessful,
            boolean needsReschedule) {

        Logger.debug(TAG, "onExecuted (%s, %s, %s)", workSpecId, isSuccessful, needsReschedule);
        mCommandHandler.onExecuted(workSpecId, isSuccessful, needsReschedule);
        // check if we need to stop service
        postOnMainThread(new CheckForCompletionRunnable(this));
    }

    /**
     * Adds the {@link Intent} intent and the startId to the command processor queue.
     *
     * @param intent The {@link Intent} command that needs to be added to the command queue.
     * @param startId The command startId
     */
    @MainThread
    public void add(@NonNull final Intent intent, final int startId) {
        assertMainThread();
        String action = intent.getAction();
        if (TextUtils.isEmpty(action)) {
            Logger.warn(TAG, "Unknown command. Ignoring");
            return;
        }
        intent.putExtra(KEY_START_ID, startId);
        Logger.debug(TAG, "Adding intent to the queue %s, %s", intent, startId);
        mIntents.add(intent);
        processCommand();
    }

    void setCompletedListener(@NonNull CommandsCompletedListener listener) {
        mCompletedListener = listener;
    }

    Processor getProcessor() {
        return mProcessor;
    }

    WorkTimer getWorkTimer() {
        return mWorkTimer;
    }

    WorkManagerImpl getWorkManager() {
        return mWorkManager;
    }

    void postOnMainThread(@NonNull Runnable runnable) {
        mMainHandler.post(runnable);
    }

    @MainThread
    private void checkForCommandsCompleted() {
        assertMainThread();
        Logger.debug(TAG, "Checking if we are done executing all commands");
        // if there are no more intents to process, and the command handler
        // has no more pending commands, stop the service.
        if (!mCommandHandler.hasPendingCommands() && mIntents.isEmpty()) {
            Logger.debug(TAG, "No more commands & intents.");
            if (mCompletedListener != null) {
                mCompletedListener.onAllCommandsCompleted();
            }
        }
    }

    @MainThread
    @SuppressWarnings("FutureReturnValueIgnored")
    private void processCommand() {
        assertMainThread();
        PowerManager.WakeLock processCommandLock = newProcessCommandWakeLock();
        try {
            processCommandLock.acquire();
            // Process commands on the actual executor service,
            // so we are no longer blocking the main thread.
            mCommandExecutorService.submit(new Runnable() {
                @Override
                public void run() {
                    final Intent intent = mIntents.get(0);
                    if (intent != null) {
                        final String action = intent.getAction();
                        final int startId = intent.getIntExtra(KEY_START_ID, 0);
                        Logger.debug(TAG, "Processing command %s, %s", intent, startId);
                        final PowerManager.WakeLock wakeLock =
                                newOperationWakeLock(action, startId);
                        try {
                            Logger.debug(TAG, "Acquiring operation wake lock (%s) %s", action,
                                    wakeLock);
                            wakeLock.acquire();
                            mCommandHandler.onHandleIntent(intent, startId,
                                    SystemAlarmDispatcher.this);
                        } finally {
                            // Remove the intent from the queue, only after it has been processed.

                            // We are doing this to avoid a race condition between completion of a
                            // command in the command handler, and the checkForCompletion triggered
                            // by a worker's onExecutionComplete().
                            // For e.g.
                            // t0 -> delay_met_intent
                            // t1 -> bgProcessor.startWork(workSpec)
                            // t2 -> constraints_changed_intent
                            // t3 -> bgProcessor.onExecutionCompleted(...)
                            // t4 -> CheckForCompletionRunnable (while constraints_changed_intent is
                            // still being processed).

                            // Note: this works only because mCommandExecutor service is a single
                            // threaded executor. If that assumption changes in the future, use a
                            // ReentrantLock, and lock the queue while command processor processes
                            // an intent.
                            mIntents.remove(0);
                            Logger.debug(TAG, "Releasing operation wake lock (%s) %s", action,
                                    wakeLock);
                            wakeLock.release();
                            // Check if we have processed all commands
                            postOnMainThread(
                                    new CheckForCompletionRunnable(SystemAlarmDispatcher.this));
                        }
                    }
                }
            });
        } finally {
            processCommandLock.release();
        }
    }

    private void assertMainThread() {
        if (mMainHandler.getLooper().getThread() != Thread.currentThread()) {
            throw new IllegalStateException("Needs to be invoked on the main thread.");
        }
    }

    private PowerManager.WakeLock newProcessCommandWakeLock() {
        return mPowerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, PROCESS_COMMAND_TAG);
    }

    private PowerManager.WakeLock newOperationWakeLock(String action, int startId) {
        String tag = String.format("%s (%s)", action, startId);
        return mPowerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, tag);
    }

    /**
     * Checks if we are done executing all commands.
     */
    static class CheckForCompletionRunnable implements Runnable {
        private final SystemAlarmDispatcher mDispatcher;

        CheckForCompletionRunnable(@NonNull SystemAlarmDispatcher dispatcher) {
            mDispatcher = dispatcher;
        }

        @Override
        public void run() {
            mDispatcher.checkForCommandsCompleted();
        }
    }

    /**
     * Adds a new intent to the SystemAlarmDispatcher.
     */
    static class AddRunnable implements Runnable {
        private final SystemAlarmDispatcher mDispatcher;
        private final Intent mIntent;
        private final int mStartId;

        AddRunnable(@NonNull SystemAlarmDispatcher dispatcher,
                @NonNull Intent intent,
                int startId) {
            mDispatcher = dispatcher;
            mIntent = intent;
            mStartId = startId;
        }

        @Override
        public void run() {
            mDispatcher.add(mIntent, mStartId);
        }
    }

    /**
     * Used to notify interested parties when all pending commands and work is complete.
     */
    interface CommandsCompletedListener {
        void onAllCommandsCompleted();
    }
}
