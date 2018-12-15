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

import androidx.ui.foundation.diagnostics.describeIdentity
import androidx.ui.scheduleMicrotask

/**
 * Manages the focus tree.
 *
 * The focus tree keeps track of which widget is the user's current focus. The focused widget often
 * listens for keyboard events.
 *
 * The focus manager is responsible for holding the [FocusScopeNode] that is the root of the focus
 * tree and tracking which [FocusNode] has the overall focus.
 *
 * The [FocusManager] is held by the [WidgetsBinding] as [WidgetsBinding.focusManager]. The
 * [FocusManager] is rarely accessed directly. Instead, to find the [FocusScopeNode] for a given
 * [BuildContext], use [FocusScope.of].
 *
 * See also:
 *
 *  * [FocusNode], which is a leaf node in the focus tree that can receive focus.
 *  * [FocusScopeNode], which is an interior node in the focus tree.
 *  * [FocusScope.of], which provides the [FocusScopeNode] for a given [BuildContext].
 *
 * @constructor Creates an object that manages the focus tree.
 *
 * This constructor is rarely called directly. To access the [FocusManager], consider using
 * [WidgetsBinding.focusManager] instead.
 */
class FocusManager(
    // For mocking the scheduler for testing purposes.
    private val scheduleMicrotaskDelegate: (() -> Unit) -> Unit = ::scheduleMicrotask
) {

    /**
     * The root [FocusScopeNode] in the focus tree.
     *
     * This field is rarely used direction. Instead, to find the [FocusScopeNode] for a given
     * [BuildContext], use [FocusScope.of].
     */
    internal val rootScope = FocusScopeNode()
    internal var haveScheduledUpdate = false

    init {
        rootScope.manager = this
        assert(rootScope.firstChild == null)
        assert(rootScope.lastChild == null)
    }

    internal var currentFocus: FocusNode? = null
        private set

    fun willDisposeFocusNode(node: FocusNode) {
        if (currentFocus == node) currentFocus = null
    }

    internal fun markNeedsUpdate() {
        if (haveScheduledUpdate) return
        haveScheduledUpdate = true
        scheduleMicrotaskDelegate(::update)
    }

    private fun findNextFocus(): FocusNode? {
        var scope = rootScope
        while (true) {
            scope = scope.firstChild ?: break
        }
        return scope.focus
    }

    private fun update() {
        haveScheduledUpdate = false
        val nextFocus = findNextFocus()
        if (currentFocus == nextFocus) return
        val previousFocus = currentFocus
        currentFocus = nextFocus
        previousFocus?.notifyFocusChange()
        currentFocus?.notifyFocusChange()
    }

    override fun toString(): String {
        val status = if (haveScheduledUpdate) " UPDATE SCHEDULED" else ""
        val indent = "  "
        return "${describeIdentity(this)}$status\n" +
                "${indent}currentFocus: $currentFocus\n"
        // TODO(Migration/nona): Implemnet toStringDeep
        // "${rootScope.toStringDeep(prefixLineOne: indent, prefixOtherLines: indent)}"
    }
}