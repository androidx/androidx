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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import android.support.test.filters.MediumTest;
import android.support.test.filters.SdkSuppress;
import android.support.test.runner.AndroidJUnit4;

import androidx.webkit.internal.WebViewFeatureInternal;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests ensuring that Android versions/setups that are incompatible with the WebView Support
 * Library are handled gracefully.
 *
 * Only L+ Android versions are compatible with the WebView Support Library, so any tests in this
 * class that guarantee certain behaviour for incompatible Android versions will only be run on
 * pre-L devices.
 */
@MediumTest
@RunWith(AndroidJUnit4.class)
public class IncompatibilityTest {
    @Test
    @SdkSuppress(maxSdkVersion = 20)
    public void testPreLDeviceHasNoWebViewFeatures() {
        assertEquals(0, WebViewFeatureInternal.getWebViewApkFeaturesForTesting().length);
    }

    @Test
    @SdkSuppress(maxSdkVersion = 20)
    public void testPreLDeviceDoesNotSupportVisualStateCallback() {
        assertFalse(WebViewFeature.isFeatureSupported(WebViewFeature.VISUAL_STATE_CALLBACK));
    }
}
