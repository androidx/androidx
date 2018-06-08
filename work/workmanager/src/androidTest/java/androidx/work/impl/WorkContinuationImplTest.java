/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.work.impl;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.arch.core.executor.ArchTaskExecutor;
import android.arch.lifecycle.Lifecycle;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.LargeTest;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import androidx.work.Configuration;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.State;
import androidx.work.TestLifecycleOwner;
import androidx.work.WorkContinuation;
import androidx.work.WorkManagerTest;
import androidx.work.WorkStatus;
import androidx.work.impl.model.WorkSpec;
import androidx.work.impl.model.WorkSpecDao;
import androidx.work.impl.utils.taskexecutor.InstantTaskExecutorRule;
import androidx.work.worker.TestWorker;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executors;


@RunWith(AndroidJUnit4.class)
@SmallTest
public class WorkContinuationImplTest extends WorkManagerTest {

    private Configuration mConfiguration;
    private WorkDatabase mDatabase;
    private WorkManagerImpl mWorkManagerImpl;
    private Scheduler mScheduler;

    @Rule
    public InstantTaskExecutorRule mRule = new InstantTaskExecutorRule();

    @Before
    public void setUp() {
        ArchTaskExecutor.getInstance().setDelegate(new android.arch.core.executor.TaskExecutor() {
            @Override
            public void executeOnDiskIO(@NonNull Runnable runnable) {
                runnable.run();
            }

            @Override
            public void postToMainThread(@NonNull Runnable runnable) {
                runnable.run();
            }

            @Override
            public boolean isMainThread() {
                return true;
            }
        });

        TestLifecycleOwner lifecycleOwner = new TestLifecycleOwner();
        lifecycleOwner.mLifecycleRegistry.markState(Lifecycle.State.CREATED);

        mScheduler = mock(Scheduler.class);
        Context context = InstrumentationRegistry.getTargetContext();
        mConfiguration = new Configuration.Builder()
                .setExecutor(Executors.newSingleThreadExecutor())
                .build();

        mWorkManagerImpl = spy(new WorkManagerImpl(context, mConfiguration));
        when(mWorkManagerImpl.getSchedulers()).thenReturn(Collections.singletonList(mScheduler));
        WorkManagerImpl.setDelegate(mWorkManagerImpl);
        mDatabase = mWorkManagerImpl.getWorkDatabase();
    }

    @After
    public void tearDown() {
        List<String> ids = mDatabase.workSpecDao().getAllWorkSpecIds();
        for (String id : ids) {
            mWorkManagerImpl.cancelWorkByIdSync(UUID.fromString(id));
        }
        WorkManagerImpl.setDelegate(null);
        ArchTaskExecutor.getInstance().setDelegate(null);
    }

    @Test
    public void testContinuation_noParent() {
        OneTimeWorkRequest testWork = createTestWorker();
        WorkContinuationImpl continuation =
                new WorkContinuationImpl(mWorkManagerImpl, Collections.singletonList(testWork));

        assertThat(continuation.getParents(), is(nullValue()));
        assertThat(continuation.getIds().size(), is(1));
        assertThat(continuation.getIds().get(0), is(testWork.getStringId()));
        assertThat(continuation.getAllIds().size(), is(1));
    }

    @Test
    public void testContinuation_singleChain() {
        OneTimeWorkRequest testWork = createTestWorker();
        OneTimeWorkRequest dependentWork = createTestWorker();
        WorkContinuationImpl continuation =
                new WorkContinuationImpl(mWorkManagerImpl, Collections.singletonList(testWork));
        WorkContinuationImpl dependent = (WorkContinuationImpl) (continuation.then(
                dependentWork));

        assertThat(dependent.getParents(), containsInAnyOrder(continuation));
        assertThat(dependent.getIds().size(), is(1));
        assertThat(dependent.getIds().get(0), is(dependentWork.getStringId()));
        assertThat(dependent.getAllIds().size(), is(2));
        assertThat(
                dependent.getAllIds(),
                containsInAnyOrder(dependentWork.getStringId(), testWork.getStringId()));
    }

