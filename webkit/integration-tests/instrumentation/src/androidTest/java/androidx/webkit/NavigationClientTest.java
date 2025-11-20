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

import android.os.Bundle;
import android.webkit.WebView;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.webkit.test.common.WebViewOnUiThread;
import androidx.webkit.test.common.WebkitUtils;

import org.jspecify.annotations.NonNull;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

@SuppressWarnings("deprecation")
@LargeTest
@RunWith(AndroidJUnit4.class)
public class NavigationClientTest {
    private static final String START_URL = "about:blank";
    private static final String SAME_DOCUMENT_URL = "about:blank#fragment";
    private static final int HTTP_OK = 200;

    public WebViewOnUiThread mWebViewOnUiThread;
    private WebView mWebView;
    private RecordingWebNavigationClient mClient;
    private MockWebServer mWebServer;
    private String mWebServerUrl;

    private static class RecordingWebNavigationClient implements WebNavigationClient {
        public final BlockingQueue<Navigation> startedNavigations = new LinkedBlockingQueue<>();
        public final BlockingQueue<Navigation> completedNavigations = new LinkedBlockingQueue<>();

        @Override
        public void onNavigationStarted(@NonNull Navigation navigation) {
            startedNavigations.add(navigation);
        }

        @Override
        public void onNavigationRedirected(@NonNull Navigation navigation) {
        }

        @Override
        public void onNavigationCompleted(@NonNull Navigation navigation) {
            completedNavigations.add(navigation);
        }

        @Override
        public void onPageDeleted(@NonNull Page page) {
        }

        @Override
        public void onPageLoadEventFired(@NonNull Page page) {
        }

        @Override
        public void onPageDomContentLoadedEventFired(@NonNull Page page) {
        }

        @Override
        public void onFirstContentfulPaint(@NonNull Page page) {
        }

        public void clearRecordedEvents() {
            startedNavigations.clear();
            completedNavigations.clear();
        }

        public Navigation waitForNavigationStart() throws InterruptedException {
            return startedNavigations.poll(1, TimeUnit.SECONDS);
        }

        public Navigation waitForNavigationComplete() throws InterruptedException {
            return completedNavigations.poll(10, TimeUnit.SECONDS);
        }
    }

