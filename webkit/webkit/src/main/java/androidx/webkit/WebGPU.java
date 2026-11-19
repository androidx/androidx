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

import androidx.annotation.AnyThread;
import androidx.annotation.CheckResult;
import androidx.annotation.IntRange;
import androidx.webkit.internal.WebViewFeatureInternal;
import androidx.webkit.internal.WebViewGlueCommunicator;

import org.chromium.support_lib_boundary.WebViewProviderFactoryBoundaryInterface;

/**
 * Provides access to the <a href="https://dawn.googlesource.com/dawn">Dawn</a> implementation of
 * <a href="https://www.w3.org/TR/webgpu/">WebGPU</a> packaged in Android System WebView.
 *
 * <p>Most apps should use the {@code androidx.webgpu} library instead of calling this class
 * directly.
 */
// WebGPU is a proper noun.
@SuppressWarnings("AcronymName")
public final class WebGPU {

    private static final int MAX_MAJOR_VERSION = (1 << 24) - 1;
    private static final long MAX_MINOR_VERSION = (1L << 40) - 1L;

    /**
     * Returned by {@link #getWGPUProcGetProcAddress(int, long)} when WebView does not expose
     * WebGPU or its Dawn version is older than requested.
     */
    public static final long ERROR_WEBVIEW_TOO_OLD =
            WebViewProviderFactoryBoundaryInterface.WEBGPU_ERROR_WEBVIEW_TOO_OLD;

    /**
     * Returned by {@link #getWGPUProcGetProcAddress(int, long)} when WebView no longer supports
     * the requested Dawn major version.
     */
    public static final long ERROR_APP_TOO_OLD =
            WebViewProviderFactoryBoundaryInterface.WEBGPU_ERROR_APP_TOO_OLD;

    private WebGPU() {}

    /**
     * Returns the native {@code WGPUProcGetProcAddress} function pointer (see <a
     * href="https://github.com/webgpu-native/webgpu-headers/blob/main/webgpu.h">webgpu.h</a>) from
     * WebView when it supports the requested Dawn version.
     *
     * <p>Native C/C++ code casts a positive return value to {@code WGPUProcGetProcAddress} to look
     * up WebGPU functions. The returned pointer is valid for the lifetime of the process. Values
     * {@code <= 0} are error codes and must not be cast to a pointer.
     *
     * <p>Dawn uses a 24-bit unsigned major version (fits in {@code int}) and a 40-bit unsigned
     * minor version (needs {@code long}).
     *
     * @param majorVersion exact Dawn major version required ({@code 0} to {@code 2^24 - 1}).
     * @param minorVersion minimum Dawn minor version required ({@code 0} to {@code 2^40 - 1}).
     * @return a positive function pointer address if compatible, {@link #ERROR_WEBVIEW_TOO_OLD},
     *     or {@link #ERROR_APP_TOO_OLD}.
     * @throws IllegalArgumentException if {@code majorVersion} or {@code minorVersion} is out of
     *     range.
     */
    @AnyThread
    @CheckResult
    // WGPU is an acronym from webgpu.h.
    @SuppressWarnings("AcronymName")
    public static long getWGPUProcGetProcAddress(
            @IntRange(from = 0, to = MAX_MAJOR_VERSION) int majorVersion,
            @IntRange(from = 0, to = MAX_MINOR_VERSION) long minorVersion) {
        if (majorVersion < 0 || majorVersion > MAX_MAJOR_VERSION) {
            throw new IllegalArgumentException(
                    "majorVersion must be in the range [0, "
                            + MAX_MAJOR_VERSION
                            + "], but was "
                            + majorVersion
                            + ".");
        }
        if (minorVersion < 0L || minorVersion > MAX_MINOR_VERSION) {
            throw new IllegalArgumentException(
                    "minorVersion must be in the range [0, "
                            + MAX_MINOR_VERSION
                            + "], but was "
                            + minorVersion
                            + ".");
        }
        if (WebViewFeatureInternal.WEBGPU.isSupportedByWebView()) {
            return WebViewGlueCommunicator.getFactory()
                    .getWGPUProcGetProcAddress(majorVersion, minorVersion);
        } else {
            return ERROR_WEBVIEW_TOO_OLD;
        }
    }
}
