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

import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import androidx.webkit.internal.WebViewFeatureInternal;
import androidx.webkit.internal.WebViewGlueCommunicator;
import androidx.webkit.internal.WebkitToCompatConverter;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests related to the interface between the support library and the Chromium support library glue.
 */
@RunWith(AndroidJUnit4.class)
public class BoundaryInterfaceTest {

    /**
     * Test ensuring that we can create a {@link androidx.webkit.internal.WebkitToCompatConverter}.
     * This test catches cases where we try to pass post-L android.webkit classes across the
     * boundary - doing so will fail when we (at run-time) create a {@link java.lang.reflect.Proxy}
     * for {@link org.chromium.support_lib_boundary.WebkitToCompatConverterBoundaryInterface} since
     * that proxy will need to look up all the classes referenced from
     * {@link org.chromium.support_lib_boundary.WebkitToCompatConverterBoundaryInterface}.
     */
    @SmallTest
    @Test
    public void testCreateWebkitToCompatConverter() {
        // Use the SERVICE_WORKER_BASIC_USAGE feature as a proxy for knowing whether the current
        // WebView APK is compatible with the support library.
        if (WebViewFeatureInternal.SERVICE_WORKER_BASIC_USAGE.isSupportedByWebView()) {
            WebkitToCompatConverter converter = WebViewGlueCommunicator.getCompatConverter();
        }
    }

}
