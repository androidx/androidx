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
import androidx.activity.compose.ReportDrawn
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.remote.creation.compose.layout.RemoteAlignment
import androidx.compose.remote.creation.compose.layout.RemoteArrangement
import androidx.compose.remote.creation.compose.layout.RemoteColumn
import androidx.compose.remote.creation.compose.layout.RemoteText
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.contentDescription
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.modifier.padding
import androidx.compose.remote.creation.compose.modifier.semantics
import androidx.compose.remote.creation.compose.state.RemoteString.Companion.createNamedRemoteString
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.creation.compose.state.rsp
import androidx.compose.remote.player.compose.ExperimentalRemotePlayerApi
import androidx.compose.remote.player.compose.RemoteComposePlayerFlags
import androidx.compose.remote.player.compose.embedded.RcPlayerState
import androidx.compose.remote.player.view.RemoteComposePlayer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.util.trace
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Target activity for benchmarking state publishing every 100ms, updating text.
 *
 * Compares how the embedded Compose player ([RcPlayer]) vs Java player ([RemoteComposePlayer])
 * handles rapid state updates and UI invalidations.
 */
class RemotePlayerStateUpdateActivity : ComponentActivity() {
    private var updateJob: Job? = null

    @OptIn(ExperimentalRemotePlayerApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val mode = intent.getStringExtra(BENCHMARK_MODE_ARG) ?: MODE_EMBEDDED_PLAYER
        RemoteComposePlayerFlags.isEmbeddedPlayerEnabled = playerVariantFromMode(mode).second

        setContent {
            StateUpdateScreen(
                mode = mode,
                onJavaPlayerReady = { player -> startJavaPlayerUpdates(player) },
                onEmbeddedStateReady = { state -> startEmbeddedPlayerUpdates(state) },
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        updateJob?.cancel()
    }

    private fun startJavaPlayerUpdates(player: RemoteComposePlayer) {
        updateJob?.cancel()
        updateJob = lifecycleScope.launch {
            var count = 0
            while (isActive) {
                delay(100L)
                count++
                trace("RemoteBench:hostUpdate") {
                    player.setUserLocalString("counterText", "Count: $count")
                    // TODO(b/585202905): Remove explicit invalidate() once
                    // RemoteComposeView.setLocal*
                    // invalidates the view.
                    player.invalidate()
                }
            }
        }
    }

    private fun startEmbeddedPlayerUpdates(state: RcPlayerState) {
        updateJob?.cancel()
        val counterState = state.stringState("counterText")
        updateJob = lifecycleScope.launch {
            var count = 0
            while (isActive) {
                delay(100L)
                count++
                trace("RemoteBench:hostUpdate") {
                    counterState.value = "Count: $count"
                }
            }
        }
    }

    @Composable
    private fun StateUpdateScreen(
        mode: String,
        onJavaPlayerReady: (RemoteComposePlayer) -> Unit,
        onEmbeddedStateReady: (RcPlayerState) -> Unit,
    ) {
        var documentBytes by remember { mutableStateOf<ByteArray?>(null) }
        val context = LocalContext.current

        LaunchedEffect(Unit) {
            documentBytes =
                BenchDocuments.getOrCapture(context, "state_update_counter") {
                    val counterText = remember {
                        createNamedRemoteString("counterText", "Count: 0")
                    }
                    RemoteColumn(
                        horizontalAlignment = RemoteAlignment.CenterHorizontally,
                        verticalArrangement = RemoteArrangement.Center,
                        modifier =
                            RemoteModifier.fillMaxSize().padding(16.rdp).semantics {
                                contentDescription = STATE_UPDATE_CONTENT_DESCRIPTION.rs
                            },
                    ) {
                        RemoteText(
                            text = "State Update Benchmark".rs,
                            fontSize = 24.rsp,
                            modifier = RemoteModifier.padding(bottom = 16.rdp),
                        )
                        RemoteText(
                            text = counterText,
                            fontSize = 32.rsp,
                            modifier =
                                RemoteModifier.padding(16.rdp).semantics {
                                    contentDescription = STATE_UPDATE_CONTENT_DESCRIPTION.rs
                                },
                        )
                    }
                }
        }

        documentBytes?.let { bytes ->
            RemotePlayerHost(
                playerMode = mode,
                remoteDocumentBytes = bytes,
                modifier = Modifier.fillMaxSize(),
                onJavaPlayerReady = onJavaPlayerReady,
                onEmbeddedStateReady = onEmbeddedStateReady,
            )
            ReportDrawn()
        }
    }
}
