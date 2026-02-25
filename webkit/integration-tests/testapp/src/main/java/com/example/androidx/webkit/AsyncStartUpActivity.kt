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

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.webkit.StartUpLocation
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewStartUpConfig
import androidx.webkit.WebViewStartUpResult
import java.io.PrintWriter
import java.io.StringWriter
import java.util.concurrent.Executors

/**
 * An {@link Activity} that calls {@link WebViewCompat#startUpWebView(android.content.Context,
 * WebViewStartUpConfig, WebViewCompat.WebViewStartUpCallback)} to startup WebView asynchronously
 * and displays the summary of startup.
 */
@SuppressLint("NullAnnotationGroup")
@OptIn(WebViewCompat.ExperimentalAsyncStartUp::class)
class AsyncStartUpActivity : AppCompatActivity() {

    private var startCaptureTime = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        // Take care not to startup WebView (including layout inflation) before the explicit call
        // to `startUpWebView()`.

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_async_startup)
        setTitle(R.string.async_startup_activity_title)
        setUpDemoAppActivity()

        startCaptureTime = System.currentTimeMillis()
        WebViewCompat.startUpWebView(
            this,
            WebViewStartUpConfig.Builder(Executors.newSingleThreadExecutor()).build(),
            this::onWebViewReady,
        )
    }

    private fun onWebViewReady(result: WebViewStartUpResult) {
        val duration = System.currentTimeMillis() - startCaptureTime

        findViewById<TextView>(R.id.async_startup_textview).apply {
            movementMethod = ScrollingMovementMethod()
            text =
                """
                WebView Async StartUp onSuccess() called in $duration ms.
                totalTimeInUiThreadMillis: ${result.totalTimeInUiThreadMillis}
                maxTimePerTaskInUiThreadMillis: ${result.maxTimePerTaskInUiThreadMillis}
                uiThreadBlockingStartUpLocations:
                ${getStackInformation(result.uiThreadBlockingStartUpLocations)}
                nonUiThreadBlockingStartUpLocations:
                ${getStackInformation(result.nonUiThreadBlockingStartUpLocations)}
            """
                    .trimIndent()
        }

        //  Render content in a WebView similar to real-life usage.
        val webView =
            WebView(this).apply {
                webViewClient = WebViewClient()
                loadUrl("www.google.com")
            }
        findViewById<LinearLayout>(R.id.activity_async_startup)
            .addView(
                webView,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT,
                ),
            )
    }

    private fun getStackInformation(startUpLocations: List<StartUpLocation>?): String {
        return when (startUpLocations) {
            null -> "null"
            emptyList<WebViewStartUpResult>() -> "empty list"
            else -> {
                startUpLocations.joinToString("\n") { convertToString(it.stackInformation) }
            }
        }
    }

    private fun convertToString(t: Throwable): String {
        val sw = StringWriter()
        t.printStackTrace(PrintWriter(sw))
        return sw.toString()
    }
}
