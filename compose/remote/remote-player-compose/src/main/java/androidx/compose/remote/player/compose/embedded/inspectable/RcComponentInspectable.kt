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

import androidx.compose.remote.core.Operation
import androidx.compose.remote.core.RemoteContext
import androidx.compose.remote.core.operations.layout.CanvasContent
import androidx.compose.remote.core.operations.layout.Component
import androidx.compose.remote.core.operations.layout.LayoutComponent
import androidx.compose.remote.core.operations.layout.RootLayoutComponent
import androidx.compose.remote.core.operations.layout.managers.BoxLayout
import androidx.compose.remote.core.operations.layout.managers.CanvasLayout
import androidx.compose.remote.core.operations.layout.managers.CollapsibleColumnLayout
import androidx.compose.remote.core.operations.layout.managers.CollapsibleRowLayout
import androidx.compose.remote.core.operations.layout.managers.ColumnLayout
import androidx.compose.remote.core.operations.layout.managers.CoreText
import androidx.compose.remote.core.operations.layout.managers.Custom
import androidx.compose.remote.core.operations.layout.managers.FitBoxLayout
import androidx.compose.remote.core.operations.layout.managers.FlowLayout
import androidx.compose.remote.core.operations.layout.managers.ImageLayout
import androidx.compose.remote.core.operations.layout.managers.RowLayout
import androidx.compose.remote.core.operations.layout.managers.StateLayout
import androidx.compose.remote.core.operations.layout.managers.TextLayout
import androidx.compose.remote.player.compose.embedded.draw.ComposeLocalPaint
import androidx.compose.remote.player.compose.embedded.draw.updatePaintFromBundle
import androidx.compose.ui.Modifier
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.SemanticsModifierNode
import androidx.compose.ui.platform.InspectableValue
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.util.fastFilter
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMap

/**
 * Base [ModifierNodeElement] and [InspectableValue] representing a RemoteCompose [Component] in
 * both the live Compose `LayoutNode` modifier chain and the document tree.
 *
 * Each subclass is a lightweight, disposable wrapper holding only [component] and [remoteContext]
 * references; all core reads and state lookups are performed lazily when property getters or
 * [inspectableProperties] are invoked.
 */
