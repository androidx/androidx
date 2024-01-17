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

package androidx.compose.material3.adaptive

/**
 * Provides the information about how the associated pane should be adapted if it cannot be
 * displayed in its [PaneAdaptedValue.Expanded] state.
 */
@ExperimentalMaterial3AdaptiveApi
interface AdaptStrategy {
    /**
     * Override this function to provide the resulted adapted state.
     */
    fun adapt(): PaneAdaptedValue

    private class BaseAdaptStrategy(
        private val description: String,
        private val adaptedState: PaneAdaptedValue
    ) : AdaptStrategy {
        override fun adapt() = adaptedState

        override fun toString() = "AdaptStrategy[$description]"
    }

    companion object {
        /**
         * The default [AdaptStrategy] that suggests the layout to hide the associated pane when
         * it has to be adapted, i.e., cannot be displayed in its [PaneAdaptedValue.Expanded] state.
         */
        val Hide: AdaptStrategy = BaseAdaptStrategy("Hide", PaneAdaptedValue.Hidden)
    }
}
