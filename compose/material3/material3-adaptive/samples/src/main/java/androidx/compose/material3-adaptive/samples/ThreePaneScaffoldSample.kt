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

package androidx.compose.material3.adaptive.samples

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.ListDetailPaneScaffold
import androidx.compose.material3.adaptive.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.ThreePaneScaffold
import androidx.compose.material3.adaptive.ThreePaneScaffoldDefaults
import androidx.compose.material3.adaptive.calculateStandardPaneScaffoldDirective
import androidx.compose.material3.adaptive.calculateThreePaneScaffoldValue
import androidx.compose.material3.adaptive.calculateWindowAdaptiveInfo
import androidx.compose.material3.adaptive.rememberListDetailPaneScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Preview
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
internal fun ListDetailPaneScaffoldSample() {
    val layoutState = rememberListDetailPaneScaffoldState(
        initialFocusHistory = listOf(ListDetailPaneScaffoldRole.List)
    )
    ListDetailPaneScaffold(
        layoutState = layoutState,
        listPane = {
            Surface(
                modifier = Modifier.preferredWidth(200.dp),
                color = MaterialTheme.colorScheme.secondary,
                onClick = {
                    layoutState.navigateTo(ListDetailPaneScaffoldRole.Detail)
                }
            ) {
                Text("List")
            }
        },
    ) {
        Surface(
            color = MaterialTheme.colorScheme.primary,
            onClick = {
                layoutState.navigateBack()
            }
        ) {
            Text("Details")
        }
    }
}

@Preview
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
internal fun ListDetailExtraPaneScaffoldSample() {
    val layoutState = rememberListDetailPaneScaffoldState(
        initialFocusHistory = listOf(ListDetailPaneScaffoldRole.List)
    )

    ListDetailPaneScaffold(
        layoutState = layoutState,
        listPane = {
            Surface(
                modifier = Modifier.preferredWidth(200.dp),
                color = MaterialTheme.colorScheme.secondary,
                onClick = {
                    layoutState.navigateTo(ListDetailPaneScaffoldRole.Detail)
                }
            ) {
                Text("List")
            }
        },
        extraPane = {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.tertiary,
                onClick = {
                    layoutState.navigateBack()
                }
            ) {
                Text("Extra")
            }
        }
    ) {
        Surface(
            color = MaterialTheme.colorScheme.primary,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Details")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        onClick = {
                            layoutState.navigateBack()
                        },
                        modifier = Modifier
                            .weight(0.5f)
                            .fillMaxHeight(),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Previous")
                        }
                    }
                    VerticalDivider()
                    Surface(
                        onClick = {
                            layoutState.navigateTo(ListDetailPaneScaffoldRole.Extra)
                        },
                        modifier = Modifier
                            .weight(0.5f)
                            .fillMaxHeight(),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Next")
                        }
                    }
                }
            }
        }
    }
}

@Preview
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
internal fun ThreePaneScaffoldSample() {
    val scaffoldDirective = calculateStandardPaneScaffoldDirective(calculateWindowAdaptiveInfo())
    ThreePaneScaffold(
        modifier = Modifier.fillMaxSize(),
        scaffoldDirective = scaffoldDirective,
        scaffoldValue = calculateThreePaneScaffoldValue(scaffoldDirective.maxHorizontalPartitions),
        arrangement = ThreePaneScaffoldDefaults.ListDetailLayoutArrangement,
        secondaryPane = {
            Surface(
                modifier = Modifier.preferredWidth(100.dp),
                color = MaterialTheme.colorScheme.secondary
            ) {
                Text("Secondary")
            }
        },
        tertiaryPane = {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.tertiary
            ) {
                Text("Tertiary")
            }
        }
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.primary
        ) {
            Text("Primary")
        }
    }
}
