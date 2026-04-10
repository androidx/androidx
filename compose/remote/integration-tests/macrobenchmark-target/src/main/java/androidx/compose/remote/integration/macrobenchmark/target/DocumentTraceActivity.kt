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
import java.util.Objects.requireNonNull

object BenchmarkCache {
    var documentBytes: ByteArray? = null
}

open class DocumentTraceActivity : ComponentActivity() {

    @Composable
    open fun RemoteComposePlayer(@NonNull remoteDocumentBytes: ByteArray) {
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

        setContent {
            when (intent.getStringExtra(BENCHMARK_MODE_ARG)) {
                MODE_RENDER_FROM_CACHE -> {
                    requireNonNull(BenchmarkCache.documentBytes, "BenchmarkCache is empty")
                    RemoteComposePlayer(BenchmarkCache.documentBytes!!)
                }
                MODE_LOCAL -> {
                    Column(
                        modifier =
                            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).semantics {
                                contentDescription = LIST_CONTENT_DESCRIPTION
                            }
                    ) {
                        repeat(500) { index ->
                            Text(
                                "Item $index",
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            )
                        }
                    }
                }
                else -> {
                    ScrollableRemoteContent()
                }
            }
        }
    }

    @Composable
    @Suppress("RestrictedApiAndroidX")
    fun ScrollableRemoteContent() {
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

        // Play the remote document
        documentBytes?.let { RemoteComposePlayer(it) }
    }
}
