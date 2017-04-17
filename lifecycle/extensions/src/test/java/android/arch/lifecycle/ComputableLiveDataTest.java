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

package android.arch.lifecycle;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import android.support.annotation.Nullable;

import android.arch.core.executor.AppToolkitTaskExecutor;
import android.arch.core.executor.TaskExecutor;
import android.arch.lifecycle.util.InstantTaskExecutor;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

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
        AppToolkitTaskExecutor.getInstance().setDelegate(mTaskExecutor);
    }

    @After
    public void removeExecutorDelegate() {
        AppToolkitTaskExecutor.getInstance().setDelegate(null);
    }

    @Test
    public void noComputeWithoutObservers() {
        final TestComputable computable = new TestComputable();
        verify(mTaskExecutor, never()).executeOnDiskIO(computable.mRefreshRunnable);
        verify(mTaskExecutor, never()).executeOnDiskIO(computable.mInvalidationRunnable);
    }

    @Test
    public void addingObserverShouldTriggerAComputation() {
        TestComputable computable = new TestComputable(1);
        mLifecycleOwner.handleEvent(Lifecycle.ON_CREATE);
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
        mLifecycleOwner.handleEvent(Lifecycle.ON_START);
        verify(mTaskExecutor).executeOnDiskIO(computable.mRefreshRunnable);
        assertThat(mValue.get(), is(1));
    }

    @Test
    public void invalidationShouldNotReTriggerComputationIfObserverIsInActive() {
        TestComputable computable = new TestComputable(1, 2);
        mLifecycleOwner.handleEvent(Lifecycle.ON_START);
        final AtomicInteger mValue = new AtomicInteger(-1);
        computable.getLiveData().observe(mLifecycleOwner, new Observer<Integer>() {
            @Override
            public void onChanged(@Nullable Integer integer) {
                //noinspection ConstantConditions
                mValue.set(integer);
            }
        });
        assertThat(mValue.get(), is(1));
        mLifecycleOwner.handleEvent(Lifecycle.ON_STOP);
        computable.invalidate();
        reset(mTaskExecutor);
        verify(mTaskExecutor, never()).executeOnDiskIO(computable.mRefreshRunnable);
        assertThat(mValue.get(), is(1));
    }

    @Test
    public void invalidationShouldReTriggerQueryIfObserverIsActive() {
        TestComputable computable = new TestComputable(1, 2);
        mLifecycleOwner.handleEvent(Lifecycle.ON_START);
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
        AtomicInteger mValueCounter;
        TestComputable(int... values) {
            mValueCounter = new AtomicInteger();
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

        void handleEvent(@Lifecycle.Event int event) {
            mLifecycle.handleLifecycleEvent(event);
        }
    }
}
