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

import android.os.Handler;
import android.os.Looper;

import androidx.concurrent.futures.ResolvableFuture;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.MediumTest;
import androidx.webkit.internal.WebViewGlueCommunicator;
import androidx.webkit.test.common.WebViewOnUiThread;
import androidx.webkit.test.common.WebkitUtils;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.Field;
import java.util.Objects;
import java.util.concurrent.Executors;

/**
 * Tests for behaviours related to
 * {@link WebViewCompat#startUpWebView(android.content.Context, WebViewStartUpConfig, WebViewCompat.WebViewStartUpCallback)}
 *
 * NOTE: Unfortunately, the test infra does not allow spinning up a new process for each test.
 * Therefore, WebView started up in one test causes assumption failures in others
 * (See b/376656739).
 * For the time being, please run each test thoroughly locally till the above bug is fixed.
 */
@MediumTest
@RunWith(AndroidJUnit4.class)
public class AsyncStartUpTest {
    /**
     * Tests that
     * {@link WebViewCompat#startUpWebView(android.content.Context, WebViewStartUpConfig, WebViewCompat.WebViewStartUpCallback)}
     * has loaded WebView when `onSuccess` is triggered.
     */
    @Test
    @MediumTest
    @Ignore("b/376656739")
    public void testAsyncStartUp_onSuccessLoadsWebView() throws Throwable {
        Assume.assumeFalse(webViewCurrentlyLoaded());
        WebViewStartUpConfig config = new WebViewStartUpConfig.Builder(
                Executors.newSingleThreadExecutor()).build();
        final ResolvableFuture<WebViewStartUpResult> startUpFinishedFuture =
                ResolvableFuture.create();

        WebViewCompat.startUpWebView(ApplicationProvider.getApplicationContext(), config,
                startUpFinishedFuture::set);
        // Wait until the callback has triggered.
        WebkitUtils.waitForFuture(startUpFinishedFuture);

        Assert.assertTrue(webViewCurrentlyLoaded());
    }

    /**
     * Tests that
     * {@link WebViewCompat#startUpWebView(android.content.Context, WebViewStartUpConfig, WebViewCompat.WebViewStartUpCallback)}
     * returns timing info as part of the startup result.
     */
    @Test
    @MediumTest
    @Ignore("b/376656739")
    public void testAsyncStartUp_onSuccessReturnsTimingInfo() throws Throwable {
        Assume.assumeFalse(webViewCurrentlyLoaded());
        WebViewStartUpConfig config = new WebViewStartUpConfig.Builder(
                Executors.newSingleThreadExecutor()).build();
        final ResolvableFuture<WebViewStartUpResult> startUpFinishedFuture =
                ResolvableFuture.create();

        WebViewCompat.startUpWebView(ApplicationProvider.getApplicationContext(), config,
                startUpFinishedFuture::set);
        // Wait until the callback has triggered.
        WebViewStartUpResult result = WebkitUtils.waitForFuture(startUpFinishedFuture);

        Assert.assertNotNull(result);
        Assert.assertNotNull(result.getTotalTimeInUiThreadMillis());
        Assert.assertNotNull(result.getMaxTimePerTaskInUiThreadMillis());
    }

    /**
     * Tests that
     * {@link WebViewCompat#startUpWebView(android.content.Context, WebViewStartUpConfig, WebViewCompat.WebViewStartUpCallback)}
     * returns NO blocking startup location if WebView is started up by calling `startUpWebView()`.
     */
    @Test
    @MediumTest
    @Ignore("b/376656739")
    public void testAsyncStartUp_onSuccessReturnsNoBlockingLocationWithoutBlockingInit()
            throws Throwable {
        Assume.assumeFalse(webViewCurrentlyLoaded());
        WebViewStartUpConfig config = new WebViewStartUpConfig.Builder(
                Executors.newSingleThreadExecutor()).build();
        final ResolvableFuture<WebViewStartUpResult> startUpFinishedFuture =
                ResolvableFuture.create();

        WebViewCompat.startUpWebView(ApplicationProvider.getApplicationContext(), config,
                startUpFinishedFuture::set);
        // Wait until the callback has triggered.
        WebViewStartUpResult result = WebkitUtils.waitForFuture(startUpFinishedFuture);

        Assert.assertNotNull(result);
        Assert.assertEquals(0,
                Objects.requireNonNull(result.getBlockingStartUpLocations()).size());
    }

