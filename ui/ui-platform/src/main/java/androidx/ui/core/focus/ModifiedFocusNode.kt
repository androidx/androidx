/*
 * Copyright 2020 The Android Open Source Project
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
package androidx.ui.core.focus

import androidx.ui.core.LayoutCoordinates
import androidx.ui.core.Ref
import androidx.ui.focus.FocusDetailedState
import androidx.ui.focus.FocusDetailedState.Active
import androidx.ui.focus.FocusDetailedState.ActiveParent
import androidx.ui.focus.FocusDetailedState.Captured
import androidx.ui.focus.FocusDetailedState.Disabled
import androidx.ui.focus.FocusDetailedState.Inactive

/**
 * Backing node that implements focus.
 */
internal class ModifiedFocusNode {
    /**
     * Implementation oddity around composition; used to capture a reference to this
     * [ModifiedFocusNode] when composed. This is a reverse property that mutates its right-hand
     * side.
     *
     * TODO: Once we finalize the API consider removing this and replace this with an
     *  interface that sets the value as a property on the object that needs it.
     */
    var ref: Ref<ModifiedFocusNode>?
        get() = null
        set(value) {
            value?.value = this
        }

    /**
     * The recompose function of the Recompose component this [ModifiedFocusNode] is hosted in.
     *
     * We need to trigger re-composition manually because we determine focus during composition, and
     * editing an @Model object during composition does not trigger a re-composition.
     *
     * TODO (b/144897112): Remove manual recomposition.
     */
    private lateinit var _recompose: () -> Unit
    var recompose: () -> Unit
        get() = _recompose
        set(value) {
            _recompose = value
        }

    /**
     * The focus state for the current component. When the component is in the [Active] state, it
     * receives key events and other actions. We use [FocusDetailedState]s internally and
     * developers have the option to build their components using [FocusDetailedState], or a
     * subset of states defined in [FocusState][androidx.ui.focus.FocusState].
     */
    var focusState: FocusDetailedState = Inactive
        internal set

    /**
     * The [LayoutCoordinates] of the [OnChildPositioned][androidx.ui.core.OnChildPositioned]
     * component that hosts the child components of this [FocusNode].
     */
    @Suppress("KDocUnresolvedReference")
    var layoutCoordinates: LayoutCoordinates? = null

    /**
     * The list of focusable children of this [ModifiedFocusNode]. The [ComponentNode][androidx
     * .ui.core.ComponentNode]
     * base
     * class defines children of this node, but the [focusableChildren] set includes all the
     * [ModifiedFocusNode]s
     * that are directly reachable from this [ModifiedFocusNode].
     */
    private val focusableChildren = mutableSetOf<ModifiedFocusNode>()

    /**
     * The [ModifiedFocusNode] from the set of [focusableChildren] that is currently [Active].
     */
    private var focusedChild: ModifiedFocusNode? = null

    /**
     * Add this focusable child to the parent's focusable children list.
     */
    fun attach() {
        findParentFocusNode()?.focusableChildren?.add(this)
    }

    /**
     * Remove this focusable child from the parent's focusable children list.
     */
    fun detach() {
        // TODO (b/144119129): If this node is focused, let the parent know that it needs to
        //  grant focus to another focus node.
        findParentFocusNode()?.focusableChildren?.remove(this)
    }

    /**
     * Request focus for this node.
     *
     * @param propagateFocus Whether the focus should be propagated to the node's children.
     *
     * In Compose, the parent [ModifiedFocusNode] controls focus for its focusable children.Calling
     * this function will send a focus request to this [ModifiedFocusNode]'s parent
     * [ModifiedFocusNode].
     */
    fun requestFocus(propagateFocus: Boolean = true) {

        when (focusState) {
            Active, Captured, Disabled -> return
            ActiveParent -> {
                /** We don't need to do anything if [propagateFocus] is true,
                since this subtree already has focus.*/
                if (!propagateFocus && focusedChild?.clearFocus() ?: true) {
                    grantFocus(propagateFocus)
                }
            }
            Inactive -> {
                val focusParent = findParentFocusNode()
                if (focusParent == null) {
                    // TODO(b/144116848): Find out if the view hosting this composable is in focus.
                    //  The top most focusable is [Active] only if the view hosting this composable
                    //  is in focus. For now, we are making the assumption that our activity has
                    //  only one view, and it is always in focus.
                    //  Also, if the host AndroidComposeView does not have focus, request focus.
                    //  Proceed to grant focus to this node only if the host view gains focus.
                    grantFocus(propagateFocus)
                    recompose()
                } else {
                    focusParent.requestFocusForChild(this, propagateFocus)
                }
            }
        }
    }

    /**
     * Deny requests to clear focus.
     *
     * This is used when a component wants to hold onto focus (eg. A phone number field with an
     * invalid number.
     *
     * @return true if the focus was successfully captured. False otherwise.
     */
    fun captureFocus(): Boolean {
        if (focusState == Active) {
            focusState = Captured
            return true
        } else {
            return false
        }
    }

