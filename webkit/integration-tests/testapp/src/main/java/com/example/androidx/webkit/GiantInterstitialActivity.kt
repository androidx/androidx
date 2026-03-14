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
import android.util.DisplayMetrics
import android.webkit.WebView
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature
import kotlin.math.ceil

class GiantInterstitialActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_giant_interstitial)
        setTitle(R.string.giant_interstitial_activity_title)
        setUpDemoAppActivity()

        val displayMetrics = DisplayMetrics()
        @Suppress("DEPRECATION") windowManager.defaultDisplay.getMetrics(displayMetrics)
        val height = ceil(displayMetrics.heightPixels * SCALE_FACTOR).toInt()
        val width = ceil(displayMetrics.widthPixels * SCALE_FACTOR).toInt()
        val params = LinearLayout.LayoutParams(width, height)

        findViewById<WebView>(R.id.giant_webview).apply {
            layoutParams = params

            if (WebViewFeature.isFeatureSupported(WebViewFeature.SAFE_BROWSING_ENABLE)) {
                WebSettingsCompat.setSafeBrowsingEnabled(this.settings, true)
            }

            loadUrl(SafeBrowsingHelpers.MALWARE_URL)
        }
    }

    companion object {
        private const val SCALE_FACTOR = 1.1
    }
}
