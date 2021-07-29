/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.window.layout

import android.graphics.Rect
import androidx.window.core.Bounds

/**
 * @param [type] that is either [HardwareFoldingFeature.Type.FOLD] or
 * [HardwareFoldingFeature.Type.HINGE] @param [state] the physical state of the hinge that is
 * either [FoldingFeature.State.FLAT] or [FoldingFeature.State.HALF_OPENED]
 */
internal class HardwareFoldingFeature(
    /**
     * The bounding rectangle of the feature within the application window in the window
     * coordinate space.
     */
    private val featureBounds: Bounds,
    internal val type: Type,
    override val state: FoldingFeature.State
) : FoldingFeature {

    init {
        validateFeatureBounds(featureBounds)
    }

    override val bounds: Rect
        get() = featureBounds.toRect()

    override val isSeparating: Boolean
        get() = when {
            type == Type.HINGE -> true
            type == Type.FOLD && state == FoldingFeature.State.HALF_OPENED -> true
            else -> false
        }

    override val occlusionType: FoldingFeature.OcclusionType
        get() = if (featureBounds.width == 0 || featureBounds.height == 0) {
            FoldingFeature.OcclusionType.NONE
        } else {
            FoldingFeature.OcclusionType.FULL
        }

    override val orientation: FoldingFeature.Orientation
        get() {
            return if (featureBounds.width > featureBounds.height) {
                FoldingFeature.Orientation.HORIZONTAL
            } else {
                FoldingFeature.Orientation.VERTICAL
            }
        }

    override fun toString(): String {
        return (
            "${FoldingFeature::class.java.simpleName} { $featureBounds, " +
                "type=$type, state=$state }"
            )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as HardwareFoldingFeature

        if (featureBounds != other.featureBounds) return false
        if (type != other.type) return false
        if (state != other.state) return false

        return true
    }

    override fun hashCode(): Int {
        var result = featureBounds.hashCode()
        result = 31 * result + type.hashCode()
        result = 31 * result + state.hashCode()
        return result
    }

    internal companion object {
        /**
         * Verifies the bounds of the folding feature.
         */
        internal fun validateFeatureBounds(bounds: Bounds) {
            require(!(bounds.width == 0 && bounds.height == 0)) { "Bounds must be non zero" }
            require(!(bounds.left != 0 && bounds.top != 0)) {
                "Bounding rectangle must start at the top or left window edge for folding features"
            }
        }
    }

    /**
     * Represents the type of hinge.
     */
    internal class Type private constructor(private val description: String) {

        override fun toString(): String {
            return description
        }

        internal companion object {
            /**
             * Represent a continuous screen that folds.
             */
            val FOLD: Type = Type("FOLD")

            /**
             * Represents a hinge connecting two separate display panels.
             */
            val HINGE: Type = Type("HINGE")
        }
    }
}