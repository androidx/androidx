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

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.ReportDrawn
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.remote.creation.compose.capture.captureSingleRemoteDocument
import androidx.compose.remote.creation.compose.layout.RemoteAlignment
import androidx.compose.remote.creation.compose.layout.RemoteArrangement
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteColumn
import androidx.compose.remote.creation.compose.layout.RemoteRow
import androidx.compose.remote.creation.compose.layout.RemoteText
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.background
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.modifier.fillMaxWidth
import androidx.compose.remote.creation.compose.modifier.padding
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.creation.profile.RcPlatformProfiles
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import kotlinx.coroutines.runBlocking

/**
 * Activity measuring the load time and first-frame rendering time of a rich Remote Compose document
 * using either the Embedded Player or Java Player.
 */
class RemotePlayerLoadTimeActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val mode = intent.getStringExtra(BENCHMARK_MODE_ARG) ?: MODE_EMBEDDED_PLAYER

        setContent {
            val context = LocalContext.current
            var documentBytes by remember {
                mutableStateOf(
                    cachedLoadTimeDocument
                        ?: runBlocking {
                            createLoadTimeDocument(context).also { cachedLoadTimeDocument = it }
                        }
                )
            }

            Box(
                modifier =
                    Modifier.fillMaxSize().semantics {
                        contentDescription = LOAD_TIME_CONTENT_DESCRIPTION
                    }
            ) {
                RemotePlayerHost(
                    playerMode = mode,
                    remoteDocumentBytes = documentBytes,
                    modifier = Modifier.fillMaxSize(),
                )
                ReportDrawn()
            }
        }
    }

    companion object {
        private var cachedLoadTimeDocument: ByteArray? = null

        private suspend fun createLoadTimeDocument(context: Context): ByteArray {
            return captureSingleRemoteDocument(
                    profile = RcPlatformProfiles.ANDROIDX,
                    context = context,
                ) {
                    RemoteBox(
                        modifier = RemoteModifier.fillMaxSize().padding(16.rdp),
                        contentAlignment = RemoteAlignment.TopCenter,
                    ) {
                        RemoteColumn(
                            modifier = RemoteModifier.fillMaxWidth(),
                            verticalArrangement = RemoteArrangement.Top,
                            horizontalAlignment = RemoteAlignment.CenterHorizontally,
                        ) {
                            RemoteText(
                                text = "Dashboard Overview".rs,
                                modifier = RemoteModifier.padding(bottom = 8.rdp),
                            )
                            RemoteText(
                                text = "Real-time Metrics & Insights".rs,
                                modifier = RemoteModifier.padding(bottom = 16.rdp),
                            )
                            RemoteBox(
                                modifier =
                                    RemoteModifier.fillMaxWidth()
                                        .size(320.rdp, 120.rdp)
                                        .background(Color(0xFF3F51B5).rc)
                                        .padding(16.rdp),
                                contentAlignment = RemoteAlignment.Center,
                            ) {
                                RemoteText(text = "Primary Status Banner".rs)
                            }
                            repeat(3) { rowIndex ->
                                RemoteRow(
                                    modifier = RemoteModifier.fillMaxWidth().padding(top = 12.rdp),
                                    horizontalArrangement = RemoteArrangement.SpaceEvenly,
                                ) {
                                    repeat(2) { colIndex ->
                                        val cardIndex = rowIndex * 2 + colIndex + 1
                                        RemoteBox(
                                            modifier =
                                                RemoteModifier.size(150.rdp, 90.rdp)
                                                    .background(Color(0xFF26A69A).rc)
                                                    .padding(8.rdp),
                                            contentAlignment = RemoteAlignment.Center,
                                        ) {
                                            RemoteColumn(
                                                horizontalAlignment =
                                                    RemoteAlignment.CenterHorizontally
                                            ) {
                                                RemoteText(text = "Card $cardIndex".rs)
                                                RemoteBox(
                                                    modifier =
                                                        RemoteModifier.size(24.rdp)
                                                            .background(Color.White.rc)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                .bytes
        }
    }
}