    @Before
    public void setUp() throws Exception {
        WebkitUtils.checkFeature((WebViewFeature.NAVIGATION_CALLBACK_BASIC));
        mWebViewOnUiThread = new WebViewOnUiThread();
        mWebServer = new MockWebServer();
        mWebServer.start();
        mWebServerUrl = mWebServer.url("/").toString();
        WebkitUtils.onMainThreadSync(() -> {
            mWebView = mWebViewOnUiThread.getWebViewOnCurrentThread();
            mClient = new RecordingWebNavigationClient();
            WebViewCompat.setWebNavigationClient(mWebView, mClient);
        });
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
    public void getPage_returnsNonNullOnCommit() throws InterruptedException {
        Navigation navigation = loadUrlAndGetCompletedNavigation(mWebServerUrl);
        Assert.assertNotNull(navigation.getPage());
    }

    @Test
    public void wasInitiatedByPage_isFalseForLoadUrl() throws InterruptedException {
        Navigation navigation = loadUrlAndGetStartedNavigation(mWebServerUrl);
        Assert.assertFalse(navigation.wasInitiatedByPage());
    }

    @Test
    public void isSameDocument_isTrueForFragmentNavigation() throws InterruptedException {
        mWebViewOnUiThread.loadUrlAndWaitForCompletion(START_URL);
        mClient.clearRecordedEvents();
        mWebViewOnUiThread.loadUrlAndWaitForCompletion(SAME_DOCUMENT_URL);
        Navigation navigation = mClient.waitForNavigationStart();
        Assert.assertTrue(navigation.isSameDocument());
    }

    @Test
    public void isSameDocument_isFalseForFullNavigation() throws InterruptedException {
        Navigation navigation = loadUrlAndGetStartedNavigation(mWebServerUrl);
        Assert.assertFalse(navigation.isSameDocument());
    }

    @Test
    public void isReload_isFalseForInitialLoad() throws InterruptedException {
        Navigation navigation = loadUrlAndGetStartedNavigation(mWebServerUrl);
        Assert.assertFalse(navigation.isReload());
    }

    @Test
    public void isReload_isTrueForReload() throws InterruptedException {
        mWebServer.enqueue(new MockResponse().setBody("test"));
        mWebViewOnUiThread.loadUrl(mWebServerUrl);
        mClient.waitForNavigationComplete();

        mClient.clearRecordedEvents();
        mWebServer.enqueue(new MockResponse().setBody("test"));
        WebkitUtils.onMainThreadSync(mWebView::reload);
        mClient.waitForNavigationComplete();
        Navigation navigation = mClient.waitForNavigationStart();
        Assert.assertTrue(navigation.isReload());
    }

    @Test
    public void isHistory_isTrueForBackForward() throws InterruptedException {
        mWebServer.enqueue(new MockResponse().setBody("test"));
        mWebViewOnUiThread.loadUrlAndWaitForCompletion(mWebServer.url("/1").toString());
        mWebServer.enqueue(new MockResponse().setBody("test"));
        mWebViewOnUiThread.loadUrlAndWaitForCompletion(mWebServer.url("/2").toString());

        mClient.clearRecordedEvents();
        WebkitUtils.onMainThreadSync(mWebView::goBack);
        mClient.waitForNavigationComplete();
        Navigation backNavigation = mClient.waitForNavigationStart();
        Assert.assertTrue(backNavigation.isHistory());
        Assert.assertTrue(backNavigation.isBack());
        Assert.assertFalse(backNavigation.isForward());

        mClient.clearRecordedEvents();
        WebkitUtils.onMainThreadSync(mWebView::goForward);
        mClient.waitForNavigationComplete();
        Navigation forwardNavigation = mClient.waitForNavigationStart();
        Assert.assertTrue(forwardNavigation.isHistory());
        Assert.assertFalse(forwardNavigation.isBack());
        Assert.assertTrue(forwardNavigation.isForward());
    }

    @Test
    public void didCommit_isTrueForSuccessfulNavigation() throws InterruptedException {
        Navigation navigation = loadUrlAndGetCompletedNavigation(mWebServerUrl);
        Assert.assertTrue(navigation.didCommit());
        Assert.assertFalse(navigation.didCommitErrorPage());
        Assert.assertEquals(HTTP_OK, navigation.getStatusCode());
        Assert.assertEquals(mWebServerUrl, navigation.getUrl());
    }

    @Test
    public void didCommit_isFalseForFailedNavigation() throws InterruptedException {
        Navigation navigation = loadUrlAndGetCompletedNavigation(mWebServerUrl,
                new MockResponse().setResponseCode(500));
        Assert.assertTrue(navigation.getStatusCode() >= 500);
    }

    @Test
    public void didCommitErrorPage_isTrueForNotFoundError() throws InterruptedException {
        Navigation navigation = loadUrlAndGetCompletedNavigation(mWebServerUrl,
                new MockResponse().setResponseCode(404));
        Assert.assertTrue(navigation.didCommitErrorPage());
    }

    @Test
    public void isRestore_isFalseForRegularNavigation() throws InterruptedException {
        Navigation navigation = loadUrlAndGetCompletedNavigation(mWebServerUrl);
        Assert.assertFalse(navigation.isRestore());
    }

    @Test
    public void isRestore_isTrueAfterRestoreState() throws InterruptedException {
        mWebServer.enqueue(new MockResponse().setBody("test"));
        mWebViewOnUiThread.loadUrlAndWaitForCompletion(mWebServer.url("/1").toString());
        Bundle bundle = new Bundle();
        WebkitUtils.onMainThreadSync((() -> mWebView.saveState(bundle)));

        mWebServer.enqueue(new MockResponse().setBody("test"));
        mWebViewOnUiThread.loadUrlAndWaitForCompletion(mWebServer.url("/2").toString());

        mClient.clearRecordedEvents();
        WebkitUtils.onMainThreadSync(() -> mWebView.restoreState(bundle));
        Navigation navigation = mClient.waitForNavigationStart();
        Assert.assertTrue(navigation.isRestore());
    }

    private Navigation loadUrlAndGetStartedNavigation(String url) throws InterruptedException {
        mWebServer.enqueue(new MockResponse().setBody("test"));
        mWebViewOnUiThread.loadUrl(url);
        return mClient.waitForNavigationStart();
    }

    private Navigation loadUrlAndGetCompletedNavigation(String url) throws InterruptedException {
        return loadUrlAndGetCompletedNavigation(url, new MockResponse().setBody("test"));
    }

    private Navigation loadUrlAndGetCompletedNavigation(String url, MockResponse response)
            throws InterruptedException {
        mWebServer.enqueue(response);
        mWebViewOnUiThread.loadUrl(url);
        return mClient.waitForNavigationComplete();
    }
}
