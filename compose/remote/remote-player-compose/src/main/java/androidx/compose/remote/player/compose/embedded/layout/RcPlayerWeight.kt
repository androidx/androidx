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

package androidx.compose.remote.player.compose.embedded.layout

import androidx.compose.remote.core.operations.layout.Component
import androidx.compose.remote.core.operations.layout.LayoutComponent
import androidx.compose.remote.core.operations.layout.modifiers.DimensionModifierOperation
import androidx.compose.remote.core.operations.layout.modifiers.HeightModifierOperation
import androidx.compose.remote.core.operations.layout.modifiers.WidthModifierOperation
import androidx.compose.remote.player.compose.embedded.state.rememberRemoteFloatAsState
import androidx.compose.runtime.Composable
import androidx.compose.ui.util.fastFirstOrNull

/**
 * The weight [child] asks for along a Row's ([vertical] false) or Column's ([vertical] true) main
 * axis, or 0 when it has none.
 *
 * The weight may be a NaN-encoded float variable (`RemoteRow`'s `weight(RemoteFloat)`), which core
 * leaves unresolved in `DimensionModifierOperation.getValue` for `WEIGHT`, so the raw value is
 * resolved reactively here. Compose only accepts a finite weight above 0, so a weight that resolves
 * to NaN, 0 or below (for example before the value it depends on is known) leaves the child
 * unweighted instead of throwing. Like a variable width or height, a weight derived from the
 * component's own published size follows it a frame late.
 */
@Composable
internal fun rememberChildWeight(child: Component, vertical: Boolean): Float {
    val op = weightModifier(child, vertical) ?: return 0f
    val weight = rememberRemoteFloatAsState(op.rawValue).value
    return if (weight.isFinite() && weight > 0f) weight else 0f
}

private fun weightModifier(child: Component, vertical: Boolean): DimensionModifierOperation? {
    val layoutChild = child as? LayoutComponent ?: return null
    if (vertical) {
        layoutChild.heightModifier?.let { if (it.hasWeight()) return it }
    }
    return layoutChild.componentModifiers?.getList()?.fastFirstOrNull {
        it is DimensionModifierOperation &&
            it.hasWeight() &&
            (if (vertical) it is HeightModifierOperation else it is WidthModifierOperation)
    } as? DimensionModifierOperation
}
