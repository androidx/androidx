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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import androidx.annotation.NonNull;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.MediumTest;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@RunWith(AndroidJUnit4.class)
@MediumTest
public class AsyncTaskLoaderTest {

    @Test
    public void testForceLoad_runsAsyncTask() throws InterruptedException {
        final TestAsyncTaskLoader loader = new TestAsyncTaskLoader(1);
        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                loader.forceLoad();
            }
        });

        assertTrue(loader.mLoadInBackgoundLatch.await(1, TimeUnit.SECONDS));
    }

    @Test
    public void testForceLoad_runsOnCustomExecutor() throws InterruptedException {
        final CountDownLatch executorLatch = new CountDownLatch(1);
        final TestAsyncTaskLoader loader = new TestAsyncTaskLoader(1);
        loader.mExecutor = new Executor() {
            @Override
            public void execute(@NonNull Runnable command) {
                executorLatch.countDown();
                command.run();
            }
        };
        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                loader.forceLoad();
            }
        });

        assertTrue(executorLatch.await(1, TimeUnit.SECONDS));
    }

    @Test
    public void testGetExecutor_notCalledOnConstruction() {
        TestAsyncTaskLoader loader = new TestAsyncTaskLoader(1);

        assertEquals(0, loader.mGetExecutorCallCount);
    }

    @Test
    public void testGetExecutor_forceLoadMultipleTimes_getExecutorCalledOnce()
            throws InterruptedException {
        final TestAsyncTaskLoader loader = new TestAsyncTaskLoader(3);
        final AtomicInteger loadCount = new AtomicInteger(3);
        loader.registerListener(0, new Loader.OnLoadCompleteListener<Void>() {
            @Override
            public void onLoadComplete(@NonNull Loader<Void> loader, Void data) {
                if (loadCount.getAndDecrement() > 0) {
                    loader.forceLoad();
                }
            }
        });
        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                loader.forceLoad();
            }
        });

        assertTrue(loader.mLoadInBackgoundLatch.await(1, TimeUnit.SECONDS));
        assertEquals(1, loader.mGetExecutorCallCount);
    }

    private static class TestAsyncTaskLoader extends AsyncTaskLoader<Void> {
        final CountDownLatch mLoadInBackgoundLatch;
        Executor mExecutor = null;
        int mGetExecutorCallCount = 0;

        TestAsyncTaskLoader(int latchCount) {
            super(ApplicationProvider.getApplicationContext());
            mLoadInBackgoundLatch = new CountDownLatch(latchCount);
        }

        @Override
        public Void loadInBackground() {
            mLoadInBackgoundLatch.countDown();
            return null;
        }

        @NonNull
        @Override
        protected Executor getExecutor() {
            mGetExecutorCallCount += 1;
            if (mExecutor != null) {
                return mExecutor;
            }
            return super.getExecutor();
        }
    }
}
