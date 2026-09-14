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
import androidx.compose.remote.player.compose.embedded.LocalCoreDocument
import androidx.compose.remote.player.compose.embedded.paddingRawValues
import androidx.compose.remote.player.compose.embedded.rawDimensionDp
import androidx.compose.remote.player.compose.embedded.state.rememberRemoteFloatAsState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity

@Composable
internal fun Modifier.padding(op: PaddingModifierOperation): Modifier {
    val density = LocalDensity.current.density
    val behavior = LocalCoreDocument.current.densityBehavior
    val (leftSource, topSource, rightSource, bottomSource) = paddingRawValues(op)
    val left = rememberRemoteFloatAsState(leftSource).value
    val top = rememberRemoteFloatAsState(topSource).value
    val right = rememberRemoteFloatAsState(rightSource).value
    val bottom = rememberRemoteFloatAsState(bottomSource).value

    return this.padding(
        start = rawDimensionDp(left, behavior, density),
        top = rawDimensionDp(top, behavior, density),
        end = rawDimensionDp(right, behavior, density),
        bottom = rawDimensionDp(bottom, behavior, density),
    )
}
