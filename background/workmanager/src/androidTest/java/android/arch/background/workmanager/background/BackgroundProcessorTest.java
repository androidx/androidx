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
package android.arch.background.workmanager.background;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import android.arch.background.workmanager.ExecutionListener;
import android.arch.background.workmanager.Scheduler;
import android.arch.background.workmanager.Work;
import android.arch.background.workmanager.WorkDatabase;
import android.arch.background.workmanager.executors.SynchronousExecutorService;
import android.arch.background.workmanager.model.WorkSpec;
import android.arch.background.workmanager.utils.taskexecutor.InstantTaskExecutorRule;
import android.arch.background.workmanager.worker.TestWorker;
import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class BackgroundProcessorTest {
    private WorkDatabase mWorkDatabase;
    private ExecutionListener mMockListener;
    private BackgroundProcessor mProcessor;

    @Rule
    public InstantTaskExecutorRule mRule = new InstantTaskExecutorRule();

    @Before
    public void setUp() {
        Context appContext = InstrumentationRegistry.getTargetContext().getApplicationContext();
        mWorkDatabase = WorkDatabase.create(appContext, true);
        mMockListener = mock(ExecutionListener.class);
        mProcessor = new BackgroundProcessor(
                appContext,
                mWorkDatabase,
                mock(Scheduler.class),
                mMockListener,
                new SynchronousExecutorService());
    }

    @After
    public void tearDown() {
        mWorkDatabase.close();
    }

    @Test
    @SmallTest
    public void testOnExecuted() throws InterruptedException {
        WorkSpec workSpec = new Work.Builder(TestWorker.class).build().getWorkSpec();
        String workSpecId = workSpec.getId();

        mWorkDatabase.workSpecDao().insertWorkSpec(workSpec);
        mProcessor.process(workSpecId, 0L);

        verify(mMockListener).onExecuted(workSpecId, false);
    }
}
