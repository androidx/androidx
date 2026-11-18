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
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteColumn
import androidx.compose.remote.creation.compose.layout.RemoteStateLayout
import androidx.compose.remote.creation.compose.layout.RemoteText
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.background
import androidx.compose.remote.creation.compose.modifier.contentDescription
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.modifier.fillMaxWidth
import androidx.compose.remote.creation.compose.modifier.height
import androidx.compose.remote.creation.compose.modifier.padding
import androidx.compose.remote.creation.compose.modifier.semantics
import androidx.compose.remote.creation.compose.state.RemoteInt.Companion.createNamedRemoteInt
import androidx.compose.remote.creation.compose.state.RemoteString.Companion.createNamedRemoteString
import androidx.compose.remote.creation.compose.state.rc
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.util.trace
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Target activity for parameterized state benchmarks:
 * - `mode = "s1"`: [valueCount] (1/8/32) named strings, updating `v0` every [intervalMs] ms.
 * - `mode = "s2a"`: 4-state [RemoteStateLayout], cycling `stateIndex` every [intervalMs] ms.
 */
class RemoteStateBenchActivity : ComponentActivity() {
    private var updateJob: Job? = null

    @OptIn(ExperimentalRemotePlayerApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val playerMode = intent.getStringExtra("player") ?: MODE_EMBEDDED_PLAYER
        val benchMode = intent.getStringExtra("mode") ?: "s1"
        val valueCount = intent.getIntExtra("valueCount", 1).coerceAtLeast(1)
        val intervalMs = intent.getLongExtra("intervalMs", 100L).coerceAtLeast(1L)

        RemoteComposePlayerFlags.isEmbeddedPlayerEnabled = playerVariantFromMode(playerMode).second

        val updateIndex = valueCount / 2
        setContent {
            StateBenchScreen(
                playerMode = playerMode,
                benchMode = benchMode,
                valueCount = valueCount,
                onJavaPlayerReady = { player ->
                    startJavaUpdates(player, benchMode, updateIndex, intervalMs)
                },
                onEmbeddedStateReady = { state ->
                    startEmbeddedUpdates(state, benchMode, updateIndex, intervalMs)
                },
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        updateJob?.cancel()
    }

    private fun startJavaUpdates(
        player: RemoteComposePlayer,
        benchMode: String,
        updateIndex: Int,
        intervalMs: Long,
    ) {
        updateJob?.cancel()
        updateJob = lifecycleScope.launch {
            var tick = 0
            while (isActive) {
                delay(intervalMs)
                tick++
                trace("RemoteBench:hostUpdate") {
                    if (benchMode == "s2a") {
                        player.setUserLocalInt("stateIndex", tick % 4)
                    } else {
                        player.setUserLocalString("v$updateIndex", "Value $updateIndex: $tick")
                    }
                    // TODO(b/585202905): Remove explicit invalidateViewTree() once
                    // RemoteComposeView.setLocal*
                    // invalidates the view.
                    invalidateViewTree(player)
                }
            }
        }
    }

    private fun startEmbeddedUpdates(
        rcState: RcPlayerState,
        benchMode: String,
        updateIndex: Int,
        intervalMs: Long,
    ) {
        updateJob?.cancel()
        if (benchMode == "s2a") {
            val stateIndex = rcState.intState("stateIndex")
            updateJob = lifecycleScope.launch {
                var tick = 0
                while (isActive) {
                    delay(intervalMs)
                    tick++
                    trace("RemoteBench:hostUpdate") { stateIndex.value = tick % 4 }
                }
            }
        } else {
            val updated = rcState.stringState("v$updateIndex")
            updateJob = lifecycleScope.launch {
                var tick = 0
                while (isActive) {
                    delay(intervalMs)
                    tick++
                    trace("RemoteBench:hostUpdate") { updated.value = "Value $updateIndex: $tick" }
                }
            }
        }
    }

    @Composable
    private fun StateBenchScreen(
        playerMode: String,
        benchMode: String,
        valueCount: Int,
        onJavaPlayerReady: (RemoteComposePlayer) -> Unit,
        onEmbeddedStateReady: (RcPlayerState) -> Unit,
    ) {
        var documentBytes by remember { mutableStateOf<ByteArray?>(null) }
        val context = LocalContext.current

        LaunchedEffect(benchMode, valueCount, playerMode) {
            val useGlobalScope = playerVariantFromMode(playerMode).first != PlayerVariant.Embedded
            documentBytes =
                if (benchMode == "s2a") {
                    BenchDocuments.getOrCapture(
                        context = context,
                        name = "state_layout_4state",
                        globalScope = useGlobalScope,
                    ) {
                        val stateIndex = remember {
                            createNamedRemoteInt("stateIndex", 0)
                        }
                            .withGlobalScopeIf(useGlobalScope)
                        RemoteColumn(
                            horizontalAlignment = RemoteAlignment.CenterHorizontally,
                            verticalArrangement = RemoteArrangement.Center,
                            modifier =
                                RemoteModifier.fillMaxSize()
                                    .background(Color.White.rc)
                                    .padding(16.rdp)
                                    .semantics {
                                        contentDescription = STATE_BENCH_CONTENT_DESCRIPTION.rs
                                    },
                        ) {
                            RemoteStateLayout(
                                currentState = stateIndex,
                                0,
                                1,
                                2,
                                3,
                                modifier = RemoteModifier.fillMaxWidth().height(200.rdp),
                            ) { index ->
                                val (bg, fg, label) =
                                    when (index) {
                                        0 -> Triple(Color(0xFFFFCDD2), Color(0xFFB71C1C), "State 0")
                                        1 -> Triple(Color(0xFFC8E6C9), Color(0xFF1B5E20), "State 1")
                                        2 -> Triple(Color(0xFFBBDEFB), Color(0xFF0D47A1), "State 2")
                                        else ->
                                            Triple(Color(0xFFFFF9C4), Color(0xFFF57F17), "State 3")
                                    }
                                RemoteBox(
                                    contentAlignment = RemoteAlignment.Center,
                                    modifier =
                                        RemoteModifier.fillMaxSize()
                                            .background(bg.rc)
                                            .padding(16.rdp),
                                ) {
                                    RemoteText(text = label.rs, fontSize = 24.rsp, color = fg.rc)
                                }
                            }
                        }
                    }
                } else {
                    BenchDocuments.getOrCapture(
                        context = context,
                        name = "state_s1_$valueCount",
                        globalScope = useGlobalScope,
                    ) {
                        val values = remember {
                            List(valueCount) { i ->
                                createNamedRemoteString("v$i", "Value $i: 0")
                            }
                        }
                            .map { it.withGlobalScopeIf(useGlobalScope) }
                        RemoteColumn(
                            horizontalAlignment = RemoteAlignment.CenterHorizontally,
                            verticalArrangement = RemoteArrangement.Center,
                            modifier =
                                RemoteModifier.fillMaxSize()
                                    .background(Color.White.rc)
                                    .padding(16.rdp)
                                    .semantics {
                                        contentDescription = STATE_BENCH_CONTENT_DESCRIPTION.rs
                                    },
                        ) {
                            values.forEach { textVar ->
                                RemoteText(
                                    text = textVar,
                                    fontSize = 20.rsp,
                                    color = Color.Black.rc,
                                    modifier = RemoteModifier.padding(vertical = 2.rdp),
                                )
                            }
                        }
                    }
                }
        }

        documentBytes?.let { bytes ->
            RemotePlayerHost(
                playerMode = playerMode,
                remoteDocumentBytes = bytes,
                modifier = Modifier.fillMaxSize(),
                onJavaPlayerReady = onJavaPlayerReady,
                onEmbeddedStateReady = onEmbeddedStateReady,
            )
            ReportDrawn()
        }
    }
}
