/*
 * Copyright 2019 The Android Open Source Project
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

package com.example.androidx.webkit;

import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.webkit.WebResourceErrorCompat;
import androidx.webkit.WebViewClientCompat;
import androidx.webkit.WebViewFeature;

import java.util.Locale;

/**
 * A {@link android.webkit.WebViewClient} which logs network errors in a provided {@link TextView}.
 */
class ErrorLoggingWebViewClient extends WebViewClientCompat {

    private TextView mTextView;
    ErrorLoggingWebViewClient(TextView textView) {
        mTextView = textView;
        mTextView.setText(R.string.error_log_title);
    }

    @Override
    @RequiresApi(21)
    public void onReceivedError(@NonNull WebView view, @NonNull WebResourceRequest request,
            @NonNull WebResourceErrorCompat error) {
        if (WebViewFeature.isFeatureSupported(WebViewFeature.WEB_RESOURCE_ERROR_GET_CODE)) {
            logErrors(Api21Impl.getUrl(request).toString(), error.getErrorCode());
        }
    }

    @Override
    @SuppressWarnings("deprecation") // use the old one for compatibility with all API levels.
    public void onReceivedError(WebView view, int errorCode, String description,
            String failingUrl) {
        logErrors(failingUrl, errorCode);
    }

    private void logErrors(@NonNull String url, int errorCode) {
        CharSequence text = mTextView.getText();
        mTextView.setText(String.format(Locale.getDefault(), "%s\n%s|%d", text, url, errorCode));
    }
}