internal sealed class RcComponentInspectable :
    ModifierNodeElement<RcComponentOuterInspectorNode>() {
    abstract val component: Component
    internal abstract val remoteContext: RemoteContext

    protected abstract val inspectorName: String

    val componentId: Int
        get() = component.getId()

    val kind: String
        get() = component::class.java.simpleName.trimStart('_')

    val visibilityCode: Int
        get() = RcPlayerInspector.resolveComponentVisibilityCode(component, remoteContext)

    val visibility: String
        get() =
            when (visibilityCode) {
                Component.Visibility.GONE -> "GONE"
                Component.Visibility.INVISIBLE -> "INVISIBLE"
                else -> "VISIBLE"
            }

    val x: Float
        get() = component.getX()

    val y: Float
        get() = component.getY()

    val width: Float
        get() = component.getWidth()

    val height: Float
        get() = component.getHeight()

    val modifiers: List<RcModifierInspectable>
        get() =
            (component as? LayoutComponent)?.componentModifiers?.list?.fastMap {
                RcModifierInspectable.of(it, remoteContext)
            } ?: emptyList()

    val drawContentOperations: List<RcOperationInspectable>
        get() =
            (component as? LayoutComponent)?.drawContentOperations?.list?.fastMap {
                RcOperationInspectable.of(it, remoteContext)
            } ?: emptyList()

    val operations: List<InspectableValue>
        get() =
            component.list
                .fastFilter { it !is Component }
                .fastMap { RcPlayerInspector.wrapOperation(it, remoteContext) }

    val children: List<RcComponentInspectable>
        get() {
            val childComponents = ArrayList<Component>()
            if (component is LayoutComponent) {
                childComponents.addAll((component as LayoutComponent).childrenComponents)
            } else {
                component.getComponents(childComponents)
            }
            return childComponents.fastMap { of(it, remoteContext) }
        }

    override fun create(): RcComponentOuterInspectorNode =
        RcComponentOuterInspectorNode(component.getId())

    override fun update(node: RcComponentOuterInspectorNode) {
        node.componentId = component.getId()
    }

    final override fun InspectorInfo.inspectableProperties() {
        name = inspectorName
        value = componentId
        properties["component"] = component
        properties["componentId"] = componentId
        properties["kind"] = kind
        properties["visibility"] = visibility
        properties["x"] = x
        properties["y"] = y
        properties["width"] = width
        properties["height"] = height
        componentProperties()
        val mods = modifiers
        if (mods.isNotEmpty()) {
            properties["modifiers"] = mods
        }
        val kids = children
        if (kids.isNotEmpty()) {
            properties["children"] = kids
        }
    }

    protected open fun InspectorInfo.componentProperties() {}

    companion object {
        @Suppress("ModifierFactoryExtensionFunction", "ModifierFactoryReturnType")
        fun of(component: Component, remoteContext: RemoteContext): RcComponentInspectable =
            when (component) {
                is CoreText -> RcCoreTextInspectable(component, remoteContext)
                is TextLayout -> RcTextLayoutInspectable(component, remoteContext)
                is RowLayout -> RcRowLayoutInspectable(component, remoteContext)
                is ColumnLayout -> RcColumnLayoutInspectable(component, remoteContext)
                is FlowLayout -> RcFlowLayoutInspectable(component, remoteContext)
                is FitBoxLayout -> RcFitBoxLayoutInspectable(component, remoteContext)
                is StateLayout -> RcStateLayoutInspectable(component, remoteContext)
                is ImageLayout -> RcImageLayoutInspectable(component, remoteContext)
                is CanvasLayout -> RcCanvasLayoutInspectable(component, remoteContext)
                is Custom -> RcCustomInspectable(component, remoteContext)
                is BoxLayout -> RcBoxLayoutInspectable(component, remoteContext)
                is RootLayoutComponent -> RcRootLayoutInspectable(component, remoteContext)
                else -> RcOtherComponentInspectable(component, remoteContext)
            }
    }
}

internal data class RcCoreTextInspectable(
    override val component: CoreText,
    override val remoteContext: RemoteContext,
) : RcComponentInspectable() {
    override val inspectorName: String
        get() = "rcCoreText"

    private fun resolvePaint(): ComposeLocalPaint? =
        component.mPaint?.let { bundle ->
            ComposeLocalPaint().also { paint ->
                updatePaintFromBundle(bundle, paint, remoteContext)
            }
        }

    val textId: Int?
        get() = component.textId

    val text: String?
        get() = component.textId?.takeIf { it != 0 && it != -1 }?.let { remoteContext.getText(it) }

    val color: Int
        get() {
            val paint = resolvePaint()
            if (paint != null && paint.isColorSet) return paint.color
            return if (component.isDynamicColorEnabled) {
                remoteContext.getColor(component.colorId)
            } else {
                component.colorValue
            }
        }

    val colorId: Int
        get() = component.colorId

    val isDynamicColorEnabled: Boolean
        get() = component.isDynamicColorEnabled

    val fontSize: Float
        get() {
            val paint = resolvePaint()
            if (paint != null && paint.isTextSizeSet) return paint.textSize
            return component.fontSizeValue
        }

    val fontWeight: Float
        get() {
            val paint = resolvePaint()
            if (paint != null && paint.isTypefaceSet) return paint.fontWeight.toFloat()
            return component.fontWeightValue
        }

    val fontStyle: Int
        get() {
            val paint = resolvePaint()
            if (paint != null && paint.isTypefaceSet) {
                return if (paint.fontStyle == FontStyle.Italic) 1 else 0
            }
            return component.fontStyle
        }

    val fontFamilyType: Int
        get() {
            val paint = resolvePaint()
            if (paint != null && paint.isTypefaceSet) return paint.fontFamily
            return component.type
        }

    val textAlign: Int
        get() = component.textAlignValue

    val overflow: Int
        get() = component.overflow

    val maxLines: Int
        get() = component.maxLines

    val letterSpacing: Float
        get() = component.letterSpacing

    val lineHeightMultiplier: Float
        get() = component.lineHeightMultiplier

    val lineHeightAdd: Float
        get() = component.lineHeightAdd

    val underline: Boolean
        get() = component.underline

    val strikethrough: Boolean
        get() = component.strikethrough

    val autosize: Boolean
        get() = component.autosize

    val minFontSize: Float
        get() = component.minFontSize

    val maxFontSize: Float
        get() = component.maxFontSize

    override fun InspectorInfo.componentProperties() {
        val paint = resolvePaint()
        val resolvedColor =
            if (paint != null && paint.isColorSet) {
                paint.color
            } else if (component.isDynamicColorEnabled) {
                remoteContext.getColor(component.colorId)
            } else {
                component.colorValue
            }
        val resolvedFontSize =
            if (paint != null && paint.isTextSizeSet) paint.textSize else component.fontSizeValue
        val resolvedFontWeight =
            if (paint != null && paint.isTypefaceSet) paint.fontWeight.toFloat()
            else component.fontWeightValue
        val resolvedFontStyle =
            if (paint != null && paint.isTypefaceSet) {
                if (paint.fontStyle == FontStyle.Italic) 1 else 0
            } else {
                component.fontStyle
            }
        val resolvedFontFamily =
            if (paint != null && paint.isTypefaceSet) paint.fontFamily else component.type

        properties["textId"] = component.textId
        properties["text"] = text
        properties["color"] = resolvedColor
        properties["colorId"] = component.colorId
        properties["isDynamicColorEnabled"] = component.isDynamicColorEnabled
        properties["fontSize"] = resolvedFontSize
        properties["fontWeight"] = resolvedFontWeight
        properties["fontStyle"] = resolvedFontStyle
        properties["fontFamilyType"] = resolvedFontFamily
        properties["textAlign"] = component.textAlignValue
        properties["overflow"] = component.overflow
        properties["maxLines"] = component.maxLines
        properties["letterSpacing"] = component.letterSpacing
        properties["lineHeightMultiplier"] = component.lineHeightMultiplier
        properties["lineHeightAdd"] = component.lineHeightAdd
        properties["underline"] = component.underline
        properties["strikethrough"] = component.strikethrough
        properties["autosize"] = component.autosize
    }
}

