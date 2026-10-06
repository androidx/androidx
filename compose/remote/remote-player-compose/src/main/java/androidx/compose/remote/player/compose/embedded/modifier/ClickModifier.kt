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

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.Operation
import androidx.compose.remote.core.RemoteContext
import androidx.compose.remote.core.operations.Theme
import androidx.compose.remote.core.operations.Utils
import androidx.compose.remote.core.operations.layout.ClickModifierOperation
import androidx.compose.remote.core.operations.layout.MultiClickModifier
import androidx.compose.remote.core.operations.layout.TouchCancelModifierOperation
import androidx.compose.remote.core.operations.layout.TouchDownModifierOperation
import androidx.compose.remote.core.operations.layout.TouchUpModifierOperation
import androidx.compose.remote.core.operations.layout.modifiers.HostActionOperation
import androidx.compose.remote.core.operations.layout.modifiers.HostNamedActionOperation
import androidx.compose.remote.core.operations.layout.modifiers.RunActionOperation
import androidx.compose.remote.core.operations.layout.modifiers.ValueFloatChangeActionOperation
import androidx.compose.remote.core.operations.layout.modifiers.ValueFloatExpressionChangeActionOperation
import androidx.compose.remote.core.operations.layout.modifiers.ValueIntegerChangeActionOperation
import androidx.compose.remote.core.operations.layout.modifiers.ValueIntegerExpressionChangeActionOperation
import androidx.compose.remote.core.operations.layout.modifiers.ValueStringChangeActionOperation
import androidx.compose.remote.player.compose.embedded.LocalCoreDocument
import androidx.compose.remote.player.compose.embedded.LocalRemoteActionHandler
import androidx.compose.remote.player.compose.embedded.LocalRemoteContext
import androidx.compose.remote.player.compose.embedded.LocalRemoteNamedActionHandler
import androidx.compose.remote.player.compose.embedded.clickTypeReflection
import androidx.compose.remote.player.compose.embedded.getOperationsReflection
import androidx.compose.remote.player.compose.embedded.readData
import androidx.compose.remote.player.compose.embedded.state.rememberRemoteStringAsState
import androidx.compose.remote.player.compose.embedded.targetValueIdReflection
import androidx.compose.remote.player.compose.embedded.updateVariablesReflection
import androidx.compose.remote.player.compose.embedded.valueExpressionIdReflection
import androidx.compose.remote.player.compose.embedded.valueIdReflection
import androidx.compose.remote.player.compose.embedded.valueReflection
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.util.fastFilter
import androidx.compose.ui.util.fastFirstOrNull
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMap

/**
 * The click action handlers for a component: each is `null` if the component has no actions for
 * that gesture type, so callers can leave the gesture disabled when unused.
 */
internal class ClickActionHandlers(
    val onClick: (() -> Unit)?,
    val onDoubleClick: (() -> Unit)?,
    val onLongClick: (() -> Unit)?,
)

/**
 * Returns the single-click, double-click, and long-click handlers for [ops] (both
 * [ClickModifierOperation]s and [MultiClickModifier]s). Each handler is `null` if there are no
 * actions for that gesture type. Used by custom components whose plugin dispatches clicks from its
 * own native component.
 */
