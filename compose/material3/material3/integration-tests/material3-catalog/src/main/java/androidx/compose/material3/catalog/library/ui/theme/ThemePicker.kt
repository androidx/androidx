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
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.catalog.library.R
import androidx.compose.material3.catalog.library.model.ColorMode
import androidx.compose.material3.catalog.library.model.ExpressiveThemeMode
import androidx.compose.material3.catalog.library.model.FontScaleMode
import androidx.compose.material3.catalog.library.model.MaxFontScale
import androidx.compose.material3.catalog.library.model.MinFontScale
import androidx.compose.material3.catalog.library.model.TextDirection
import androidx.compose.material3.catalog.library.model.Theme
import androidx.compose.material3.catalog.library.model.ThemeColorMode
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

@Composable
fun ThemePicker(theme: Theme, onThemeChange: (theme: Theme) -> Unit) {
    val openExpressiveDialog = remember { mutableStateOf(false) }
    val expressiveThemeValue = remember { mutableStateOf(ExpressiveThemeMode.NonExpressive) }

    LazyColumn(
        contentPadding =
            WindowInsets.safeDrawing
                .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)
                .add(WindowInsets(top = ThemePickerPadding, bottom = ThemePickerPadding))
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
            val themeColorModes = ThemeColorMode.values()
            Column(
                modifier = Modifier.padding(ThemePickerPadding),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(ThemePickerPadding)) {
                    RadioButtonOption(
                        modifier = Modifier.weight(1f),
                        option = themeColorModes[0],
                        selected = themeColorModes[0] == theme.themeColorMode,
                        onClick = { onThemeChange(theme.copy(themeColorMode = it)) }
                    )
                    RadioButtonOption(
                        modifier = Modifier.weight(1f),
                        option = themeColorModes[1],
                        selected = themeColorModes[1] == theme.themeColorMode,
                        onClick = { onThemeChange(theme.copy(themeColorMode = it)) }
                    )
                }
                Row {
                    RadioButtonOption(
                        modifier = Modifier.weight(1f),
                        option = themeColorModes[2],
                        selected = themeColorModes[2] == theme.themeColorMode,
                        onClick = { onThemeChange(theme.copy(themeColorMode = it)) }
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
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(ThemePickerPadding)) {
                    RadioButtonOption(
                        modifier = Modifier.weight(1f),
                        option = colorModes[0],
                        selected = colorModes[0] == theme.colorMode,
                        onClick = { onThemeChange(theme.copy(colorMode = it)) }
                    )
                    RadioButtonOption(
                        modifier = Modifier.weight(1f),
                        option = colorModes[1],
                        selected = colorModes[1] == theme.colorMode,
                        onClick = { onThemeChange(theme.copy(colorMode = it)) }
                    )
                }
                Row {
                    RadioButtonOption(
                        modifier = Modifier.weight(1f),
                        option = colorModes[2],
                        selected = colorModes[2] == theme.colorMode,
                        enabled = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
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
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(ThemePickerPadding)) {
                    RadioButtonOption(
                        modifier = Modifier.weight(1f),
                        option = textDirections[0],
                        selected = textDirections[0] == theme.textDirection,
                        onClick = { onThemeChange(theme.copy(textDirection = it)) }
                    )
                    RadioButtonOption(
                        modifier = Modifier.weight(1f),
                        option = textDirections[1],
                        selected = textDirections[1] == theme.textDirection,
                        onClick = { onThemeChange(theme.copy(textDirection = it)) }
                    )
                }
                Row {
                    RadioButtonOption(
                        modifier = Modifier.weight(1f),
                        option = textDirections[2],
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
            Column(
                modifier = Modifier.padding(ThemePickerPadding),
            ) {
                RadioButtonOption(
                    option = FontScaleMode.System,
                    selected = theme.fontScaleMode == FontScaleMode.System,
                    onClick = { onThemeChange(theme.copy(fontScaleMode = FontScaleMode.System)) }
                )
                RadioButtonOption(
                    option = FontScaleMode.Custom,
                    selected = theme.fontScaleMode == FontScaleMode.Custom,
                    onClick = { onThemeChange(theme.copy(fontScaleMode = FontScaleMode.Custom)) }
                )

                var fontScale by remember(theme.fontScale) { mutableFloatStateOf(theme.fontScale) }
                CustomFontScaleSlider(
                    modifier = Modifier.fillMaxWidth(),
                    enabled = theme.fontScaleMode == FontScaleMode.Custom,
                    fontScale = fontScale,
                    onValueChange = { fontScale = it },
                    onValueChangeFinished = { onThemeChange(theme.copy(fontScale = fontScale)) }
                )
            }
            HorizontalDivider(Modifier.padding(horizontal = ThemePickerPadding))
        }
        item {
            Row {
                Text(
                    text = stringResource(id = R.string.expressive_theme_mode),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier =
                        Modifier.padding(
                            start = ThemePickerPadding,
                            // Align Badge closer to text
                            end = ThemePickerPadding / 2
                        )
                )
                Badge { Text(stringResource(R.string.experimental)) }
            }
            // LazyVerticalGrid can't be used within LazyColumn due to nested scrolling
            val expressiveThemeModes = ExpressiveThemeMode.values()
            Column(
                modifier = Modifier.padding(ThemePickerPadding),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(ThemePickerPadding)) {
                    RadioButtonOption(
                        modifier = Modifier.weight(1f),
                        option = expressiveThemeModes[0],
                        selected = expressiveThemeModes[0] == theme.expressiveThemeMode,
                        onClick = {
                            if (theme.expressiveThemeMode != it) {
                                expressiveThemeValue.value = it
                                openExpressiveDialog.value = true
                            }
                        }
                    )
                    RadioButtonOption(
                        modifier = Modifier.weight(1f),
                        option = expressiveThemeModes[1],
                        selected = expressiveThemeModes[1] == theme.expressiveThemeMode,
                        onClick = {
                            if (theme.expressiveThemeMode != it) {
                                expressiveThemeValue.value = it
                                openExpressiveDialog.value = true
                            }
                        }
                    )
                }
            }
            HorizontalDivider(Modifier.padding(horizontal = ThemePickerPadding))
        }
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(onClick = { onThemeChange(Theme()) }) {
                    Text(text = stringResource(id = R.string.reset_all))
                }
            }
        }
    }
    if (openExpressiveDialog.value) {
        ExpressiveAlertDialog(
            onDismissRequest = {
                // Return to previous data type.
                if (expressiveThemeValue.value == ExpressiveThemeMode.NonExpressive) {
                    expressiveThemeValue.value = ExpressiveThemeMode.Expressive
                } else {
                    expressiveThemeValue.value = ExpressiveThemeMode.NonExpressive
                }
                openExpressiveDialog.value = false
            },
            onDismissButtonClick = {
                // Return to previous data type.
                if (expressiveThemeValue.value == ExpressiveThemeMode.NonExpressive) {
                    expressiveThemeValue.value = ExpressiveThemeMode.Expressive
                } else {
                    expressiveThemeValue.value = ExpressiveThemeMode.NonExpressive
                }
                openExpressiveDialog.value = false
            },
            onConfirmButtonClick = {
                onThemeChange(theme.copy(expressiveThemeMode = expressiveThemeValue.value))
            }
        )
    }
}

