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

@file:Suppress("RestrictedApiAndroidX", "PrimitiveInCollection")

package androidx.compose.remote.player.compose.embedded.inspectable

import android.graphics.Color as AndroidColor
import androidx.compose.remote.core.Operation
import androidx.compose.remote.core.RemoteContext
import androidx.compose.remote.core.operations.layout.ClickModifierOperation
import androidx.compose.remote.core.operations.layout.Component
import androidx.compose.remote.core.operations.layout.MultiClickModifier
import androidx.compose.remote.core.operations.layout.animation.AnimationSpec
import androidx.compose.remote.core.operations.layout.modifiers.BackgroundModifierOperation
import androidx.compose.remote.core.operations.layout.modifiers.BorderModifierOperation
import androidx.compose.remote.core.operations.layout.modifiers.ComponentVisibilityOperation
import androidx.compose.remote.core.operations.layout.modifiers.DimensionConstraintsModifierOperation
import androidx.compose.remote.core.operations.layout.modifiers.DimensionModifierOperation
import androidx.compose.remote.core.operations.layout.modifiers.DrawContentOperation
import androidx.compose.remote.core.operations.layout.modifiers.GraphicsLayerModifierOperation
import androidx.compose.remote.core.operations.layout.modifiers.HeightInModifierOperation
import androidx.compose.remote.core.operations.layout.modifiers.HeightModifierOperation
import androidx.compose.remote.core.operations.layout.modifiers.MarqueeModifierOperation
import androidx.compose.remote.core.operations.layout.modifiers.OffsetModifierOperation
import androidx.compose.remote.core.operations.layout.modifiers.PaddingModifierOperation
import androidx.compose.remote.core.operations.layout.modifiers.ScrollModifierOperation
import androidx.compose.remote.core.operations.layout.modifiers.WidthInModifierOperation
import androidx.compose.remote.core.operations.layout.modifiers.WidthModifierOperation
import androidx.compose.remote.core.operations.layout.modifiers.ZIndexModifierOperation
import androidx.compose.remote.core.semantics.AccessibleComponent
import androidx.compose.remote.core.semantics.CoreSemantics
import androidx.compose.remote.player.compose.embedded.state.MaybeVariableFloat
import androidx.compose.ui.Modifier
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.InspectableValue
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.util.fastMap

/**
 * Base [ModifierNodeElement] and [InspectableValue] representing a RemoteCompose modifier
 * [Operation] on a component's `LayoutNode`.
 *
 * Each subclass is a lightweight, disposable wrapper holding only [operation] and [remoteContext]
 * references; all core reads and variable resolution are performed lazily when property getters or
 * [inspectableProperties] are invoked.
 */
internal sealed class RcModifierInspectable : ModifierNodeElement<RcModifierInspectorNode>() {
    abstract val operation: Operation
    internal abstract val remoteContext: RemoteContext

    protected abstract val inspectorName: String

    val kind: String
        get() = operation::class.java.simpleName

    override fun create(): RcModifierInspectorNode = RcModifierInspectorNode(operation)

    override fun update(node: RcModifierInspectorNode) {
        node.operation = operation
    }

    final override fun InspectorInfo.inspectableProperties() {
        name = inspectorName
        value = operation
        properties["operation"] = operation
        properties["kind"] = kind
        modifierProperties()
    }

    protected open fun InspectorInfo.modifierProperties() {}

    companion object {
        @Suppress("ModifierFactoryExtensionFunction", "ModifierFactoryReturnType")
        fun of(operation: Operation, remoteContext: RemoteContext): RcModifierInspectable =
            when (operation) {
                is PaddingModifierOperation ->
                    RcPaddingModifierInspectable(operation, remoteContext)
                is WidthModifierOperation -> RcWidthModifierInspectable(operation, remoteContext)
                is HeightModifierOperation -> RcHeightModifierInspectable(operation, remoteContext)
                is WidthInModifierOperation ->
                    RcWidthInModifierInspectable(operation, remoteContext)
                is HeightInModifierOperation ->
                    RcHeightInModifierInspectable(operation, remoteContext)
                is DimensionConstraintsModifierOperation ->
                    RcDimensionConstraintsModifierInspectable(operation, remoteContext)
                is BackgroundModifierOperation ->
                    RcBackgroundModifierInspectable(operation, remoteContext)
                is BorderModifierOperation -> RcBorderModifierInspectable(operation, remoteContext)
                is OffsetModifierOperation -> RcOffsetModifierInspectable(operation, remoteContext)
                is ZIndexModifierOperation -> RcZIndexModifierInspectable(operation, remoteContext)
                is GraphicsLayerModifierOperation ->
                    RcGraphicsLayerModifierInspectable(operation, remoteContext)
                is ScrollModifierOperation -> RcScrollModifierInspectable(operation, remoteContext)
                is MarqueeModifierOperation ->
                    RcMarqueeModifierInspectable(operation, remoteContext)
                is ComponentVisibilityOperation ->
                    RcVisibilityModifierInspectable(operation, remoteContext)
                is CoreSemantics -> RcSemanticsModifierInspectable(operation, remoteContext)
                is ClickModifierOperation -> RcClickModifierInspectable(operation, remoteContext)
                is MultiClickModifier -> RcMultiClickModifierInspectable(operation, remoteContext)
                is DrawContentOperation ->
                    RcDrawContentModifierInspectable(operation, remoteContext)
                is AnimationSpec -> RcAnimationSpecModifierInspectable(operation, remoteContext)
                else -> RcOtherModifierInspectable(operation, remoteContext)
            }
    }
}

