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

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * A Material opinionated implementation of [ThreePaneScaffold] that will display the provided three
 * panes in a canonical list-detail layout.
 */
@ExperimentalMaterial3AdaptiveApi
@Composable
internal fun ListDetailPaneScaffold(
    modifier: Modifier = Modifier,
    listPane: @Composable ThreePaneScaffoldScope.(PaneAdaptedValue) -> Unit,
    extraPane: (@Composable ThreePaneScaffoldScope.(PaneAdaptedValue) -> Unit)? = null,
    detailPane: @Composable ThreePaneScaffoldScope.(PaneAdaptedValue) -> Unit
) {
    // TODO(conradchen): Add support of condition & strategy customization
    val layoutDirective = calculateStandardAdaptiveLayoutDirective(calculateWindowAdaptiveInfo())
    val scaffoldValue = calculateThreePaneScaffoldValue(
        layoutDirective.maxHorizontalPartitions,
        ListDetailPaneScaffoldDefaults.adaptStrategies()
    )
    ThreePaneScaffold(
        modifier = Modifier.fillMaxSize().then(modifier),
        layoutDirective = layoutDirective,
        scaffoldValue = scaffoldValue,
        arrangement = ThreePaneScaffoldDefaults.ListDetailLayoutArrangement,
        secondaryPane = listPane,
        tertiaryPane = extraPane,
        primaryPane = detailPane
    )
}

/**
 * Provides default values of [ListDetailPaneScaffold].
 */
@ExperimentalMaterial3AdaptiveApi
internal object ListDetailPaneScaffoldDefaults {
    /**
     * Creates a default [ThreePaneScaffoldAdaptStrategies] for [ListDetailPaneScaffold].
     *
     * @param detailPaneAdaptStrategy the adapt strategy of the primary pane
     * @param listPaneAdaptStrategy the adapt strategy of the secondary pane
     * @param extraPaneAdaptStrategy the adapt strategy of the tertiary pane
     */
    fun adaptStrategies(
        detailPaneAdaptStrategy: AdaptStrategy = AdaptStrategy.Hide,
        listPaneAdaptStrategy: AdaptStrategy = AdaptStrategy.Hide,
        extraPaneAdaptStrategy: AdaptStrategy = AdaptStrategy.Hide,
    ): ThreePaneScaffoldAdaptStrategies =
        ThreePaneScaffoldAdaptStrategies(
            detailPaneAdaptStrategy,
            listPaneAdaptStrategy,
            extraPaneAdaptStrategy
        )
}