@Composable
private fun <T> RadioButtonOption(
    option: T,
    selected: Boolean,
    onClick: (T) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Row(
        modifier =
            modifier
                .selectable(
                    selected = selected,
                    enabled = enabled,
                    onClick = { onClick(option) },
                    role = Role.RadioButton,
                )
                .minimumInteractiveComponentSize(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ThemePickerPadding)
    ) {
        RadioButton(
            selected = selected,
            enabled = enabled,
            onClick = null,
        )
        Text(text = option.toString(), style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun CustomFontScaleSlider(
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

@Composable
private fun ExpressiveAlertDialog(
    onDismissRequest: () -> Unit,
    onConfirmButtonClick: () -> Unit,
    onDismissButtonClick: () -> Unit,
) {
    AlertDialog(
        icon = { Icon(imageVector = Icons.Filled.Warning, contentDescription = null) },
        title = { Text("Warning") },
        text = {
            Text(
                "Setting a new Material theme will reset the catalog and progress will be " +
                    "lost. Please confirm before proceeding."
            )
        },
        onDismissRequest = onDismissRequest,
        confirmButton = { Button(onClick = onConfirmButtonClick) { Text("Confirm") } },
        dismissButton = { Button(onClick = onDismissButtonClick) { Text("Cancel") } },
    )
}

private val ThemePickerPadding = 16.dp
