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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import static java.util.Arrays.asList;

import android.arch.lifecycle.activity.EmptyActivity;
import android.arch.lifecycle.extensions.test.R;
import android.support.test.annotation.UiThreadTest;
import android.support.test.filters.MediumTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.support.v4.app.FragmentManager;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;

@MediumTest
@RunWith(AndroidJUnit4.class)
public class FragmentOperationsLifecycleTest {

    @Rule
    public ActivityTestRule<EmptyActivity> mActivityTestRule = new ActivityTestRule<>(
            EmptyActivity.class);

    @Test
    @UiThreadTest
    public void addRemoveFragment() {
        EmptyActivity activity = mActivityTestRule.getActivity();
        LifecycleFragment fragment = new LifecycleFragment();
        FragmentManager fm = activity.getSupportFragmentManager();
        fm.beginTransaction().add(fragment, "tag").commitNow();
        CollectingObserver observer = observeAndCollectIn(fragment);
        assertThat(observer.getEventsAndReset(), is(asList(ON_CREATE, ON_START, ON_RESUME)));
        fm.beginTransaction().remove(fragment).commitNow();
        assertThat(observer.getEventsAndReset(), is(asList(ON_PAUSE, ON_STOP, ON_DESTROY)));
        fm.beginTransaction().add(fragment, "tag").commitNow();
        assertThat(observer.getEventsAndReset(), is(asList(ON_CREATE, ON_START, ON_RESUME)));
    }

    @Test
    @UiThreadTest
    public void fragmentInBackstack() {
        EmptyActivity activity = mActivityTestRule.getActivity();
        LifecycleFragment fragment1 = new LifecycleFragment();
        FragmentManager fm = activity.getSupportFragmentManager();
        fm.beginTransaction().add(R.id.fragment_container, fragment1, "tag").addToBackStack(null)
                .commit();
        fm.executePendingTransactions();
        CollectingObserver observer1 = observeAndCollectIn(fragment1);
        assertThat(observer1.getEventsAndReset(), is(asList(ON_CREATE, ON_START, ON_RESUME)));

        LifecycleFragment fragment2 = new LifecycleFragment();
        fm.beginTransaction().replace(R.id.fragment_container, fragment2).addToBackStack(null)
                .commit();
        fm.executePendingTransactions();

        CollectingObserver observer2 = observeAndCollectIn(fragment2);
        assertThat(observer1.getEventsAndReset(), is(asList(ON_PAUSE, ON_STOP)));
        assertThat(observer2.getEventsAndReset(), is(asList(ON_CREATE, ON_START, ON_RESUME)));

        assertThat(fm.popBackStackImmediate(), is(true));
        assertThat(observer1.getEventsAndReset(), is(asList(ON_START, ON_RESUME)));
        assertThat(observer2.getEventsAndReset(), is(asList(ON_PAUSE, ON_STOP, ON_DESTROY)));

        assertThat(fm.popBackStackImmediate(), is(true));
        assertThat(observer1.getEventsAndReset(), is(asList(ON_PAUSE, ON_STOP, ON_DESTROY)));
    }

    private static CollectingObserver observeAndCollectIn(LifecycleFragment fragment) {
        CollectingObserver observer = new CollectingObserver();
        fragment.getLifecycle().addObserver(observer);
        return observer;
    }

    private static class CollectingObserver implements LifecycleObserver {
        final List<Lifecycle.Event> mCollectedEvents = new ArrayList<>();

        @OnLifecycleEvent(Lifecycle.Event.ON_ANY)
        public void anyEvent(LifecycleOwner owner, Lifecycle.Event event) {
            mCollectedEvents.add(event);
        }

        List<Lifecycle.Event> getEventsAndReset() {
            ArrayList<Lifecycle.Event> events = new ArrayList<>(mCollectedEvents);
            mCollectedEvents.clear();
            return events;
        }
    }
}
