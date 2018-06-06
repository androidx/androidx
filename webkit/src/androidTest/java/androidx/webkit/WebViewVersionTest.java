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

import static androidx.webkit.WebViewCompat.getCurrentWebViewPackage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeNotNull;

import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SdkSuppress;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests related to the Chrome version of the WebView implementation on the device.
 */
@RunWith(AndroidJUnit4.class)
public class WebViewVersionTest {

    /**
     * A test ensuring that our test configuration is correct. In each configuration file we declare
     * which WebView APK to install, and pass an argument to our instrumentation declaring the
     * version of the WebView APK we intend to install. This test ensures the version passed as an
     * instrumentation argument matches the WebView implementation on the device (to ensure the
     * WebView APK was indeed installed correctly).
     */
    @SmallTest
    @Test
    // This test only makes sense on L+ (21+) devices where the WebView implementation is provided
    // by a WebView APK rather than the framework itself.
    @SdkSuppress(minSdkVersion = 21)
    public void testWebViewMajorVersionMatchesInstrumentationArgs() {
        // Major WebView version: e.g. 55, 67, or 69.
        String expectedMajorWebViewVersion =
                InstrumentationRegistry.getArguments().getString("webview-version");
        assumeNotNull(expectedMajorWebViewVersion);
        // Actual WebView version, e.g. 55.0.2883.91
        String actualWebViewVersion =
                getCurrentWebViewPackage(InstrumentationRegistry.getTargetContext()).versionName;
        String actualMajorWebViewVersion = actualWebViewVersion.split("\\.")[0];
        assertEquals(expectedMajorWebViewVersion, actualMajorWebViewVersion);
    }
}
