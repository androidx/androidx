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

@file:Suppress("Deprecation")

package androidx.compose.ui.text

import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.isUnspecified
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontSynthesis
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextGeometricTransform
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.text.style.TextMotion
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.util.fastFilteredMap
import androidx.compose.ui.util.fastMap
import kotlin.jvm.JvmField

/**
 * Utility function to be able to save nullable values. It also enables not to use with() scope for
 * readability/syntactic purposes.
 */
internal fun <T : Saver<Original, Saveable>, Original, Saveable : Any> save(
    value: Original?,
    saver: T,
    scope: SaverScope,
): Any {
    return value?.let { with(saver) { scope.save(value) } } ?: false
}

/**
 * Utility function to restore nullable values. It also enables not to use with() scope for
 * readability/syntactic purposes.
 */
internal inline fun <
    T : Saver<Original, Saveable>,
    Original,
    Saveable : Any,
    reified Result,
> restore(value: Saveable?, saver: T): Result? {
    // Most of the types we save are nullable. However, value classes are usually not but instead
    // have a special Unspecified value. In that case we delegate handling of the "false"
    // value restoration to the corresponding saver that will restore "false" as an Unspecified
    // of the corresponding type.
    if (value == false && saver !is NonNullValueClassSaver<*, *>) return null
    return value?.let { with(saver) { restore(value) } as Result }
}

/**
 * Use for non-null value classes where the Unspecified value needs to be separately handled, for
 * example as in the `OffsetSaver`
 */
internal interface NonNullValueClassSaver<Original, Saveable : Any> : Saver<Original, Saveable>

private fun <Original, Saveable : Any> NonNullValueClassSaver(
    save: SaverScope.(value: Original) -> Saveable?,
    restore: (value: Saveable) -> Original?,
): NonNullValueClassSaver<Original, Saveable> {
    return object : NonNullValueClassSaver<Original, Saveable> {
        override fun SaverScope.save(value: Original) = save.invoke(this, value)

        override fun restore(value: Saveable) = restore.invoke(value)
    }
}

/** Utility function to save nullable values that does not require a Saver. */
internal fun <T> save(value: T?): T? {
    return value
}

/** Utility function to restore nullable values that does not require a Saver. */
internal inline fun <reified Result> restore(value: Any?): Result? {
    return value?.let { it as Result }
}

/**
 * Holds the [TextRange] [Saver] instance on a `private object` with [@JvmField] so `SaversKt` has
 * no `<clinit>` class initializer and R8 can tree-shake unused saver groups. All saver properties
 * in this holder must remain [@JvmField].
 */
private object TextRangeSaverHolder {
    @JvmField
    internal val Instance: Saver<TextRange, Any> =
        Saver<TextRange, Any>(
            save = { arrayListOf(save(it.start), save(it.end)) },
            restore = {
                @Suppress("UNCHECKED_CAST") val list = it as List<Any>
                TextRange(restore(list[0])!!, restore(list[1])!!)
            },
        )
}

/**
 * Holds the [AnnotatedString] [Saver] instances on a `private object` with [@JvmField] so
 * `SaversKt` has no `<clinit>` class initializer and R8 can tree-shake unused saver groups. All
 * saver properties in this holder must remain [@JvmField].
 */
private object AnnotatedStringSaverHolder {
    @JvmField
    internal val Instance: Saver<AnnotatedString, Any> =
        Saver<AnnotatedString, Any>(
            save = {
                arrayListOf(save(it.text), save(it.annotations, RangeListInstance, this))
            },
            restore = {
                val list = it as List<Any?>
                // lift these to make types work
                val annotationsOrNull:
                    List<AnnotatedString.Range<out AnnotatedString.Annotation>>? =
                    restore(list[1], RangeListInstance)
                AnnotatedString(text = restore(list[0])!!, annotations = annotationsOrNull)
            },
        )

