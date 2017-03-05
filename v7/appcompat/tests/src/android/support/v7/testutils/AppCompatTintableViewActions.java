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

package android.support.v7.testutils;

import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;

import static org.hamcrest.core.AllOf.allOf;

import android.content.res.ColorStateList;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.support.annotation.DrawableRes;
import android.support.test.espresso.UiController;
import android.support.test.espresso.ViewAction;
import android.support.v4.view.ViewCompat;
import android.view.View;

import org.hamcrest.Matcher;

public class AppCompatTintableViewActions {
    /**
     * Sets the passed color state list as the background layer on a {@link View}.
     */
    public static ViewAction setBackgroundTintList(final ColorStateList tint) {
        return new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return isDisplayed();
            }

            @Override
            public String getDescription() {
                return "set background tint list";
            }

            @Override
            public void perform(UiController uiController, View view) {
                uiController.loopMainThreadUntilIdle();

                ViewCompat.setBackgroundTintList(view, tint);

                uiController.loopMainThreadUntilIdle();
            }
        };
    }

    /**
     * Sets the passed mode as the background tint mode on a <code>View</code>.
     */
    public static ViewAction setBackgroundTintMode(final PorterDuff.Mode mode) {
        return new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return isDisplayed();
            }

            @Override
            public String getDescription() {
                return "set background tint mode";
            }

            @Override
            public void perform(UiController uiController, View view) {
                uiController.loopMainThreadUntilIdle();

                ViewCompat.setBackgroundTintMode(view, mode);

                uiController.loopMainThreadUntilIdle();
            }
        };
    }

    /**
     * Sets background drawable on a <code>View</code> that implements the
     * <code>TintableBackgroundView</code> interface.
     */
    public static ViewAction setBackgroundDrawable(final Drawable background) {
        return new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return allOf(TestUtilsMatchers.isTintableBackgroundView());
            }

            @Override
            public String getDescription() {
                return "set background drawable";
            }

            @Override
            public void perform(UiController uiController, View view) {
                uiController.loopMainThreadUntilIdle();

                view.setBackgroundDrawable(background);

                uiController.loopMainThreadUntilIdle();
            }
        };
    }

    /**
     * Sets background resource on a <code>View</code> that implements the
     * <code>TintableBackgroundView</code> interface.
     */
    public static ViewAction setBackgroundResource(final @DrawableRes int resId) {
        return new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return allOf(TestUtilsMatchers.isTintableBackgroundView());
            }

            @Override
            public String getDescription() {
                return "set background resource";
            }

            @Override
            public void perform(UiController uiController, View view) {
                uiController.loopMainThreadUntilIdle();

                view.setBackgroundResource(resId);

                uiController.loopMainThreadUntilIdle();
            }
        };
    }

}
