/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.compose.material3.samples

import android.os.Build
import androidx.annotation.Sampled
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Button
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Preview
@Sampled
@Composable
fun MaterialThemeSample() {

    val isDarkTheme = isSystemInDarkTheme()
    val supportsDynamicColor = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    val lightColorScheme = lightColorScheme(primary = Color(0xFF1EB980))

    val darkColorScheme = darkColorScheme(primary = Color(0xFF66ffc7))

    val colorScheme =
        when {
            supportsDynamicColor && isDarkTheme -> {
                dynamicDarkColorScheme(LocalContext.current)
            }
            supportsDynamicColor && !isDarkTheme -> {
                dynamicLightColorScheme(LocalContext.current)
            }
            isDarkTheme -> darkColorScheme
            else -> lightColorScheme
        }

    val typography =
        Typography(
            displaySmall = TextStyle(fontWeight = FontWeight.W100, fontSize = 96.sp),
            labelLarge = TextStyle(fontWeight = FontWeight.W600, fontSize = 14.sp)
        )

    val shapes = Shapes(extraSmall = RoundedCornerShape(3.0.dp), small = RoundedCornerShape(6.0.dp))

    MaterialTheme(colorScheme = colorScheme, typography = typography, shapes = shapes) {
        val currentTheme = if (!isSystemInDarkTheme()) "light" else "dark"
        ExtendedFloatingActionButton(
            text = { Text("FAB with text style and color from $currentTheme theme") },
            icon = { Icon(Icons.Filled.Favorite, contentDescription = "Localized Description") },
            onClick = {}
        )
    }
}

@Preview
@Sampled
@Composable
fun ThemeColorSample() {
    val colorScheme = MaterialTheme.colorScheme
    Box(Modifier.aspectRatio(1f).fillMaxSize().background(color = colorScheme.primary))
}

@Preview
@Sampled
@Composable
fun ThemeTextStyleSample() {
    val typography = MaterialTheme.typography
    Text(text = "Headline small styled text", style = typography.headlineSmall)
}

@Preview
@Sampled
@Composable
fun ThemeShapeSample() {
    val shape = MaterialTheme.shapes
    Button(shape = shape.small, onClick = {}) { Text("FAB with ${shape.small} shape") }
}