    @JvmField
    internal val RangeListInstance: Saver<List<AnnotatedString.Range<out Any>>, Any> =
        Saver<List<AnnotatedString.Range<out Any>>, Any>(
            save = { list ->
                list
                    .fastFilteredMap({ range ->
                        val item = range.item
                        when (item) {
                            is Bullet -> false
                            is DrawAnnotation -> item !is CombinedDrawAnnotation && canBeSaved(item)
                            else -> true
                        }
                    }) { range ->
                        save(range, RangeInstance, this)
                    }
                    .ifEmpty { null }
            },
            restore = {
                @Suppress("UNCHECKED_CAST") val list = it as List<Any>
                list.fastMap { item ->
                    val range: AnnotatedString.Range<out Any> = restore(item, RangeInstance)!!
                    range
                }
            },
        )

    @JvmField
    internal val RangeInstance: Saver<AnnotatedString.Range<out Any>, Any> =
        Saver<AnnotatedString.Range<out Any>, Any>(
            save = {
                val savedAnnotation =
                    save(it.item as AnnotatedString.Annotation, AnnotationSaver, this)
                arrayListOf(savedAnnotation, save(it.start), save(it.end), save(it.tag))
            },
            restore = {
                @Suppress("UNCHECKED_CAST") val list = it as List<Any>
                val item: AnnotatedString.Annotation = restore(list[0], AnnotationSaver)!!
                val start = restore<Int>(list[1])!!
                val end = restore<Int>(list[2])!!
                val tag = restore<String>(list[3])!!
                AnnotatedString.Range(item = item, start = start, end = end, tag = tag)
            },
        )
}

/**
 * Holds the common style and annotation [Saver] instances on a `private object` with [@JvmField] so
 * `SaversKt` has no `<clinit>` class initializer and R8 can strip unused savers. Future additions
 * to [StyleSavers] must keep [@JvmField] on a private holder object to avoid reintroducing
 * companion or top-level `<clinit>` retention.
 */
@OptIn(ExperimentalTextApi::class)
private object StyleSavers {
    @JvmField
    internal val AnnotationSaverInstance: Saver<AnnotatedString.Annotation, Any> =
        Saver(
            save = { annotation ->
                val marker =
                    when (annotation) {
                        is ParagraphStyle -> AnnotationType.Paragraph
                        is SpanStyle -> AnnotationType.Span
                        is VerbatimTtsAnnotation -> AnnotationType.VerbatimTts
                        is UrlAnnotation -> AnnotationType.Url
                        is LinkAnnotation.Url -> AnnotationType.Link
                        is LinkAnnotation.Clickable -> AnnotationType.Clickable
                        is StringAnnotation -> AnnotationType.String
                        is DrawAnnotation ->
                            if (annotation !is CombinedDrawAnnotation && canBeSaved(annotation)) {
                                AnnotationType.Draw
                            } else {
                                throw UnsupportedOperationException()
                            }
                        else -> throw UnsupportedOperationException()
                    }

                val item =
                    when (marker) {
                        AnnotationType.Paragraph ->
                            save(annotation as ParagraphStyle, ParagraphStyleSaverInstance, this)
                        AnnotationType.Span ->
                            save(annotation as SpanStyle, SpanStyleSaverInstance, this)
                        AnnotationType.VerbatimTts ->
                            save(
                                annotation as VerbatimTtsAnnotation,
                                VerbatimTtsAnnotationSaverInstance,
                                this,
                            )
                        AnnotationType.Url ->
                            save(annotation as UrlAnnotation, UrlAnnotationSaverInstance, this)
                        AnnotationType.Link ->
                            save(annotation as LinkAnnotation.Url, LinkSaverInstance, this)
                        AnnotationType.Clickable ->
                            save(
                                annotation as LinkAnnotation.Clickable,
                                ClickableSaverInstance,
                                this,
                            )
                        AnnotationType.String -> save((annotation as StringAnnotation).value)
                        AnnotationType.Draw -> save(annotation as DrawAnnotation)
                    }

                arrayListOf(save(marker), item)
            },
            restore = {
                @Suppress("UNCHECKED_CAST") val list = it as List<Any>
                val marker: AnnotationType = restore(list[0])!!
                when (marker) {
                    AnnotationType.Paragraph -> restore(list[1], ParagraphStyleSaverInstance)!!
                    AnnotationType.Span -> restore(list[1], SpanStyleSaverInstance)!!
                    AnnotationType.VerbatimTts ->
                        restore(list[1], VerbatimTtsAnnotationSaverInstance)!!
                    AnnotationType.Url -> restore(list[1], UrlAnnotationSaverInstance)!!
                    AnnotationType.Link -> restore(list[1], LinkSaverInstance)!!
                    AnnotationType.Clickable -> restore(list[1], ClickableSaverInstance)!!
                    AnnotationType.String -> {
                        val item: String = restore(list[1])!!
                        StringAnnotation(item)
                    }
                    AnnotationType.Draw -> restore<DrawAnnotation>(list[1])!!
                }
            },
        )

