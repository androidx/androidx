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

package androidx.compose.ui.viewinterop

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.annotation.RememberInComposition
import androidx.compose.runtime.collection.MutableVector
import androidx.compose.runtime.collection.mutableVectorOf
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.node.requireLayoutNode

/**
 * Allows explicitly requesting remeasurement of [UIKitView] or [UIKitViewController].
 *
 * Calling [requestRemeasure] schedules a new Compose measurement pass for the associated interop node.
 * This is useful because UIKit does not reliably propagate intrinsic/fitting-size changes to Compose.
 *
 * Call [requestRemeasure] after UIKit-side changes that may affect fitting size (e.g. changing
 * `UILabel.text`/`attributedText`/`font`, updating constraint constants, or adding/removing subviews).
 *
 * @see Modifier.remeasureRequester
 * @see rememberUIKitInteropRemeasureRequester
 */
@Stable
@ExperimentalComposeUiApi
class UIKitInteropRemeasureRequester @RememberInComposition constructor() {
    internal val remeasureRequesterNodes: MutableVector<UIKitInteropRemeasureRequesterModifierNode> =
        mutableVectorOf()

    /**
     * Requests remeasurement for all currently associated [UIKitView] or [UIKitViewController] interop nodes.
     *
     * @return `true` if remeasurement was requested for at least one target node, `false` otherwise.
     */
    fun requestRemeasure(): Boolean {
        if (remeasureRequesterNodes.isEmpty()) return false
        remeasureRequesterNodes.forEach { it.requireLayoutNode().requestRemeasure() }
        return true
    }
}


/**
 * Creates and remembers a [UIKitInteropRemeasureRequester] to allow explicitly requesting
 * remeasurement of [UIKitView] or [UIKitViewController].
 */
@Composable
@ExperimentalComposeUiApi
fun rememberUIKitInteropRemeasureRequester(): UIKitInteropRemeasureRequester =
    remember { UIKitInteropRemeasureRequester() }