internal data class RcTextLayoutInspectable(
    override val component: TextLayout,
    override val remoteContext: RemoteContext,
) : RcComponentInspectable() {
    override val inspectorName: String
        get() = "rcTextLayout"

    private fun resolvePaint(): ComposeLocalPaint? =
        component.mPaint?.let { bundle ->
            ComposeLocalPaint().also { paint ->
                updatePaintFromBundle(bundle, paint, remoteContext)
            }
        }

    val textId: Int?
        get() = component.textId

    val text: String?
        get() = component.textId?.takeIf { it != 0 && it != -1 }?.let { remoteContext.getText(it) }

    val color: Int
        get() {
            val paint = resolvePaint()
            if (paint != null && paint.isColorSet) return paint.color
            return if (component.isDynamicColorEnabled) {
                remoteContext.getColor(component.color)
            } else {
                component.colorValue
            }
        }

    val colorId: Int
        get() = component.color

    val isDynamicColorEnabled: Boolean
        get() = component.isDynamicColorEnabled

    val fontSize: Float
        get() {
            val paint = resolvePaint()
            if (paint != null && paint.isTextSizeSet) return paint.textSize
            return component.fontSizeValue
        }

    val fontWeight: Float
        get() {
            val paint = resolvePaint()
            if (paint != null && paint.isTypefaceSet) return paint.fontWeight.toFloat()
            return component.fontWeight
        }

    val fontFamilyType: Int
        get() {
            val paint = resolvePaint()
            if (paint != null && paint.isTypefaceSet) return paint.fontFamily
            return component.type
        }

    val textAlign: Int
        get() = component.textAlignValue

    val overflow: Int
        get() = component.overflow

    val maxLines: Int
        get() = component.maxLines

    override fun InspectorInfo.componentProperties() {
        val paint = resolvePaint()
        val resolvedColor =
            if (paint != null && paint.isColorSet) {
                paint.color
            } else if (component.isDynamicColorEnabled) {
                remoteContext.getColor(component.color)
            } else {
                component.colorValue
            }
        val resolvedFontSize =
            if (paint != null && paint.isTextSizeSet) paint.textSize else component.fontSizeValue
        val resolvedFontWeight =
            if (paint != null && paint.isTypefaceSet) paint.fontWeight.toFloat()
            else component.fontWeight
        val resolvedFontFamily =
            if (paint != null && paint.isTypefaceSet) paint.fontFamily else component.type

        properties["textId"] = component.textId
        properties["text"] = text
        properties["color"] = resolvedColor
        properties["colorId"] = component.color
        properties["isDynamicColorEnabled"] = component.isDynamicColorEnabled
        properties["fontSize"] = resolvedFontSize
        properties["fontWeight"] = resolvedFontWeight
        properties["fontFamilyType"] = resolvedFontFamily
        properties["textAlign"] = component.textAlignValue
        properties["overflow"] = component.overflow
        properties["maxLines"] = component.maxLines
    }
}

