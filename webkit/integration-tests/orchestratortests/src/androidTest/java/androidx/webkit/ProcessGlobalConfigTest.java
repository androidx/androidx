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

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.MediumTest;
import androidx.webkit.test.common.WebViewOnUiThread;
import androidx.webkit.test.common.WebkitUtils;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.lang.reflect.Field;

@MediumTest
@RunWith(AndroidJUnit4.class)
public class ProcessGlobalConfigTest {

    @Before
    public void setUp() {
        Assume.assumeFalse("WebView should not be loaded before ProcessGlobalConfig tests run",
                webViewCurrentlyLoaded());
    }

    private static boolean webViewCurrentlyLoaded() {
        try {
            Class<?> webViewFactoryClass = Class.forName("android.webkit.WebViewFactory");
            Field providerInstanceField =
                    webViewFactoryClass.getDeclaredField("sProviderInstance");
            providerInstanceField.setAccessible(true);
            return providerInstanceField.get(null) != null;
        } catch (Exception e) {
            return false;
        }
    }

    @Test
    @MediumTest
    public void testApplyTwiceThrowsException() throws Exception {
        ProcessGlobalConfig config1 = new ProcessGlobalConfig();
        ProcessGlobalConfig.apply(config1);

        ProcessGlobalConfig config2 = new ProcessGlobalConfig();
        Assert.assertThrows(
                "Expected IllegalStateException when calling apply twice",
                IllegalStateException.class,
                () -> ProcessGlobalConfig.apply(config2));
    }

    @Test
    @MediumTest
    public void testApplyAfterWebViewLoadedThrowsException() throws Exception {
        try (WebViewOnUiThread webViewOnUiThread = new WebViewOnUiThread()) {
            ProcessGlobalConfig config = new ProcessGlobalConfig();
            Assert.assertThrows(
                    "Expected IllegalStateException when calling apply after WebView loaded",
                    IllegalStateException.class,
                    () -> ProcessGlobalConfig.apply(config));
        }
    }

    @Test
    @MediumTest
    public void testSetDataDirectorySuffix() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        WebkitUtils.checkStartupFeature(context,
                WebViewFeature.STARTUP_FEATURE_SET_DATA_DIRECTORY_SUFFIX);

        String suffix = "test_orchestrator_suffix";
        ProcessGlobalConfig config = new ProcessGlobalConfig();
        config.setDataDirectorySuffix(context, suffix);
        ProcessGlobalConfig.apply(config);

        File suffixedDir = new File(context.getDataDir(), "app_webview_" + suffix);
        WebkitUtils.recursivelyDeleteFile(suffixedDir);
        Assert.assertFalse("WebView directory exists before test", suffixedDir.exists());

        try (WebViewOnUiThread webViewOnUiThread = new WebViewOnUiThread()) {
            Assert.assertTrue(
                    "WebView suffixed directory should be created when creating a WebView"
                            + " instance.",
                    suffixedDir.exists());
        }
    }

    @Test
    @MediumTest
    @SuppressWarnings("deprecation")
    public void testSetDirectoryBasePaths() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        WebkitUtils.checkStartupFeature(context,
                WebViewFeature.STARTUP_FEATURE_SET_DIRECTORY_BASE_PATHS);

        File dataDir = new File(context.getDataDir(), "orchestrator_data_base");
        File cacheDir = new File(context.getDataDir(), "orchestrator_cache_base");
        WebkitUtils.recursivelyDeleteFile(dataDir);
        WebkitUtils.recursivelyDeleteFile(cacheDir);

        ProcessGlobalConfig config = new ProcessGlobalConfig();
        config.setDirectoryBasePaths(context, dataDir, cacheDir);
        ProcessGlobalConfig.apply(config);

        try (WebViewOnUiThread webViewOnUiThread = new WebViewOnUiThread()) {
            Assert.assertTrue(
                    "Data base directory should be created when creating a WebView instance.",
                    dataDir.exists());
            Assert.assertTrue(
                    "Cache base directory should be created when creating a WebView instance.",
                    cacheDir.exists());
        }
    }

    @Test
    @MediumTest
    public void testSetPartitionedCookiesEnabled() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        WebkitUtils.checkStartupFeature(context,
                WebViewFeature.STARTUP_FEATURE_CONFIGURE_PARTITIONED_COOKIES);

        ProcessGlobalConfig config = new ProcessGlobalConfig();
        config.setPartitionedCookiesEnabled(context, true);
        ProcessGlobalConfig.apply(config);

        try (WebViewOnUiThread webViewOnUiThread = new WebViewOnUiThread()) {
            // Verify WebView loads without error after applying partitioned cookies config
            Assert.assertNotNull(
                    "WebView should be successfully created after configuring partitioned cookies.",
                    webViewOnUiThread.getWebViewOnCurrentThread());
        }
    }

    @Test
    @MediumTest
    public void testSetUiThreadStartupModeV2() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        WebkitUtils.checkStartupFeature(context,
                WebViewFeature.STARTUP_FEATURE_SET_UI_THREAD_STARTUP_MODE_V2);

        ProcessGlobalConfig config = new ProcessGlobalConfig();
        config.setUiThreadStartupModeV2(context, ProcessGlobalConfig.UI_THREAD_STARTUP_MODE_ASYNC);
        ProcessGlobalConfig.apply(config);

        try (WebViewOnUiThread webViewOnUiThread = new WebViewOnUiThread()) {
            // Verify WebView loads without error after applying async startup mode
            Assert.assertNotNull(
                    "WebView should be successfully created after configuring UI thread"
                            + " startup mode.",
                    webViewOnUiThread.getWebViewOnCurrentThread());
        }
    }
}
