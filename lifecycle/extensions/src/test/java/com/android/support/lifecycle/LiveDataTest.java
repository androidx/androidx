/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.support.lifecycle;

import static com.android.support.lifecycle.Lifecycle.ON_CREATE;
import static com.android.support.lifecycle.Lifecycle.ON_DESTROY;
import static com.android.support.lifecycle.Lifecycle.ON_PAUSE;
import static com.android.support.lifecycle.Lifecycle.ON_RESUME;
import static com.android.support.lifecycle.Lifecycle.ON_START;
import static com.android.support.lifecycle.Lifecycle.ON_STOP;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import android.support.annotation.Nullable;
import android.support.test.filters.SmallTest;

import com.android.support.executors.AppToolkitTaskExecutor;
import com.android.support.lifecycle.util.InstantTaskExecutor;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

@SuppressWarnings({"unchecked"})
@SmallTest
public class LiveDataTest {
    private PublicLiveData<String> mLiveData;
    private LifecycleOwner mOwner;
    private LifecycleRegistry mRegistry;
    private MethodExec mActiveObserversChanged;
    private boolean mInObserver;

    @Before
    public void init() {
        mLiveData = new PublicLiveData<>();
        mOwner = mock(LifecycleOwner.class);
        mRegistry = new LifecycleRegistry(mOwner);
        when(mOwner.getLifecycle()).thenReturn(mRegistry);
        mActiveObserversChanged = mock(MethodExec.class);
        mLiveData.activeObserversChanged = mActiveObserversChanged;
        mInObserver = false;
    }

    @Before
    public void swapExecutorDelegate() {
        AppToolkitTaskExecutor.getInstance().setDelegate(new InstantTaskExecutor());
    }

    @After
    public void removeExecutorDelegate() {
        AppToolkitTaskExecutor.getInstance().setDelegate(null);
    }

    @Test
    public void testObserverToggle() {
        Observer<String> observer = (Observer<String>) mock(Observer.class);
        mLiveData.observe(mOwner, observer);

        verify(mActiveObserversChanged, never()).onCall(anyBoolean());
        assertThat(mLiveData.getObserverCount(), is(1));
        assertThat(mLiveData.getActiveObserverCount(), is(0));

        mLiveData.removeObserver(observer);
        verify(mActiveObserversChanged, never()).onCall(anyBoolean());
        assertThat(mLiveData.getObserverCount(), is(0));
        assertThat(mLiveData.getActiveObserverCount(), is(0));
    }

    @Test
    public void testActiveObserverToggle() {
        Observer<String> observer = (Observer<String>) mock(Observer.class);
        mLiveData.observe(mOwner, observer);

        verify(mActiveObserversChanged, never()).onCall(anyBoolean());
        assertThat(mLiveData.getObserverCount(), is(1));
        assertThat(mLiveData.getActiveObserverCount(), is(0));

        mRegistry.handleLifecycleEvent(ON_START);
        verify(mActiveObserversChanged).onCall(true);
        assertThat(mLiveData.getActiveObserverCount(), is(1));
        reset(mActiveObserversChanged);

        mRegistry.handleLifecycleEvent(ON_STOP);
        verify(mActiveObserversChanged).onCall(false);
        assertThat(mLiveData.getActiveObserverCount(), is(0));
        assertThat(mLiveData.getObserverCount(), is(1));

        reset(mActiveObserversChanged);
        mRegistry.handleLifecycleEvent(ON_START);
        verify(mActiveObserversChanged).onCall(true);
        assertThat(mLiveData.getActiveObserverCount(), is(1));
        assertThat(mLiveData.getObserverCount(), is(1));

        reset(mActiveObserversChanged);
        mLiveData.removeObserver(observer);
        verify(mActiveObserversChanged).onCall(false);
        assertThat(mLiveData.getActiveObserverCount(), is(0));
        assertThat(mLiveData.getObserverCount(), is(0));

        verifyNoMoreInteractions(mActiveObserversChanged);
    }

    @Test
    public void testReAddSameObserverTuple() {
        Observer<String> observer = (Observer<String>) mock(Observer.class);
        mLiveData.observe(mOwner, observer);
        mLiveData.observe(mOwner, observer);
        assertThat(mLiveData.getObserverCount(), is(1));
    }

    @Test
    public void testAdd2ObserversWithSameOwnerAndRemove() {
        Observer<String> o1 = (Observer<String>) mock(Observer.class);
        Observer<String> o2 = (Observer<String>) mock(Observer.class);
        mLiveData.observe(mOwner, o1);
        mLiveData.observe(mOwner, o2);
        assertThat(mLiveData.getObserverCount(), is(2));
        verify(mActiveObserversChanged, never()).onCall(anyBoolean());

        mRegistry.handleLifecycleEvent(ON_START);
        verify(mActiveObserversChanged).onCall(true);
        mLiveData.setValue("a");
        verify(o1).onChanged("a");
        verify(o2).onChanged("a");

        mLiveData.removeObservers(mOwner);

        assertThat(mLiveData.getObserverCount(), is(0));
        assertThat(mRegistry.getObserverCount(), is(0));
    }

