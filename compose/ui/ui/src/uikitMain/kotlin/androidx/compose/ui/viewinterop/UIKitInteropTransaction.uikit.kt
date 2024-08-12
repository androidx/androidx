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

package androidx.compose.ui.viewinterop

import platform.QuartzCore.CATransaction

/**
 * Enum which is used to define if rendering strategy should be changed along with this transaction.
 * If [BEGAN], it will wait until a next CATransaction on every frame and make the metal layer transparent.
 * If [ENDED] it will fallback to the most efficient rendering strategy
 *   (opaque layer, no transaction waiting, asynchronous encoding and GPU-driven presentation).
 * If [UNCHANGED] it will keep the current rendering strategy.
 */
internal enum class UIKitInteropState {
    BEGAN, UNCHANGED, ENDED
}

/**
 * Lambda containing changes to UIKit objects, which can be synchronized within [CATransaction]
 */
internal typealias UIKitInteropAction = () -> Unit

/**
 * A transaction containing changes to UIKit objects to be synchronized within [CATransaction] inside a
 * renderer to make sure that changes in UIKit and Compose are visually simultaneous.
 * [actions] contains a list of lambdas that will be executed in the same CATransaction.
 * [state] defines if rendering strategy should be changed along with this transaction.
 */
internal interface UIKitInteropTransaction {
    val actions: List<UIKitInteropAction>
    val state: UIKitInteropState
}

internal fun UIKitInteropTransaction.isEmpty() =
    actions.isEmpty() && state == UIKitInteropState.UNCHANGED

internal fun UIKitInteropTransaction.isNotEmpty() = !isEmpty()

/**
 * A mutable transaction managed by [UIKitInteropContainer] to collect changes
 * to UIKit objects to be executed later.
 *
 * @see UIKitInteropContainer.scheduleUpdate
 */
internal class UIKitInteropMutableTransaction : UIKitInteropTransaction {
    private val _actions = mutableListOf<UIKitInteropAction>()

    override val actions
        get() = _actions

    override var state = UIKitInteropState.UNCHANGED
        set(value) {
            field = when (value) {
                UIKitInteropState.UNCHANGED -> error("Can't assign UNCHANGED value explicitly")
                UIKitInteropState.BEGAN -> {
                    when (field) {
                        UIKitInteropState.BEGAN -> error("Can't assign BEGAN twice in the same transaction")
                        UIKitInteropState.UNCHANGED -> value
                        UIKitInteropState.ENDED -> UIKitInteropState.UNCHANGED
                    }
                }
                UIKitInteropState.ENDED -> {
                    when (field) {
                        UIKitInteropState.BEGAN -> UIKitInteropState.UNCHANGED
                        UIKitInteropState.UNCHANGED -> value
                        UIKitInteropState.ENDED -> error("Can't assign ENDED twice in the same transaction")
                    }
                }
            }
        }

    fun add(action: UIKitInteropAction) {
        _actions.add(action)
    }
}
