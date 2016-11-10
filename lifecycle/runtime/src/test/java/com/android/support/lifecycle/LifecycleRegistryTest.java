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
import static com.android.support.lifecycle.Lifecycle.ON_PAUSE;
import static com.android.support.lifecycle.Lifecycle.ON_START;
import static com.android.support.lifecycle.Lifecycle.ON_STOP;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class LifecycleRegistryTest {
    private LifecycleProvider mLifecycleProvider;
    private Lifecycle mLifecycle;
    private LifecycleRegistry mRegistry;
    @Before
    public void init() {
        mLifecycleProvider = mock(LifecycleProvider.class);
        mLifecycle = mock(Lifecycle.class);
        when(mLifecycleProvider.getLifecycle()).thenReturn(mLifecycle);
        mRegistry = new LifecycleRegistry(mLifecycleProvider);
    }
    @Test
    public void addRemove() {
        LifecycleObserver observer = mock(LifecycleObserver.class);
        mRegistry.addObserver(observer);
        assertThat(mRegistry.size(), is(1));
        mRegistry.removeObserver(observer);
        assertThat(mRegistry.size(), is(0));
    }

    @Test
    public void addGenericAndObserve() {
        GenericLifecycleObserver generic = mock(GenericLifecycleObserver.class);
        mRegistry.addObserver(generic);
        dispatchEvent(ON_CREATE);
        verify(generic).onStateChanged(mLifecycleProvider, ON_CREATE);
        reset(generic);
        dispatchEvent(ON_CREATE);
        verify(generic, never()).onStateChanged(mLifecycleProvider, ON_CREATE);
    }

    @Test
    public void addRegularClass() {
        TestObserver testObserver = mock(TestObserver.class);
        mRegistry.addObserver(testObserver);
        dispatchEvent(ON_START);
        verify(testObserver, never()).onStopped();
        dispatchEvent(ON_STOP);
        verify(testObserver).onStopped();
    }

    @Test
    public void add2RemoveOne() {
        TestObserver observer1 = mock(TestObserver.class);
        TestObserver observer2 = mock(TestObserver.class);
        TestObserver observer3 = mock(TestObserver.class);
        mRegistry.addObserver(observer1);
        mRegistry.addObserver(observer2);
        mRegistry.addObserver(observer3);

        dispatchEvent(ON_STOP);

        verify(observer1).onStopped();
        verify(observer2).onStopped();
        verify(observer3).onStopped();
        reset(observer1, observer2, observer3);

        mRegistry.removeObserver(observer2);
        dispatchEvent(ON_PAUSE);

        dispatchEvent(ON_STOP);
        verify(observer1).onStopped();
        verify(observer2, never()).onStopped();
        verify(observer3).onStopped();
    }

    @Test
    public void removeWhileTraversing() {
        final TestObserver observer2 = mock(TestObserver.class);
        TestObserver observer1 = spy(new TestObserver() {
            @Override
            public void onStopped() {
                mRegistry.removeObserver(observer2);
            }
        });
        mRegistry.addObserver(observer1);
        mRegistry.addObserver(observer2);
        dispatchEvent(ON_STOP);
        verify(observer2, never()).onStopped();
        verify(observer1).onStopped();
    }

    private void dispatchEvent(@Lifecycle.Event int event) {
        when(mLifecycle.getCurrentState()).thenReturn(LifecycleRegistry.getStateAfter(event));
        mRegistry.handleLifecycleEvent(event);
    }

    private interface TestObserver extends LifecycleObserver {
        @OnLifecycleEvent(ON_STOP)
        void onStopped();
    }
}
