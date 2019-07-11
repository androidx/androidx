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

package androidx.webkit;

import static androidx.webkit.WebViewAssetLoader.AssetsPathHandler;
import static androidx.webkit.WebViewAssetLoader.ResourcesPathHandler;

import android.net.Uri;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;

import androidx.test.filters.MediumTest;
import androidx.test.rule.ActivityTestRule;
import androidx.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class WebViewAssetLoaderIntegrationTest {
    private static final String TAG = "WebViewAssetLoaderIntegrationTest";

    @Rule
    public final ActivityTestRule<WebViewTestActivity> mActivityRule =
                                    new ActivityTestRule<>(WebViewTestActivity.class);

    private WebViewOnUiThread mOnUiThread;

    private static class AssetLoadingWebViewClient extends WebViewOnUiThread.WaitForLoadedClient {
        private final WebViewAssetLoader mAssetLoader;
        AssetLoadingWebViewClient(WebViewOnUiThread onUiThread,
                WebViewAssetLoader assetLoader) {
            super(onUiThread);
            mAssetLoader = assetLoader;
        }

        @SuppressWarnings({"deprecated"})
        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
            return mAssetLoader.shouldInterceptRequest(Uri.parse(url));
        }

        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view,
                                            WebResourceRequest request) {
            return mAssetLoader.shouldInterceptRequest(request.getUrl());
        }
    }

    @Before
    public void setUp() {
        mOnUiThread = new WebViewOnUiThread(mActivityRule.getActivity().getWebView());
    }

    @After
    public void tearDown() {
        if (mOnUiThread != null) {
            mOnUiThread.cleanUp();
        }
    }

    @Test
    @MediumTest
    public void testAssetsHosting() throws Exception {
        final WebViewTestActivity activity = mActivityRule.getActivity();

        WebViewAssetLoader assetLoader = new WebViewAssetLoader.Builder()
                .register("/assets/", new AssetsPathHandler(activity))
                .build();

        mOnUiThread.setWebViewClient(new AssetLoadingWebViewClient(mOnUiThread, assetLoader));

        String url = new Uri.Builder()
                        .scheme("https")
                        .authority(WebViewAssetLoader.DEFAULT_DOMAIN)
                        .appendPath("assets")
                        .appendPath("www")
                        .appendPath("test_with_title.html")
                        .build()
                        .toString();
        mOnUiThread.loadUrlAndWaitForCompletion(url);

        Assert.assertEquals("WebViewAssetLoaderTest", mOnUiThread.getTitle());
    }

    @Test
    @MediumTest
    public void testResourcesHosting() throws Exception {
        final WebViewTestActivity activity = mActivityRule.getActivity();

        WebViewAssetLoader assetLoader = new WebViewAssetLoader.Builder()
                .register("/res/", new ResourcesPathHandler(activity))
                .build();

        mOnUiThread.setWebViewClient(new AssetLoadingWebViewClient(mOnUiThread, assetLoader));

        String url = new Uri.Builder()
                        .scheme("https")
                        .authority(WebViewAssetLoader.DEFAULT_DOMAIN)
                        .appendPath("res")
                        .appendPath("raw")
                        .appendPath("test_with_title.html")
                        .build()
                        .toString();
        mOnUiThread.loadUrlAndWaitForCompletion(url);

        Assert.assertEquals("WebViewAssetLoaderTest", mOnUiThread.getTitle());
    }
}
