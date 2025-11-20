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

package androidx.work.impl.background.greedy;

import static androidx.work.WorkRequest.DEFAULT_BACKOFF_DELAY_MILLIS;
import static androidx.work.impl.model.WorkSpecKt.generationalId;
import static androidx.work.impl.testutils.TestConstraintsKt.getConstraintsNotMet;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;
import androidx.work.Configuration;
import androidx.work.Constraints;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManagerTest;
import androidx.work.impl.Processor;
import androidx.work.impl.StartStopToken;
import androidx.work.impl.WorkLauncher;
import androidx.work.impl.constraints.ConstraintsState.ConstraintsMet;
import androidx.work.impl.constraints.trackers.Trackers;
import androidx.work.impl.model.WorkSpec;
import androidx.work.impl.testutils.TestConstraintTracker;
import androidx.work.impl.utils.taskexecutor.InstantWorkTaskExecutor;
import androidx.work.worker.TestWorker;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;

import java.util.concurrent.TimeUnit;

@RunWith(AndroidJUnit4.class)
public class GreedySchedulerTest extends WorkManagerTest {
    private final Context mContext = ApplicationProvider.getApplicationContext();
    private Processor mMockProcessor;
    private GreedyScheduler mGreedyScheduler;
    private DelayedWorkTracker mDelayedWorkTracker;

    private WorkLauncher mWorkLauncher;
    private TestConstraintTracker mBatteryTracker;

    @Before
    public void setUp() {
        mMockProcessor = mock(Processor.class);
        InstantWorkTaskExecutor taskExecutor = new InstantWorkTaskExecutor();
        mBatteryTracker = new TestConstraintTracker(false, mContext, taskExecutor);
        Trackers trackers = new Trackers(mContext, taskExecutor, mBatteryTracker);
        mWorkLauncher = mock(WorkLauncher.class);
        Configuration configuration = new Configuration.Builder().build();
        mGreedyScheduler = new GreedyScheduler(
                mContext, configuration, trackers, mMockProcessor, mWorkLauncher, taskExecutor);
        mGreedyScheduler.mInDefaultProcess = true;
        mDelayedWorkTracker = mock(DelayedWorkTracker.class);
        mGreedyScheduler.setDelayedWorkTracker(mDelayedWorkTracker);
    }

    @Test
    @SmallTest
    public void testGreedyScheduler_startsUnconstrainedWork() {
        OneTimeWorkRequest work = new OneTimeWorkRequest.Builder(TestWorker.class).build();
        WorkSpec workSpec = work.getWorkSpec();
        workSpec.lastEnqueueTime = System.currentTimeMillis();
        mGreedyScheduler.schedule(workSpec);
        ArgumentCaptor<StartStopToken> captor = ArgumentCaptor.forClass(StartStopToken.class);
        verify(mWorkLauncher).startWork(captor.capture());
        assertThat(captor.getValue().getId().getWorkSpecId()).isEqualTo(workSpec.id);
    }

    @Test
    @SmallTest
    public void testGreedyScheduler_startsPeriodicWorkRequests() {
        PeriodicWorkRequest periodicWork =
                new PeriodicWorkRequest.Builder(TestWorker.class, 0L, TimeUnit.MILLISECONDS)
                        .build();
        mGreedyScheduler.schedule(periodicWork.getWorkSpec());
        // PeriodicWorkRequests are special because their periodStartTime is set to 0.
        // So the first invocation will always result in startWork(). Subsequent runs will
        // use `delayedStartWork()`.
        ArgumentCaptor<StartStopToken> captor = ArgumentCaptor.forClass(StartStopToken.class);
        verify(mWorkLauncher).startWork(captor.capture());
        assertThat(captor.getValue().getId().getWorkSpecId()).isEqualTo(periodicWork.getStringId());
    }

