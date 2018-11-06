/*
 * Copyright 2017 The Android Open Source Project
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

import static androidx.work.WorkInfo.State.BLOCKED;
import static androidx.work.WorkInfo.State.CANCELLED;
import static androidx.work.WorkInfo.State.ENQUEUED;
import static androidx.work.WorkInfo.State.FAILED;
import static androidx.work.WorkInfo.State.RUNNING;
import static androidx.work.WorkInfo.State.SUCCEEDED;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.greaterThan;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.test.InstrumentationRegistry;
import androidx.test.filters.LargeTest;
import androidx.test.filters.SmallTest;
import androidx.test.runner.AndroidJUnit4;
import androidx.work.ArrayCreatingInputMerger;
import androidx.work.Configuration;
import androidx.work.Data;
import androidx.work.DatabaseTest;
import androidx.work.ListenableWorker;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkerParameters;
import androidx.work.impl.model.Dependency;
import androidx.work.impl.model.DependencyDao;
import androidx.work.impl.model.WorkSpec;
import androidx.work.impl.model.WorkSpecDao;
import androidx.work.impl.utils.SynchronousExecutor;
import androidx.work.impl.utils.taskexecutor.InstantWorkTaskExecutor;
import androidx.work.impl.utils.taskexecutor.TaskExecutor;
import androidx.work.worker.ChainedArgumentWorker;
import androidx.work.worker.EchoingWorker;
import androidx.work.worker.ExceptionWorker;
import androidx.work.worker.FailureWorker;
import androidx.work.worker.InfiniteTestWorker;
import androidx.work.worker.InterruptionAwareWorker;
import androidx.work.worker.LatchWorker;
import androidx.work.worker.RetryWorker;
import androidx.work.worker.SleepTestWorker;
import androidx.work.worker.TestWorker;
import androidx.work.worker.UsedWorker;

import com.google.common.util.concurrent.ListenableFuture;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@RunWith(AndroidJUnit4.class)
public class WorkerWrapperTest extends DatabaseTest {

    private Configuration mConfiguration;
    private TaskExecutor mWorkTaskExecutor;
    private WorkSpecDao mWorkSpecDao;
    private DependencyDao mDependencyDao;
    private Context mContext;
    private Scheduler mMockScheduler;
    private Executor mSynchronousExecutor = new SynchronousExecutor();

    @Before
    public void setUp() {
        mContext = InstrumentationRegistry.getTargetContext();
        mConfiguration = new Configuration.Builder()
                .setExecutor(new SynchronousExecutor())
                .setMinimumLoggingLevel(Log.VERBOSE)
                .build();
        mWorkTaskExecutor = new InstantWorkTaskExecutor();
        mWorkSpecDao = spy(mDatabase.workSpecDao());
        mDependencyDao = mDatabase.dependencyDao();
        mMockScheduler = mock(Scheduler.class);
    }

    @Test
    @SmallTest
    public void testSuccess() {
        OneTimeWorkRequest work = new OneTimeWorkRequest.Builder(TestWorker.class).build();
        insertWork(work);
        WorkerWrapper workerWrapper = createBuilder(work.getStringId()).build();
        FutureListener listener = createAndAddFutureListener(workerWrapper);
        workerWrapper.run();
        assertThat(listener.mResult, is(false));
        assertThat(mWorkSpecDao.getState(work.getStringId()), is(SUCCEEDED));
    }

    @Test
    @SmallTest
    public void testRunAttemptCountIncremented_successfulExecution() {
        OneTimeWorkRequest work = new OneTimeWorkRequest.Builder(TestWorker.class).build();
        insertWork(work);
        createBuilder(work.getStringId())
                .withSchedulers(Collections.singletonList(mMockScheduler))
                .build()
                .run();
        WorkSpec latestWorkSpec = mWorkSpecDao.getWorkSpec(work.getStringId());
        assertThat(latestWorkSpec.runAttemptCount, is(1));
    }

    @Test
    @SmallTest
    public void testRunAttemptCountIncremented_failedExecution() {
        OneTimeWorkRequest work = new OneTimeWorkRequest.Builder(FailureWorker.class).build();
        insertWork(work);
        createBuilder(work.getStringId())
                .build()
                .run();
        WorkSpec latestWorkSpec = mWorkSpecDao.getWorkSpec(work.getStringId());
        assertThat(latestWorkSpec.runAttemptCount, is(1));
    }

    @Test
    @SmallTest
    public void testPermanentErrorWithInvalidWorkSpecId() {
        final String invalidWorkSpecId = "INVALID_ID";
        WorkerWrapper workerWrapper = createBuilder(invalidWorkSpecId).build();
        FutureListener listener = createAndAddFutureListener(workerWrapper);
        workerWrapper.run();
        assertThat(listener.mResult, is(false));
    }

    @Test
    @SmallTest
    public void testInvalidWorkerClassName() {
        OneTimeWorkRequest work = new OneTimeWorkRequest.Builder(TestWorker.class).build();
        work.getWorkSpec().workerClassName = "dummy";
        insertWork(work);
        WorkerWrapper workerWrapper = createBuilder(work.getStringId())
                .withSchedulers(Collections.singletonList(mMockScheduler))
                .build();
        FutureListener listener = createAndAddFutureListener(workerWrapper);
        workerWrapper.run();
        assertThat(listener.mResult, is(false));
        verify(mMockScheduler, never()).schedule(any(WorkSpec[].class));
        assertThat(mWorkSpecDao.getState(work.getStringId()), is(FAILED));
    }

    @Test
    @SmallTest
    public void testUsedWorker_failsExecution() {
        OneTimeWorkRequest work = new OneTimeWorkRequest.Builder(TestWorker.class).build();
        insertWork(work);

        UsedWorker usedWorker = (UsedWorker) mConfiguration.getWorkerFactory()
                .createWorkerWithDefaultFallback(
                        mContext.getApplicationContext(),
                        UsedWorker.class.getName(),
                        new WorkerParameters(
                                work.getId(),
                                Data.EMPTY,
                                work.getTags(),
                                new WorkerParameters.RuntimeExtras(),
                                1,
                                mSynchronousExecutor,
                                mWorkTaskExecutor,
                                mConfiguration.getWorkerFactory()));

        WorkerWrapper workerWrapper = createBuilder(work.getStringId())
                .withWorker(usedWorker)
                .build();
        workerWrapper.run();
        assertThat(mWorkSpecDao.getState(work.getStringId()), is(FAILED));
    }

    @Test
    @SmallTest
    public void testNotEnqueued() {
        OneTimeWorkRequest work = new OneTimeWorkRequest.Builder(TestWorker.class)
                .setInitialState(RUNNING)
                .build();
        insertWork(work);
        WorkerWrapper workerWrapper = createBuilder(work.getStringId()).build();
        FutureListener listener = createAndAddFutureListener(workerWrapper);
        workerWrapper.run();
        assertThat(listener.mResult, is(true));
    }

    @Test
    @SmallTest
    public void testCancelled() {
        OneTimeWorkRequest work = new OneTimeWorkRequest.Builder(TestWorker.class)
                .setInitialState(CANCELLED)
                .build();
        insertWork(work);
        WorkerWrapper workerWrapper = createBuilder(work.getStringId()).build();
        FutureListener listener = createAndAddFutureListener(workerWrapper);
        workerWrapper.run();
        assertThat(listener.mResult, is(false));
        assertThat(mWorkSpecDao.getState(work.getStringId()), is(CANCELLED));
    }

    @Test
    @SmallTest
    public void testPermanentErrorWithInvalidWorkerClass() {
        OneTimeWorkRequest work = new OneTimeWorkRequest.Builder(TestWorker.class).build();
        getWorkSpec(work).workerClassName = "INVALID_CLASS_NAME";
        insertWork(work);
        WorkerWrapper workerWrapper = createBuilder(work.getStringId()).build();
        FutureListener listener = createAndAddFutureListener(workerWrapper);
        workerWrapper.run();
        assertThat(listener.mResult, is(false));
        assertThat(mWorkSpecDao.getState(work.getStringId()), is(FAILED));
    }

    @Test
    @SmallTest
    public void testPermanentErrorWithInvalidInputMergerClass() {
        OneTimeWorkRequest work = new OneTimeWorkRequest.Builder(TestWorker.class).build();
        getWorkSpec(work).inputMergerClassName = "INVALID_CLASS_NAME";
        insertWork(work);
        WorkerWrapper workerWrapper = createBuilder(work.getStringId())
                .withSchedulers(Collections.singletonList(mMockScheduler))
                .build();
        FutureListener listener = createAndAddFutureListener(workerWrapper);
        workerWrapper.run();
        assertThat(listener.mResult, is(false));
        assertThat(mWorkSpecDao.getState(work.getStringId()), is(FAILED));
    }

    @Test
    @SmallTest
    public void testFailed() {
        OneTimeWorkRequest work = new OneTimeWorkRequest.Builder(FailureWorker.class).build();
        insertWork(work);
        WorkerWrapper workerWrapper = createBuilder(work.getStringId()).build();
        FutureListener listener = createAndAddFutureListener(workerWrapper);
        workerWrapper.run();
        assertThat(listener.mResult, is(false));
        assertThat(mWorkSpecDao.getState(work.getStringId()), is(FAILED));
    }

    @Test
    @LargeTest
    public void testRunning() throws InterruptedException {
        OneTimeWorkRequest work = new OneTimeWorkRequest.Builder(SleepTestWorker.class).build();
        insertWork(work);
        WorkerWrapper wrapper = createBuilder(work.getStringId()).build();
        FutureListener listener = createAndAddFutureListener(wrapper);
        Executors.newSingleThreadExecutor().submit(wrapper);
        Thread.sleep(2000L); // Async wait duration.
        assertThat(mWorkSpecDao.getState(work.getStringId()), is(RUNNING));
        Thread.sleep(SleepTestWorker.SLEEP_DURATION);
        assertThat(listener.mResult, is(false));
    }

    @Test
    @SmallTest
    public void testRunning_onlyWhenEnqueued() {
        OneTimeWorkRequest work = new OneTimeWorkRequest.Builder(TestWorker.class)
                .setInitialState(RUNNING)
                .build();
        insertWork(work);
        WorkerWrapper workerWrapper = createBuilder(work.getStringId()).build();
        FutureListener listener = createAndAddFutureListener(workerWrapper);
        workerWrapper.run();
        assertThat(listener.mResult, is(true));
    }

    @Test
    @SmallTest
    public void testDependencies() {
        OneTimeWorkRequest prerequisiteWork =
                new OneTimeWorkRequest.Builder(TestWorker.class).build();
        OneTimeWorkRequest work = new OneTimeWorkRequest.Builder(TestWorker.class)
                .setInitialState(BLOCKED).build();
        Dependency dependency = new Dependency(work.getStringId(), prerequisiteWork.getStringId());

        mDatabase.beginTransaction();
        try {
            insertWork(prerequisiteWork);
            insertWork(work);
            mDependencyDao.insertDependency(dependency);
            mDatabase.setTransactionSuccessful();
        } finally {
            mDatabase.endTransaction();
        }

        assertThat(mWorkSpecDao.getState(prerequisiteWork.getStringId()), is(ENQUEUED));
        assertThat(mWorkSpecDao.getState(work.getStringId()), is(BLOCKED));
        assertThat(mDependencyDao.hasCompletedAllPrerequisites(work.getStringId()), is(false));

        createBuilder(prerequisiteWork.getStringId())
                .withSchedulers(Collections.singletonList(mMockScheduler))
                .build()
                .run();

        assertThat(mWorkSpecDao.getState(prerequisiteWork.getStringId()), is(SUCCEEDED));
        assertThat(mWorkSpecDao.getState(work.getStringId()), is(ENQUEUED));
        assertThat(mDependencyDao.hasCompletedAllPrerequisites(work.getStringId()), is(true));

        ArgumentCaptor<WorkSpec> captor = ArgumentCaptor.forClass(WorkSpec.class);
        verify(mMockScheduler).schedule(captor.capture());
        assertThat(captor.getValue().id, is(work.getStringId()));
    }

    @Test
    @SmallTest
    public void testDependencies_passesOutputs() {
        OneTimeWorkRequest prerequisiteWork =
                new OneTimeWorkRequest.Builder(ChainedArgumentWorker.class).build();
        OneTimeWorkRequest work = new OneTimeWorkRequest.Builder(TestWorker.class)
                .setInitialState(BLOCKED)
                .build();
        Dependency dependency = new Dependency(work.getStringId(), prerequisiteWork.getStringId());

        mDatabase.beginTransaction();
        try {
            insertWork(prerequisiteWork);
            insertWork(work);
            mDependencyDao.insertDependency(dependency);
            mDatabase.setTransactionSuccessful();
        } finally {
            mDatabase.endTransaction();
        }

        createBuilder(prerequisiteWork.getStringId())
                .withSchedulers(Collections.singletonList(mMockScheduler))
                .build().run();

        List<Data> arguments = mWorkSpecDao.getInputsFromPrerequisites(work.getStringId());
        assertThat(arguments.size(), is(1));
        assertThat(arguments, contains(ChainedArgumentWorker.getChainedArguments()));
    }

    @Test
    @SmallTest
    public void testDependencies_passesMergedOutputs() {
        String key = "key";
        String value1 = "value1";
        String value2 = "value2";

        OneTimeWorkRequest prerequisiteWork1 = new OneTimeWorkRequest.Builder(EchoingWorker.class)
                .setInputData(new Data.Builder().putString(key, value1).build())
                .build();
        OneTimeWorkRequest prerequisiteWork2 = new OneTimeWorkRequest.Builder(EchoingWorker.class)
                .setInputData(new Data.Builder().putString(key, value2).build())
                .build();
        OneTimeWorkRequest work = new OneTimeWorkRequest.Builder(TestWorker.class)
                .setInputMerger(ArrayCreatingInputMerger.class)
                .build();
        Dependency dependency1 =
                new Dependency(work.getStringId(), prerequisiteWork1.getStringId());
        Dependency dependency2 =
                new Dependency(work.getStringId(), prerequisiteWork2.getStringId());

        mDatabase.beginTransaction();
        try {
            insertWork(prerequisiteWork1);
            insertWork(prerequisiteWork2);
            insertWork(work);
            mDependencyDao.insertDependency(dependency1);
            mDependencyDao.insertDependency(dependency2);
            mDatabase.setTransactionSuccessful();
        } finally {
            mDatabase.endTransaction();
        }

        // Run the prerequisites.
        createBuilder(prerequisiteWork1.getStringId())
                .withSchedulers(Collections.singletonList(mMockScheduler))
                .build().run();

        createBuilder(prerequisiteWork2.getStringId())
                .withSchedulers(Collections.singletonList(mMockScheduler))
                .build().run();

        // Create and run the dependent work.
        WorkerWrapper workerWrapper =
                createBuilder(work.getStringId())
                        .withSchedulers(Collections.singletonList(mMockScheduler))
                        .build();
        workerWrapper.run();

        Data input = workerWrapper.mWorker.getInputData();
        assertThat(input.size(), is(1));
        assertThat(Arrays.asList(input.getStringArray(key)),
                containsInAnyOrder(value1, value2));
    }

    @Test
    @SmallTest
    public void testDependencies_setsPeriodStartTimesForUnblockedWork() {
        OneTimeWorkRequest prerequisiteWork =
                new OneTimeWorkRequest.Builder(TestWorker.class).build();
        OneTimeWorkRequest work = new OneTimeWorkRequest.Builder(TestWorker.class)
                .setInitialState(BLOCKED)
                .build();
        Dependency dependency = new Dependency(work.getStringId(), prerequisiteWork.getStringId());

        mDatabase.beginTransaction();
        try {
            insertWork(prerequisiteWork);
            insertWork(work);
            mDependencyDao.insertDependency(dependency);
            mDatabase.setTransactionSuccessful();
        } finally {
            mDatabase.endTransaction();
        }

        long beforeUnblockedTime = System.currentTimeMillis();

        createBuilder(prerequisiteWork.getStringId())
                .withSchedulers(Collections.singletonList(mMockScheduler))
                .build()
                .run();

        WorkSpec workSpec = mWorkSpecDao.getWorkSpec(work.getStringId());
        assertThat(workSpec.periodStartTime, is(greaterThan(beforeUnblockedTime)));
    }

    @Test
    @SmallTest
    public void testDependencies_failsUncancelledDependentsOnFailure() {
        OneTimeWorkRequest prerequisiteWork =
                new OneTimeWorkRequest.Builder(FailureWorker.class).build();
        OneTimeWorkRequest work = new OneTimeWorkRequest.Builder(TestWorker.class)
                .setInitialState(BLOCKED)
                .build();
        OneTimeWorkRequest cancelledWork = new OneTimeWorkRequest.Builder(TestWorker.class)
                .setInitialState(CANCELLED)
                .build();
        Dependency dependency1 = new Dependency(work.getStringId(), prerequisiteWork.getStringId());
        Dependency dependency2 =
                new Dependency(cancelledWork.getStringId(), prerequisiteWork.getStringId());

        mDatabase.beginTransaction();
        try {
            insertWork(prerequisiteWork);
            insertWork(work);
            insertWork(cancelledWork);
            mDependencyDao.insertDependency(dependency1);
            mDependencyDao.insertDependency(dependency2);
            mDatabase.setTransactionSuccessful();
        } finally {
            mDatabase.endTransaction();
        }

        createBuilder(prerequisiteWork.getStringId())
                .build()
                .run();

        assertThat(mWorkSpecDao.getState(prerequisiteWork.getStringId()), is(FAILED));
        assertThat(mWorkSpecDao.getState(work.getStringId()), is(FAILED));
        assertThat(mWorkSpecDao.getState(cancelledWork.getStringId()), is(CANCELLED));
    }

    @Test
    @SmallTest
    public void testRun_periodicWork_success_updatesPeriodStartTime() {
        long intervalDuration = PeriodicWorkRequest.MIN_PERIODIC_INTERVAL_MILLIS;
        long periodStartTime = System.currentTimeMillis();
        long expectedNextPeriodStartTime = periodStartTime + intervalDuration;

        PeriodicWorkRequest periodicWork = new PeriodicWorkRequest.Builder(
                TestWorker.class, intervalDuration, TimeUnit.MILLISECONDS).build();

        getWorkSpec(periodicWork).periodStartTime = periodStartTime;

        insertWork(periodicWork);

        createBuilder(periodicWork.getStringId())
                .build()
                .run();

        WorkSpec updatedWorkSpec = mWorkSpecDao.getWorkSpec(periodicWork.getStringId());
        assertThat(updatedWorkSpec.periodStartTime, is(expectedNextPeriodStartTime));
    }

    @Test
    @SmallTest
    public void testRun_periodicWork_failure_updatesPeriodStartTime() {
        long intervalDuration = PeriodicWorkRequest.MIN_PERIODIC_INTERVAL_MILLIS;
        long periodStartTime = System.currentTimeMillis();
        long expectedNextPeriodStartTime = periodStartTime + intervalDuration;

        PeriodicWorkRequest periodicWork = new PeriodicWorkRequest.Builder(
                FailureWorker.class, intervalDuration, TimeUnit.MILLISECONDS).build();

        getWorkSpec(periodicWork).periodStartTime = periodStartTime;

        insertWork(periodicWork);

        createBuilder(periodicWork.getStringId())
                .build()
                .run();

        WorkSpec updatedWorkSpec = mWorkSpecDao.getWorkSpec(periodicWork.getStringId());
        assertThat(updatedWorkSpec.periodStartTime, is(expectedNextPeriodStartTime));
    }

    @Test
    @SmallTest
    public void testPeriodicWork_success() {
        PeriodicWorkRequest periodicWork = new PeriodicWorkRequest.Builder(
                TestWorker.class,
                PeriodicWorkRequest.MIN_PERIODIC_INTERVAL_MILLIS,
                TimeUnit.MILLISECONDS)
                .build();

        final String periodicWorkId = periodicWork.getStringId();
        insertWork(periodicWork);
        WorkerWrapper workerWrapper = createBuilder(periodicWorkId)
                .withSchedulers(Collections.singletonList(mMockScheduler))
                .build();
        FutureListener listener = createAndAddFutureListener(workerWrapper);
        workerWrapper.run();

        WorkSpec periodicWorkSpecAfterFirstRun = mWorkSpecDao.getWorkSpec(periodicWorkId);
        assertThat(listener.mResult, is(false));
        assertThat(periodicWorkSpecAfterFirstRun.runAttemptCount, is(0));
        assertThat(periodicWorkSpecAfterFirstRun.state, is(ENQUEUED));
        // SystemAlarmScheduler needs to reschedule the same worker.
        if (Build.VERSION.SDK_INT <= WorkManagerImpl.MAX_PRE_JOB_SCHEDULER_API_LEVEL) {
            ArgumentCaptor<WorkSpec> captor = ArgumentCaptor.forClass(WorkSpec.class);
            verify(mMockScheduler, atLeast(1))
                    .schedule(captor.capture());

            WorkSpec workSpec = captor.getValue();
            assertThat(workSpec.id, is(periodicWorkId));
        }
    }

    @Test
    @SmallTest
    public void testPeriodicWork_fail() {
        PeriodicWorkRequest periodicWork = new PeriodicWorkRequest.Builder(
                FailureWorker.class,
                PeriodicWorkRequest.MIN_PERIODIC_INTERVAL_MILLIS,
                TimeUnit.MILLISECONDS)
                .build();

        final String periodicWorkId = periodicWork.getStringId();
        insertWork(periodicWork);
        WorkerWrapper workerWrapper = createBuilder(periodicWorkId).build();
        FutureListener listener = createAndAddFutureListener(workerWrapper);
        workerWrapper.run();

        WorkSpec periodicWorkSpecAfterFirstRun = mWorkSpecDao.getWorkSpec(periodicWorkId);
        assertThat(listener.mResult, is(false));
        assertThat(periodicWorkSpecAfterFirstRun.runAttemptCount, is(0));
        assertThat(periodicWorkSpecAfterFirstRun.state, is(ENQUEUED));
    }

    @Test
    @SmallTest
    public void testPeriodicWork_retry() {
        PeriodicWorkRequest periodicWork = new PeriodicWorkRequest.Builder(
                RetryWorker.class,
                PeriodicWorkRequest.MIN_PERIODIC_INTERVAL_MILLIS,
                TimeUnit.MILLISECONDS)
                .build();

        final String periodicWorkId = periodicWork.getStringId();
        insertWork(periodicWork);
        WorkerWrapper workerWrapper = createBuilder(periodicWorkId).build();
        FutureListener listener = createAndAddFutureListener(workerWrapper);
        workerWrapper.run();

        WorkSpec periodicWorkSpecAfterFirstRun = mWorkSpecDao.getWorkSpec(periodicWorkId);
        assertThat(listener.mResult, is(true));
        assertThat(periodicWorkSpecAfterFirstRun.runAttemptCount, is(1));
        assertThat(periodicWorkSpecAfterFirstRun.state, is(ENQUEUED));
    }

    @Test
    @SmallTest
    public void testScheduler() {
        OneTimeWorkRequest prerequisiteWork =
                new OneTimeWorkRequest.Builder(TestWorker.class).build();
        OneTimeWorkRequest work = new OneTimeWorkRequest.Builder(TestWorker.class)
                .setInitialState(BLOCKED).build();
        Dependency dependency = new Dependency(work.getStringId(), prerequisiteWork.getStringId());

        mDatabase.beginTransaction();
        try {
            insertWork(prerequisiteWork);
            insertWork(work);
            mDependencyDao.insertDependency(dependency);
            mDatabase.setTransactionSuccessful();
        } finally {
            mDatabase.endTransaction();
        }

        createBuilder(prerequisiteWork.getStringId())
                .withSchedulers(Collections.singletonList(mMockScheduler))
                .build()
                .run();

        ArgumentCaptor<WorkSpec> captor = ArgumentCaptor.forClass(WorkSpec.class);
        verify(mMockScheduler).schedule(captor.capture());
        assertThat(captor.getValue().id, is(work.getStringId()));
    }

    @Test
    @SmallTest
    public void testFromWorkSpec_hasAppContext() {
        OneTimeWorkRequest work = new OneTimeWorkRequest.Builder(TestWorker.class).build();
        ListenableWorker worker = mConfiguration.getWorkerFactory().createWorkerWithDefaultFallback(
                mContext.getApplicationContext(),
                TestWorker.class.getName(),
                new WorkerParameters(
                        work.getId(),
                        Data.EMPTY,
                        work.getTags(),
                        new WorkerParameters.RuntimeExtras(),
                        1,
                        mSynchronousExecutor,
                        mWorkTaskExecutor,
                        mConfiguration.getWorkerFactory()));

        assertThat(worker, is(notNullValue()));
        assertThat(worker.getApplicationContext(), is(equalTo(mContext.getApplicationContext())));
    }

    @Test
    @SmallTest
    public void testFromWorkSpec_hasCorrectArguments() {
        String key = "KEY";
        String expectedValue = "VALUE";
        Data input = new Data.Builder().putString(key, expectedValue).build();

        OneTimeWorkRequest work =
                new OneTimeWorkRequest.Builder(TestWorker.class).setInputData(input).build();
        ListenableWorker worker = mConfiguration.getWorkerFactory().createWorkerWithDefaultFallback(
                mContext.getApplicationContext(),
                TestWorker.class.getName(),
                new WorkerParameters(
                        work.getId(),
                        input,
                        work.getTags(),
                        new WorkerParameters.RuntimeExtras(),
                        1,
                        mSynchronousExecutor,
                        mWorkTaskExecutor,
                        mConfiguration.getWorkerFactory()));

        assertThat(worker, is(notNullValue()));
        assertThat(worker.getInputData().getString(key), is(expectedValue));

        work = new OneTimeWorkRequest.Builder(TestWorker.class).build();
        worker = mConfiguration.getWorkerFactory().createWorkerWithDefaultFallback(
                mContext.getApplicationContext(),
                TestWorker.class.getName(),
                new WorkerParameters(
                        work.getId(),
                        Data.EMPTY,
                        work.getTags(),
                        new WorkerParameters.RuntimeExtras(),
                        1,
                        mSynchronousExecutor,
                        mWorkTaskExecutor,
                        mConfiguration.getWorkerFactory()));

        assertThat(worker, is(notNullValue()));
        assertThat(worker.getInputData().size(), is(0));
    }

    @Test
    @SmallTest
    public void testFromWorkSpec_hasCorrectTags() {
        OneTimeWorkRequest work =
                new OneTimeWorkRequest.Builder(TestWorker.class)
                        .addTag("one")
                        .addTag("two")
                        .addTag("three")
                        .build();
        ListenableWorker worker = mConfiguration.getWorkerFactory().createWorkerWithDefaultFallback(
                mContext.getApplicationContext(),
                TestWorker.class.getName(),
                new WorkerParameters(
                        work.getId(),
                        Data.EMPTY,
                        Arrays.asList("one", "two", "three"),
                        new WorkerParameters.RuntimeExtras(),
                        1,
                        mSynchronousExecutor,
                        mWorkTaskExecutor,
                        mConfiguration.getWorkerFactory()));

        assertThat(worker, is(notNullValue()));
        assertThat(worker.getTags(), containsInAnyOrder("one", "two", "three"));
    }

    @Test
    @SmallTest
    public void testFromWorkSpec_hasCorrectRuntimeExtras() {
        OneTimeWorkRequest work =
                new OneTimeWorkRequest.Builder(TestWorker.class).build();
        WorkerParameters.RuntimeExtras runtimeExtras = new WorkerParameters.RuntimeExtras();
        runtimeExtras.triggeredContentAuthorities = Arrays.asList("tca1", "tca2", "tca3");
        runtimeExtras.triggeredContentUris = Arrays.asList(Uri.parse("tcu1"), Uri.parse("tcu2"));

        ListenableWorker worker = mConfiguration.getWorkerFactory().createWorkerWithDefaultFallback(
                mContext.getApplicationContext(),
                TestWorker.class.getName(),
                new WorkerParameters(
                        work.getId(),
                        Data.EMPTY,
                        work.getTags(),
                        runtimeExtras,
                        1,
                        mSynchronousExecutor,
                        mWorkTaskExecutor,
                        mConfiguration.getWorkerFactory()));

        assertThat(worker, is(notNullValue()));
        assertThat(worker.getTriggeredContentAuthorities(),
                containsInAnyOrder(runtimeExtras.triggeredContentAuthorities.toArray()));
        assertThat(worker.getTriggeredContentUris(),
                containsInAnyOrder(runtimeExtras.triggeredContentUris.toArray()));
    }

    @Test
    @SmallTest
    public void testSuccess_withPendingScheduledWork() {
        OneTimeWorkRequest work = new OneTimeWorkRequest.Builder(TestWorker.class).build();
        insertWork(work);

        OneTimeWorkRequest unscheduled = new OneTimeWorkRequest.Builder(TestWorker.class).build();
        insertWork(unscheduled);

        WorkerWrapper workerWrapper = createBuilder(work.getStringId())
                .withSchedulers(Collections.singletonList(mMockScheduler))
                .build();
        FutureListener listener = createAndAddFutureListener(workerWrapper);
        workerWrapper.run();

        verify(mMockScheduler, times(1)).schedule(unscheduled.getWorkSpec());
        assertThat(listener.mResult, is(false));
        assertThat(mWorkSpecDao.getState(work.getStringId()), is(SUCCEEDED));
    }

    @Test
    @SmallTest
    public void testFailure_withPendingScheduledWork() {
        OneTimeWorkRequest work = new OneTimeWorkRequest.Builder(FailureWorker.class).build();
        insertWork(work);

        OneTimeWorkRequest unscheduled = new OneTimeWorkRequest.Builder(TestWorker.class).build();
        insertWork(unscheduled);

        WorkerWrapper workerWrapper = createBuilder(work.getStringId())
                .withSchedulers(Collections.singletonList(mMockScheduler))
                .build();
        FutureListener listener = createAndAddFutureListener(workerWrapper);
        workerWrapper.run();

        verify(mMockScheduler, times(1)).schedule(unscheduled.getWorkSpec());
        assertThat(listener.mResult, is(false));
        assertThat(mWorkSpecDao.getState(work.getStringId()), is(FAILED));
    }

    @Test
    @LargeTest
    public void testInterruption() throws InterruptedException {
        OneTimeWorkRequest work = new OneTimeWorkRequest.Builder(TestWorker.class).build();
        insertWork(work);

        WorkerWrapper workerWrapper =
                createBuilder(work.getStringId())
                        .withSchedulers(Collections.singletonList(mMockScheduler))
                        .build();
        FutureListener listener = createAndAddFutureListener(workerWrapper);
        Executors.newSingleThreadExecutor().submit(workerWrapper);
        workerWrapper.interrupt(false);
        Thread.sleep(6000L);
        assertThat(listener.mResult, is(true));
    }

    @Test
    @LargeTest
    public void testPruneWhileRunning_callsSchedulerCancel() throws InterruptedException {
        OneTimeWorkRequest work = new OneTimeWorkRequest.Builder(LatchWorker.class).build();
        insertWork(work);

        LatchWorker latchWorker =
                (LatchWorker) mConfiguration.getWorkerFactory().createWorkerWithDefaultFallback(
                        mContext.getApplicationContext(),
                        LatchWorker.class.getName(),
                        new WorkerParameters(
                                work.getId(),
                                Data.EMPTY,
                                work.getTags(),
                                new WorkerParameters.RuntimeExtras(),
                                1,
                                Executors.newSingleThreadExecutor(),
                                mWorkTaskExecutor,
                                mConfiguration.getWorkerFactory()));

        WorkerWrapper workerWrapper =
                createBuilder(work.getStringId())
                        .withSchedulers(Collections.singletonList(mMockScheduler))
                        .withWorker(latchWorker)
                        .build();
        FutureListener listener = createAndAddFutureListener(workerWrapper);
        Executors.newSingleThreadExecutor().submit(workerWrapper);

        Thread.sleep(1000L);

        mDatabase.workSpecDao().delete(work.getStringId());
        assertThat(latchWorker.mLatch.getCount(), is(greaterThan(0L)));

        latchWorker.mLatch.countDown();

        Thread.sleep(1000L);

        assertThat(listener.mResult, is(notNullValue()));
        verify(mMockScheduler, times(1)).cancel(work.getStringId());
    }

    @Test
    @SmallTest
    public void testInterruptionWithoutCancellation_isMarkedOnRunningWorker() {
        OneTimeWorkRequest work =
                new OneTimeWorkRequest.Builder(InterruptionAwareWorker.class).build();
        insertWork(work);

        ListenableWorker worker = mConfiguration.getWorkerFactory().createWorkerWithDefaultFallback(
                mContext.getApplicationContext(),
                InterruptionAwareWorker.class.getName(),
                new WorkerParameters(
                        work.getId(),
                        Data.EMPTY,
                        work.getTags(),
                        new WorkerParameters.RuntimeExtras(),
                        1,
                        mSynchronousExecutor,
                        mWorkTaskExecutor,
                        mConfiguration.getWorkerFactory()));
        assertThat(worker, is(notNullValue()));
        assertThat(worker.isStopped(), is(false));

        WorkerWrapper workerWrapper =
                createBuilder(work.getStringId())
                        .withSchedulers(Collections.singletonList(mMockScheduler))
                        .withWorker(worker)
                        .build();
        Executors.newSingleThreadExecutor().submit(workerWrapper);
        workerWrapper.interrupt(false);
        assertThat(worker.isStopped(), is(true));
    }

    @Test
    @SmallTest
    public void testInterruptionWithCancellation_isMarkedOnRunningWorker() {
        OneTimeWorkRequest work =
                new OneTimeWorkRequest.Builder(InterruptionAwareWorker.class).build();
        insertWork(work);

        ListenableWorker worker = mConfiguration.getWorkerFactory().createWorkerWithDefaultFallback(
                mContext.getApplicationContext(),
                InterruptionAwareWorker.class.getName(),
                new WorkerParameters(
                        work.getId(),
                        Data.EMPTY,
                        Collections.<String>emptyList(),
                        new WorkerParameters.RuntimeExtras(),
                        1,
                        mSynchronousExecutor,
                        mWorkTaskExecutor,
                        mConfiguration.getWorkerFactory()));
        assertThat(worker, is(notNullValue()));
        assertThat(worker.isStopped(), is(false));

        WorkerWrapper workerWrapper =
                createBuilder(work.getStringId())
                        .withSchedulers(Collections.singletonList(mMockScheduler))
                        .withWorker(worker)
                        .build();
        Executors.newSingleThreadExecutor().submit(workerWrapper);
        workerWrapper.interrupt(true);
        assertThat(worker.isStopped(), is(true));
    }

    @Test
    @SmallTest
    public void testException_isTreatedAsFailure() {
        OneTimeWorkRequest work = new OneTimeWorkRequest.Builder(ExceptionWorker.class).build();
        insertWork(work);

        createBuilder(work.getStringId())
                .withSchedulers(Collections.singletonList(mMockScheduler))
                .build()
                .run();

        assertThat(mWorkSpecDao.getState(work.getStringId()), is(FAILED));
    }

    @Test
    @LargeTest
    public void testWorkerWrapper_handlesWorkSpecDeletion() throws InterruptedException {
        OneTimeWorkRequest work = new OneTimeWorkRequest.Builder(SleepTestWorker.class).build();
        insertWork(work);

        WorkerWrapper workerWrapper =
                createBuilder(work.getStringId())
                        .withSchedulers(Collections.singletonList(mMockScheduler))
                        .build();
        FutureListener listener = createAndAddFutureListener(workerWrapper);
        Executors.newSingleThreadExecutor().submit(workerWrapper);
        mWorkSpecDao.delete(work.getStringId());
        Thread.sleep(6000L);
        assertThat(listener.mResult, is(false));
    }

    @Test
    @LargeTest
    public void testWorker_getsRunAttemptCount() throws InterruptedException {
        OneTimeWorkRequest work = new OneTimeWorkRequest.Builder(InfiniteTestWorker.class)
                .setInitialRunAttemptCount(10)
                .build();
        insertWork(work);

        WorkerWrapper workerWrapper =
                createBuilder(work.getStringId())
                        .withSchedulers(Collections.singletonList(mMockScheduler))
                        .build();

        Executors.newSingleThreadExecutor().submit(workerWrapper);
        Thread.sleep(1000L);
        assertThat(workerWrapper.mWorker.getRunAttemptCount(), is(10));
    }

    private WorkerWrapper.Builder createBuilder(String workSpecId) {
        return new WorkerWrapper.Builder(
                mContext,
                mConfiguration,
                mWorkTaskExecutor,
                mDatabase,
                workSpecId);
    }

    private FutureListener createAndAddFutureListener(WorkerWrapper workerWrapper) {
        ListenableFuture<Boolean> future = workerWrapper.getFuture();
        FutureListener listener = new FutureListener(future);
        future.addListener(listener, mSynchronousExecutor);
        return listener;
    }

    private static class FutureListener implements Runnable {

        ListenableFuture<Boolean> mFuture;
        Boolean mResult;

        FutureListener(ListenableFuture<Boolean> future) {
            mFuture = future;
        }

        @Override
        public void run() {
            try {
                mResult = mFuture.get();
            } catch (InterruptedException | ExecutionException e) {
                // Do nothing.
            }
        }
    }
}
