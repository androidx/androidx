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

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import androidx.web.WebContentView

/**
 * A headless [FrameLayout] that anchors a [WebContentView] into the View hierarchy for lifecycle,
 * insets, focus, IME, autofill, and accessibility, without Compose draw or pointer input overhead.
 */
@SuppressLint("ViewConstructor")
internal class HeadlessViewHolder(context: Context) : FrameLayout(context) {

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

    fun updateSize(width: Int, height: Int) {
        forceLayout()
        measure(
            MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY),
        )
        layout(0, 0, width, height)
    }

    // Touch input is handled directly by WebSurfaceNode.
    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean = true

    // Rendering is handled directly by WebSurfaceNode via Chromium's draw functor.
    override fun dispatchDraw(canvas: Canvas) {}

    // Suppress child invalidation so Chromium frame updates do not trigger Compose redraw passes.
    @SuppressLint("MissingSuperCall")
    override fun onDescendantInvalidated(child: View, target: View) {}

    @SuppressLint("MissingSuperCall")
    override fun requestLayout() {
        cleanupLayoutState(this)
    }
}
