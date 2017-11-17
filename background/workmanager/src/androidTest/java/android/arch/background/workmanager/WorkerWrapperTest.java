/*
 * Copyright (C) 2017 The Android Open Source Project
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

package android.arch.background.workmanager;

import static android.arch.background.workmanager.Work.STATUS_BLOCKED;
import static android.arch.background.workmanager.Work.STATUS_ENQUEUED;
import static android.arch.background.workmanager.Work.STATUS_SUCCEEDED;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import android.arch.background.workmanager.model.Dependency;
import android.arch.background.workmanager.model.DependencyDao;
import android.arch.background.workmanager.model.WorkSpec;
import android.arch.background.workmanager.model.WorkSpecDao;
import android.arch.background.workmanager.utils.taskexecutor.InstantTaskExecutorRule;
import android.arch.background.workmanager.worker.FailureWorker;
import android.arch.background.workmanager.worker.SleepTestWorker;
import android.arch.background.workmanager.worker.TestWorker;
import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.LargeTest;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;

import java.util.concurrent.Executors;

@RunWith(AndroidJUnit4.class)
public class WorkerWrapperTest {
    private WorkDatabase mDatabase;
    private WorkSpecDao mWorkSpecDao;
    private DependencyDao mDependencyDao;
    private Context mContext;
    private ExecutionListener mMockListener;
    private Scheduler mMockScheduler;

    @Rule
    public InstantTaskExecutorRule mRule = new InstantTaskExecutorRule();

    @Before
    public void setUp() {
        mContext = InstrumentationRegistry.getTargetContext();
        mDatabase = WorkDatabase.create(mContext, true);
        mWorkSpecDao = mDatabase.workSpecDao();
        mDependencyDao = mDatabase.dependencyDao();
        mMockListener = mock(ExecutionListener.class);
        mMockScheduler = mock(Scheduler.class);
    }

    @After
    public void tearDown() {
        mDatabase.close();
    }

    @Test
    @SmallTest
    public void testSuccess() throws InterruptedException {
        Work work = new Work.Builder(TestWorker.class).build();
        mWorkSpecDao.insertWorkSpec(work.getWorkSpec());
        new WorkerWrapper.Builder(mContext, mDatabase, work.getId())
                .withListener(mMockListener)
                .build()
                .run();
        verify(mMockListener).onExecuted(work.getId(), WorkerWrapper.EXECUTION_RESULT_SUCCESS);
        assertThat(mWorkSpecDao.getWorkSpecStatus(work.getId()), is(Work.STATUS_SUCCEEDED));
    }

    @Test
    @SmallTest
    public void testRunAttemptCountIncremented_successfulExecution() {
        Work work = new Work.Builder(TestWorker.class).build();
        mWorkSpecDao.insertWorkSpec(work.getWorkSpec());
        new WorkerWrapper.Builder(mContext, mDatabase, work.getId())
                .withListener(mMockListener)
                .build()
                .run();
        WorkSpec latestWorkSpec = mWorkSpecDao.getWorkSpec(work.getId());
        assertThat(latestWorkSpec.getRunAttemptCount(), is(1));
    }

    @Test
    @SmallTest
    public void testRunAttemptCountIncremented_failedExecution() {
        Work work = new Work.Builder(FailureWorker.class).build();
        mWorkSpecDao.insertWorkSpec(work.getWorkSpec());
        new WorkerWrapper.Builder(mContext, mDatabase, work.getId())
                .withListener(mMockListener)
                .build()
                .run();
        WorkSpec latestWorkSpec = mWorkSpecDao.getWorkSpec(work.getId());
        assertThat(latestWorkSpec.getRunAttemptCount(), is(1));
    }

    @Test
    @SmallTest
    public void testRunAttemptCountIncremented_periodic_failedExecution() {
        PeriodicWork work = new PeriodicWork.Builder(
                FailureWorker.class,
                PeriodicWork.MIN_PERIODIC_INTERVAL_MILLIS)
                .build();

        mWorkSpecDao.insertWorkSpec(work.getWorkSpec());
        new WorkerWrapper.Builder(mContext, mDatabase, work.getId())
                .withListener(mMockListener)
                .build()
                .run();
        WorkSpec latestWorkSpec = mWorkSpecDao.getWorkSpec(work.getId());
        assertThat(latestWorkSpec.getRunAttemptCount(), is(1));
    }

    @Test
    @SmallTest
    public void testPermanentErrorWithInvalidWorkSpecId() throws InterruptedException {
        final String invalidWorkSpecId = "INVALID_ID";
        new WorkerWrapper.Builder(mContext, mDatabase, invalidWorkSpecId)
                .withListener(mMockListener)
                .build()
                .run();
        verify(mMockListener)
                .onExecuted(invalidWorkSpecId, WorkerWrapper.EXECUTION_RESULT_PERMANENT_ERROR);
    }

    @Test
    @SmallTest
    public void testNotEnqueued() throws InterruptedException {
        Work work = new Work.Builder(TestWorker.class).build();
        work.getWorkSpec().setStatus(Work.STATUS_RUNNING);
        mWorkSpecDao.insertWorkSpec(work.getWorkSpec());
        new WorkerWrapper.Builder(mContext, mDatabase, work.getId())
                .withListener(mMockListener)
                .build()
                .run();
        verify(mMockListener)
                .onExecuted(work.getId(), WorkerWrapper.EXECUTION_RESULT_RESCHEDULE);
    }

    @Test
    @SmallTest
    public void testPermanentErrorWithInvalidWorkerClass() throws InterruptedException {
        Work work = new Work.Builder(TestWorker.class).build();
        work.getWorkSpec().setWorkerClassName("INVALID_CLASS_NAME");
        mWorkSpecDao.insertWorkSpec(work.getWorkSpec());
        new WorkerWrapper.Builder(mContext, mDatabase, work.getId())
                .withListener(mMockListener)
                .build()
                .run();
        verify(mMockListener)
                .onExecuted(work.getId(), WorkerWrapper.EXECUTION_RESULT_PERMANENT_ERROR);
        assertThat(mWorkSpecDao.getWorkSpecStatus(work.getId()), is(Work.STATUS_FAILED));
    }

    @Test
    @SmallTest
    public void testFailed() throws InterruptedException {
        Work work = new Work.Builder(FailureWorker.class).build();
        mWorkSpecDao.insertWorkSpec(work.getWorkSpec());
        new WorkerWrapper.Builder(mContext, mDatabase, work.getId())
                .withListener(mMockListener)
                .build()
                .run();
        verify(mMockListener).onExecuted(work.getId(), WorkerWrapper.EXECUTION_RESULT_FAILURE);
        assertThat(mWorkSpecDao.getWorkSpecStatus(work.getId()), is(Work.STATUS_FAILED));
    }

    @Test
    @LargeTest
    public void testRunning() throws InterruptedException {
        Work work = new Work.Builder(SleepTestWorker.class).build();
        mWorkSpecDao.insertWorkSpec(work.getWorkSpec());
        WorkerWrapper wrapper = new WorkerWrapper.Builder(mContext, mDatabase, work.getId())
                .withListener(mMockListener)
                .build();
        Executors.newSingleThreadExecutor().submit(wrapper);
        Thread.sleep(2000L); // Async wait duration.
        assertThat(mWorkSpecDao.getWorkSpecStatus(work.getId()), is(Work.STATUS_RUNNING));
        Thread.sleep(SleepTestWorker.SLEEP_DURATION);
        verify(mMockListener).onExecuted(work.getId(), WorkerWrapper.EXECUTION_RESULT_SUCCESS);
    }

    @Test
    @SmallTest
    public void testDependencies() {
        Work prerequisiteWork = new Work.Builder(TestWorker.class).build();
        Work work = new Work.Builder(TestWorker.class).withInitialStatus(STATUS_BLOCKED).build();
        Dependency dependency = new Dependency(work.getId(), prerequisiteWork.getId());

        mDatabase.beginTransaction();
        try {
            mWorkSpecDao.insertWorkSpec(prerequisiteWork.getWorkSpec());
            mWorkSpecDao.insertWorkSpec(work.getWorkSpec());
            mDependencyDao.insertDependency(dependency);
            mDatabase.setTransactionSuccessful();
        } finally {
            mDatabase.endTransaction();
        }

        assertThat(mWorkSpecDao.getWorkSpecStatus(prerequisiteWork.getId()), is(STATUS_ENQUEUED));
        assertThat(mWorkSpecDao.getWorkSpecStatus(work.getId()), is(STATUS_BLOCKED));
        assertThat(mDependencyDao.hasPrerequisites(work.getId()), is(true));

        new WorkerWrapper.Builder(mContext, mDatabase, prerequisiteWork.getId())
                .withListener(mMockListener)
                .withScheduler(mMockScheduler)
                .build()
                .run();

        assertThat(mWorkSpecDao.getWorkSpecStatus(prerequisiteWork.getId()), is(STATUS_SUCCEEDED));
        assertThat(mWorkSpecDao.getWorkSpecStatus(work.getId()), is(STATUS_ENQUEUED));
        assertThat(mDependencyDao.hasPrerequisites(work.getId()), is(false));

        ArgumentCaptor<WorkSpec> captor = ArgumentCaptor.forClass(WorkSpec.class);
        verify(mMockScheduler).schedule(captor.capture());
        assertThat(captor.getValue().getId(), is(work.getId()));
    }

    @Test
    @SmallTest
    public void testPeriodicWork() throws InterruptedException {
        PeriodicWork periodicWork = new PeriodicWork.Builder(
                TestWorker.class,
                PeriodicWork.MIN_PERIODIC_INTERVAL_MILLIS)
                .build();

        final String periodicWorkId = periodicWork.getId();
        mWorkSpecDao.insertWorkSpec(periodicWork.getWorkSpec());
        new WorkerWrapper.Builder(mContext, mDatabase, periodicWorkId)
                .withListener(mMockListener)
                .build()
                .run();

        WorkSpec periodicWorkSpecAfterFirstRun = mWorkSpecDao.getWorkSpec(periodicWorkId);
        verify(mMockListener).onExecuted(periodicWorkId, WorkerWrapper.EXECUTION_RESULT_SUCCESS);
        assertThat(periodicWorkSpecAfterFirstRun.getRunAttemptCount(), is(0));
        assertThat(periodicWorkSpecAfterFirstRun.getStatus(), is(Work.STATUS_ENQUEUED));
    }

    @Test
    @SmallTest
    public void testScheduler() throws InterruptedException {
        Work work = new Work.Builder(TestWorker.class).build();
        mWorkSpecDao.insertWorkSpec(work.getWorkSpec());
        Scheduler mockScheduler = mock(Scheduler.class);

        new WorkerWrapper.Builder(mContext, mDatabase, work.getId())
                .withScheduler(mockScheduler)
                .build()
                .run();

        verify(mockScheduler).schedule();
    }
}
