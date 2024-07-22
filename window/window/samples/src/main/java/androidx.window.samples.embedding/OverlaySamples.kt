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

package androidx.window.samples.embedding

import android.app.Activity
import android.content.Intent
import android.graphics.Rect
import androidx.annotation.Sampled
import androidx.core.app.ActivityOptionsCompat
import androidx.window.embedding.EmbeddingBounds
import androidx.window.embedding.EmbeddingBounds.Dimension.Companion.ratio
import androidx.window.embedding.OverlayAttributes
import androidx.window.embedding.OverlayController
import androidx.window.embedding.OverlayCreateParams
import androidx.window.embedding.setOverlayCreateParams

@Sampled
fun launchOverlayActivityStackSample() {
    // Creates an overlay container on the right
    val params =
        OverlayCreateParams(
            overlayAttributes =
                OverlayAttributes(
                    EmbeddingBounds(
                        alignment = EmbeddingBounds.Alignment.ALIGN_RIGHT,
                        width = ratio(0.5f),
                        height = EmbeddingBounds.Dimension.DIMENSION_EXPANDED,
                    )
                )
        )

    val optionsWithOverlayParams =
        ActivityOptionsCompat.makeBasic()
            .toBundle()
            ?.setOverlayCreateParams(launchingActivity, params)

    // Start INTENT to the overlay container specified by params.
    launchingActivity.startActivity(INTENT, optionsWithOverlayParams)
}

@Sampled
fun overlayAttributesCalculatorSample() {
    // A sample to show overlay on the bottom if the device is portrait, and on the right when
    // the device is landscape.
    OverlayController.getInstance(launchingActivity).setOverlayAttributesCalculator { params ->
        val taskBounds = params.parentWindowMetrics.bounds
        return@setOverlayAttributesCalculator OverlayAttributes(
            if (taskBounds.isPortrait()) {
                EmbeddingBounds(
                    alignment = EmbeddingBounds.Alignment.ALIGN_BOTTOM,
                    width = EmbeddingBounds.Dimension.DIMENSION_EXPANDED,
                    height = ratio(0.5f),
                )
            } else {
                EmbeddingBounds(
                    alignment = EmbeddingBounds.Alignment.ALIGN_RIGHT,
                    width = ratio(0.5f),
                    height = EmbeddingBounds.Dimension.DIMENSION_EXPANDED,
                )
            }
        )
    }
}

private fun Rect.isPortrait(): Boolean = height() >= width()

val launchingActivity: Activity = Activity()

val INTENT = Intent()
