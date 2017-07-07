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

import static android.support.v4.app.FragmentManager.FragmentLifecycleCallbacks;

import static com.android.support.lifecycle.testapp.LiveDataTestActivity.LIVE_DATA_VALUE;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import android.os.Bundle;
import android.support.test.filters.SmallTest;
import android.support.test.rule.ActivityTestRule;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;

import com.android.support.lifecycle.testapp.LiveDataTestActivity;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

@SmallTest
public class LiveDataTransactionTest {

    @Rule
    public ActivityTestRule<LiveDataTestActivity> mActivityTestRule =
            new ActivityTestRule<>(LiveDataTestActivity.class);

    private boolean mVisited;

    @Test
    public void transactionInOnStateChanged() throws Throwable {
        LiveDataTestActivity activity = mActivityTestRule.getActivity();

        activity.getSupportFragmentManager().registerFragmentLifecycleCallbacks(
                new FragmentLifecycleCallbacks() {
                    @Override
                    public void onFragmentCreated(FragmentManager fm, Fragment f,
                            Bundle savedInstanceState) {
                    }
                }, true);
        mActivityTestRule.runOnUiThread(() -> {
            assertThat(activity.fragmentsNumber,  /** 2^MAX_DEPTH - 1 */ is(31));
            activity.viewModel.liveData.observe(activity,
                    s -> Assert.fail("savedInstance state triggered an update"));
        });
        LiveDataTestActivity newActivity = TestUtils.recreateActivity(activity,
                mActivityTestRule);
        TestUtils.waitTillResumed(newActivity, mActivityTestRule);
        mActivityTestRule.runOnUiThread(() -> {
            newActivity.viewModel.liveData.observe(newActivity,
                    s -> {
                        assertThat(s, is(LIVE_DATA_VALUE));
                        mVisited = true;
                    });
            assertThat(newActivity.fragmentsNumber, /** 2 * (2^MAX_DEPTH - 1) + 1 */is(63));
            assertThat(mVisited, is(true));
        });
    }

}
