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

import static android.arch.background.workmanager.BaseWork.STATUS_BLOCKED;
import static android.arch.background.workmanager.BaseWork.STATUS_CANCELLED;
import static android.arch.background.workmanager.BaseWork.STATUS_ENQUEUED;
import static android.arch.background.workmanager.BaseWork.STATUS_FAILED;
import static android.arch.background.workmanager.BaseWork.STATUS_RUNNING;
import static android.arch.background.workmanager.BaseWork.STATUS_SUCCEEDED;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import android.arch.background.workmanager.model.Arguments;
import android.arch.background.workmanager.model.ArrayCreatingInputMerger;
import android.arch.background.workmanager.model.Dependency;
import android.arch.background.workmanager.model.DependencyDao;
import android.arch.background.workmanager.model.WorkInputDao;
import android.arch.background.workmanager.model.WorkSpec;
import android.arch.background.workmanager.model.WorkSpecDao;
import android.arch.background.workmanager.utils.taskexecutor.InstantTaskExecutorRule;
import android.arch.background.workmanager.worker.ChainedArgumentWorker;
import android.arch.background.workmanager.worker.EchoingWorker;
import android.arch.background.workmanager.worker.FailureWorker;
import android.arch.background.workmanager.worker.RetryWorker;
import android.arch.background.workmanager.worker.SleepTestWorker;
import android.arch.background.workmanager.worker.TestWorker;
import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.LargeTest;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;

@RunWith(AndroidJUnit4.class)
public class WorkerWrapperTest extends DatabaseTest {
    private WorkSpecDao mWorkSpecDao;
    private DependencyDao mDependencyDao;
    private WorkInputDao mWorkInputDao;
    private Context mContext;
    private ExecutionListener mMockListener;
    private Scheduler mMockScheduler;

    @Rule
    public InstantTaskExecutorRule mRule = new InstantTaskExecutorRule();

    @Before
    public void setUp() {
        mContext = InstrumentationRegistry.getTargetContext();
        mWorkSpecDao = mDatabase.workSpecDao();
        mDependencyDao = mDatabase.dependencyDao();
        mWorkInputDao = mDatabase.workInputDao();
        mMockListener = mock(ExecutionListener.class);
        mMockScheduler = mock(Scheduler.class);
    }

    @Test
    @SmallTest
    public void testSuccess() throws InterruptedException {
        Work work = new Work.Builder(TestWorker.class).build();
        insertBaseWork(work);
        new WorkerWrapper.Builder(mContext, mDatabase, work.getId())
                .withListener(mMockListener)
                .build()
                .run();
        verify(mMockListener).onExecuted(work.getId(), false);
        assertThat(mWorkSpecDao.getWorkSpecStatus(work.getId()), is(STATUS_SUCCEEDED));
    }

    @Test
    @SmallTest
    public void testRunAttemptCountIncremented_successfulExecution() {
        Work work = new Work.Builder(TestWorker.class).build();
        insertBaseWork(work);
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
        insertBaseWork(work);
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
        verify(mMockListener).onExecuted(invalidWorkSpecId, false);
    }

    @Test
    @SmallTest
    public void testNotEnqueued() throws InterruptedException {
        Work work = new Work.Builder(TestWorker.class).withInitialStatus(STATUS_RUNNING).build();
        insertBaseWork(work);
        new WorkerWrapper.Builder(mContext, mDatabase, work.getId())
                .withListener(mMockListener)
                .build()
                .run();
        verify(mMockListener).onExecuted(work.getId(), true);
    }

    @Test
    @SmallTest
    public void testCancelled() throws InterruptedException {
        Work work = new Work.Builder(TestWorker.class).withInitialStatus(STATUS_CANCELLED).build();
        insertBaseWork(work);
        new WorkerWrapper.Builder(mContext, mDatabase, work.getId())
                .withListener(mMockListener)
                .build()
                .run();
        verify(mMockListener).onExecuted(work.getId(), false);
        assertThat(mWorkSpecDao.getWorkSpecStatus(work.getId()), is(STATUS_CANCELLED));
    }

