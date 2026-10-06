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

package androidx.compose.remote.player.compose.embedded

import android.graphics.Color as AndroidColor
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.MatrixAccess
import androidx.compose.remote.core.Operation
import androidx.compose.remote.core.RemoteContext
import androidx.compose.remote.core.VariableProvider
import androidx.compose.remote.core.operations.ClipPath
import androidx.compose.remote.core.operations.ClipRect
import androidx.compose.remote.core.operations.ColorConstant
import androidx.compose.remote.core.operations.ColorExpression
import androidx.compose.remote.core.operations.ColorTheme
import androidx.compose.remote.core.operations.ConditionalOperations
import androidx.compose.remote.core.operations.DataListFloat
import androidx.compose.remote.core.operations.DrawArc
import androidx.compose.remote.core.operations.DrawBase2
import androidx.compose.remote.core.operations.DrawBase3
import androidx.compose.remote.core.operations.DrawBase4
import androidx.compose.remote.core.operations.DrawBase6
import androidx.compose.remote.core.operations.DrawBitmap
import androidx.compose.remote.core.operations.DrawBitmapFontText
import androidx.compose.remote.core.operations.DrawBitmapFontTextOnPath
import androidx.compose.remote.core.operations.DrawBitmapInt
import androidx.compose.remote.core.operations.DrawBitmapScaled
import androidx.compose.remote.core.operations.DrawBitmapTextAnchored
import androidx.compose.remote.core.operations.DrawCircle
import androidx.compose.remote.core.operations.DrawLine
import androidx.compose.remote.core.operations.DrawOval
import androidx.compose.remote.core.operations.DrawPath
import androidx.compose.remote.core.operations.DrawRect
import androidx.compose.remote.core.operations.DrawRoundRect
import androidx.compose.remote.core.operations.DrawSector
import androidx.compose.remote.core.operations.DrawTextAnchored
import androidx.compose.remote.core.operations.DrawTextOnCircle
import androidx.compose.remote.core.operations.DrawTextOnPath
import androidx.compose.remote.core.operations.DrawToBitmap
import androidx.compose.remote.core.operations.DrawTweenPath
import androidx.compose.remote.core.operations.FloatConstant
import androidx.compose.remote.core.operations.FloatExpression
import androidx.compose.remote.core.operations.FloatFunctionCall
import androidx.compose.remote.core.operations.FloatFunctionDefine
import androidx.compose.remote.core.operations.IntegerExpression
import androidx.compose.remote.core.operations.MatrixRestore
import androidx.compose.remote.core.operations.MatrixRotate
import androidx.compose.remote.core.operations.MatrixSave
import androidx.compose.remote.core.operations.MatrixScale
import androidx.compose.remote.core.operations.MatrixSkew
import androidx.compose.remote.core.operations.MatrixTranslate
import androidx.compose.remote.core.operations.NamedVariable
import androidx.compose.remote.core.operations.PaintData
import androidx.compose.remote.core.operations.ParticlesCreate
import androidx.compose.remote.core.operations.ParticlesLoop
import androidx.compose.remote.core.operations.PathCreate
import androidx.compose.remote.core.operations.PathData
import androidx.compose.remote.core.operations.PathTween
import androidx.compose.remote.core.operations.ShaderData
import androidx.compose.remote.core.operations.TextFromFloat
import androidx.compose.remote.core.operations.Utils
import androidx.compose.remote.core.operations.layout.CanvasContent
import androidx.compose.remote.core.operations.layout.ClickModifierOperation
import androidx.compose.remote.core.operations.layout.Component
import androidx.compose.remote.core.operations.layout.Container
import androidx.compose.remote.core.operations.layout.ImpulseOperation
import androidx.compose.remote.core.operations.layout.LayoutComponent
import androidx.compose.remote.core.operations.layout.LoopOperation
import androidx.compose.remote.core.operations.layout.MultiClickModifier
import androidx.compose.remote.core.operations.layout.RootLayoutComponent
import androidx.compose.remote.core.operations.layout.TouchCancelModifierOperation
import androidx.compose.remote.core.operations.layout.TouchDownModifierOperation
import androidx.compose.remote.core.operations.layout.TouchUpModifierOperation
import androidx.compose.remote.core.operations.layout.animation.AnimationSpec
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
import androidx.compose.remote.core.operations.utilities.ArrayAccess
import androidx.compose.remote.core.semantics.AccessibleComponent
import androidx.compose.remote.core.semantics.CoreSemantics
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.LayoutInfo
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.SemanticsModifierNode
import androidx.compose.ui.platform.InspectableValue
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.ValueElement
import androidx.compose.ui.platform.isDebugInspectorInfoEnabled
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.util.fastFilter
import androidx.compose.ui.util.fastFirstOrNull
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMap

/**
 * Inspection and test-harness utilities for [RcPlayer] built on Compose's [InspectableValue],
 * [ModifierNodeElement], and [LayoutInfo] abstractions.
 *
 * When [isDebugInspectorInfoEnabled] is `false` (the default in production), [RcPlayer] attaches no
 * inspection modifiers (`Modifier`), incurring zero object allocations and zero layout or draw
 * overhead.
 *
 * When [isDebugInspectorInfoEnabled] is set to `true`, [RcPlayer] attaches lightweight, disposable
 * [ModifierNodeElement]s ([RcComponentInspectable], [RcModifierInspectable], `rcPlayerRoot`, and
 * `rcComponentContent`) that store only object references on creation and compute all properties
 * lazily on demand when inspected.
 */
internal object RcPlayerInspector {

    const val INSPECTOR_ROOT_NAME: String = "rcPlayerRoot"
    const val INSPECTOR_OUTER_NAME: String = "rcComponentOuter"
    const val INSPECTOR_CONTENT_NAME: String = "rcComponentContent"
    const val INSPECTOR_MODIFIER_NAME: String = "rcModifier"

    val PlayerStateKey: SemanticsPropertyKey<RcPlayerState> = SemanticsPropertyKey("RcPlayerState")

    val ComponentIdKey: SemanticsPropertyKey<Int> = SemanticsPropertyKey("RcComponentId")

    val ContentComponentIdKey: SemanticsPropertyKey<Int> =
        SemanticsPropertyKey("RcContentComponentId")

    internal data class TreeNodeSnapshot(
        val id: Int,
        val kind: String,
        val x: Float,
        val y: Float,
        val width: Float,
        val height: Float,
        val depth: Int,
        val isGone: Boolean,
        val visibility: String,
        val absX: Float = x,
        val absY: Float = y,
    )

    /**
     * Captures the live Compose layout tree for [RcPlayer] from [rootSemanticsNode] by inspecting
     * [LayoutInfo.getModifierInfo] and reading [RcComponentInspectable] / [InspectableValue]
     * elements (`rcPlayerRoot`, `rcComponentOuter`, `rcComponentContent`).
     *
     * Requires [isDebugInspectorInfoEnabled] to be `true` when [RcPlayer] was composed.
     */
    fun captureTreeSnapshot(
        rootSemanticsNode: SemanticsNode,
        state: RcPlayerState? = null,
    ): List<TreeNodeSnapshot> {
        var resolvedState = state
        var rootCoords: LayoutCoordinates? = null
        val outerCoords = HashMap<Int, LayoutCoordinates>()
        val contentCoords = HashMap<Int, LayoutCoordinates>()
        val visitedLayouts = HashSet<LayoutInfo>()

        fun inspectLayoutInfo(layoutInfo: LayoutInfo) {
            var current: LayoutInfo? = layoutInfo
            while (current != null && visitedLayouts.add(current)) {
                val modifierInfos = current.getModifierInfo()
                for (i in modifierInfos.indices) {
                    val modInfo = modifierInfos[i]
                    when (val mod = modInfo.modifier) {
                        is RcPlayerRootInspectorElement -> {
                            rootCoords = modInfo.coordinates
                            if (resolvedState == null) {
                                resolvedState = mod.state
                            }
                        }
                        is RcComponentInspectable -> {
                            val id = mod.componentId
                            if (!outerCoords.containsKey(id)) {
                                outerCoords[id] = modInfo.coordinates
                            }
                        }
                        is RcComponentContentInspectorElement -> {
                            val id = mod.component.getId()
                            if (!contentCoords.containsKey(id)) {
                                contentCoords[id] = modInfo.coordinates
                            }
                        }
                        is InspectableValue -> {
                            when (mod.nameFallback) {
                                INSPECTOR_ROOT_NAME -> {
                                    rootCoords = modInfo.coordinates
                                    if (resolvedState == null) {
                                        resolvedState =
                                            mod.inspectableElements
                                                .firstOrNull { it.name == "state" }
                                                ?.value as? RcPlayerState
                                    }
                                }
                                INSPECTOR_OUTER_NAME -> {
                                    val id =
                                        (mod.valueOverride as? Int)
                                            ?: (mod.inspectableElements
                                                .firstOrNull { it.name == "componentId" }
                                                ?.value as? Int)
                                    if (id != null && !outerCoords.containsKey(id)) {
                                        outerCoords[id] = modInfo.coordinates
                                    }
                                }
                                INSPECTOR_CONTENT_NAME -> {
                                    val id =
                                        (mod.valueOverride as? Int)
                                            ?: (mod.inspectableElements
                                                .firstOrNull { it.name == "componentId" }
                                                ?.value as? Int)
                                    if (id != null && !contentCoords.containsKey(id)) {
                                        contentCoords[id] = modInfo.coordinates
                                    }
                                }
                            }
                        }
                    }
                }
                current = current.parentInfo
            }
        }

        fun walkSemantics(node: SemanticsNode) {
            inspectLayoutInfo(node.layoutInfo)
            val children = node.children
            for (i in children.indices) {
                walkSemantics(children[i])
            }
        }

        walkSemantics(rootSemanticsNode)

        val activeState = resolvedState ?: return emptyList()
        val document = activeState.document
        val remoteContext = activeState.remoteContext
        val root = document.rootLayoutComponent ?: return emptyList()
        val out = ArrayList<TreeNodeSnapshot>()
        buildNodeSnapshot(
            component = root,
            parentContentCoords = null,
            parentAbsX = 0f,
            parentAbsY = 0f,
            depth = 0,
            ancestorGone = false,
            remoteContext = remoteContext,
            rootCoords = rootCoords,
            outerCoords = outerCoords,
            contentCoords = contentCoords,
            out = out,
        )
        return out
    }

