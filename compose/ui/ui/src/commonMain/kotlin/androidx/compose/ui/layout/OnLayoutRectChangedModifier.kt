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
import androidx.compose.ui.node.DelegatableNode.RegistrationHandle
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.requireLayoutNode
import androidx.compose.ui.node.requireOwner
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.spatial.RelativeLayoutBounds

/**
 * Invokes [callback] with the position of this layout node relative to the coordinate system of the
 * root of the composition, as well as in screen coordinates and window coordinates. This will be
 * called after layout pass. This API allows for throttling and debouncing parameters in order to
 * moderate the frequency with which the callback gets invoked during high rates of change (e.g.
 * scrolling).
 *
 * Specifying [throttleMillis] will prevent [callback] from being executed more than once over that
 * time period. Specifying [debounceMillis] will delay the execution of [callback] until that amount
 * of time has elapsed without a new position, scheduling the callback to be executed when that
 * amount of time expires.
 *
 * Specifying 0 for both [throttleMillis] and [debounceMillis] will result in the callback being
 * executed every time the position has changed. Specifying non-zero amounts for both will result in
 * both conditions being met. Specifying a non-zero [throttleMillis] but a zero [debounceMillis] is
 * equivalent to providing the same value for both [throttleMillis] and [debounceMillis].
 *
 * @param throttleMillis The duration, in milliseconds, to prevent [callback] from being executed
 *   more than once over that time period.
 * @param debounceMillis The duration, in milliseconds, to delay the execution of [callback] until
 *   that amount of time has elapsed without a new position.
 * @param callback The callback to be executed.
 * @see RelativeLayoutBounds
 * @see onGloballyPositioned
 * @see registerOnLayoutRectChanged
 */
@Stable
fun Modifier.onLayoutRectChanged(
    throttleMillis: Long = 0,
    debounceMillis: Long = 64,
    callback: (RelativeLayoutBounds) -> Unit
) = this then OnLayoutRectChangedElement(throttleMillis, debounceMillis, callback)

private class OnLayoutRectChangedElement(
    val throttleMillis: Long,
    val debounceMillis: Long,
    val callback: (RelativeLayoutBounds) -> Unit
) : ModifierNodeElement<OnLayoutRectChangedNode>() {
    override fun create() = OnLayoutRectChangedNode(throttleMillis, debounceMillis, callback)

    override fun update(node: OnLayoutRectChangedNode) {
        node.throttleMillis = throttleMillis
        node.debounceMillis = debounceMillis
        node.callback = callback
        node.disposeAndRegister()
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "onRectChanged"
        properties["throttleMillis"] = throttleMillis
        properties["debounceMillis"] = debounceMillis
        properties["callback"] = callback
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is OnLayoutRectChangedElement) return false

        if (throttleMillis != other.throttleMillis) return false
        if (debounceMillis != other.debounceMillis) return false
        if (callback !== other.callback) return false

        return true
    }

    override fun hashCode(): Int {
        var result = throttleMillis.hashCode()
        result = 31 * result + debounceMillis.hashCode()
        result = 31 * result + callback.hashCode()
        return result
    }
}

private class OnLayoutRectChangedNode(
    var throttleMillis: Long,
    var debounceMillis: Long,
    var callback: (RelativeLayoutBounds) -> Unit,
) : Modifier.Node() {
    var handle: RegistrationHandle? = null

    fun disposeAndRegister() {
        handle?.unregister()
        handle = registerOnLayoutRectChanged(throttleMillis, debounceMillis, callback)
    }

    override fun onAttach() {
        disposeAndRegister()
    }

    override fun onDetach() {
        handle?.unregister()
    }
}

/**
 * Registers a [callback] to be executed with the position of this modifier node relative to the
 * coordinate system of the root of the composition, as well as in screen coordinates and window
 * coordinates. This will be called after layout pass. This API allows for throttling and debouncing
 * parameters in order to moderate the frequency with which the callback gets invoked during high
 * rates of change (e.g. scrolling).
 *
 * Specifying [throttleMillis] will prevent [callback] from being executed more than once over that
 * time period. Specifying [debounceMillis] will delay the execution of [callback] until that amount
 * of time has elapsed without a new position.
 *
 * Specifying 0 for both [throttleMillis] and [debounceMillis] will result in the callback being
 * executed every time the position has changed. Specifying non-zero amounts for both will result in
 * both conditions being met.
 *
 * @param throttleMillis The duration, in milliseconds, to prevent [callback] from being executed
 *   more than once over that time period.
 * @param debounceMillis The duration, in milliseconds, to delay the execution of [callback] until
 *   that amount of time has elapsed without a new position.
 * @param callback The callback to be executed.
 * @return an object which should be used to unregister/dispose this callback
 * @see onLayoutRectChanged
 */
fun DelegatableNode.registerOnLayoutRectChanged(
    throttleMillis: Long,
    debounceMillis: Long,
    callback: (RelativeLayoutBounds) -> Unit,
): RegistrationHandle {
    val layoutNode = requireLayoutNode()
    val id = layoutNode.semanticsId
    val rectManager = layoutNode.requireOwner().rectManager
    return rectManager.registerOnRectChangedCallback(
        id,
        throttleMillis,
        debounceMillis,
        this,
        callback,
    )
}