    @Test
    @SmallTest
    public void testPermanentErrorWithInvalidWorkerClass() throws InterruptedException {
        Work work = new Work.Builder(TestWorker.class).build();
        work.getWorkSpec().setWorkerClassName("INVALID_CLASS_NAME");
        insertBaseWork(work);
        new WorkerWrapper.Builder(mContext, mDatabase, work.getId())
                .withListener(mMockListener)
                .build()
                .run();
        verify(mMockListener).onExecuted(work.getId(), false);
        assertThat(mWorkSpecDao.getWorkSpecStatus(work.getId()), is(STATUS_FAILED));
    }

    @Test
    @SmallTest
    public void testFailed() throws InterruptedException {
        Work work = new Work.Builder(FailureWorker.class).build();
        insertBaseWork(work);
        new WorkerWrapper.Builder(mContext, mDatabase, work.getId())
                .withListener(mMockListener)
                .build()
                .run();
        verify(mMockListener).onExecuted(work.getId(), false);
        assertThat(mWorkSpecDao.getWorkSpecStatus(work.getId()), is(STATUS_FAILED));
    }

    @Test
    @LargeTest
    public void testRunning() throws InterruptedException {
        Work work = new Work.Builder(SleepTestWorker.class).build();
        insertBaseWork(work);
        WorkerWrapper wrapper = new WorkerWrapper.Builder(mContext, mDatabase, work.getId())
                .withListener(mMockListener)
                .build();
        Executors.newSingleThreadExecutor().submit(wrapper);
        Thread.sleep(2000L); // Async wait duration.
        assertThat(mWorkSpecDao.getWorkSpecStatus(work.getId()), is(STATUS_RUNNING));
        Thread.sleep(SleepTestWorker.SLEEP_DURATION);
        verify(mMockListener).onExecuted(work.getId(), false);
    }

