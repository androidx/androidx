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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import android.arch.background.workmanager.DatabaseTest;
import android.arch.background.workmanager.Work;
import android.arch.background.workmanager.impl.ExecutionListener;
import android.arch.background.workmanager.impl.Scheduler;
import android.arch.background.workmanager.worker.TestWorker;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.Executors;

@RunWith(AndroidJUnit4.class)
public class TimedBackgroundProcessorTest extends DatabaseTest {

    private ExecutionListener mMockExecutionListener;
    private WorkTimer mMockWorkTimer;
    private TimedBackgroundProcessor mTimedBackgroundProcessor;

    @Before
    public void setUp() {
        mMockExecutionListener = mock(ExecutionListener.class);
        mMockWorkTimer = mock(WorkTimer.class);
        mTimedBackgroundProcessor = new TimedBackgroundProcessor(
                InstrumentationRegistry.getTargetContext().getApplicationContext(),
                mDatabase,
                mock(Scheduler.class),
                Executors.newSingleThreadExecutor(),
                mMockExecutionListener,
                mMockWorkTimer);
    }

    @Test
    @SmallTest
    public void testConstructor() {
        verify(mMockWorkTimer).setOnTimeLimitExceededListener(mTimedBackgroundProcessor);
    }

    @Test
    @SmallTest
    public void testProcess_startsTimer() {
        Work work = new Work.Builder(TestWorker.class).build();
        String id = work.getId();
        insertWork(work);
        assertThat(mTimedBackgroundProcessor.process(id), is(true));
        verify(mMockWorkTimer)
                .startTimer(id, TimedBackgroundProcessor.PROCESSING_TIME_LIMIT_MILLIS);
    }

    @Test
    @SmallTest
    public void testCancel_stopsTimer() {
        Work work = new Work.Builder(TestWorker.class).build();
        String id = work.getId();
        insertWork(work);
        mTimedBackgroundProcessor.process(id);
        assertThat(mTimedBackgroundProcessor.cancel(id, true), is(true));
        verify(mMockWorkTimer).stopTimer(id);
    }

    @Test
    @SmallTest
    public void testOnExecuted_stopsTimer() {
        final String testId = "workSpecId";
        mTimedBackgroundProcessor.onExecuted(testId, false);
        verify(mMockExecutionListener).onExecuted(testId, false);
        verify(mMockWorkTimer).stopTimer(testId);
    }

    @Test
    @SmallTest
    public void testOnTimeLimitedExceeded() {
        // TODO(rahulrav): Verify that ExecutionListener#onCancelled(id) was called.
        // mTimedBackgroundProcessor.onTimeLimitedExceeded(id);
        // verify(mMockExecutionListener).onCancelled(id);
    }
}
