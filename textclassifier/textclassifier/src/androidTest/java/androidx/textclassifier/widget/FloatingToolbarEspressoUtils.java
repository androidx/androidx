/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.textclassifier.widget;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.matcher.RootMatchers.isPlatformPopup;
import static androidx.test.espresso.matcher.RootMatchers.withDecorView;
import static androidx.test.espresso.matcher.ViewMatchers.hasDescendant;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withTagValue;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;

import android.view.View;

import androidx.test.espresso.ViewInteraction;
import androidx.textclassifier.R;

import org.hamcrest.Matcher;

/**
 * Espresso utils for interacting with the {@link FloatingToolbar}.
 */
public class FloatingToolbarEspressoUtils {

    private FloatingToolbarEspressoUtils() {}

    public static ViewInteraction onFloatingToolbar() {
        final Object tag = FloatingToolbar.FLOATING_TOOLBAR_TAG;
        return onView(withTagValue(is(tag)))
                .inRoot(allOf(
                        isPlatformPopup(),
                        withDecorView(hasDescendant(withTagValue(is(tag))))));
    }

    public static ViewInteraction onFloatingToolbarItem(CharSequence itemLabel) {
        return onFloatingToolbarComponent(withText(itemLabel.toString()));
    }

    public static ViewInteraction onFloatingToolbarOverflowButton() {
        return onFloatingToolbarComponent(withId(R.id.overflow));
    }

    public static ViewInteraction onFloatingToolbarMainPanel() {
        final Object tag = FloatingToolbar.MAIN_PANEL_TAG;
        return onFloatingToolbarComponent(withTagValue(is(tag)));
    }

    public static ViewInteraction onFloatingToolbarOverflowPanel() {
        final Object tag = FloatingToolbar.OVERFLOW_PANEL_TAG;
        return onFloatingToolbarComponent(withTagValue(is(tag)));
    }

    private static ViewInteraction onFloatingToolbarComponent(Matcher<View> viewMatcher) {
        final Object tag = FloatingToolbar.FLOATING_TOOLBAR_TAG;
        return onView(viewMatcher)
                .inRoot(withDecorView(hasDescendant(withTagValue(is(tag)))));
    }
}
