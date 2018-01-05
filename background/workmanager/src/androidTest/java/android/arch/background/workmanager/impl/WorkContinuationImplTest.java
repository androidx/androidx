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

        assertThat(continuation.getParent(), is(nullValue()));
        assertThat(continuation.getIds().length, is(1));
        assertThat(continuation.getIds()[0], is(testWork.getId()));
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


        assertThat(dependent.getParent(), is(notNullValue()));
        assertThat(dependent.getParent(), is(continuation));
        assertThat(dependent.getIds().length, is(1));
        assertThat(dependent.getIds()[0], is(dependentWork.getId()));
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

    private void verifyEnqueued(WorkContinuationImpl continuation) {
        assertThat(continuation.isEnqueued(), is(true));
        WorkContinuationImpl parent = continuation.getParent();
        while (parent != null) {
            assertThat(parent.isEnqueued(), is(true));
            parent = parent.getParent();
        }
    }

    private static Work createTestWorker() {
        return Work.newBuilder(TestWorker.class).build();
    }
}
