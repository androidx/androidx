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
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.support.annotation.VisibleForTesting;
import android.text.TextUtils;
import android.util.Log;

import androidx.work.impl.ExecutionListener;
import androidx.work.impl.Processor;
import androidx.work.impl.WorkManagerImpl;
import androidx.work.impl.utils.WakeLocks;

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
    private static final int DEFAULT_START_ID = 0;

    private final Context mContext;
    private final WorkTimer mWorkTimer;
    private final Processor mProcessor;
    private final WorkManagerImpl mWorkManager;
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

        mContext = context.getApplicationContext();
        mCommandHandler = new CommandHandler(mContext);
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
        mCompletedListener = null;
    }

    @Override
    public void onExecuted(
            @NonNull String workSpecId,
            boolean isSuccessful,
            boolean needsReschedule) {

        // When there are lots of workers completing at around the same time,
        // this creates lock contention for the DelayMetCommandHandlers inside the CommandHandler.
        // So move the actual execution of the post completion callbacks on the command executor
        // thread.
        postOnMainThread(
                new AddRunnable(
                        this,
                        CommandHandler.createExecutionCompletedIntent(
                                mContext,
                                workSpecId,
                                isSuccessful,
                                needsReschedule),
                        DEFAULT_START_ID));
    }

    /**
     * Adds the {@link Intent} intent and the startId to the command processor queue.
     *
     * @param intent The {@link Intent} command that needs to be added to the command queue.
     * @param startId The command startId
     * @return <code>true</code> when the command was added to the command processor queue.
     */
    @MainThread
    public boolean add(@NonNull final Intent intent, final int startId) {
        assertMainThread();
        String action = intent.getAction();
        if (TextUtils.isEmpty(action)) {
            Log.w(TAG, "Unknown command. Ignoring");
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
            mIntents.add(intent);
        }
        processCommand();
        return true;
    }

    void setCompletedListener(@NonNull CommandsCompletedListener listener) {
        if (mCompletedListener != null) {
            Log.e(TAG, "A completion listener for SystemAlarmDispatcher already exists.");
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

    void postOnMainThread(@NonNull Runnable runnable) {
        mMainHandler.post(runnable);
    }

    @MainThread
    private void checkForCommandsCompleted() {
        assertMainThread();
        // if there are no more intents to process, and the command handler
        // has no more pending commands, stop the service.
        synchronized (mIntents) {
            if (!mCommandHandler.hasPendingCommands() && mIntents.isEmpty()) {
                Log.d(TAG, "No more commands & intents.");
                if (mCompletedListener != null) {
                    mCompletedListener.onAllCommandsCompleted();
                }
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
            // Process commands on the actual executor service,
            // so we are no longer blocking the main thread.
            mCommandExecutorService.submit(new Runnable() {
                @Override
                public void run() {
                    final Intent intent;
                    synchronized (mIntents) {
                        intent = mIntents.get(0);
                    }

                    if (intent != null) {
                        final String action = intent.getAction();
                        final int startId = intent.getIntExtra(KEY_START_ID, DEFAULT_START_ID);
                        Log.d(TAG, String.format("Processing command %s, %s", intent, startId));
                        final PowerManager.WakeLock wakeLock = WakeLocks.newWakeLock(
                                mContext,
                                String.format("%s (%s)", action, startId));
                        try {
                            Log.d(TAG, String.format(
                                    "Acquiring operation wake lock (%s) %s",
                                    action,
                                    wakeLock));

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
                            // an intent. Synchronized to prevent ConcurrentModificationExceptions.
                            synchronized (mIntents) {
                                mIntents.remove(0);
                            }

                            Log.d(TAG, String.format(
                                    "Releasing operation wake lock (%s) %s",
                                    action,
                                    wakeLock));

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
        if (mMainHandler.getLooper().getThread() != Thread.currentThread()) {
            throw new IllegalStateException("Needs to be invoked on the main thread.");
        }
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