internal data class RcPaddingModifierInspectable(
    override val operation: PaddingModifierOperation,
    override val remoteContext: RemoteContext,
) : RcModifierInspectable() {
    override val inspectorName: String
        get() = "rcPadding"

    val left: Float
        get() = RcPlayerInspector.resolveFloatOrRaw(operation.rawLeft, remoteContext)

    val top: Float
        get() = RcPlayerInspector.resolveFloatOrRaw(operation.rawTop, remoteContext)

    val right: Float
        get() = RcPlayerInspector.resolveFloatOrRaw(operation.rawRight, remoteContext)

    val bottom: Float
        get() = RcPlayerInspector.resolveFloatOrRaw(operation.rawBottom, remoteContext)

    override fun InspectorInfo.modifierProperties() {
        properties["left"] = RcPlayerInspector.resolveFloatOrRaw(operation.rawLeft, remoteContext)
        properties["top"] = RcPlayerInspector.resolveFloatOrRaw(operation.rawTop, remoteContext)
        properties["right"] = RcPlayerInspector.resolveFloatOrRaw(operation.rawRight, remoteContext)
        properties["bottom"] =
            RcPlayerInspector.resolveFloatOrRaw(operation.rawBottom, remoteContext)
    }
}

internal data class RcWidthModifierInspectable(
    override val operation: WidthModifierOperation,
    override val remoteContext: RemoteContext,
) : RcModifierInspectable() {
    override val inspectorName: String
        get() = "rcWidth"

    val dimensionType: DimensionModifierOperation.Type
        get() = operation.type

    val rawValue: Float
        get() = operation.rawValue

    val value: Float
        get() = RcPlayerInspector.resolveFloatOrRaw(operation.rawValue, remoteContext)

    val variableId: Int?
        get() = MaybeVariableFloat(operation.rawValue).takeIf { it.isReference }?.id

    override fun InspectorInfo.modifierProperties() {
        val raw = operation.rawValue
        properties["dimensionType"] = operation.type
        properties["value"] = RcPlayerInspector.resolveFloatOrRaw(raw, remoteContext)
        variableId?.let { properties["variableId"] = it }
    }
}

internal data class RcHeightModifierInspectable(
    override val operation: HeightModifierOperation,
    override val remoteContext: RemoteContext,
) : RcModifierInspectable() {
    override val inspectorName: String
        get() = "rcHeight"

    val dimensionType: DimensionModifierOperation.Type
        get() = operation.type

    val rawValue: Float
        get() = operation.rawValue

    val value: Float
        get() = RcPlayerInspector.resolveFloatOrRaw(operation.rawValue, remoteContext)

    val variableId: Int?
        get() = MaybeVariableFloat(operation.rawValue).takeIf { it.isReference }?.id

    override fun InspectorInfo.modifierProperties() {
        val raw = operation.rawValue
        properties["dimensionType"] = operation.type
        properties["value"] = RcPlayerInspector.resolveFloatOrRaw(raw, remoteContext)
        variableId?.let { properties["variableId"] = it }
    }
}

internal data class RcWidthInModifierInspectable(
    override val operation: WidthInModifierOperation,
    override val remoteContext: RemoteContext,
) : RcModifierInspectable() {
    override val inspectorName: String
        get() = "rcWidthIn"

    val min: Float
        get() = RcPlayerInspector.resolveFloatOrRaw(operation.rawMin, remoteContext)

    val max: Float
        get() = RcPlayerInspector.resolveFloatOrRaw(operation.rawMax, remoteContext)

    override fun InspectorInfo.modifierProperties() {
        properties["min"] = RcPlayerInspector.resolveFloatOrRaw(operation.rawMin, remoteContext)
        properties["max"] = RcPlayerInspector.resolveFloatOrRaw(operation.rawMax, remoteContext)
    }
}

