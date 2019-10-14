/*
 * Copyright 2019 The Android Open Source Project
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

package com.example.androidx.webkit;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA;
import static androidx.test.espresso.matcher.ViewMatchers.withClassName;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import static org.hamcrest.core.AllOf.allOf;
import static org.hamcrest.core.Is.is;

import android.content.Context;

import androidx.annotation.IdRes;
import androidx.annotation.StringRes;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;
import androidx.webkit.WebViewFeature;

import org.junit.Assume;

/**
 * Launch, interact, and verify conditions in the integration test app.
 *
 * <p>
 * Note that this class relies on Espresso's wait for idle behavior to synchronize
 * operations with UI updates from the test application.  If there are any async
 * UI updates, then the test activity must ensure that actions and assertions do not execute
 * before pending they are completed.  Implementing an
 * {@link androidx.test.espresso.IdlingResource} in the test application is the standard way
 * of doing this.
 */
public class IntegrationAppTestRule extends ActivityTestRule<MainActivity> {

    public IntegrationAppTestRule() {
        super(MainActivity.class);
    }

    /**
     * Throws {@link org.junit.AssumptionViolatedException} if the device does not support the
     * particular feature, otherwise returns.
     *
     * <p>
     * This provides a more descriptive message than a bare {@code assumeTrue} call.
     *
     * @param featureName the feature to be checked
     */
    public static void assumeFeature(String featureName) {
        final String msg = "This device does not have the feature '" +  featureName + "'";
        final boolean hasFeature = WebViewFeature.isFeatureSupported(featureName);
        Assume.assumeTrue(msg, hasFeature);
    }

    /**
     * Throws {@link org.junit.AssumptionViolatedException} if the device supports the
     * particular feature, otherwise returns.
     *
     * <p>
     * This provides a more descriptive message than a bare {@code assumeFalse} call.
     *
     * @param featureName the feature to be checked
     */
    public static void assumeFeatureNotAvailable(String featureName) {
        final String msg = "This device has the feature '" +  featureName + "'";
        final boolean hasFeature = WebViewFeature.isFeatureSupported(featureName);
        Assume.assumeFalse(msg, hasFeature);
    }

    /**
     * Click on a MenuListView entry.
     *
     * @param itemStringResourceId string id of menu item
     */
    public void clickMenuListItem(@StringRes int itemStringResourceId) {
        onView(allOf(isDescendantOfA(withClassName(is(MenuListView.class.getName()))),
               withText(itemStringResourceId))).perform(click());
    }

    /**
     * Click on a view by id.
     *
     * @param viewId view to be clicked on
     */
    public void clickViewWithId(@IdRes int viewId) {
        onView(withId(viewId)).perform(click());
    }

    /**
     * Asserts that a view displays the expected text.
     *
     * @param viewId the view to be checked
     * @param stringResourceId the text's resource id
     * @param fmtArgs optional format args used by the text string
     */
    public void assertViewHasText(@IdRes int viewId, @StringRes int stringResourceId,
                                  Object... fmtArgs) {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        onView(withId(viewId)).check(
                matches(
                        withText(context.getString(
                            stringResourceId, fmtArgs)
                        )
                )
        );
    }
}
