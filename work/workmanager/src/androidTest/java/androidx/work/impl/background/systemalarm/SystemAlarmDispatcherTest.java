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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import androidx.test.InstrumentationRegistry;
import androidx.test.filters.LargeTest;
import androidx.test.filters.MediumTest;
import androidx.test.runner.AndroidJUnit4;
import androidx.work.Configuration;
import androidx.work.Constraints;
import androidx.work.DatabaseTest;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.impl.Processor;
import androidx.work.impl.Scheduler;
import androidx.work.impl.WorkManagerImpl;
import androidx.work.impl.constraints.trackers.BatteryChargingTracker;
import androidx.work.impl.constraints.trackers.BatteryNotLowTracker;
import androidx.work.impl.constraints.trackers.NetworkStateTracker;
import androidx.work.impl.constraints.trackers.StorageNotLowTracker;
import androidx.work.impl.constraints.trackers.Trackers;
import androidx.work.impl.model.WorkSpec;
import androidx.work.impl.model.WorkSpecDao;
import androidx.work.impl.utils.RepeatRule;
import androidx.work.impl.utils.SynchronousExecutor;
import androidx.work.impl.utils.taskexecutor.InstantWorkTaskExecutor;
import androidx.work.impl.utils.taskexecutor.TaskExecutor;
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
import java.util.concurrent.TimeUnit;

@RunWith(AndroidJUnit4.class)
@MediumTest
public class SystemAlarmDispatcherTest extends DatabaseTest {

    @Rule
    public RepeatRule mRepeatRule = new RepeatRule();

    private static final int START_ID = 0;
    // Test timeout in seconds - this needs to be longer than SleepTestWorker.SLEEP_DURATION
    private static final int TEST_TIMEOUT = 6;

    private Context mContext;
    private Scheduler mScheduler;
    private WorkManagerImpl mWorkManager;
    private Configuration mConfiguration;
    private Processor mProcessor;
    private Processor mSpyProcessor;
    private CommandInterceptingSystemDispatcher mDispatcher;
    private CommandInterceptingSystemDispatcher mSpyDispatcher;
    private SystemAlarmDispatcher.CommandsCompletedListener mCompletedListener;
    private CountDownLatch mLatch;

    private Trackers mTracker;
    private BatteryChargingTracker mBatteryChargingTracker;
    private BatteryNotLowTracker mBatteryNotLowTracker;
    private NetworkStateTracker mNetworkStateTracker;
    private StorageNotLowTracker mStorageNotLowTracker;

    @Before
    public void setUp() {
        mContext = InstrumentationRegistry.getTargetContext().getApplicationContext();
        mScheduler = mock(Scheduler.class);
        mWorkManager = mock(WorkManagerImpl.class);
        mLatch = new CountDownLatch(1);
        mCompletedListener = new SystemAlarmDispatcher.CommandsCompletedListener() {
            @Override
            public void onAllCommandsCompleted() {
                mLatch.countDown();
            }
        };
        mConfiguration = new Configuration.Builder()
                .setExecutor(new SynchronousExecutor())
                .setMinimumLoggingLevel(Log.VERBOSE)
                .build();
        when(mWorkManager.getWorkDatabase()).thenReturn(mDatabase);
        when(mWorkManager.getConfiguration()).thenReturn(mConfiguration);
        TaskExecutor instantTaskExecutor = new InstantWorkTaskExecutor();
        when(mWorkManager.getWorkTaskExecutor()).thenReturn(instantTaskExecutor);
        mProcessor = new Processor(
                mContext,
                mConfiguration,
                new InstantWorkTaskExecutor(),
                mDatabase,
                Collections.singletonList(mScheduler));
        mSpyProcessor = spy(mProcessor);

        mDispatcher =
                new CommandInterceptingSystemDispatcher(mContext, mSpyProcessor, mWorkManager);
        mDispatcher.setCompletedListener(mCompletedListener);
        mSpyDispatcher = spy(mDispatcher);

        mBatteryChargingTracker = spy(new BatteryChargingTracker(mContext));
        mBatteryNotLowTracker = spy(new BatteryNotLowTracker(mContext));
        // Requires API 24+ types.
        mNetworkStateTracker = mock(NetworkStateTracker.class);
        mStorageNotLowTracker = spy(new StorageNotLowTracker(mContext));
        mTracker = mock(Trackers.class);

        when(mTracker.getBatteryChargingTracker()).thenReturn(mBatteryChargingTracker);
        when(mTracker.getBatteryNotLowTracker()).thenReturn(mBatteryNotLowTracker);
        when(mTracker.getNetworkStateTracker()).thenReturn(mNetworkStateTracker);
        when(mTracker.getStorageNotLowTracker()).thenReturn(mStorageNotLowTracker);

        // Override Trackers being used by WorkConstraintsProxy
        Trackers.setInstance(mTracker);
    }

