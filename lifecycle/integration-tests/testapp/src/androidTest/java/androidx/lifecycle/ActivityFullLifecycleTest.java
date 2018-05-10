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

import static androidx.lifecycle.TestUtils.OrderedTuples.CREATE;
import static androidx.lifecycle.TestUtils.OrderedTuples.DESTROY;
import static androidx.lifecycle.TestUtils.OrderedTuples.PAUSE;
import static androidx.lifecycle.TestUtils.OrderedTuples.RESUME;
import static androidx.lifecycle.TestUtils.OrderedTuples.START;
import static androidx.lifecycle.TestUtils.OrderedTuples.STOP;
import static androidx.lifecycle.TestUtils.flatMap;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import android.app.Activity;
import android.support.test.filters.LargeTest;
import android.support.test.rule.ActivityTestRule;

import androidx.core.util.Pair;
import androidx.lifecycle.Lifecycle.Event;
import androidx.lifecycle.testapp.CollectingLifecycleOwner;
import androidx.lifecycle.testapp.CollectingSupportActivity;
import androidx.lifecycle.testapp.FrameworkLifecycleRegistryActivity;
import androidx.lifecycle.testapp.TestEvent;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.List;

@LargeTest
@RunWith(Parameterized.class)
public class ActivityFullLifecycleTest {
    @Rule
    public final ActivityTestRule<? extends CollectingLifecycleOwner> activityTestRule;

    @Parameterized.Parameters
    public static Class[] params() {
        return new Class[]{CollectingSupportActivity.class,
                FrameworkLifecycleRegistryActivity.class};
    }

    public ActivityFullLifecycleTest(Class<? extends Activity> activityClass) {
        //noinspection unchecked
        activityTestRule = new ActivityTestRule(activityClass);
    }


    @Test
    public void testFullLifecycle() throws Throwable {
        CollectingLifecycleOwner owner = activityTestRule.getActivity();
        TestUtils.waitTillResumed(owner, activityTestRule);
        activityTestRule.finishActivity();

        TestUtils.waitTillDestroyed(owner, activityTestRule);
        List<Pair<TestEvent, Event>> results = owner.copyCollectedEvents();
        assertThat(results, is(flatMap(CREATE, START, RESUME, PAUSE, STOP, DESTROY)));
    }
}
