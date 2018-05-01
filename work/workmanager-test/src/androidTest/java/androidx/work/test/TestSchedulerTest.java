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

package androidx.work.test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import androidx.work.Constraints;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkContinuation;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;
import androidx.work.WorkStatus;
import androidx.work.test.workers.TestWorker;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class TestSchedulerTest {

    private TestDriver mTestDriver;

    @Before
    public void setUp() {
        Context context = InstrumentationRegistry.getTargetContext();
        WorkManagerTestInitHelper.initializeTestWorkManager(context);
        mTestDriver = WorkManagerTestInitHelper.getTestDriver();
    }

    @Test
    public void testTestWorkerShouldSucceedSynchronously() {
        WorkRequest request = createWorkRequest();
        WorkManager workManager = WorkManager.getInstance();
        workManager.synchronous().enqueueSync(request);
        WorkStatus status = workManager.synchronous().getStatusByIdSync(request.getId());
        assertThat(status.getState().isFinished(), is(true));
    }

    @Test
    public void testTestWorkerShouldSucceedSynchronously_withDependentWork() {
        OneTimeWorkRequest request = createWorkRequest();
        OneTimeWorkRequest dependentRequest = createWorkRequest();
        WorkManager workManager = WorkManager.getInstance();
        WorkContinuation continuation = workManager.beginWith(request)
                .then(dependentRequest);
        continuation.synchronous().enqueueSync();
        WorkStatus requestStatus = workManager.synchronous().getStatusByIdSync(request.getId());
        WorkStatus dependentStatus = workManager
                .synchronous()
                .getStatusByIdSync(dependentRequest.getId());

        assertThat(requestStatus.getState().isFinished(), is(true));
        assertThat(dependentStatus.getState().isFinished(), is(true));
    }

    @Test
    public void testTestWorkerWithConstraintsShouldNoOp() {
        OneTimeWorkRequest request = createWorkRequestWithNetworkConstraints();
        WorkManager workManager = WorkManager.getInstance();
        workManager.enqueue(request);
        WorkStatus requestStatus = workManager.synchronous().getStatusByIdSync(request.getId());
        assertThat(requestStatus.getState().isFinished(), is(false));
    }

    @Test
    public void testTestWorkerWithConstraintsShouldSucceedAfterSetConstraints() {
        OneTimeWorkRequest request = createWorkRequestWithNetworkConstraints();
        WorkManager workManager = WorkManager.getInstance();
        workManager.enqueue(request);
        WorkStatus requestStatus = workManager.synchronous().getStatusByIdSync(request.getId());
        assertThat(requestStatus.getState().isFinished(), is(false));
        mTestDriver.setAllConstraintsMet(request.getId());
        requestStatus = workManager.synchronous().getStatusByIdSync(request.getId());
        assertThat(requestStatus.getState().isFinished(), is(true));
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
}
