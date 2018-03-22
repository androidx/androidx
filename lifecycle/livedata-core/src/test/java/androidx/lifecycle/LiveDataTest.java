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

import static androidx.lifecycle.Lifecycle.Event.ON_CREATE;
import static androidx.lifecycle.Lifecycle.Event.ON_DESTROY;
import static androidx.lifecycle.Lifecycle.Event.ON_PAUSE;
import static androidx.lifecycle.Lifecycle.Event.ON_RESUME;
import static androidx.lifecycle.Lifecycle.Event.ON_START;
import static androidx.lifecycle.Lifecycle.Event.ON_STOP;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import androidx.annotation.Nullable;
import androidx.arch.core.executor.ArchTaskExecutor;
import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mockito;

@SuppressWarnings({"unchecked"})
@RunWith(JUnit4.class)
public class LiveDataTest {

    @Rule
    public InstantTaskExecutorRule mInstantTaskExecutorRule = new InstantTaskExecutorRule();

    private PublicLiveData<String> mLiveData;
    private MethodExec mActiveObserversChanged;

    private LifecycleOwner mOwner;
    private LifecycleRegistry mRegistry;

    private LifecycleOwner mOwner2;
    private LifecycleRegistry mRegistry2;

    private LifecycleOwner mOwner3;
    private Lifecycle mLifecycle3;
    private Observer<String> mObserver3;

    private LifecycleOwner mOwner4;
    private Lifecycle mLifecycle4;
    private Observer<String> mObserver4;

    private boolean mInObserver;

    @Before
    public void init() {
        mLiveData = new PublicLiveData<>();

        mActiveObserversChanged = mock(MethodExec.class);
        mLiveData.activeObserversChanged = mActiveObserversChanged;

        mOwner = mock(LifecycleOwner.class);
        mRegistry = new LifecycleRegistry(mOwner);
        when(mOwner.getLifecycle()).thenReturn(mRegistry);

        mOwner2 = mock(LifecycleOwner.class);
        mRegistry2 = new LifecycleRegistry(mOwner2);
        when(mOwner2.getLifecycle()).thenReturn(mRegistry2);

        mInObserver = false;
    }

    @Before
    public void initNonLifecycleRegistry() {
        mOwner3 = mock(LifecycleOwner.class);
        mLifecycle3 = mock(Lifecycle.class);
        mObserver3 = (Observer<String>) mock(Observer.class);
        when(mOwner3.getLifecycle()).thenReturn(mLifecycle3);

        mOwner4 = mock(LifecycleOwner.class);
        mLifecycle4 = mock(Lifecycle.class);
        mObserver4 = (Observer<String>) mock(Observer.class);
        when(mOwner4.getLifecycle()).thenReturn(mLifecycle4);
    }

