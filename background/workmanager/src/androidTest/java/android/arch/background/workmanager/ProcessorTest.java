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
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.Executors;

@RunWith(AndroidJUnit4.class)
public class ProcessorTest {
    private WorkDatabase mWorkDatabase;
    private Processor mProcessor;

    @Before
    public void setUp() {
        Context appContext = InstrumentationRegistry.getTargetContext().getApplicationContext();
        mWorkDatabase = WorkDatabase.create(appContext, true);
        mProcessor = new Processor(
                appContext,
                mWorkDatabase,
                mock(Scheduler.class),
                Executors.newSingleThreadScheduledExecutor()) {
        };
    }

    @After
    public void tearDown() {
        mWorkDatabase.close();
    }

    /* TODO(xbhatnag): Solve race condition without thread sleeps or drain
    @Test
    @SmallTest
    public void testProcess_noWorkInitialDelay() throws InterruptedException {
        Work work = new Work.Builder(InfiniteTestWorker.class).build();
        mWorkDatabase.workSpecDao().insertWorkSpec(work.getWorkSpec());
        mProcessor.process(work.getId(), work.getWorkSpec().calculateDelay());
        // Race condition here
        assertThat(mWorkDatabase.workSpecDao().getWorkSpecStatus(work.getId()),
                is(Work.STATUS_RUNNING));
    } */

    @Test
    @SmallTest
    public void testCancel_invalidWorkId() {
        String id = "INVALID_WORK_ID";
        assertThat(mProcessor.cancel(id, true), is(false));
        assertThat(mProcessor.cancel(id, false), is(false));
    }
}