internal data class RcHeightInModifierInspectable(
    override val operation: HeightInModifierOperation,
    override val remoteContext: RemoteContext,
) : RcModifierInspectable() {
    override val inspectorName: String
        get() = "rcHeightIn"

    val min: Float
        get() = RcPlayerInspector.resolveFloatOrRaw(operation.rawMin, remoteContext)

    val max: Float
        get() = RcPlayerInspector.resolveFloatOrRaw(operation.rawMax, remoteContext)

    override fun InspectorInfo.modifierProperties() {
        properties["min"] = RcPlayerInspector.resolveFloatOrRaw(operation.rawMin, remoteContext)
        properties["max"] = RcPlayerInspector.resolveFloatOrRaw(operation.rawMax, remoteContext)
    }
}

internal data class RcDimensionConstraintsModifierInspectable(
    override val operation: DimensionConstraintsModifierOperation,
    override val remoteContext: RemoteContext,
) : RcModifierInspectable() {
    override val inspectorName: String
        get() = "rcDimensionConstraints"

    val constraintsType: Int
        get() = operation.type

    val min: Float
        get() = RcPlayerInspector.resolveFloatOrRaw(operation.rawMin, remoteContext)

    val max: Float
        get() = RcPlayerInspector.resolveFloatOrRaw(operation.rawMax, remoteContext)

    override fun InspectorInfo.modifierProperties() {
        properties["constraintsType"] = operation.type
        properties["min"] = RcPlayerInspector.resolveFloatOrRaw(operation.rawMin, remoteContext)
        properties["max"] = RcPlayerInspector.resolveFloatOrRaw(operation.rawMax, remoteContext)
    }
}

internal data class RcBackgroundModifierInspectable(
    override val operation: BackgroundModifierOperation,
    override val remoteContext: RemoteContext,
) : RcModifierInspectable() {
    override val inspectorName: String
        get() = "rcBackground"

    val useColorId: Boolean
        get() = operation.useColorId

    val colorId: Int
        get() = operation.colorId

    val shapeType: Int
        get() = operation.shapeType

    val color: Int
        get() = resolveColor()

    private fun resolveColor(): Int {
        return if (operation.useColorId) {
            remoteContext.getColor(operation.colorId)
        } else {
            val a =
                (RcPlayerInspector.resolveFloatOrRaw(operation.aId, remoteContext) * 255f)
                    .toInt()
                    .coerceIn(0, 255)
            val r =
                (RcPlayerInspector.resolveFloatOrRaw(operation.rId, remoteContext) * 255f)
                    .toInt()
                    .coerceIn(0, 255)
            val g =
                (RcPlayerInspector.resolveFloatOrRaw(operation.gId, remoteContext) * 255f)
                    .toInt()
                    .coerceIn(0, 255)
            val b =
                (RcPlayerInspector.resolveFloatOrRaw(operation.bId, remoteContext) * 255f)
                    .toInt()
                    .coerceIn(0, 255)
            AndroidColor.argb(a, r, g, b)
        }
    }

    override fun InspectorInfo.modifierProperties() {
        properties["color"] = resolveColor()
        properties["colorId"] = operation.colorId
        properties["useColorId"] = operation.useColorId
        properties["shapeType"] = operation.shapeType
    }
}

internal data class RcBorderModifierInspectable(
    override val operation: BorderModifierOperation,
    override val remoteContext: RemoteContext,
) : RcModifierInspectable() {
    override val inspectorName: String
        get() = "rcBorder"

    val borderWidth: Float
        get() =
            RcPlayerInspector.resolveFloatOrRaw(
                operation.borderWidth,
                remoteContext,
            )

    val roundedCorner: Float
        get() =
            RcPlayerInspector.resolveFloatOrRaw(
                operation.roundedCorner,
                remoteContext,
            )

    val shapeType: Int
        get() = operation.shapeType

    val colorId: Int
        get() = operation.colorId

    val useColorId: Boolean
        get() = operation.useColorId

    override fun InspectorInfo.modifierProperties() {
        properties["borderWidth"] =
            RcPlayerInspector.resolveFloatOrRaw(operation.borderWidth, remoteContext)
        properties["roundedCorner"] =
            RcPlayerInspector.resolveFloatOrRaw(operation.roundedCorner, remoteContext)
        properties["shapeType"] = operation.shapeType
        properties["colorId"] = operation.colorId
        properties["useColorId"] = operation.useColorId
    }
}

