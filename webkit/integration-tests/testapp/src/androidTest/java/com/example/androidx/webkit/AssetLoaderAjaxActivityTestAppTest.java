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

import androidx.test.espresso.IdlingRegistry;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Integration test for {@link AssetLoaderAjaxActivity}.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public final class AssetLoaderAjaxActivityTestAppTest {

    @Rule
    public ActivityScenarioRule<AssetLoaderAjaxActivity> mRule =
            new ActivityScenarioRule<>(AssetLoaderAjaxActivity.class);

    @Before
    public void setUp() {
        WebkitTestHelpers.enableJavaScript(R.id.webview_asset_loader_webview);
        mRule.getScenario().onActivity(activity -> IdlingRegistry.getInstance().register(
                activity.getUriIdlingResource()));
    }

    @After
    public void tearDown() {
        mRule.getScenario().onActivity(activity -> IdlingRegistry.getInstance().unregister(
                activity.getUriIdlingResource()));
    }

    @Test
    public void testAssetLoaderAjaxActivity() {
        mRule.getScenario().onActivity(AssetLoaderAjaxActivity::loadUrl);
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        WebkitTestHelpers.assertHtmlElementContainsText(R.id.webview_asset_loader_webview,
                "title", "Loaded HTML should appear below on success");
        WebkitTestHelpers.assertHtmlElementContainsText(R.id.webview_asset_loader_webview,
                "assets_html", "Successfully loaded html from assets!");
        WebkitTestHelpers.assertHtmlElementContainsText(R.id.webview_asset_loader_webview,
                "res_html", "Successfully loaded html from resources!");
    }
}
