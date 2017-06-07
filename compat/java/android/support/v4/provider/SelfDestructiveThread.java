/*
 * Copyright (C) 2017 The Android Open Source Project
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

package android.support.v4.provider;


import static android.support.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.support.annotation.GuardedBy;
import android.support.annotation.RestrictTo;
import android.support.annotation.VisibleForTesting;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Background thread which is destructed after certain period after all pending activities are
 * finished.
 * @hide
 */
@RestrictTo(LIBRARY_GROUP)
public class SelfDestructiveThread {
    private final Object mLock = new Object();

    @GuardedBy("mLock")
    private HandlerThread mThread;

    @GuardedBy("mLock")
    private Handler mHandler;

    @GuardedBy("mLock")
    private int mGeneration;  // The thread generation. Only for testing purpose.

    private static final int MSG_INVOKE_RUNNABLE = 1;
    private static final int MSG_DESTRUCTION = 0;

    private Handler.Callback mCallback = new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_INVOKE_RUNNABLE:
                    onInvokeRunnable((Runnable) msg.obj);
                    return true;
                case MSG_DESTRUCTION:
                    onDestruction();
                    return true;
            }
            return true;
        }
    };

    // Milliseconds the thread will be destructed after the last activity.
    private final int mDestructAfterMillisec;
    private final int mPriority;  // The priority of the thread.
    private final String mThreadName;  // The name of the thread.

    public SelfDestructiveThread(
            String threadName, int priority, int destructAfterMillisec) {
        mThreadName = threadName;
        mPriority = priority;
        mDestructAfterMillisec = destructAfterMillisec;
        mGeneration = 0;
    }

    /**
     * Returns true if the thread is alive.
     */
    @VisibleForTesting
    public boolean isRunning() {
        synchronized (mLock) {
            return mThread != null;
        }
    }

    /**
     * Returns the thread generation.
     */
    @VisibleForTesting
    public int getGeneration() {
        synchronized (mLock) {
            return mGeneration;
        }
    }

    private void post(Runnable runnable) {
        synchronized (mLock) {
            if (mThread == null) {
                mThread = new HandlerThread(mThreadName, mPriority);
                mThread.start();
                mHandler = new Handler(mThread.getLooper(), mCallback);
                mGeneration++;
            }
            mHandler.removeMessages(MSG_DESTRUCTION);
            mHandler.sendMessage(mHandler.obtainMessage(MSG_INVOKE_RUNNABLE, runnable));
        }
    }

    /**
     * Reply callback for postAndReply
     *
     * @param <T> A type which will be received as the argument.
     */
    public interface ReplyCallback<T> {
        /**
         * Called when the task was finished.
         */
        void onReply(T value);
    }

    /**
     * Execute the specific callable object on this thread and call the reply callback on the
     * calling thread once it finishs.
     */
    public <T> void postAndReply(final Callable<T> callable, final ReplyCallback<T> reply) {
        final Handler callingHandler = new Handler();
        post(new Runnable() {
            @Override
            public void run() {
                T t;
                try {
                    t = callable.call();
                } catch (Exception e) {
                    t = null;
                }
                final T result = t;
                callingHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        reply.onReply(result);
                    }
                });
            }
        });
    }

    /**
     * Execute the specified callable object on this thread and returns the returned value to the
     * caller.
     *
     * If the execution takes longer time than specified timeout duration, this function throws
     * InterruptedException.
     */
    public <T> T postAndWait(final Callable<T> callable, int timeoutMillis)
            throws InterruptedException {
        final ReentrantLock lock = new ReentrantLock();
        final Condition cond = lock.newCondition();

        final AtomicReference<T> holder = new AtomicReference();
        final AtomicBoolean running = new AtomicBoolean(true);
        post(new Runnable() {
            @Override
            public void run() {
                try {
                    holder.set(callable.call());
                } catch (Exception e) {
                    // Do nothing.
                }
                lock.lock();
                try {
                    running.set(false);
                    cond.signal();
                } finally {
                    lock.unlock();
                }
            }
        });

        lock.lock();
        try {
            if (!running.get()) {
                return holder.get();  // already finished.
            }
            long remaining = TimeUnit.MILLISECONDS.toNanos(timeoutMillis);
            for (;;) {
                try {
                    remaining = cond.awaitNanos(remaining);
                } catch (InterruptedException e) {
                    // ignore
                }
                if (!running.get()) {
                    return holder.get();  // Successfully finished.
                }
                if (remaining <= 0) {
                    throw new InterruptedException("timeout");  // Timeout
                }
            }
        } finally {
            lock.unlock();
        }
    }

    private void onInvokeRunnable(Runnable runnable) {
        runnable.run();
        synchronized (mLock) {
            mHandler.removeMessages(MSG_DESTRUCTION);
            mHandler.sendMessageDelayed(mHandler.obtainMessage(MSG_DESTRUCTION),
                    mDestructAfterMillisec);
        }
    }

    private void onDestruction() {
        synchronized (mLock) {
            if (mHandler.hasMessages(MSG_INVOKE_RUNNABLE)) {
                // This happens if post() is called after onDestruction and before synchronization
                // block.
                return;
            }
            mThread.quit();
            mThread = null;
            mHandler = null;
        }
    }
}