    /**
     * When the node is in the [Captured] state, it rejects all requests to clear focus. Calling
     * [freeFocus] puts the node in the [Active] state, where it is no longer preventing other
     * nodes from requesting focus.
     *
     * @return true if the captured focus was released. If the node is not in the [Captured]
     * state. this function returns false to indicate that this operation was a no-op.
     */
    fun freeFocus(): Boolean {
        if (focusState == Captured) {
            focusState = Active
            return true
        } else {
            return false
        }
    }

    /**
     * This function grants focus to this node.
     *
     * @param propagateFocus Whether the focus should be propagated to the node's children.
     *
     * Note: This function is private, and should only be called by a parent [ModifiedFocusNode] to
     * grant focus to one of its child [ModifiedFocusNode]s.
     */
    private fun grantFocus(propagateFocus: Boolean) {

        // TODO (b/144126570) use ChildFocusablility.
        //  For now we assume children get focus before parent).

        // TODO (b/144126759): Design a system to decide which child get's focus.
        //  for now we grant focus to the first child.
        val focusedCandidate = focusableChildren.firstOrNull()

        if (focusedCandidate == null || !propagateFocus) {
            // No Focused Children, or we don't want to propagate focus to children.
            focusState = Active
        } else {
            focusState = ActiveParent
            focusedChild = focusedCandidate
            focusedCandidate.grantFocus(propagateFocus)
            focusedCandidate.recompose()
        }
    }

    /**
     * This function clears focus from this node.
     *
     * Note: This function is private, and should only be called by a parent [ModifiedFocusNode] to
     * clear focus from one of its child [ModifiedFocusNode]s.
     */
    private fun clearFocus(): Boolean {
        return when (focusState) {

            Active -> {
                focusState = Inactive
                true
            }
            /**
             * If the node is [ActiveParent], we need to clear focus from the [Active] descendant
             * first, before clearing focus of this node.
             */
            ActiveParent -> focusedChild?.clearFocus() ?: error("No Focused Child")
            /**
             * If the node is [Captured], deny requests to clear focus.
             */
            Captured -> false
            /**
             * Nothing to do if the node is not focused. Even though the node ends up in a
             * cleared state, we return false to indicate that we didn't change any state (This
             * return value is used to trigger a recomposition, so returning false will not
             * trigger any recomposition).
             */
            Inactive, Disabled -> false
        }
    }

    /**
     * Focusable children of this [ModifiedFocusNode] can use this function to request focus.
     *
     * @param childNode: The node that is requesting focus.
     * @param propagateFocus Whether the focus should be propagated to the node's children.
     * @return true if focus was granted, false otherwise.
     */
    private fun requestFocusForChild(
        childNode: ModifiedFocusNode,
        propagateFocus: Boolean
    ): Boolean {

        // Only this node's children can ask for focus.
        if (!focusableChildren.contains(childNode)) {
            error("Non child node cannot request focus.")
        }

        return when (focusState) {
            /**
             * If this node is [Active], it can give focus to the requesting child.
             */
            Active -> {
                focusState = ActiveParent
                focusedChild = childNode
                childNode.grantFocus(propagateFocus)
                recompose()
                true
            }
            /**
             * If this node is [ActiveParent] ie, one of the parent's descendants is [Active],
             * remove focus from the currently focused child and grant it to the requesting child.
             */
            ActiveParent -> {
                val previouslyFocusedNode = focusedChild ?: error("no focusedChild found")
                if (previouslyFocusedNode.clearFocus()) {
                    focusedChild = childNode
                    childNode.grantFocus(propagateFocus)
                    previouslyFocusedNode.recompose()
                    childNode.recompose()
                    true
                } else {
                    // Currently focused component does not want to give up focus.
                    false
                }
            }
            /**
             * If this node is not [Active], we must gain focus first before granting it
             * to the requesting child.
             */
            Inactive -> {
                val focusParent = findParentFocusNode()
                if (focusParent == null) {
                    requestFocusForOwner()
                    // If the owner successfully gains focus, proceed otherwise return false.
                    if (ownerHasFocus()) {
                        focusState = Active
                        requestFocusForChild(childNode, propagateFocus)
                    } else {
                        false
                    }
                } else if (focusParent.requestFocusForChild(this, propagateFocus = false)) {
                    requestFocusForChild(childNode, propagateFocus)
                } else {
                    // Could not gain focus, so have no focus to give.
                    false
                }
            }
            /**
             * If this node is [Captured], decline requests from the children.
             */
            Captured -> false
            /**
             * Children of a [Disabled] parent should also be [Disabled].
             */
            Disabled -> error("non root FocusNode needs a focusable parent")
        }
    }
}
