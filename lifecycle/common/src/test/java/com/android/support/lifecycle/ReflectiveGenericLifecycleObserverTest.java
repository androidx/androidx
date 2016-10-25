package com.android.support.lifecycle;

import static com.android.support.lifecycle.Lifecycle.CREATED;
import static com.android.support.lifecycle.Lifecycle.DESTROYED;
import static com.android.support.lifecycle.Lifecycle.FINISHED;
import static com.android.support.lifecycle.Lifecycle.INITIALIZED;
import static com.android.support.lifecycle.Lifecycle.PAUSED;
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
    LifecycleProvider provider;
    Lifecycle lifecycle;

    @Before
    public void initMocks() {
        provider = mock(LifecycleProvider.class);
        lifecycle = mock(Lifecycle.class);
        when(provider.getLifecycle()).thenReturn(lifecycle);
    }

    @Test
    public void anyState() {
        AnyStateListener obj = mock(AnyStateListener.class);
        ReflectiveGenericLifecycleObserver observer = new ReflectiveGenericLifecycleObserver(obj);
        when(lifecycle.getCurrentState()).thenReturn(STARTED);
        observer.onStateChanged(provider, CREATED);
        verify(obj).onAnyState();
        reset(obj);

        observer.onStateChanged(provider, STARTED);
        verify(obj).onAnyState();
        reset(obj);

        observer.onStateChanged(provider, RESUMED);
        verify(obj).onAnyState();
        reset(obj);

        observer.onStateChanged(provider, PAUSED);
        verify(obj).onAnyState();
        reset(obj);

        observer.onStateChanged(provider, STOPPED);
        verify(obj).onAnyState();
        reset(obj);

        observer.onStateChanged(provider, DESTROYED);
        verify(obj).onAnyState();
        reset(obj);

        observer.onStateChanged(provider, FINISHED);
        verify(obj).onAnyState();
        reset(obj);
    }

    private static class AnyStateListener implements LifecycleObserver {
        @OnState(Lifecycle.ANY)
        public void onAnyState() {

        }
    }

    @Test
    public void onCreated() {
        CreatedStateListener obj = mock(CreatedStateListener.class);
        ReflectiveGenericLifecycleObserver observer = new ReflectiveGenericLifecycleObserver(obj);
        when(lifecycle.getCurrentState()).thenReturn(CREATED);
        observer.onStateChanged(provider, INITIALIZED);
        verify(obj).onCreated();
        verify(obj).onCreated(provider);
        verify(obj).onCreated(provider, INITIALIZED);
    }

    private static class CreatedStateListener implements LifecycleObserver {
        @OnState(Lifecycle.CREATED)
        public void onCreated() {

        }
        @OnState(Lifecycle.CREATED)
        public void onCreated(LifecycleProvider provider) {

        }
        @OnState(Lifecycle.CREATED)
        public void onCreated(LifecycleProvider provider, int prevState) {

        }
    }
}
