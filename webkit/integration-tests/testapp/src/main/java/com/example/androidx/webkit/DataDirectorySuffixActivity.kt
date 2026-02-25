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
import android.webkit.WebViewClient
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.webkit.ProcessGlobalConfig
import androidx.webkit.WebViewFeature

/**
 * An {@link Activity} which makes use of {@link
 * androidx.webkit.ProcessGlobalConfig#setDataDirectorySuffix(Context, String)}.
 */
class DataDirectorySuffixActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTitle(R.string.data_directory_suffix_activity_title)
        setUpDemoAppActivity()

        if (
            !WebViewFeature.isStartupFeatureSupported(
                this,
                WebViewFeature.STARTUP_FEATURE_SET_DATA_DIRECTORY_SUFFIX,
            )
        ) {
            WebkitHelpers.showMessageInActivity(this, R.string.webkit_api_not_available)
            return
        }

        ProcessGlobalConfig().run {
            setDataDirectorySuffix(
                this@DataDirectorySuffixActivity,
                "data_directory_suffix_activity_suffix",
            )
            ProcessGlobalConfig.apply(this)
        }

        setContentView(R.layout.activity_data_directory_config)

        findViewById<WebView>(R.id.data_directory_config_webview).apply {
            settings.javaScriptEnabled = true
            webViewClient = WebViewClient()
            loadUrl("www.google.com")
        }

        findViewById<TextView>(R.id.data_directory_config_textview).setText(R.string.webview_loaded)
    }
}
