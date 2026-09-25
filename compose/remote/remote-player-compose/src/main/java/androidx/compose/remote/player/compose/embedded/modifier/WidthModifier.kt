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
import androidx.compose.foundation.layout.requiredHeightIn
import androidx.compose.foundation.layout.requiredWidthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.operations.Utils
import androidx.compose.remote.core.operations.layout.modifiers.DimensionConstraintsModifierOperation
import androidx.compose.remote.core.operations.layout.modifiers.DimensionModifierOperation
import androidx.compose.remote.core.operations.layout.modifiers.WidthInModifierOperation
import androidx.compose.remote.core.operations.layout.modifiers.WidthModifierOperation
import androidx.compose.remote.player.compose.embedded.LocalCoreDocument
import androidx.compose.remote.player.compose.embedded.dimensionConstraintsType
import androidx.compose.remote.player.compose.embedded.dimensionInRawValues
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
            val behavior = LocalCoreDocument.current.densityBehavior
            val resolved = rememberRemoteFloatAsState(dimensionRawValue(op)).value
            // EXACT stores px unless DENSITY_BEHAVIOR_DP is set on the document; EXACT_DP always
            // stores dp.
            val widthDp =
                if (
                    op.type == DimensionModifierOperation.Type.EXACT &&
                        behavior != CoreDocument.DENSITY_BEHAVIOR_DP
                ) {
                    resolved / density
                } else {
                    resolved
                }
            this.width(widthDp.dp)
        }
        DimensionModifierOperation.Type.FILL,
        DimensionModifierOperation.Type.FILL_PARENT_MAX_WIDTH ->
            this.fillMaxWidth(op.fillFraction())
        DimensionModifierOperation.Type.WRAP -> this // Default
        else -> this
    }
}

@Composable
internal fun Modifier.widthIn(op: WidthInModifierOperation): Modifier {
    val density = LocalDensity.current.density
    val behavior = LocalCoreDocument.current.densityBehavior
    val (minDimension, maxDimension) = dimensionInRawValues(op)
    val widthMinDp =
        rememberRemoteFloatAsState(minDimension).value.constraintDimensionToDp(behavior, density)
    val widthMaxDp =
        rememberRemoteFloatAsState(maxDimension).value.constraintDimensionToDp(behavior, density)
    return this.widthIn(widthMinDp, widthMaxDp)
}

internal fun Float.constraintDimensionToDp(behavior: Int, density: Float): Dp =
    if (this == -1f) {
        Dp.Unspecified
    } else if (behavior == CoreDocument.DENSITY_BEHAVIOR_PIXELS) {
        (this / density).dp
    } else {
        this.dp
    }

/**
 * Maps a [DimensionConstraintsModifierOperation] (emitted by `widthIn`/`heightIn`) to a Compose
 * width/height-in constraint. Without this, such constraints were silently dropped (the dispatch
 * `when` only matched the
 * [WidthInModifierOperation]/[androidx.compose.remote.core.operations.layout.modifiers.HeightInModifierOperation]
 * siblings). Min/max follow the same dp convention as [widthIn]/[heightIn]; -1 means "unspecified".
 */
@Composable
internal fun Modifier.dimensionConstraints(op: DimensionConstraintsModifierOperation): Modifier {
    val density = LocalDensity.current.density
    val behavior = LocalCoreDocument.current.densityBehavior
    val (minDimension, maxDimension) = dimensionInRawValues(op)
    val minDp =
        rememberRemoteFloatAsState(minDimension).value.constraintDimensionToDp(behavior, density)
    val maxDp =
        rememberRemoteFloatAsState(maxDimension).value.constraintDimensionToDp(behavior, density)
    return when (dimensionConstraintsType(op)) {
        DimensionConstraintsModifierOperation.HORIZONTAL_CONSTRAINTS -> this.widthIn(minDp, maxDp)
        DimensionConstraintsModifierOperation.REQUIRED_HORIZONTAL_CONSTRAINTS ->
            this.requiredWidthIn(minDp, maxDp)
        DimensionConstraintsModifierOperation.VERTICAL_CONSTRAINTS -> this.heightIn(minDp, maxDp)
        DimensionConstraintsModifierOperation.REQUIRED_VERTICAL_CONSTRAINTS ->
            this.requiredHeightIn(minDp, maxDp)
        else -> this
    }
}

@Composable
internal fun DimensionModifierOperation.fillFraction(): Float {
    val source = dimensionRawValue(this)
    return if (source.isNaN() && !Utils.isVariable(source)) {
        1f
    } else {
        rememberRemoteFloatAsState(source).value
    }
}
