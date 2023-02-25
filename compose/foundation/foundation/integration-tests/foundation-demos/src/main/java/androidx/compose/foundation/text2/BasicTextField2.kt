/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.foundation.text2

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text2.service.AndroidTextInputPlugin
import androidx.compose.foundation.text2.service.TextInputSession
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.layout.LastBaseline
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.platform.LocalPlatformTextInputPluginRegistry
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.Paragraph
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.ceil
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

/**
 * @param enabled controls the enabled state of the text field. When `false`, the text
 * field will be neither editable nor focusable, the input of the text field will not be selectable
 */
@OptIn(
    ExperimentalTextApi::class,
    ExperimentalFoundationApi::class
)
@Composable
fun BasicTextField2(
    state: TextFieldState,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = TextStyle.Default,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource? = null,
    onTextLayout: (TextLayoutResult) -> Unit = {}
) {
    val textInputAdapter = LocalPlatformTextInputPluginRegistry.current
        .rememberAdapter(AndroidTextInputPlugin)
    val focusRequester = remember { FocusRequester() }

    // Caching is not useful for dynamic content. We keep cache for a single layout when the
    // content is not edited but layout phase still occurs.
    // b/261581753
    val textMeasurer = rememberTextMeasurer(cacheSize = 1)
    val scope = rememberCoroutineScope()
    val fontFamilyResolver = LocalFontFamilyResolver.current
    val density = LocalDensity.current
    val bringIntoViewRequester = remember { BringIntoViewRequester() }

    val textLayoutState = remember {
        TextLayoutState(
            TextMeasureParams(
                text = state.value.annotatedString,
                style = textStyle,
                density = density,
                fontFamilyResolver = fontFamilyResolver,
                softWrap = true,
                placeholders = emptyList()
            )
        )
    }

    textLayoutState.updateParams(
        visualText = state.value.annotatedString,
        textStyle = textStyle,
        softWrap = true,
        density = density,
        fontFamilyResolver = fontFamilyResolver
    )

    var isFocused by remember { mutableStateOf(false) }
    val borderWidth by animateFloatAsState(targetValue = if (isFocused) 2f else 0f)

    val textInputSessionState = remember { mutableStateOf<TextInputSession?>(null) }

    DisposableEffect(state) {
        // restart input if it's active
        val textInputSession = textInputSessionState.value
        if (textInputSession != null) {
            textInputSession.dispose()
            if (isFocused) {
                textInputSessionState.value = textInputAdapter.startInputSession(state)
            }
        }
        onDispose { }
    }

    val drawModifier = Modifier.drawBehind {
        textLayoutState.layoutResult?.let { layoutResult ->
            // TODO: draw selection
            drawText(layoutResult)
        }
    }

    val focusModifier = Modifier
        .focusRequester(focusRequester)
        .onFocusChanged {
            if (isFocused == it.isFocused) {
                return@onFocusChanged
            }
            isFocused = it.isFocused

            if (it.isFocused) {
                textInputSessionState.value = textInputAdapter.startInputSession(state)
            }

            // The focusable modifier itself will request the entire focusable be brought into view
            // when it gains focus – in this case, that's the decoration box. However, since text
            // fields may have their own internal scrolling, and the decoration box can do anything,
            // we also need to specifically request that the cursor itself be brought into view.
            // TODO(b/216790855) If this request happens after the focusable's request, the field
            //  will only be scrolled far enough to show the cursor, _not_ the entire decoration
            //  box.
            if (it.isFocused) {
                textLayoutState.layoutResult?.let { layoutResult ->
                    scope.launch {
                        val selectionEnd = state.value.selection.max
                        val selectionEndBounds = when {
                            selectionEnd < layoutResult.layoutInput.text.length -> {
                                layoutResult.getBoundingBox(selectionEnd)
                            }

                            selectionEnd != 0 -> {
                                layoutResult.getBoundingBox(selectionEnd - 1)
                            }

                            else -> { // empty text.
                                val defaultSize = computeSizeForDefaultText(
                                    textLayoutState.textMeasureParams.style,
                                    textLayoutState.textMeasureParams.density,
                                    textLayoutState.textMeasureParams.fontFamilyResolver
                                )
                                Rect(0f, 0f, 1.0f, defaultSize.height.toFloat())
                            }
                        }
                        bringIntoViewRequester.bringIntoView(selectionEndBounds)
                    }
                }
            } else {
                state.deselect()
            }
        }
        .focusable(interactionSource = interactionSource, enabled = enabled)

    Layout(
        content = {},
        modifier = modifier
            .then(focusModifier)
            .then(drawModifier)
            .clickable { focusRequester.requestFocus() }
            .border(
                width = borderWidth.dp,
                color = LocalTextSelectionColors.current.backgroundColor
            )
    ) { _, constraints ->
        val result = with(textLayoutState) { layout(textMeasurer, constraints, onTextLayout) }

        // TODO: min height

        layout(
            width = result.size.width,
            height = result.size.height,
            alignmentLines = mapOf(
                FirstBaseline to result.firstBaseline.roundToInt(),
                LastBaseline to result.lastBaseline.roundToInt()
            )
        ) {}
    }
}