    @JvmField
    internal val VerbatimTtsAnnotationSaverInstance: Saver<VerbatimTtsAnnotation, Any> =
        Saver<VerbatimTtsAnnotation, Any>(
            save = { save(it.verbatim) },
            restore = { VerbatimTtsAnnotation(restore(it)!!) },
        )

    @JvmField
    internal val UrlAnnotationSaverInstance: Saver<UrlAnnotation, Any> =
        Saver<UrlAnnotation, Any>(
            save = { save(it.url) },
            restore = { UrlAnnotation(restore(it)!!) },
        )

    @JvmField
    internal val LinkSaverInstance: Saver<LinkAnnotation.Url, Any> =
        Saver<LinkAnnotation.Url, Any>(
            save = {
                arrayListOf(save(it.url), save(it.styles, TextLinkStylesSaverInstance, this))
            },
            restore = {
                val list = it as List<Any?>
                val url: String = restore(list[0])!!
                val stylesOrNull: TextLinkStyles? = restore(list[1], TextLinkStylesSaverInstance)
                LinkAnnotation.Url(url = url, styles = stylesOrNull)
            },
        )

    @JvmField
    internal val ClickableSaverInstance: Saver<LinkAnnotation.Clickable, Any> =
        Saver<LinkAnnotation.Clickable, Any>(
            save = {
                arrayListOf(save(it.tag), save(it.styles, TextLinkStylesSaverInstance, this))
            },
            restore = {
                val list = it as List<Any?>
                val tag: String = restore(list[0])!!
                val stylesOrNull: TextLinkStyles? = restore(list[1], TextLinkStylesSaverInstance)
                LinkAnnotation.Clickable(
                    tag = tag,
                    styles = stylesOrNull,
                    linkInteractionListener = null,
                )
            },
        )

    @JvmField
    internal val ParagraphStyleSaverInstance: Saver<ParagraphStyle, Any> =
        Saver<ParagraphStyle, Any>(
            save = {
                if (!PackedTextStylesLatch.isEnabled) {
                    arrayListOf(
                        save(it.textAlign, TextAlign.Saver, this),
                        save(it.textDirection, TextDirection.Saver, this),
                        save(it.lineHeight, TextUnit.Saver, this),
                        save(it.textIndent, TextIndent.Saver, this),
                        save(it.platformStyle, PlatformParagraphStyle.Saver, this),
                        save(it.lineHeightStyle, LineHeightStyle.Saver, this),
                        save(it.lineBreak, LineBreak.Saver, this),
                        save(it.hyphens, Hyphens.Saver, this),
                        save(it.textMotion, TextMotion.Saver, this),
                    )
                } else {
                    val extras = it.extras
                    arrayListOf(
                        it.bits,
                        save(extras?.textIndent, TextIndentSaverInstance, this),
                        save(extras?.platformStyle, PlatformParagraphStyle.Saver, this),
                        save(it.lineHeightStyle, LineHeightStyleSaverInstance, this),
                        save(extras?.textMotion, TextMotion.Saver, this),
                    )
                }
            },
            restore = {
                val list = it as List<Any?>
                if (list.size > 5) {
                    ParagraphStyle(
                        textAlign = restore(list[0], TextAlign.Saver)!!,
                        textDirection = restore(list[1], TextDirection.Saver)!!,
                        lineHeight = restore(list[2], TextUnit.Saver)!!,
                        textIndent = restore(list[3], TextIndent.Saver),
                        platformStyle = restore(list[4], PlatformParagraphStyle.Saver),
                        lineHeightStyle = restore(list[5], LineHeightStyle.Saver),
                        lineBreak = restore(list[6], LineBreak.Saver)!!,
                        hyphens = restore(list[7], Hyphens.Saver)!!,
                        textMotion = restore(list[8], TextMotion.Saver),
                    )
                } else {
                    ParagraphStyle(
                            list[0] as Long,
                            restore(list[3], LineHeightStyleSaverInstance),
                            createParagraphStyleExtras(
                                restore(list[1], TextIndentSaverInstance),
                                restore(list[2], PlatformParagraphStyle.Saver),
                                restore(list[4], TextMotion.Saver),
                            ),
                        )
                        .let { if (PackedTextStylesLatch.isEnabled) it else it.copy() }
                }
            },
        )

