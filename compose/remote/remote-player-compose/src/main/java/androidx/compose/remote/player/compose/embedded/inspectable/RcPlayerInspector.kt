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

import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.MatrixAccess
import androidx.compose.remote.core.Operation
import androidx.compose.remote.core.RemoteContext
import androidx.compose.remote.core.operations.ColorConstant
import androidx.compose.remote.core.operations.ColorExpression
import androidx.compose.remote.core.operations.ColorTheme
import androidx.compose.remote.core.operations.ConditionalOperations
import androidx.compose.remote.core.operations.DataListFloat
import androidx.compose.remote.core.operations.FloatConstant
import androidx.compose.remote.core.operations.FloatExpression
import androidx.compose.remote.core.operations.FloatFunctionDefine
import androidx.compose.remote.core.operations.IntegerExpression
import androidx.compose.remote.core.operations.NamedVariable
import androidx.compose.remote.core.operations.ParticlesCreate
import androidx.compose.remote.core.operations.TextFromFloat
import androidx.compose.remote.core.operations.layout.ClickModifierOperation
import androidx.compose.remote.core.operations.layout.Component
import androidx.compose.remote.core.operations.layout.Container
import androidx.compose.remote.core.operations.layout.LayoutComponent
import androidx.compose.remote.core.operations.layout.MultiClickModifier
import androidx.compose.remote.core.operations.layout.RootLayoutComponent
import androidx.compose.remote.core.operations.layout.TouchCancelModifierOperation
import androidx.compose.remote.core.operations.layout.TouchDownModifierOperation
import androidx.compose.remote.core.operations.layout.TouchUpModifierOperation
import androidx.compose.remote.core.operations.layout.animation.AnimationSpec
import androidx.compose.remote.core.operations.layout.managers.FitBoxLayout
import androidx.compose.remote.core.operations.layout.managers.StateLayout
import androidx.compose.remote.core.operations.layout.modifiers.BackgroundModifierOperation
import androidx.compose.remote.core.operations.layout.modifiers.BorderModifierOperation
import androidx.compose.remote.core.operations.layout.modifiers.ComponentVisibilityOperation
import androidx.compose.remote.core.operations.layout.modifiers.DimensionConstraintsModifierOperation
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
import androidx.compose.remote.core.semantics.CoreSemantics
import androidx.compose.remote.player.compose.embedded.LocalGraphContext
import androidx.compose.remote.player.compose.embedded.LocalRemoteContext
import androidx.compose.remote.player.compose.embedded.RcPlayer
import androidx.compose.remote.player.compose.embedded.RcPlayerState
import androidx.compose.remote.player.compose.embedded.doc.findVariableId
import androidx.compose.remote.player.compose.embedded.state.MaybeVariableFloat
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.LayoutInfo
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.SemanticsModifierNode
import androidx.compose.ui.platform.InspectableValue
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.platform.isDebugInspectorInfoEnabled
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.util.fastFirstOrNull

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
                val visId = visibilityOp.getVisibilityId()
                return remoteContext.getInteger(visId)
            }
        }
        return component.mVisibility
    }

    internal fun resolveFloatOrRaw(raw: Float, remoteContext: RemoteContext): Float =
        MaybeVariableFloat(raw).resolve(remoteContext)

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
            val varId = state.remoteContext.findVariableId(candidate)
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
                    val content = op.getLayoutContent()
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
        walk(document.operations)
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
        visit(document.operations)
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
 * Mirrors Compose UI's [debugInspectorInfo] inline gating pattern so call sites remain clean,
 * unconditional modifier chain calls.
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
