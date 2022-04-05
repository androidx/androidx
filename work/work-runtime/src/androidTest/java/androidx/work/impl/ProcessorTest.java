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

package androidx.work.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;
import androidx.work.Configuration;
import androidx.work.DatabaseTest;
import androidx.work.OneTimeWorkRequest;
import androidx.work.impl.utils.taskexecutor.InstantWorkTaskExecutor;
import androidx.work.worker.InfiniteTestWorker;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collections;

@RunWith(AndroidJUnit4.class)
public class ProcessorTest extends DatabaseTest {

    private Scheduler mMockScheduler;
    private Processor mProcessor;

    @Before
    public void setUp() {
        Context appContext = ApplicationProvider.getApplicationContext().getApplicationContext();
        Configuration configuration = new Configuration.Builder().build();
        mMockScheduler = mock(Scheduler.class);
        mProcessor = new Processor(
                appContext,
                configuration,
                new InstantWorkTaskExecutor(),
                mDatabase,
                Collections.singletonList(mMockScheduler)) {
        };
    }

    @Test
    @SmallTest
    public void testStopWork_invalidWorkId() {
        String id = "INVALID_WORK_ID";
        assertThat(mProcessor.stopWork(new WorkRunId(id)), is(false));
    }

    @Test
    @SmallTest
    public void testStartWork_doesNotStartWorkTwice() {
        OneTimeWorkRequest work = new OneTimeWorkRequest.Builder(InfiniteTestWorker.class).build();
        String id = work.getStringId();
        insertWork(work);
        assertThat(mProcessor.startWork(new WorkRunId(id)), is(true));
        assertThat(mProcessor.startWork(new WorkRunId(id)), is(false));
    }

    @Test
    @SmallTest
    public void testHasWork() {
        OneTimeWorkRequest work = new OneTimeWorkRequest.Builder(InfiniteTestWorker.class).build();
        insertWork(work);

        assertThat(mProcessor.hasWork(), is(false));
        mProcessor.startWork(new WorkRunId(work.getStringId()));
        assertThat(mProcessor.hasWork(), is(true));
    }

    @Test
    @SmallTest
    public void testDontCancelWhenNeedsReschedule() {
        mProcessor.onExecuted("dummy", true);
        verify(mMockScheduler, never()).cancel("dummy");
    }
}
