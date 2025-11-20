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

package androidx.work;

import static android.net.NetworkCapabilities.NET_CAPABILITY_MMS;
import static android.net.NetworkCapabilities.NET_CAPABILITY_NOT_VPN;
import static android.net.NetworkCapabilities.TRANSPORT_CELLULAR;

import static androidx.work.WorkInfo.State.BLOCKED;
import static androidx.work.WorkInfo.State.FAILED;
import static androidx.work.WorkInfo.State.SUCCEEDED;
import static androidx.work.impl.Scheduler.MAX_SCHEDULER_LIMIT;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;

import android.app.job.JobParameters;
import android.net.NetworkRequest;
import android.os.Build;
import android.provider.MediaStore.Images.Media;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SdkSuppress;
import androidx.test.filters.SmallTest;
import androidx.work.impl.model.WorkSpec;
import androidx.work.impl.model.WorkSpecDao;
import androidx.work.worker.TestWorker;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.concurrent.TimeUnit;

@RunWith(AndroidJUnit4.class)
public class WorkSpecDaoTest extends DatabaseTest {

    @Test
    @SmallTest
    public void testWorkSpecsForInserting() {
        long startTime = System.currentTimeMillis();
        OneTimeWorkRequest work = new OneTimeWorkRequest.Builder(TestWorker.class)
                .setLastEnqueueTime(
                        startTime + TimeUnit.HOURS.toMillis(1),
                        TimeUnit.MILLISECONDS)
                .build();
        OneTimeWorkRequest succeeded = new OneTimeWorkRequest.Builder(TestWorker.class)
                .setLastEnqueueTime(startTime, TimeUnit.MILLISECONDS)
                .setInitialState(SUCCEEDED)
                .build();
        OneTimeWorkRequest scheduled = new OneTimeWorkRequest.Builder(TestWorker.class)
                .setLastEnqueueTime(startTime, TimeUnit.MILLISECONDS)
                .build();
        OneTimeWorkRequest enqueued = new OneTimeWorkRequest.Builder(TestWorker.class)
                .setLastEnqueueTime(startTime, TimeUnit.MILLISECONDS)
                .build();

        insertWork(work);
        insertWork(succeeded);
        insertWork(scheduled);
        insertWork(enqueued);

        WorkSpecDao workSpecDao = mDatabase.workSpecDao();
        List<String> allWorkSpecIds =
                workSpecDao.getAllWorkSpecIds();
        assertThat(allWorkSpecIds.size(), equalTo(4));
        assertThat(allWorkSpecIds, containsInAnyOrder(
                work.getStringId(),
                enqueued.getStringId(),
                scheduled.getStringId(),
                succeeded.getStringId()
        ));
    }

    @Test
    @SmallTest
    public void testEligibleWorkSpecsForScheduling() {
        long startTime = System.currentTimeMillis();
        OneTimeWorkRequest work = new OneTimeWorkRequest.Builder(TestWorker.class)
                .setLastEnqueueTime(
                        startTime + TimeUnit.HOURS.toMillis(1),
                        TimeUnit.MILLISECONDS)
                .build();
        OneTimeWorkRequest succeeded = new OneTimeWorkRequest.Builder(TestWorker.class)
                .setLastEnqueueTime(startTime, TimeUnit.MILLISECONDS)
                .setInitialState(SUCCEEDED)
                .build();
        OneTimeWorkRequest scheduled = new OneTimeWorkRequest.Builder(TestWorker.class)
                .setLastEnqueueTime(startTime, TimeUnit.MILLISECONDS)
                .build();
        OneTimeWorkRequest enqueued = new OneTimeWorkRequest.Builder(TestWorker.class)
                .setLastEnqueueTime(startTime, TimeUnit.MILLISECONDS)
                .build();

        insertWork(work);
        insertWork(succeeded);
        insertWork(scheduled);
        insertWork(enqueued);

        WorkSpecDao workSpecDao = mDatabase.workSpecDao();
        // Treat the scheduled request as previously scheduled
        workSpecDao.markWorkSpecScheduled(scheduled.getStringId(), System.currentTimeMillis());
        List<WorkSpec> eligibleWorkSpecs =
                workSpecDao.getEligibleWorkForScheduling(MAX_SCHEDULER_LIMIT);

        assertThat(eligibleWorkSpecs.size(), equalTo(2));
        assertThat(eligibleWorkSpecs,
                containsInAnyOrder(work.getWorkSpec(), enqueued.getWorkSpec()));
    }