@Composable
internal fun rememberClickActionHandlers(ops: List<Operation>): ClickActionHandlers {
    val coreDocument = LocalCoreDocument.current
    val remoteContext = LocalRemoteContext.current
    val onAction = LocalRemoteActionHandler.current
    val onNamedAction = LocalRemoteNamedActionHandler.current

    val singleActions = ArrayList<Operation>()
    val doubleActions = ArrayList<Operation>()
    val longActions = ArrayList<Operation>()
    ops.fastForEach { op ->
        when (op) {
            is ClickModifierOperation -> singleActions.addAll(op.mList)
            is MultiClickModifier ->
                when (op.clickTypeReflection) {
                    MultiClickModifier.CLICK_TYPE_SINGLE -> singleActions.addAll(op.mList)
                    MultiClickModifier.CLICK_TYPE_DOUBLE -> doubleActions.addAll(op.mList)
                    MultiClickModifier.CLICK_TYPE_LONG -> longActions.addAll(op.mList)
                }
        }
    }

    fun handlerFor(actions: List<Operation>): (() -> Unit)? {
        if (actions.isEmpty()) return null
        return {
            actions.fastForEach {
                applyClickAction(it, coreDocument, remoteContext, onAction, onNamedAction)
            }
            coreDocument.updateVariablesReflection(
                remoteContext,
                Theme.SYSTEM,
                coreDocument.getOperationsReflection(),
            )
        }
    }

    return ClickActionHandlers(
        onClick = handlerFor(singleActions),
        onDoubleClick = handlerFor(doubleActions),
        onLongClick = handlerFor(longActions),
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun Modifier.combinedClick(
    clickOps: List<ClickModifierOperation>,
    multiClickOps: List<MultiClickModifier>,
): Modifier {
    val coreDocument = LocalCoreDocument.current
    val remoteContext = LocalRemoteContext.current
    val onAction = LocalRemoteActionHandler.current
    val onNamedAction = LocalRemoteNamedActionHandler.current
    val contentDescription =
        clickOps
            .fastFirstOrNull { it.contentDescriptionId != null }
            ?.contentDescriptionId
            ?.let {
                rememberRemoteStringAsState(it).value
            }

    val singleActionLists =
        ArrayList<List<Operation>>().apply {
            clickOps.fastForEach { add(it.mList) }
            multiClickOps.fastForEach {
                if (it.clickTypeReflection == MultiClickModifier.CLICK_TYPE_SINGLE) {
                    add(it.mList)
                }
            }
        }
    val doubleActionLists =
        multiClickOps
            .fastFilter { it.clickTypeReflection == MultiClickModifier.CLICK_TYPE_DOUBLE }
            .fastMap { it.mList }
    val longActionLists =
        multiClickOps
            .fastFilter { it.clickTypeReflection == MultiClickModifier.CLICK_TYPE_LONG }
            .fastMap { it.mList }

    fun dispatchActionLists(actionLists: List<List<Operation>>) {
        actionLists.fastForEach { actions ->
            actions.fastForEach { action ->
                applyClickAction(action, coreDocument, remoteContext, onAction, onNamedAction)
            }
        }
        coreDocument.updateVariablesReflection(
            remoteContext,
            Theme.SYSTEM,
            coreDocument.getOperationsReflection(),
        )
    }

    return if (doubleActionLists.isEmpty() && longActionLists.isEmpty()) {
        this.clickable(onClickLabel = contentDescription) {
            dispatchActionLists(singleActionLists)
        }
    } else {
        this.combinedClickable(
            onClickLabel = contentDescription,
            onClick = { dispatchActionLists(singleActionLists) },
            onDoubleClick =
                if (doubleActionLists.isNotEmpty()) {
                    { dispatchActionLists(doubleActionLists) }
                } else {
                    null
                },
            onLongClick =
                if (longActionLists.isNotEmpty()) {
                    { dispatchActionLists(longActionLists) }
                } else {
                    null
                },
        )
    }
}

/**
 * Dispatches the component's TouchDown, TouchUp and TouchCancel actions.
 *
 * Every TouchDown is followed by exactly one TouchUp or TouchCancel: TouchCancel is sent when a
 * child or scroller consumes the gesture, when the pointer disappears, or when the handler is
 * cancelled or restarted mid-gesture.
 *
 * @param hasClickHandler whether something else on the component handles clicks (the player's click
 *   modifier or a custom plugin that handles clicks itself). The release is then left unconsumed
 *   for that handler, and a release it consumed still counts as TouchUp.
 */
@Composable
internal fun Modifier.touchActions(
    touchDownOps: List<TouchDownModifierOperation>,
    touchUpOps: List<TouchUpModifierOperation>,
    touchCancelOps: List<TouchCancelModifierOperation>,
    hasClickHandler: Boolean = false,
): Modifier {
    val coreDocument = LocalCoreDocument.current
    val remoteContext = LocalRemoteContext.current
    val onAction = LocalRemoteActionHandler.current
    val onNamedAction = LocalRemoteNamedActionHandler.current

    // The pointer handler outlives recompositions, so read the latest document, context, host
    // handlers and actions through updated state instead of capturing them or restarting the
    // handler (which would end an in-progress gesture) whenever new lists are passed in.
    val dispatchActions by rememberUpdatedState { ops: List<List<Operation>> ->
        if (ops.isNotEmpty()) {
            ops.fastForEach { actions ->
                actions.fastForEach { action ->
                    applyClickAction(action, coreDocument, remoteContext, onAction, onNamedAction)
                }
            }
            coreDocument.updateVariablesReflection(
                remoteContext,
                Theme.SYSTEM,
                coreDocument.getOperationsReflection(),
            )
        }
    }
    val downLists by rememberUpdatedState(touchDownOps.fastMap { it.mList })
    val upLists by rememberUpdatedState(touchUpOps.fastMap { it.mList })
    val cancelLists by rememberUpdatedState(touchCancelOps.fastMap { it.mList })
    val currentHasClickHandler by rememberUpdatedState(hasClickHandler)

    return this.pointerInput(Unit) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            val pointerId = down.id
            dispatchActions(downLists)
            var ended = false
            try {
                while (!ended) {
                    val event = awaitPointerEvent(PointerEventPass.Main)
                    val change = event.changes.fastFirstOrNull { it.id == pointerId } ?: break
                    if (!change.pressed) {
                        val hasClickHandler = currentHasClickHandler
                        if (change.isConsumed && !hasClickHandler) {
                            dispatchActions(cancelLists)
                        } else {
                            if (!hasClickHandler) {
                                change.consume()
                            }
                            dispatchActions(upLists)
                        }
                        ended = true
                    } else if (change.isConsumed) {
                        dispatchActions(cancelLists)
                        ended = true
                    } else {
                        val finalEvent = awaitPointerEvent(PointerEventPass.Final)
                        val finalChange =
                            finalEvent.changes.fastFirstOrNull { it.id == pointerId } ?: break
                        if (finalChange.isConsumed) {
                            dispatchActions(cancelLists)
                            ended = true
                        }
                    }
                }
            } finally {
                if (!ended) {
                    dispatchActions(cancelLists)
                }
            }
        }
    }
}

