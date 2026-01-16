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

package androidx.core.pip

import android.graphics.Rect
import android.util.Rational
import androidx.core.app.PictureInPictureParamsCompat
import kotlin.math.roundToInt

/** Utility class to validate the [PictureInPictureParamsCompat] instance. */
internal class PictureInPictureParamsValidator {
    companion object {
        /** Minimal aspect ratio for [PictureInPictureParamsCompat.Builder.setAspectRatio] */
        val MIN_ASPECT_RATIO: Rational = Rational(100, 239)

        /** Maximum aspect ratio for [PictureInPictureParamsCompat.Builder.setAspectRatio] */
        val MAX_ASPECT_RATIO: Rational = Rational(239, 100)

        /**
         * Default aspect ratio if it's not provided via [PictureInPictureParamsCompat.aspectRatio]
         */
        val DEFAULT_ASPECT_RATIO: Rational = Rational(16, 9)

        /**
         * Calculates the largest possible Rect that fits within the [hint] while maintaining the
         * specified [aspectRatio]. The resulting Rect is centered within the [hint].
         *
         * @param hint The source rect hint Rect.
         * @param aspectRatio The desired width/height ratio.
         * @return A new Rect instance representing the largest possible area with the given
         *   aspectRatio, centered within hint. `null` if the hint has zero or negative width/height
         * @throws IllegalArgumentException if the aspectRatio has non-positive numerator or
         *   denominator.
         */
        @JvmStatic
        private fun centerCrop(aspectRatio: Rational, hint: Rect): Rect? {
            if (aspectRatio.numerator <= 0 || aspectRatio.denominator <= 0) {
                throw IllegalArgumentException("Invalidated aspect ratio $aspectRatio")
            }

            val origW = hint.width()
            val origH = hint.height()

            if (origW <= 0 || origH <= 0) {
                return null // Cannot form a valid rect from a zero or negative size one
            }

            val num =
                aspectRatio.numerator.toLong() // Use Long to prevent overflow during comparison
            val den = aspectRatio.denominator.toLong()

            val newW: Int
            val newH: Int

            // Compare target ratio (num/den) vs original ratio (origW/origH)
            // To avoid floating point issues in comparison, we cross-multiply:
            // num/den vs origW/origH  =>  num * origH  vs  origW * den
            val targetRatioProduct = num * origH
            val originalRatioProduct = den * origW

            if (targetRatioProduct < originalRatioProduct) {
                // Target aspect ratio (num/den) is "taller" or less wide than the original
                // (origW/origH).
                // This means num/den < origW/origH.
                // The height of the new Rect will be limited by originalRect.height().
                newH = origH
                // Calculate newW based on newH and target aspectRatio
                newW =
                    ((origH.toDouble() * aspectRatio.numerator) / aspectRatio.denominator)
                        .roundToInt()
            } else if (targetRatioProduct > originalRatioProduct) {
                // Target aspect ratio (num/den) is "wider" than the original (origW/origH).
                // This means num/den > origW/origH.
                // The width of the new Rect will be limited by originalRect.width().
                newW = origW
                // Calculate newH based on newW and target aspectRatio
                newH =
                    ((origW.toDouble() * aspectRatio.denominator) / aspectRatio.numerator)
                        .roundToInt()
            } else {
                // Aspect ratios are the same. The new Rect is the originalRect.
                newW = origW
                newH = origH
            }

            // Calculate the top-left corner to center the new Rect within the originalRect.
            val dX = origW - newW
            val dY = origH - newH

            val left = hint.left + dX / 2
            val top = hint.top + dY / 2

            // The right and bottom are calculated from left, top, newW, and newH
            // to ensure the dimensions are correct after integer division of dX/2, dY/2.
            val right = left + newW
            val bottom = top + newH

            return Rect(left, top, right, bottom)
        }

        /**
         * Validates the [PictureInPictureParamsCompat] instances.
         *
         * @param originalParams The original [PictureInPictureParamsCompat] instance.
         * @return The validated [PictureInPictureParamsCompat] instance.
         */
        @JvmStatic
        fun validate(originalParams: PictureInPictureParamsCompat): PictureInPictureParamsCompat {
            val builder: PictureInPictureParamsCompat.Builder =
                PictureInPictureParamsCompat.Builder()
            // Validate the aspect ratio is within the range.
            val originalAspectRatio: Rational = originalParams.aspectRatio ?: DEFAULT_ASPECT_RATIO
            val validatedAspectRatio: Rational =
                originalAspectRatio.coerceIn(MIN_ASPECT_RATIO, MAX_ASPECT_RATIO)
            builder.setAspectRatio(validatedAspectRatio)

            // Validate the source rect hint matches the aspect ratio.
            val originalSourceRectHint: Rect? = originalParams.sourceRectHint
            if (originalSourceRectHint != null && !originalSourceRectHint.isEmpty) {
                builder.setSourceRectHint(centerCrop(validatedAspectRatio, originalSourceRectHint))
            } else {
                builder.setSourceRectHint(null)
            }

            // Copy the other parameters and return the validated instance
            builder
                .setEnabled(originalParams.isEnabled)
                .setActions(originalParams.actions)
                .setSeamlessResizeEnabled(originalParams.isSeamlessResizeEnabled)
                .setCloseAction(originalParams.closeAction)
                .setExpandedAspectRatio(originalParams.expandedAspectRatio)
                .setTitle(originalParams.title)
                .setSubTitle(originalParams.subTitle)
            return builder.build()
        }
    }
}
