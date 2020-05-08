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

package android.view

import android.content.Context
import android.graphics.Canvas
import android.util.LayoutDirection

open class View(val context: Context) {
    companion object {
        @JvmField
        val LAYOUT_DIRECTION_LTR = LayoutDirection.LTR
        @JvmField
        val LAYOUT_DIRECTION_RTL = LayoutDirection.RTL
    }

    open class AccessibilityDelegate()

    interface OnApplyWindowInsetsListener {
        fun onApplyWindowInsets(v: View, insets: WindowInsets): WindowInsets
    }

    interface OnUnhandledKeyEventListener {
        fun onUnhandledKeyEvent(v: View, event: KeyEvent): Boolean
    }

    interface OnAttachStateChangeListener {
        fun onViewAttachedToWindow(v: View)
        fun onViewDetachedFromWindow(v: View)
    }

    var willNotDraw: Boolean = false
    var focusable: Boolean = true
    var focusableInTouchMode: Boolean = true

    var accessibilityDelegate: AccessibilityDelegate? = null

    fun requestLayout() {}

    fun invalidate() {}

    open fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {}

    open fun onAttachedToWindow() {}

    open fun dispatchDraw(canvas: Canvas) {}

    fun setMeasuredDimension(measuredWidth: Int, measuredHeight: Int) {}

    fun getTag(key: Int): Any? = null
    fun setTag(key: Int, obj: Any?) {}

    fun getParent(): ViewParent? = null

    class MeasureSpec {
        companion object {
            @JvmStatic
            fun getMode(measureSpec: Int): Int {
                return 1073741824 // EXACTLY
            }

            @JvmStatic
            fun getSize(measureSpec: Int): Int {
                return measureSpec
            }
        }
    }
}
