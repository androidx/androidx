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


import static android.arch.lifecycle.Lifecycle.Event.ON_CREATE;
import static android.arch.lifecycle.Lifecycle.Event.ON_DESTROY;
import static android.arch.lifecycle.Lifecycle.Event.ON_PAUSE;
import static android.arch.lifecycle.Lifecycle.Event.ON_RESUME;
import static android.arch.lifecycle.Lifecycle.Event.ON_START;
import static android.arch.lifecycle.Lifecycle.Event.ON_STOP;
import static android.arch.lifecycle.TestUtils.recreateActivity;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.iterableWithSize;

import static java.util.Arrays.asList;

import android.arch.lifecycle.Lifecycle.Event;
import android.arch.lifecycle.testapp.EmptyActivity;
import android.arch.lifecycle.testapp.R;
import android.support.test.filters.SmallTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;

@SmallTest
@RunWith(AndroidJUnit4.class)
public class FragmentInBackStackLifecycleTest {
    @Rule
    public ActivityTestRule<EmptyActivity> activityTestRule = new ActivityTestRule<>(
            EmptyActivity.class);

    @Test
    public void test() throws Throwable {
        final ArrayList<Event> collectedEvents = new ArrayList<>();
        LifecycleObserver collectingObserver = new LifecycleObserver() {
            @OnLifecycleEvent(Event.ON_ANY)
            void onAny(LifecycleOwner owner, Event event) {
                collectedEvents.add(event);
            }
        };
        final FragmentActivity activity = activityTestRule.getActivity();
        activityTestRule.runOnUiThread(() -> {
            FragmentManager fm = activity.getSupportFragmentManager();
            LifecycleFragment fragment = new LifecycleFragment();
            fm.beginTransaction().add(R.id.fragment_container, fragment, "tag").addToBackStack(null)
                    .commit();
            fm.executePendingTransactions();

            fragment.getLifecycle().addObserver(collectingObserver);
            LifecycleFragment fragment2 = new LifecycleFragment();
            fm.beginTransaction().replace(R.id.fragment_container, fragment2).addToBackStack(null)
                    .commit();
            fm.executePendingTransactions();
            assertThat(collectedEvents, is(asList(ON_CREATE, ON_START, ON_RESUME,
                    ON_PAUSE, ON_STOP)));
            collectedEvents.clear();
        });
        EmptyActivity newActivity = recreateActivity(activityTestRule.getActivity(),
                activityTestRule);

        assertThat(collectedEvents, is(asList(ON_DESTROY)));
        collectedEvents.clear();
        EmptyActivity lastActivity = recreateActivity(newActivity, activityTestRule);
        activityTestRule.runOnUiThread(() -> {
            FragmentManager fm = lastActivity.getSupportFragmentManager();
            LifecycleFragment fragment = (LifecycleFragment) fm.findFragmentByTag("tag");
            fragment.getLifecycle().addObserver(collectingObserver);
            assertThat(collectedEvents, iterableWithSize(0));
            fm.popBackStackImmediate();
            assertThat(collectedEvents, is(asList(ON_CREATE, ON_START, ON_RESUME)));
        });
    }


}
