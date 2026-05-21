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

package androidx.compose.remote.integration.demos.modifier

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.remote.creation.compose.layout.RemoteAlignment
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteColumn
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.layout.RemoteText
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.background
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.modifier.rotate
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.integration.demos.common.RemoteDemo
import androidx.compose.remote.tooling.preview.RemoteComponentPreview
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Suppress("RestrictedApiAndroidX")
@Composable
fun RotateDemo() {
    RemoteDemo(modifier = Modifier.fillMaxSize().padding(16.dp)) { RotateDemoContent() }
}

@Suppress("RestrictedApiAndroidX")
@RemoteComponentPreview
@Composable
@RemoteComposable
private fun RotateDemoContent() {
    RemoteColumn(modifier = RemoteModifier.fillMaxSize()) {
        RemoteText("Rotate: 0f")
        Content(RemoteModifier.rotate(0f.rf))

        RemoteText("Rotate: 45f")
        Content(RemoteModifier.rotate(45f.rf))

        RemoteText("Rotate: 60f")
        Content(RemoteModifier.rotate(60f.rf))

        RemoteText("Rotate: 90f")
        Content(RemoteModifier.rotate(90f.rf))

        RemoteText("Rotate: -45f")
        Content(RemoteModifier.rotate((-45f).rf))
    }
}

@Suppress("RestrictedApiAndroidX")
@Composable
@RemoteComposable
private fun Content(testModifier: RemoteModifier) {
    RemoteBox(
        modifier = RemoteModifier.size(100.rdp).background(Color.Red),
        contentAlignment = RemoteAlignment.Center,
    ) {
        RemoteBox(modifier = testModifier.size(50.rdp).background(Color.Blue))
    }
}
