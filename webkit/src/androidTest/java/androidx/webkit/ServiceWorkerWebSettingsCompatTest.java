/*
 * Copyright (C) 2018 The Android Open Source Project
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

import androidx.test.filters.SmallTest;
import androidx.test.runner.AndroidJUnit4;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@SmallTest
@RunWith(AndroidJUnit4.class)
public class ServiceWorkerWebSettingsCompatTest {
    private ServiceWorkerWebSettingsCompat mSettings;

    @Before
    public void setUp() throws Exception {
        WebkitUtils.checkFeature(WebViewFeature.SERVICE_WORKER_BASIC_USAGE);
        mSettings = ServiceWorkerControllerCompat.getInstance().getServiceWorkerWebSettings();
    }

    /**
     * This should remain functionally equivalent to
     * android.webkit.cts.ServiceWorkerWebSettingsTest#testCacheMode. Modifications to this test
     * should be reflected in that test as necessary. See http://go/modifying-webview-cts.
     */
    @Test
    public void testCacheMode() {
        WebkitUtils.checkFeature(WebViewFeature.SERVICE_WORKER_CACHE_MODE);

        int i = WebSettings.LOAD_DEFAULT;
        Assert.assertEquals(i, mSettings.getCacheMode());
        for (; i <= WebSettings.LOAD_CACHE_ONLY; i++) {
            mSettings.setCacheMode(i);
            Assert.assertEquals(i, mSettings.getCacheMode());
        }
    }

    /**
     * This should remain functionally equivalent to
     * android.webkit.cts.ServiceWorkerWebSettingsTest#testAllowContentAccess. Modifications to
     * this test should be reflected in that test as necessary. See http://go/modifying-webview-cts.
     */
    @Test
    public void testAllowContentAccess() {
        WebkitUtils.checkFeature(WebViewFeature.SERVICE_WORKER_CONTENT_ACCESS);

        Assert.assertEquals(mSettings.getAllowContentAccess(), true);
        for (boolean b : new boolean[]{false, true}) {
            mSettings.setAllowContentAccess(b);
            Assert.assertEquals(b, mSettings.getAllowContentAccess());
        }
    }

    /**
     * This should remain functionally equivalent to
     * android.webkit.cts.ServiceWorkerWebSettingsTest#testAllowFileAccess. Modifications to this
     * test should be reflected in that test as necessary. See http://go/modifying-webview-cts.
     */
    @Test
    public void testAllowFileAccess() {
        WebkitUtils.checkFeature(WebViewFeature.SERVICE_WORKER_FILE_ACCESS);

        Assert.assertEquals(mSettings.getAllowFileAccess(), true);
        for (boolean b : new boolean[]{false, true}) {
            mSettings.setAllowFileAccess(b);
            Assert.assertEquals(b, mSettings.getAllowFileAccess());
        }
    }

    /**
     * This should remain functionally equivalent to
     * android.webkit.cts.ServiceWorkerWebSettingsTest#testBlockNetworkLoads. Modifications to this
     * test should be reflected in that test as necessary. See http://go/modifying-webview-cts.
     */
    @Test
    public void testBlockNetworkLoads() {
        WebkitUtils.checkFeature(WebViewFeature.SERVICE_WORKER_BLOCK_NETWORK_LOADS);

        // Note: we cannot test this setter unless we provide the INTERNET permission, otherwise we
        // get a SecurityException when we pass 'false'.
        final boolean hasInternetPermission = true;

        Assert.assertEquals(mSettings.getBlockNetworkLoads(), !hasInternetPermission);
        for (boolean b : new boolean[]{false, true}) {
            mSettings.setBlockNetworkLoads(b);
            Assert.assertEquals(b, mSettings.getBlockNetworkLoads());
        }
    }
}
