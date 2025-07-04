/*
 * Copyright 2024 The Android Open Source Project
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

import static android.webkit.WebSettings.LOAD_DEFAULT;

import android.os.Build;
import android.webkit.WebStorage;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.MediumTest;
import androidx.test.filters.SdkSuppress;
import androidx.webkit.test.common.WebViewOnUiThread;
import androidx.webkit.test.common.WebkitUtils;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.QueueDispatcher;
import okhttp3.mockwebserver.RecordedRequest;

@SdkSuppress(minSdkVersion = Build.VERSION_CODES.N)

@RunWith(AndroidJUnit4.class)
public class WebStorageTest {
    WebViewOnUiThread mWebViewOnUiThread;

    @Before
    public void setUp() {
        mWebViewOnUiThread = new WebViewOnUiThread();
        mWebViewOnUiThread.getSettings().setCacheMode(LOAD_DEFAULT);
    }

    @After
    public void tearDown() {
        if (mWebViewOnUiThread != null) {
            mWebViewOnUiThread.cleanUp();
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.N)
    @Test
    @MediumTest
    public void testDeleteBrowsingDataDeletesCache() throws Exception {
        WebkitUtils.checkFeature(WebViewFeature.DELETE_BROWSING_DATA);
        try (MockWebServer server = new MockWebServer()) {
            CountingQueueDispatcher dispatcher = new CountingQueueDispatcher();
            server.setDispatcher(dispatcher);
            server.start();

            MockResponse response = new MockResponse();
            response.setBody("response body");
            response.setHeader("Cache-Control", "max-age=604800");

            dispatcher.enqueueResponse(response);

            String url = server.url("/").toString();

            // Load twice, but we should only see one request.
            mWebViewOnUiThread.loadUrlAndWaitForCompletion(url);
            mWebViewOnUiThread.loadUrlAndWaitForCompletion(url);

            Assert.assertEquals(
                    "With cache headers, only one request should have made it to the server", 1,
                    dispatcher.getCountForPath("/"));

            CompletableFuture<Void> resultFuture = new CompletableFuture<>();
            WebkitUtils.onMainThread(
                    () -> WebStorageCompat.deleteBrowsingData(WebStorage.getInstance(),
                            () -> resultFuture.complete(null)));
            resultFuture.get(1, TimeUnit.SECONDS);

            dispatcher.enqueueResponse(response);
            mWebViewOnUiThread.loadUrlAndWaitForCompletion(url);

            Assert.assertEquals("After deleting cache, a new request should be seen by the server",
                    2, dispatcher.getCountForPath("/"));
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.N)
    @Test
    @MediumTest
    public void testDeleteBrowsingDataForSiteDeletesCache() throws Exception {
        WebkitUtils.checkFeature(WebViewFeature.DELETE_BROWSING_DATA);
        try (MockWebServer server = new MockWebServer();
                MockWebServer otherServer = new MockWebServer()) {
            CountingQueueDispatcher dispatcher = new CountingQueueDispatcher();
            server.setDispatcher(dispatcher);
            server.start();
            CountingQueueDispatcher otherDispatcher = new CountingQueueDispatcher();
            otherServer.setDispatcher(otherDispatcher);
            otherServer.start();

            MockResponse response = new MockResponse();
            response.setBody("response body");
            response.setHeader("Cache-Control", "max-age=604800");

            dispatcher.enqueueResponse(response);
            otherDispatcher.enqueueResponse(response);

            String url = server.url("/").toString();
            Assert.assertEquals("localhost", server.getHostName());
            String otherUrl = otherServer.url("/").toString().replace("localhost", "127.0.0.1");

            // First load the other site twice and check that it was only requested once.
            mWebViewOnUiThread.loadUrlAndWaitForCompletion(otherUrl);
            mWebViewOnUiThread.loadUrlAndWaitForCompletion(otherUrl);
            Assert.assertEquals(1, otherDispatcher.getCountForPath("/"));

            // Load twice, but we should only see one request.
            mWebViewOnUiThread.loadUrlAndWaitForCompletion(url);
            mWebViewOnUiThread.loadUrlAndWaitForCompletion(url);
            Assert.assertEquals(
                    "With cache headers, only one request should have made it to the server", 1,
                    dispatcher.getCountForPath("/"));

            CompletableFuture<Void> resultFuture = new CompletableFuture<>();
            WebkitUtils.onMainThreadSync(() -> {
                String actualSite = WebStorageCompat.deleteBrowsingDataForSite(
                        WebStorage.getInstance(), url, () -> resultFuture.complete(null));
                Assert.assertEquals("localhost", actualSite);
            });
            resultFuture.get(1, TimeUnit.SECONDS);

            dispatcher.enqueueResponse(response);
            mWebViewOnUiThread.loadUrlAndWaitForCompletion(url);
            Assert.assertEquals("After deleting cache, a new request should be seen by the server",
                    2, dispatcher.getCountForPath("/"));

            mWebViewOnUiThread.loadUrlAndWaitForCompletion(otherUrl);
            Assert.assertEquals("The other site should still be in cache", 1,
                    otherDispatcher.getCountForPath("/"));

        }
    }

    /**
     * {@link MockWebServer} dispatcher that counts the number of sent responses.
     *
     * @noinspection NewClassNamingConvention
     */
    private static class CountingQueueDispatcher extends QueueDispatcher {

        private final Map<String, Integer> mCounts = new HashMap<>();

        @Override
        public MockResponse dispatch(RecordedRequest request) throws InterruptedException {
            mCounts.compute(request.getPath(), (s, count) -> count == null ? 1 : count + 1);
            return super.dispatch(request);
        }

        int getCountForPath(String path) {
            //noinspection DataFlowIssue
            return mCounts.getOrDefault(path, 0);
        }
    }
}
