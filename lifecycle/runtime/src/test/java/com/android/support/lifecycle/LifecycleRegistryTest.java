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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.support.test.filters.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.InOrder;

@RunWith(JUnit4.class)
@SmallTest
public class LifecycleRegistryTest {
    private LifecycleOwner mLifecycleOwner;
    private Lifecycle mLifecycle;
    private LifecycleRegistry mRegistry;

    @Before
    public void init() {
        mLifecycleOwner = mock(LifecycleOwner.class);
        mLifecycle = mock(Lifecycle.class);
        when(mLifecycleOwner.getLifecycle()).thenReturn(mLifecycle);
        mRegistry = new LifecycleRegistry(mLifecycleOwner);
    }

    @Test
    public void addRemove() {
        LifecycleObserver observer = mock(LifecycleObserver.class);
        mRegistry.addObserver(observer);
        assertThat(mRegistry.getObserverCount(), is(1));
        mRegistry.removeObserver(observer);
        assertThat(mRegistry.getObserverCount(), is(0));
    }

    @Test
    public void addGenericAndObserve() {
        GenericLifecycleObserver generic = mock(GenericLifecycleObserver.class);
        mRegistry.addObserver(generic);
        dispatchEvent(ON_CREATE);
        verify(generic).onStateChanged(mLifecycleOwner, ON_CREATE);
        reset(generic);
        dispatchEvent(ON_CREATE);
        verify(generic, never()).onStateChanged(mLifecycleOwner, ON_CREATE);
    }

    @Test
    public void addRegularClass() {
        TestObserver testObserver = mock(TestObserver.class);
        mRegistry.addObserver(testObserver);
        dispatchEvent(ON_START);
        verify(testObserver, never()).onStop();
        dispatchEvent(ON_STOP);
        verify(testObserver).onStop();
    }

    @Test
    public void add2RemoveOne() {
        TestObserver observer1 = mock(TestObserver.class);
        TestObserver observer2 = mock(TestObserver.class);
        TestObserver observer3 = mock(TestObserver.class);
        mRegistry.addObserver(observer1);
        mRegistry.addObserver(observer2);
        mRegistry.addObserver(observer3);

        dispatchEvent(ON_CREATE);

        verify(observer1).onCreate();
        verify(observer2).onCreate();
        verify(observer3).onCreate();
        reset(observer1, observer2, observer3);

        mRegistry.removeObserver(observer2);
        dispatchEvent(ON_START);

        verify(observer1).onStart();
        verify(observer2, never()).onStart();
        verify(observer3).onStart();
    }

    @Test
    public void removeWhileTraversing() {
        final TestObserver observer2 = mock(TestObserver.class);
        TestObserver observer1 = spy(new TestObserver() {
            @Override
            public void onCreate() {
                mRegistry.removeObserver(observer2);
            }
        });
        mRegistry.addObserver(observer1);
        mRegistry.addObserver(observer2);
        dispatchEvent(ON_CREATE);
        verify(observer2, never()).onCreate();
        verify(observer1).onCreate();
    }

    @Test
    public void constructionDestruction1() {
        fullyInitializeRegistry();
        final TestObserver observer = mock(TestObserver.class);
        mRegistry.addObserver(observer);
        InOrder constructionVerifier = inOrder(observer);
        constructionVerifier.verify(observer).onCreate();
        constructionVerifier.verify(observer).onStart();
        constructionVerifier.verify(observer).onResume();
    }

    @Test
    public void constructionDestruction2() {
        fullyInitializeRegistry();
        final TestObserver observer = spy(new TestObserver() {
            @Override
            void onStart() {
                dispatchEvent(ON_PAUSE);
            }
        });
        mRegistry.addObserver(observer);
        InOrder constructionOrder = inOrder(observer);
        constructionOrder.verify(observer).onCreate();
        constructionOrder.verify(observer).onStart();
        constructionOrder.verify(observer, never()).onResume();
    }

    @Test
    public void constructionDestruction3() {
        fullyInitializeRegistry();
        final TestObserver observer = spy(new TestObserver() {
            @Override
            void onStart() {
                dispatchEvent(ON_PAUSE);
                dispatchEvent(ON_STOP);
                dispatchEvent(ON_DESTROY);
            }
        });
        mRegistry.addObserver(observer);
        InOrder orderVerifier = inOrder(observer);
        orderVerifier.verify(observer).onCreate();
        orderVerifier.verify(observer).onStart();
        orderVerifier.verify(observer).onStop();
        orderVerifier.verify(observer).onDestroy();
        orderVerifier.verify(observer, never()).onResume();
    }

    @Test
    public void twoObserversChangingState() {
        final TestObserver observer1 = spy(new TestObserver() {
            @Override
            void onCreate() {
                dispatchEvent(ON_START);
            }
        });
        final TestObserver observer2 = mock(TestObserver.class);
        mRegistry.addObserver(observer1);
        mRegistry.addObserver(observer2);
        dispatchEvent(ON_CREATE);
        verify(observer1, times(1)).onCreate();
        verify(observer2, times(1)).onCreate();
        verify(observer1, times(1)).onStart();
        verify(observer2, times(1)).onStart();
    }

    private void dispatchEvent(@Lifecycle.Event int event) {
        when(mLifecycle.getCurrentState()).thenReturn(LifecycleRegistry.getStateAfter(event));
        mRegistry.handleLifecycleEvent(event);
    }

    private void fullyInitializeRegistry() {
        dispatchEvent(ON_CREATE);
        dispatchEvent(ON_START);
        dispatchEvent(ON_RESUME);
    }

    private abstract class TestObserver implements LifecycleObserver {
        @OnLifecycleEvent(ON_CREATE)
        void onCreate() {
        }

        @OnLifecycleEvent(ON_START)
        void onStart() {
        }

        @OnLifecycleEvent(ON_RESUME)
        void onResume() {
        }

        @OnLifecycleEvent(ON_PAUSE)
        void onPause() {
        }

        @OnLifecycleEvent(ON_STOP)
        void onStop() {
        }

        @OnLifecycleEvent(ON_DESTROY)
        void onDestroy() {
        }
    }
}
