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
import androidx.compose.foundation.TextLinkClickHandler
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.layout.ParentDataModifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.textSelectionRange
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.roundToIntRect
import androidx.compose.ui.util.fastForEach

internal typealias LinkRange = AnnotatedString.Range<LinkAnnotation>

/**
 * A scope that provides necessary information to attach a hyperlink to the text range.
 *
 * This class assumes that links exist and does not perform any additional check inside its methods.
 * Therefore this class initialisation should be guarded by the `hasLinks` check.
 */
@OptIn(ExperimentalFoundationApi::class)
internal class TextLinkScope(
    val text: AnnotatedString,
    private val linkClickHandler: TextLinkClickHandler?
) {
    var textLayoutResult: TextLayoutResult? by mutableStateOf(null)

    // indicates whether the links should be measured or not. The latter needed to handle
    // case where translated string forces measurement before the recomposition. Recomposition in
    // this case will dispose the links altogether because translator returns plain text
    val shouldMeasureLinks: () -> Boolean
        get() = { text == textLayoutResult?.layoutInput?.text }

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

    private fun pathForRangeInRangeCoordinates(range: LinkRange): Path? {
        return if (!shouldMeasureLinks()) null else {
            textLayoutResult?.let {
                val path = it.getPathForRange(range.start, range.end)

                val firstCharBoundingBox = it.getBoundingBox(range.start)
                val lastCharBoundingBox = it.getBoundingBox(range.end - 1)

                val rangeStartLine = it.getLineForOffset(range.start)
                val rangeEndLine = it.getLineForOffset(range.end)

                val xOffset = if (rangeStartLine == rangeEndLine) {
                    // if the link occupies a single line, we take the left most position of the
                    // link's range
                    minOf(lastCharBoundingBox.left, firstCharBoundingBox.left)
                } else {
                    // if the link occupies more than one line, the left sides of the link node and
                    // text node match so we don't need to do anything
                    0f
                }

                // the top of the top-most (first) character
                val yOffset = firstCharBoundingBox.top

                path.translate(-Offset(xOffset, yOffset))
                return path
            }
        }
    }

    /**
     * This composable responsible for creating layout nodes for each link annotation. Since
     * [TextLinkScope] object created *only* when there are links present in the text, we don't
     * need to do any additional guarding inside this composable function.
     */
    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    fun LinksComposables() {
        val indication = LocalIndication.current
        val uriHandler = LocalUriHandler.current

        val links = text.getLinkAnnotations(0, text.length)
        links.fastForEach { range ->
            val shape = shapeForRange(range)
            val clipModifier = shape?.let { Modifier.clip(it) } ?: Modifier
            Box(
                clipModifier
                    .textRange(range.start, range.end)
                    .semantics {
                        linkClickHandler?.let {
                            customActions = listOf(
                                // this action will be passed down to the Talkback through the
                                // ClickableSpan's onClick method
                                CustomAccessibilityAction("") {
                                    it.onClick(range.item)
                                    true
                                }
                            )
                            textSelectionRange = TextRange(range.start, range.end)
                        }
                    }
                    .pointerHoverIcon(PointerIcon.Hand)
                    .combinedClickable(null, indication, onClick = {
                        handleLink(range.item, uriHandler, linkClickHandler)
                    })
            )
        }
    }

    private fun handleLink(
        link: LinkAnnotation,
        uriHandler: UriHandler,
        clickHandler: TextLinkClickHandler?
    ) {
        when (link) {
            is LinkAnnotation.Url -> {
                // if a handler is present, we delegate link handling to it. If not, we try to
                // handle links ourselves. And if we can't (the uri is invalid or there's no app to
                // handle such a uri), we silently fail
                clickHandler?.onClick(link)
                    ?: try {
                        uriHandler.openUri(link.url)
                    } catch (_: IllegalArgumentException) {
                        // we choose to silently fail when the uri can't be opened to avoid crashes
                        // for users. This is the case where developer don't provide the link
                        // handlers themselves and therefore I suspect are less likely to test them
                        // manually.
                    }
            }
            is LinkAnnotation.Clickable -> clickHandler?.onClick(link)
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
