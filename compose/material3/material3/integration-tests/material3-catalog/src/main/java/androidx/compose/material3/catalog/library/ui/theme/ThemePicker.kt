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

package androidx.compose.material3.catalog.library.ui.theme

import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.catalog.library.R
import androidx.compose.material3.catalog.library.model.ColorMode
import androidx.compose.material3.catalog.library.model.FontScaleMode
import androidx.compose.material3.catalog.library.model.MaxFontScale
import androidx.compose.material3.catalog.library.model.MinFontScale
import androidx.compose.material3.catalog.library.model.TextDirection
import androidx.compose.material3.catalog.library.model.Theme
import androidx.compose.material3.catalog.library.model.ThemeMode
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

@Composable
fun ThemePicker(
    theme: Theme,
    onThemeChange: (theme: Theme) -> Unit
) {
    LazyColumn(
        contentPadding = WindowInsets.safeDrawing
            .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)
            .add(
                WindowInsets(
                    top = ThemePickerPadding,
                    bottom = ThemePickerPadding
                )
            )
            .asPaddingValues(),
        verticalArrangement = Arrangement.spacedBy(ThemePickerPadding)
    ) {
        item {
            Text(
                text = stringResource(id = R.string.theme),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = ThemePickerPadding)
            )
            // LazyVerticalGrid can't be used within LazyColumn due to nested scrolling
            val themeModes = ThemeMode.values()
            Column(
                modifier = Modifier.padding(ThemePickerPadding),
                verticalArrangement = Arrangement.spacedBy(ThemePickerPadding)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(ThemePickerPadding)) {
                    ThemeModeItem(
                        modifier = Modifier.weight(1f),
                        themeMode = themeModes[0],
                        selected = themeModes[0] == theme.themeMode,
                        onClick = { onThemeChange(theme.copy(themeMode = it)) }
                    )
                    ThemeModeItem(
                        modifier = Modifier.weight(1f),
                        themeMode = themeModes[1],
                        selected = themeModes[1] == theme.themeMode,
                        onClick = { onThemeChange(theme.copy(themeMode = it)) }
                    )
                }
                Row {
                    ThemeModeItem(
                        modifier = Modifier.weight(1f),
                        themeMode = themeModes[2],
                        selected = themeModes[2] == theme.themeMode,
                        onClick = { onThemeChange(theme.copy(themeMode = it)) }
                    )
                }
            }
            HorizontalDivider(Modifier.padding(horizontal = ThemePickerPadding))
        }
        item {
            Text(
                text = stringResource(id = R.string.color_mode),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = ThemePickerPadding)
            )
            // LazyVerticalGrid can't be used within LazyColumn due to nested scrolling
            val colorModes = ColorMode.values()
            Column(
                modifier = Modifier.padding(ThemePickerPadding),
                verticalArrangement = Arrangement.spacedBy(ThemePickerPadding)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(ThemePickerPadding)) {
                    ColorModeItem(
                        modifier = Modifier.weight(1f),
                        colorMode = colorModes[0],
                        selected = colorModes[0] == theme.colorMode,
                        onClick = { onThemeChange(theme.copy(colorMode = it)) }
                    )
                    ColorModeItem(
                        modifier = Modifier.weight(1f),
                        colorMode = colorModes[1],
                        selected = colorModes[1] == theme.colorMode,
                        onClick = { onThemeChange(theme.copy(colorMode = it)) }
                    )
                }
                Row {
                    ColorModeItem(
                        modifier = Modifier.weight(1f),
                        colorMode = colorModes[2],
                        selected = colorModes[2] == theme.colorMode,
                        onClick = { onThemeChange(theme.copy(colorMode = it)) }
                    )
                }
            }
            HorizontalDivider(Modifier.padding(horizontal = ThemePickerPadding))
        }
        item {
            Text(
                text = stringResource(id = R.string.text_direction),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = ThemePickerPadding)
            )
            // LazyVerticalGrid can't be used within LazyColumn due to nested scrolling
            val textDirections = TextDirection.values()
            Column(
                modifier = Modifier.padding(ThemePickerPadding),
                verticalArrangement = Arrangement.spacedBy(ThemePickerPadding)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(ThemePickerPadding)) {
                    TextDirectionItem(
                        modifier = Modifier.weight(1f),
                        textDirection = textDirections[0],
                        selected = textDirections[0] == theme.textDirection,
                        onClick = { onThemeChange(theme.copy(textDirection = it)) }
                    )
                    TextDirectionItem(
                        modifier = Modifier.weight(1f),
                        textDirection = textDirections[1],
                        selected = textDirections[1] == theme.textDirection,
                        onClick = { onThemeChange(theme.copy(textDirection = it)) }
                    )
                }
                Row {
                    TextDirectionItem(
                        modifier = Modifier.weight(1f),
                        textDirection = textDirections[2],
                        selected = textDirections[2] == theme.textDirection,
                        onClick = { onThemeChange(theme.copy(textDirection = it)) }
                    )
                }
            }
            HorizontalDivider(Modifier.padding(horizontal = ThemePickerPadding))
        }
        item {
            Text(
                text = stringResource(id = R.string.font_scale),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = ThemePickerPadding)
            )
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(ThemePickerPadding)
                ) {
                    RadioButton(
                        selected = theme.fontScaleMode == FontScaleMode.System,
                        onClick = {
                            onThemeChange(theme.copy(fontScaleMode = FontScaleMode.System))
                        },
                    )
                    Text(
                        text = FontScaleMode.System.label,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(ThemePickerPadding)
                ) {
                    RadioButton(
                        selected = theme.fontScaleMode == FontScaleMode.Custom,
                        onClick = {
                            onThemeChange(theme.copy(fontScaleMode = FontScaleMode.Custom))
                        },
                    )
                    Text(
                        text = FontScaleMode.Custom.label,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                var fontScale by remember { mutableFloatStateOf(theme.fontScale) }
                FontScaleItem(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = ThemePickerPadding),
                    enabled = theme.fontScaleMode == FontScaleMode.Custom,
                    fontScale = fontScale,
                    onValueChange = { fontScale = it },
                    onValueChangeFinished = { onThemeChange(theme.copy(fontScale = fontScale)) }
                )
            }
        }
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = { onThemeChange(Theme()) }
                ) {
                    Text(text = stringResource(id = R.string.reset_all))
                }
            }
        }
    }
}