    @Test
    @SmallTest
    public void testScheduledWorkSpecCount() {
        WorkSpecDao workSpecDao = mDatabase.workSpecDao();

        long startTime = System.currentTimeMillis();
        OneTimeWorkRequest enqueued = new OneTimeWorkRequest.Builder(TestWorker.class)
                .setLastEnqueueTime(startTime, TimeUnit.MILLISECONDS)
                .build();
        OneTimeWorkRequest succeeded = new OneTimeWorkRequest.Builder(TestWorker.class)
                .setScheduleRequestedAt(startTime, TimeUnit.MILLISECONDS)
                .setLastEnqueueTime(startTime, TimeUnit.MILLISECONDS)
                .setInitialState(SUCCEEDED)
                .build();
        OneTimeWorkRequest failed = new OneTimeWorkRequest.Builder(TestWorker.class)
                .setScheduleRequestedAt(startTime, TimeUnit.MILLISECONDS)
                .setLastEnqueueTime(startTime, TimeUnit.MILLISECONDS)
                .setInitialState(FAILED)
                .build();

        insertWork(enqueued);
        insertWork(succeeded);
        insertWork(failed);

        List<WorkSpec> eligibleWorkSpecs =
                workSpecDao.getEligibleWorkForScheduling(MAX_SCHEDULER_LIMIT);
        assertThat(eligibleWorkSpecs, notNullValue());
        assertThat(eligibleWorkSpecs.size(), is(1));
        assertThat(eligibleWorkSpecs, containsInAnyOrder(enqueued.getWorkSpec()));
    }

    @Test
    @SmallTest
    public void testAlreadyScheduledWorkIsNotRescheduled() {
        WorkSpecDao workSpecDao = mDatabase.workSpecDao();

        long startTime = System.currentTimeMillis();
        OneTimeWorkRequest enqueued = new OneTimeWorkRequest.Builder(TestWorker.class)
                .setScheduleRequestedAt(startTime, TimeUnit.MILLISECONDS) // already scheduled
                .setLastEnqueueTime(startTime, TimeUnit.MILLISECONDS)
                .build();
        OneTimeWorkRequest succeeded = new OneTimeWorkRequest.Builder(TestWorker.class)
                .setScheduleRequestedAt(startTime, TimeUnit.MILLISECONDS)
                .setLastEnqueueTime(startTime, TimeUnit.MILLISECONDS)
                .setInitialState(SUCCEEDED)
                .build();
        OneTimeWorkRequest failed = new OneTimeWorkRequest.Builder(TestWorker.class)
                .setScheduleRequestedAt(startTime, TimeUnit.MILLISECONDS)
                .setLastEnqueueTime(startTime, TimeUnit.MILLISECONDS)
                .setInitialState(FAILED)
                .build();

        insertWork(enqueued);
        insertWork(succeeded);
        insertWork(failed);

        List<WorkSpec> eligibleWorkSpecs =
                workSpecDao.getEligibleWorkForScheduling(MAX_SCHEDULER_LIMIT);
        assertThat(eligibleWorkSpecs, notNullValue());
        assertThat(eligibleWorkSpecs.size(), is(0));
    }

