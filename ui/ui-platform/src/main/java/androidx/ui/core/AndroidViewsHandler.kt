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

import android.annotation.SuppressLint
import android.content.Context
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewParent

/**
 * Used by [AndroidComposeView] to handle the Android [View]s attached to its hierarchy.
 * The [AndroidComposeView] has one direct [AndroidViewsHandler], which is responsible
 * of intercepting [requestLayout]s, [onMeasure]s, [invalidate]s, etc. sent from or towards
 * children.
 */
internal class AndroidViewsHandler(context: Context) : ViewGroup(context) {
    val layoutNode = hashMapOf<View, LayoutNode>()

    // No call to super to avoid invalidating the AndroidComposeView and rely on
    // component nodes logic.
    @SuppressLint("MissingSuperCall")
    override fun onDescendantInvalidated(child: View, target: View) {
        layoutNode[child]!!.onInvalidate()
    }

    // No call to super to avoid invalidating the AndroidComposeView and rely on
    // component nodes logic.
    override fun invalidateChildInParent(
        location: IntArray?,
        dirty: android.graphics.Rect?
    ): ViewParent? = null

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // Layout will be handled by component nodes.
        setMeasuredDimension(0, 0)
    }

    override fun measureChildren(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // Layout will be handled by component nodes.
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        // Layout will be handled by component nodes.
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
            if (child.isLayoutRequested && layoutNode[child]?.needsRemeasure == false) {
                layoutNode[child]!!.requestRemeasure()
            }
        }
    }
}