    @Test
    public void testContinuation_enqueue() {
        WorkContinuationImpl continuation = new WorkContinuationImpl(mWorkManagerImpl,
                createTestWorkerList());
        assertThat(continuation.isEnqueued(), is(false));
        continuation.enqueueSync();
        verifyEnqueued(continuation);
        verifyScheduled(mScheduler, continuation);
    }

    @Test
    public void testContinuation_chainEnqueue() {
        WorkContinuationImpl continuation =
                new WorkContinuationImpl(mWorkManagerImpl, createTestWorkerList());
        WorkContinuationImpl chain = (WorkContinuationImpl) (
                continuation.then(createTestWorker()).then(createTestWorker(), createTestWorker()));
        chain.enqueueSync();
        verifyEnqueued(continuation);
        verifyScheduled(mScheduler, continuation);
    }

    @Test
    public void testContinuation_chainEnqueueNoOpOnRetry() {
        WorkContinuationImpl continuation =
                new WorkContinuationImpl(mWorkManagerImpl, createTestWorkerList());
        WorkContinuationImpl chain = (WorkContinuationImpl) (
                continuation.then(createTestWorker()).then(createTestWorker(), createTestWorker()));
        chain.enqueueSync();
        verifyEnqueued(continuation);
        verifyScheduled(mScheduler, continuation);
        WorkContinuationImpl spy = spy(chain);
        spy.enqueueSync();
        // Verify no more calls to markEnqueued().
        verify(spy, times(0)).markEnqueued();
    }

    @Test
    public void testContinuation_join() {
        WorkContinuationImpl first = new WorkContinuationImpl(mWorkManagerImpl,
                createTestWorkerList());
        WorkContinuationImpl second = new WorkContinuationImpl(mWorkManagerImpl,
                createTestWorkerList());

        WorkContinuationImpl dependent = (WorkContinuationImpl) WorkContinuation.combine(first,
                second);
        assertThat(dependent.getParents(), is(notNullValue()));
        assertThat(dependent.getParents(), containsInAnyOrder(first, second));
    }

    public void testContinuation_withWorkJoin() {
        WorkContinuationImpl first = new WorkContinuationImpl(mWorkManagerImpl,
                createTestWorkerList());
        WorkContinuationImpl second = new WorkContinuationImpl(mWorkManagerImpl,
                createTestWorkerList());

        OneTimeWorkRequest work = createTestWorker();

        WorkContinuationImpl dependent = (WorkContinuationImpl) WorkContinuation.combine(
                work, first, second);

        assertThat(dependent.getIds(), containsInAnyOrder(work.getStringId()));
        assertThat(dependent.getParents(), is(notNullValue()));
        assertThat(dependent.getParents(), containsInAnyOrder(first, second));
    }

    @Test
    public void testContinuation_joinAndEnqueue() {
        WorkContinuationImpl first = new WorkContinuationImpl(mWorkManagerImpl,
                createTestWorkerList());
        WorkContinuationImpl second = new WorkContinuationImpl(mWorkManagerImpl,
                createTestWorkerList());

        WorkContinuationImpl third = new WorkContinuationImpl(mWorkManagerImpl,
                createTestWorkerList());
        WorkContinuationImpl fourth = new WorkContinuationImpl(mWorkManagerImpl,
                createTestWorkerList());

        WorkContinuationImpl firstDependent = (WorkContinuationImpl) WorkContinuation.combine(
                first, second);
        WorkContinuationImpl secondDependent = (WorkContinuationImpl) WorkContinuation.combine(
                third, fourth);
        WorkContinuationImpl dependent = (WorkContinuationImpl) WorkContinuation.combine(
                firstDependent, secondDependent);
        dependent.enqueueSync();
        verifyEnqueued(dependent);
        verifyScheduled(mScheduler, dependent);
    }

