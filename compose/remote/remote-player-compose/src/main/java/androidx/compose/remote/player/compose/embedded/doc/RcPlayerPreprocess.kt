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

@file:Suppress("RestrictedApiAndroidX", "PrimitiveInCollection", "VisibleForTests")

package androidx.compose.remote.player.compose.embedded.doc

import android.util.Log
import androidx.collection.IntObjectMap
import androidx.collection.IntSet
import androidx.collection.emptyIntSet
import androidx.collection.mutableIntObjectMapOf
import androidx.collection.mutableIntSetOf
import androidx.compose.animation.core.Easing as ComposeEasing
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.Operation
import androidx.compose.remote.core.RemoteComposeState
import androidx.compose.remote.core.RemoteContext
import androidx.compose.remote.core.VariableProvider
import androidx.compose.remote.core.VariableSupport
import androidx.compose.remote.core.operations.BitmapTextMeasure
import androidx.compose.remote.core.operations.ColorConstant
import androidx.compose.remote.core.operations.ColorTheme
import androidx.compose.remote.core.operations.ComponentValue
import androidx.compose.remote.core.operations.ConditionalOperations
import androidx.compose.remote.core.operations.FloatConstant
import androidx.compose.remote.core.operations.FloatExpression
import androidx.compose.remote.core.operations.FloatFunctionDefine
import androidx.compose.remote.core.operations.Header
import androidx.compose.remote.core.operations.IdLookup
import androidx.compose.remote.core.operations.ImageAttribute
import androidx.compose.remote.core.operations.IntegerExpression
import androidx.compose.remote.core.operations.NamedVariable
import androidx.compose.remote.core.operations.ParticlesCompare
import androidx.compose.remote.core.operations.ParticlesLoop
import androidx.compose.remote.core.operations.PathCombine
import androidx.compose.remote.core.operations.PathCreate
import androidx.compose.remote.core.operations.PathData
import androidx.compose.remote.core.operations.PathExpression
import androidx.compose.remote.core.operations.PathTween
import androidx.compose.remote.core.operations.TextAttribute
import androidx.compose.remote.core.operations.TextFromFloat
import androidx.compose.remote.core.operations.TextLookup
import androidx.compose.remote.core.operations.TextLookupInt
import androidx.compose.remote.core.operations.TextMeasure
import androidx.compose.remote.core.operations.TextSubtext
import androidx.compose.remote.core.operations.TextTransform
import androidx.compose.remote.core.operations.TimeAttribute
import androidx.compose.remote.core.operations.TouchExpression
import androidx.compose.remote.core.operations.WakeIn
import androidx.compose.remote.core.operations.layout.CanvasContent
import androidx.compose.remote.core.operations.layout.Component
import androidx.compose.remote.core.operations.layout.Container
import androidx.compose.remote.core.operations.layout.ImpulseOperation
import androidx.compose.remote.core.operations.layout.LayoutComponent
import androidx.compose.remote.core.operations.layout.LayoutComponentContent
import androidx.compose.remote.core.operations.layout.LoopOperation
import androidx.compose.remote.core.operations.layout.managers.StateLayout
import androidx.compose.remote.core.operations.layout.modifiers.ComponentModifiers
import androidx.compose.remote.core.operations.layout.modifiers.ComponentVisibilityOperation
import androidx.compose.remote.core.operations.layout.modifiers.ScrollModifierOperation
import androidx.compose.remote.core.operations.matrix.MatrixConstant
import androidx.compose.remote.core.operations.matrix.MatrixExpression
import androidx.compose.remote.core.operations.utilities.easing.Easing as RemoteEasing
import androidx.compose.remote.core.types.BooleanConstant
import androidx.compose.remote.core.types.IntegerConstant
import androidx.compose.remote.core.types.LongConstant
import androidx.compose.remote.player.compose.embedded.components.rootTouchExpressions
import androidx.compose.remote.player.compose.embedded.state.MaybeVariableFloat
import androidx.compose.remote.player.compose.embedded.state.anyScalarReference
import androidx.compose.remote.player.compose.embedded.state.forEachScalarReference
import androidx.compose.remote.player.core.platform.AndroidRemoteContext
import androidx.compose.ui.util.fastForEach
import androidx.tracing.trace

