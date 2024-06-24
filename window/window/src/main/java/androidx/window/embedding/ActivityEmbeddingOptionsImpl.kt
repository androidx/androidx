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

package androidx.window.embedding

import android.os.Bundle
import androidx.window.RequiresWindowSdkExtension
import androidx.window.WindowSdkExtensions
import androidx.window.embedding.EmbeddingBounds.Dimension
import androidx.window.embedding.EmbeddingBounds.Dimension.Companion.DIMENSION_EXPANDED
import androidx.window.embedding.EmbeddingBounds.Dimension.Companion.DIMENSION_HINGE
import androidx.window.embedding.OverlayController.Companion.OVERLAY_FEATURE_VERSION
import androidx.window.extensions.embedding.ActivityEmbeddingOptionsProperties.KEY_ACTIVITY_STACK_TOKEN
import androidx.window.extensions.embedding.ActivityEmbeddingOptionsProperties.KEY_OVERLAY_TAG
import androidx.window.extensions.embedding.ActivityStack.Token

/**
 * The implementation of ActivityEmbeddingOptions in WM Jetpack which uses constants defined in
 * [androidx.window.extensions.embedding.ActivityEmbeddingOptionsProperties] and this object.
 */
internal object ActivityEmbeddingOptionsImpl {

    /**
     * Key of [EmbeddingBounds].
     *
     * Type: [Bundle]
     *
     * Properties:
     *
     * | Key                              | Type     | Property                    |
     * |----------------------------------|----------|-----------------------------|
     * | [KEY_EMBEDDING_BOUNDS_ALIGNMENT] | [Int]    | [EmbeddingBounds.alignment] |
     * | [KEY_EMBEDDING_BOUNDS_WIDTH]     | [Bundle] | [EmbeddingBounds.width]     |
     * | [KEY_EMBEDDING_BOUNDS_HEIGHT]    | [Bundle] | [EmbeddingBounds.height]    |
     */
    private const val KEY_EMBEDDING_BOUNDS = "androidx.window.embedding.EmbeddingBounds"

    /**
     * Key of [EmbeddingBounds.alignment].
     *
     * Type: [Int]
     *
     * Valid values are:
     * - 0: [EmbeddingBounds.Alignment.ALIGN_LEFT]
     * - 1: [EmbeddingBounds.Alignment.ALIGN_TOP]
     * - 2: [EmbeddingBounds.Alignment.ALIGN_RIGHT]
     * - 3: [EmbeddingBounds.Alignment.ALIGN_BOTTOM]
     */
    private const val KEY_EMBEDDING_BOUNDS_ALIGNMENT =
        "androidx.window.embedding.EmbeddingBounds.alignment"

    /**
     * Key of [EmbeddingBounds.width].
     *
     * Type: [Bundle] with [putDimension]
     *
     * Properties:
     *
     * | Key                                    | Type           | Property            |
     * |----------------------------------------|----------------|---------------------|
     * | [KEY_EMBEDDING_BOUNDS_DIMENSION_TYPE]  | [String]       | The dimension type  |
     * | [KEY_EMBEDDING_BOUNDS_DIMENSION_VALUE] | [Int], [Float] | The dimension value |
     */
    private const val KEY_EMBEDDING_BOUNDS_WIDTH = "androidx.window.embedding.EmbeddingBounds.width"

    /**
     * Key of [EmbeddingBounds.width].
     *
     * Type: [Bundle] with [putDimension]
     *
     * Properties:
     *
     * | Key                                    | Type           | Property            |
     * |----------------------------------------|----------------|---------------------|
     * | [KEY_EMBEDDING_BOUNDS_DIMENSION_TYPE]  | [String]       | The dimension type  |
     * | [KEY_EMBEDDING_BOUNDS_DIMENSION_VALUE] | [Int], [Float] | The dimension value |
     */
    private const val KEY_EMBEDDING_BOUNDS_HEIGHT =
        "androidx.window.embedding.EmbeddingBounds.height"

    /**
     * A [Dimension] type passed with [KEY_EMBEDDING_BOUNDS_DIMENSION_TYPE], which indicates the
     * [Dimension] is [DIMENSION_EXPANDED].
     */
    private const val DIMENSION_TYPE_EXPANDED = "expanded"

    /**
     * A [Dimension] type passed with [KEY_EMBEDDING_BOUNDS_DIMENSION_TYPE], which indicates the
     * [Dimension] is [DIMENSION_HINGE].
     */
    private const val DIMENSION_TYPE_HINGE = "hinge"

