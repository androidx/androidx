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

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.remote.core.operations.layout.modifiers.DimensionConstraintsModifierOperation
import androidx.compose.remote.core.operations.layout.modifiers.DimensionModifierOperation
import androidx.compose.remote.core.operations.layout.modifiers.WidthInModifierOperation
import androidx.compose.remote.core.operations.layout.modifiers.WidthModifierOperation
import androidx.compose.remote.player.compose.embedded.dimensionConstraintsType
import androidx.compose.remote.player.compose.embedded.dimensionRawValue
import androidx.compose.remote.player.compose.embedded.state.rememberRemoteFloatAsState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
internal fun Modifier.width(op: WidthModifierOperation): Modifier {
    val density = LocalDensity.current.density
    return when (op.type) {
        DimensionModifierOperation.Type.EXACT,
        DimensionModifierOperation.Type.EXACT_DP -> {
            // Resolve the *raw* source value (`mValue`) rather than the core-resolved `getValue()`
            // (`mOutValue`). For a variable-, time-, or animation-backed dimension, `mValue` is the
            // NaN-encoded variable id, whereas `getValue()` has already been flattened to a plain
            // float by the core's updateVariables and carries no id — feeding that to
            // rememberRemoteFloatAsState yields a non-reactive snapshot that never tracks the
            // source.
            // Resolving the id routes through the reactive state graph (time bridge, component
            // values, animatables, or the rememberRemoteExpression derivedStateOf tree) so dynamic
            // sizes update like normal Compose. (`mValue` is package private in remote-core, which
            // we
            // leave unchanged, so read it reflectively — same approach as RcPlayerColumnLayout.)
            val resolved = rememberRemoteFloatAsState(dimensionRawValue(op)).value
            // EXACT stores px (getFloat resolves to px); EXACT_DP stores dp (the core multiplies by
            // density into mOutValue, which we deliberately bypass by reading mValue).
            val widthDp =
                if (op.type == DimensionModifierOperation.Type.EXACT) resolved / density
                else resolved
            this.width(widthDp.dp)
        }
        DimensionModifierOperation.Type.FILL -> this.fillMaxWidth()
        DimensionModifierOperation.Type.WRAP -> this // Default
        else -> this
    }
}

@Composable
internal fun Modifier.widthIn(op: WidthInModifierOperation): Modifier {
    val widthMinDp = rememberRemoteFloatAsState(op.min).value.dp
    val widthMaxDp = rememberRemoteFloatAsState(op.max).value.dp
    return this.widthIn(widthMinDp.minusOneUnspecified(), widthMaxDp.minusOneUnspecified())
}

internal fun Dp.minusOneUnspecified(): Dp =
    if (value == -1f) {
        Dp.Unspecified
    } else {
        this
    }

/**
 * Maps a [DimensionConstraintsModifierOperation] (emitted by `widthIn`/`heightIn`) to a Compose
 * width/height-in constraint. Without this, such constraints were silently dropped (the dispatch
 * `when` only matched the [WidthInModifierOperation]/[HeightInModifierOperation] siblings). Min/max
 * follow the same dp convention as [widthIn]/[heightIn]; -1 means "unspecified".
 */
@Composable
internal fun Modifier.dimensionConstraints(op: DimensionConstraintsModifierOperation): Modifier {
    val minDp = rememberRemoteFloatAsState(op.min).value.dp.minusOneUnspecified()
    val maxDp = rememberRemoteFloatAsState(op.max).value.dp.minusOneUnspecified()
    return when (dimensionConstraintsType(op)) {
        DimensionConstraintsModifierOperation.HORIZONTAL_CONSTRAINTS,
        DimensionConstraintsModifierOperation.REQUIRED_HORIZONTAL_CONSTRAINTS ->
            this.widthIn(minDp, maxDp)
        DimensionConstraintsModifierOperation.VERTICAL_CONSTRAINTS,
        DimensionConstraintsModifierOperation.REQUIRED_VERTICAL_CONSTRAINTS ->
            this.heightIn(minDp, maxDp)
        else -> this
    }
}