    private fun buildNodeSnapshot(
        component: Component,
        parentContentCoords: LayoutCoordinates?,
        parentAbsX: Float,
        parentAbsY: Float,
        depth: Int,
        ancestorGone: Boolean,
        remoteContext: RemoteContext,
        rootCoords: LayoutCoordinates?,
        outerCoords: Map<Int, LayoutCoordinates>,
        contentCoords: Map<Int, LayoutCoordinates>,
        out: MutableList<TreeNodeSnapshot>,
    ) {
        val id = component.getId()
        val typeName = component::class.java.simpleName.trimStart('_')
        val ownVisibilityCode = resolveComponentVisibilityCode(component, remoteContext)
        val isGone = ancestorGone || ownVisibilityCode == Component.Visibility.GONE
        val visibilityStr =
            when {
                isGone -> "GONE"
                ownVisibilityCode == Component.Visibility.INVISIBLE -> "INVISIBLE"
                else -> "VISIBLE"
            }

        val outer = outerCoords[id]
        val content = contentCoords[id]

        val x: Float
        val y: Float
        val width: Float
        val height: Float

        if (component is RootLayoutComponent) {
            x = 0f
            y = 0f
            width = rootCoords?.size?.width?.toFloat() ?: component.getWidth()
            height = rootCoords?.size?.height?.toFloat() ?: component.getHeight()
        } else if (component is StateLayout) {
            val children = ArrayList<Component>().apply { component.getComponents(this) }
            val activeChild =
                children.fastFirstOrNull { it.mVisibility != Component.Visibility.GONE }
                    ?: children.firstOrNull()
            val activeOuter = activeChild?.let { outerCoords[it.getId()] }
            width =
                content?.takeIf { it.isAttached }?.size?.width?.toFloat()
                    ?: activeOuter?.takeIf { it.isAttached }?.size?.width?.toFloat()
                    ?: outer?.takeIf { it.isAttached }?.size?.width?.toFloat()
                    ?: component.getWidth()
            height =
                content?.takeIf { it.isAttached }?.size?.height?.toFloat()
                    ?: activeOuter?.takeIf { it.isAttached }?.size?.height?.toFloat()
                    ?: outer?.takeIf { it.isAttached }?.size?.height?.toFloat()
                    ?: component.getHeight()
            if (
                parentContentCoords != null &&
                    parentContentCoords.isAttached &&
                    outer != null &&
                    outer.isAttached
            ) {
                val rel = parentContentCoords.localPositionOf(outer, Offset.Zero)
                x = rel.x
                y = rel.y
            } else {
                x = component.getX()
                y = component.getY()
            }
        } else if (outer != null && outer.isAttached) {
            width = outer.size.width.toFloat()
            height = outer.size.height.toFloat()
            if (parentContentCoords != null && parentContentCoords.isAttached) {
                val rel = parentContentCoords.localPositionOf(outer, Offset.Zero)
                x = rel.x
                y = rel.y
            } else {
                x = component.getX()
                y = component.getY()
            }
        } else {
            x = component.getX()
            y = component.getY()
            width = component.getWidth()
            height = component.getHeight()
        }

        val absX = if (depth == 0) x else parentAbsX + x
        val absY = if (depth == 0) y else parentAbsY + y

        val effectiveContentCoords =
            when {
                component is RootLayoutComponent -> rootCoords
                content != null && content.isAttached -> content
                else -> outer
            }

        val childComponents = ArrayList<Component>()
        if (component is LayoutComponent) {
            childComponents.addAll(component.childrenComponents)
        } else {
            component.getComponents(childComponents)
        }

        for (i in childComponents.indices) {
            buildNodeSnapshot(
                component = childComponents[i],
                parentContentCoords = effectiveContentCoords,
                parentAbsX = absX,
                parentAbsY = absY,
                depth = depth + 1,
                ancestorGone = isGone,
                remoteContext = remoteContext,
                rootCoords = rootCoords,
                outerCoords = outerCoords,
                contentCoords = contentCoords,
                out = out,
            )
        }

        out.add(
            TreeNodeSnapshot(
                id = id,
                kind = typeName,
                x = x,
                y = y,
                width = width,
                height = height,
                depth = depth,
                isGone = isGone,
                visibility = visibilityStr,
                absX = absX,
                absY = absY,
            )
        )
    }

    internal fun resolveComponentVisibilityCode(
        component: Component,
        remoteContext: RemoteContext,
    ): Int {
        if (component.mVisibility == Component.Visibility.GONE) {
            return Component.Visibility.GONE
        }
        if (component.parent is FitBoxLayout || component.parent is StateLayout) {
            return component.mVisibility
        }
        if (component is LayoutComponent) {
            val visibilityOp =
                component.componentModifiers.list.fastFirstOrNull {
                    it is ComponentVisibilityOperation
                } as? ComponentVisibilityOperation
            if (visibilityOp != null) {
                val visId = visibilityOp.getVisibilityIdReflection()
                return remoteContext.getInteger(visId)
            }
        }
        return component.mVisibility
    }

    internal fun resolveFloatOrRaw(raw: Float, remoteContext: RemoteContext): Float {
        return if (Utils.isVariable(raw)) {
            remoteContext.getFloat(Utils.idFromNan(raw))
        } else {
            raw
        }
    }

    fun resolveVariableId(state: RcPlayerState, target: Any): Int? {
        if (target is Number) return target.toInt()
        val name = target.toString()
        val asInt = name.toIntOrNull()
        if (asInt != null) return asInt

        val candidates = buildList {
            add(name)
            if (!name.contains(':') && !state.defaultPrefix.isNullOrEmpty()) {
                add("${state.defaultPrefix}:$name")
            } else if (name.contains(':')) {
                add(name.substringAfter(':'))
            }
        }

        for (i in candidates.indices) {
            val candidate = candidates[i]
            val varId = state.remoteContext.getVariableIdReflection(candidate)
            if (varId != -1) {
                return varId
            }
        }

        val allOps = collectAllOperations(state.document)
        for (i in allOps.indices) {
            val op = allOps[i]
            if (op is NamedVariable && op.mVarName in candidates) {
                return op.mVarId
            }
        }
        return null
    }

    fun resolveFloat(state: RcPlayerState, target: Any): Float? {
        val id = resolveVariableId(state, target) ?: return null
        return state.graphContext.getFloat(id)
    }

    fun resolveInt(state: RcPlayerState, target: Any): Int? {
        val id = resolveVariableId(state, target) ?: return null
        return state.graphContext.getInteger(id)
    }

    fun resolveColor(state: RcPlayerState, target: Any): Long? {
        val id = resolveVariableId(state, target) ?: return null
        val argb = state.graphContext.getColor(id)
        return argb.toLong() and 0xFFFFFFFFL
    }

    fun resolveText(state: RcPlayerState, target: Any): String? {
        val id = resolveVariableId(state, target) ?: return null
        return state.graphContext.getText(id)
    }

    fun resolveMatrix(state: RcPlayerState, target: Int): FloatArray? {
        val obj = state.remoteContext.getObject(target)
        if (obj is MatrixAccess) {
            return obj.get()
        }
        return null
    }

    fun resolveFloatArray(state: RcPlayerState, target: Int): FloatArray? {
        val remoteContext = state.remoteContext
        val fromCollections = remoteContext.getCollectionsAccess()?.getFloats(target)
        if (fromCollections != null) return fromCollections
        val fromState = remoteContext.mRemoteComposeState.getFromId(target)
        when (fromState) {
            is ArrayAccess -> return fromState.getFloats()
            is FloatArray -> return fromState
        }
        val obj = remoteContext.getObject(target)
        when (obj) {
            is ArrayAccess -> return obj.getFloats()
            is FloatArray -> return obj
        }
        return null
    }

