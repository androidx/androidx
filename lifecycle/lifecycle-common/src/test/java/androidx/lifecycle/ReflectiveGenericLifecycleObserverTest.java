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

import static androidx.lifecycle.Lifecycle.Event.ON_ANY;
import static androidx.lifecycle.Lifecycle.Event.ON_CREATE;
import static androidx.lifecycle.Lifecycle.Event.ON_DESTROY;
import static androidx.lifecycle.Lifecycle.Event.ON_PAUSE;
import static androidx.lifecycle.Lifecycle.Event.ON_RESUME;
import static androidx.lifecycle.Lifecycle.Event.ON_START;
import static androidx.lifecycle.Lifecycle.Event.ON_STOP;
import static androidx.lifecycle.Lifecycle.State.CREATED;
import static androidx.lifecycle.Lifecycle.State.INITIALIZED;
import static androidx.lifecycle.Lifecycle.State.RESUMED;
import static androidx.lifecycle.Lifecycle.State.STARTED;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Matchers;

@RunWith(JUnit4.class)
public class ReflectiveGenericLifecycleObserverTest {
    private LifecycleOwner mOwner;
    private Lifecycle mLifecycle;

    @Before
    public void initMocks() {
        mOwner = mock(LifecycleOwner.class);
        mLifecycle = mock(Lifecycle.class);
        when(mOwner.getLifecycle()).thenReturn(mLifecycle);
    }

    @Test
    public void anyState() {
        AnyStateListener obj = mock(AnyStateListener.class);
        ReflectiveGenericLifecycleObserver observer = new ReflectiveGenericLifecycleObserver(obj);
        when(mLifecycle.getCurrentState()).thenReturn(STARTED);
        observer.onStateChanged(mOwner, ON_CREATE);
        verify(obj).onAnyState(mOwner, ON_CREATE);
        reset(obj);

        observer.onStateChanged(mOwner, ON_START);
        verify(obj).onAnyState(mOwner, ON_START);
        reset(obj);

        observer.onStateChanged(mOwner, ON_RESUME);
        verify(obj).onAnyState(mOwner, ON_RESUME);
        reset(obj);

        observer.onStateChanged(mOwner, ON_PAUSE);
        verify(obj).onAnyState(mOwner, ON_PAUSE);
        reset(obj);

        observer.onStateChanged(mOwner, ON_STOP);
        verify(obj).onAnyState(mOwner, ON_STOP);
        reset(obj);

        observer.onStateChanged(mOwner, ON_DESTROY);
        verify(obj).onAnyState(mOwner, ON_DESTROY);
        reset(obj);
    }

    private static class AnyStateListener implements LifecycleObserver {
        @OnLifecycleEvent(ON_ANY)
        void onAnyState(LifecycleOwner owner, Lifecycle.Event event) {

        }
    }

    @Test
    public void singleMethod() {
        CreatedStateListener obj = mock(CreatedStateListener.class);
        ReflectiveGenericLifecycleObserver observer = new ReflectiveGenericLifecycleObserver(obj);
        when(mLifecycle.getCurrentState()).thenReturn(CREATED);
        observer.onStateChanged(mOwner, ON_CREATE);
        verify(obj).onCreated();
        verify(obj).onCreated(mOwner);
    }

    private static class CreatedStateListener implements LifecycleObserver {
        @OnLifecycleEvent(ON_CREATE)
        void onCreated() {

        }
        @SuppressWarnings("UnusedParameters")
        @OnLifecycleEvent(ON_CREATE)
        void onCreated(LifecycleOwner provider) {

        }
    }

    @Test
    public void eachEvent() {
        AllMethodsListener obj = mock(AllMethodsListener.class);
        ReflectiveGenericLifecycleObserver observer = new ReflectiveGenericLifecycleObserver(obj);
        when(mLifecycle.getCurrentState()).thenReturn(CREATED);

        observer.onStateChanged(mOwner, ON_CREATE);
        verify(obj).created();
        reset(obj);

        when(mLifecycle.getCurrentState()).thenReturn(STARTED);
        observer.onStateChanged(mOwner, ON_START);
        verify(obj).started();
        reset(obj);

        when(mLifecycle.getCurrentState()).thenReturn(RESUMED);
        observer.onStateChanged(mOwner, ON_RESUME);
        verify(obj).resumed();
        reset(obj);

        when(mLifecycle.getCurrentState()).thenReturn(STARTED);
        observer.onStateChanged(mOwner, ON_PAUSE);
        verify(obj).paused();
        reset(obj);

        when(mLifecycle.getCurrentState()).thenReturn(CREATED);
        observer.onStateChanged(mOwner, ON_STOP);
        verify(obj).stopped();
        reset(obj);

        when(mLifecycle.getCurrentState()).thenReturn(INITIALIZED);
        observer.onStateChanged(mOwner, ON_DESTROY);
        verify(obj).destroyed();
        reset(obj);
    }


    private static class AllMethodsListener implements LifecycleObserver {
        @OnLifecycleEvent(ON_CREATE)
        void created() {}

        @OnLifecycleEvent(ON_START)
        void started() {}

        @OnLifecycleEvent(ON_RESUME)
        void resumed() {}

        @OnLifecycleEvent(ON_PAUSE)
        void paused() {}

        @OnLifecycleEvent(ON_STOP)
        void stopped() {}

        @OnLifecycleEvent(ON_DESTROY)
        void destroyed() {
        }
    }

