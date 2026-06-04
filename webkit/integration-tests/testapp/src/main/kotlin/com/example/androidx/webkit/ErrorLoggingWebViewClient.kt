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

package com.example.androidx.webkit

import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.widget.TextView
import androidx.webkit.WebResourceErrorCompat
import androidx.webkit.WebViewClientCompat

class ErrorLoggingWebViewClient(val textView: TextView) : WebViewClientCompat() {

    init {
        textView.setText(R.string.error_log_title)
    }

    override fun onReceivedError(
        view: WebView,
        request: WebResourceRequest,
        error: WebResourceErrorCompat,
    ) = logErrors(request.url.toString(), error.errorCode)

    // use the old one for compatibility with all API levels.
    @Deprecated("Deprecated in Java")
    override fun onReceivedError(
        view: WebView,
        errorCode: Int,
        description: String,
        failingUrl: String,
    ) = logErrors(failingUrl, errorCode)

    private fun logErrors(url: String, errorCode: Int) = textView.append("\n$url|$errorCode")
}