    fun resolveParticles(state: RcPlayerState): List<FloatArray> {
        val result = ArrayList<FloatArray>()
        val ops = collectAllOperations(state.document)
        for (i in ops.indices) {
            val op = ops[i]
            if (op is ParticlesCreate) {
                for (p in op.particles) {
                    result.add(p)
                }
            }
        }
        return result
    }

    fun collectAllOperations(document: CoreDocument): List<Operation> {
        val result = ArrayList<Operation>()
        fun walk(ops: Collection<Operation>) {
            for (op in ops) {
                result.add(op)
                if (op is Container) {
                    walk(op.getList())
                }
                if (op is LayoutComponent) {
                    val content = op.getContentReflection()
                    if (content != null) {
                        walk(listOf(content))
                    }
                    val modOps = op.componentModifiers?.list
                    if (modOps != null) {
                        walk(modOps)
                    }
                    val canvasOps = op.getCanvasOperations()
                    if (canvasOps != null) {
                        walk(listOf(canvasOps))
                    }
                }
            }
        }
        walk(document.getOperationsReflection())
        return result
    }

    /**
     * Collects operations in the document's active execution hierarchy (walking into [Component]
     * and [ConditionalOperations] lists).
     */
    fun collectActiveOperations(document: CoreDocument): List<Operation> {
        val result = ArrayList<Operation>()
        fun visit(ops: List<Operation>) {
            for (i in ops.indices) {
                val op = ops[i]
                result.add(op)
                if (op is Component) {
                    visit(op.list)
                } else if (op is ConditionalOperations) {
                    visit(op.list)
                }
            }
        }
        visit(document.getOperationsReflection())
        return result
    }

    /**
     * Returns lazy [RcVariableInspectable] wrappers for all variable/constant/expression
     * definitions in [state].
     */
    fun inspectVariables(state: RcPlayerState): List<RcVariableInspectable> {
        val result = ArrayList<RcVariableInspectable>()
        val ops = collectAllOperations(state.document)
        val remoteContext = state.graphContext
        for (i in ops.indices) {
            val op = ops[i]
            if (
                op is NamedVariable ||
                    op is FloatConstant ||
                    op is ColorConstant ||
                    op is ColorTheme ||
                    op is FloatExpression ||
                    op is IntegerExpression ||
                    op is ColorExpression ||
                    op is DataListFloat ||
                    op is TextFromFloat
            ) {
                result.add(RcVariableInspectable(op, remoteContext))
            }
        }
        return result
    }

    /**
     * Returns lazy [RcFunctionDefineInspectable] wrappers for all [FloatFunctionDefine] operations
     * in [state].
     */
    fun inspectFunctions(state: RcPlayerState): List<RcFunctionDefineInspectable> {
        val result = ArrayList<RcFunctionDefineInspectable>()
        val ops = collectAllOperations(state.document)
        val remoteContext = state.graphContext
        for (i in ops.indices) {
            val op = ops[i]
            if (op is FloatFunctionDefine) {
                result.add(RcFunctionDefineInspectable(op, remoteContext))
            }
        }
        return result
    }

    /**
     * Returns `true` if the point `(x, y)` (in root player coordinates) hits a visible component
     * that has an interactive click or touch modifier (`ClickModifierOperation`,
     * `MultiClickModifier`, `TouchDownModifierOperation`, `TouchUpModifierOperation`, or
     * `TouchCancelModifierOperation`).
     */
    fun isInteractivePoint(
        rootSemanticsNode: SemanticsNode,
        state: RcPlayerState,
        x: Float,
        y: Float,
    ): Boolean {
        val root = state.document.rootLayoutComponent ?: return false
        val nodes = captureTreeSnapshot(rootSemanticsNode, state)
        for (i in nodes.size - 1 downTo 0) {
            val node = nodes[i]
            if (node.visibility != "VISIBLE" || node.isGone) continue
            val inside =
                x >= node.absX &&
                    x <= node.absX + node.width &&
                    y >= node.absY &&
                    y <= node.absY + node.height
            if (inside) {
                val comp = findComponentById(root, node.id)
                if (comp is LayoutComponent) {
                    val modList = comp.componentModifiers.list
                    var hasInteractiveMod = false
                    for (j in modList.indices) {
                        val op = modList[j]
                        if (
                            op is ClickModifierOperation ||
                                op is MultiClickModifier ||
                                op is TouchDownModifierOperation ||
                                op is TouchUpModifierOperation ||
                                op is TouchCancelModifierOperation
                        ) {
                            hasInteractiveMod = true
                            break
                        }
                    }
                    if (!hasInteractiveMod) {
                        val compOps = comp.list
                        for (j in compOps.indices) {
                            val op = compOps[j]
                            if (
                                op is ClickModifierOperation ||
                                    op is MultiClickModifier ||
                                    op is TouchDownModifierOperation ||
                                    op is TouchUpModifierOperation ||
                                    op is TouchCancelModifierOperation
                            ) {
                                hasInteractiveMod = true
                                break
                            }
                        }
                    }
                    if (hasInteractiveMod) return true
                }
            }
        }
        return false
    }

    fun findComponentById(comp: Component, id: Int): Component? {
        if (comp.getId() == id || comp.componentId == id) return comp
        val children = ArrayList<Component>()
        if (comp is LayoutComponent) {
            children.addAll(comp.childrenComponents)
        } else {
            comp.getComponents(children)
        }
        for (i in children.indices) {
            val found = findComponentById(children[i], id)
            if (found != null) return found
        }
        return null
    }

    fun wrapOperation(operation: Operation, remoteContext: RemoteContext): InspectableValue =
        when (operation) {
            is Component -> RcComponentInspectable.of(operation, remoteContext)
            is PaddingModifierOperation,
            is WidthModifierOperation,
            is HeightModifierOperation,
            is WidthInModifierOperation,
            is HeightInModifierOperation,
            is DimensionConstraintsModifierOperation,
            is BackgroundModifierOperation,
            is BorderModifierOperation,
            is OffsetModifierOperation,
            is ZIndexModifierOperation,
            is GraphicsLayerModifierOperation,
            is ScrollModifierOperation,
            is MarqueeModifierOperation,
            is ComponentVisibilityOperation,
            is CoreSemantics,
            is ClickModifierOperation,
            is MultiClickModifier,
            is DrawContentOperation,
            is AnimationSpec -> RcModifierInspectable.of(operation, remoteContext)
            else -> RcOperationInspectable.of(operation, remoteContext)
        }
}

/**
 * Attaches an inspectable [Modifier.Element] when [isDebugInspectorInfoEnabled] is `true`, or
 * returns `this` [Modifier] unchanged (zero allocations, zero node creation) when `false`.
 *
 * Mirrors Compose UI's [androidx.compose.ui.platform.debugInspectorInfo] inline gating pattern so
 * call sites remain clean, unconditional modifier chain calls.
 */
internal inline fun Modifier.remoteInspectable(element: () -> Modifier.Element): Modifier =
    if (isDebugInspectorInfoEnabled) this.then(element()) else this

internal fun Modifier.rcPlayerRootInspector(state: RcPlayerState): Modifier = remoteInspectable {
    RcPlayerRootInspectorElement(state)
}

@Composable
internal fun Modifier.rcComponentOuterInspector(component: Component): Modifier =
    remoteInspectable {
        RcComponentInspectable.of(
            component,
            LocalGraphContext.current ?: LocalRemoteContext.current,
        )
    }

internal fun Modifier.rcComponentContentInspector(
    component: Component,
    forStateLayoutContent: Boolean = false,
): Modifier =
    if (!forStateLayoutContent && component is StateLayout) {
        this
    } else {
        remoteInspectable { RcComponentContentInspectorElement(component) }
    }

@Composable
internal fun Modifier.rcModifierInspector(operation: Operation): Modifier = remoteInspectable {
    RcModifierInspectable.of(operation, LocalGraphContext.current ?: LocalRemoteContext.current)
}

// ============================================================================================
// 1. Component Inspectables (attached directly to LayoutNode modifiers AND in document tree)
// ============================================================================================

