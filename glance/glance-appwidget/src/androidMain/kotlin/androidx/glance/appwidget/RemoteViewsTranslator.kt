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
import android.os.Build
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
import androidx.annotation.DoNotInline
import androidx.annotation.LayoutRes
import androidx.annotation.RequiresApi
import androidx.core.widget.setLinearLayoutGravity
import androidx.core.widget.setRelativeLayoutGravity
import androidx.glance.Emittable
import androidx.glance.GlanceInternalApi
import androidx.glance.appwidget.layout.EmittableLazyColumn
import androidx.glance.appwidget.layout.EmittableLazyListItem
import androidx.glance.layout.Alignment
import androidx.glance.layout.EmittableBox
import androidx.glance.layout.EmittableColumn
import androidx.glance.layout.EmittableRow
import androidx.glance.layout.EmittableText
import androidx.glance.layout.FontStyle
import androidx.glance.layout.FontWeight
import androidx.glance.layout.TextDecoration
import androidx.glance.layout.TextStyle

internal fun translateComposition(context: Context, appWidgetId: Int, element: RemoteViewsRoot) =
    translateComposition(TranslationContext(context, appWidgetId), element)

private fun translateComposition(
    translationContext: TranslationContext,
    element: RemoteViewsRoot
): RemoteViews {
    if (element.children.size == 1) {
        return translateChild(translationContext, element.children[0])
    }
    return translateChild(
        translationContext,
        EmittableBox().also { it.children.addAll(element.children) }
    )
}

internal data class TranslationContext(
    val context: Context,
    val appWidgetId: Int,
    var listCount: Int = 0
)

internal fun translateChild(
    translationContext: TranslationContext,
    element: Emittable
): RemoteViews {
    return when (element) {
        is EmittableBox -> translateEmittableBox(translationContext, element)
        is EmittableRow -> translateEmittableRow(translationContext, element)
        is EmittableColumn -> translateEmittableColumn(translationContext, element)
        is EmittableText -> translateEmittableText(translationContext, element)
        is EmittableLazyListItem -> translateEmittableLazyListItem(translationContext, element)
        is EmittableLazyColumn -> translateEmittableLazyColumn(translationContext, element)
        else -> throw IllegalArgumentException("Unknown element type ${element::javaClass}")
    }
}

internal fun remoteViews(translationContext: TranslationContext, @LayoutRes layoutId: Int) =
    RemoteViews(translationContext.context.packageName, layoutId)

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

private fun translateEmittableBox(
    translationContext: TranslationContext,
    element: EmittableBox
): RemoteViews =
    remoteViews(translationContext, R.layout.box_layout)
        .also { rv ->
            rv.setRelativeLayoutGravity(R.id.glanceView, element.contentAlignment.toGravity())
            applyModifiers(translationContext.context, rv, element.modifier)
            rv.setChildren(translationContext, R.id.glanceView, element.children)
        }

private fun translateEmittableRow(
    translationContext: TranslationContext,
    element: EmittableRow
): RemoteViews =
    remoteViews(translationContext, R.layout.row_layout)
        .also { rv ->
            rv.setLinearLayoutGravity(
                R.id.glanceView,
                element.horizontalAlignment.toGravity() or element.verticalAlignment.toGravity()
            )
            applyModifiers(translationContext.context, rv, element.modifier)
            rv.setChildren(translationContext, R.id.glanceView, element.children)
        }

private fun translateEmittableColumn(
    translationContext: TranslationContext,
    element: EmittableColumn
): RemoteViews =
    remoteViews(translationContext, R.layout.column_layout)
        .also { rv ->
            rv.setLinearLayoutGravity(
                R.id.glanceView,
                element.horizontalAlignment.toGravity() or element.verticalAlignment.toGravity()
            )
            applyModifiers(translationContext.context, rv, element.modifier)
            rv.setChildren(translationContext, R.id.glanceView, element.children)
        }

private fun translateEmittableText(
    translationContext: TranslationContext,
    element: EmittableText
): RemoteViews =
    remoteViews(translationContext, R.layout.text_layout)
        .also { rv ->
            rv.setText(translationContext.context, element.text, element.style)
            applyModifiers(translationContext.context, rv, element.modifier)
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

// Sets the emittables as children to the view. This first remove any previously added view, the
// add a view per child, with a stable id if of Android S+. Currently the stable id is the index
// of the child in the iterable.
internal fun RemoteViews.setChildren(
    translationContext: TranslationContext,
    viewId: Int,
    children: Iterable<Emittable>
) {
    removeAllViews(viewId)
    children.forEachIndexed { index, child ->
        addChildView(viewId, translateChild(translationContext, child), index)
    }
}

// Add stable view if on Android S+, otherwise simply add the view.
private fun RemoteViews.addChildView(viewId: Int, childView: RemoteViews, stableId: Int) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        Api31Impl.addChildView(this, viewId, childView, stableId)
        return
    }
    addView(viewId, childView)
}

@RequiresApi(Build.VERSION_CODES.S)
private object Api31Impl {
    @DoNotInline
    fun addChildView(rv: RemoteViews, viewId: Int, childView: RemoteViews, stableId: Int) {
        rv.addStableView(viewId, childView, stableId)
    }
}
