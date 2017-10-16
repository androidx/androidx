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

package android.support.design.testutils;

import static android.support.test.espresso.matcher.ViewMatchers.isAssignableFrom;

import android.graphics.Typeface;
import android.support.design.R;
import android.support.design.widget.TextInputLayout;
import android.support.test.espresso.UiController;
import android.support.test.espresso.ViewAction;
import android.support.test.espresso.matcher.ViewMatchers;
import android.view.View;

import org.hamcrest.Matcher;

public class TextInputLayoutActions {

    public static ViewAction setErrorEnabled(final boolean enabled) {
        return new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return isAssignableFrom(TextInputLayout.class);
            }

            @Override
            public String getDescription() {
                return "Enables/disables the error";
            }

            @Override
            public void perform(UiController uiController, View view) {
                uiController.loopMainThreadUntilIdle();

                TextInputLayout layout = (TextInputLayout) view;
                layout.setErrorEnabled(enabled);

                uiController.loopMainThreadUntilIdle();
            }
        };
    }

    public static ViewAction setError(final CharSequence error) {
        return new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return isAssignableFrom(TextInputLayout.class);
            }

            @Override
            public String getDescription() {
                return "Sets the error";
            }

            @Override
            public void perform(UiController uiController, View view) {
                uiController.loopMainThreadUntilIdle();

                TextInputLayout layout = (TextInputLayout) view;
                layout.setError(error);

                uiController.loopMainThreadUntilIdle();
            }
        };
    }

    public static ViewAction setErrorTextAppearance(final int resId) {
        return new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return isAssignableFrom(TextInputLayout.class);
            }

            @Override
            public String getDescription() {
                return "Sets the error text appearance";
            }

            @Override
            public void perform(UiController uiController, View view) {
                uiController.loopMainThreadUntilIdle();

                TextInputLayout layout = (TextInputLayout) view;
                layout.setErrorTextAppearance(resId);

                uiController.loopMainThreadUntilIdle();
            }
        };
    }

    public static ViewAction setTypeface(final Typeface typeface) {
        return new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return isAssignableFrom(TextInputLayout.class);
            }

            @Override
            public String getDescription() {
                return "Sets the typeface";
            }

            @Override
            public void perform(UiController uiController, View view) {
                uiController.loopMainThreadUntilIdle();

                TextInputLayout layout = (TextInputLayout) view;
                layout.setTypeface(typeface);

                uiController.loopMainThreadUntilIdle();
            }
        };
    }

    public static ViewAction setPasswordVisibilityToggleEnabled(final boolean enabled) {
        return new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return isAssignableFrom(TextInputLayout.class);
            }

            @Override
            public String getDescription() {
                return "Sets the error";
            }

            @Override
            public void perform(UiController uiController, View view) {
                uiController.loopMainThreadUntilIdle();

                TextInputLayout layout = (TextInputLayout) view;
                layout.setPasswordVisibilityToggleEnabled(enabled);

                uiController.loopMainThreadUntilIdle();
            }
        };
    }

    public static ViewAction setCounterEnabled(final boolean enabled) {
        return new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return isAssignableFrom(TextInputLayout.class);
            }

            @Override
            public String getDescription() {
                return "Sets the counter enabled";
            }

            @Override
            public void perform(UiController uiController, View view) {
                uiController.loopMainThreadUntilIdle();

                TextInputLayout layout = (TextInputLayout) view;
                layout.setCounterEnabled(enabled);

                uiController.loopMainThreadUntilIdle();
            }
        };
    }

    public static ViewAction setCounterMaxLength(final int maxLength) {
        return new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return isAssignableFrom(TextInputLayout.class);
            }

            @Override
            public String getDescription() {
                return "Sets the counter max length";
            }

            @Override
            public void perform(UiController uiController, View view) {
                uiController.loopMainThreadUntilIdle();

                TextInputLayout layout = (TextInputLayout) view;
                layout.setCounterMaxLength(maxLength);

                uiController.loopMainThreadUntilIdle();
            }
        };
    }

    /**
     * Toggles password.
     */
    public static ViewAction clickPasswordToggle() {
        return new ViewAction() {

            @Override
            public Matcher<View> getConstraints() {
                return ViewMatchers.isAssignableFrom(TextInputLayout.class);
            }

            @Override
            public String getDescription() {
                return "Clicks the password toggle";
            }

            @Override
            public void perform(UiController uiController, View view) {
                TextInputLayout textInputLayout = (TextInputLayout) view;
                // Reach in and find the password toggle since we don't have a public API
                // to get a reference to it
                View passwordToggle = textInputLayout.findViewById(R.id.text_input_password_toggle);
                passwordToggle.performClick();
                uiController.loopMainThreadUntilIdle();
            }
        };
    }

}
