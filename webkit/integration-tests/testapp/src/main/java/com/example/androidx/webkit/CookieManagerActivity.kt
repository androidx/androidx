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
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.webkit.CookieManagerCompat
import androidx.webkit.WebViewFeature

/** An {@link android.app.Activity} to demonstrate {@link CookieManagerCompat#getCookieInfo}. */
class CookieManagerActivity : AppCompatActivity() {

    val MAIN_PAGE_URL = "https://developer.android.com/reference/androidx/webkit/package-summary"
    val COOKIE_URL = "https://developer.android.com"
    val COOKIE_NAME = "signin="

    val webViewClientObject =
        object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)

                val oldApiText = findViewById<TextView>(R.id.textViewTop)
                val newApiText = findViewById<TextView>(R.id.textViewBottom)

                CookieManager.getInstance()
                    .getCookie(COOKIE_URL)
                    ?.split("; ")
                    ?.firstOrNull { it.startsWith(COOKIE_NAME) }
                    ?.let { oldApiText.append(it) }

                CookieManagerCompat.getCookieInfo(CookieManager.getInstance(), COOKIE_URL)
                    .firstOrNull { it.startsWith(COOKIE_NAME) }
                    ?.let { newApiText.append(it) }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_get_cookie_info)
        setTitle(R.string.cookie_manager_activity_title)
        setUpDemoAppActivity()

        if (!WebViewFeature.isFeatureSupported(WebViewFeature.GET_COOKIE_INFO)) {
            showMessage(R.string.cookie_manager_get_cookie_info_not_supported)
            return
        }

        with(findViewById<WebView>(R.id.webView)) {
            webViewClient = webViewClientObject
            loadUrl(MAIN_PAGE_URL)
        }
    }
}