    @JvmField
    internal val SpanStyleSaverInstance: Saver<SpanStyle, Any> =
        Saver<SpanStyle, Any>(
            save = {
                if (!PackedTextStylesLatch.isEnabled) {
                    arrayListOf(
                        save(it.color, Color.Saver, this),
                        save(it.fontSize, TextUnit.Saver, this),
                        save(it.fontWeight, FontWeight.Saver, this),
                        save(it.fontStyle, FontStyle.Saver, this),
                        save(it.fontSynthesis, FontSynthesis.Saver, this),
                        save(-1), // TODO save fontFamily
                        save(it.fontFeatureSettings),
                        save(it.letterSpacing, TextUnit.Saver, this),
                        save(it.baselineShift, BaselineShift.Saver, this),
                        save(it.textGeometricTransform, TextGeometricTransform.Saver, this),
                        save(it.localeList, LocaleList.Saver, this),
                        save(it.background, Color.Saver, this),
                        save(it.textDecoration, TextDecoration.Saver, this),
                        save(it.shadow, Shadow.Saver, this),
                    )
                } else {
                    val extras = it.extras
                    // When background == Color.Transparent and extras == null, save writes false at
                    // index 9 because extras == null. HAS_BACKGROUND in bits (index 1) preserves
                    // the presence of background so restore decodes Color.Transparent.
                    arrayListOf(
                        if (extras?.brush != null) 0L else it.colorXor,
                        it.bits and UNSAVED_SPAN_BITS.inv(),
                        save(it.fontSize, TextUnitSaverInstance, this),
                        save(it.letterSpacing, TextUnitSaverInstance, this),
                        save(it.fontWeight, FontWeightSaverInstance, this),
                        save(extras?.fontFeatureSettings),
                        save(extras?.baselineShift, BaselineShiftSaverInstance, this),
                        save(
                            extras?.textGeometricTransform,
                            TextGeometricTransformSaverInstance,
                            this,
                        ),
                        save(extras?.localeList, LocaleListSaverInstance, this),
                        if (extras == null) false
                        else save(extras.background, ColorSaverInstance, this),
                        save(extras?.shadow, ShadowSaverInstance, this),
                    )
                }
            },
            restore = {
                val list = it as List<Any?>
                if (list.size > 11) {
                    SpanStyle(
                        color = restore(list[0], Color.Saver)!!,
                        fontSize = restore(list[1], TextUnit.Saver)!!,
                        fontWeight = restore(list[2], FontWeight.Saver),
                        fontStyle = restore(list[3], FontStyle.Saver),
                        fontSynthesis = restore(list[4], FontSynthesis.Saver),
                        // val fontFamily = list[5] // TODO restore fontFamily
                        fontFeatureSettings = restore(list[6]),
                        letterSpacing = restore(list[7], TextUnit.Saver)!!,
                        baselineShift = restore(list[8], BaselineShift.Saver),
                        textGeometricTransform = restore(list[9], TextGeometricTransform.Saver),
                        localeList = restore(list[10], LocaleList.Saver),
                        background = restore(list[11], Color.Saver)!!,
                        textDecoration = restore(list[12], TextDecoration.Saver),
                        shadow = restore(list[13], Shadow.Saver),
                    )
                } else {
                    val extras =
                        createSpanStyleExtras(
                            brush = null,
                            alpha = Float.NaN,
                            fontFeatureSettings = restore(list[5]),
                            baselineShift = restore(list[6], BaselineShiftSaverInstance),
                            textGeometricTransform =
                                restore(list[7], TextGeometricTransformSaverInstance),
                            localeList = restore(list[8], LocaleListSaverInstance),
                            background = restore(list[9], ColorSaverInstance)!!,
                            shadow = restore(list[10], ShadowSaverInstance),
                            drawStyle = null,
                        )
                    val colorVal = list[0]
                    val colorXor =
                        when (colorVal) {
                            is Int ->
                                if (colorVal == 0) 0L
                                else (Color(colorVal).value.toLong() xor 0x10L)
                            is Long -> colorVal
                            else -> 0L
                        }
                    SpanStyle(
                            colorXor = colorXor,
                            bits = list[1] as Int,
                            extras = extras,
                            fontSize = restore(list[2], TextUnitSaverInstance)!!,
                            letterSpacing = restore(list[3], TextUnitSaverInstance)!!,
                            fontWeight = restore(list[4], FontWeightSaverInstance),
                            fontFamily = null,
                        )
                        .let { if (PackedTextStylesLatch.isEnabled) it else it.copy() }
                }
            },
        )