internal data class RcRowLayoutInspectable(
    override val component: RowLayout,
    override val remoteContext: RemoteContext,
) : RcComponentInspectable() {
    override val inspectorName: String
        get() = "rcRowLayout"

    val horizontalPositioning: Int
        get() = component.horizontalPositioning

    val verticalPositioning: Int
        get() = component.verticalPositioning

    val spacedBy: Float
        get() = RcPlayerInspector.resolveFloatOrRaw(component.spacedBy, remoteContext)

    val isCollapsible: Boolean
        get() = component is CollapsibleRowLayout

    override fun InspectorInfo.componentProperties() {
        properties["horizontalPositioning"] = horizontalPositioning
        properties["verticalPositioning"] = verticalPositioning
        properties["spacedBy"] = spacedBy
        properties["isCollapsible"] = isCollapsible
    }
}

internal data class RcColumnLayoutInspectable(
    override val component: ColumnLayout,
    override val remoteContext: RemoteContext,
) : RcComponentInspectable() {
    override val inspectorName: String
        get() = "rcColumnLayout"

    val horizontalPositioning: Int
        get() = component.horizontalPositioning

    val verticalPositioning: Int
        get() = component.verticalPositioning

    val spacedBy: Float
        get() = RcPlayerInspector.resolveFloatOrRaw(component.spacedBy, remoteContext)

    val isCollapsible: Boolean
        get() = component is CollapsibleColumnLayout

    override fun InspectorInfo.componentProperties() {
        properties["horizontalPositioning"] = horizontalPositioning
        properties["verticalPositioning"] = verticalPositioning
        properties["spacedBy"] = spacedBy
        properties["isCollapsible"] = isCollapsible
    }
}

internal data class RcBoxLayoutInspectable(
    override val component: BoxLayout,
    override val remoteContext: RemoteContext,
) : RcComponentInspectable() {
    override val inspectorName: String
        get() = "rcBoxLayout"

    val horizontalPositioning: Int
        get() = component.horizontalPositioning

    val verticalPositioning: Int
        get() = component.verticalPositioning

    override fun InspectorInfo.componentProperties() {
        properties["horizontalPositioning"] = horizontalPositioning
        properties["verticalPositioning"] = verticalPositioning
    }
}

internal data class RcFlowLayoutInspectable(
    override val component: FlowLayout,
    override val remoteContext: RemoteContext,
) : RcComponentInspectable() {
    override val inspectorName: String
        get() = "rcFlowLayout"

    val horizontalPositioning: Int
        get() = component.horizontalPositioning

    val verticalPositioning: Int
        get() = component.verticalPositioning

    val spacedBy: Float
        get() = RcPlayerInspector.resolveFloatOrRaw(component.spacedBy, remoteContext)

    val maxItemsInEachRow: Int
        get() = component.mMaxItemsInEachRow

    val maxLines: Int
        get() = component.mMaxLines

    override fun InspectorInfo.componentProperties() {
        properties["horizontalPositioning"] = horizontalPositioning
        properties["verticalPositioning"] = verticalPositioning
        properties["spacedBy"] = spacedBy
        properties["maxItemsInEachRow"] = maxItemsInEachRow
        properties["maxLines"] = maxLines
    }
}

