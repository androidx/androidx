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
    private WorkDatabase mDatabase;
    private WorkSpecDao mWorkSpecDao;
    private Context mContext;

    @Before
    public void setUp() {
        mContext = InstrumentationRegistry.getTargetContext();
        mDatabase = WorkDatabase.getInMemoryInstance(mContext);
        mWorkSpecDao = mDatabase.workSpecDao();
    }

    @After
    public void tearDown() {
        //TODO(xbhatnag): Include any tear down needed here.
    }

    @Test
    public void testSuccess() {
        Work work = new Work.Builder(TestWorker.class).build();
        mWorkSpecDao.insertWorkSpec(work.getWorkSpec());
        WorkerWrapper.Listener mockListener = Mockito.mock(WorkerWrapper.Listener.class);
        new WorkerWrapper(mContext, mDatabase, work.getId(), mockListener).run();
        Mockito.verify(mockListener).onExecuted(work.getId(), WorkerWrapper.RESULT_SUCCEEDED);
        assertEquals(Work.STATUS_SUCCEEDED, mWorkSpecDao.getWorkSpecStatus(work.getId()));
    }

    @Test
    public void testPermanentErrorWithInvalidWorkSpecId() {
        final String invalidWorkSpecId = "INVALID_ID";
        WorkerWrapper.Listener mockListener = Mockito.mock(WorkerWrapper.Listener.class);
        new WorkerWrapper(mContext, mDatabase, invalidWorkSpecId, mockListener).run();
        Mockito.verify(mockListener)
                .onExecuted(invalidWorkSpecId, WorkerWrapper.RESULT_PERMANENT_ERROR);
    }

    @Test
    public void testNotEnqueued() {
        Work work = new Work.Builder(TestWorker.class).build();
        work.getWorkSpec().mStatus = Work.STATUS_RUNNING;
        mWorkSpecDao.insertWorkSpec(work.getWorkSpec());
        WorkerWrapper.Listener mockListener = Mockito.mock(WorkerWrapper.Listener.class);
        new WorkerWrapper(mContext, mDatabase, work.getId(), mockListener).run();
        Mockito.verify(mockListener)
                .onExecuted(work.getId(), WorkerWrapper.RESULT_NOT_ENQUEUED);
    }

    @Test
    public void testPermanentErrorWithInvalidWorkerClass() {
        Work work = new Work.Builder(TestWorker.class).build();
        work.getWorkSpec().mWorkerClassName = "INVALID_CLASS_NAME";
        mWorkSpecDao.insertWorkSpec(work.getWorkSpec());
        WorkerWrapper.Listener mockListener = Mockito.mock(WorkerWrapper.Listener.class);
        new WorkerWrapper(mContext, mDatabase, work.getId(), mockListener).run();
        Mockito.verify(mockListener)
                .onExecuted(work.getId(), WorkerWrapper.RESULT_PERMANENT_ERROR);
        assertEquals(Work.STATUS_FAILED, mWorkSpecDao.getWorkSpecStatus(work.getId()));
    }

    @Test
    public void testFailed() throws InterruptedException {
        Work work = new Work.Builder(ExceptionTestWorker.class).build();
        mWorkSpecDao.insertWorkSpec(work.getWorkSpec());
        WorkerWrapper.Listener mockListener = Mockito.mock(WorkerWrapper.Listener.class);
        new WorkerWrapper(mContext, mDatabase, work.getId(), mockListener).run();
        Mockito.verify(mockListener).onExecuted(work.getId(), WorkerWrapper.RESULT_FAILED);
        assertEquals(Work.STATUS_FAILED, mWorkSpecDao.getWorkSpecStatus(work.getId()));
    }

    @Test
    public void testRunning() throws InterruptedException {
        Work work = new Work.Builder(SleepTestWorker.class).build();
        mWorkSpecDao.insertWorkSpec(work.getWorkSpec());
        WorkerWrapper.Listener mockListener = Mockito.mock(WorkerWrapper.Listener.class);
        Runnable wrapper = new WorkerWrapper(mContext, mDatabase, work.getId(), mockListener);
        Executors.newSingleThreadExecutor().submit(wrapper);
        Thread.sleep(2000);
        assertEquals(Work.STATUS_RUNNING, mWorkSpecDao.getWorkSpecStatus(work.getId()));
    }
}
