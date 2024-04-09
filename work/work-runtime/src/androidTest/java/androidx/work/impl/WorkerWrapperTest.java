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

import static android.app.job.JobParameters.STOP_REASON_CONSTRAINT_CHARGING;

import static androidx.work.WorkInfo.State.BLOCKED;
import static androidx.work.WorkInfo.State.CANCELLED;
import static androidx.work.WorkInfo.State.ENQUEUED;
import static androidx.work.WorkInfo.State.FAILED;
import static androidx.work.WorkInfo.State.RUNNING;
import static androidx.work.WorkInfo.State.SUCCEEDED;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.isOneOf;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import static java.util.concurrent.TimeUnit.HOURS;
import static java.util.concurrent.TimeUnit.MINUTES;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Consumer;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.filters.MediumTest;
import androidx.test.filters.SdkSuppress;
import androidx.test.filters.SmallTest;
import androidx.testutils.RepeatRule;
import androidx.work.ArrayCreatingInputMerger;
import androidx.work.BackoffPolicy;
import androidx.work.Configuration;
import androidx.work.Data;
import androidx.work.DatabaseTest;
import androidx.work.ForegroundUpdater;
import androidx.work.ListenableWorker;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.ProgressUpdater;
import androidx.work.Tracer;
import androidx.work.WorkInfo;
import androidx.work.WorkRequest;
import androidx.work.Worker;
import androidx.work.WorkerExceptionInfo;
import androidx.work.WorkerFactory;
import androidx.work.WorkerParameters;
import androidx.work.impl.foreground.ForegroundProcessor;
import androidx.work.impl.model.Dependency;
import androidx.work.impl.model.DependencyDao;
import androidx.work.impl.model.WorkSpec;
import androidx.work.impl.model.WorkSpecDao;
import androidx.work.impl.testutils.TestOverrideClock;
import androidx.work.impl.testutils.TrackingWorkerFactory;
import androidx.work.impl.utils.SynchronousExecutor;
import androidx.work.impl.utils.taskexecutor.InstantWorkTaskExecutor;
import androidx.work.impl.utils.taskexecutor.TaskExecutor;
import androidx.work.worker.ChainedArgumentWorker;
import androidx.work.worker.EchoingWorker;
import androidx.work.worker.ExceptionInConstructionWorker;
import androidx.work.worker.ExceptionWorker;
import androidx.work.worker.FailureWorker;
import androidx.work.worker.InterruptionAwareWorker;
import androidx.work.worker.LatchWorker;
import androidx.work.worker.NeverResolvedWorker;
import androidx.work.worker.RetryWorker;
import androidx.work.worker.ReturnNullResultWorker;
import androidx.work.worker.TestWorker;
import androidx.work.worker.UsedWorker;

import com.google.common.util.concurrent.ListenableFuture;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import kotlinx.coroutines.Dispatchers;

@RunWith(AndroidJUnit4.class)
public class WorkerWrapperTest extends DatabaseTest {

    private Configuration mConfiguration;
    private TaskExecutor mWorkTaskExecutor;
    private WorkSpecDao mWorkSpecDao;
    private DependencyDao mDependencyDao;
    private Context mContext;
    private TestOverrideClock mTestClock = new TestOverrideClock();
    private ForegroundProcessor mMockForegroundProcessor;
    private ProgressUpdater mMockProgressUpdater;
    private ForegroundUpdater mMockForegroundUpdater;
    private Executor mSynchronousExecutor = new SynchronousExecutor();
    private final ExecutorService mExecutorService = Executors.newSingleThreadExecutor();
    private TestWorkerExceptionHandler mWorkerExceptionHandler;
    private Tracer mTracer;

    @Before
    public void setUp() {
        mContext = ApplicationProvider.getApplicationContext();
        mTracer = mock(Tracer.class);
        mWorkerExceptionHandler = new TestWorkerExceptionHandler();
        mConfiguration = new Configuration.Builder()
                .setExecutor(new SynchronousExecutor())
                .setMinimumLoggingLevel(Log.VERBOSE)
                .setClock(mTestClock)
                .setWorkerInitializationExceptionHandler(mWorkerExceptionHandler)
                .setWorkerExecutionExceptionHandler(mWorkerExceptionHandler)
                .setTracer(mTracer)
                .build();
        mWorkTaskExecutor = new InstantWorkTaskExecutor();
        mWorkSpecDao = mDatabase.workSpecDao();
        mDependencyDao = mDatabase.dependencyDao();
        mMockForegroundProcessor = mock(ForegroundProcessor.class);
        mMockProgressUpdater = mock(ProgressUpdater.class);
        mMockForegroundUpdater = mock(ForegroundUpdater.class);
    }

