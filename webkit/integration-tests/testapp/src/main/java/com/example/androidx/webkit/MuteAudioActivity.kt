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
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature

class MuteAudioActivity : AppCompatActivity() {

    lateinit var webView: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_mute_audio)
        setUpDemoAppActivity()
        if (!WebViewFeature.isFeatureSupported(WebViewFeature.MUTE_AUDIO)) {
            showMessage(R.string.mute_audio_not_supported)
            return
        }

        setUpWebView()
        findViewById<Button>(R.id.mute_audio_mute).setOnClickListener(this::mute)
        findViewById<Button>(R.id.mute_audio_unmute).setOnClickListener(this::unMute)
        findViewById<Button>(R.id.mute_audio_check).setOnClickListener(this::checkMutedAndShowToast)
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    private fun setUpWebView() =
        findViewById<WebView>(R.id.mute_audio_webview).run {
            settings.javaScriptEnabled = true

            WebViewCompat.setAudioMuted(this, true)

            assets.open(FILE_NAME).use {
                it.readBytes().toString(Charsets.UTF_8).let { data ->
                    loadDataWithBaseURL(BASE_URL, data, MIME_TYPE, null, null)
                }
            }
        }

    private fun mute(view: View) = WebViewCompat.setAudioMuted(webView, true)

    private fun unMute(view: View) = WebViewCompat.setAudioMuted(webView, false)

    private fun checkMutedAndShowToast(view: View) {
        val text = if (WebViewCompat.isAudioMuted(webView)) MUTED_TEXT else UNMUTED_TEXT

        Toast.makeText(this, resources.getString(text), Toast.LENGTH_SHORT).show()
    }

    companion object {
        private val MUTED_TEXT = R.string.mute_audio_audio_is_muted
        private val UNMUTED_TEXT = R.string.mute_audio_audio_is_unmuted
        private const val BASE_URL = "https://example.com"
        private const val MIME_TYPE = "text/html"
        private const val FILE_NAME = "www/mute_audio.html"
    }
}