/**
 * Base [ModifierNodeElement] and [InspectableValue] representing a RemoteCompose [Component] in
 * both the live Compose `LayoutNode` modifier chain and the document tree.
 *
 * Each subclass is a lightweight, disposable wrapper holding only [component] and [remoteContext]
 * references; all reflection and state lookups are performed lazily when property getters or
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
            (component as? LayoutComponent)?.getDrawContentOperationsListReflection()?.fastMap {
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
            val data = component.readDataReflection()
            return if (data.isDynamicColorEnabled) {
                remoteContext.getColor(data.colorId)
            } else {
                data.colorValue
            }
        }

    val colorId: Int
        get() = component.readDataReflection().colorId

    val isDynamicColorEnabled: Boolean
        get() = component.readDataReflection().isDynamicColorEnabled

    val fontSize: Float
        get() {
            val paint = resolvePaint()
            if (paint != null && paint.isTextSizeSet) return paint.textSize
            return component.readDataReflection().fontSizeValue
        }

    val fontWeight: Float
        get() {
            val paint = resolvePaint()
            if (paint != null && paint.isTypefaceSet) return paint.fontWeight.toFloat()
            return component.readDataReflection().fontWeightValue
        }

    val fontStyle: Int
        get() {
            val paint = resolvePaint()
            if (paint != null && paint.isTypefaceSet) {
                return if (paint.fontStyle == FontStyle.Italic) 1 else 0
            }
            return component.readDataReflection().fontStyle
        }

    val fontFamilyType: Int
        get() {
            val paint = resolvePaint()
            if (paint != null && paint.isTypefaceSet) return paint.fontFamily
            return component.readDataReflection().type
        }

    val textAlign: Int
        get() = component.readDataReflection().textAlignValue

    val overflow: Int
        get() = component.readDataReflection().overflow

    val maxLines: Int
        get() = component.readDataReflection().maxLines

    val letterSpacing: Float
        get() = component.readDataReflection().letterSpacing

    val lineHeightMultiplier: Float
        get() = component.readDataReflection().lineHeightMultiplier

    val lineHeightAdd: Float
        get() = component.readDataReflection().lineHeightAdd

    val underline: Boolean
        get() = component.readDataReflection().underline

    val strikethrough: Boolean
        get() = component.readDataReflection().strikethrough

    val autosize: Boolean
        get() = component.readDataReflection().autosize

    val minFontSize: Float
        get() = component.readDataReflection().minFontSize

    val maxFontSize: Float
        get() = component.readDataReflection().maxFontSize

    override fun InspectorInfo.componentProperties() {
        val data = component.readDataReflection()
        val paint = resolvePaint()
        val resolvedColor =
            if (paint != null && paint.isColorSet) {
                paint.color
            } else if (data.isDynamicColorEnabled) {
                remoteContext.getColor(data.colorId)
            } else {
                data.colorValue
            }
        val resolvedFontSize =
            if (paint != null && paint.isTextSizeSet) paint.textSize else data.fontSizeValue
        val resolvedFontWeight =
            if (paint != null && paint.isTypefaceSet) paint.fontWeight.toFloat()
            else data.fontWeightValue
        val resolvedFontStyle =
            if (paint != null && paint.isTypefaceSet) {
                if (paint.fontStyle == FontStyle.Italic) 1 else 0
            } else {
                data.fontStyle
            }
        val resolvedFontFamily =
            if (paint != null && paint.isTypefaceSet) paint.fontFamily else data.type

        properties["textId"] = component.textId
        properties["text"] = text
        properties["color"] = resolvedColor
        properties["colorId"] = data.colorId
        properties["isDynamicColorEnabled"] = data.isDynamicColorEnabled
        properties["fontSize"] = resolvedFontSize
        properties["fontWeight"] = resolvedFontWeight
        properties["fontStyle"] = resolvedFontStyle
        properties["fontFamilyType"] = resolvedFontFamily
        properties["textAlign"] = data.textAlignValue
        properties["overflow"] = data.overflow
        properties["maxLines"] = data.maxLines
        properties["letterSpacing"] = data.letterSpacing
        properties["lineHeightMultiplier"] = data.lineHeightMultiplier
        properties["lineHeightAdd"] = data.lineHeightAdd
        properties["underline"] = data.underline
        properties["strikethrough"] = data.strikethrough
        properties["autosize"] = data.autosize
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
            val data = component.readDataReflection()
            return if (data.isDynamicColorEnabled) {
                remoteContext.getColor(data.colorId)
            } else {
                data.colorValue
            }
        }

    val colorId: Int
        get() = component.readDataReflection().colorId

    val isDynamicColorEnabled: Boolean
        get() = component.readDataReflection().isDynamicColorEnabled

    val fontSize: Float
        get() {
            val paint = resolvePaint()
            if (paint != null && paint.isTextSizeSet) return paint.textSize
            return component.readDataReflection().fontSizeValue
        }

    val fontWeight: Float
        get() {
            val paint = resolvePaint()
            if (paint != null && paint.isTypefaceSet) return paint.fontWeight.toFloat()
            return component.readDataReflection().fontWeight
        }

    val fontFamilyType: Int
        get() {
            val paint = resolvePaint()
            if (paint != null && paint.isTypefaceSet) return paint.fontFamily
            return component.readDataReflection().type
        }

    val textAlign: Int
        get() = component.readDataReflection().textAlignValue

    val overflow: Int
        get() = component.readDataReflection().overflow

    val maxLines: Int
        get() = component.readDataReflection().maxLines

    override fun InspectorInfo.componentProperties() {
        val data = component.readDataReflection()
        val paint = resolvePaint()
        val resolvedColor =
            if (paint != null && paint.isColorSet) {
                paint.color
            } else if (data.isDynamicColorEnabled) {
                remoteContext.getColor(data.colorId)
            } else {
                data.colorValue
            }
        val resolvedFontSize =
            if (paint != null && paint.isTextSizeSet) paint.textSize else data.fontSizeValue
        val resolvedFontWeight =
            if (paint != null && paint.isTypefaceSet) paint.fontWeight.toFloat()
            else data.fontWeight
        val resolvedFontFamily =
            if (paint != null && paint.isTypefaceSet) paint.fontFamily else data.type

        properties["textId"] = component.textId
        properties["text"] = text
        properties["color"] = resolvedColor
        properties["colorId"] = data.colorId
        properties["isDynamicColorEnabled"] = data.isDynamicColorEnabled
        properties["fontSize"] = resolvedFontSize
        properties["fontWeight"] = resolvedFontWeight
        properties["fontFamilyType"] = resolvedFontFamily
        properties["textAlign"] = data.textAlignValue
        properties["overflow"] = data.overflow
        properties["maxLines"] = data.maxLines
    }
}

internal data class RcRowLayoutInspectable(
    override val component: RowLayout,
    override val remoteContext: RemoteContext,
) : RcComponentInspectable() {
    override val inspectorName: String
        get() = "rcRowLayout"

    val horizontalPositioning: Int
        get() = component.horizontalPositioningReflection

    val verticalPositioning: Int
        get() = component.verticalPositioningReflection

    val spacedBy: Float
        get() = RcPlayerInspector.resolveFloatOrRaw(rowSpacedBy(component), remoteContext)

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
        get() = component.horizontalPositioningReflection

    val verticalPositioning: Int
        get() = component.verticalPositioningReflection

    val spacedBy: Float
        get() = RcPlayerInspector.resolveFloatOrRaw(columnSpacedBy(component), remoteContext)

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
        get() = component.horizontalPositioningReflection

    val verticalPositioning: Int
        get() = component.verticalPositioningReflection

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
        get() = component.horizontalPositioningReflection

    val verticalPositioning: Int
        get() = component.verticalPositioningReflection

    val spacedBy: Float
        get() = RcPlayerInspector.resolveFloatOrRaw(rowSpacedBy(component), remoteContext)

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
        get() = component.horizontalPositioningReflection

    val verticalPositioning: Int
        get() = component.verticalPositioningReflection

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
        get() = component.indexIdReflection

    val currentIndex: Int
        get() = remoteContext.getInteger(component.indexIdReflection)

    override fun InspectorInfo.componentProperties() {
        val id = component.indexIdReflection
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
        get() = component.readData().configId

    val config: String
        get() {
            val d = component.readData()
            return if (d.configId != -1) {
                remoteContext.getText(d.configId) ?: ""
            } else {
                d.config ?: ""
            }
        }

    override fun InspectorInfo.componentProperties() {
        val d = component.readData()
        properties["configId"] = d.configId
        properties["config"] =
            if (d.configId != -1) remoteContext.getText(d.configId) ?: "" else d.config ?: ""
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

// ============================================================================================
// 2. Modifier Inspectables (attached directly to LayoutNode modifiers AND on components)
// ============================================================================================

/**
 * Base [ModifierNodeElement] and [InspectableValue] representing a RemoteCompose modifier
 * [Operation] on a component's `LayoutNode`.
 *
 * Each subclass is a lightweight, disposable wrapper holding only [operation] and [remoteContext]
 * references; all reflection and variable resolution are performed lazily when property getters or
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
        get() = RcPlayerInspector.resolveFloatOrRaw(paddingRawValues(operation)[0], remoteContext)

    val top: Float
        get() = RcPlayerInspector.resolveFloatOrRaw(paddingRawValues(operation)[1], remoteContext)

    val right: Float
        get() = RcPlayerInspector.resolveFloatOrRaw(paddingRawValues(operation)[2], remoteContext)

    val bottom: Float
        get() = RcPlayerInspector.resolveFloatOrRaw(paddingRawValues(operation)[3], remoteContext)

    override fun InspectorInfo.modifierProperties() {
        val raw = paddingRawValues(operation)
        properties["left"] = RcPlayerInspector.resolveFloatOrRaw(raw[0], remoteContext)
        properties["top"] = RcPlayerInspector.resolveFloatOrRaw(raw[1], remoteContext)
        properties["right"] = RcPlayerInspector.resolveFloatOrRaw(raw[2], remoteContext)
        properties["bottom"] = RcPlayerInspector.resolveFloatOrRaw(raw[3], remoteContext)
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
        get() = dimensionRawValue(operation)

    val value: Float
        get() = RcPlayerInspector.resolveFloatOrRaw(dimensionRawValue(operation), remoteContext)

    val variableId: Int?
        get() {
            val raw = dimensionRawValue(operation)
            return if (Utils.isVariable(raw)) Utils.idFromNan(raw) else null
        }

    override fun InspectorInfo.modifierProperties() {
        val raw = dimensionRawValue(operation)
        properties["dimensionType"] = operation.type
        properties["value"] = RcPlayerInspector.resolveFloatOrRaw(raw, remoteContext)
        if (Utils.isVariable(raw)) {
            properties["variableId"] = Utils.idFromNan(raw)
        }
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
        get() = dimensionRawValue(operation)

    val value: Float
        get() = RcPlayerInspector.resolveFloatOrRaw(dimensionRawValue(operation), remoteContext)

    val variableId: Int?
        get() {
            val raw = dimensionRawValue(operation)
            return if (Utils.isVariable(raw)) Utils.idFromNan(raw) else null
        }

    override fun InspectorInfo.modifierProperties() {
        val raw = dimensionRawValue(operation)
        properties["dimensionType"] = operation.type
        properties["value"] = RcPlayerInspector.resolveFloatOrRaw(raw, remoteContext)
        if (Utils.isVariable(raw)) {
            properties["variableId"] = Utils.idFromNan(raw)
        }
    }
}

internal data class RcWidthInModifierInspectable(
    override val operation: WidthInModifierOperation,
    override val remoteContext: RemoteContext,
) : RcModifierInspectable() {
    override val inspectorName: String
        get() = "rcWidthIn"

    val min: Float
        get() =
            RcPlayerInspector.resolveFloatOrRaw(dimensionInRawValues(operation)[0], remoteContext)

    val max: Float
        get() =
            RcPlayerInspector.resolveFloatOrRaw(dimensionInRawValues(operation)[1], remoteContext)

    override fun InspectorInfo.modifierProperties() {
        val raw = dimensionInRawValues(operation)
        properties["min"] = RcPlayerInspector.resolveFloatOrRaw(raw[0], remoteContext)
        properties["max"] = RcPlayerInspector.resolveFloatOrRaw(raw[1], remoteContext)
    }
}

internal data class RcHeightInModifierInspectable(
    override val operation: HeightInModifierOperation,
    override val remoteContext: RemoteContext,
) : RcModifierInspectable() {
    override val inspectorName: String
        get() = "rcHeightIn"

    val min: Float
        get() =
            RcPlayerInspector.resolveFloatOrRaw(dimensionInRawValues(operation)[0], remoteContext)

    val max: Float
        get() =
            RcPlayerInspector.resolveFloatOrRaw(dimensionInRawValues(operation)[1], remoteContext)

    override fun InspectorInfo.modifierProperties() {
        val raw = dimensionInRawValues(operation)
        properties["min"] = RcPlayerInspector.resolveFloatOrRaw(raw[0], remoteContext)
        properties["max"] = RcPlayerInspector.resolveFloatOrRaw(raw[1], remoteContext)
    }
}

internal data class RcDimensionConstraintsModifierInspectable(
    override val operation: DimensionConstraintsModifierOperation,
    override val remoteContext: RemoteContext,
) : RcModifierInspectable() {
    override val inspectorName: String
        get() = "rcDimensionConstraints"

    val constraintsType: Int
        get() = dimensionConstraintsType(operation)

    val min: Float
        get() =
            RcPlayerInspector.resolveFloatOrRaw(dimensionInRawValues(operation)[0], remoteContext)

    val max: Float
        get() =
            RcPlayerInspector.resolveFloatOrRaw(dimensionInRawValues(operation)[1], remoteContext)

    override fun InspectorInfo.modifierProperties() {
        val raw = dimensionInRawValues(operation)
        properties["constraintsType"] = dimensionConstraintsType(operation)
        properties["min"] = RcPlayerInspector.resolveFloatOrRaw(raw[0], remoteContext)
        properties["max"] = RcPlayerInspector.resolveFloatOrRaw(raw[1], remoteContext)
    }
}

internal data class RcBackgroundModifierInspectable(
    override val operation: BackgroundModifierOperation,
    override val remoteContext: RemoteContext,
) : RcModifierInspectable() {
    override val inspectorName: String
        get() = "rcBackground"

    val useColorId: Boolean
        get() = operation.readDataReflection().useColorId

    val colorId: Int
        get() = operation.readDataReflection().colorId

    val shapeType: Int
        get() = operation.readDataReflection().shapeType

    val color: Int
        get() = resolveColor(operation.readDataReflection())

    private fun resolveColor(d: BackgroundModifierOperationData): Int {
        return if (d.useColorId) {
            remoteContext.getColor(d.colorId)
        } else {
            val a =
                (RcPlayerInspector.resolveFloatOrRaw(d.aId, remoteContext) * 255f)
                    .toInt()
                    .coerceIn(0, 255)
            val r =
                (RcPlayerInspector.resolveFloatOrRaw(d.rId, remoteContext) * 255f)
                    .toInt()
                    .coerceIn(0, 255)
            val g =
                (RcPlayerInspector.resolveFloatOrRaw(d.gId, remoteContext) * 255f)
                    .toInt()
                    .coerceIn(0, 255)
            val b =
                (RcPlayerInspector.resolveFloatOrRaw(d.bId, remoteContext) * 255f)
                    .toInt()
                    .coerceIn(0, 255)
            AndroidColor.argb(a, r, g, b)
        }
    }

    override fun InspectorInfo.modifierProperties() {
        val d = operation.readDataReflection()
        properties["color"] = resolveColor(d)
        properties["colorId"] = d.colorId
        properties["useColorId"] = d.useColorId
        properties["shapeType"] = d.shapeType
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
                operation.readDataReflection().borderWidth,
                remoteContext,
            )

    val roundedCorner: Float
        get() =
            RcPlayerInspector.resolveFloatOrRaw(
                operation.readDataReflection().roundedCorner,
                remoteContext,
            )

    val shapeType: Int
        get() = operation.readDataReflection().shapeType

    val colorId: Int
        get() = operation.readDataReflection().colorId

    val useColorId: Boolean
        get() = operation.readDataReflection().useColorId

    override fun InspectorInfo.modifierProperties() {
        val d = operation.readDataReflection()
        properties["borderWidth"] =
            RcPlayerInspector.resolveFloatOrRaw(d.borderWidth, remoteContext)
        properties["roundedCorner"] =
            RcPlayerInspector.resolveFloatOrRaw(d.roundedCorner, remoteContext)
        properties["shapeType"] = d.shapeType
        properties["colorId"] = d.colorId
        properties["useColorId"] = d.useColorId
    }
}

internal data class RcOffsetModifierInspectable(
    override val operation: OffsetModifierOperation,
    override val remoteContext: RemoteContext,
) : RcModifierInspectable() {
    override val inspectorName: String
        get() = "rcOffset"

    val x: Float
        get() = RcPlayerInspector.resolveFloatOrRaw(offsetRawValues(operation)[0], remoteContext)

    val y: Float
        get() = RcPlayerInspector.resolveFloatOrRaw(offsetRawValues(operation)[1], remoteContext)

    override fun InspectorInfo.modifierProperties() {
        val raw = offsetRawValues(operation)
        properties["x"] = RcPlayerInspector.resolveFloatOrRaw(raw[0], remoteContext)
        properties["y"] = RcPlayerInspector.resolveFloatOrRaw(raw[1], remoteContext)
    }
}

internal data class RcZIndexModifierInspectable(
    override val operation: ZIndexModifierOperation,
    override val remoteContext: RemoteContext,
) : RcModifierInspectable() {
    override val inspectorName: String
        get() = "rcZIndex"

    val zIndex: Float
        get() = RcPlayerInspector.resolveFloatOrRaw(operation.valueReflection, remoteContext)

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
                operation.getValuesReflection().fastForEach {
                    map[it.name] = RcPlayerInspector.resolveFloatOrRaw(it.source, remoteContext)
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
        get() = RcPlayerInspector.resolveFloatOrRaw(scrollPosition(operation), remoteContext)

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
        get() = operation.readDataReflection().iterations

    val animationMode: Int
        get() = operation.readDataReflection().animationMode

    val repeatDelayMillis: Float
        get() = operation.readDataReflection().repeatDelayMillis

    val initialDelayMillis: Float
        get() = operation.readDataReflection().initialDelayMillis

    val spacing: Float
        get() = operation.readDataReflection().spacing

    val velocity: Float
        get() = operation.readDataReflection().velocity

    override fun InspectorInfo.modifierProperties() {
        val d = operation.readDataReflection()
        properties["iterations"] = d.iterations
        properties["animationMode"] = d.animationMode
        properties["repeatDelayMillis"] = d.repeatDelayMillis
        properties["initialDelayMillis"] = d.initialDelayMillis
        properties["spacing"] = d.spacing
        properties["velocity"] = d.velocity
    }
}

internal data class RcVisibilityModifierInspectable(
    override val operation: ComponentVisibilityOperation,
    override val remoteContext: RemoteContext,
) : RcModifierInspectable() {
    override val inspectorName: String
        get() = "rcVisibility"

    val visibilityId: Int
        get() = operation.getVisibilityIdReflection()

    val visibilityCode: Int
        get() = remoteContext.getInteger(operation.getVisibilityIdReflection())

    val visibility: String
        get() =
            when (visibilityCode) {
                Component.Visibility.GONE -> "GONE"
                Component.Visibility.INVISIBLE -> "INVISIBLE"
                else -> "VISIBLE"
            }

    override fun InspectorInfo.modifierProperties() {
        val id = operation.getVisibilityIdReflection()
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
        get() = operation.clickTypeReflection

    val actions: List<RcOperationInspectable>
        get() = operation.list.fastMap { RcOperationInspectable.of(it, remoteContext) }

    override fun InspectorInfo.modifierProperties() {
        properties["clickType"] = operation.clickTypeReflection
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

// ============================================================================================
// 3. Operation Inspectables (Canvas draws, Loops, Conditionals, Functions, Variables)
// ============================================================================================

/**
 * Base [InspectableValue] representing a non-component, non-modifier RemoteCompose [Operation]
 * (such as a canvas draw command, [LoopOperation], [ParticlesLoop], [ConditionalOperations],
 * [FloatFunctionDefine], [FloatFunctionCall], or variable definition).
 *
 * Each subclass is a lightweight, disposable wrapper holding only [operation] and [remoteContext]
 * references; all reflection and state lookups are performed lazily when property getters or
 * [inspectableElements] are iterated.
 */