    /**
     * A [Dimension] type passed with [KEY_EMBEDDING_BOUNDS_DIMENSION_TYPE], which indicates the
     * [Dimension] is from [Dimension.ratio]. If this type is used,
     * [KEY_EMBEDDING_BOUNDS_DIMENSION_VALUE] should also be specified with a [Float] value.
     */
    private const val DIMENSION_TYPE_RATIO = "ratio"

    /**
     * A [Dimension] type passed with [KEY_EMBEDDING_BOUNDS_DIMENSION_TYPE], which indicates the
     * [Dimension] is from [Dimension.pixel]. If this type is used,
     * [KEY_EMBEDDING_BOUNDS_DIMENSION_VALUE] should also be specified with a [Int] value.
     */
    private const val DIMENSION_TYPE_PIXEL = "pixel"

    /**
     * Key of [EmbeddingBounds.Dimension] type.
     *
     * Type: [String]
     *
     * Valid values are:
     * - [DIMENSION_TYPE_EXPANDED]: [DIMENSION_EXPANDED]
     * - [DIMENSION_TYPE_HINGE]: [DIMENSION_HINGE]
     * - [DIMENSION_TYPE_RATIO]: [Dimension.ratio]
     * - [DIMENSION_TYPE_PIXEL]: [Dimension.pixel]
     */
    private const val KEY_EMBEDDING_BOUNDS_DIMENSION_TYPE =
        "androidx.window.embedding.EmbeddingBounds.dimension_type"

    /**
     * Key of [EmbeddingBounds.Dimension] value.
     *
     * Type: [Float] or [Int]
     *
     * The value passed in [Dimension.pixel] or [Dimension.ratio]:
     * - Accept [Float] if [KEY_EMBEDDING_BOUNDS_DIMENSION_TYPE] is [DIMENSION_TYPE_RATIO]
     * - Accept [Int] if [KEY_EMBEDDING_BOUNDS_DIMENSION_TYPE] is [DIMENSION_TYPE_PIXEL]
     */
    private const val KEY_EMBEDDING_BOUNDS_DIMENSION_VALUE =
        "androidx.window.embedding.EmbeddingBounds.dimension_value"

    /**
     * Key of [ActivityStack] alignment relative to the parent container.
     *
     * Type: [Int]
     *
     * Valid values are:
     * - 0: [EmbeddingBounds.Alignment.ALIGN_LEFT]
     * - 1: [EmbeddingBounds.Alignment.ALIGN_TOP]
     * - 2: [EmbeddingBounds.Alignment.ALIGN_RIGHT]
     * - 3: [EmbeddingBounds.Alignment.ALIGN_BOTTOM]
     */
    private const val KEY_ACTIVITY_STACK_ALIGNMENT =
        "androidx.window.embedding.ActivityStackAlignment"

    /**
     * Puts [OverlayCreateParams] information to [android.app.ActivityOptions] bundle to launch the
     * overlay container
     *
     * @param overlayCreateParams The [OverlayCreateParams] to launch the overlay container
     */
    @RequiresWindowSdkExtension(OVERLAY_FEATURE_VERSION)
    internal fun setOverlayCreateParams(
        options: Bundle,
        overlayCreateParams: OverlayCreateParams,
    ) {
        WindowSdkExtensions.getInstance().requireExtensionVersion(OVERLAY_FEATURE_VERSION)

        options.putString(KEY_OVERLAY_TAG, overlayCreateParams.tag)
        options.putEmbeddingBounds(overlayCreateParams.overlayAttributes.bounds)
    }

    /** Puts [EmbeddingBounds] information into a bundle for tracking. */
    private fun Bundle.putEmbeddingBounds(embeddingBounds: EmbeddingBounds) {
        putBundle(
            KEY_EMBEDDING_BOUNDS,
            Bundle().apply {
                putInt(KEY_EMBEDDING_BOUNDS_ALIGNMENT, embeddingBounds.alignment.value)
                putDimension(KEY_EMBEDDING_BOUNDS_WIDTH, embeddingBounds.width)
                putDimension(KEY_EMBEDDING_BOUNDS_HEIGHT, embeddingBounds.height)
            }
        )
    }

