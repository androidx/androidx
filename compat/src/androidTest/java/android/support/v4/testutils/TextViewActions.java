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

package android.support.v4.testutils;

import static android.support.test.espresso.matcher.ViewMatchers.isAssignableFrom;

import android.graphics.drawable.Drawable;
import android.support.test.espresso.UiController;
import android.support.test.espresso.ViewAction;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.annotation.StyleRes;
import androidx.core.widget.TextViewCompat;

import org.hamcrest.Matcher;

public class TextViewActions {
    /**
     * Sets max lines count on <code>TextView</code>.
     */
    public static ViewAction setMaxLines(final int maxLines) {
        return new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return isAssignableFrom(TextView.class);
            }

            @Override
            public String getDescription() {
                return "TextView set max lines";
            }

            @Override
            public void perform(UiController uiController, View view) {
                uiController.loopMainThreadUntilIdle();

                TextView textView = (TextView) view;
                textView.setMaxLines(maxLines);

                uiController.loopMainThreadUntilIdle();
            }
        };
    }

    /**
     * Sets min lines count on <code>TextView</code>.
     */
    public static ViewAction setMinLines(final int minLines) {
        return new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return isAssignableFrom(TextView.class);
            }

            @Override
            public String getDescription() {
                return "TextView set min lines";
            }

            @Override
            public void perform(UiController uiController, View view) {
                uiController.loopMainThreadUntilIdle();

                TextView textView = (TextView) view;
                textView.setMinLines(minLines);

                uiController.loopMainThreadUntilIdle();
            }
        };
    }

    /**
     * Sets text content on <code>TextView</code>.
     */
    public static ViewAction setText(final @StringRes int stringResId) {
        return new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return isAssignableFrom(TextView.class);
            }

            @Override
            public String getDescription() {
                return "TextView set text";
            }

            @Override
            public void perform(UiController uiController, View view) {
                uiController.loopMainThreadUntilIdle();

                TextView textView = (TextView) view;
                textView.setText(stringResId);

                uiController.loopMainThreadUntilIdle();
            }
        };
    }

    /**
     * Sets text appearance on <code>TextView</code>.
     */
    public static ViewAction setTextAppearance(final @StyleRes int styleResId) {
        return new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return isAssignableFrom(TextView.class);
            }

            @Override
            public String getDescription() {
                return "TextView set text appearance";
            }

            @Override
            public void perform(UiController uiController, View view) {
                uiController.loopMainThreadUntilIdle();

                TextView textView = (TextView) view;
                TextViewCompat.setTextAppearance(textView, styleResId);

                uiController.loopMainThreadUntilIdle();
            }
        };
    }

    /**
     * Sets compound drawables on <code>TextView</code>.
     */
    public static ViewAction setCompoundDrawablesRelative(final @Nullable Drawable start,
            final @Nullable Drawable top, final @Nullable Drawable end,
            final @Nullable Drawable bottom) {
        return new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return isAssignableFrom(TextView.class);
            }

            @Override
            public String getDescription() {
                return "TextView set compound drawables";
            }

            @Override
            public void perform(UiController uiController, View view) {
                uiController.loopMainThreadUntilIdle();

                TextView textView = (TextView) view;
                TextViewCompat.setCompoundDrawablesRelative(textView, start, top, end, bottom);

                uiController.loopMainThreadUntilIdle();
            }
        };
    }

    /**
     * Sets compound drawables on <code>TextView</code>.
     */
    public static ViewAction setCompoundDrawablesRelativeWithIntrinsicBounds(
            final @Nullable Drawable start, final @Nullable Drawable top,
            final @Nullable Drawable end, final @Nullable Drawable bottom) {
        return new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return isAssignableFrom(TextView.class);
            }

            @Override
            public String getDescription() {
                return "TextView set compound drawables";
            }

            @Override
            public void perform(UiController uiController, View view) {
                uiController.loopMainThreadUntilIdle();

                TextView textView = (TextView) view;
                TextViewCompat.setCompoundDrawablesRelativeWithIntrinsicBounds(
                        textView, start, top, end, bottom);

                uiController.loopMainThreadUntilIdle();
            }
        };
    }

    /**
     * Sets compound drawables on <code>TextView</code>.
     */
    public static ViewAction setCompoundDrawablesRelativeWithIntrinsicBounds(
            final @DrawableRes int start, final @DrawableRes int top, final @DrawableRes int end,
            final @DrawableRes int bottom) {
        return new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return isAssignableFrom(TextView.class);
            }

            @Override
            public String getDescription() {
                return "TextView set compound drawables";
            }

            @Override
            public void perform(UiController uiController, View view) {
                uiController.loopMainThreadUntilIdle();

                TextView textView = (TextView) view;
                TextViewCompat.setCompoundDrawablesRelativeWithIntrinsicBounds(
                        textView, start, top, end, bottom);

                uiController.loopMainThreadUntilIdle();
            }
        };
    }
}
