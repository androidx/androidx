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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import kotlinx.coroutines.runBlocking

open class DocumentGenerationActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        runBlocking {
            BenchmarkCache.documentBytes =
                captureSingleRemoteDocument(
                        profile = RcPlatformProfiles.ANDROIDX,
                        context = this@DocumentGenerationActivity,
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
        setContent {
            Box(
                Modifier.semantics { contentDescription = DOCUMENT_READY }.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(DOCUMENT_READY)
            }
        }
    }
}
