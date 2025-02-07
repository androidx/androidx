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

import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DelegatableNode.RegistrationHandle
import androidx.compose.ui.node.requireLayoutNode
import androidx.compose.ui.node.requireOwner
import androidx.compose.ui.spatial.RelativeLayoutBounds

/**
 * Registers a [callback] to be executed with the position of this modifier node relative to the
 * coordinate system of the root of the composition, as well as in screen coordinates and window
 * coordinates, see [RelativeLayoutBounds].
 *
 * It may also be used to calculate certain Layout relationships at the time of the callback
 * execution, such as [RelativeLayoutBounds.calculateOcclusions].
 *
 * This will be called after layout pass. This API allows for throttling and debouncing parameters
 * in order to moderate the frequency with which the callback gets invoked during high rates of
 * change (e.g. scrolling).
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
 * @param callback The callback to be executed, provides a new [RelativeLayoutBounds] instance
 *   associated to this [DelegatableNode]. Keep in mind this callback is executed on the main thread
 *   even when debounced.
 * @return an object which should be used to unregister/dispose this callback, such as when a node
 *   is detached
 */
@Suppress("PairedRegistration") // User expected to handle disposing
fun DelegatableNode.registerOnGlobalLayoutListener(
    throttleMillis: Long,
    debounceMillis: Long,
    callback: (RelativeLayoutBounds) -> Unit
): RegistrationHandle {
    val layoutNode = requireLayoutNode()
    val id = layoutNode.semanticsId
    val rectManager = layoutNode.requireOwner().rectManager
    return rectManager.registerOnGlobalLayoutCallback(
        id = id,
        throttleMillis = throttleMillis,
        debounceMillis = debounceMillis,
        node = node,
        callback = callback
    )
}