internal sealed class RcOperationInspectable : InspectableValue {
    abstract val operation: Operation
    internal abstract val remoteContext: RemoteContext

    val kind: String
        get() = operation::class.java.simpleName.trimStart('_')

    override val nameFallback: String
        get() = kind

    override val valueOverride: Any
        get() = operation

    companion object {
        fun of(operation: Operation, remoteContext: RemoteContext): RcOperationInspectable =
            when (operation) {
                is LoopOperation -> RcLoopInspectable(operation, remoteContext)
                is ParticlesLoop -> RcParticlesLoopInspectable(operation, remoteContext)
                is ConditionalOperations -> RcConditionalInspectable(operation, remoteContext)
                is FloatFunctionDefine -> RcFunctionDefineInspectable(operation, remoteContext)
                is FloatFunctionCall -> RcFunctionCallInspectable(operation, remoteContext)
                is NamedVariable,
                is FloatConstant,
                is ColorConstant,
                is ColorTheme,
                is FloatExpression,
                is TextFromFloat -> RcVariableInspectable(operation, remoteContext)
                is ImpulseOperation -> RcImpulseInspectable(operation, remoteContext)
                is ShaderData -> RcShaderDataInspectable(operation, remoteContext)
                is PathData,
                is PathCreate,
                is PathTween -> RcPathOperationInspectable(operation, remoteContext)
                is DrawRect,
                is DrawCircle,
                is DrawOval,
                is DrawLine,
                is DrawRoundRect,
                is DrawArc,
                is DrawSector,
                is DrawPath,
                is DrawTweenPath,
                is DrawBitmap,
                is DrawBitmapInt,
                is DrawBitmapScaled,
                is DrawTextAnchored,
                is DrawTextOnPath,
                is DrawTextOnCircle,
                is DrawBitmapFontText,
                is DrawBitmapFontTextOnPath,
                is DrawBitmapTextAnchored,
                is DrawToBitmap,
                is PaintData,
                is ClipRect,
                is ClipPath,
                is MatrixSave,
                is MatrixRestore,
                is MatrixTranslate,
                is MatrixRotate,
                is MatrixScale,
                is MatrixSkew -> RcDrawOperationInspectable(operation, remoteContext)
                else -> RcOtherOperationInspectable(operation, remoteContext)
            }
    }
}

