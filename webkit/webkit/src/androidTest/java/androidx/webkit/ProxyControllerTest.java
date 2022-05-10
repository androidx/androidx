/*
 * Copyright 2018 The Android Open Source Project
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

import static androidx.webkit.ProxyConfig.MATCH_ALL_SCHEMES;
import static androidx.webkit.ProxyConfig.MATCH_HTTP;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import android.os.Build;

import androidx.concurrent.futures.ResolvableFuture;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.MediumTest;
import androidx.test.filters.SdkSuppress;
import androidx.webkit.internal.ProxyControllerImpl;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import okhttp3.mockwebserver.MockWebServer;


@MediumTest
@RunWith(AndroidJUnit4.class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.LOLLIPOP)
public class ProxyControllerTest {
    private WebViewOnUiThread mWebViewOnUiThread;
    private MockWebServer mContentServer;
    private MockWebServer mProxyServer;

    @Before
    public void setUp() throws IOException {
        WebkitUtils.checkFeature(WebViewFeature.PROXY_OVERRIDE);

        mWebViewOnUiThread = new androidx.webkit.WebViewOnUiThread();
        mContentServer = new MockWebServer();
        mProxyServer = new MockWebServer();
        mContentServer.start();
        mProxyServer.start();
    }

    @After
    public void tearDown() throws Exception {
        if (!WebViewFeature.isFeatureSupported(WebViewFeature.PROXY_OVERRIDE)) return;

        clearProxyOverrideSync();
        if (mWebViewOnUiThread != null) {
            mWebViewOnUiThread.cleanUp();
        }
        if (mContentServer != null) {
            mContentServer.shutdown();
        }
        if (mProxyServer != null) {
            mProxyServer.shutdown();
        }
    }

    /**
     * This test should have an equivalent in CTS when this is implemented in the framework.
     *
     * Tests that ClearProxyOverride callback is called even if there is no settings to clear.
     */
    @Test
    public void testCallbacks() throws Exception {
        // Test setProxyOverride's callback
        setProxyOverrideSync(new ProxyConfig.Builder().build());
        // Test clearProxyOverride's callback with a proxy override setting
        clearProxyOverrideSync();
        // Test clearProxyOverride's callback without a proxy override setting
        clearProxyOverrideSync();
        // If we got to this point it means all callbacks were called as expected
    }

    /**
     * This test should have an equivalent in CTS when this is implemented in the framework.
     */
    @Test
    public void testProxyOverride() throws Exception {
        final String contentUrl = mContentServer.url("/").toString();
        final String proxyUrl = mProxyServer.getHostName() + ":" + mProxyServer.getPort();

        // Clear proxy override and load content url
        clearProxyOverrideSync();
        mWebViewOnUiThread.loadUrl(contentUrl);
        assertNotNull(mContentServer.takeRequest(WebkitUtils.TEST_TIMEOUT_MS,
                TimeUnit.MILLISECONDS));
        int contentServerRequestCount = mContentServer.getRequestCount();

        // Set proxy override and load content url
        // Localhost should use proxy with loopback rule
        setProxyOverrideSync(new ProxyConfig.Builder()
                .addProxyRule(proxyUrl)
                .removeImplicitRules()
                .build());
        mWebViewOnUiThread.loadUrl(contentUrl);
        assertNotNull(mProxyServer.takeRequest(WebkitUtils.TEST_TIMEOUT_MS,
                TimeUnit.MILLISECONDS));
        int proxyServerRequestCount = mProxyServer.getRequestCount();
        assertEquals(contentServerRequestCount, mContentServer.getRequestCount());

        // Clear proxy override and load content url
        clearProxyOverrideSync();
        mWebViewOnUiThread.loadUrl(contentUrl);
        assertNotNull(mContentServer.takeRequest(WebkitUtils.TEST_TIMEOUT_MS,
                TimeUnit.MILLISECONDS));
        assertEquals(proxyServerRequestCount, mProxyServer.getRequestCount());
    }

    /**
     * This test should have an equivalent in CTS when this is implemented in the framework.
     */
    @Test
    public void testProxyOverrideLocalhost() throws Exception {
        final String contentUrl = mContentServer.url("/").toString();
        int proxyServerRequestCount = mProxyServer.getRequestCount();

        // Set proxy override and load content url
        // Localhost should not use proxy settings
        setProxyOverrideSync(new ProxyConfig.Builder()
                .addProxyRule(mProxyServer.getHostName() + ":" + mProxyServer.getPort())
                .build());
        mWebViewOnUiThread.loadUrl(contentUrl);
        assertNotNull(mContentServer.takeRequest(WebkitUtils.TEST_TIMEOUT_MS,
                TimeUnit.MILLISECONDS));
        assertEquals(proxyServerRequestCount, mProxyServer.getRequestCount());
    }

    /**
     * This test should have an equivalent in CTS when this is implemented in the framework.
     */
    @Test
    public void testReverseBypass() throws Exception {
        WebkitUtils.checkFeature(WebViewFeature.PROXY_OVERRIDE_REVERSE_BYPASS);

        final String contentUrl = "http://www.example.com";
        final String bypassUrl = "www.example.com";
        int proxyServerRequestCount = mProxyServer.getRequestCount();

        // Set proxy override with reverse bypass and load content url
        // The content url (in the bypass list) should use proxy settings.
        setProxyOverrideSync(new ProxyConfig.Builder()
                .addProxyRule(mProxyServer.getHostName() + ":" + mProxyServer.getPort())
                .addBypassRule(bypassUrl)
                .setReverseBypassEnabled(true)
                .build());
        mWebViewOnUiThread.loadUrl(contentUrl);

        proxyServerRequestCount++;
        assertNotNull(mProxyServer.takeRequest(WebkitUtils.TEST_TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertEquals(proxyServerRequestCount, mProxyServer.getRequestCount());
    }

    /**
     * This test should have an equivalent in CTS when this is implemented in the framework.
     *
     * Enumerates valid patterns to check they are supported.
     */
    @Test
    public void testValidInput() throws Exception {
        ProxyConfig validRules = new ProxyConfig.Builder()
                .addProxyRule("direct://")
                .addProxyRule("www.example.com")
                .addProxyRule("http://www.example.com")
                .addProxyRule("https://www.example.com")
                .addProxyRule("www.example.com:123")
                .addProxyRule("http://www.example.com:123")
                .addProxyRule("10.0.0.1")
                .addProxyRule("10.0.0.1:123")
                .addProxyRule("http://10.0.0.1")
                .addProxyRule("https://10.0.0.1")
                .addProxyRule("http://10.0.0.1:123")
                .addProxyRule("[FE80:CD00:0000:0CDE:1257:0000:211E:729C]")
                .addProxyRule("[FE80:CD00:0:CDE:1257:0:211E:729C]")
                .addBypassRule("www.rule.com")
                .addBypassRule("*.rule.com")
                .addBypassRule("*rule.com")
                .addBypassRule("www.*.com")
                .addBypassRule("www.rule*")
                .build();
        setProxyOverrideSync(validRules);
        // If we got to this point it means our input was accepted as expected
    }

    /**
     * This test should have an equivalent in CTS when this is implemented in the framework.
     */
    @Test
    public void testInvalidProxyUrls() throws Exception {
        String[] invalidProxyUrls = {
                null,
                "", // empty
                "   ",  // spaces only
                "dddf:", // bad port
                "dddd:d", // bad port
                "http://", // no valid host/port
                "http:/", // ambiguous, will fail due to bad port
                "http:", // ambiguous, will fail due to bad port
                "direct://xyz", // direct shouldn't have host/port
        };

        for (String proxyUrl : invalidProxyUrls) {
            try {
                setProxyOverrideSync(new ProxyConfig.Builder()
                        .addProxyRule(proxyUrl).build());
                fail("No exception for invalid proxy url: " + proxyUrl);
            } catch (IllegalArgumentException e) {
                // Expected
            }
        }
    }

    /**
     * This test should have an equivalent in CTS when this is implemented in the framework.
     */
    @Test
    public void testInvalidBypassRules() throws Exception {
        String[] invalidBypassRules = {
                null,
                "", // empty
                "http://", // no valid host/port
                "20:example.com", // bad port
                "example.com:-20" // bad port
        };

        for (String bypassRule : invalidBypassRules) {
            try {
                setProxyOverrideSync(new ProxyConfig.Builder()
                        .addBypassRule(bypassRule).build());
                fail("No exception for invalid bypass rule: " + bypassRule);
            } catch (IllegalArgumentException e) {
                // Expected
            }
        }
    }

    @Test
    public void testProxyRulesToArray() throws Exception {
        String[][] actual;
        String[][] expected;

        actual = ProxyControllerImpl.proxyRulesToStringArray(
                new ProxyConfig.Builder().build().getProxyRules());
        expected = new String[][]{};
        Assert.assertArrayEquals(expected, actual);

        actual = ProxyControllerImpl.proxyRulesToStringArray(
                new ProxyConfig.Builder().addProxyRule("proxy1.com").build().getProxyRules());
        expected = new String[][]{{MATCH_ALL_SCHEMES, "proxy1.com"}};
        Assert.assertArrayEquals(expected, actual);

        actual = ProxyControllerImpl.proxyRulesToStringArray(
                new ProxyConfig.Builder().addProxyRule("proxy1.com").addProxyRule(
                        "proxy2.com").build().getProxyRules());
        expected = new String[][]{{MATCH_ALL_SCHEMES, "proxy1.com"},
                {MATCH_ALL_SCHEMES, "proxy2.com"}};
        Assert.assertArrayEquals(expected, actual);

        actual = ProxyControllerImpl.proxyRulesToStringArray(
                new ProxyConfig.Builder().addProxyRule("proxy1.com").addProxyRule("proxy2.com",
                        MATCH_HTTP).build().getProxyRules());
        expected = new String[][]{{MATCH_ALL_SCHEMES, "proxy1.com"}, {MATCH_HTTP, "proxy2.com"}};
        Assert.assertArrayEquals(expected, actual);
    }

    private void setProxyOverrideSync(final ProxyConfig proxyRules) {
        final ResolvableFuture<Void> future = ResolvableFuture.create();
        ProxyController.getInstance().setProxyOverride(proxyRules, new SynchronousExecutor(),
                () -> future.set(null));
        // This future is used to ensure that setProxyOverride's callback was called
        WebkitUtils.waitForFuture(future);
    }

    private void clearProxyOverrideSync() {
        final ResolvableFuture<Void> future = ResolvableFuture.create();
        ProxyController.getInstance().clearProxyOverride(
                new SynchronousExecutor(), () -> future.set(null));
        // This future is used to ensure that clearProxyOverride's callback was called
        WebkitUtils.waitForFuture(future);
    }

    static class SynchronousExecutor implements Executor {
        @Override
        public void execute(Runnable r) {
            r.run();
        }
    }
}
