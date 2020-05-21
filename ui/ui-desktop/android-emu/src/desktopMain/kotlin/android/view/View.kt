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
import android.graphics.Matrix
import android.graphics.Rect
import android.os.Handler
import android.util.LayoutDirection

import javax.swing.SwingUtilities

open class View(val context: Context) {
    companion object {
        @JvmField
        val LAYOUT_DIRECTION_LTR = LayoutDirection.LTR
        @JvmField
        val LAYOUT_DIRECTION_RTL = LayoutDirection.RTL

        @JvmStatic
        fun generateViewId(): Int {
            // TODO: atomic in Android.
            return sNextGeneratedId++
        }

        var sNextGeneratedId: Int = 1
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

    fun requestFocus(): Boolean {
        return true
    }

    fun requestRectangleOnScreen(rectangle: Rect): Boolean {
        return false
    }

    open fun invalidate() {}

    open fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {}

    open fun onLayout (changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {}

    open fun onAttachedToWindow() {}

    open fun dispatchDraw(canvas: Canvas) {}

    open fun dispatchTouchEvent(event: MotionEvent): Boolean = true

    open fun getLocationOnScreen(outLocation: IntArray) {
        outLocation[0] = 0
        outLocation[1] = 0
    }

    fun setMeasuredDimension(measuredWidth: Int, measuredHeight: Int) {}

    fun getTag(key: Int): Any? = null
    fun setTag(key: Int, obj: Any?) {}

    var parent: ViewParent? = null

    class MeasureSpec {
        companion object {
            @JvmStatic
            fun getMode(measureSpec: Int): Int {
                return -2147483648 // AT_MOST
            }

            @JvmStatic
            fun getSize(measureSpec: Int): Int {
                return measureSpec
            }
        }
    }

    var id: Int = 0

    var alpha: Float = 1.0f
    var scaleX: Float = 1.0f
    var scaleY: Float = 1.0f
    var translationX: Float = 0.0f
    var translationY: Float = 0.0f
    var elevation: Float = 0.0f
    var rotation: Float = 0.0f
    var rotationX: Float = 0.0f
    var rotationY: Float = 0.0f
    var pivotX: Float = 0.0f
    var pivotY: Float = 0.0f

    var left: Int = 0
    var top: Int = 0
    var right: Int = 0
    var bottom: Int = 0
    val width: Int get() = right - left
    val height: Int get() = bottom - top

    var clipToBounds: Boolean = false
    var clipBounds: Rect? = null

    var clipToOutline: Boolean = false

    var outlineProvider: ViewOutlineProvider? = null

    var drawingTime: Long = 0

    var mRecreateDisplayList: Boolean = false

    fun layout(left: Int, top: Int, right: Int, bottom: Int) {
        this.left = left
        this.top = top
        this.right = right
        this.bottom = bottom
    }

    fun updateDisplayListIfDirty() {
        println("View.updateDisplayListIfDirty")
    }

    fun getMatrix(): Matrix {
        return Matrix()
    }

    val handler = Handler()

    open fun post(runnable: Runnable): Boolean {
        SwingUtilities.invokeLater {
            runnable.run()
        }
        return true
    }
}