    @JvmField
    internal val TextLinkStylesSaverInstance: Saver<TextLinkStyles, Any> =
        Saver<TextLinkStyles, Any>(
            save = {
                arrayListOf(
                    save(it.style, SpanStyleSaverInstance, this),
                    save(it.focusedStyle, SpanStyleSaverInstance, this),
                    save(it.hoveredStyle, SpanStyleSaverInstance, this),
                    save(it.pressedStyle, SpanStyleSaverInstance, this),
                )
            },
            restore = {
                val list = it as List<Any?>
                val styleOrNull: SpanStyle? = restore(list[0], SpanStyleSaverInstance)
                val focusedStyleOrNull: SpanStyle? = restore(list[1], SpanStyleSaverInstance)
                val hoveredStyleOrNull: SpanStyle? = restore(list[2], SpanStyleSaverInstance)
                val pressedStyleOrNull: SpanStyle? = restore(list[3], SpanStyleSaverInstance)
                TextLinkStyles(
                    styleOrNull,
                    focusedStyleOrNull,
                    hoveredStyleOrNull,
                    pressedStyleOrNull,
                )
            },
        )

    @JvmField
    internal val TextDecorationSaverInstance: Saver<TextDecoration, Any> =
        Saver<TextDecoration, Any>(save = { it.mask }, restore = { TextDecoration(it as Int) })

    @JvmField
    internal val TextGeometricTransformSaverInstance: Saver<TextGeometricTransform, Any> =
        Saver<TextGeometricTransform, Any>(
            save = { arrayListOf(it.scaleX, it.skewX) },
            restore = {
                @Suppress("UNCHECKED_CAST") val list = it as List<Float>
                TextGeometricTransform(scaleX = list[0], skewX = list[1])
            },
        )

    @JvmField
    internal val TextIndentSaverInstance: Saver<TextIndent, Any> =
        Saver<TextIndent, Any>(
            save = {
                arrayListOf(
                    save(it.firstLine, TextUnitSaverInstance, this),
                    save(it.restLine, TextUnitSaverInstance, this),
                )
            },
            restore = {
                @Suppress("UNCHECKED_CAST") val list = it as List<Any>
                TextIndent(
                    firstLine = restore(list[0], TextUnitSaverInstance)!!,
                    restLine = restore(list[1], TextUnitSaverInstance)!!,
                )
            },
        )

    @JvmField
    internal val FontWeightSaverInstance: Saver<FontWeight, Any> =
        Saver<FontWeight, Any>(save = { it.weight }, restore = { FontWeight(it as Int) })

