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
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SdkSuppress;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import androidx.webkit.internal.WebViewGlueCommunicator;

import org.chromium.support_lib_boundary.util.Features;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SmallTest
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.LOLLIPOP)
@RunWith(AndroidJUnit4.class)
public class WebViewApkTest {
    /**
     * Represents a WebView version. Is comparable.
     */
    private static class WebViewVersion implements Comparable<WebViewVersion> {
        private static final Pattern CHROMIUM_VERSION_REGEX =
                Pattern.compile("^(\\d+)\\.(\\d+)\\.(\\d+)\\.(\\d+)$");

        private int[] mComponents;

        WebViewVersion(String versionString) {
            final Matcher m = CHROMIUM_VERSION_REGEX.matcher(versionString);
            if (m.matches()) {
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
    }

    @Test
    public void testApkSupportsExpectedFeatures() {
        Context context = InstrumentationRegistry.getTargetContext();
        final WebViewVersion apkVersion =
                new WebViewVersion(WebViewCompat.getCurrentWebViewPackage(context).versionName);

        // For simplicity, we require this test to run on at least the first WebView canary
        // supporting the bulk of WebView features (there's nothing interesting to test before the
        // first M67).
        Assume.assumeTrue(apkVersion.compareTo(new WebViewVersion("67.0.3396.0")) >= 0);

        // Add/remove Features for each WebView version.
        final HashSet<String> expectedFeatures = new HashSet<>();

        expectedFeatures.add(Features.SERVICE_WORKER_BASIC_USAGE);
        expectedFeatures.add(Features.WEB_RESOURCE_ERROR_GET_CODE);
        expectedFeatures.add(Features.SHOULD_OVERRIDE_WITH_REDIRECTS);
        expectedFeatures.add(Features.SAFE_BROWSING_RESPONSE_SHOW_INTERSTITIAL);
        expectedFeatures.add(Features.VISUAL_STATE_CALLBACK);
        expectedFeatures.add(Features.SAFE_BROWSING_PRIVACY_POLICY_URL);
        expectedFeatures.add(Features.SAFE_BROWSING_HIT);
        expectedFeatures.add(Features.OFF_SCREEN_PRERASTER);
        expectedFeatures.add(Features.WEB_RESOURCE_REQUEST_IS_REDIRECT);
        expectedFeatures.add(Features.SAFE_BROWSING_WHITELIST);
        expectedFeatures.add(Features.SERVICE_WORKER_FILE_ACCESS);
        expectedFeatures.add(Features.SERVICE_WORKER_CONTENT_ACCESS);
        expectedFeatures.add(Features.SAFE_BROWSING_RESPONSE_BACK_TO_SAFETY);
        expectedFeatures.add(Features.SERVICE_WORKER_BLOCK_NETWORK_LOADS);
        expectedFeatures.add(Features.SERVICE_WORKER_CACHE_MODE);
        expectedFeatures.add(Features.SAFE_BROWSING_ENABLE);
        expectedFeatures.add(Features.RECEIVE_HTTP_ERROR);
        expectedFeatures.add(Features.SAFE_BROWSING_RESPONSE_PROCEED);
        expectedFeatures.add(Features.DISABLED_ACTION_MODE_MENU_ITEMS);
        expectedFeatures.add(Features.RECEIVE_WEB_RESOURCE_ERROR);
        expectedFeatures.add(Features.SERVICE_WORKER_SHOULD_INTERCEPT_REQUEST);
        expectedFeatures.add(Features.START_SAFE_BROWSING);
        expectedFeatures.add(Features.WEB_RESOURCE_ERROR_GET_DESCRIPTION);

        if (apkVersion.compareTo(new WebViewVersion("68.0.3432.0")) >= 0) {
            expectedFeatures.add(Features.WEB_MESSAGE_PORT_POST_MESSAGE);
            expectedFeatures.add(Features.WEB_MESSAGE_PORT_CLOSE);
            expectedFeatures.add(Features.WEB_MESSAGE_PORT_SET_MESSAGE_CALLBACK);
            expectedFeatures.add(Features.CREATE_WEB_MESSAGE_CHANNEL);
            expectedFeatures.add(Features.POST_WEB_MESSAGE);
            expectedFeatures.add(Features.WEB_MESSAGE_CALLBACK_ON_MESSAGE);
        }

        final HashSet<String> apkFeatures = new HashSet<>(
                Arrays.asList(WebViewGlueCommunicator.getFactory().getWebViewFeatures()));

        Assert.assertEquals(expectedFeatures, apkFeatures);
    }
}
