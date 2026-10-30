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

import android.net.Uri
import android.os.Bundle
import android.webkit.WebView
import android.widget.TextView
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.webkit.DownloadException
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import androidx.webkit.WebViewOutcomeReceiver
import java.io.File

/** Demonstration of how to use the [WebViewCompat.setDownloadHandler] API. */
@OptIn(WebViewCompat.ExperimentalDownloadHandler::class)
class DownloadHandlerActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_download_handler)
        setTitle(R.string.download_handler_activity_title)
        setUpDemoAppActivity()

        if (!WebViewFeature.isFeatureSupported(WebViewFeature.DOWNLOAD_HANDLER_EXPERIMENTAL_V1)) {
            showMessage(R.string.webkit_api_not_available)
            return
        }

        val statusTextView = findViewById<TextView>(R.id.download_handler_textview)
        val webView = findViewById<WebView>(R.id.download_handler_webview)

        // WebView downloads the file itself. The app only decides where to save it.
        WebViewCompat.setDownloadHandler(webView) { _, download ->
            val file = File(cacheDir, download.suggestedFilename)
            download.saveTo(
                Uri.fromFile(file),
                ContextCompat.getMainExecutor(this),
                object : WebViewOutcomeReceiver<Long, DownloadException> {
                    override fun onResult(result: Long) {
                        statusTextView.text =
                            getString(R.string.download_handler_saved, result, file.path)
                    }

                    override fun onError(error: DownloadException) {
                        statusTextView.text =
                            getString(R.string.download_handler_failed, error.errorCode)
                    }
                },
            )
        }

        webView.loadData(DOWNLOAD_PAGE, "text/html", null)
    }

    private companion object {
        const val DOWNLOAD_PAGE =
            "<a href=\"data:text/plain,Hello\" download=\"hello.txt\">Download hello.txt</a>"
    }
}
