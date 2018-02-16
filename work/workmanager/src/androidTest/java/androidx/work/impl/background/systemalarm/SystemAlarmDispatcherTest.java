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
import android.support.test.filters.MediumTest;
import android.support.test.runner.AndroidJUnit4;

import org.hamcrest.collection.IsIterableContainingInOrder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import androidx.work.Constraints;
import androidx.work.DatabaseTest;
import androidx.work.State;
import androidx.work.Work;
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

@RunWith(AndroidJUnit4.class)
@MediumTest
public class SystemAlarmDispatcherTest extends DatabaseTest {

    private static final int START_ID = 0;
    // Test timeout in seconds - this needs to be longer than SleepTestWorker.SLEEP_DURATION
    private static final int TEST_TIMEOUT = 6;

    private Context mContext;
    private Scheduler mScheduler;
    private WorkManagerImpl mWorkManager;
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

        when(mWorkManager.getWorkDatabase()).thenReturn(mDatabase);
        mProcessor = new Processor(
                mContext,
                mDatabase,
                Collections.singletonList(mScheduler),
                // simulate real world use-case
                Executors.newSingleThreadExecutor());
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
        Work work = new Work.Builder(TestWorker.class)
                .withPeriodStartTime(System.currentTimeMillis())
                .withInitialDelay(TimeUnit.HOURS.toMillis(1)).build();

