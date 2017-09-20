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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

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
@SmallTest
public class WorkerTests {
    private WorkDatabase mDatabase;
    private WorkSpecDao mWorkSpecDao;
    private Context mContext;

    @Before
    public void setup() {
        mContext = InstrumentationRegistry.getTargetContext();
        mDatabase = WorkDatabase.getInMemoryInstance(mContext);
        mWorkSpecDao = mDatabase.workSpecDao();
    }

    @After
    public void tearDown() {
        //TODO(sumir): Include any tear down needed.
    }

    @Test
    public void testRunsSuccessfully() throws InterruptedException {
        Work work = new Work.Builder(TestWorker.class).build();
        mWorkSpecDao.insertWorkSpec(work.getWorkSpec());
        Worker worker = Worker.fromWorkSpec(mContext, mDatabase, work.getWorkSpec());
        assertNotNull(worker);
        worker.call();
        assertEquals(Work.STATUS_SUCCEEDED, mWorkSpecDao.getWorkSpecStatus(work.getId()));
    }

    @Test
    public void testUncaughtException() throws InterruptedException {
        Work work = new Work.Builder(ExceptionTestWorker.class).build();
        mWorkSpecDao.insertWorkSpec(work.getWorkSpec());
        Worker worker = Worker.fromWorkSpec(mContext, mDatabase, work.getWorkSpec());
        assertNotNull(worker);
        worker.call();
        assertEquals(Work.STATUS_FAILED, mWorkSpecDao.getWorkSpecStatus(work.getId()));
    }

    @Test
    public void testStatusRunning() throws InterruptedException {
        Work work = new Work.Builder(SleepTestWorker.class).build();
        mWorkSpecDao.insertWorkSpec(work.getWorkSpec());
        Worker worker = Worker.fromWorkSpec(mContext, mDatabase, work.getWorkSpec());
        assertNotNull(worker);
        Executors.newSingleThreadExecutor().submit(worker);
        Thread.sleep(2000);
        assertEquals(Work.STATUS_RUNNING, mWorkSpecDao.getWorkSpecStatus(work.getId()));
    }

    @Test
    public void testArguments() throws InterruptedException {
        Arguments arguments = new Arguments();
        String key = "KEY";
        String expectedValue = "VALUE";
        arguments.putString(key, expectedValue);

        Work work = new Work.Builder(ArgumentsTestWorker.class).withArguments(arguments).build();
        mWorkSpecDao.insertWorkSpec(work.getWorkSpec());
        Worker worker = Worker.fromWorkSpec(mContext, mDatabase, work.getWorkSpec());
        assertNotNull(worker);
        Object result = worker.call();

        assertTrue(result instanceof Arguments);
        Arguments actualArguments = (Arguments) result;
        assertTrue(actualArguments.containsKey(key));
        assertEquals(expectedValue, actualArguments.getString(key, null));
    }
}
