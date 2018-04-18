/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.browser.customtabs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.support.test.filters.SmallTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import androidx.core.app.BundleCompat;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests for TrustedWebUtils.
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
public class TrustedWebUtilsTest {
    @Rule
    public final ActivityTestRule<TestActivity> mActivityTestRule;

    public TrustedWebUtilsTest() {
        mActivityTestRule = new ActivityTestRule<>(TestActivity.class);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testTrustedWebIntentRequiresValidSession() {
        CustomTabsIntent customTabsIntent = new CustomTabsIntent.Builder().build();
        TrustedWebUtils.launchAsTrustedWebActivity(
                mActivityTestRule.getActivity(), customTabsIntent, Uri.EMPTY);
    }

    @Test(expected = ActivityNotFoundException.class)
    public void testTrustedWebIntentContainsRequiredExtra() {
        CustomTabsSession mockSession = CustomTabsSession.createMockSessionForTesting(
                mActivityTestRule.getActivity().getComponentName());
        CustomTabsIntent customTabsIntent = new CustomTabsIntent.Builder(mockSession).build();
        TrustedWebUtils.launchAsTrustedWebActivity(
                mActivityTestRule.getActivity(), customTabsIntent, Uri.EMPTY);
        assertNotNull(BundleCompat.getBinder(
                customTabsIntent.intent.getExtras(), CustomTabsIntent.EXTRA_SESSION));
        assertEquals(customTabsIntent.intent.getAction(), Intent.ACTION_VIEW);
        assertTrue(customTabsIntent.intent.hasExtra(
                TrustedWebUtils.EXTRA_LAUNCH_AS_TRUSTED_WEB_ACTIVITY));
    }
}
