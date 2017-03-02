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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;

import android.support.test.filters.SmallTest;
import android.support.v4.app.FragmentManager;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@RunWith(JUnit4.class)
@SmallTest
public class FragmentCallbackTest {
    private final FragmentLifecycleDispatcher.FragmentCallback mCallback =
            new FragmentLifecycleDispatcher.FragmentCallback();
    private final FragmentManager mFragmentManager = mock(FragmentManager.class);
    private final LifecycleFragment mFragment = new LifecycleFragment();
    private final List<Integer> mRecordedEvents = new ArrayList<>();

    public FragmentCallbackTest() {
        mFragment.getLifecycle().addObserver(new LifecycleObserver() {
            @OnLifecycleEvent(Lifecycle.ANY)
            public void onEvent(LifecycleProvider provider, @Lifecycle.Event int event) {
                assertThat(provider, is((LifecycleProvider) mFragment));
                mRecordedEvents.add(event);
            }
        });
    }

    private void assertEvents(Integer... events) {
        assertThat(mRecordedEvents, is(Arrays.asList(events)));
    }

    @Test
    public void testCreate() {
        mCallback.onFragmentCreated(mFragmentManager, mFragment, null);
        assertEvents(Lifecycle.ON_CREATE);
    }

    @Test
    public void testStart() {
        mCallback.onFragmentStarted(mFragmentManager, mFragment);
        assertEvents(Lifecycle.ON_START);
    }

    @Test
    public void testResume() {
        mCallback.onFragmentResumed(mFragmentManager, mFragment);
        assertEvents(Lifecycle.ON_RESUME);
    }

    @Test
    public void testPause() {
        mCallback.onFragmentPaused(mFragmentManager, mFragment);
        assertEvents(Lifecycle.ON_PAUSE);
    }

    @Test
    public void testStop() {
        mCallback.onFragmentStopped(mFragmentManager, mFragment);
        assertEvents(Lifecycle.ON_STOP);
    }

    @Test
    public void testDestroy() {
        mCallback.onFragmentDestroyed(mFragmentManager, mFragment);
        assertEvents(Lifecycle.ON_DESTROY);
    }
}
