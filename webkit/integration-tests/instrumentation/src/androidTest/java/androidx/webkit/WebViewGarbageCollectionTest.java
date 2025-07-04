/*
 * Copyright 2025 The Android Open Source Project
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

import android.webkit.WebView;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.MediumTest;
import androidx.webkit.internal.WebViewProviderAdapter;
import androidx.webkit.test.common.WebViewOnUiThread;
import androidx.webkit.test.common.WebkitUtils;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;
import java.util.WeakHashMap;

@RunWith(AndroidJUnit4.class)
public class WebViewGarbageCollectionTest {

    @Test
    @MediumTest
    public void testOneWebViewGc() throws Exception {
        WebkitUtils.checkFeature(WebViewFeature.CACHE_PROVIDER);
        runGcTest(() -> {
            WebView wv = WebViewOnUiThread.createWebView();
            // This triggers the call to WebViewCompat.getProvider(..) and is expected to add an
            // entry to the provider cache.
            WebkitUtils.onMainThreadSync(
                    () -> WebViewCompat.getProfile(wv));

            WeakHashMap<WebView, WebViewProviderAdapter>
                    providerAdapterCache = WebViewCompat.getProviderAdapterCacheForTesting();
            Assert.assertEquals(1, providerAdapterCache.size());
            Assert.assertTrue(providerAdapterCache.containsKey(wv));
        });
    }

    @Test
    @MediumTest
    public void testManyWebViewGc() throws Exception {
        WebkitUtils.checkFeature(WebViewFeature.CACHE_PROVIDER);
        runGcTest(() -> {
            final int instancesCount = 32;
            List<WebView> webViews = new ArrayList<>();
            for (int i = 0; i < instancesCount; ++i) {
                WebView wv = WebViewOnUiThread.createWebView();
                // This triggers the call to WebViewCompat.getProvider(..) and is expected to add an
                // entry to the provider cache.
                WebkitUtils.onMainThreadSync(
                        () -> WebViewCompat.getProfile(wv));
                webViews.add(wv);
            }

            WeakHashMap<WebView, WebViewProviderAdapter>
                    providerAdapterCache = WebViewCompat.getProviderAdapterCacheForTesting();
            Assert.assertEquals(instancesCount, providerAdapterCache.size());
            for (WebView wv : webViews) {
                Assert.assertTrue(providerAdapterCache.containsKey(wv));
            }
        });
    }

    // Ensures that the test local variables are in another stack frame.
    private void runGcTest(Runnable testBody) throws Exception {
        gcAndAssertProviderAdapterCacheIsEmpty();
        testBody.run();
        gcAndAssertProviderAdapterCacheIsEmpty();
    }

    private void gcAndAssertProviderAdapterCacheIsEmpty() {
        WeakHashMap<WebView, WebViewProviderAdapter>
                providerAdapterCache = WebViewCompat.getProviderAdapterCacheForTesting();
        for (int i = 0; i < 15; ++i) {
            if (providerAdapterCache.isEmpty()) {
                break;
            }
            Runtime.getRuntime().gc();
            Runtime.getRuntime().runFinalization();
        }
        // Entries are never removed from the cache by us, and so if the cache is empty then the
        // WebViews must have been garbage collected.
        Assert.assertTrue(providerAdapterCache.isEmpty());
    }
}
