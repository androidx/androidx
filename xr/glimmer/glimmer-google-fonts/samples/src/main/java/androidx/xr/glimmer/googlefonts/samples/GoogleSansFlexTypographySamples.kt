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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.xr.glimmer.GlimmerTheme
import androidx.xr.glimmer.Text
import androidx.xr.glimmer.googlefonts.createGoogleSansFlexTypography
import androidx.xr.glimmer.list.GlimmerLazyColumn

@Sampled
@Composable
fun GoogleSansFlexTypographySample() {
    val typography = createGoogleSansFlexTypography()
    GlimmerTheme(typography = typography) {
        Text("Hello World", style = GlimmerTheme.typography.titleLarge)
    }
}

@Composable
fun GoogleSansFlexTypographyUsage() {
    val typography = createGoogleSansFlexTypography()
    GlimmerTheme(typography = typography) {
        GlimmerLazyColumn(modifier = Modifier.background(GlimmerTheme.colors.background)) {
            item { TypeItem("titleLarge", style = typography.titleLarge) }
            item { TypeItem("titleMedium", style = typography.titleMedium) }
            item { TypeItem("titleSmall", style = typography.titleSmall) }
            item { TypeItem("bodyLarge", style = typography.bodyLarge) }
            item { TypeItem("bodyMedium", style = typography.bodyMedium) }
            item { TypeItem("bodySmall", style = typography.bodySmall) }
            item { TypeItem("caption", style = typography.caption) }
        }
    }
}

@Composable
private fun TypeItem(name: String, style: TextStyle, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxWidth().padding(horizontal = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(name, style = style)
        val typeInformation = with(style) { "$fontSize / $lineHeight • $letterSpacing" }
        Text(typeInformation, fontSize = 16.sp)
    }
}

@Preview
@Composable
private fun GoogleSansFlexTypographyPreview() {
    GoogleSansFlexTypographyUsage()
}
