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

import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import android.os.Build;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SdkSuppress;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import androidx.webkit.internal.WebViewFeatureInternal;

import org.junit.Test;
import org.junit.runner.RunWith;

@SmallTest
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.LOLLIPOP)
@RunWith(AndroidJUnit4.class)
public class WebViewApkTest {
    @Test
    public void testApkSupportsCurrentFeatures() {
        // If the 'webview-version' argument is provided, it means that the bot updated the device's
        // WebView APK (and we can continue with this test). This is usually null for local runs, in
        // which case we skip the test.
        boolean webviewUpdatedByTestInfra =
                (InstrumentationRegistry.getArguments().getString("webview-version") != null);
        assumeTrue("Device must have an updated webview", webviewUpdatedByTestInfra);

        for (WebViewFeatureInternal feature : WebViewFeatureInternal.values()) {
            assertTrue(feature.isSupportedByWebView());
        }
    }
}
