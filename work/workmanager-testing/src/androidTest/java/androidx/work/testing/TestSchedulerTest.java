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

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class TestSchedulerTest {

    private Context mContext;
    private TestDriver mTestDriver;

    @Before
    public void setUp() {
        mContext = ApplicationProvider.getApplicationContext();
        WorkManagerTestInitHelper.initializeTestWorkManager(mContext);
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
