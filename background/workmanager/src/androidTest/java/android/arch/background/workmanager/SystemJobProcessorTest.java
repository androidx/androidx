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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import android.arch.background.workmanager.model.Dependency;
import android.arch.background.workmanager.model.WorkSpec;
import android.arch.background.workmanager.systemjob.SystemJobProcessor;
import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;

@RunWith(AndroidJUnit4.class)
public class SystemJobProcessorTest {

    private WorkDatabase mWorkDatabase;
    private ExecutionListener mMockListener;
    private Scheduler mMockScheduler;
    private SystemJobProcessor mSystemJobProcessor;

    @Before
    public void setUp() {
        Context appContext = InstrumentationRegistry.getTargetContext().getApplicationContext();
        mWorkDatabase = WorkDatabase.create(appContext, true);
        mMockListener = mock(ExecutionListener.class);
        mMockScheduler = mock(Scheduler.class);
        mSystemJobProcessor =
                new SystemJobProcessor(appContext, mWorkDatabase, mMockScheduler, mMockListener);
    }

    @After
    public void tearDown() {
        mWorkDatabase.close();
    }

    @Test
    @SmallTest
    public void testSimpleWorker() throws InterruptedException {
        Work work = new Work.Builder(TestWorker.class).build();
        mWorkDatabase.workSpecDao().insertWorkSpec(work.getWorkSpec());
        mSystemJobProcessor.process(work.getId());
        Thread.sleep(1000L);
        verify(mMockListener).onExecuted(work.getId(), WorkerWrapper.RESULT_SUCCEEDED);
        assertThat(mWorkDatabase.workSpecDao().getWorkSpecStatus(work.getId()),
                is(Work.STATUS_SUCCEEDED));
        verify(mMockScheduler).schedule();
    }

    @Test
    @SmallTest
    public void testDependencies() throws InterruptedException {
        Work work1 = new Work.Builder(TestWorker.class).build();
        Work work2 =
                new Work.Builder(TestWorker.class).withInitialStatus(Work.STATUS_BLOCKED).build();
        mWorkDatabase.workSpecDao().insertWorkSpec(work1.getWorkSpec());
        mWorkDatabase.workSpecDao().insertWorkSpec(work2.getWorkSpec());
        mWorkDatabase.dependencyDao().insertDependency(
                new Dependency(work2.getId(), work1.getId()));
        mSystemJobProcessor.process(work1.getId());
        Thread.sleep(1000L);
        verify(mMockListener).onExecuted(work1.getId(), WorkerWrapper.RESULT_SUCCEEDED);
        assertThat(mWorkDatabase.workSpecDao().getWorkSpecStatus(work1.getId()),
                is(Work.STATUS_SUCCEEDED));
        ArgumentCaptor<WorkSpec> captor = ArgumentCaptor.forClass(WorkSpec.class);
        verify(mMockScheduler).schedule(captor.capture());
        assertThat(captor.getValue().getId(), is(work2.getId()));
    }
}