    @Test
    @SmallTest
    public void testResetScheduledState() {
        WorkSpecDao workSpecDao = mDatabase.workSpecDao();

        long startTime = System.currentTimeMillis();
        OneTimeWorkRequest enqueued = new OneTimeWorkRequest.Builder(TestWorker.class)
                .setScheduleRequestedAt(startTime, TimeUnit.MILLISECONDS)
                .setLastEnqueueTime(startTime, TimeUnit.MILLISECONDS)
                .build();
        OneTimeWorkRequest succeeded = new OneTimeWorkRequest.Builder(TestWorker.class)
                .setScheduleRequestedAt(startTime, TimeUnit.MILLISECONDS)
                .setLastEnqueueTime(startTime, TimeUnit.MILLISECONDS)
                .setInitialState(SUCCEEDED)
                .build();
        OneTimeWorkRequest blocked = new OneTimeWorkRequest.Builder(TestWorker.class)
                .setScheduleRequestedAt(startTime, TimeUnit.MILLISECONDS)
                .setLastEnqueueTime(startTime, TimeUnit.MILLISECONDS)
                .setInitialState(BLOCKED)
                .build();
        OneTimeWorkRequest failed = new OneTimeWorkRequest.Builder(TestWorker.class)
                .setScheduleRequestedAt(startTime, TimeUnit.MILLISECONDS)
                .setLastEnqueueTime(startTime, TimeUnit.MILLISECONDS)
                .setInitialState(FAILED)
                .build();

        insertWork(enqueued);
        workSpecDao.markWorkSpecScheduled(enqueued.getStringId(), startTime);

        insertWork(succeeded);
        insertWork(failed);
        insertWork(blocked);

        workSpecDao.resetScheduledState();

        List<WorkSpec> eligibleWorkSpecs =
                workSpecDao.getEligibleWorkForScheduling(MAX_SCHEDULER_LIMIT);
        assertThat(eligibleWorkSpecs.size(), is(1));
        // Not using contains in any order as the scheduleRequestedAt changes post reset.
        assertThat(eligibleWorkSpecs.get(0).id, is(enqueued.getStringId()));
    }

    @SdkSuppress(minSdkVersion = 24)
    @Test
    @SmallTest
    public void testEligibleWorkForSchedulingWithContentUris() {
        long startTime = System.currentTimeMillis();
        Constraints constraints = new Constraints.Builder().addContentUriTrigger(
                Media.EXTERNAL_CONTENT_URI, true).build();
        OneTimeWorkRequest enqueuedNoUris = new OneTimeWorkRequest.Builder(TestWorker.class)
                .setLastEnqueueTime(startTime, TimeUnit.MILLISECONDS)
                .build();
        OneTimeWorkRequest enqueuedWithUris = new OneTimeWorkRequest.Builder(TestWorker.class)
                .setLastEnqueueTime(startTime, TimeUnit.MILLISECONDS)
                .setConstraints(constraints)
                .build();
        insertWork(enqueuedNoUris);
        insertWork(enqueuedWithUris);

        WorkSpecDao workSpecDao = mDatabase.workSpecDao();
        List<WorkSpec> workSpecs =
                workSpecDao.getEligibleWorkForSchedulingWithContentUris();
        assertThat(workSpecs.size(), is(1));
        assertThat(workSpecs.get(0).id, is(enqueuedWithUris.getStringId()));
    }