@Composable
private fun ThemeModeItem(
    modifier: Modifier = Modifier,
    themeMode: ThemeMode,
    selected: Boolean,
    onClick: (ThemeMode) -> Unit
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ThemePickerPadding)
    ) {
        RadioButton(
            selected = selected,
            onClick = { onClick(themeMode) },
        )
        Text(
            text = themeMode.toString(),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun ColorModeItem(
    modifier: Modifier = Modifier,
    colorMode: ColorMode,
    selected: Boolean,
    onClick: (ColorMode) -> Unit
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ThemePickerPadding)
    ) {
        val enabled = when {
            colorMode == ColorMode.Dynamic && Build.VERSION.SDK_INT < Build.VERSION_CODES.S -> false
            else -> true
        }
        RadioButton(
            selected = selected,
            enabled = enabled,
            onClick = { onClick(colorMode) },
        )
        Text(
            text = colorMode.label,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun TextDirectionItem(
    modifier: Modifier = Modifier,
    textDirection: TextDirection,
    selected: Boolean,
    onClick: (TextDirection) -> Unit
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ThemePickerPadding)
    ) {
        RadioButton(
            selected = selected,
            onClick = { onClick(textDirection) },
        )
        Text(
            text = textDirection.toString(),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun FontScaleItem(
    modifier: Modifier = Modifier,
    enabled: Boolean,
    fontScale: Float,
    fontScaleMin: Float = MinFontScale,
    fontScaleMax: Float = MaxFontScale,
    onValueChange: (textScale: Float) -> Unit,
    onValueChangeFinished: () -> Unit
) {
    Column(modifier = modifier) {
        Slider(
            enabled = enabled,
            value = fontScale,
            onValueChange = onValueChange,
            onValueChangeFinished = onValueChangeFinished,
            valueRange = fontScaleMin..fontScaleMax,
        )
        Text(
            text = stringResource(id = R.string.scale, fontScale),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

private val ThemePickerPadding = 16.dp
