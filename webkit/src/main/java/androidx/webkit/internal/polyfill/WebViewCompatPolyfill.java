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

package androidx.webkit.internal.polyfill;

import android.os.Build;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.NonNull;

import java.lang.reflect.Field;

/**
 * Polyfill implementations for {@link androidx.webkit.WebViewCompat}
 */
public class WebViewCompatPolyfill {
    private WebViewCompatPolyfill() {}

    /**
     * Polyfill for getWebViewClient for Android < O and WebView < 68.
     */
    public static WebViewClient getWebViewClient(@NonNull WebView webView) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) return null;
        try {
            Field fmProvider = WebView.class.getDeclaredField("mProvider");
            fmProvider.setAccessible(true);
            Object webViewProvider = fmProvider.get(webView);
            Field fmContentsClientAdapter = webViewProvider.getClass().getDeclaredField(
                    "mContentsClientAdapter");
            fmContentsClientAdapter.setAccessible(true);
            Object contentsClientAdapter = fmContentsClientAdapter.get(webViewProvider);
            Field fmWebViewClient = contentsClientAdapter.getClass().getDeclaredField(
                    "mWebViewClient");
            fmWebViewClient.setAccessible(true);
            return (WebViewClient) fmWebViewClient.get(contentsClientAdapter);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            return null;
        }
    }
}
