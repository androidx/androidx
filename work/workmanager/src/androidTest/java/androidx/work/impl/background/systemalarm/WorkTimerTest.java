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
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import android.support.annotation.NonNull;
import android.support.test.filters.LargeTest;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class WorkTimerTest {

    private static final String WORKSPEC_ID_1 = "1";

    private WorkTimer mWorkTimer;
    private TestTimeLimitExceededListener mListener;

    @Before
    public void setUp() {
        mWorkTimer = new WorkTimer();
        mListener = new TestTimeLimitExceededListener();
    }

    @Test
    @LargeTest
    public void testTimer_withListenerAndCleanUp() throws InterruptedException {
        TestTimeLimitExceededListener listenerSpy = spy(mListener);
        mWorkTimer.startTimer(WORKSPEC_ID_1, 0, listenerSpy);
        Thread.sleep(100); // introduce a small delay
        verify(listenerSpy, times(1)).onTimeLimitExceeded(WORKSPEC_ID_1);
        assertThat(mWorkTimer.getTimerMap().size(), is(0));
        assertThat(mWorkTimer.getListeners().size(), is(0));
    }

    @Test
    @LargeTest
    public void testStopTimer_withCleanUp() throws InterruptedException {
        TestTimeLimitExceededListener listenerSpy = spy(mListener);
        mWorkTimer.startTimer(WORKSPEC_ID_1, 100, listenerSpy);
        mWorkTimer.stopTimer(WORKSPEC_ID_1);
        Thread.sleep(200);
        verify(listenerSpy, times(0)).onTimeLimitExceeded(WORKSPEC_ID_1);
        assertThat(mWorkTimer.getTimerMap().size(), is(0));
        assertThat(mWorkTimer.getListeners().size(), is(0));
    }

    // Making this a defined class to its easy to proxy
    public static class TestTimeLimitExceededListener implements
            WorkTimer.TimeLimitExceededListener {
        @Override
        public void onTimeLimitExceeded(@NonNull String workSpecId) {
            // does nothing
        }
    }
}