@OptIn(ExperimentalTextApi::class)
internal class TextLayoutState(
    initialTextMeasureParams: TextMeasureParams
) {
    /**
     * Set of parameters to compute text layout.
     */
    var textMeasureParams: TextMeasureParams by mutableStateOf(initialTextMeasureParams)
        private set

    /**
     * TextFieldState holds both TextDelegate and layout result. However, these two values are not
     * updated at the same time. TextDelegate is updated during composition according to new
     * arguments while layoutResult is updated during layout phase. Therefore, [layoutResult] might
     * not indicate the layout calculated from [textMeasureParams] at a given time during
     * composition. This variable tells whether layout result is in sync with the latest
     * [TextMeasureParams].
     */
    private var isLayoutResultStale: Boolean = true

    var layoutResult: TextLayoutResult? by mutableStateOf(null)
        private set

    fun updateParams(
        visualText: AnnotatedString,
        textStyle: TextStyle,
        softWrap: Boolean,
        density: Density,
        fontFamilyResolver: FontFamily.Resolver
    ) {
        if (!textMeasureParams.layoutCompatible(
                text = visualText,
                style = textStyle,
                softWrap = softWrap,
                density = density,
                fontFamilyResolver = fontFamilyResolver,
                placeholders = emptyList()
        )) {
            val newTextMeasureParams = TextMeasureParams(
                text = visualText,
                style = textStyle,
                softWrap = softWrap,
                density = density,
                fontFamilyResolver = fontFamilyResolver,
                placeholders = emptyList(),
            )
            isLayoutResultStale = true
            textMeasureParams = newTextMeasureParams
        }
    }

    fun MeasureScope.layout(
        textMeasurer: TextMeasurer,
        constraints: Constraints,
        onTextLayout: (TextLayoutResult) -> Unit
    ): TextLayoutResult {
        val prevResult = Snapshot.withoutReadObservation { layoutResult }
        val result = if (isLayoutResultStale || prevResult == null) {
            textMeasurer.measure(
                text = textMeasureParams.text,
                style = textMeasureParams.style,
                softWrap = textMeasureParams.softWrap,
                density = this,
                fontFamilyResolver = textMeasureParams.fontFamilyResolver,
                layoutDirection = layoutDirection,
                constraints = constraints
            ).also {
                layoutResult = it
                isLayoutResultStale = false
                onTextLayout(it)
            }
        } else {
            prevResult
        }
        return result
    }
}

internal data class TextMeasureParams(
    val text: AnnotatedString,
    val style: TextStyle,
    val softWrap: Boolean,
    val density: Density,
    val fontFamilyResolver: FontFamily.Resolver,
    val placeholders: List<Placeholder>
) {
    /**
     * Returns whether given parameters and values in this [TextMeasureParams] would create the same
     * layout or they are layout incompatible.
     */
    fun layoutCompatible(
        text: AnnotatedString,
        style: TextStyle,
        softWrap: Boolean,
        density: Density,
        fontFamilyResolver: FontFamily.Resolver,
        placeholders: List<Placeholder>
    ): Boolean {
        return this.text == text &&
            this.style.hasSameLayoutAffectingAttributes(style) &&
            this.softWrap == softWrap &&
            this.density == density &&
            this.fontFamilyResolver == fontFamilyResolver &&
            this.placeholders == placeholders
    }
}

/**
 * Computed the default width and height for TextField.
 *
 * The bounding box or x-advance of the empty text is empty, i.e. 0x0 box or 0px advance. However
 * this is not useful for TextField since text field want to reserve some amount of height for
 * accepting touch for starting text input. In Android, uses FontMetrics of the first font in the
 * fallback chain to compute this height, this is because custom font may have different
 * ascender/descender from the default font in Android.
 *
 * Until we have font metrics APIs, use the height of reference text as a workaround.
 */
internal fun computeSizeForDefaultText(
    style: TextStyle,
    density: Density,
    fontFamilyResolver: FontFamily.Resolver,
    text: String = EmptyTextReplacement,
    maxLines: Int = 1
): IntSize {
    val paragraph = Paragraph(
        text = text,
        style = style,
        spanStyles = listOf(),
        maxLines = maxLines,
        ellipsis = false,
        density = density,
        fontFamilyResolver = fontFamilyResolver,
        constraints = Constraints()
    )
    return IntSize(paragraph.minIntrinsicWidth.ceilToIntPx(), paragraph.height.ceilToIntPx())
}

internal fun Float.ceilToIntPx(): Int = ceil(this).roundToInt()
internal const val DefaultWidthCharCount = 10 // min width for TextField is 10 chars long
internal val EmptyTextReplacement = "H".repeat(DefaultWidthCharCount) // just a reference character.