    /**
     * Tests that
     * {@link WebViewCompat#startUpWebView(android.content.Context, WebViewStartUpConfig, WebViewCompat.WebViewStartUpCallback)}
     * returns a blocking startup location if provider init is triggered on the main looper.
     */
    @Test
    @MediumTest
    @Ignore("b/376656739")
    public void testAsyncStartUp_returnsSingleBlockingLocationWithProviderInitOnMainLooper()
            throws Throwable {
        Assume.assumeFalse(webViewCurrentlyLoaded());
        WebViewStartUpConfig config = new WebViewStartUpConfig.Builder(
                Executors.newSingleThreadExecutor()).build();
        final ResolvableFuture<WebViewStartUpResult> startUpFinishedFuture =
                ResolvableFuture.create();

        // Triggers provider init.
        new Handler(Looper.getMainLooper()).post(WebViewGlueCommunicator::getWebViewClassLoader);
        WebViewCompat.startUpWebView(ApplicationProvider.getApplicationContext(), config,
                startUpFinishedFuture::set);
        // Wait until the callback has triggered.
        WebViewStartUpResult result = WebkitUtils.waitForFuture(startUpFinishedFuture);
        Assert.assertNotNull(result);
        Assert.assertEquals(1,
                Objects.requireNonNull(result.getBlockingStartUpLocations()).size());
        Assert.assertTrue(result.getBlockingStartUpLocations().get(0).getStackInformation()
                .contains("Provider init"));
    }

    /**
     * Tests that
     * {@link WebViewCompat#startUpWebView(android.content.Context, WebViewStartUpConfig, WebViewCompat.WebViewStartUpCallback)}
     * returns a blocking startup location if Chromium init blocks the UI thread.
     */
    @Test
    @MediumTest
    @Ignore("b/376656739")
    public void testAsyncStartUp_returnsSingleBlockingLocationWithChromiumInitOnUiThread()
            throws Throwable {
        Assume.assumeFalse(webViewCurrentlyLoaded());
        WebViewStartUpConfig config = new WebViewStartUpConfig.Builder(
                Executors.newSingleThreadExecutor()).build();
        final ResolvableFuture<WebViewStartUpResult> startUpFinishedFuture =
                ResolvableFuture.create();

        WebViewGlueCommunicator.getWebViewClassLoader();
        try (WebViewOnUiThread webViewOnUiThread = new WebViewOnUiThread()) {
            WebViewCompat.startUpWebView(ApplicationProvider.getApplicationContext(), config,
                    startUpFinishedFuture::set);
            // Wait until the callback has triggered.
            WebViewStartUpResult result = WebkitUtils.waitForFuture(startUpFinishedFuture);

            Assert.assertNotNull(result);
            Assert.assertEquals(1,
                    Objects.requireNonNull(result.getBlockingStartUpLocations()).size());
            Assert.assertTrue(result.getBlockingStartUpLocations().get(0).getStackInformation()
                    .contains("Chromium init"));
        }
    }

