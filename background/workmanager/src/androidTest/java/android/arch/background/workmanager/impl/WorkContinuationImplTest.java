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

package android.arch.background.workmanager.impl;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import android.arch.background.workmanager.TestLifecycleOwner;
import android.arch.background.workmanager.Work;
import android.arch.background.workmanager.WorkContinuation;
import android.arch.background.workmanager.executors.SynchronousExecutorService;
import android.arch.background.workmanager.impl.utils.taskexecutor.InstantTaskExecutorRule;
import android.arch.background.workmanager.worker.TestWorker;
import android.arch.core.executor.ArchTaskExecutor;
import android.arch.lifecycle.Lifecycle;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;


@RunWith(AndroidJUnit4.class)
@SmallTest
public class WorkContinuationImplTest {

    private WorkDatabase mDatabase;
    private WorkManagerImpl mWorkManagerImpl;

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

        Context context = InstrumentationRegistry.getTargetContext();
        WorkManagerConfiguration configuration = new WorkManagerConfiguration(
                context,
                true,
                new SynchronousExecutorService(),
                new SynchronousExecutorService(),
                lifecycleOwner);
        mWorkManagerImpl = new WorkManagerImpl(context, configuration);
        mDatabase = mWorkManagerImpl.getWorkDatabase();
    }

    @After
    public void tearDown() {
        List<String> ids = mDatabase.workSpecDao().getAllWorkSpecIds();
        for (String id : ids) {
            mWorkManagerImpl.cancelWorkForId(id);
        }
        mDatabase.close();
        ArchTaskExecutor.getInstance().setDelegate(null);
    }

    @Test
    public void testContinuation_noParent() {
        Work testWork = createTestWorker();
        WorkContinuationImpl continuation =
                new WorkContinuationImpl(mWorkManagerImpl, testWork);

        assertThat(continuation.getParents(), is(nullValue()));
        assertThat(continuation.getIds().size(), is(1));
        assertThat(continuation.getIds().get(0), is(testWork.getId()));
        assertThat(continuation.getAllIds().size(), is(1));
    }

    @Test
    public void testContinuation_singleChain() {
        Work testWork = createTestWorker();
        Work dependentWork = createTestWorker();
        WorkContinuationImpl continuation =
                new WorkContinuationImpl(mWorkManagerImpl, testWork);
        WorkContinuationImpl dependent = (WorkContinuationImpl) (continuation.then(
                dependentWork));

        assertThat(dependent.getParents(), containsInAnyOrder(continuation));
        assertThat(dependent.getIds().size(), is(1));
        assertThat(dependent.getIds().get(0), is(dependentWork.getId()));
        assertThat(dependent.getAllIds().size(), is(2));
        assertThat(
                dependent.getAllIds(),
                containsInAnyOrder(dependentWork.getId(), testWork.getId()));
    }

    @Test
    public void testContinuation_enqueue() {
        Work testWork = createTestWorker();
        WorkContinuationImpl continuation = new WorkContinuationImpl(mWorkManagerImpl,
                testWork);
        assertThat(continuation.isEnqueued(), is(false));
        continuation.enqueue();
        verifyEnqueued(continuation);
    }

    @Test
    public void testContinuation_chainEnqueue() {
        Work testWork = createTestWorker();
        WorkContinuationImpl continuation =
                new WorkContinuationImpl(mWorkManagerImpl, testWork);
        WorkContinuationImpl chain = (WorkContinuationImpl) (
                continuation.then(createTestWorker()).then(createTestWorker(), createTestWorker()));
        chain.enqueue();
        verifyEnqueued(continuation);
    }

    @Test
    public void testContinuation_chainEnqueueNoOpOnRetry() {
        Work testWork = createTestWorker();
        WorkContinuationImpl continuation =
                new WorkContinuationImpl(mWorkManagerImpl, testWork);
        WorkContinuationImpl chain = (WorkContinuationImpl) (
                continuation.then(createTestWorker()).then(createTestWorker(), createTestWorker()));
        chain.enqueue();
        verifyEnqueued(continuation);
        WorkContinuationImpl spy = spy(chain);
        spy.enqueue();
        // Verify no more calls to markEnqueued().
        verify(spy, times(0)).markEnqueued();
    }

    @Test
    public void testContinuation_join() {
        WorkContinuationImpl first = new WorkContinuationImpl(mWorkManagerImpl,
                createTestWorker());
        WorkContinuationImpl second = new WorkContinuationImpl(mWorkManagerImpl,
                createTestWorker());
        WorkContinuationImpl dependent = (WorkContinuationImpl) WorkContinuation.join(first,
                second);
        assertThat(dependent.getParents(), is(notNullValue()));
        assertThat(dependent.getParents(), containsInAnyOrder(first, second));
    }

    @Test
    public void testContinuation_joinAndEnqueue() {
        WorkContinuationImpl first = new WorkContinuationImpl(mWorkManagerImpl,
                createTestWorker());
        WorkContinuationImpl second = new WorkContinuationImpl(mWorkManagerImpl,
                createTestWorker());

        WorkContinuationImpl third = new WorkContinuationImpl(mWorkManagerImpl,
                createTestWorker());
        WorkContinuationImpl fourth = new WorkContinuationImpl(mWorkManagerImpl,
                createTestWorker());

        WorkContinuationImpl firstDependent = (WorkContinuationImpl) WorkContinuation.join(first,
                second);
        WorkContinuationImpl secondDependent = (WorkContinuationImpl) WorkContinuation.join(third,
                fourth);
        WorkContinuationImpl dependent = (WorkContinuationImpl) WorkContinuation.join(
                firstDependent, secondDependent);
        dependent.enqueue();
        verifyEnqueued(dependent);
    }

    @Test
    public void testContinuation_joinAndEnqueueWithOverlaps() {
        WorkContinuationImpl first = new WorkContinuationImpl(mWorkManagerImpl,
                createTestWorker());
        WorkContinuationImpl second = new WorkContinuationImpl(mWorkManagerImpl,
                createTestWorker());
        WorkContinuationImpl third = new WorkContinuationImpl(mWorkManagerImpl,
                createTestWorker());
        WorkContinuationImpl firstDependent = (WorkContinuationImpl) WorkContinuation.join(first,
                second);
        WorkContinuationImpl secondDependent = (WorkContinuationImpl) WorkContinuation.join(first,
                third);
        WorkContinuationImpl dependent = (WorkContinuationImpl) WorkContinuation.join(
                firstDependent, secondDependent);
        dependent.enqueue();
        verifyEnqueued(dependent);
    }

    private void verifyEnqueued(WorkContinuationImpl continuation) {
        assertThat(continuation.isEnqueued(), is(true));
        List<WorkContinuationImpl> parents = continuation.getParents();
        if (parents != null) {
            for (WorkContinuationImpl parent : parents) {
                verifyEnqueued(parent);
            }
        }
    }

    private static Work createTestWorker() {
        return Work.newBuilder(TestWorker.class).build();
    }
}
