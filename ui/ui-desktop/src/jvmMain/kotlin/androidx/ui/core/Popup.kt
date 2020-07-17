/*
 * Copyright 2020 The Android Open Source Project
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
package androidx.ui.desktop.core

import androidx.compose.Composable
import androidx.compose.emptyContent
import androidx.compose.onDispose
import androidx.compose.remember
import androidx.ui.core.Alignment
import androidx.ui.core.Modifier
import androidx.ui.core.ViewAmbient
import androidx.ui.desktop.view.LayoutScope
import androidx.ui.core.gesture.tapGestureFilter
import androidx.compose.foundation.Box
import androidx.ui.layout.fillMaxSize
import androidx.ui.unit.IntOffset
import androidx.ui.unit.dp

@Composable
fun Popup(
    alignment: Alignment = Alignment.TopStart,
    offset: IntOffset = IntOffset(0, 0),
    isFocusable: Boolean = false,
    onDismissRequest: (() -> Unit)? = null,
    children: @Composable () -> Unit = emptyContent()
) {
    PopupLayout(
        alignment = alignment,
        offset = offset
    ) {
        Box(
            modifier = Modifier.fillMaxSize().tapGestureFilter {
                if (isFocusable) {
                    onDismissRequest?.invoke()
                }
            },
            paddingStart = offset.x.dp,
            paddingTop = offset.y.dp,
            gravity = alignment
        ) {
            Box(
                modifier = Modifier.tapGestureFilter {}
            ) {
                children()
            }
        }
    }
}

@Composable
private fun PopupLayout(
    alignment: Alignment = Alignment.TopStart,
    offset: IntOffset = IntOffset(0, 0),
    children: @Composable () -> Unit
) {
    val view = ViewAmbient.current
    val layout = remember { LayoutScope(view, view.context) }
    layout.setLayoutParams(alignment, offset)
    layout.setContent(children)

    onDispose {
        layout.dismiss()
    }
}
