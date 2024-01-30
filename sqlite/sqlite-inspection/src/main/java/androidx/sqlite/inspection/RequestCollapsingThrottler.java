/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.sqlite.inspection;

import androidx.annotation.GuardedBy;

/**
 * Throttler implementation ensuring that events are run not more frequently that specified
 * interval. Events submitted during the interval period are collapsed into one (i.e. only one is
 * executed).
 *
 * Thread safe.
 */
final class RequestCollapsingThrottler {
    private static final long NEVER = -1;

    private final Runnable mAction;
    private final long mMinIntervalMs;
    private final DeferredExecutor mExecutor;
    private final Object mLock = new Object();

    @GuardedBy("mLock") private boolean mPendingDispatch = false;
    @GuardedBy("mLock") private long mLastSubmitted = NEVER;

    RequestCollapsingThrottler(long minIntervalMs, Runnable action, DeferredExecutor executor) {
        mExecutor = executor;
        mAction = action;
        mMinIntervalMs = minIntervalMs;
    }

    public void submitRequest() {
        synchronized (mLock) {
            if (mPendingDispatch) {
                return;
            } else {
                mPendingDispatch = true; // about to schedule
            }
        }
        long delayMs = mMinIntervalMs - sinceLast(); // delayMs < 0 is OK
        scheduleDispatch(delayMs);
    }

    // TODO: switch to ListenableFuture to react on failures
    @SuppressWarnings("FutureReturnValueIgnored")
    private void scheduleDispatch(long delayMs) {
        mExecutor.schedule(new Runnable() {
            @Override
            public void run() {
                try {
                    mAction.run();
                } finally {
                    synchronized (mLock) {
                        mLastSubmitted = now();
                        mPendingDispatch = false;
                    }
                }
            }
        }, delayMs);
    }

    private static long now() {
        return System.currentTimeMillis();
    }

    private long sinceLast() {
        synchronized (mLock) {
            final long lastSubmitted = mLastSubmitted;
            return lastSubmitted == NEVER
                    ? (mMinIntervalMs + 1) // more than mMinIntervalMs
                    : (now() - lastSubmitted);
        }
    }

    interface DeferredExecutor {
        void schedule(Runnable command, long delayMs);
    }
}
