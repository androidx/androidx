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

import static androidx.webkit.test.common.WebkitUtils.waitForNextQueueElement;

import android.os.Bundle;
import android.util.Pair;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.test.core.app.ActivityScenario;
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

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import okhttp3.HttpUrl;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class NavigationListenerTest {

    private static class PerformanceMark {
        public final Page page;
        public final String markName;
        public final long markTimeMs;

        PerformanceMark(Page markPage, String name, long timeMs) {
            page = markPage;
            markName = name;
            markTimeMs = timeMs;
        }
    }

    public WebViewOnUiThread mWebViewOnUiThread;
    private WebView mWebView;
    private RecordingNavigationListener mListener;
    private MockWebServer mWebServer;

    @SuppressWarnings("NewClassNamingConvention") // Not a class containing tests.
    private static class RecordingNavigationListener implements NavigationListener {
        public final BlockingQueue<Navigation> mOnNavigationStartedQueue =
                new LinkedBlockingQueue<>();
        public final BlockingQueue<Navigation> mOnNavigationRedirectedQueue =
                new LinkedBlockingQueue<>();
        public final BlockingQueue<Navigation> mOnNavigationCompletedQueue =
                new LinkedBlockingQueue<>();
        public final BlockingQueue<Page> mOnPageDeletedQueue = new LinkedBlockingQueue<>();
        public final BlockingQueue<Page> mOnPageLoadEventFiredQueue = new LinkedBlockingQueue<>();
        public final BlockingQueue<Page> mOnPageDomContentLoadedEventFiredQueue =
                new LinkedBlockingQueue<>();

        public final BlockingQueue<Pair<Page, Long>> mOnFirstContentfulPaintMicrosQueue =
                new LinkedBlockingQueue<>();
        public final BlockingQueue<Pair<Page, Long>> mOnFirstContentfulPaintQueue =
                new LinkedBlockingQueue<>();
        public final BlockingQueue<Pair<Page, Long>> mOnLargestContentfulPaintQueue =
                new LinkedBlockingQueue<>();
        public final BlockingQueue<PerformanceMark> mOnPerformanceMarkQueue =
                new LinkedBlockingQueue<>();


        @Override
        public void onNavigationStarted(@NonNull Navigation navigation) {
            mOnNavigationStartedQueue.add(navigation);
        }

        @Override
        public void onNavigationRedirected(@NonNull Navigation navigation) {
            mOnNavigationRedirectedQueue.add(navigation);
        }

        @Override
        public void onNavigationCompleted(@NonNull Navigation navigation) {
            mOnNavigationCompletedQueue.add(navigation);
        }

        @Override
        public void onPageDeleted(@NonNull Page page) {
            mOnPageDeletedQueue.add(page);

        }

        @Override
        public void onPageLoadEvent(@NonNull Page page) {
            mOnPageLoadEventFiredQueue.add(page);
        }

        @Override
        public void onPageDomContentLoadedEvent(@NonNull Page page) {
            mOnPageDomContentLoadedEventFiredQueue.add(page);

        }

        @Override
        public void onFirstContentfulPaint(@NonNull Page page, long fcpDurationUs) {
            mOnFirstContentfulPaintMicrosQueue.add(new Pair<>(page, fcpDurationUs));
        }

        @Override
        public void onFirstContentfulPaintMillis(@NonNull Page page, long fcpDurationMs) {
            mOnFirstContentfulPaintQueue.add(new Pair<>(page, fcpDurationMs));
        }

        @Override
        public void onLargestContentfulPaintMillis(@NonNull Page page, long lcpDurationMs) {
            mOnLargestContentfulPaintQueue.add(new Pair<>(page, lcpDurationMs));
        }

        @Override
        public void onPerformanceMarkMillis(@NonNull Page page, String markName, long markTimeMs) {
            mOnPerformanceMarkQueue.add(new PerformanceMark(page, markName, markTimeMs));
        }

        public void clearRecordedEvents() {
            mOnNavigationStartedQueue.clear();
            mOnNavigationRedirectedQueue.clear();
            mOnNavigationCompletedQueue.clear();
            mOnPageDeletedQueue.clear();
            mOnPageLoadEventFiredQueue.clear();
            mOnPageDomContentLoadedEventFiredQueue.clear();
            mOnFirstContentfulPaintMicrosQueue.clear();
            mOnFirstContentfulPaintQueue.clear();
            mOnLargestContentfulPaintQueue.clear();
            mOnPerformanceMarkQueue.clear();
        }

    }

    @Before
    public void setUp() throws Exception {
        WebkitUtils.checkFeature((WebViewFeature.NAVIGATION_LISTENER_V1));
        mWebViewOnUiThread = new WebViewOnUiThread();
        mWebServer = new MockWebServer();
        mWebServer.setDispatcher(new TestDispatcher());
        mWebServer.start();

        WebkitUtils.onMainThreadSync(() -> {
            mWebView = mWebViewOnUiThread.getWebViewOnCurrentThread();
            mListener = new RecordingNavigationListener();
            WebViewCompat.addNavigationListener(mWebView, mListener);
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
    public void getPage_returnsNonNullOnCommit() {
        mWebViewOnUiThread.loadUrl(getSuccessUrl());
        Navigation navigation = waitForNextQueueElement(mListener.mOnNavigationCompletedQueue);
        Assert.assertNotNull(navigation.getPage());
    }

    @Test
    public void wasInitiatedByPage_isFalseForLoadUrl() {
        mWebViewOnUiThread.loadUrl(getSuccessUrl());
        Navigation navigation = waitForNextQueueElement(mListener.mOnNavigationStartedQueue);
        Assert.assertFalse(navigation.wasInitiatedByPage());
    }

    @Test
    public void isSameDocument_isTrueForFragmentNavigation() {
        mWebViewOnUiThread.loadUrlAndWaitForCompletion("about:blank");
        mListener.clearRecordedEvents();
        mWebViewOnUiThread.loadUrlAndWaitForCompletion("about:blank#fragment");
        Navigation navigation = waitForNextQueueElement(mListener.mOnNavigationStartedQueue);
        Assert.assertTrue(navigation.isSameDocument());
    }

    @Test
    public void isSameDocument_isFalseForFullNavigation() {
        mWebViewOnUiThread.loadUrl(getSuccessUrl());
        Navigation navigation = waitForNextQueueElement(mListener.mOnNavigationStartedQueue);
        Assert.assertFalse(navigation.isSameDocument());
    }

    @Test
    public void isReload_isFalseForInitialLoad() {
        mWebViewOnUiThread.loadUrl(getSuccessUrl());
        Navigation navigation = waitForNextQueueElement(mListener.mOnNavigationStartedQueue);
        Assert.assertFalse(navigation.isReload());
    }

    @Test
    public void isReload_isTrueForReload() {
        mWebViewOnUiThread.loadUrl(getSuccessUrl());
        waitForNextQueueElement(mListener.mOnNavigationCompletedQueue);

        mListener.clearRecordedEvents();
        WebkitUtils.onMainThreadSync(mWebView::reload);
        waitForNextQueueElement(mListener.mOnNavigationCompletedQueue);
        Navigation navigation = waitForNextQueueElement(mListener.mOnNavigationStartedQueue);
        Assert.assertTrue(navigation.isReload());
    }

    @Test
    public void isHistory_isTrueForBackForward() {
        mWebViewOnUiThread.loadUrlAndWaitForCompletion(mWebServer.url("/1").toString());
        mWebViewOnUiThread.loadUrlAndWaitForCompletion(mWebServer.url("/2").toString());

        mListener.clearRecordedEvents();
        WebkitUtils.onMainThreadSync(mWebView::goBack);
        waitForNextQueueElement(mListener.mOnNavigationCompletedQueue);
        Navigation backNavigation = waitForNextQueueElement(mListener.mOnNavigationStartedQueue);
        Assert.assertTrue(backNavigation.isHistory());
        Assert.assertTrue(backNavigation.isBack());
        Assert.assertFalse(backNavigation.isForward());

        mListener.clearRecordedEvents();
        WebkitUtils.onMainThreadSync(mWebView::goForward);
        waitForNextQueueElement(mListener.mOnNavigationCompletedQueue);
        Navigation forwardNavigation = waitForNextQueueElement(
                mListener.mOnNavigationStartedQueue);
        Assert.assertTrue(forwardNavigation.isHistory());
        Assert.assertFalse(forwardNavigation.isBack());
        Assert.assertTrue(forwardNavigation.isForward());
    }

    @Test
    public void didCommit_isTrueForSuccessfulNavigation() {
        mWebViewOnUiThread.loadUrl(getSuccessUrl());
        Navigation navigation = waitForNextQueueElement(mListener.mOnNavigationCompletedQueue);
        Assert.assertTrue(navigation.didCommit());
        Assert.assertFalse(navigation.didCommitErrorPage());
        Assert.assertEquals(200, navigation.getStatusCode());
        Assert.assertEquals(getSuccessUrl(), navigation.getUrl());
    }

    @Test
    public void didCommit_isFalseForFailedNavigation() {
        mWebViewOnUiThread.loadUrl(getErrorUrl(500));
        Navigation navigation = waitForNextQueueElement(mListener.mOnNavigationCompletedQueue);
        Assert.assertTrue(navigation.getStatusCode() >= 500);
    }

    @Test
    public void didCommitErrorPage_isTrueForNotFoundError() {
        mWebViewOnUiThread.loadUrl(getErrorUrl(404));
        Navigation navigation = waitForNextQueueElement(mListener.mOnNavigationCompletedQueue);
        Assert.assertTrue(navigation.didCommitErrorPage());
    }

    @Test
    public void didCommitErrorPage_webResourceErrorReturned() {
        WebkitUtils.checkFeature(WebViewFeature.NAVIGATION_GET_WEB_RESOURCE_ERROR);
        mWebViewOnUiThread.loadUrl("malformed-url");
        Navigation navigation = waitForNextQueueElement(mListener.mOnNavigationCompletedQueue);
        Assert.assertTrue(navigation.didCommitErrorPage());
        Assert.assertNotNull(navigation.getWebResourceError());
        Assert.assertEquals(WebViewClient.ERROR_HOST_LOOKUP,
                navigation.getWebResourceError().getErrorCode());
    }

    @Test
    public void isRestore_isFalseForRegularNavigation() {
        mWebViewOnUiThread.loadUrl(getSuccessUrl());
        Navigation navigation = waitForNextQueueElement(mListener.mOnNavigationCompletedQueue);
        Assert.assertFalse(navigation.isRestore());
    }

    @Test
    public void isRestore_isTrueAfterRestoreState() {
        mWebViewOnUiThread.loadUrlAndWaitForCompletion(mWebServer.url("/1").toString());
        Bundle bundle = new Bundle();
        WebkitUtils.onMainThreadSync((() -> mWebView.saveState(bundle)));

        mWebViewOnUiThread.loadUrlAndWaitForCompletion(mWebServer.url("/2").toString());

        mListener.clearRecordedEvents();
        WebkitUtils.onMainThreadSync(() -> mWebView.restoreState(bundle));
        Navigation navigation = waitForNextQueueElement(mListener.mOnNavigationStartedQueue);
        Assert.assertTrue(navigation.isRestore());
    }

    @Test
    public void isRedirect_isSameNavigationObject() {
        String redirectUrl = getRedirectUrl(getSuccessUrl());
        mWebViewOnUiThread.loadUrl(redirectUrl);

        Navigation startNavigation = waitForNextQueueElement(mListener.mOnNavigationStartedQueue);
        Navigation redirectNavigation = waitForNextQueueElement(
                mListener.mOnNavigationRedirectedQueue);
        Navigation completedNavigation = waitForNextQueueElement(
                mListener.mOnNavigationCompletedQueue);

        Assert.assertEquals(startNavigation, redirectNavigation);
        Assert.assertEquals(startNavigation, completedNavigation);
        Assert.assertEquals(redirectNavigation, completedNavigation);
    }

    @Test
    public void isSamePageObject() {
        // Success URL is obtained outside of the activity scope in order to avoid a
        // StrictModeViolation for attempting to resolve the hostname on the main thread.
        final String successUrl = getSuccessUrl();
        Page loadedPage;
        try (ActivityScenario<WebViewTestActivity> scenario = ActivityScenario.launch(
                WebViewTestActivity.class)) {
            // The onFirstContentfulPaint event is only triggered if the WebView is attached to
            // the view hierarchy, so this test runs in an Activity.
            scenario.onActivity(activity -> {
                WebView webView = activity.getWebView();
                WebViewCompat.addNavigationListener(webView, mListener);
                webView.loadUrl(successUrl);
            });

            Navigation completedNavigation = waitForNextQueueElement(
                    mListener.mOnNavigationCompletedQueue);

            Page navigationCompletePage = completedNavigation.getPage();
            Assert.assertNotNull(navigationCompletePage);

            loadedPage = waitForNextQueueElement(mListener.mOnPageLoadEventFiredQueue);
            Assert.assertEquals(navigationCompletePage, loadedPage);

            Page domContentLoadedPage = waitForNextQueueElement(
                    mListener.mOnPageDomContentLoadedEventFiredQueue);
            Assert.assertEquals(navigationCompletePage, domContentLoadedPage);

            Pair<Page, Long> firstContentfulPaintMicros = waitForNextQueueElement(
                    mListener.mOnFirstContentfulPaintMicrosQueue);
            Page firstContentfulPaintMicrosPage = firstContentfulPaintMicros.first;
            Assert.assertEquals(navigationCompletePage, firstContentfulPaintMicrosPage);
            Assert.assertTrue(firstContentfulPaintMicros.second > 0);
        }

        // Tearing down the activity and WebView will delete the page.
        Page deletedPage = waitForNextQueueElement(mListener.mOnPageDeletedQueue);
        Assert.assertEquals(loadedPage, deletedPage);
    }

    @Test
    public void isSamePageObject_listenerV2() throws Exception {
        WebkitUtils.checkFeature(WebViewFeature.NAVIGATION_LISTENER_V2);
        // Success URL is obtained outside of the activity scope in order to avoid a
        // StrictModeViolation for attempting to resolve the hostname on the main thread.
        final String successUrl = getSuccessUrl();
        Page loadedPage;
        try (ActivityScenario<WebViewTestActivity> scenario = ActivityScenario.launch(
                WebViewTestActivity.class)) {
            // The onFirstContentfulPaint event is only triggered if the WebView is attached to
            // the view hierarchy, so this test runs in an Activity.
            scenario.onActivity(activity -> {
                WebView webView = activity.getWebView();
                WebViewCompat.addNavigationListener(webView, mListener);
                webView.getSettings().setJavaScriptEnabled(true);
                webView.loadUrl(successUrl);
            });

            Navigation completedNavigation = waitForNextQueueElement(
                    mListener.mOnNavigationCompletedQueue);

            Page navigationCompletePage = completedNavigation.getPage();
            Assert.assertNotNull(navigationCompletePage);

            loadedPage = waitForNextQueueElement(mListener.mOnPageLoadEventFiredQueue);
            Assert.assertEquals(navigationCompletePage, loadedPage);

            Page domContentLoadedPage = waitForNextQueueElement(
                    mListener.mOnPageDomContentLoadedEventFiredQueue);
            Assert.assertEquals(navigationCompletePage, domContentLoadedPage);

            Pair<Page, Long> firstContentfulPaint = waitForNextQueueElement(
                    mListener.mOnFirstContentfulPaintQueue);
            Page firstContentfulPaintPage = firstContentfulPaint.first;
            Assert.assertEquals(navigationCompletePage, firstContentfulPaintPage);
            Assert.assertTrue(firstContentfulPaint.second > 0);

            Pair<Page, Long> largestContentfulPaint = waitForNextQueueElement(
                    mListener.mOnLargestContentfulPaintQueue);
            Page largestContentfulPaintPage = largestContentfulPaint.first;
            Assert.assertEquals(navigationCompletePage, largestContentfulPaintPage);
            Assert.assertTrue(largestContentfulPaint.second > 0);

            PerformanceMark performanceMark = waitForNextQueueElement(
                    mListener.mOnPerformanceMarkQueue);
            Assert.assertEquals(navigationCompletePage, performanceMark.page);
            Assert.assertTrue(performanceMark.markName.equals("testMark"));
            Assert.assertTrue(performanceMark.markTimeMs > 0);
        }

        // Tearing down the activity and WebView will delete the page.
        Page deletedPage = waitForNextQueueElement(mListener.mOnPageDeletedQueue);
        Assert.assertEquals(loadedPage, deletedPage);
    }

    @Test
    public void isSamePageObject_sameUrl() {
        WebkitUtils.checkFeature(WebViewFeature.PAGE_GET_URL);
        final String successUrl = getSuccessUrl();
        mWebViewOnUiThread.loadUrl(successUrl);
        Navigation completedNavigation = waitForNextQueueElement(
                mListener.mOnNavigationCompletedQueue);
        Page navigationCompletePage = completedNavigation.getPage();
        Assert.assertNotNull(navigationCompletePage);
        Assert.assertEquals(navigationCompletePage.getUrl(), successUrl);
    }

    @Test
    public void canRemoveListener() throws InterruptedException {
        WebkitUtils.onMainThreadSync(
                () -> WebViewCompat.removeNavigationListener(mWebView, mListener));
        mWebViewOnUiThread.loadUrlAndWaitForCompletion(getSuccessUrl());
        // Waiting for 200ms for a result to _not_ show up is not ideal, but there is no other
        // way to determine if the client was removed.
        Navigation startedNavigation = mListener.mOnNavigationStartedQueue.poll(200,
                TimeUnit.MILLISECONDS);
        Assert.assertNull("The listener should not receive any navigation events",
                startedNavigation);
    }

    @Test
    public void listenersOnDifferentExecutorsGetSameObjects() {
        final RecordingNavigationListener backgroundListener = new RecordingNavigationListener();
        WebkitUtils.onMainThreadSync(
                () -> WebViewCompat.addNavigationListener(mWebView,
                        Executors.newSingleThreadExecutor(), backgroundListener));
        mWebViewOnUiThread.loadUrl(getSuccessUrl());

        Navigation mainExecutorNavigation = waitForNextQueueElement(
                mListener.mOnNavigationCompletedQueue);
        Navigation backgroundExecutorNavigation = waitForNextQueueElement(
                backgroundListener.mOnNavigationCompletedQueue);

        Assert.assertNotNull(mainExecutorNavigation);
        Assert.assertNotNull(backgroundExecutorNavigation);

        // Multiple listeners should still get the same navigation and page objects.
        Assert.assertEquals(mainExecutorNavigation, backgroundExecutorNavigation);

        Page mainExecutorPage = waitForNextQueueElement(
                mListener.mOnPageLoadEventFiredQueue);
        Page backgroundExecutorPage = waitForNextQueueElement(
                backgroundListener.mOnPageLoadEventFiredQueue);
        Assert.assertNotNull(mainExecutorPage);
        Assert.assertNotNull(backgroundExecutorPage);
        Assert.assertEquals(mainExecutorPage, backgroundExecutorPage);
    }

    @NonNull
    private String getSuccessUrl() {
        return mWebServer.url("/").toString();
    }

    @NonNull
    private String getErrorUrl(int errorCode) {
        return mWebServer.url("/error/" + errorCode).toString();
    }

    @NonNull
    private String getRedirectUrl(String destination) {
        return mWebServer.url("/redirect").newBuilder().addQueryParameter("destination",
                destination).build().toString();
    }

    /**
     * Dispatcher for {@link #mWebServer} that handles the various URL types in this test.
     */
    private static class TestDispatcher extends Dispatcher {

        @NonNull
        @Override
        public MockResponse dispatch(@NonNull RecordedRequest recordedRequest) {
            final MockResponse response = new MockResponse();
            HttpUrl url = recordedRequest.getRequestUrl();
            Assert.assertNotNull(url);
            List<String> segments = url.pathSegments();
            if (segments.size() >= 2 && "error".equals(segments.get(0))) {
                int errorCode = Integer.parseInt(segments.get(1));
                response.setResponseCode(errorCode);
            } else if (!segments.isEmpty() && "redirect".equals(segments.get(0))) {
                String destination = url.queryParameter("destination");
                Assert.assertNotNull(destination);
                response.setResponseCode(301);
                response.setHeader("Location", destination);
            } else {
                response.setHeader("Content-Type", "text/html");
                response.setBody("<!DOCTYPE html>\n"
                        + "<script>performance.mark(\"testMark\");</script>\n"
                        + "<body><h1>Success</h1></body>");
            }
            return response;
        }
    }
}
