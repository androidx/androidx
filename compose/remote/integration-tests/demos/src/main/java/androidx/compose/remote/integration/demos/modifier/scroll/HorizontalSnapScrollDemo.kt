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
import androidx.compose.remote.creation.compose.layout.RemoteAlignment
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.layout.RemoteRow
import androidx.compose.remote.creation.compose.layout.RemoteText
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.background
import androidx.compose.remote.creation.compose.modifier.fillMaxHeight
import androidx.compose.remote.creation.compose.modifier.fillMaxWidth
import androidx.compose.remote.creation.compose.modifier.horizontalScroll
import androidx.compose.remote.creation.compose.modifier.rememberRemoteScrollState
import androidx.compose.remote.creation.compose.modifier.width
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.integration.demos.common.RemoteDemo
import androidx.compose.remote.tooling.preview.RemoteComponentPreview
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

@Suppress("RestrictedApiAndroidX")
@Composable
fun HorizontalSnapScrollDemo() {
    RemoteDemo(modifier = Modifier.fillMaxSize()) { HorizontalSnapScrollDemoContent() }
}

@Suppress("RestrictedApiAndroidX")
@RemoteComponentPreview
@Composable
@RemoteComposable
private fun HorizontalSnapScrollDemoContent() {
    val itemsQuantity = 50
    val scrollState = rememberRemoteScrollState(evenNotches = itemsQuantity - 1)
    val colors = arrayOf(Color(0xFFE0E0E0), Color(0xFFBDBDBD), Color(0xFF9E9E9E), Color(0xFF757575))
    RemoteRow(
        modifier = RemoteModifier.horizontalScroll(scrollState).fillMaxWidth().fillMaxHeight()
    ) {
        repeat(itemsQuantity) { index ->
            val color = colors[index % colors.size].rc
            RemoteBox(
                modifier = RemoteModifier.fillMaxHeight().width(200.rdp).background(color),
                contentAlignment = RemoteAlignment.Center,
            ) {
                RemoteText("Item #$index", color = Color.White.rc)
            }
        }
    }
}