    @Test
    public void testContinuation_joinAndEnqueueWithOverlaps() {
        WorkContinuationImpl first = new WorkContinuationImpl(mWorkManagerImpl,
                createTestWorkerList());
        WorkContinuationImpl second = new WorkContinuationImpl(mWorkManagerImpl,
                createTestWorkerList());
        WorkContinuationImpl third = new WorkContinuationImpl(mWorkManagerImpl,
                createTestWorkerList());
        WorkContinuationImpl firstDependent = (WorkContinuationImpl) WorkContinuation.combine(
                first, second);
        WorkContinuationImpl secondDependent = (WorkContinuationImpl) WorkContinuation.combine(
                first, third);
        WorkContinuationImpl dependent = (WorkContinuationImpl) WorkContinuation.combine(
                firstDependent, secondDependent);
        dependent.enqueueSync();
        verifyEnqueued(dependent);
        verifyScheduled(mScheduler, dependent);
    }

    @Test
    @LargeTest
    @SuppressWarnings("unchecked")
    public void testContinuation_joinPassesAllOutput() throws InterruptedException {
        final String intTag = "myint";
        final String stringTag = "mystring";

        OneTimeWorkRequest firstWork = new OneTimeWorkRequest.Builder(TestWorker.class)
                .setInitialState(State.SUCCEEDED)
                .build();
        OneTimeWorkRequest secondWork = new OneTimeWorkRequest.Builder(TestWorker.class)
                .setInitialState(State.SUCCEEDED)
                .build();

        WorkSpecDao workSpecDao = mDatabase.workSpecDao();
        workSpecDao.insertWorkSpec(getWorkSpec(firstWork));
        workSpecDao.insertWorkSpec(getWorkSpec(secondWork));

        workSpecDao.setOutput(
                firstWork.getStringId(),
                new Data.Builder().putInt(intTag, 0).build());
        workSpecDao.setOutput(
                secondWork.getStringId(),
                new Data.Builder().putInt(intTag, 1).putString(stringTag, "hello").build());

        WorkContinuationImpl firstContinuation =
                new WorkContinuationImpl(mWorkManagerImpl, Collections.singletonList(firstWork));
        WorkContinuationImpl secondContinuation =
                new WorkContinuationImpl(mWorkManagerImpl, Collections.singletonList(secondWork));
        WorkContinuationImpl dependentContinuation =
                (WorkContinuationImpl) WorkContinuation.combine(
                        firstContinuation, secondContinuation);
        dependentContinuation.enqueueSync();

        String joinId = null;
        for (String id : dependentContinuation.getAllIds()) {
            if (!firstWork.getStringId().equals(id) && !secondWork.getStringId().equals(id)) {
                joinId = id;
                break;
            }
        }

        Thread.sleep(5000L);

        // TODO(sumir): I can't seem to get this kicked off automatically, so I'm running it myself.
        // Figure out what's going on here.
        Context context = InstrumentationRegistry.getTargetContext();
        new WorkerWrapper.Builder(context, mConfiguration, mDatabase, joinId)
                .build()
                .run();

        assertThat(joinId, is(not(nullValue())));
        WorkSpec joinWorkSpec = mDatabase.workSpecDao().getWorkSpec(joinId);
        assertThat(joinWorkSpec, is(not(nullValue())));
        assertThat(joinWorkSpec.state, is(State.SUCCEEDED));

        Data output = joinWorkSpec.output;
        int[] intArray = output.getIntArray(intTag);

        assertThat(intArray, is(not(nullValue())));
        Arrays.sort(intArray);
        assertThat(Arrays.binarySearch(intArray, 0), is(not(-1)));
        assertThat(Arrays.binarySearch(intArray, 1), is(not(-1)));
        assertThat(output.getStringArray(stringTag), is(not(nullValue())));
        assertThat(Arrays.asList(output.getStringArray(stringTag)), contains("hello"));

    }

