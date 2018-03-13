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

import static org.junit.Assert.fail;

import android.graphics.drawable.Drawable;
import android.support.test.espresso.matcher.BoundedMatcher;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

import androidx.annotation.ColorInt;
import androidx.core.view.ViewCompat;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

import java.util.List;

public class TestUtilsMatchers {
    /**
     * Returns a matcher that matches views which have specific background color.
     */
    public static Matcher backgroundColor(@ColorInt final int backgroundColor) {
        return new BoundedMatcher<View, View>(View.class) {
            private String failedComparisonDescription;

            @Override
            public void describeTo(final Description description) {
                description.appendText("with background color: ");

                description.appendText(failedComparisonDescription);
            }

            @Override
            public boolean matchesSafely(final View view) {
                Drawable actualBackgroundDrawable = view.getBackground();
                if (actualBackgroundDrawable == null) {
                    return false;
                }

                // One option is to check if we have a ColorDrawable and then call getColor
                // but that API is v11+. Instead, we call our helper method that checks whether
                // all pixels in a Drawable are of the same specified color. Here we pass
                // hard-coded dimensions of 40x40 since we can't rely on the intrinsic dimensions
                // being set on our drawable.
                try {
                    TestUtils.assertAllPixelsOfColor("", actualBackgroundDrawable,
                            40, 40, backgroundColor, true);
                    // If we are here, the color comparison has passed.
                    failedComparisonDescription = null;
                    return true;
                } catch (Throwable t) {
                    // If we are here, the color comparison has failed.
                    failedComparisonDescription = t.getMessage();
                    return false;
                }
            }
        };
    }

    /**
     * Returns a matcher that matches Views which are an instance of the provided class.
     */
    public static Matcher<View> isOfClass(final Class<? extends View> clazz) {
        if (clazz == null) {
            fail("Passed null Class instance");
        }
        return new TypeSafeMatcher<View>() {
            @Override
            public void describeTo(Description description) {
                description.appendText("is identical to class: " + clazz);
            }

            @Override
            public boolean matchesSafely(View view) {
                return clazz.equals(view.getClass());
            }
        };
    }

    /**
     * Returns a matcher that matches Views that are aligned to the left / start edge of
     * their parent.
     */
    public static Matcher<View> startAlignedToParent() {
        return new BoundedMatcher<View, View>(View.class) {
            private String failedCheckDescription;

            @Override
            public void describeTo(final Description description) {
                description.appendText(failedCheckDescription);
            }

            @Override
            public boolean matchesSafely(final View view) {
                final ViewParent parent = view.getParent();
                if (!(parent instanceof ViewGroup)) {
                    return false;
                }
                final ViewGroup parentGroup = (ViewGroup) parent;

                final int parentLayoutDirection = ViewCompat.getLayoutDirection(parentGroup);
                if (parentLayoutDirection == ViewCompat.LAYOUT_DIRECTION_LTR) {
                    if (view.getLeft() == 0) {
                        return true;
                    } else {
                        failedCheckDescription =
                                "not aligned to start (left) edge of parent : left=" +
                                        view.getLeft();
                        return false;
                    }
                } else {
                    if (view.getRight() == parentGroup.getWidth()) {
                        return true;
                    } else {
                        failedCheckDescription =
                                "not aligned to start (right) edge of parent : right=" +
                                        view.getRight() + ", parent width=" +
                                        parentGroup.getWidth();
                        return false;
                    }
                }
            }
        };
    }

    /**
     * Returns a matcher that matches Views that are aligned to the right / end edge of
     * their parent.
     */
    public static Matcher<View> endAlignedToParent() {
        return new BoundedMatcher<View, View>(View.class) {
            private String failedCheckDescription;

            @Override
            public void describeTo(final Description description) {
                description.appendText(failedCheckDescription);
            }

            @Override
            public boolean matchesSafely(final View view) {
                final ViewParent parent = view.getParent();
                if (!(parent instanceof ViewGroup)) {
                    return false;
                }
                final ViewGroup parentGroup = (ViewGroup) parent;

                final int parentLayoutDirection = ViewCompat.getLayoutDirection(parentGroup);
                if (parentLayoutDirection == ViewCompat.LAYOUT_DIRECTION_LTR) {
                    if (view.getRight() == parentGroup.getWidth()) {
                        return true;
                    } else {
                        failedCheckDescription =
                                "not aligned to end (right) edge of parent : right=" +
                                        view.getRight() + ", parent width=" +
                                        parentGroup.getWidth();
                        return false;
                    }
                } else {
                    if (view.getLeft() == 0) {
                        return true;
                    } else {
                        failedCheckDescription =
                                "not aligned to end (left) edge of parent : left=" +
                                        view.getLeft();
                        return false;
                    }
                }
            }
        };
    }

    /**
     * Returns a matcher that matches Views that are centered horizontally in their parent.
     */
    public static Matcher<View> centerAlignedInParent() {
        return new BoundedMatcher<View, View>(View.class) {
            private String failedCheckDescription;

            @Override
            public void describeTo(final Description description) {
                description.appendText(failedCheckDescription);
            }

            @Override
            public boolean matchesSafely(final View view) {
                final ViewParent parent = view.getParent();
                if (!(parent instanceof ViewGroup)) {
                    return false;
                }
                final ViewGroup parentGroup = (ViewGroup) parent;

                final int viewLeft = view.getLeft();
                final int viewRight = view.getRight();
                final int parentWidth = parentGroup.getWidth();

                final int viewMiddle = (viewLeft + viewRight) / 2;
                final int parentMiddle = parentWidth / 2;

                // Check that the view is centered in its parent, accounting for off-by-one
                // pixel difference in case one is even and the other is odd.
                if (Math.abs(viewMiddle - parentMiddle) > 1) {
                    failedCheckDescription =
                            "not aligned to center of parent : own span=[" +
                                    viewLeft + "-" + viewRight + "], parent width=" + parentWidth;
                    return false;
                }

                return true;
            }
        };
    }

    /**
     * Returns a matcher that matches lists of integer values that match the specified sequence
     * of values.
     */
    public static Matcher<List<Integer>> matches(final int ... expectedValues) {
        return new TypeSafeMatcher<List<Integer>>() {
            private String mFailedDescription;

            @Override
            public void describeTo(Description description) {
                description.appendText(mFailedDescription);
            }

            @Override
            protected boolean matchesSafely(List<Integer> item) {
                int actualCount = item.size();
                int expectedCount = expectedValues.length;

                if (actualCount != expectedCount) {
                    mFailedDescription = "Expected " + expectedCount + " values, but got " +
                            actualCount;
                    return false;
                }

                for (int i = 0; i < expectedCount; i++) {
                    int curr = item.get(i);

                    if (curr != expectedValues[i]) {
                        mFailedDescription = "At #" + i + " got " + curr + " but should be " +
                                expectedValues[i];
                        return false;
                    }
                }

                return true;
            }
        };
    }

}
