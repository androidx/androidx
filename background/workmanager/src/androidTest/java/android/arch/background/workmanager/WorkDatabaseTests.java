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
        mDatabase = WorkDatabase.getInMemoryInstance(InstrumentationRegistry.getTargetContext());
        mWorkManager = new WorkManager.Builder().build(InstrumentationRegistry.getTargetContext());
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

        assertNotNull(workSpec0.mConstraints);
        assertTrue(workSpec0.mConstraints.mRequiresCharging);
        assertTrue(workSpec0.mConstraints.mRequiresDeviceIdle);
        assertTrue(workSpec0.mConstraints.mRequiresBatteryNotLow);
        assertTrue(workSpec0.mConstraints.mRequiresStorageNotLow);
        assertEquals(5000, workSpec0.mConstraints.mInitialDelay);
        assertEquals(Constraints.NETWORK_TYPE_METERED, workSpec0.mConstraints.mRequiredNetworkType);

        assertNotNull(workSpec1.mConstraints);
        assertFalse(workSpec1.mConstraints.mRequiresCharging);
        assertFalse(workSpec1.mConstraints.mRequiresDeviceIdle);
        assertFalse(workSpec1.mConstraints.mRequiresBatteryNotLow);
        assertFalse(workSpec1.mConstraints.mRequiresStorageNotLow);
        assertEquals(0, workSpec1.mConstraints.mInitialDelay);
        assertEquals(Constraints.NETWORK_TYPE_ANY, workSpec1.mConstraints.mRequiredNetworkType);
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

        assertEquals(Work.BACKOFF_POLICY_LINEAR, workSpec0.mBackoffPolicy);
        assertEquals(50000, workSpec0.mBackoffDelayDuration);

        assertEquals(Work.BACKOFF_POLICY_EXPONENTIAL, workSpec1.mBackoffPolicy);
        assertEquals(Work.DEFAULT_BACKOFF_DELAY_DURATION, workSpec1.mBackoffDelayDuration);
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

        assertNotNull(workSpec0.mArguments);
        assertNotNull(workSpec1.mArguments);

        assertEquals(1, workSpec0.mArguments.size());
        assertEquals(0, workSpec1.mArguments.size());

        String actualValue = workSpec0.mArguments.getString(key, null);
        assertNotNull(actualValue);
        assertEquals(expectedValue, actualValue);
    }
}
