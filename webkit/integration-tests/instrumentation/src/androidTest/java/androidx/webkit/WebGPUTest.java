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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.MediumTest;
import androidx.webkit.internal.WebViewFeatureInternal;

import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;

/** Tests for {@link WebGPU}. */
@MediumTest
@RunWith(AndroidJUnit4.class)
public class WebGPUTest {

    private static final int MAX_MAJOR_VERSION = (1 << 24) - 1;
    private static final long MAX_MINOR_VERSION = (1L << 40) - 1L;

    @Test
    public void testGetWGPUProcGetProcAddress() {
        Assume.assumeTrue(WebViewFeatureInternal.WEBGPU.isSupportedByWebView());
        long handle = WebGPU.getWGPUProcGetProcAddress(1, 0L);
        assertTrue("Expected positive proc pointer address, got " + handle, handle > 0L);
        assertEquals(WebGPU.ERROR_APP_TOO_OLD, WebGPU.getWGPUProcGetProcAddress(0, 0L));
        assertEquals(
                WebGPU.ERROR_WEBVIEW_TOO_OLD,
                WebGPU.getWGPUProcGetProcAddress(MAX_MAJOR_VERSION, 0L));
        assertEquals(
                WebGPU.ERROR_WEBVIEW_TOO_OLD,
                WebGPU.getWGPUProcGetProcAddress(1, MAX_MINOR_VERSION));
    }

    @Test
    public void testGetWGPUProcGetProcAddressOutOfRangeThrows() {
        assertThrows(
                IllegalArgumentException.class, () -> WebGPU.getWGPUProcGetProcAddress(-1, 0L));
        assertThrows(
                IllegalArgumentException.class,
                () -> WebGPU.getWGPUProcGetProcAddress(Integer.MIN_VALUE, 0L));
        assertThrows(
                IllegalArgumentException.class,
                () -> WebGPU.getWGPUProcGetProcAddress(MAX_MAJOR_VERSION + 1, 0L));
        assertThrows(
                IllegalArgumentException.class,
                () -> WebGPU.getWGPUProcGetProcAddress(Integer.MAX_VALUE, 0L));

        assertThrows(
                IllegalArgumentException.class, () -> WebGPU.getWGPUProcGetProcAddress(1, -1L));
        assertThrows(
                IllegalArgumentException.class,
                () -> WebGPU.getWGPUProcGetProcAddress(1, Long.MIN_VALUE));
        assertThrows(
                IllegalArgumentException.class,
                () -> WebGPU.getWGPUProcGetProcAddress(1, MAX_MINOR_VERSION + 1L));
        assertThrows(
                IllegalArgumentException.class,
                () -> WebGPU.getWGPUProcGetProcAddress(1, Long.MAX_VALUE));
    }
}
