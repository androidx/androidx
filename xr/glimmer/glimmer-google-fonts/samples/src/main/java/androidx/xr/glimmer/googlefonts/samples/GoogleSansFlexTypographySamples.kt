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

package androidx.xr.glimmer.googlefonts.samples

import androidx.annotation.Sampled
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.xr.glimmer.GlimmerTheme
import androidx.xr.glimmer.Text
import androidx.xr.glimmer.googlefonts.createGoogleSansFlexTypography

@Sampled
@Composable
fun GoogleSansFlexTypographySample() {
    val typography = createGoogleSansFlexTypography()
    GlimmerTheme(typography = typography) {
        Column(modifier = Modifier.background(GlimmerTheme.colors.background)) {
            Text("titleLarge", style = GlimmerTheme.typography.titleLarge)
            Text("titleMedium", style = GlimmerTheme.typography.titleMedium)
            Text("titleSmall", style = GlimmerTheme.typography.titleSmall)
            Text("bodyLarge", style = GlimmerTheme.typography.bodyLarge)
            Text("bodyMedium", style = GlimmerTheme.typography.bodyMedium)
            Text("bodySmall", style = GlimmerTheme.typography.bodySmall)
            Text("caption", style = GlimmerTheme.typography.caption)
        }
    }
}

@Preview
@Composable
private fun GoogleSansFlexTypographyPreview() {
    GoogleSansFlexTypographySample()
}
