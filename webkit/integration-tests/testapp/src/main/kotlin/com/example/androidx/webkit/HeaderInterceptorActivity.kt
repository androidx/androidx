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
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.webkit.HeaderInterceptor
import androidx.webkit.HeaderInterceptorHandle
import androidx.webkit.HeaderUpdates
import androidx.webkit.Profile
import androidx.webkit.ProfileStore
import androidx.webkit.RequestMatcher
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import java.util.concurrent.atomic.AtomicInteger

/**
 * Demonstration of how to use [Profile.addHeaderInterceptor] and
 * [WebViewCompat.addHeaderInterceptor] to dynamically set or remove HTTP request headers.
 */
class HeaderInterceptorActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var profile: Profile
    private lateinit var scopeGroup: RadioGroup
    private lateinit var urlPatternInput: EditText
    private lateinit var headerNameInput: EditText
    private lateinit var headerValueInput: EditText
    private lateinit var targetUrlInput: EditText
    private lateinit var statusTextView: TextView
    private var httpServer: HttpServer? = null
    private val activeHandles = mutableListOf<HeaderInterceptorHandle>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_header_interceptor)
        setTitle(R.string.header_interceptor_activity_title)
        setUpDemoAppActivity()

        if (
            !areAllFeaturesSupported(
                WebViewFeature.HEADER_INTERCEPTOR,
                WebViewFeature.MULTI_PROFILE,
            )
        ) {
            showMessage(R.string.webkit_api_not_available)
            return
        }

        val server = startEchoServer()
        val baseServerUrl = "http://localhost:${server.getPort()}"

        webView = findViewById(R.id.header_interceptor_webview)
        webView.webViewClient = WebViewClient()
        profile = ProfileStore.getInstance().getProfile(Profile.DEFAULT_PROFILE_NAME)!!

        scopeGroup = findViewById(R.id.header_interceptor_scope_group)
        urlPatternInput = findViewById(R.id.header_interceptor_url_pattern)
        headerNameInput = findViewById(R.id.header_interceptor_name)
        headerValueInput = findViewById(R.id.header_interceptor_value)
        targetUrlInput = findViewById(R.id.header_interceptor_target_url)
        statusTextView = findViewById(R.id.header_interceptor_status)

        urlPatternInput.setText("$baseServerUrl/echo*")
        targetUrlInput.setText("$baseServerUrl/echo")
        updateStatus()

        findViewById<Button>(R.id.header_interceptor_add_button).setOnClickListener {
            onHeaderInterceptorAddClicked()
        }

        findViewById<Button>(R.id.header_interceptor_unregister_all_button).setOnClickListener {
            onUnregisterAllClicked()
        }

        findViewById<Button>(R.id.header_interceptor_load_button).setOnClickListener {
            onLoadClicked()
        }
    }

    override fun onDestroy() {
        unregisterAllInterceptors()
        httpServer?.shutdown()
        httpServer = null
        if (::webView.isInitialized) {
            webView.destroy()
        }
        super.onDestroy()
    }

    private fun onHeaderInterceptorAddClicked() {
        val headerName = headerNameInput.text.toString().trim()
        if (headerName.isEmpty()) {
            Toast.makeText(
                    this,
                    R.string.header_interceptor_missing_name_warning,
                    Toast.LENGTH_SHORT,
                )
                .show()
            return
        }

        val patternText = urlPatternInput.text.toString().trim()
        val valueTemplate = headerValueInput.text.toString().trim()
        val isProfileScope =
            scopeGroup.checkedRadioButtonId == R.id.header_interceptor_scope_profile

        val matcher =
            if (patternText.isEmpty()) {
                RequestMatcher.forAnyUrl()
            } else {
                RequestMatcher.forUrlPatterns(setOf(patternText))
            }
        val interceptor =
            createHeaderInterceptor(
                isProfileScope = isProfileScope,
                headerName = headerName,
                valueTemplate = valueTemplate,
            )

        try {
            val handle =
                if (isProfileScope) {
                    profile.addHeaderInterceptor(matcher, interceptor)
                } else {
                    WebViewCompat.addHeaderInterceptor(webView, matcher, interceptor)
                }
            activeHandles.add(handle)
            updateStatus()
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "Failed to register header interceptor", e)
            Toast.makeText(this, e.message, Toast.LENGTH_LONG).show()
        }
    }

    private fun onUnregisterAllClicked() {
        unregisterAllInterceptors()
        updateStatus()
    }

    private fun onLoadClicked() {
        val targetUrl = targetUrlInput.text.toString().trim()
        if (targetUrl.isEmpty()) {
            Toast.makeText(
                    this,
                    R.string.header_interceptor_missing_url_warning,
                    Toast.LENGTH_SHORT,
                )
                .show()
            return
        }
        webView.loadUrl(targetUrl, mapOf(DEFAULT_LOAD_URL_HEADER to "from-loadUrl"))
    }

    private fun startEchoServer(): HttpServer {
        val server =
            HttpServer(
                /* port= */ 0,
                { HttpServer.EchoRequestHandler(it, this) },
                callback = null,
            )
        server.start()
        httpServer = server
        return server
    }

    private fun unregisterAllInterceptors() {
        for (handle in activeHandles) {
            handle.unregister()
        }
        activeHandles.clear()
    }

    private fun updateStatus() {
        statusTextView.text =
            getString(R.string.header_interceptor_active_count, activeHandles.size)
    }

    companion object {
        private const val TAG = "HeaderInterceptorDemo"
        private const val COUNT_TOKEN = "#count"
        private const val DEFAULT_LOAD_URL_HEADER = "X-LoadUrl-Header"

        private fun createHeaderInterceptor(
            isProfileScope: Boolean,
            headerName: String,
            valueTemplate: String,
        ): HeaderInterceptor {
            val scopeLabel = if (isProfileScope) "Profile" else "WebView"
            val requestCounter = AtomicInteger(0)
            return HeaderInterceptor { request ->
                Log.i(
                    TAG,
                    "[$scopeLabel] Intercepted ${request.method} ${request.url} " +
                        "(isForMainFrame=${request.isForMainFrame}, " +
                        "headers=${request.requestHeaders})",
                )
                val builder = HeaderUpdates.Builder()
                if (valueTemplate.isEmpty()) {
                    builder.setHeaderRemoved(headerName)
                } else {
                    val count = requestCounter.incrementAndGet()
                    val resolvedValue = valueTemplate.replace(COUNT_TOKEN, count.toString())
                    builder.setHeader(headerName, resolvedValue)
                }
                builder.build()
            }
        }
    }
}
