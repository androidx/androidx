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

package androidx.wear.compose.remote.material3.previews

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.remote.creation.compose.capture.LocalRemoteComposeCreationState
import androidx.compose.remote.creation.compose.capture.RemoteDensity
import androidx.compose.remote.creation.compose.layout.RemoteAlignment
import androidx.compose.remote.creation.compose.layout.RemoteArrangement
import androidx.compose.remote.creation.compose.layout.RemoteColumn
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.creation.compose.state.rsp
import androidx.compose.remote.tooling.preview.RemotePreview
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.Text
import androidx.wear.compose.remote.material3.RemoteText
import androidx.wear.compose.ui.tooling.preview.WearPreviewFontScales

private const val size1 = 12
private const val size2 = 16
private const val size3 = 30
private const val size4 = 50
private const val text = "HH"

@WearPreviewFontScales
@Composable
fun RemoteTextFontScaleComparisonPreview() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().wrapContentHeight(),
            horizontalArrangement = Arrangement.SpaceAround,
        ) {
            Text(text = "Compose")
            Text(text = "RC")
        }
        Row(modifier = Modifier.fillMaxWidth().wrapContentHeight()) {
            ComposeText()
            Spacer(modifier = Modifier.width(4.dp).fillMaxHeight().background(Color.Red))
            RCText()
        }
    }
}

@Composable
private fun RowScope.RCText() {
    Box(modifier = Modifier.weight(1f)) {
        RemotePreview {
            Container {
                val state = LocalRemoteComposeCreationState.current
                val density = LocalDensity.current
                state.remoteDensity =
                    RemoteDensity(density = density.density.rf, fontScale = density.fontScale.rf)

                RemoteText(text = text.rs, fontSize = size1.rsp)
                RemoteText(text = text.rs, fontSize = size2.rsp)
                RemoteText(text = text.rs, fontSize = size3.rsp)
                RemoteText(text = text.rs, fontSize = size4.rsp)
            }
        }
    }
}

@Composable
private fun RowScope.ComposeText() {
    Column(
        modifier = Modifier.weight(1f).align(Alignment.CenterVertically),
        horizontalAlignment = Alignment.End,
    ) {
        Text(text = text, fontSize = size1.sp)
        Text(text = text, fontSize = size2.sp)
        Text(text = text, fontSize = size3.sp)
        Text(text = text, fontSize = size4.sp)
    }
}

@Composable
@RemoteComposable
private fun Container(
    modifier: RemoteModifier = RemoteModifier.fillMaxSize(),
    content: @Composable @RemoteComposable () -> Unit,
) {
    RemoteColumn(
        modifier,
        horizontalAlignment = RemoteAlignment.Start,
        verticalArrangement = RemoteArrangement.Center,
    ) {
        content()
    }
}
