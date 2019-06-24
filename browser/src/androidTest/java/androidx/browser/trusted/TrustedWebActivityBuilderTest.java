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

package androidx.browser.trusted;

import static androidx.browser.customtabs.TrustedWebUtils.EXTRA_LAUNCH_AS_TRUSTED_WEB_ACTIVITY;
import static androidx.browser.customtabs.testutil.TestUtil.getBrowserActivityWhenLaunched;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.browser.customtabs.CustomTabsIntent;
import androidx.browser.customtabs.CustomTabsSession;
import androidx.browser.customtabs.CustomTabsSessionToken;
import androidx.browser.customtabs.EnableComponentsTestRule;
import androidx.browser.customtabs.TestActivity;
import androidx.browser.customtabs.TestCustomTabsServiceSupportsTwas;
import androidx.browser.customtabs.testutil.CustomTabConnectionRule;
import androidx.browser.trusted.splashscreens.SplashScreenParamKey;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.MediumTest;
import androidx.test.rule.ActivityTestRule;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.List;

/**
 * Tests for {@link TrustedWebActivityBuilder}.
 */
@RunWith(AndroidJUnit4.class)
@MediumTest
public class TrustedWebActivityBuilderTest {

    @Rule
    public final EnableComponentsTestRule mEnableComponents = new EnableComponentsTestRule(
            TestActivity.class,
            TestBrowser.class,
            TestCustomTabsServiceSupportsTwas.class
    );

    @Rule
    public final ActivityTestRule<TestActivity> mActivityTestRule =
            new ActivityTestRule<>(TestActivity.class, false, true);

    @Rule
    public final CustomTabConnectionRule mConnectionRule = new CustomTabConnectionRule();

    private TestActivity mActivity;
    private CustomTabsSession mSession;

    @Before
    public void setUp() {
        mActivity = mActivityTestRule.getActivity();
        mSession = mConnectionRule.establishSessionBlocking(mActivity);
    }

    @Test
    public void intentIsConstructedCorrectly() {
        Uri url = Uri.parse("https://test.com/page");
        int statusBarColor = 0xaabbcc;
        List<String> additionalTrustedOrigins =
                Arrays.asList("https://m.test.com", "https://test.org");

        Bundle splashScreenParams = new Bundle();
        int splashBgColor = 0x112233;
        splashScreenParams.putInt(
                SplashScreenParamKey.BACKGROUND_COLOR, splashBgColor);

        final TrustedWebActivityBuilder builder =
                new TrustedWebActivityBuilder(mActivity, url)
                        .setStatusBarColor(statusBarColor)
                        .setAdditionalTrustedOrigins(additionalTrustedOrigins)
                        .setSplashScreenParams(splashScreenParams);
        Intent intent =
                getBrowserActivityWhenLaunched(new Runnable() {
                    @Override
                    public void run() {
                        builder.launchActivity(mSession);
                    }
                }).getIntent();

        assertTrue(intent.getBooleanExtra(EXTRA_LAUNCH_AS_TRUSTED_WEB_ACTIVITY, false));
        assertTrue(CustomTabsSessionToken.getSessionTokenFromIntent(intent)
                .isAssociatedWith(mSession));
        assertEquals(url, intent.getData());
        assertEquals(statusBarColor, intent.getIntExtra(CustomTabsIntent.EXTRA_TOOLBAR_COLOR, 0));
        assertEquals(additionalTrustedOrigins, intent.getStringArrayListExtra(
                TrustedWebActivityBuilder.EXTRA_ADDITIONAL_TRUSTED_ORIGINS));

        Bundle splashScreenParamsReceived =
                intent.getBundleExtra(TrustedWebActivityBuilder.EXTRA_SPLASH_SCREEN_PARAMS);

        // No need to test every splash screen param: they are sent in as-is in provided Bundle.
        assertEquals(splashBgColor, splashScreenParamsReceived.getInt(
                SplashScreenParamKey.BACKGROUND_COLOR));
    }
}
