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
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.webkit.WebView
import android.widget.BaseAdapter
import android.widget.FrameLayout
import android.widget.ListView
import android.widget.RemoteViews
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.NonNull
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.remote.creation.compose.capture.captureSingleRemoteDocument
import androidx.compose.remote.creation.compose.layout.RemoteColumn
import androidx.compose.remote.creation.compose.layout.RemoteText
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.contentDescription
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.modifier.fillMaxWidth
import androidx.compose.remote.creation.compose.modifier.padding
import androidx.compose.remote.creation.compose.modifier.rememberRemoteScrollState
import androidx.compose.remote.creation.compose.modifier.semantics
import androidx.compose.remote.creation.compose.modifier.verticalScroll
import androidx.compose.remote.creation.compose.state.rdp
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.trace

class ScrollableListActivity : ComponentActivity() {

    @Composable
    fun RemoteComposePlayer(@NonNull remoteDocumentBytes: ByteArray) {
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
            MODE_REMOTE_VIEW -> setRemoteViewsScrollContent()
            else -> setContent { RemoteCompose() }
        }
    }

    private fun setRemoteViewsScrollContent() {
        val container =
            FrameLayout(this).apply {
                layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT)
            }

        val remoteViews = RemoteViews(packageName, R.layout.remoteviews_native_scroll_container)

        // 3. Apply the RemoteViews hierarchy and attach it to the FrameLayout
        val appliedView = remoteViews.apply(this, container)
        container.addView(appliedView)

        val listView = appliedView.findViewById<ListView>(R.id.list_view)
        val adapter =
            object : BaseAdapter() {
                override fun getCount(): Int = 500

                override fun getItem(position: Int): Any = position

                override fun getItemId(position: Int): Long = position.toLong()

                override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
                    val item = RemoteViews(packageName, R.layout.remoteviews_text_item)
                    item.setTextViewText(R.id.text_view, "Item $position")
                    return item.apply(this@ScrollableListActivity, parent ?: listView)
                }
            }
        listView.adapter = adapter

        setContentView(container)
    }

    private fun setWebViewContent() {
        setContentView(
            WebView(this@ScrollableListActivity).apply {
                contentDescription = LIST_CONTENT_DESCRIPTION
                // 1. Initialize the HTML string with basic mobile styling
                val htmlBuilder = StringBuilder()
                htmlBuilder.append(
                    """
                    <!DOCTYPE html>
                    <html>
                    <head>
                        <meta name="viewport" content="width=device-width, initial-scale=1">
                        <style>
                            body { font-family: sans-serif; margin: 0; padding: 0; background-color: #ffffff; }
                            ul { list-style-type: none; padding: 0; margin: 0; }
                            li {
                                padding: 18px 16px;
                                border-bottom: 1px solid #e0e0e0;
                                font-size: 16px;
                                color: #333333;
                            }
                        </style>
                    </head>
                    <body>
                        <ul aria-label="$LIST_CONTENT_DESCRIPTION">
                    """
                        .trimIndent()
                )

                // 2. Loop to generate the 500 items
                for (i in 1..500) {
                    htmlBuilder.append("<li>Item ${i}</li>\n")
                }

                // 3. Close the HTML tags
                htmlBuilder.append(
                    """
                        </ul>
                    </body>
                    </html>
                    """
                        .trimIndent()
                )

                loadDataWithBaseURL(null, htmlBuilder.toString(), "text/html", "UTF-8", null)
            }
        )
    }

    @Composable
    fun LiveCompose() {
        Column(
            modifier =
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).semantics {
                    contentDescription = LIST_CONTENT_DESCRIPTION
                }
        ) {
            repeat(500) { index ->
                Text("Item $index", modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp))
            }
        }
    }

    @Composable
    fun RemoteCompose() {
        var documentBytes by remember { mutableStateOf<ByteArray?>(null) }
        val context = LocalContext.current
        LaunchedEffect(Unit) {
            documentBytes =
                captureSingleRemoteDocument(
                        profile = RcPlatformProfiles.ANDROIDX,
                        context = context,
                    ) {
                        val scrollState = rememberRemoteScrollState()
                        RemoteColumn(
                            modifier =
                                RemoteModifier.fillMaxSize()
                                    .semantics { contentDescription = LIST_CONTENT_DESCRIPTION.rs }
                                    .verticalScroll(scrollState)
                        ) {
                            repeat(500) { index ->
                                RemoteText(
                                    ("Item $index").rs,
                                    modifier =
                                        RemoteModifier.fillMaxWidth().padding(vertical = 8.rdp),
                                )
                            }
                        }
                    }
                    .bytes
        }

        documentBytes?.let { RemoteComposePlayer(it) }
    }
}
