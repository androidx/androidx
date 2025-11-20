/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.webkit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

import android.os.Build;
import android.webkit.WebView;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SdkSuppress;
import androidx.test.filters.SmallTest;
import androidx.webkit.test.common.WebViewOnUiThread;
import androidx.webkit.test.common.WebkitUtils;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * A class for testing common usages for ProfileStore, Profile.
 *
 * TODO(b/304456333) Delete the profile used in each test.
 */
@SmallTest
@RunWith(AndroidJUnit4.class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.N)
public class MultiProfileTest {

    ProfileStore mProfileStore;

    // We are unifying the name as there's no way at the moment to delete the loaded profiles, we
    // should be able to use different test profiles once b/304456333 is fixed.
    private static final String PROFILE_TEST_NAME = "Test";

    @Before
    public void setUp() {
        WebkitUtils.checkFeature(WebViewFeature.MULTI_PROFILE);
        mProfileStore = WebkitUtils.onMainThreadSync(ProfileStore::getInstance);
    }

    /**
     * Tests creation and the created profile is ready to use.
     */
    @Test
    public void testCreateProfile() {
        WebkitUtils.onMainThreadSync(() -> {
            Profile createdProfile = mProfileStore.getOrCreateProfile(PROFILE_TEST_NAME);

            Assert.assertNotNull(createdProfile);
            Assert.assertEquals(createdProfile.getName(), PROFILE_TEST_NAME);
            Assert.assertNotNull(createdProfile.getCookieManager());
            Assert.assertNotNull(createdProfile.getGeolocationPermissions());
            Assert.assertNotNull(createdProfile.getWebStorage());
            Assert.assertNotNull(createdProfile.getServiceWorkerController());
        });
    }

    /**
     * Tests getting, verifying the default profile.
     */
    @Test
    public void testGetDefaultProfile() {
        WebkitUtils.onMainThreadSync(() -> {
            Profile defaultProfile = mProfileStore.getProfile(Profile.DEFAULT_PROFILE_NAME);

            Assert.assertNotNull(defaultProfile);
            Assert.assertEquals(defaultProfile.getName(), Profile.DEFAULT_PROFILE_NAME);
        });
    }

    /**
     * Test profile deletion with three inner asserts.
     */
    @Test
    public void testDeleteProfile() {
        WebkitUtils.onMainThreadSync(
                () -> {
                    // DeleteProfileInUseFails
                    mProfileStore.getOrCreateProfile(PROFILE_TEST_NAME);

                    Assert.assertThrows(
                            IllegalStateException.class,
                            () -> mProfileStore.deleteProfile(PROFILE_TEST_NAME));

                    // DeleteProfileNonExistent
                    Assert.assertFalse(mProfileStore.deleteProfile("Not-Found"));

                    // DeleteDefaultProfileFails
                    Assert.assertThrows(
                            IllegalArgumentException.class,
                            () -> mProfileStore.deleteProfile(Profile.DEFAULT_PROFILE_NAME));
                });
    }

    // setProfile, getProfile tests.

    /**
     * Test getting profile that was previously set should return the correct object.
     */
    @Test
    public void testSetGetProfile() {
        WebkitUtils.checkFeature(WebViewFeature.MULTI_PROFILE);

        Profile testProfile =
                WebkitUtils.onMainThreadSync(() -> ProfileStore.getInstance().getOrCreateProfile(
                        PROFILE_TEST_NAME));
        WebView webView = WebViewOnUiThread.createWebView();
        try {
            WebkitUtils.onMainThreadSync(() -> WebViewCompat.setProfile(webView,
                    testProfile.getName()));

            Profile expectedProfile = WebkitUtils.onMainThreadSync(
                    () -> WebViewCompat.getProfile(webView));

            assertSame(testProfile.getName(), expectedProfile.getName());
            assertSame(testProfile.getCookieManager(), expectedProfile.getCookieManager());
            assertSame(testProfile.getWebStorage(), expectedProfile.getWebStorage());
        } finally {
            WebViewOnUiThread.destroy(webView);
        }

    }

    /**
     * Test getting profile returns the Default profile by default.
     */
    @Test
    public void testGetProfileReturnsDefault() {
        WebkitUtils.checkFeature(WebViewFeature.MULTI_PROFILE);

        WebView webView = WebViewOnUiThread.createWebView();
        try {
            Profile expectedProfile = WebkitUtils.onMainThreadSync(
                    () -> WebViewCompat.getProfile(webView));

            assertNotNull(expectedProfile);
            assertEquals(Profile.DEFAULT_PROFILE_NAME, expectedProfile.getName());
        } finally {
            WebViewOnUiThread.destroy(webView);
        }

    }
}
