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

package androidx.loader.content;

import static org.junit.Assert.fail;

import androidx.annotation.NonNull;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

@RunWith(AndroidJUnit4.class)
@SuppressWarnings("deprecation") /* AsyncTask */
public class ModernAsyncTaskTest {

    private ModernAsyncTask mModernAsyncTask;

    /**
     * Test to ensure that onCancelled is always called, even if doInBackground throws an exception.
     */
    @LargeTest
    @Test
    public void testCancellationWithException() throws Throwable {
        final CountDownLatch readyToCancel = new CountDownLatch(1);
        final CountDownLatch readyToThrow = new CountDownLatch(1);
        final CountDownLatch calledOnCancelled = new CountDownLatch(1);
        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                // we need to class load AsyncTask on API 15 in a thread with Looper,
                // because sInternalHandler was initialized as static field.
                // https://github.com/aosp-mirror/platform_frameworks_base/blob/ics-mr1/core/java/android/os/AsyncTask.java#L190
                //noinspection unused
                Executor unused = android.os.AsyncTask.THREAD_POOL_EXECUTOR;
                mModernAsyncTask = new ModernAsyncTask() {
                    @Override
                    protected Object doInBackground() {
                        readyToCancel.countDown();
                        try {
                            readyToThrow.await();
                        } catch (InterruptedException ignored) { }
                        // This exception is expected to be caught and ignored
                        throw new RuntimeException();
                    }

                    @Override
                    protected void onCancelled(Object o) {
                        calledOnCancelled.countDown();
                    }
                };
            }
        });

        mModernAsyncTask.executeOnExecutor(android.os.AsyncTask.THREAD_POOL_EXECUTOR);
        if (!readyToCancel.await(5, TimeUnit.SECONDS)) {
            fail("Test failure: doInBackground did not run in time.");
        }
        mModernAsyncTask.cancel(false);
        readyToThrow.countDown();
        if (!calledOnCancelled.await(5, TimeUnit.SECONDS)) {
            fail("onCancelled not called!");
        }
    }

    /**
     * Test to ensure that onCancelled is always called instead of onPostExecute when the exception
     * is not suppressed by cancelling the task.
     */
    @LargeTest
    @Test
    public void testException() throws Throwable {
        final CountDownLatch calledOnCancelled = new CountDownLatch(1);
        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                mModernAsyncTask = new ModernAsyncTask() {
                    @Override
                    protected Object doInBackground() {
                        throw new RuntimeException();
                    }

                    @Override
                    protected void onPostExecute(Object o) {
                        fail("onPostExecute should not be called");
                    }

                    @Override
                    protected void onCancelled(Object o) {
                        calledOnCancelled.countDown();
                    }
                };
            }
        });

        mModernAsyncTask.executeOnExecutor(new Executor() {
            @Override
            public void execute(@NonNull Runnable command) {
                try {
                    command.run();
                    fail("Exception not thrown");
                } catch (Throwable tr) {
                    // expected
                }
            }
        });

        if (!calledOnCancelled.await(5, TimeUnit.SECONDS)) {
            fail("onCancelled not called");
        }
    }
}