    @JvmField
    internal val BaselineShiftSaverInstance: Saver<BaselineShift, Any> =
        Saver<BaselineShift, Any>(
            save = { it.multiplier },
            restore = { BaselineShift(it as Float) },
        )

    @JvmField
    internal val ShadowSaverInstance: Saver<Shadow, Any> =
        Saver<Shadow, Any>(
            save = {
                arrayListOf(
                    save(it.color, ColorSaverInstance, this),
                    save(it.offset, OffsetSaverInstance, this),
                    save(it.blurRadius),
                )
            },
            restore = {
                @Suppress("UNCHECKED_CAST") val list = it as List<Any>
                Shadow(
                    color = restore(list[0], ColorSaverInstance)!!,
                    offset = restore(list[1], OffsetSaverInstance)!!,
                    blurRadius = restore(list[2])!!,
                )
            },
        )

    @JvmField
    internal val ColorSaverInstance: NonNullValueClassSaver<Color, Any> =
        NonNullValueClassSaver<Color, Any>(
            save = {
                if (it.isUnspecified) {
                    false
                } else {
                    it.toArgb()
                }
            },
            restore = {
                if (it == false) {
                    Color.Unspecified
                } else {
                    Color(it as Int)
                }
            },
        )

    @JvmField
    internal val TextAlignSaverInstance: NonNullValueClassSaver<TextAlign, Any> =
        NonNullValueClassSaver<TextAlign, Any>(
            save = { it.value },
            restore = { TextAlign(it as Int) },
        )

    @JvmField
    internal val TextDirectionSaverInstance: NonNullValueClassSaver<TextDirection, Any> =
        NonNullValueClassSaver<TextDirection, Any>(
            save = { it.value },
            restore = { TextDirection(it as Int) },
        )

    @JvmField
    internal val HyphensSaverInstance: NonNullValueClassSaver<Hyphens, Any> =
        NonNullValueClassSaver<Hyphens, Any>(save = { it.value }, restore = { Hyphens(it as Int) })

    @JvmField
    internal val FontStyleSaverInstance: Saver<FontStyle, Any> =
        Saver<FontStyle, Any>(save = { save(it.value) }, restore = { FontStyle(it as Int) })

    @JvmField
    internal val FontSynthesisSaverInstance: Saver<FontSynthesis, Any> =
        Saver<FontSynthesis, Any>(save = { it.value }, restore = { FontSynthesis(it as Int) })

    @JvmField
    internal val TextUnitSaverInstance: NonNullValueClassSaver<TextUnit, Any> =
        NonNullValueClassSaver<TextUnit, Any>(
            save = {
                if (it == TextUnit.Unspecified) {
                    false
                } else {
                    arrayListOf(save(it.value), save(it.type, TextUnitTypeSaverInstance, this))
                }
            },
            restore = {
                if (it == false) {
                    TextUnit.Unspecified
                } else {
                    @Suppress("UNCHECKED_CAST") val list = it as List<Any>
                    TextUnit(restore(list[0])!!, restore(list[1], TextUnitTypeSaverInstance)!!)
                }
            },
        )

    @JvmField
    internal val TextUnitTypeSaverInstance: NonNullValueClassSaver<TextUnitType, Any> =
        NonNullValueClassSaver<TextUnitType, Any>(
            save = {
                when (it) {
                    TextUnitType.Em -> 0
                    TextUnitType.Sp -> 1
                    else -> false
                }
            },
            restore = {
                when (it) {
                    0 -> TextUnitType.Em
                    1 -> TextUnitType.Sp
                    else -> TextUnitType.Unspecified
                }
            },
        )

    @JvmField
    internal val OffsetSaverInstance: NonNullValueClassSaver<Offset, Any> =
        NonNullValueClassSaver<Offset, Any>(
            save = {
                if (it == Offset.Unspecified) {
                    false
                } else {
                    arrayListOf(save(it.x), save(it.y))
                }
            },
            restore = {
                if (it == false) {
                    Offset.Unspecified
                } else {
                    val list = it as List<Any?>
                    Offset(restore(list[0])!!, restore(list[1])!!)
                }
            },
        )

