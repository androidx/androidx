/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.fragment.app;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;

import android.os.Build;
import android.support.test.filters.MediumTest;
import android.support.test.filters.SdkSuppress;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import androidx.fragment.app.test.NonConfigOnStopActivity;
import androidx.testutils.FragmentActivityUtils;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@MediumTest
@RunWith(AndroidJUnit4.class)
public class FragmentManagerNonConfigTest {

    @Rule
    public ActivityTestRule<NonConfigOnStopActivity> mActivityRule =
            new ActivityTestRule<>(NonConfigOnStopActivity.class);

    /**
     * When a fragment is added during onStop(), it shouldn't show up in non-config
     * state when restored before P, because OnSaveInstanceState was already called.
     */
    @Test
    @SdkSuppress(maxSdkVersion = Build.VERSION_CODES.O_MR1)
    public void nonConfigStop() throws Throwable {
        FragmentActivity activity = FragmentActivityUtils.recreateActivity(mActivityRule,
                mActivityRule.getActivity());

        // A fragment was added in onStop(), but we shouldn't see it here...
        assertTrue(activity.getSupportFragmentManager().getFragments().isEmpty());
    }

    /**
     * When a fragment is added during onStop(), it shouldn't show up in non-config
     * state when restored after (>=) P, because OnSaveInstanceState isn't yet called.
     */
    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.P)
    public void nonConfigStopSavingFragment() throws Throwable {
        FragmentActivity activity = FragmentActivityUtils.recreateActivity(mActivityRule,
                mActivityRule.getActivity());

        assertThat(activity.getSupportFragmentManager().getFragments().size(), is(1));
    }
}