internal data class RcLoopInspectable(
    override val operation: LoopOperation,
    override val remoteContext: RemoteContext,
) : RcOperationInspectable() {
    override val nameFallback: String
        get() = "rcLoop"

    val indexVariableId: Int
        get() = operation.readData().indexVariableId

    val from: Float
        get() = RcPlayerInspector.resolveFloatOrRaw(operation.fromRawReflection, remoteContext)

    val until: Float
        get() = RcPlayerInspector.resolveFloatOrRaw(operation.untilRawReflection, remoteContext)

    val step: Float
        get() = RcPlayerInspector.resolveFloatOrRaw(operation.stepRawReflection, remoteContext)

    val estimatedIterations: Int
        get() {
            val f = from
            val u = until
            val s = if (step == 0f) 1f else step
            return if ((s > 0f && u > f) || (s < 0f && u < f)) {
                ((u - f) / s).toInt().coerceAtLeast(0)
            } else {
                0
            }
        }

    val operations: List<Operation>
        get() = operation.list

    val operationsInspectable: List<InspectableValue>
        get() = operation.list.fastMap { RcPlayerInspector.wrapOperation(it, remoteContext) }

    override val inspectableElements: Sequence<ValueElement>
        get() = sequence {
            yield(ValueElement("kind", kind))
            yield(ValueElement("indexVariableId", indexVariableId))
            yield(ValueElement("from", from))
            yield(ValueElement("until", until))
            yield(ValueElement("step", step))
            yield(ValueElement("estimatedIterations", estimatedIterations))
            yield(ValueElement("operations", operationsInspectable))
        }
}

internal data class RcParticlesLoopInspectable(
    override val operation: ParticlesLoop,
    override val remoteContext: RemoteContext,
) : RcOperationInspectable() {
    override val nameFallback: String
        get() = "rcParticlesLoop"

    val source: ParticlesCreate?
        get() = operation.particlesSourceReflection

    val particleCount: Int
        get() = source?.particles?.size ?: 0

    val operationsInspectable: List<InspectableValue>
        get() = operation.list.fastMap { RcPlayerInspector.wrapOperation(it, remoteContext) }

    override val inspectableElements: Sequence<ValueElement>
        get() = sequence {
            yield(ValueElement("kind", kind))
            yield(ValueElement("particleCount", particleCount))
            yield(ValueElement("operations", operationsInspectable))
        }
}

internal data class RcConditionalInspectable(
    override val operation: ConditionalOperations,
    override val remoteContext: RemoteContext,
) : RcOperationInspectable() {
    override val nameFallback: String
        get() = "rcConditional"

    val typeCode: Int
        get() = operation.readData().type.toInt()

    val type: String
        get() =
            when (typeCode) {
                0 -> "eq"
                1 -> "neq"
                2 -> "lt"
                3 -> "lte"
                4 -> "gt"
                5 -> "gte"
                6 -> "changed"
                else -> "eq"
            }

    val varA: Float
        get() = RcPlayerInspector.resolveFloatOrRaw(operation.varARawReflection, remoteContext)

    val varB: Float
        get() = RcPlayerInspector.resolveFloatOrRaw(operation.varBRawReflection, remoteContext)

    val conditionMet: Boolean
        get() {
            val va = varA
            val vb = varB
            return when (typeCode) {
                0 -> va == vb
                1 -> va != vb
                2 -> va < vb
                3 -> va <= vb
                4 -> va > vb
                5 -> va >= vb
                else -> false
            }
        }

    val operations: List<Operation>
        get() = operation.list

    val operationsInspectable: List<InspectableValue>
        get() = operation.list.fastMap { RcPlayerInspector.wrapOperation(it, remoteContext) }

    override val inspectableElements: Sequence<ValueElement>
        get() = sequence {
            yield(ValueElement("kind", kind))
            yield(ValueElement("type", type))
            yield(ValueElement("varA", varA))
            yield(ValueElement("varB", varB))
            yield(ValueElement("conditionMet", conditionMet))
            yield(ValueElement("operations", operationsInspectable))
        }
}

internal data class RcFunctionDefineInspectable(
    override val operation: FloatFunctionDefine,
    override val remoteContext: RemoteContext,
) : RcOperationInspectable() {
    override val nameFallback: String
        get() = "rcFunctionDefine"

    val functionId: Int
        get() = operation.idReflection

    val argVarIds: List<Int>
        get() = operation.floatVarIdsReflection.toList()

    val operations: List<Operation>
        get() = operation.list

    val operationsInspectable: List<InspectableValue>
        get() = operation.list.fastMap { RcPlayerInspector.wrapOperation(it, remoteContext) }

    override val inspectableElements: Sequence<ValueElement>
        get() = sequence {
            yield(ValueElement("kind", kind))
            yield(ValueElement("functionId", functionId))
            yield(ValueElement("argVarIds", argVarIds))
            yield(ValueElement("operations", operationsInspectable))
        }
}