    @After
    public void removeExecutorDelegate() {
        ArchTaskExecutor.getInstance().setDelegate(null);
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

        mLiveData.observe(mOwner, observer);
        Throwable throwable = null;
        try {
            mLiveData.observe(mOwner2, observer);
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

    @Test
    public void testRemoveDuringAddition() {
        mRegistry.handleLifecycleEvent(ON_START);
        mLiveData.setValue("bla");
        mLiveData.observeForever(new Observer<String>() {
            @Override
            public void onChanged(@Nullable String s) {
                mLiveData.removeObserver(this);
            }
        });
        assertThat(mLiveData.hasActiveObservers(), is(false));
        InOrder inOrder = Mockito.inOrder(mActiveObserversChanged);
        inOrder.verify(mActiveObserversChanged).onCall(true);
        inOrder.verify(mActiveObserversChanged).onCall(false);
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testRemoveDuringBringingUpToState() {
        mLiveData.setValue("bla");
        mLiveData.observeForever(new Observer<String>() {
            @Override
            public void onChanged(@Nullable String s) {
                mLiveData.removeObserver(this);
            }
        });
        mRegistry.handleLifecycleEvent(ON_RESUME);
        assertThat(mLiveData.hasActiveObservers(), is(false));
        InOrder inOrder = Mockito.inOrder(mActiveObserversChanged);
        inOrder.verify(mActiveObserversChanged).onCall(true);
        inOrder.verify(mActiveObserversChanged).onCall(false);
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void setValue_neverActive_observerOnChangedNotCalled() {
        Observer<String> observer = (Observer<String>) mock(Observer.class);
        mLiveData.observe(mOwner, observer);

        mLiveData.setValue("1");

        verify(observer, never()).onChanged(anyString());
    }

    @Test
    public void setValue_twoObserversTwoStartedOwners_onChangedCalledOnBoth() {
        Observer<String> observer1 = mock(Observer.class);
        Observer<String> observer2 = mock(Observer.class);

        mLiveData.observe(mOwner, observer1);
        mLiveData.observe(mOwner2, observer2);

        mRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START);
        mRegistry2.handleLifecycleEvent(Lifecycle.Event.ON_START);

        mLiveData.setValue("1");

        verify(observer1).onChanged("1");
        verify(observer2).onChanged("1");
    }

    @Test
    public void setValue_twoObserversOneStartedOwner_onChangedCalledOnOneCorrectObserver() {
        Observer<String> observer1 = mock(Observer.class);
        Observer<String> observer2 = mock(Observer.class);

        mLiveData.observe(mOwner, observer1);
        mLiveData.observe(mOwner2, observer2);

        mRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START);

        mLiveData.setValue("1");

        verify(observer1).onChanged("1");
        verify(observer2, never()).onChanged(anyString());
    }

    @Test
    public void setValue_twoObserversBothStartedAfterSetValue_onChangedCalledOnBoth() {
        Observer<String> observer1 = mock(Observer.class);
        Observer<String> observer2 = mock(Observer.class);

        mLiveData.observe(mOwner, observer1);
        mLiveData.observe(mOwner2, observer2);

        mLiveData.setValue("1");

        mRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START);
        mRegistry2.handleLifecycleEvent(Lifecycle.Event.ON_START);

        verify(observer1).onChanged("1");
        verify(observer1).onChanged("1");
    }

    @Test
    public void setValue_twoObserversOneStartedAfterSetValue_onChangedCalledOnCorrectObserver() {
        Observer<String> observer1 = mock(Observer.class);
        Observer<String> observer2 = mock(Observer.class);

        mLiveData.observe(mOwner, observer1);
        mLiveData.observe(mOwner2, observer2);

        mLiveData.setValue("1");

        mRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START);

        verify(observer1).onChanged("1");
        verify(observer2, never()).onChanged(anyString());
    }

    @Test
    public void setValue_twoObserversOneStarted_liveDataBecomesActive() {
        Observer<String> observer1 = mock(Observer.class);
        Observer<String> observer2 = mock(Observer.class);

        mLiveData.observe(mOwner, observer1);
        mLiveData.observe(mOwner2, observer2);

        mRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START);

