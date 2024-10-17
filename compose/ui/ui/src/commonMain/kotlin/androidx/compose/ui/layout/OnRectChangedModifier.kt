/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.compose.ui.layout

import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.requireLayoutNode
import androidx.compose.ui.node.requireOwner
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.spatial.RectInfo
import kotlinx.coroutines.DisposableHandle

/**
 * Invokes [callback] with the position of this layout node relative to the coordinate system of the
 * root of the composition, as well as in screen coordinates and window coordinates. This will be
 * called after layout pass. This API allows for throttling and debouncing parameters in order to
 * moderate the frequency with which the callback gets invoked during high rates of change (e.g.
 * scrolling).
 *
 * Specifying [throttleMs] will prevent [callback] from being executed more than once over that time
 * period. Specifying [debounceMs] will delay the execution of [callback] until that amount of time
 * has elapsed without a new position, scheduling the callback to be executed when that amount of
 * time expires.
 *
 * Specifying 0 for both [throttleMs] and [debounceMs] will result in the callback being executed
 * every time the position has changed. Specifying non-zero amounts for both will result in both
 * conditions being met. Specifying a non-zero [throttleMs] but a zero [debounceMs] is equivalent to
 * providing the same value for both [throttleMs] and [debounceMs].
 *
 * @param throttleMs The duration, in milliseconds, to prevent [callback] from being executed more
 *   than once over that time period.
 * @param debounceMs The duration, in milliseconds, to delay the execution of [callback] until that
 *   amount of time has elapsed without a new position.
 * @param callback The callback to be executed.
 * @see RectInfo
 * @see onGloballyPositioned
 * @see registerOnRectChanged
 */
@Stable
fun Modifier.onRectChanged(
    throttleMs: Int = 0,
    debounceMs: Int = 64,
    callback: (RectInfo) -> Unit
) = this then OnRectChangedElement(throttleMs, debounceMs, callback)

private data class OnRectChangedElement(
    val throttleMs: Int,
    val debounceMs: Int,
    val callback: (RectInfo) -> Unit
) : ModifierNodeElement<OnRectChangedNode>() {
    override fun create() = OnRectChangedNode(throttleMs, debounceMs, callback)

    override fun update(node: OnRectChangedNode) {
        node.throttleMs = throttleMs
        node.debounceMs = debounceMs
        node.callback = callback
        node.disposeAndRegister()
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "onRectChanged"
        properties["throttleMs"] = throttleMs
        properties["debounceMs"] = debounceMs
        properties["callback"] = callback
    }
}

private class OnRectChangedNode(
    var throttleMs: Int,
    var debounceMs: Int,
    var callback: (RectInfo) -> Unit,
) : Modifier.Node() {
    var handle: DisposableHandle? = null

    fun disposeAndRegister() {
        handle?.dispose()
        handle = registerOnRectChanged(throttleMs, debounceMs, callback)
    }

    override fun onAttach() {
        disposeAndRegister()
    }

    override fun onDetach() {
        handle?.dispose()
    }
}

/**
 * Registers a [callback] to be executed with the position of this modifier node relative to the
 * coordinate system of the root of the composition, as well as in screen coordinates and window
 * coordinates. This will be called after layout pass. This API allows for throttling and debouncing
 * parameters in order to moderate the frequency with which the callback gets invoked during high
 * rates of change (e.g. scrolling).
 *
 * Specifying [throttleMs] will prevent [callback] from being executed more than once over that time
 * period. Specifying [debounceMs] will delay the execution of [callback] until that amount of time
 * has elapsed without a new position.
 *
 * Specifying 0 for both [throttleMs] and [debounceMs] will result in the callback being executed
 * every time the position has changed. Specifying non-zero amounts for both will result in both
 * conditions being met.
 *
 * @param throttleMs The duration, in milliseconds, to prevent [callback] from being executed more
 *   than once over that time period.
 * @param debounceMs The duration, in milliseconds, to delay the execution of [callback] until that
 *   amount of time has elapsed without a new position.
 * @param callback The callback to be executed.
 * @return an object which should be used to unregister/dispose this callback
 * @see onRectChanged
 */
fun DelegatableNode.registerOnRectChanged(
    throttleMs: Int,
    debounceMs: Int,
    callback: (RectInfo) -> Unit,
): DisposableHandle {
    val layoutNode = requireLayoutNode()
    val id = layoutNode.semanticsId
    val rectManager = layoutNode.requireOwner().rectManager
    return rectManager.registerOnRectChangedCallback(
        id,
        throttleMs,
        debounceMs,
        this,
        callback,
    )
}