internal data class RcOffsetModifierInspectable(
    override val operation: OffsetModifierOperation,
    override val remoteContext: RemoteContext,
) : RcModifierInspectable() {
    override val inspectorName: String
        get() = "rcOffset"

    val x: Float
        get() = RcPlayerInspector.resolveFloatOrRaw(operation.rawX, remoteContext)

    val y: Float
        get() = RcPlayerInspector.resolveFloatOrRaw(operation.rawY, remoteContext)

    override fun InspectorInfo.modifierProperties() {
        properties["x"] = RcPlayerInspector.resolveFloatOrRaw(operation.rawX, remoteContext)
        properties["y"] = RcPlayerInspector.resolveFloatOrRaw(operation.rawY, remoteContext)
    }
}

internal data class RcZIndexModifierInspectable(
    override val operation: ZIndexModifierOperation,
    override val remoteContext: RemoteContext,
) : RcModifierInspectable() {
    override val inspectorName: String
        get() = "rcZIndex"

    val zIndex: Float
        get() = RcPlayerInspector.resolveFloatOrRaw(operation.value, remoteContext)

    override fun InspectorInfo.modifierProperties() {
        properties["zIndex"] = zIndex
    }
}

internal data class RcGraphicsLayerModifierInspectable(
    override val operation: GraphicsLayerModifierOperation,
    override val remoteContext: RemoteContext,
) : RcModifierInspectable() {
    override val inspectorName: String
        get() = "rcGraphicsLayer"

    val attributes: Map<String, Float>
        get() =
            LinkedHashMap<String, Float>().also { map ->
                for (attr in operation.values) {
                    map[attr.name] = RcPlayerInspector.resolveFloatOrRaw(attr.source, remoteContext)
                }
            }

    override fun InspectorInfo.modifierProperties() {
        val attrs = attributes
        properties["attributes"] = attrs
        for ((k, v) in attrs) {
            properties[k] = v
        }
    }
}

internal data class RcScrollModifierInspectable(
    override val operation: ScrollModifierOperation,
    override val remoteContext: RemoteContext,
) : RcModifierInspectable() {
    override val inspectorName: String
        get() = "rcScroll"

    val position: Float
        get() = RcPlayerInspector.resolveFloatOrRaw(operation.positionExpression, remoteContext)

    override fun InspectorInfo.modifierProperties() {
        properties["position"] = position
    }
}

internal data class RcMarqueeModifierInspectable(
    override val operation: MarqueeModifierOperation,
    override val remoteContext: RemoteContext,
) : RcModifierInspectable() {
    override val inspectorName: String
        get() = "rcMarquee"

    val iterations: Int
        get() = operation.iterations

    val animationMode: Int
        get() = operation.animationMode

    val repeatDelayMillis: Float
        get() = operation.repeatDelayMillis

    val initialDelayMillis: Float
        get() = operation.initialDelayMillis

    val spacing: Float
        get() = operation.spacing

    val velocity: Float
        get() = operation.velocity

    override fun InspectorInfo.modifierProperties() {
        properties["iterations"] = operation.iterations
        properties["animationMode"] = operation.animationMode
        properties["repeatDelayMillis"] = operation.repeatDelayMillis
        properties["initialDelayMillis"] = operation.initialDelayMillis
        properties["spacing"] = operation.spacing
        properties["velocity"] = operation.velocity
    }
}

internal data class RcVisibilityModifierInspectable(
    override val operation: ComponentVisibilityOperation,
    override val remoteContext: RemoteContext,
) : RcModifierInspectable() {
    override val inspectorName: String
        get() = "rcVisibility"

    val visibilityId: Int
        get() = operation.getVisibilityId()

    val visibilityCode: Int
        get() = remoteContext.getInteger(operation.getVisibilityId())

    val visibility: String
        get() =
            when (visibilityCode) {
                Component.Visibility.GONE -> "GONE"
                Component.Visibility.INVISIBLE -> "INVISIBLE"
                else -> "VISIBLE"
            }

    override fun InspectorInfo.modifierProperties() {
        val id = operation.getVisibilityId()
        val code = remoteContext.getInteger(id)
        properties["visibilityId"] = id
        properties["visibilityCode"] = code
        properties["visibility"] =
            when (code) {
                Component.Visibility.GONE -> "GONE"
                Component.Visibility.INVISIBLE -> "INVISIBLE"
                else -> "VISIBLE"
            }
    }
}

