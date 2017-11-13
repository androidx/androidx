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
package android.arch.background.workmanager.constraints;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.mock;

import android.arch.background.workmanager.Processor;
import android.arch.background.workmanager.Scheduler;
import android.arch.background.workmanager.TestLifecycleOwner;
import android.arch.background.workmanager.WorkDatabase;
import android.arch.background.workmanager.executors.SynchronousExecutorService;
import android.arch.core.executor.testing.CountingTaskExecutorRule;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@SmallTest
@RunWith(AndroidJUnit4.class)
public class ConstraintsTrackerTest {

    private static final List<String> TEST_IDS = Arrays.asList("a", "b", "c");

    @Rule
    public CountingTaskExecutorRule mExecutorRule = new CountingTaskExecutorRule();

    private Set<String> mProcessingIds = new HashSet<>();

    private ConstraintsTracker mConstraintsTracker;
    private TestLifecycleOwner mTestLifecycleOwner = new TestLifecycleOwner();
    private WorkDatabase mWorkDatabase = WorkDatabase.create(
            InstrumentationRegistry.getTargetContext(),
            true);

    @Before
    public void setUp() throws TimeoutException, InterruptedException {
        mConstraintsTracker = new ConstraintsTracker(
                InstrumentationRegistry.getTargetContext(),
                mTestLifecycleOwner,
                mWorkDatabase,
                new Processor(
                        InstrumentationRegistry.getTargetContext(),
                        mWorkDatabase,
                        mock(Scheduler.class),
                        new SynchronousExecutorService()) {
                    @Override
                    public void process(String id, long delay) {
                        mProcessingIds.add(id);
                    }

                    @Override
                    public boolean cancel(String id, boolean mayInterruptIfRunning) {
                        mProcessingIds.remove(id);
                        return true;
                    }

                    @Override
                    public void onExecuted(String workSpecId, int result) {
                        super.onExecuted(workSpecId, result);
                    }
                },
                false
        );
        drain();
    }

    @After
    public void tearDown() {
        mProcessingIds.clear();
    }

    @Test
    public void testOnConstraintMet_startsProcessing()
            throws TimeoutException, InterruptedException  {
        mConstraintsTracker.onConstraintMet(TEST_IDS);
        assertThat(mProcessingIds, containsInAnyOrder(TEST_IDS.toArray()));
    }

    @Test
    public void testOnConstraintNotMet_stopsProcessing()
            throws TimeoutException, InterruptedException {
        mConstraintsTracker.onConstraintNotMet(TEST_IDS);
        assertThat(mProcessingIds, not(containsInAnyOrder(TEST_IDS.toArray())));
    }

    @Test
    public void testConstraintsChange_updatesProcessing() {
        mConstraintsTracker.onConstraintMet(TEST_IDS);
        mConstraintsTracker.onConstraintNotMet(Collections.singletonList(TEST_IDS.get(0)));
        assertThat(mProcessingIds, not(containsInAnyOrder(TEST_IDS.get(0))));
        assertThat(
                mProcessingIds,
                containsInAnyOrder(TEST_IDS.subList(1, TEST_IDS.size()).toArray()));
    }

    private void drain() throws TimeoutException, InterruptedException {
        mExecutorRule.drainTasks(1, TimeUnit.MINUTES);
    }
}
