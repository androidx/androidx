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

package android.arch.lifecycle;

import static android.arch.lifecycle.testapp.TestEvent.ACTIVITY_CALLBACK;
import static android.arch.lifecycle.testapp.TestEvent.LIFECYCLE_EVENT;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import android.app.Activity;
import android.support.test.filters.SmallTest;
import android.support.test.rule.ActivityTestRule;
import android.util.Pair;

import android.arch.lifecycle.testapp.CollectingActivity;
import android.arch.lifecycle.testapp.FrameworkLifecycleRegistryActivity;
import android.arch.lifecycle.testapp.FullLifecycleTestActivity;
import android.arch.lifecycle.testapp.SupportLifecycleRegistryActivity;
import android.arch.lifecycle.testapp.TestEvent;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.List;

@SmallTest
@RunWith(Parameterized.class)
public class ActivityFullLifecycleTest {
    @Rule
    public ActivityTestRule activityTestRule =
            new ActivityTestRule<>(FullLifecycleTestActivity.class);

    @Parameterized.Parameters
    public static Class[] params() {
        return new Class[]{FullLifecycleTestActivity.class,
                SupportLifecycleRegistryActivity.class,
                FrameworkLifecycleRegistryActivity.class};
    }

    public ActivityFullLifecycleTest(Class<? extends Activity> activityClass) {
        //noinspection unchecked
        activityTestRule = new ActivityTestRule(activityClass);
    }


    @Test
    public void testFullLifecycle() throws InterruptedException {
        Activity activity = activityTestRule.getActivity();
        List<Pair<TestEvent, Integer>> results = ((CollectingActivity) activity)
                .waitForCollectedEvents();

        int[] expectedEvents =
                new int[]{Lifecycle.ON_CREATE, Lifecycle.ON_START, Lifecycle.ON_RESUME,
                        Lifecycle.ON_PAUSE, Lifecycle.ON_STOP, Lifecycle.ON_DESTROY};

        List<Pair<TestEvent, Integer>> expected = new ArrayList<>();
        for (int i : expectedEvents) {
            if (i <= Lifecycle.ON_RESUME) {
                expected.add(new Pair<>(ACTIVITY_CALLBACK, i));
                expected.add(new Pair<>(LIFECYCLE_EVENT, i));
            } else {
                expected.add(new Pair<>(LIFECYCLE_EVENT, i));
                expected.add(new Pair<>(ACTIVITY_CALLBACK, i));
            }
        }
        assertThat(results, is(expected));
    }
}
