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

import androidx.annotation.IntRange
import androidx.window.RequiresWindowSdkExtension

/**
 * Configurations of Activity Embedding environment that defines how the embedded Activities behave.
 *
 * @constructor The [EmbeddingConfiguration] constructor. The properties are undefined if not
 *   specified.
 * @property dimAreaBehavior The requested dim area behavior.
 * @see ActivityEmbeddingController.setEmbeddingConfiguration
 */
class EmbeddingConfiguration
@JvmOverloads
constructor(
    @RequiresWindowSdkExtension(5) val dimAreaBehavior: DimAreaBehavior = DimAreaBehavior.UNDEFINED
) {
    /**
     * The area of dimming to apply.
     *
     * @see [android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND]
     */
    class DimAreaBehavior private constructor(@IntRange(from = 0, to = 2) internal val value: Int) {
        companion object {
            /**
             * The dim area is not defined.
             *
             * This is the default value while building a [EmbeddingConfiguration]. This would also
             * keep the existing dim area configuration of the current Activity Embedding
             * environment unchanged when [ActivityEmbeddingController.setEmbeddingConfiguration] is
             * called.
             *
             * @see ActivityEmbeddingController.setEmbeddingConfiguration
             */
            @JvmField val UNDEFINED = DimAreaBehavior(0)

            /**
             * The dim effect is applying on the [ActivityStack] of the Activity window when needed.
             * If the [ActivityStack] is split and displayed side-by-side with another
             * [ActivityStack], the dim effect is applying only on the [ActivityStack] of the
             * requested Activity.
             */
            @JvmField val ON_ACTIVITY_STACK = DimAreaBehavior(1)

            /**
             * The dimming effect is applying on the area of the whole Task when needed. If the
             * embedded transparent activity is split and displayed side-by-side with another
             * activity, the dim effect is applying on the Task, which across over the two
             * [ActivityStack]s.
             *
             * This is the default dim area configuration of the Activity Embedding environment,
             * before the [DimAreaBehavior] is explicitly set by
             * [ActivityEmbeddingController.setEmbeddingConfiguration].
             */
            @JvmField val ON_TASK = DimAreaBehavior(2)
        }

        override fun toString(): String {
            return "DimAreaBehavior=" +
                when (value) {
                    0 -> "UNDEFINED"
                    1 -> "ON_ACTIVITY_STACK"
                    2 -> "ON_TASK"
                    else -> "UNKNOWN"
                }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is EmbeddingConfiguration) return false

        if (dimAreaBehavior != other.dimAreaBehavior) return false
        return true
    }

    override fun hashCode(): Int {
        return dimAreaBehavior.hashCode()
    }

    override fun toString(): String = "EmbeddingConfiguration{$dimAreaBehavior}"

    /** Builder for creating an instance of [EmbeddingConfiguration]. */
    class Builder {
        private var mDimAreaBehavior = DimAreaBehavior.UNDEFINED

        /**
         * Sets the dim area behavior. By default, the [DimAreaBehavior.UNDEFINED] is used if not
         * set.
         *
         * @param area The dim area.
         * @return This [Builder]
         */
        @SuppressWarnings("MissingGetterMatchingBuilder")
        fun setDimAreaBehavior(area: DimAreaBehavior): Builder = apply { mDimAreaBehavior = area }

        /**
         * Builds a[EmbeddingConfiguration] instance.
         *
         * @return The new [EmbeddingConfiguration] instance.
         */
        fun build(): EmbeddingConfiguration = EmbeddingConfiguration(mDimAreaBehavior)
    }
}
