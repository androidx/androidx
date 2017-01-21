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

package com.android.support.lifecycle;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertTrue;

import android.support.test.filters.LargeTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.util.Pair;

import com.android.support.lifecycle.testapp.SimpleAppLifecycleTestActivity;
import com.android.support.lifecycle.testapp.SimpleAppLifecycleTestActivity.TestEventType;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class SimpleAppFullLifecycleTest {

    @SuppressWarnings("unchecked")
    private static final Pair[] EXPECTED_EVENTS =
            new Pair[] {
                new Pair(TestEventType.PROCESS_EVENT, Lifecycle.ON_CREATE),
                new Pair(TestEventType.ACTIVITY_EVENT, Lifecycle.ON_CREATE),
                new Pair(TestEventType.PROCESS_EVENT, Lifecycle.ON_START),
                new Pair(TestEventType.ACTIVITY_EVENT, Lifecycle.ON_START),
                new Pair(TestEventType.PROCESS_EVENT, Lifecycle.ON_RESUME),
                new Pair(TestEventType.ACTIVITY_EVENT, Lifecycle.ON_RESUME),

                new Pair(TestEventType.ACTIVITY_EVENT, Lifecycle.ON_PAUSE),
                new Pair(TestEventType.ACTIVITY_EVENT, Lifecycle.ON_STOP),
                new Pair(TestEventType.ACTIVITY_EVENT, Lifecycle.ON_DESTROY),

                new Pair(TestEventType.PROCESS_EVENT, Lifecycle.ON_PAUSE),
                new Pair(TestEventType.PROCESS_EVENT, Lifecycle.ON_STOP),
                new Pair(TestEventType.PROCESS_EVENT, Lifecycle.ON_DESTROY),
            };

    @Rule
    public ActivityTestRule<SimpleAppLifecycleTestActivity> activityTestRule =
            new ActivityTestRule<>(SimpleAppLifecycleTestActivity.class, false, false);

    @Before
    public void setup() {
        // cool down period, so application state will become DESTROYED
        try {
            Thread.sleep(ProcessProvider.TIMEOUT_MS * 2);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        SimpleAppLifecycleTestActivity.startProcessObserver();
    }

    @After
    public void tearDown() {
        SimpleAppLifecycleTestActivity.stopProcessObserver();
    }

    @Test
    public void testFullLifecycle() throws InterruptedException {
        int currentState = ProcessProvider.get().getLifecycle().getCurrentState();
        assertTrue("Unexpected State  " + currentState,
                currentState == Lifecycle.INITIALIZED || currentState == Lifecycle.DESTROYED);
        activityTestRule.launchActivity(null);
        List<Pair<TestEventType, Integer>> events = SimpleAppLifecycleTestActivity.awaitForEvents();
        assertThat("Failed to await for events", events, notNullValue());
        assertThat(events != null ? events.toArray() : null, is(EXPECTED_EVENTS));
    }

}
