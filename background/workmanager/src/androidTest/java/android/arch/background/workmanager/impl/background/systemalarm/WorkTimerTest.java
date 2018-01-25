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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;

import android.arch.background.workmanager.WorkManagerTest;
import android.arch.background.workmanager.impl.background.systemalarm.WorkTimer
        .TimeLimitExceededListener;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class WorkTimerTest extends WorkManagerTest {
    private static final String TEST_WORK_SPEC_ID = "TEST";
    private static final long TEST_TIME_LIMIT_MILLIS = 100L;
    private static final long HALF_TEST_TIME_LIMIT_MILLIS = TEST_TIME_LIMIT_MILLIS / 2;

    private WorkTimer mWorkTimer;
    private TimeLimitExceededListener mMockListener;

    @Before
    public void setUp() {
        mMockListener = mock(TimeLimitExceededListener.class);

        mWorkTimer = new WorkTimer();
        mWorkTimer.setOnTimeLimitExceededListener(mMockListener);
    }

    @Test
    @SmallTest
    public void testStartTimer() throws InterruptedException {
        mWorkTimer.startTimer(TEST_WORK_SPEC_ID, TEST_TIME_LIMIT_MILLIS);
        verifyZeroInteractions(mMockListener);
        Thread.sleep(TEST_TIME_LIMIT_MILLIS + 50L);
        verify(mMockListener).onTimeLimitExceeded(TEST_WORK_SPEC_ID);
    }

    @Test
    @SmallTest
    public void testStartTimer_twiceWithSameId_secondTimerNotifies() throws InterruptedException {
        mWorkTimer.startTimer(TEST_WORK_SPEC_ID, TEST_TIME_LIMIT_MILLIS);

        // Wait a bit before starting second timer.
        Thread.sleep(HALF_TEST_TIME_LIMIT_MILLIS);
        verifyZeroInteractions(mMockListener);

        mWorkTimer.startTimer(TEST_WORK_SPEC_ID, TEST_TIME_LIMIT_MILLIS);

        // First timer should have notified here, but was stopped after the startTimer was called.
        Thread.sleep(HALF_TEST_TIME_LIMIT_MILLIS + 10L);
        verify(mMockListener, never()).onTimeLimitExceeded(TEST_WORK_SPEC_ID);

        // Second startTimer() should notify here. Listener should only be notified by second call.
        Thread.sleep(HALF_TEST_TIME_LIMIT_MILLIS);
        verify(mMockListener, times(1)).onTimeLimitExceeded(TEST_WORK_SPEC_ID);
    }

    @Test
    @SmallTest
    public void testStopTimer_beforeTimeLimit() throws InterruptedException {
        mWorkTimer.startTimer(TEST_WORK_SPEC_ID, TEST_TIME_LIMIT_MILLIS);
        mWorkTimer.stopTimer(TEST_WORK_SPEC_ID);
        Thread.sleep(TEST_TIME_LIMIT_MILLIS + 50L);
        verifyZeroInteractions(mMockListener);
    }

    @Test
    @SmallTest
    public void testStopTimer_afterTimeLimit() throws InterruptedException {
        mWorkTimer.startTimer(TEST_WORK_SPEC_ID, TEST_TIME_LIMIT_MILLIS);

        Thread.sleep(TEST_TIME_LIMIT_MILLIS + 50L);
        verify(mMockListener).onTimeLimitExceeded(TEST_WORK_SPEC_ID);

        mWorkTimer.stopTimer(TEST_WORK_SPEC_ID);
        verifyNoMoreInteractions(mMockListener);
    }
}