internal data class RcFitBoxLayoutInspectable(
    override val component: FitBoxLayout,
    override val remoteContext: RemoteContext,
) : RcComponentInspectable() {
    override val inspectorName: String
        get() = "rcFitBoxLayout"

    val horizontalPositioning: Int
        get() = component.horizontalPositioning

    val verticalPositioning: Int
        get() = component.verticalPositioning

    override fun InspectorInfo.componentProperties() {
        properties["horizontalPositioning"] = horizontalPositioning
        properties["verticalPositioning"] = verticalPositioning
    }
}

internal data class RcStateLayoutInspectable(
    override val component: StateLayout,
    override val remoteContext: RemoteContext,
) : RcComponentInspectable() {
    override val inspectorName: String
        get() = "rcStateLayout"

    val indexId: Int
        get() = component.indexId

    val currentIndex: Int
        get() = remoteContext.getInteger(component.indexId)

    override fun InspectorInfo.componentProperties() {
        val id = component.indexId
        properties["indexId"] = id
        properties["currentIndex"] = remoteContext.getInteger(id)
    }
}

internal data class RcImageLayoutInspectable(
    override val component: ImageLayout,
    override val remoteContext: RemoteContext,
) : RcComponentInspectable() {
    override val inspectorName: String
        get() = "rcImageLayout"

    val bitmapId: Int
        get() = component.bitmapId

    val scaleType: Int
        get() = component.scaleType

    val alpha: Float
        get() = RcPlayerInspector.resolveFloatOrRaw(component.alpha, remoteContext)

    override fun InspectorInfo.componentProperties() {
        properties["bitmapId"] = component.bitmapId
        properties["scaleType"] = component.scaleType
        properties["alpha"] = alpha
    }
}

internal data class RcCanvasLayoutInspectable(
    override val component: CanvasLayout,
    override val remoteContext: RemoteContext,
) : RcComponentInspectable() {
    override val inspectorName: String
        get() = "rcCanvasLayout"

    val canvasOperations: List<RcOperationInspectable>
        get() {
            val rawOps = findCanvasContentList(component.mList) ?: component.mList
            return rawOps.fastMap { RcOperationInspectable.of(it, remoteContext) }
        }

    override fun InspectorInfo.componentProperties() {
        properties["operations"] = canvasOperations
    }

    private fun findCanvasContentList(ops: List<Operation>): List<Operation>? {
        ops.fastForEach { op ->
            if (op is CanvasContent) return op.mList
            if (op is Component) {
                findCanvasContentList(op.mList)?.let {
                    return it
                }
            }
        }
        return null
    }
}

internal data class RcCustomInspectable(
    override val component: Custom,
    override val remoteContext: RemoteContext,
) : RcComponentInspectable() {
    override val inspectorName: String
        get() = "rcCustom"

    val configId: Int
        get() = component.configId

    val config: String
        get() {
            return if (component.configId != -1) {
                remoteContext.getText(component.configId) ?: ""
            } else {
                component.config ?: ""
            }
        }

    override fun InspectorInfo.componentProperties() {
        properties["configId"] = component.configId
        properties["config"] =
            if (component.configId != -1) {
                remoteContext.getText(component.configId) ?: ""
            } else {
                component.config ?: ""
            }
    }
}

internal data class RcRootLayoutInspectable(
    override val component: RootLayoutComponent,
    override val remoteContext: RemoteContext,
) : RcComponentInspectable() {
    override val inspectorName: String
        get() = "rcRootLayout"

    override fun InspectorInfo.componentProperties() {
        val ops = operations
        if (ops.isNotEmpty()) {
            properties["operations"] = ops
        }
    }
}

internal data class RcOtherComponentInspectable(
    override val component: Component,
    override val remoteContext: RemoteContext,
) : RcComponentInspectable() {
    override val inspectorName: String
        get() = RcPlayerInspector.INSPECTOR_OUTER_NAME
}

internal class RcComponentOuterInspectorNode(var componentId: Int) :
    Modifier.Node(), SemanticsModifierNode {
    override fun SemanticsPropertyReceiver.applySemantics() {
        this[RcPlayerInspector.ComponentIdKey] = componentId
    }
}
