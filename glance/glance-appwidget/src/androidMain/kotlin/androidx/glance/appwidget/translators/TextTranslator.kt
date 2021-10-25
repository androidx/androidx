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

package androidx.glance.appwidget.translators

import android.graphics.Typeface
import android.os.Build
import android.text.Layout.Alignment
import android.text.ParcelableSpan
import android.text.SpannableString
import android.text.Spanned
import android.text.style.AlignmentSpan
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.text.style.TextAppearanceSpan
import android.text.style.UnderlineSpan
import android.util.TypedValue
import android.view.Gravity
import android.widget.RemoteViews
import androidx.annotation.DoNotInline
import androidx.annotation.RequiresApi
import androidx.core.widget.setTextViewGravity
import androidx.glance.appwidget.LayoutSelector
import androidx.glance.appwidget.R
import androidx.glance.appwidget.TranslationContext
import androidx.glance.appwidget.applyModifiers
import androidx.glance.appwidget.createRemoteViews
import androidx.glance.layout.EmittableText
import androidx.glance.text.FontStyle
import androidx.glance.text.FontWeight
import androidx.glance.text.TextAlign
import androidx.glance.text.TextDecoration
import androidx.glance.text.TextStyle

internal fun translateEmittableText(
    translationContext: TranslationContext,
    element: EmittableText
): RemoteViews {
    val layoutDef =
        createRemoteViews(translationContext, LayoutSelector.Type.Text, element.modifier)
    val rv = layoutDef.remoteViews
    rv.setText(
        translationContext,
        layoutDef.mainViewId,
        element.text,
        element.style
    )
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        element.style?.textAlign?.let { align ->
            TextTranslatorApi31Impl
                .setTextViewGravity(rv, layoutDef.mainViewId, align.toGravity())
        }
    }
    applyModifiers(translationContext, rv, element.modifier, layoutDef)
    return rv
}

internal fun RemoteViews.setText(
    translationContext: TranslationContext,
    resId: Int,
    text: String,
    style: TextStyle?
) {
    if (style == null) {
        setTextViewText(resId, text)
        return
    }
    val content = SpannableString(text)
    val length = content.length

    // TODO(b/203656358): Can we support Em here too?
    style.fontSize?.let {
        if (!it.isSp) {
            throw IllegalArgumentException("Only Sp is currently supported for font sizes")
        }
        setTextViewTextSize(resId, TypedValue.COMPLEX_UNIT_SP, it.value)
    }
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
        spans.add(TextAppearanceSpan(translationContext.context, textAppearance))
    }
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
        style.textAlign?.let { align ->
            spans.add(AlignmentSpan.Standard(align.toAlignment(translationContext.isRtl)))
        }
    }
    spans.forEach { span ->
        content.setSpan(span, 0, length, Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
    }
    setTextViewText(resId, content)
}

private fun TextAlign.toGravity(): Int =
    when (this) {
        TextAlign.Center -> Gravity.CENTER_HORIZONTAL
        TextAlign.Left -> Gravity.LEFT
        TextAlign.Right -> Gravity.RIGHT
        TextAlign.Start -> Gravity.START
        TextAlign.End -> Gravity.END
        else -> throw IllegalArgumentException("Unknown TextAlign: $this")
    }

private fun TextAlign.toAlignment(isRtl: Boolean): Alignment =
    when (this) {
        TextAlign.Center -> Alignment.ALIGN_CENTER
        TextAlign.Left -> if (isRtl) Alignment.ALIGN_OPPOSITE else Alignment.ALIGN_NORMAL
        TextAlign.Right -> if (isRtl) Alignment.ALIGN_NORMAL else Alignment.ALIGN_OPPOSITE
        TextAlign.Start -> Alignment.ALIGN_NORMAL
        TextAlign.End -> Alignment.ALIGN_OPPOSITE
        else -> throw IllegalArgumentException("Unknown TextAlign: $this")
    }

@RequiresApi(Build.VERSION_CODES.S)
private object TextTranslatorApi31Impl {
    @DoNotInline
    fun setTextViewGravity(rv: RemoteViews, viewId: Int, gravity: Int) {
        rv.setTextViewGravity(viewId, gravity)
    }
}