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

/**
 * A container that controls interop views/components.
 * It's using a modifier of [TrackInteropPlacementModifierNode] to properly sort native interop
 * elements and contains a logic for syncing changes to UIKit objects driven by Compose state
 * changes with Compose rendering.
 */
internal class UIKitInteropContainer(
    override val root: InteropViewGroup,
    val requestRedraw: () -> Unit
) : InteropContainer {
    override var rootModifier: TrackInteropPlacementModifierNode? = null
    override var interopViews = mutableSetOf<InteropViewHolder>()
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
     * Return an object containing pending changes and reset internal storage
     */
    fun retrieveTransaction(): UIKitInteropTransaction {
        val result = transaction
        transaction = UIKitInteropMutableTransaction()
        return result
    }

    override fun placeInteropView(interopView: InteropViewHolder) {
        if (interopViews.isEmpty()) {
            transaction.state = UIKitInteropState.BEGAN
        }
        interopViews.add(interopView)

        val countBelow = countInteropComponentsBelow(interopView)
        changeInteropViewLayout {
            root.insertSubview(interopView.group, countBelow.toLong())
        }
    }

    override fun unplaceInteropView(interopView: InteropViewHolder) {
        interopViews.remove(interopView)
        if (interopViews.isEmpty()) {
            transaction.state = UIKitInteropState.ENDED
        }

        changeInteropViewLayout {
            interopView.group.removeFromSuperview()
        }
    }

    override fun changeInteropViewLayout(action: () -> Unit) {
        requestRedraw()

        // Add lambda to a list of commands which will be executed later
        // in the same [CATransaction], when the next rendered Compose frame is presented.
        transaction.add(action)
    }
}