internal data class RcFunctionCallInspectable(
    override val operation: FloatFunctionCall,
    override val remoteContext: RemoteContext,
) : RcOperationInspectable() {
    override val nameFallback: String
        get() = "rcFunctionCall"

    val functionId: Int
        get() = operation.idReflection

    val rawArgs: List<Float>
        get() = operation.argsReflection?.toList() ?: emptyList()

    val resolvedArgs: List<Float>
        get() =
            operation.argsReflection?.map {
                RcPlayerInspector.resolveFloatOrRaw(it, remoteContext)
            } ?: emptyList()

    val function: RcFunctionDefineInspectable?
        get() {
            val def =
                (operation.readData().function as? FloatFunctionDefine)
                    ?: (remoteContext.getObject(functionId) as? FloatFunctionDefine)
            return def?.let { RcFunctionDefineInspectable(it, remoteContext) }
        }

    override val inspectableElements: Sequence<ValueElement>
        get() = sequence {
            yield(ValueElement("kind", kind))
            yield(ValueElement("functionId", functionId))
            yield(ValueElement("args", resolvedArgs))
            yield(ValueElement("function", function))
        }
}

internal data class RcVariableInspectable(
    override val operation: Operation,
    override val remoteContext: RemoteContext,
) : RcOperationInspectable() {
    override val nameFallback: String
        get() = "rcVariable"

    val variableId: Int
        get() =
            when (val op = operation) {
                is NamedVariable -> op.mVarId
                is VariableProvider -> op.id
                else -> -1
            }

    val variableName: String?
        get() = (operation as? NamedVariable)?.mVarName

    val variableType: Int?
        get() = (operation as? NamedVariable)?.mVarType

    val resolvedValue: Any?
        get() {
            val id = variableId
            if (id <= 0) return null
            return when (val op = operation) {
                is NamedVariable ->
                    when (op.mVarType) {
                        NamedVariable.FLOAT_TYPE -> remoteContext.getFloat(id)
                        NamedVariable.INT_TYPE -> remoteContext.getInteger(id)
                        NamedVariable.STRING_TYPE -> remoteContext.getText(id)
                        NamedVariable.COLOR_TYPE -> remoteContext.getColor(id)
                        else -> remoteContext.getFloat(id)
                    }
                is ColorConstant,
                is ColorExpression,
                is ColorTheme -> remoteContext.getColor(id)
                is TextFromFloat -> remoteContext.getText(id)
                is IntegerExpression -> remoteContext.getInteger(id)
                is FloatConstant,
                is FloatExpression -> remoteContext.getFloat(id)
                else -> null
            }
        }

    override val inspectableElements: Sequence<ValueElement>
        get() = sequence {
            yield(ValueElement("kind", kind))
            yield(ValueElement("variableId", variableId))
            variableName?.let { yield(ValueElement("variableName", it)) }
            variableType?.let { yield(ValueElement("variableType", it)) }
            yield(ValueElement("value", resolvedValue))
        }
}

internal data class RcImpulseInspectable(
    override val operation: ImpulseOperation,
    override val remoteContext: RemoteContext,
) : RcOperationInspectable() {
    override val nameFallback: String
        get() = "rcImpulse"

    val duration: Float
        get() = RcPlayerInspector.resolveFloatOrRaw(operation.durationReflection, remoteContext)

    val startAt: Float
        get() = RcPlayerInspector.resolveFloatOrRaw(operation.startAtReflection, remoteContext)

    val operationsInspectable: List<InspectableValue>
        get() = operation.list.fastMap { RcPlayerInspector.wrapOperation(it, remoteContext) }

    override val inspectableElements: Sequence<ValueElement>
        get() = sequence {
            yield(ValueElement("kind", kind))
            yield(ValueElement("duration", duration))
            yield(ValueElement("startAt", startAt))
            yield(ValueElement("operations", operationsInspectable))
        }
}

internal data class RcShaderDataInspectable(
    override val operation: ShaderData,
    override val remoteContext: RemoteContext,
) : RcOperationInspectable() {
    override val nameFallback: String
        get() = "rcShaderData"

    val shaderId: Int
        get() = operation.id

    val uniformFloatNames: List<String>
        get() = operation.uniformFloatNames?.toList() ?: emptyList()

    val uniformIntegerNames: List<String>
        get() = operation.uniformIntegerNames?.toList() ?: emptyList()

    val uniformBitmapNames: List<String>
        get() = operation.uniformBitmapNames?.toList() ?: emptyList()

    override val inspectableElements: Sequence<ValueElement>
        get() = sequence {
            yield(ValueElement("kind", kind))
            yield(ValueElement("shaderId", shaderId))
            yield(ValueElement("uniformFloatNames", uniformFloatNames))
            yield(ValueElement("uniformIntegerNames", uniformIntegerNames))
            yield(ValueElement("uniformBitmapNames", uniformBitmapNames))
        }
}

internal data class RcPathOperationInspectable(
    override val operation: Operation,
    override val remoteContext: RemoteContext,
) : RcOperationInspectable() {
    override val nameFallback: String
        get() =
            when (operation) {
                is PathData -> "rcPathData"
                is PathCreate -> "rcPathCreate"
                is PathTween -> "rcPathTween"
                else -> "rcPath"
            }

    val instanceId: Int?
        get() =
            when (val op = operation) {
                is PathData -> op.instanceIdReflection
                is PathCreate -> op.id
                else -> null
            }

    val outId: Int?
        get() = (operation as? PathTween)?.id

    val floatPath: FloatArray?
        get() = (operation as? PathData)?.floatPathReflection

    val floatPathLength: Int?
        get() = floatPath?.size

    override val inspectableElements: Sequence<ValueElement>
        get() = sequence {
            yield(ValueElement("kind", kind))
            instanceId?.let { yield(ValueElement("instanceId", it)) }
            outId?.let { yield(ValueElement("outId", it)) }
            floatPathLength?.let { yield(ValueElement("floatPathLength", it)) }
        }
}

