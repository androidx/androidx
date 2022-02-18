/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.wear.compose.integration.demos

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.ScalingLazyColumn
import androidx.wear.compose.material.Text

@Composable
fun ThemeFonts() {
    ScalingLazyColumn {
        item {
            ThemeFontRow(style = MaterialTheme.typography.display1, description = "display1")
        }
        item {
            ThemeFontRow(style = MaterialTheme.typography.display2, description = "display2")
        }
        item {
            ThemeFontRow(style = MaterialTheme.typography.display3, description = "display3")
        }
        item {
            ThemeFontRow(style = MaterialTheme.typography.title1, description = "title1")
        }
        item {
            ThemeFontRow(style = MaterialTheme.typography.title2, description = "title2")
        }
        item {
            ThemeFontRow(style = MaterialTheme.typography.title3, description = "title3")
        }
        item {
            ThemeFontRow(style = MaterialTheme.typography.body1, description = "body1")
        }
        item {
            ThemeFontRow(style = MaterialTheme.typography.body2, description = "body2")
        }
        item {
            ThemeFontRow(style = MaterialTheme.typography.button, description = "button")
        }
        item {
            ThemeFontRow(style = MaterialTheme.typography.caption1, description = "caption1")
        }
        item {
            ThemeFontRow(style = MaterialTheme.typography.caption2, description = "caption2")
        }
        item {
            ThemeFontRow(style = MaterialTheme.typography.caption3, description = "caption3")
        }
    }
}

@Composable
fun ThemeFontRow(style: TextStyle, description: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentWidth(align = Alignment.CenterHorizontally)
    ) {
        Text(text = description, style = style)
    }
}

@Composable
fun ThemeColors() {
    ScalingLazyColumn {
        item {
            ThemeColorRow(
                backgroundColor = MaterialTheme.colors.background,
                backgroundColorName = "background",
                foregroundColor = MaterialTheme.colors.onBackground,
                foregroundColorName = "onBackground"
            )
        }
        item {
            ThemeColorRow(
                backgroundColor = MaterialTheme.colors.surface,
                backgroundColorName = "surface",
                foregroundColor = MaterialTheme.colors.onSurface,
                foregroundColorName = "onSurface"
            )
        }
        item {
            ThemeColorRow(
                backgroundColor = MaterialTheme.colors.surface,
                backgroundColorName = "surface",
                foregroundColor = MaterialTheme.colors.onSurfaceVariant,
                foregroundColorName = "onSurfaceVariant"
            )
        }
        item {
            ThemeColorRow(
                backgroundColor = MaterialTheme.colors.primary,
                backgroundColorName = "primary",
                foregroundColor = MaterialTheme.colors.onPrimary,
                foregroundColorName = "onPrimary"
            )
        }
        item {
            ThemeColorRow(
                backgroundColor = MaterialTheme.colors.primaryVariant,
                backgroundColorName = "primaryVariant",
                foregroundColor = MaterialTheme.colors.onPrimary,
                foregroundColorName = "onPrimary"
            )
        }
        item {
            ThemeColorRow(
                backgroundColor = MaterialTheme.colors.secondary,
                backgroundColorName = "secondary",
                foregroundColor = MaterialTheme.colors.onSecondary,
                foregroundColorName = "onSecondary"
            )
        }
        item {
            ThemeColorRow(
                backgroundColor = MaterialTheme.colors.secondaryVariant,
                backgroundColorName = "secondaryVariant",
                foregroundColor = MaterialTheme.colors.onSecondary,
                foregroundColorName = "onSecondary"
            )
        }
        item {
            ThemeColorRow(
                backgroundColor = MaterialTheme.colors.error,
                backgroundColorName = "error",
                foregroundColor = MaterialTheme.colors.onError,
                foregroundColorName = "onError"
            )
        }
    }
}

@Composable
fun ThemeColorRow(
    backgroundColor: Color,
    backgroundColorName: String,
    foregroundColor: Color,
    foregroundColorName: String
) {
    Row(
        modifier = Modifier
            .padding(vertical = 2.dp)
            .background(color = backgroundColor)
            .fillMaxWidth()
            .wrapContentWidth(align = Alignment.CenterHorizontally)
            .border(width = 1.dp, color = MaterialTheme.colors.onBackground)
    ) {
        Text(
            text = "foreground=$foregroundColorName background=$backgroundColorName",
            color = foregroundColor,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.button,
        )
    }
}