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

import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.inputmethod.EditorInfo
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.webkit.TracingConfig
import androidx.webkit.TracingController
import androidx.webkit.WebViewFeature
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.Executors
import okio.IOException
import org.json.JSONException
import org.json.JSONObject

/** An {@link Activity} to exercise Tracing Controller functionality. */
class TracingControllerActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_tracing_controller)
        setTitle(R.string.tracing_controller_activity_title)
        setUpDemoAppActivity()

        if (!WebViewFeature.isFeatureSupported(WebViewFeature.TRACING_CONTROLLER_BASIC_USAGE)) {
            showMessage(R.string.webkit_api_not_available)
            return
        }

        val webView =
            findViewById<WebView>(R.id.tracing_controller_webview).apply {
                webViewClient = WebViewClient()
            }
        val navigationBar = findViewById<EditText>(R.id.tracing_controller_edittext)
        val infoView =
            findViewById<TextView>(R.id.tracing_controller_textview).apply {
                visibility = View.GONE
            }
        val tracingButton = findViewById<Button>(R.id.tracing_controller_button)

        val tracingController = TracingController.getInstance()

        navigationBar.setOnEditorActionListener { _, actionId: Int, _ ->
            if (actionId == EditorInfo.IME_ACTION_NEXT) {
                navigationBar.text
                    .toString()
                    .takeIf { it.isNotBlank() }
                    ?.let { url ->
                        val formattedUrl = if (url.startsWith("http")) url else "https://$url"
                        webView.loadUrl(formattedUrl)
                        navigationBar.setText("")
                    }
                return@setOnEditorActionListener true
            }
            return@setOnEditorActionListener false
        }

        tracingButton.setOnClickListener {
            if (tracingController.isTracing) {
                runCatching {
                        tracingButton.isEnabled = false
                        val os = VerifyingFileOutputStream(getLogPath(), infoView, tracingButton)
                        tracingController.stop(os, Executors.newSingleThreadExecutor())
                    }
                    .onFailure { Log.i(TAG, it.toString()) }
            } else {
                val config =
                    TracingConfig.Builder()
                        .addCategories(TracingConfig.CATEGORIES_ANDROID_WEBVIEW)
                        .build()
                tracingController.start(config)
                tracingButton.text = getString(R.string.tracing_controller_stop_tracing)
            }
        }
    }

    private inner class VerifyingFileOutputStream(
        private val logPath: String,
        private val infoView: TextView,
        private val tracingButton: Button,
    ) : FileOutputStream(logPath) {

        override fun close() {
            super.close()
            runOnUiThread {
                infoView.visibility = View.VISIBLE
                tracingButton.visibility = View.GONE
                infoView.text = getString(R.string.tracing_controller_log_path, logPath)
                runCatching { verifyJSON(logPath) }
                    .onFailure { e ->
                        when (e) {
                            is IOException,
                            is JSONException -> {
                                infoView.text = getString(R.string.tracing_controller_invalid_log)
                            }
                            else -> throw e
                        }
                    }
            }
        }

        // This function verifies the JSON is valid by attempting to pass it to a JSONObject
        private fun verifyJSON(logPath: String) = JSONObject(File(logPath).readText())
    }

    private fun getLogPath(): String {
        return getExternalFilesDir(null).toString() + File.separator + TRACING_FILE_EXTENSION
    }

    companion object {
        private const val TAG = "TracingControllerActivity"
        private const val TRACING_FILE_EXTENSION = "tc.json"
    }
}
