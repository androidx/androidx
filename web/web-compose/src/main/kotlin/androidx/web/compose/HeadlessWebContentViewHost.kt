/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.web.compose

import android.accessibilityservice.AccessibilityServiceInfo
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.accessibility.AccessibilityManager
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityNodeProvider
import android.widget.FrameLayout
import androidx.annotation.RequiresApi
import androidx.compose.ui.graphics.toComposeRect
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.RootForTest
import androidx.compose.ui.relocation.bringIntoView
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.getAllSemanticsNodes
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.util.fastFirstOrNull
import androidx.web.WebContentView
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch

internal val WebSurfaceHolderKey = SemanticsPropertyKey<HeadlessViewHolder>("WebSurfaceHolder")

// TODO: Migrate remaining View-anchored integrations (IME InputConnection, Autofill virtual
// structure, and floating text-selection ActionMode positioning) to Compose / WebSurfaceChromium so
// HeadlessViewHolder is no longer needed.
@SuppressLint("ViewConstructor")
internal class HeadlessViewHolder(
    context: Context,
    private val node: DelegatableNode,
) : FrameLayout(context) {

    init {
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
        accessibilityDelegate =
            object : AccessibilityDelegate() {
                override fun onInitializeAccessibilityNodeInfo(
                    host: View,
                    info: AccessibilityNodeInfo,
                ) {
                    super.onInitializeAccessibilityNodeInfo(host, info)
                    // Prevent TalkBack from trying to focus the HeadlessViewHolder wrapper.
                    // Do not set this when no accessibility services are enabled so UIAutomator
                    // can still traverse through the holder.
                    if (hasEnabledAccessibilityService(host.context)) {
                        info.isVisibleToUser = false
                    }
                    val composeHost = parent as? View ?: return
                    val semanticsId = findSemanticsId(composeHost)
                    if (semanticsId != null) {
                        info.setParent(composeHost, semanticsId)
                    }
                }
            }
    }

    var view: WebContentView? = null
        set(value) {
            if (field === value) return
            field = value
            removeAllViews()
            if (value != null) {
                addView(value)
                updateSize(width, height)
            }
        }

    // Chromium positions anchor views for in-page popups (e.g. <datalist>, tablet <select>) via
    // layout params, so lay the holder subtree out locally instead of propagating the request to
    // the Compose host. Deferred to the next frame to coalesce requests and avoid re-entrancy.
    private val layoutLocally = Runnable { updateSize(width, height) }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        (parent as? View)?.let(::installHostAccessibilityDelegate)
    }

    fun updateSize(width: Int, height: Int) {
        forceLayout()
        measure(
            MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY),
        )
        layout(0, 0, width, height)
    }

    override fun requestFocus(direction: Int, previouslyFocusedRect: Rect?): Boolean =
        view?.hasFocus() == true

    override fun requestChildRectangleOnScreen(
        child: View,
        rectangle: Rect?,
        immediate: Boolean,
    ): Boolean {
        if (!node.node.isAttached) return false
        val composeRect =
            rectangle
                ?.toComposeRect()
                ?.translate(
                    (child.left - child.scrollX).toFloat(),
                    (child.top - child.scrollY).toFloat(),
                )
        // Start undispatched (matching FocusableNode) so the visibility check runs before any
        // subsequent scroll launched in the same frame, e.g. after View.onFocusChanged's
        // requestRectangleOnScreen on API 37+.
        node.node.coroutineScope.launch(start = CoroutineStart.UNDISPATCHED) {
            node.bringIntoView { composeRect }
        }
        return true
    }

    // Touch input is handled directly by WebSurfaceNode.
    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean = true

    /**
     * Whether [view] asked its parents not to intercept the current gesture, which Chromium does
     * when it handles a touch itself, e.g. a text selection drag. Reset by WebSurfaceNode at the
     * start of each gesture.
     */
    var disallowIntercept: Boolean = false

    override fun requestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {
        this.disallowIntercept = disallowIntercept
        super.requestDisallowInterceptTouchEvent(disallowIntercept)
    }

    // Key events are routed through Compose's KeyInputModifierNode on WebSurface.
    override fun dispatchKeyEvent(event: KeyEvent): Boolean = false

    fun dispatchKeyEventToView(event: KeyEvent): Boolean = super.dispatchKeyEvent(event)

    // Generic motion events are routed through Compose's PointerInputModifierNode on
    // WebSurfaceNode.
    override fun dispatchGenericMotionEvent(event: MotionEvent): Boolean = false

    fun dispatchGenericMotionEventToView(event: MotionEvent): Boolean =
        super.dispatchGenericMotionEvent(event)

    // Rendering is handled directly by WebSurfaceNode via Chromium's draw functor.
    override fun dispatchDraw(canvas: Canvas) {}

    // Suppress child invalidation so Chromium frame updates do not trigger Compose redraw passes.
    @SuppressLint("MissingSuperCall")
    override fun onDescendantInvalidated(child: View, target: View) {}

    @SuppressLint("MissingSuperCall")
    override fun requestLayout() {
        cleanupLayoutState(this)
        // Nothing to lay out before the first updateSize(). This also runs from the superclass
        // constructor, before layoutLocally is initialized.
        if (width == 0 || height == 0) return
        removeCallbacks(layoutLocally)
        postOnAnimation(layoutLocally)
    }

    private fun findSemanticsId(composeHost: View): Int? {
        val semanticsOwner = (composeHost as? RootForTest)?.semanticsOwner ?: return null
        return semanticsOwner
            .getAllSemanticsNodes(mergingEnabled = false)
            .fastFirstOrNull { it.config.getOrNull(WebSurfaceHolderKey) === this }
            ?.id
    }
}

