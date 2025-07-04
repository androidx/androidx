/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.pdf.compose

import android.content.Context
import android.graphics.drawable.Drawable
import android.net.Uri
import androidx.annotation.DimenRes
import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastRoundToInt
import androidx.compose.ui.viewinterop.AndroidView
import androidx.pdf.PdfDocument
import androidx.pdf.R
import androidx.pdf.content.ExternalLink
import androidx.pdf.view.PdfView
import kotlin.random.Random

/**
 * A [Composable] that presents PDF content, provided as [PdfDocument]
 *
 * @param pdfDocument the PDF content to present
 * @param state the state object used to observe and control content position
 * @param modifier the [Modifier] to be applied to this PDF viewer
 * @param minZoom the minimum zoom / scaling factor that can be applied to the PDF viewer
 * @param maxZoom the maximum zoom / scaling factor that can be applied to the PDF viewer
 * @param fastScrollConfig a [FastScrollConfiguration] instance to customize the fast scoller's
 *   appearance
 * @param onUrlLinkClicked a callback to be invoked when the user taps a URL link in this PDF viewer
 */
@Composable
public fun PdfViewer(
    pdfDocument: PdfDocument?,
    state: PdfViewerState,
    modifier: Modifier = Modifier,
    minZoom: Float = PdfView.DEFAULT_MIN_ZOOM,
    maxZoom: Float = PdfView.DEFAULT_MAX_ZOOM,
    fastScrollConfig: FastScrollConfiguration =
        FastScrollConfiguration.withDrawableAndDimensionIds(),
    onUrlLinkClicked: ((Uri) -> Boolean)? = null,
) {
    // Create and remember an ID for PdfView so that it retains state across compositions and
    // recreations
    val pdfViewId = rememberSaveable { Random(System.currentTimeMillis()).nextInt() }
    // Only reload fast scroll resources when we need to, i.e. when the developer provides new
    // values or when our Context changes.
    val context = LocalContext.current
    val pageIndicatorDrawable =
        remember(fastScrollConfig, context) {
            fastScrollConfig.pageIndicatorBackgroundDrawable(context)
        }
    val verticalThumbDrawable =
        remember(fastScrollConfig, context) { fastScrollConfig.verticalThumbDrawable(context) }
    val pageIndicatorMarginEnd =
        remember(fastScrollConfig, context) { fastScrollConfig.pageIndicatorMarginEnd(context) }
    val verticalThumbMarginEnd =
        remember(fastScrollConfig, context) { fastScrollConfig.verticalThumbMarginEnd(context) }
    AndroidView(
        modifier = modifier,
        factory = { context ->
            PdfView(context).apply {
                this.id = pdfViewId
                state.pdfView = this
            }
        },
        onRelease = { view ->
            if (view == state.pdfView) state.pdfView = null
            view.setLinkClickListener(null)
        },
        // Factory will execute exactly once; update is the correct place to supply mutable states
        update = { view ->
            view.pdfDocument = pdfDocument
            view.minZoom = minZoom
            view.maxZoom = maxZoom
            view.fastScrollVerticalThumbDrawable = verticalThumbDrawable
            view.fastScrollPageIndicatorBackgroundDrawable = pageIndicatorDrawable
            view.fastScrollPageIndicatorMarginEnd = pageIndicatorMarginEnd
            view.fastScrollVerticalThumbMarginEnd = verticalThumbMarginEnd
            view.setLinkClickListener(PdfViewerLinkClickListener(onUrlLinkClicked))
        },
    )
}

/**
 * Value class describing visual customization to the fast scrolling affordance. Namely, the page
 * indicator and visual thumb background images and end margins can be customized.
 *
 * @see [withDrawableAndDimensionIds]
 * @see [withDrawableIdsAndDp]
 */