internal data class RcDrawOperationInspectable(
    override val operation: Operation,
    override val remoteContext: RemoteContext,
) : RcOperationInspectable() {
    val commandName: String?
        get() =
            when (operation) {
                is PaintData -> "paint"
                is DrawRect -> "drawRect"
                is DrawCircle -> "drawCircle"
                is DrawRoundRect -> "drawRoundRect"
                is DrawLine -> "drawLine"
                is DrawOval -> "drawOval"
                is DrawArc -> "drawArc"
                is DrawSector -> "drawSector"
                is DrawPath -> "drawPath"
                is DrawTweenPath -> "drawTweenPath"
                is DrawBitmap -> "drawBitmap"
                is DrawBitmapInt -> "drawBitmapInt"
                is DrawBitmapScaled -> "drawBitmapScaled"
                is DrawTextAnchored -> "drawTextAnchored"
                is DrawTextOnPath -> "drawTextOnPath"
                is DrawTextOnCircle -> "drawTextOnCircle"
                is DrawBitmapFontText -> "drawBitmapFontText"
                is DrawBitmapFontTextOnPath -> "drawBitmapFontTextOnPath"
                is DrawBitmapTextAnchored -> "drawBitmapTextAnchored"
                is DrawToBitmap -> "drawToBitmap"
                is ClipRect -> "clipRect"
                is ClipPath -> "clipPath"
                is MatrixSave -> "save"
                is MatrixRestore -> "restore"
                is MatrixTranslate -> "translate"
                is MatrixRotate -> "rotate"
                is MatrixScale -> "scale"
                is MatrixSkew -> "skew"
                else -> null
            }

    override val nameFallback: String
        get() = commandName ?: kind

    val textId: Int?
        get() =
            when (val op = operation) {
                is DrawTextAnchored -> op.readData().textId
                is DrawTextOnPath -> op.mTextId
                is DrawTextOnCircle -> op.mTextId
                is DrawBitmapFontText -> op.readData().textId
                is DrawBitmapFontTextOnPath -> op.readData().textId
                is DrawBitmapTextAnchored -> op.readData().textId
                else -> null
            }

    val text: String?
        get() = textId?.let { remoteContext.getText(it) }

    val pathId: Int?
        get() =
            when (val op = operation) {
                is DrawPath -> op.readDataReflection().id
                is DrawTextOnPath -> op.readData().pathId
                is DrawBitmapFontTextOnPath -> op.readData().pathId
                is ClipPath -> op.readData().id
                else -> null
            }

    val x: Float?
        get() =
            when (val op = operation) {
                is DrawTextAnchored ->
                    if (Utils.isVariable(op.xRawReflection)) {
                        remoteContext.getFloat(Utils.idFromNan(op.xRawReflection))
                    } else if (op.xRawReflection.isNaN()) {
                        0f
                    } else {
                        op.xRawReflection
                    }
                else -> null
            }

    val y: Float?
        get() =
            when (val op = operation) {
                is DrawTextAnchored ->
                    if (Utils.isVariable(op.yRawReflection)) {
                        remoteContext.getFloat(Utils.idFromNan(op.yRawReflection))
                    } else if (op.yRawReflection.isNaN()) {
                        0f
                    } else {
                        op.yRawReflection
                    }
                else -> null
            }

    val panX: Float?
        get() =
            when (val op = operation) {
                is DrawTextAnchored ->
                    if (Utils.isVariable(op.panXRawReflection)) {
                        remoteContext.getFloat(Utils.idFromNan(op.panXRawReflection))
                    } else if (op.panXRawReflection.isNaN()) {
                        0f
                    } else {
                        op.panXRawReflection
                    }
                else -> null
            }

    val panY: Float?
        get() =
            when (val op = operation) {
                is DrawTextAnchored ->
                    if (Utils.isVariable(op.panYRawReflection)) {
                        remoteContext.getFloat(Utils.idFromNan(op.panYRawReflection))
                    } else {
                        op.panYRawReflection
                    }
                else -> null
            }

    override val inspectableElements: Sequence<ValueElement>
        get() = sequence {
            yield(ValueElement("kind", kind))
            yield(ValueElement("command", commandName))
            when (val op = operation) {
                is DrawBase2 -> {
                    val d = op.readDataReflection()
                    yield(
                        ValueElement(
                            "v1",
                            RcPlayerInspector.resolveFloatOrRaw(d.value1, remoteContext),
                        )
                    )
                    yield(
                        ValueElement(
                            "v2",
                            RcPlayerInspector.resolveFloatOrRaw(d.value2, remoteContext),
                        )
                    )
                }
                is DrawBase3 -> {
                    val d = op.readDataReflection()
                    yield(
                        ValueElement(
                            "v1",
                            RcPlayerInspector.resolveFloatOrRaw(d.value1, remoteContext),
                        )
                    )
                    yield(
                        ValueElement(
                            "v2",
                            RcPlayerInspector.resolveFloatOrRaw(d.value2, remoteContext),
                        )
                    )
                    yield(
                        ValueElement(
                            "v3",
                            RcPlayerInspector.resolveFloatOrRaw(d.value3, remoteContext),
                        )
                    )
                }
                is DrawBase4 -> {
                    val d = op.readDataReflection()
                    yield(
                        ValueElement(
                            "x1",
                            RcPlayerInspector.resolveFloatOrRaw(d.x1Value, remoteContext),
                        )
                    )
                    yield(
                        ValueElement(
                            "y1",
                            RcPlayerInspector.resolveFloatOrRaw(d.y1Value, remoteContext),
                        )
                    )
                    yield(
                        ValueElement(
                            "x2",
                            RcPlayerInspector.resolveFloatOrRaw(d.x2Value, remoteContext),
                        )
                    )
                    yield(
                        ValueElement(
                            "y2",
                            RcPlayerInspector.resolveFloatOrRaw(d.y2Value, remoteContext),
                        )
                    )
                }
                is DrawBase6 -> {
                    val d = op.readDataReflection()
                    yield(
                        ValueElement(
                            "v1",
                            RcPlayerInspector.resolveFloatOrRaw(d.value1, remoteContext),
                        )
                    )
                    yield(
                        ValueElement(
                            "v2",
                            RcPlayerInspector.resolveFloatOrRaw(d.value2, remoteContext),
                        )
                    )
                    yield(
                        ValueElement(
                            "v3",
                            RcPlayerInspector.resolveFloatOrRaw(d.value3, remoteContext),
                        )
                    )
                    yield(
                        ValueElement(
                            "v4",
                            RcPlayerInspector.resolveFloatOrRaw(d.value4, remoteContext),
                        )
                    )
                    yield(
                        ValueElement(
                            "v5",
                            RcPlayerInspector.resolveFloatOrRaw(d.value5, remoteContext),
                        )
                    )
                    yield(
                        ValueElement(
                            "v6",
                            RcPlayerInspector.resolveFloatOrRaw(d.value6, remoteContext),
                        )
                    )
                }
                is DrawPath -> {
                    val d = op.readDataReflection()
                    yield(ValueElement("pathId", d.id))
                    yield(ValueElement("start", d.start))
                    yield(ValueElement("end", d.end))
                }
                is DrawTweenPath -> {
                    val d = op.readData()
                    yield(ValueElement("path1Id", d.path1Id))
                    yield(ValueElement("path2Id", d.path2Id))
                    yield(ValueElement("tween", d.tween))
                }
                is DrawBitmap -> {
                    val d = op.readDataReflection()
                    yield(ValueElement("imageId", d.id))
                    yield(
                        ValueElement(
                            "left",
                            RcPlayerInspector.resolveFloatOrRaw(d.left, remoteContext),
                        )
                    )
                    yield(
                        ValueElement(
                            "top",
                            RcPlayerInspector.resolveFloatOrRaw(d.top, remoteContext),
                        )
                    )
                    yield(
                        ValueElement(
                            "right",
                            RcPlayerInspector.resolveFloatOrRaw(d.right, remoteContext),
                        )
                    )
                    yield(
                        ValueElement(
                            "bottom",
                            RcPlayerInspector.resolveFloatOrRaw(d.bottom, remoteContext),
                        )
                    )
                }
                is DrawBitmapScaled -> {
                    val d = op.readDataReflection()
                    yield(ValueElement("imageId", d.imageId))
                    yield(ValueElement("scaleType", d.scaleType))
                }
                is DrawTextAnchored -> {
                    val d = op.readData()
                    yield(ValueElement("textId", d.textId))
                    yield(ValueElement("text", text))
                    yield(ValueElement("x", x))
                    yield(ValueElement("y", y))
                    yield(ValueElement("panX", panX))
                    yield(ValueElement("panY", panY))
                    yield(ValueElement("flags", d.flags))
                }
                is DrawTextOnPath -> {
                    val d = op.readData()
                    yield(ValueElement("textId", op.mTextId))
                    yield(ValueElement("text", text))
                    yield(ValueElement("pathId", d.pathId))
                    yield(
                        ValueElement(
                            "hOffset",
                            RcPlayerInspector.resolveFloatOrRaw(
                                op.hOffsetRawReflection,
                                remoteContext,
                            ),
                        )
                    )
                    yield(
                        ValueElement(
                            "vOffset",
                            RcPlayerInspector.resolveFloatOrRaw(
                                op.vOffsetRawReflection,
                                remoteContext,
                            ),
                        )
                    )
                }
                is ClipPath -> {
                    val d = op.readData()
                    yield(ValueElement("pathId", d.id))
                    yield(ValueElement("regionOp", d.regionOp))
                }
            }
        }
}

internal data class RcOtherOperationInspectable(
    override val operation: Operation,
    override val remoteContext: RemoteContext,
) : RcOperationInspectable() {
    override val inspectableElements: Sequence<ValueElement>
        get() = sequence {
            yield(ValueElement("kind", kind))
            yield(ValueElement("operation", operation))
        }
}

// ============================================================================================
// 4. Root and Content Coordinate Inspector Elements
// ============================================================================================

private data class RcPlayerRootInspectorElement(val state: RcPlayerState) :
    ModifierNodeElement<RcPlayerRootInspectorNode>() {
    override fun create(): RcPlayerRootInspectorNode = RcPlayerRootInspectorNode(state)

    override fun update(node: RcPlayerRootInspectorNode) {
        node.state = state
    }

    override fun InspectorInfo.inspectableProperties() {
        name = RcPlayerInspector.INSPECTOR_ROOT_NAME
        value = state
        properties["state"] = state
        properties["document"] = state.document
        properties["componentId"] = state.document.rootLayoutComponent?.getId()
    }
}

private class RcPlayerRootInspectorNode(var state: RcPlayerState) :
    Modifier.Node(), SemanticsModifierNode {
    override fun SemanticsPropertyReceiver.applySemantics() {
        this[RcPlayerInspector.PlayerStateKey] = state
        state.document.rootLayoutComponent?.let {
            this[RcPlayerInspector.ComponentIdKey] = it.getId()
        }
    }
}

private data class RcComponentContentInspectorElement(val component: Component) :
    ModifierNodeElement<RcComponentContentInspectorNode>() {
    override fun create(): RcComponentContentInspectorNode =
        RcComponentContentInspectorNode(component.getId())

    override fun update(node: RcComponentContentInspectorNode) {
        node.componentId = component.getId()
    }

    override fun InspectorInfo.inspectableProperties() {
        name = RcPlayerInspector.INSPECTOR_CONTENT_NAME
        value = component.getId()
        properties["component"] = component
        properties["componentId"] = component.getId()
    }
}

private class RcComponentContentInspectorNode(var componentId: Int) :
    Modifier.Node(), SemanticsModifierNode {
    override fun SemanticsPropertyReceiver.applySemantics() {
        this[RcPlayerInspector.ContentComponentIdKey] = componentId
    }
}
