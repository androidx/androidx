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

package androidx.web.testapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.Button
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.retain.RetainedEffect
import androidx.compose.runtime.retain.retain
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.web.WebContent
import androidx.web.WebContentView
import androidx.web.WebFeature

/**
 * Creates and retains a [WebContent] instance across configuration changes, automatically releasing
 * it via [RetainedEffect] when retired.
 */
@Composable
@Suppress("RestrictedApiAndroidX")
fun rememberWebContent(block: WebContent.Builder.() -> Unit = {}): WebContent {
    val webContent = retain { WebContent(block) }
    RetainedEffect(webContent) {
        onRetire {
            webContent.close()
        }
    }
    return webContent
}

/**
 * The primary Compose-based Activity demonstrating [WebContent] usage. Showcases embedding a
 * WebContentView within Compose and retaining WebContent instances.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { AppContent() }
    }
}

@Composable
@Suppress("RestrictedApiAndroidX")
fun AppContent() {
    if (!WebFeature.isFeatureSupported(WebFeature.WEB_CONTENT)) {
        Text("WebContent feature is not supported on this device.")
        return
    }

    val webContent = rememberWebContent()
    var lastLoadedUrl by retain { mutableStateOf<String?>(null) }

    var currentUrl by rememberSaveable { mutableStateOf("https://www.example.com") }
    val urlInputState = rememberTextFieldState(currentUrl)
    var showWebView by rememberSaveable { mutableStateOf(true) }

    Scaffold(
        topBar = {
            Column(Modifier.padding(8.dp)) {
                Row(Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        state = urlInputState,
                        modifier = Modifier.weight(1f),
                        lineLimits = TextFieldLineLimits.SingleLine,
                    )
                    Button(
                        onClick = { currentUrl = urlInputState.text.toString() },
                        modifier = Modifier.padding(start = 8.dp),
                    ) {
                        Text("Go")
                    }
                }
                Row(Modifier.fillMaxWidth()) {
                    Button(onClick = { showWebView = !showWebView }) {
                        Text(
                            if (showWebView) "Hide WebView (Leave Composition)"
                            else "Show WebView (Enter Composition)"
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        if (showWebView) {
            key(webContent) {
                AndroidView(
                    factory = { ctx ->
                        webContent.attach(ctx, ::WebContentView).also {
                            it.settings.javaScriptEnabled = true
                        }
                    },
                    update = { view ->
                        if (lastLoadedUrl != currentUrl) {
                            view.loadUrl(currentUrl)
                            lastLoadedUrl = currentUrl
                        }
                    },
                    onRelease = { webContent.detach() },
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                )
            }
        }
    }
}
