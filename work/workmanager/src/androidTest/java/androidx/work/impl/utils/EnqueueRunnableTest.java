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

package androidx.work.impl.utils;

import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import androidx.work.impl.WorkContinuationImpl;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class EnqueueRunnableTest {

    private WorkContinuationImpl mWorkContinuation;

    @Before
    public void setup() {
        mWorkContinuation = mock(WorkContinuationImpl.class);
    }

    @Test
    public void testScheduleWorkInBackground_isCalled() {
        EnqueueRunnable runnable = spy(new EnqueueRunnable(mWorkContinuation));
        // For some reason when().thenReturn() does not seem to work here. :(
        // TODO(rahulrav@) Figure out why.
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                return true;
            }
        }).when(runnable).addToDatabase();
        doNothing().when(runnable).scheduleWorkInBackground();
        runnable.run();
        verify(runnable, times(1)).scheduleWorkInBackground();
    }

    @Test
    public void testScheduleWorkInBackground_isNotCalled() {
        EnqueueRunnable runnable = spy(new EnqueueRunnable(mWorkContinuation));
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                return false;
            }
        }).when(runnable).addToDatabase();
        doNothing().when(runnable).scheduleWorkInBackground();
        runnable.run();
        verify(runnable, times(0)).scheduleWorkInBackground();
    }
}
