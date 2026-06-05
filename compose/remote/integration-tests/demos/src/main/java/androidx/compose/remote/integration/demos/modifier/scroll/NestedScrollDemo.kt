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
import androidx.compose.foundation.layout.padding
import androidx.compose.remote.creation.compose.layout.RemoteAlignment
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteColumn
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.layout.RemoteRow
import androidx.compose.remote.creation.compose.layout.RemoteText
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.border
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.modifier.fillMaxWidth
import androidx.compose.remote.creation.compose.modifier.horizontalScroll
import androidx.compose.remote.creation.compose.modifier.padding
import androidx.compose.remote.creation.compose.modifier.rememberRemoteScrollState
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.modifier.verticalScroll
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.integration.demos.common.RemoteDemo
import androidx.compose.remote.tooling.preview.RemoteComponentPreview
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Suppress("RestrictedApiAndroidX")
@Composable
fun HorizontalScrollersInVerticalScrollerDemo() {
    RemoteDemo(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        HorizontalScrollersInVerticalScrollerDemoContent()
    }
}

@Suppress("RestrictedApiAndroidX")
@RemoteComponentPreview
@Composable
@RemoteComposable
private fun HorizontalScrollersInVerticalScrollerDemoContent() {
    val verticalScrollState = rememberRemoteScrollState()
    RemoteColumn(modifier = RemoteModifier.fillMaxSize()) {
        RemoteColumn(modifier = RemoteModifier.fillMaxSize().verticalScroll(verticalScrollState)) {
            repeat(10) { rowIndex ->
                val horizontalScrollState = rememberRemoteScrollState()
                RemoteBox(modifier = RemoteModifier.padding(vertical = 8.rdp)) {
                    RemoteRow(
                        modifier =
                            RemoteModifier.fillMaxWidth().horizontalScroll(horizontalScrollState)
                    ) {
                        repeat(10) { colIndex ->
                            RemoteBox(
                                modifier =
                                    RemoteModifier.size(96.rdp).border(1.rdp, Color.Black.rc),
                                contentAlignment = RemoteAlignment.Center,
                            ) {
                                RemoteText("$rowIndex,$colIndex")
                            }
                        }
                    }
                }
            }
        }
    }
}
