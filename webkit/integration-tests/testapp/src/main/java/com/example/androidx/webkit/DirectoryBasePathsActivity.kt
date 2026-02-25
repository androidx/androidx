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
import androidx.core.content.ContextCompat
import androidx.webkit.ProcessGlobalConfig
import androidx.webkit.WebViewFeature
import java.io.File

/**
 * An {@link Activity} which makes use of {@link
 * androidx.webkit.ProcessGlobalConfig#setDirectoryBasePaths(Context, File, File)}
 */
class DirectoryBasePathsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setTitle(R.string.directory_base_path_activity_title)
        setUpDemoAppActivity()

        if (
            !WebViewFeature.isStartupFeatureSupported(
                this,
                WebViewFeature.STARTUP_FEATURE_SET_DIRECTORY_BASE_PATHS,
            )
        ) {
            showMessage(R.string.webkit_api_not_available)
            return
        }

        val dataBasePath = File(ContextCompat.getDataDir(this), "data_dir")
        val cacheBasePath = File(ContextCompat.getDataDir(this), "cache_dir")

        @Suppress("DEPRECATION")
        ProcessGlobalConfig().run {
            setDirectoryBasePaths(this@DirectoryBasePathsActivity, dataBasePath, cacheBasePath)
            setDataDirectorySuffix(
                this@DirectoryBasePathsActivity,
                "directory_base_path_activity_suffix",
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
