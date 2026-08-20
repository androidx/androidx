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

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import java.util.function.Function

/**
 * A fallback mock implementation of [WebContent] used when the true WebContent feature isn't
 * supported, or when manually forcing the fallback mode for testing layout and behavior.
 */
class FallbackWebContent(private val onDetach: () -> Unit = {}) : WebContent {
    override fun <T : WebContentView> attach(context: Context, factory: Function<Context, T>): T =
        factory.apply(context)

    override fun detach() {
        onDetach()
    }

    override fun close() {}
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
    var forceFallback by rememberSaveable { mutableStateOf(false) }

    var lastLoadedUrl by retain { mutableStateOf<String?>(null) }

    val webContent =
        retain(forceFallback) {
            if (!forceFallback && WebFeature.isFeatureSupported(WebFeature.WEB_CONTENT)) {
                WebContent()
            } else {
                FallbackWebContent(onDetach = { lastLoadedUrl = null })
            }
        }

    RetainedEffect(webContent) {
        onRetire {
            webContent.close()
            lastLoadedUrl = null
        }
    }

    var currentUrl by rememberSaveable { mutableStateOf("https://www.example.com") }
    var urlInput by rememberSaveable { mutableStateOf(currentUrl) }
    var showWebView by rememberSaveable { mutableStateOf(true) }

    Scaffold(
        topBar = {
            Column(Modifier.padding(8.dp)) {
                Row(Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = urlInput,
                        onValueChange = { urlInput = it },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                    )
                    Button(
                        onClick = { currentUrl = urlInput },
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
                Row(Modifier.fillMaxWidth()) {
                    Button(onClick = { forceFallback = !forceFallback }) {
                        Text(
                            if (forceFallback) "Using Fallback (Click to use WebContent)"
                            else "Using WebContent (Click to force Fallback)"
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