internal class DocumentPreprocessResult(
    val globalOps: ArrayList<Operation>,
    val constantOps: ArrayList<Operation>,
    val touchExpressions: ArrayList<TouchExpression>,
    val rootTouchExpressions: ArrayList<TouchExpression>,
    val componentTouchExpressionsMap: Map<Int, List<TouchExpression>>,
    val touchExpressionIds: IntSet,
    val computedOpIndex: IntObjectMap<Operation>,
    val componentValueMap: Map<Int, List<ComponentValue>>,
    val hasParticles: Boolean,
    val hasWakeIn: Boolean,
    val hasContinuousTime: Boolean,
    val hasDiscreteTime: Boolean,
    /**
     * Bitmask of the discrete time variables (indexed as in [DISCRETE_TIME_VARIABLES]) the document
     * refers to. All bits are set when the document reads the clock in a way the scan can't
     * attribute to specific fields.
     */
    val discreteTimeMask: Int = 0,
    /**
     * Whether an operation other than a [TouchExpression] reads `ID_TOUCH_POS_X`/`Y` through an
     * expression, so the root pointer handler has to publish the touch position. See also
     * [listensToTouchPosition], which covers operations that bind the position directly.
     */
    val readsTouchPosition: Boolean = false,
    /**
     * Float variables a draw stream writes in order, more than once per pass: variables assigned
     * more than once, assigned inside a loop, conditional or float function, function parameters,
     * loop indexes, and float expressions that read any of these. They are left out of
     * [computedOpIndex], so a read returns the value the stream wrote last instead of re-deriving
     * it from inputs that have since moved on.
     */
    val streamFloatIds: IntSet = emptyIntSet(),
)

/**
 * Index of computed-value operations by the id they produce — `VariableSupport`+`VariableProvider`
 * ops that compute from other variables. Everything, including
 * `FloatExpression`/`IntegerExpression`, is included so the graph can resolve them when a derived
 * op reads them as an input (chains).
 */
internal fun buildComputedOpIndex(operations: Collection<Operation>): IntObjectMap<Operation> {
    val map = mutableIntObjectMapOf<Operation>()
    val touchIds = mutableIntSetOf()
    fun walk(ops: Collection<Operation>) {
        for (op in ops) {
            if (op is TouchExpression) {
                val id = op.id
                if (id > 0) {
                    touchIds.add(id)
                    map.remove(id)
                }
            } else if (
                op is VariableSupport &&
                    op is VariableProvider &&
                    op !is MatrixExpression &&
                    op !is MatrixConstant
            ) {
                val id = op.id
                if (id > 0 && !touchIds.contains(id) && !map.containsKey(id)) {
                    map[id] = op
                }
            }
            if (op is Container) walk(op.getList())
            if (op is LayoutComponent) {
                val canvasOps = op.getCanvasOperations()
                if (canvasOps != null) {
                    walk(listOf(canvasOps))
                }
            }
        }
    }
    walk(operations)
    return map
}

internal fun preprocessDocument(document: CoreDocument): DocumentPreprocessResult =
    trace("RcPlayer:preprocessDocument") { preprocessDocumentImpl(document) }

