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

package androidx.compose.foundation.text

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isClick
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.layout.ParentDataModifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.UrlAnnotation
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.roundToIntRect
import androidx.compose.ui.util.fastForEach
import kotlin.math.min

@OptIn(ExperimentalTextApi::class)
internal typealias LinkRange = AnnotatedString.Range<UrlAnnotation>

/** A scope that provides necessary information to attach a hyperlink to the text range */
@OptIn(ExperimentalTextApi::class)
internal class TextLinkScope(val text: AnnotatedString) {
    var textLayoutResult: TextLayoutResult? by mutableStateOf(null)

    /**
     * Causes the modified element to be measured with fixed constraints equal to the bounds of the
     * text range [[start], [end]) and placed over that range of text.
     */
    private fun Modifier.textRange(start: Int, end: Int): Modifier = this.then(
        TextRangeLayoutModifier {
            val layoutResult = textLayoutResult
                ?: return@TextRangeLayoutModifier layout(0, 0) { IntOffset.Zero }
            val bounds = layoutResult.getPathForRange(start, end).getBounds().roundToIntRect()
            layout(bounds.width, bounds.height) { bounds.topLeft }
        }
    )

    private fun shapeForRange(range: LinkRange): Shape? =
        pathForRangeInRangeCoordinates(range)?.let {
            object : Shape {
                override fun createOutline(
                    size: Size,
                    layoutDirection: LayoutDirection,
                    density: Density
                ): Outline {
                    return Outline.Generic(it)
                }
            }
        }

    private fun pathForRangeInRangeCoordinates(range: LinkRange): Path? =
        textLayoutResult?.let {
            val path = it.getPathForRange(range.start, range.end)

            val firstCharBoundingBox = it.getBoundingBox(range.start)
            val minTop = firstCharBoundingBox.top
            var minLeft = firstCharBoundingBox.left
            val firstLine = it.getLineForOffset(range.start)
            val lastLine = it.getLineForOffset(range.end)
            // might be enough to just check if the second line exist
            // if yes - take it's left bound or even just 0
            // TODO(soboleva) check in RTL
            for (line in firstLine + 1..lastLine) {
                val lineLeft = it.getLineLeft(line)
                minLeft = min(minLeft, lineLeft)
            }

            path.translate(-Offset(minLeft, minTop))
            return path
        }

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    fun LinksComposables() = with(this) {
        val indication = LocalIndication.current
        val uriHandler = LocalUriHandler.current
        val links = text.getUrlAnnotations(0, text.length)
        links.fastForEach { range ->
            val interactionSource = remember(range) { MutableInteractionSource() }
            val shape = shapeForRange(range)
            val clipModifier = shape?.let { Modifier.clip(it) } ?: Modifier
            Box(
                clipModifier
                    .focusable(true, interactionSource)
                    .textRange(range.start, range.end)
                    .combinedClickable(interactionSource, indication, onClick = {
                        uriHandler.openUri(range.item.url)
                    })
                    .onKeyEvent {
                        // TODO(soboleva) do we need press indication?
                        when {
                            // TODO(soboleva) support Clickables as well
                            it.isClick -> {
                                uriHandler.openUri(range.item.url)
                                true
                            }

                            else -> false
                        }
                    }
            )
        }
    }
}

/**
 * Interface holding the width, height and positioning logic.
 */
internal class TextRangeLayoutMeasureResult internal constructor(
    val width: Int,
    val height: Int,
    val place: () -> IntOffset
)

/**
 * The receiver scope of a text range layout's measure lambda. The return value of the
 * measure lambda is [TextRangeLayoutMeasureResult], which should be returned by [layout]
 */
internal class TextRangeLayoutMeasureScope {
    fun layout(width: Int, height: Int, place: () -> IntOffset): TextRangeLayoutMeasureResult =
        TextRangeLayoutMeasureResult(width, height, place)
}

/** Provides the size and placement for an element inside a [TextLinkScope] */
internal fun interface TextRangeScopeMeasurePolicy {
    fun TextRangeLayoutMeasureScope.measure(): TextRangeLayoutMeasureResult
}

internal class TextRangeLayoutModifier(val measurePolicy: TextRangeScopeMeasurePolicy) :
    ParentDataModifier {
    override fun Density.modifyParentData(parentData: Any?) = this@TextRangeLayoutModifier
}
