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

package androidx.compose.remote.player.compose.embedded

import androidx.compose.remote.core.operations.layout.ClickModifierOperation
import androidx.compose.remote.core.operations.layout.Component
import androidx.compose.remote.core.operations.layout.modifiers.AlignByModifierOperation
import androidx.compose.remote.core.operations.layout.modifiers.BackgroundModifierOperation
import androidx.compose.remote.core.operations.layout.modifiers.BorderModifierOperation
import androidx.compose.remote.core.operations.layout.modifiers.ClipRectModifierOperation
import androidx.compose.remote.core.operations.layout.modifiers.CollapsiblePriorityModifierOperation
import androidx.compose.remote.core.operations.layout.modifiers.ComponentModifiers
import androidx.compose.remote.core.operations.layout.modifiers.ComponentVisibilityOperation
import androidx.compose.remote.core.operations.layout.modifiers.DimensionConstraintsModifierOperation
import androidx.compose.remote.core.operations.layout.modifiers.DrawContentOperation
import androidx.compose.remote.core.operations.layout.modifiers.GraphicsLayerModifierOperation
import androidx.compose.remote.core.operations.layout.modifiers.HeightInModifierOperation
import androidx.compose.remote.core.operations.layout.modifiers.HeightModifierOperation
import androidx.compose.remote.core.operations.layout.modifiers.LayoutComputeOperation
import androidx.compose.remote.core.operations.layout.modifiers.MarqueeModifierOperation
import androidx.compose.remote.core.operations.layout.modifiers.OffsetModifierOperation
import androidx.compose.remote.core.operations.layout.modifiers.PaddingModifierOperation
import androidx.compose.remote.core.operations.layout.modifiers.RippleModifierOperation
import androidx.compose.remote.core.operations.layout.modifiers.RoundedClipRectModifierOperation
import androidx.compose.remote.core.operations.layout.modifiers.ScrollModifierOperation
import androidx.compose.remote.core.operations.layout.modifiers.WidthInModifierOperation
import androidx.compose.remote.core.operations.layout.modifiers.WidthModifierOperation
import androidx.compose.remote.core.operations.layout.modifiers.ZIndexModifierOperation
import androidx.compose.remote.core.semantics.AccessibleComponent
import androidx.compose.remote.core.semantics.CoreSemantics
import androidx.compose.remote.player.compose.embedded.modifier.background
import androidx.compose.remote.player.compose.embedded.modifier.border
import androidx.compose.remote.player.compose.embedded.modifier.click
import androidx.compose.remote.player.compose.embedded.modifier.clipRect
import androidx.compose.remote.player.compose.embedded.modifier.dimensionConstraints
import androidx.compose.remote.player.compose.embedded.modifier.graphicsLayer
import androidx.compose.remote.player.compose.embedded.modifier.height
import androidx.compose.remote.player.compose.embedded.modifier.heightIn
import androidx.compose.remote.player.compose.embedded.modifier.marquee
import androidx.compose.remote.player.compose.embedded.modifier.offset
import androidx.compose.remote.player.compose.embedded.modifier.padding
import androidx.compose.remote.player.compose.embedded.modifier.ripple
import androidx.compose.remote.player.compose.embedded.modifier.roundedClipRect
import androidx.compose.remote.player.compose.embedded.modifier.scroll
import androidx.compose.remote.player.compose.embedded.modifier.width
import androidx.compose.remote.player.compose.embedded.modifier.widthIn
import androidx.compose.remote.player.compose.embedded.modifier.zIndex
import androidx.compose.remote.player.compose.embedded.state.rememberRemoteIntAsState
import androidx.compose.remote.player.compose.embedded.state.rememberRemoteStringAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.util.fastForEach

