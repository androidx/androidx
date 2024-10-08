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

import android.graphics.Rect
import android.view.FocusFinder
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.FOCUS_DOWN
import android.view.ViewTreeObserver
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusDirection.Companion.Exit
import androidx.compose.ui.focus.FocusOwner
import androidx.compose.ui.focus.FocusProperties
import androidx.compose.ui.focus.FocusPropertiesModifierNode
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.FocusRequester.Companion.Cancel
import androidx.compose.ui.focus.FocusRequester.Companion.Default
import androidx.compose.ui.focus.FocusTargetNode
import androidx.compose.ui.focus.focusTarget
import androidx.compose.ui.focus.performRequestFocus
import androidx.compose.ui.focus.requestInteropFocus
import androidx.compose.ui.focus.toAndroidFocusDirection
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.Nodes
import androidx.compose.ui.node.requireLayoutNode
import androidx.compose.ui.node.requireOwner
import androidx.compose.ui.node.visitLocalDescendants
import androidx.compose.ui.platform.InspectorInfo

internal fun Modifier.focusInteropModifier(): Modifier =
    this
        // Focus Group to intercept focus enter/exit.
        .then(FocusGroupPropertiesElement)
        .focusTarget()
        // Focus Target to make the embedded view focusable.
        .then(FocusTargetPropertiesElement)
        .focusTarget()

private class FocusTargetPropertiesNode : Modifier.Node(), FocusPropertiesModifierNode {
    override fun applyFocusProperties(focusProperties: FocusProperties) {
        focusProperties.canFocus = node.isAttached && getView().hasFocusable()
    }
}

private class FocusGroupPropertiesNode :
    Modifier.Node(), FocusPropertiesModifierNode, ViewTreeObserver.OnGlobalFocusChangeListener {
    var focusedChild: View? = null

    override fun applyFocusProperties(focusProperties: FocusProperties) {
        focusProperties.canFocus = false
        focusProperties.enter = ::onEnter
        focusProperties.exit = ::onExit
    }

    fun onEnter(focusDirection: FocusDirection): FocusRequester {
        // If this requestFocus is triggered by the embedded view getting focus,
        // then we don't perform this onEnter logic.
        val embeddedView = getView()
        if (embeddedView.isFocused || embeddedView.hasFocus()) return Default

        val focusOwner = requireOwner().focusOwner
        val hostView = requireOwner() as View

        val targetViewFocused =
            embeddedView.requestInteropFocus(
                direction = focusDirection.toAndroidFocusDirection(),
                rect = getCurrentlyFocusedRect(focusOwner, hostView, embeddedView)
            )
        return if (targetViewFocused) Default else Cancel
    }

    fun onExit(focusDirection: FocusDirection): FocusRequester {
        val embeddedView = getView()
        if (!embeddedView.hasFocus()) return Default

        val focusOwner = requireOwner().focusOwner
        val hostView = requireOwner() as View

        // If the embedded view is not a view group, then we can safely exit this focus group.
        if (embeddedView !is ViewGroup) {
            check(hostView.requestFocus()) { "host view did not take focus" }
            return Default
        }

        val focusedRect = getCurrentlyFocusedRect(focusOwner, hostView, embeddedView)
        val androidFocusDirection = focusDirection.toAndroidFocusDirection() ?: FOCUS_DOWN

        val nextView =
            with(FocusFinder.getInstance()) {
                if (focusedChild != null) {
                    findNextFocus(hostView as ViewGroup, focusedChild, androidFocusDirection)
                } else {
                    findNextFocusFromRect(hostView as ViewGroup, focusedRect, androidFocusDirection)
                }
            }
        if (nextView != null && embeddedView.containsDescendant(nextView)) {
            nextView.requestFocus(androidFocusDirection, focusedRect)
            return Cancel
        } else {
            check(hostView.requestFocus()) { "host view did not take focus" }
            return Default
        }
    }

    private fun getFocusTargetOfEmbeddedViewWrapper(): FocusTargetNode {
        var foundFocusTargetOfFocusGroup = false
        visitLocalDescendants(Nodes.FocusTarget) {
            if (foundFocusTargetOfFocusGroup) return it
            foundFocusTargetOfFocusGroup = true
        }
        error("Could not find focus target of embedded view wrapper")
    }

    override fun onGlobalFocusChanged(oldFocus: View?, newFocus: View?) {
        if (requireLayoutNode().owner == null) return
        val embeddedView = getView()
        val focusOwner = requireOwner().focusOwner
        val hostView = requireOwner()
        val subViewLostFocus =
            oldFocus != null && oldFocus != hostView && embeddedView.containsDescendant(oldFocus)
        val subViewGotFocus =
            newFocus != null && newFocus != hostView && embeddedView.containsDescendant(newFocus)
        when {
            subViewLostFocus && subViewGotFocus -> {
                // Focus Moving within embedded view. Do nothing.
                focusedChild = newFocus
            }
            subViewGotFocus -> {
                // Focus moved to the embedded view.
                focusedChild = newFocus
                val focusTargetNode = getFocusTargetOfEmbeddedViewWrapper()
                if (!focusTargetNode.focusState.hasFocus)
                    focusOwner.focusTransactionManager.withNewTransaction {
                        focusTargetNode.performRequestFocus()
                    }
            }
            subViewLostFocus -> {
                focusedChild = null
                val focusTargetNode = getFocusTargetOfEmbeddedViewWrapper()
                if (focusTargetNode.focusState.isFocused) {
                    focusOwner.clearFocus(
                        force = false,
                        refreshFocusEvents = true,
                        clearOwnerFocus = false,
                        focusDirection = Exit
                    )
                }
            }
            else -> {
                // Focus Change not applicable to this node.
                focusedChild = null
            }
        }
    }

    override fun onAttach() {
        super.onAttach()
        (requireOwner() as View).viewTreeObserver.addOnGlobalFocusChangeListener(this)
    }

    override fun onDetach() {
        (requireOwner() as View).viewTreeObserver.removeOnGlobalFocusChangeListener(this)
        focusedChild = null
        super.onDetach()
    }
}

