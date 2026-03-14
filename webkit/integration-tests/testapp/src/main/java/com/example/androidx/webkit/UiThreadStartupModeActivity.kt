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
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.LinearLayout
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.webkit.ProcessGlobalConfig
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import androidx.webkit.WebViewOutcomeReceiver
import androidx.webkit.WebViewStartUpConfig
import androidx.webkit.WebViewStartUpResult
import java.util.concurrent.Executors

/**
 * An {@link Activity} which makes use of {@link
 * androidx.webkit.ProcessGlobalConfig#setUiThreadStartupMode(Context, int)}.
 */
@SuppressLint("NullAnnotationGroup")
@OptIn(WebViewCompat.ExperimentalAsyncStartUp::class)
class UiThreadStartupModeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setTitle(R.string.ui_thread_startup_mode_activity_title)
        appendWebViewVersionToTitle()

        if (
            !WebViewFeature.isStartupFeatureSupported(
                this,
                WebViewFeature.STARTUP_FEATURE_SET_UI_THREAD_STARTUP_MODE_V2,
            )
        ) {
            showMessage(R.string.webkit_api_not_available)
            return
        }

        ProcessGlobalConfig().run {
            setUiThreadStartupModeV2(
                this@UiThreadStartupModeActivity,
                ProcessGlobalConfig.UI_THREAD_STARTUP_MODE_ASYNC_WITHOUT_MULTI_PROCESS_STARTUP,
            )
            ProcessGlobalConfig.apply(this)
        }

        setContentView(R.layout.activity_ui_thread_startup_mode)

        val startUpConfig =
            WebViewStartUpConfig.Builder(Executors.newSingleThreadExecutor()).build()

        WebViewCompat.startUpWebView(
            this,
            startUpConfig,
            WebViewOutcomeReceiver(this::onWebViewStartupComplete),
        )
    }

    private fun onWebViewStartupComplete(result: WebViewStartUpResult) {
        val webView = WebView(this)

        findViewById<LinearLayout>(R.id.ui_thread_startup_mode_webview)
            .addView(
                webView,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT,
                ),
            )

        with(webView) {
            webViewClient = WebViewClient()
            loadUrl("www.google.com")
        }
    }
}
