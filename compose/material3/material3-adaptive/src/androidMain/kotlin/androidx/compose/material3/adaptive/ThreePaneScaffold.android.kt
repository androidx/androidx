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

import androidx.compose.runtime.Stable

/**
 * The state of [ThreePaneScaffold]. It provides the layout directive and value state that will
 * be updated directly. It also provides functions to perform navigation.
 *
 * @property scaffoldDirective the current layout directives that the associated
 *           [ThreePaneScaffold] needs to follow. It's supposed to be automatically updated
 *           when the window configuration changes.
 * @property scaffoldValue the current layout value of the associated [ThreePaneScaffold],
 *           which represents unique layout states of the scaffold.
 */
@ExperimentalMaterial3AdaptiveApi
@Stable
interface ThreePaneScaffoldState {
    val scaffoldDirective: PaneScaffoldDirective
    val scaffoldValue: ThreePaneScaffoldValue
}

@ExperimentalMaterial3AdaptiveApi
internal class ThreePaneScaffoldStateImpl(
    override val scaffoldDirective: PaneScaffoldDirective,
    override val scaffoldValue: ThreePaneScaffoldValue
) : ThreePaneScaffoldState
