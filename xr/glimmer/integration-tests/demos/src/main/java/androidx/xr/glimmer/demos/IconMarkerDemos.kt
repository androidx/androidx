/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.xr.glimmer.demos

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.xr.glimmer.Icon
import androidx.xr.glimmer.IconMarker
import androidx.xr.glimmer.IconMarkerSize
import androidx.xr.glimmer.list.GlimmerLazyColumn

internal val IconMarkerDemos =
    listOf(
        ComposableDemo("Clickable Icon Markers") { ClickableIconMarkers() },
        ComposableDemo("Non-interactive Icon Markers") { NonInteractiveIconMarkers() },
    )

@Composable
private fun ClickableIconMarkers() {
    GlimmerLazyColumn(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize().wrapContentSize(Alignment.Center),
    ) {
        item {
            IconMarker(
                onClick = {},
                contentDescription = "Dot marker medium",
                size = IconMarkerSize.Medium,
            )
        }
        item {
            IconMarker(
                onClick = {},
                contentDescription = "Favorite icon medium",
                size = IconMarkerSize.Medium,
            ) {
                Icon(Icons.FavoriteIcon, contentDescription = null)
            }
        }
        item {
            IconMarker(
                onClick = {},
                contentDescription = "Dot marker small",
                size = IconMarkerSize.Small,
            )
        }
        item {
            IconMarker(
                onClick = {},
                contentDescription = "Favorite icon small",
                size = IconMarkerSize.Small,
            ) {
                Icon(Icons.FavoriteIcon, contentDescription = null)
            }
        }
    }
}

@Composable
private fun NonInteractiveIconMarkers() {
    GlimmerLazyColumn(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize().wrapContentSize(Alignment.Center),
    ) {
        item {
            IconMarker(
                contentDescription = "Dot marker medium",
                size = IconMarkerSize.Medium,
            )
        }
        item {
            IconMarker(
                contentDescription = "Favorite icon medium",
                size = IconMarkerSize.Medium,
            ) {
                Icon(Icons.FavoriteIcon, contentDescription = null)
            }
        }
        item {
            IconMarker(
                contentDescription = "Dot marker small",
                size = IconMarkerSize.Small,
            )
        }
        item {
            IconMarker(
                contentDescription = "Favorite icon small",
                size = IconMarkerSize.Small,
            ) {
                Icon(Icons.FavoriteIcon, contentDescription = null)
            }
        }
    }
}
