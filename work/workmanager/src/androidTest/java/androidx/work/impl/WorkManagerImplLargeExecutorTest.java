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

package androidx.work.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.lessThanOrEqualTo;

import static java.util.concurrent.TimeUnit.SECONDS;

import android.arch.core.executor.ArchTaskExecutor;
import android.arch.core.executor.TaskExecutor;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.LargeTest;
import android.support.test.filters.SdkSuppress;
import android.support.test.runner.AndroidJUnit4;

import androidx.work.Configuration;
import androidx.work.OneTimeWorkRequest;
import androidx.work.impl.model.WorkSpec;
import androidx.work.impl.utils.taskexecutor.InstantTaskExecutorRule;
import androidx.work.worker.TestWorker;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;

@RunWith(AndroidJUnit4.class)
public class WorkManagerImplLargeExecutorTest {

    private static final int NUM_WORKERS = 500;

    // ThreadPoolExecutor parameters.
    private static final int MIN_POOL_SIZE = 0;
    // Allocate more threads than the MAX_SCHEDULER_LIMIT
    private static final int MAX_POOL_SIZE = 150;
    // Keep alive time for a thread before its claimed.
    private static final long KEEP_ALIVE_TIME = 2L;

    private WorkManagerImpl mWorkManagerImpl;

    @Rule
    public InstantTaskExecutorRule mRule = new InstantTaskExecutorRule();

    @Before
    public void setUp() {
        ArchTaskExecutor.getInstance().setDelegate(new TaskExecutor() {
            @Override
            public void executeOnDiskIO(@NonNull Runnable runnable) {
                runnable.run();
            }

            @Override
            public void postToMainThread(@NonNull Runnable runnable) {
                runnable.run();
            }

            @Override
            public boolean isMainThread() {
                return true;
            }
        });

        Context context = InstrumentationRegistry.getTargetContext();
        BlockingQueue<Runnable> queue = new LinkedBlockingQueue<>();
        Executor executor = new ThreadPoolExecutor(
                MIN_POOL_SIZE, MAX_POOL_SIZE, KEEP_ALIVE_TIME, SECONDS, queue);
        Configuration configuration = new Configuration.Builder()
                .setExecutor(executor)
                .build();
        mWorkManagerImpl = new WorkManagerImpl(context, configuration);
        WorkManagerImpl.setDelegate(mWorkManagerImpl);
    }

    @After
    public void tearDown() {
        WorkManagerImpl.setDelegate(null);
        ArchTaskExecutor.getInstance().setDelegate(null);
    }

    @Test
    @LargeTest
    @SdkSuppress(maxSdkVersion = 22)
    public void testSchedulerLimits() {
        for (int i = 0; i < NUM_WORKERS; i++) {
            mWorkManagerImpl.enqueue(OneTimeWorkRequest.from(TestWorker.class));
            List<WorkSpec> eligibleWorkSpecs = mWorkManagerImpl.getWorkDatabase()
                    .workSpecDao()
                    .getEligibleWorkForScheduling();

            int size = eligibleWorkSpecs != null ? eligibleWorkSpecs.size() : 0;
            assertThat(size, lessThanOrEqualTo(Scheduler.MAX_SCHEDULER_LIMIT));
        }
    }
}
