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

package androidx.compose.material3.adaptive.layout

import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable

/**
 * Provides the information about how the associated pane should be adapted if it cannot be
 * displayed in its [PaneAdaptedValue.Expanded] state.
 */
@ExperimentalMaterial3AdaptiveApi
@Stable
sealed interface AdaptStrategy {
    /** Override this function to provide the resulted adapted state. */
    fun adapt(): PaneAdaptedValue

    @Immutable
    private class SimpleAdaptStrategy(
        private val description: String,
        private val adaptedValue: PaneAdaptedValue
    ) : AdaptStrategy {
        override fun adapt() = adaptedValue

        override fun toString() = "AdaptStrategy[$description]"

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is SimpleAdaptStrategy) return false
            if (description != other.description) return false
            if (adaptedValue != other.adaptedValue) return false
            return true
        }

        override fun hashCode(): Int {
            var result = description.hashCode()
            result = 31 * result + adaptedValue.hashCode()
            return result
        }
    }

    companion object {
        /**
         * The default [AdaptStrategy] that suggests the layout to hide the associated pane when it
         * has to be adapted, i.e., cannot be displayed in its [PaneAdaptedValue.Expanded] state.
         */
        val Hide: AdaptStrategy = SimpleAdaptStrategy("Hide", PaneAdaptedValue.Hidden)
    }
}
