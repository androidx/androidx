/*
 * Copyright (C) 2021 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package app.cash.paparazzi.accessibility

import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import app.cash.paparazzi.RenderExtension
import app.cash.paparazzi.accessibility.RenderSettings.DEFAULT_DESCRIPTION_BACKGROUND_COLOR
import app.cash.paparazzi.accessibility.RenderSettings.DEFAULT_RECT_SIZE
import app.cash.paparazzi.accessibility.RenderSettings.DEFAULT_RENDER_ALPHA
import app.cash.paparazzi.accessibility.RenderSettings.DEFAULT_TEXT_COLOR
import app.cash.paparazzi.accessibility.RenderSettings.DEFAULT_TEXT_SIZE
import app.cash.paparazzi.accessibility.RenderSettings.getColor
import app.cash.paparazzi.accessibility.RenderSettings.toColorInt
import app.cash.paparazzi.accessibility.RenderSettings.withAlpha

class AccessibilityRenderExtension : RenderExtension {
  override fun renderView(
    contentView: View
  ): View {
    val accessibilityViews = contentView.findAccessibilityViews()
    accessibilityViews.forEach { view ->
      val color = getColor(view)
      val colorInt = color.toColorInt()

      val colorDrawable = GradientDrawable(
        GradientDrawable.Orientation.TOP_BOTTOM,
        intArrayOf(colorInt, colorInt)
      ).apply {
        setStroke(2, color.withAlpha(DEFAULT_RENDER_ALPHA * 2).toColorInt())
      }

      view.foreground = view.foreground?.let { drawable ->
        // If there is an existing foreground layer the color on top of it.
        LayerDrawable(arrayOf(drawable, colorDrawable))
      } ?: colorDrawable
    }

    return LinearLayout(contentView.context).apply {
      orientation = LinearLayout.HORIZONTAL
      weightSum = 2f
      layoutParams = ViewGroup.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.MATCH_PARENT
      )

      val contentLayoutParams = contentView.layoutParams ?: generateLayoutParams(null)
      addView(
        contentView,
        LinearLayout.LayoutParams(
          contentLayoutParams.width,
          contentLayoutParams.height,
          1f
        )
      )
      addView(
        buildAccessibilityView(contentView),
        LinearLayout.LayoutParams(
          ViewGroup.LayoutParams.MATCH_PARENT,
          ViewGroup.LayoutParams.MATCH_PARENT,
          1f
        )
      )
    }
  }

  private fun View.findAccessibilityViews(): List<View> {
    val accessibilityViews = mutableListOf<View>()
    if (isImportantForAccessibility && !iterableTextForAccessibility.isNullOrBlank()) {
      accessibilityViews.add(this)
    }

    if (this is ViewGroup) {
      (0 until childCount).forEach {
        accessibilityViews += getChildAt(it).findAccessibilityViews()
      }
    }

    return accessibilityViews
  }

  private fun buildAccessibilityView(contentView: View): View {
    val linearLayout = LinearLayout(contentView.context).apply {
      orientation = LinearLayout.VERTICAL
      setBackgroundColor(DEFAULT_DESCRIPTION_BACKGROUND_COLOR.toColorInt())
    }

    fun renderAccessibility(view: View) {
      if (view.isImportantForAccessibility && !view.iterableTextForAccessibility.isNullOrBlank()) {
        linearLayout.addView(buildAccessibilityRow(view, view.iterableTextForAccessibility))
      }

      if (view is ViewGroup) {
        (0 until view.childCount).forEach {
          renderAccessibility(view.getChildAt(it))
        }
      }
    }

    renderAccessibility(contentView)
    return linearLayout
  }

  private fun buildAccessibilityRow(view: View, iterableTextForAccessibility: CharSequence): View {
    val context = view.context
    val color = getColor(view).toColorInt()
    val margin = view.dip(8)
    val innerMargin = view.dip(4)

    return LinearLayout(context).apply {
      orientation = LinearLayout.HORIZONTAL
      layoutParams = ViewGroup.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
      )
      setPaddingRelative(margin, innerMargin, margin, innerMargin)

      addView(
        View(context).apply {
          layoutParams = ViewGroup.LayoutParams(dip(DEFAULT_RECT_SIZE), dip(DEFAULT_RECT_SIZE))
          background = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(color, color)
          ).apply {
            cornerRadius = dip(DEFAULT_RECT_SIZE / 4f)
          }
          setPaddingRelative(innerMargin, innerMargin, innerMargin, innerMargin)
        }
      )
      addView(
        TextView(context).apply {
          layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
          )
          text = iterableTextForAccessibility
          textSize = DEFAULT_TEXT_SIZE
          setTextColor(DEFAULT_TEXT_COLOR.toColorInt())
          setPaddingRelative(innerMargin, 0, innerMargin, 0)
        }
      )
    }
  }
}

private fun View.dip(value: Float): Float =
  TypedValue.applyDimension(
    TypedValue.COMPLEX_UNIT_DIP,
    value,
    resources.displayMetrics
  )

private fun View.dip(value: Int): Int = dip(value.toFloat()).toInt()
