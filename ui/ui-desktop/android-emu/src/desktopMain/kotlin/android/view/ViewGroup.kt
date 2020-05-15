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
import android.graphics.Region
import android.graphics.Outline
import android.graphics.Rect
import org.jetbrains.skija.RoundedRect

abstract class ViewGroup(context: Context) : View(context), ViewParent {
    var clipChildren: Boolean = true
    var children = mutableListOf<View>()
    val childCount = children.count()

    fun getChildAt(i: Int) = children[i]

    fun removeAllViews() {
        children.clear()
    }

    fun removeView(view: View) {
        view.parent = null
        children.remove(view)
    }

    fun addView(child: android.view.View) {
        addView(child, null)
    }

    fun addView(child: android.view.View, params: ViewGroup.LayoutParams?) {
        child.parent = this
        children.add(child)
    }

    override fun requestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {}

    fun drawChild(canvas: Canvas, view: View, drawingTime: Long): Boolean {
        canvas.save()
        canvas.translate(
            view.left.toFloat() + view.translationX,
            view.top.toFloat() + view.translationY
        )
        if (view.clipToBounds && view.clipBounds != null) {
            canvas.clipRect(view.clipBounds!!)
        }
        if (view.clipToOutline) {
            val outline = Outline()
            view.outlineProvider?.getOutline(view, outline)
            canvas.clipOutline(outline)
        }
        if (view.scaleX != 1f || view.scaleY != 1f) {
            canvas.scale(view.scaleX, view.scaleY)
        }

        view.dispatchDraw(canvas)
        canvas.restore()
        return true
    }

    private fun Canvas.clipRect(rect: Rect) {
        clipRect(
            rect.left.toFloat(),
            rect.top.toFloat(),
            rect.right.toFloat(),
            rect.bottom.toFloat(),
            Region.Op.INTERSECT
        )
    }

    private fun Canvas.clipOutline(outline: Outline) {
        val rect = outline.rect
        val radius = outline.radius
        val path = outline.path
        if (path != null) {
            clipPath(path, Region.Op.INTERSECT)
        } else if (radius != null && rect != null) {
            skijaCanvas!!.clipRoundedRect(
                RoundedRect.makeLTRB(
                    rect.left.toFloat(),
                    rect.top.toFloat(),
                    rect.right.toFloat(),
                    rect.bottom.toFloat(),
                    radius,
                    radius
                )
            )
        } else if (rect != null) {
            clipRect(rect)
        }
    }

    class LayoutParams(width: Int, height: Int) {
        companion object {
            const val WRAP_CONTENT = 0
        }
    }
}
