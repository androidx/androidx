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
import android.arch.background.workmanager.TestWorker;
import android.arch.background.workmanager.Work;
import android.arch.background.workmanager.WorkDatabase;
import android.arch.background.workmanager.WorkSpecs;
import android.arch.background.workmanager.executors.SynchronousExecutorService;
import android.arch.background.workmanager.model.Dependency;
import android.arch.background.workmanager.model.WorkSpec;
import android.arch.background.workmanager.model.WorkSpecDao;
import android.arch.core.executor.testing.CountingTaskExecutorRule;
import android.arch.lifecycle.Lifecycle;
import android.arch.lifecycle.LifecycleOwner;
import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@SmallTest
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
        mForegroundProcessor = new SynchronousForegroundProcessor(
                appContext,
                mWorkDatabase,
                mock(Scheduler.class),
                mLifecycleOwner);
    }

    @After
    public void tearDown() {
        mLifecycleOwner.mLifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP);
        try {
            drain();
        } catch (TimeoutException | InterruptedException e) {
            // Do nothing
        } finally {
            mWorkDatabase.close();
        }
    }

    @Test
    public void testIsActive_onStart_returnsTrue() throws TimeoutException, InterruptedException {
        mLifecycleOwner.mLifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP);
        mLifecycleOwner.mLifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START);
        drain();
        assertThat(mForegroundProcessor.isActive(), is(true));
    }

    @Test
    public void testIsActive_onStop_returnsFalse() throws TimeoutException, InterruptedException {
        assertThat(mForegroundProcessor.isActive(), is(true));
        mLifecycleOwner.mLifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP);
        drain();
        assertThat(mForegroundProcessor.isActive(), is(false));
    }

    @Test
    public void testProcess_singleWorker() throws InterruptedException {
        WorkSpec workSpec = WorkSpecs.getWorkSpec(TestWorker.class);
        mWorkDatabase.workSpecDao().insertWorkSpec(workSpec);
        mForegroundProcessor.process(workSpec.getId(), 0L);
        assertThat(mWorkDatabase.workSpecDao().getWorkSpecStatus(workSpec.getId()),
                is(Work.STATUS_SUCCEEDED));
    }

    @Test
    @SmallTest
    public void testProcess_dependentWorkers() throws TimeoutException, InterruptedException {
        WorkSpec prerequisite = WorkSpecs.getWorkSpec(TestWorker.class);
        WorkSpec workSpec = WorkSpecs.getWorkSpec(TestWorker.class, STATUS_BLOCKED);

        WorkSpecDao workSpecDao = mWorkDatabase.workSpecDao();
        workSpecDao.insertWorkSpec(prerequisite);
        workSpecDao.insertWorkSpec(workSpec);
        mWorkDatabase.dependencyDao().insertDependency(
                new Dependency(workSpec.getId(), prerequisite.getId()));

        mForegroundProcessor.process(prerequisite.getId(), 0L);

        assertThat(workSpecDao.getWorkSpecStatus(prerequisite.getId()), is(STATUS_SUCCEEDED));
        drain();
        assertThat(workSpecDao.getWorkSpecStatus(workSpec.getId()), is(STATUS_SUCCEEDED));
    }

    private void drain() throws TimeoutException, InterruptedException {
        mExecutorRule.drainTasks(1, TimeUnit.MINUTES);
    }

    /**
     * A test {@link ForegroundProcessor} with a customized {@link ExecutorService}.
     * TODO(sumir): Make the ExecutorService passed in.
     */
    private static class SynchronousForegroundProcessor extends ForegroundProcessor {

        SynchronousForegroundProcessor(
                Context appContext,
                WorkDatabase workDatabase,
                Scheduler scheduler,
                LifecycleOwner lifecycleOwner) {
            super(appContext, workDatabase, scheduler, lifecycleOwner);
        }

        @Override
        public ScheduledExecutorService createExecutorService() {
            return new SynchronousExecutorService();
        }
    }
}