private fun preprocessDocumentImpl(document: CoreDocument): DocumentPreprocessResult {
    val rootComponent = document.rootLayoutComponent
    val ops = document.operations
    val globalOps = ArrayList<Operation>()
    if (rootComponent != null) {
        for (i in 0 until ops.size) {
            val op = ops[i]
            if (op === rootComponent) break
            globalOps.add(op)
        }
    } else {
        globalOps.addAll(ops)
    }

    val constantOps = ArrayList<Operation>()
    val touchExpressions = ArrayList<TouchExpression>()
    val rootTouchExpressions = ArrayList<TouchExpression>()
    val componentTouchExpressionsMap = HashMap<Int, MutableList<TouchExpression>>()
    val computedOpIndex = mutableIntObjectMapOf<Operation>()
    val touchExpressionIds = mutableIntSetOf()
    val rawComponentValues = ArrayList<ComponentValue>()
    val componentsById = mutableIntObjectMapOf<Component>()
    var hasParticles = false
    var hasWakeIn = false
    var hasContinuousTime = false
    var hasDiscreteTime = false
    var discreteTimeMask = 0
    var readsTouchPosition = false
    var currentLayoutComponent: LayoutComponent? = null
    var inScrollModifier = false
    var inLoop = false
    var inBranch = false
    // Float ids written imperatively by a draw stream; see DocumentPreprocessResult.streamFloatIds.
    val streamFloatIds = mutableIntSetOf()
    // The first expression assigned to each float id; the same operation can be visited twice.
    val firstAssignments = mutableIntObjectMapOf<Operation>()

    fun collectFunctionWrites(ops: List<Operation>) {
        ops.fastForEach { op ->
            // Register names declared in the body at setup, so a host can set them before the
            // function first runs.
            if (op is NamedVariable) constantOps.add(op)
            if (op is VariableProvider) streamFloatIds.add(op.id)
            if (op is LoopOperation) streamFloatIds.add(op.indexVariableId)
            if (op is Container) collectFunctionWrites(op.list)
        }
    }

    fun visitOp(op: Operation) {
        if (op is FloatFunctionDefine) {
            // Function bodies only run when called, writing their parameters and every variable
            // they assign once per call.
            op.args.forEach { streamFloatIds.add(it) }
            collectFunctionWrites(op.list)
            return
        }
        val prevLayoutComponent = currentLayoutComponent
        val prevInScroll = inScrollModifier
        val prevInLoop = inLoop
        val prevInBranch = inBranch
        if (op is LayoutComponent) {
            currentLayoutComponent = op
        }
        if (op is ScrollModifierOperation) {
            inScrollModifier = true
        }
        if (op is LoopOperation) {
            inLoop = true
            streamFloatIds.add(op.indexVariableId)
        }
        if (op is ConditionalOperations) {
            inBranch = true
        }
        try {
            val definedId =
                when (op) {
                    is NamedVariable -> op.mVarId
                    is VariableProvider -> op.id
                    is TextMeasure -> op.mId
                    is ImageAttribute -> op.mId
                    is TextAttribute -> op.mId
                    is TimeAttribute -> op.mId
                    is BitmapTextMeasure -> op.mId
                    else -> -1
                }
            // Warn when a document operation defines an ID that collides with a reserved system
            // variable.
            if (definedId > 0 && isTimeVariable(definedId)) {
                Log.w(
                    "RcPlayer",
                    "Operation ${op.javaClass.simpleName} defines reserved system variable ID $definedId",
                )
            }

            // Collect direct references to continuous or discrete time variables from expressions
            // and variable-reading operations so we know which clock loop (if any) needs to run.
            if (op is StateLayout) {
                // StateLayout does not implement VariableSupport, so inspect its indexId directly.
                val id = op.indexId
                if (isContinuousTimeVariable(id)) {
                    hasContinuousTime = true
                } else if (isDiscreteTimeVariable(id)) {
                    hasDiscreteTime = true
                    discreteTimeMask = discreteTimeMask or (1 shl discreteTimeIndex(id))
                }
            } else {
                forEachListenedVariableId(op) { id ->
                    if (isTouchPositionVariable(id)) {
                        readsTouchPosition = true
                    } else if (isContinuousTimeVariable(id)) {
                        hasContinuousTime = true
                    } else if (isDiscreteTimeVariable(id)) {
                        hasDiscreteTime = true
                        discreteTimeMask = discreteTimeMask or (1 shl discreteTimeIndex(id))
                    }
                }
            }

            if (op is TimeAttribute) {
                val type = op.mType.toInt() and 255
                if (
                    type == TimeAttribute.TIME_FROM_NOW_SEC.toInt() ||
                        type == TimeAttribute.TIME_FROM_NOW_MIN.toInt() ||
                        type == TimeAttribute.TIME_FROM_NOW_HR.toInt() ||
                        type == TimeAttribute.TIME_FROM_LOAD_SEC.toInt()
                ) {
                    hasContinuousTime = true
                } else {
                    hasDiscreteTime = true
                    discreteTimeMask = ALL_DISCRETE_TIME_MASK
                }
            }

            if (
                op is NamedVariable ||
                    (!inLoop &&
                        (op is ColorConstant ||
                            op is FloatConstant ||
                            op is ColorTheme ||
                            op is IntegerConstant ||
                            op is LongConstant ||
                            op is BooleanConstant ||
                            op is MatrixConstant))
            ) {
                constantOps.add(op)
            }

            if (op is ParticlesLoop || op is ParticlesCompare) {
                hasParticles = true
            }

            if (op is WakeIn) {
                hasWakeIn = true
            }

            if (op is ImpulseOperation) {
                // Core's ImpulseOperation.paint schedules a wakeIn until its window opens, and its
                // process runs every frame inside the window, so the document needs the frame
                // loop. The impulse draw reads the animation time only before and inside its
                // window, so unlike WakeIn it lets the loop idle once the window has closed.
                hasContinuousTime = true
                // registerListening may already have moved the trailing ImpulseProcess out of the
                // impulse's list; visit it here so its contents are still scanned.
                val process = op.process
                if (process != null && !op.list.contains(process)) {
                    visitOp(process)
                }
            }

            if (op is TouchExpression) {
                val id = op.id
                if (id > 0) {
                    touchExpressionIds.add(id)
                    computedOpIndex.remove(id)
                }
                if (!inScrollModifier) {
                    touchExpressions.add(op)
                    val enclosingComponent = currentLayoutComponent
                    if (enclosingComponent != null) {
                        // Bind the enclosing LayoutComponent: core only binds TouchExpressions
                        // that are direct ComponentData children of a component, not those
                        // nested in CanvasOperations / DrawContent. The component supplies the
                        // hit-test bounds (TouchExpression.updateBounds) and scopes dispatch to
                        // this component's pointerInput. TouchExpressions outside any
                        // LayoutComponent (bound by core to the RootLayoutComponent, if any) are
                        // dispatched by the root pointer handler via rootTouchExpressions; the
                        // player does not read CoreDocument.mTouchListeners.
                        op.setComponent(enclosingComponent)
                        val list =
                            componentTouchExpressionsMap.getOrPut(enclosingComponent.componentId) {
                                ArrayList()
                            }
                        if (!list.contains(op)) {
                            list.add(op)
                        }
                    } else {
                        if (!rootTouchExpressions.contains(op)) {
                            rootTouchExpressions.add(op)
                        }
                    }
                }
            } else if (
                op is VariableSupport &&
                    op is VariableProvider &&
                    op !is PathData &&
                    op !is PathCreate &&
                    op !is PathCombine &&
                    op !is PathTween &&
                    op !is PathExpression &&
                    op !is MatrixExpression &&
                    op !is MatrixConstant
            ) {
                val id = op.id
                if (op is FloatExpression) {
                    val first = firstAssignments.getOrPut(id) { op }
                    if (first !== op || inLoop || inBranch) streamFloatIds.add(id)
                }
                if (
                    id > 0 && !touchExpressionIds.contains(id) && !computedOpIndex.containsKey(id)
                ) {
                    computedOpIndex[id] = op
                }
            } else if (op is TextMeasure) {
                val id = op.mId
                if (id > 0 && !computedOpIndex.containsKey(id)) {
                    computedOpIndex[id] = op
                }
            } else if (op is ImageAttribute) {
                val id = op.mId
                if (id > 0 && !computedOpIndex.containsKey(id)) {
                    computedOpIndex[id] = op
                }
            } else {
                // Value-producing operations that aren't VariableProviders: their output is
                // computed on demand through the GraphContext, like TextMeasure, so it follows
                // inputs that change after setup.
                val id =
                    when (op) {
                        is TextAttribute -> op.mId
                        is TimeAttribute -> op.mId
                        is BitmapTextMeasure -> op.mId
                        is TextLookup -> op.mTextId
                        is IdLookup -> op.mTextId
                        is TextSubtext -> op.textId
                        is TextTransform -> op.textId
                        else -> -1
                    }
                if (id > 0 && !computedOpIndex.containsKey(id)) {
                    computedOpIndex[id] = op
                }
            }

            if (op is ComponentValue) {
                rawComponentValues.add(op)
            }

            if (op is Component) {
                if (op.componentId !in componentsById) {
                    componentsById[op.componentId] = op
                }
            }

            if (op is LayoutComponent) {
                val content = op.getLayoutContent()
                if (content != null) {
                    if (content.componentId !in componentsById) {
                        componentsById[content.componentId] = content
                    }
                    content.list.fastForEach { childOp ->
                        if (childOp is NamedVariable) {
                            visitOp(childOp)
                        }
                    }
                }
                op.componentModifiers?.list?.fastForEach { visitOp(it) }
                val canvasOps = op.getCanvasOperations()
                if (canvasOps != null) {
                    canvasOps.list.fastForEach { childOp ->
                        if (childOp is ComponentValue && childOp.componentId == -1) {
                            childOp.componentId = op.componentId
                        }
                    }
                    visitOp(canvasOps)
                }
            }

            if (op is Container) {
                val list = op.getList()
                for (i in 0 until list.size) {
                    val child = list[i]
                    if (op is LayoutComponent && child is ComponentModifiers) continue
                    visitOp(child)
                }
            }
        } finally {
            currentLayoutComponent = prevLayoutComponent
            inScrollModifier = prevInScroll
            inLoop = prevInLoop
            inBranch = prevInBranch
        }
    }

    for (i in 0 until ops.size) {
        visitOp(ops[i])
    }

    // An expression reading a stream-written variable is only correct where the stream evaluates
    // it, so it is stream-written too.
    var changed = true
    while (changed) {
        changed = false
        computedOpIndex.forEach { id, op ->
            if (
                op is FloatExpression &&
                    !streamFloatIds.contains(id) &&
                    op.mSrcValue.anyScalarReference { streamFloatIds.contains(it) }
            ) {
                streamFloatIds.add(id)
                changed = true
            }
        }
    }
    streamFloatIds.forEach { computedOpIndex.remove(it) }

    val componentValueMap = HashMap<Int, MutableList<ComponentValue>>()
    for (i in 0 until rawComponentValues.size) {
        val op = rawComponentValues[i]
        var targetId = op.componentId
        val targetComponent = componentsById[targetId]
        if (targetComponent is LayoutComponentContent || targetComponent is CanvasContent) {
            val parent = targetComponent.parent
            parent?.let { targetId = it.id }
        }
        componentValueMap.getOrPut(targetId) { ArrayList() }.add(op)
    }

    return DocumentPreprocessResult(
        globalOps = globalOps,
        constantOps = constantOps,
        touchExpressions = touchExpressions,
        rootTouchExpressions = rootTouchExpressions,
        componentTouchExpressionsMap = componentTouchExpressionsMap,
        touchExpressionIds = touchExpressionIds,
        computedOpIndex = computedOpIndex,
        componentValueMap = componentValueMap,
        hasParticles = hasParticles,
        hasWakeIn = hasWakeIn,
        hasContinuousTime = hasContinuousTime,
        hasDiscreteTime = hasDiscreteTime,
        discreteTimeMask = discreteTimeMask,
        readsTouchPosition = readsTouchPosition,
        streamFloatIds = streamFloatIds,
    )
}

