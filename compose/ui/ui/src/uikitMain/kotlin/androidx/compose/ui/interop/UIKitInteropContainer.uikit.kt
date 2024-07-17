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

package androidx.compose.ui.interop

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.node.InteropContainer
import androidx.compose.ui.node.LayoutNode
import androidx.compose.ui.node.TrackInteropModifierElement
import androidx.compose.ui.node.TrackInteropModifierNode
import androidx.compose.ui.node.countInteropComponentsBelow
import kotlinx.cinterop.CValue
import kotlinx.cinterop.readValue
import platform.CoreGraphics.CGPoint
import platform.CoreGraphics.CGRectZero
import platform.QuartzCore.CATransaction
import platform.UIKit.UIEvent
import platform.UIKit.UIView

/**
 * Providing interop container as composition local, so [UIKitView]/[UIKitViewController] can use it
 * to add native views to the hierarchy.
 */
internal val LocalUIKitInteropContainer = staticCompositionLocalOf<UIKitInteropContainer> {
    error("UIKitInteropContainer not provided")
}


/**
 * Enum which is used to define if rendering strategy should be changed along with this transaction.
 * If [BEGAN], it will wait until a next CATransaction on every frame and make the metal layer opaque.
 * If [ENDED] it will fallback to the most efficient rendering strategy (opaque layer, no transaction waiting, asynchronous encoding and GPU-driven presentation).
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
 * A mutable transaction managed by [UIKitInteropContainer] to collect changes to UIKit objects to be executed later.
 * @see UIKitView
 * @see UIKitViewController
 * @see UIKitInteropContainer.deferAction
 */
private class UIKitInteropMutableTransaction : UIKitInteropTransaction {
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

/**
 * A container that controls interop views/components. It's using a modifier of [TrackInteropModifierNode]
 * to properly sort native interop elements and contains a logic for syncing changes to UIKit objects
 * driven by Compose state changes with Compose rendering.
 */
internal class UIKitInteropContainer(
    val containerView: UIView,
    val requestRedraw: () -> Unit
) : InteropContainer<UIView> {
    override var rootModifier: TrackInteropModifierNode<UIView>? = null
    override var interopViews = mutableSetOf<UIView>()
        private set

    private var transaction = UIKitInteropMutableTransaction()

    /**
     * Dispose by immediately executing all UIKit interop actions that can't be deferred to be
     * synchronized with rendering because scene will never be rendered past that moment.
     */
    fun dispose() {
        val lastTransaction = retrieveTransaction()

        for (action in lastTransaction.actions) {
            action.invoke()
        }
    }

    /**
     * Add lambda to a list of commands which will be executed later in the same CATransaction, when the next rendered Compose frame is presented
     */
    fun deferAction(action: () -> Unit) {
        requestRedraw()

        transaction.add(action)
    }

    /**
     * Return an object containing pending changes and reset internal storage
     */
    fun retrieveTransaction(): UIKitInteropTransaction {
        val result = transaction
        transaction = UIKitInteropMutableTransaction()
        return result
    }

    /**
     * Counts the number of interop components before the given native view in the container.
     * And updates the index of the native view in the container for proper Z-ordering.
     * @see TrackInteropModifierNode.onPlaced
     */
    override fun placeInteropView(nativeView: UIView) {
        val index = countInteropComponentsBelow(nativeView)

        deferAction {
            containerView.insertSubview(nativeView, index.toLong())
        }
    }

    override fun unplaceInteropView(nativeView: UIView) {
        deferAction {
            nativeView.removeFromSuperview()
        }
    }

    fun startTrackingInteropView(nativeView: UIView) {
        if (interopViews.isEmpty()) {
            transaction.state = UIKitInteropState.BEGAN
        }

        interopViews.add(nativeView)
    }

    fun stopTrackingInteropView(nativeView: UIView) {
        interopViews.remove(nativeView)

        if (interopViews.isEmpty()) {
            transaction.state = UIKitInteropState.ENDED
        }
    }
}

/**
 * Modifier to track interop view inside [LayoutNode] hierarchy. Used to properly
 * sort interop views in the tree.
 *
 * @param view The [UIView] that matches the current node.
 */
internal fun Modifier.trackUIKitInterop(
    container: UIKitInteropContainer,
    view: UIView
): Modifier = this then TrackInteropModifierElement(
    container = container,
    nativeView = view
)

