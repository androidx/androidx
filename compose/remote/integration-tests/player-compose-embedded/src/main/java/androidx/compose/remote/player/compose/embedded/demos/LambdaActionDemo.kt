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

@file:Suppress("RestrictedApiAndroidX")

package androidx.compose.remote.player.compose.embedded.demos

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.remote.creation.compose.action.lambdaAction
import androidx.compose.remote.creation.compose.capture.captureSingleRemoteDocument
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.background
import androidx.compose.remote.creation.compose.modifier.clickable
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.player.compose.embedded.RcPlayer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.runBlocking

/**
 * A demo showing how to use [lambdaAction] to execute host-side code from a Remote Compose document
 * played via the embedded [RcPlayer].
 */
@Composable
@Preview(showBackground = true)
public fun LambdaActionDemo() {
    val context = LocalContext.current
    var clickCount by remember { mutableIntStateOf(0) }

    // 1. Define the Remote Content
    // This is what would typically be generated on the server or in another process.
    val remoteContent: @Composable @RemoteComposable () -> Unit = {
        RemoteBox(
            modifier =
                RemoteModifier.size(100.rdp)
                    .background(Color.Blue)
                    .clickable(action = lambdaAction { clickCount++ })
        )
    }

    // 2. Capture the document
    // In a real scenario, this might be received over the network as bytes.
    // For this demo, we generate it inline.
    val capturedDoc = remember {
        runBlocking { captureSingleRemoteDocument(context = context, content = remoteContent) }
    }

    // 3. Playback with Embedded Player
    Column(modifier = Modifier.padding(16.dp)) {
        Text("Host State (Click Count): $clickCount")
        Spacer(modifier = Modifier.height(8.dp))

        RcPlayer(capturedDocument = capturedDoc, modifier = Modifier.size(100.dp))
    }
}
