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
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import androidx.test.filters.LargeTest;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Negative tests for features that use the feature not available fallback message in the test app.
 */
@RunWith(Parameterized.class)
@LargeTest
public class FeatureNotAvailableTestAppTest {
    private static Map<String, Integer> sFeatureActivities = new HashMap<>();

    static {
        //  Add features that displays the webkit_api_not_available error in the test app when
        //  feature is not detected to the sFeatureActivities list.
        //  If there is a different fallback behavior in the test app, please add tests in the
        //  <Feature Name>TestAppTest.java file instead.
        sFeatureActivities.put(androidx.webkit.WebViewFeature.PROXY_OVERRIDE,
                               R.string.proxy_override_activity_title);
    }

    // Method annotated with Parameters will be called by the runner for parameterized testing.
    @Parameterized.Parameters
    public static Collection<Map.Entry<String, Integer>> getFeatures() {
        return sFeatureActivities.entrySet();
    }

    public FeatureNotAvailableTestAppTest(Map.Entry<String, Integer> featureActivity) {
        mFeature = featureActivity.getKey();
        mTitleResId = featureActivity.getValue();
    }

    public String mFeature;

    public Integer mTitleResId;

    @Rule
    public IntegrationAppTestRule mRule = new IntegrationAppTestRule();

    @Before
    public void setUp() {
        mRule.getActivity();
        mRule.assumeFeatureNotAvailable(mFeature);
        mRule.clickMenuListItem(mTitleResId);
    }

    @Test
    public void testFeatureNotAvailable() {
        onView(withText(R.string.webkit_api_not_available)).check(matches(isDisplayed()));
    }
}
