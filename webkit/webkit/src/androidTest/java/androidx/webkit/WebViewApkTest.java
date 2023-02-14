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

import android.os.Build;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SdkSuppress;
import androidx.test.filters.SmallTest;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests related to the state of the WebView APK on the device. These tests only makes sense on L+
 * (21+) devices where the WebView implementation is provided by a WebView APK rather than the
 * framework itself.
 */
@SmallTest
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.LOLLIPOP)
@RunWith(AndroidJUnit4.class)
public class WebViewApkTest {

    /**
     * A test ensuring that our test configuration is correct. In each configuration file we declare
     * which WebView APK to install, and pass an argument to our instrumentation declaring the
     * version of the WebView APK we intend to install. This test ensures the version passed as an
     * instrumentation argument matches the WebView implementation on the device (to ensure the
     * WebView APK was indeed installed correctly).
     */
    @Test
    public void testWebViewVersionMatchesInstrumentationArgs() {
        // WebView version: e.g. 46.0.2490.14, or 67.0.3396.17.
        String expectedWebViewVersionString =
                InstrumentationRegistry.getArguments().getString("webview-version");
        // Use assumeTrue instead of using assumeNotNull so that we can provide a more descriptive
        // message.
        Assume.assumeTrue("Did not receive a WebView version as an instrumentation argument",
                expectedWebViewVersionString != null);
        // Convert to a WebViewVersion to ensure these are well-formed
        // Chromium-style version strings.
        WebViewVersion expectedWebViewVersion = new WebViewVersion(expectedWebViewVersionString);
        WebViewVersion actualWebViewVersion =
                WebViewVersion.getInstalledWebViewVersionFromPackage();
        Assert.assertEquals(expectedWebViewVersion, actualWebViewVersion);
    }
}
