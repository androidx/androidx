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

package androidx.webkit.internal;

import android.webkit.WebView;

import org.chromium.support_lib_boundary.ServiceWorkerControllerBoundaryInterface;
import org.chromium.support_lib_boundary.StaticsBoundaryInterface;
import org.chromium.support_lib_boundary.TracingControllerBoundaryInterface;
import org.chromium.support_lib_boundary.WebViewProviderBoundaryInterface;
import org.chromium.support_lib_boundary.WebkitToCompatConverterBoundaryInterface;

/**
 * This is a stub class used when the WebView Support Library is invoked on a device incompatible
 * with the library (either a pre-L device or a device without a compatible WebView APK).
 * The only method in this class that should be called is {@link #getWebViewFeatures()}.
 */
public class IncompatibleApkWebViewProviderFactory implements WebViewProviderFactory {
    private static final String[] EMPTY_STRING_ARRAY = new String[0];
    private static final String UNSUPPORTED_EXCEPTION_EXPLANATION =
            "This should never happen, if this method was called it means we're trying to reach "
            + "into WebView APK code on an incompatible device. This most likely means the current "
            + "method is being called too early, or is being called on start-up rather than lazily";

    @Override
    public WebViewProviderBoundaryInterface createWebView(WebView webview) {
        throw new UnsupportedOperationException(UNSUPPORTED_EXCEPTION_EXPLANATION);
    }

    @Override
    public WebkitToCompatConverterBoundaryInterface getWebkitToCompatConverter() {
        throw new UnsupportedOperationException(UNSUPPORTED_EXCEPTION_EXPLANATION);
    }

    @Override
    public StaticsBoundaryInterface getStatics() {
        throw new UnsupportedOperationException(UNSUPPORTED_EXCEPTION_EXPLANATION);
    }

    @Override
    public String[] getWebViewFeatures() {
        return EMPTY_STRING_ARRAY;
    }

    @Override
    public ServiceWorkerControllerBoundaryInterface getServiceWorkerController() {
        throw new UnsupportedOperationException(UNSUPPORTED_EXCEPTION_EXPLANATION);
    }

    @Override
    public TracingControllerBoundaryInterface getTracingController() {
        throw new UnsupportedOperationException(UNSUPPORTED_EXCEPTION_EXPLANATION);
    }
}