    @Test
    public void testAddSameObserverIn2LifecycleOwners() {
        Observer<String> observer = (Observer<String>) mock(Observer.class);
        LifecycleOwner owner2 = mock(LifecycleOwner.class);
        LifecycleRegistry registry2 = new LifecycleRegistry(owner2);
        when(owner2.getLifecycle()).thenReturn(registry2);

        mLiveData.observe(mOwner, observer);
        Throwable throwable = null;
        try {
            mLiveData.observe(owner2, observer);
        } catch (Throwable t) {
            throwable = t;
        }
        assertThat(throwable, instanceOf(IllegalArgumentException.class));
        //noinspection ConstantConditions
        assertThat(throwable.getMessage(),
                is("Cannot add the same observer with different lifecycles"));
    }

    @Test
    public void testRemoveDestroyedObserver() {
        Observer<String> observer = (Observer<String>) mock(Observer.class);
        mLiveData.observe(mOwner, observer);
        mRegistry.handleLifecycleEvent(ON_START);
        verify(mActiveObserversChanged).onCall(true);
        assertThat(mLiveData.getObserverCount(), is(1));
        assertThat(mLiveData.getActiveObserverCount(), is(1));

        reset(mActiveObserversChanged);

        mRegistry.handleLifecycleEvent(ON_DESTROY);
        assertThat(mLiveData.getObserverCount(), is(0));
        assertThat(mLiveData.getActiveObserverCount(), is(0));
        verify(mActiveObserversChanged).onCall(false);
    }

    @Test
    public void testInactiveRegistry() {
        Observer<String> observer = (Observer<String>) mock(Observer.class);
        mRegistry.handleLifecycleEvent(ON_DESTROY);
        mLiveData.observe(mOwner, observer);
        assertThat(mLiveData.getObserverCount(), is(0));
    }

    @Test
    public void testNotifyActiveInactive() {
        Observer<String> observer = (Observer<String>) mock(Observer.class);
        mRegistry.handleLifecycleEvent(ON_CREATE);
        mLiveData.observe(mOwner, observer);
        mLiveData.setValue("a");
        verify(observer, never()).onChanged(anyString());
        mRegistry.handleLifecycleEvent(ON_START);
        verify(observer).onChanged("a");

        mLiveData.setValue("b");
        verify(observer).onChanged("b");

        mRegistry.handleLifecycleEvent(ON_STOP);
        mLiveData.setValue("c");
        verify(observer, never()).onChanged("c");

        mRegistry.handleLifecycleEvent(ON_START);
        verify(observer).onChanged("c");

        reset(observer);
        mRegistry.handleLifecycleEvent(ON_STOP);
        mRegistry.handleLifecycleEvent(ON_START);
        verify(observer, never()).onChanged(anyString());
    }

    @Test
    public void testStopObservingOwner_onDestroy() {
        Observer<String> observer = (Observer<String>) mock(Observer.class);
        mRegistry.handleLifecycleEvent(ON_CREATE);
        mLiveData.observe(mOwner, observer);
        assertThat(mRegistry.getObserverCount(), is(1));
        mRegistry.handleLifecycleEvent(ON_DESTROY);
        assertThat(mRegistry.getObserverCount(), is(0));
    }

    @Test
    public void testStopObservingOwner_onStopObserving() {
        Observer<String> observer = (Observer<String>) mock(Observer.class);
        mRegistry.handleLifecycleEvent(ON_CREATE);
        mLiveData.observe(mOwner, observer);
        assertThat(mRegistry.getObserverCount(), is(1));

        mLiveData.removeObserver(observer);
        assertThat(mRegistry.getObserverCount(), is(0));
    }

    @Test
    public void testActiveChangeInCallback() {
        mRegistry.handleLifecycleEvent(ON_START);
        Observer<String> observer1 = spy(new Observer<String>() {
            @Override
            public void onChanged(@Nullable String s) {
                mRegistry.handleLifecycleEvent(ON_STOP);
                assertThat(mLiveData.getObserverCount(), is(2));
                assertThat(mLiveData.getActiveObserverCount(), is(0));
            }
        });
        final Observer observer2 = mock(Observer.class);
        mLiveData.observe(mOwner, observer1);
        mLiveData.observe(mOwner, observer2);
        mLiveData.setValue("bla");
        verify(observer1).onChanged(anyString());
        verify(observer2, Mockito.never()).onChanged(anyString());
        assertThat(mLiveData.getObserverCount(), is(2));
        assertThat(mLiveData.getActiveObserverCount(), is(0));
    }

