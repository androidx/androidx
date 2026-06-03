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

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.MediumTest;
import androidx.webkit.test.common.WebViewOnUiThread;
import androidx.webkit.test.common.WebkitUtils;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

@MediumTest
@RunWith(AndroidJUnit4.class)
public class NavigateTest {

    private WebViewOnUiThread mWebViewOnUiThread;
    private MockWebServer mWebServer;

    @Before
    public void setUp() throws Exception {
        mWebViewOnUiThread = new WebViewOnUiThread();
        mWebServer = new MockWebServer();
        mWebServer.start();
    }

    @After
    public void tearDown() throws Exception {
        if (mWebViewOnUiThread != null) {
            mWebViewOnUiThread.cleanUp();
        }

        if (mWebServer != null) {
            mWebServer.shutdown();
        }
    }

    @Test
    public void testNavigateWithParams() throws Exception {
        WebkitUtils.checkFeature(WebViewFeature.WEBVIEW_NAVIGATE_EXPERIMENTAL_V1);

        mWebServer.enqueue(new MockResponse().setBody("<html><body>Navigate Test</body></html>"));
        String testUrl = mWebServer.url("/navigate-test").toString();

        final String headerKey = "X-Test-Navigate-Header";
        final String headerValue = "TestValue";

        NavigationParameters params = new NavigationParameters.Builder()
                .setShouldReplaceCurrentEntry(true)
                .addAdditionalHeaders(Map.of(headerKey, headerValue))
                .build();

        mWebViewOnUiThread.navigateAndWaitForCompletion(testUrl, params);

        RecordedRequest request = mWebServer.takeRequest(5, TimeUnit.SECONDS);

        Assert.assertNotNull("The server did not receive the request", request);
        Assert.assertEquals("The URL path did not match", "/navigate-test", request.getPath());
        Assert.assertEquals("The custom header was not received by the server",
                headerValue, request.getHeader(headerKey));
    }
}
