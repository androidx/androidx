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

package androidx.webkit.internal;

import android.webkit.WebView;

import androidx.webkit.DownloadHandler;
import androidx.webkit.WebDownload;
import androidx.webkit.WebViewCompat;

import org.chromium.support_lib_boundary.DownloadHandlerBoundaryInterface;
import org.chromium.support_lib_boundary.util.Features;
import org.jspecify.annotations.NonNull;

import java.lang.reflect.InvocationHandler;

/**
 * Adapter between {@link DownloadHandler} and {@link DownloadHandlerBoundaryInterface}, which
 * wraps each download that WebView passes in as a {@link WebDownload}.
 */
@WebViewCompat.ExperimentalDownloadHandler
public class DownloadHandlerAdapter implements DownloadHandlerBoundaryInterface {
    private static final String[] SUPPORTED_FEATURES = {Features.DOWNLOAD_HANDLER};

    private final @NonNull DownloadHandler mHandler;

    public DownloadHandlerAdapter(@NonNull DownloadHandler handler) {
        mHandler = handler;
    }

    @Override
    public void onDownloadRequested(
            @NonNull WebView webView, /* WebDownload */ @NonNull InvocationHandler download) {
        mHandler.onDownloadRequested(webView, new WebDownload(download));
    }

    @Override
    public String @NonNull [] getSupportedFeatures() {
        return SUPPORTED_FEATURES;
    }
}
