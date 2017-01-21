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

import android.support.test.rule.ActivityTestRule;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;

import com.android.support.lifecycle.testapp.MainActivity;
import com.android.support.lifecycle.testapp.R;

import org.junit.Rule;
import org.junit.runners.Parameterized;

import java.util.Arrays;

abstract class BaseStateProviderTest<T> {

    enum TestVariant {
        ACTIVITY,
        FRAGMENT
    }

    @Parameterized.Parameters
    public static Iterable<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {TestVariant.ACTIVITY},
                {TestVariant.FRAGMENT}
        });
    }

    @Rule
    public ActivityTestRule<MainActivity> activityTestRule =
            new ActivityTestRule<>(MainActivity.class);

    @SuppressWarnings("WeakerAccess")
    @Parameterized.Parameter
    public TestVariant mTestVariant;

    private boolean mForceRecreation;

    BaseStateProviderTest(boolean mForceRecreation) {
        this.mForceRecreation = mForceRecreation;
    }

    private MainActivity getActivity() {
        return activityTestRule.getActivity();
    }

    protected abstract T getStateProvider(MainActivity activity);

    private void stopRetainingInstances(MainActivity activity) {
        FragmentManager fragmentManager;
        if (mTestVariant == TestVariant.FRAGMENT) {
            Fragment fragment = activity.getSupportFragmentManager()
                    .findFragmentById(R.id.main_fragment);
            fragmentManager = fragment.getChildFragmentManager();
        } else {
            fragmentManager = activity.getSupportFragmentManager();
        }
        fragmentManager.findFragmentByTag(StateProviders.HOLDER_TAG).setRetainInstance(false);
    }

    @SafeVarargs
    final void testRecreation(Action<T>... actions) throws Throwable {
        MainActivity currentActivity = getActivity();
        for (int i = 0; i < actions.length; i++) {
            final Action<T> action = actions[i];
            final MainActivity activity = currentActivity;
            activityTestRule.runOnUiThread(() -> {
                action.run(getStateProvider(activity));
                if (mForceRecreation) {
                    stopRetainingInstances(activity);
                }
            });

            if (i != actions.length - 1) {
                currentActivity = recreateActivity(currentActivity, activityTestRule);
            }
        }
    }

    interface Action<X> {
        void run(X provider);
    }
}