    @Test
    @SmallTest
    public void testContinuation_hasCycles() {
        OneTimeWorkRequest aWork = createTestWorker(); // A
        OneTimeWorkRequest bWork = createTestWorker(); // B
        OneTimeWorkRequest cWork = createTestWorker(); // C

        WorkContinuation continuationA = new WorkContinuationImpl(
                mWorkManagerImpl, Collections.singletonList(aWork));

        WorkContinuation continuationB = new WorkContinuationImpl(
                mWorkManagerImpl, Collections.singletonList(bWork));

        // B -> C
        WorkContinuation continuationBC = continuationB.then(cWork);

        // combine -> A, C
        WorkContinuation join = WorkContinuation.combine(continuationA, continuationBC);

        // withCycles -> B
        WorkContinuationImpl withCycles = (WorkContinuationImpl) join.then(bWork);
        assertThat(withCycles.hasCycles(), is(true));
    }

    @Test
    @SmallTest
    public void testContinuation_hasCycles2() {
        OneTimeWorkRequest aWork = createTestWorker(); // A
        WorkContinuation continuationA = new WorkContinuationImpl(
                mWorkManagerImpl, Collections.singletonList(aWork));

        // A -> A
        WorkContinuationImpl withCycles = (WorkContinuationImpl) continuationA.then(aWork);
        assertThat(withCycles.hasCycles(), is(true));
    }

    @Test
    @SmallTest
    public void testContinuation_hasCycles3() {
        OneTimeWorkRequest aWork = createTestWorker(); // A
        WorkContinuation continuationA = new WorkContinuationImpl(
                mWorkManagerImpl, Collections.singletonList(aWork));

        // A -> A
        WorkContinuation first = continuationA.then(aWork);
        // A -> A
        WorkContinuation second = continuationA.then(aWork);
        //   A
        //  A A
        //   A
        WorkContinuationImpl joined = (WorkContinuationImpl) WorkContinuation.combine(
                first, second);
        assertThat(joined.hasCycles(), is(true));
    }

    @Test
    @SmallTest
    public void testContinuation_hasCycles4() {
        OneTimeWorkRequest aWork = createTestWorker(); // A
        OneTimeWorkRequest cWork = createTestWorker(); // C

        WorkContinuation continuationA = new WorkContinuationImpl(
                mWorkManagerImpl, Collections.singletonList(aWork));

        // A   A
        //   B
        WorkContinuation continuationB = WorkContinuation.combine(continuationA, continuationA);
        // A   A
        //   B
        //   C
        WorkContinuation continuationC = continuationB.then(cWork);
        // A   A
        //   B
        //   C
        //   A
        WorkContinuationImpl withCycles = (WorkContinuationImpl) continuationC.then(aWork);
        assertThat(withCycles.hasCycles(), is(true));
    }

    @Test
    @SmallTest
    public void testContinuation_hasNoCycles() {
        OneTimeWorkRequest aWork = createTestWorker(); // A
        OneTimeWorkRequest bWork = createTestWorker(); // B
        OneTimeWorkRequest cWork = createTestWorker(); // C

        WorkContinuation continuationAB = new WorkContinuationImpl(
                mWorkManagerImpl, Arrays.asList(aWork, bWork));

        WorkContinuation continuationBC = new WorkContinuationImpl(
                mWorkManagerImpl, Arrays.asList(bWork, cWork));

        WorkContinuationImpl joined =
                (WorkContinuationImpl) WorkContinuation.combine(continuationAB, continuationBC);

        assertThat(joined.hasCycles(), is(false));
    }

    @Test
    @SmallTest
    public void testContinuation_hasNoCycles2() {
        OneTimeWorkRequest aWork = createTestWorker(); // A
        OneTimeWorkRequest bWork = createTestWorker(); // B
        OneTimeWorkRequest cWork = createTestWorker(); // C

        WorkContinuation continuationA = new WorkContinuationImpl(
                mWorkManagerImpl, Collections.singletonList(aWork));

        // A -> B
        WorkContinuation continuationB = continuationA.then(bWork);
        // A -> C
        WorkContinuation continuationC = continuationA.then(cWork);

        WorkContinuation continuationA2 = new WorkContinuationImpl(
                mWorkManagerImpl, Collections.singletonList(aWork));
        // A -> B
        WorkContinuation continuationB2 = continuationA2.then(bWork);
        // A -> C
        WorkContinuation continuationC2 = continuationA2.then(cWork);

        //    A      A
        //  B   C  B   C
        //       D
        WorkContinuationImpl joined = (WorkContinuationImpl) WorkContinuation.combine(
                continuationB, continuationC, continuationB2, continuationC2);

        assertThat(joined.hasCycles(), is(false));
    }

