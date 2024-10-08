/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.compose.ui.text.platform

import android.graphics.Paint
import android.graphics.Typeface
import android.view.View
import androidx.compose.runtime.State
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.EmojiSupportMatch
import androidx.compose.ui.text.ParagraphIntrinsics
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.android.LayoutCompat
import androidx.compose.ui.text.android.LayoutIntrinsics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontSynthesis
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.TypefaceResult
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.platform.extensions.applySpanStyle
import androidx.compose.ui.text.platform.extensions.setTextMotion
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.Density
import androidx.compose.ui.util.fastFirstOrNull
import androidx.core.text.TextUtilsCompat
import java.util.Locale

internal class AndroidParagraphIntrinsics(
    val text: String,
    val style: TextStyle,
    val annotations: List<AnnotatedString.Range<out AnnotatedString.Annotation>>,
    val placeholders: List<AnnotatedString.Range<Placeholder>>,
    val fontFamilyResolver: FontFamily.Resolver,
    val density: Density
) : ParagraphIntrinsics {

    internal val textPaint = AndroidTextPaint(Paint.ANTI_ALIAS_FLAG, density.density)

    internal val charSequence: CharSequence

    internal val layoutIntrinsics: LayoutIntrinsics

    override val maxIntrinsicWidth: Float
        get() = layoutIntrinsics.maxIntrinsicWidth

    override val minIntrinsicWidth: Float
        get() = layoutIntrinsics.minIntrinsicWidth

    private var resolvedTypefaces: TypefaceDirtyTrackerLinkedList? = null

    /**
     * If emojiCompat is used in the making of this Paragraph
     *
     * This value will never change
     */
    private val emojiCompatProcessed: Boolean =
        if (!style.hasEmojiCompat) {
            false
        } else {
            EmojiCompatStatus.fontLoaded.value
        }

    override val hasStaleResolvedFonts: Boolean
        get() =
            (resolvedTypefaces?.isStaleResolvedFont ?: false) ||
                (!emojiCompatProcessed &&
                    style.hasEmojiCompat &&
                    /* short-circuit this state read */ EmojiCompatStatus.fontLoaded.value)

    internal val textDirectionHeuristic =
        resolveTextDirectionHeuristics(style.textDirection, style.localeList)

    init {
        val resolveTypeface: (FontFamily?, FontWeight, FontStyle, FontSynthesis) -> Typeface =
            { fontFamily, fontWeight, fontStyle, fontSynthesis ->
                val result =
                    fontFamilyResolver.resolve(fontFamily, fontWeight, fontStyle, fontSynthesis)
                if (result !is TypefaceResult.Immutable) {
                    val newHead = TypefaceDirtyTrackerLinkedList(result, resolvedTypefaces)
                    resolvedTypefaces = newHead
                    newHead.typeface
                } else {
                    result.value as Typeface
                }
            }

        textPaint.setTextMotion(style.textMotion)

        val notAppliedStyle =
            textPaint.applySpanStyle(
                style = style.toSpanStyle(),
                resolveTypeface = resolveTypeface,
                density = density,
                requiresLetterSpacing =
                    annotations.fastFirstOrNull { it.item is SpanStyle } != null,
            )

        val finalSpanStyles =
            if (notAppliedStyle != null) {
                // This is just a prepend operation, written in a lower alloc way
                // equivalent to: `AnnotatedString.Range(...) + spanStyles`
                List(annotations.size + 1) { position ->
                    when (position) {
                        0 ->
                            AnnotatedString.Range(
                                item = notAppliedStyle,
                                start = 0,
                                end = text.length
                            )
                        else -> annotations[position - 1]
                    }
                }
            } else {
                annotations
            }
        charSequence =
            createCharSequence(
                text = text,
                contextFontSize = textPaint.textSize,
                contextTextStyle = style,
                annotations = finalSpanStyles,
                placeholders = placeholders,
                density = density,
                resolveTypeface = resolveTypeface,
                useEmojiCompat = emojiCompatProcessed
            )

        layoutIntrinsics = LayoutIntrinsics(charSequence, textPaint, textDirectionHeuristic)
    }
}

/**
 * For a given [TextDirection] return [androidx.compose.ui.text.android.TextLayout] constants for
 * text direction heuristics.
 */
internal fun resolveTextDirectionHeuristics(
    textDirection: TextDirection,
    localeList: LocaleList? = null
): Int {
    return when (textDirection) {
        TextDirection.ContentOrLtr -> LayoutCompat.TEXT_DIRECTION_FIRST_STRONG_LTR
        TextDirection.ContentOrRtl -> LayoutCompat.TEXT_DIRECTION_FIRST_STRONG_RTL
        TextDirection.Ltr -> LayoutCompat.TEXT_DIRECTION_LTR
        TextDirection.Rtl -> LayoutCompat.TEXT_DIRECTION_RTL
        TextDirection.Content,
        TextDirection.Unspecified -> {
            val currentLocale = localeList?.let { it[0].platformLocale } ?: Locale.getDefault()
            when (TextUtilsCompat.getLayoutDirectionFromLocale(currentLocale)) {
                View.LAYOUT_DIRECTION_LTR -> LayoutCompat.TEXT_DIRECTION_FIRST_STRONG_LTR
                View.LAYOUT_DIRECTION_RTL -> LayoutCompat.TEXT_DIRECTION_FIRST_STRONG_RTL
                else -> LayoutCompat.TEXT_DIRECTION_FIRST_STRONG_LTR
            }
        }
        else -> error("Invalid TextDirection.")
    }
}

internal actual fun ActualParagraphIntrinsics(
    text: String,
    style: TextStyle,
    annotations: List<AnnotatedString.Range<out AnnotatedString.Annotation>>,
    placeholders: List<AnnotatedString.Range<Placeholder>>,
    density: Density,
    fontFamilyResolver: FontFamily.Resolver
): ParagraphIntrinsics =
    AndroidParagraphIntrinsics(
        text = text,
        style = style,
        placeholders = placeholders,
        fontFamilyResolver = fontFamilyResolver,
        annotations = annotations,
        density = density
    )

private class TypefaceDirtyTrackerLinkedList(
    private val resolveResult: State<Any>,
    private val next: TypefaceDirtyTrackerLinkedList? = null
) {
    val initial = resolveResult.value
    val typeface: Typeface
        get() = initial as Typeface

    val isStaleResolvedFont: Boolean
        get() = resolveResult.value !== initial || (next != null && next.isStaleResolvedFont)
}

private val TextStyle.hasEmojiCompat: Boolean
    get() = platformStyle?.paragraphStyle?.emojiSupportMatch != EmojiSupportMatch.None
