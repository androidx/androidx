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

import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.web.assertion.WebViewAssertions.webMatches;
import static androidx.test.espresso.web.sugar.Web.onWebView;
import static androidx.test.espresso.web.webdriver.DriverAtoms.findElement;
import static androidx.test.espresso.web.webdriver.DriverAtoms.getText;

import static org.hamcrest.Matchers.containsString;

import androidx.annotation.IdRes;
import androidx.test.espresso.web.webdriver.Locator;
import androidx.test.filters.LargeTest;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Integeration tests for AssetLoader demo activities.
 *
 * Tests the following activities:
 * <ul>
 * <li>AssetLoaderDemoActivity.</li>
 * <li>AssetLoaderAjaxActivity.</li>
 * <li>AssetLoaderInternalStorageActivity.</li>
 * </ul>
 */
@RunWith(JUnit4.class)
@LargeTest
public final class WebViewAssetLoaderTestAppTest {

    /**
     * A test rule to set up the tests for WebViewAssetLoader demo Activities.
     * It does the following:
     * <ol>
     * <li> Launch {@code MainActivity}. </li>
     * <li> Clicks on the "WebView Asset Loader Demos" in the main menu to open
     * {@code AssetLoaderListActivity}. </li>
     * <li> Clicks on the Activity to be tested in the menu.</li>
     * <li> Force enables Javascript since it's required by Espresso library to test WebView
     * contents.</li>
     * </ol>
     */
    public class IntegrationAppWebViewTestRule extends IntegrationAppTestRule {
        /**
         * Opens the activity to be tested from WebViewAssetLoader demos activity list and force
         * Javascript to be enabled in the WebView instance in that activity.
         * @param activityTitleId resource id for the title of the activity to be opened.
         */
        public void openByActivityTitle(@IdRes int activityTitleId) {
            clickMenuListItem(activityTitleId);
            // All the activities in "WebView Asset Loader Demos" uses the same
            // "layout/activity_asset_loader" so all the WebViews have the same ID:
            // R.id.webview_asset_loader_webview.
            // Not all WebViews in the app have javascript turned on, however since the only way
            // to automate WebViews is through javascript, it must be enabled.
            onWebView(withId(R.id.webview_asset_loader_webview)).forceJavascriptEnabled();
        }

    }

    @Rule
    public IntegrationAppWebViewTestRule mRule = new IntegrationAppWebViewTestRule();

    @Before
    public void setUp() {
        // Open the activity of WebViewAssetLoader demo activities from the main Activity list.
        mRule.clickMenuListItem(R.string.asset_loader_list_activity_title);
    }

    @Test
    public void testAssetLoaderSimpleActivity() {
        mRule.openByActivityTitle(R.string.asset_loader_simple_activity_title);
        onWebView()
                .withElement(findElement(Locator.TAG_NAME, "h3"))
                .check(webMatches(getText(),
                        containsString("Successfully loaded html from assets!")));
    }

    @Test
    public void testAssetLoaderAjaxActivity() {
        mRule.openByActivityTitle(R.string.asset_loader_ajax_activity_title);
        onWebView()
                .withElement(findElement(Locator.TAG_NAME, "h1"))
                .check(webMatches(getText(),
                        containsString("Loaded HTML should appear below on success")))
                .withElement(findElement(Locator.ID, "assets_html"))
                .check(webMatches(getText(),
                        containsString("Successfully loaded html from assets!")))
                .withElement(findElement(Locator.ID, "res_html"))
                .check(webMatches(getText(),
                        containsString("Successfully loaded html from resources!")));
    }

    @Test
    public void testAssetLoaderInternalStorageActivity() {
        mRule.openByActivityTitle(R.string.asset_loader_internal_storage_activity_title);
        onWebView()
                .withElement(findElement(Locator.TAG_NAME, "h3"))
                .check(webMatches(getText(),
                        containsString("Successfully loaded html from app files dir!")));
    }
}
