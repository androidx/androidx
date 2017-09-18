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

import java.util.List;
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
        mWorkManager = new WorkManager.Builder("test")
                .build(InstrumentationRegistry.getTargetContext());
    }

    @After
    public void tearDown() {
    }

    @Test
    public void insert() throws InterruptedException, ExecutionException, TimeoutException {
        Work work = new Work.Builder(TestWorker.class)
                .then(TestWorker.class)
                .then(TestWorker.class)
                .build();
        mWorkManager.enqueue(work);
        Thread.sleep(5000);

        List<String> workItemIds = work.getWorkItemIds();
        assertEquals(3, workItemIds.size());
        for (String id : workItemIds) {
            assertNotNull(mDatabase.workItemDao().getWorkItem(id));
        }
        assertFalse(mDatabase.dependencyDao().hasDependencies(workItemIds.get(0)));
        assertTrue(mDatabase.dependencyDao().hasDependencies(workItemIds.get(1)));
        assertTrue(mDatabase.dependencyDao().hasDependencies(workItemIds.get(2)));
    }

    @Test
    public void constraints() throws InterruptedException, ExecutionException, TimeoutException {
        Work work = new Work.Builder(TestWorker.class)
                .withConstraints(
                        new Constraints.Builder()
                                .setRequiresCharging(true)
                                .setRequiresDeviceIdle(true)
                                .setRequiresNetworkType(Constraints.NETWORK_TYPE_METERED)
                                .setRequiresBatteryNotLow(true)
                                .setRequiresStorageNotLow(true)
                                .setInitialDelay(5000)
                                .build())
                .then(TestWorker.class)
                .build();
        mWorkManager.enqueue(work);
        Thread.sleep(5000);

        List<String> workItemIds = work.getWorkItemIds();
        WorkItem workItem0 = mDatabase.workItemDao().getWorkItem(workItemIds.get(0));
        WorkItem workItem1 = mDatabase.workItemDao().getWorkItem(workItemIds.get(1));

        assertNotNull(workItem0.mConstraints);
        assertTrue(workItem0.mConstraints.mRequiresCharging);
        assertTrue(workItem0.mConstraints.mRequiresDeviceIdle);
        assertTrue(workItem0.mConstraints.mRequiresBatteryNotLow);
        assertTrue(workItem0.mConstraints.mRequiresStorageNotLow);
        assertEquals(5000, workItem0.mConstraints.mInitialDelay);
        assertEquals(Constraints.NETWORK_TYPE_METERED, workItem0.mConstraints.mRequiresNetworkType);

        assertNotNull(workItem1.mConstraints);
        assertFalse(workItem1.mConstraints.mRequiresCharging);
        assertFalse(workItem1.mConstraints.mRequiresDeviceIdle);
        assertFalse(workItem1.mConstraints.mRequiresBatteryNotLow);
        assertFalse(workItem1.mConstraints.mRequiresStorageNotLow);
        assertEquals(0, workItem1.mConstraints.mInitialDelay);
        assertEquals(Constraints.NETWORK_TYPE_ANY, workItem1.mConstraints.mRequiresNetworkType);
    }

    @Test
    public void backoffPolicy() throws InterruptedException, ExecutionException, TimeoutException {
        Work work = new Work.Builder(TestWorker.class)
                .withBackoffCriteria(WorkItem.BACKOFF_POLICY_LINEAR, 50000)
                .then(TestWorker.class)
                .build();
        mWorkManager.enqueue(work);
        Thread.sleep(5000);

        List<String> workItemIds = work.getWorkItemIds();
        WorkItem workItem0 = mDatabase.workItemDao().getWorkItem(workItemIds.get(0));
        WorkItem workItem1 = mDatabase.workItemDao().getWorkItem(workItemIds.get(1));

        assertEquals(WorkItem.BACKOFF_POLICY_LINEAR, workItem0.mBackoffPolicy);
        assertEquals(50000, workItem0.mBackoffDelayDuration);

        assertEquals(WorkItem.BACKOFF_POLICY_EXPONENTIAL, workItem1.mBackoffPolicy);
        assertEquals(WorkItem.DEFAULT_BACKOFF_DELAY_DURATION, workItem1.mBackoffDelayDuration);
    }

    @Test
    public void arguments() throws InterruptedException, ExecutionException, TimeoutException {
        String key = "key";
        String expectedValue = "value";

        Arguments args = new Arguments();
        args.putString(key, expectedValue);

        Work work = new Work.Builder(TestWorker.class)
                .withArguments(args)
                .then(TestWorker.class)
                .build();
        mWorkManager.enqueue(work);
        Thread.sleep(5000);

        List<String> workItemIds = work.getWorkItemIds();
        WorkItem workItem0 = mDatabase.workItemDao().getWorkItem(workItemIds.get(0));
        WorkItem workItem1 = mDatabase.workItemDao().getWorkItem(workItemIds.get(1));

        assertNotNull(workItem0.mArguments);
        assertNotNull(workItem1.mArguments);

        assertEquals(1, workItem0.mArguments.size());
        assertEquals(0, workItem1.mArguments.size());

        String actualValue = workItem0.mArguments.getString(key, null);
        assertNotNull(actualValue);
        assertEquals(expectedValue, actualValue);
    }
}
