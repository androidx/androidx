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
import static org.mockito.Mockito.verify;

import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;

import java.util.Collections;

import androidx.work.PeriodicWork;
import androidx.work.Work;
import androidx.work.WorkManagerTest;
import androidx.work.impl.WorkManagerImpl;
import androidx.work.impl.constraints.WorkConstraintsTracker;
import androidx.work.impl.model.WorkSpec;
import androidx.work.worker.TestWorker;

@RunWith(AndroidJUnit4.class)
public class GreedySchedulerTest extends WorkManagerTest {

    private static final String TEST_ID = "test";

    private WorkManagerImpl mMockWorkManagerImpl;
    private WorkConstraintsTracker mMockWorkConstraintsTracker;
    private GreedyScheduler mGreedyScheduler;

    @Before
    public void setUp() {
        mMockWorkManagerImpl = mock(WorkManagerImpl.class);
        mMockWorkConstraintsTracker = mock(WorkConstraintsTracker.class);
        mGreedyScheduler = new GreedyScheduler(mMockWorkManagerImpl, mMockWorkConstraintsTracker);
    }

    @Test
    @SmallTest
    public void testGreedyScheduler_tracksSimpleWork() {
        Work work = new Work.Builder(TestWorker.class).build();
        WorkSpec workSpec = getWorkSpec(work);
        mGreedyScheduler.schedule(workSpec);
        verify(mMockWorkConstraintsTracker).replace(Collections.singletonList(workSpec));
    }

    @Test
    @SmallTest
    public void testGreedyScheduler_ignoresPeriodicWork() {
        PeriodicWork periodicWork =
                new PeriodicWork.Builder(TestWorker.class, 0L).build();
        mGreedyScheduler.schedule(getWorkSpec(periodicWork));
        verify(mMockWorkConstraintsTracker, never()).replace(ArgumentMatchers.<WorkSpec>anyList());
    }

    @Test
    @SmallTest
    public void testGreedyScheduler_ignoresInitialDelayWork() {
        Work work = new Work.Builder(TestWorker.class).withInitialDelay(1000L).build();
        mGreedyScheduler.schedule(getWorkSpec(work));
        verify(mMockWorkConstraintsTracker, never()).replace(ArgumentMatchers.<WorkSpec>anyList());
    }

    @Test
    @SmallTest
    public void testGreedyScheduler_startsWorkWhenConstraintsMet() {
        mGreedyScheduler.onAllConstraintsMet(Collections.singletonList(TEST_ID));
        verify(mMockWorkManagerImpl).startWork(TEST_ID);
    }

    @Test
    @SmallTest
    public void testGreedyScheduler_stopsWorkWhenConstraintsNotMet() {
        mGreedyScheduler.onAllConstraintsNotMet(Collections.singletonList(TEST_ID));
        verify(mMockWorkManagerImpl).stopWork(TEST_ID);
    }
}