private object FocusGroupPropertiesElement : ModifierNodeElement<FocusGroupPropertiesNode>() {
    override fun create(): FocusGroupPropertiesNode = FocusGroupPropertiesNode()

    override fun update(node: FocusGroupPropertiesNode) {}

    override fun InspectorInfo.inspectableProperties() {
        name = "FocusGroupProperties"
    }

    override fun hashCode() = "FocusGroupProperties".hashCode()

    override fun equals(other: Any?) = other === this
}

private object FocusTargetPropertiesElement : ModifierNodeElement<FocusTargetPropertiesNode>() {
    override fun create(): FocusTargetPropertiesNode = FocusTargetPropertiesNode()

    override fun update(node: FocusTargetPropertiesNode) {}

    override fun InspectorInfo.inspectableProperties() {
        name = "FocusTargetProperties"
    }

    override fun hashCode() = "FocusTargetProperties".hashCode()

    override fun equals(other: Any?) = other === this
}

private fun Modifier.Node.getView(): View {
    @OptIn(InternalComposeUiApi::class)
    return checkNotNull(node.requireLayoutNode().getInteropView()) {
        "Could not fetch interop view"
    }
}

private fun View.containsDescendant(other: View): Boolean {
    var viewParent = other.parent
    while (viewParent != null) {
        if (viewParent === this.parent) return true
        viewParent = viewParent.parent
    }
    return false
}

private fun getCurrentlyFocusedRect(
    focusOwner: FocusOwner,
    hostView: View,
    embeddedView: View
): Rect? {
    val hostViewOffset = IntArray(2).also { hostView.getLocationOnScreen(it) }
    val embeddedViewOffset = IntArray(2).also { embeddedView.getLocationOnScreen(it) }
    val focusedRect = focusOwner.getFocusRect() ?: return null
    return Rect(
        focusedRect.left.toInt() + hostViewOffset[0] - embeddedViewOffset[0],
        focusedRect.top.toInt() + hostViewOffset[1] - embeddedViewOffset[1],
        focusedRect.right.toInt() + hostViewOffset[0] - embeddedViewOffset[0],
        focusedRect.bottom.toInt() + hostViewOffset[1] - embeddedViewOffset[1]
    )
}
