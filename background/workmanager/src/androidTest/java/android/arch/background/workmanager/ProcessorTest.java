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

package android.arch.background.workmanager;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;

import android.arch.background.workmanager.worker.InfiniteTestWorker;
import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@RunWith(AndroidJUnit4.class)
public class ProcessorTest extends DatabaseTest {
    private static final long ASYNC_WAIT_DURATION = 2000L;
    private Processor mProcessor;

    @Before
    public void setUp() {
        Context appContext = InstrumentationRegistry.getTargetContext().getApplicationContext();
        mProcessor = new Processor(
                appContext,
                mDatabase,
                mock(Scheduler.class),
                Executors.newSingleThreadScheduledExecutor()) {
        };
    }

    @After
    public void tearDown() {
        mProcessor = null;
    }

    @Test
    @SmallTest
    public void testProcess_noWorkInitialDelay() throws InterruptedException {
        Work work = new Work.Builder(InfiniteTestWorker.class).build();
        insertBaseWork(work);
        mProcessor.process(work.getId(), work.getWorkSpec().calculateDelay());
        Thread.sleep(ASYNC_WAIT_DURATION);
        assertThat(mDatabase.workSpecDao().getWorkSpecStatus(work.getId()),
                is(Work.STATUS_RUNNING));
    }

    @Test
    @SmallTest
    public void testCancel_invalidWorkId() {
        String id = "INVALID_WORK_ID";
        assertThat(mProcessor.cancel(id, true), is(false));
        assertThat(mProcessor.cancel(id, false), is(false));
    }

    @Test
    @SmallTest
    public void testProcess_doesNotProcessTwice() {
        Work work = new Work.Builder(InfiniteTestWorker.class).build();
        String id = work.getId();
        insertBaseWork(work);
        mProcessor.process(id, 0);
        assertThat(mProcessor.mEnqueuedWorkMap, hasKey(id));
        Future future = mProcessor.mEnqueuedWorkMap.get(id);
        mProcessor.process(id, 0);
        assertThat(mProcessor.mEnqueuedWorkMap, hasKey(id));
        assertThat(mProcessor.mEnqueuedWorkMap.get(id), is(future));
    }

    @Test
    @SmallTest
    public void testHasWork() {
        Work work = new Work.Builder(InfiniteTestWorker.class).build();
        insertBaseWork(work);

        assertThat(mProcessor.hasWork(), is(false));
        mProcessor.process(work.getId(), 0L);
        assertThat(mProcessor.hasWork(), is(true));
    }
}
