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
import androidx.appcompat.app.AppCompatActivity

/**
 * An Activity to demonstrate example for dragging image out to other apps. You can use google logo
 * to test.
 *
 * <p>
 * This activity relies on {@code androidx.webkit.DropDataContentProvider} configured in the
 * AndroidManifest.xml to function correctly.
 */
class ImageDragActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_image_drag)
        setTitle(R.string.image_drag_drop_activity_title)
        setUpDemoAppActivity()

        findViewById<WebView>(R.id.image_webview).apply {
            webViewClient = WebViewClient()
            loadUrl(EXAMPLE_SITE)
        }
    }

    companion object {
        private const val EXAMPLE_SITE = "www.google.com"
    }
}
