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
import android.webkit.WebSettings;

import androidx.concurrent.futures.ResolvableFuture;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.MediumTest;
import androidx.webkit.internal.StartupFeatures;
import androidx.webkit.internal.WebViewFeatureInternal;
import androidx.webkit.internal.WebViewGlueCommunicator;
import androidx.webkit.test.common.WebViewOnUiThread;
import androidx.webkit.test.common.WebkitUtils;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.Field;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

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

        // We are running the assumption here so that checking for the feature doesn't
        // interfere with our test as feature checks run provider init.
        Assume.assumeTrue(WebViewFeatureInternal.ASYNC_WEBVIEW_STARTUP.isSupportedByWebView());
        Assert.assertNotNull(result);
        Assert.assertNotNull(result.getTotalTimeInUiThreadMillis());
        Assert.assertNotNull(result.getMaxTimePerTaskInUiThreadMillis());
    }

    /**
     * Tests that
     * {@link WebViewCompat#startUpWebView(android.content.Context, WebViewStartUpConfig, WebViewCompat.WebViewStartUpCallback)}
     * returns NO startup locations if WebView is started up by calling `startUpWebView()`.
     */
    @Test
    @MediumTest
    public void testAsyncStartUp_onSuccessReturnsNoStartupLocationWithStartUpApi()
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

        // We are running the assumption here so that checking for the feature doesn't
        // interfere with our test as feature checks run provider init.
        Assume.assumeTrue(WebViewFeatureInternal.ASYNC_WEBVIEW_STARTUP.isSupportedByWebView());
        Assert.assertNotNull(result);
        Assert.assertEquals(0,
                Objects.requireNonNull(result.getUiThreadBlockingStartUpLocations()).size());
        if (WebViewFeatureInternal
                .ASYNC_WEBVIEW_STARTUP_ASYNC_STARTUP_LOCATIONS.isSupportedByWebView()) {
            Assert.assertEquals(0,
                    Objects.requireNonNull(result.getNonUiThreadBlockingStartUpLocations()).size());
        } else {
            Assert.assertNull(result.getNonUiThreadBlockingStartUpLocations());
        }
    }

    /**
     * Tests that
     * {@link WebViewCompat#startUpWebView(android.content.Context, WebViewStartUpConfig, WebViewCompat.WebViewStartUpCallback)}
     * returns appropriate startup locations if provider init is triggered on the main looper.
     */
    @Test
    @MediumTest
    public void testAsyncStartUp_returnsAppropriateStartupLocationWithProviderInitOnMainLooper()
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

        // We are running the assumption here so that checking for the feature doesn't
        // interfere with our test as feature checks run provider init.
        Assume.assumeTrue(WebViewFeatureInternal.ASYNC_WEBVIEW_STARTUP.isSupportedByWebView());
        Assert.assertNotNull(result);
        Assert.assertEquals(1,
                Objects.requireNonNull(result.getUiThreadBlockingStartUpLocations()).size());
        Assert.assertTrue(result.getUiThreadBlockingStartUpLocations().get(0).getStackInformation()
                .contains("Provider init"));
        if (WebViewFeatureInternal
                .ASYNC_WEBVIEW_STARTUP_ASYNC_STARTUP_LOCATIONS.isSupportedByWebView()) {
            Assert.assertEquals(0,
                    Objects.requireNonNull(result.getNonUiThreadBlockingStartUpLocations()).size());
        } else {
            Assert.assertNull(result.getNonUiThreadBlockingStartUpLocations());
        }
    }

    /**
     * Tests that
     * {@link WebViewCompat#startUpWebView(android.content.Context, WebViewStartUpConfig, WebViewCompat.WebViewStartUpCallback)}
     * returns appropriate startups location if Chromium init blocks the UI thread.
     */
    @Test
    @MediumTest
    public void testAsyncStartUp_returnsAppropriateStartupLocationWithChromiumInitOnUiThread()
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

            // We are running the assumption here so that checking for the feature doesn't
            // interfere with our test as feature checks run provider init.
            Assume.assumeTrue(WebViewFeatureInternal.ASYNC_WEBVIEW_STARTUP.isSupportedByWebView());
            Assert.assertNotNull(result);
            Assert.assertEquals(1,
                    Objects.requireNonNull(result.getUiThreadBlockingStartUpLocations()).size());
            Assert.assertTrue(
                    result.getUiThreadBlockingStartUpLocations().get(0).getStackInformation()
                    .contains("Chromium init"));
            if (WebViewFeatureInternal
                    .ASYNC_WEBVIEW_STARTUP_ASYNC_STARTUP_LOCATIONS.isSupportedByWebView()) {
                Assert.assertEquals(0,
                        Objects.requireNonNull(
                                result.getNonUiThreadBlockingStartUpLocations()).size());
            } else {
                Assert.assertNull(result.getNonUiThreadBlockingStartUpLocations());
            }
        }
    }

    /**
     * Tests that
     * {@link WebViewCompat#startUpWebView(android.content.Context, WebViewStartUpConfig, WebViewCompat.WebViewStartUpCallback)}
     * returns appropriate startup locations if provider init happens on the main looper and
     * Chromium init blocks the UI thread.
     */
    @Test
    @MediumTest
    public void testAsyncStartUp_returnsAppropriateStartupLocationWithWebViewInitOnUiThread()
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

            // We are running the assumption here so that checking for the feature doesn't
            // interfere with our test as feature checks run provider init.
            Assume.assumeTrue(WebViewFeatureInternal.ASYNC_WEBVIEW_STARTUP.isSupportedByWebView());
            Assert.assertNotNull(result);
            Assert.assertEquals(2,
                    Objects.requireNonNull(result.getUiThreadBlockingStartUpLocations()).size());
            Assert.assertTrue(
                    result.getUiThreadBlockingStartUpLocations().get(0).getStackInformation()
                    .contains("Chromium init"));
            Assert.assertTrue(
                    result.getUiThreadBlockingStartUpLocations().get(1).getStackInformation()
                    .contains("Provider init"));
            if (WebViewFeatureInternal
                    .ASYNC_WEBVIEW_STARTUP_ASYNC_STARTUP_LOCATIONS.isSupportedByWebView()) {
                Assert.assertEquals(0,
                        Objects.requireNonNull(
                                result.getNonUiThreadBlockingStartUpLocations()).size());
            } else {
                Assert.assertNull(result.getNonUiThreadBlockingStartUpLocations());
            }
        }
    }

    /**
     * Tests that
     * {@link WebViewCompat#startUpWebView(android.content.Context, WebViewStartUpConfig, WebViewCompat.WebViewStartUpCallback)}
     * returns the same information when triggered multiple times.
     */
    @Test
    @MediumTest
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

        // We are running the assumption here so that checking for the feature doesn't
        // interfere with our test as feature checks run provider init.
        Assume.assumeTrue(WebViewFeatureInternal.ASYNC_WEBVIEW_STARTUP.isSupportedByWebView());
        Assert.assertEquals(result1.getTotalTimeInUiThreadMillis(),
                result2.getTotalTimeInUiThreadMillis());
        Assert.assertEquals(result2.getTotalTimeInUiThreadMillis(),
                result3.getTotalTimeInUiThreadMillis());
        Assert.assertEquals(result1.getMaxTimePerTaskInUiThreadMillis(),
                result2.getMaxTimePerTaskInUiThreadMillis());
        Assert.assertEquals(result2.getMaxTimePerTaskInUiThreadMillis(),
                result3.getMaxTimePerTaskInUiThreadMillis());
        Assert.assertEquals(
                Objects.requireNonNull(result1.getUiThreadBlockingStartUpLocations()).size(),
                Objects.requireNonNull(result2.getUiThreadBlockingStartUpLocations()).size());
        Assert.assertEquals(
                Objects.requireNonNull(result2.getUiThreadBlockingStartUpLocations()).size(),
                Objects.requireNonNull(result3.getUiThreadBlockingStartUpLocations()).size());
        if (WebViewFeatureInternal
                .ASYNC_WEBVIEW_STARTUP_ASYNC_STARTUP_LOCATIONS.isSupportedByWebView()) {
            Assert.assertEquals(
                    0,
                    Objects.requireNonNull(
                            result1.getNonUiThreadBlockingStartUpLocations()).size());
            Assert.assertEquals(
                    0,
                    Objects.requireNonNull(
                            result2.getNonUiThreadBlockingStartUpLocations()).size());
            Assert.assertEquals(
                    0,
                    Objects.requireNonNull(
                            result3.getNonUiThreadBlockingStartUpLocations()).size());
        } else {
            Assert.assertNull(result1.getNonUiThreadBlockingStartUpLocations());
            Assert.assertNull(result2.getNonUiThreadBlockingStartUpLocations());
            Assert.assertNull(result3.getNonUiThreadBlockingStartUpLocations());
        }
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

        // We are running the assumption here so that checking for the feature doesn't
        // interfere with our test as feature checks run provider init.
        Assume.assumeTrue(WebViewFeatureInternal.ASYNC_WEBVIEW_STARTUP.isSupportedByWebView());
        Assert.assertTrue(webViewCurrentlyLoaded());
        Assert.assertNull(result.getTotalTimeInUiThreadMillis());
        Assert.assertNull(result.getMaxTimePerTaskInUiThreadMillis());
    }

    /**
     * Verifies that when {@link WebViewCompat#startUpWebView} is called with a configuration
     * specifying a custom profile, only that profile is created and the default profile is not.
     * TODO(b/300281790): Write tests for the other scenarios when we get the ability to unload
     * profiles from memory.
     */
    @Test
    @MediumTest
    public void testAsyncStartUp_withCreatingCustomProfile_createsRequestedProfiles() {
        Assume.assumeFalse(webViewCurrentlyLoaded());

        WebkitUtils.checkStartupFeature(ApplicationProvider.getApplicationContext(),
                StartupFeatures.STARTUP_FEATURE_SET_PROFILES_TO_LOAD);
        WebViewStartUpConfig config = new WebViewStartUpConfig.Builder(
                Executors.newSingleThreadExecutor())
                .setShouldRunUiThreadStartUpTasks(true).setProfilesToLoadDuringStartup(
                        Set.of("TestX", "TestY")).build();
        final ResolvableFuture<WebViewStartUpResult> startUpFinishedFuture =
                ResolvableFuture.create();

        WebViewCompat.startUpWebView(ApplicationProvider.getApplicationContext(), config,
                startUpFinishedFuture::set);
        // Wait until the callback has triggered.
        WebkitUtils.waitForFuture(startUpFinishedFuture);

        // We are running the assumption here so that checking for the feature doesn't
        // interfere with our test as feature checks run provider init.
        Assume.assumeTrue(WebViewFeatureInternal.ASYNC_WEBVIEW_STARTUP.isSupportedByWebView());
        WebkitUtils.onMainThreadSync(() -> {
            Assert.assertTrue(webViewCurrentlyLoaded());
            Assert.assertTrue(ProfileStore.getInstance().getAllProfileNames().contains("TestX"));
            Assert.assertTrue(ProfileStore.getInstance().getAllProfileNames().contains("TestY"));
        });
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

        // We are running the assumption here so that checking for the feature doesn't
        // interfere with our test as feature checks run provider init.
        Assume.assumeTrue(WebViewFeatureInternal.ASYNC_WEBVIEW_STARTUP.isSupportedByWebView());
        Assert.assertNull(result.getTotalTimeInUiThreadMillis());
        Assert.assertNull(result.getMaxTimePerTaskInUiThreadMillis());
        Assert.assertNotNull(result);
        Assert.assertEquals(1,
                Objects.requireNonNull(result.getUiThreadBlockingStartUpLocations()).size());
        Assert.assertTrue(result.getUiThreadBlockingStartUpLocations().get(0).getStackInformation()
                .contains("Provider init"));
    }

    /**
     * Tests that
     * {@link WebViewCompat#startUpWebView(android.content.Context, WebViewStartUpConfig, WebViewCompat.WebViewStartUpCallback)}
     * returns async startup locations if started up asynchronously.
     */
    @Test
    @MediumTest
    public void testAsyncStartUp_returnsAsyncLocationsWhenInitializedAsync()
            throws Throwable {
        Assume.assumeFalse(webViewCurrentlyLoaded());

        CountDownLatch latch = new CountDownLatch(1);
        Executors.newSingleThreadExecutor().execute(() -> {
                    WebSettings.getDefaultUserAgent(ApplicationProvider.getApplicationContext());
                    latch.countDown();
                }
        );
        WebViewStartUpConfig config = new WebViewStartUpConfig.Builder(
                Executors.newSingleThreadExecutor()).build();
        final ResolvableFuture<WebViewStartUpResult> startUpFinishedFuture =
                ResolvableFuture.create();
        Assert.assertTrue(latch.await(10, TimeUnit.SECONDS));
        WebViewCompat.startUpWebView(ApplicationProvider.getApplicationContext(), config,
                startUpFinishedFuture::set);
        // Wait until the callback has triggered.
        WebViewStartUpResult result = WebkitUtils.waitForFuture(startUpFinishedFuture);

        // We are running the assumption here so that checking for the feature doesn't
        // interfere with our test as feature checks run provider init.
        Assume.assumeTrue(WebViewFeatureInternal
                .ASYNC_WEBVIEW_STARTUP_ASYNC_STARTUP_LOCATIONS.isSupportedByWebView());
        Assert.assertNotNull(result);
        Assert.assertNotNull(result.getTotalTimeInUiThreadMillis());
        Assert.assertNotNull(result.getMaxTimePerTaskInUiThreadMillis());
        Assert.assertNotNull(result.getNonUiThreadBlockingStartUpLocations());
        Assert.assertEquals(1, result.getNonUiThreadBlockingStartUpLocations().size());
        Assert.assertTrue(
                result.getNonUiThreadBlockingStartUpLocations().get(0).getStackInformation()
                .contains("Chromium init"));
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
