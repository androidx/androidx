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
import static androidx.lifecycle.Lifecycle.Event.ON_RESUME;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.ArrayList;
import java.util.List;

@RunWith(JUnit4.class)
public class GeneratedAdaptersTest {

    private LifecycleOwner mOwner;
    @SuppressWarnings("FieldCanBeLocal")
    private Lifecycle mLifecycle;

    @Before
    public void initMocks() {
        mOwner = mock(LifecycleOwner.class);
        mLifecycle = mock(Lifecycle.class);
        when(mOwner.getLifecycle()).thenReturn(mLifecycle);
    }

    static class SimpleObserver implements LifecycleObserver {
        List<String> mLog;

        SimpleObserver(List<String> log) {
            mLog = log;
        }

        @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
        void onCreate() {
            mLog.add("onCreate");
        }
    }

    @Test
    public void testSimpleSingleGeneratedAdapter() {
        List<String>  actual = new ArrayList<>();
        GenericLifecycleObserver callback = Lifecycling.getCallback(new SimpleObserver(actual));
        callback.onStateChanged(mOwner, Lifecycle.Event.ON_CREATE);
        assertThat(callback, instanceOf(SingleGeneratedAdapterObserver.class));
        assertThat(actual, is(singletonList("onCreate")));
    }

    static class TestObserver implements LifecycleObserver {
        List<String> mLog;

        TestObserver(List<String> log) {
            mLog = log;
        }

        @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
        void onCreate() {
            mLog.add("onCreate");
        }

        @OnLifecycleEvent(ON_ANY)
        void onAny() {
            mLog.add("onAny");
        }
    }

    @Test
    public void testOnAny() {
        List<String>  actual = new ArrayList<>();
        GenericLifecycleObserver callback = Lifecycling.getCallback(new TestObserver(actual));
        callback.onStateChanged(mOwner, Lifecycle.Event.ON_CREATE);
        assertThat(callback, instanceOf(SingleGeneratedAdapterObserver.class));
        assertThat(actual, is(asList("onCreate", "onAny")));
    }

    interface OnPauses extends LifecycleObserver {
        @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        void onPause();

        @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        void onPause(LifecycleOwner owner);
    }

    interface OnPauseResume extends LifecycleObserver {
        @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        void onPause();

        @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
        void onResume();
    }

    class Impl1 implements OnPauses, OnPauseResume {

        List<String> mLog;

        Impl1(List<String> log) {
            mLog = log;
        }

        @Override
        public void onPause() {
            mLog.add("onPause_0");
        }

        @Override
        public void onResume() {
            mLog.add("onResume");
        }

        @Override
        public void onPause(LifecycleOwner owner) {
            mLog.add("onPause_1");
        }
    }

    @Test
    public void testClashingInterfaces() {
        List<String>  actual = new ArrayList<>();
        GenericLifecycleObserver callback = Lifecycling.getCallback(new Impl1(actual));
        callback.onStateChanged(mOwner, Lifecycle.Event.ON_PAUSE);
        assertThat(callback, instanceOf(CompositeGeneratedAdaptersObserver.class));
        assertThat(actual, is(asList("onPause_0", "onPause_1")));
        actual.clear();
        callback.onStateChanged(mOwner, Lifecycle.Event.ON_RESUME);
        assertThat(actual, is(singletonList("onResume")));
    }

    class Base implements LifecycleObserver {

        List<String> mLog;

        Base(List<String> log) {
            mLog = log;
        }

        @OnLifecycleEvent(ON_ANY)
        void onAny() {
            mLog.add("onAny_0");
        }

        @OnLifecycleEvent(ON_ANY)
        void onAny(LifecycleOwner owner) {
            mLog.add("onAny_1");
        }

        @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
        void onResume() {
            mLog.add("onResume");
        }
    }

    interface OnAny extends LifecycleObserver {
        @OnLifecycleEvent(ON_ANY)
        void onAny();

        @OnLifecycleEvent(ON_ANY)
        void onAny(LifecycleOwner owner, Lifecycle.Event event);
    }

    class Derived extends Base implements OnAny {
        Derived(List<String> log) {
            super(log);
        }

        @Override
        public void onAny() {
            super.onAny();
        }

        @Override
        public void onAny(LifecycleOwner owner, Lifecycle.Event event) {
            mLog.add("onAny_2");
            assertThat(event, is(ON_RESUME));
        }
    }

    @Test
    public void testClashingClassAndInterface() {
        List<String>  actual = new ArrayList<>();
        GenericLifecycleObserver callback = Lifecycling.getCallback(new Derived(actual));
        callback.onStateChanged(mOwner, Lifecycle.Event.ON_RESUME);
        assertThat(callback, instanceOf(CompositeGeneratedAdaptersObserver.class));
        assertThat(actual, is(asList("onResume", "onAny_0", "onAny_1", "onAny_2")));
    }

}
