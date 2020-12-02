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

package androidx.work.testing;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.lifecycle.Observer;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.work.Configuration;
import androidx.work.Constraints;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkContinuation;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;
import androidx.work.impl.WorkManagerImpl;
import androidx.work.testing.workers.CountingTestWorker;
import androidx.work.testing.workers.TestWorker;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class TestSchedulerTest {

    private Context mContext;
    private TestDriver mTestDriver;
    private Handler mHandler;

    @Before
    public void setUp() {
        mContext = ApplicationProvider.getApplicationContext();
        mHandler = new Handler(Looper.getMainLooper());
        // Don't set the task executor
        Configuration configuration = new Configuration.Builder()
                .setExecutor(new SynchronousExecutor())
                .setMinimumLoggingLevel(Log.DEBUG)
                .build();
        WorkManagerTestInitHelper.initializeTestWorkManager(mContext, configuration);
        mTestDriver = WorkManagerTestInitHelper.getTestDriver(mContext);
        CountingTestWorker.COUNT.set(0);
    }

    @Test
    public void testWorker_shouldSucceedSynchronously()
            throws InterruptedException, ExecutionException {

        WorkRequest request = createWorkRequest();
        // TestWorkManagerImpl is a subtype of WorkManagerImpl.
        WorkManagerImpl workManagerImpl = WorkManagerImpl.getInstance(mContext);
        workManagerImpl.enqueue(Collections.singletonList(request)).getResult().get();
        WorkInfo status = workManagerImpl.getWorkInfoById(request.getId()).get();
        assertThat(status.getState().isFinished(), is(true));
    }

    @Test
    public void testWorker_withDependentWork_shouldSucceedSynchronously()
            throws InterruptedException, ExecutionException {

        OneTimeWorkRequest request = createWorkRequest();
        OneTimeWorkRequest dependentRequest = createWorkRequest();
        WorkManager workManager = WorkManager.getInstance(mContext);
        WorkContinuation continuation = workManager.beginWith(request)
                .then(dependentRequest);
        continuation.enqueue().getResult().get();
        WorkInfo requestStatus = workManager.getWorkInfoById(request.getId()).get();
        WorkInfo dependentStatus = workManager
                .getWorkInfoById(dependentRequest.getId()).get();

        assertThat(requestStatus.getState().isFinished(), is(true));
        assertThat(dependentStatus.getState().isFinished(), is(true));
    }

    @Test
    public void testWorker_withConstraints_shouldNoOp()
            throws InterruptedException, ExecutionException {

        OneTimeWorkRequest request = createWorkRequestWithNetworkConstraints();
        WorkManager workManager = WorkManager.getInstance(mContext);
        workManager.enqueue(request);
        WorkInfo requestStatus = workManager.getWorkInfoById(request.getId()).get();
        assertThat(requestStatus.getState().isFinished(), is(false));
    }

    @Test
    public void testWorker_withConstraints_shouldSucceedAfterSetConstraints()
            throws InterruptedException, ExecutionException {

        OneTimeWorkRequest request = createWorkRequestWithNetworkConstraints();
        WorkManager workManager = WorkManager.getInstance(mContext);
        workManager.enqueue(request);
        WorkInfo requestStatus = workManager.getWorkInfoById(request.getId()).get();
        assertThat(requestStatus.getState().isFinished(), is(false));
        mTestDriver.setAllConstraintsMet(request.getId());
        requestStatus = workManager.getWorkInfoById(request.getId()).get();
        assertThat(requestStatus.getState().isFinished(), is(true));
    }

    @Test
    public void testWorker_withInitialDelay_shouldNoOp()
            throws InterruptedException, ExecutionException {

        OneTimeWorkRequest request = createWorkRequestWithInitialDelay();
        WorkManager workManager = WorkManager.getInstance(mContext);
        workManager.enqueue(request);
        WorkInfo requestStatus = workManager.getWorkInfoById(request.getId()).get();
        assertThat(requestStatus.getState().isFinished(), is(false));
    }

    @Test
    public void testWorker_withInitialDelay_shouldSucceedAfterSetInitialDelay()
            throws InterruptedException, ExecutionException {

        OneTimeWorkRequest request = createWorkRequestWithInitialDelay();
        WorkManager workManager = WorkManager.getInstance(mContext);
        workManager.enqueue(request);
        mTestDriver.setInitialDelayMet(request.getId());
        WorkInfo requestStatus = workManager.getWorkInfoById(request.getId()).get();
        assertThat(requestStatus.getState().isFinished(), is(true));
    }

    @Test
    public void testWorker_withPeriodDelay_shouldRun() {
        PeriodicWorkRequest request = createWorkRequestWithPeriodDelay();
        WorkManager workManager = WorkManager.getInstance(mContext);
        workManager.enqueue(request);
        assertThat(CountingTestWorker.COUNT.get(), is(1));
    }

    @Test
    public void testWorker_withPeriod_cancelAndResume_shouldRun()
            throws InterruptedException, ExecutionException {

        PeriodicWorkRequest request = createWorkRequestWithPeriodDelay();
        WorkManager workManager = WorkManager.getInstance(mContext);
        workManager.enqueue(request);
        workManager.cancelWorkById(request.getId());
        mTestDriver.setPeriodDelayMet(request.getId());
        WorkInfo requestStatus = workManager.getWorkInfoById(request.getId()).get();
        assertThat(requestStatus.getState().isFinished(), is(true));
    }

    @Test
    public void testWorker_withPeriodDelay_shouldRunAfterEachSetPeriodDelay()
            throws InterruptedException, ExecutionException {

        PeriodicWorkRequest request = createWorkRequestWithPeriodDelay();
        WorkManager workManager = WorkManager.getInstance(mContext);
        workManager.enqueue(request);
        assertThat(CountingTestWorker.COUNT.get(), is(1));
        for (int i = 0; i < 5; ++i) {
            mTestDriver.setPeriodDelayMet(request.getId());
            assertThat(CountingTestWorker.COUNT.get(), is(i + 2));
            WorkInfo requestStatus = workManager.getWorkInfoById(request.getId()).get();
            assertThat(requestStatus.getState().isFinished(), is(false));
        }
    }

    @Test
    public void testWorker_afterSuccessfulRun_postConditions()
            throws InterruptedException, ExecutionException {

        OneTimeWorkRequest request = createWorkRequest();
        WorkManager workManager = WorkManager.getInstance(mContext);
        workManager.enqueue(request).getResult().get();
        WorkInfo status = workManager.getWorkInfoById(request.getId()).get();
        assertThat(status.getState().isFinished(), is(true));
        mTestDriver.setAllConstraintsMet(request.getId());
        mTestDriver.setInitialDelayMet(request.getId());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testWorker_afterSuccessfulRun_throwsExceptionWhenSetPeriodDelayMet()
            throws InterruptedException, ExecutionException {

        OneTimeWorkRequest request = createWorkRequest();
        WorkManager workManager = WorkManager.getInstance(mContext);
        workManager.enqueue(request).getResult().get();
        WorkInfo status = workManager.getWorkInfoById(request.getId()).get();
        assertThat(status.getState().isFinished(), is(true));
        mTestDriver.setPeriodDelayMet(request.getId());
    }

    @Test
    @LargeTest
    public void testWorker_multipleSetInitialDelayMet_noDeadLock()
            throws InterruptedException, ExecutionException {

        Configuration configuration = new Configuration.Builder()
                .setMinimumLoggingLevel(Log.DEBUG)
                .build();
        WorkManagerTestInitHelper.initializeTestWorkManager(mContext, configuration);
        mTestDriver = WorkManagerTestInitHelper.getTestDriver(mContext);

        // This should not deadlock
        final OneTimeWorkRequest request = createWorkRequest();
        final WorkManager workManager = WorkManager.getInstance(mContext);
        workManager.enqueue(request).getResult().get();
        mTestDriver.setInitialDelayMet(request.getId());
        mTestDriver.setInitialDelayMet(request.getId());

        final CountDownLatch latch = new CountDownLatch(1);
        // Use the main looper to observe LiveData because we are using a SerialExecutor which is
        // wrapping a SynchronousExecutor.
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                workManager.getWorkInfoByIdLiveData(request.getId()).observeForever(
                        new Observer<WorkInfo>() {
                            @Override
                            public void onChanged(WorkInfo workInfo) {
                                if (workInfo != null && workInfo.getState().isFinished()) {
                                    latch.countDown();
                                }
                            }
                        });
            }
        });

        latch.await(5, TimeUnit.SECONDS);
        assertThat(latch.getCount(), is(0L));
    }

    @Test
    @LargeTest
    public void testWorker_multipleSetInitialDelayMetMultiThreaded_noDeadLock()
            throws InterruptedException {

        Configuration configuration = new Configuration.Builder()
                .setMinimumLoggingLevel(Log.DEBUG)
                .build();
        WorkManagerTestInitHelper.initializeTestWorkManager(mContext, configuration);
        mTestDriver = WorkManagerTestInitHelper.getTestDriver(mContext);

        // This should not deadlock
        final WorkManager workManager = WorkManager.getInstance(mContext);
        int numberOfWorkers = 10;
        final ExecutorService service = Executors.newFixedThreadPool(numberOfWorkers);
        for (int i = 0; i < numberOfWorkers; i++) {
            service.submit(new Runnable() {
                @Override
                public void run() {
                    final OneTimeWorkRequest request = createWorkRequest();
                    workManager.enqueue(request);
                    mTestDriver.setInitialDelayMet(request.getId());
                    mTestDriver.setInitialDelayMet(request.getId());
                }
            });
        }

        final CountDownLatch latch = new CountDownLatch(1);
        // Use the main looper to observe LiveData because we are using a SerialExecutor which is
        // wrapping a SynchronousExecutor.
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                // Using the implicit tag name.
                workManager.getWorkInfosByTagLiveData(TestWorker.class.getName()).observeForever(
                        new Observer<List<WorkInfo>>() {
                            @Override
                            public void onChanged(List<WorkInfo> workInfos) {
                                boolean completed = true;
                                if (workInfos != null && !workInfos.isEmpty()) {
                                    for (WorkInfo workInfo : workInfos) {
                                        if (!workInfo.getState().isFinished()) {
                                            completed = false;
                                            break;
                                        }
                                    }
                                }
                                if (completed) {
                                    latch.countDown();
                                }
                            }
                        });
            }
        });

        latch.await(10, TimeUnit.SECONDS);
        service.shutdownNow();
        assertThat(latch.getCount(), is(0L));
    }

    private static OneTimeWorkRequest createWorkRequest() {
        return new OneTimeWorkRequest.Builder(TestWorker.class).build();
    }

    private static OneTimeWorkRequest createWorkRequestWithNetworkConstraints() {
        return new OneTimeWorkRequest.Builder(TestWorker.class)
                .setConstraints(new Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build();
    }

    private static OneTimeWorkRequest createWorkRequestWithInitialDelay() {
        return new OneTimeWorkRequest.Builder(TestWorker.class)
                .setInitialDelay(10L, TimeUnit.DAYS)
                .build();
    }

    private static PeriodicWorkRequest createWorkRequestWithPeriodDelay() {
        return new PeriodicWorkRequest.Builder(CountingTestWorker.class, 10L, TimeUnit.DAYS)
                .build();
    }
}
