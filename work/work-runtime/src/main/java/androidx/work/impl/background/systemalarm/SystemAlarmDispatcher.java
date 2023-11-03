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
import android.os.Looper;
import android.os.PowerManager;
import android.text.TextUtils;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;
import androidx.work.Logger;
import androidx.work.impl.ExecutionListener;
import androidx.work.impl.Processor;
import androidx.work.impl.StartStopTokens;
import androidx.work.impl.WorkLauncher;
import androidx.work.impl.WorkLauncherImpl;
import androidx.work.impl.WorkManagerImpl;
import androidx.work.impl.model.WorkGenerationalId;
import androidx.work.impl.utils.WakeLocks;
import androidx.work.impl.utils.WorkTimer;
import androidx.work.impl.utils.taskexecutor.SerialExecutor;
import androidx.work.impl.utils.taskexecutor.TaskExecutor;

import java.util.ArrayList;
import java.util.List;

/**
 * The dispatcher used by the background processor which is based on
 * {@link android.app.AlarmManager}.
 *
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class SystemAlarmDispatcher implements ExecutionListener {

    // Synthetic accessor
    static final String TAG = Logger.tagWithPrefix("SystemAlarmDispatcher");

    private static final String PROCESS_COMMAND_TAG = "ProcessCommand";
    private static final String KEY_START_ID = "KEY_START_ID";
    private static final int DEFAULT_START_ID = 0;

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    final Context mContext;
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    final TaskExecutor mTaskExecutor;
    private final WorkTimer mWorkTimer;
    private final Processor mProcessor;
    private final WorkManagerImpl mWorkManager;
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    final CommandHandler mCommandHandler;
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    final List<Intent> mIntents;
    Intent mCurrentIntent;

    @Nullable
    private CommandsCompletedListener mCompletedListener;

    private StartStopTokens mStartStopTokens;
    private final WorkLauncher mWorkLauncher;

    SystemAlarmDispatcher(@NonNull Context context) {
        this(context, null, null, null);
    }

    @VisibleForTesting
    SystemAlarmDispatcher(
            @NonNull Context context,
            @Nullable Processor processor,
            @Nullable WorkManagerImpl workManager,
            @Nullable WorkLauncher launcher
    ) {
        mContext = context.getApplicationContext();
        mStartStopTokens = new StartStopTokens();
        mWorkManager = workManager != null ? workManager : WorkManagerImpl.getInstance(context);
        mCommandHandler = new CommandHandler(
                mContext, mWorkManager.getConfiguration().getClock(), mStartStopTokens);
        mWorkTimer = new WorkTimer(mWorkManager.getConfiguration().getRunnableScheduler());
        mProcessor = processor != null ? processor : mWorkManager.getProcessor();
        mTaskExecutor = mWorkManager.getWorkTaskExecutor();
        mWorkLauncher = launcher != null ? launcher :
                new WorkLauncherImpl(mProcessor, mTaskExecutor);
        mProcessor.addExecutionListener(this);
        // a list of pending intents which need to be processed
        mIntents = new ArrayList<>();
        // the current intent (command) being processed.
        mCurrentIntent = null;
    }

    /**
     * This method needs to be idempotent. This could be called more than once, and therefore,
     * this method should only perform cleanup when necessary.
     */
    void onDestroy() {
        Logger.get().debug(TAG, "Destroying SystemAlarmDispatcher");
        mProcessor.removeExecutionListener(this);
        mCompletedListener = null;
    }

    @Override
    public void onExecuted(@NonNull WorkGenerationalId id, boolean needsReschedule) {

        // When there are lots of workers completing at around the same time,
        // this creates lock contention for the DelayMetCommandHandlers inside the CommandHandler.
        // So move the actual execution of the post completion callbacks on the command executor
        // thread.
        mTaskExecutor.getMainThreadExecutor().execute(
                new AddRunnable(
                        this,
                        CommandHandler.createExecutionCompletedIntent(
                                mContext,
                                id,
                                needsReschedule),
                        DEFAULT_START_ID));
    }

    /**
     * Adds the {@link Intent} intent and the startId to the command processor queue.
     *
     * @param intent  The {@link Intent} command that needs to be added to the command queue.
     * @param startId The command startId
     * @return <code>true</code> when the command was added to the command processor queue.
     */
    @MainThread
    public boolean add(@NonNull final Intent intent, final int startId) {
        Logger.get().debug(TAG, "Adding command " + intent + " (" + startId + ")");
        assertMainThread();
        String action = intent.getAction();
        if (TextUtils.isEmpty(action)) {
            Logger.get().warning(TAG, "Unknown command. Ignoring");
            return false;
        }

        // If we have a constraints changed intent in the queue don't add a second one. We are
        // treating this intent as special because every time a worker with constraints is complete
        // it kicks off an update for constraint proxies.
        if (CommandHandler.ACTION_CONSTRAINTS_CHANGED.equals(action)
                && hasIntentWithAction(CommandHandler.ACTION_CONSTRAINTS_CHANGED)) {
            return false;
        }

        intent.putExtra(KEY_START_ID, startId);
        synchronized (mIntents) {
            boolean hasCommands = !mIntents.isEmpty();
            mIntents.add(intent);
            if (!hasCommands) {
                // Only call processCommand if this is the first command.
                // The call to dequeueAndCheckForCompletion will process the remaining commands
                // in the order that they were added.
                processCommand();
            }
        }
        return true;
    }

    void setCompletedListener(@NonNull CommandsCompletedListener listener) {
        if (mCompletedListener != null) {
            Logger.get().error(
                    TAG,
                    "A completion listener for SystemAlarmDispatcher already exists.");
            return;
        }
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

    TaskExecutor getTaskExecutor() {
        return mTaskExecutor;
    }

    WorkLauncher getWorkerLauncher() {
        return mWorkLauncher;
    }

    @MainThread
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    void dequeueAndCheckForCompletion() {
        Logger.get().debug(TAG, "Checking if commands are complete.");
        assertMainThread();

        synchronized (mIntents) {
            // Remove the intent from the list of processed commands.
            // We are doing this to avoid a race condition between completion of a
            // command in the command handler, and the checkForCompletion triggered
            // by a worker's onExecutionComplete().
            // For e.g.
            // t0 -> delay_met_intent
            // t1 -> bgProcessor.startWork(workSpec)
            // t2 -> constraints_changed_intent
            // t3 -> bgProcessor.onExecutionCompleted(...)
            // t4 -> DequeueAndCheckForCompletion (while constraints_changed_intent is
            // still being processed).

            // Note: this works only because mCommandExecutor service is a single
            // threaded executor. If that assumption changes in the future, use a
            // ReentrantLock, and lock the queue while command processor processes
            // an intent. Synchronized to prevent ConcurrentModificationExceptions.
            if (mCurrentIntent != null) {
                Logger.get().debug(TAG, "Removing command " + mCurrentIntent);
                if (!mIntents.remove(0).equals(mCurrentIntent)) {
                    throw new IllegalStateException("Dequeue-d command is not the first.");
                }
                mCurrentIntent = null;
            }
            SerialExecutor serialExecutor = mTaskExecutor.getSerialTaskExecutor();
            if (!mCommandHandler.hasPendingCommands()
                    && mIntents.isEmpty()
                    && !serialExecutor.hasPendingTasks()) {

                // If there are no more intents to process, and the command handler
                // has no more pending commands, stop the service.
                Logger.get().debug(TAG, "No more commands & intents.");
                if (mCompletedListener != null) {
                    mCompletedListener.onAllCommandsCompleted();
                }
            } else if (!mIntents.isEmpty()) {
                // Only process the next command if we have more commands.
                processCommand();
            }
        }
    }

    @MainThread
    @SuppressWarnings("FutureReturnValueIgnored")
    private void processCommand() {
        assertMainThread();
        PowerManager.WakeLock processCommandLock =
                WakeLocks.newWakeLock(mContext, PROCESS_COMMAND_TAG);
        try {
            processCommandLock.acquire();
            // Process commands on the background thread.
            mWorkManager.getWorkTaskExecutor().executeOnTaskThread(new Runnable() {
                @Override
                public void run() {
                    synchronized (mIntents) {
                        mCurrentIntent = mIntents.get(0);
                    }

                    if (mCurrentIntent != null) {
                        final String action = mCurrentIntent.getAction();
                        final int startId = mCurrentIntent.getIntExtra(KEY_START_ID,
                                DEFAULT_START_ID);
                        Logger.get().debug(TAG,
                                "Processing command " + mCurrentIntent + ", " + startId);
                        final PowerManager.WakeLock wakeLock = WakeLocks.newWakeLock(
                                mContext,
                                action + " (" + startId + ")");
                        try {
                            Logger.get().debug(TAG,
                                    "Acquiring operation wake lock (" + action + ") " + wakeLock);
                            wakeLock.acquire();
                            mCommandHandler.onHandleIntent(mCurrentIntent, startId,
                                    SystemAlarmDispatcher.this);
                        } catch (Throwable throwable) {
                            Logger.get().error(
                                    TAG,
                                    "Unexpected error in onHandleIntent",
                                    throwable);
                        }  finally {
                            Logger.get().debug(
                                    TAG,
                                    "Releasing operation wake lock (" + action + ") " + wakeLock);
                            wakeLock.release();
                            // Check if we have processed all commands
                            mTaskExecutor.getMainThreadExecutor().execute(
                                    new DequeueAndCheckForCompletion(SystemAlarmDispatcher.this)
                            );
                        }
                    }
                }
            });
        } finally {
            processCommandLock.release();
        }
    }

    @MainThread
    private boolean hasIntentWithAction(@NonNull String action) {
        assertMainThread();
        synchronized (mIntents) {
            for (Intent intent : mIntents) {
                if (action.equals(intent.getAction())) {
                    return true;
                }
            }
            return false;
        }
    }

    private void assertMainThread() {
        if (Looper.getMainLooper().getThread() != Thread.currentThread()) {
            throw new IllegalStateException("Needs to be invoked on the main thread.");
        }
    }

    /**
     * Checks if we are done executing all commands after dequeue-ing the current command.
     */
    static class DequeueAndCheckForCompletion implements Runnable {
        private final SystemAlarmDispatcher mDispatcher;

        DequeueAndCheckForCompletion(@NonNull SystemAlarmDispatcher dispatcher) {
            mDispatcher = dispatcher;
        }

        @Override
        public void run() {
            mDispatcher.dequeueAndCheckForCompletion();
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
