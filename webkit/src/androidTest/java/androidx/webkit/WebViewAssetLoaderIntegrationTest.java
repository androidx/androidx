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
import android.net.Uri;
import android.os.Bundle;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.test.filters.MediumTest;
import androidx.test.rule.ActivityTestRule;
import androidx.test.runner.AndroidJUnit4;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;

@RunWith(AndroidJUnit4.class)
public class WebViewAssetLoaderIntegrationTest {
    private static final String TAG = "WebViewAssetLoaderIntegrationTest";

    @Rule
    public final ActivityTestRule<TestActivity> mActivityRule =
                                    new ActivityTestRule<>(TestActivity.class);

    // An Activity for Integeration tests
    public static class TestActivity extends Activity {
        private class MyWebViewClient extends WebViewClient {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return false;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                mOnPageFinishedUrl.add(url);
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

        private WebViewAssetLoader mAssetLoader;
        private WebView mWebView;
        private ArrayBlockingQueue<String> mOnPageFinishedUrl = new ArrayBlockingQueue<String>(5);

        public WebViewAssetLoader getAssetLoader() {
            return mAssetLoader;

        }

        public WebView getWebView() {
            return mWebView;
        }

        public ArrayBlockingQueue<String> getOnPageFinishedUrl() {
            return mOnPageFinishedUrl;
        }

        private void setUpWebView(WebView view) {
            view.setWebViewClient(new MyWebViewClient());
        }

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            mAssetLoader = (new WebViewAssetLoader.Builder(this)).build();
            mWebView = new WebView(this);
            setUpWebView(mWebView);
            setContentView(mWebView);
        }

        @Override
        protected void onDestroy() {
            super.onDestroy();
            mWebView.destroy();
            mWebView = null;
        }
    }

    @Test
    @MediumTest
    public void testAssetHosting() throws Exception {
        final TestActivity activity = mActivityRule.getActivity();
        final String test_with_title_path = "www/test_with_title.html";

        String url = WebkitUtils.onMainThreadSync(new Callable<String>() {
            @Override
            public String call() {
                WebViewAssetLoader assetLoader = activity.getAssetLoader();
                Uri.Builder testPath =
                        assetLoader.getAssetsHttpsPrefix().buildUpon()
                                .appendPath(test_with_title_path);

                String url = testPath.toString();
                activity.getWebView().loadUrl(url);

                return url;
            }
        });

        String onPageFinishedUrl = activity.getOnPageFinishedUrl().take();
        Assert.assertEquals(url, onPageFinishedUrl);

        String title = WebkitUtils.onMainThreadSync(new Callable<String>() {
            @Override
            public String call() {
                return activity.getWebView().getTitle();
            }
        });
        Assert.assertEquals("WebViewAssetLoaderTest", title);
    }

    @Test
    @MediumTest
    public void testResourcesHosting() throws Exception {
        final TestActivity activity = mActivityRule.getActivity();
        final String test_with_title_path = "test_with_title.html";

        String url = WebkitUtils.onMainThreadSync(new Callable<String>() {
            @Override
            public String call() {
                WebViewAssetLoader assetLoader = activity.getAssetLoader();
                Uri.Builder testPath =
                        assetLoader.getResourcesHttpsPrefix().buildUpon()
                        .appendPath("raw")
                        .appendPath(test_with_title_path);

                String url = testPath.toString();
                activity.getWebView().loadUrl(url);

                return url;
            }
        });

        String onPageFinishedUrl = activity.getOnPageFinishedUrl().take();
        Assert.assertEquals(url, onPageFinishedUrl);

        String title = WebkitUtils.onMainThreadSync(new Callable<String>() {
            @Override
            public String call() {
                return activity.getWebView().getTitle();
            }
        });
        Assert.assertEquals("WebViewAssetLoaderTest", title);
    }
}
