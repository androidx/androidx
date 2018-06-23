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
import static org.hamcrest.MatcherAssert.assertThat;
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
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.LargeTest;
import android.support.test.filters.MediumTest;
import android.support.test.runner.AndroidJUnit4;

import androidx.work.Configuration;
import androidx.work.Constraints;
import androidx.work.DatabaseTest;
import androidx.work.OneTimeWorkRequest;
import androidx.work.State;
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
import androidx.work.worker.SleepTestWorker;
import androidx.work.worker.TestWorker;

import org.hamcrest.collection.IsIterableContainingInOrder;
import org.junit.After;
import org.junit.Before;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@RunWith(AndroidJUnit4.class)
@MediumTest
public class SystemAlarmDispatcherTest extends DatabaseTest {

    private static final int START_ID = 0;
    // Test timeout in seconds - this needs to be longer than SleepTestWorker.SLEEP_DURATION
    private static final int TEST_TIMEOUT = 6;

    private Context mContext;
    private Scheduler mScheduler;
    private WorkManagerImpl mWorkManager;
    private Configuration mConfiguration;
    private ExecutorService mExecutorService;
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

        mConfiguration = new Configuration.Builder().build();
        when(mWorkManager.getWorkDatabase()).thenReturn(mDatabase);
        when(mWorkManager.getConfiguration()).thenReturn(mConfiguration);
        mExecutorService = Executors.newSingleThreadExecutor();
        mProcessor = new Processor(
                mContext,
                mConfiguration,
                mDatabase,
                Collections.singletonList(mScheduler),
                // simulate real world use-case
                mExecutorService);
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
        mExecutorService.shutdownNow();
        try {
            mExecutorService.awaitTermination(1, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            // Do nothing.
        }
        mSpyDispatcher.onDestroy();
    }

    @Test
    public void testSchedule() throws InterruptedException {
        OneTimeWorkRequest work = new OneTimeWorkRequest.Builder(TestWorker.class)
                .setPeriodStartTime(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
                .setInitialDelay(TimeUnit.HOURS.toMillis(1), TimeUnit.MILLISECONDS).build();

        insertWork(work);
        String workSpecId = work.getStringId();
        final Intent intent = CommandHandler.createScheduleWorkIntent(mContext, workSpecId);
        mSpyDispatcher.postOnMainThread(
                new SystemAlarmDispatcher.AddRunnable(mSpyDispatcher, intent, START_ID));
        mLatch.await(TEST_TIMEOUT, TimeUnit.SECONDS);
    }

    @Test
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
    @LargeTest
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

        List<String> intentActions = intentActionsFor(mSpyDispatcher.getCommands());
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

        assertThat(workSpec.state, is(State.ENQUEUED));
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

        List<String> intentActions = intentActionsFor(mSpyDispatcher.getCommands());
        WorkSpecDao workSpecDao = mDatabase.workSpecDao();
        WorkSpec workSpec = workSpecDao.getWorkSpec(work.getStringId());

        assertThat(mLatch.getCount(), is(0L));
        // Assert order of events
        assertThat(intentActions,
                IsIterableContainingInOrder.contains(
                        CommandHandler.ACTION_DELAY_MET,
                        CommandHandler.ACTION_EXECUTION_COMPLETED,
                        CommandHandler.ACTION_CONSTRAINTS_CHANGED));

        assertThat(workSpec.state, is(State.SUCCEEDED));
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
                .setInitialState(State.FAILED)
                .build();

        OneTimeWorkRequest succeeded = new OneTimeWorkRequest.Builder(TestWorker.class)
                .setPeriodStartTime(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
                .setInitialState(State.SUCCEEDED)
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

    private static List<String> intentActionsFor(@NonNull List<Intent> intents) {
        List<String> intentActions = new ArrayList<>(intents.size());
        for (Intent intent : intents) {
            intentActions.add(intent.getAction());
        }
        return intentActions;
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

        private void update(Intent intent) {
            String action = intent.getAction();
            Integer count = mActionCount.get(intent.getAction());
            int incremented = count != null ? count + 1 : 1;
            mActionCount.put(action, incremented);
            mCommands.add(intent);
        }

        Map<String, Integer> getActionCount() {
            return mActionCount;
        }

        List<Intent> getCommands() {
            return mCommands;
        }
    }
}
