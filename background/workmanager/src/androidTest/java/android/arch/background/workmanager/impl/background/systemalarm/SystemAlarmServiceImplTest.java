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

package android.arch.background.workmanager.impl.background.systemalarm;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

import android.arch.background.workmanager.Constraints;
import android.arch.background.workmanager.Work;
import android.arch.background.workmanager.WorkManagerTest;
import android.arch.background.workmanager.impl.background.BackgroundProcessor;
import android.arch.background.workmanager.impl.constraints.WorkConstraintsTracker;
import android.arch.background.workmanager.impl.model.WorkSpec;
import android.arch.background.workmanager.worker.TestWorker;
import android.content.Context;
import android.content.Intent;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;

import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.List;

public class SystemAlarmServiceImplTest extends WorkManagerTest {
    private SystemAlarmServiceImpl mSystemAlarmServiceImpl;
    private BackgroundProcessor mMockProcessor;
    private WorkConstraintsTracker mMockWorkConstraintsTracker;
    private SystemAlarmServiceImpl.AllWorkExecutedCallback mMockCallback;
    private Context mContext;

    @Before
    public void setUp() {
        mContext = InstrumentationRegistry.getTargetContext();
        mMockProcessor = mock(BackgroundProcessor.class);
        mMockWorkConstraintsTracker = mock(WorkConstraintsTracker.class);
        mMockCallback = mock(SystemAlarmServiceImpl.AllWorkExecutedCallback.class);
        mSystemAlarmServiceImpl = new SystemAlarmServiceImpl(
                mContext, mMockProcessor, mMockWorkConstraintsTracker, mMockCallback);
    }

    @Test
    @SmallTest
    public void testOnChanged_processImmediately() {
        Work work = Work.newBuilder(TestWorker.class)
                .withPeriodStartTime(0)
                .build();
        WorkSpec workSpec = getWorkSpec(work);
        List<WorkSpec> workSpecs = Collections.singletonList(workSpec);

        mSystemAlarmServiceImpl.onEligibleWorkChanged(workSpecs);

        verify(mMockProcessor).process(workSpec.getId());
        verify(mMockWorkConstraintsTracker).replace(Collections.<WorkSpec>emptyList());
        verifyZeroInteractions(mMockCallback);
        assertThat(mSystemAlarmServiceImpl.getDelayMetWorkSpecs(), is(empty()));
        assertThat(mSystemAlarmServiceImpl.getDelayNotMetWorkSpecs(), is(empty()));
    }

    @Test
    @SmallTest
    public void testOnChanged_constrained() {
        Work work = Work.newBuilder(TestWorker.class)
                .withPeriodStartTime(0)
                .withConstraints(new Constraints.Builder().setRequiresBatteryNotLow(true).build())
                .build();
        WorkSpec workSpec = getWorkSpec(work);
        List<WorkSpec> workSpecs = Collections.singletonList(workSpec);

        mSystemAlarmServiceImpl.onEligibleWorkChanged(workSpecs);

        verify(mMockWorkConstraintsTracker).replace(workSpecs);
        verifyZeroInteractions(mMockProcessor, mMockCallback);
        assertThat(mSystemAlarmServiceImpl.getDelayMetWorkSpecs(), is(workSpecs));
        assertThat(mSystemAlarmServiceImpl.getDelayNotMetWorkSpecs(), is(empty()));
    }

    @Test
    @SmallTest
    public void testOnChanged_delayNotMet() {
        long futureTime = System.currentTimeMillis() + (60 * 60 * 1000); // 1 hour from now
        Work work = Work.newBuilder(TestWorker.class)
                .withPeriodStartTime(futureTime)
                .build();
        WorkSpec workSpec = getWorkSpec(work);
        List<WorkSpec> workSpecs = Collections.singletonList(workSpec);

        mSystemAlarmServiceImpl.onEligibleWorkChanged(workSpecs);

        verify(mMockWorkConstraintsTracker).replace(Collections.<WorkSpec>emptyList());
        verifyZeroInteractions(mMockProcessor);
        assertThat(mSystemAlarmServiceImpl.getDelayMetWorkSpecs(), is(empty()));
        assertThat(mSystemAlarmServiceImpl.getDelayNotMetWorkSpecs(), is(workSpecs));
    }

    @Test
    @SmallTest
    public void testOnIntentReceived_delayMet_workSpecNotInList() {
        String workSpecId = "SOME_WORKSPEC_WHOSE_DELAY_IS_MET";
        Intent intent = SystemAlarmService.createDelayMetIntent(mContext, workSpecId);
        mSystemAlarmServiceImpl.onStartCommand(intent);
        verifyZeroInteractions(mMockWorkConstraintsTracker, mMockProcessor, mMockCallback);
    }