    /**
     * Tests that
     * {@link WebViewCompat#startUpWebView(android.content.Context, WebViewStartUpConfig, WebViewCompat.WebViewStartUpCallback)}
     * returns two blocking startup locations if provider init happens on the main looper and
     * Chromium init blocks the UI thread.
     */
    @Test
    @MediumTest
    @Ignore("b/376656739")
    public void testAsyncStartUp_returnsBlockingLocationsWithWebViewInitOnUiThread()
            throws Throwable {
        Assume.assumeFalse(webViewCurrentlyLoaded());
        WebViewStartUpConfig config = new WebViewStartUpConfig.Builder(
                Executors.newSingleThreadExecutor()).build();
        final ResolvableFuture<WebViewStartUpResult> startUpFinishedFuture =
                ResolvableFuture.create();
        try (WebViewOnUiThread webViewOnUiThread = new WebViewOnUiThread()) {
            WebViewCompat.startUpWebView(ApplicationProvider.getApplicationContext(), config,
                    startUpFinishedFuture::set);
            // Wait until the callback has triggered.
            WebViewStartUpResult result = WebkitUtils.waitForFuture(startUpFinishedFuture);

            Assert.assertNotNull(result);
            Assert.assertEquals(2,
                    Objects.requireNonNull(result.getBlockingStartUpLocations()).size());
            Assert.assertTrue(result.getBlockingStartUpLocations().get(0).getStackInformation()
                    .contains("Chromium init"));
            Assert.assertTrue(result.getBlockingStartUpLocations().get(1).getStackInformation()
                    .contains("Provider init"));
        }
    }

    /**
     * Tests that
     * {@link WebViewCompat#startUpWebView(android.content.Context, WebViewStartUpConfig, WebViewCompat.WebViewStartUpCallback)}
     * returns the same information when triggered multiple times.
     */
    @Test
    @MediumTest
    @Ignore("b/376656739")
    public void testAsyncStartUp_returnsSameInfoForMultipleCalls() throws Throwable {
        Assume.assumeFalse(webViewCurrentlyLoaded());
        WebViewStartUpConfig config = new WebViewStartUpConfig.Builder(
                Executors.newSingleThreadExecutor()).build();
        final ResolvableFuture<WebViewStartUpResult> startUpFinishedFuture1 =
                ResolvableFuture.create();
        final ResolvableFuture<WebViewStartUpResult> startUpFinishedFuture2 =
                ResolvableFuture.create();
        final ResolvableFuture<WebViewStartUpResult> startUpFinishedFuture3 =
                ResolvableFuture.create();

        // Invoke provider init on main looper.
        new Handler(Looper.getMainLooper()).post(WebViewGlueCommunicator::getWebViewClassLoader);
        WebViewCompat.startUpWebView(ApplicationProvider.getApplicationContext(), config,
                startUpFinishedFuture1::set);
        WebViewCompat.startUpWebView(ApplicationProvider.getApplicationContext(), config,
                startUpFinishedFuture2::set);
        WebViewCompat.startUpWebView(ApplicationProvider.getApplicationContext(), config,
                startUpFinishedFuture3::set);
        // Wait until the callback has triggered.
        WebViewStartUpResult result1 = WebkitUtils.waitForFuture(startUpFinishedFuture1);
        WebViewStartUpResult result2 = WebkitUtils.waitForFuture(startUpFinishedFuture2);
        WebViewStartUpResult result3 = WebkitUtils.waitForFuture(startUpFinishedFuture3);

        Assert.assertEquals(result1.getTotalTimeInUiThreadMillis(),
                result2.getTotalTimeInUiThreadMillis());
        Assert.assertEquals(result2.getTotalTimeInUiThreadMillis(),
                result3.getTotalTimeInUiThreadMillis());
        Assert.assertEquals(result1.getMaxTimePerTaskInUiThreadMillis(),
                result2.getMaxTimePerTaskInUiThreadMillis());
        Assert.assertEquals(result2.getMaxTimePerTaskInUiThreadMillis(),
                result3.getMaxTimePerTaskInUiThreadMillis());
        Assert.assertEquals(Objects.requireNonNull(result1.getBlockingStartUpLocations()).size(),
                Objects.requireNonNull(result2.getBlockingStartUpLocations()).size());
        Assert.assertEquals(Objects.requireNonNull(result2.getBlockingStartUpLocations()).size(),
                Objects.requireNonNull(result3.getBlockingStartUpLocations()).size());
    }

