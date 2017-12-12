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
package android.arch.background.workmanager.impl.foreground;

import static android.arch.background.workmanager.BaseWork.STATUS_BLOCKED;
import static android.arch.background.workmanager.BaseWork.STATUS_ENQUEUED;
import static android.arch.background.workmanager.BaseWork.STATUS_SUCCEEDED;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;

import android.arch.background.workmanager.DatabaseTest;
import android.arch.background.workmanager.TestLifecycleOwner;
import android.arch.background.workmanager.Work;
import android.arch.background.workmanager.executors.SynchronousExecutorService;
import android.arch.background.workmanager.impl.Scheduler;
import android.arch.background.workmanager.impl.model.Dependency;
import android.arch.background.workmanager.worker.TestWorker;
import android.arch.core.executor.ArchTaskExecutor;
import android.arch.core.executor.testing.CountingTaskExecutorRule;
import android.arch.lifecycle.Lifecycle;
import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@RunWith(AndroidJUnit4.class)
public class ForegroundProcessorTest extends DatabaseTest {

    @Rule
    public CountingTaskExecutorRule mExecutorRule = new CountingTaskExecutorRule();

    private ForegroundProcessor mForegroundProcessor;
    private TestLifecycleOwner mLifecycleOwner;

    @Before
    public void setUp() {
        Context appContext = InstrumentationRegistry.getTargetContext().getApplicationContext();
        mLifecycleOwner = new TestLifecycleOwner();
        mForegroundProcessor = new ForegroundProcessor(
                appContext,
                mDatabase,
                mock(Scheduler.class),
                mLifecycleOwner,
                new SynchronousExecutorService());
    }

    @After
    @Override
    public void closeDb() {
        postLifecycleStopOnMainThread();
        try {
            drain();
        } catch (Exception e) {
            // Do nothing.
        } finally {
            super.closeDb();
        }
    }

    @Test
    @SmallTest
    public void testProcess_singleWorker() throws TimeoutException, InterruptedException {
        Work work = Work.newBuilder(TestWorker.class).build();
        insertWork(work);
        drain();
        mForegroundProcessor.process(work.getId());
        drain();
        assertThat(mDatabase.workSpecDao().getWorkSpecStatus(work.getId()),
                is(STATUS_SUCCEEDED));
    }

    @Test
    @SmallTest
    public void testProcess_dependentWorkers() throws TimeoutException, InterruptedException {
        Work prerequisite = Work.newBuilder(TestWorker.class).build();
        Work workSpec = Work.newBuilder(TestWorker.class)
                .withInitialStatus(STATUS_BLOCKED)
                .build();

        insertWork(prerequisite);
        insertWork(workSpec);
        mDatabase.dependencyDao().insertDependency(
                new Dependency(workSpec.getId(), prerequisite.getId()));
        drain();
        mForegroundProcessor.process(prerequisite.getId());
        drain();

        assertThat(mDatabase.workSpecDao().getWorkSpecStatus(prerequisite.getId()),
                is(STATUS_SUCCEEDED));
        assertThat(mDatabase.workSpecDao().getWorkSpecStatus(workSpec.getId()),
                is(STATUS_SUCCEEDED));
    }

    @Test
    @SmallTest
    public void testProcess_processorInactive() throws TimeoutException, InterruptedException {
        postLifecycleStopOnMainThread();
        drain();
        Work work = Work.newBuilder(TestWorker.class).build();
        insertWork(work);
        drain();
        mForegroundProcessor.process(work.getId());
        drain();
        assertThat(mDatabase.workSpecDao().getWorkSpecStatus(work.getId()),
                is(STATUS_ENQUEUED));
    }

    private void postLifecycleStopOnMainThread() {
        ArchTaskExecutor.getInstance().postToMainThread(new Runnable() {
            @Override
            public void run() {
                mLifecycleOwner.mLifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP);
            }
        });
    }

    private void drain() throws TimeoutException, InterruptedException {
        mExecutorRule.drainTasks(1, TimeUnit.MINUTES);
    }
}