private fun isTouchPositionVariable(id: Int): Boolean =
    id == RemoteContext.ID_TOUCH_POS_X || id == RemoteContext.ID_TOUCH_POS_Y

/**
 * Whether an operation other than a [TouchExpression] registered a listener for `ID_TOUCH_POS_X` or
 * `ID_TOUCH_POS_Y` while the document was set up, i.e. reads the touch position directly (such as a
 * draw positioned at the touch point). TouchExpressions read the position the touch handlers
 * publish right before evaluating them, so they don't need it published otherwise.
 */
internal fun RemoteComposeState.listensToTouchPosition(): Boolean =
    hasNonTouchExpressionListener(RemoteContext.ID_TOUCH_POS_X) ||
        hasNonTouchExpressionListener(RemoteContext.ID_TOUCH_POS_Y)

private fun RemoteComposeState.hasNonTouchExpressionListener(id: Int): Boolean {
    val listeners = getListeners(id) ?: return false
    for (i in 0 until listeners.size) {
        if (listeners[i] !is TouchExpression) return true
    }
    return false
}

internal fun isExpressionContinuousTimeDependent(expr: FloatExpression): Boolean {
    return expr.mSrcValue.anyScalarReference(::isContinuousTimeVariable)
}

internal fun isExpressionDiscreteTimeDependent(expr: FloatExpression): Boolean {
    return expr.mSrcValue.anyScalarReference(::isDiscreteTimeVariable)
}