    @Test
    public void testFailingObserver() {
        class UnprecedentedError extends Error {
        }

        LifecycleObserver obj = new LifecycleObserver() {
            @OnLifecycleEvent(ON_START)
            void started() {
                throw new UnprecedentedError();
            }
        };
        ReflectiveGenericLifecycleObserver observer = new ReflectiveGenericLifecycleObserver(obj);
        try {
            observer.onStateChanged(mOwner, ON_START);
            fail();
        } catch (Exception e) {
            assertThat("exception cause is wrong",
                    e.getCause() instanceof UnprecedentedError);
        }
    }

    @Test
    public void testPrivateObserverMethods() {
        class ObserverWithPrivateMethod implements LifecycleObserver {
            boolean mCalled = false;
            @OnLifecycleEvent(ON_START)
            private void started() {
                mCalled = true;
            }
        }

        ObserverWithPrivateMethod obj = mock(ObserverWithPrivateMethod.class);
        ReflectiveGenericLifecycleObserver observer = new ReflectiveGenericLifecycleObserver(obj);
        observer.onStateChanged(mOwner, ON_START);
        assertThat(obj.mCalled, is(true));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testWrongFirstParam1() {
        LifecycleObserver observer = new LifecycleObserver() {
            @OnLifecycleEvent(ON_START)
            private void started(Lifecycle.Event e) {
            }
        };
        new ReflectiveGenericLifecycleObserver(observer);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testWrongFirstParam2() {
        LifecycleObserver observer = new LifecycleObserver() {
            @OnLifecycleEvent(ON_ANY)
            private void started(Lifecycle l, Lifecycle.Event e) {
            }
        };
        new ReflectiveGenericLifecycleObserver(observer);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testWrongSecondParam() {
        LifecycleObserver observer = new LifecycleObserver() {
            @OnLifecycleEvent(ON_START)
            private void started(LifecycleOwner owner, Lifecycle l) {
            }
        };
        new ReflectiveGenericLifecycleObserver(observer);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testThreeParams() {
        LifecycleObserver observer = new LifecycleObserver() {
            @OnLifecycleEvent(ON_ANY)
            private void started(LifecycleOwner owner, Lifecycle.Event e, int i) {
            }
        };
        new ReflectiveGenericLifecycleObserver(observer);
    }

    static class BaseClass1 implements LifecycleObserver {
        @OnLifecycleEvent(ON_START)
        void foo(LifecycleOwner owner) {
        }
    }

    static class DerivedClass1 extends BaseClass1 {
        @Override
        @OnLifecycleEvent(ON_STOP)
        void foo(LifecycleOwner owner) {
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidSuper1() {
        new ReflectiveGenericLifecycleObserver(new DerivedClass1());
    }

    static class BaseClass2 implements LifecycleObserver {
        @OnLifecycleEvent(ON_START)
        void foo(LifecycleOwner owner) {
        }
    }

    static class DerivedClass2 extends BaseClass1 {
        @OnLifecycleEvent(ON_STOP)
        void foo() {
        }
    }

    @Test
    public void testValidSuper1() {
        DerivedClass2 obj = mock(DerivedClass2.class);
        ReflectiveGenericLifecycleObserver observer = new ReflectiveGenericLifecycleObserver(obj);
        observer.onStateChanged(mock(LifecycleOwner.class), ON_START);
        verify(obj).foo(Matchers.<LifecycleOwner>any());
        verify(obj, never()).foo();
        reset(obj);
        observer.onStateChanged(mock(LifecycleOwner.class), ON_STOP);
        verify(obj).foo();
        verify(obj, never()).foo(Matchers.<LifecycleOwner>any());
    }

    static class BaseClass3 implements LifecycleObserver {
        @OnLifecycleEvent(ON_START)
        void foo(LifecycleOwner owner) {
        }
    }

    interface Interface3 extends LifecycleObserver {
        @OnLifecycleEvent(ON_STOP)
        void foo(LifecycleOwner owner);
    }

    static class DerivedClass3 extends BaseClass3 implements Interface3 {
        @Override
        public void foo(LifecycleOwner owner) {
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidSuper2() {
        new ReflectiveGenericLifecycleObserver(new DerivedClass3());
    }

    static class BaseClass4 implements LifecycleObserver {
        @OnLifecycleEvent(ON_START)
        void foo(LifecycleOwner owner) {
        }
    }

    interface Interface4 extends LifecycleObserver {
        @OnLifecycleEvent(ON_START)
        void foo(LifecycleOwner owner);
    }

    static class DerivedClass4 extends BaseClass4 implements Interface4 {
        @Override
        @OnLifecycleEvent(ON_START)
        public void foo(LifecycleOwner owner) {
        }

        @OnLifecycleEvent(ON_START)
        public void foo() {
        }
    }

    @Test
    public void testValidSuper2() {
        DerivedClass4 obj = mock(DerivedClass4.class);
        ReflectiveGenericLifecycleObserver observer = new ReflectiveGenericLifecycleObserver(obj);
        observer.onStateChanged(mock(LifecycleOwner.class), ON_START);
        verify(obj).foo(Matchers.<LifecycleOwner>any());
        verify(obj).foo();
    }

    interface InterfaceStart extends LifecycleObserver {
        @OnLifecycleEvent(ON_START)
        void foo(LifecycleOwner owner);
    }

    interface InterfaceStop extends LifecycleObserver {
        @OnLifecycleEvent(ON_STOP)
        void foo(LifecycleOwner owner);
    }

    static class DerivedClass5 implements InterfaceStart, InterfaceStop {
        @Override
        public void foo(LifecycleOwner owner) {
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidSuper3() {
        new ReflectiveGenericLifecycleObserver(new DerivedClass5());
    }
}