private fun hasEnabledAccessibilityService(context: Context): Boolean {
    val accessibilityManager =
        context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
            ?: return false
    return accessibilityManager.isEnabled &&
        accessibilityManager
            .getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
            .isNotEmpty()
}

internal fun installHostAccessibilityDelegate(host: View) {
    val currentProvider = host.accessibilityNodeProvider
    if (currentProvider == null || currentProvider is WebSurfaceAccessibilityNodeProvider) {
        return
    }
    val provider = WebSurfaceAccessibilityNodeProvider(host, currentProvider)
    host.accessibilityDelegate =
        object : View.AccessibilityDelegate() {
            override fun getAccessibilityNodeProvider(host: View): AccessibilityNodeProvider =
                provider
        }
}

private class WebSurfaceAccessibilityNodeProvider(
    private val host: View,
    private val delegate: AccessibilityNodeProvider,
) : AccessibilityNodeProvider() {

    override fun createAccessibilityNodeInfo(virtualViewId: Int): AccessibilityNodeInfo? {
        val info = delegate.createAccessibilityNodeInfo(virtualViewId) ?: return null
        val semanticsOwner = (host as? RootForTest)?.semanticsOwner ?: return info
        val semanticsNode =
            semanticsOwner.getAllSemanticsNodes(mergingEnabled = false).fastFirstOrNull {
                it.id == virtualViewId
            }
        val holder = semanticsNode?.config?.getOrNull(WebSurfaceHolderKey)
        if (
            holder != null &&
                !semanticsNode.config.contains(SemanticsProperties.HideFromAccessibility)
        ) {
            info.addChild(holder)
        }
        return info
    }

    override fun addExtraDataToAccessibilityNodeInfo(
        virtualViewId: Int,
        info: AccessibilityNodeInfo,
        extraDataKey: String,
        arguments: Bundle?,
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Api26Impl.addExtraDataToAccessibilityNodeInfo(
                delegate,
                virtualViewId,
                info,
                extraDataKey,
                arguments,
            )
        }
    }

    override fun performAction(
        virtualViewId: Int,
        action: Int,
        arguments: Bundle?,
    ): Boolean = delegate.performAction(virtualViewId, action, arguments)

    override fun findAccessibilityNodeInfosByText(
        text: String,
        virtualViewId: Int,
    ): List<AccessibilityNodeInfo>? = delegate.findAccessibilityNodeInfosByText(text, virtualViewId)

    override fun findFocus(focus: Int): AccessibilityNodeInfo? = delegate.findFocus(focus)

    @RequiresApi(Build.VERSION_CODES.O)
    private object Api26Impl {
        fun addExtraDataToAccessibilityNodeInfo(
            delegate: AccessibilityNodeProvider,
            virtualViewId: Int,
            info: AccessibilityNodeInfo,
            extraDataKey: String,
            arguments: Bundle?,
        ) {
            delegate.addExtraDataToAccessibilityNodeInfo(
                virtualViewId,
                info,
                extraDataKey,
                arguments,
            )
        }
    }
}
