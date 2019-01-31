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

package androidx.appcompat.testutils;

import static androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayingAtLeast;

import static org.hamcrest.core.AllOf.allOf;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.content.res.ColorStateList;
import android.graphics.PorterDuff;
import android.view.View;
import android.widget.TextView;

import androidx.core.view.ViewCompat;
import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;

import org.hamcrest.Matcher;

public class TestUtilsActions {
    /**
     * Sets layout direction on the view.
     */
    public static ViewAction setLayoutDirection(final int layoutDirection) {
        return new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return isDisplayed();
            }

            @Override
            public String getDescription() {
                return "set layout direction";
            }

            @Override
            public void perform(UiController uiController, View view) {
                uiController.loopMainThreadUntilIdle();

                ViewCompat.setLayoutDirection(view, layoutDirection);

                uiController.loopMainThreadUntilIdle();
            }
        };
    }

    /**
     * Sets text appearance on {@code TextView}.
     */
    public static ViewAction setTextAppearance(final int resId) {
        return new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return allOf(isDisplayingAtLeast(90), isAssignableFrom(TextView.class));
            }

            @Override
            public String getDescription() {
                return "TextView set text appearance";
            }

            @Override
            public void perform(UiController uiController, View view) {
                uiController.loopMainThreadUntilIdle();

                TextView textView = (TextView) view;
                textView.setTextAppearance(textView.getContext(), resId);

                uiController.loopMainThreadUntilIdle();
            }
        };
    }

    /**
     * Sets the passed color state list as the background layer on a {@link View} with
     * {@link ViewCompat#setBackgroundTintList(View, ColorStateList)} API.
     */
    public static ViewAction setBackgroundTintListViewCompat(final ColorStateList colorStateList) {
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

                ViewCompat.setBackgroundTintList(view, colorStateList);

                uiController.loopMainThreadUntilIdle();
            }
        };
    }

    /**
     * Sets the passed mode as the background tint mode on a {@link View} with
     * {@link ViewCompat#setBackgroundTintMode(View, PorterDuff.Mode)} API.
     */
    public static ViewAction setBackgroundTintModeViewCompat(final PorterDuff.Mode tintMode) {
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

                ViewCompat.setBackgroundTintMode(view, tintMode);

                uiController.loopMainThreadUntilIdle();
            }
        };
    }

    public static ViewAction setEnabled(final boolean enabled) {
        return new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return isDisplayed();
            }

            @Override
            public String getDescription() {
                return "set enabled";
            }

            @Override
            public void perform(UiController uiController, View view) {
                uiController.loopMainThreadUntilIdle();

                view.setEnabled(enabled);

                uiController.loopMainThreadUntilIdle();
            }
        };
    }

    public static ViewAction setSystemUiVisibility(final int sysUiVisibility) {
        return new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return isDisplayed();
            }

            @Override
            public String getDescription() {
                return "Sets system ui visibility";
            }

            @Override
            public void perform(UiController uiController, View view) {
                uiController.loopMainThreadUntilIdle();

                view.setSystemUiVisibility(sysUiVisibility);

                uiController.loopMainThreadUntilIdle();
            }
        };
    }

    public static ViewAction rotateScreenOrientation(final Activity activity) {
        return new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return isDisplayed();
            }

            @Override
            public String getDescription() {
                return "Rotating screen orientation";
            }

            @Override
            public void perform(UiController uiController, View view) {
                uiController.loopMainThreadUntilIdle();

                switch (activity.getRequestedOrientation()) {
                    case ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE:
                        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                        break;
                    default:
                        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                        break;
                }

                uiController.loopMainThreadUntilIdle();
            }
        };
    }
}