internal fun isExpressionTimeDependent(expr: FloatExpression): Boolean =
    isExpressionContinuousTimeDependent(expr) || isExpressionDiscreteTimeDependent(expr)

/**
 * Invokes [action] for each variable ID that [op] would pass to [RemoteContext.listensTo] from its
 * `registerListening` implementation.
 *
 * Only [FloatExpression], [IntegerExpression], [TextFromFloat], [TextLookupInt] and
 * [ComponentVisibilityOperation] are decoded; other operations are ignored. This mirrors core's
 * `registerListening` without allocating a throwaway [RemoteContext] (and its [CoreDocument] and
 * state stores) during document preprocessing. Keep in sync with core; parity is verified by
 * `RcPlayerExpressionTest`.
 *
 * Operations that bind a time variable directly (modifiers, draws, matrix ops, tweens, loops,
 * conditionals, ...) are picked up after setup instead, from the listeners core registered (see
 * [listensToContinuousTime] and [listensToDiscreteTime]).
 */
internal inline fun forEachListenedVariableId(op: Operation, action: (Int) -> Unit) {
    when (op) {
        is FloatExpression -> op.mSrcValue.forEachScalarReference(action)
        is IntegerExpression -> {
            val mask = op.mask
            val srcValues = op.mSrcValue
            for (i in srcValues.indices) {
                if (IntegerExpression.isId(mask, i, srcValues[i])) {
                    action(srcValues[i])
                }
            }
        }
        is TextFromFloat -> {
            val value = MaybeVariableFloat(op.mValue)
            if (!value.isLiteral) action(value.id)
        }
        is TextLookupInt -> {
            action(op.mIndex)
            action(op.mDataSetId)
        }
        is ComponentVisibilityOperation -> action(op.getVisibilityId())
    }
}

