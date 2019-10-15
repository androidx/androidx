/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.ui.core.input

/**
 * Manages focused composable and available.
 *
 * You can access the active focus manager via ambient.
 */
open class FocusManager internal constructor() {

    /**
     * An interface for focusable object
     */
    // TODO(nona): Consider making this public for custom text field.
    internal interface FocusNode {
        /** Called when this component gained the focus */
        fun onFocus()

        /** Called when this component is about to lose the focus */
        fun onBlur()
    }

    /**
     * The focused client. Maybe null if nothing is focused.
     */
    private var focusedClient: FocusNode? = null

    /**
     * The identifier to focusable node map.
     */
    private val focusMap = mutableMapOf<String, FocusNode>()

    /**
     * Request the focus assiciated with the identifier.
     *
     * Do nothing if there is no focusable node assciated with given identifier.
     *
     * @param identifier The focusable node identifier.
     */
    fun requestFocusById(identifier: String) {
        // TODO: Good to defer the task for avoiding possible infinity loop.
        focusMap[identifier]?.let { requestFocus(it) }
    }

    /**
     * Register the focusable node with identifier.
     *
     * The caller must unregister when the focusable node is disposed.
     * This function overwrites if the focusable node is already registered.
     *
     */
    // TODO(nona): Consider making this public for custom text field.
    internal fun registerFocusNode(identifier: String, node: FocusNode) {
        focusMap[identifier] = node
    }

    /**
     * Unregister the focusable node associated with given identifier.
     */
    // TODO(nona): Consider making this public for custom text field.
    internal fun unregisterFocusNode(identifier: String) {
        focusMap.remove(identifier)
    }

    /**
     * Request the input focus
     */
    // TODO(nona): Consider making this public for custom text field.
    internal open fun requestFocus(client: FocusNode) {
        val currentFocus = focusedClient
        if (currentFocus == client) {
            return // focus in to the same component. Do nothing.
        }

        if (currentFocus != null) {
            focusOut(currentFocus)
        }

        focusedClient = client
        focusIn(client)
    }

    private fun focusIn(client: FocusNode) {
        client.onFocus()
    }

    private fun focusOut(client: FocusNode) {
        client.onBlur()
    }
}