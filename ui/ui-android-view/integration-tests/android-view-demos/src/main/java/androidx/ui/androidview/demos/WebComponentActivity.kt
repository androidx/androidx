/*
 * Copyright 2020 The Android Open Source Project
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

/* ktlint-disable no-unused-imports */
@file:Suppress("UnusedImport")

package androidx.ui.androidview.demos

import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.activity.ComponentActivity
import androidx.ui.androidview.adapters.dp
import androidx.ui.androidview.adapters.setControlledText
import androidx.ui.androidview.adapters.setLayoutHeight
import androidx.ui.androidview.adapters.setLayoutWeight
import androidx.ui.androidview.adapters.setLayoutWidth
import androidx.ui.androidview.adapters.setOnClick
import androidx.ui.androidview.adapters.setOnTextChanged
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.state
import androidx.ui.androidview.WebComponent
import androidx.ui.androidview.WebContext
import androidx.ui.core.setViewContent
import androidx.ui.viewinterop.emitView

@Stable
class WebParams {
    var url: String by mutableStateOf("https://www.google.com")
}

open class WebComponentActivity : ComponentActivity() {

    val webContext = WebContext()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setViewContent {
            if (WebContext.debug) {
                Log.e("WebCompAct", "setContent")
            }

            emitView(::FrameLayout, {}) {
                renderViews(webContext = webContext)
            }
        }
    }

    override fun onBackPressed() {
        if (webContext.canGoBack()) {
            webContext.goBack()
        } else {
            super.onBackPressed()
        }
    }
}

@Composable
fun renderViews(webParams: WebParams = WebParams(), webContext: WebContext) {
    if (WebContext.debug) {
        Log.d("WebCompAct", "renderViews")
    }

    val displayedUrl = state { "https://www.google.com" }

    fun updateDisplayedUrl(newValue: String?) {
        if (!newValue.isNullOrBlank() && newValue != displayedUrl.value) {
            displayedUrl.value = newValue
        }
    }

    emitView(::LinearLayout, {
        it.orientation = LinearLayout.VERTICAL
        it.setLayoutWidth(MATCH_PARENT)
        it.setLayoutHeight(MATCH_PARENT)
    }) {
        emitView(::LinearLayout, {
            it.orientation = LinearLayout.HORIZONTAL
            it.setLayoutWidth(MATCH_PARENT)
            it.setLayoutHeight(WRAP_CONTENT)
            it.weightSum = 1f
        }) {
            emitView(::Button) {
                it.setLayoutWidth(40.dp)
                it.setLayoutHeight(WRAP_CONTENT)
                it.text = ""
                it.setOnClickListener {
                    webContext.goBack()
                }
            }
            emitView(::Button) {
                it.setLayoutWidth(40.dp)
                it.setLayoutHeight(WRAP_CONTENT)
                it.text = ""
                it.setOnClickListener {
                    webContext.goForward()
                }
            }
            emitView(::EditText) {
                it.setLayoutWidth(0.dp)
                it.setLayoutHeight(WRAP_CONTENT)
                it.setLayoutWeight(1f)
                it.isSingleLine = true
                it.setControlledText(displayedUrl.value)
                it.setOnTextChanged { s: CharSequence?, _, _, _ ->
                    displayedUrl.value = s.toString()
                }
            }
            emitView(::Button) {
                it.setLayoutWidth(WRAP_CONTENT)
                it.setLayoutHeight(WRAP_CONTENT)
                it.text = "Go"
                it.setOnClickListener {
                    if (displayedUrl.value.isNotBlank()) {
                        if (WebContext.debug) {
                            Log.d("WebCompAct", "setting url to " + displayedUrl.value)
                        }
                        webParams.url = displayedUrl.value
                    }
                }
            }
        }

        if (WebContext.debug) {
            Log.d("WebCompAct", "webComponent: start")
        }

        WebComponent(
            url = webParams.url,
            webViewClient = object : WebViewClient() {

                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                    updateDisplayedUrl(url)
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    updateDisplayedUrl(url)
                }

                // We support API 21 and above, so we're using the deprecated version.
                override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                    updateDisplayedUrl(url)
                    return false
                }
            },
            webContext = webContext
        )

        if (WebContext.debug) {
            Log.d("WebCompAct", "webComponent: end")
        }
    }
}