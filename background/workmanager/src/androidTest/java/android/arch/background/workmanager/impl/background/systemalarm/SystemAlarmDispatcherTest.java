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
import android.arch.background.workmanager.impl.Scheduler;
import android.arch.background.workmanager.impl.WorkManagerImpl;
import android.arch.background.workmanager.impl.background.BackgroundProcessor;
import android.arch.background.workmanager.worker.SleepTestWorker;
import android.arch.background.workmanager.worker.TestWorker;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.MediumTest;
import android.support.test.runner.AndroidJUnit4;

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
    private static final int TEST_TIMEOUT = 5; // seconds;

    private Context mContext;
    private Scheduler mScheduler;
    private WorkManagerImpl mWorkManager;
    private BackgroundProcessor mProcessor;
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
        mProcessor = spy(new BackgroundProcessor(
                mContext,
                mDatabase,
                mScheduler,
                // simulate real world use-case
                Executors.newSingleThreadExecutor(),
                mExecutionListener));

        mDispatcher = spy(new SystemAlarmDispatcher(mContext, mProcessor, mWorkManager));
        mDispatcher.setCompletedListener(mCompletedListener);

        // set forwarding of execution listener to the dispatcher
        mExecutionListener.setDispatcher(mDispatcher);
    }

    @Test
    public void testSchedule() throws InterruptedException {
        Work work = new Work.Builder(TestWorker.class)
                .withPeriodStartTime(System.currentTimeMillis())
                .withInitialDelay(TimeUnit.HOURS.toMillis(1)).build();

        insertWork(work);
        String workSpecId = work.getId();
        final Intent intent = CommandHandler.createScheduleWorkIntent(mContext, workSpecId);

        mDispatcher.postOnMainThread(new Runnable() {
            @Override
            public void run() {
                mDispatcher.add(intent, START_ID);
            }
        });
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
        mDispatcher.postOnMainThread(new Runnable() {
            @Override
            public void run() {
                mDispatcher.add(intent, START_ID);
            }
        });

        mLatch.await(TEST_TIMEOUT, TimeUnit.SECONDS);
        assertThat(mLatch.getCount(), equalTo(0L));
        verify(mProcessor, times(1)).process(workSpecId);
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
        mDispatcher.postOnMainThread(new Runnable() {
            @Override
            public void run() {
                mDispatcher.add(delayMet, START_ID);
                mDispatcher.add(cancel, START_ID);
            }
        });

        mLatch.await(TEST_TIMEOUT, TimeUnit.SECONDS);

        assertThat(mLatch.getCount(), equalTo(0L));
        verify(mProcessor, times(1)).process(workSpecId);
        verify(mProcessor, times(1)).cancel(workSpecId, true);
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
