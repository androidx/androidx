/*
 * Copyright 2023 The Android Open Source Project
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
package androidx.coordinatorlayout.testutils;

import static androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import static androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayingAtLeast;
import static androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;

import android.util.Log;
import android.view.View;
import android.view.ViewParent;

import androidx.core.widget.NestedScrollView;
import androidx.test.espresso.PerformException;
import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.espresso.util.HumanReadables;

import org.hamcrest.Matcher;
import org.hamcrest.Matchers;

/**
 * Supports scrolling with the NestedScrollView in Espresso.
 */
public final class NestedScrollViewActions {
    private static final String TAG = NestedScrollViewActions.class.getSimpleName();

    public static ViewAction scrollToTop() {
        return new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return Matchers.allOf(
                        isDescendantOfA(isAssignableFrom(NestedScrollView.class)),
                        withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)
                );
            }

            @Override
            public String getDescription() {
                return "Find the first NestedScrollView parent (of the matched view) and "
                        + "scroll to it.";
            }

            @Override
            public void perform(UiController uiController, View view) {
                if (isDisplayingAtLeast(90).matches(view)) {
                    Log.i(TAG, "View is already completely displayed at top. Returning.");
                    return;
                }
                try {
                    NestedScrollView nestedScrollView = findFirstNestedScrollViewParent(view);

                    if (nestedScrollView != null) {
                        nestedScrollView.scrollTo(0, view.getTop());
                    } else {
                        throw new Exception("Can not find NestedScrollView parent.");
                    }

                } catch (Exception exception) {
                    throw new PerformException.Builder()
                            .withActionDescription(this.getDescription())
                            .withViewDescription(HumanReadables.describe(view))
                            .withCause(exception)
                            .build();
                }
                uiController.loopMainThreadUntilIdle();

                if (!isDisplayingAtLeast(90).matches(view)) {
                    throw new PerformException.Builder()
                            .withActionDescription(this.getDescription())
                            .withViewDescription(HumanReadables.describe(view))
                            .withCause(
                                    new RuntimeException(
                                            "Scrolling to view was attempted, but the view is"
                                                    + "not displayed"))
                            .build();
                }
            }
        };
    }

    private static NestedScrollView findFirstNestedScrollViewParent(View view) {
        ViewParent viewParent = view.getParent();

        while (viewParent != null && !(viewParent.getClass() == NestedScrollView.class)) {
            viewParent = viewParent.getParent();
        }

        if (viewParent == null) {
            return null;
        } else {
            return (NestedScrollView) viewParent;
        }
    }
}