    @Test
    @SmallTest
    public void testContinuation_hasNoCycles3() {
        OneTimeWorkRequest aWork = createTestWorker(); // A
        OneTimeWorkRequest bWork = createTestWorker(); // B
        OneTimeWorkRequest cWork = createTestWorker(); // C

        WorkContinuation continuationA = new WorkContinuationImpl(
                mWorkManagerImpl, Collections.singletonList(aWork));

        WorkContinuation continuationB = new WorkContinuationImpl(
                mWorkManagerImpl, Collections.singletonList(bWork));

        WorkContinuation continuationC = new WorkContinuationImpl(
                mWorkManagerImpl, Collections.singletonList(cWork));

        WorkContinuation first = WorkContinuation.combine(continuationA, continuationB);
        WorkContinuation second = WorkContinuation.combine(continuationA, continuationC);

        WorkContinuationImpl joined = (WorkContinuationImpl) WorkContinuation.combine(
                first, second);
        assertThat(joined.hasCycles(), is(false));
    }

    @Test
    @SmallTest
    public void testGetStatusesSync() {
        OneTimeWorkRequest aWork = createTestWorker(); // A
        OneTimeWorkRequest bWork = createTestWorker(); // B
        OneTimeWorkRequest cWork = createTestWorker(); // C
        OneTimeWorkRequest dWork = createTestWorker(); // D

        WorkContinuation firstChain = mWorkManagerImpl.beginWith(aWork).then(bWork);
        WorkContinuation secondChain = mWorkManagerImpl.beginWith(cWork);
        WorkContinuation combined = WorkContinuation.combine(dWork, firstChain, secondChain);

        combined.synchronous().enqueueSync();
        List<WorkStatus> statuses = combined.synchronous().getStatusesSync();
        assertThat(statuses, is(notNullValue()));
        List<UUID> ids = new ArrayList<>(statuses.size());
        for (WorkStatus status : statuses) {
            ids.add(status.getId());
        }
        assertThat(ids, containsInAnyOrder(
                aWork.getId(), bWork.getId(), cWork.getId(), dWork.getId()));
    }

    private static void verifyEnqueued(WorkContinuationImpl continuation) {
        assertThat(continuation.isEnqueued(), is(true));
        List<WorkContinuationImpl> parents = continuation.getParents();
        if (parents != null) {
            for (WorkContinuationImpl parent : parents) {
                verifyEnqueued(parent);
            }
        }
    }

    private static void verifyScheduled(Scheduler scheduler, WorkContinuationImpl continuation) {
        Configuration configuration = continuation.getWorkManagerImpl().getConfiguration();
        ArgumentCaptor<WorkSpec> captor = ArgumentCaptor.forClass(WorkSpec.class);
        verify(scheduler, times(1)).schedule(captor.capture());
        List<WorkSpec> workSpecs = captor.getAllValues();
        assertThat(workSpecs, notNullValue());

        WorkDatabase workDatabase = continuation.getWorkManagerImpl().getWorkDatabase();
        List<WorkSpec> eligibleWorkSpecs =
                workDatabase
                        .workSpecDao()
                        .getEligibleWorkForScheduling(
                                configuration.getMaxSchedulerLimit());

        Set<String> capturedIds = new HashSet<>();
        for (WorkSpec workSpec : workSpecs) {
            capturedIds.add(workSpec.id);
        }

        for (WorkSpec eligibleWorkSpec : eligibleWorkSpecs) {
            assertThat(capturedIds.contains(eligibleWorkSpec.id), is(true));
        }
    }

    private static OneTimeWorkRequest createTestWorker() {
        return new OneTimeWorkRequest.Builder(TestWorker.class).build();
    }

    private static List<OneTimeWorkRequest> createTestWorkerList() {
        return Collections.singletonList(createTestWorker());
    }
}
