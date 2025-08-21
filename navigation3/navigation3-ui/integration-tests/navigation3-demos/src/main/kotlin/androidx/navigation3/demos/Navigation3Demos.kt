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

package androidx.navigation3.demos

import androidx.compose.integration.demos.common.ComposableDemo
import androidx.compose.integration.demos.common.DemoCategory
import androidx.navigation3.ui.samples.SceneNav
import androidx.navigation3.ui.samples.SceneNavSharedElementSample

val Navigation3Demos =
    DemoCategory(
        "Navigation3",
        listOf(
            ComposableDemo("Basic Nav3") { SceneNav() },
            ComposableDemo("Nav3 Shared Element Demo") { SceneNavSharedElementSample() },
            ComposableDemo("Hierarchical Scene Strategy Demo") { HierarchicalSceneSample() },
        ),
    )