/**
 * Whether any operation registered a listener for a continuous time variable
 * ([CONTINUOUS_TIME_VARIABLES]) while the document was set up.
 *
 * Core's `CoreDocument.registerVariables` calls `registerListening` on every `VariableSupport`
 * operation, including component modifiers and canvas operations, and core's View player picks its
 * repaint cadence from the resulting listeners. This catches operations that read a time variable
 * directly, without a wrapping expression, which [preprocessDocument] does not decode.
 */
internal fun RemoteComposeState.listensToContinuousTime(): Boolean = CONTINUOUS_TIME_VARIABLES.any {
    hasListener(it)
}

/**
 * Whether any operation registered a listener for a discrete wall-clock or calendar variable
 * ([DISCRETE_TIME_VARIABLES]) while the document was set up. See [listensToContinuousTime].
 */
internal fun RemoteComposeState.listensToDiscreteTime(): Boolean = DISCRETE_TIME_VARIABLES.any {
    hasListener(it)
}

/**
 * Bitmask (indexed as in [DISCRETE_TIME_VARIABLES]) of the discrete time variables that some
 * operation registered a listener for while the document was set up. See [listensToDiscreteTime].
 */
internal fun RemoteComposeState.discreteTimeListenerMask(): Int {
    var mask = 0
    DISCRETE_TIME_VARIABLES.forEachIndexed { index, id ->
        if (hasListener(id)) mask = mask or (1 shl index)
    }
    return mask
}

internal fun mapEasing(type: Int): ComposeEasing {
    return when (type) {
        RemoteEasing.CUBIC_LINEAR -> LinearEasing
        RemoteEasing.CUBIC_STANDARD -> FastOutSlowInEasing
        RemoteEasing.CUBIC_ACCELERATE -> FastOutLinearInEasing
        RemoteEasing.CUBIC_DECELERATE -> LinearOutSlowInEasing
        else -> LinearEasing
    }
}

internal fun initializePlayerRemoteContext(
    document: CoreDocument,
    preprocessed: DocumentPreprocessResult,
): AndroidRemoteContext =
    trace("RcPlayer:initializeRemoteContext") {
        initializePlayerRemoteContextImpl(document, preprocessed)
    }

private fun initializePlayerRemoteContextImpl(
    document: CoreDocument,
    preprocessed: DocumentPreprocessResult,
): AndroidRemoteContext {
    val ctx = AndroidRemoteContext(document.clock)
    ctx.useChoreographer = true
    val touchVersion = document.featureIntValue(Header.FEATURE_TOUCH_VERSION)
    if (touchVersion != -1) {
        ctx.touchVersion = touchVersion
    }
    if (document.remoteComposeState !is SnapshotRemoteComposeState) {
        document.setRemoteComposeState(SnapshotRemoteComposeState())
    }
    (document.remoteComposeState as SnapshotRemoteComposeState).streamFloatIds =
        preprocessed.streamFloatIds
    ctx.withOpCountReset {
        document.initializeContext(ctx, null)
        document.recollectCollections()
        document.applyDataOperationsWithoutBitmaps(ctx)
        document.setLayoutCallback {}
        applyOperationsWithoutBitmaps(ctx, preprocessed.globalOps)
        applyOperationsWithoutBitmaps(ctx, preprocessed.constantOps)
        val dataOps = ArrayList<Operation>()
        document.rootLayoutComponent?.getData(dataOps, true)
        applyOperationsWithoutBitmaps(ctx, dataOps)
        preprocessed.touchExpressions.fastForEach { te ->
            te.updateVariables(ctx)
            te.apply(ctx)
        }
    }
    return ctx
}
