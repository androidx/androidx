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

package com.android.support.lifecycle.state;

import static com.android.support.lifecycle.TestUtils.recreateActivity;
import static com.android.support.lifecycle.state.StateProviders.savedStateProvider;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import android.support.test.filters.MediumTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;

import com.android.support.lifecycle.testapp.MainActivity;
import com.android.support.lifecycle.testapp.R;
import com.android.support.lifecycle.testapp.UsualFragment;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@MediumTest
@RunWith(AndroidJUnit4.class)
public class FragmentStatesTests {

    @Rule
    public ActivityTestRule<MainActivity> activityTestRule =
            new ActivityTestRule<>(MainActivity.class);

    @Test
    public void testFragmentSavedStateInBackStack() throws Throwable {
        testRecreationInBackStack(true,
                fragment -> savedStateProvider(fragment).intStateValue("key", 261),
                fragment -> {
                    IntStateValue intValue = savedStateProvider(fragment).intStateValue("key");
                    assertThat(intValue.get(), is(261));
                });
    }

    private void testRecreationInBackStack(boolean forceRecreation,
            Action init, Action checkAfterRecreation) throws Throwable {
        String tag = "fragment_tag";
        MainActivity activity = activityTestRule.getActivity();
        activityTestRule.runOnUiThread(() -> {
            FragmentManager fragmentManager = activity.getSupportFragmentManager();
            UsualFragment fragment = new UsualFragment();
            fragmentManager.beginTransaction().add(R.id.root, fragment, tag).commitNow();
            init.run(fragment);
            if (forceRecreation) {
                fragment.getChildFragmentManager()
                        .findFragmentByTag(StateProviders.HOLDER_TAG).setRetainInstance(false);
            }

            Fragment newFragment = new UsualFragment();
            fragmentManager.beginTransaction()
                    .replace(R.id.root, newFragment)
                    .addToBackStack(null)
                    .commit();
            fragmentManager.executePendingTransactions();
        });

        final MainActivity newActivity = recreateActivity(activity, activityTestRule);
        activityTestRule.runOnUiThread(() -> {
            FragmentManager fragmentManager = newActivity.getSupportFragmentManager();
            boolean popped = fragmentManager.popBackStackImmediate();
            assertThat(popped, is(true));
            checkAfterRecreation.run(fragmentManager.findFragmentByTag(tag));
        });
    }

    interface Action {
        void run(Fragment fragment);
    }

}
