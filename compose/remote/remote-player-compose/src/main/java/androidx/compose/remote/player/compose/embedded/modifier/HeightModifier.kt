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

import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.remote.core.operations.layout.modifiers.DimensionModifierOperation
import androidx.compose.remote.core.operations.layout.modifiers.HeightInModifierOperation
import androidx.compose.remote.core.operations.layout.modifiers.HeightModifierOperation
import androidx.compose.remote.player.compose.embedded.dimensionRawValue
import androidx.compose.remote.player.compose.embedded.state.rememberRemoteFloatAsState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

@Composable
internal fun Modifier.height(op: HeightModifierOperation): Modifier {
    val density = LocalDensity.current.density
    return when (op.type) {
        DimensionModifierOperation.Type.EXACT,
        DimensionModifierOperation.Type.EXACT_DP -> {
            // See WidthModifier.width: resolve the raw source value (`mValue`, the variable id for
            // dynamic dimensions) reactively rather than the core-flattened `getValue()`, so
            // time-/animation-/host-driven heights update like normal Compose.
            val resolved = rememberRemoteFloatAsState(dimensionRawValue(op)).value
            val heightDp =
                if (op.type == DimensionModifierOperation.Type.EXACT) resolved / density
                else resolved
            this.height(heightDp.dp)
        }
        DimensionModifierOperation.Type.FILL -> this.fillMaxHeight()
        DimensionModifierOperation.Type.WRAP -> this // Default
        else -> this
    }
}

@Composable
internal fun Modifier.heightIn(op: HeightInModifierOperation): Modifier {
    val density = LocalDensity.current.density
    val heightMinDp = rememberRemoteFloatAsState(op.min).value.constraintPxToDp(density)
    val heightMaxDp = rememberRemoteFloatAsState(op.max).value.constraintPxToDp(density)
    return this.heightIn(heightMinDp, heightMaxDp)
}
