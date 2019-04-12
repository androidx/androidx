/*
 * Copyright (C) 2019 The Android Open Source Project
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

package androidx.camera.core;

import androidx.annotation.GuardedBy;
import androidx.camera.core.impl.utils.executor.CameraXExecutors;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.Executor;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@SmallTest
@RunWith(AndroidJUnit4.class)
public final class IoExecutorTest {

    private Executor mIoExecutor;
    private Lock mLock = new ReentrantLock();
    private Condition mCondition = mLock.newCondition();
    @GuardedBy("lock")
    private RunnableState mState = RunnableState.CLEAR;
    private final Runnable mRunnable1 =
            new Runnable() {
                @Override
                public void run() {
                    mLock.lock();
                    try {
                        mState = RunnableState.RUNNABLE1_WAITING;
                        mCondition.signalAll();
                        while (mState != RunnableState.CLEAR) {
                            mCondition.await();
                        }

                        mState = RunnableState.RUNNABLE1_FINISHED;
                        mCondition.signalAll();
                    } catch (InterruptedException e) {
                        throw new RuntimeException("Thread interrupted unexpectedly", e);
                    } finally {
                        mLock.unlock();
                    }
                }
            };
    private final Runnable mRunnable2 =
            new Runnable() {
                @Override
                public void run() {
                    mLock.lock();
                    try {
                        while (mState != RunnableState.RUNNABLE1_WAITING) {
                            mCondition.await();
                        }

                        mState = RunnableState.RUNNABLE2_FINISHED;
                        mCondition.signalAll();
                    } catch (InterruptedException e) {
                        throw new RuntimeException("Thread interrupted unexpectedly", e);
                    } finally {
                        mLock.unlock();
                    }
                }
            };
    private final Runnable mSimpleRunnable1 =
            new Runnable() {
                @Override
                public void run() {
                    mLock.lock();
                    try {
                        mState = RunnableState.RUNNABLE1_FINISHED;
                        mCondition.signalAll();
                    } finally {
                        mLock.unlock();
                    }
                }
            };

    @Before
    public void setup() {
        mLock.lock();
        try {
            mState = RunnableState.CLEAR;
        } finally {
            mLock.unlock();
        }
        mIoExecutor = CameraXExecutors.ioExecutor();
    }

    @Test
    public void canRunRunnable() throws InterruptedException {
        mIoExecutor.execute(mSimpleRunnable1);
        mLock.lock();
        try {
            while (mState != RunnableState.RUNNABLE1_FINISHED) {
                mCondition.await();
            }
        } finally {
            mLock.unlock();
        }

        // No need to check anything here. Completing this method should signal success.
    }

    @Test
    public void canRunMultipleRunnableInParallel() throws InterruptedException {
        mIoExecutor.execute(mRunnable1);
        mIoExecutor.execute(mRunnable2);

        mLock.lock();
        try {
            // mRunnable2 cannot finish until mRunnable1 has started
            while (mState != RunnableState.RUNNABLE2_FINISHED) {
                mCondition.await();
            }

            // Allow mRunnable1 to finish
            mState = RunnableState.CLEAR;
            mCondition.signalAll();

            while (mState != RunnableState.RUNNABLE1_FINISHED) {
                mCondition.await();
            }
        } finally {
            mLock.unlock();
        }

        // No need to check anything here. Completing this method should signal success.
    }

    private enum RunnableState {
        CLEAR,
        RUNNABLE1_WAITING,
        RUNNABLE1_FINISHED,
        RUNNABLE2_FINISHED
    }
}
