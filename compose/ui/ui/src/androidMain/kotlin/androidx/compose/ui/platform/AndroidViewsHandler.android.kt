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
package androidx.compose.ui.platform

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import android.view.MotionEvent
import android.view.View
import android.view.View.MeasureSpec.EXACTLY
import android.view.View.MeasureSpec.getMode
import android.view.ViewGroup
import androidx.compose.ui.internal.requirePrecondition
import androidx.compose.ui.node.LayoutNode
import androidx.compose.ui.viewinterop.AndroidViewHolder

/**
 * Used by [AndroidComposeView] to handle the Android [View]s attached to its hierarchy.
 * The [AndroidComposeView] has one direct [AndroidViewsHandler], which is responsible
 * of intercepting [requestLayout]s, [onMeasure]s, [invalidate]s, etc. sent from or towards
 * children.
 */
internal class AndroidViewsHandler(context: Context) : ViewGroup(context) {
    init {
        clipChildren = false
    }

    val holderToLayoutNode = hashMapOf<AndroidViewHolder, LayoutNode>()
    val layoutNodeToHolder = hashMapOf<LayoutNode, AndroidViewHolder>()

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // Layout will be handled by component nodes. However, we act like proper measurement
        // here in case ViewRootImpl did forceLayout().
        requirePrecondition(getMode(widthMeasureSpec) == EXACTLY) {
            "widthMeasureSpec should be EXACTLY"
        }
        requirePrecondition(getMode(heightMeasureSpec) == EXACTLY) {
            "heightMeasureSpec should be EXACTLY"
        }
        setMeasuredDimension(
            MeasureSpec.getSize(widthMeasureSpec),
            MeasureSpec.getSize(heightMeasureSpec)
        )
        // Remeasure children, such that, if ViewRootImpl did forceLayout(), the holders
        // will be set PFLAG_LAYOUT_REQUIRED and they will be relaid out during the next layout.
        // This will ensure that the need relayout flags will be cleared correctly.
        holderToLayoutNode.keys.forEach { it.remeasure() }
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        // Layout was already handled by component nodes, but replace here because
        // the View system has forced relayout on children. This method will only be called
        // when forceLayout is called on the Views hierarchy.
        holderToLayoutNode.keys.forEach { it.layout(it.left, it.top, it.right, it.bottom) }
    }

    // No call to super to avoid invalidating the AndroidComposeView and the handler, and rely on
    // component nodes logic. The layer invalidation will have been already done by the holder.
    @SuppressLint("MissingSuperCall")
    override fun onDescendantInvalidated(child: View, target: View) { }

    override fun invalidateChildInParent(location: IntArray?, dirty: Rect?) = null

    fun drawView(view: AndroidViewHolder, canvas: Canvas) {
        view.draw(canvas)
    }

    // Touch events forwarding will be handled by component nodes.
    override fun dispatchTouchEvent(ev: MotionEvent?) = true

    // No call to super to avoid invalidating the AndroidComposeView and rely on
    // component nodes logic.
    @SuppressLint("MissingSuperCall")
    override fun requestLayout() {
        // Hack to cleanup the dirty layout flag on ourselves, such that this method continues
        // to be called for further children requestLayout().
        cleanupLayoutState(this)
        // requestLayout() was called by a child, so we have to request remeasurement for
        // their corresponding layout node.
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            val node = holderToLayoutNode[child]
            if (child.isLayoutRequested && node != null) {
                node.requestRemeasure()
            }
        }
    }

    override fun shouldDelayChildPressedState(): Boolean = false

    // We don't want the AndroidComposeView drawing the holder and its children. All draw
    // calls should come through AndroidViewHolder or ViewLayer.
    override fun dispatchDraw(canvas: Canvas) {
    }
}
