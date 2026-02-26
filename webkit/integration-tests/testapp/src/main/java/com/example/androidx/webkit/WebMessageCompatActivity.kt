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

import android.net.Uri
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.AbsoluteSizeSpan
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.webkit.WebMessageCompat
import androidx.webkit.WebMessagePortCompat
import androidx.webkit.WebMessagePortCompat.WebMessageCallbackCompat
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import com.google.common.base.Charsets
import com.google.common.io.ByteStreams
import java.nio.ByteBuffer

/** An {@link Activity} to exercise WebMessageCompat related functionality. */
class WebMessageCompatActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var spinner: Spinner
    private var messageCount = 0
    private var expectedCount = 0
    private var timeStamp = 0L
    private lateinit var port: WebMessagePortCompat

    private inner class MessagePortClient : WebViewClient() {
        override fun onPageFinished(view: WebView?, url: String?) {
            super.onPageFinished(view, url)
            setupMessagePort()
        }
    }

    fun createNativeTitle(): CharSequence {
        val title = "Native View\n"
        val ss = SpannableString(title)
        ss.setSpan(
            AbsoluteSizeSpan(55, true),
            0,
            title.length - 1,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
        )
        return ss
    }

    private fun setupMessagePort() {
        val ports = WebViewCompat.createWebMessageChannel(webView)
        WebViewCompat.postWebMessage(
            webView,
            WebMessageCompat("setup", arrayOf<WebMessagePortCompat?>(ports[0])),
            Uri.EMPTY,
        )
        port = ports[1]
        port.setWebMessageCallback(
            object : WebMessageCallbackCompat() {
                override fun onMessage(port: WebMessagePortCompat, message: WebMessageCompat?) {
                    messageCount =
                        when (message?.type) {
                            WebMessageCompat.TYPE_STRING -> message.data!!.toInt()
                            WebMessageCompat.TYPE_ARRAY_BUFFER ->
                                ByteBuffer.wrap(message.arrayBuffer).getInt()
                            else -> throw RuntimeException("Invalid type: " + message?.type)
                        }
                    if (messageCount % 100 == 0) {
                        refreshNativeText()
                    }
                    if (messageCount == expectedCount) {
                        refreshPerfText()
                        spinner.setEnabled(ARRAY_BUFFER_SUPPORTED)
                    }
                    sendMessage()
                }
            }
        )
    }

    private fun sendMessage() {
        if (messageCount >= expectedCount) {
            return
        }

        val message =
            when (spinner.getSelectedItem() as? String) {
                "String" -> WebMessageCompat((messageCount + 1).toString())
                "ArrayBuffer" -> {
                    WebMessageCompat(
                        ByteBuffer.allocate(Integer.BYTES).putInt(messageCount + 1).array()
                    )
                }
                else -> throw IllegalArgumentException("Invalid WebMessage type")
            }
        if (findViewById<CheckBox>(R.id.checkbox_window_message).isChecked) {
            WebViewCompat.postWebMessage(webView, message, Uri.EMPTY)
        } else {
            port.postMessage(message)
        }
    }

    private fun sendButtonClicked(v: View) {
        expectedCount = messageCount + 5000
        timeStamp = System.currentTimeMillis()
        spinner.setEnabled(false)
        sendMessage()
    }

    private fun refreshNativeText() {
        findViewById<TextView>(R.id.textview).text =
            "${createNativeTitle()} $messageCount messages received"
    }

    private fun refreshPerfText() {
        val averageTime = (System.currentTimeMillis() - timeStamp) / 5000.0
        findViewById<TextView>(R.id.textview_perf).text =
            "Average time over 5000 messages: $averageTime ms"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_web_message_compat)
        setTitle(R.string.web_message_compat_activity_title)
        setUpDemoAppActivity()

        if (!WebViewFeature.isFeatureSupported(WebViewFeature.POST_WEB_MESSAGE)) {
            showMessage(R.string.webkit_api_not_available)
            return
        }

        webView =
            findViewById<WebView>(R.id.webview).apply {
                settings.javaScriptEnabled = true
                webViewClient = MessagePortClient()
            }
        val arrayAdapter =
            ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, MESSAGE_TYPES)
        spinner =
            findViewById<Spinner>(R.id.message_type_spinner).apply {
                adapter = arrayAdapter
                isEnabled = ARRAY_BUFFER_SUPPORTED
            }

        assets.open("www/web_message_compat.html").use {
            String(ByteStreams.toByteArray(it), Charsets.UTF_8).let { webContent ->
                webView.loadDataWithBaseURL(
                    "https://example.com",
                    webContent,
                    "text/html",
                    null,
                    null,
                )
            }
        }

        findViewById<Button>(R.id.button_send).setOnClickListener(this::sendButtonClicked)
    }

    companion object {
        private val ARRAY_BUFFER_SUPPORTED =
            WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_ARRAY_BUFFER)
        private val MESSAGE_TYPES = arrayOf("String", "ArrayBuffer")
    }
}
