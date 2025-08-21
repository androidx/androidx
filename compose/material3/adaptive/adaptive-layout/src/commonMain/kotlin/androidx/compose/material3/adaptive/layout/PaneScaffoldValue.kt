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

package androidx.compose.material3.adaptive.layout

import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi

/**
 * Interface to provide adapted value of panes.
 *
 * @param Role The type of the [PaneScaffoldRole] that will be used by the pane scaffold
 *   implementation to index the pane and its associated [PaneAdaptedValue].
 * @see ThreePaneScaffoldValue
 */
@ExperimentalMaterial3AdaptiveApi
sealed interface PaneScaffoldValue<Role : PaneScaffoldRole> {
    /** Returns the [PaneAdaptedValue] of the given [role] of a pane. */
    operator fun get(role: Role): PaneAdaptedValue
}