    @Test
    public void testActiveChangeInCallback2() {
        Observer<String> observer1 = spy(new Observer<String>() {
            @Override
            public void onChanged(@Nullable String s) {
                assertThat(mInObserver, is(false));
                mInObserver = true;
                mRegistry.handleLifecycleEvent(ON_START);
                assertThat(mLiveData.getActiveObserverCount(), is(2));
                mInObserver = false;
            }
        });
        final Observer observer2 = spy(new FailReentranceObserver());
        mLiveData.observeForever(observer1);
        mLiveData.observe(mOwner, observer2);
        mLiveData.setValue("bla");
        verify(observer1).onChanged(anyString());
        verify(observer2).onChanged(anyString());
        assertThat(mLiveData.getObserverCount(), is(2));
        assertThat(mLiveData.getActiveObserverCount(), is(2));
    }

    @Test
    public void testObserverRemovalInCallback() {
        mRegistry.handleLifecycleEvent(ON_START);
        Observer<String> observer = spy(new Observer<String>() {
            @Override
            public void onChanged(@Nullable String s) {
                assertThat(mLiveData.getObserverCount(), is(1));
                mLiveData.removeObserver(this);
                assertThat(mLiveData.getObserverCount(), is(0));
            }
        });
        mLiveData.observe(mOwner, observer);
        mLiveData.setValue("bla");
        verify(observer).onChanged(anyString());
        assertThat(mLiveData.getObserverCount(), is(0));
    }

    @Test
    public void testObserverAdditionInCallback() {
        mRegistry.handleLifecycleEvent(ON_START);
        final Observer observer2 = spy(new FailReentranceObserver());
        Observer<String> observer1 = spy(new Observer<String>() {
            @Override
            public void onChanged(@Nullable String s) {
                assertThat(mInObserver, is(false));
                mInObserver = true;
                mLiveData.observe(mOwner, observer2);
                assertThat(mLiveData.getObserverCount(), is(2));
                assertThat(mLiveData.getActiveObserverCount(), is(2));
                mInObserver = false;
            }
        });
        mLiveData.observe(mOwner, observer1);
        mLiveData.setValue("bla");
        verify(observer1).onChanged(anyString());
        verify(observer2).onChanged(anyString());
        assertThat(mLiveData.getObserverCount(), is(2));
        assertThat(mLiveData.getActiveObserverCount(), is(2));
    }

    @Test
    public void testObserverWithoutLifecycleOwner() {
        Observer<String> observer = (Observer<String>) mock(Observer.class);
        mLiveData.setValue("boring");
        mLiveData.observeForever(observer);
        verify(mActiveObserversChanged).onCall(true);
        verify(observer).onChanged("boring");
        mLiveData.setValue("tihs");
        verify(observer).onChanged("tihs");
        mLiveData.removeObserver(observer);
        verify(mActiveObserversChanged).onCall(false);
        mLiveData.setValue("boring");
        reset(observer);
        verify(observer, never()).onChanged(anyString());
    }

    @Test
    public void testSetValueDuringSetValue() {
        mRegistry.handleLifecycleEvent(ON_START);
        final Observer observer1 = spy(new Observer<String>() {
            @Override
            public void onChanged(String o) {
                assertThat(mInObserver, is(false));
                mInObserver = true;
                if (o.equals(("bla"))) {
                    mLiveData.setValue("gt");
                }
                mInObserver = false;
            }
        });
        final Observer observer2 = spy(new FailReentranceObserver());
        mLiveData.observe(mOwner, observer1);
        mLiveData.observe(mOwner, observer2);
        mLiveData.setValue("bla");
        verify(observer1, Mockito.atMost(2)).onChanged("gt");
        verify(observer2, Mockito.atMost(2)).onChanged("gt");
    }

    @Test
    public void testDataChangeDuringStateChange() {
        mRegistry.handleLifecycleEvent(ON_START);
        mRegistry.addObserver(new LifecycleObserver() {
            @OnLifecycleEvent(ON_STOP)
            public void onStop() {
                // change data in onStop, observer should not be called!
                mLiveData.setValue("b");
            }
        });
        Observer<String> observer = (Observer<String>) mock(Observer.class);
        mLiveData.setValue("a");
        mLiveData.observe(mOwner, observer);
        verify(observer).onChanged("a");
        mRegistry.handleLifecycleEvent(ON_PAUSE);
        mRegistry.handleLifecycleEvent(ON_STOP);
        verify(observer, never()).onChanged("b");

        mRegistry.handleLifecycleEvent(ON_RESUME);
        verify(observer).onChanged("b");
    }

    @SuppressWarnings("WeakerAccess")
    static class PublicLiveData<T> extends LiveData<T> {
        // cannot spy due to internal calls
        public MethodExec activeObserversChanged;

        @Override
        protected void onActive() {
            if (activeObserversChanged != null) {
                activeObserversChanged.onCall(true);
            }
        }

        @Override
        protected void onInactive() {
            if (activeObserversChanged != null) {
                activeObserversChanged.onCall(false);
            }
        }
    }

    private class FailReentranceObserver<T> implements Observer<T> {
        @Override
        public void onChanged(@Nullable T t) {
            assertThat(mInObserver, is(false));
        }
    }

    interface MethodExec {
        void onCall(boolean value);
    }
}
