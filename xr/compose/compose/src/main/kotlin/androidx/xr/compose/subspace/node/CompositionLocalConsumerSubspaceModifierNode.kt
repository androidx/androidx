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

package androidx.xr.compose.subspace.node

import androidx.compose.runtime.CompositionLocal
import androidx.xr.compose.subspace.layout.DelegatableSubspaceNode
import androidx.xr.compose.subspace.layout.SubspaceModifier

/**
 * Implementing this interface allows your [SubspaceModifier.Node] subclass to read
 * [CompositionLocals][CompositionLocal] via the [currentValueOf] function. The values of each
 * CompositionLocal will be resolved based on the context of the layout node that the modifier is
 * attached to, meaning that the modifier will see the same values of each CompositionLocal as its
 * corresponding layout node.
 *
 * @see SubspaceModifier.Node
 * @see CompositionLocal
 */
public interface CompositionLocalConsumerSubspaceModifierNode : DelegatableSubspaceNode

/**
 * Returns the current value of [local] at the position in the composition hierarchy of this
 * modifier's attached layout node.
 *
 * Unlike [CompositionLocal.current], reads via this function are not automatically tracked by
 * Compose. Modifiers are not able to recompose in the same way that a Composable can, and therefore
 * can't receive updates arbitrarily for a CompositionLocal.
 *
 * Because CompositionLocals may change arbitrarily, it is strongly recommended to ensure that the
 * composition local is observed instead of being read once. If you call [currentValueOf] inside of
 * a modifier callback like [SubspaceLayoutModifierNode.measure], then Compose will track the
 * CompositionLocal read. This happens automatically, because these Compose UI phases take place in
 * a snapshot observer that tracks which states are read. If the value of the CompositionLocal
 * changes, and it was read inside of the measure or draw phase, then that phase will automatically
 * be invalidated.
 *
 * For all other reads of a CompositionLocal, this function will **not** notify you when the value
 * of the local changes.
 *
 * This function will fail with an [IllegalStateException] if you attempt to read a CompositionLocal
 * before the node is [attached][SubspaceModifier.Node.onAttach] or after the node is
 * [detached][SubspaceModifier.Node.onDetach].
 *
 * @param local The CompositionLocal to get the current value of
 * @return The value provided by the nearest CompositionLocalProvider component that invokes,
 *   directly or indirectly, the composable function that this modifier is attached to. If [local]
 *   was never provided, its default value will be returned instead.
 */
public fun <T : Any?> CompositionLocalConsumerSubspaceModifierNode.currentValueOf(
    local: CompositionLocal<T>
): T {
    val compositionLocalMap =
        checkNotNull(node.layoutNode?.compositionLocalMap) {
            "Expected layoutNode and compositionLocalMap to be set before requesting composition locals."
        }
    return compositionLocalMap[local]
}