    /**
     * Puts the alignment of the overlay [ActivityStack] relative to its parent container.
     *
     * It could be used as a hint of the open/close animation direction.
     */
    internal fun Bundle.putActivityStackAlignment(embeddingBounds: EmbeddingBounds) {
        putInt(KEY_ACTIVITY_STACK_ALIGNMENT, embeddingBounds.alignment.value)
    }

    internal fun Bundle.getOverlayAttributes(): OverlayAttributes? {
        val embeddingBounds = getEmbeddingBounds() ?: return null
        return OverlayAttributes(embeddingBounds)
    }

    private fun Bundle.getEmbeddingBounds(): EmbeddingBounds? {
        val embeddingBoundsBundle = getBundle(KEY_EMBEDDING_BOUNDS) ?: return null
        return EmbeddingBounds(
            EmbeddingBounds.Alignment(embeddingBoundsBundle.getInt(KEY_EMBEDDING_BOUNDS_ALIGNMENT)),
            embeddingBoundsBundle.getDimension(KEY_EMBEDDING_BOUNDS_WIDTH),
            embeddingBoundsBundle.getDimension(KEY_EMBEDDING_BOUNDS_HEIGHT)
        )
    }

    /**
     * Retrieves [EmbeddingBounds.Dimension] value from bundle with given [key].
     *
     * See [putDimension] for the data structure of [EmbeddingBounds.Dimension] as bundle
     */
    private fun Bundle.getDimension(key: String): Dimension {
        val dimensionBundle = getBundle(key)!!
        return when (val type = dimensionBundle.getString(KEY_EMBEDDING_BOUNDS_DIMENSION_TYPE)) {
            DIMENSION_TYPE_EXPANDED -> DIMENSION_EXPANDED
            DIMENSION_TYPE_HINGE -> DIMENSION_HINGE
            DIMENSION_TYPE_RATIO ->
                Dimension.ratio(dimensionBundle.getFloat(KEY_EMBEDDING_BOUNDS_DIMENSION_VALUE))
            DIMENSION_TYPE_PIXEL ->
                Dimension.pixel(dimensionBundle.getInt(KEY_EMBEDDING_BOUNDS_DIMENSION_VALUE))
            else -> throw IllegalArgumentException("Illegal type $type")
        }
    }

    /**
     * Puts [EmbeddingBounds.Dimension] information into bundle with a given [key].
     *
     * [EmbeddingBounds.Dimension] is encoded as a [Bundle] with following data structure:
     * - [KEY_EMBEDDING_BOUNDS_DIMENSION_TYPE]: A `string` type. Must be one of:
     *     - [DIMENSION_TYPE_EXPANDED]
     *     - [DIMENSION_TYPE_HINGE]
     *     - [DIMENSION_TYPE_RATIO]
     *     - [DIMENSION_TYPE_PIXEL]
     * - [KEY_EMBEDDING_BOUNDS_DIMENSION_VALUE]: Only specified for [DIMENSION_TYPE_RATIO] and
     *   [DIMENSION_TYPE_PIXEL]. [DIMENSION_TYPE_RATIO] requires a [Float], while
     *   [DIMENSION_TYPE_PIXEL] requires a [Int].
     */
    private fun Bundle.putDimension(key: String, dimension: Dimension) {
        putBundle(
            key,
            Bundle().apply {
                when (dimension) {
                    DIMENSION_EXPANDED -> {
                        putString(KEY_EMBEDDING_BOUNDS_DIMENSION_TYPE, DIMENSION_TYPE_EXPANDED)
                    }
                    DIMENSION_HINGE -> {
                        putString(KEY_EMBEDDING_BOUNDS_DIMENSION_TYPE, DIMENSION_TYPE_HINGE)
                    }
                    is Dimension.Ratio -> {
                        putString(KEY_EMBEDDING_BOUNDS_DIMENSION_TYPE, DIMENSION_TYPE_RATIO)
                        putFloat(KEY_EMBEDDING_BOUNDS_DIMENSION_VALUE, dimension.value)
                    }
                    is Dimension.Pixel -> {
                        putString(KEY_EMBEDDING_BOUNDS_DIMENSION_TYPE, DIMENSION_TYPE_PIXEL)
                        putInt(KEY_EMBEDDING_BOUNDS_DIMENSION_VALUE, dimension.value)
                    }
                }
            }
        )
    }

    @RequiresWindowSdkExtension(5)
    internal fun setActivityStackToken(options: Bundle, activityStackToken: Token) {
        options.putBundle(KEY_ACTIVITY_STACK_TOKEN, activityStackToken.toBundle())
    }
}
