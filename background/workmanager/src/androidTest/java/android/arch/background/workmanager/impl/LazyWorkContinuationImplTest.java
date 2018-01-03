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


import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import android.arch.background.workmanager.Work;
import android.arch.background.workmanager.impl.utils.EnqueueRunnable;
import android.arch.background.workmanager.impl.utils.taskexecutor.TaskExecutor;
import android.arch.background.workmanager.worker.TestWorker;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;


@RunWith(AndroidJUnit4.class)
@SmallTest
public class LazyWorkContinuationImplTest {

    private WorkManagerImpl mWorkManagerImpl;
    private TaskExecutor mTaskExecutor;

    @Before
    public void setup() {
        mWorkManagerImpl = mock(WorkManagerImpl.class);
        mTaskExecutor = mock(TaskExecutor.class);
        when(mWorkManagerImpl.getTaskExecutor()).thenReturn(mTaskExecutor);
    }

    @Test
    public void testLazyContinuation_noParent() {
        Work testWork = createTestWorker();
        LazyWorkContinuationImpl continuation = new LazyWorkContinuationImpl(mWorkManagerImpl,
                testWork);
        assertNull(continuation.mParent);

        assertEquals(continuation.mIds[0], testWork.getId());
        assertEquals(continuation.mIds.length, 1);
        assertEquals(continuation.mAllIds.size(), 1);
    }

    @Test
    public void testLazyContinuation_singleChain() {
        Work testWork = createTestWorker();
        Work dependentWork = createTestWorker();
        LazyWorkContinuationImpl continuation =
                new LazyWorkContinuationImpl(mWorkManagerImpl, testWork);
        LazyWorkContinuationImpl dependent = (LazyWorkContinuationImpl) (continuation.then(
                dependentWork));

        assertNotNull(dependent.mParent);
        assertEquals(dependent.mParent, continuation);

        assertEquals(dependent.mIds.length, 1);
        assertEquals(dependent.mAllIds.size(), 2);
    }

    @Test
    public void testLazyContinuation_enqueue() {
        Work testWork = createTestWorker();
        LazyWorkContinuationImpl continuation = new LazyWorkContinuationImpl(mWorkManagerImpl,
                testWork);
        assertEquals(continuation.mEnqueued, false);
        continuation.enqueue();

        verifyEnqueued(continuation);
        verify(mTaskExecutor, times(1)).executeOnBackgroundThread(any(EnqueueRunnable.class));
    }

    @Test
    public void testLazyContinuation_chainEnqueue() {
        Work testWork = createTestWorker();
        LazyWorkContinuationImpl continuation =
                new LazyWorkContinuationImpl(mWorkManagerImpl, testWork);
        LazyWorkContinuationImpl chain = (LazyWorkContinuationImpl) (
                continuation.then(createTestWorker()).then(createTestWorker(), createTestWorker()));

        chain.enqueue();
        verifyEnqueued(continuation);
        verify(mTaskExecutor, times(3)).executeOnBackgroundThread(any(EnqueueRunnable.class));
    }

    @Test
    public void testLazyContinuation_chainEnqueueNoOpOnRetry() {
        Work testWork = createTestWorker();
        LazyWorkContinuationImpl continuation =
                new LazyWorkContinuationImpl(mWorkManagerImpl, testWork);
        LazyWorkContinuationImpl chain = (LazyWorkContinuationImpl) (
                continuation.then(createTestWorker()).then(createTestWorker(), createTestWorker()));

        chain.enqueue();
        verifyEnqueued(continuation);
        verify(mTaskExecutor, times(3)).executeOnBackgroundThread(any(EnqueueRunnable.class));
        chain.enqueue();
        verifyNoMoreInteractions(mTaskExecutor);
    }

    private void verifyEnqueued(LazyWorkContinuationImpl continuation) {
        assertEquals(continuation.mEnqueued, true);
        LazyWorkContinuationImpl parent = continuation.mParent;
        while (parent != null) {
            assertEquals(parent.mEnqueued, true);
            parent = parent.mParent;
        }
    }

    private static Work createTestWorker() {
        return Work.newBuilder(TestWorker.class).build();
    }
}
