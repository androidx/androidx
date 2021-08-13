/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.test.screenshot.matchers

import android.graphics.Bitmap

/**
 * Interface to implement to provide custom bitmap matchers.
 */
interface BitmapMatcher {
    /**
     * Compares the given bitmaps and returns result of the operation.
     *
     * The images need to have same size.
     *
     * @param expected The reference / golden image.
     * @param given The image taken during the test.
     * @param width Width of both of the images.
     * @param height Height of both of the images.
     */
    fun compareBitmaps(expected: IntArray, given: IntArray, width: Int, height: Int): MatchResult
}

/**
 * Result of the matching performed by [BitmapMatcher].
 *
 * @param matches True if bitmaps match.
 * @param comparisonStatistics Matching statistics provided by this matcher that performed the
 * comparison.
 * @param diff Diff bitmap that highlights the differences between the images. Can be null if match
 * was found.
 */
class MatchResult(
    val matches: Boolean,
    val comparisonStatistics: String,
    val diff: Bitmap?
)