private fun applyClickAction(
    action: Operation,
    coreDocument: CoreDocument,
    remoteContext: RemoteContext,
    onAction: (Int, String?) -> Unit,
    onNamedAction: (String, Any?) -> Unit,
) {
    when (action) {
        is ValueIntegerChangeActionOperation ->
            remoteContext.overrideInteger(action.targetValueIdReflection, action.valueReflection)
        is ValueFloatChangeActionOperation ->
            remoteContext.overrideFloat(action.targetValueIdReflection, action.valueReflection)
        is ValueStringChangeActionOperation ->
            remoteContext.overrideText(action.targetValueIdReflection, action.valueIdReflection)
        is ValueIntegerExpressionChangeActionOperation -> {
            val targetId = Utils.idFromLong(action.targetValueIdReflection).toInt()
            val expressionId = Utils.idFromLong(action.valueExpressionIdReflection)
            coreDocument.evaluateIntExpression(expressionId, targetId, remoteContext)
        }
        is ValueFloatExpressionChangeActionOperation -> {
            val targetId = action.targetValueIdReflection
            val expressionId = action.valueExpressionIdReflection
            coreDocument.evaluateFloatExpression(expressionId, targetId, remoteContext)
        }
        // Host callback (id only; HostActionOperation carries no value).
        is HostActionOperation -> onAction(action.actionId, null)
        // Named host action (what the public hostAction(name, value) authors): resolve the name
        // and the typed value, then notify the host.
        is HostNamedActionOperation -> {
            val data = action.readData()
            val name = remoteContext.getText(data.textId) ?: ""
            val valueId = data.valueId
            val value: Any? =
                if (valueId == -1) {
                    null
                } else {
                    when (data.type) {
                        HostNamedActionOperation.FLOAT_TYPE -> remoteContext.getFloat(valueId)
                        HostNamedActionOperation.INT_TYPE -> remoteContext.getInteger(valueId)
                        HostNamedActionOperation.STRING_TYPE -> remoteContext.getText(valueId)
                        else -> null
                    }
                }
            onNamedAction(name, value)
        }
        // A container of nested actions — run each.
        is RunActionOperation ->
            action.getList().fastForEach {
                applyClickAction(it, coreDocument, remoteContext, onAction, onNamedAction)
            }
    }
}
