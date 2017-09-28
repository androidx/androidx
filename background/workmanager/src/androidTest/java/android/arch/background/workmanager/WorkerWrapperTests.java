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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;

import android.arch.background.workmanager.model.Constraints;
import android.arch.background.workmanager.model.WorkSpec;
import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

import java.util.concurrent.Executors;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class WorkerWrapperTests {
    private static final long LISTENER_SLEEP_DURATION = 2000;
    private WorkDatabase mDatabase;
    private WorkSpecDao mWorkSpecDao;
    private DependencyDao mDependencyDao;
    private Context mContext;
    private ExecutionListener mMockListener;

    @Before
    public void setUp() {
        mContext = InstrumentationRegistry.getTargetContext();
        mDatabase = WorkDatabase.create(mContext, true);
        mWorkSpecDao = mDatabase.workSpecDao();
        mDependencyDao = mDatabase.dependencyDao();
        mMockListener = Mockito.mock(ExecutionListener.class);
    }

    @After
    public void tearDown() {
        //TODO(xbhatnag): Include any tear down needed here.
    }

    @Test
    public void testSuccess() throws InterruptedException {
        Work work = new Work.Builder(TestWorker.class).build();
        mWorkSpecDao.insertWorkSpec(work.getWorkSpec());
        new WorkerWrapper.Builder(mContext, mDatabase, work.getId())
                .withListener(mMockListener)
                .build()
                .run();
        Thread.sleep(LISTENER_SLEEP_DURATION);
        Mockito.verify(mMockListener).onExecuted(work.getId(), WorkerWrapper.RESULT_SUCCEEDED);
        assertEquals(Work.STATUS_SUCCEEDED, mWorkSpecDao.getWorkSpecStatus(work.getId()));
    }

    @Test
    public void testPermanentErrorWithInvalidWorkSpecId() throws InterruptedException {
        final String invalidWorkSpecId = "INVALID_ID";
        new WorkerWrapper.Builder(mContext, mDatabase, invalidWorkSpecId)
                .withListener(mMockListener)
                .build()
                .run();
        Thread.sleep(LISTENER_SLEEP_DURATION);
        Mockito.verify(mMockListener)
                .onExecuted(invalidWorkSpecId, WorkerWrapper.RESULT_PERMANENT_ERROR);
    }

    @Test
    public void testNotEnqueued() throws InterruptedException {
        Work work = new Work.Builder(TestWorker.class).build();
        work.getWorkSpec().setStatus(Work.STATUS_RUNNING);
        mWorkSpecDao.insertWorkSpec(work.getWorkSpec());
        new WorkerWrapper.Builder(mContext, mDatabase, work.getId())
                .withListener(mMockListener)
                .build()
                .run();
        Thread.sleep(LISTENER_SLEEP_DURATION);
        Mockito.verify(mMockListener)
                .onExecuted(work.getId(), WorkerWrapper.RESULT_NOT_ENQUEUED);
    }

    @Test
    public void testPermanentErrorWithInvalidWorkerClass() throws InterruptedException {
        Work work = new Work.Builder(TestWorker.class).build();
        work.getWorkSpec().setWorkerClassName("INVALID_CLASS_NAME");
        mWorkSpecDao.insertWorkSpec(work.getWorkSpec());
        new WorkerWrapper.Builder(mContext, mDatabase, work.getId())
                .withListener(mMockListener)
                .build()
                .run();
        Thread.sleep(LISTENER_SLEEP_DURATION);
        Mockito.verify(mMockListener)
                .onExecuted(work.getId(), WorkerWrapper.RESULT_PERMANENT_ERROR);
        assertEquals(Work.STATUS_FAILED, mWorkSpecDao.getWorkSpecStatus(work.getId()));
    }

    @Test
    public void testFailed() throws InterruptedException {
        Work work = new Work.Builder(ExceptionTestWorker.class).build();
        mWorkSpecDao.insertWorkSpec(work.getWorkSpec());
        new WorkerWrapper.Builder(mContext, mDatabase, work.getId())
                .withListener(mMockListener)
                .build()
                .run();
        Thread.sleep(LISTENER_SLEEP_DURATION);
        Mockito.verify(mMockListener).onExecuted(work.getId(), WorkerWrapper.RESULT_FAILED);
        assertEquals(Work.STATUS_FAILED, mWorkSpecDao.getWorkSpecStatus(work.getId()));
    }

    @Test
    public void testRunning() throws InterruptedException {
        Work work = new Work.Builder(SleepTestWorker.class).build();
        mWorkSpecDao.insertWorkSpec(work.getWorkSpec());
        WorkerWrapper wrapper = new WorkerWrapper.Builder(mContext, mDatabase, work.getId())
                .withListener(mMockListener)
                .build();
        Executors.newSingleThreadExecutor().submit(wrapper);
        Thread.sleep(LISTENER_SLEEP_DURATION);
        assertEquals(Work.STATUS_RUNNING, mWorkSpecDao.getWorkSpecStatus(work.getId()));
        Thread.sleep(SleepTestWorker.SLEEP_DURATION);
        Mockito.verify(mMockListener).onExecuted(work.getId(), WorkerWrapper.RESULT_SUCCEEDED);
    }

    @Test
    public void testDependenciesRemoved() throws InterruptedException {
        Work work1 = new Work.Builder(TestWorker.class).build();
        Work work2 = new Work.Builder(TestWorker.class).build();
        Dependency dependency = new Dependency(work2.getId(), work1.getId());
        mWorkSpecDao.insertWorkSpec(work1.getWorkSpec());
        mWorkSpecDao.insertWorkSpec(work2.getWorkSpec());
        mDependencyDao.insertDependency(dependency);

        assertTrue(mDependencyDao.hasDependencies(work2.getId()));
        new WorkerWrapper.Builder(mContext, mDatabase, work1.getId())
                .withListener(mMockListener)
                .build()
                .run();
        assertFalse(mDependencyDao.hasDependencies(work2.getId()));
    }

    @Test
    public void testConstraints() throws InterruptedException {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(Constraints.NETWORK_TYPE_CONNECTED)
                .setRequiresBatteryNotLow(true)
                .setRequiresCharging(true)
                .setRequiresDeviceIdle(true)
                .setRequiresStorageNotLow(true)
                .build();

        Work work = new Work.Builder(TestWorker.class)
                .withConstraints(constraints)
                .build();
        mWorkSpecDao.insertWorkSpec(work.getWorkSpec());

        ConstraintsChecker mockChecker = Mockito.mock(ConstraintsChecker.class);
        doReturn(false).when(mockChecker).areAllConstraintsMet(any(WorkSpec.class));

        new WorkerWrapper.Builder(mContext, mDatabase, work.getId())
                .withListener(mMockListener)
                .verifyAllConstraints(mockChecker)
                .build()
                .run();

        Thread.sleep(LISTENER_SLEEP_DURATION);
        Mockito.verify(mMockListener).onExecuted(work.getId(), WorkerWrapper.RESULT_RESCHEDULED);
        assertEquals(Work.STATUS_ENQUEUED, mWorkSpecDao.getWorkSpecStatus(work.getId()));

        doReturn(true).when(mockChecker).areAllConstraintsMet(any(WorkSpec.class));

        new WorkerWrapper.Builder(mContext, mDatabase, work.getId())
                .withListener(mMockListener)
                .verifyAllConstraints(mockChecker)
                .build()
                .run();

        Thread.sleep(LISTENER_SLEEP_DURATION);
        Mockito.verify(mMockListener).onExecuted(work.getId(), WorkerWrapper.RESULT_SUCCEEDED);
        assertEquals(Work.STATUS_SUCCEEDED, mWorkSpecDao.getWorkSpecStatus(work.getId()));
    }
}
