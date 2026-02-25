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

import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.webkit.WebViewFeature
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Integration test for {@link ProxyOverrideActivity}. */
@RunWith(AndroidJUnit4::class)
@LargeTest
class ProxyOverrideTestAppTest {

    @get:Rule val rule = ActivityScenarioRule(MainActivity::class.java)

    @Before
    fun setUp() {
        assumeFeature(WebViewFeature.PROXY_OVERRIDE)
    }

    @Test
    fun testProxyOverride() {
        clickMenuListItemWithString(R.string.proxy_override_activity_title)
        assertViewHasText(R.id.proxy_override_textview, R.string.proxy_override_requests_served, 0)

        // set proxy override and load url
        clickViewWithId(R.id.proxy_override_button)
        clickViewWithId(R.id.proxy_override_load_url_button)

        // assert proxy served 1 request
        assertViewHasText(R.id.proxy_override_textview, R.string.proxy_override_requests_served, 1)
    }

    @Test
    fun testReverseBypass() {
        clickMenuListItemWithString(R.string.proxy_override_activity_title)
        assertViewHasText(R.id.proxy_override_textview, R.string.proxy_override_requests_served, 0)

        // check proxy override bypass, set proxy override and load bypass url
        clickViewWithId(R.id.proxy_override_reverse_bypass_checkbox)
        clickViewWithId(R.id.proxy_override_button)
        clickViewWithId(R.id.proxy_override_load_bypass_button)

        // assert proxy served 1 request
        assertViewHasText(R.id.proxy_override_textview, R.string.proxy_override_requests_served, 1)
    }
}
