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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@RunWith(JUnit4.class)
public class ObserverSetTest {
    private ObserverSet<String> mObserverSet;

    @Before
    public void init() {
        mObserverSet = new ObserverSet<String>() {
            @Override
            protected boolean checkEquality(String existing, String added) {
                return existing.equals(added);
            }
        };
    }

    private void add(String value) {
        mObserverSet.add(value);
    }

    private void remove(String value) {
        mObserverSet.remove(value);
    }

    @Test
    public void add() {
        add("a");
        assertThat(mObserverSet.size(), is(1));
        assertThat(collect().get(0), is("a"));
        remove("a");
        assertThat(mObserverSet.size(), is(0));
    }

    @Test
    public void addTwice() {
        add("a");
        assertThat(mObserverSet.size(), is(1));
        add("a");
        assertThat(mObserverSet.size(), is(1));
    }

    @Test
    public void addTwiceWhileIterating() {
        add("a");
        final AtomicInteger cnt = new AtomicInteger(0);
        mObserverSet.forEach(new ObserverSet.Callback<String>() {
            @Override
            public void run(String key) {
                add("a");
                cnt.incrementAndGet();
            }
        });
        assertThat(cnt.get(), is(1));
        assertThat(mObserverSet.size(), is(1));
    }

    @Test
    public void addRemoveAdd() {
        final ObserverSet<ObserveAll> list = new ObserverSet<ObserveAll>() {
            @Override
            protected boolean checkEquality(ObserveAll existing,
                    ObserveAll added) {
                return existing == added;
            }
        };
        final ObserveAll observer2 = mock(ObserveAll.class);
        final ObserveAll observer1 = spy(new ObserveAll() {
            @Override
            public void onAny() {
                list.remove(this);
                list.add(this);
            }
        });
        list.add(observer1);
        list.add(observer2);
        assertThat(collect(list), equalTo(Arrays.asList(observer1, observer2)));
        list.forEach(new ObserverSet.Callback<ObserveAll>() {
            @Override
            public void run(ObserveAll key) {
                key.onAny();
            }
        });

        verify(observer1, times(1)).onAny();
        verify(observer2, times(1)).onAny();
        // because 1 has been removed and re-added, it should get the next event after o1
        assertThat(collect(list), equalTo(Arrays.asList(observer2, observer1)));
    }

    @Test
    public void remove() {
        add("a");
        assertThat(mObserverSet.size(), is(1));
        assertThat(collect().get(0), is("a"));
        remove("a");
        assertThat(mObserverSet.size(), is(0));
    }

    @Test
    public void removeWhileTraversing() {
        add("a");
        add("b");
        final AtomicBoolean first = new AtomicBoolean(true);
        mObserverSet.forEach(new ObserverSet.Callback<String>() {
            @Override
            public void run(String key) {
                if (first.getAndSet(false)) {
                    remove("b");
                } else {
                    fail("should never receive this call");
                }
            }
        });
    }

    private List<String> collect() {
        return collect(mObserverSet);
    }

    private <T> List<T> collect(ObserverSet<T> list) {
        final ArrayList<T> items = new ArrayList<>();
        list.forEach(new ObserverSet.Callback<T>() {
            @Override
            public void run(T value) {
                items.add(value);
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
