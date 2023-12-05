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

package androidx.compose.material3.adaptive.benchmark

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.PaneScaffoldDirective
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
val singlePaneDirective = PaneScaffoldDirective(
    contentPadding = PaddingValues(16.dp),
    maxHorizontalPartitions = 1,
    horizontalPartitionSpacerSize = 0.dp,
    maxVerticalPartitions = 1,
    verticalPartitionSpacerSize = 0.dp,
    excludedBounds = emptyList()
)

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
val dualPaneDirective = PaneScaffoldDirective(
    contentPadding = PaddingValues(24.dp),
    maxHorizontalPartitions = 2,
    horizontalPartitionSpacerSize = 24.dp,
    maxVerticalPartitions = 1,
    verticalPartitionSpacerSize = 0.dp,
    excludedBounds = emptyList()
)
