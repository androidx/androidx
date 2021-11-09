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

import static android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;

import static androidx.browser.customtabs.CustomTabsIntent.COLOR_SCHEME_DARK;
import static androidx.browser.customtabs.TrustedWebUtils.EXTRA_LAUNCH_AS_TRUSTED_WEB_ACTIVITY;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.RequiresApi;
import androidx.browser.customtabs.CustomTabColorSchemeParams;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.browser.customtabs.CustomTabsSession;
import androidx.browser.customtabs.TestUtil;
import androidx.browser.trusted.TrustedWebActivityDisplayMode.ImmersiveMode;
import androidx.browser.trusted.sharing.ShareData;
import androidx.browser.trusted.sharing.ShareTarget;
import androidx.browser.trusted.splashscreens.SplashScreenParamKey;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.internal.DoNotInstrument;

import java.util.Arrays;
import java.util.List;

/**
 * Tests for {@link TrustedWebActivityIntentBuilder}.
 */
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
// minSdk For Bundle#getBinder
@RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
@Config(minSdk = Build.VERSION_CODES.JELLY_BEAN_MR2)
public class TrustedWebActivityIntentBuilderTest {

    @SuppressWarnings("deprecation")
    @Test
    public void intentIsConstructedCorrectly() {
        Uri url = Uri.parse("https://test.com/page");
        int toolbarColor = 0xffaabbcc;
        int navigationBarColor = 0xffccaabb;
        int navigationBarDividerColor = 0xffdddddd;
        List<String> additionalTrustedOrigins =
                Arrays.asList("https://m.test.com", "https://test.org");

        Bundle splashScreenParams = new Bundle();
        int splashBgColor = 0x112233;
        splashScreenParams.putInt(SplashScreenParamKey.KEY_BACKGROUND_COLOR, splashBgColor);

        CustomTabColorSchemeParams colorSchemeParams = new CustomTabColorSchemeParams.Builder()
                    .setToolbarColor(toolbarColor).build();

        CustomTabColorSchemeParams defaultColorSchemeParams =
                new CustomTabColorSchemeParams.Builder().setToolbarColor(toolbarColor).build();

        ShareData shareData = new ShareData("share_title", "share_text", null);
        ShareTarget shareTarget = new ShareTarget("action", null, null,
                new ShareTarget.Params(null, null, null));

        CustomTabsSession session = TestUtil.makeMockSession();

        ImmersiveMode displayMode = new ImmersiveMode(true,
                LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES);
        Intent intent = new TrustedWebActivityIntentBuilder(url)
                        .setNavigationBarColor(navigationBarColor)
                        .setNavigationBarDividerColor(navigationBarDividerColor)
                        .setColorScheme(COLOR_SCHEME_DARK)
                        .setColorSchemeParams(COLOR_SCHEME_DARK, colorSchemeParams)
                        .setDefaultColorSchemeParams(defaultColorSchemeParams)
                        .setAdditionalTrustedOrigins(additionalTrustedOrigins)
                        .setSplashScreenParams(splashScreenParams)
                        .setShareParams(shareTarget, shareData)
                        .setDisplayMode(displayMode)
                        .build(session)
                        .getIntent();

        assertTrue(intent.getBooleanExtra(EXTRA_LAUNCH_AS_TRUSTED_WEB_ACTIVITY, false));
        TestUtil.assertIntentHasSession(intent, session);
        assertEquals(url, intent.getData());
        assertEquals(toolbarColor, intent.getIntExtra(CustomTabsIntent.EXTRA_TOOLBAR_COLOR, 0));
        assertEquals(navigationBarColor, intent.getIntExtra(
                CustomTabsIntent.EXTRA_NAVIGATION_BAR_COLOR, 0));
        assertEquals(navigationBarDividerColor, intent.getIntExtra(
                CustomTabsIntent.EXTRA_NAVIGATION_BAR_DIVIDER_COLOR, 0));
        assertEquals(additionalTrustedOrigins, intent.getStringArrayListExtra(
                TrustedWebActivityIntentBuilder.EXTRA_ADDITIONAL_TRUSTED_ORIGINS));
        assertEquals(COLOR_SCHEME_DARK, intent.getIntExtra(
                CustomTabsIntent.EXTRA_COLOR_SCHEME, -1));
        assertEquals(colorSchemeParams.toolbarColor,
                CustomTabsIntent.getColorSchemeParams(intent, COLOR_SCHEME_DARK).toolbarColor);

        Bundle splashScreenParamsReceived =
                intent.getBundleExtra(TrustedWebActivityIntentBuilder.EXTRA_SPLASH_SCREEN_PARAMS);

        // No need to test every splash screen param: they are sent in as-is in provided Bundle.
        assertEquals(splashBgColor, splashScreenParamsReceived.getInt(
                SplashScreenParamKey.KEY_BACKGROUND_COLOR));

        ShareData shareDataFromIntent = ShareData.fromBundle(intent.getBundleExtra(
                TrustedWebActivityIntentBuilder.EXTRA_SHARE_DATA));
        // Bundling-unbundling of the ShareData and ShareTarget is tested in more detail elsewhere.
        // Here we only check that the Builder correctly added the extras.
        assertEquals(shareData.title, shareDataFromIntent.title);

        ShareTarget shareTargetFromIntent = ShareTarget.fromBundle(intent.getBundleExtra(
                TrustedWebActivityIntentBuilder.EXTRA_SHARE_TARGET));
        assertEquals(shareTarget.action, shareTargetFromIntent.action);

        TrustedWebActivityDisplayMode displayModeFromIntent =
                TrustedWebActivityDisplayMode.fromBundle(intent.getBundleExtra(
                        TrustedWebActivityIntentBuilder.EXTRA_DISPLAY_MODE));

        assertEquals(displayMode.isSticky(), ((ImmersiveMode) displayModeFromIntent).isSticky());
        assertEquals(displayMode.layoutInDisplayCutoutMode(),
                ((ImmersiveMode) displayModeFromIntent).layoutInDisplayCutoutMode());
    }
}
