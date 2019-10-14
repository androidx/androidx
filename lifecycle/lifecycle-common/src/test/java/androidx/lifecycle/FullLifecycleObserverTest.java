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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import androidx.annotation.NonNull;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.InOrder;
import org.mockito.Mockito;

@RunWith(JUnit4.class)
public class FullLifecycleObserverTest {
    private LifecycleOwner mOwner;
    private Lifecycle mLifecycle;

    @Before
    public void initMocks() {
        mOwner = mock(LifecycleOwner.class);
        mLifecycle = mock(Lifecycle.class);
        when(mOwner.getLifecycle()).thenReturn(mLifecycle);
    }

    @Test
    public void eachEvent() {
        FullLifecycleObserver obj = mock(FullLifecycleObserver.class);
        FullLifecycleObserverAdapter observer = new FullLifecycleObserverAdapter(obj, null);
        when(mLifecycle.getCurrentState()).thenReturn(CREATED);

        observer.onStateChanged(mOwner, ON_CREATE);
        InOrder inOrder = Mockito.inOrder(obj);
        inOrder.verify(obj).onCreate(mOwner);
        reset(obj);

        when(mLifecycle.getCurrentState()).thenReturn(STARTED);
        observer.onStateChanged(mOwner, ON_START);
        inOrder.verify(obj).onStart(mOwner);
        reset(obj);

        when(mLifecycle.getCurrentState()).thenReturn(RESUMED);
        observer.onStateChanged(mOwner, ON_RESUME);
        inOrder.verify(obj).onResume(mOwner);
        reset(obj);

        when(mLifecycle.getCurrentState()).thenReturn(STARTED);
        observer.onStateChanged(mOwner, ON_PAUSE);
        inOrder.verify(obj).onPause(mOwner);
        reset(obj);

        when(mLifecycle.getCurrentState()).thenReturn(CREATED);
        observer.onStateChanged(mOwner, ON_STOP);
        inOrder.verify(obj).onStop(mOwner);
        reset(obj);

        when(mLifecycle.getCurrentState()).thenReturn(INITIALIZED);
        observer.onStateChanged(mOwner, ON_DESTROY);
        inOrder.verify(obj).onDestroy(mOwner);
        reset(obj);
    }

    @Test
    public void fullLifecycleObserverAndLifecycleEventObserver() {
        class AllObservers implements FullLifecycleObserver, LifecycleEventObserver {

            @Override
            public void onCreate(LifecycleOwner owner) {

            }

            @Override
            public void onStart(LifecycleOwner owner) {

            }

            @Override
            public void onResume(LifecycleOwner owner) {

            }

            @Override
            public void onPause(LifecycleOwner owner) {

            }

            @Override
            public void onStop(LifecycleOwner owner) {

            }

            @Override
            public void onDestroy(LifecycleOwner owner) {

            }

            @Override
            public void onStateChanged(@NonNull LifecycleOwner source,
                    @NonNull Lifecycle.Event event) {

            }
        }

        AllObservers obj = mock(AllObservers.class);
        FullLifecycleObserverAdapter observer = new FullLifecycleObserverAdapter(obj, obj);
        when(mLifecycle.getCurrentState()).thenReturn(CREATED);

        observer.onStateChanged(mOwner, ON_CREATE);
        InOrder inOrder = Mockito.inOrder(obj);
        inOrder.verify(obj).onCreate(mOwner);
        inOrder.verify(obj).onStateChanged(mOwner, ON_CREATE);
        reset(obj);

        when(mLifecycle.getCurrentState()).thenReturn(STARTED);
        observer.onStateChanged(mOwner, ON_START);
        inOrder.verify(obj).onStart(mOwner);
        inOrder.verify(obj).onStateChanged(mOwner, ON_START);
        reset(obj);

        when(mLifecycle.getCurrentState()).thenReturn(RESUMED);
        observer.onStateChanged(mOwner, ON_RESUME);
        inOrder.verify(obj).onResume(mOwner);
        inOrder.verify(obj).onStateChanged(mOwner, ON_RESUME);
        reset(obj);

        when(mLifecycle.getCurrentState()).thenReturn(STARTED);
        observer.onStateChanged(mOwner, ON_PAUSE);
        inOrder.verify(obj).onPause(mOwner);
        inOrder.verify(obj).onStateChanged(mOwner, ON_PAUSE);
        reset(obj);

        when(mLifecycle.getCurrentState()).thenReturn(CREATED);
        observer.onStateChanged(mOwner, ON_STOP);
        inOrder.verify(obj).onStop(mOwner);
        inOrder.verify(obj).onStateChanged(mOwner, ON_STOP);
        reset(obj);

        when(mLifecycle.getCurrentState()).thenReturn(INITIALIZED);
        observer.onStateChanged(mOwner, ON_DESTROY);
        inOrder.verify(obj).onDestroy(mOwner);
        inOrder.verify(obj).onStateChanged(mOwner, ON_DESTROY);
        reset(obj);
    }

    public void fullLifecycleObserverAndAnnotations() {
        class AnnotatedFullLifecycleObserver implements FullLifecycleObserver {
            @OnLifecycleEvent(ON_ANY)
            public void onAny() {
                throw new IllegalStateException("Annotations in FullLifecycleObserver "
                        + "must not be called");
            }

            @Override
            public void onCreate(LifecycleOwner owner) {

            }

            @Override
            public void onStart(LifecycleOwner owner) {

            }

            @Override
            public void onResume(LifecycleOwner owner) {

            }

            @Override
            public void onPause(LifecycleOwner owner) {

            }

            @Override
            public void onStop(LifecycleOwner owner) {

            }

            @Override
            public void onDestroy(LifecycleOwner owner) {

            }
        }
    }
}