internal data class RcSemanticsModifierInspectable(
    override val operation: CoreSemantics,
    override val remoteContext: RemoteContext,
) : RcModifierInspectable() {
    override val inspectorName: String
        get() = "rcSemantics"

    val contentDescriptionId: Int
        get() = operation.mContentDescriptionId

    val contentDescription: String?
        get() =
            if (operation.mContentDescriptionId != 0) {
                remoteContext.getText(operation.mContentDescriptionId)
            } else {
                null
            }

    val textId: Int
        get() = operation.mTextId

    val text: String?
        get() = if (operation.mTextId != 0) remoteContext.getText(operation.mTextId) else null

    val stateDescriptionId: Int
        get() = operation.mStateDescriptionId

    val stateDescription: String?
        get() =
            if (operation.mStateDescriptionId != 0) {
                remoteContext.getText(operation.mStateDescriptionId)
            } else {
                null
            }

    val role: AccessibleComponent.Role?
        get() = operation.mRole

    val mode: AccessibleComponent.Mode
        get() = operation.mMode

    val enabled: Boolean
        get() = operation.mEnabled

    val clickable: Boolean
        get() = operation.mClickable

    override fun InspectorInfo.modifierProperties() {
        properties["contentDescriptionId"] = operation.mContentDescriptionId
        properties["contentDescription"] = contentDescription
        properties["textId"] = operation.mTextId
        properties["text"] = text
        properties["stateDescriptionId"] = operation.mStateDescriptionId
        properties["stateDescription"] = stateDescription
        properties["role"] = operation.mRole
        properties["mode"] = operation.mMode
        properties["enabled"] = operation.mEnabled
        properties["clickable"] = operation.mClickable
    }
}

internal data class RcClickModifierInspectable(
    override val operation: ClickModifierOperation,
    override val remoteContext: RemoteContext,
) : RcModifierInspectable() {
    override val inspectorName: String
        get() = "rcClick"

    val actions: List<RcOperationInspectable>
        get() = operation.list.fastMap { RcOperationInspectable.of(it, remoteContext) }

    override fun InspectorInfo.modifierProperties() {
        properties["actions"] = actions
    }
}

internal data class RcMultiClickModifierInspectable(
    override val operation: MultiClickModifier,
    override val remoteContext: RemoteContext,
) : RcModifierInspectable() {
    override val inspectorName: String
        get() = "rcMultiClick"

    val clickType: Int
        get() = operation.clickType

    val actions: List<RcOperationInspectable>
        get() = operation.list.fastMap { RcOperationInspectable.of(it, remoteContext) }

    override fun InspectorInfo.modifierProperties() {
        properties["clickType"] = operation.clickType
        properties["actions"] = actions
    }
}

internal data class RcDrawContentModifierInspectable(
    override val operation: DrawContentOperation,
    override val remoteContext: RemoteContext,
) : RcModifierInspectable() {
    override val inspectorName: String
        get() = "rcDrawContent"
}

internal data class RcAnimationSpecModifierInspectable(
    override val operation: AnimationSpec,
    override val remoteContext: RemoteContext,
) : RcModifierInspectable() {
    override val inspectorName: String
        get() = "rcAnimationSpec"

    val animationId: Int
        get() = operation.animationId

    val animationEnabled: Boolean
        get() = operation.isAnimationEnabled

    val motionDuration: Float
        get() = operation.motionDuration

    val motionEasingType: Int
        get() = operation.motionEasingType

    val visibilityDuration: Float
        get() = operation.visibilityDuration

    val visibilityEasingType: Int
        get() = operation.visibilityEasingType

    val enterAnimation: Int
        get() = operation.enterAnimation.ordinal

    val exitAnimation: Int
        get() = operation.exitAnimation.ordinal

    override fun InspectorInfo.modifierProperties() {
        properties["animationId"] = operation.animationId
        properties["animationEnabled"] = operation.isAnimationEnabled
        properties["motionDuration"] = operation.motionDuration
        properties["motionEasingType"] = operation.motionEasingType
        properties["visibilityDuration"] = operation.visibilityDuration
        properties["visibilityEasingType"] = operation.visibilityEasingType
        properties["enterAnimation"] = operation.enterAnimation.ordinal
        properties["exitAnimation"] = operation.exitAnimation.ordinal
    }
}

internal data class RcOtherModifierInspectable(
    override val operation: Operation,
    override val remoteContext: RemoteContext,
) : RcModifierInspectable() {
    override val inspectorName: String
        get() = RcPlayerInspector.INSPECTOR_MODIFIER_NAME
}

internal class RcModifierInspectorNode(var operation: Operation) : Modifier.Node()
