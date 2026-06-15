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

package androidx.webkit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.MediumTest;
import androidx.webkit.test.common.WebkitUtils;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@MediumTest
@RunWith(AndroidJUnit4.class)
public class HttpCacheTest {
    private static final String TEST_PROFILE_NAME = "http-cache-test-profile";
    private static final long MEBIBYTE = 1024 * 1024;

    private Profile mProfile;
    private HttpCache mHttpCache;

    @Before
    public void setUp() {
        WebkitUtils.checkFeature(WebViewFeature.MULTI_PROFILE);
        WebkitUtils.checkFeature(WebViewFeature.HTTP_CACHE_MANAGER);
        WebkitUtils.onMainThreadSync(() -> {
            // Deleting a loaded profile is not supported, so no teardown code.
            mProfile = ProfileStore.getInstance().getOrCreateProfile(TEST_PROFILE_NAME);
            mHttpCache = mProfile.getHttpCache();
        });
    }

    @Test
    public void testSetAndGetQuota() {
        WebkitUtils.onMainThreadSync(() -> {
            mHttpCache.setQuotaBytes(10 * MEBIBYTE);
            assertEquals("Quota should match the set value (10MiB)", 10 * MEBIBYTE,
                    mHttpCache.getQuotaBytes());
            assertFalse("Should not be using default quota after setting a custom one",
                    mHttpCache.isUsingDefaultQuota());

            mHttpCache.setQuotaBytes(40 * MEBIBYTE);
            assertEquals("Quota should match the set value (40MiB)", 40 * MEBIBYTE,
                    mHttpCache.getQuotaBytes());
        });
    }

    @Test
    public void testDefaultQuotaMethods() {
        WebkitUtils.onMainThreadSync(() -> {
            long defaultQuota = mHttpCache.getDefaultQuotaBytes();
            assertTrue("Default quota should be positive", defaultQuota > 0);

            // Any call to setQuotaBytes should make it non-default, even if setting the same value
            mHttpCache.setQuotaBytes(defaultQuota);
            assertFalse("Should not be using default quota after calling setQuotaBytes()",
                    mHttpCache.isUsingDefaultQuota());

            mHttpCache.useDefaultQuota();
            assertTrue("Should be using default quota after useDefaultQuota()",
                    mHttpCache.isUsingDefaultQuota());
            assertEquals("Current quota should match default quota after reset",
                    defaultQuota, mHttpCache.getQuotaBytes());
        });
    }
}
