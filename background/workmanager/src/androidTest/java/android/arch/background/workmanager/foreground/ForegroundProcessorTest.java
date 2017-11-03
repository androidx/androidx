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
package android.arch.background.workmanager.foreground;

import static android.arch.background.workmanager.Work.STATUS_BLOCKED;
import static android.arch.background.workmanager.Work.STATUS_SUCCEEDED;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;

import android.arch.background.workmanager.Scheduler;
import android.arch.background.workmanager.TestLifecycleOwner;
import android.arch.background.workmanager.Work;
import android.arch.background.workmanager.WorkDatabase;
import android.arch.background.workmanager.executors.SynchronousExecutorService;
import android.arch.background.workmanager.model.Dependency;
import android.arch.background.workmanager.model.WorkSpec;
import android.arch.background.workmanager.model.WorkSpecDao;
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
public class ForegroundProcessorTest {

    @Rule
    public CountingTaskExecutorRule mExecutorRule = new CountingTaskExecutorRule();

    private WorkDatabase mWorkDatabase;
    private ForegroundProcessor mForegroundProcessor;
    private TestLifecycleOwner mLifecycleOwner;

    @Before
    public void setUp() {
        Context appContext = InstrumentationRegistry.getTargetContext().getApplicationContext();
        mLifecycleOwner = new TestLifecycleOwner();
        mWorkDatabase = WorkDatabase.create(appContext, true);
        mForegroundProcessor = new ForegroundProcessor(
                appContext,
                mWorkDatabase,
                mock(Scheduler.class),
                mLifecycleOwner,
                new SynchronousExecutorService());
    }

    @After
    public void tearDown() throws TimeoutException, InterruptedException {
        postLifecycleStopOnMainThread();
        try {
            drain();
        } finally {
            mWorkDatabase.close();
        }
    }

    @Test
    @SmallTest
    public void testProcess_singleWorker() throws InterruptedException {
        WorkSpec workSpec = new Work.Builder(TestWorker.class)
                .build()
                .getWorkSpec();
        mWorkDatabase.workSpecDao().insertWorkSpec(workSpec);
        mForegroundProcessor.process(workSpec.getId(), 0L);
        assertThat(mWorkDatabase.workSpecDao().getWorkSpecStatus(workSpec.getId()),
                is(Work.STATUS_SUCCEEDED));
    }

    @Test
    @SmallTest
    public void testProcess_dependentWorkers() throws TimeoutException, InterruptedException {
        WorkSpec prerequisite = new Work.Builder(TestWorker.class)
                .build()
                .getWorkSpec();
        WorkSpec workSpec = new Work.Builder(TestWorker.class)
                .withInitialStatus(STATUS_BLOCKED)
                .build()
                .getWorkSpec();

        WorkSpecDao workSpecDao = mWorkDatabase.workSpecDao();
        workSpecDao.insertWorkSpec(prerequisite);
        workSpecDao.insertWorkSpec(workSpec);
        mWorkDatabase.dependencyDao().insertDependency(
                new Dependency(workSpec.getId(), prerequisite.getId()));
        drain();
        mForegroundProcessor.process(prerequisite.getId(), 0L);
        drain();

        assertThat(workSpecDao.getWorkSpecStatus(prerequisite.getId()), is(STATUS_SUCCEEDED));
        assertThat(workSpecDao.getWorkSpecStatus(workSpec.getId()), is(STATUS_SUCCEEDED));
    }

    @Test
    @SmallTest
    public void testProcess_processorInactive() throws TimeoutException, InterruptedException {
        postLifecycleStopOnMainThread();
        drain();
        WorkSpec workSpec = new Work.Builder(TestWorker.class)
                .build()
                .getWorkSpec();
        mWorkDatabase.workSpecDao().insertWorkSpec(workSpec);
        mForegroundProcessor.process(workSpec.getId(), 0L);
        drain();
        assertThat(
                mWorkDatabase.workSpecDao().getWorkSpecStatus(workSpec.getId()),
                is(Work.STATUS_ENQUEUED));
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