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

package android.support.v4.app;

import static org.junit.Assert.assertTrue;

import android.support.test.filters.MediumTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.support.v4.app.test.NonConfigOnStopActivity;

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
     * state when restored.
     */
    @Test
    public void nonConfigStop() throws Throwable {
        FragmentActivity activity = FragmentTestUtil.recreateActivity(mActivityRule,
                mActivityRule.getActivity());

        // A fragment was added in onStop(), but we shouldn't see it here...
        assertTrue(activity.getSupportFragmentManager().getFragments().isEmpty());
    }
}
