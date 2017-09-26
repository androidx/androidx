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
    private static final long LISTENER_SLEEP_DURATION = 2000;
    private WorkDatabase mDatabase;
    private WorkSpecDao mWorkSpecDao;
    private Context mContext;
    private ExecutionListener mMockListener;

    @Before
    public void setUp() {
        mContext = InstrumentationRegistry.getTargetContext();
        mDatabase = WorkDatabase.create(mContext, true);
        mWorkSpecDao = mDatabase.workSpecDao();
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
        new WorkerWrapper(mContext, mDatabase, work.getId(), mMockListener).run();
        Thread.sleep(LISTENER_SLEEP_DURATION);
        Mockito.verify(mMockListener).onExecuted(work.getId(), WorkerWrapper.RESULT_SUCCEEDED);
        assertEquals(Work.STATUS_SUCCEEDED, mWorkSpecDao.getWorkSpecStatus(work.getId()));
    }

    @Test
    public void testPermanentErrorWithInvalidWorkSpecId() throws InterruptedException {
        final String invalidWorkSpecId = "INVALID_ID";
        new WorkerWrapper(mContext, mDatabase, invalidWorkSpecId, mMockListener).run();
        Thread.sleep(LISTENER_SLEEP_DURATION);
        Mockito.verify(mMockListener)
                .onExecuted(invalidWorkSpecId, WorkerWrapper.RESULT_PERMANENT_ERROR);
    }

    @Test
    public void testNotEnqueued() throws InterruptedException {
        Work work = new Work.Builder(TestWorker.class).build();
        work.getWorkSpec().setStatus(Work.STATUS_RUNNING);
        mWorkSpecDao.insertWorkSpec(work.getWorkSpec());
        new WorkerWrapper(mContext, mDatabase, work.getId(), mMockListener).run();
        Thread.sleep(LISTENER_SLEEP_DURATION);
        Mockito.verify(mMockListener)
                .onExecuted(work.getId(), WorkerWrapper.RESULT_NOT_ENQUEUED);
    }

    @Test
    public void testPermanentErrorWithInvalidWorkerClass() throws InterruptedException {
        Work work = new Work.Builder(TestWorker.class).build();
        work.getWorkSpec().setWorkerClassName("INVALID_CLASS_NAME");
        mWorkSpecDao.insertWorkSpec(work.getWorkSpec());
        new WorkerWrapper(mContext, mDatabase, work.getId(), mMockListener).run();
        Thread.sleep(LISTENER_SLEEP_DURATION);
        Mockito.verify(mMockListener)
                .onExecuted(work.getId(), WorkerWrapper.RESULT_PERMANENT_ERROR);
        assertEquals(Work.STATUS_FAILED, mWorkSpecDao.getWorkSpecStatus(work.getId()));
    }

    @Test
    public void testFailed() throws InterruptedException {
        Work work = new Work.Builder(ExceptionTestWorker.class).build();
        mWorkSpecDao.insertWorkSpec(work.getWorkSpec());
        new WorkerWrapper(mContext, mDatabase, work.getId(), mMockListener).run();
        Thread.sleep(LISTENER_SLEEP_DURATION);
        Mockito.verify(mMockListener).onExecuted(work.getId(), WorkerWrapper.RESULT_FAILED);
        assertEquals(Work.STATUS_FAILED, mWorkSpecDao.getWorkSpecStatus(work.getId()));
    }

    @Test
    public void testRunning() throws InterruptedException {
        Work work = new Work.Builder(SleepTestWorker.class).build();
        mWorkSpecDao.insertWorkSpec(work.getWorkSpec());
        WorkerWrapper wrapper = new WorkerWrapper(mContext, mDatabase, work.getId(), mMockListener);
        Executors.newSingleThreadExecutor().submit(wrapper);
        Thread.sleep(LISTENER_SLEEP_DURATION);
        assertEquals(Work.STATUS_RUNNING, mWorkSpecDao.getWorkSpecStatus(work.getId()));
        Thread.sleep(SleepTestWorker.SLEEP_DURATION);
        Mockito.verify(mMockListener).onExecuted(work.getId(), WorkerWrapper.RESULT_SUCCEEDED);
    }
}
