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

import static com.android.support.lifecycle.testapp.FullLifecycleTestActivity.TestEvent
        .ACTIVITY_CALLBACK;
import static com.android.support.lifecycle.testapp.FullLifecycleTestActivity.TestEvent
        .LIFECYCLE_EVENT;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import android.support.test.filters.SmallTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.util.Pair;

import com.android.support.lifecycle.testapp.FullLifecycleTestActivity;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;

@SmallTest
@RunWith(AndroidJUnit4.class)
public class ActivityFullLifecycleTest {
    @Rule
    public ActivityTestRule<FullLifecycleTestActivity> activityTestRule =
            new ActivityTestRule<>(FullLifecycleTestActivity.class);

    @Test
    public void testFullLifecycle() throws InterruptedException {
        FullLifecycleTestActivity activity = activityTestRule.getActivity();
        List<Pair<FullLifecycleTestActivity.TestEvent, Integer>> results =
                activity.waitForCollectedEvents();

        int[] expectedEvents =
                new int[]{Lifecycle.ON_CREATE, Lifecycle.ON_START, Lifecycle.ON_RESUME,
                        Lifecycle.ON_PAUSE, Lifecycle.ON_STOP, Lifecycle.ON_DESTROY};

        List<Pair<FullLifecycleTestActivity.TestEvent, Integer>> expected = new ArrayList<>();
        for (int i : expectedEvents) {
            expected.add(new Pair<>(ACTIVITY_CALLBACK, i));
            expected.add(new Pair<>(LIFECYCLE_EVENT, i));
        }
        assertThat(results, is(expected));
    }
}
