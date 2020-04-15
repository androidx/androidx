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

package androidx.ui.core

import android.content.Context
import android.os.Bundle
import android.text.TextUtils
import android.view.MotionEvent
import android.view.View
import android.view.ViewParent
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityManager
import androidx.core.view.AccessibilityDelegateCompat
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.core.view.accessibility.AccessibilityNodeProviderCompat
import androidx.ui.core.semantics.SemanticsNode
import androidx.ui.core.semantics.findChildById
import androidx.ui.semantics.SemanticsActions
import androidx.ui.semantics.SemanticsProperties

internal class AndroidComposeViewAccessibilityDelegateCompat(val view: AndroidComposeView) :
    AccessibilityDelegateCompat() {
    companion object {
        /** Virtual node identifier value for invalid nodes. */
        const val InvalidId = Integer.MIN_VALUE
        const val ClassName = "android.view.View"
    }
    /** Virtual view id for the currently hovered logical item. */
    private var hoveredVirtualViewId = InvalidId
    private val accessibilityManager: AccessibilityManager =
        view.getContext().getSystemService(Context.ACCESSIBILITY_SERVICE) as
                AccessibilityManager
    private var nodeProvider: AccessibilityNodeProviderCompat = MyNodeProvider()
    private var focusedVirtualViewId = InvalidId

    fun createNodeInfo(virtualViewId: Int):
            AccessibilityNodeInfoCompat {
        val info: AccessibilityNodeInfoCompat = AccessibilityNodeInfoCompat.obtain()
        // the hidden property is often not there
        info.isVisibleToUser = true
        var semanticsNode: SemanticsNode?
        if (virtualViewId == AccessibilityNodeProviderCompat.HOST_VIEW_ID) {
            info.setSource(view)
            semanticsNode = view.semanticsOwner.rootSemanticsNode
            info.setParent(ViewCompat.getParentForAccessibility(view) as View)
        } else {
            semanticsNode = view.semanticsOwner.rootSemanticsNode.findChildById(virtualViewId)
            if (semanticsNode == null) {
                // throw IllegalStateException("Semantics node $virtualViewId is not attached")
                return info
            }
            info.setSource(view, semanticsNode.id)
            // TODO(b/154023028): Semantics: Immediate children of the root node report parent ==
            // null
            if (semanticsNode.parent != null) {
                var parentId = semanticsNode.parent!!.id
                if (parentId == view.semanticsOwner.rootSemanticsNode.id) {
                    parentId = AccessibilityNodeProviderCompat.HOST_VIEW_ID
                }
                info.setParent(view, parentId)
            } else {
                // throw IllegalStateException("semanticsNode $virtualViewId has null parent")
            }
        }

        // TODO(b/151240295): Should we have widgets class name?
        info.className = ClassName
        info.setPackageName(view.getContext().getPackageName())
        try {
            info.setBoundsInScreen(
                android.graphics.Rect(
                    semanticsNode.globalBounds.left.value.toInt(),
                    semanticsNode.globalBounds.top.value.toInt(),
                    semanticsNode.globalBounds.right.value.toInt(),
                    semanticsNode.globalBounds.bottom.value.toInt()
                )
            )
        } catch (e: IllegalStateException) {
            // We may get "Asking for measurement result of unmeasured layout modifier" error.
            // TODO(b/153198816): check whether we still get this exception when R is in.
            info.setBoundsInScreen(android.graphics.Rect())
        }

        for (child in semanticsNode.children) {
            info.addChild(view, child.id)
        }

        // Manage internal accessibility focus state.
        if (focusedVirtualViewId == virtualViewId) {
            info.setAccessibilityFocused(true)
            info.addAction(
                AccessibilityNodeInfoCompat.AccessibilityActionCompat
                    .ACTION_CLEAR_ACCESSIBILITY_FOCUS
            )
        } else {
            info.setAccessibilityFocused(false)
            info.addAction(
                AccessibilityNodeInfoCompat.AccessibilityActionCompat
                    .ACTION_ACCESSIBILITY_FOCUS
            )
        }

        // TODO(b/151847522): use state description when android R is available
        info.contentDescription = semanticsNode.config.getOrElse(
            SemanticsProperties.AccessibilityValue) { "" }
        var content = semanticsNode.config.getOrElse(
            SemanticsProperties.AccessibilityLabel) { "" }
        if (content != "") {
            info.contentDescription = TextUtils.concat(content, ",", info.contentDescription)
        }
        info.isEnabled = semanticsNode.config.getOrElse(
            SemanticsProperties.Enabled) { true }
        info.isVisibleToUser = !(semanticsNode.config.getOrElse(
            SemanticsProperties.Hidden) { false })
        info.isClickable = semanticsNode.config.contains(SemanticsActions.OnClick)
        if (info.isClickable) {
            info.addAction(
                AccessibilityNodeInfoCompat
                    .AccessibilityActionCompat.ACTION_CLICK
            )
        }

        return info
    }

    /**
     * Returns whether this virtual view is accessibility focused.
     *
     * @return True if the view is accessibility focused.
     */
    private fun isAccessibilityFocused(virtualViewId: Int): Boolean {
        return (focusedVirtualViewId == virtualViewId)
    }

    /**
     * Attempts to give accessibility focus to a virtual view.
     * <p>
     * A virtual view will not actually take focus if
     * {@link AccessibilityManager#isEnabled()} returns false,
     * {@link AccessibilityManager#isTouchExplorationEnabled()} returns false,
     * or the view already has accessibility focus.
     *
     * @param virtualViewId The id of the virtual view on which to place
     *            accessibility focus.
     * @return Whether this virtual view actually took accessibility focus.
     */
    private fun requestAccessibilityFocus(virtualViewId: Int): Boolean {
        if (!accessibilityManager.isEnabled() ||
            !accessibilityManager.isTouchExplorationEnabled()) {
            return false
        }
        // TODO: Check virtual view visibility.
        if (!isAccessibilityFocused(virtualViewId)) {
            // Clear focus from the previously focused view, if applicable.
            if (focusedVirtualViewId != InvalidId) {
                sendEventForVirtualView(focusedVirtualViewId,
                    AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUS_CLEARED, null)
            }

            // Set focus on the new view.
            focusedVirtualViewId = virtualViewId

            view.invalidate()
            sendEventForVirtualView(virtualViewId,
                AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED, null)
            return true
        }
        return false
    }

    /**
     * Populates an event of the specified type with information about an item
     * and attempts to send it up through the view hierarchy.
     * <p>
     * You should call this method after performing a user action that normally
     * fires an accessibility event, such as clicking on an item.
     *
     * <pre>public void performItemClick(T item) {
     *   ...
     *   sendEventForVirtualViewId(item.id, AccessibilityEvent.TYPE_VIEW_CLICKED)
     * }
     * </pre>
     *
     * @param virtualViewId The virtual view id for which to send an event.
     * @param eventType The type of event to send.
     * @return true if the event was sent successfully.
     */
    fun sendEventForVirtualView(
        virtualViewId: Int,
        eventType: Int,
        contentChangeType: Int?
    ): Boolean {
        if ((virtualViewId == InvalidId) || !accessibilityManager.isEnabled()) {
            return false
        }

        val parent: ViewParent = view.parent

        val event: AccessibilityEvent = createEvent(virtualViewId, eventType)
        if (contentChangeType != null) {
            event.contentChangeTypes = contentChangeType
        }
        return parent.requestSendAccessibilityEvent(view, event)
    }

    /**
     * Constructs and returns an {@link AccessibilityEvent} populated with
     * information about the specified item.
     *
     * @param virtualViewId The virtual view id for the item for which to
     *            construct an event.
     * @param eventType The type of event to construct.
     * @return An {@link AccessibilityEvent} populated with information about
     *         the specified item.
     */
    private fun createEvent(virtualViewId: Int, eventType: Int): AccessibilityEvent {
        val event: AccessibilityEvent = AccessibilityEvent.obtain(eventType)
        event.isEnabled = true
        event.className = ClassName

        // Don't allow the client to override these properties.
        event.packageName = view.getContext().getPackageName()
        event.setSource(view, virtualViewId)

        return event
    }

    /**
     * Attempts to clear accessibility focus from a virtual view.
     *
     * @param virtualViewId The id of the virtual view from which to clear
     *            accessibility focus.
     * @return Whether this virtual view actually cleared accessibility focus.
     */
    private fun clearAccessibilityFocus(virtualViewId: Int): Boolean {
        if (isAccessibilityFocused(virtualViewId)) {
            focusedVirtualViewId = InvalidId
            view.invalidate()
            sendEventForVirtualView(virtualViewId,
                AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUS_CLEARED, null)
            return true
        }
        return false
    }

    fun performActionHelper(
        virtualViewId: Int,
        action: Int
    ): Boolean {
        var node: SemanticsNode
        if (virtualViewId == AccessibilityNodeProviderCompat.HOST_VIEW_ID) {
            node = view.semanticsOwner.rootSemanticsNode
        } else {
            node = view.semanticsOwner.rootSemanticsNode.findChildById(virtualViewId)
                ?: return false
        }
        when (action) {
            AccessibilityNodeInfoCompat.ACTION_ACCESSIBILITY_FOCUS ->
                return requestAccessibilityFocus(virtualViewId)
            AccessibilityNodeInfoCompat.ACTION_CLEAR_ACCESSIBILITY_FOCUS ->
                return clearAccessibilityFocus(virtualViewId)
            AccessibilityNodeInfoCompat.ACTION_CLICK -> {
                if (node.config.contains(SemanticsActions.OnClick)) {
                    node.config[SemanticsActions.OnClick].action()
                    return true
                } else {
                    return false
                }
            }
            else -> {
                // TODO: handling for other system and custom events
                return false
            }
        }
    }

    /**
     * Dispatches hover {@link android.view.MotionEvent}s to the virtual view hierarchy when
     * the Explore by Touch feature is enabled.
     * <p>
     * This method should be called by overriding
     * {@link View#dispatchHoverEvent}:
     *
     * <pre>&#64;Override
     * public boolean dispatchHoverEvent(MotionEvent event) {
     *   if (mHelper.dispatchHoverEvent(this, event) {
     *     return true;
     *   }
     *   return super.dispatchHoverEvent(event);
     * }
     * </pre>
     *
     * @param event The hover event to dispatch to the virtual view hierarchy.
     * @return Whether the hover event was handled.
     */
    fun dispatchHoverEvent(event: MotionEvent): Boolean {
        if (!accessibilityManager.isEnabled() ||
            !accessibilityManager.isTouchExplorationEnabled()) {
            return false
        }

        when (event.getAction()) {
            MotionEvent.ACTION_HOVER_MOVE, MotionEvent.ACTION_HOVER_ENTER -> {
                val virtualViewId: Int = getVirtualViewAt(event.getX(), event.getY())
                updateHoveredVirtualView(virtualViewId)
                return (virtualViewId != InvalidId)
            }
            MotionEvent.ACTION_HOVER_EXIT -> {
                if (hoveredVirtualViewId != InvalidId) {
                    updateHoveredVirtualView(InvalidId)
                    return true
                }
                return false
            }
            else -> {
                return false
            }
        }
    }

    private fun getVirtualViewAt(x: Float, y: Float): Int {
        var node = view.semanticsOwner.rootSemanticsNode
        var id = findVirtualViewAt(x + node.globalBounds.left.value,
            y + node.globalBounds.top.value, node)
        if (id == node.id) {
            return AccessibilityNodeProviderCompat.HOST_VIEW_ID
        }
        return id
    }

    // TODO(b/151729467): compose accessibility getVirtualViewAt needs to be more efficient
    private fun findVirtualViewAt(x: Float, y: Float, node: SemanticsNode): Int {
        node.children.forEach {
            var id = findVirtualViewAt(x, y, it)
            if (id != InvalidId) {
                return id
            }
        }

        if (node.globalBounds.left.value < x && node.globalBounds.right.value > x && node
                .globalBounds.top.value < y && node.globalBounds.bottom.value > y) {
            return node.id
        }

        return InvalidId
    }

    /**
     * Sets the currently hovered item, sending hover accessibility events as
     * necessary to maintain the correct state.
     *
     * @param virtualViewId The virtual view id for the item currently being
     *            hovered, or {@link #InvalidId} if no item is hovered within
     *            the parent view.
     */
    private fun updateHoveredVirtualView(virtualViewId: Int) {
        if (hoveredVirtualViewId == virtualViewId) {
            return
        }

        val previousVirtualViewId: Int = hoveredVirtualViewId
        hoveredVirtualViewId = virtualViewId

        /*
        Stay consistent with framework behavior by sending ENTER/EXIT pairs
        in reverse order. This is accurate as of API 18.
        */
        sendEventForVirtualView(virtualViewId, AccessibilityEvent.TYPE_VIEW_HOVER_ENTER, null)
        sendEventForVirtualView(previousVirtualViewId, AccessibilityEvent
            .TYPE_VIEW_HOVER_EXIT, null)
    }

    override fun getAccessibilityNodeProvider(host: View?): AccessibilityNodeProviderCompat {
        return nodeProvider
    }

    inner class MyNodeProvider() : AccessibilityNodeProviderCompat() {
        override fun createAccessibilityNodeInfo(virtualViewId: Int):
                AccessibilityNodeInfoCompat? {
            return createNodeInfo(virtualViewId)
        }

        override fun performAction(
            virtualViewId: Int,
            action: Int,
            arguments: Bundle?
        ): Boolean {
            return performActionHelper(virtualViewId, action)
        }
    }
}