    @After
    public void tearDown() {
        mExecutorService.shutdown();
        try {
            assertThat(mExecutorService.awaitTermination(3, TimeUnit.SECONDS), is(true));
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        mDatabase.close();
    }

    @Rule
    public RepeatRule mRepeatRule = new RepeatRule();

    @Test
    @SmallTest
    public void testSuccess() {
        OneTimeWorkRequest work = new OneTimeWorkRequest.Builder(TestWorker.class).build();
        insertWork(work);
        WorkerWrapper workerWrapper = createBuilder(work.getStringId()).build();
        FutureListener listener = createAndAddFutureListener(workerWrapper);
        assertThat(listener.mResult, is(false));
        assertThat(mWorkSpecDao.getState(work.getStringId()), is(SUCCEEDED));
        assertBeginEndTraceSpans(work);
    }

    @Test
    @SmallTest
    public void testRunAttemptCountIncremented_successfulExecution() {
        OneTimeWorkRequest work = new OneTimeWorkRequest.Builder(TestWorker.class).build();
        insertWork(work);
        createBuilder(work.getStringId())
                .build()
                .launch();
        WorkSpec latestWorkSpec = mWorkSpecDao.getWorkSpec(work.getStringId());
        assertThat(latestWorkSpec.runAttemptCount, is(1));
        assertBeginEndTraceSpans(work);
    }

    @Test
    @SmallTest
    public void testRunAttemptCountIncremented_failedExecution() {
        OneTimeWorkRequest work = new OneTimeWorkRequest.Builder(FailureWorker.class).build();
        insertWork(work);
        createBuilder(work.getStringId())
                .build()
                .launch();
        WorkSpec latestWorkSpec = mWorkSpecDao.getWorkSpec(work.getStringId());
        assertThat(latestWorkSpec.runAttemptCount, is(1));
        assertBeginEndTraceSpans(work);
    }

    @Test
    @SmallTest
    public void testInvalidWorkerClassName() {
        OneTimeWorkRequest work = new OneTimeWorkRequest.Builder(TestWorker.class).build();
        work.getWorkSpec().workerClassName = "dummy";
        insertWork(work);
        WorkerWrapper workerWrapper = createBuilder(work.getStringId()).build();
        FutureListener listener = createAndAddFutureListener(workerWrapper);
        assertThat(listener.mResult, is(false));
        assertThat(mWorkSpecDao.getState(work.getStringId()), is(FAILED));
    }

    @Test(expected = IllegalStateException.class)
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
                                0,
                                mSynchronousExecutor,
                                Dispatchers.getDefault(),
                                mWorkTaskExecutor,
                                mConfiguration.getWorkerFactory(),
                                mMockProgressUpdater,
                                mMockForegroundUpdater));

        WorkerWrapper workerWrapper = createBuilder(work.getStringId())
                .withWorker(usedWorker)
                .build();
        workerWrapper.launch();
        assertBeginEndTraceSpans(work);
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
        assertThat(listener.mResult, is(false));
        assertThat(mWorkSpecDao.getState(work.getStringId()), is(CANCELLED));
        assertBeginEndTraceSpans(work);
    }

    @Test
    @SmallTest
    public void testPermanentErrorWithInvalidWorkerClass() {
        OneTimeWorkRequest work = new OneTimeWorkRequest.Builder(TestWorker.class).build();
        work.getWorkSpec().workerClassName = "INVALID_CLASS_NAME";
        insertWork(work);
        WorkerWrapper workerWrapper = createBuilder(work.getStringId()).build();
        FutureListener listener = createAndAddFutureListener(workerWrapper);
        assertThat(listener.mResult, is(false));
        assertThat(mWorkSpecDao.getState(work.getStringId()), is(FAILED));
    }

    @Test
    @SmallTest
    public void testPermanentErrorWithInvalidInputMergerClass() {
        OneTimeWorkRequest work = new OneTimeWorkRequest.Builder(TestWorker.class).build();
        work.getWorkSpec().inputMergerClassName = "INVALID_CLASS_NAME";
        insertWork(work);
        WorkerWrapper workerWrapper = createBuilder(work.getStringId())
                .build();
        FutureListener listener = createAndAddFutureListener(workerWrapper);
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
        assertThat(listener.mResult, is(false));
        assertThat(mWorkSpecDao.getState(work.getStringId()), is(FAILED));
    }

    @Test
    @LargeTest
    public void testFailedOnDeepHierarchy() {
        OneTimeWorkRequest work = new OneTimeWorkRequest.Builder(FailureWorker.class).build();
        insertWork(work);
        String previousId = work.getStringId();
        String firstWorkId = previousId;
        for (int i = 0; i < 500; ++i) {
            work = new OneTimeWorkRequest.Builder(FailureWorker.class).build();
            insertWork(work);
            mDependencyDao.insertDependency(new Dependency(work.getStringId(), previousId));
            previousId = work.getStringId();
        }
        WorkerWrapper workerWrapper = createBuilder(firstWorkId).build();
        workerWrapper.setFailed(new ListenableWorker.Result.Failure());
        assertThat(mWorkSpecDao.getState(firstWorkId), is(FAILED));
        assertThat(mWorkSpecDao.getState(previousId), is(FAILED));
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
        assertThat(listener.mResult, is(true));
        assertBeginEndTraceSpans(work);
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
                .build().launch();

        List<Data> arguments = mWorkSpecDao.getInputsFromPrerequisites(work.getStringId());
        assertThat(arguments.size(), is(1));
        assertThat(arguments, contains(ChainedArgumentWorker.getChainedArguments()));
        assertBeginEndTraceSpans(prerequisiteWork);
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
        String id = work.getStringId();
        Dependency dependency1 =
                new Dependency(id, prerequisiteWork1.getStringId());
        Dependency dependency2 =
                new Dependency(id, prerequisiteWork2.getStringId());

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
        createBuilder(prerequisiteWork1.getStringId()).build().launch();

        createBuilder(prerequisiteWork2.getStringId()).build().launch();

        TrackingWorkerFactory factory = new TrackingWorkerFactory();
        Configuration configuration = new Configuration.Builder(mConfiguration)
                .setWorkerFactory(factory)
                .build();
        WorkerWrapper workerWrapper = new WorkerWrapper.Builder(
                mContext,
                configuration,
                mWorkTaskExecutor,
                mMockForegroundProcessor,
                mDatabase,
                mWorkSpecDao.getWorkSpec(id),
                mDatabase.workTagDao().getTagsForWorkSpecId(id)
        ).build();
        // Create and run the dependent work.
        workerWrapper.launch();

        ListenableWorker worker = factory.awaitWorker(UUID.fromString(id));
        Data input = worker.getInputData();
        assertThat(input.size(), is(1));
        assertThat(Arrays.asList(input.getStringArray(key)), containsInAnyOrder(value1, value2));
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

        assertThat(mWorkSpecDao.getWorkSpec(work.getStringId()).lastEnqueueTime, is(-1L));

        long beforeUnblockedTime = System.currentTimeMillis();

        createBuilder(prerequisiteWork.getStringId()).build().launch();

        WorkSpec workSpec = mWorkSpecDao.getWorkSpec(work.getStringId());
        assertThat(workSpec.lastEnqueueTime, is(greaterThanOrEqualTo(beforeUnblockedTime)));
    }

    @Test
    @SmallTest
    public void testDependencies_enqueuesBlockedDependentsOnSuccess() {
        OneTimeWorkRequest prerequisiteWork =
                new OneTimeWorkRequest.Builder(TestWorker.class).build();
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
                .launch();

        assertThat(mWorkSpecDao.getState(prerequisiteWork.getStringId()), is(SUCCEEDED));
        assertThat(mWorkSpecDao.getState(work.getStringId()),
                isOneOf(ENQUEUED, RUNNING, SUCCEEDED));
        assertThat(mWorkSpecDao.getState(cancelledWork.getStringId()), is(CANCELLED));
        assertBeginEndTraceSpans(prerequisiteWork);
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
                .launch();

        assertThat(mWorkSpecDao.getState(prerequisiteWork.getStringId()), is(FAILED));
        assertThat(mWorkSpecDao.getState(work.getStringId()), is(FAILED));
        assertThat(mWorkSpecDao.getState(cancelledWork.getStringId()), is(CANCELLED));
    }

    @Test
    @SmallTest
    public void testBackedOffOneTimeWork_doesNotRun() {
        OneTimeWorkRequest retryWork =
                new OneTimeWorkRequest.Builder(RetryWorker.class).build();

        long future = System.currentTimeMillis() + HOURS.toMillis(1);
        mDatabase.beginTransaction();
        try {
            mWorkSpecDao.insertWorkSpec(retryWork.getWorkSpec());
            mWorkSpecDao.setLastEnqueueTime(retryWork.getStringId(), future);
            mWorkSpecDao.incrementWorkSpecRunAttemptCount(retryWork.getStringId());
            mDatabase.setTransactionSuccessful();
        } finally {
            mDatabase.endTransaction();
        }

        createBuilder(retryWork.getStringId())
                .build()
                .launch();

        WorkSpec workSpec = mWorkSpecDao.getWorkSpec(retryWork.getStringId());
        // The run attempt count should remain the same
        assertThat(workSpec.runAttemptCount, is(1));
        assertBeginEndTraceSpans(retryWork);
    }

    @Test
    @SmallTest
    public void testRun_periodicWork_success_updatesPeriodStartTime() {
        long intervalDurationMillis = PeriodicWorkRequest.MIN_PERIODIC_INTERVAL_MILLIS;
        long periodStartTimeMillis = System.currentTimeMillis();

        PeriodicWorkRequest periodicWork = new PeriodicWorkRequest.Builder(
                TestWorker.class, intervalDurationMillis, TimeUnit.MILLISECONDS).build();

        periodicWork.getWorkSpec().lastEnqueueTime = periodStartTimeMillis;
        insertWork(periodicWork);

        createBuilder(periodicWork.getStringId())
                .build()
                .launch();

        WorkSpec updatedWorkSpec = mWorkSpecDao.getWorkSpec(periodicWork.getStringId());
        assertThat(updatedWorkSpec.calculateNextRunTime(), greaterThan(periodStartTimeMillis));
        assertBeginEndTraceSpans(periodicWork);
    }

    @Test
    @SmallTest
    public void testRun_periodicWork_failure_updatesPeriodStartTime() {
        long intervalDurationMillis = PeriodicWorkRequest.MIN_PERIODIC_INTERVAL_MILLIS;
        long periodStartTimeMillis = System.currentTimeMillis();

        PeriodicWorkRequest periodicWork = new PeriodicWorkRequest.Builder(
                FailureWorker.class, intervalDurationMillis, TimeUnit.MILLISECONDS).build();

        periodicWork.getWorkSpec().lastEnqueueTime = periodStartTimeMillis;
        insertWork(periodicWork);

        createBuilder(periodicWork.getStringId())
                .build()
                .launch();

        WorkSpec updatedWorkSpec = mWorkSpecDao.getWorkSpec(periodicWork.getStringId());
        assertThat(updatedWorkSpec.calculateNextRunTime(), greaterThan(periodStartTimeMillis));
        assertBeginEndTraceSpans(periodicWork);
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
        WorkerWrapper workerWrapper = createBuilder(periodicWorkId).build();
        FutureListener listener = createAndAddFutureListener(workerWrapper);

        WorkSpec periodicWorkSpecAfterFirstRun = mWorkSpecDao.getWorkSpec(periodicWorkId);
        assertThat(listener.mResult, is(false));
        assertThat(periodicWorkSpecAfterFirstRun.runAttemptCount, is(0));
        assertThat(periodicWorkSpecAfterFirstRun.state, is(ENQUEUED));
        assertBeginEndTraceSpans(periodicWork);
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

        WorkSpec periodicWorkSpecAfterFirstRun = mWorkSpecDao.getWorkSpec(periodicWorkId);
        assertThat(listener.mResult, is(false));
        assertThat(periodicWorkSpecAfterFirstRun.runAttemptCount, is(0));
        assertThat(periodicWorkSpecAfterFirstRun.state, is(ENQUEUED));
        assertBeginEndTraceSpans(periodicWork);
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

        WorkSpec periodicWorkSpecAfterFirstRun = mWorkSpecDao.getWorkSpec(periodicWorkId);
        assertThat(listener.mResult, is(true));
        assertThat(periodicWorkSpecAfterFirstRun.runAttemptCount, is(1));
        assertThat(periodicWorkSpecAfterFirstRun.state, is(ENQUEUED));
        assertBeginEndTraceSpans(periodicWork);
    }


    @Test
    @SmallTest
    public void testPeriodic_dedupe() {
        PeriodicWorkRequest periodicWork = new PeriodicWorkRequest.Builder(
                TestWorker.class,
                PeriodicWorkRequest.MIN_PERIODIC_INTERVAL_MILLIS,
                TimeUnit.MILLISECONDS)
                .build();

        final String periodicWorkId = periodicWork.getStringId();
        final WorkSpec workSpec = periodicWork.getWorkSpec();
        long now = System.currentTimeMillis();
        workSpec.lastEnqueueTime = now + workSpec.intervalDuration;
        workSpec.setPeriodCount(1);
        insertWork(periodicWork);
        WorkerWrapper workerWrapper = createBuilder(periodicWorkId).build();
        FutureListener listener = createAndAddFutureListener(workerWrapper);
        // Should get rescheduled
        assertThat(listener.mResult, is(true));
        assertBeginEndTraceSpans(periodicWork);
    }

    @Test
    @SmallTest
    public void testPeriodic_firstRun_flexApplied() {
        PeriodicWorkRequest periodicWork = new PeriodicWorkRequest.Builder(
                TestWorker.class,
                PeriodicWorkRequest.MIN_PERIODIC_INTERVAL_MILLIS,
                TimeUnit.MILLISECONDS,
                PeriodicWorkRequest.MIN_PERIODIC_FLEX_MILLIS,
                TimeUnit.MILLISECONDS)
                .build();

        final String periodicWorkId = periodicWork.getStringId();
        final WorkSpec workSpec = periodicWork.getWorkSpec();
        workSpec.lastEnqueueTime = System.currentTimeMillis();
        insertWork(periodicWork);
        WorkerWrapper workerWrapper = createBuilder(periodicWorkId).build();
        FutureListener listener = createAndAddFutureListener(workerWrapper);
        // Should get rescheduled because flex should be respected.
        assertThat(listener.mResult, is(true));
        assertBeginEndTraceSpans(periodicWork);
    }

    @Test
    @SmallTest
    public void testNextScheduleTimeOverride_delayNormalNextSchedule() {
        mTestClock.currentTimeMillis = HOURS.toMillis(5);
        long lastEnqueueTimeMillis = HOURS.toMillis(4);
        long intervalDurationMillis = HOURS.toMillis(1);
        // Delay the next run
        long nextScheduleTimeOverrideMillis = lastEnqueueTimeMillis + HOURS.toMillis(10);

        PeriodicWorkRequest periodicWork = new PeriodicWorkRequest.Builder(
                TestWorker.class, intervalDurationMillis, TimeUnit.MILLISECONDS)
                .setNextScheduleTimeOverride(nextScheduleTimeOverrideMillis)
                .build();

        periodicWork.getWorkSpec().lastEnqueueTime = lastEnqueueTimeMillis;
        insertWork(periodicWork);

        // Try to run when the normal period would have happened
        mTestClock.currentTimeMillis = lastEnqueueTimeMillis + intervalDurationMillis + 1;
        createBuilder(periodicWork.getStringId()).build().launch();

        // Didn't actually run or do anything, since it's too soon to run
        WorkSpec firstTryWorkSpec = mWorkSpecDao.getWorkSpec(periodicWork.getStringId());
        assertThat(firstTryWorkSpec.getNextScheduleTimeOverride(),
                equalTo(nextScheduleTimeOverrideMillis));
        assertThat(firstTryWorkSpec.getPeriodCount(), equalTo(0));
        assertThat(firstTryWorkSpec.calculateNextRunTime(),
                equalTo(nextScheduleTimeOverrideMillis));

        // Try again at the override time
        long actualWorkRunTime = nextScheduleTimeOverrideMillis;
        mTestClock.currentTimeMillis = actualWorkRunTime;
        createBuilder(periodicWork.getStringId()).build().launch();

        // Override is cleared and we're scheduled for now + period
        WorkSpec afterRunWorkSpec = mWorkSpecDao.getWorkSpec(periodicWork.getStringId());
        assertThat(afterRunWorkSpec.getNextScheduleTimeOverride(), equalTo(Long.MAX_VALUE));
        assertThat(afterRunWorkSpec.getPeriodCount(), equalTo(1));
        assertThat(afterRunWorkSpec.calculateNextRunTime(),
                equalTo(actualWorkRunTime + intervalDurationMillis));
    }

    @Test
    @SmallTest
    public void testNextScheduleTimeOverride_backsOffNextTimeAfterRetry() {
        mTestClock.currentTimeMillis = HOURS.toMillis(5);
        long lastEnqueueTimeMillis = HOURS.toMillis(4);
        long intervalDurationMillis = HOURS.toMillis(100);
        long nextScheduleTimeOverrideMillis = lastEnqueueTimeMillis + HOURS.toMillis(10);
        long backoffLinearDurationMillis = MINUTES.toMillis(30);

        PeriodicWorkRequest periodicWork = new PeriodicWorkRequest.Builder(
                // RetryWorker always returns Result.Retry
                RetryWorker.class, intervalDurationMillis, TimeUnit.MILLISECONDS)
                .setBackoffCriteria(BackoffPolicy.LINEAR, backoffLinearDurationMillis,
                        TimeUnit.MILLISECONDS)
                .setNextScheduleTimeOverride(nextScheduleTimeOverrideMillis)
                .build();

        periodicWork.getWorkSpec().lastEnqueueTime = lastEnqueueTimeMillis;
        mTestClock.currentTimeMillis = nextScheduleTimeOverrideMillis;
        insertWork(periodicWork);

        createBuilder(periodicWork.getStringId()).build().launch();

        // Override is cleared and we're rescheduled according to the backoff policy
        WorkSpec afterRunWorkSpec = mWorkSpecDao.getWorkSpec(periodicWork.getStringId());
        assertThat(afterRunWorkSpec.getPeriodCount(), equalTo(0));
        assertThat(afterRunWorkSpec.runAttemptCount, equalTo(1));
        assertThat(afterRunWorkSpec.state, is(ENQUEUED));
        assertThat(afterRunWorkSpec.getNextScheduleTimeOverride(), equalTo(Long.MAX_VALUE));
        // Should be scheduled again for now + one backoff.
        assertThat(afterRunWorkSpec.calculateNextRunTime(),
                equalTo(nextScheduleTimeOverrideMillis + backoffLinearDurationMillis));
    }

    @Test
    @SmallTest
    public void testNextScheduleTimeOverride_whileWorkerIsRunning_appliesToNextRun()
            throws ExecutionException, InterruptedException {
        mTestClock.currentTimeMillis = HOURS.toMillis(5);
        long lastEnqueueTimeMillis = HOURS.toMillis(4);
        long intervalDurationMillis = HOURS.toMillis(100);

        long firstOverride = lastEnqueueTimeMillis + HOURS.toMillis(10);
        long secondOverride = firstOverride + HOURS.toMillis(20);
        long backoffLinearDurationMillis = MINUTES.toMillis(30);

        PeriodicWorkRequest periodicWork = new PeriodicWorkRequest.Builder(
                LatchWorker.class, intervalDurationMillis, TimeUnit.MILLISECONDS)
                .setBackoffCriteria(BackoffPolicy.LINEAR, backoffLinearDurationMillis,
                        TimeUnit.MILLISECONDS)
                .build();

        periodicWork.getWorkSpec().lastEnqueueTime = lastEnqueueTimeMillis;
        mTestClock.currentTimeMillis = firstOverride;
        insertWork(periodicWork);

        LatchWorker latchWorker = getLatchWorker(periodicWork, mExecutorService);
        FutureListener listener = runWorker(periodicWork, latchWorker);

        // Wait for the worker to start, to verify WorkerWrapper got through its initialization
        latchWorker.mEntrySignal.await();

        // Update the work with a new 'next' runtime while it's already running.
        WorkSpec inflightWorkSpec = mWorkSpecDao.getWorkSpec(periodicWork.getStringId());
        inflightWorkSpec.setNextScheduleTimeOverride(secondOverride);
        inflightWorkSpec.setNextScheduleTimeOverrideGeneration(3);
        mDatabase.workSpecDao().delete(inflightWorkSpec.id);
        mDatabase.workSpecDao().insertWorkSpec(inflightWorkSpec);

        // Finish the Worker so WorkerWrapper can clean up....
        latchWorker.mLatch.countDown();
        listener.mFuture.get();

        // We should still be overridden, even though the worker finished after the override
        WorkSpec afterRunWorkSpec = mWorkSpecDao.getWorkSpec(periodicWork.getStringId());
        assertThat(afterRunWorkSpec.getPeriodCount(), equalTo(1));
        assertThat(afterRunWorkSpec.runAttemptCount, equalTo(0));
        assertThat(afterRunWorkSpec.state, is(ENQUEUED));
        assertThat(afterRunWorkSpec.getNextScheduleTimeOverrideGeneration(), equalTo(3));
        assertThat(afterRunWorkSpec.getNextScheduleTimeOverride(), equalTo(secondOverride));
        assertThat(afterRunWorkSpec.calculateNextRunTime(),
                equalTo(secondOverride));
        assertBeginEndTraceSpans(periodicWork);
    }

    @Test
    @SmallTest
    public void testNextScheduleTimeOverride_whileWorkerIsRunning_returnsRetry_usesOverride()
            throws ExecutionException, InterruptedException {
        mTestClock.currentTimeMillis = HOURS.toMillis(5);
        long lastEnqueueTimeMillis = HOURS.toMillis(4);
        long intervalDurationMillis = HOURS.toMillis(100);

        long firstOverrideMillis = lastEnqueueTimeMillis + HOURS.toMillis(10);
        long secondOverrideMillis = firstOverrideMillis + HOURS.toMillis(20);
        long backoffLinearDurationMillis = MINUTES.toMillis(30);

        PeriodicWorkRequest periodicWork = new PeriodicWorkRequest.Builder(
                LatchWorker.class, intervalDurationMillis, TimeUnit.MILLISECONDS)
                .setBackoffCriteria(BackoffPolicy.LINEAR, backoffLinearDurationMillis,
                        TimeUnit.MILLISECONDS)
                .build();

        periodicWork.getWorkSpec().lastEnqueueTime = lastEnqueueTimeMillis;
        mTestClock.currentTimeMillis = firstOverrideMillis;
        insertWork(periodicWork);

        // Start the worker running
        LatchWorker latchWorker = getLatchWorker(periodicWork, mExecutorService);
        latchWorker.returnResult = ListenableWorker.Result.retry();
        FutureListener listener = runWorker(periodicWork, latchWorker);

        // Wait for the worker to start, to verify WorkerWrapper got through its initialization
        latchWorker.mEntrySignal.await();

        // Update the work with a new 'next' runtime while it's already running.
        WorkSpec inflightWorkSpec = mWorkSpecDao.getWorkSpec(periodicWork.getStringId());
        inflightWorkSpec.setNextScheduleTimeOverride(secondOverrideMillis);
        inflightWorkSpec.setNextScheduleTimeOverrideGeneration(3);
        mDatabase.workSpecDao().delete(inflightWorkSpec.id);
        mDatabase.workSpecDao().insertWorkSpec(inflightWorkSpec);

        // Allow the worker to finish
        latchWorker.mLatch.countDown();
        listener.mFuture.get();

        // We should still be overridden, even though the worker finished after the override
        WorkSpec afterRunWorkSpec = mWorkSpecDao.getWorkSpec(periodicWork.getStringId());
        assertThat(afterRunWorkSpec.getPeriodCount(), equalTo(0));
        // We still write runAttemptCount, etc, so if the override is cleared the 'correct' retry
        // time is applied based on the actual previous run time.
        assertThat(afterRunWorkSpec.runAttemptCount, equalTo(1));
        assertThat(afterRunWorkSpec.state, is(ENQUEUED));
        assertThat(afterRunWorkSpec.getNextScheduleTimeOverrideGeneration(), equalTo(3));
        assertThat(afterRunWorkSpec.getNextScheduleTimeOverride(), equalTo(secondOverrideMillis));
        assertThat(afterRunWorkSpec.calculateNextRunTime(),
                equalTo(secondOverrideMillis));
        assertBeginEndTraceSpans(periodicWork);
    }

    @Test
    @SmallTest
    public void testClearNextScheduleTimeOverride_whileWorkerIsRunning_schedulesNextBasedOnEnqueue()
            throws InterruptedException, ExecutionException {
        long lastEnqueueTimeMillis = HOURS.toMillis(4);
        long intervalDurationMillis = HOURS.toMillis(100);
        long nextScheduleTimeOverrideMillis = lastEnqueueTimeMillis + HOURS.toMillis(10);
        mTestClock.currentTimeMillis = nextScheduleTimeOverrideMillis;

        PeriodicWorkRequest periodicWork = new PeriodicWorkRequest.Builder(
                LatchWorker.class, intervalDurationMillis, TimeUnit.MILLISECONDS)
                .build();
        periodicWork.getWorkSpec().lastEnqueueTime = lastEnqueueTimeMillis;
        insertWork(periodicWork);

        // Start the worker running
        LatchWorker latchWorker = getLatchWorker(periodicWork, mExecutorService);
        latchWorker.returnResult = ListenableWorker.Result.success();
        FutureListener listener = runWorker(periodicWork, latchWorker);

        // Wait for the worker to start, to verify WorkerWrapper got through its initialization
        latchWorker.mEntrySignal.await();

        // Update the override generation, but leave the override time to MAX_LONG.
        // This replicates calling .override(), then .clearOverride() to return /back/ to normal.
        WorkSpec inflightWorkSpec = mWorkSpecDao.getWorkSpec(periodicWork.getStringId());
        inflightWorkSpec.setNextScheduleTimeOverride(Long.MAX_VALUE);
        inflightWorkSpec.setNextScheduleTimeOverrideGeneration(5);
        mDatabase.workSpecDao().delete(inflightWorkSpec.id);
        mDatabase.workSpecDao().insertWorkSpec(inflightWorkSpec);

        // Allow the worker to finish
        latchWorker.mLatch.countDown();
        listener.mFuture.get();

        // We should be scheduled for a "normal" next time, even though overrideGen was changed.
        WorkSpec afterRunWorkSpec = mWorkSpecDao.getWorkSpec(periodicWork.getStringId());
        assertThat(afterRunWorkSpec.getPeriodCount(), equalTo(1));
        assertThat(afterRunWorkSpec.runAttemptCount, equalTo(0));
        assertThat(afterRunWorkSpec.state, is(ENQUEUED));
        assertThat(afterRunWorkSpec.getNextScheduleTimeOverrideGeneration(), equalTo(5));
        assertThat(afterRunWorkSpec.getNextScheduleTimeOverride(), equalTo(Long.MAX_VALUE));
        // Normal next period is scheduled.
        assertThat(afterRunWorkSpec.calculateNextRunTime(),
                equalTo(mTestClock.currentTimeMillis + intervalDurationMillis));
        assertBeginEndTraceSpans(periodicWork);
    }


    @Test
    @SmallTest
    public void testClearNextScheduleTimeOverride_whileWorkerIsRunning_schedulesNextBasedOnBackoff()
            throws InterruptedException, ExecutionException {
        long lastEnqueueTimeMillis = HOURS.toMillis(4);
        long intervalDurationMillis = HOURS.toMillis(10);
        long backoffLinearDurationMillis = HOURS.toMillis(1);
        long nextScheduleTimeOverrideMillis = lastEnqueueTimeMillis + HOURS.toMillis(2);
        mTestClock.currentTimeMillis = nextScheduleTimeOverrideMillis;

        PeriodicWorkRequest periodicWork = new PeriodicWorkRequest.Builder(
                LatchWorker.class, intervalDurationMillis, TimeUnit.MILLISECONDS)
                .setBackoffCriteria(BackoffPolicy.LINEAR, backoffLinearDurationMillis,
                        TimeUnit.MILLISECONDS)
                .build();
        periodicWork.getWorkSpec().lastEnqueueTime = lastEnqueueTimeMillis;
        insertWork(periodicWork);

        // Start the worker running
        LatchWorker latchWorker = getLatchWorker(periodicWork, mExecutorService);
        latchWorker.returnResult = ListenableWorker.Result.retry();
        FutureListener listener = runWorker(periodicWork, latchWorker);

        // Wait for the worker to start, to verify WorkerWrapper got through its initialization
        latchWorker.mEntrySignal.await();

        // Update the override generation, but leave the override time to MAX_LONG.
        // This replicates calling .override(), then .clearOverride() to return /back/ to normal.
        WorkSpec inflightWorkSpec = mWorkSpecDao.getWorkSpec(periodicWork.getStringId());
        inflightWorkSpec.setNextScheduleTimeOverride(Long.MAX_VALUE);
        inflightWorkSpec.setNextScheduleTimeOverrideGeneration(5);
        mDatabase.workSpecDao().delete(inflightWorkSpec.id);
        mDatabase.workSpecDao().insertWorkSpec(inflightWorkSpec);

        // Allow the worker to finish
        latchWorker.mLatch.countDown();
        listener.mFuture.get();

        // We should be scheduled for a normal backoff, even though overrideGen was changed.
        WorkSpec afterRunWorkSpec = mWorkSpecDao.getWorkSpec(periodicWork.getStringId());
        assertThat(afterRunWorkSpec.getPeriodCount(), equalTo(0));
        assertThat(afterRunWorkSpec.runAttemptCount, equalTo(1));
        assertThat(afterRunWorkSpec.state, is(ENQUEUED));
        assertThat(afterRunWorkSpec.getNextScheduleTimeOverrideGeneration(), equalTo(5));
        assertThat(afterRunWorkSpec.getNextScheduleTimeOverride(), equalTo(Long.MAX_VALUE));
        // Backoff timing is respected
        assertThat(afterRunWorkSpec.calculateNextRunTime(),
                equalTo(mTestClock.currentTimeMillis + backoffLinearDurationMillis));
        assertBeginEndTraceSpans(periodicWork);
    }

    @NonNull
    private FutureListener runWorker(PeriodicWorkRequest periodicWork, Worker worker) {
        WorkerWrapper workerWrapper =
                createBuilder(periodicWork.getStringId()).withWorker(worker).build();
        FutureListener listener = createAndAddFutureListener(workerWrapper);
        return listener;
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
                        0,
                        mSynchronousExecutor,
                        Dispatchers.getDefault(),
                        mWorkTaskExecutor,
                        mConfiguration.getWorkerFactory(),
                        mMockProgressUpdater,
                        mMockForegroundUpdater));

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
                        0,
                        mSynchronousExecutor,
                        Dispatchers.getDefault(),
                        mWorkTaskExecutor,
                        mConfiguration.getWorkerFactory(),
                        mMockProgressUpdater,
                        mMockForegroundUpdater));

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
                        0,
                        mSynchronousExecutor,
                        Dispatchers.getDefault(),
                        mWorkTaskExecutor,
                        mConfiguration.getWorkerFactory(),
                        mMockProgressUpdater,
                        mMockForegroundUpdater));

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
                        0,
                        mSynchronousExecutor,
                        Dispatchers.getDefault(),
                        mWorkTaskExecutor,
                        mConfiguration.getWorkerFactory(),
                        mMockProgressUpdater,
                        mMockForegroundUpdater));

        assertThat(worker, is(notNullValue()));
        assertThat(worker.getTags(), containsInAnyOrder("one", "two", "three"));
    }

    @Test
    @SmallTest
    @SdkSuppress(minSdkVersion = 24)
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
                        0,
                        mSynchronousExecutor,
                        Dispatchers.getDefault(),
                        mWorkTaskExecutor,
                        mConfiguration.getWorkerFactory(),
                        mMockProgressUpdater,
                        mMockForegroundUpdater));

        assertThat(worker, is(notNullValue()));
        assertThat(worker.getTriggeredContentAuthorities(),
                containsInAnyOrder(runtimeExtras.triggeredContentAuthorities.toArray()));
        assertThat(worker.getTriggeredContentUris(),
                containsInAnyOrder(runtimeExtras.triggeredContentUris.toArray()));
    }

    // getStopReason() requires API level 31, but only because JobScheduler provides them
    // since API level 31, but in this isolated test we don't care.
    @SuppressLint("NewApi")
    @Test
    @SmallTest
    public void testInterruption_isMarkedOnRunningWorker() throws InterruptedException {
        OneTimeWorkRequest work =
                new OneTimeWorkRequest.Builder(InterruptionAwareWorker.class).build();
        insertWork(work);

        InterruptionAwareWorker worker = (InterruptionAwareWorker)
                mConfiguration.getWorkerFactory().createWorkerWithDefaultFallback(
                        mContext.getApplicationContext(),
                        InterruptionAwareWorker.class.getName(),
                        new WorkerParameters(
                                work.getId(),
                                Data.EMPTY,
                                Collections.<String>emptyList(),
                                new WorkerParameters.RuntimeExtras(),
                                1,
                                0,
                                mExecutorService,
                                Dispatchers.getDefault(),
                                mWorkTaskExecutor,
                                mConfiguration.getWorkerFactory(),
                                mMockProgressUpdater,
                                mMockForegroundUpdater));
        assertThat(worker, is(notNullValue()));
        assertThat(worker.isStopped(), is(false));

        WorkerWrapper workerWrapper =
                createBuilder(work.getStringId()).withWorker(worker).build();
        workerWrapper.launch();
        worker.doWorkLatch.await();
        workerWrapper.interrupt(STOP_REASON_CONSTRAINT_CHARGING);
        assertThat(worker.isStopped(), is(true));
        assertThat(worker.getStopReason(), is(STOP_REASON_CONSTRAINT_CHARGING));
        assertThat(mWorkSpecDao.getState(work.getStringId()), is(ENQUEUED));
    }

    @Test
    @SmallTest
    public void testException_isTreatedAsFailure() {
        OneTimeWorkRequest work = new OneTimeWorkRequest.Builder(ExceptionWorker.class).build();
        insertWork(work);

        createBuilder(work.getStringId()).build().launch();

        assertThat(mWorkSpecDao.getState(work.getStringId()), is(FAILED));
    }

    @Test
    @SmallTest
    public void testWorkerThatReturnsNullResult() {
        OneTimeWorkRequest work =
                new OneTimeWorkRequest.Builder(ReturnNullResultWorker.class).build();
        insertWork(work);
        WorkerWrapper workerWrapper = createBuilder(work.getStringId()).build();
        FutureListener listener = createAndAddFutureListener(workerWrapper);
        workerWrapper.launch();
        assertThat(listener.mResult, is(false));
        assertThat(mWorkSpecDao.getState(work.getStringId()), is(FAILED));
    }

    @Test
    @SmallTest
    public void testWorkerThatThrowsAnException() {
        OneTimeWorkRequest work = new OneTimeWorkRequest.Builder(ExceptionWorker.class)
                .setInputData(new Data.Builder().putString("foo", "bar").build()).build();
        insertWork(work);
        WorkerWrapper workerWrapper = createBuilder(work.getStringId()).build();
        FutureListener listener = createAndAddFutureListener(workerWrapper);
        assertThat(listener.mResult, is(false));
        assertThat(mWorkSpecDao.getState(work.getStringId()), is(FAILED));
        assertThat(mWorkerExceptionHandler.mWorkerClassName,
                is("androidx.work.worker.ExceptionWorker"));
        assertThat(mWorkerExceptionHandler.mWorkerParameters.getInputData().getString("foo"),
                is("bar"));
        assertThat(mWorkerExceptionHandler.mThrowable, instanceOf(IllegalStateException.class));
        assertThat(mWorkerExceptionHandler.mThrowable.getMessage(), is(
                "Thrown in doWork Exception"));
    }

    @Test
    @SmallTest
    public void testWorkerThatThrowsAnExceptionInConstruction() {
        OneTimeWorkRequest work =
                new OneTimeWorkRequest.Builder(ExceptionInConstructionWorker.class)
                        .setInputData(new Data.Builder().putString("foo", "bar").build()).build();
        insertWork(work);
        WorkerWrapper workerWrapper = createBuilder(work.getStringId()).build();
        FutureListener listener = createAndAddFutureListener(workerWrapper);
        assertThat(listener.mResult, is(false));
        assertThat(mWorkSpecDao.getState(work.getStringId()), is(FAILED));
        assertThat(mWorkerExceptionHandler.mWorkerClassName,
                is("androidx.work.worker.ExceptionInConstructionWorker"));
        assertThat(mWorkerExceptionHandler.mWorkerParameters.getInputData().getString("foo"),
                is("bar"));
        assertThat(mWorkerExceptionHandler.mThrowable,
                is(instanceOf(InvocationTargetException.class)));
        Throwable e = ((InvocationTargetException) mWorkerExceptionHandler.mThrowable)
                .getTargetException();
        assertThat(e, instanceOf(IllegalStateException.class));
        assertThat(e.getMessage(), is("Thrown in constructor Exception"));
    }

    @Test
    @SmallTest
    public void testCancellationDoesNotTriggerExceptionHandler() {
        OneTimeWorkRequest work = new OneTimeWorkRequest.Builder(NeverResolvedWorker.class)
                .build();
        insertWork(work);
        WorkerWrapper workerWrapper = createBuilder(work.getStringId()).build();
        FutureListener listener = createAndAddFutureListener(workerWrapper);

        assertThat(mWorkSpecDao.getState(work.getStringId()), is(RUNNING));
        workerWrapper.interrupt(0);
        assertThat(listener.mResult, is(true));
        assertThat(mWorkSpecDao.getState(work.getStringId()), is(ENQUEUED));
        assertThat(mWorkerExceptionHandler.mThrowable, nullValue());
    }

    @Test
    @SmallTest
    public void testExceptionInWorkerFactory() {
        OneTimeWorkRequest work =
                new OneTimeWorkRequest.Builder(TestWorker.class)
                        .setInputData(new Data.Builder().putString("foo", "bar").build()).build();
        insertWork(work);
        WorkerFactory factory = new WorkerFactory() {
            @Nullable
            @Override
            public ListenableWorker createWorker(@NonNull Context appContext,
                    @NonNull String workerClassName, @NonNull WorkerParameters workerParameters) {
                throw new IllegalStateException("Thrown in WorkerFactory Exception");
            }
        };
        Configuration configuration = new Configuration.Builder(mConfiguration)
                .setWorkerFactory(factory)
                .build();
        WorkerWrapper workerWrapper = new WorkerWrapper.Builder(
                mContext,
                configuration,
                mWorkTaskExecutor,
                mMockForegroundProcessor,
                mDatabase,
                mWorkSpecDao.getWorkSpec(work.getStringId()),
                mDatabase.workTagDao().getWorkSpecIdsWithTag(work.getStringId())
        ).build();
        FutureListener listener = createAndAddFutureListener(workerWrapper);
        assertThat(listener.mResult, is(false));
        assertThat(mWorkSpecDao.getState(work.getStringId()), is(FAILED));
        assertThat(mWorkerExceptionHandler.mWorkerClassName,
                is("androidx.work.worker.TestWorker"));
        assertThat(mWorkerExceptionHandler.mWorkerParameters.getInputData().getString("foo"),
                is("bar"));
        assertThat(mWorkerExceptionHandler.mThrowable,
                is(instanceOf(IllegalStateException.class)));
        assertThat(mWorkerExceptionHandler.mThrowable.getMessage(),
                is("Thrown in WorkerFactory Exception"));
    }

    @SuppressLint("NewApi")
    @Test
    @MediumTest
    @SdkSuppress(minSdkVersion = 21)
    public void testInterruptionsAfterCompletion() {
        // Suppressing this test prior to API 21, because creating a spy() ends up loading
        // android.net.Network class which does not exist before API 21.

        OneTimeWorkRequest work = new OneTimeWorkRequest.Builder(TestWorker.class).build();
        insertWork(work);
        TrackingWorkerFactory factory = new TrackingWorkerFactory();
        Configuration configuration = new Configuration.Builder(mConfiguration)
                .setWorkerFactory(factory)
                .build();

        String id = work.getStringId();
        WorkerWrapper workerWrapper = new WorkerWrapper.Builder(
                mContext,
                configuration,
                mWorkTaskExecutor,
                mMockForegroundProcessor,
                mDatabase,
                mWorkSpecDao.getWorkSpec(id),
                mDatabase.workTagDao().getTagsForWorkSpecId(id)
        ).build();

        FutureListener listener = createAndAddFutureListener(workerWrapper);
        assertThat(listener.mResult, is(false));
        assertThat(mWorkSpecDao.getState(id), is(SUCCEEDED));
        workerWrapper.interrupt(0);
        ListenableWorker worker = factory.awaitWorker(UUID.fromString(id));
        assertThat(worker.getStopReason(), is(WorkInfo.STOP_REASON_NOT_STOPPED));
    }

    @Test
    @MediumTest
    @SdkSuppress(minSdkVersion = 21)
    public void testInterruptionsBeforeCompletion() {
        // Suppressing this test prior to API 21, because creating a spy() ends up loading
        // android.net.Network class which does not exist before API 21.

        OneTimeWorkRequest work = new OneTimeWorkRequest.Builder(TestWorker.class).build();
        insertWork(work);
        // Mark scheduled
        mWorkSpecDao.markWorkSpecScheduled(work.getStringId(), System.currentTimeMillis());

        WorkerWrapper workerWrapper = new WorkerWrapper.Builder(
                mContext,
                mConfiguration,
                mWorkTaskExecutor,
                mMockForegroundProcessor,
                mDatabase,
                mWorkSpecDao.getWorkSpec(work.getStringId()),
                mDatabase.workTagDao().getWorkSpecIdsWithTag(work.getStringId())
        ).build();

        workerWrapper.interrupt(0);
        workerWrapper.launch();
        WorkSpec workSpec = mWorkSpecDao.getWorkSpec(work.getStringId());
        assertThat(workSpec.scheduleRequestedAt, is(-1L));
    }

    @Test
    @SmallTest
    public void testWorkRequest_withInvalidClassName() {
        OneTimeWorkRequest work =
                new OneTimeWorkRequest.Builder(TestWorker.class).build();
        work.getWorkSpec().workerClassName = "Bad class name";
        insertWork(work);
        WorkerWrapper workerWrapper = createBuilder(work.getStringId()).build();
        workerWrapper.launch();
        assertThat(mWorkSpecDao.getState(work.getStringId()), is(FAILED));
    }

    private WorkerWrapper.Builder createBuilder(String workSpecId) {
        return new WorkerWrapper.Builder(
                mContext,
                mConfiguration,
                mWorkTaskExecutor,
                mMockForegroundProcessor,
                mDatabase,
                mWorkSpecDao.getWorkSpec(workSpecId),
                mDatabase.workTagDao().getWorkSpecIdsWithTag(workSpecId)
        );
    }

    @Nullable
    private LatchWorker getLatchWorker(WorkRequest work, ExecutorService executorService) {
        return (LatchWorker) mConfiguration.getWorkerFactory().createWorkerWithDefaultFallback(
                mContext.getApplicationContext(),
                LatchWorker.class.getName(),
                new WorkerParameters(
                        work.getId(),
                        Data.EMPTY,
                        work.getTags(),
                        new WorkerParameters.RuntimeExtras(),
                        1,
                        0,
                        executorService,
                        Dispatchers.getDefault(),
                        mWorkTaskExecutor,
                        mConfiguration.getWorkerFactory(),
                        mMockProgressUpdater,
                        mMockForegroundUpdater));
    }

    private FutureListener createAndAddFutureListener(WorkerWrapper workerWrapper) {
        ListenableFuture<Boolean> future = workerWrapper.launch();
        FutureListener listener = new FutureListener(future);
        future.addListener(listener, mSynchronousExecutor);
        return listener;
    }

    private void assertBeginEndTraceSpans(WorkRequest workRequest) {
        ArgumentCaptor<String> traceSpan = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Integer> generation = ArgumentCaptor.forClass(Integer.class);
        verify(mTracer).beginAsyncSection(traceSpan.capture(), generation.capture());
        assertThat(workRequest.getWorkSpec().workerClassName, containsString(traceSpan.getValue()));
        assertThat(workRequest.getWorkSpec().getGeneration(), is(generation.getValue()));
        verify(mTracer).beginAsyncSection(traceSpan.capture(), generation.capture());
        assertThat(workRequest.getWorkSpec().workerClassName, containsString(traceSpan.getValue()));
        assertThat(workRequest.getWorkSpec().getGeneration(), is(generation.getValue()));
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

    private static class TestWorkerExceptionHandler implements Consumer<WorkerExceptionInfo> {
        String mWorkerClassName;
        WorkerParameters mWorkerParameters;
        Throwable mThrowable;

        @Override
        public void accept(WorkerExceptionInfo params) {
            this.mWorkerClassName = params.getWorkerClassName();
            this.mWorkerParameters = params.getWorkerParameters();
            this.mThrowable = params.getThrowable();
        }
    }

    ;
}
