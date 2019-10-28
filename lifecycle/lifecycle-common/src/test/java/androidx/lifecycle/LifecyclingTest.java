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
import static androidx.lifecycle.Lifecycling.lifecycleEventObserver;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import androidx.annotation.NonNull;
import androidx.lifecycle.observers.DerivedSequence1;
import androidx.lifecycle.observers.DerivedSequence2;
import androidx.lifecycle.observers.DerivedWithNewMethods;
import androidx.lifecycle.observers.DerivedWithNoNewMethods;
import androidx.lifecycle.observers.DerivedWithOverridenMethodsWithLfAnnotation;
import androidx.lifecycle.observers.InterfaceImpl1;
import androidx.lifecycle.observers.InterfaceImpl2;
import androidx.lifecycle.observers.InterfaceImpl3;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class LifecyclingTest {

    @Test
    public void testDerivedWithNewLfMethodsNoGeneratedAdapter() {
        LifecycleEventObserver callback = lifecycleEventObserver(new DerivedWithNewMethods());
        assertThat(callback, instanceOf(ReflectiveGenericLifecycleObserver.class));
    }

    @Test
    public void testDerivedWithNoNewLfMethodsNoGeneratedAdapter() {
        LifecycleEventObserver callback = lifecycleEventObserver(new DerivedWithNoNewMethods());
        assertThat(callback, instanceOf(SingleGeneratedAdapterObserver.class));
    }

    @Test
    public void testDerivedWithOverridenMethodsNoGeneratedAdapter() {
        LifecycleEventObserver callback = lifecycleEventObserver(
                new DerivedWithOverridenMethodsWithLfAnnotation());
        // that is not effective but...
        assertThat(callback, instanceOf(ReflectiveGenericLifecycleObserver.class));
    }

    @Test
    public void testInterfaceImpl1NoGeneratedAdapter() {
        LifecycleEventObserver callback = lifecycleEventObserver(new InterfaceImpl1());
        assertThat(callback, instanceOf(SingleGeneratedAdapterObserver.class));
    }

    @Test
    public void testInterfaceImpl2NoGeneratedAdapter() {
        LifecycleEventObserver callback = lifecycleEventObserver(new InterfaceImpl2());
        assertThat(callback, instanceOf(CompositeGeneratedAdaptersObserver.class));
    }

    @Test
    public void testInterfaceImpl3NoGeneratedAdapter() {
        LifecycleEventObserver callback = lifecycleEventObserver(new InterfaceImpl3());
        assertThat(callback, instanceOf(CompositeGeneratedAdaptersObserver.class));
    }

    @Test
    public void testDerivedSequence() {
        LifecycleEventObserver callback2 = lifecycleEventObserver(new DerivedSequence2());
        assertThat(callback2, instanceOf(ReflectiveGenericLifecycleObserver.class));
        LifecycleEventObserver callback1 = lifecycleEventObserver(new DerivedSequence1());
        assertThat(callback1, instanceOf(SingleGeneratedAdapterObserver.class));
    }

    // MUST BE HERE TILL Lifecycle 3.0.0 release for back-compatibility with other modules
    @SuppressWarnings("deprecation")
    @Test
    public void testDeprecatedGenericLifecycleObserver() {
        GenericLifecycleObserver genericLifecycleObserver = new GenericLifecycleObserver() {
            @Override
            public void onStateChanged(@NonNull LifecycleOwner source,
                    @NonNull Lifecycle.Event event) {
            }
        };
        LifecycleEventObserver observer = lifecycleEventObserver(genericLifecycleObserver);
        assertThat(observer, is(observer));
    }

    // MUST BE HERE TILL Lifecycle 3.0.0 release for back-compatibility with other modules
    @SuppressWarnings("deprecation")
    @Test
    public void testDeprecatedLifecyclingCallback() {
        GenericLifecycleObserver genericLifecycleObserver = new GenericLifecycleObserver() {
            @Override
            public void onStateChanged(@NonNull LifecycleOwner source,
                    @NonNull Lifecycle.Event event) {
            }
        };
        LifecycleEventObserver observer = Lifecycling.getCallback(genericLifecycleObserver);
        assertThat(observer, is(observer));
    }

    @Test
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

        LifecycleEventObserver callback = lifecycleEventObserver(
                new AnnotatedFullLifecycleObserver());
        // check that neither of these calls fail
        callback.onStateChanged(new DefaultLifecycleOwner(), Lifecycle.Event.ON_CREATE);
        callback.onStateChanged(new DefaultLifecycleOwner(), Lifecycle.Event.ON_START);
        callback.onStateChanged(new DefaultLifecycleOwner(), Lifecycle.Event.ON_RESUME);
        callback.onStateChanged(new DefaultLifecycleOwner(), Lifecycle.Event.ON_PAUSE);
        callback.onStateChanged(new DefaultLifecycleOwner(), Lifecycle.Event.ON_STOP);
        callback.onStateChanged(new DefaultLifecycleOwner(), Lifecycle.Event.ON_DESTROY);
    }

    @Test
    public void lifecycleEventObserverAndAnnotations() {
        class AnnotatedLifecycleEventObserver implements LifecycleEventObserver {
            @OnLifecycleEvent(ON_ANY)
            public void onAny() {
                throw new IllegalStateException("Annotations in FullLifecycleObserver "
                        + "must not be called");
            }

            @Override
            public void onStateChanged(@NonNull LifecycleOwner source,
                    @NonNull Lifecycle.Event event) {
            }
        }

        LifecycleEventObserver callback = lifecycleEventObserver(
                new AnnotatedLifecycleEventObserver());
        // check that neither of these calls fail
        callback.onStateChanged(new DefaultLifecycleOwner(), Lifecycle.Event.ON_CREATE);
        callback.onStateChanged(new DefaultLifecycleOwner(), Lifecycle.Event.ON_START);
        callback.onStateChanged(new DefaultLifecycleOwner(), Lifecycle.Event.ON_RESUME);
        callback.onStateChanged(new DefaultLifecycleOwner(), Lifecycle.Event.ON_PAUSE);
        callback.onStateChanged(new DefaultLifecycleOwner(), Lifecycle.Event.ON_STOP);
        callback.onStateChanged(new DefaultLifecycleOwner(), Lifecycle.Event.ON_DESTROY);
    }


    static class DefaultLifecycleOwner implements LifecycleOwner {
        @NonNull
        @Override
        public Lifecycle getLifecycle() {
            throw new UnsupportedOperationException("getLifecycle is not supported");
        }
    }
}
