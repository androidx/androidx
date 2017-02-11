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

import com.android.support.executors.AppToolkitTaskExecutor;
import com.android.support.lifecycle.util.InstantTaskExecutor;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

@SuppressWarnings({"unchecked"})
public class LiveDataTest {
    private PublicLiveData<String> mLiveData;
    private LifecycleProvider mProvider;
    private LifecycleRegistry mRegistry;
    private MethodExec mActiveObserversChanged;

    @Before
    public void init() {
        mLiveData = new PublicLiveData<>();
        mProvider = mock(LifecycleProvider.class);
        mRegistry = new LifecycleRegistry(mProvider);
        when(mProvider.getLifecycle()).thenReturn(mRegistry);
        mActiveObserversChanged = mock(MethodExec.class);
        mLiveData.activeObserversChanged = mActiveObserversChanged;
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
        mLiveData.observe(mProvider, observer);

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
        mLiveData.observe(mProvider, observer);

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
        mLiveData.observe(mProvider, observer);
        mLiveData.observe(mProvider, observer);
        assertThat(mLiveData.getObserverCount(), is(1));
    }

    @Test
    public void testAdd2ObserversWithSameProviderAndRemove() {
        Observer<String> o1 = (Observer<String>) mock(Observer.class);
        Observer<String> o2 = (Observer<String>) mock(Observer.class);
        mLiveData.observe(mProvider, o1);
        mLiveData.observe(mProvider, o2);
        assertThat(mLiveData.getObserverCount(), is(2));
        verify(mActiveObserversChanged, never()).onCall(anyBoolean());

        mRegistry.handleLifecycleEvent(ON_START);
        verify(mActiveObserversChanged).onCall(true);
        mLiveData.setValue("a");
        verify(o1).onChanged("a");
        verify(o2).onChanged("a");

        mLiveData.removeObservers(mProvider);

        assertThat(mLiveData.getObserverCount(), is(0));
        assertThat(mRegistry.size(), is(0));
    }

    @Test
    public void testAddSameObserverIn2LifecycleProviders() {
        Observer<String> observer = (Observer<String>) mock(Observer.class);
        LifecycleProvider provider2 = mock(LifecycleProvider.class);
        LifecycleRegistry registry2 = new LifecycleRegistry(provider2);
        when(provider2.getLifecycle()).thenReturn(registry2);

        mLiveData.observe(mProvider, observer);
        Throwable throwable = null;
        try {
            mLiveData.observe(provider2, observer);
        } catch (Throwable t) {
            throwable = t;
        }
        assertThat(throwable, instanceOf(IllegalArgumentException.class));
        assertThat(throwable.getMessage(),
                is("Cannot add the same observer twice to the LiveData"));
    }

    @Test
    public void testRemoveDestroyedObserver() {
        Observer<String> observer = (Observer<String>) mock(Observer.class);
        mLiveData.observe(mProvider, observer);
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
        mLiveData.observe(mProvider, observer);
        assertThat(mLiveData.getObserverCount(), is(0));
    }

    @Test
    public void testNotifyActiveInactive() {
        Observer<String> observer = (Observer<String>) mock(Observer.class);
        mRegistry.handleLifecycleEvent(ON_CREATE);
        mLiveData.observe(mProvider, observer);
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
    public void testStopObservingProvider_onDestroy() {
        Observer<String> observer = (Observer<String>) mock(Observer.class);
        mRegistry.handleLifecycleEvent(ON_CREATE);
        mLiveData.observe(mProvider, observer);
        assertThat(mRegistry.size(), is(1));
        mRegistry.handleLifecycleEvent(ON_DESTROY);
        assertThat(mRegistry.size(), is(0));
    }

    @Test
    public void testStopObservingProvider_onStopObserving() {
        Observer<String> observer = (Observer<String>) mock(Observer.class);
        mRegistry.handleLifecycleEvent(ON_CREATE);
        mLiveData.observe(mProvider, observer);
        assertThat(mRegistry.size(), is(1));

        mLiveData.removeObserver(observer);
        assertThat(mRegistry.size(), is(0));
    }

    @Test
    public void testActiveChangeInCallback() {
        mRegistry.handleLifecycleEvent(ON_START);
        Observer<String> observer = spy(new Observer<String>() {
            @Override
            public void onChanged(@Nullable String s) {
                mRegistry.handleLifecycleEvent(ON_STOP);
                assertThat(mLiveData.getObserverCount(), is(1));
                assertThat(mLiveData.getActiveObserverCount(), is(1));
            }
        });
        mLiveData.observe(mProvider, observer);
        mLiveData.setValue("bla");
        verify(observer).onChanged(anyString());
        assertThat(mLiveData.getObserverCount(), is(1));
        assertThat(mLiveData.getActiveObserverCount(), is(0));
    }

    @Test
    public void testObserverRemovalInCallback() {
        mRegistry.handleLifecycleEvent(ON_START);
        Observer<String> observer = spy(new Observer<String>() {
            @Override
            public void onChanged(@Nullable String s) {
                mLiveData.removeObserver(this);
                assertThat(mLiveData.getObserverCount(), is(1));
            }
        });
        mLiveData.observe(mProvider, observer);
        mLiveData.setValue("bla");
        verify(observer).onChanged(anyString());
        assertThat(mLiveData.getObserverCount(), is(0));
    }

    @Test
    public void testObserverAdditionInCallback() {
        mRegistry.handleLifecycleEvent(ON_START);
        Observer<String> observer = spy(new Observer<String>() {
            @Override
            public void onChanged(@Nullable String s) {
                mLiveData.observe(mProvider, mock(Observer.class));
                assertThat(mLiveData.getObserverCount(), is(1));
                assertThat(mLiveData.getActiveObserverCount(), is(1));
            }
        });
        mLiveData.observe(mProvider, observer);
        mLiveData.setValue("bla");
        verify(observer).onChanged(anyString());
        assertThat(mLiveData.getObserverCount(), is(2));
        assertThat(mLiveData.getActiveObserverCount(), is(2));
    }

    @Test
    public void testObserverWithoutLifecycleProvider() {
        Observer<String> observer = (Observer<String>) mock(Observer.class);
        mLiveData.setValue("boring");
        mLiveData.observe(observer);
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

    @SuppressWarnings("WeakerAccess")
    static class PublicLiveData<T> extends LiveData<T> {
        // cannot spy due to internal calls
        public MethodExec activeObserversChanged;
        @Override
        public void setValue(T value) {
            super.setValue(value);
        }

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

    interface MethodExec {
        void onCall(boolean value);
    }
}
