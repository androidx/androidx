/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.compose.remote.integration.demos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.remote.creation.compose.capture.RememberRemoteDocumentInline
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.layout.RemoteText
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.integration.demos.preview.RemoteComposePreview
import androidx.compose.remote.integration.demos.ui.theme.RemoteComposeDemosTheme
import androidx.compose.remote.player.compose.RemoteDocumentPlayer
import androidx.compose.remote.player.core.RemoteComposeDocument
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.tooling.preview.Preview

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RemoteComposeDemosTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Main(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
@Suppress("RestrictedApiAndroidX")
fun Main(modifier: Modifier = Modifier) {
    var documentState by remember { mutableStateOf<RemoteComposeDocument?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        RememberRemoteDocumentInline(
            onDocument = { doc ->
                println("Document generated: $doc")
                if (documentState == null) {
                    // Generate seems to get called again with a partial document
                    // Essentially re-recording but with existing state, so document is incomplete
                    documentState = RemoteComposeDocument(doc)
                }
            }
        ) {
            Greeting(modifier = RemoteModifier.fillMaxSize())
        }

        if (documentState != null) {
            val windowInfo = LocalWindowInfo.current
            RemoteDocumentPlayer(
                document = documentState!!.document,
                windowInfo.containerSize.width,
                windowInfo.containerSize.height,
                modifier = modifier.fillMaxSize(),
                0,
                onNamedAction = { _, _, _ -> },
            )
        }
    }
}

@RemoteComposable
@Composable
@Suppress("RestrictedApiAndroidX")
fun Greeting(modifier: RemoteModifier = RemoteModifier) {
    RemoteBox(modifier = modifier) { RemoteText(text = "Hello world!") }
}

@Suppress("RestrictedApiAndroidX")
@Preview
@RemoteComposable
@Composable
fun GreetingPreview() = RemoteComposePreview { Greeting() }
