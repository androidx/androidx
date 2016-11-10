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

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@RunWith(JUnit4.class)
public class ObserverListTest {
    private ObserverList mObserverList;

    @Before
    public void init() {
        mObserverList = new ObserverList();
    }

    @Test
    public void add() {
        GenericLifecycleObserver observer = mock(GenericLifecycleObserver.class);
        mObserverList.add(observer);
        assertThat(mObserverList.size(), is(1));
        assertThat(collect().get(0), is(observer));
        mObserverList.remove(observer);
        assertThat(mObserverList.size(), is(0));
    }

    @Test
    public void addTwice() {
        GenericLifecycleObserver observer = mock(GenericLifecycleObserver.class);
        mObserverList.add(observer);
        assertThat(mObserverList.size(), is(1));
        mObserverList.add(observer);
        assertThat(mObserverList.size(), is(1));
    }

    @Test
    public void addTwiceWhileIterating() {
        final ObserveAll observer = spy(new ObserveAll() {
            @Override
            public void onAny() {
                mObserverList.add(this);
                mObserverList.add(this);
                mObserverList.add(this);
            }
        });
        mObserverList.add(observer);
        mObserverList.forEach(new ObserverList.Callback() {
            @Override
            public void run(GenericLifecycleObserver ignored) {
                observer.onAny();
            }
        });
        verify(observer, times(1)).onAny();
        Assert.assertThat(mObserverList.size(), is(1));
    }

    @Test
    public void addRemoveAdd() {
        final ObserveAll observer2 = mock(ObserveAll.class);
        final ObserveAll observer1 = spy(new ObserveAll() {
            @Override
            public void onAny() {
                mObserverList.remove(this);
                mObserverList.add(this);
            }
        });
        mObserverList.add(observer1);
        mObserverList.add(observer2);
        assertThat(collectObservers(),
                equalTo(Arrays.asList((LifecycleObserver) observer1, observer2)));
        mObserverList.forEach(new ObserverList.Callback() {
            @Override
            public void run(GenericLifecycleObserver glo) {
                ((ObserveAll) glo.getReceiver()).onAny();
            }
        });
        verify(observer1, times(1)).onAny();
        verify(observer2, times(1)).onAny();
        // because 1 has been removed and re-added, it should get the next event after o1
        assertThat(collectObservers(),
                equalTo(Arrays.asList((LifecycleObserver) observer2, observer1)));
    }

    @Test
    public void remove() {
        GenericLifecycleObserver observer = mock(GenericLifecycleObserver.class);
        LifecycleObserver obj = mock(LifecycleObserver.class);
        when(observer.getReceiver()).thenReturn(obj);
        mObserverList.add(observer);
        assertThat(mObserverList.size(), is(1));
        assertThat(collect().get(0), is(observer));
        mObserverList.remove(observer);
        assertThat(mObserverList.size(), is(0));
    }

    @Test
    public void removeWhileTraversing() {
        GenericLifecycleObserver observer1 = mock(GenericLifecycleObserver.class);
        final GenericLifecycleObserver observer2 = mock(GenericLifecycleObserver.class);
        mObserverList.add(observer1);
        mObserverList.add(observer2);
        final AtomicBoolean first = new AtomicBoolean(true);
        mObserverList.forEach(new ObserverList.Callback() {
            @Override
            public void run(GenericLifecycleObserver observer) {
                if (first.getAndSet(false)) {
                    mObserverList.remove(observer2);
                } else {
                    fail("should never receive this call");
                }
            }
        });
    }

    @Test
    public void removeObjectWithMultipleCallbacksWhenTraversing() {
        // if the removed object has multiple callbacks and already received one, should receive
        // all.
        final AtomicBoolean first = new AtomicBoolean(true);
        final StartedObserverWith2Methods observer = spy(new StartedObserverWith2Methods() {
            @Override
            public void onStarted1() {
                handle();
            }

            @Override
            public void onStarted2() {
                handle();
            }

            private void handle() {
                if (first.getAndSet(false)) {
                    mObserverList.remove(this);
                }
            }
        });
        mObserverList.add(observer);

        final LifecycleProvider lifecycleProvider = mock(LifecycleProvider.class);
        Lifecycle lifecycle = mock(Lifecycle.class);
        when(lifecycleProvider.getLifecycle()).thenReturn(lifecycle);
        when(lifecycle.getCurrentState()).thenReturn(Lifecycle.STARTED);

        mObserverList.forEach(new ObserverList.Callback() {
            @Override
            public void run(GenericLifecycleObserver observer) {
                observer.onStateChanged(lifecycleProvider, Lifecycle.ON_START);
            }
        });

        verify(observer).onStarted1();
        verify(observer).onStarted2();
    }

    private List<GenericLifecycleObserver> collect() {
        final ArrayList<GenericLifecycleObserver> items = new ArrayList<>();
        mObserverList.forEach(new ObserverList.Callback() {
            @Override
            public void run(GenericLifecycleObserver observer) {
                items.add(observer);
            }
        });
        return items;
    }

    private List<LifecycleObserver> collectObservers() {
        final ArrayList<LifecycleObserver> items = new ArrayList<>();
        mObserverList.forEach(new ObserverList.Callback() {
            @Override
            public void run(GenericLifecycleObserver observer) {
                items.add((LifecycleObserver) observer.getReceiver());
            }
        });
        return items;
    }

    @SuppressWarnings("unused")
    private interface StartedObserverWith2Methods extends LifecycleObserver {
        @OnLifecycleEvent(Lifecycle.ON_START)
        void onStarted1();

        @OnLifecycleEvent(Lifecycle.ON_START)
        void onStarted2();
    }

    private interface ObserveAll extends LifecycleObserver {
        @OnLifecycleEvent(Lifecycle.ANY)
        void onAny();
    }
}
