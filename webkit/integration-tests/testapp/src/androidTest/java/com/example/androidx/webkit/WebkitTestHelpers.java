/*
 * Copyright 2021 The Android Open Source Project
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
import static androidx.test.espresso.web.assertion.WebViewAssertions.webMatches;
import static androidx.test.espresso.web.sugar.Web.onWebView;
import static androidx.test.espresso.web.webdriver.DriverAtoms.findElement;
import static androidx.test.espresso.web.webdriver.DriverAtoms.getText;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.core.AllOf.allOf;
import static org.hamcrest.core.Is.is;

import android.content.Context;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.test.espresso.web.webdriver.Locator;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.webkit.WebViewFeature;

import org.junit.Assume;

/**
 * Helper methods for testing.
 */
public final class WebkitTestHelpers {

    /**
     * Click on a MenuListView entry.
     *
     * @param resourceId string id of menu item
     */
    public static void clickMenuListItemWithString(@StringRes int resourceId) {
        onView(allOf(isDescendantOfA(withClassName(is(MenuListView.class.getName()))),
                withText(resourceId))).perform(click());
    }

    /**
     * Click on a view by id.
     *
     * @param viewId view to be clicked on
     */
    public static void clickViewWithId(@IdRes int viewId) {
        onView(withId(viewId)).perform(click());
    }

    /**
     * Asserts that a view displays the expected text.
     *
     * @param viewId the view to be checked
     * @param stringResourceId the text's resource id
     * @param formatArgs optional format args used by the text string
     */
    public static void assertViewHasText(@IdRes int viewId, @StringRes int stringResourceId,
            Object... formatArgs) {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        onView(withId(viewId))
                .check(matches(withText(context.getString(stringResourceId, formatArgs))));
    }

    /**
     * Assert that an HTML element in the given WebView object contains the given text.
     *
     * @param webViewId ID of the WebView object that contains the HTML object.
     * @param tagId the ID attribute of the HTML tag.
     * @param text the expected text inside the HTML element.
     */
    public static void assertHtmlElementContainsText(@IdRes int webViewId,
            @NonNull String tagId, @NonNull String text) {
        onWebView(withId(webViewId))
                .withElement(findElement(Locator.ID, tagId))
                .check(webMatches(getText(),
                        containsString(text)));
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
    public static void assumeFeature(@WebViewFeature.WebViewSupportFeature String featureName) {
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
    public static void assumeFeatureNotAvailable(
            @WebViewFeature.WebViewSupportFeature String featureName) {
        final String msg = "This device has the feature '" +  featureName + "'";
        final boolean hasFeature = WebViewFeature.isFeatureSupported(featureName);
        Assume.assumeFalse(msg, hasFeature);
    }

    /**
     * Javascript has to be enabled for espresso tests to work.
     *
     * @param webViewIds WebView IDs for which to enable JavaScript
     */
    public static void enableJavaScript(@IdRes int... webViewIds) {
        for (int webViewId : webViewIds) {
            onWebView(withId(webViewId)).forceJavascriptEnabled();
        }
    }

    // Do not instantiate this class.
    private WebkitTestHelpers() {}
}