@Composable
@Suppress("ModifierFactoryExtensionFunction")
internal fun ComponentModifiers.toModifier(): Modifier {
    var modifier: Modifier = Modifier
    list.fastForEach { op ->
        modifier =
            when (op) {
                is PaddingModifierOperation -> modifier.padding(op)
                is WidthModifierOperation -> modifier.width(op)
                is HeightModifierOperation -> modifier.height(op)
                is BorderModifierOperation -> modifier.border(op)
                is BackgroundModifierOperation -> modifier.background(op)
                is OffsetModifierOperation -> modifier.offset(op)
                is ClipRectModifierOperation -> modifier.clipRect(op)
                is RoundedClipRectModifierOperation -> modifier.roundedClipRect(op)
                is ZIndexModifierOperation -> modifier.zIndex(op)
                is GraphicsLayerModifierOperation -> modifier.graphicsLayer(op)
                is RippleModifierOperation -> modifier.ripple(op)
                is ScrollModifierOperation -> modifier.scroll(op)
                is WidthInModifierOperation -> modifier.widthIn(op)
                is HeightInModifierOperation -> modifier.heightIn(op)
                is DimensionConstraintsModifierOperation -> modifier.dimensionConstraints(op)
                is ClickModifierOperation -> modifier.click(op)
                is ComponentVisibilityOperation -> modifier.visible(op)
                is MarqueeModifierOperation -> modifier.marquee(op)
                is CoreSemantics -> modifier.semantics(op)
                is DrawContentOperation -> modifier
                // AlignBy is applied per-child inside Row/Column scope (RcPlayerRow), where the
                // alignment lines are available; it is a no-op in the generic modifier chain.
                is AlignByModifierOperation -> modifier
                // CollapsiblePriority is a hint for collapsible layouts (which currently render as
                // plain Row/Column), and LayoutCompute is custom measure/position logic the
                // embedded
                // player doesn't support — consume both rather than warn.
                is CollapsiblePriorityModifierOperation -> modifier
                is LayoutComputeOperation -> modifier
                else -> {
                    println("Warning: Unsupported modifier $op")
                    modifier
                }
            }
    }
    return modifier
}

@Composable
private fun Modifier.visible(op: ComponentVisibilityOperation): Modifier {
    val visible by rememberRemoteIntAsState(op.getVisibilityIdReflection())

    return this.layout { measurable, constraints ->
            val placeable = measurable.measure(constraints)
            layout(placeable.width, placeable.height) {
                if (visible != Component.Visibility.GONE) {
                    placeable.place(0, 0)
                }
            }
        }
        .graphicsLayer { alpha = if (visible == Component.Visibility.VISIBLE) 1f else 0f }
}

@Composable
private fun Modifier.semantics(op: CoreSemantics): Modifier {
    val contentDescriptionId = op.getContentDescriptionIdReflection()
    val contentDescription = contentDescriptionId?.let { rememberRemoteStringAsState(it).value }
    val text = op.mTextId.takeIf { it != 0 }?.let { rememberRemoteStringAsState(it).value }

    if (contentDescription == null && text == null && op.mRole == null) return this

    val properties: SemanticsPropertyReceiver.() -> Unit = {
        if (contentDescription != null) {
            this.contentDescription = contentDescription
        }

        if (text != null) {
            this.text = AnnotatedString(text)
        }

        op.mRole?.let { this.role = it.toComposeRole() }
    }
    return when (op.mMode) {
        AccessibleComponent.Mode.SET -> this.semantics(properties = properties)
        AccessibleComponent.Mode.CLEAR_AND_SET -> this.clearAndSetSemantics(properties = properties)
        AccessibleComponent.Mode.MERGE ->
            this.semantics(mergeDescendants = true, properties = properties)
    }
}

private fun AccessibleComponent.Role.toComposeRole(): androidx.compose.ui.semantics.Role {
    return when (this) {
        AccessibleComponent.Role.BUTTON -> androidx.compose.ui.semantics.Role.Button
        AccessibleComponent.Role.CHECKBOX -> androidx.compose.ui.semantics.Role.Checkbox
        AccessibleComponent.Role.SWITCH -> androidx.compose.ui.semantics.Role.Switch
        AccessibleComponent.Role.RADIO_BUTTON -> androidx.compose.ui.semantics.Role.RadioButton
        AccessibleComponent.Role.TAB -> androidx.compose.ui.semantics.Role.Tab
        AccessibleComponent.Role.IMAGE -> androidx.compose.ui.semantics.Role.Image
        AccessibleComponent.Role.DROPDOWN_LIST -> androidx.compose.ui.semantics.Role.DropdownList
        // No direct Compose Role equivalents; Button is the closest interactive fallback.
        AccessibleComponent.Role.PICKER -> androidx.compose.ui.semantics.Role.Button
        AccessibleComponent.Role.CAROUSEL -> androidx.compose.ui.semantics.Role.Button
        else -> androidx.compose.ui.semantics.Role.Button
    }
}