    @JvmField
    internal val LocaleListSaverInstance: Saver<LocaleList, Any> =
        Saver<LocaleList, Any>(
            save = { it.localeList.fastMap { locale -> save(locale, LocaleSaverInstance, this) } },
            restore = {
                @Suppress("UNCHECKED_CAST") val list = it as List<Any>
                LocaleList(list.fastMap { item -> restore(item, LocaleSaverInstance)!! })
            },
        )

    @JvmField
    internal val LocaleSaverInstance: Saver<Locale, Any> =
        Saver<Locale, Any>(
            save = { it.toLanguageTag() },
            restore = { Locale(languageTag = it as String) },
        )

    @JvmField
    internal val LineHeightStyleSaverInstance: Saver<LineHeightStyle, Any> =
        Saver<LineHeightStyle, Any>(
            save = {
                arrayListOf(
                    save(it.alignment, LineHeightStyleAlignmentSaverInstance, this),
                    save(it.trim, LineHeightStyleTrimSaverInstance, this),
                    save(it.mode, LineHeightStyleModeSaverInstance, this),
                )
            },
            restore = {
                @Suppress("UNCHECKED_CAST") val list = it as List<Any>
                LineHeightStyle(
                    alignment = restore(list[0], LineHeightStyleAlignmentSaverInstance)!!,
                    trim = restore(list[1], LineHeightStyleTrimSaverInstance)!!,
                    mode = restore(list[2], LineHeightStyleModeSaverInstance)!!,
                )
            },
        )

    @JvmField
    internal val LineHeightStyleAlignmentSaverInstance:
        NonNullValueClassSaver<LineHeightStyle.Alignment, Any> =
        NonNullValueClassSaver<LineHeightStyle.Alignment, Any>(
            save = { it.topRatio },
            restore = { LineHeightStyle.Alignment(it as Float) },
        )

    @JvmField
    internal val LineHeightStyleTrimSaverInstance:
        NonNullValueClassSaver<LineHeightStyle.Trim, Any> =
        NonNullValueClassSaver<LineHeightStyle.Trim, Any>(
            save = { it.value },
            restore = { LineHeightStyle.Trim(it as Int) },
        )

    @JvmField
    internal val LineHeightStyleModeSaverInstance:
        NonNullValueClassSaver<LineHeightStyle.Mode, Any> =
        NonNullValueClassSaver<LineHeightStyle.Mode, Any>(
            save = { it.value },
            restore = { LineHeightStyle.Mode(it as Int) },
        )
}

internal val AnnotatedStringSaver: Saver<AnnotatedString, Any>
    get() = AnnotatedStringSaverHolder.Instance

private enum class AnnotationType {
    Paragraph,
    Span,
    VerbatimTts,
    Url, // UrlAnnotation
    Link, // LinkAnnotation.Url
    Clickable,
    String,
    Draw,
}

/**
 * Saves and restores [AnnotatedString.Annotation] objects.
 *
 * Supports the following annotation types:
 * - [ParagraphStyle]
 * - [SpanStyle]
 * - [VerbatimTtsAnnotation]
 * - [UrlAnnotation]
 * - [LinkAnnotation.Url]
 * - [LinkAnnotation.Clickable]
 * - [StringAnnotation]
 * - [DrawAnnotation] subclasses that satisfy [SaverScope.canBeSaved] (such as `Parcelable` `data
 *   class`es on Android)
 *
 * Note: This [Saver] does not preserve the [LinkInteractionListener] of [LinkAnnotation]s,
 * lambda-backed [DrawAnnotation] instances, or [Bullet] annotations. If you need to restore them,
 * you must handle saving and restoring them manually.
 */
internal val AnnotationSaver: Saver<AnnotatedString.Annotation, Any>
    get() = StyleSavers.AnnotationSaverInstance

internal val ParagraphStyleSaver: Saver<ParagraphStyle, Any>
    get() = StyleSavers.ParagraphStyleSaverInstance

