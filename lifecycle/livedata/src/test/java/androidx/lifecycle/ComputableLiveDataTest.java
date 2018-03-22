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

package androidx.lifecycle;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.arch.core.executor.ArchTaskExecutor;
import androidx.arch.core.executor.TaskExecutor;
import androidx.arch.core.executor.TaskExecutorWithFakeMainThread;
import androidx.lifecycle.util.InstantTaskExecutor;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.ArgumentCaptor;

import java.util.Collections;
import java.util.concurrent.Executor;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@RunWith(JUnit4.class)
public class ComputableLiveDataTest {
    private TaskExecutor mTaskExecutor;
    private TestLifecycleOwner mLifecycleOwner;

    @Before
    public void setup() {
        mLifecycleOwner = new TestLifecycleOwner();
    }

    @Before
    public void swapExecutorDelegate() {
        mTaskExecutor = spy(new InstantTaskExecutor());
        ArchTaskExecutor.getInstance().setDelegate(mTaskExecutor);
    }

    @After
    public void removeExecutorDelegate() {
        ArchTaskExecutor.getInstance().setDelegate(null);
    }

    @Test
    public void noComputeWithoutObservers() {
        final TestComputable computable = new TestComputable();
        verify(mTaskExecutor, never()).executeOnDiskIO(computable.mRefreshRunnable);
        verify(mTaskExecutor, never()).executeOnDiskIO(computable.mInvalidationRunnable);
    }

    @Test
    public void noConcurrentCompute() throws InterruptedException {
        TaskExecutorWithFakeMainThread executor = new TaskExecutorWithFakeMainThread(2);
        ArchTaskExecutor.getInstance().setDelegate(executor);
        try {
            // # of compute calls
            final Semaphore computeCounter = new Semaphore(0);
            // available permits for computation
            final Semaphore computeLock = new Semaphore(0);
            final TestComputable computable = new TestComputable(1, 2) {
                @Override
                protected Integer compute() {
                    try {
                        computeCounter.release(1);
                        computeLock.tryAcquire(1, 20, TimeUnit.SECONDS);
                    } catch (InterruptedException e) {
                        throw new AssertionError(e);
                    }
                    return super.compute();
                }
            };
            final ArgumentCaptor<Integer> captor = ArgumentCaptor.forClass(Integer.class);
            //noinspection unchecked
            final Observer<Integer> observer = mock(Observer.class);
            executor.postToMainThread(new Runnable() {
                @Override
                public void run() {
                    computable.getLiveData().observeForever(observer);
                    verify(observer, never()).onChanged(anyInt());
                }
            });
            // wait for first compute call
            assertThat(computeCounter.tryAcquire(1, 2, TimeUnit.SECONDS), is(true));
            // re-invalidate while in compute
            computable.invalidate();
            computable.invalidate();
            computable.invalidate();
            computable.invalidate();
            // ensure another compute call does not arrive
            assertThat(computeCounter.tryAcquire(1, 2, TimeUnit.SECONDS), is(false));
            // allow computation to finish
            computeLock.release(2);
            // wait for the second result, first will be skipped due to invalidation during compute
            verify(observer, timeout(2000)).onChanged(captor.capture());
            assertThat(captor.getAllValues(), is(Collections.singletonList(2)));
            reset(observer);
            // allow all computations to run, there should not be any.
            computeLock.release(100);
            // unfortunately, Mockito.after is not available in 1.9.5
            executor.drainTasks(2);
            // assert no other results arrive
            verify(observer, never()).onChanged(anyInt());
        } finally {
            ArchTaskExecutor.getInstance().setDelegate(null);
        }
    }

    @Test
    public void addingObserverShouldTriggerAComputation() {
        TestComputable computable = new TestComputable(1);
        mLifecycleOwner.handleEvent(Lifecycle.Event.ON_CREATE);
        final AtomicInteger mValue = new AtomicInteger(-1);
        computable.getLiveData().observe(mLifecycleOwner, new Observer<Integer>() {
            @Override
            public void onChanged(@Nullable Integer integer) {
                //noinspection ConstantConditions
                mValue.set(integer);
            }
        });
        verify(mTaskExecutor, never()).executeOnDiskIO(any(Runnable.class));
        assertThat(mValue.get(), is(-1));
        mLifecycleOwner.handleEvent(Lifecycle.Event.ON_START);
        verify(mTaskExecutor).executeOnDiskIO(computable.mRefreshRunnable);
        assertThat(mValue.get(), is(1));
    }

    @Test
    public void customExecutor() {
        Executor customExecutor = mock(Executor.class);
        TestComputable computable = new TestComputable(customExecutor, 1);
        mLifecycleOwner.handleEvent(Lifecycle.Event.ON_CREATE);
        computable.getLiveData().observe(mLifecycleOwner, new Observer<Integer>() {
            @Override
            public void onChanged(@Nullable Integer integer) {
                // ignored
            }
        });
        verify(mTaskExecutor, never()).executeOnDiskIO(any(Runnable.class));
        verify(customExecutor, never()).execute(any(Runnable.class));

        mLifecycleOwner.handleEvent(Lifecycle.Event.ON_START);

        verify(mTaskExecutor, never()).executeOnDiskIO(computable.mRefreshRunnable);
        verify(customExecutor).execute(computable.mRefreshRunnable);
    }

    @Test
    public void invalidationShouldNotReTriggerComputationIfObserverIsInActive() {
        TestComputable computable = new TestComputable(1, 2);
        mLifecycleOwner.handleEvent(Lifecycle.Event.ON_START);
        final AtomicInteger mValue = new AtomicInteger(-1);
        computable.getLiveData().observe(mLifecycleOwner, new Observer<Integer>() {
            @Override
            public void onChanged(@Nullable Integer integer) {
                //noinspection ConstantConditions
                mValue.set(integer);
            }
        });
        assertThat(mValue.get(), is(1));
        mLifecycleOwner.handleEvent(Lifecycle.Event.ON_STOP);
        computable.invalidate();
        reset(mTaskExecutor);
        verify(mTaskExecutor, never()).executeOnDiskIO(computable.mRefreshRunnable);
        assertThat(mValue.get(), is(1));
    }

    @Test
    public void invalidationShouldReTriggerQueryIfObserverIsActive() {
        TestComputable computable = new TestComputable(1, 2);
        mLifecycleOwner.handleEvent(Lifecycle.Event.ON_START);
        final AtomicInteger mValue = new AtomicInteger(-1);
        computable.getLiveData().observe(mLifecycleOwner, new Observer<Integer>() {
            @Override
            public void onChanged(@Nullable Integer integer) {
                //noinspection ConstantConditions
                mValue.set(integer);
            }
        });
        assertThat(mValue.get(), is(1));
        computable.invalidate();
        assertThat(mValue.get(), is(2));
    }

    static class TestComputable extends ComputableLiveData<Integer> {
        final int[] mValues;
        AtomicInteger mValueCounter = new AtomicInteger();

        TestComputable(@NonNull Executor executor, int... values) {
            super(executor);
            mValues = values;
        }

        TestComputable(int... values) {
            mValues = values;
        }

        @Override
        protected Integer compute() {
            return mValues[mValueCounter.getAndIncrement()];
        }
    }

    static class TestLifecycleOwner implements LifecycleOwner {
        private LifecycleRegistry mLifecycle;

        TestLifecycleOwner() {
            mLifecycle = new LifecycleRegistry(this);
        }

        @Override
        public Lifecycle getLifecycle() {
            return mLifecycle;
        }

        void handleEvent(Lifecycle.Event event) {
            mLifecycle.handleLifecycleEvent(event);
        }
    }
}