    @Test
    @SmallTest
    public void testDependencies() {
        Work prerequisiteWork = new Work.Builder(TestWorker.class).build();
        Work work = new Work.Builder(TestWorker.class).withInitialStatus(STATUS_BLOCKED).build();
        Dependency dependency = new Dependency(work.getId(), prerequisiteWork.getId());

        mDatabase.beginTransaction();
        try {
            insertBaseWork(prerequisiteWork);
            insertBaseWork(work);
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
    public void testDependencies_passesArguments() {
        Work prerequisiteWork = new Work.Builder(ChainedArgumentWorker.class).build();
        Work work = new Work.Builder(TestWorker.class).withInitialStatus(STATUS_BLOCKED).build();
        Dependency dependency = new Dependency(work.getId(), prerequisiteWork.getId());

        mDatabase.beginTransaction();
        try {
            insertBaseWork(prerequisiteWork);
            insertBaseWork(work);
            mDependencyDao.insertDependency(dependency);
            mDatabase.setTransactionSuccessful();
        } finally {
            mDatabase.endTransaction();
        }

        List<Arguments> arguments = mWorkInputDao.getArguments(work.getId());
        assertThat(arguments.size(), is(1));
        assertThat(arguments.get(0).size(), is(0));

        new WorkerWrapper.Builder(mContext, mDatabase, prerequisiteWork.getId()).build().run();

        arguments = mWorkInputDao.getArguments(work.getId());
        assertThat(arguments.size(), is(2));
        assertThat(arguments, containsInAnyOrder(
                ChainedArgumentWorker.getChainedArguments(), Arguments.EMPTY));
    }

    @Test
    @SmallTest
    public void testDependencies_passesMergedArguments() {
        String key = "key";
        String value1 = "value1";
        String value2 = "value2";

        Work prerequisiteWork1 = new Work.Builder(EchoingWorker.class)
                .withArguments(new Arguments.Builder().putString(key, value1).build())
                .build();
        Work prerequisiteWork2 = new Work.Builder(EchoingWorker.class)
                .withArguments(new Arguments.Builder().putString(key, value2).build())
                .build();
        Work work = new Work.Builder(TestWorker.class)
                .withInputMerger(ArrayCreatingInputMerger.class)
                .build();
        Dependency dependency1 = new Dependency(work.getId(), prerequisiteWork1.getId());
        Dependency dependency2 = new Dependency(work.getId(), prerequisiteWork2.getId());

        mDatabase.beginTransaction();
        try {
            insertBaseWork(prerequisiteWork1);
            insertBaseWork(prerequisiteWork2);
            insertBaseWork(work);
            mDependencyDao.insertDependency(dependency1);
            mDependencyDao.insertDependency(dependency2);
            mDatabase.setTransactionSuccessful();
        } finally {
            mDatabase.endTransaction();
        }

        // Run the prerequisites.
        new WorkerWrapper.Builder(mContext, mDatabase, prerequisiteWork1.getId()).build().run();
        new WorkerWrapper.Builder(mContext, mDatabase, prerequisiteWork2.getId()).build().run();

        // Create and run the dependent work.
        WorkerWrapper workerWrapper = new WorkerWrapper.Builder(mContext, mDatabase, work.getId())
                .build();
        workerWrapper.run();

        Arguments arguments = workerWrapper.mWorker.getArguments();
        assertThat(arguments.size(), is(1));
        assertThat(Arrays.asList(arguments.getStringArray(key)),
                containsInAnyOrder(value1, value2));
    }

    @Test
    @SmallTest
    public void testPeriodicWork_success() throws InterruptedException {
        PeriodicWork periodicWork = new PeriodicWork.Builder(
                TestWorker.class,
                PeriodicWork.MIN_PERIODIC_INTERVAL_MILLIS)
                .build();

        final String periodicWorkId = periodicWork.getId();
        insertBaseWork(periodicWork);
        new WorkerWrapper.Builder(mContext, mDatabase, periodicWorkId)
                .withListener(mMockListener)
                .build()
                .run();

        WorkSpec periodicWorkSpecAfterFirstRun = mWorkSpecDao.getWorkSpec(periodicWorkId);
        verify(mMockListener).onExecuted(periodicWorkId, false);
        assertThat(periodicWorkSpecAfterFirstRun.getRunAttemptCount(), is(0));
        assertThat(periodicWorkSpecAfterFirstRun.getStatus(), is(STATUS_ENQUEUED));
    }

    @Test
    @SmallTest
    public void testPeriodicWork_fail() throws InterruptedException {
        PeriodicWork periodicWork = new PeriodicWork.Builder(
                FailureWorker.class,
                PeriodicWork.MIN_PERIODIC_INTERVAL_MILLIS)
                .build();

        final String periodicWorkId = periodicWork.getId();
        insertBaseWork(periodicWork);
        new WorkerWrapper.Builder(mContext, mDatabase, periodicWorkId)
                .withListener(mMockListener)
                .build()
                .run();

        WorkSpec periodicWorkSpecAfterFirstRun = mWorkSpecDao.getWorkSpec(periodicWorkId);
        verify(mMockListener).onExecuted(periodicWorkId, false);
        assertThat(periodicWorkSpecAfterFirstRun.getRunAttemptCount(), is(0));
        assertThat(periodicWorkSpecAfterFirstRun.getStatus(), is(STATUS_ENQUEUED));
    }

    @Test
    @SmallTest
    public void testPeriodicWork_retry() throws InterruptedException {
        PeriodicWork periodicWork = new PeriodicWork.Builder(
                RetryWorker.class,
                PeriodicWork.MIN_PERIODIC_INTERVAL_MILLIS)
                .build();

        final String periodicWorkId = periodicWork.getId();
        insertBaseWork(periodicWork);
        new WorkerWrapper.Builder(mContext, mDatabase, periodicWorkId)
                .withListener(mMockListener)
                .build()
                .run();

        WorkSpec periodicWorkSpecAfterFirstRun = mWorkSpecDao.getWorkSpec(periodicWorkId);
        verify(mMockListener).onExecuted(periodicWorkId, true);
        assertThat(periodicWorkSpecAfterFirstRun.getRunAttemptCount(), is(1));
        assertThat(periodicWorkSpecAfterFirstRun.getStatus(), is(STATUS_ENQUEUED));
    }

    @Test
    @SmallTest
    public void testScheduler() throws InterruptedException {
        Work work = new Work.Builder(TestWorker.class).build();
        insertBaseWork(work);
        Scheduler mockScheduler = mock(Scheduler.class);

        new WorkerWrapper.Builder(mContext, mDatabase, work.getId())
                .withScheduler(mockScheduler)
                .build()
                .run();

        verify(mockScheduler).schedule();
    }
}
