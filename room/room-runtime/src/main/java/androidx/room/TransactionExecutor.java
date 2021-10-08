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

package androidx.room;

import androidx.annotation.NonNull;

import java.util.ArrayDeque;
import java.util.concurrent.Executor;

/**
 * Executor wrapper for performing database transactions serially.
 * <p>
 * Since database transactions are exclusive, this executor ensures that transactions are performed
 * in-order and one at a time, preventing threads from blocking each other when multiple concurrent
 * transactions are attempted.
 */
class TransactionExecutor implements Executor {

    private final Executor mExecutor;
    private final ArrayDeque<Runnable> mTasks = new ArrayDeque<>();
    private Runnable mActive;

    TransactionExecutor(@NonNull Executor executor) {
        mExecutor = executor;
    }

    @Override
    public synchronized void execute(final Runnable command) {
        mTasks.offer(new Runnable() {
            @Override
            public void run() {
                try {
                    command.run();
                } finally {
                    scheduleNext();
                }
            }
        });
        if (mActive == null) {
            scheduleNext();
        }
    }

    @SuppressWarnings("WeakerAccess")
    synchronized void scheduleNext() {
        if ((mActive = mTasks.poll()) != null) {
            mExecutor.execute(mActive);
        }
    }
}
