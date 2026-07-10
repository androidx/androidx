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

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.webkit.WebSettings
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.webkit.WebStorageCompat
import androidx.webkit.WebViewFeature
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest

/** An [Activity] to exercise WebStorageCompat related functionality. */
class WebStorageCompatActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var mockWebServer: MockWebServer
    private lateinit var pageUrl: String

    private inner class MockDispatcher : Dispatcher() {
        private fun createMockResponse(): MockResponse {
            return runCatching {
                    val dateFormat = SimpleDateFormat(DATE_FORMAT_PATTERN, Locale.US).format(Date())
                    val bodyContent =
                        resources
                            .openRawResource(R.raw.web_storage_html_template)
                            .readText()
                            .format(dateFormat)

                    return@runCatching MockResponse().apply {
                        setHeader(CACHE_CONTROL_HEADER_NAME, "max-age=604800")
                        setBody(bodyContent)
                    }
                }
                .onFailure { Log.e(TAG, TEMPLATE_LOADING_ERROR, it) }
                .getOrDefault(
                    MockResponse().apply {
                        setResponseCode(500)
                        setBody(TEMPLATE_LOADING_ERROR)
                    }
                )
        }

        override fun dispatch(request: RecordedRequest): MockResponse {
            return if (request.path != "/") {
                MockResponse().setResponseCode(400)
            } else {
                createMockResponse()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_web_storage)
        setTitle(R.string.web_storage_activity_title)
        setUpDemoAppActivity()

        if (!WebViewFeature.isFeatureSupported(WebViewFeature.DELETE_BROWSING_DATA)) {
            showMessage(R.string.webkit_api_not_available)
            return
        }

        webView =
            findViewById<WebView>(R.id.web_storage_webview).apply {
                settings.cacheMode = WebSettings.LOAD_DEFAULT
                webViewClient = WebViewClient()
            }

        findViewById<Button>(R.id.web_storage_load_page_button)
            .setOnClickListener(this::onLoadButtonClicked)
        findViewById<Button>(R.id.web_storage_delete_data_button)
            .setOnClickListener(this::onDeleteButtonClicked)

        mockWebServer = MockWebServer().apply { dispatcher = MockDispatcher() }

        lifecycleScope.launch {
            pageUrl = startMockServerAndGetPageUrl()
            webView.loadUrl(pageUrl)
        }
    }

    private suspend fun startMockServerAndGetPageUrl(): String {
        // The mockWebServer accesses networking APIs during startup and URL construction that
        // are not allowed on the main thread.
        return withContext(Dispatchers.IO) {
            mockWebServer.start()
            mockWebServer.url("/").toString()
        }
    }

    private fun onLoadButtonClicked(view: View) {
        webView.loadUrl(pageUrl)
    }

    private fun onDeleteButtonClicked(view: View) {
        WebStorageCompat.deleteBrowsingData(WebStorage.getInstance(), ::onDeletionComplete)
    }

    private fun onDeletionComplete() {
        Toast.makeText(this, R.string.web_storage_delete_complete, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        runCatching { mockWebServer.close() }.onFailure { Log.e(TAG, WEB_SERVER_CLOSING_ERROR, it) }
    }

    companion object {
        private const val TAG = "WebStorageActivity"
        private const val TEMPLATE_LOADING_ERROR = "Error loading html template"
        private const val WEB_SERVER_CLOSING_ERROR = "Error closing mock web server"
        private const val CACHE_CONTROL_HEADER_NAME = "Cache-Control"
        private const val DATE_FORMAT_PATTERN = "yyyy.MM.dd HH:mm:ss z"
    }
}