    @Test
    @SmallTest
    public void testOnIntentReceived_delayMet_processImmediately() {
        // Add a WorkSpec with a delay
        long futureTime = System.currentTimeMillis() + (60 * 60 * 1000); // 1 hour from now
        Work work = Work.newBuilder(TestWorker.class)
                .withPeriodStartTime(futureTime)
                .build();
        WorkSpec workSpec = getWorkSpec(work);
        List<WorkSpec> workSpecs = Collections.singletonList(workSpec);
        mSystemAlarmServiceImpl.onEligibleWorkChanged(workSpecs);

        assertThat(mSystemAlarmServiceImpl.getDelayNotMetWorkSpecs(), is(workSpecs));
        verifyZeroInteractions(mMockProcessor);

        // Delay has been met
        Intent intent = SystemAlarmService.createDelayMetIntent(mContext, workSpec.getId());
        mSystemAlarmServiceImpl.onStartCommand(intent);

        assertThat(mSystemAlarmServiceImpl.getDelayNotMetWorkSpecs(), is(empty()));
        verify(mMockProcessor).process(workSpec.getId());
    }

    @Test
    @SmallTest
    public void testOnIntentReceived_delayMet_observe() {
        // Add a WorkSpec with a delay
        long futureTime = System.currentTimeMillis() + (60 * 60 * 1000); // 1 hour from now
        Work work = Work.newBuilder(TestWorker.class)
                .withPeriodStartTime(futureTime)
                .withConstraints(new Constraints.Builder().setRequiresBatteryNotLow(true).build())
                .build();
        WorkSpec workSpec = getWorkSpec(work);
        List<WorkSpec> workSpecs = Collections.singletonList(workSpec);
        mSystemAlarmServiceImpl.onEligibleWorkChanged(workSpecs);

        assertThat(mSystemAlarmServiceImpl.getDelayNotMetWorkSpecs(), is(workSpecs));
        assertThat(mSystemAlarmServiceImpl.getDelayMetWorkSpecs(), is(empty()));
        verify(mMockWorkConstraintsTracker).replace(Collections.<WorkSpec>emptyList());

        // Delay has been met
        Intent intent = SystemAlarmService.createDelayMetIntent(mContext, workSpec.getId());
        mSystemAlarmServiceImpl.onStartCommand(intent);

        assertThat(mSystemAlarmServiceImpl.getDelayNotMetWorkSpecs(), is(empty()));
        assertThat(mSystemAlarmServiceImpl.getDelayMetWorkSpecs(), is(workSpecs));
        // TODO(xbhatnag): Why times(2)? this method is invoked exactly once with these arguments.
        verify(mMockWorkConstraintsTracker, times(2)).replace(workSpecs);
    }

    @Test
    @SmallTest
    public void testOnExecuted_removedFromObserverList() {
        // Add a WorkSpec with a delay
        long futureTime = System.currentTimeMillis() + (60 * 60 * 1000); // 1 hour from now
        Work work = Work.newBuilder(TestWorker.class)
                .withPeriodStartTime(futureTime)
                .withConstraints(new Constraints.Builder().setRequiresBatteryNotLow(true).build())
                .build();
        WorkSpec workSpec = getWorkSpec(work);
        List<WorkSpec> workSpecs = Collections.singletonList(workSpec);
        mSystemAlarmServiceImpl.onEligibleWorkChanged(workSpecs);
        verify(mMockWorkConstraintsTracker).replace(Collections.<WorkSpec>emptyList());

        // Delay has been met
        Intent intent = SystemAlarmService.createDelayMetIntent(mContext, workSpec.getId());
        mSystemAlarmServiceImpl.onStartCommand(intent);
        assertThat(mSystemAlarmServiceImpl.getDelayMetWorkSpecs(), is(workSpecs));
        // TODO(xbhatnag): Why times(2)? this method is invoked exactly once with these arguments.
        verify(mMockWorkConstraintsTracker, times(2)).replace(workSpecs);

        // Constraints met
        List<String> workSpecIds = Collections.singletonList(workSpec.getId());
        mSystemAlarmServiceImpl.onAllConstraintsMet(workSpecIds);
        verify(mMockProcessor).process(workSpec.getId());

        // Processing complete
        mSystemAlarmServiceImpl.onExecuted(workSpec.getId(), false);
        assertThat(mSystemAlarmServiceImpl.getDelayMetWorkSpecs(), is(empty()));
        // TODO(xbhatnag): Why times(3)? this method is invoked exactly twice with these arguments.
        verify(mMockWorkConstraintsTracker, times(3)).replace(Collections.<WorkSpec>emptyList());
    }
}
