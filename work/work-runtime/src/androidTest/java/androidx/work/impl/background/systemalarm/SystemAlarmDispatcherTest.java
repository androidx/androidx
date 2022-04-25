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

package androidx.work.impl.background.systemalarm;

import static com.google.common.truth.Truth.assertThat;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.testutils.RepeatRule;
import androidx.work.Configuration;
import androidx.work.Constraints;
import androidx.work.DatabaseTest;
import androidx.work.Logger;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.impl.Processor;
import androidx.work.impl.Scheduler;
import androidx.work.impl.WorkManagerImpl;
import androidx.work.impl.WorkRunId;
import androidx.work.impl.constraints.NetworkState;
import androidx.work.impl.constraints.trackers.BatteryNotLowTracker;
import androidx.work.impl.constraints.trackers.ConstraintTracker;
import androidx.work.impl.constraints.trackers.Trackers;
import androidx.work.impl.model.WorkSpec;
import androidx.work.impl.model.WorkSpecDao;
import androidx.work.impl.utils.SerialExecutor;
import androidx.work.impl.utils.SynchronousExecutor;
import androidx.work.impl.utils.taskexecutor.TaskExecutor;
import androidx.work.worker.RetryWorker;
import androidx.work.worker.SleepTestWorker;
import androidx.work.worker.TestWorker;

