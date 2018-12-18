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

import android.content.Context;
import android.os.Build;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SdkSuppress;
import androidx.test.filters.SmallTest;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
     * Represents a WebView version. Is comparable. This supports version numbers following the
     * scheme outlined at https://www.chromium.org/developers/version-numbers.
     */
    private static class WebViewVersion implements Comparable<WebViewVersion> {
        private static final Pattern CHROMIUM_VERSION_REGEX =
                Pattern.compile("^(\\d+)\\.(\\d+)\\.(\\d+)\\.(\\d+)$");

        private int[] mComponents;

        WebViewVersion(String versionString) {
            Matcher m;
            if ((m = CHROMIUM_VERSION_REGEX.matcher(versionString)).matches()) {
                mComponents = new int[] { Integer.parseInt(m.group(1)),
                    Integer.parseInt(m.group(2)), Integer.parseInt(m.group(3)),
                    Integer.parseInt(m.group(4)) };
            } else {
                throw new IllegalArgumentException("Invalid WebView version string: '"
                        + versionString + "'");
            }
        }

        public boolean equals(Object obj) {
            if (!(obj instanceof WebViewVersion)) {
                return false;
            }
            WebViewVersion that = (WebViewVersion) obj;
            return Arrays.equals(this.mComponents, that.mComponents);
        }

        public int hashCode() {
            return Arrays.hashCode(mComponents);
        }

        @Override
        public int compareTo(WebViewVersion that) {
            for (int i = 0; i < 4; i++) {
                int diff = this.mComponents[i] - that.mComponents[i];
                if (diff != 0) return diff;
            }
            return 0;
        }

        @Override
        public String toString() {
            return this.mComponents[0] + "." + this.mComponents[1] + "."
                    + this.mComponents[2] + "." + this.mComponents[3];
        }
    }

    private WebViewVersion getInstalledWebViewVersionFromPackage() {
        Context context = ApplicationProvider.getApplicationContext();
        // Before M42, we used the major version number, followed by other text wrapped in
        // parentheses.
        final Pattern oldVersionNameFormat =
                Pattern.compile("^(37|38|39|40|41) \\(.*\\)$");
        String installedVersionName = WebViewCompat.getCurrentWebViewPackage(context).versionName;
        Matcher m = oldVersionNameFormat.matcher(installedVersionName);
        if (m.matches()) {
            // There's no way to get the full version number, so just assume 0s for the later
            // numbers.
            installedVersionName = "" + m.group(1) + ".0.0.0";
        }
        return new WebViewVersion(installedVersionName);
    }

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
        // Convert to a WebViewVersion as a sanity check to ensure these are well-formed
        // Chromium-style version strings.
        WebViewVersion expectedWebViewVersion = new WebViewVersion(expectedWebViewVersionString);
        WebViewVersion actualWebViewVersion = getInstalledWebViewVersionFromPackage();
        Assert.assertEquals(expectedWebViewVersion, actualWebViewVersion);
    }
}