internal val SpanStyleSaver: Saver<SpanStyle, Any>
    get() = StyleSavers.SpanStyleSaverInstance

internal val TextLinkStylesSaver: Saver<TextLinkStyles, Any>
    get() = StyleSavers.TextLinkStylesSaverInstance

internal val TextDecoration.Companion.Saver: Saver<TextDecoration, Any>
    get() = StyleSavers.TextDecorationSaverInstance

internal val TextGeometricTransform.Companion.Saver: Saver<TextGeometricTransform, Any>
    get() = StyleSavers.TextGeometricTransformSaverInstance

internal val TextIndent.Companion.Saver: Saver<TextIndent, Any>
    get() = StyleSavers.TextIndentSaverInstance

internal val FontWeight.Companion.Saver: Saver<FontWeight, Any>
    get() = StyleSavers.FontWeightSaverInstance

internal val BaselineShift.Companion.Saver: Saver<BaselineShift, Any>
    get() = StyleSavers.BaselineShiftSaverInstance

internal val TextRange.Companion.Saver: Saver<TextRange, Any>
    get() = TextRangeSaverHolder.Instance

internal val Shadow.Companion.Saver: Saver<Shadow, Any>
    get() = StyleSavers.ShadowSaverInstance

internal val Color.Companion.Saver: Saver<Color, Any>
    get() = StyleSavers.ColorSaverInstance

internal val TextAlign.Companion.Saver: Saver<TextAlign, Any>
    get() = StyleSavers.TextAlignSaverInstance

internal val TextDirection.Companion.Saver: Saver<TextDirection, Any>
    get() = StyleSavers.TextDirectionSaverInstance

internal val Hyphens.Companion.Saver: Saver<Hyphens, Any>
    get() = StyleSavers.HyphensSaverInstance

internal val FontStyle.Companion.Saver: Saver<FontStyle, Any>
    get() = FontStyleSaver

internal val FontStyleSaver: Saver<FontStyle, Any>
    get() = StyleSavers.FontStyleSaverInstance

internal val FontSynthesis.Companion.Saver: Saver<FontSynthesis, Any>
    get() = FontSynthesisSaver

internal val FontSynthesisSaver: Saver<FontSynthesis, Any>
    get() = StyleSavers.FontSynthesisSaverInstance

internal val TextUnit.Companion.Saver: Saver<TextUnit, Any>
    get() = StyleSavers.TextUnitSaverInstance

internal val TextUnitType.Companion.Saver: Saver<TextUnitType, Any>
    get() = TextUnitTypeSaver

internal val TextUnitTypeSaver: NonNullValueClassSaver<TextUnitType, Any>
    get() = StyleSavers.TextUnitTypeSaverInstance

internal val Offset.Companion.Saver: Saver<Offset, Any>
    get() = StyleSavers.OffsetSaverInstance

internal val LocaleList.Companion.Saver: Saver<LocaleList, Any>
    get() = StyleSavers.LocaleListSaverInstance

internal val Locale.Companion.Saver: Saver<Locale, Any>
    get() = StyleSavers.LocaleSaverInstance

internal val LineHeightStyle.Companion.Saver: Saver<LineHeightStyle, Any>
    get() = StyleSavers.LineHeightStyleSaverInstance

private val LineHeightStyle.Alignment.Companion.Saver: Saver<LineHeightStyle.Alignment, Any>
    get() = StyleSavers.LineHeightStyleAlignmentSaverInstance

private val LineHeightStyle.Trim.Companion.Saver: Saver<LineHeightStyle.Trim, Any>
    get() = StyleSavers.LineHeightStyleTrimSaverInstance

private val LineHeightStyle.Mode.Companion.Saver: Saver<LineHeightStyle.Mode, Any>
    get() = StyleSavers.LineHeightStyleModeSaverInstance

internal expect val PlatformParagraphStyle.Companion.Saver: Saver<PlatformParagraphStyle, Any>

internal expect val LineBreak.Companion.Saver: Saver<LineBreak, Any>

internal expect val TextMotion.Companion.Saver: Saver<TextMotion, Any>
