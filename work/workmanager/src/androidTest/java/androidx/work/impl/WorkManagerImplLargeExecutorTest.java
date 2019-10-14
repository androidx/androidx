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

import static androidx.work.worker.RandomSleepTestWorker.MAX_SLEEP_DURATION_MS;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import static java.util.concurrent.TimeUnit.SECONDS;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.arch.core.executor.ArchTaskExecutor;
import androidx.lifecycle.Observer;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.filters.SdkSuppress;
import androidx.work.Configuration;
import androidx.work.OneTimeWorkRequest;
import androidx.work.TestLifecycleOwner;
import androidx.work.WorkContinuation;
import androidx.work.WorkInfo;
import androidx.work.impl.background.greedy.GreedyScheduler;
import androidx.work.impl.model.WorkSpec;
import androidx.work.impl.utils.taskexecutor.InstantWorkTaskExecutor;
import androidx.work.impl.utils.taskexecutor.TaskExecutor;
import androidx.work.worker.RandomSleepTestWorker;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@RunWith(AndroidJUnit4.class)
public class WorkManagerImplLargeExecutorTest {

    private static final int NUM_WORKERS = 200;

    // ThreadPoolExecutor parameters.
    private static final int MIN_POOL_SIZE = 0;
    // Allocate more threads than the MAX_SCHEDULER_LIMIT
    private static final int MAX_POOL_SIZE = 150;
    // Keep alive time for a thread before its claimed.
    private static final long KEEP_ALIVE_TIME = 2L;

    private static final int TEST_SCHEDULER_LIMIT = 50;


    private WorkManagerImpl mWorkManagerImplSpy;
    private TestLifecycleOwner mLifecycleOwner;

    @Before
    public void setUp() {
        ArchTaskExecutor.getInstance().setDelegate(new androidx.arch.core.executor.TaskExecutor() {
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

        Context context = ApplicationProvider.getApplicationContext();
        BlockingQueue<Runnable> queue = new LinkedBlockingQueue<>();
        Executor executor = new ThreadPoolExecutor(
                MIN_POOL_SIZE, MAX_POOL_SIZE, KEEP_ALIVE_TIME, SECONDS, queue);
        Configuration configuration = new Configuration.Builder()
                .setExecutor(executor)
                .setMaxSchedulerLimit(TEST_SCHEDULER_LIMIT)
                .build();
        TaskExecutor taskExecutor = new InstantWorkTaskExecutor();
        mWorkManagerImplSpy = spy(
                new WorkManagerImpl(context, configuration, taskExecutor, true));

        TrackingScheduler trackingScheduler =
                new TrackingScheduler(context, taskExecutor, mWorkManagerImplSpy);

        Processor processor = new Processor(context,
                configuration,
                mWorkManagerImplSpy.getWorkTaskExecutor(),
                mWorkManagerImplSpy.getWorkDatabase(),
                Collections.singletonList((Scheduler) trackingScheduler));

        when(mWorkManagerImplSpy.getSchedulers()).thenReturn(
                Collections.singletonList((Scheduler) trackingScheduler));
        when(mWorkManagerImplSpy.getProcessor()).thenReturn(processor);

        mLifecycleOwner = new TestLifecycleOwner();
        WorkManagerImpl.setDelegate(mWorkManagerImplSpy);
    }

    @After
    public void tearDown() {
        WorkManagerImpl.setDelegate(null);
        ArchTaskExecutor.getInstance().setDelegate(null);
    }

    @Test
    @LargeTest
    @SdkSuppress(minSdkVersion = 22, maxSdkVersion = 22)
    public void testSchedulerLimits() throws InterruptedException {
        List<OneTimeWorkRequest> workRequests = new ArrayList<>(NUM_WORKERS);
        final Set<UUID> completed = new HashSet<>(NUM_WORKERS);

        for (int i = 0; i < NUM_WORKERS; i++) {
            OneTimeWorkRequest request =
                    new OneTimeWorkRequest.Builder(RandomSleepTestWorker.class).build();
            workRequests.add(request);
        }


        final CountDownLatch latch = new CountDownLatch(NUM_WORKERS);
        WorkContinuation continuation = mWorkManagerImplSpy.beginWith(workRequests);

        continuation.getWorkInfosLiveData()
                .observe(mLifecycleOwner, new Observer<List<WorkInfo>>() {
                    @Override
                    public void onChanged(@Nullable List<WorkInfo> workInfos) {
                        if (workInfos == null || workInfos.isEmpty()) {
                            return;
                        }

                        for (WorkInfo workInfo : workInfos) {
                            if (workInfo.getState().isFinished()) {
                                if (!completed.contains(workInfo.getId())) {
                                    completed.add(workInfo.getId());
                                    latch.countDown();
                                }
                            }
                        }
                    }
                });

        continuation.enqueue();
        latch.await((NUM_WORKERS * MAX_SLEEP_DURATION_MS) + 1000L, TimeUnit.MILLISECONDS);
        assertThat(latch.getCount(), is(0L));
    }

    /**
     * A GreedyScheduler that makes sure we never exceed TEST_SCHEDULER_LIMIT.
     */
    private static class TrackingScheduler extends GreedyScheduler {

        private static final Object sLock = new Object();

        private Set<String> mScheduledWorkSpecIds;

        TrackingScheduler(Context context,
                TaskExecutor taskExecutor,
                WorkManagerImpl workManagerImpl) {
            super(context, taskExecutor, workManagerImpl);
            mScheduledWorkSpecIds = new HashSet<>();
        }

        @Override
        public void schedule(@NonNull WorkSpec... workSpecs) {
            synchronized (sLock) {
                for (WorkSpec workSpec : workSpecs) {
                    assertThat(mScheduledWorkSpecIds.contains(workSpec.id), is(false));
                    mScheduledWorkSpecIds.add(workSpec.id);
                    assertThat(mScheduledWorkSpecIds.size() <= TEST_SCHEDULER_LIMIT, is(true));
                }
            }
            super.schedule(workSpecs);
        }

        @Override
        public void onExecuted(@NonNull String workSpecId, boolean needsReschedule) {
            synchronized (sLock) {
                assertThat(mScheduledWorkSpecIds.contains(workSpecId), is(true));
                mScheduledWorkSpecIds.remove(workSpecId);
            }
            super.onExecuted(workSpecId, needsReschedule);
        }
    }
}
