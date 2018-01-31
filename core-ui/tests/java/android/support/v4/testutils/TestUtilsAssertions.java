/*
 * Copyright (C) 2015 The Android Open Source Project
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

package android.support.v4.testutils;

import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;

import android.support.test.espresso.NoMatchingViewException;
import android.support.test.espresso.ViewAssertion;
import android.view.View;
import android.view.ViewGroup;

public class TestUtilsAssertions {

    /**
     * Returns an assertion that asserts that there is specified number of fully displayed
     * children.
     */
    public static ViewAssertion hasDisplayedChildren(final int expectedCount) {
        return new ViewAssertion() {
            @Override
            public void check(final View foundView, NoMatchingViewException noViewException) {
                if (noViewException != null) {
                    throw noViewException;
                } else {
                    if (!(foundView instanceof ViewGroup)) {
                        throw new AssertionError("View "
                                + foundView.getClass().getSimpleName() + " is not a ViewGroup");
                    }
                    final ViewGroup foundGroup = (ViewGroup) foundView;

                    final int childrenCount = foundGroup.getChildCount();
                    int childrenDisplayedCount = 0;
                    for (int i = 0; i < childrenCount; i++) {
                        if (isDisplayed().matches(foundGroup.getChildAt(i))) {
                            childrenDisplayedCount++;
                        }
                    }

                    if (childrenDisplayedCount != expectedCount) {
                        throw new AssertionError("Expected " + expectedCount
                                + " displayed children, but found " + childrenDisplayedCount);
                    }
                }
            }
        };
    }
}