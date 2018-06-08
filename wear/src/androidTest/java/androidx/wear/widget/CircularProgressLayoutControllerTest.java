/*
 * Copyright (C) 2017 The Android Open Source Project
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

package androidx.wear.widget;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.support.test.annotation.UiThreadTest;
import android.support.test.filters.LargeTest;
import android.support.test.filters.MediumTest;
import android.support.test.runner.AndroidJUnit4;

import androidx.swiperefreshlayout.widget.CircularProgressDrawable;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import java.util.concurrent.TimeUnit;

@MediumTest
@RunWith(AndroidJUnit4.class)
public class CircularProgressLayoutControllerTest {

    private static final long TOTAL_TIME = TimeUnit.SECONDS.toMillis(1);
    private static final long UPDATE_INTERVAL = TimeUnit.MILLISECONDS.toMillis(30);

    private CircularProgressLayoutController mControllerUnderTest;

    @Mock
    CircularProgressDrawable mMockDrawable;
    @Mock
    CircularProgressLayout mMockLayout;
    @Mock
    CircularProgressLayout.OnTimerFinishedListener mMockListener;

    @Before
    public void setUp() {
        mMockDrawable = mock(CircularProgressDrawable.class);
        mMockLayout = mock(CircularProgressLayout.class);
        mMockListener = mock(CircularProgressLayout.OnTimerFinishedListener.class);
        when(mMockLayout.getProgressDrawable()).thenReturn(mMockDrawable);
        when(mMockLayout.getOnTimerFinishedListener()).thenReturn(mMockListener);
        mControllerUnderTest = new CircularProgressLayoutController(mMockLayout);
    }

    @Test
    public void testSetIndeterminate() {
        mControllerUnderTest.setIndeterminate(true);

        assertEquals(true, mControllerUnderTest.isIndeterminate());
        verify(mMockDrawable).start();
    }

    @Test
    public void testIsIndeterminateAfterSetToFalse() {
        mControllerUnderTest.setIndeterminate(true);
        mControllerUnderTest.setIndeterminate(false);

        assertEquals(false, mControllerUnderTest.isIndeterminate());
        verify(mMockDrawable).stop();
    }

    @LargeTest
    @Test
    @UiThreadTest
    public void testIsTimerRunningAfterStart() {
        mControllerUnderTest.startTimer(TOTAL_TIME, UPDATE_INTERVAL);

        assertEquals(true, mControllerUnderTest.isTimerRunning());
    }

    @Test
    @UiThreadTest
    public void testIsTimerRunningAfterStop() {
        mControllerUnderTest.startTimer(TOTAL_TIME, UPDATE_INTERVAL);
        mControllerUnderTest.stopTimer();

        assertEquals(false, mControllerUnderTest.isTimerRunning());
    }

    @Test
    @UiThreadTest
    public void testSwitchFromIndeterminateToDeterminate() {
        mControllerUnderTest.setIndeterminate(true);
        mControllerUnderTest.startTimer(TOTAL_TIME, UPDATE_INTERVAL);

        assertEquals(false, mControllerUnderTest.isIndeterminate());
        assertEquals(true, mControllerUnderTest.isTimerRunning());
        verify(mMockDrawable).stop();
    }

    @Test
    @UiThreadTest
    public void testSwitchFromDeterminateToIndeterminate() {
        mControllerUnderTest.startTimer(TOTAL_TIME, UPDATE_INTERVAL);
        mControllerUnderTest.setIndeterminate(true);

        assertEquals(true, mControllerUnderTest.isIndeterminate());
        assertEquals(false, mControllerUnderTest.isTimerRunning());
        verify(mMockDrawable).start();
    }
}
