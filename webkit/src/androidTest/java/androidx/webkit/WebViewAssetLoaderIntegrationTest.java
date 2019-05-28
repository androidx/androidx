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

import android.app.Activity;
import android.os.Bundle;
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
    public final ActivityTestRule<TestActivity> mActivityRule =
                                    new ActivityTestRule<>(TestActivity.class);

    private WebViewOnUiThread mOnUiThread;
    private WebViewAssetLoader mAssetLoader;

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
            return mAssetLoader.shouldInterceptRequest(url);
        }

        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view,
                                            WebResourceRequest request) {
            return mAssetLoader.shouldInterceptRequest(request);
        }
    }

    // An Activity for Integeration tests
    public static class TestActivity extends Activity {
        private WebView mWebView;

        public WebView getWebView() {
            return mWebView;
        }

        // Runs before test suite's @Before.
        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            mWebView = new WebView(this);
            setContentView(mWebView);
        }
    }

    @Before
    public void setUp() {
        mAssetLoader = (new WebViewAssetLoader.Builder(mActivityRule.getActivity())).build();
        mOnUiThread = new WebViewOnUiThread(mActivityRule.getActivity().getWebView());
        mOnUiThread.setWebViewClient(new AssetLoadingWebViewClient(mOnUiThread, mAssetLoader));
    }

    @After
    public void tearDown() {
        if (mOnUiThread != null) {
            mOnUiThread.cleanUp();
        }
    }

    @Test
    @MediumTest
    public void testAssetHosting() throws Exception {
        final TestActivity activity = mActivityRule.getActivity();

        String url =
                mAssetLoader.getAssetsHttpsPrefix().buildUpon()
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
        final TestActivity activity = mActivityRule.getActivity();

        String url =
                mAssetLoader.getResourcesHttpsPrefix().buildUpon()
                .appendPath("raw")
                .appendPath("test_with_title.html")
                .build()
                .toString();

        mOnUiThread.loadUrlAndWaitForCompletion(url);

        Assert.assertEquals("WebViewAssetLoaderTest", mOnUiThread.getTitle());
    }
}
