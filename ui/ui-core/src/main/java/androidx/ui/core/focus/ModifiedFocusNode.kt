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

import androidx.ui.core.DelegatingLayoutNodeWrapper
import androidx.ui.core.LayoutNodeWrapper
import androidx.ui.core.focus.FocusDetailedState.Active
import androidx.ui.core.focus.FocusDetailedState.ActiveParent
import androidx.ui.core.focus.FocusDetailedState.Captured
import androidx.ui.core.focus.FocusDetailedState.Disabled
import androidx.ui.core.focus.FocusDetailedState.Inactive
import androidx.ui.util.fastForEach

internal class ModifiedFocusNode(
    wrapped: LayoutNodeWrapper,
    modifier: FocusModifierImpl
) : DelegatingLayoutNodeWrapper<FocusModifierImpl>(wrapped, modifier) {

    /**
     * Request focus for this node.
     *
     * @param propagateFocus Whether the focus should be propagated to the node's children.
     *
     * In Compose, the parent [FocusNode][ModifiedFocusNode] controls focus for its focusable
     * children. Calling this function will send a focus request to this
     * [FocusNode][ModifiedFocusNode]'s parent [FocusNode][ModifiedFocusNode].
     */
    fun requestFocus(propagateFocus: Boolean = true) {
        when (modifier.focusDetailedState) {
            Active, Captured, Disabled -> return
            ActiveParent -> {
                val focusedChild = modifier.focusedChild
                requireNotNull(focusedChild)

                /** We don't need to do anything if [propagateFocus] is true,
                since this subtree already has focus.*/
                if (propagateFocus) return

                if (focusedChild.clearFocus()) {
                    grantFocus(propagateFocus)
                }
            }
            Inactive -> {
                val focusParent = findParentFocusNode()
                if (focusParent == null) {
                    if (requestFocusForOwner()) {
                        grantFocus(propagateFocus)
                    }
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
    fun captureFocus() = when (modifier.focusDetailedState) {
        Active, Captured -> {
            modifier.focusDetailedState = Captured
            true
        }
        else -> false
    }

    /**
     * When the node is in the [Captured] state, it rejects all requests to clear focus. Calling
     * [freeFocus] puts the node in the [Active] state, where it is no longer preventing other
     * nodes from requesting focus.
     *
     * @return true if the captured focus was released. If the node is not in the [Captured]
     * state. this function returns false to indicate that this operation was a no-op.
     */
    fun freeFocus() = if (modifier.focusDetailedState == Captured) {
        modifier.focusDetailedState = Active
        true
    } else {
        false
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
        val focusedCandidate = focusableChildren().firstOrNull()

        if (focusedCandidate == null || !propagateFocus) {
            // No Focused Children, or we don't want to propagate focus to children.
            modifier.focusDetailedState = Active
        } else {
            modifier.focusDetailedState = ActiveParent
            modifier.focusedChild = focusedCandidate
            focusedCandidate.grantFocus(propagateFocus)
        }
    }

    /**
     * This function clears focus from this node.
     *
     * Note: This function is private, and should only be called by a parent [ModifiedFocusNode] to
     * clear focus from one of its child [ModifiedFocusNode]s.
     */
    private fun clearFocus(): Boolean {
        return when (modifier.focusDetailedState) {
            Active -> {
                modifier.focusDetailedState = Inactive
                findParentFocusNode()?.modifier?.focusedChild = null
                true
            }
            /**
             * If the node is [ActiveParent], we need to clear focus from the [Active] descendant
             * first, before clearing focus of this node.
             */
            ActiveParent -> {
                if (clearFocusFromChildren()) {
                    modifier.focusDetailedState = Active
                    clearFocus()
                } else {
                    false
                }
            }
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

    private fun clearFocusFromChildren(): Boolean {
        require(modifier.focusDetailedState == ActiveParent)

        val focusedChild = modifier.focusedChild
        requireNotNull(focusedChild)

        return focusedChild.clearFocus()
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
        if (!focusableChildren().contains(childNode)) {
            error("Non child node cannot request focus.")
        }

        return when (modifier.focusDetailedState) {
            /**
             * If this node is [Active], it can give focus to the requesting child.
             */
            Active -> {
                modifier.focusDetailedState = ActiveParent
                modifier.focusedChild = childNode
                childNode.grantFocus(propagateFocus)
                true
            }
            /**
             * If this node is [ActiveParent] ie, one of the parent's descendants is [Active],
             * remove focus from the currently focused child and grant it to the requesting child.
             */
            ActiveParent -> {
                val previouslyFocusedNode = modifier.focusedChild
                requireNotNull(previouslyFocusedNode)
                if (previouslyFocusedNode.clearFocus()) {
                    modifier.focusedChild = childNode
                    childNode.grantFocus(propagateFocus)
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
                    // If the owner successfully gains focus, proceed otherwise return false.
                    if (requestFocusForOwner()) {
                        modifier.focusDetailedState = Active
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

    private fun requestFocusForOwner(): Boolean {
        val owner = layoutNode.owner
        requireNotNull(owner, { "Owner not initialized." })
        return owner.requestFocus()
    }

    override fun findPreviousFocusWrapper() = this

    override fun findNextFocusWrapper() = this

    // TODO(b/152051577): Measure the performance of focusableChildren.
    //  Consider caching the children.
    internal fun focusableChildren(): List<ModifiedFocusNode> {
        // Check the modifier chain that this focus node is part of. If it has a focus modifier,
        // that means you have found the only focusable child for this node.
        val focusableChild = wrapped.findNextFocusWrapper()
        // findChildFocusNodeInWrapperChain()
        if (focusableChild != null) {
            return listOf(focusableChild)
        }

        // Go through all your children and find the first focusable node from each child.
        val focusableChildren = mutableListOf<ModifiedFocusNode>()
        layoutNode.children.fastForEach { node ->
            focusableChildren.addAll(node.focusableChildren())
        }
        return focusableChildren
    }

    internal fun findActiveFocusNode(): ModifiedFocusNode? {
        return when (modifier.focusDetailedState) {
            Active, Captured -> this
            ActiveParent -> modifier.focusedChild?.findActiveFocusNode()
            Inactive, Disabled -> null
        }
    }
}
