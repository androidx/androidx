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

import static androidx.work.State.BLOCKED;
import static androidx.work.State.FAILED;
import static androidx.work.State.SUCCEEDED;
import static androidx.work.impl.Scheduler.MAX_SCHEDULER_LIMIT;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;

import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

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
    public void testEligibleWorkSpecsForScheduling() {
        long startTime = System.currentTimeMillis();
        OneTimeWorkRequest work = new OneTimeWorkRequest.Builder(TestWorker.class)
                .setPeriodStartTime(
                        startTime + TimeUnit.HOURS.toMillis(1),
                        TimeUnit.MILLISECONDS)
                .build();
        OneTimeWorkRequest succeeded = new OneTimeWorkRequest.Builder(TestWorker.class)
                .setPeriodStartTime(startTime, TimeUnit.MILLISECONDS)
                .setInitialState(SUCCEEDED)
                .build();
        OneTimeWorkRequest scheduled = new OneTimeWorkRequest.Builder(TestWorker.class)
                .setPeriodStartTime(startTime, TimeUnit.MILLISECONDS)
                .build();
        OneTimeWorkRequest enqueued = new OneTimeWorkRequest.Builder(TestWorker.class)
                .setPeriodStartTime(startTime, TimeUnit.MILLISECONDS)
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
                .setPeriodStartTime(startTime, TimeUnit.MILLISECONDS)
                .build();
        OneTimeWorkRequest succeeded = new OneTimeWorkRequest.Builder(TestWorker.class)
                .setScheduleRequestedAt(startTime, TimeUnit.MILLISECONDS)
                .setPeriodStartTime(startTime, TimeUnit.MILLISECONDS)
                .setInitialState(SUCCEEDED)
                .build();
        OneTimeWorkRequest failed = new OneTimeWorkRequest.Builder(TestWorker.class)
                .setScheduleRequestedAt(startTime, TimeUnit.MILLISECONDS)
                .setPeriodStartTime(startTime, TimeUnit.MILLISECONDS)
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
                .setPeriodStartTime(startTime, TimeUnit.MILLISECONDS)
                .build();
        OneTimeWorkRequest succeeded = new OneTimeWorkRequest.Builder(TestWorker.class)
                .setScheduleRequestedAt(startTime, TimeUnit.MILLISECONDS)
                .setPeriodStartTime(startTime, TimeUnit.MILLISECONDS)
                .setInitialState(SUCCEEDED)
                .build();
        OneTimeWorkRequest failed = new OneTimeWorkRequest.Builder(TestWorker.class)
                .setScheduleRequestedAt(startTime, TimeUnit.MILLISECONDS)
                .setPeriodStartTime(startTime, TimeUnit.MILLISECONDS)
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
                .setPeriodStartTime(startTime, TimeUnit.MILLISECONDS)
                .build();
        OneTimeWorkRequest succeeded = new OneTimeWorkRequest.Builder(TestWorker.class)
                .setScheduleRequestedAt(startTime, TimeUnit.MILLISECONDS)
                .setPeriodStartTime(startTime, TimeUnit.MILLISECONDS)
                .setInitialState(SUCCEEDED)
                .build();
        OneTimeWorkRequest blocked = new OneTimeWorkRequest.Builder(TestWorker.class)
                .setScheduleRequestedAt(startTime, TimeUnit.MILLISECONDS)
                .setPeriodStartTime(startTime, TimeUnit.MILLISECONDS)
                .setInitialState(BLOCKED)
                .build();
        OneTimeWorkRequest failed = new OneTimeWorkRequest.Builder(TestWorker.class)
                .setScheduleRequestedAt(startTime, TimeUnit.MILLISECONDS)
                .setPeriodStartTime(startTime, TimeUnit.MILLISECONDS)
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
}
