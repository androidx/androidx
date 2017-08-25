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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.InOrder;

@RunWith(JUnit4.class)
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
    public void constructionOrder() {
        fullyInitializeRegistry();
        final TestObserver observer = mock(TestObserver.class);
        mRegistry.addObserver(observer);
        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer).onCreate();
        inOrder.verify(observer).onStart();
        inOrder.verify(observer).onResume();
    }

    @Test
    public void constructionDestruction1() {
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
    public void constructionDestruction2() {
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

    @Test
    public void addDuringTraversing() {
        final TestObserver observer3 = mock(TestObserver.class);
        final TestObserver observer1 = spy(new TestObserver() {
            @Override
            public void onStart() {
                mRegistry.addObserver(observer3);
            }
        });
        final TestObserver observer2 = mock(TestObserver.class);

        mRegistry.addObserver(observer1);
        mRegistry.addObserver(observer2);

        dispatchEvent(ON_CREATE);
        dispatchEvent(ON_START);

        InOrder inOrder = inOrder(observer1, observer2, observer3);
        inOrder.verify(observer1).onCreate();
        inOrder.verify(observer2).onCreate();
        inOrder.verify(observer1).onStart();
        inOrder.verify(observer3).onCreate();
        inOrder.verify(observer2).onStart();
        inOrder.verify(observer3).onStart();
    }

    @Test
    public void addDuringAddition() {
        final TestObserver observer3 = mock(TestObserver.class);
        final TestObserver observer2 = spy(new TestObserver() {
            @Override
            public void onCreate() {
                mRegistry.addObserver(observer3);
            }
        });

        final TestObserver observer1 = spy(new TestObserver() {
            @Override
            public void onResume() {
                mRegistry.addObserver(observer2);
            }
        });

        mRegistry.addObserver(observer1);

        dispatchEvent(ON_CREATE);
        dispatchEvent(ON_START);
        dispatchEvent(ON_RESUME);

        InOrder inOrder = inOrder(observer1, observer2, observer3);
        inOrder.verify(observer1).onCreate();
        inOrder.verify(observer1).onStart();
        inOrder.verify(observer1).onResume();
        inOrder.verify(observer2).onCreate();
        inOrder.verify(observer2).onStart();
        inOrder.verify(observer2).onResume();
        inOrder.verify(observer3).onCreate();
        inOrder.verify(observer3).onStart();
        inOrder.verify(observer3).onResume();
    }

    @Test
    public void subscribeToDead() {
        dispatchEvent(ON_CREATE);
        final TestObserver observer1 = mock(TestObserver.class);
        mRegistry.addObserver(observer1);
        verify(observer1).onCreate();
        dispatchEvent(ON_DESTROY);
        verify(observer1).onDestroy();
        final TestObserver observer2 = mock(TestObserver.class);
        mRegistry.addObserver(observer2);
        verify(observer2, never()).onCreate();
        reset(observer1);
        dispatchEvent(ON_CREATE);
        verify(observer1).onCreate();
        verify(observer2).onCreate();
    }

    @Test
    public void downEvents() {
        fullyInitializeRegistry();
        final TestObserver observer1 = mock(TestObserver.class);
        final TestObserver observer2 = mock(TestObserver.class);
        mRegistry.addObserver(observer1);
        mRegistry.addObserver(observer2);
        InOrder orderVerifier = inOrder(observer1, observer2);
        dispatchEvent(ON_PAUSE);
        orderVerifier.verify(observer2).onPause();
        orderVerifier.verify(observer1).onPause();
        dispatchEvent(ON_STOP);
        orderVerifier.verify(observer2).onStop();
        orderVerifier.verify(observer1).onStop();
        dispatchEvent(ON_DESTROY);
        orderVerifier.verify(observer2).onDestroy();
        orderVerifier.verify(observer1).onDestroy();
    }

    @Test
    public void downEventsAddition() {
        dispatchEvent(ON_CREATE);
        dispatchEvent(ON_START);
        final TestObserver observer1 = mock(TestObserver.class);
        final TestObserver observer3 = mock(TestObserver.class);
        final TestObserver observer2 = spy(new TestObserver() {
            @Override
            void onStop() {
                mRegistry.addObserver(observer3);
            }
        });
        mRegistry.addObserver(observer1);
        mRegistry.addObserver(observer2);
        InOrder orderVerifier = inOrder(observer1, observer2, observer3);
        dispatchEvent(ON_STOP);
        orderVerifier.verify(observer2).onStop();
        orderVerifier.verify(observer3).onCreate();
        orderVerifier.verify(observer1).onStop();
        dispatchEvent(ON_DESTROY);
        orderVerifier.verify(observer3).onDestroy();
        orderVerifier.verify(observer2).onDestroy();
        orderVerifier.verify(observer1).onDestroy();
    }

    @Test
    public void downEventsRemoveAll() {
        fullyInitializeRegistry();
        final TestObserver observer1 = mock(TestObserver.class);
        final TestObserver observer3 = mock(TestObserver.class);
        final TestObserver observer2 = spy(new TestObserver() {
            @Override
            void onStop() {
                mRegistry.removeObserver(observer3);
                mRegistry.removeObserver(this);
                mRegistry.removeObserver(observer1);
                assertThat(mRegistry.getObserverCount(), is(0));
            }
        });
        mRegistry.addObserver(observer1);
        mRegistry.addObserver(observer2);
        mRegistry.addObserver(observer3);
        InOrder orderVerifier = inOrder(observer1, observer2, observer3);
        dispatchEvent(ON_PAUSE);
        orderVerifier.verify(observer3).onPause();
        orderVerifier.verify(observer2).onPause();
        orderVerifier.verify(observer1).onPause();
        dispatchEvent(ON_STOP);
        orderVerifier.verify(observer3).onStop();
        orderVerifier.verify(observer2).onStop();
        orderVerifier.verify(observer1, never()).onStop();
        dispatchEvent(ON_PAUSE);
        orderVerifier.verify(observer3, never()).onPause();
        orderVerifier.verify(observer2, never()).onPause();
        orderVerifier.verify(observer1, never()).onPause();
    }

    @Test
    public void deadParentInAddition() {
        fullyInitializeRegistry();
        final TestObserver observer2 = mock(TestObserver.class);
        final TestObserver observer3 = mock(TestObserver.class);

        TestObserver observer1 = spy(new TestObserver() {
            @Override
            void onStart() {
                mRegistry.removeObserver(this);
                assertThat(mRegistry.getObserverCount(), is(0));
                mRegistry.addObserver(observer2);
                mRegistry.addObserver(observer3);
            }
        });

        mRegistry.addObserver(observer1);

        InOrder inOrder = inOrder(observer1, observer2, observer3);
        inOrder.verify(observer1).onCreate();
        inOrder.verify(observer1).onStart();
        inOrder.verify(observer2).onCreate();
        inOrder.verify(observer3).onCreate();
        inOrder.verify(observer2).onStart();
        inOrder.verify(observer2).onResume();
        inOrder.verify(observer3).onStart();
        inOrder.verify(observer3).onResume();
    }

    @Test
    public void deadParentWhileTraversing() {
        final TestObserver observer2 = mock(TestObserver.class);
        final TestObserver observer3 = mock(TestObserver.class);
        TestObserver observer1 = spy(new TestObserver() {
            @Override
            void onStart() {
                mRegistry.removeObserver(this);
                assertThat(mRegistry.getObserverCount(), is(0));
                mRegistry.addObserver(observer2);
                mRegistry.addObserver(observer3);
            }
        });
        InOrder inOrder = inOrder(observer1, observer2, observer3);
        mRegistry.addObserver(observer1);
        dispatchEvent(ON_CREATE);
        dispatchEvent(ON_START);
        inOrder.verify(observer1).onCreate();
        inOrder.verify(observer1).onStart();
        inOrder.verify(observer2).onCreate();
        inOrder.verify(observer3).onCreate();
        inOrder.verify(observer2).onStart();
        inOrder.verify(observer3).onStart();
    }

    @Test
    public void removeCascade() {
        final TestObserver observer3 = mock(TestObserver.class);
        final TestObserver observer4 = mock(TestObserver.class);

        final TestObserver observer2 = spy(new TestObserver() {
            @Override
            void onStart() {
                mRegistry.removeObserver(this);
            }
        });

        TestObserver observer1 = spy(new TestObserver() {
            @Override
            void onResume() {
                mRegistry.removeObserver(this);
                mRegistry.addObserver(observer2);
                mRegistry.addObserver(observer3);
                mRegistry.addObserver(observer4);
            }
        });
        fullyInitializeRegistry();
        mRegistry.addObserver(observer1);
        InOrder inOrder = inOrder(observer1, observer2, observer3, observer4);
        inOrder.verify(observer1).onCreate();
        inOrder.verify(observer1).onStart();
        inOrder.verify(observer1).onResume();
        inOrder.verify(observer2).onCreate();
        inOrder.verify(observer2).onStart();
        inOrder.verify(observer3).onCreate();
        inOrder.verify(observer3).onStart();
        inOrder.verify(observer4).onCreate();
        inOrder.verify(observer4).onStart();
        inOrder.verify(observer3).onResume();
        inOrder.verify(observer4).onResume();
    }

    @Test
    public void changeStateDuringDescending() {
        final TestObserver observer2 = mock(TestObserver.class);
        final TestObserver observer1 = spy(new TestObserver() {
            @Override
            void onPause() {
                // but tonight I bounce back
                mRegistry.handleLifecycleEvent(ON_RESUME);
                mRegistry.addObserver(observer2);
            }
        });
        fullyInitializeRegistry();
        mRegistry.addObserver(observer1);
        mRegistry.handleLifecycleEvent(ON_PAUSE);
        InOrder inOrder = inOrder(observer1, observer2);
        inOrder.verify(observer1).onPause();
        inOrder.verify(observer2).onCreate();
        inOrder.verify(observer2).onStart();
        inOrder.verify(observer1).onResume();
        inOrder.verify(observer2).onResume();
    }

    @Test
    public void siblingLimitationCheck() {
        fullyInitializeRegistry();
        final TestObserver observer2 = mock(TestObserver.class);
        final TestObserver observer3 = mock(TestObserver.class);
        final TestObserver observer1 = spy(new TestObserver() {
            @Override
            void onStart() {
                mRegistry.addObserver(observer2);
            }

            @Override
            void onResume() {
                mRegistry.addObserver(observer3);
            }
        });
        mRegistry.addObserver(observer1);
        InOrder inOrder = inOrder(observer1, observer2, observer3);
        inOrder.verify(observer1).onCreate();
        inOrder.verify(observer1).onStart();
        inOrder.verify(observer2).onCreate();
        inOrder.verify(observer1).onResume();
        inOrder.verify(observer3).onCreate();
        inOrder.verify(observer2).onStart();
        inOrder.verify(observer2).onResume();
        inOrder.verify(observer3).onStart();
        inOrder.verify(observer3).onResume();
    }

    @Test
    public void siblingRemovalLimitationCheck1() {
        fullyInitializeRegistry();
        final TestObserver observer2 = mock(TestObserver.class);
        final TestObserver observer3 = mock(TestObserver.class);
        final TestObserver observer4 = mock(TestObserver.class);
        final TestObserver observer1 = spy(new TestObserver() {
            @Override
            void onStart() {
                mRegistry.addObserver(observer2);
            }

            @Override
            void onResume() {
                mRegistry.removeObserver(observer2);
                mRegistry.addObserver(observer3);
                mRegistry.addObserver(observer4);
            }
        });
        mRegistry.addObserver(observer1);
        InOrder inOrder = inOrder(observer1, observer2, observer3, observer4);
        inOrder.verify(observer1).onCreate();
        inOrder.verify(observer1).onStart();
        inOrder.verify(observer2).onCreate();
        inOrder.verify(observer1).onResume();
        inOrder.verify(observer3).onCreate();
        inOrder.verify(observer3).onStart();
        inOrder.verify(observer4).onCreate();
        inOrder.verify(observer4).onStart();
        inOrder.verify(observer3).onResume();
        inOrder.verify(observer4).onResume();
    }

    @Test
    public void siblingRemovalLimitationCheck2() {
        fullyInitializeRegistry();
        final TestObserver observer2 = mock(TestObserver.class);
        final TestObserver observer3 = spy(new TestObserver() {
            @Override
            void onCreate() {
                mRegistry.removeObserver(observer2);
            }
        });
        final TestObserver observer4 = mock(TestObserver.class);
        final TestObserver observer1 = spy(new TestObserver() {
            @Override
            void onStart() {
                mRegistry.addObserver(observer2);
            }

            @Override
            void onResume() {
                mRegistry.addObserver(observer3);
                mRegistry.addObserver(observer4);
            }
        });

        mRegistry.addObserver(observer1);
        InOrder inOrder = inOrder(observer1, observer2, observer3, observer4);
        inOrder.verify(observer1).onCreate();
        inOrder.verify(observer1).onStart();
        inOrder.verify(observer2).onCreate();
        inOrder.verify(observer1).onResume();
        inOrder.verify(observer3).onCreate();
        inOrder.verify(observer3).onStart();
        inOrder.verify(observer4).onCreate();
        inOrder.verify(observer4).onStart();
        inOrder.verify(observer3).onResume();
        inOrder.verify(observer4).onResume();
    }

    @Test
    public void sameObserverReAddition() {
        TestObserver observer = mock(TestObserver.class);
        mRegistry.addObserver(observer);
        mRegistry.removeObserver(observer);
        mRegistry.addObserver(observer);
        dispatchEvent(ON_CREATE);
        verify(observer).onCreate();
    }

    private void dispatchEvent(Lifecycle.Event event) {
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
