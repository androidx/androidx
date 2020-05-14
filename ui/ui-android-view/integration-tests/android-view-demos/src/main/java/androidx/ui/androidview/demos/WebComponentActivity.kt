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
import androidx.compose.Composable
import androidx.compose.Stable
import androidx.compose.getValue
import androidx.compose.mutableStateOf
import androidx.compose.setValue
import androidx.compose.state
import androidx.ui.androidview.WebComponent
import androidx.ui.androidview.WebContext
import androidx.ui.core.setViewContent

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

            FrameLayout {
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

    LinearLayout(
        orientation = LinearLayout.VERTICAL,
        layoutWidth = MATCH_PARENT,
        layoutHeight = MATCH_PARENT
    ) {
        LinearLayout(
            orientation = LinearLayout.HORIZONTAL,
            layoutWidth = MATCH_PARENT,
            layoutHeight = WRAP_CONTENT,
            weightSum = 1f
        ) {
            Button(
                layoutWidth = 40.dp,
                layoutHeight = WRAP_CONTENT,
                text = "",
                onClick = {
                    webContext.goBack()
                })
            Button(
                layoutWidth = 40.dp,
                layoutHeight = WRAP_CONTENT,
                text = ") {",
                onClick = {
                    webContext.goForward()
                })
            EditText(
                layoutWidth = 0.dp,
                layoutHeight = WRAP_CONTENT,
                layoutWeight = 1f,
                singleLine = true,
                controlledText = displayedUrl.value,
                onTextChanged = { s: CharSequence?, _, _, _ ->
                    displayedUrl.value = s.toString()
                })
            Button(
                layoutWidth = WRAP_CONTENT,
                layoutHeight = WRAP_CONTENT,
                text = "Go",
                onClick = {
                    if (displayedUrl.value.isNotBlank()) {
                        if (WebContext.debug) {
                            Log.d("WebCompAct", "setting url to " + displayedUrl.value)
                        }
                        webParams.url = displayedUrl.value
                    }
                })
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