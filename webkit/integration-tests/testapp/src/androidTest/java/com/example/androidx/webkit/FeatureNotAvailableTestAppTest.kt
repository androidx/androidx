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

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.filters.LargeTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * Negative tests for features that use the feature not available fallback message in the test app.
 */
@RunWith(Parameterized::class)
@LargeTest
class FeatureNotAvailableTestAppTest(featureActivity: Pair<String, Int>) {

    val feature = featureActivity.first
    val titleResourceId = featureActivity.second

    companion object {
        @Parameterized.Parameters
        @JvmStatic
        fun getFeatures(): Collection<Pair<String, Int>> {
            //  Add features, that displays the webkit_api_not_available error in the test app when
            //  feature is not detected, to the map below
            //  If there is a different fallback behavior in the test app, please add tests in the
            //  <Feature Name>TestAppTest.kt file instead.
            return listOf(
                Pair(
                    androidx.webkit.WebViewFeature.PROXY_OVERRIDE,
                    R.string.proxy_override_activity_title,
                )
            )
        }
    }

    @get:Rule val rule = ActivityScenarioRule(MainActivity::class.java)

    @Before
    fun setUp() {
        assumeFeatureNotAvailable(feature)
        clickMenuListItemWithString(titleResourceId)
    }

    @Test
    fun testFeatureNotAvailable() {
        onView(withText(R.string.webkit_api_not_available)).check(matches(isDisplayed()))
    }
}
