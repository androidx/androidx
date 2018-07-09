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

import static androidx.work.worker.CheckLimitsWorker.KEY_EXCEEDS_SCHEDULER_LIMIT;
import static androidx.work.worker.CheckLimitsWorker.KEY_LIMIT_TO_ENFORCE;
import static androidx.work.worker.CheckLimitsWorker.KEY_RECURSIVE;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import static java.util.concurrent.TimeUnit.SECONDS;

import android.arch.core.executor.ArchTaskExecutor;
import android.arch.core.executor.TaskExecutor;
import android.arch.lifecycle.Observer;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.LargeTest;
import android.support.test.filters.SdkSuppress;
import android.support.test.runner.AndroidJUnit4;

import androidx.work.Configuration;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.TestLifecycleOwner;
import androidx.work.WorkContinuation;
import androidx.work.WorkStatus;
import androidx.work.impl.utils.taskexecutor.InstantTaskExecutorRule;
import androidx.work.worker.CheckLimitsWorker;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
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

    private WorkManagerImpl mWorkManagerImpl;
    private TestLifecycleOwner mLifecycleOwner;

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
                .setMaxSchedulerLimit(50)
                .build();
        mWorkManagerImpl = new WorkManagerImpl(context, configuration, true);
        mLifecycleOwner = new TestLifecycleOwner();
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
    public void testSchedulerLimits() throws InterruptedException {
        List<OneTimeWorkRequest> workRequests = new ArrayList<>(NUM_WORKERS);
        final Set<UUID> completed = new HashSet<>(NUM_WORKERS);
        final int schedulerLimit = mWorkManagerImpl
                .getConfiguration()
                .getMaxSchedulerLimit();

        final Data input = new Data.Builder()
                .putInt(KEY_LIMIT_TO_ENFORCE, schedulerLimit)
                .build();

        for (int i = 0; i < NUM_WORKERS; i++) {
            OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(CheckLimitsWorker.class)
                    .setInputData(input)
                    .build();

            workRequests.add(request);
        }


        final CountDownLatch latch = new CountDownLatch(NUM_WORKERS);
        WorkContinuation continuation = mWorkManagerImpl.beginWith(workRequests);

        continuation.getStatuses()
                .observe(mLifecycleOwner, new Observer<List<WorkStatus>>() {
                    @Override
                    public void onChanged(@Nullable List<WorkStatus> workStatuses) {
                        if (workStatuses == null || workStatuses.isEmpty()) {
                            return;
                        }

                        for (WorkStatus workStatus: workStatuses) {
                            if (workStatus.getState().isFinished()) {

                                Data output = workStatus.getOutputData();

                                boolean exceededLimits = output.getBoolean(
                                        KEY_EXCEEDS_SCHEDULER_LIMIT, true);

                                assertThat(exceededLimits, is(false));
                                if (!completed.contains(workStatus.getId())) {
                                    completed.add(workStatus.getId());
                                    latch.countDown();
                                }
                            }
                        }
                    }
                });

        continuation.enqueue();
        latch.await(120L, TimeUnit.SECONDS);
        assertThat(latch.getCount(), is(0L));
    }

    @Test
    @LargeTest
    @SdkSuppress(maxSdkVersion = 22)
    public void testSchedulerLimitsRecursive() throws InterruptedException {
        List<OneTimeWorkRequest> workRequests = new ArrayList<>(NUM_WORKERS);
        final Set<UUID> completed = new HashSet<>(NUM_WORKERS);
        final int schedulerLimit = mWorkManagerImpl
                .getConfiguration()
                .getMaxSchedulerLimit();

        final Data input = new Data.Builder()
                .putBoolean(KEY_RECURSIVE, true)
                .putInt(KEY_LIMIT_TO_ENFORCE, schedulerLimit)
                .build();

        for (int i = 0; i < NUM_WORKERS; i++) {
            OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(CheckLimitsWorker.class)
                    .setInputData(input)
                    .build();

            workRequests.add(request);
        }


        final CountDownLatch latch = new CountDownLatch(NUM_WORKERS * 2); // recursive
        WorkContinuation continuation = mWorkManagerImpl.beginWith(workRequests);

        // There are more workers being enqueued recursively so use implicit tags.
        mWorkManagerImpl.getStatusesByTag(CheckLimitsWorker.class.getName())
                .observe(mLifecycleOwner, new Observer<List<WorkStatus>>() {
                    @Override
                    public void onChanged(@Nullable List<WorkStatus> workStatuses) {
                        if (workStatuses == null || workStatuses.isEmpty()) {
                            return;
                        }

                        for (WorkStatus workStatus: workStatuses) {
                            if (workStatus.getState().isFinished()) {

                                Data output = workStatus.getOutputData();

                                boolean exceededLimits = output.getBoolean(
                                        KEY_EXCEEDS_SCHEDULER_LIMIT, true);

                                assertThat(exceededLimits, is(false));
                                if (!completed.contains(workStatus.getId())) {
                                    completed.add(workStatus.getId());
                                    latch.countDown();
                                }
                            }
                        }
                    }
                });

        continuation.enqueue();
        latch.await(240L, TimeUnit.SECONDS);
        assertThat(latch.getCount(), is(0L));
    }
}