public class FastScrollConfiguration
private constructor(
    private val pageIndicatorImageSpec: FastScrollImageSpec,
    private val verticalThumbImageSpec: FastScrollImageSpec,
    private val pageIndicatorMarginEndSpec: FastScrollDimensionSpec,
    private val verticalThumbMarginEndSpec: FastScrollDimensionSpec,
) {
    internal fun pageIndicatorBackgroundDrawable(context: Context): Drawable {
        return pageIndicatorImageSpec.getDrawable(context)
    }

    internal fun verticalThumbDrawable(context: Context): Drawable {
        return verticalThumbImageSpec.getDrawable(context)
    }

    internal fun pageIndicatorMarginEnd(context: Context): Int {
        return pageIndicatorMarginEndSpec.getPixelSize(context)
    }

    internal fun verticalThumbMarginEnd(context: Context): Int {
        return verticalThumbMarginEndSpec.getPixelSize(context)
    }

    private sealed interface FastScrollDimensionSpec {
        fun getPixelSize(context: Context): Int
    }

    private class ResIdDimensionSpec(@DimenRes private val dimenResId: Int) :
        FastScrollDimensionSpec {
        override fun getPixelSize(context: Context): Int {
            return context.resources.getDimensionPixelSize(dimenResId)
        }
    }

    private class DpDimensionSpec(private val dimenDp: Dp) : FastScrollDimensionSpec {
        override fun getPixelSize(context: Context): Int {
            return dimenDp.times(context.resources.displayMetrics.density).value.fastRoundToInt()
        }
    }

    private sealed interface FastScrollImageSpec {
        fun getDrawable(context: Context): Drawable
    }

    private class DrawableImageSpec(@DrawableRes private val drawableRes: Int) :
        FastScrollImageSpec {
        override fun getDrawable(context: Context): Drawable {
            return requireNotNull(context.getDrawable(drawableRes)) { "Invalid drawable resource" }
        }
    }

    public companion object {
        /**
         * Instantiates a [FastScrollConfiguration] using [Drawable] resource IDs to describe the
         * backgrounds for the page indicator and vertical thumb of the fast scroller, and using
         * dimension resource IDs to describe the end margin applied to the same elements.
         */
        public fun withDrawableAndDimensionIds(
            @DrawableRes
            fastScrollPageIndicatorBackgroundDrawableRes: Int =
                R.drawable.page_indicator_background,
            @DrawableRes
            fastScrollVerticalThumbDrawableRes: Int = R.drawable.fast_scroll_thumb_drawable,
            @DimenRes
            fastScrollPageIndicatorMarginEndRes: Int = R.dimen.page_indicator_right_margin,
            @DimenRes fastScrollVerticalThumbMarginEndRes: Int = R.dimen.scroll_thumb_margin_end,
        ): FastScrollConfiguration {
            return FastScrollConfiguration(
                DrawableImageSpec(fastScrollPageIndicatorBackgroundDrawableRes),
                DrawableImageSpec(fastScrollVerticalThumbDrawableRes),
                ResIdDimensionSpec(fastScrollPageIndicatorMarginEndRes),
                ResIdDimensionSpec(fastScrollVerticalThumbMarginEndRes),
            )
        }

        /**
         * Instantiates a [FastScrollConfiguration] using [Drawable] resource IDs to describe the
         * backgrounds for the page indicator and vertical thumb of the fast scroller, and using
         * [Dp] values to describe the end margin applied to the same elements.
         */
        public fun withDrawableIdsAndDp(
            @DrawableRes
            fastScrollPageIndicatorBackgroundDrawableRes: Int =
                R.drawable.page_indicator_background,
            @DrawableRes
            fastScrollVerticalThumbDrawableRes: Int = R.drawable.fast_scroll_thumb_drawable,
            fastScrollPageIndicatorMarginEnd: Dp = 42.dp,
            fastScrollVerticalThumbMarginEnd: Dp = 0.dp,
        ): FastScrollConfiguration {
            return FastScrollConfiguration(
                DrawableImageSpec(fastScrollPageIndicatorBackgroundDrawableRes),
                DrawableImageSpec(fastScrollVerticalThumbDrawableRes),
                DpDimensionSpec(fastScrollPageIndicatorMarginEnd),
                DpDimensionSpec(fastScrollVerticalThumbMarginEnd),
            )
        }
    }
}

/**
 * Bridge between the lambda-based [PdfViewer] API for custom link handling, and the listener
 * interface [PdfView] API for the same.
 *
 * Notably this defers to the default behavior in [PdfView] in the event [behavior] is null, i.e. by
 * returning false from [onLinkClicked]
 */
private class PdfViewerLinkClickListener(private val behavior: ((Uri) -> Boolean)?) :
    PdfView.LinkClickListener {
    override fun onLinkClicked(externalLink: ExternalLink): Boolean {
        return behavior?.invoke(externalLink.uri) ?: false
    }
}