    /**
     * Tests that
     * {@link WebViewCompat#startUpWebView(android.content.Context, WebViewStartUpConfig, WebViewCompat.WebViewStartUpCallback)}
     * with {@link WebViewStartUpConfig.Builder#setShouldRunUiThreadStartUpTasks()} as
     * {@code false} has loaded WebView when `onSuccess` is triggered and the resulting diagnostic
     * information are null which implies that Chromium init hasn't taken place.
     */
    @Test
    @MediumTest
    @Ignore("b/376656739")
    public void
    testAsyncStartUp_withoutRunningUiThreadStartUpLoadsWebViewWithoutStartingChromium()
            throws Throwable {
        Assume.assumeFalse(webViewCurrentlyLoaded());
        WebViewStartUpConfig config = new WebViewStartUpConfig.Builder(
                Executors.newSingleThreadExecutor())
                .setShouldRunUiThreadStartUpTasks(false).build();
        final ResolvableFuture<WebViewStartUpResult> startUpFinishedFuture =
                ResolvableFuture.create();

        WebViewCompat.startUpWebView(ApplicationProvider.getApplicationContext(), config,
                startUpFinishedFuture::set);
        // Wait until the callback has triggered.
        WebViewStartUpResult result = WebkitUtils.waitForFuture(startUpFinishedFuture);

        Assert.assertTrue(webViewCurrentlyLoaded());
        Assert.assertNull(result.getTotalTimeInUiThreadMillis());
        Assert.assertNull(result.getMaxTimePerTaskInUiThreadMillis());
    }

    /**
     * Tests that
     * {@link WebViewCompat#startUpWebView(android.content.Context, WebViewStartUpConfig, WebViewCompat.WebViewStartUpCallback)}
     * with {@link WebViewStartUpConfig.Builder#setShouldRunUiThreadStartUpTasks()} as
     * {@code false} returns a blocking startup location if provider init is triggered on the main
     * looper.
     */
    @Test
    @MediumTest
    @Ignore("b/376656739")
    public void
    testAsyncStartUp_withoutRunningUiThreadStartUpReturnsBlockingLocationWithProviderInit()
            throws Throwable {
        Assume.assumeFalse(webViewCurrentlyLoaded());
        WebViewStartUpConfig config = new WebViewStartUpConfig.Builder(
                Executors.newSingleThreadExecutor())
                .setShouldRunUiThreadStartUpTasks(false).build();
        final ResolvableFuture<WebViewStartUpResult> startUpFinishedFuture =
                ResolvableFuture.create();

        // Triggers provider init.
        new Handler(Looper.getMainLooper()).post(WebViewGlueCommunicator::getWebViewClassLoader);
        WebViewCompat.startUpWebView(ApplicationProvider.getApplicationContext(), config,
                startUpFinishedFuture::set);
        // Wait until the callback has triggered.
        WebViewStartUpResult result = WebkitUtils.waitForFuture(startUpFinishedFuture);

        Assert.assertNull(result.getTotalTimeInUiThreadMillis());
        Assert.assertNull(result.getMaxTimePerTaskInUiThreadMillis());
        Assert.assertNotNull(result);
        Assert.assertEquals(1,
                Objects.requireNonNull(result.getBlockingStartUpLocations()).size());
        Assert.assertTrue(result.getBlockingStartUpLocations().get(0).getStackInformation()
                .contains("Provider init"));
    }

    /**
     * Checks if WebView is currently loaded in the current process.
     */
    private static boolean webViewCurrentlyLoaded() {
        // TODO(crbug.com/1355297): This is racy but it is the best we can do for now since we can't
        //  access the lock for sProviderInstance in WebView. Evaluate a framework path for
        //  ProcessGlobalConfig.
        try {
            Class<?> webViewFactoryClass = Class.forName("android.webkit.WebViewFactory");
            Field providerInstanceField =
                    webViewFactoryClass.getDeclaredField("sProviderInstance");
            providerInstanceField.setAccessible(true);
            return providerInstanceField.get(null) != null;
        } catch (Exception e) {
            // This means WebViewFactory was not found or sProviderInstance was not found within
            // the class. If that is true, WebView doesn't seem to be loaded.
            return false;
        }
    }
}
