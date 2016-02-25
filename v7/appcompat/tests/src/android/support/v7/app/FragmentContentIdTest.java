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

package android.support.v7.app;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.support.v7.appcompat.test.R;
import android.test.suitebuilder.annotation.SmallTest;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.assertion.ViewAssertions.doesNotExist;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;

import org.junit.Test;

public class FragmentContentIdTest extends BaseInstrumentationTestCase<FragmentContentIdActivity> {

    public FragmentContentIdTest() {
        super(FragmentContentIdActivity.class);
    }

    @SmallTest
    @Test
    public void testFragmentAddedToAndroidContentIdCanBeRemoved() {
        getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                getActivity().replaceWithFragmentB();
            }
        });

        // Ensure that fragment_a has been removed from the view hierarchy
        onView(withId(R.id.fragment_a)).check(doesNotExist());
        // And that fragment_b is displayed
        onView(withId(R.id.fragment_b)).check(matches(isDisplayed()));
    }

}
