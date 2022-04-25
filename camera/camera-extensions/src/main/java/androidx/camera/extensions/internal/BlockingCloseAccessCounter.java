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

package androidx.camera.extensions.internal;

import androidx.annotation.GuardedBy;
import androidx.annotation.RequiresApi;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A counter for blocking closing until all access to the counter has been completed.
 *
 * <pre>{@code
 * public void callingMethod() {
 *     if (!mAtomicAccessCounter.tryIncrement()) {
 *         return;
 *     }
 *
 *     try {
 *         // Some work that needs to be done
 *     } finally {
 *         mAtomicAccessCounter.decrement();
 *     }
 * }
 *
 * // Method that can only be called after all callingMethods are done with access
 * public void blockingMethod() {
 *     destroyAndWaitForZeroAccess();
 * }
 * }</pre>
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
final class BlockingCloseAccessCounter {
    @GuardedBy("mLock")
    private AtomicInteger mAccessCount = new AtomicInteger(0);
    private final Lock mLock = new ReentrantLock();
    private final Condition mDoneCondition = mLock.newCondition();

    private static final int COUNTER_DESTROYED_FLAG = -1;

    /**
     * Attempt to increment the access counter.
     *
     * <p>Once {@link #destroyAndWaitForZeroAccess()} has returned this will always fail to
     * increment, meaning access is not safe.
     *
     * @return true if the counter was incremented, false otherwise
     */
    boolean tryIncrement() {
        mLock.lock();
        try {
            if (mAccessCount.get() == COUNTER_DESTROYED_FLAG) {
                return false;
            }
            mAccessCount.getAndIncrement();
        } finally {
            mLock.unlock();
        }
        return true;
    }

    /**
     * Decrement the access counter.
     **/
    void decrement() {
        mLock.lock();
        try {
            switch (mAccessCount.getAndDecrement()) {
                case COUNTER_DESTROYED_FLAG:
                    throw new IllegalStateException("Unable to decrement. Counter already "
                            + "destroyed");
                case 0:
                    throw new IllegalStateException("Unable to decrement. No corresponding "
                            + "counter increment");
                default:
                    //
            }
            mDoneCondition.signal();
        } finally {
            mLock.unlock();
        }
    }

    /**
     * Blocks until there are zero accesses in the counter.
     *
     * <p>Once this call completes, the counter is destroyed and can not be incremented and
     * decremented.
     */
    void destroyAndWaitForZeroAccess() {
        mLock.lock();

        try {
            while (!mAccessCount.compareAndSet(0, COUNTER_DESTROYED_FLAG)) {
                try {
                    mDoneCondition.await();
                } catch (InterruptedException e) {
                    // Continue to check
                }
            }
        } finally {
            mLock.unlock();
        }
    }
}
