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

package androidx.xr.glimmer.samples

import androidx.annotation.Sampled
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.xr.glimmer.GlimmerTheme
import androidx.xr.glimmer.Icon
import androidx.xr.glimmer.IconMarker

@Sampled
@Composable
fun IconMarkerSample() {
    IconMarker(
        onClick = {},
        // Provide a description of the real-world object that the marker tracks.
        contentDescription = "Northern Cardinal",
    )
}

@Sampled
@Composable
fun IconMarkerWithCustomIconSample() {
    IconMarker(
        onClick = {},
        contentDescription = "Favourite meal",
        // The content description for the icon is redundant, as the marker merges the semantics.
        content = { Icon(FavoriteIcon, contentDescription = null) },
    )
}

@Preview
@Composable
private fun IconMarkerPreview() {
    GlimmerTheme { IconMarkerSample() }
}

@Preview
@Composable
private fun IconMarkerWithCustomIconSamplePreview() {
    GlimmerTheme { IconMarkerWithCustomIconSample() }
}
