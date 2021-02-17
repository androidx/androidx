/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.core.provider;

import android.os.Handler;
import android.os.Process;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

class FontRequestThreadPool {
    private final ThreadPoolExecutor mExecutor;
    private final ThreadFactory mThreadFactory;

    FontRequestThreadPool(
            @NonNull String threadName,
            int threadPriority,
            @IntRange(from = 0) int keepAliveTimeInMillis
    ) {
        mThreadFactory = new ThreadFactory(threadName, threadPriority);

        // allow core thread timeout to timeout the core threads so that
        // when no tasks arrive, the core threads can be killed
        mExecutor = new ThreadPoolExecutor(
                0 /* corePoolSize */,
                1 /* maximumPoolSize */,
                keepAliveTimeInMillis /* keepAliveTime */,
                TimeUnit.MILLISECONDS /* keepAliveTime TimeUnit */,
                new LinkedBlockingDeque<Runnable>() /* unbounded queue*/,
                mThreadFactory
        );
        mExecutor.allowCoreThreadTimeOut(true);
    }

    <T> T postAndWait(
            @NonNull final Callable<T> callable,
            @IntRange(from = 0) int timeoutMillis
    ) throws InterruptedException {
        Future<T> future = mExecutor.submit(callable);
        try {
            return future.get(timeoutMillis, TimeUnit.MILLISECONDS);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw e;
        } catch (TimeoutException e) {
            throw new InterruptedException("timeout");
        }
    }

    <T> void postAndReply(
            @NonNull final Callable<T> callable,
            @NonNull final ReplyCallback<T> callback
    ) {
        final Handler calleeHandler = CalleeHandler.create();
        mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                T t;
                try {
                    t = callable.call();
                } catch (Exception e) {
                    t = null;
                }
                final T result = t;

                calleeHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        callback.onReply(result);
                    }
                });
            }
        });
    }

    // TODO Remove
    @VisibleForTesting
    boolean isRunning() {
        return mExecutor.getPoolSize() != 0;
    }

    /**
     * Reply callback for postAndReply
     *
     * @param <T> A type which will be received as the argument.
     */
    interface ReplyCallback<T> {
        /**
         * Called when the task was finished.
         */
        void onReply(T value);
    }

    private static class ThreadFactory implements java.util.concurrent.ThreadFactory {
        private String mThreadName;
        private int mPriority;

        ThreadFactory(@NonNull String threadName, int priority) {
            mThreadName = threadName;
            mPriority = priority;
        }

        @Override
        public Thread newThread(Runnable runnable) {
            return new ProcessPriorityThread(runnable, mThreadName, mPriority);
        }

        private static class ProcessPriorityThread extends Thread {
            private final int mPriority;

            ProcessPriorityThread(Runnable target, String name, int priority) {
                super(target, name);
                mPriority = priority;
            }

            @Override
            public void run() {
                Process.setThreadPriority(mPriority);
                super.run();
            }
        }
    }
}
