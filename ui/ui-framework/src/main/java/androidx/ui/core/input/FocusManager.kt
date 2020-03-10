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
    internal interface FocusNode {
        /** Called when this component gained the focus */
        fun onFocus()

        /**
         *  Called when this component is about to lose the focus
         *
         *  @param hasNextClient True if this node loses focus due to focusing in to another node
         *  . False if this node loses focus due to calling [FocusManager#blur].
         */
        fun onBlur(hasNextClient: Boolean)
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
    internal fun registerFocusNode(identifier: String, node: FocusNode) {
        focusMap[identifier] = node
    }

    /**
     * Unregister the focusable node associated with given identifier.
     */
    internal fun unregisterFocusNode(identifier: String) {
        focusMap.remove(identifier)
    }

    /**
     * Request the input focus
     */
    internal open fun requestFocus(client: FocusNode) {
        val currentFocus = focusedClient
        if (currentFocus == client) {
            return // focus in to the same component. Do nothing.
        }

        if (currentFocus != null) {
            currentFocus.onBlur(true)
        }

        focusedClient = client
        client.onFocus()
    }

    /**
     * Release the focus if given focus node is focused
     */
    internal fun blur(client: FocusNode) {
        if (focusedClient == client) {
            focusedClient = null
            client.onBlur(false)
        }
    }
}