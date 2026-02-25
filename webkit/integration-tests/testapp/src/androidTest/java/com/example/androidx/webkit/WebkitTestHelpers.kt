/*
 * Copyright 2026 The Android Open Source Project
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

package com.example.androidx.webkit

import android.content.Context
import androidx.annotation.IdRes
import androidx.annotation.StringRes
import androidx.test.espresso.Espresso.onData
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.espresso.web.assertion.WebViewAssertions.webMatches
import androidx.test.espresso.web.sugar.Web.onWebView
import androidx.test.espresso.web.webdriver.DriverAtoms.findElement
import androidx.test.espresso.web.webdriver.DriverAtoms.getText
import androidx.test.espresso.web.webdriver.Locator
import androidx.test.platform.app.InstrumentationRegistry
import androidx.webkit.WebViewFeature
import org.hamcrest.Matchers.containsString
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.hasToString
import org.junit.Assume

/** Helper methods for testing. */

/**
 * Click on a MenuListView entry.
 *
 * @param resourceId string id of menu item
 */
fun clickMenuListItemWithString(@StringRes resourceId: Int) {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    onData(hasToString(equalTo(context.getString(resourceId)))).perform(click())
}

/**
 * Click on a view by id.
 *
 * @param viewId view to be clicked on
 */
fun clickViewWithId(@IdRes viewId: Int) {
    onView(withId(viewId)).perform(click())
}

/**
 * Asserts that a view displays the expected text.
 *
 * @param viewId the view to be checked
 * @param stringResourceId the text's resource id
 * @param formatArgs optional format args used by the text string
 */
fun assertViewHasText(@IdRes viewId: Int, @StringRes stringResourceId: Int, formatArgs: Any) {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    onView(withId(viewId)).check(matches(withText(context.getString(stringResourceId, formatArgs))))
}

/**
 * Assert that an HTML element in the given WebView object contains the given text.
 *
 * @param webViewId ID of the WebView object that contains the HTML object.
 * @param tagId the ID attribute of the HTML tag.
 * @param text the expected text inside the HTML element.
 */
fun assertHtmlElementContainsText(@IdRes webViewId: Int, tagId: String, text: String) {
    onWebView(withId(webViewId))
        .withElement(findElement(Locator.ID, tagId))
        .check(webMatches(getText(), containsString(text)))
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
fun assumeFeature(@WebViewFeature.WebViewSupportFeature featureName: String) {
    val hasFeature = WebViewFeature.isFeatureSupported(featureName)
    Assume.assumeTrue("This device does not have the feature '$featureName'", hasFeature)
}

/**
 * Throws {@link org.junit.AssumptionViolatedException} if the device supports the particular
 * feature, otherwise returns.
 *
 * <p>
 * This provides a more descriptive message than a bare {@code assumeFalse} call.
 *
 * @param featureName the feature to be checked
 */
fun assumeFeatureNotAvailable(@WebViewFeature.WebViewSupportFeature featureName: String) {
    val hasFeature = WebViewFeature.isFeatureSupported(featureName)
    Assume.assumeFalse("This device has the feature '$featureName'", hasFeature)
}

/**
 * Throws {@link org.junit.AssumptionViolatedException} if the device supports the particular
 * startup feature, otherwise returns.
 *
 * <p>
 * This provides a more descriptive message than a bare {@code assumeFalse} call.
 *
 * @param featureName the feature to be checked
 */
fun assumeStartupFeature(
    @WebViewFeature.WebViewSupportFeature featureName: String,
    context: Context,
) {
    val hasFeature = WebViewFeature.isStartupFeatureSupported(context, featureName)
    Assume.assumeTrue("This device does not have the feature '$featureName'", hasFeature)
}

/**
 * Javascript has to be enabled for espresso tests to work.
 *
 * @param webViewIds WebView IDs for which to enable JavaScript
 */
fun enableJavaScript(@IdRes vararg webViewIds: Int) {
    for (webViewId in webViewIds) {
        onWebView(withId(webViewId)).forceJavascriptEnabled()
    }
}