import org.hamcrest.collection.IsIterableContainingInOrder;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class SystemAlarmDispatcherTest extends DatabaseTest {

    @Rule
    public RepeatRule mRepeatRule = new RepeatRule();

    private static final int START_ID = 0;
    // Test timeout in seconds - this needs to be longer than SleepTestWorker.SLEEP_DURATION
    private static final int TEST_TIMEOUT = 6;

    private Context mContext;
    private WorkManagerImpl mWorkManager;
    private Processor mSpyProcessor;
    private CommandInterceptingSystemDispatcher mDispatcher;
    private CommandInterceptingSystemDispatcher mSpyDispatcher;
    private CountDownLatch mLatch;

    private FakeConstraintTracker mBatteryChargingTracker;
    private FakeConstraintTracker mStorageNotLowTracker;
    private final Executor mMainThreadExecutor = new Executor() {
        private final Handler mHandler = new Handler(Looper.getMainLooper());

        @Override
        public void execute(Runnable runnable) {
            mHandler.post(runnable);
        }
    };

    @Before
    @SuppressWarnings("unchecked")
    public void setUp() {
        mContext = ApplicationProvider.getApplicationContext().getApplicationContext();
        Scheduler scheduler = mock(Scheduler.class);
        mWorkManager = mock(WorkManagerImpl.class);
        mLatch = new CountDownLatch(1);
        SystemAlarmDispatcher.CommandsCompletedListener completedListener =
                new SystemAlarmDispatcher.CommandsCompletedListener() {
                    @Override
                    public void onAllCommandsCompleted() {
                        mLatch.countDown();
                    }
                };

        TaskExecutor instantTaskExecutor = new TaskExecutor() {

            @Override
            public Executor getMainThreadExecutor() {
                return mMainThreadExecutor;
            }

            @Override
            public SerialExecutor getSerialTaskExecutor() {
                return new SerialExecutor(new SynchronousExecutor());
            }
        };
        mBatteryChargingTracker = new FakeConstraintTracker(mContext, instantTaskExecutor);
        BatteryNotLowTracker batteryNotLowTracker =
                new BatteryNotLowTracker(mContext, instantTaskExecutor);
        // Requires API 24+ types.
        ConstraintTracker<NetworkState> networkStateTracker =
                new ConstraintTracker<NetworkState>(mContext, instantTaskExecutor) {
                    @Override
                    public NetworkState getInitialState() {
                        return new NetworkState(true, true, true, true);
                    }

                    @Override
                    public void startTracking() {
                    }

                    @Override
                    public void stopTracking() {
                    }
                };
        mStorageNotLowTracker = new FakeConstraintTracker(mContext, instantTaskExecutor);
        Trackers trackers = new Trackers(mContext, instantTaskExecutor,
                mBatteryChargingTracker, batteryNotLowTracker, networkStateTracker,
                mStorageNotLowTracker);
        Logger.setLogger(new Logger.LogcatLogger(Log.DEBUG));
        Configuration configuration = new Configuration.Builder()
                .setExecutor(new SynchronousExecutor())
                .build();
        when(mWorkManager.getWorkDatabase()).thenReturn(mDatabase);
        when(mWorkManager.getConfiguration()).thenReturn(configuration);
        when(mWorkManager.getWorkTaskExecutor()).thenReturn(instantTaskExecutor);
        when(mWorkManager.getTrackers()).thenReturn(trackers);
        Processor processor = new Processor(
                mContext,
                configuration,
                instantTaskExecutor,
                mDatabase,
                Collections.singletonList(scheduler));
        mSpyProcessor = spy(processor);

        mDispatcher =
                new CommandInterceptingSystemDispatcher(mContext, mSpyProcessor, mWorkManager);
        mDispatcher.setCompletedListener(completedListener);
        mSpyDispatcher = spy(mDispatcher);
    }

    @After
    public void tearDown() {
        mSpyDispatcher.onDestroy();
    }

    @Test
    public void testSchedule() throws InterruptedException {
        OneTimeWorkRequest work = new OneTimeWorkRequest.Builder(TestWorker.class)
                .setLastEnqueueTime(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
                .setInitialDelay(TimeUnit.HOURS.toMillis(1), TimeUnit.MILLISECONDS)
                .build();

        insertWork(work);
        String workSpecId = work.getStringId();
        final Intent intent = CommandHandler.createScheduleWorkIntent(mContext, workSpecId);
        mMainThreadExecutor.execute(
                new SystemAlarmDispatcher.AddRunnable(mSpyDispatcher, intent, START_ID));
        mLatch.await(TEST_TIMEOUT, TimeUnit.SECONDS);
        assertThat(mDatabase.systemIdInfoDao().getSystemIdInfo(work.getStringId()),
                is(notNullValue()));
    }

    @Test
    public void testSchedule_whenOriginalWorkDoesNotExist() throws InterruptedException {
        OneTimeWorkRequest work = new OneTimeWorkRequest.Builder(TestWorker.class)
                .setLastEnqueueTime(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
                .setInitialDelay(TimeUnit.HOURS.toMillis(1), TimeUnit.MILLISECONDS)
                .build();
        // DO NOT insert it into the DB.
        String workSpecId = work.getStringId();
        final Intent intent = CommandHandler.createScheduleWorkIntent(mContext, workSpecId);
        mMainThreadExecutor.execute(
                new SystemAlarmDispatcher.AddRunnable(mSpyDispatcher, intent, START_ID));
        mLatch.await(TEST_TIMEOUT, TimeUnit.SECONDS);
        assertThat(mDatabase.systemIdInfoDao().getSystemIdInfo(work.getStringId()),
                is(nullValue()));
    }

    @Test
    @RepeatRule.Repeat(times = 1)
    public void testDelayMet_success() throws InterruptedException {
        OneTimeWorkRequest work = new OneTimeWorkRequest.Builder(TestWorker.class)
                .setLastEnqueueTime(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
                .build();

        insertWork(work);
        String workSpecId = work.getStringId();
        final Intent intent = CommandHandler.createDelayMetIntent(mContext, workSpecId);
        mMainThreadExecutor.execute(
                new SystemAlarmDispatcher.AddRunnable(mSpyDispatcher, intent, START_ID));
        mLatch.await(TEST_TIMEOUT, TimeUnit.SECONDS);
        assertThat(mLatch.getCount(), is(0L));
        ArgumentCaptor<WorkRunId> captor = ArgumentCaptor.forClass(WorkRunId.class);
        verify(mSpyProcessor, times(1)).startWork(captor.capture());
        assertThat(captor.getValue().getWorkSpecId()).isEqualTo(workSpecId);
    }

    @Test
    public void testDelayMet_noWorkSpec() throws InterruptedException {
        OneTimeWorkRequest work = new OneTimeWorkRequest.Builder(TestWorker.class)
                .setLastEnqueueTime(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
                .build();

        // Not inserting the workSpec.
        String workSpecId = work.getStringId();
        final Intent intent = CommandHandler.createDelayMetIntent(mContext, workSpecId);
        mMainThreadExecutor.execute(
                new SystemAlarmDispatcher.AddRunnable(mSpyDispatcher, intent, START_ID));
        mLatch.await(TEST_TIMEOUT, TimeUnit.SECONDS);
        assertThat(mLatch.getCount(), is(0L));
        List<String> intentActions = mSpyDispatcher.getIntentActions();
        // Verify order of events
        assertThat(intentActions,
                IsIterableContainingInOrder.contains(
                        CommandHandler.ACTION_DELAY_MET,
                        CommandHandler.ACTION_STOP_WORK,
                        CommandHandler.ACTION_EXECUTION_COMPLETED));
        verify(mSpyProcessor, times(0)).startWork(any(WorkRunId.class));
    }

    @Test
    public void testDelayMet_withStop() throws InterruptedException {
        // SleepTestWorker sleeps for 5 seconds
        OneTimeWorkRequest work = new OneTimeWorkRequest.Builder(SleepTestWorker.class)
                .setLastEnqueueTime(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
                .setInitialDelay(TimeUnit.HOURS.toMillis(1), TimeUnit.MILLISECONDS)
                .build();

        insertWork(work);
        String workSpecId = work.getStringId();

        final Intent delayMet = CommandHandler.createDelayMetIntent(mContext, workSpecId);
        final Intent stopWork = CommandHandler.createStopWorkIntent(mContext, workSpecId);

        mMainThreadExecutor.execute(
                new SystemAlarmDispatcher.AddRunnable(mSpyDispatcher, delayMet, START_ID));

        mMainThreadExecutor.execute(
                new SystemAlarmDispatcher.AddRunnable(mSpyDispatcher, stopWork, START_ID));

        mLatch.await(TEST_TIMEOUT, TimeUnit.SECONDS);

        assertThat(mLatch.getCount(), is(0L));
        ArgumentCaptor<WorkRunId> captor = ArgumentCaptor.forClass(WorkRunId.class);
        verify(mSpyProcessor, times(1)).startWork(captor.capture());
        assertThat(captor.getValue().getWorkSpecId()).isEqualTo(workSpecId);

        ArgumentCaptor<WorkRunId> captorStop = ArgumentCaptor.forClass(WorkRunId.class);
        verify(mWorkManager, times(1)).stopWork(captorStop.capture());
        assertThat(captorStop.getValue().getWorkSpecId()).isEqualTo(workSpecId);
    }

    @Test
    public void testDelayMet_withStopWhenCancelled() throws InterruptedException {
        OneTimeWorkRequest work = new OneTimeWorkRequest.Builder(SleepTestWorker.class)
                .setLastEnqueueTime(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
                .build();

        insertWork(work);
        String workSpecId = work.getStringId();

        final Intent scheduleWork = CommandHandler.createDelayMetIntent(mContext, workSpecId);
        final Intent stopWork = CommandHandler.createStopWorkIntent(mContext, workSpecId);

        mMainThreadExecutor.execute(
                new SystemAlarmDispatcher.AddRunnable(mSpyDispatcher, scheduleWork, START_ID));

        mMainThreadExecutor.execute(
                new SystemAlarmDispatcher.AddRunnable(mSpyDispatcher, stopWork, START_ID));

        mLatch.await(TEST_TIMEOUT, TimeUnit.SECONDS);

        assertThat(mLatch.getCount(), is(0L));
        ArgumentCaptor<WorkRunId> captor = ArgumentCaptor.forClass(WorkRunId.class);
        verify(mSpyProcessor, times(1)).startWork(captor.capture());
        assertThat(captor.getValue().getWorkSpecId()).isEqualTo(workSpecId);

        ArgumentCaptor<WorkRunId> captorStop = ArgumentCaptor.forClass(WorkRunId.class);
        verify(mWorkManager, times(1)).stopWork(captorStop.capture());
        assertThat(captorStop.getValue().getWorkSpecId()).isEqualTo(workSpecId);
    }

    @Test
    public void testDelayMet_withAlwaysRetryWorker() throws InterruptedException {
        OneTimeWorkRequest work = new OneTimeWorkRequest.Builder(RetryWorker.class)
                .setLastEnqueueTime(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
                .build();

        insertWork(work);
        String workSpecId = work.getStringId();

        final Intent scheduleWork = CommandHandler.createDelayMetIntent(mContext, workSpecId);
        mMainThreadExecutor.execute(
                new SystemAlarmDispatcher.AddRunnable(mSpyDispatcher, scheduleWork, START_ID));
        mLatch.await(TEST_TIMEOUT, TimeUnit.SECONDS);

        assertThat(mLatch.getCount(), is(0L));
        ArgumentCaptor<WorkRunId> captor = ArgumentCaptor.forClass(WorkRunId.class);
        verify(mSpyProcessor, times(1)).startWork(captor.capture());
        assertThat(captor.getValue().getWorkSpecId()).isEqualTo(workSpecId);

        List<String> intentActions = mSpyDispatcher.getIntentActions();
        assertThat(intentActions,
                IsIterableContainingInOrder.contains(
                        CommandHandler.ACTION_DELAY_MET,
                        CommandHandler.ACTION_EXECUTION_COMPLETED,
                        CommandHandler.ACTION_SCHEDULE_WORK));

        WorkSpecDao workSpecDao = mDatabase.workSpecDao();
        WorkSpec workSpec = workSpecDao.getWorkSpec(work.getStringId());

        assertThat(workSpec.state, is(WorkInfo.State.ENQUEUED));
        // It should be scheduled
        assertThat(workSpec.scheduleRequestedAt, is(not(WorkSpec.SCHEDULE_NOT_REQUESTED_YET)));
    }

    @Test
    public void testSchedule_withConstraints() throws InterruptedException {
        mBatteryChargingTracker.setInitialState(true);
        OneTimeWorkRequest work = new OneTimeWorkRequest.Builder(TestWorker.class)
                .setLastEnqueueTime(
                        System.currentTimeMillis() + TimeUnit.HOURS.toMillis(1),
                        TimeUnit.MILLISECONDS)
                .setConstraints(new Constraints.Builder()
                        .setRequiresCharging(true)
                        .build())
                .build();

        insertWork(work);
        String workSpecId = work.getStringId();

        final Intent scheduleWork = CommandHandler.createScheduleWorkIntent(mContext, workSpecId);

        mMainThreadExecutor.execute(
                new SystemAlarmDispatcher.AddRunnable(mSpyDispatcher, scheduleWork, START_ID));

        mLatch.await(TEST_TIMEOUT, TimeUnit.SECONDS);
        assertThat(mLatch.getCount(), is(0L));
        // Should not call startWork, but schedule an alarm.
        verify(mSpyProcessor, times(0)).startWork(any(WorkRunId.class));
    }

    @Test
    public void testConstraintsChanged_withNoConstraints() throws InterruptedException {
        OneTimeWorkRequest work = new OneTimeWorkRequest.Builder(TestWorker.class)
                .setScheduleRequestedAt(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
                .setLastEnqueueTime(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
                .build();

        insertWork(work);
        final Intent constraintChanged = CommandHandler.createConstraintsChangedIntent(mContext);
        mMainThreadExecutor.execute(
                new SystemAlarmDispatcher.AddRunnable(mSpyDispatcher, constraintChanged, START_ID));

        mLatch.await(TEST_TIMEOUT, TimeUnit.SECONDS);
        assertThat(mLatch.getCount(), is(0L));
    }

    @LargeTest
    @Test
    public void testConstraintsChangedMarkedNotScheduled_withNoConstraints()
            throws InterruptedException {
        OneTimeWorkRequest work = new OneTimeWorkRequest.Builder(TestWorker.class)
                .setScheduleRequestedAt(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
                .setLastEnqueueTime(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
                .build();

        insertWork(work);
        String workSpecId = work.getStringId();
        final Intent constraintChanged = CommandHandler.createConstraintsChangedIntent(mContext);
        mMainThreadExecutor.execute(
                new SystemAlarmDispatcher.AddRunnable(mSpyDispatcher, constraintChanged, START_ID));

        mLatch.await(TEST_TIMEOUT, TimeUnit.SECONDS);
        assertThat(mLatch.getCount(), is(0L));
        ArgumentCaptor<WorkRunId> captor = ArgumentCaptor.forClass(WorkRunId.class);
        verify(mSpyProcessor, times(1)).startWork(captor.capture());
        assertThat(captor.getValue().getWorkSpecId()).isEqualTo(workSpecId);
    }

    @Test
    @LargeTest
    @RepeatRule.Repeat(times = 1)
    public void testDelayMet_withUnMetConstraint() throws InterruptedException {
        // fake BatteryCharging tracker says by default that it is not charging
        OneTimeWorkRequest work = new OneTimeWorkRequest.Builder(TestWorker.class)
                .setLastEnqueueTime(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
                .setConstraints(new Constraints.Builder()
                        .setRequiresCharging(true)
                        .build())
                .build();

        insertWork(work);

        Intent delayMet = CommandHandler.createDelayMetIntent(mContext, work.getStringId());
        mMainThreadExecutor.execute(
                new SystemAlarmDispatcher.AddRunnable(mSpyDispatcher, delayMet, START_ID));

        mLatch.await(TEST_TIMEOUT, TimeUnit.SECONDS);

        List<String> intentActions = mSpyDispatcher.getIntentActions();
        WorkSpecDao workSpecDao = mDatabase.workSpecDao();
        WorkSpec workSpec = workSpecDao.getWorkSpec(work.getStringId());

        assertThat(mLatch.getCount(), is(0L));

        // Verify order of events
        assertThat(intentActions,
                IsIterableContainingInOrder.contains(
                        CommandHandler.ACTION_DELAY_MET,
                        CommandHandler.ACTION_STOP_WORK,
                        CommandHandler.ACTION_EXECUTION_COMPLETED,
                        CommandHandler.ACTION_CONSTRAINTS_CHANGED));

        assertThat(workSpec.state, is(WorkInfo.State.ENQUEUED));
    }

    @Test
    @LargeTest
    @RepeatRule.Repeat(times = 1)
    public void testDelayMet_withPartiallyMetConstraint() throws InterruptedException {
        mStorageNotLowTracker.setInitialState(true);
        OneTimeWorkRequest work = new OneTimeWorkRequest.Builder(TestWorker.class)
                .setLastEnqueueTime(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
                .setConstraints(new Constraints.Builder()
                        .setRequiresStorageNotLow(true)
                        // fake BatteryCharging tracker says by default that it is not charging
                        .setRequiresCharging(true)
                        .build())
                .build();

        insertWork(work);

        Intent delayMet = CommandHandler.createDelayMetIntent(mContext, work.getStringId());
        mMainThreadExecutor.execute(
                new SystemAlarmDispatcher.AddRunnable(mSpyDispatcher, delayMet, START_ID));

        mLatch.await(TEST_TIMEOUT, TimeUnit.SECONDS);

        List<String> intentActions = mSpyDispatcher.getIntentActions();
        WorkSpecDao workSpecDao = mDatabase.workSpecDao();
        WorkSpec workSpec = workSpecDao.getWorkSpec(work.getStringId());

        assertThat(mLatch.getCount(), is(0L));

        // Verify order of events
        assertThat(intentActions,
                IsIterableContainingInOrder.contains(
                        CommandHandler.ACTION_DELAY_MET,
                        CommandHandler.ACTION_STOP_WORK,
                        CommandHandler.ACTION_EXECUTION_COMPLETED,
                        CommandHandler.ACTION_CONSTRAINTS_CHANGED));

        assertThat(workSpec.state, is(WorkInfo.State.ENQUEUED));
    }

    @Test
    public void testConstraintsChanged_withConstraint() throws InterruptedException {
        mBatteryChargingTracker.setInitialState(true);
        OneTimeWorkRequest work = new OneTimeWorkRequest.Builder(TestWorker.class)
                .setLastEnqueueTime(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
                .setConstraints(new Constraints.Builder()
                        .setRequiresCharging(true)
                        .build())
                .build();

        insertWork(work);
        final Intent constraintChanged = CommandHandler.createConstraintsChangedIntent(mContext);
        mMainThreadExecutor.execute(
                new SystemAlarmDispatcher.AddRunnable(mSpyDispatcher, constraintChanged, START_ID));
        mLatch.await(TEST_TIMEOUT, TimeUnit.SECONDS);
        assertThat(mLatch.getCount(), is(0L));
    }

    @Test
    public void testDelayMet_withMetConstraint() throws InterruptedException {
        mBatteryChargingTracker.setInitialState(true);
        OneTimeWorkRequest work = new OneTimeWorkRequest.Builder(TestWorker.class)
                .setLastEnqueueTime(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
                .setConstraints(new Constraints.Builder()
                        .setRequiresCharging(true)
                        .build())
                .build();

        insertWork(work);

        Intent delayMet = CommandHandler.createDelayMetIntent(mContext, work.getStringId());
        mMainThreadExecutor.execute(
                new SystemAlarmDispatcher.AddRunnable(mSpyDispatcher, delayMet, START_ID));

        mLatch.await(TEST_TIMEOUT, TimeUnit.SECONDS);
        List<String> intentActions = mSpyDispatcher.getIntentActions();
        WorkSpecDao workSpecDao = mDatabase.workSpecDao();
        WorkSpec workSpec = workSpecDao.getWorkSpec(work.getStringId());

        assertThat(mLatch.getCount(), is(0L));
        // Assert order of events
        assertThat(intentActions,
                IsIterableContainingInOrder.contains(
                        CommandHandler.ACTION_DELAY_MET,
                        CommandHandler.ACTION_EXECUTION_COMPLETED,
                        CommandHandler.ACTION_CONSTRAINTS_CHANGED));

        assertThat(workSpec.state, is(WorkInfo.State.SUCCEEDED));
    }

    @Test
    public void testReschedule() throws InterruptedException {
        // Use a mocked scheduler in this test.
        Scheduler scheduler = mock(Scheduler.class);
        doCallRealMethod().when(mWorkManager).rescheduleEligibleWork();
        when(mWorkManager.getApplicationContext()).thenReturn(mContext);
        when(mWorkManager.getSchedulers()).thenReturn(Collections.singletonList(scheduler));

        OneTimeWorkRequest failed = new OneTimeWorkRequest.Builder(TestWorker.class)
                .setLastEnqueueTime(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
                .setInitialState(WorkInfo.State.FAILED)
                .build();

        OneTimeWorkRequest succeeded = new OneTimeWorkRequest.Builder(TestWorker.class)
                .setLastEnqueueTime(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
                .setInitialState(WorkInfo.State.SUCCEEDED)
                .build();

        OneTimeWorkRequest noConstraints = new OneTimeWorkRequest.Builder(TestWorker.class)
                .setLastEnqueueTime(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
                .build();

        OneTimeWorkRequest workWithConstraints = new OneTimeWorkRequest.Builder(TestWorker.class)
                .setLastEnqueueTime(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
                .setConstraints(new Constraints.Builder()
                        .setRequiresCharging(true)
                        .build())
                .build();

        insertWork(failed);
        insertWork(succeeded);
        insertWork(noConstraints);
        insertWork(workWithConstraints);

        Intent reschedule = CommandHandler.createRescheduleIntent(mContext);
        mMainThreadExecutor.execute(
                new SystemAlarmDispatcher.AddRunnable(mSpyDispatcher, reschedule, START_ID));

        mLatch.await(TEST_TIMEOUT, TimeUnit.SECONDS);
        assertThat(mLatch.getCount(), is(0L));

        ArgumentCaptor<WorkSpec> captor = ArgumentCaptor.forClass(WorkSpec.class);
        verify(scheduler, times(1))
                .schedule(captor.capture());

        Set<String> capturedIds = new HashSet<>();
        List<WorkSpec> workSpecs = captor.getAllValues();
        for (WorkSpec workSpec : workSpecs) {
            capturedIds.add(workSpec.id);
        }

        assertThat(capturedIds.size(), is(2));
        assertThat(capturedIds.contains(noConstraints.getStringId()), is(true));
        assertThat(capturedIds.contains(workWithConstraints.getStringId()), is(true));
        assertThat(capturedIds.contains(failed.getStringId()), is(false));
        assertThat(capturedIds.contains(succeeded.getStringId()), is(false));
    }

    @Test
    public void testConstraintsChanged_withFutureWork() throws InterruptedException {
        mBatteryChargingTracker.setInitialState(true);
        // Use a mocked scheduler in this test.
        Scheduler scheduler = mock(Scheduler.class);
        doCallRealMethod().when(mWorkManager).rescheduleEligibleWork();
        when(mWorkManager.getApplicationContext()).thenReturn(mContext);
        when(mWorkManager.getSchedulers()).thenReturn(Collections.singletonList(scheduler));

        OneTimeWorkRequest failed = new OneTimeWorkRequest.Builder(TestWorker.class)
                .setLastEnqueueTime(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
                .setInitialState(WorkInfo.State.FAILED)
                .build();

        OneTimeWorkRequest succeeded = new OneTimeWorkRequest.Builder(TestWorker.class)
                .setLastEnqueueTime(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
                .setInitialState(WorkInfo.State.SUCCEEDED)
                .build();

        OneTimeWorkRequest noConstraints = new OneTimeWorkRequest.Builder(TestWorker.class)
                .setScheduleRequestedAt(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
                .setLastEnqueueTime(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
                .build();

        OneTimeWorkRequest workWithConstraints = new OneTimeWorkRequest.Builder(TestWorker.class)
                .setLastEnqueueTime(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
                .setScheduleRequestedAt(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
                .setConstraints(new Constraints.Builder()
                        .setRequiresCharging(true)
                        .build())
                .build();

        long hourFromNow = System.currentTimeMillis() + TimeUnit.HOURS.toMillis(1);
        OneTimeWorkRequest workInTheFuture = new OneTimeWorkRequest.Builder(TestWorker.class)
                .setLastEnqueueTime(hourFromNow, TimeUnit.MILLISECONDS)
                .setScheduleRequestedAt(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
                .build();

        insertWork(failed);
        insertWork(succeeded);
        insertWork(noConstraints);
        insertWork(workWithConstraints);
        insertWork(workInTheFuture);

        Intent reschedule = CommandHandler.createConstraintsChangedIntent(mContext);
        mMainThreadExecutor.execute(
                new SystemAlarmDispatcher.AddRunnable(mSpyDispatcher, reschedule, START_ID));

        mLatch.await(TEST_TIMEOUT, TimeUnit.SECONDS);
        assertThat(mLatch.getCount(), is(0L));

        List<String> intentActions = mSpyDispatcher.getIntentActions();

        // Ordering of events can change slightly due to timing, so this test can become flaky if we
        // don't compare the relative order of things.

        assertThat(intentActions.size(), is(6));
        // The last action must be a constraints changed event to update proxies.
        assertThat(intentActions.get(5), is(CommandHandler.ACTION_CONSTRAINTS_CHANGED));

        int numConstraintsChanged = 0;
        int numDelayMet = 0;
        int numExecutionCompleted = 0;
        for (int i = 0; i < intentActions.size() - 1; ++i) {
            switch (intentActions.get(i)) {
                case CommandHandler.ACTION_CONSTRAINTS_CHANGED: {
                    ++numConstraintsChanged;
                    break;
                }

                case CommandHandler.ACTION_DELAY_MET: {
                    ++numDelayMet;
                    break;
                }

                case CommandHandler.ACTION_EXECUTION_COMPLETED: {
                    ++numExecutionCompleted;
                    assertThat(numConstraintsChanged, greaterThan(0));
                    assertThat(numDelayMet, greaterThanOrEqualTo(numExecutionCompleted));
                    break;
                }
            }
        }
        assertThat(numExecutionCompleted, is(2));
    }

    @Test
    @LargeTest
    @RepeatRule.Repeat(times = 1)
    public void testDelayMet_withUnMetConstraintShouldNotCrashOnDestroy()
            throws InterruptedException {
        OneTimeWorkRequest work = new OneTimeWorkRequest.Builder(TestWorker.class)
                .setLastEnqueueTime(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
                .setConstraints(new Constraints.Builder()
                        // fake BatteryCharging tracker says by default that it is not charging
                        .setRequiresCharging(true)
                        .build())
                .build();

        insertWork(work);

        Intent delayMet = CommandHandler.createDelayMetIntent(mContext, work.getStringId());
        mMainThreadExecutor.execute(
                new SystemAlarmDispatcher.AddRunnable(mSpyDispatcher, delayMet, START_ID));

        mLatch.await(TEST_TIMEOUT, TimeUnit.SECONDS);
        assertThat(mLatch.getCount(), is(0L));

        // Should not crash after we destroy the dispatcher
        mDispatcher.onDestroy();
        mBatteryChargingTracker.setState(true);
    }

    @Test
    public void tearDownTest() {
        mDispatcher.onDestroy();
    }

    // Marking it public for mocking
    public static class CommandInterceptingSystemDispatcher extends SystemAlarmDispatcher {
        private final List<Intent> mCommands;
        private final Map<String, Integer> mActionCount;

        CommandInterceptingSystemDispatcher(@NonNull Context context,
                @Nullable Processor processor,
                @Nullable WorkManagerImpl workManager) {
            super(context, processor, workManager);
            mCommands = new ArrayList<>();
            mActionCount = new HashMap<>();
        }

        @Override
        public boolean add(@NonNull Intent intent, int startId) {
            boolean isAdded = super.add(intent, startId);
            if (isAdded) {
                update(intent);
            }
            return isAdded;
        }

        List<String> getIntentActions() {
            List<String> intentActions = new ArrayList<>(mCommands.size());
            for (Intent intent : mCommands) {
                intentActions.add(intent.getAction());
            }
            return intentActions;
        }

        private void update(Intent intent) {
            String action = intent.getAction();
            Integer count = mActionCount.get(intent.getAction());
            int incremented = count != null ? count + 1 : 1;
            mActionCount.put(action, incremented);
            mCommands.add(intent);
        }
    }

    private static final class FakeConstraintTracker extends ConstraintTracker<Boolean> {
        private boolean mInitialState = false;

        FakeConstraintTracker(@NonNull Context context,
                @NonNull TaskExecutor taskExecutor) {
            super(context, taskExecutor);
        }

        private void setInitialState(boolean initialState) {
            mInitialState = initialState;
        }

        @Override
        public Boolean getInitialState() {
            return mInitialState;
        }

        @Override
        public void startTracking() {
        }

        @Override
        public void stopTracking() {
        }
    }
}
