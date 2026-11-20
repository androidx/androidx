/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.ui.text.benchmark

import android.content.Context
import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.MultiParagraph
import androidx.compose.ui.text.MultiParagraphIntrinsics
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.PlatformParagraphStyle
import androidx.compose.ui.text.PlatformSpanStyle
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.VerbatimTtsAnnotation
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontSynthesis
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.lerp
import androidx.compose.ui.text.resolveDefaults
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
import androidx.compose.ui.text.toUpperCase
import androidx.compose.ui.text.withAnnotation
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Benchmarks for fast and slow paths in [TextStyle], [SpanStyle], [ParagraphStyle], and
 * [AnnotatedString].
 *
 * Measures style merge, hash, builder, saver, and layout speed across the text stack. Set
 * [DoFullBenchmark] to `true` to run this suite on a real device.
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class AnnotatedStringTextStylePackedBenchmark {

    init {
        // we only need this for full reporting
        assumeTrue(DoFullBenchmark)
    }

    @get:Rule val benchmarkRule = BenchmarkRule()

    // Sink stops dead code removal.
    @Volatile var sink: Any? = null
    @Volatile var intSink: Int = 0

    // Android context, font loader, and text tool.
    private lateinit var context: Context
    private lateinit var defaultDensity: Density
    private lateinit var fontFamilyResolver: FontFamily.Resolver
    private lateinit var textMeasurer: TextMeasurer
    private val defaultLocaleList = LocaleList("en")
    private val alternateLocaleList = LocaleList("fr")
    private val allowAllSaverScope = SaverScope { true }

    var colorRed = Color.Red
    var colorBlue = Color.Blue
    var colorYellow = Color.Yellow
    var fontSize16 = 16.sp
    var fontWeightBold = FontWeight.Bold
    var fontWeightCustom = FontWeight(350)
    var layoutDirectionLtr = LayoutDirection.Ltr
    var measurerSampleText = "Packed style cache lookup sample text for TextMeasurer"

    // Fast `ParagraphStyle` cases with no extras.
    var baseParagraphStyle =
        ParagraphStyle(
            textAlign = TextAlign.Start,
            lineHeight = 20.sp,
            hyphens = Hyphens.None,
        )

    var overrideParagraphStyle =
        ParagraphStyle(
            textAlign = TextAlign.Center,
            textDirection = TextDirection.Ltr,
            lineBreak = LineBreak.Paragraph,
        )

    var defaultParagraphStyle = ParagraphStyle(textAlign = TextAlign.Start)

    // Slow `ParagraphStyle` cases with `ParagraphStyleExtras`.
    @Suppress("DEPRECATION")
    var customPlatformParagraphStyle = PlatformParagraphStyle(includeFontPadding = true)
    var customLineHeightStyleStart =
        LineHeightStyle(
            alignment = LineHeightStyle.Alignment(0.25f),
            trim = LineHeightStyle.Trim.FirstLineTop,
        )
    var customLineHeightStyleEnd =
        LineHeightStyle(
            alignment = LineHeightStyle.Alignment(0.75f),
            trim = LineHeightStyle.Trim.LastLineBottom,
        )
    var customTextIndentStart = TextIndent(firstLine = 12.sp, restLine = 4.sp)
    var customTextIndentEnd = TextIndent(firstLine = 18.sp, restLine = 6.sp)

    var slowParagraphStyleLeft =
        ParagraphStyle(
            textAlign = TextAlign.Start,
            lineHeight = 20.sp,
            textIndent = customTextIndentStart,
            platformStyle = customPlatformParagraphStyle,
            lineHeightStyle = customLineHeightStyleStart,
            lineBreak = LineBreak.Simple,
            hyphens = Hyphens.Auto,
            textMotion = TextMotion.Static,
        )

    var slowParagraphStyleRight =
        ParagraphStyle(
            textAlign = TextAlign.Justify,
            textDirection = TextDirection.ContentOrLtr,
            lineHeight = 24.sp,
            textIndent = customTextIndentEnd,
            platformStyle = customPlatformParagraphStyle,
            lineHeightStyle = customLineHeightStyleEnd,
            lineBreak = LineBreak.Heading,
            hyphens = Hyphens.None,
            textMotion = TextMotion.Animated,
        )

    // Fast `SpanStyle` cases with no extras.
    var baseSpanStyle =
        SpanStyle(
            color = Color.Red,
            fontSize = 14.sp,
            fontWeight = FontWeight.Normal,
            fontStyle = FontStyle.Normal,
        )

    var overrideSpanStyle =
        SpanStyle(
            color = Color.Blue,
            fontWeight = FontWeight.Bold,
            fontStyle = FontStyle.Italic,
            textDecoration = TextDecoration.Underline,
        )

    var boldSpanStyle =
        SpanStyle(
            fontWeight = FontWeight.Bold,
            fontStyle = FontStyle.Italic,
        )

    var accentSpanStyle =
        SpanStyle(
            color = Color.Green,
            fontSize = 18.sp,
            textDecoration = TextDecoration.LineThrough,
        )

    // Slow `SpanStyle` cases with `SpanStyleExtras`.
    var primaryGradientBrush = Brush.linearGradient(listOf(Color.Red, Color.Blue))
    var secondaryGradientBrush = Brush.linearGradient(listOf(Color.Green, Color.Yellow))
    var customShadowStart = Shadow(color = Color.Black, offset = Offset(2f, 2f), blurRadius = 4f)
    var customShadowEnd = Shadow(color = Color.DarkGray, offset = Offset(1f, 3f), blurRadius = 6f)
    var customGeometricTransformStart = TextGeometricTransform(scaleX = 1.1f, skewX = 0.05f)
    var customGeometricTransformEnd = TextGeometricTransform(scaleX = 0.95f, skewX = -0.05f)
    var customBaselineShiftStart = BaselineShift(0.35f)
    var customBaselineShiftEnd = BaselineShift(-0.25f)
    var strokeDrawStyle = Stroke(width = 2f)

    var slowSpanStyleLeft =
        SpanStyle(
            brush = primaryGradientBrush,
            alpha = 0.85f,
            fontSize = 15.sp,
            fontWeight = fontWeightCustom,
            fontStyle = FontStyle.Italic,
            fontSynthesis = FontSynthesis.Weight,
            fontFamily = FontFamily.Serif,
            fontFeatureSettings = "smcp",
            letterSpacing = 0.12.em,
            baselineShift = customBaselineShiftStart,
            textGeometricTransform = customGeometricTransformStart,
            localeList = defaultLocaleList,
            background = Color.Yellow,
            textDecoration = TextDecoration.Underline + TextDecoration.LineThrough,
            shadow = customShadowStart,
            platformStyle = PlatformSpanStyle.Default,
            drawStyle = strokeDrawStyle,
        )

    var slowSpanStyleRight =
        SpanStyle(
            brush = secondaryGradientBrush,
            alpha = 0.95f,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            fontStyle = FontStyle.Normal,
            fontSynthesis = FontSynthesis.All,
            fontFamily = FontFamily.Monospace,
            fontFeatureSettings = "liga",
            letterSpacing = 0.5.sp,
            baselineShift = customBaselineShiftEnd,
            textGeometricTransform = customGeometricTransformEnd,
            localeList = alternateLocaleList,
            background = Color.Cyan,
            textDecoration = TextDecoration.Underline,
            shadow = customShadowEnd,
            platformStyle = PlatformSpanStyle.Default,
            drawStyle = strokeDrawStyle,
        )

    // Fast and slow `TextStyle` cases.
    var baseTextStyle =
        TextStyle(
            color = Color.Red,
            fontSize = 16.sp,
            fontWeight = FontWeight.Normal,
            textAlign = TextAlign.Start,
            lineHeight = 22.sp,
        )

    var baseTextStyleCopy =
        TextStyle(
            color = Color.Blue,
            fontSize = 16.sp,
            fontWeight = FontWeight.Normal,
            textAlign = TextAlign.Start,
            lineHeight = 22.sp,
        )

    var slowTextStyleLeft = TextStyle.Default.merge(slowSpanStyleLeft).merge(slowParagraphStyleLeft)
    var slowTextStyleRight =
        TextStyle.Default.merge(slowSpanStyleRight).merge(slowParagraphStyleRight)
    lateinit var resolvedFastTextStyleStart: TextStyle
    lateinit var resolvedFastTextStyleEnd: TextStyle
    lateinit var resolvedSlowTextStyle: TextStyle

    @Suppress("DEPRECATION")
    var materialDefaultPlatformStyle = PlatformTextStyle(includeFontPadding = false)
    var materialDefaultLineHeightStyle =
        LineHeightStyle(
            alignment = LineHeightStyle.Alignment.Center,
            trim = LineHeightStyle.Trim.None,
        )

    var materialBodyLargeTextStyle =
        TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            letterSpacing = 0.5.sp,
            platformStyle = materialDefaultPlatformStyle,
            lineHeightStyle = materialDefaultLineHeightStyle,
        )

    var materialLabelLargeTextStyle =
        TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.1.sp,
            platformStyle = materialDefaultPlatformStyle,
            lineHeightStyle = materialDefaultLineHeightStyle,
        )

    var materialOnSurfaceColor = Color(0xFF1D1B20)
    var materialLinkStyles =
        TextLinkStyles(
            style = SpanStyle(color = Color.Blue, textDecoration = TextDecoration.Underline),
            focusedStyle = SpanStyle(color = Color.Red),
            hoveredStyle = SpanStyle(color = Color.Green),
            pressedStyle = SpanStyle(color = Color.Magenta),
        )
    var sampleUrlLink =
        LinkAnnotation.Url("https://developer.android.com", styles = materialLinkStyles)
    var sampleUnstyledUrlLink = LinkAnnotation.Url("https://kotlinlang.org")
    var sampleClickableLink =
        LinkAnnotation.Clickable(
            tag = "action_open",
            styles = materialLinkStyles,
            linkInteractionListener = null,
        )
    var sampleTtsAnnotation = VerbatimTtsAnnotation("Compose UI")
    var samplePlaceholders =
        listOf(
            AnnotatedString.Range(
                item =
                    Placeholder(
                        width = 18.sp,
                        height = 18.sp,
                        placeholderVerticalAlign = PlaceholderVerticalAlign.AboveBaseline,
                    ),
                start = 6,
                end = 7,
            ),
            AnnotatedString.Range(
                item =
                    Placeholder(
                        width = 1.2.em,
                        height = 1.2.em,
                        placeholderVerticalAlign = PlaceholderVerticalAlign.Center,
                    ),
                start = 15,
                end = 16,
            ),
        )

    lateinit var styledAnnotatedString: AnnotatedString
    lateinit var suffixAnnotatedString: AnnotatedString
    lateinit var materialEightSpanAnnotatedString: AnnotatedString
    lateinit var multiParagraphSlowAnnotatedString: AnnotatedString
    lateinit var saverRichAnnotatedString: AnnotatedString

    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().context
        defaultDensity = Density(context)
        fontFamilyResolver = createFontFamilyResolver(context)
        textMeasurer =
            TextMeasurer(
                defaultFontFamilyResolver = fontFamilyResolver,
                defaultDensity = defaultDensity,
                defaultLayoutDirection = LayoutDirection.Ltr,
                cacheSize = 8,
                defaultLocaleList = defaultLocaleList,
            )
        textMeasurer.measure(
            text = AnnotatedString(measurerSampleText),
            style = TextStyle(color = colorRed, fontSize = fontSize16),
        )
        resolvedFastTextStyleStart = resolveDefaults(baseTextStyle, LayoutDirection.Ltr)
        resolvedFastTextStyleEnd = resolveDefaults(baseTextStyleCopy, LayoutDirection.Ltr)
        resolvedSlowTextStyle = resolveDefaults(slowTextStyleLeft, LayoutDirection.Ltr)
        styledAnnotatedString = buildAnnotatedString {
            withStyle(baseSpanStyle) { append("Hello ") }
            withStyle(overrideSpanStyle) { append("Compose ") }
            withStyle(boldSpanStyle) { append("Packed ") }
            withStyle(accentSpanStyle) { append("Text!") }
        }
        suffixAnnotatedString = buildAnnotatedString {
            withStyle(boldSpanStyle) { append(" suffix") }
        }
        materialEightSpanAnnotatedString = buildAnnotatedString {
            repeat(2) {
                withStyle(baseSpanStyle) { append("App ") }
                withStyle(overrideSpanStyle) { append("Icon ") }
                withStyle(boldSpanStyle) { append("Label ") }
                withStyle(accentSpanStyle) { append("Title ") }
            }
        }
        multiParagraphSlowAnnotatedString = buildAnnotatedString {
            withStyle(slowParagraphStyleLeft) {
                withStyle(slowSpanStyleLeft) { append("First paragraph with extras. ") }
                withStyle(overrideSpanStyle) { append("Inline override.") }
            }
            withStyle(slowParagraphStyleRight) {
                withStyle(slowSpanStyleRight) { append("Second paragraph with custom indent.") }
            }
        }
        saverRichAnnotatedString = buildAnnotatedString {
            withStyle(baseParagraphStyle) {
                withStyle(baseSpanStyle) { append("Saved ") }
                withLink(sampleUrlLink) { append("Link ") }
                withLink(sampleClickableLink) { append("Click ") }
                withAnnotation(sampleTtsAnnotation) { append("TTS ") }
                withAnnotation("tag_key", "tag_val") { append("Tagged") }
            }
            withStyle(slowParagraphStyleLeft) {
                withStyle(slowSpanStyleLeft) { append(" SlowExtras") }
            }
        }
    }

    @Test
    fun textMeasurer_cached_styleCreate_and_hash_regression() {
        val annotatedString = AnnotatedString(measurerSampleText)
        benchmarkRule.measureRepeated {
            sink =
                textMeasurer.measure(
                    text = annotatedString,
                    style = TextStyle(color = colorRed, fontSize = fontSize16),
                )
        }
    }

    @Test
    fun m3_annotatedString_mapAnnotations_noOp_regression() {
        benchmarkRule.measureRepeated {
            sink = materialEightSpanAnnotatedString.mapAnnotations { range ->
                val linkAnnotation = range.item
                if (linkAnnotation is LinkAnnotation.Url && linkAnnotation.styles == null) {
                    AnnotatedString.Range(
                        linkAnnotation.copy(styles = materialLinkStyles),
                        range.start,
                        range.end,
                        range.tag,
                    )
                } else if (
                    linkAnnotation is LinkAnnotation.Clickable && linkAnnotation.styles == null
                ) {
                    AnnotatedString.Range(
                        linkAnnotation.copy(styles = materialLinkStyles),
                        range.start,
                        range.end,
                        range.tag,
                    )
                } else {
                    range
                }
            }
        }
    }

    @Test
    fun textStyle_create_and_resolveDefaults() {
        benchmarkRule.measureRepeated {
            val textStyle =
                TextStyle(
                    color = colorRed,
                    fontSize = fontSize16,
                    fontWeight = fontWeightBold,
                )
            sink = resolveDefaults(textStyle, layoutDirectionLtr)
        }
    }

    @Test
    fun textStyle_create_and_resolveDefaults_withExtras_slow() {
        benchmarkRule.measureRepeated {
            val textStyle =
                TextStyle(
                    color = colorRed,
                    fontSize = fontSize16,
                    fontWeight = fontWeightCustom,
                    fontStyle = FontStyle.Italic,
                    fontSynthesis = FontSynthesis.Weight,
                    fontFamily = FontFamily.Serif,
                    fontFeatureSettings = "smcp",
                    letterSpacing = 0.1.em,
                    baselineShift = customBaselineShiftStart,
                    textGeometricTransform = customGeometricTransformStart,
                    localeList = defaultLocaleList,
                    background = colorYellow,
                    textDecoration = TextDecoration.Underline,
                    shadow = customShadowStart,
                    textAlign = TextAlign.Justify,
                    textDirection = TextDirection.Ltr,
                    lineHeight = 22.sp,
                    textIndent = customTextIndentStart,
                    lineHeightStyle = customLineHeightStyleStart,
                    lineBreak = LineBreak.Paragraph,
                    hyphens = Hyphens.Auto,
                    textMotion = TextMotion.Animated,
                )
            sink = resolveDefaults(textStyle, layoutDirectionLtr)
        }
    }

    @Test
    fun paragraphStyle_merge() {
        benchmarkRule.measureRepeated { sink = baseParagraphStyle.merge(overrideParagraphStyle) }
    }

    @Test
    fun paragraphStyle_merge_withExtras_slow() {
        benchmarkRule.measureRepeated {
            sink = slowParagraphStyleLeft.merge(slowParagraphStyleRight)
        }
    }

    @Test
    fun spanStyle_merge() {
        benchmarkRule.measureRepeated { sink = baseSpanStyle.merge(overrideSpanStyle) }
    }

    @Test
    fun spanStyle_merge_withExtras_slow() {
        benchmarkRule.measureRepeated { sink = slowSpanStyleLeft.merge(slowSpanStyleRight) }
    }

    @Test
    fun textStyle_merge() {
        benchmarkRule.measureRepeated { sink = baseTextStyle.merge(boldSpanStyle) }
    }

    @Test
    fun m3_component_textStyle_merge() {
        benchmarkRule.measureRepeated {
            sink =
                materialBodyLargeTextStyle.merge(
                    color = materialOnSurfaceColor,
                    fontSize = TextUnit.Unspecified,
                    fontWeight = null,
                    textAlign = TextAlign.Unspecified,
                    lineHeight = TextUnit.Unspecified,
                    fontFamily = null,
                    textDecoration = null,
                    fontStyle = null,
                    letterSpacing = TextUnit.Unspecified,
                )
        }
    }

    @Test
    fun textStyle_fastMerge_withExtras_slow() {
        benchmarkRule.measureRepeated {
            sink =
                slowTextStyleLeft.merge(
                    color = colorBlue,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    fontStyle = FontStyle.Normal,
                    fontSynthesis = FontSynthesis.All,
                    fontFamily = FontFamily.Monospace,
                    fontFeatureSettings = "liga",
                    letterSpacing = 0.2.sp,
                    baselineShift = customBaselineShiftEnd,
                    textGeometricTransform = customGeometricTransformEnd,
                    localeList = alternateLocaleList,
                    background = colorYellow,
                    textDecoration = TextDecoration.LineThrough,
                    shadow = customShadowEnd,
                    drawStyle = null,
                    textAlign = TextAlign.Center,
                    textDirection = TextDirection.Rtl,
                    lineHeight = 24.sp,
                    textIndent = customTextIndentEnd,
                    platformStyle = null,
                    lineHeightStyle = customLineHeightStyleEnd,
                    lineBreak = LineBreak.Heading,
                    hyphens = Hyphens.None,
                    textMotion = TextMotion.Static,
                )
        }
    }

    @Test
    fun textStyle_hasSameLayoutAffectingAttributes_fast() {
        benchmarkRule.measureRepeated {
            sink =
                resolvedFastTextStyleStart.hasSameLayoutAffectingAttributes(
                    resolvedFastTextStyleEnd
                )
        }
    }

    @Test
    fun textStyle_hasSameLayoutAffectingAttributes_withExtras_slow() {
        val slowTextStyleCopy = slowTextStyleLeft.copy(shadow = customShadowEnd)
        benchmarkRule.measureRepeated {
            sink = slowTextStyleLeft.hasSameLayoutAffectingAttributes(slowTextStyleCopy)
        }
    }

    @Test
    fun textStyle_hashCode_fast() {
        benchmarkRule.measureRepeated { intSink = resolvedFastTextStyleStart.hashCode() }
    }

    @Test
    fun textStyle_hashCode_withExtras_slow() {
        benchmarkRule.measureRepeated { intSink = slowTextStyleLeft.hashCode() }
    }

    @Test
    fun annotatedString_builder_multiSpan() {
        benchmarkRule.measureRepeated {
            sink = buildAnnotatedString {
                withStyle(baseSpanStyle) { append("Hello ") }
                withStyle(overrideSpanStyle) { append("Compose ") }
                withStyle(boldSpanStyle) { append("Packed ") }
                withStyle(accentSpanStyle) { append("Text!") }
            }
        }
    }

    @Test
    fun annotatedString_builder_withExtras_slow() {
        benchmarkRule.measureRepeated {
            sink = buildAnnotatedString {
                withStyle(slowParagraphStyleLeft) {
                    withStyle(slowSpanStyleLeft) { append("Paragraph one ") }
                    val linkIndex = pushLink(sampleUnstyledUrlLink)
                    append("AndroidLink")
                    pop(linkIndex)
                }
                withStyle(overrideParagraphStyle) {
                    withStyle(overrideSpanStyle) { append(" Paragraph two") }
                }
                addStringAnnotation(tag = "meta", annotation = "v1", start = 0, end = 13)
            }
        }
    }

    @Test
    fun annotatedString_subSequence() {
        benchmarkRule.measureRepeated { sink = saverRichAnnotatedString.subSequence(0, 25) }
    }

    @Test
    fun annotatedString_plus_concatenation() {
        benchmarkRule.measureRepeated { sink = styledAnnotatedString + suffixAnnotatedString }
    }

    @Test
    fun multiParagraphIntrinsics_plain_and_styled() {
        benchmarkRule.measureRepeated {
            sink =
                MultiParagraphIntrinsics(
                    annotatedString = styledAnnotatedString,
                    style = resolvedFastTextStyleStart,
                    placeholders = emptyList(),
                    density = defaultDensity,
                    fontFamilyResolver = fontFamilyResolver,
                    softWrap = true,
                    defaultLocaleList = defaultLocaleList,
                )
        }
    }

    @Test
    fun multiParagraphIntrinsics_multiParagraph_withExtras_slow() {
        benchmarkRule.measureRepeated {
            sink =
                MultiParagraphIntrinsics(
                    annotatedString = multiParagraphSlowAnnotatedString,
                    style = resolvedSlowTextStyle,
                    placeholders = emptyList(),
                    density = defaultDensity,
                    fontFamilyResolver = fontFamilyResolver,
                    softWrap = true,
                    defaultLocaleList = defaultLocaleList,
                )
        }
    }

    @Test
    fun annotatedString_getLinkAnnotations() {
        val textLength = saverRichAnnotatedString.length
        benchmarkRule.measureRepeated {
            sink = saverRichAnnotatedString.getLinkAnnotations(0, textLength)
        }
    }

    @Test
    fun multiParagraph_placeholders_layout() {
        val layoutConstraints = Constraints(maxWidth = 600)
        val paragraphIntrinsics =
            MultiParagraphIntrinsics(
                annotatedString = styledAnnotatedString,
                style = resolvedFastTextStyleStart,
                placeholders = samplePlaceholders,
                density = defaultDensity,
                fontFamilyResolver = fontFamilyResolver,
                softWrap = true,
                defaultLocaleList = defaultLocaleList,
            )
        benchmarkRule.measureRepeated {
            sink =
                MultiParagraph(
                    intrinsics = paragraphIntrinsics,
                    constraints = layoutConstraints,
                    maxLines = 4,
                )
        }
    }

    @Test
    fun annotatedString_builder_bulletList() {
        benchmarkRule.measureRepeated {
            sink = buildAnnotatedString {
                withBulletList {
                    withBulletListItem {
                        withStyle(baseSpanStyle) { append("First bullet ") }
                        withAnnotation(sampleTtsAnnotation) { append("Compose") }
                    }
                    withBulletListItem {
                        withStyle(boldSpanStyle) { append("Second bullet ") }
                        withAnnotation("topic", "perf") { append("Speed") }
                    }
                }
            }
        }
    }

    @Test
    fun annotatedString_saver_saveAndRestore() {
        @Suppress("UNCHECKED_CAST") val saver = AnnotatedString.Saver as Saver<AnnotatedString, Any>
        benchmarkRule.measureRepeated {
            val savedState = with(saver) { allowAllSaverScope.save(saverRichAnnotatedString) }!!
            sink = saver.restore(savedState)
        }
    }

    @Test
    fun textStyle_lerp_fast() {
        benchmarkRule.measureRepeated { sink = lerp(baseTextStyle, baseTextStyleCopy, 0.5f) }
    }

    @Test
    fun textStyle_lerp_withExtras_slow() {
        val targetSlowTextStyle = slowTextStyleRight.copy(letterSpacing = 0.24.em)
        benchmarkRule.measureRepeated { sink = lerp(slowTextStyleLeft, targetSlowTextStyle, 0.35f) }
    }

    @Suppress("DEPRECATION")
    @Test
    fun annotatedString_toUpperCase() {
        benchmarkRule.measureRepeated {
            sink = styledAnnotatedString.toUpperCase(defaultLocaleList)
        }
    }
}
