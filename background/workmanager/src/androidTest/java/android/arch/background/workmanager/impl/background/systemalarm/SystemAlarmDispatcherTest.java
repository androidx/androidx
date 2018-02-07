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

package android.arch.background.workmanager.impl.background.systemalarm;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.arch.background.workmanager.DatabaseTest;
import android.arch.background.workmanager.Work;
import android.arch.background.workmanager.impl.ExecutionListener;
import android.arch.background.workmanager.impl.Processor;
import android.arch.background.workmanager.impl.Scheduler;
import android.arch.background.workmanager.impl.WorkManagerImpl;
import android.arch.background.workmanager.worker.SleepTestWorker;
import android.arch.background.workmanager.worker.TestWorker;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.MediumTest;
import android.support.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;
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
    private Processor mProcessor;
    private Processor mSpyProcessor;
    private SystemAlarmDispatcher mDispatcher;
    private SystemAlarmDispatcher.CommandsCompletedListener mCompletedListener;
    private ForwardingExecutionListener mExecutionListener;
    private CountDownLatch mLatch;

    @Before
    public void setUp() {
        mContext = InstrumentationRegistry.getTargetContext().getApplicationContext();
        mScheduler = mock(Scheduler.class);
        mWorkManager = mock(WorkManagerImpl.class);
        mExecutionListener = new ForwardingExecutionListener();
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
                mScheduler,
                // simulate real world use-case
                Executors.newSingleThreadExecutor());
        mProcessor.addExecutionListener(mExecutionListener);
        mSpyProcessor = spy(mProcessor);

        mDispatcher = spy(new SystemAlarmDispatcher(mContext, mSpyProcessor, mWorkManager));
        mDispatcher.setCompletedListener(mCompletedListener);

        // set forwarding of execution listener to the dispatcher
        mExecutionListener.setDispatcher(mDispatcher);
    }

    @After
    public void tearDown() {
        mProcessor.removeExecutionListener(mExecutionListener);
        mDispatcher.onDestroy();
    }

    @Test
    public void testSchedule() throws InterruptedException {
        Work work = new Work.Builder(TestWorker.class)
                .withPeriodStartTime(System.currentTimeMillis())
                .withInitialDelay(TimeUnit.HOURS.toMillis(1)).build();

        insertWork(work);
        String workSpecId = work.getId();
        final Intent intent = CommandHandler.createScheduleWorkIntent(mContext, workSpecId);
        mDispatcher.postOnMainThread(
                new SystemAlarmDispatcher.AddRunnable(mDispatcher, intent, START_ID));
        mLatch.await(TEST_TIMEOUT, TimeUnit.SECONDS);
        assertThat(mLatch.getCount(), equalTo(0L));
    }

    @Test
    public void testDelayMet_success() throws InterruptedException {
        Work work = new Work.Builder(TestWorker.class)
                .withPeriodStartTime(System.currentTimeMillis())
                .build();

        insertWork(work);
        String workSpecId = work.getId();
        final Intent intent = CommandHandler.createDelayMetIntent(mContext, workSpecId);
        mDispatcher.postOnMainThread(
                new SystemAlarmDispatcher.AddRunnable(mDispatcher, intent, START_ID));
        mLatch.await(TEST_TIMEOUT, TimeUnit.SECONDS);
        assertThat(mLatch.getCount(), equalTo(0L));
        verify(mSpyProcessor, times(1)).process(workSpecId);
    }

    @Test
    public void testDelayMet_withCancel() throws InterruptedException {
        // SleepTestWorker sleeps for 5 seconds
        Work work = new Work.Builder(SleepTestWorker.class)
                .withPeriodStartTime(System.currentTimeMillis())
                .withInitialDelay(TimeUnit.HOURS.toMillis(1))
                .build();

        insertWork(work);
        String workSpecId = work.getId();

        final Intent delayMet = CommandHandler.createDelayMetIntent(mContext, workSpecId);
        final Intent cancel = CommandHandler.createCancelWorkIntent(mContext, workSpecId);

        mDispatcher.postOnMainThread(
                new SystemAlarmDispatcher.AddRunnable(mDispatcher, delayMet, START_ID));

        mDispatcher.postOnMainThread(
                new SystemAlarmDispatcher.AddRunnable(mDispatcher, cancel, START_ID));

        mLatch.await(TEST_TIMEOUT, TimeUnit.SECONDS);

        assertThat(mLatch.getCount(), equalTo(0L));
        verify(mSpyProcessor, times(1)).process(workSpecId);
        verify(mSpyProcessor, times(1)).cancel(workSpecId, true);
    }

    @Test
    public void testSchedule_withCancel() throws InterruptedException {
        Work work = new Work.Builder(SleepTestWorker.class)
                .withPeriodStartTime(System.currentTimeMillis())
                .build();

        insertWork(work);
        String workSpecId = work.getId();

        final Intent delayMet = CommandHandler.createScheduleWorkIntent(mContext, workSpecId);
        final Intent cancel = CommandHandler.createCancelWorkIntent(mContext, workSpecId);

        mDispatcher.postOnMainThread(
                new SystemAlarmDispatcher.AddRunnable(mDispatcher, delayMet, START_ID));

        mDispatcher.postOnMainThread(
                new SystemAlarmDispatcher.AddRunnable(mDispatcher, cancel, START_ID));

        mLatch.await(TEST_TIMEOUT, TimeUnit.SECONDS);

        assertThat(mLatch.getCount(), equalTo(0L));
        verify(mSpyProcessor, times(1)).process(workSpecId);
        verify(mSpyProcessor, times(1)).cancel(workSpecId, true);
    }

    static class ForwardingExecutionListener implements ExecutionListener {
        private SystemAlarmDispatcher mDispatcher;

        void setDispatcher(@NonNull SystemAlarmDispatcher dispatcher) {
            mDispatcher = dispatcher;
        }

        @Override
        public void onExecuted(@NonNull String workSpecId, boolean isSuccessful,
                boolean needsReschedule) {
            if (mDispatcher != null) {
                mDispatcher.onExecuted(workSpecId, isSuccessful, needsReschedule);
            }
        }
    }

}
