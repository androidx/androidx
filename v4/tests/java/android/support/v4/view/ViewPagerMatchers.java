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

package android.support.v4.view;

import java.lang.String;

import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.support.annotation.ColorInt;
import android.support.test.espresso.matcher.BoundedMatcher;
import android.support.v4.testutils.TestUtils;
import android.support.v4.view.ViewPager;
import android.view.View;

import junit.framework.Assert;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

public class ViewPagerMatchers {
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
            Assert.fail("Passed null Class instance");
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
}
