/*
 * Copyright 2025 The Android Open Source Project
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

/**
 * The interface that represents roles of panes in pane scaffold implementations.
 *
 * @see ThreePaneScaffoldRole
 */
interface PaneScaffoldRole

/** The set of the available pane roles of [ThreePaneScaffold]. */
enum class ThreePaneScaffoldRole : PaneScaffoldRole {
    /**
     * The primary pane of [ThreePaneScaffold]. It is supposed to have the highest priority during
     * layout adaptation and usually contains the most important content of the screen, like content
     * details in a list-detail settings.
     */
    Primary,

    /**
     * The secondary pane of [ThreePaneScaffold]. It is supposed to have the second highest priority
     * during layout adaptation and usually contains the supplement content of the screen, like
     * content list in a list-detail settings.
     */
    Secondary,

    /**
     * The tertiary pane of [ThreePaneScaffold]. It is supposed to have the lowest priority during
     * layout adaptation and usually contains the additional info which will only be shown under
     * user interaction.
     */
    Tertiary,
}
