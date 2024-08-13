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
import kotlin.jvm.JvmInline

/**
 * The adapted state of a pane. It gives clues to pane scaffolds about if a certain pane should be
 * composed and how.
 */
@ExperimentalMaterial3AdaptiveApi
@JvmInline
value class PaneAdaptedValue private constructor(private val description: String) {
    companion object {
        /** Denotes that the associated pane should be displayed in its full width and height. */
        val Expanded = PaneAdaptedValue("Expanded")
        /** Denotes that the associated pane should be hidden. */
        val Hidden = PaneAdaptedValue("Hidden")
    }
}
