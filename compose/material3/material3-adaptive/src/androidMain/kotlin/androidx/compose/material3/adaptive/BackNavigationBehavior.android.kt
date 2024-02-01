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

package androidx.compose.material3.adaptive

/**
 * A class to control how back navigation should behave in a [ThreePaneScaffoldNavigator].
 */
@ExperimentalMaterial3AdaptiveApi
enum class BackNavigationBehavior {
    /** Pop the latest destination from the backstack. */
    PopLatest,

    /**
     * Pop destinations from the backstack until there is a change in the scaffold value.
     *
     * For example, in a single-pane layout, this will skip entries until the current destination
     * is a different [ThreePaneScaffoldRole]. In a multi-pane layout, this will skip entries until
     * the [PaneAdaptedValue] of any pane changes.
     */
    PopUntilScaffoldValueChange,

    /**
     * Pop destinations from the backstack until there is a change in the current destination pane.
     *
     * In a single-pane layout, this should behave similarly to [PopUntilScaffoldValueChange]. In a
     * multi-pane layout, it is possible for both the current destination and previous destination
     * to be showing at the same time, so this may not result in a visual change in the scaffold.
     */
    PopUntilCurrentDestinationChange,

    /**
     * Pop destinations from the backstack until there is a content change.
     *
     * A "content change" is defined as either a change in the content of the current
     * [ThreePaneScaffoldDestinationItem], or a change in the scaffold value (similar to
     * [PopUntilScaffoldValueChange]).
     */
    PopUntilContentChange,
}
