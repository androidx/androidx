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
import android.webkit.WebView
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.webkit.CustomHeader
import androidx.webkit.Profile
import androidx.webkit.ProfileStore
import androidx.webkit.WebViewFeature

/** Demonstration of how to use the custom header API to set custom headers on requests. */
class CustomHeaderActivity : AppCompatActivity() {

    lateinit var webView: WebView
    lateinit var nameInput: EditText
    lateinit var valueInput: EditText
    lateinit var profile: Profile

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_custom_header)
        setTitle(R.string.custom_header_activity_title)
        setUpDemoAppActivity()

        if (!WebViewFeature.isFeatureSupported(WebViewFeature.CUSTOM_REQUEST_HEADERS)) {
            showMessage(R.string.webkit_api_not_available)
            return
        }
        HttpServer(SERVER_PORT, { HttpServer.EchoRequestHandler(it) }, {}).start()

        webView = findViewById(R.id.custom_header_webview)
        profile = ProfileStore.getInstance().getProfile(Profile.DEFAULT_PROFILE_NAME)!!
        nameInput = findViewById(R.id.custom_header_name)
        valueInput = findViewById(R.id.custom_header_value)

        findViewById<Button>(R.id.custom_headers_add).setOnClickListener {
            val name = nameInput.text.toString().trim()
            val value = valueInput.text.toString().trim()
            if (name.isEmpty() || value.isEmpty()) {
                Toast.makeText(
                    this,
                    R.string.custom_header_missing_input_warning,
                    Toast.LENGTH_SHORT,
                )
                return@setOnClickListener
            }
            profile.addCustomHeader(CustomHeader(name, value, setOf(SERVER_URL)))
            nameInput.text.clear()
            valueInput.text.clear()
        }

        findViewById<Button>(R.id.custom_headers_clear).setOnClickListener {
            profile.clearAllCustomHeaders()
        }

        findViewById<Button>(R.id.custom_headers_load).setOnClickListener {
            webView.loadUrl(SERVER_URL)
        }
    }

    companion object {
        private const val SERVER_PORT = 17001
        private const val SERVER_URL = "http://localhost:$SERVER_PORT"
    }
}
