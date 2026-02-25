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

import android.webkit.WebSettings;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;
import androidx.webkit.test.common.WebViewOnUiThread;
import androidx.webkit.test.common.WebkitUtils;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@SmallTest
@RunWith(AndroidJUnit4.class)
public class BackForwardCacheSettingsTest {
    WebViewOnUiThread mWebViewOnUiThread;

    @Before
    public void setUp() {
        mWebViewOnUiThread = new WebViewOnUiThread();
    }

    @After
    public void tearDown() {
        if (mWebViewOnUiThread != null) {
            mWebViewOnUiThread.cleanUp();
        }
    }

    @Test
    public void testGetBackForwardCacheSettings() {
        WebkitUtils.checkFeature(WebViewFeature.BACK_FORWARD_CACHE_SETTINGS_EXPERIMENTAL_V3);

        WebSettings settings = mWebViewOnUiThread.getSettings();
        BackForwardCacheSettings backForwardCacheSettings =
                WebSettingsCompat.getBackForwardCacheSettings(settings);

        final int pageLimit = 7;
        final int timeout = 120;

        backForwardCacheSettings.setMaxPagesInCache(pageLimit);
        backForwardCacheSettings.setTimeoutSeconds(timeout);

        Assert.assertEquals(pageLimit, backForwardCacheSettings.getMaxPagesInCache());
        Assert.assertEquals(timeout, backForwardCacheSettings.getTimeoutSeconds());
    }

    @Test
    public void testGetBackForwardCacheSettings_updatesReflected() {
        WebkitUtils.checkFeature(WebViewFeature.BACK_FORWARD_CACHE_SETTINGS_EXPERIMENTAL_V3);

        WebSettings settings = mWebViewOnUiThread.getSettings();
        BackForwardCacheSettings backForwardCacheSettings1 =
                WebSettingsCompat.getBackForwardCacheSettings(settings);

        final int pageLimit = 3;
        backForwardCacheSettings1.setMaxPagesInCache(pageLimit);

        // Fetch settings again to verify the change is persistent/reflected
        BackForwardCacheSettings backForwardCacheSettings2 =
                WebSettingsCompat.getBackForwardCacheSettings(settings);

        Assert.assertEquals(pageLimit, backForwardCacheSettings2.getMaxPagesInCache());
    }

    @Test
    public void testLargeTimeout() {
        WebkitUtils.checkFeature(WebViewFeature.BACK_FORWARD_CACHE_SETTINGS_EXPERIMENTAL_V3);

        WebSettings settings = mWebViewOnUiThread.getSettings();
        BackForwardCacheSettings backForwardCacheSettings =
                WebSettingsCompat.getBackForwardCacheSettings(settings);

        final long largeTimeout = Integer.MAX_VALUE + 100L;
        backForwardCacheSettings.setTimeoutSeconds(largeTimeout);
        Assert.assertEquals(largeTimeout, backForwardCacheSettings.getTimeoutSeconds());
    }
}
