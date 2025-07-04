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

@LargeTest
@RunWith(AndroidJUnit4.class)
public class NavigationTest {
    private static final String START_URL = "about:blank";
    private static final String FINAL_URL = "https://www.google.com/";
    private static final String SAME_DOCUMENT_URL = "about:blank#fragment";
    private static final String RELOAD_URL = "https://www.example.com/";
    private static final String HISTORY_URL_1 = "https://www.example1.com/";
    private static final String HISTORY_URL_2 = "https://www.example2.com/";
    private static final int HTTP_OK = 200;

    public WebViewOnUiThread mWebViewOnUiThread;
    private WebView mWebView;
    private RecordingWebNavigationClient mClient;

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
    }

    @Test
    public void getPage_returnsNonNullOnCommit() throws InterruptedException {
        mWebViewOnUiThread.loadUrl(FINAL_URL);
        Navigation navigation = mClient.waitForNavigationComplete();
        Assert.assertNotNull(navigation.getPage());
    }

    @Test
    public void wasInitiatedByPage_isFalseForLoadUrl() throws InterruptedException {
        mWebViewOnUiThread.loadUrl(FINAL_URL);
        Navigation navigation = mClient.waitForNavigationStart();
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
        mWebViewOnUiThread.loadUrl(FINAL_URL);
        Navigation navigation = mClient.waitForNavigationStart();
        Assert.assertFalse(navigation.isSameDocument());
    }

    @Test
    public void isReload_isFalseForInitialLoad() throws InterruptedException {
        mWebViewOnUiThread.loadUrl(FINAL_URL);
        Navigation navigation = mClient.waitForNavigationStart();
        Assert.assertFalse(navigation.isReload());
    }

    @Test
    public void isReload_isTrueForReload() throws InterruptedException {
        mWebViewOnUiThread.loadUrl(RELOAD_URL);
        mClient.waitForNavigationComplete();

        mClient.clearRecordedEvents();
        WebkitUtils.onMainThreadSync(mWebView::reload);
        mClient.waitForNavigationComplete();
        Navigation navigation = mClient.waitForNavigationStart();
        Assert.assertTrue(navigation.isReload());
    }

    @Test
    public void isHistory_isTrueForBackForward() throws InterruptedException {
        mWebViewOnUiThread.loadUrlAndWaitForCompletion(HISTORY_URL_1 + "1");
        mWebViewOnUiThread.loadUrlAndWaitForCompletion(HISTORY_URL_2 + "2");

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
        mWebViewOnUiThread.loadUrl(FINAL_URL);
        Navigation navigation = mClient.waitForNavigationComplete();
        Assert.assertTrue(navigation.didCommit());
        Assert.assertFalse(navigation.didCommitErrorPage());
        Assert.assertEquals(HTTP_OK, navigation.getStatusCode());
        Assert.assertEquals(FINAL_URL, navigation.getUrl());
    }

    @Test
    public void didCommit_isFalseForFailedNavigation() throws InterruptedException {
        mWebViewOnUiThread.loadUrl("https://this-domain-does-not-exist.com");
        Navigation navigation = mClient.waitForNavigationComplete();
        Assert.assertTrue(navigation.getStatusCode() < 400 || navigation.getStatusCode() >= 500);
    }

    @Test
    public void didCommitErrorPage_isTrueForNotFoundError() throws InterruptedException {
        String notFoundUrl = "https://this-domain-does-not-exist-at-all.invalid/";
        mWebViewOnUiThread.loadUrl(notFoundUrl);
        Navigation navigation = mClient.waitForNavigationComplete();
        Assert.assertTrue(navigation.didCommitErrorPage());
    }

    @Test
    public void isRestore_isFalseForRegularNavigation() throws InterruptedException {
        mWebViewOnUiThread.loadUrl(FINAL_URL);
        Navigation navigation = mClient.waitForNavigationComplete();
        Assert.assertFalse(navigation.isRestore());
    }

    @Test
    public void isRestore_isTrueAfterRestoreState() throws InterruptedException {
        mWebViewOnUiThread.loadUrlAndWaitForCompletion(HISTORY_URL_1);
        Bundle bundle = new Bundle();
        WebkitUtils.onMainThreadSync((() -> mWebView.saveState(bundle)));

        mWebViewOnUiThread.loadUrlAndWaitForCompletion(HISTORY_URL_2);

        mClient.clearRecordedEvents();
        WebkitUtils.onMainThreadSync(() -> mWebView.restoreState(bundle));
        Navigation navigation = mClient.waitForNavigationStart();
        Assert.assertTrue(navigation.isRestore());
    }
}
