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
 * Interface of manager of focused composable.
 *
 * Focus manager keeps tracking the input focused node and provides focus transitions.
 */
interface FocusManager {
    /**
     * An interface for focusable object.
     *
     * Any component that will have input focus must implement this FocusNode and observe focus
     * state.
     */
    interface FocusNode {
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
     * Request the focus assiciated with the identifier.
     *
     * Do nothing if there is no focusable node assciated with given identifier.
     *
     * @param identifier The focusable node identifier.
     */
    fun requestFocusById(identifier: String)

    /**
     * Register the focusable node with identifier.
     *
     * The caller must unregister when the focusable node is disposed.
     * This function overwrites if the focusable node is already registered.
     *
     * @param identifier focusable client identifier
     * @param node focusable client.
     */
    fun registerFocusNode(identifier: String, node: FocusNode)

    /**
     * Unregister the focusable node associated with given identifier.
     *
     * @param identifier focusable client identifier
     */
    fun unregisterFocusNode(identifier: String)

    /**
     * Request the input focus
     *
     * @param client A focusable client.
     */
    fun requestFocus(client: FocusNode)

    /**
     * Release the focus if given focus node is focused
     *
     * @param client A focusable client.
     */
    fun blur(client: FocusNode)
}

/**
 * Manages focused composable and available.
 */
internal class FocusManagerImpl : FocusManager {
    /**
     * The focused client. Maybe null if nothing is focused.
     */
    private var focusedClient: FocusManager.FocusNode? = null

    /**
     * The identifier to focusable node map.
     */
    private val focusMap = mutableMapOf<String, FocusManager.FocusNode>()

    override fun requestFocusById(identifier: String) {
        // TODO(nona): Good to defer the task for avoiding possible infinity loop.
        focusMap[identifier]?.let { requestFocus(it) }
    }

    override fun registerFocusNode(identifier: String, node: FocusManager.FocusNode) {
        focusMap[identifier] = node
    }

    override fun unregisterFocusNode(identifier: String) {
        focusMap.remove(identifier)
    }

    override fun requestFocus(client: FocusManager.FocusNode) {
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

    override fun blur(client: FocusManager.FocusNode) {
        if (focusedClient == client) {
            focusedClient = null
            client.onBlur(false)
        }
    }
}