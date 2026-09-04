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
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.Operation
import androidx.compose.remote.core.operations.NamedVariable
import androidx.compose.remote.core.operations.layout.Container
import androidx.compose.remote.creation.compose.capture.captureSingleRemoteDocument
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
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rememberNamedRemoteInt
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.creation.compose.state.rsp
import androidx.compose.remote.creation.profile.RcPlatformProfiles
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
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Target activity for benchmarking State Layout changes every 100ms.
 *
 * Compares how the embedded Compose player ([RcPlayer]) vs Java player ([RemoteComposePlayer])
 * handles structural layout switching and conditional branch transitions driven by state machine
 * updates.
 */
class RemotePlayerStateLayoutActivity : ComponentActivity() {
    private var updateJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val mode = intent.getStringExtra(BENCHMARK_MODE_ARG) ?: MODE_EMBEDDED_PLAYER

        setContent {
            StateLayoutScreen(
                mode = mode,
                onJavaPlayerReady = { player -> startJavaPlayerUpdates(player) },
                onEmbeddedDocReady = { coreDoc -> startEmbeddedPlayerUpdates(coreDoc) },
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
            var state = 0
            while (isActive) {
                delay(100L)
                state = (state + 1) % 4
                player.setUserLocalInt("stateIndex", state)
            }
        }
    }

    private fun startEmbeddedPlayerUpdates(coreDoc: CoreDocument) {
        updateJob?.cancel()
        val varId = findVariableId(coreDoc, "stateIndex") ?: return
        updateJob = lifecycleScope.launch {
            var state = 0
            while (isActive) {
                delay(100L)
                state = (state + 1) % 4
                coreDoc.remoteComposeState.overrideInteger(varId, state)
            }
        }
    }

    private fun findVariableId(doc: CoreDocument, name: String): Int? {
        fun findIn(ops: List<Operation>): Int? {
            for (op in ops) {
                if (op is NamedVariable && (op.mVarName == name || op.mVarName == "USER:$name")) {
                    return op.mVarId
                }
                if (op is Container) {
                    val found = findIn(op.list)
                    if (found != null) return found
                }
            }
            return null
        }
        return findIn(doc.operations)
    }

    @Composable
    private fun StateLayoutScreen(
        mode: String,
        onJavaPlayerReady: (RemoteComposePlayer) -> Unit,
        onEmbeddedDocReady: (CoreDocument) -> Unit,
    ) {
        var documentBytes by remember { mutableStateOf<ByteArray?>(null) }
        val context = LocalContext.current

        LaunchedEffect(Unit) {
            documentBytes =
                captureSingleRemoteDocument(
                        profile = RcPlatformProfiles.ANDROIDX,
                        context = context,
                    ) {
                        val stateIndex = rememberNamedRemoteInt("stateIndex", 0)
                        RemoteColumn(
                            horizontalAlignment = RemoteAlignment.CenterHorizontally,
                            verticalArrangement = RemoteArrangement.Center,
                            modifier =
                                RemoteModifier.fillMaxSize().padding(16.rdp).semantics {
                                    contentDescription = STATE_LAYOUT_CONTENT_DESCRIPTION.rs
                                },
                        ) {
                            RemoteText(
                                text = "State Layout Benchmark".rs,
                                fontSize = 24.rsp,
                                modifier = RemoteModifier.padding(bottom = 16.rdp),
                            )
                            RemoteStateLayout(
                                currentState = stateIndex,
                                0,
                                1,
                                2,
                                3,
                                modifier = RemoteModifier.fillMaxWidth().height(200.rdp),
                            ) { index ->
                                when (index) {
                                    0 ->
                                        RemoteBox(
                                            modifier =
                                                RemoteModifier.fillMaxSize()
                                                    .background(Color(0xFFFFCDD2).rc)
                                                    .padding(16.rdp)
                                        ) {
                                            RemoteText(
                                                text = "State 0: Initializing".rs,
                                                fontSize = 20.rsp,
                                                color = Color(0xFFB71C1C).rc,
                                            )
                                        }
                                    1 ->
                                        RemoteBox(
                                            modifier =
                                                RemoteModifier.fillMaxSize()
                                                    .background(Color(0xFFC8E6C9).rc)
                                                    .padding(16.rdp)
                                        ) {
                                            RemoteText(
                                                text = "State 1: Processing".rs,
                                                fontSize = 20.rsp,
                                                color = Color(0xFF1B5E20).rc,
                                            )
                                        }
                                    2 ->
                                        RemoteBox(
                                            modifier =
                                                RemoteModifier.fillMaxSize()
                                                    .background(Color(0xFFBBDEFB).rc)
                                                    .padding(16.rdp)
                                        ) {
                                            RemoteText(
                                                text = "State 2: Transferring".rs,
                                                fontSize = 20.rsp,
                                                color = Color(0xFF0D47A1).rc,
                                            )
                                        }
                                    3 ->
                                        RemoteBox(
                                            modifier =
                                                RemoteModifier.fillMaxSize()
                                                    .background(Color(0xFFFFF9C4).rc)
                                                    .padding(16.rdp)
                                        ) {
                                            RemoteText(
                                                text = "State 3: Completed".rs,
                                                fontSize = 20.rsp,
                                                color = Color(0xFFF57F17).rc,
                                            )
                                        }
                                }
                            }
                        }
                    }
                    .bytes
        }

        documentBytes?.let { bytes ->
            RemotePlayerHost(
                playerMode = mode,
                remoteDocumentBytes = bytes,
                modifier = Modifier.fillMaxSize(),
                onJavaPlayerReady = onJavaPlayerReady,
                onEmbeddedDocReady = onEmbeddedDocReady,
            )
            ReportDrawn()
        }
    }
}