        verify(mActiveObserversChanged).onCall(true);
    }

    @Test
    public void setValue_twoObserversOneStopped_liveDataStaysActive() {
        Observer<String> observer1 = mock(Observer.class);
        Observer<String> observer2 = mock(Observer.class);

        mLiveData.observe(mOwner, observer1);
        mLiveData.observe(mOwner2, observer2);

        mRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START);
        mRegistry2.handleLifecycleEvent(Lifecycle.Event.ON_START);

        verify(mActiveObserversChanged).onCall(true);

        reset(mActiveObserversChanged);
        mRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP);

        verify(mActiveObserversChanged, never()).onCall(anyBoolean());
    }

    @Test
    public void setValue_lifecycleIsCreatedNoEvent_liveDataBecomesInactiveAndObserverNotCalled() {

        // Arrange.

        mLiveData.observe(mOwner3, mObserver3);

        GenericLifecycleObserver lifecycleObserver = getGenericLifecycleObserver(mLifecycle3);

        when(mLifecycle3.getCurrentState()).thenReturn(Lifecycle.State.STARTED);
        lifecycleObserver.onStateChanged(mOwner3, Lifecycle.Event.ON_START);

        when(mLifecycle3.getCurrentState()).thenReturn(Lifecycle.State.CREATED);

        reset(mActiveObserversChanged);
        reset(mObserver3);

        // Act.

        mLiveData.setValue("1");

        // Assert.

        verify(mActiveObserversChanged).onCall(false);
        verify(mObserver3, never()).onChanged(anyString());
    }

    /*
     * Arrange: LiveData was made inactive via SetValue (because the Lifecycle it was
     * observing was in the CREATED state and no event was dispatched).
     * Act: Lifecycle enters Started state and dispatches event.
     * Assert: LiveData becomes active and dispatches new value to observer.
     */
    @Test
    public void test_liveDataInactiveViaSetValueThenLifecycleResumes() {

        // Arrange.

        mLiveData.observe(mOwner3, mObserver3);

        GenericLifecycleObserver lifecycleObserver = getGenericLifecycleObserver(mLifecycle3);

        when(mLifecycle3.getCurrentState()).thenReturn(Lifecycle.State.STARTED);
        lifecycleObserver.onStateChanged(mOwner3, Lifecycle.Event.ON_START);

        when(mLifecycle3.getCurrentState()).thenReturn(Lifecycle.State.CREATED);
        mLiveData.setValue("1");

        reset(mActiveObserversChanged);
        reset(mObserver3);

        // Act.

        when(mLifecycle3.getCurrentState()).thenReturn(Lifecycle.State.STARTED);
        lifecycleObserver.onStateChanged(mOwner3, Lifecycle.Event.ON_START);

        // Assert.

        verify(mActiveObserversChanged).onCall(true);
        verify(mObserver3).onChanged("1");
    }

    /*
     * Arrange: One of two Lifecycles enter the CREATED state without sending an event.
     * Act: Lifecycle's setValue method is called with new value.
     * Assert: LiveData stays active and new value is dispatched to Lifecycle that is still at least
     * STARTED.
     */
    @Test
    public void setValue_oneOfTwoLifecyclesAreCreatedNoEvent() {

        // Arrange.

        mLiveData.observe(mOwner3, mObserver3);
        mLiveData.observe(mOwner4, mObserver4);

        GenericLifecycleObserver lifecycleObserver3 = getGenericLifecycleObserver(mLifecycle3);
        GenericLifecycleObserver lifecycleObserver4 = getGenericLifecycleObserver(mLifecycle4);

        when(mLifecycle3.getCurrentState()).thenReturn(Lifecycle.State.STARTED);
        when(mLifecycle4.getCurrentState()).thenReturn(Lifecycle.State.STARTED);
        lifecycleObserver3.onStateChanged(mOwner3, Lifecycle.Event.ON_START);
        lifecycleObserver4.onStateChanged(mOwner4, Lifecycle.Event.ON_START);

        when(mLifecycle3.getCurrentState()).thenReturn(Lifecycle.State.CREATED);

        reset(mActiveObserversChanged);
        reset(mObserver3);
        reset(mObserver4);

        // Act.

        mLiveData.setValue("1");

        // Assert.

        verify(mActiveObserversChanged, never()).onCall(anyBoolean());
        verify(mObserver3, never()).onChanged(anyString());
        verify(mObserver4).onChanged("1");
    }

    /*
     * Arrange: Two observed Lifecycles enter the CREATED state without sending an event.
     * Act: Lifecycle's setValue method is called with new value.
     * Assert: LiveData becomes inactive and nothing is dispatched to either observer.
     */
    @Test
    public void setValue_twoLifecyclesAreCreatedNoEvent() {

        // Arrange.

        mLiveData.observe(mOwner3, mObserver3);
        mLiveData.observe(mOwner4, mObserver4);

        GenericLifecycleObserver lifecycleObserver3 = getGenericLifecycleObserver(mLifecycle3);
        GenericLifecycleObserver lifecycleObserver4 = getGenericLifecycleObserver(mLifecycle4);

        when(mLifecycle3.getCurrentState()).thenReturn(Lifecycle.State.STARTED);
        when(mLifecycle4.getCurrentState()).thenReturn(Lifecycle.State.STARTED);
        lifecycleObserver3.onStateChanged(mOwner3, Lifecycle.Event.ON_START);
        lifecycleObserver4.onStateChanged(mOwner4, Lifecycle.Event.ON_START);

        when(mLifecycle3.getCurrentState()).thenReturn(Lifecycle.State.CREATED);
        when(mLifecycle4.getCurrentState()).thenReturn(Lifecycle.State.CREATED);

        reset(mActiveObserversChanged);
        reset(mObserver3);
        reset(mObserver4);

        // Act.

        mLiveData.setValue("1");

        // Assert.

        verify(mActiveObserversChanged).onCall(false);
        verify(mObserver3, never()).onChanged(anyString());
        verify(mObserver3, never()).onChanged(anyString());
    }

    /*
     * Arrange: LiveData was made inactive via SetValue (because both Lifecycles it was
     * observing were in the CREATED state and no event was dispatched).
     * Act: One Lifecycle enters STARTED state and dispatches lifecycle event.
     * Assert: LiveData becomes active and dispatches new value to observer associated with started
     * Lifecycle.
     */
    @Test
    public void test_liveDataInactiveViaSetValueThenOneLifecycleResumes() {

        // Arrange.

        mLiveData.observe(mOwner3, mObserver3);
        mLiveData.observe(mOwner4, mObserver4);

        GenericLifecycleObserver lifecycleObserver3 = getGenericLifecycleObserver(mLifecycle3);
        GenericLifecycleObserver lifecycleObserver4 = getGenericLifecycleObserver(mLifecycle4);

        when(mLifecycle3.getCurrentState()).thenReturn(Lifecycle.State.STARTED);
        when(mLifecycle4.getCurrentState()).thenReturn(Lifecycle.State.STARTED);
        lifecycleObserver3.onStateChanged(mOwner3, Lifecycle.Event.ON_START);
        lifecycleObserver4.onStateChanged(mOwner4, Lifecycle.Event.ON_START);

        when(mLifecycle3.getCurrentState()).thenReturn(Lifecycle.State.CREATED);
        when(mLifecycle4.getCurrentState()).thenReturn(Lifecycle.State.CREATED);

        mLiveData.setValue("1");

        reset(mActiveObserversChanged);
        reset(mObserver3);
        reset(mObserver4);

        // Act.

        when(mLifecycle3.getCurrentState()).thenReturn(Lifecycle.State.STARTED);
        lifecycleObserver3.onStateChanged(mOwner3, Lifecycle.Event.ON_START);

        // Assert.

        verify(mActiveObserversChanged).onCall(true);
        verify(mObserver3).onChanged("1");
        verify(mObserver4, never()).onChanged(anyString());
    }

    @Test
    public void nestedForeverObserver() {
        mLiveData.setValue(".");
        mLiveData.observeForever(new Observer<String>() {
            @Override
            public void onChanged(@Nullable String s) {
                mLiveData.observeForever(mock(Observer.class));
                mLiveData.removeObserver(this);
            }
        });
        verify(mActiveObserversChanged, only()).onCall(true);
    }

    @Test
    public void readdForeverObserver() {
        Observer observer = mock(Observer.class);
        mLiveData.observeForever(observer);
        mLiveData.observeForever(observer);
        mLiveData.removeObserver(observer);
        assertThat(mLiveData.hasObservers(), is(false));
    }

    private GenericLifecycleObserver getGenericLifecycleObserver(Lifecycle lifecycle) {
        ArgumentCaptor<GenericLifecycleObserver> captor =
                ArgumentCaptor.forClass(GenericLifecycleObserver.class);
        verify(lifecycle).addObserver(captor.capture());
        return (captor.getValue());
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
