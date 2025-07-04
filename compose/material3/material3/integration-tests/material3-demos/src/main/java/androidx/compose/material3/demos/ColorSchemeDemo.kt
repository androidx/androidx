/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.compose.material3.demos

import android.annotation.SuppressLint
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.carousel.HorizontalMultiBrowseCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColorSchemeDemo() {
    val colorScheme = MaterialTheme.colorScheme
    data class CarouselItem(val colorName: String, val color: Color)

    val surfaceColors =
        listOf(
            CarouselItem("Surface", colorScheme.surface),
            CarouselItem("Surface Dim", colorScheme.surfaceDim),
            CarouselItem("Surface Bright", colorScheme.surfaceBright),
            CarouselItem("Surface Container Lowest", colorScheme.surfaceContainerLowest),
            CarouselItem("Surface Container Low", colorScheme.surfaceContainerLow),
            CarouselItem("Surface Container", colorScheme.surfaceContainer),
            CarouselItem("Surface Container High", colorScheme.surfaceContainerHigh),
            CarouselItem("Surface Container Highest", colorScheme.surfaceContainerHighest),
            CarouselItem("Surface Variant", colorScheme.surfaceVariant),
            CarouselItem("On Surface", colorScheme.onSurface),
            CarouselItem("On Surface Variant", colorScheme.onSurfaceVariant),
            CarouselItem("Inverse Surface", colorScheme.inverseSurface),
            CarouselItem("Inverse On Surface", colorScheme.inverseOnSurface),
        )

    val primaryColors =
        listOf(
            CarouselItem("Primary", colorScheme.primary),
            CarouselItem("On Primary", colorScheme.onPrimary),
            CarouselItem("Primary Container", colorScheme.primaryContainer),
            CarouselItem("On Primary Container", colorScheme.onPrimaryContainer),
            CarouselItem("Primary Fixed", colorScheme.primaryFixed),
            CarouselItem("Primary Fixed Dim", colorScheme.primaryFixedDim),
            CarouselItem("On Primary Fixed", colorScheme.onPrimaryFixed),
            CarouselItem("On Primary Fixed Variant", colorScheme.onPrimaryFixedVariant),
            CarouselItem("Inverse Primary", colorScheme.inversePrimary),
        )

    val secondaryColors =
        listOf(
            CarouselItem("Secondary", colorScheme.secondary),
            CarouselItem("On Secondary", colorScheme.onSecondary),
            CarouselItem("Secondary Container", colorScheme.secondaryContainer),
            CarouselItem("On Secondary Container", colorScheme.onSecondaryContainer),
            CarouselItem("Secondary Fixed", colorScheme.secondaryFixed),
            CarouselItem("Secondary Fixed Dim", colorScheme.secondaryFixedDim),
            CarouselItem("On Secondary Fixed", colorScheme.onSecondaryFixed),
            CarouselItem("On Secondary Fixed Variant", colorScheme.onSecondaryFixedVariant),
        )

    val tertiaryColors =
        listOf(
            CarouselItem("Tertiary", colorScheme.tertiary),
            CarouselItem("On Tertiary", colorScheme.onTertiary),
            CarouselItem("Tertiary Container", colorScheme.tertiaryContainer),
            CarouselItem("On Tertiary Container", colorScheme.onTertiaryContainer),
            CarouselItem("Tertiary Fixed", colorScheme.tertiaryFixed),
            CarouselItem("Tertiary Fixed Dim", colorScheme.tertiaryFixedDim),
            CarouselItem("On Tertiary Fixed", colorScheme.onTertiaryFixed),
            CarouselItem("On Tertiary Fixed Variant", colorScheme.onTertiaryFixedVariant),
        )

    val miscColors =
        listOf(
            CarouselItem("Error", colorScheme.error),
            CarouselItem("On Error", colorScheme.onError),
            CarouselItem("Error Container", colorScheme.errorContainer),
            CarouselItem("On Error Container", colorScheme.onErrorContainer),
            CarouselItem("Outline", colorScheme.outline),
            CarouselItem("Outline Variant", colorScheme.outlineVariant),
            CarouselItem("Scrim", colorScheme.scrim),
            CarouselItem("Surface Tint", colorScheme.surfaceTint),
        )

    Column(Modifier.padding(8.dp)) {
        Text("Surface Colors", style = MaterialTheme.typography.bodyLarge)
        HorizontalMultiBrowseCarousel(
            state = rememberCarouselState { surfaceColors.count() },
            modifier = Modifier.width(400.dp).height(100.dp),
            itemSpacing = 8.dp,
            preferredItemWidth = 100.dp,
        ) { i ->
            val item = surfaceColors[i]
            ColorTile(item.colorName, item.color)
        }
        Text("Primary Colors", style = MaterialTheme.typography.bodyLarge)
        HorizontalMultiBrowseCarousel(
            state = rememberCarouselState { primaryColors.count() },
            modifier = Modifier.width(400.dp).height(100.dp),
            itemSpacing = 8.dp,
            preferredItemWidth = 100.dp,
        ) { i ->
            val item = primaryColors[i]
            ColorTile(item.colorName, item.color)
        }
        Text("Secondary Colors", style = MaterialTheme.typography.bodyLarge)
        HorizontalMultiBrowseCarousel(
            state = rememberCarouselState { secondaryColors.count() },
            modifier = Modifier.width(400.dp).height(100.dp),
            itemSpacing = 8.dp,
            preferredItemWidth = 100.dp,
        ) { i ->
            val item = secondaryColors[i]
            ColorTile(item.colorName, item.color)
        }
        Text("Tertiary Colors", style = MaterialTheme.typography.bodyLarge)
        HorizontalMultiBrowseCarousel(
            state = rememberCarouselState { tertiaryColors.count() },
            modifier = Modifier.width(400.dp).height(100.dp),
            itemSpacing = 8.dp,
            preferredItemWidth = 100.dp,
        ) { i ->
            val item = tertiaryColors[i]
            ColorTile(item.colorName, item.color)
        }
        Text("Misc Colors", style = MaterialTheme.typography.bodyLarge)
        HorizontalMultiBrowseCarousel(
            state = rememberCarouselState { miscColors.count() },
            modifier = Modifier.width(400.dp).height(100.dp),
            itemSpacing = 8.dp,
            preferredItemWidth = 100.dp,
        ) { i ->
            val item = miscColors[i]
            ColorTile(item.colorName, item.color)
        }
    }
}

@SuppressLint("NullAnnotationGroup")
@OptIn(ExperimentalTextApi::class)
@Composable
private fun ColorTile(text: String, color: Color) {
    var borderColor: Color
    if (color.luminance() < 0.5) {
        borderColor = Color.White
    } else {
        borderColor = Color.Black
    }

    OutlinedCard(border = BorderStroke(1.dp, borderColor)) {
        Surface(modifier = Modifier.height(221.dp).fillMaxWidth(), color = color) {
            Text(
                text,
                Modifier.padding(4.dp),
                style =
                    MaterialTheme.typography.bodyMedium.copy(
                        if (color.luminance() < .25) Color.White else Color.Black
                    ),
            )
        }
    }
}
