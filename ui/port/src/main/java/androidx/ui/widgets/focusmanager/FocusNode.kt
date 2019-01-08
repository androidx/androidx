/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.ui.widgets.focusmanager

import androidx.ui.foundation.change_notifier.ChangeNotifier
import androidx.ui.foundation.diagnostics.describeIdentity

/**
 * A leaf node in the focus tree that can receive focus.
 *
 * The focus tree keeps track of which widget is the user's current focus. The focused widget often
 * listens for keyboard events.
 *
 * To request focus, find the [FocusScopeNode] for the current [BuildContext] and call the
 * [FocusScopeNode.requestFocus] method:
 *
 * //TODO(Migration/nona): rewrite example with Kotlin
 * ```dart
 * FocusScope.of(context).requestFocus(focusNode);
 * ```
 *
 * If your widget requests focus, be sure to call
 * `FocusScope.of(context).reparentIfNeeded(focusNode);` in your `build` method to reparent your
 * [FocusNode] if your widget moves from one location in the tree to another.
 *
 * ## Lifetime
 *
 * Focus nodes are long-lived objects. For example, if a stateful widget has a focusable child
 * widget, it should create a [FocusNode] in the [State.initState] method, and [dispose] it in the
 * [State.dispose] method, providing the same [FocusNode] to the focusable child each time the
 * [State.build] method is run. In particular, creating a [FocusNode] each time [State.build] is
 * invoked will cause the focus to be lost each time the widget is built.
 *
 * See also:
 *
 *  * [FocusScopeNode], which is an interior node in the focus tree.
 *  * [FocusScope.of], which provides the [FocusScopeNode] for a given [BuildContext].
 */
class FocusNode : ChangeNotifier() {
    var parent: FocusScopeNode? = null
    var manager: FocusManager? = null
    internal var hasKeyboardToken = false

    /**
     * Whether this node has the overall focus.
     *
     * A [FocusNode] has the overall focus when the node is focused in its parent [FocusScopeNode]
     * and [FocusScopeNode.isFirstFocus] is true for that scope and all its ancestor scopes.
     *
     * To request focus, find the [FocusScopeNode] for the current [BuildContext] and call the
     * [FocusScopeNode.requestFocus] method:
     *
     * TODO(Migration/nona): Rewrite example with Kotlin
     * ```dart
     * FocusScope.of(context).requestFocus(focusNode);
     * ```
     *
     * This object notifies its listeners whenever this value changes.
     */
    val hasFocus
        get() = manager?.currentFocus == this

    /**
     * Removes the keyboard token from this focus node if it has one.
     *
     * This mechanism helps distinguish between an input control gaining focus by default and
     * gaining focus as a result of an explicit user action.
     *
     * When a focus node requests the focus (either via [FocusScopeNode.requestFocus] or
     * [FocusScopeNode.autofocus]), the focus node receives a keyboard token if it does not already
     * have one. Later, when the focus node becomes focused, the widget that manages the
     * [TextInputConnection] should show the keyboard (i.e., call [TextInputConnection.show]) only
     * if it successfully consumes the keyboard token from the focus node.
     *
     * Returns whether this function successfully consumes a keyboard token.
     */
    fun consumeKeyboardToken(): Boolean {
        if (!hasKeyboardToken) return false
        hasKeyboardToken = false
        return true
    }

    /**
     * Cancels any outstanding requests for focus.
     *
     * This method is safe to call regardless of whether this node has ever requested focus.
     */
    fun unfocus() {
        parent?.resignFocus(this)
        assert(parent == null)
        assert(manager == null)
    }

    override fun dispose() {
        manager?.willDisposeFocusNode(this)
        parent?.resignFocus(this)
        assert(parent == null) // resignFocus will make parent null.
        assert(manager == null) // resignFocus will make manager null.
        super.dispose()
    }

    fun notifyFocusChange() {
        notifyListeners()
    }

    override fun toString(): String =
        "${describeIdentity(this)}${if (hasFocus) "(FOCUSED)" else ""}"
}