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
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteColumn
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.layout.RemoteText
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.background
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.modifier.padding
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.integration.demos.common.RemoteDemo
import androidx.compose.remote.tooling.preview.RemoteComponentPreview
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Suppress("RestrictedApiAndroidX") // Referring to RemoteComponentPreview, RemoteText, background
@Composable
fun PaddingDemo() {
    RemoteDemo(modifier = Modifier.fillMaxSize().padding(16.dp)) { PaddingDemoContent() }
}

@Suppress("RestrictedApiAndroidX") // Referring to RemoteText, background
@RemoteComponentPreview
@Composable
@RemoteComposable
private fun PaddingDemoContent() {
    RemoteColumn(modifier = RemoteModifier.fillMaxSize()) {
        RemoteText("Padding Start: 20dp".rs)
        RemoteBox(
            modifier =
                RemoteModifier.size(100.rdp)
                    .background(Color.Red.rc)
                    .padding(start = 20.rdp)
                    .background(Color.Blue.rc)
        )

        RemoteText("Padding End: 20dp".rs)
        RemoteBox(
            modifier =
                RemoteModifier.size(100.rdp)
                    .background(Color.Red.rc)
                    .padding(end = 20.rdp)
                    .background(Color.Blue.rc)
        )

        RemoteText("Padding Horizontal: 20dp, Vertical: 10dp".rs)
        RemoteBox(
            modifier =
                RemoteModifier.size(100.rdp)
                    .background(Color.Red.rc)
                    .padding(horizontal = 20.rdp, vertical = 10.rdp)
                    .background(Color.Blue.rc)
        )
    }
}
