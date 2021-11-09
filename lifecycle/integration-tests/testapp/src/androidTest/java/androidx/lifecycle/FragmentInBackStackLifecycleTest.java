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


import static androidx.lifecycle.Lifecycle.Event.ON_CREATE;
import static androidx.lifecycle.Lifecycle.Event.ON_DESTROY;
import static androidx.lifecycle.Lifecycle.Event.ON_PAUSE;
import static androidx.lifecycle.Lifecycle.Event.ON_RESUME;
import static androidx.lifecycle.Lifecycle.Event.ON_START;
import static androidx.lifecycle.Lifecycle.Event.ON_STOP;
import static androidx.lifecycle.TestUtils.recreateActivity;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import static java.util.Arrays.asList;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Lifecycle.Event;
import androidx.lifecycle.testapp.EmptyActivity;
import androidx.lifecycle.testapp.R;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.MediumTest;
import androidx.test.rule.ActivityTestRule;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;

@MediumTest
@RunWith(AndroidJUnit4.class)
public class FragmentInBackStackLifecycleTest {
    @Rule
    public ActivityTestRule<EmptyActivity> activityTestRule = new ActivityTestRule<>(
            EmptyActivity.class);

    @SuppressWarnings("ArraysAsListWithZeroOrOneArgument")
    @Test
    public void test() throws Throwable {
        final ArrayList<Event> collectedEvents = new ArrayList<>();
        LifecycleObserver collectingObserver = new LifecycleObserver() {
            @OnLifecycleEvent(Event.ON_ANY)
            void onAny(@SuppressWarnings("unused") LifecycleOwner owner, Event event) {
                collectedEvents.add(event);
            }
        };
        final FragmentActivity activity = activityTestRule.getActivity();
        activityTestRule.runOnUiThread(() -> {
            FragmentManager fm = activity.getSupportFragmentManager();
            Fragment fragment = new Fragment();
            fm.beginTransaction().add(R.id.fragment_container, fragment, "tag").addToBackStack(null)
                    .commit();
            fm.executePendingTransactions();

            fragment.getLifecycle().addObserver(collectingObserver);
            Fragment fragment2 = new Fragment();
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
            Fragment fragment = fm.findFragmentByTag("tag");
            fragment.getLifecycle().addObserver(collectingObserver);
            assertThat(collectedEvents, is(asList(ON_CREATE)));
            collectedEvents.clear();
            fm.popBackStackImmediate();
            assertThat(collectedEvents, is(asList(ON_START, ON_RESUME)));
        });
    }


}
