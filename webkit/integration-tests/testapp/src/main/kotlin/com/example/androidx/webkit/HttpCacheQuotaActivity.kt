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
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.webkit.HttpCache
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature

/** An {@link android.app.Activity} to demonstrate using HTTP Cache Quota feature. */
class HttpCacheQuotaActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var httpCache: HttpCache

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_http_cache_quota)
        setTitle(R.string.http_cache_quota_activity_title)
        setUpDemoAppActivity()

        if (!WebViewFeature.isFeatureSupported(WebViewFeature.HTTP_CACHE_MANAGER)) {
            showToast(R.string.webkit_api_not_available)
            return
        }

        webView =
            findViewById<WebView>(R.id.http_cache_quota_webview).apply {
                webViewClient = WebViewClient()
                settings.javaScriptEnabled = true
            }
        webView.loadUrl("https://www.google.com/")

        httpCache = WebViewCompat.getProfile(webView).httpCache
        setOnClickListeners()
    }

    override fun onDestroy() {
        webView.destroy()
        super.onDestroy()
    }

    private fun setOnClickListeners() {
        findViewById<Button>(R.id.http_cache_quota_set_button)
            .setOnClickListener(this::onSetQuotaClick)
        findViewById<Button>(R.id.http_cache_quota_get_default_button)
            .setOnClickListener(this::onGetDefaultQuotaClick)
        findViewById<Button>(R.id.http_cache_quota_get_button)
            .setOnClickListener(this::onGetQuotaClick)
        findViewById<Button>(R.id.http_cache_quota_use_default_button)
            .setOnClickListener(this::onUseDefaultQuotaClick)
        findViewById<Button>(R.id.http_cache_quota_is_default_button)
            .setOnClickListener(this::onIsDefaultQuotaClick)
    }

    private fun onSetQuotaClick(v: View) {
        val input = findViewById<EditText>(R.id.http_cache_quota_input).text.toString()
        if (input.isNotEmpty()) {
            val quota = input.toLong()
            httpCache.setQuotaBytes(quota)
            val realQuota = httpCache.getQuotaBytes()
            showToast("now $realQuota bytes")
        } else {
            showToast(R.string.http_cache_quota_missing_input_warning)
        }
    }

    private fun onGetDefaultQuotaClick(v: View) {
        val defaultQuota = httpCache.getDefaultQuotaBytes()
        showToast("$defaultQuota bytes")
    }

    private fun onGetQuotaClick(v: View) {
        val quota = httpCache.getQuotaBytes()
        showToast("$quota bytes")
    }

    private fun onUseDefaultQuotaClick(v: View) {
        httpCache.useDefaultQuota()
        val quota = httpCache.getQuotaBytes()
        showToast("now $quota bytes")
    }

    private fun onIsDefaultQuotaClick(v: View) {
        val isDefault = httpCache.isUsingDefaultQuota()
        showToast(if (isDefault) "Using default" else "Not using default")
    }

    private fun showToast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    private fun showToast(msgResourceId: Int) {
        Toast.makeText(this, msgResourceId, Toast.LENGTH_SHORT).show()
    }
}
