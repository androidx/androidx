/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.work.impl.background.greedy;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;
import androidx.work.Logger;
import androidx.work.impl.model.WorkSpec;


/**
 * Keeps track of {@link androidx.work.WorkRequest}s that have a timing component in a
 * {@link GreedyScheduler}.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class DelayedWorkTracker {

    private final DelayedCallback mCallback;

    public DelayedWorkTracker(@NonNull GreedyScheduler scheduler) {
        mCallback = new DelayedCallback(scheduler);
        mCallback.setHandler(new Handler(Looper.getMainLooper(), mCallback));
    }

    @VisibleForTesting
    public DelayedWorkTracker(@NonNull DelayedCallback callback) {
        mCallback = callback;
    }

    /**
     * Cancels the existing instance of a {@link Runnable} if any, and schedules a new
     * {@link Runnable}; which eventually calls {@link GreedyScheduler#schedule(WorkSpec...)} at
     * the {@link WorkSpec}'s scheduled run time.
     *
     * @param workSpec The {@link WorkSpec} corresponding to the {@link androidx.work.WorkRequest}
     */
    public void schedule(@NonNull WorkSpec workSpec) {
        Message cancelMessage = DelayedCallback.unschedule(workSpec.id);
        Message message = DelayedCallback.schedule(workSpec);
        Handler handler = mCallback.getHandler();
        if (handler != null) {
            // Cancel any existing timers associated.
            handler.sendMessage(cancelMessage);
            handler.sendMessage(message);
        }
    }

    /**
     * Cancels the existing instance of a {@link Runnable} if any.
     *
     * @param workSpecId The {@link androidx.work.WorkRequest} id
     */
    public void unschedule(@NonNull String workSpecId) {
        Message message = DelayedCallback.unschedule(workSpecId);
        Handler handler = mCallback.getHandler();
        if (handler != null) {
            handler.sendMessage(message);
        }
    }

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public static class DelayedCallback implements android.os.Handler.Callback {
        // Synthetic access
        static final String TAG = Logger.tagWithPrefix("DelayedCallback");
        // Message ids
        @VisibleForTesting
        public static final int SCHEDULE = 1;
        @VisibleForTesting
        public static final int UNSCHEDULE = 2;

        @Nullable
        private Handler mHandler;

        // Synthetic access
        final GreedyScheduler mScheduler;

        public DelayedCallback(@NonNull GreedyScheduler scheduler) {
            mScheduler = scheduler;
        }

        @Override
        @MainThread
        public boolean handleMessage(@NonNull Message msg) {
            if (msg.what == SCHEDULE) {
                final WorkSpec workSpec = (WorkSpec) msg.obj;
                if (workSpec != null && mHandler != null) {
                    mHandler.postAtTime(new Runnable() {
                        @Override
                        public void run() {
                            Logger.get().debug(TAG,
                                    String.format("Scheduling work %s", workSpec.id));
                            mScheduler.schedule(workSpec);
                        }
                    }, workSpec.id, workSpec.calculateNextRunTime());
                }
                return true;
            } else if (msg.what == UNSCHEDULE) {
                String workSpecId = (String) msg.obj;
                if (!TextUtils.isEmpty(workSpecId) && mHandler != null) {
                    mHandler.removeMessages(SCHEDULE, workSpecId);
                }
                return true;
            } else {
                Logger.get().warning(TAG, "Unknown message. Ignoring");
                return false;
            }
        }

        public void setHandler(@NonNull Handler handler) {
            mHandler = handler;
        }

        /**
         * @return The {@link Handler} instance.
         */
        @Nullable
        public Handler getHandler() {
            return mHandler;
        }

        /**
         * Creates a {@link Message} which can be sent via a {@link Handler} to schedule a
         * delayed {@link androidx.work.WorkRequest}.
         *
         * @param workSpec The {@link WorkSpec} corresponding to the
         *                 {@link androidx.work.WorkRequest}
         * @return The {@link Message}
         */
        @NonNull
        public static Message schedule(@NonNull WorkSpec workSpec) {
            Message message = Message.obtain();
            message.what = SCHEDULE;
            message.obj = workSpec;
            return message;
        }

        /**
         * Creates a {@link Message} which can be sent via a {@link Handler} to cancel scheduling
         * of a delayed {@link androidx.work.WorkRequest}.
         *
         * @param workSpecId The {@link androidx.work.WorkRequest} id
         * @return The {@link Message}
         */
        @NonNull
        public static Message unschedule(@NonNull String workSpecId) {
            Message message = Message.obtain();
            message.what = UNSCHEDULE;
            message.obj = workSpecId;
            return message;
        }
    }
}