        insertWork(work);
        String workSpecId = work.getId();
        final Intent intent = CommandHandler.createScheduleWorkIntent(mContext, workSpecId);
        mSpyDispatcher.postOnMainThread(
                new SystemAlarmDispatcher.AddRunnable(mSpyDispatcher, intent, START_ID));
        mLatch.await(TEST_TIMEOUT, TimeUnit.SECONDS);
        assertThat(mLatch.getCount(), is(0L));
    }

    @Test
    public void testDelayMet_success() throws InterruptedException {
        Work work = new Work.Builder(TestWorker.class)
                .withPeriodStartTime(System.currentTimeMillis())
                .build();

        insertWork(work);
        String workSpecId = work.getId();
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
        Work work = new Work.Builder(SleepTestWorker.class)
                .withPeriodStartTime(System.currentTimeMillis())
                .withInitialDelay(TimeUnit.HOURS.toMillis(1))
                .build();

        insertWork(work);
        String workSpecId = work.getId();

        final Intent delayMet = CommandHandler.createDelayMetIntent(mContext, workSpecId);
        final Intent stopWork = CommandHandler.createStopWorkIntent(mContext, workSpecId);

        mSpyDispatcher.postOnMainThread(
                new SystemAlarmDispatcher.AddRunnable(mSpyDispatcher, delayMet, START_ID));

        mSpyDispatcher.postOnMainThread(
                new SystemAlarmDispatcher.AddRunnable(mSpyDispatcher, stopWork, START_ID));

        mLatch.await(TEST_TIMEOUT, TimeUnit.SECONDS);

        assertThat(mLatch.getCount(), is(0L));
        verify(mSpyProcessor, times(1)).startWork(workSpecId);
        verify(mSpyProcessor, times(1)).stopWork(workSpecId, true);
    }

    @Test
    public void testSchedule_withStopWhenCancelled() throws InterruptedException {
        Work work = new Work.Builder(SleepTestWorker.class)
                .withPeriodStartTime(System.currentTimeMillis())
                .build();

        insertWork(work);
        String workSpecId = work.getId();

        when(mSpyProcessor.isCancelled(workSpecId)).thenReturn(true);

        final Intent scheduleWork = CommandHandler.createScheduleWorkIntent(mContext, workSpecId);
        final Intent stopWork = CommandHandler.createStopWorkIntent(mContext, workSpecId);

        mSpyDispatcher.postOnMainThread(
                new SystemAlarmDispatcher.AddRunnable(mSpyDispatcher, scheduleWork, START_ID));

        mSpyDispatcher.postOnMainThread(
                new SystemAlarmDispatcher.AddRunnable(mSpyDispatcher, stopWork, START_ID));


        mLatch.await(TEST_TIMEOUT, TimeUnit.SECONDS);

        assertThat(mLatch.getCount(), is(0L));
        verify(mSpyProcessor, times(1)).startWork(workSpecId);
        verify(mSpyProcessor, times(1)).stopWork(workSpecId, true);
    }

    @Test
    public void testSchedule_withConstraints() throws InterruptedException {
        when(mBatteryChargingTracker.getInitialState()).thenReturn(true);
        Work work = new Work.Builder(TestWorker.class)
                .withPeriodStartTime(System.currentTimeMillis() + TimeUnit.HOURS.toMillis(1))
                .withConstraints(new Constraints.Builder()
                        .setRequiresCharging(true)
                        .build())
                .build();

        insertWork(work);
        String workSpecId = work.getId();

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
        Work work = new Work.Builder(TestWorker.class)
                .withPeriodStartTime(System.currentTimeMillis())
                .build();

        insertWork(work);
        String workSpecId = work.getId();
        final Intent constraintChanged = CommandHandler.createConstraintsChangedIntent(mContext);
        mSpyDispatcher.postOnMainThread(
                new SystemAlarmDispatcher.AddRunnable(mSpyDispatcher, constraintChanged, START_ID));

        mLatch.await(TEST_TIMEOUT, TimeUnit.SECONDS);

        assertThat(mLatch.getCount(), is(0L));
        verify(mSpyProcessor, times(1)).startWork(workSpecId);
    }

    @Test
    public void testConstraintsChanged_withConstraint() throws InterruptedException {
        when(mBatteryChargingTracker.getInitialState()).thenReturn(true);
        Work work = new Work.Builder(TestWorker.class)
                .withPeriodStartTime(System.currentTimeMillis())
                .withConstraints(new Constraints.Builder()
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
    public void testDelayMet_withUnMetConstraint() throws InterruptedException {
        when(mBatteryChargingTracker.getInitialState()).thenReturn(false);
        Work work = new Work.Builder(TestWorker.class)
                .withPeriodStartTime(System.currentTimeMillis())
                .withConstraints(new Constraints.Builder()
                        .setRequiresCharging(true)
                        .build())
                .build();

        insertWork(work);

        Intent delayMet = CommandHandler.createDelayMetIntent(mContext, work.getId());
        mSpyDispatcher.postOnMainThread(
                new SystemAlarmDispatcher.AddRunnable(mSpyDispatcher, delayMet, START_ID));

        mLatch.await(TEST_TIMEOUT, TimeUnit.SECONDS);

        List<String> intentActions = intentActionsFor(mSpyDispatcher.getCommands());
        WorkSpecDao workSpecDao = mDatabase.workSpecDao();
        WorkSpec workSpec = workSpecDao.getWorkSpec(work.getId());

        assertThat(mLatch.getCount(), is(0L));
        // Verify order of events
        assertThat(intentActions,
                IsIterableContainingInOrder.contains(
                        CommandHandler.ACTION_DELAY_MET,
                        CommandHandler.ACTION_STOP_WORK,
                        CommandHandler.ACTION_CONSTRAINTS_CHANGED));

        assertThat(workSpec.getState(), is(State.ENQUEUED));
    }

    @Test
    public void testDelayMet_withMetConstraint() throws InterruptedException {
        when(mBatteryChargingTracker.getInitialState()).thenReturn(true);
        Work work = new Work.Builder(TestWorker.class)
                .withPeriodStartTime(System.currentTimeMillis())
                .withConstraints(new Constraints.Builder()
                        .setRequiresCharging(true)
                        .build())
                .build();

        insertWork(work);

        Intent delayMet = CommandHandler.createDelayMetIntent(mContext, work.getId());
        mSpyDispatcher.postOnMainThread(
                new SystemAlarmDispatcher.AddRunnable(mSpyDispatcher, delayMet, START_ID));

        mLatch.await(TEST_TIMEOUT, TimeUnit.SECONDS);

        List<String> intentActions = intentActionsFor(mSpyDispatcher.getCommands());
        WorkSpecDao workSpecDao = mDatabase.workSpecDao();
        WorkSpec workSpec = workSpecDao.getWorkSpec(work.getId());


        assertThat(mLatch.getCount(), is(0L));
        // Assert order of events
        assertThat(intentActions,
                IsIterableContainingInOrder.contains(
                        CommandHandler.ACTION_DELAY_MET,
                        CommandHandler.ACTION_CONSTRAINTS_CHANGED));

        assertThat(workSpec.getState(), is(State.SUCCEEDED));
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
