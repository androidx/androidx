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

package androidx.appcompat.app;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.assertion.ViewAssertions.doesNotExist;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;

import android.support.test.filters.SmallTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import androidx.appcompat.test.R;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class FragmentContentIdTest {
    @Rule
    public final ActivityTestRule<FragmentContentIdActivity> mActivityTestRule =
            new ActivityTestRule<>(FragmentContentIdActivity.class);

    @SmallTest
    @Test
    public void testFragmentAddedToAndroidContentIdCanBeRemoved() throws Throwable {
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mActivityTestRule.getActivity().replaceWithFragmentB();
            }
        });

        // Ensure that fragment_a has been removed from the view hierarchy
        onView(withId(R.id.fragment_a)).check(doesNotExist());
        // And that fragment_b is displayed
        onView(withId(R.id.fragment_b)).check(matches(isDisplayed()));
    }
}
