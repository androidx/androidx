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

import static com.android.support.lifecycle.Lifecycle.INITIALIZED;
import static com.android.support.lifecycle.Lifecycle.ON_CREATE;
import static com.android.support.lifecycle.Lifecycle.ON_DESTROY;
import static com.android.support.lifecycle.Lifecycle.ON_PAUSE;
import static com.android.support.lifecycle.Lifecycle.ON_RESUME;
import static com.android.support.lifecycle.Lifecycle.ON_START;
import static com.android.support.lifecycle.Lifecycle.ON_STOP;
import static com.android.support.lifecycle.Lifecycle.RESUMED;
import static com.android.support.lifecycle.Lifecycle.STARTED;
import static com.android.support.lifecycle.Lifecycle.STOPPED;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class ReflectiveGenericLifecycleObserverTest {
    private LifecycleProvider mProvider;
    private Lifecycle mLifecycle;

    @Before
    public void initMocks() {
        mProvider = mock(LifecycleProvider.class);
        mLifecycle = mock(Lifecycle.class);
        when(mProvider.getLifecycle()).thenReturn(mLifecycle);
    }

    @Test
    public void anyState() {
        AnyStateListener obj = mock(AnyStateListener.class);
        ReflectiveGenericLifecycleObserver observer = new ReflectiveGenericLifecycleObserver(obj);
        when(mLifecycle.getCurrentState()).thenReturn(STARTED);
        observer.onStateChanged(mProvider, ON_CREATE);
        verify(obj).onAnyState();
        reset(obj);

        observer.onStateChanged(mProvider, ON_START);
        verify(obj).onAnyState();
        reset(obj);

        observer.onStateChanged(mProvider, ON_RESUME);
        verify(obj).onAnyState();
        reset(obj);

        observer.onStateChanged(mProvider, ON_PAUSE);
        verify(obj).onAnyState();
        reset(obj);

        observer.onStateChanged(mProvider, ON_STOP);
        verify(obj).onAnyState();
        reset(obj);

        observer.onStateChanged(mProvider, ON_DESTROY);
        verify(obj).onAnyState();
        reset(obj);
    }

    private static class AnyStateListener implements LifecycleObserver {
        @OnLifecycleEvent(Lifecycle.ON_ANY)
        void onAnyState() {

        }
    }

    @Test
    public void singleMethod() {
        CreatedStateListener obj = mock(CreatedStateListener.class);
        ReflectiveGenericLifecycleObserver observer = new ReflectiveGenericLifecycleObserver(obj);
        when(mLifecycle.getCurrentState()).thenReturn(STOPPED);
        observer.onStateChanged(mProvider, ON_CREATE);
        verify(obj).onCreated();
        verify(obj).onCreated(mProvider);
        verify(obj).onCreated(mProvider, ON_CREATE);
    }

    private static class CreatedStateListener implements LifecycleObserver {
        @OnLifecycleEvent(ON_CREATE)
        void onCreated() {

        }
        @SuppressWarnings("UnusedParameters")
        @OnLifecycleEvent(ON_CREATE)
        void onCreated(LifecycleProvider provider) {

        }
        @SuppressWarnings("UnusedParameters")
        @OnLifecycleEvent(ON_CREATE)
        void onCreated(LifecycleProvider provider, int event) {

        }
    }

    @Test
    public void eachEvent() {
        AllMethodsListener obj = mock(AllMethodsListener.class);
        ReflectiveGenericLifecycleObserver observer = new ReflectiveGenericLifecycleObserver(obj);
        when(mLifecycle.getCurrentState()).thenReturn(STOPPED);

        observer.onStateChanged(mProvider, ON_CREATE);
        verify(obj).created();
        reset(obj);

        when(mLifecycle.getCurrentState()).thenReturn(STARTED);
        observer.onStateChanged(mProvider, ON_START);
        verify(obj).started();
        reset(obj);

        when(mLifecycle.getCurrentState()).thenReturn(RESUMED);
        observer.onStateChanged(mProvider, ON_RESUME);
        verify(obj).resumed();
        reset(obj);

        when(mLifecycle.getCurrentState()).thenReturn(STARTED);
        observer.onStateChanged(mProvider, ON_PAUSE);
        verify(obj).paused();
        reset(obj);

        when(mLifecycle.getCurrentState()).thenReturn(STOPPED);
        observer.onStateChanged(mProvider, ON_STOP);
        verify(obj).stopped();
        reset(obj);

        when(mLifecycle.getCurrentState()).thenReturn(INITIALIZED);
        observer.onStateChanged(mProvider, ON_DESTROY);
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
        void destroyed() {}
    }
}
