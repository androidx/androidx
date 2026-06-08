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

package androidx.compose.remote.integration.demos.modifier.scroll

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.remote.creation.compose.action.Action
import androidx.compose.remote.creation.compose.action.valueChange
import androidx.compose.remote.creation.compose.layout.RemoteAlignment
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteColumn
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.layout.RemoteRow
import androidx.compose.remote.creation.compose.layout.RemoteText
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.background
import androidx.compose.remote.creation.compose.modifier.clickable
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.modifier.fillMaxWidth
import androidx.compose.remote.creation.compose.modifier.horizontalScroll
import androidx.compose.remote.creation.compose.modifier.padding
import androidx.compose.remote.creation.compose.modifier.rememberRemoteScrollState
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.integration.demos.common.RemoteDemo
import androidx.compose.remote.tooling.preview.RemoteComponentPreview
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

@Suppress("RestrictedApiAndroidX")
@Composable
fun ControlledScrollableRowDemo() {
    RemoteDemo(modifier = Modifier.fillMaxSize()) { ControlledScrollableRowDemoContent() }
}

@Suppress("RestrictedApiAndroidX")
@RemoteComponentPreview
@Composable
@RemoteComposable
private fun ControlledScrollableRowDemoContent() {
    val scrollState = rememberRemoteScrollState()
    RemoteColumn(modifier = RemoteModifier.fillMaxSize()) {
        RemoteRow(
            modifier = RemoteModifier.horizontalScroll(scrollState).fillMaxWidth().weight(1f.rf)
        ) {
            repeat(1000) { index -> Square(index) }
        }
        RemoteRow(verticalAlignment = RemoteAlignment.CenterVertically) {
            RemoteText("Scroll".rs, color = Color.Black.rc)
            Button(
                onClick = valueChange(scrollState.positionState, scrollState.positionState - 1000f)
            ) {
                RemoteText("< -".rs, color = Color.Black.rc)
            }
            Button(
                onClick = valueChange(scrollState.positionState, scrollState.positionState + 10000f)
            ) {
                RemoteText("--- >".rs, color = Color.Black.rc)
            }
        }
    }
}

@Suppress("RestrictedApiAndroidX")
@Composable
@RemoteComposable
private fun Square(index: Int) {
    val colors =
        arrayOf(
            Color(0xFFffd7d7),
            Color(0xFFffe9d6),
            Color(0xFFfffbd0),
            Color(0xFFe3ffd9),
            Color(0xFFd0fff8),
        )
    RemoteBox(
        modifier = RemoteModifier.size(75.rdp, 200.rdp).background(colors[index % colors.size].rc),
        contentAlignment = RemoteAlignment.Center,
    ) {
        RemoteText(index.toString().rs, color = Color.Black.rc)
    }
}

@Suppress("RestrictedApiAndroidX")
@Composable
@RemoteComposable
private fun Button(onClick: Action, content: @Composable @RemoteComposable () -> Unit) {
    RemoteBox(
        modifier =
            RemoteModifier.padding(5.rdp)
                .size(120.rdp, 60.rdp)
                .clickable(onClick)
                .background(Color.LightGray.rc),
        contentAlignment = RemoteAlignment.Center,
        content = content,
    )
}
