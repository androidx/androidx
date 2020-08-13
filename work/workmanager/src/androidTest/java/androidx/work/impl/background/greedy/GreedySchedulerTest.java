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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SdkSuppress;
import androidx.test.filters.SmallTest;
import androidx.work.Constraints;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManagerTest;
import androidx.work.impl.Processor;
import androidx.work.impl.WorkManagerImpl;
import androidx.work.impl.constraints.WorkConstraintsTracker;
import androidx.work.impl.model.WorkSpec;
import androidx.work.impl.utils.taskexecutor.TaskExecutor;
import androidx.work.worker.TestWorker;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;


@RunWith(AndroidJUnit4.class)
public class GreedySchedulerTest extends WorkManagerTest {

    private static final String TEST_ID = "test";

    private Context mContext;
    private WorkManagerImpl mWorkManagerImpl;
    private Processor mMockProcessor;
    private WorkConstraintsTracker mMockWorkConstraintsTracker;
    private GreedyScheduler mGreedyScheduler;
    private DelayedWorkTracker mDelayedWorkTracker;

    @Before
    public void setUp() {
        mContext = mock(Context.class);
        TaskExecutor taskExecutor = mock(TaskExecutor.class);
        mWorkManagerImpl = mock(WorkManagerImpl.class);
        mMockProcessor = mock(Processor.class);
        mMockWorkConstraintsTracker = mock(WorkConstraintsTracker.class);
        when(mWorkManagerImpl.getProcessor()).thenReturn(mMockProcessor);
        when(mWorkManagerImpl.getWorkTaskExecutor()).thenReturn(taskExecutor);
        mGreedyScheduler = new GreedyScheduler(
                mContext,
                mWorkManagerImpl,
                mMockWorkConstraintsTracker);
        mGreedyScheduler.mInDefaultProcess = true;
        mDelayedWorkTracker = mock(DelayedWorkTracker.class);
        mGreedyScheduler.setDelayedWorkTracker(mDelayedWorkTracker);
    }

    @Test
    @SmallTest
    public void testGreedyScheduler_startsUnconstrainedWork() {
        OneTimeWorkRequest work = new OneTimeWorkRequest.Builder(TestWorker.class).build();
        WorkSpec workSpec = getWorkSpec(work);
        mGreedyScheduler.schedule(workSpec);
        verify(mWorkManagerImpl).startWork(workSpec.id);
    }

    @Test
    @SmallTest
    public void testGreedyScheduler_startsPeriodicWorkRequests() {
        PeriodicWorkRequest periodicWork =
                new PeriodicWorkRequest.Builder(TestWorker.class, 0L, TimeUnit.MILLISECONDS)
                        .build();
        mGreedyScheduler.schedule(getWorkSpec(periodicWork));
        // PeriodicWorkRequests are special because their periodStartTime is set to 0.
        // So the first invocation will always result in startWork(). Subsequent runs will
        // use `delayedStartWork()`.
        verify(mWorkManagerImpl).startWork(periodicWork.getStringId());
    }

    @Test
    @SmallTest
    public void testGreedyScheduler_startsDelayedWork() {
        OneTimeWorkRequest work = new OneTimeWorkRequest.Builder(TestWorker.class)
                .setInitialDelay(1000L, TimeUnit.MILLISECONDS)
                .build();
        mGreedyScheduler.schedule(getWorkSpec(work));
        verify(mDelayedWorkTracker).schedule(work.getWorkSpec());
    }

    @Test
    @SmallTest
    public void testGreedyScheduler_startsBackedOffWork() {
        OneTimeWorkRequest work = new OneTimeWorkRequest.Builder(TestWorker.class)
                .setPeriodStartTime(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
                .setInitialRunAttemptCount(5)
                .build();
        mGreedyScheduler.schedule(getWorkSpec(work));
        verify(mDelayedWorkTracker).schedule(work.getWorkSpec());
    }

    @Test
    @SmallTest
    @SdkSuppress(minSdkVersion = 23)
    public void testGreedyScheduler_ignoresIdleWorkConstraint() {
        Constraints constraints = new Constraints.Builder()
                .setRequiresDeviceIdle(true)
                .build();
        OneTimeWorkRequest work = new OneTimeWorkRequest.Builder(TestWorker.class)
                .setConstraints(constraints)
                .build();
        mGreedyScheduler.schedule(getWorkSpec(work));
        verify(mMockWorkConstraintsTracker, never()).replace(ArgumentMatchers.<WorkSpec>anyList());
    }

    @Test
    @SmallTest
    public void testGreedyScheduler_startsWorkWhenConstraintsMet() {
        mGreedyScheduler.onAllConstraintsMet(Collections.singletonList(TEST_ID));
        verify(mWorkManagerImpl).startWork(TEST_ID);
    }

    @Test
    @SmallTest
    public void testGreedyScheduler_stopsWorkWhenConstraintsNotMet() {
        mGreedyScheduler.onAllConstraintsNotMet(Collections.singletonList(TEST_ID));
        verify(mWorkManagerImpl).stopWork(TEST_ID);
    }

    @Test
    @SmallTest
    public void testGreedyScheduler_constraintsAreAddedAndRemovedForTracking() {
        final OneTimeWorkRequest work = new OneTimeWorkRequest.Builder(TestWorker.class)
                .setConstraints(new Constraints.Builder().setRequiresCharging(true).build())
                .build();
        final WorkSpec workSpec = getWorkSpec(work);
        Set<WorkSpec> expected = new HashSet<WorkSpec>();
        expected.add(workSpec);

        mGreedyScheduler.schedule(workSpec);
        verify(mMockWorkConstraintsTracker).replace(expected);
        reset(mMockWorkConstraintsTracker);

        mGreedyScheduler.onExecuted(workSpec.id, false);
        verify(mMockWorkConstraintsTracker).replace(Collections.<WorkSpec>emptySet());
    }

    @Test
    @SmallTest
    public void testGreedyScheduler_executionListenerIsRegistered() {
        OneTimeWorkRequest work = new OneTimeWorkRequest.Builder(TestWorker.class).build();
        WorkSpec workSpec = getWorkSpec(work);
        mGreedyScheduler.schedule(workSpec);
        verify(mMockProcessor).addExecutionListener(mGreedyScheduler);
    }

    @Test
    @SmallTest
    public void testGreedyScheduler_executionListenerIsRegisteredOnlyOnce() {
        for (int i = 0; i < 2; ++i) {
            OneTimeWorkRequest work = new OneTimeWorkRequest.Builder(TestWorker.class).build();
            WorkSpec workSpec = getWorkSpec(work);
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
        WorkSpec workSpec = getWorkSpec(work);
        mGreedyScheduler.schedule(workSpec);
        verify(mMockProcessor, times(0)).addExecutionListener(mGreedyScheduler);
        verify(mMockWorkConstraintsTracker, never()).replace(ArgumentMatchers.<WorkSpec>anyList());
    }
}
