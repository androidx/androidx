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
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.remote.creation.compose.capture.captureSingleRemoteDocument
import androidx.compose.remote.creation.compose.layout.RemoteAlignment
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteTime
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.background
import androidx.compose.remote.creation.compose.modifier.contentDescription
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.modifier.rotate
import androidx.compose.remote.creation.compose.modifier.semantics
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.creation.profile.RcPlatformProfiles
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.runBlocking

/**
 * Activity hosting a continuous animation (indefinite rotating spinner) to benchmark sustained
 * frame timing and jank of Embedded Player vs Java Player.
 */
class RemotePlayerAnimationActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val mode = intent.getStringExtra(BENCHMARK_MODE_ARG) ?: MODE_EMBEDDED_PLAYER

        setContent {
            val context = LocalContext.current
            var documentBytes by remember {
                mutableStateOf(
                    cachedAnimationDocument
                        ?: runBlocking {
                            createAnimationDocument(context).also { cachedAnimationDocument = it }
                        }
                )
            }

            RemotePlayerHost(
                playerMode = mode,
                remoteDocumentBytes = documentBytes,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }

    companion object {
        private var cachedAnimationDocument: ByteArray? = null

        private suspend fun createAnimationDocument(context: Context): ByteArray {
            return captureSingleRemoteDocument(
                    profile = RcPlatformProfiles.ANDROIDX,
                    context = context,
                ) {
                    val time = RemoteTime().ContinuousSec()
                    val rotation = (time * 360f.rf) % 360f.rf

                    RemoteBox(
                        modifier =
                            RemoteModifier.fillMaxSize().semantics {
                                contentDescription = SPINNER_CONTENT_DESCRIPTION.rs
                            },
                        contentAlignment = RemoteAlignment.Center,
                    ) {
                        RemoteBox(
                            modifier =
                                RemoteModifier.size(100.rdp).rotate(rotation).semantics {
                                    contentDescription = SPINNER_CONTENT_DESCRIPTION.rs
                                }
                        ) {
                            RemoteBox(
                                modifier =
                                    RemoteModifier.size(28.rdp).background(Color(0xFF2196F3).rc)
                            )
                            RemoteBox(
                                modifier =
                                    RemoteModifier.size(16.rdp).background(Color(0xFFFF5722).rc)
                            )
                        }
                    }
                }
                .bytes
        }
    }
}
