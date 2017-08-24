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

import static android.arch.lifecycle.Lifecycle.Event.ON_CREATE;
import static android.arch.lifecycle.Lifecycle.Event.ON_DESTROY;
import static android.arch.lifecycle.Lifecycle.Event.ON_PAUSE;
import static android.arch.lifecycle.Lifecycle.Event.ON_RESUME;
import static android.arch.lifecycle.Lifecycle.Event.ON_START;
import static android.arch.lifecycle.Lifecycle.Event.ON_STOP;

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

import android.arch.core.executor.AppToolkitTaskExecutor;
import android.arch.lifecycle.util.InstantTaskExecutor;
import android.support.annotation.Nullable;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

@SuppressWarnings({"unchecked"})
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
        assertThat(mLiveData.hasObservers(), is(true));
        assertThat(mLiveData.hasActiveObservers(), is(false));

        mLiveData.removeObserver(observer);
        verify(mActiveObserversChanged, never()).onCall(anyBoolean());
        assertThat(mLiveData.hasObservers(), is(false));
        assertThat(mLiveData.hasActiveObservers(), is(false));
    }

    @Test
    public void testActiveObserverToggle() {
        Observer<String> observer = (Observer<String>) mock(Observer.class);
        mLiveData.observe(mOwner, observer);

        verify(mActiveObserversChanged, never()).onCall(anyBoolean());
        assertThat(mLiveData.hasObservers(), is(true));
        assertThat(mLiveData.hasActiveObservers(), is(false));

        mRegistry.handleLifecycleEvent(ON_START);
        verify(mActiveObserversChanged).onCall(true);
        assertThat(mLiveData.hasActiveObservers(), is(true));
        reset(mActiveObserversChanged);

        mRegistry.handleLifecycleEvent(ON_STOP);
        verify(mActiveObserversChanged).onCall(false);
        assertThat(mLiveData.hasActiveObservers(), is(false));
        assertThat(mLiveData.hasObservers(), is(true));

        reset(mActiveObserversChanged);
        mRegistry.handleLifecycleEvent(ON_START);
        verify(mActiveObserversChanged).onCall(true);
        assertThat(mLiveData.hasActiveObservers(), is(true));
        assertThat(mLiveData.hasObservers(), is(true));

        reset(mActiveObserversChanged);
        mLiveData.removeObserver(observer);
        verify(mActiveObserversChanged).onCall(false);
        assertThat(mLiveData.hasActiveObservers(), is(false));
        assertThat(mLiveData.hasObservers(), is(false));

        verifyNoMoreInteractions(mActiveObserversChanged);
    }

    @Test
    public void testReAddSameObserverTuple() {
        Observer<String> observer = (Observer<String>) mock(Observer.class);
        mLiveData.observe(mOwner, observer);
        mLiveData.observe(mOwner, observer);
        assertThat(mLiveData.hasObservers(), is(true));
    }

    @Test
    public void testAdd2ObserversWithSameOwnerAndRemove() {
        Observer<String> o1 = (Observer<String>) mock(Observer.class);
        Observer<String> o2 = (Observer<String>) mock(Observer.class);
        mLiveData.observe(mOwner, o1);
        mLiveData.observe(mOwner, o2);
        assertThat(mLiveData.hasObservers(), is(true));
        verify(mActiveObserversChanged, never()).onCall(anyBoolean());

        mRegistry.handleLifecycleEvent(ON_START);
        verify(mActiveObserversChanged).onCall(true);
        mLiveData.setValue("a");
        verify(o1).onChanged("a");
        verify(o2).onChanged("a");

        mLiveData.removeObservers(mOwner);

        assertThat(mLiveData.hasObservers(), is(false));
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
        assertThat(mLiveData.hasObservers(), is(true));
        assertThat(mLiveData.hasActiveObservers(), is(true));

        reset(mActiveObserversChanged);

        mRegistry.handleLifecycleEvent(ON_DESTROY);
        assertThat(mLiveData.hasObservers(), is(false));
        assertThat(mLiveData.hasActiveObservers(), is(false));
        verify(mActiveObserversChanged).onCall(false);
    }

    @Test
    public void testInactiveRegistry() {
        Observer<String> observer = (Observer<String>) mock(Observer.class);
        mRegistry.handleLifecycleEvent(ON_DESTROY);
        mLiveData.observe(mOwner, observer);
        assertThat(mLiveData.hasObservers(), is(false));
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
                assertThat(mLiveData.hasObservers(), is(true));
                assertThat(mLiveData.hasActiveObservers(), is(false));
            }
        });
        final Observer observer2 = mock(Observer.class);
        mLiveData.observe(mOwner, observer1);
        mLiveData.observe(mOwner, observer2);
        mLiveData.setValue("bla");
        verify(observer1).onChanged(anyString());
        verify(observer2, Mockito.never()).onChanged(anyString());
        assertThat(mLiveData.hasObservers(), is(true));
        assertThat(mLiveData.hasActiveObservers(), is(false));
    }

    @Test
    public void testActiveChangeInCallback2() {
        Observer<String> observer1 = spy(new Observer<String>() {
            @Override
            public void onChanged(@Nullable String s) {
                assertThat(mInObserver, is(false));
                mInObserver = true;
                mRegistry.handleLifecycleEvent(ON_START);
                assertThat(mLiveData.hasActiveObservers(), is(true));
                mInObserver = false;
            }
        });
        final Observer observer2 = spy(new FailReentranceObserver());
        mLiveData.observeForever(observer1);
        mLiveData.observe(mOwner, observer2);
        mLiveData.setValue("bla");
        verify(observer1).onChanged(anyString());
        verify(observer2).onChanged(anyString());
        assertThat(mLiveData.hasObservers(), is(true));
        assertThat(mLiveData.hasActiveObservers(), is(true));
    }

    @Test
    public void testObserverRemovalInCallback() {
        mRegistry.handleLifecycleEvent(ON_START);
        Observer<String> observer = spy(new Observer<String>() {
            @Override
            public void onChanged(@Nullable String s) {
                assertThat(mLiveData.hasObservers(), is(true));
                mLiveData.removeObserver(this);
                assertThat(mLiveData.hasObservers(), is(false));
            }
        });
        mLiveData.observe(mOwner, observer);
        mLiveData.setValue("bla");
        verify(observer).onChanged(anyString());
        assertThat(mLiveData.hasObservers(), is(false));
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
                assertThat(mLiveData.hasObservers(), is(true));
                assertThat(mLiveData.hasActiveObservers(), is(true));
                mInObserver = false;
            }
        });
        mLiveData.observe(mOwner, observer1);
        mLiveData.setValue("bla");
        verify(observer1).onChanged(anyString());
        verify(observer2).onChanged(anyString());
        assertThat(mLiveData.hasObservers(), is(true));
        assertThat(mLiveData.hasActiveObservers(), is(true));
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
    public void testRemoveDuringSetValue() {
        mRegistry.handleLifecycleEvent(ON_START);
        final Observer observer1 = spy(new Observer<String>() {
            @Override
            public void onChanged(String o) {
                mLiveData.removeObserver(this);
            }
        });
        Observer<String> observer2 = (Observer<String>) mock(Observer.class);
        mLiveData.observeForever(observer1);
        mLiveData.observe(mOwner, observer2);
        mLiveData.setValue("gt");
        verify(observer2).onChanged("gt");
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

    @Test
    public void testNotCallInactiveWithObserveForever() {
        mRegistry.handleLifecycleEvent(ON_START);
        Observer<String> observer = (Observer<String>) mock(Observer.class);
        Observer<String> observer2 = (Observer<String>) mock(Observer.class);
        mLiveData.observe(mOwner, observer);
        mLiveData.observeForever(observer2);
        verify(mActiveObserversChanged).onCall(true);
        reset(mActiveObserversChanged);
        mRegistry.handleLifecycleEvent(ON_STOP);
        verify(mActiveObserversChanged, never()).onCall(anyBoolean());
        mRegistry.handleLifecycleEvent(ON_START);
        verify(mActiveObserversChanged, never()).onCall(anyBoolean());
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
