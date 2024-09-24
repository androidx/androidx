/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.graphics.shapes

// @TODO: Make class public as soon as all validations are implemented

/**
 * Utility class to fix invalid [RoundedPolygon]s that will otherwise break [Morph]s in one way or
 * another, as [RoundedPolygon] assumes correct input. Correct input meaning:
 * - Closed geometry
 * - Clockwise orientation of points
 * - No self-intersections
 * - No holes
 */
internal class PolygonValidator() {

    companion object {

        // @TODO: Update docs when other validations are implemented
        /**
         * Validates whether this [RoundedPolygon]'s orientation is clockwise and fixes it if
         * necessary.
         *
         * @param polygon The [RoundedPolygon] to validate
         * @return A new [RoundedPolygon] with fixed orientation, or the same [RoundedPolygon] as
         *   given when it was already valid
         */
        fun fix(polygon: RoundedPolygon): RoundedPolygon {
            var result = polygon

            debugLog(LOG_TAG) { "Validating polygon..." }

            if (isCWOriented(polygon)) {
                debugLog(LOG_TAG) { "Passed clockwise validation!" }
            } else {
                debugLog(LOG_TAG) { "Polygon is oriented anti-clockwise, fixing orientation..." }
                result = fixCWOrientation(polygon)
            }

            return result
        }

        private fun isCWOriented(polygon: RoundedPolygon): Boolean {
            var signedArea = 0.0f

            for (i in polygon.cubics.indices) {
                val cubic = polygon.cubics[i]
                signedArea += (cubic.anchor1X - cubic.anchor0X) * (cubic.anchor1Y + cubic.anchor0Y)
            }

            return signedArea < 0
        }

        private fun fixCWOrientation(polygon: RoundedPolygon): RoundedPolygon {
            // Persist first feature to stay a Corner
            val reversedFeatures = mutableListOf(polygon.features.first().reversed())

            for (i in polygon.features.lastIndex downTo 1) {
                reversedFeatures.add(polygon.features[i].reversed())
            }

            return RoundedPolygon(reversedFeatures, polygon.centerX, polygon.centerY)
        }
    }
}

private const val LOG_TAG = "PolygonValidation"
