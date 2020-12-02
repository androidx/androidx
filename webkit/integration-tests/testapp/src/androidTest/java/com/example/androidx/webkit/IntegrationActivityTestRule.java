/*
 * Copyright 2020 The Android Open Source Project
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

import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.web.assertion.WebViewAssertions.webMatches;
import static androidx.test.espresso.web.sugar.Web.onWebView;
import static androidx.test.espresso.web.webdriver.DriverAtoms.findElement;
import static androidx.test.espresso.web.webdriver.DriverAtoms.getText;

import static org.hamcrest.Matchers.containsString;

import androidx.annotation.IdRes;
import androidx.test.espresso.web.webdriver.Locator;
import androidx.test.rule.ActivityTestRule;

/**
 * Launch, interact, and verify conditions in an activity that has a WebView instance
 * in the integration test app.
 *
 * <p>
 * Note that this class relies on Espresso's wait for idle behavior to synchronize
 * operations with UI updates from the test application.  If there are any async
 * UI updates, then the test activity must ensure that actions and assertions do not execute
 * before pending they are completed.  Implementing an
 * {@link androidx.test.espresso.IdlingResource} in the test application is the standard way
 * of doing this.
 */
public class IntegrationActivityTestRule<T extends android.app.Activity>
        extends ActivityTestRule<T> {

    private @IdRes int[] mWebViewIds;

    /**
     * @param activityClass the activity to start the test from.
     * @param webViewIds IDs of all WebView objects in the launched activity to enable javascript
     *                   in them.
     */
    public IntegrationActivityTestRule(Class<T> activityClass, @IdRes int... webViewIds) {
        super(activityClass);
        mWebViewIds = webViewIds;
    }

    @Override
    public void afterActivityLaunched() {
        // Javascript has to be enabled in order for espresso tests to work.
        if (mWebViewIds == null) return;
        for (@IdRes int webViewId : mWebViewIds) {
            onWebView(withId(webViewId)).forceJavascriptEnabled();
        }
    }

    /**
     * Assert that an HTML element in the given WebView object contains the given text.
     *
     * @param webViewId ID of the WebView object that contains the HTML object.
     * @param tagId the ID attribute of the HTML tag.
     * @param text the expected text inside the HTML element.
     */
    public static void assertHtmlElementContainsText(@IdRes int webViewId,
                                                     String tagId, String text) {
        onWebView(withId(webViewId))
                .withElement(findElement(Locator.ID, tagId))
                .check(webMatches(getText(),
                        containsString(text)));
    }
}
