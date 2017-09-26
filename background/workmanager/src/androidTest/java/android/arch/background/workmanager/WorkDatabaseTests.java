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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.arch.background.workmanager.model.Arguments;
import android.arch.background.workmanager.model.Constraints;
import android.arch.background.workmanager.model.WorkSpec;
import android.arch.persistence.db.SupportSQLiteDatabase;
import android.arch.persistence.db.SupportSQLiteOpenHelper;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class WorkDatabaseTests {
    private WorkDatabase mDatabase;
    private WorkManager mWorkManager;

    @Before
    public void setUp() {
        mWorkManager = new WorkManager.Builder()
                .withInMemoryDatabase()
                .build(InstrumentationRegistry.getTargetContext());
        mDatabase = mWorkManager.getWorkDatabase();
    }

    @After
    public void tearDown() {
    }

    @Test
    public void insert() throws InterruptedException, ExecutionException, TimeoutException {
        final int workCount = 3;
        final Work[] workArray = new Work[workCount];
        for (int i = 0; i < workCount; ++i) {
            workArray[i] = new Work.Builder(TestWorker.class).build();
        }
        mWorkManager.enqueue(workArray[0]).then(workArray[1]).then(workArray[2]);
        Thread.sleep(5000);

        for (int i = 0; i < workCount; ++i) {
            String id = workArray[i].getId();
            assertNotNull(mDatabase.workSpecDao().getWorkSpec(id));
            assertEquals(mDatabase.dependencyDao().hasDependencies(id), (i > 0));
        }
    }

    @Test
    public void constraints() throws InterruptedException, ExecutionException, TimeoutException {
        Work work0 = new Work.Builder(TestWorker.class)
                .withConstraints(
                        new Constraints.Builder()
                                .setRequiresCharging(true)
                                .setRequiresDeviceIdle(true)
                                .setRequiredNetworkType(Constraints.NETWORK_TYPE_METERED)
                                .setRequiresBatteryNotLow(true)
                                .setRequiresStorageNotLow(true)
                                .setInitialDelay(5000)
                                .build())
                .build();
        Work work1 = new Work.Builder(TestWorker.class).build();
        mWorkManager.enqueue(work0).then(work1);
        Thread.sleep(5000);

        WorkSpec workSpec0 = mDatabase.workSpecDao().getWorkSpec(work0.getId());
        WorkSpec workSpec1 = mDatabase.workSpecDao().getWorkSpec(work1.getId());

        Constraints constraints = workSpec0.getConstraints();
        assertNotNull(constraints);
        assertTrue(constraints.requiresCharging());
        assertTrue(constraints.requiresDeviceIdle());
        assertTrue(constraints.requiresBatteryNotLow());
        assertTrue(constraints.requiresStorageNotLow());
        assertEquals(5000, constraints.getInitialDelay());
        assertEquals(Constraints.NETWORK_TYPE_METERED, constraints.getRequiredNetworkType());

        constraints = workSpec1.getConstraints();
        assertNotNull(constraints);
        assertFalse(constraints.requiresCharging());
        assertFalse(constraints.requiresDeviceIdle());
        assertFalse(constraints.requiresBatteryNotLow());
        assertFalse(constraints.requiresStorageNotLow());
        assertEquals(0, constraints.getInitialDelay());
        assertEquals(Constraints.NETWORK_TYPE_ANY, constraints.getRequiredNetworkType());
    }

    @Test
    public void backoffPolicy() throws InterruptedException, ExecutionException, TimeoutException {
        Work work0 = new Work.Builder(TestWorker.class)
                .withBackoffCriteria(Work.BACKOFF_POLICY_LINEAR, 50000)
                .build();
        Work work1 = new Work.Builder(TestWorker.class).build();
        mWorkManager.enqueue(work0).then(work1);
        Thread.sleep(5000);

        WorkSpec workSpec0 = mDatabase.workSpecDao().getWorkSpec(work0.getId());
        WorkSpec workSpec1 = mDatabase.workSpecDao().getWorkSpec(work1.getId());

        assertEquals(Work.BACKOFF_POLICY_LINEAR, workSpec0.getBackoffPolicy());
        assertEquals(50000, workSpec0.getBackoffDelayDuration());

        assertEquals(Work.BACKOFF_POLICY_EXPONENTIAL, workSpec1.getBackoffPolicy());
        assertEquals(Work.DEFAULT_BACKOFF_DELAY_DURATION, workSpec1.getBackoffDelayDuration());
    }

    @Test
    public void arguments() throws InterruptedException, ExecutionException, TimeoutException {
        String key = "key";
        String expectedValue = "value";

        Arguments args = new Arguments();
        args.putString(key, expectedValue);

        Work work0 = new Work.Builder(TestWorker.class)
                .withArguments(args)
                .build();
        Work work1 = new Work.Builder(TestWorker.class).build();
        mWorkManager.enqueue(work0).then(work1);
        Thread.sleep(5000);

        WorkSpec workSpec0 = mDatabase.workSpecDao().getWorkSpec(work0.getId());
        WorkSpec workSpec1 = mDatabase.workSpecDao().getWorkSpec(work1.getId());

        assertNotNull(workSpec0.getArguments());
        assertNotNull(workSpec1.getArguments());

        assertEquals(1, workSpec0.getArguments().size());
        assertEquals(0, workSpec1.getArguments().size());

        String actualValue = workSpec0.getArguments().getString(key, null);
        assertNotNull(actualValue);
        assertEquals(expectedValue, actualValue);
    }

    @Test
    public void generateCleanupCallback() {
        WorkSpecDao workSpecDao = mDatabase.workSpecDao();

        Work work = new Work.Builder(TestWorker.class).build();
        WorkSpec workSpec = work.getWorkSpec();
        workSpec.setStatus(Work.STATUS_RUNNING);
        workSpecDao.insertWorkSpec(work.getWorkSpec());

        assertEquals(workSpecDao.getWorkSpec(work.getId()).getStatus(), Work.STATUS_RUNNING);

        SupportSQLiteOpenHelper openHelper = mDatabase.getOpenHelper();
        SupportSQLiteDatabase db = openHelper.getWritableDatabase();
        WorkDatabase.generateCleanupCallback().onOpen(db);

        assertEquals(workSpecDao.getWorkSpec(work.getId()).getStatus(), Work.STATUS_ENQUEUED);
    }
}
