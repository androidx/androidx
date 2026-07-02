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

import androidx.compose.foundation.layout.padding
import androidx.compose.remote.core.operations.layout.modifiers.PaddingModifierOperation
import androidx.compose.remote.player.compose.embedded.state.rememberRemoteFloatAsState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

@Composable
internal fun Modifier.padding(op: PaddingModifierOperation): Modifier {
    // Padding values arrive in pixels (authoring stores RemoteDp via toPx()), so convert back to dp
    // by dividing by density — consistent with WidthModifier/BorderModifier/OffsetModifier. Without
    // this the padding was ~density× too large, shrinking the content so FILL children collapsed.
    val density = LocalDensity.current.density
    val left = rememberRemoteFloatAsState(op.left).value
    val top = rememberRemoteFloatAsState(op.top).value
    val right = rememberRemoteFloatAsState(op.right).value
    val bottom = rememberRemoteFloatAsState(op.bottom).value

    return this.padding(
        start = (left / density).dp,
        top = (top / density).dp,
        end = (right / density).dp,
        bottom = (bottom / density).dp,
    )
}
