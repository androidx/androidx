@file:OptIn(GlanceInternalApi::class)
/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.glance.appwidget

import android.content.Context
import android.graphics.Typeface
import android.text.ParcelableSpan
import android.text.SpannableString
import android.text.Spanned
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.text.style.TextAppearanceSpan
import android.text.style.UnderlineSpan
import android.util.TypedValue
import android.view.Gravity
import android.widget.RemoteViews
import androidx.annotation.LayoutRes
import androidx.glance.Emittable
import androidx.glance.GlanceInternalApi
import androidx.glance.layout.Alignment
import androidx.glance.layout.EmittableBox
import androidx.glance.layout.EmittableText
import androidx.glance.layout.FontStyle
import androidx.glance.layout.FontWeight
import androidx.glance.layout.TextDecoration
import androidx.glance.layout.TextStyle

internal fun translateComposition(context: Context, element: RemoteViewsRoot): RemoteViews {
    if (element.children.size == 1) {
        return translateChild(context, element.children[0])
    }
    return translateChild(context, EmittableBox().also { it.children.addAll(element.children) })
}

private fun translateChild(context: Context, element: Emittable): RemoteViews {
    return when (element) {
        is EmittableBox -> translateEmittableBox(context, element)
        is EmittableText -> translateEmittableText(context, element)
        else -> throw IllegalArgumentException("Unknown element type ${element::javaClass}")
    }
}

private fun remoteViews(context: Context, @LayoutRes layoutId: Int) =
    RemoteViews(context.packageName, layoutId)

private fun Alignment.Horizontal.toGravity(): Int =
    when (this) {
        Alignment.Horizontal.Start -> Gravity.START
        Alignment.Horizontal.End -> Gravity.END
        Alignment.Horizontal.CenterHorizontally -> Gravity.CENTER_HORIZONTAL
        else -> throw IllegalArgumentException("Unknown horizontal alignment: $this")
    }

private fun Alignment.Vertical.toGravity(): Int =
    when (this) {
        Alignment.Vertical.Top -> Gravity.TOP
        Alignment.Vertical.Bottom -> Gravity.BOTTOM
        Alignment.Vertical.CenterVertically -> Gravity.CENTER_VERTICAL
        else -> throw IllegalArgumentException("Unknown vertical alignment: $this")
    }

private fun Alignment.toGravity() = horizontal.toGravity() or vertical.toGravity()

private fun translateEmittableBox(context: Context, element: EmittableBox): RemoteViews =
    remoteViews(context, R.layout.box_layout)
        .also { rv ->
            rv.setInt(R.id.glanceView, "setGravity", element.contentAlignment.toGravity())
            applyModifiers(context, rv, element.modifier)
            element.children.forEach {
                rv.addView(R.id.glanceView, translateChild(context, it))
            }
        }

private fun translateEmittableText(context: Context, element: EmittableText): RemoteViews =
    remoteViews(context, R.layout.text_layout)
        .also { rv ->
            rv.setText(context, element.text, element.style)
            applyModifiers(context, rv, element.modifier)
        }

private fun RemoteViews.setText(context: Context, text: String, style: TextStyle?) {
    if (style == null) {
        setTextViewText(R.id.glanceView, text)
        return
    }
    val content = SpannableString(text)
    val length = content.length
    style.size?.let { setTextViewTextSize(R.id.glanceView, TypedValue.COMPLEX_UNIT_SP, it.value) }
    val spans = mutableListOf<ParcelableSpan>()
    style.textDecoration?.let {
        if (TextDecoration.LineThrough in it) {
            spans.add(StrikethroughSpan())
        }
        if (TextDecoration.Underline in it) {
            spans.add(UnderlineSpan())
        }
    }
    style.fontStyle?.let {
        spans.add(StyleSpan(if (it == FontStyle.Italic) Typeface.ITALIC else Typeface.NORMAL))
    }
    style.fontWeight?.let {
        val textAppearance = when (it) {
            FontWeight.Bold -> R.style.TextAppearance_Bold
            FontWeight.Medium -> R.style.TextAppearance_Medium
            else -> R.style.TextAppearance_Normal
        }
        spans.add(TextAppearanceSpan(context, textAppearance))
    }
    spans.forEach { span ->
        content.setSpan(span, 0, length, Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
    }
    setTextViewText(R.id.glanceView, content)
}
