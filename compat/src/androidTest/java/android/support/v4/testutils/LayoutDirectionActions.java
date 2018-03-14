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

import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;

import android.support.test.espresso.UiController;
import android.support.test.espresso.ViewAction;
import android.view.View;

import androidx.core.view.ViewCompat;

import org.hamcrest.Matcher;

public class LayoutDirectionActions {
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
}
