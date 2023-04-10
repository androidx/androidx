/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.ui.node

import androidx.compose.runtime.CompositionLocal
import androidx.compose.ui.Modifier

/**
 * Implementing this interface allows your [Modifier.Node] subclass to read
 * [CompositionLocals][CompositionLocal] via the [currentValueOf] function. The values of each
 * CompositionLocal will be resolved based on the context of the layout node that the modifier is
 * attached to, meaning that the modifier will see the same values of each CompositionLocal as its
 * corresponding layout node.
 *
 * @sample androidx.compose.ui.samples.CompositionLocalConsumingModifierSample
 *
 * @see Modifier.Node
 * @see CompositionLocal
 */
interface CompositionLocalConsumerModifierNode : DelegatableNode

/**
 * Returns the current value of [local] at the position in the composition hierarchy of this
 * modifier's attached layout node.
 *
 * CompositionLocals should only be read with [currentValueOf] during the main phase of your
 * modifier's operations. This main phase of a modifier is defined as the timeframe of your Modifier
 * after it has been [attached][Modifier.Node.onAttach] and before it is
 * [detached][Modifier.Node.onDetach]. The main phase is when you will receive calls to your
 * modifier's primary hooks like [DrawModifierNode.draw], [LayoutModifierNode.measure],
 * [PointerInputModifierNode.onPointerEvent], etc. Every callback of a modifier that influences the
 * composable and is called after `onAttach()` and before `onDetach()` is considered part of the
 * main phase.
 *
 * Unlike [CompositionLocal.current], reads via this function are not automatically tracked by
 * Compose. Modifiers are not able to recompose in the same way that a Composable can, and therefore
 * can't receive updates arbitrarily for a CompositionLocal.
 *
 * Avoid reading CompositionLocals in [onAttach()][Modifier.Node.onAttach] and
 * [onDetach()][Modifier.Node.onDetach]. These lifecycle callbacks only happen once, meaning that
 * any reads in a lifecycle event will yield the value of the CompositionLocal as it was during the
 * event, and then never again. This can lead to Modifiers using stale CompositionLocal values and
 * unexpected behaviors in the UI.
 *
 * This function will fail with an [IllegalStateException] if you attempt to read a CompositionLocal
 * before the node is [attached][Modifier.Node.onAttach] or after the node is
 * [detached][Modifier.Node.onDetach].
 */
fun <T> CompositionLocalConsumerModifierNode.currentValueOf(local: CompositionLocal<T>): T {
    check(node.isAttached) {
        "Cannot read CompositionLocal because the Modifier node is not currently attached. Make " +
            "sure to only invoke currentValueOf() in the main phase of your modifier. See " +
            "currentValueOf()'s documentation for more information."
    }
    return requireLayoutNode().compositionLocalMap[local]
}