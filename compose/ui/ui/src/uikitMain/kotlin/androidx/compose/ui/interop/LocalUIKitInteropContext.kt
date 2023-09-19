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

package androidx.compose.ui.interop

import androidx.compose.runtime.staticCompositionLocalOf
import platform.Foundation.NSLock

internal enum class UIKitInteropState {
    BEGAN, UNCHANGED, ENDED
}

internal enum class UIKitInteropViewHierarchyChange {
    VIEW_ADDED,
    VIEW_REMOVED
}

/**
 * Lambda containing changes to UIKit objects, which can be synchronized within [CATransaction]
 */
internal typealias UIKitInteropAction = () -> Unit

internal interface UIKitInteropTransaction {
    val actions: List<UIKitInteropAction>
    val state: UIKitInteropState

    companion object {
        val empty = object : UIKitInteropTransaction {
            override val actions: List<UIKitInteropAction>
                get() = listOf()

            override val state: UIKitInteropState
                get() = UIKitInteropState.UNCHANGED
        }
    }
}

internal fun UIKitInteropTransaction.isEmpty() = actions.isEmpty() && state == UIKitInteropState.UNCHANGED
internal fun UIKitInteropTransaction.isNotEmpty() = !isEmpty()

private class UIKitInteropMutableTransaction: UIKitInteropTransaction {
    override val actions = mutableListOf<UIKitInteropAction>()
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
}

/**
 * Class which can be used to add actions related to UIKit objects to be executed in sync with compose rendering,
 * Addding deferred actions is threadsafe, but they will be executed in the order of their submission, and on the main thread.
 */
internal class UIKitInteropContext(
    val requestRedraw: () -> Unit
) {
    private val lock: NSLock = NSLock()
    private var transaction = UIKitInteropMutableTransaction()

    /**
     * Number of views, created by interop API and present in current view hierarchy
     */
    private var viewsCount = 0
        set(value) {
            require(value >= 0)

            field = value
        }

    /**
     * Add lambda to a list of commands which will be executed later in the same CATransaction, when the next rendered Compose frame is presented
     */
    fun deferAction(hierarchyChange: UIKitInteropViewHierarchyChange? = null, action: () -> Unit) {
        requestRedraw()

        lock.doLocked {
            if (hierarchyChange == UIKitInteropViewHierarchyChange.VIEW_ADDED) {
                if (viewsCount == 0) {
                    transaction.state = UIKitInteropState.BEGAN
                }
                viewsCount += 1
            }

            transaction.actions.add(action)

            if (hierarchyChange == UIKitInteropViewHierarchyChange.VIEW_REMOVED) {
                viewsCount -= 1
                if (viewsCount == 0) {
                    transaction.state = UIKitInteropState.ENDED
                }
            }
        }
    }

    /**
     * Return an object containing pending changes and reset internal storage
     */
    internal fun retrieve(): UIKitInteropTransaction =
        lock.doLocked {
            val result = transaction
            transaction = UIKitInteropMutableTransaction()
            result
        }
}

private inline fun <T> NSLock.doLocked(block: () -> T): T {
    lock()

    try {
        return block()
    } finally {
        unlock()
    }
}

internal val LocalUIKitInteropContext = staticCompositionLocalOf<UIKitInteropContext> {
    error("CompositionLocal UIKitInteropContext not provided")
}
