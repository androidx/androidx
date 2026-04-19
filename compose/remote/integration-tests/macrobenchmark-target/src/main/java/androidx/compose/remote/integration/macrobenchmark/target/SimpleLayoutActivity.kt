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

package androidx.compose.remote.integration.macrobenchmark.target

import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.RemoteViews
import androidx.activity.ComponentActivity
import androidx.activity.compose.ReportDrawn
import androidx.activity.compose.setContent
import androidx.annotation.NonNull
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.remote.creation.compose.capture.captureSingleRemoteDocument
import androidx.compose.remote.creation.compose.layout.RemoteAlignment
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteText
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.contentDescription
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.modifier.semantics
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.creation.profile.RcPlatformProfiles
import androidx.compose.remote.player.compose.RemoteDocumentPlayer
import androidx.compose.remote.player.core.RemoteDocument
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.util.trace
import androidx.core.view.doOnPreDraw

class SimpleLayoutActivity : ComponentActivity() {

    @Composable
    private fun RemoteComposePlayer(@NonNull remoteDocumentBytes: ByteArray) {
        val windowInfo = LocalWindowInfo.current
        RemoteDocumentPlayer(
            document =
                remember(remoteDocumentBytes) {
                        trace("CreateRemoteDocument:parsing") {
                            RemoteDocument(remoteDocumentBytes)
                        }
                    }
                    .document,
            documentWidth = windowInfo.containerSize.width,
            documentHeight = windowInfo.containerSize.height,
            modifier = Modifier.fillMaxSize(),
            debugMode = 0,
            onNamedAction = { _, _, _ -> },
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        when (intent.getStringExtra(BENCHMARK_MODE_ARG)) {
            MODE_COMPOSE -> setContent { LiveCompose() }
            MODE_WEB_VIEW -> setWebViewContent()
            MODE_REMOTE_VIEW -> setRemoteViewsContent()
            else -> setContent { RemoteCompose() }
        }
    }

    private fun setWebViewContent() {
        setContentView(
            WebView(this@SimpleLayoutActivity).apply {
                webViewClient =
                    object : WebViewClient() {
                        override fun onPageFinished(view: WebView, url: String) {
                            super.onPageFinished(view, url)

                            // At this point, the HTML is loaded, but pixels aren't on the screen
                            // yet.
                            val requestId = 1L // Arbitrary ID to track the request

                            view.postVisualStateCallback(
                                requestId,
                                object : WebView.VisualStateCallback() {
                                    override fun onComplete(id: Long) {
                                        try {
                                            reportFullyDrawn()
                                        } catch (ignored: SecurityException) {}
                                    }
                                },
                            )
                        }
                    }
                contentDescription = LIST_CONTENT_DESCRIPTION
                val htmlBuilder = java.lang.StringBuilder()
                htmlBuilder.append(
                    """
                    <!DOCTYPE html>
                    <html>
                    <head>
                        <meta name="viewport" content="width=device-width, initial-scale=1">
                        <style>
                            body { font-family: sans-serif; margin: 0; padding: 0; background-color: #ffffff; }
                            div {
                                padding: 18px 16px;
                                font-size: 16px;
                                color: #333333;
                            }
                        </style>
                    </head>
                    <body>
                        <div>Hello World</div>
                    </body>
                    </html>
                    """
                        .trimIndent()
                )
                loadDataWithBaseURL(null, htmlBuilder.toString(), "text/html", "UTF-8", null)
            }
        )
    }

    private fun setRemoteViewsContent() {
        val container =
            android.widget.FrameLayout(this).apply {
                contentDescription = LIST_CONTENT_DESCRIPTION
                layoutParams =
                    android.view.ViewGroup.LayoutParams(
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    )
            }

        val remoteViews = RemoteViews(packageName, R.layout.remoteviews_text_item)
        remoteViews.setTextViewText(R.id.text_view, "Hello World")

        val appliedView = remoteViews.apply(this, container)
        container.addView(
            appliedView,
            FrameLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT, Gravity.CENTER),
        )
        appliedView.doOnPreDraw { reportFullyDrawn() }
        setContentView(container)
    }

    @Composable
    private fun LiveCompose() {
        Box(
            modifier =
                Modifier.fillMaxSize().semantics { contentDescription = LIST_CONTENT_DESCRIPTION },
            contentAlignment = Alignment.Center,
        ) {
            Text("Hello World")
            ReportDrawn()
        }
    }

    @Composable
    private fun RemoteCompose() {
        var documentBytes by remember { mutableStateOf<ByteArray?>(null) }
        val context = LocalContext.current
        LaunchedEffect(Unit) {
            documentBytes =
                captureSingleRemoteDocument(
                        profile = RcPlatformProfiles.ANDROIDX,
                        context = context,
                    ) {
                        RemoteBox(
                            modifier =
                                RemoteModifier.fillMaxSize().semantics {
                                    contentDescription = LIST_CONTENT_DESCRIPTION.rs
                                },
                            contentAlignment = RemoteAlignment.Center,
                        ) {
                            RemoteText(("Hello World").rs)
                        }
                    }
                    .bytes
        }

        documentBytes?.let { RemoteComposePlayer(it) }
    }
}
