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
import android.text.style.TypefaceSpan
import android.text.style.UnderlineSpan
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.widget.RemoteViews
import androidx.annotation.DoNotInline
import androidx.annotation.RequiresApi
import androidx.compose.ui.graphics.toArgb
import androidx.core.widget.RemoteViewsCompat.setTextViewGravity
import androidx.core.widget.RemoteViewsCompat.setTextViewMaxLines
import androidx.core.widget.RemoteViewsCompat.setTextViewTextColor
import androidx.core.widget.RemoteViewsCompat.setTextViewTextColorResource
import androidx.glance.appwidget.GlanceAppWidgetTag
import androidx.glance.appwidget.LayoutType
import androidx.glance.appwidget.R
import androidx.glance.appwidget.TranslationContext
import androidx.glance.appwidget.applyModifiers
import androidx.glance.appwidget.insertView
import androidx.glance.color.DayNightColorProvider
import androidx.glance.text.EmittableText
import androidx.glance.text.FontStyle
import androidx.glance.text.FontWeight
import androidx.glance.text.TextAlign
import androidx.glance.text.TextDecoration
import androidx.glance.text.TextStyle
import androidx.glance.unit.FixedColorProvider
import androidx.glance.unit.ResourceColorProvider

internal fun RemoteViews.translateEmittableText(
    translationContext: TranslationContext,
    element: EmittableText
) {
    val viewDef = insertView(translationContext, LayoutType.Text, element.modifier)
    setText(
        translationContext,
        viewDef.mainViewId,
        element.text,
        element.style,
        maxLines = element.maxLines,
    )
    applyModifiers(translationContext, this, element.modifier, viewDef)
}

internal fun RemoteViews.setText(
    translationContext: TranslationContext,
    resId: Int,
    text: String,
    style: TextStyle?,
    maxLines: Int,
    verticalTextGravity: Int = Gravity.TOP,
) {
    if (maxLines != Int.MAX_VALUE) {
        setTextViewMaxLines(resId, maxLines)
    }

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
            FontWeight.Bold -> R.style.Glance_AppWidget_TextAppearance_Bold
            FontWeight.Medium -> R.style.Glance_AppWidget_TextAppearance_Medium
            else -> R.style.Glance_AppWidget_TextAppearance_Normal
        }
        spans.add(TextAppearanceSpan(translationContext.context, textAppearance))
    }
    style.fontFamily?.let { family ->
        spans.add(TypefaceSpan(family.family))
    }
    style.textAlign?.let { align ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            TextTranslatorApi31Impl.setTextViewGravity(
                this,
                resId,
                align.toGravity() or verticalTextGravity
            )
        } else {
            spans.add(AlignmentSpan.Standard(align.toAlignment(translationContext.isRtl)))
        }
    }
    spans.forEach { span ->
        content.setSpan(span, 0, length, Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
    }
    setTextViewText(resId, content)

    when (val colorProvider = style.color) {
        is FixedColorProvider -> setTextColor(resId, colorProvider.color.toArgb())
        is ResourceColorProvider -> {
            if (Build.VERSION.SDK_INT >= 31) {
                setTextViewTextColorResource(resId, colorProvider.resId)
            } else {
                setTextColor(resId, colorProvider.getColor(translationContext.context).toArgb())
            }
        }

        is DayNightColorProvider -> {
            if (Build.VERSION.SDK_INT >= 31) {
                setTextViewTextColor(
                    resId,
                    notNight = colorProvider.day.toArgb(),
                    night = colorProvider.night.toArgb()
                )
            } else {
                setTextColor(resId, colorProvider.getColor(translationContext.context).toArgb())
            }
        }

        else -> Log.w(GlanceAppWidgetTag, "Unexpected text color: $colorProvider")
    }
}

private fun TextAlign.toGravity(): Int =
    when (this) {
        TextAlign.Center -> Gravity.CENTER_HORIZONTAL
        TextAlign.Left -> Gravity.LEFT
        TextAlign.Right -> Gravity.RIGHT
        TextAlign.Start -> Gravity.START
        TextAlign.End -> Gravity.END
        else -> {
            Log.w(GlanceAppWidgetTag, "Unknown TextAlign: $this")
            Gravity.START
        }
    }

private fun TextAlign.toAlignment(isRtl: Boolean): Alignment =
    when (this) {
        TextAlign.Center -> Alignment.ALIGN_CENTER
        TextAlign.Left -> if (isRtl) Alignment.ALIGN_OPPOSITE else Alignment.ALIGN_NORMAL
        TextAlign.Right -> if (isRtl) Alignment.ALIGN_NORMAL else Alignment.ALIGN_OPPOSITE
        TextAlign.Start -> Alignment.ALIGN_NORMAL
        TextAlign.End -> Alignment.ALIGN_OPPOSITE
        else -> {
            Log.w(GlanceAppWidgetTag, "Unknown TextAlign: $this")
            Alignment.ALIGN_NORMAL
        }
    }

@RequiresApi(Build.VERSION_CODES.S)
private object TextTranslatorApi31Impl {
    @DoNotInline
    fun setTextViewGravity(rv: RemoteViews, viewId: Int, gravity: Int) {
        rv.setTextViewGravity(viewId, gravity)
    }
}