    @Test
    @SdkSuppress(minSdkVersion = 24)
    @SmallTest
    public void testCountNonFinishedContentUriTriggerWorkers() {
        OneTimeWorkRequest request1 = new OneTimeWorkRequest.Builder(TestWorker.class).build();
        Constraints constraints = new Constraints.Builder().addContentUriTrigger(
                Media.EXTERNAL_CONTENT_URI, true).build();
        OneTimeWorkRequest request2 = new OneTimeWorkRequest.Builder(TestWorker.class)
                .setConstraints(constraints)
                .build();
        OneTimeWorkRequest request3 = new OneTimeWorkRequest.Builder(TestWorker.class)
                .setInitialState(SUCCEEDED)
                .setConstraints(constraints)
                .build();
        OneTimeWorkRequest request4 = new OneTimeWorkRequest.Builder(TestWorker.class)
                .setInitialState(FAILED)
                .setConstraints(constraints)
                .build();
        OneTimeWorkRequest request5 = new OneTimeWorkRequest.Builder(TestWorker.class)
                .setConstraints(constraints)
                .build();
        mDatabase.workSpecDao().insertWorkSpec(request1.getWorkSpec());
        mDatabase.workSpecDao().insertWorkSpec(request2.getWorkSpec());
        mDatabase.workSpecDao().insertWorkSpec(request3.getWorkSpec());
        mDatabase.workSpecDao().insertWorkSpec(request4.getWorkSpec());
        mDatabase.workSpecDao().insertWorkSpec(request5.getWorkSpec());
        WorkSpecDao workSpecDao = mDatabase.workSpecDao();
        int count = workSpecDao.countNonFinishedContentUriTriggerWorkers();
        assertThat(count, is(2));
    }

    @Test
    @SmallTest
    public void checkSetCancelled() {
        OneTimeWorkRequest request1 = new OneTimeWorkRequest.Builder(TestWorker.class)
                .setInitialState(WorkInfo.State.RUNNING)
                .build();
        OneTimeWorkRequest request2 = new OneTimeWorkRequest.Builder(TestWorker.class)
                .build();
        WorkSpecDao workSpecDao = mDatabase.workSpecDao();
        workSpecDao.insertWorkSpec(request1.getWorkSpec());
        workSpecDao.insertWorkSpec(request2.getWorkSpec());
        workSpecDao.setCancelledState(request1.getStringId());
        workSpecDao.setCancelledState(request2.getStringId());
        WorkSpec workSpec = workSpecDao.getWorkSpec(request1.getStringId());
        WorkSpec workSpec2 = workSpecDao.getWorkSpec(request2.getStringId());
        assertThat(workSpec.getStopReason(), is(JobParameters.STOP_REASON_CANCELLED_BY_APP));
        assertThat(workSpec2.getStopReason(), is(WorkInfo.STOP_REASON_NOT_STOPPED));
    }

    @Test
    @SmallTest
    public void insertWithNetworkRequest() {
        Constraints constraints;
        NetworkRequest request = new NetworkRequest.Builder()
                .addCapability(NET_CAPABILITY_MMS)
                .addCapability(NET_CAPABILITY_NOT_VPN)
                .addTransportType(TRANSPORT_CELLULAR)
                .build();
        constraints = new Constraints.Builder()
                .setRequiredNetworkRequest(request, NetworkType.CONNECTED)
                .build();

        OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(TestWorker.class)
                .setConstraints(constraints)
                .build();
        WorkSpecDao workSpecDao = mDatabase.workSpecDao();
        workSpecDao.insertWorkSpec(workRequest.getWorkSpec());

        WorkSpec workSpec = workSpecDao.getWorkSpec(workRequest.getStringId());
        Constraints newConstraints = workSpec.constraints;
        if (Build.VERSION.SDK_INT >= 28) {
            NetworkRequest actualRequest = newConstraints.getRequiredNetworkRequest();
            assertThat(actualRequest, notNullValue());
            assertThat(actualRequest.hasCapability(NET_CAPABILITY_MMS), is(true));
            assertThat(actualRequest.hasCapability(NET_CAPABILITY_NOT_VPN), is(true));
            assertThat(actualRequest.hasTransport(TRANSPORT_CELLULAR), is(true));
            assertThat(newConstraints.getRequiredNetworkType(), is(NetworkType.NOT_REQUIRED));
        } else {
            assertThat(newConstraints.getRequiredNetworkType(), is(NetworkType.CONNECTED));
        }
    }
}
