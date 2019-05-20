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
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import static org.junit.Assume.assumeFalse;
import static org.junit.Assume.assumeTrue;

import android.content.Context;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;


@RunWith(AndroidJUnit4.class)
@LargeTest
public class WebkitIntegrationTest {
    @Rule
    public ActivityTestRule<MainActivity> mActivityRule =
            new ActivityTestRule<>(MainActivity.class);

    private Context mTargetContext;

    @Before
    public void setUp() {
        mActivityRule.getActivity();
        mTargetContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
    }

    @Test
    public void testProxyOverrideNotAvailable() {
        assumeFalse(androidx.webkit.WebViewFeature.isFeatureSupported(
                androidx.webkit.WebViewFeature.PROXY_OVERRIDE));

        onView(withText(R.string.proxy_override_activity_title)).perform(click());
        onView(withText(R.string.webkit_api_not_available)).check(matches(isDisplayed()));
    }

    @Test
    public void testProxyOverride() {
        assumeTrue(androidx.webkit.WebViewFeature.isFeatureSupported(
                androidx.webkit.WebViewFeature.PROXY_OVERRIDE));

        onView(withText(R.string.proxy_override_activity_title)).perform(click());
        onView(withId(R.id.proxy_override_textview)).check(
                matches(
                        withText(mTargetContext.getString(
                            R.string.proxy_override_requests_served, 1)
                        )
                )
        );
    }
}
