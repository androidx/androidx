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

package androidx.compose.remote.player.compose.embedded.modifier

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.remote.core.operations.layout.modifiers.ClipRectModifierOperation
import androidx.compose.remote.core.operations.layout.modifiers.RoundedClipRectModifierOperation
import androidx.compose.remote.player.compose.embedded.LocalCoreDocument
import androidx.compose.remote.player.compose.embedded.readDataReflection
import androidx.compose.remote.player.compose.embedded.state.rememberRemoteFloatAsState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

@Composable
internal fun Modifier.clipRect(op: ClipRectModifierOperation): Modifier {
    return this.clip(RectangleShape)
}

@Composable
internal fun Modifier.roundedClipRect(op: RoundedClipRectModifierOperation): Modifier {
    val density = LocalDensity.current.density
    val document = LocalCoreDocument.current
    val data = op.readDataReflection()
    val topStartPx = rememberRemoteFloatAsState(data.x1Value).value
    val topEndPx = rememberRemoteFloatAsState(data.y1Value).value
    val bottomEndPx = rememberRemoteFloatAsState(data.y2Value).value
    val bottomStartPx = rememberRemoteFloatAsState(data.x2Value).value

    return this.clip(
        RoundedCornerShape(
            topStart = (topStartPx / density).dp,
            topEnd = (topEndPx / density).dp,
            bottomEnd = (bottomEndPx / density).dp,
            bottomStart = (bottomStartPx / density).dp,
        )
    )
}