    @Test
    @SmallTest
    public void testGreedyScheduler_startsDelayedWork() {
        OneTimeWorkRequest work = new OneTimeWorkRequest.Builder(TestWorker.class)
                .setLastEnqueueTime(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
                .setInitialDelay(1000L, TimeUnit.MILLISECONDS)
                .build();
        mGreedyScheduler.schedule(work.getWorkSpec());
        verify(mDelayedWorkTracker).schedule(eq(work.getWorkSpec()), anyLong());
    }

    @Test
    @SmallTest
    public void testGreedyScheduler_startsBackedOffWork() {
        OneTimeWorkRequest work = new OneTimeWorkRequest.Builder(TestWorker.class)
                .setLastEnqueueTime(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
                .setInitialRunAttemptCount(5)
                .build();
        mGreedyScheduler.schedule(work.getWorkSpec());
        verify(mDelayedWorkTracker).schedule(eq(work.getWorkSpec()), anyLong());
    }

    @Test
    @SmallTest
    public void testGreedyScheduler_ignoresIdleWorkConstraint() {
        Constraints constraints = new Constraints.Builder()
                .setRequiresDeviceIdle(true)
                .setRequiresCharging(true)
                .build();
        OneTimeWorkRequest work = new OneTimeWorkRequest.Builder(TestWorker.class)
                .setConstraints(constraints)
                .build();
        mGreedyScheduler.schedule(work.getWorkSpec());
        // shouldn't be tracked, because work has idle constraint unsupported by GreedyScheduler
        assertThat(mBatteryTracker.isTracking()).isFalse();
    }

    @Test
    @SmallTest
    public void testGreedyScheduler_startsWorkWhenConstraintsMet() {
        OneTimeWorkRequest work = new OneTimeWorkRequest.Builder(TestWorker.class).build();
        mGreedyScheduler.onConstraintsStateChanged(work.getWorkSpec(), ConstraintsMet.INSTANCE);
        ArgumentCaptor<StartStopToken> captor = ArgumentCaptor.forClass(StartStopToken.class);
        verify(mWorkLauncher).startWork(captor.capture());
        assertThat(captor.getValue().getId().getWorkSpecId()).isEqualTo(work.getWorkSpec().id);
    }

    @Test
    @SmallTest
    public void testGreedyScheduler_stopsWorkWhenConstraintsNotMet() {
        // in order to stop the work, we should start it first.
        OneTimeWorkRequest work = new OneTimeWorkRequest.Builder(TestWorker.class).build();
        mGreedyScheduler.onConstraintsStateChanged(work.getWorkSpec(), ConstraintsMet.INSTANCE);
        mGreedyScheduler.onConstraintsStateChanged(work.getWorkSpec(), getConstraintsNotMet());
        ArgumentCaptor<StartStopToken> captorToken = ArgumentCaptor.forClass(StartStopToken.class);
        verify(mWorkLauncher)
                .stopWorkWithReason(captorToken.capture(), eq(getConstraintsNotMet().getReason()));
        assertThat(captorToken.getValue().getId().getWorkSpecId()).isEqualTo(work.getWorkSpec().id);
        // doing this check because java vs inline classes
    }

    @Test
    @SmallTest
    public void testGreedyScheduler_constraintsAreAddedAndRemovedForTracking() {
        final OneTimeWorkRequest work = new OneTimeWorkRequest.Builder(TestWorker.class)
                .setConstraints(new Constraints.Builder().setRequiresCharging(true).build())
                .build();
        final WorkSpec workSpec = work.getWorkSpec();
        workSpec.lastEnqueueTime = System.currentTimeMillis();

        mGreedyScheduler.schedule(workSpec);
        assertThat(mBatteryTracker.isTracking()).isTrue();
        mGreedyScheduler.onExecuted(generationalId(workSpec), false);
        assertThat(mBatteryTracker.isTracking()).isFalse();
    }

    @Test
    @SmallTest
    public void testGreedyScheduler_executionListenerIsRegistered() {
        OneTimeWorkRequest work = new OneTimeWorkRequest.Builder(TestWorker.class).build();
        WorkSpec workSpec = work.getWorkSpec();
        mGreedyScheduler.schedule(workSpec);
        verify(mMockProcessor).addExecutionListener(mGreedyScheduler);
    }

    @Test
    @SmallTest
    public void testGreedyScheduler_executionListenerIsRegisteredOnlyOnce() {
        for (int i = 0; i < 2; ++i) {
            OneTimeWorkRequest work = new OneTimeWorkRequest.Builder(TestWorker.class).build();
            WorkSpec workSpec = work.getWorkSpec();
            mGreedyScheduler.schedule(workSpec);
        }
        verify(mMockProcessor, times(1)).addExecutionListener(mGreedyScheduler);
    }

    @Test
    @SmallTest
    public void testGreedyScheduler_ignoresRequestsInADifferentProcess() {
        // Context.getSystemService() returns null so no work should be executed.
        mGreedyScheduler.mInDefaultProcess = false;
        OneTimeWorkRequest work = new OneTimeWorkRequest.Builder(TestWorker.class).build();
        WorkSpec workSpec = work.getWorkSpec();
        mGreedyScheduler.schedule(workSpec);
        verify(mMockProcessor, times(0)).addExecutionListener(mGreedyScheduler);
    }

    @Test
    @SmallTest
    public void testGreedyScheduler_throttleWork() {
        long before = System.currentTimeMillis();
        OneTimeWorkRequest work = new OneTimeWorkRequest.Builder(TestWorker.class)
                .setLastEnqueueTime(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
                .setInitialDelay(1000L, TimeUnit.MILLISECONDS)
                .build();
        WorkSpec workSpec = work.getWorkSpec();
        mGreedyScheduler.schedule(workSpec);
        mGreedyScheduler.onExecuted(generationalId(workSpec), true);
        WorkSpec updatedRunAttemptCount = new WorkSpec(workSpec.id, workSpec);
        updatedRunAttemptCount.runAttemptCount = 10;
        reset(mDelayedWorkTracker);
        mGreedyScheduler.schedule(updatedRunAttemptCount);
        ArgumentCaptor<Long> delayCapture = ArgumentCaptor.forClass(Long.class);
        verify(mDelayedWorkTracker).schedule(eq(updatedRunAttemptCount), delayCapture.capture());
        assertThat(delayCapture.getValue()).isAtLeast(before + 5 * DEFAULT_BACKOFF_DELAY_MILLIS);
    }
}