    @After
    public void tearDown() {
        mSpyDispatcher.onDestroy();
    }

    @Test
    public void testSchedule() throws InterruptedException {
        OneTimeWorkRequest work = new OneTimeWorkRequest.Builder(TestWorker.class)
                .setPeriodStartTime(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
                .setInitialDelay(TimeUnit.HOURS.toMillis(1), TimeUnit.MILLISECONDS)
                .build();

        insertWork(work);
        String workSpecId = work.getStringId();
        final Intent intent = CommandHandler.createScheduleWorkIntent(mContext, workSpecId);
        mSpyDispatcher.postOnMainThread(
                new SystemAlarmDispatcher.AddRunnable(mSpyDispatcher, intent, START_ID));
        mLatch.await(TEST_TIMEOUT, TimeUnit.SECONDS);
        assertThat(mDatabase.systemIdInfoDao().getSystemIdInfo(work.getStringId()),
                is(notNullValue()));
    }

    @Test
    public void testSchedule_whenOriginalWorkDoesNotExist() throws InterruptedException {
        OneTimeWorkRequest work = new OneTimeWorkRequest.Builder(TestWorker.class)
                .setPeriodStartTime(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
                .setInitialDelay(TimeUnit.HOURS.toMillis(1), TimeUnit.MILLISECONDS)
                .build();
        // DO NOT insert it into the DB.
        String workSpecId = work.getStringId();
        final Intent intent = CommandHandler.createScheduleWorkIntent(mContext, workSpecId);
        mSpyDispatcher.postOnMainThread(
                new SystemAlarmDispatcher.AddRunnable(mSpyDispatcher, intent, START_ID));
        mLatch.await(TEST_TIMEOUT, TimeUnit.SECONDS);
        assertThat(mDatabase.systemIdInfoDao().getSystemIdInfo(work.getStringId()),
                is(nullValue()));
    }

    @Test
    @RepeatRule.Repeat(times = 1)
    public void testDelayMet_success() throws InterruptedException {
        OneTimeWorkRequest work = new OneTimeWorkRequest.Builder(TestWorker.class)
                .setPeriodStartTime(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
                .build();

        insertWork(work);
        String workSpecId = work.getStringId();
        final Intent intent = CommandHandler.createDelayMetIntent(mContext, workSpecId);
        mSpyDispatcher.postOnMainThread(
                new SystemAlarmDispatcher.AddRunnable(mSpyDispatcher, intent, START_ID));
        mLatch.await(TEST_TIMEOUT, TimeUnit.SECONDS);
        assertThat(mLatch.getCount(), is(0L));
        verify(mSpyProcessor, times(1)).startWork(workSpecId);
    }

    @Test
    public void testDelayMet_noWorkSpec() throws InterruptedException {
        OneTimeWorkRequest work = new OneTimeWorkRequest.Builder(TestWorker.class)
                .setPeriodStartTime(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
                .build();

        // Not inserting the workSpec.
        String workSpecId = work.getStringId();
        final Intent intent = CommandHandler.createDelayMetIntent(mContext, workSpecId);
        mSpyDispatcher.postOnMainThread(
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
        verify(mSpyProcessor, times(0)).startWork(workSpecId);
    }

    @Test
    public void testDelayMet_withStop() throws InterruptedException {
        // SleepTestWorker sleeps for 5 seconds
        OneTimeWorkRequest work = new OneTimeWorkRequest.Builder(SleepTestWorker.class)
                .setPeriodStartTime(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
                .setInitialDelay(TimeUnit.HOURS.toMillis(1), TimeUnit.MILLISECONDS)
                .build();

        insertWork(work);
        String workSpecId = work.getStringId();

        final Intent delayMet = CommandHandler.createDelayMetIntent(mContext, workSpecId);
        final Intent stopWork = CommandHandler.createStopWorkIntent(mContext, workSpecId);

        mSpyDispatcher.postOnMainThread(
                new SystemAlarmDispatcher.AddRunnable(mSpyDispatcher, delayMet, START_ID));

        mSpyDispatcher.postOnMainThread(
                new SystemAlarmDispatcher.AddRunnable(mSpyDispatcher, stopWork, START_ID));

        mLatch.await(TEST_TIMEOUT, TimeUnit.SECONDS);

        assertThat(mLatch.getCount(), is(0L));
        verify(mSpyProcessor, times(1)).startWork(workSpecId);
        verify(mWorkManager, times(1)).stopWork(workSpecId);
    }

    @Test
    public void testDelayMet_withStopWhenCancelled() throws InterruptedException {
        OneTimeWorkRequest work = new OneTimeWorkRequest.Builder(SleepTestWorker.class)
                .setPeriodStartTime(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
                .build();

        insertWork(work);
        String workSpecId = work.getStringId();

        final Intent scheduleWork = CommandHandler.createDelayMetIntent(mContext, workSpecId);
        final Intent stopWork = CommandHandler.createStopWorkIntent(mContext, workSpecId);

        mSpyDispatcher.postOnMainThread(
                new SystemAlarmDispatcher.AddRunnable(mSpyDispatcher, scheduleWork, START_ID));

        mSpyDispatcher.postOnMainThread(
                new SystemAlarmDispatcher.AddRunnable(mSpyDispatcher, stopWork, START_ID));

        mLatch.await(TEST_TIMEOUT, TimeUnit.SECONDS);

        assertThat(mLatch.getCount(), is(0L));
        verify(mSpyProcessor, times(1)).startWork(workSpecId);
        verify(mWorkManager, times(1)).stopWork(workSpecId);
    }

    @Test
    public void testSchedule_withConstraints() throws InterruptedException {
        when(mBatteryChargingTracker.getInitialState()).thenReturn(true);
        OneTimeWorkRequest work = new OneTimeWorkRequest.Builder(TestWorker.class)
                .setPeriodStartTime(
                        System.currentTimeMillis() + TimeUnit.HOURS.toMillis(1),
                        TimeUnit.MILLISECONDS)
                .setConstraints(new Constraints.Builder()
                        .setRequiresCharging(true)
                        .build())
                .build();

        insertWork(work);
        String workSpecId = work.getStringId();

        final Intent scheduleWork = CommandHandler.createScheduleWorkIntent(mContext, workSpecId);

        mSpyDispatcher.postOnMainThread(
                new SystemAlarmDispatcher.AddRunnable(mSpyDispatcher, scheduleWork, START_ID));

        mLatch.await(TEST_TIMEOUT, TimeUnit.SECONDS);
        assertThat(mLatch.getCount(), is(0L));
        // Should not call startWork, but schedule an alarm.
        verify(mSpyProcessor, times(0)).startWork(workSpecId);
    }

    @Test
    public void testConstraintsChanged_withNoConstraints() throws InterruptedException {
        OneTimeWorkRequest work = new OneTimeWorkRequest.Builder(TestWorker.class)
                .setScheduleRequestedAt(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
                .setPeriodStartTime(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
                .build();

        insertWork(work);
        final Intent constraintChanged = CommandHandler.createConstraintsChangedIntent(mContext);
        mSpyDispatcher.postOnMainThread(
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
                .setPeriodStartTime(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
                .build();

        insertWork(work);
        String workSpecId = work.getStringId();
        final Intent constraintChanged = CommandHandler.createConstraintsChangedIntent(mContext);
        mSpyDispatcher.postOnMainThread(
                new SystemAlarmDispatcher.AddRunnable(mSpyDispatcher, constraintChanged, START_ID));

        mLatch.await(TEST_TIMEOUT, TimeUnit.SECONDS);
        assertThat(mLatch.getCount(), is(0L));
        verify(mSpyProcessor, times(0)).startWork(workSpecId);
    }

    @Test
    @LargeTest
    @RepeatRule.Repeat(times = 1)
    public void testDelayMet_withUnMetConstraint() throws InterruptedException {
        when(mBatteryChargingTracker.getInitialState()).thenReturn(false);
        OneTimeWorkRequest work = new OneTimeWorkRequest.Builder(TestWorker.class)
                .setPeriodStartTime(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
                .setConstraints(new Constraints.Builder()
                        .setRequiresCharging(true)
                        .build())
                .build();

        insertWork(work);

        Intent delayMet = CommandHandler.createDelayMetIntent(mContext, work.getStringId());
        mSpyDispatcher.postOnMainThread(
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
        when(mBatteryChargingTracker.getInitialState()).thenReturn(true);
        OneTimeWorkRequest work = new OneTimeWorkRequest.Builder(TestWorker.class)
                .setPeriodStartTime(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
                .setConstraints(new Constraints.Builder()
                        .setRequiresCharging(true)
                        .build())
                .build();

        insertWork(work);
        final Intent constraintChanged = CommandHandler.createConstraintsChangedIntent(mContext);
        mSpyDispatcher.postOnMainThread(
                new SystemAlarmDispatcher.AddRunnable(mSpyDispatcher, constraintChanged, START_ID));
        mLatch.await(TEST_TIMEOUT, TimeUnit.SECONDS);
        assertThat(mLatch.getCount(), is(0L));
    }

    @Test
    public void testDelayMet_withMetConstraint() throws InterruptedException {
        when(mBatteryChargingTracker.getInitialState()).thenReturn(true);
        OneTimeWorkRequest work = new OneTimeWorkRequest.Builder(TestWorker.class)
                .setPeriodStartTime(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
                .setConstraints(new Constraints.Builder()
                        .setRequiresCharging(true)
                        .build())
                .build();

        insertWork(work);

        Intent delayMet = CommandHandler.createDelayMetIntent(mContext, work.getStringId());
        mSpyDispatcher.postOnMainThread(
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
                .setPeriodStartTime(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
                .setInitialState(WorkInfo.State.FAILED)
                .build();

        OneTimeWorkRequest succeeded = new OneTimeWorkRequest.Builder(TestWorker.class)
                .setPeriodStartTime(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
                .setInitialState(WorkInfo.State.SUCCEEDED)
                .build();

        OneTimeWorkRequest noConstraints = new OneTimeWorkRequest.Builder(TestWorker.class)
                .setPeriodStartTime(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
                .build();

        OneTimeWorkRequest workWithConstraints = new OneTimeWorkRequest.Builder(TestWorker.class)
                .setPeriodStartTime(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
                .setConstraints(new Constraints.Builder()
                        .setRequiresCharging(true)
                        .build())
                .build();

        insertWork(failed);
        insertWork(succeeded);
        insertWork(noConstraints);
        insertWork(workWithConstraints);

        Intent reschedule = CommandHandler.createRescheduleIntent(mContext);
        mSpyDispatcher.postOnMainThread(
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
        // Use a mocked scheduler in this test.
        Scheduler scheduler = mock(Scheduler.class);
        doCallRealMethod().when(mWorkManager).rescheduleEligibleWork();
        when(mWorkManager.getApplicationContext()).thenReturn(mContext);
        when(mWorkManager.getSchedulers()).thenReturn(Collections.singletonList(scheduler));

        OneTimeWorkRequest failed = new OneTimeWorkRequest.Builder(TestWorker.class)
                .setPeriodStartTime(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
                .setInitialState(WorkInfo.State.FAILED)
                .build();

        OneTimeWorkRequest succeeded = new OneTimeWorkRequest.Builder(TestWorker.class)
                .setPeriodStartTime(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
                .setInitialState(WorkInfo.State.SUCCEEDED)
                .build();

        OneTimeWorkRequest noConstraints = new OneTimeWorkRequest.Builder(TestWorker.class)
                .setPeriodStartTime(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
                .build();

        OneTimeWorkRequest workWithConstraints = new OneTimeWorkRequest.Builder(TestWorker.class)
                .setPeriodStartTime(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
                .setConstraints(new Constraints.Builder()
                        .setRequiresCharging(true)
                        .build())
                .build();

        long hourFromNow = System.currentTimeMillis() + TimeUnit.HOURS.toMillis(1);
        OneTimeWorkRequest workInTheFuture = new OneTimeWorkRequest.Builder(TestWorker.class)
                .setPeriodStartTime(hourFromNow, TimeUnit.MILLISECONDS)
                .build();

        insertWork(failed);
        insertWork(succeeded);
        insertWork(noConstraints);
        insertWork(workWithConstraints);
        insertWork(workInTheFuture);

        Intent reschedule = CommandHandler.createConstraintsChangedIntent(mContext);
        mSpyDispatcher.postOnMainThread(
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
}
