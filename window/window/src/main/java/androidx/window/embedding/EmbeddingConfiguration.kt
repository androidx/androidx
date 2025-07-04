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
 * @property dimAreaBehavior The requested dim area behavior.
 * @property isAutoSaveEmbeddingState Is auto-save embedding state enabled.
 * @see Builder
 * @see ActivityEmbeddingController.setEmbeddingConfiguration
 */
class EmbeddingConfiguration
private constructor(
    val dimAreaBehavior: DimAreaBehavior = DimAreaBehavior.UNDEFINED,
    val isAutoSaveEmbeddingState: Boolean = false,
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
        if (isAutoSaveEmbeddingState != other.isAutoSaveEmbeddingState) return false
        return true
    }

    override fun hashCode(): Int {
        var result = dimAreaBehavior.hashCode()
        result = 31 * result + isAutoSaveEmbeddingState.hashCode()
        return result
    }

    override fun toString(): String =
        "EmbeddingConfiguration{dimArea=$dimAreaBehavior, saveState=$isAutoSaveEmbeddingState}"

    /** Builder for creating an instance of [EmbeddingConfiguration]. */
    class Builder {
        private var mDimAreaBehavior = DimAreaBehavior.UNDEFINED
        private var mSaveEmbeddingState: Boolean = false

        /**
         * Sets the dim area behavior. By default, the [DimAreaBehavior.UNDEFINED] is used if not
         * set.
         *
         * This can be supported only if the Window Extensions version of the target device is
         * equals or higher than required API level. Otherwise, it would be no-op on a target device
         * that has lower API level.
         *
         * @param area The dim area.
         * @return This [Builder]
         */
        @RequiresWindowSdkExtension(5)
        fun setDimAreaBehavior(area: DimAreaBehavior): Builder = apply { mDimAreaBehavior = area }

        /**
         * Sets whether to auto save the embedding state to the system, which can be used to restore
         * the app embedding state once the app process is restarted (if applicable).
         *
         * The embedding state is not saved by default, in which case the embedding state and the
         * embedded activities are removed once the app process is killed.
         *
         * **Note** that the applications should set the [EmbeddingRule]s using
         * [RuleController.setRules] when the application is initializing, such as configured in
         * [androidx.startup.Initializer] or in [android.app.Application.onCreate], in order to
         * allow the library to restore the state properly. Otherwise, the state may not be restored
         * and the activities may not be started and layout as expected.
         *
         * This can be supported only if the Window Extensions version of the target device is
         * equals or higher than required API level. Otherwise, it would be no-op on a target device
         * that has lower API level.
         *
         * @param saveState whether to save the embedding state
         */
        @RequiresWindowSdkExtension(8)
        fun setAutoSaveEmbeddingState(saveState: Boolean): Builder = apply {
            mSaveEmbeddingState = saveState
        }

        /**
         * Builds a[EmbeddingConfiguration] instance.
         *
         * @return The new [EmbeddingConfiguration] instance.
         */
        fun build(): EmbeddingConfiguration =
            EmbeddingConfiguration(mDimAreaBehavior, mSaveEmbeddingState)
    }
}
