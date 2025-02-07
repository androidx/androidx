/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.compose.material3

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.material3.tokens.FilledIconButtonTokens
import androidx.compose.material3.tokens.FilledTonalIconButtonTokens
import androidx.compose.material3.tokens.LargeIconButtonTokens
import androidx.compose.material3.tokens.MediumIconButtonTokens
import androidx.compose.material3.tokens.OutlinedIconButtonTokens
import androidx.compose.material3.tokens.SmallIconButtonTokens
import androidx.compose.material3.tokens.StandardIconButtonTokens
import androidx.compose.material3.tokens.XLargeIconButtonTokens
import androidx.compose.material3.tokens.XSmallIconButtonTokens
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import kotlin.jvm.JvmInline

/** Contains the default values for all four icon and icon toggle button types. */
object IconButtonDefaults {
    /**
     * Contains the default values used by [IconButton]. [LocalContentColor] will be applied to the
     * icon and down the UI tree.
     *
     * See [iconButtonVibrantColors] for default values that applies the recommended high contrast
     * colors.
     */
    @Composable
    fun iconButtonColors(): IconButtonColors {
        val contentColor = LocalContentColor.current
        val colors = MaterialTheme.colorScheme.defaultIconButtonColors(contentColor)
        return if (colors.contentColor == contentColor) {
            colors
        } else {
            colors.copy(
                contentColor = contentColor,
                disabledContentColor =
                    contentColor.copy(alpha = StandardIconButtonTokens.DisabledOpacity)
            )
        }
    }

    /**
     * Creates a [IconButtonColors] that represents the default colors used in a [IconButton].
     * [LocalContentColor] will be applied to the icon and down the UI tree unless a custom
     * [contentColor] is provided.
     *
     * See [iconButtonVibrantColors] for default values that applies the recommended high contrast
     * colors.
     *
     * @param containerColor the container color of this icon button when enabled.
     * @param contentColor the content color of this icon button when enabled. By default, this will
     *   use the current LocalContentColor value.
     * @param disabledContainerColor the container color of this icon button when not enabled.
     * @param disabledContentColor the content color of this icon button when not enabled.
     */
    @Composable
    fun iconButtonColors(
        containerColor: Color = Color.Unspecified,
        contentColor: Color = LocalContentColor.current,
        disabledContainerColor: Color = Color.Unspecified,
        disabledContentColor: Color =
            contentColor.copy(alpha = StandardIconButtonTokens.DisabledOpacity)
    ): IconButtonColors =
        MaterialTheme.colorScheme
            .defaultIconButtonColors(LocalContentColor.current)
            .copy(
                containerColor = containerColor,
                contentColor = contentColor,
                disabledContainerColor = disabledContainerColor,
                disabledContentColor = disabledContentColor,
            )

    internal fun ColorScheme.defaultIconButtonColors(localContentColor: Color): IconButtonColors {
        return defaultIconButtonColorsCached
            ?: run {
                IconButtonColors(
                        containerColor = Color.Transparent,
                        contentColor = localContentColor,
                        disabledContainerColor = Color.Transparent,
                        disabledContentColor =
                            localContentColor.copy(alpha = StandardIconButtonTokens.DisabledOpacity)
                    )
                    .also { defaultIconButtonColorsCached = it }
            }
    }

    /**
     * Creates a [IconButtonColors] that represents the recommended high contrast colors used in an
     * [IconButton].
     *
     * See [iconButtonColors] for default values that applies [LocalContentColor] to the icon and
     * down the UI tree.
     */
    @Composable
    fun iconButtonVibrantColors(): IconButtonColors =
        MaterialTheme.colorScheme.defaultIconButtonVibrantColors()

    /**
     * Creates a [IconButtonColors] that represents the recommended high contrast colors used in an
     * [IconButton].
     *
     * See [iconButtonColors] for default values that applies [LocalContentColor] to the icon and
     * down the UI tree.
     *
     * @param containerColor the container color of this icon button when enabled.
     * @param contentColor the content color of this icon button when enabled.
     * @param disabledContainerColor the container color of this icon button when not enabled.
     * @param disabledContentColor the content color of this icon button when not enabled.
     */
    @Composable
    fun iconButtonVibrantColors(
        containerColor: Color = Color.Unspecified,
        contentColor: Color = Color.Unspecified,
        disabledContainerColor: Color = Color.Unspecified,
        disabledContentColor: Color =
            contentColor.copy(alpha = StandardIconButtonTokens.DisabledOpacity)
    ): IconButtonColors =
        MaterialTheme.colorScheme
            .defaultIconButtonVibrantColors()
            .copy(
                containerColor = containerColor,
                contentColor = contentColor,
                disabledContainerColor = disabledContainerColor,
                disabledContentColor = disabledContentColor,
            )

    internal fun ColorScheme.defaultIconButtonVibrantColors(): IconButtonColors {
        return defaultIconButtonVibrantColorsCached
            ?: run {
                IconButtonColors(
                        containerColor = Color.Transparent,
                        contentColor = fromToken(StandardIconButtonTokens.Color),
                        disabledContainerColor = Color.Transparent,
                        disabledContentColor =
                            fromToken(StandardIconButtonTokens.DisabledColor)
                                .copy(alpha = StandardIconButtonTokens.DisabledOpacity)
                    )
                    .also { defaultIconButtonVibrantColorsCached = it }
            }
    }

    /**
     * Creates a [IconToggleButtonColors] that represents the default colors used in a
     * [IconToggleButton]. [LocalContentColor] will be applied to the icon and down the UI tree.
     *
     * See [iconToggleButtonVibrantColors] for default values that applies the recommended high
     * contrast colors.
     */
    @Composable
    fun iconToggleButtonColors(): IconToggleButtonColors {
        val contentColor = LocalContentColor.current
        val colors = MaterialTheme.colorScheme.defaultIconToggleButtonColors(contentColor)
        if (colors.contentColor == contentColor) {
            return colors
        } else {
            return colors.copy(
                contentColor = contentColor,
                disabledContentColor =
                    contentColor.copy(alpha = StandardIconButtonTokens.DisabledOpacity)
            )
        }
    }

    /**
     * Creates a [IconToggleButtonColors] that represents the default colors used in a
     * [IconToggleButton]. [LocalContentColor] will be applied to the icon and down the UI tree
     * unless a custom [contentColor] is provided.
     *
     * See [iconToggleButtonVibrantColors] for default values that applies the recommended high
     * contrast colors.
     *
     * @param containerColor the container color of this icon button when enabled.
     * @param contentColor the content color of this icon button when enabled.
     * @param disabledContainerColor the container color of this icon button when not enabled.
     * @param disabledContentColor the content color of this icon button when not enabled.
     * @param checkedContainerColor the container color of this icon button when checked.
     * @param checkedContentColor the content color of this icon button when checked.
     */
    @Composable
    fun iconToggleButtonColors(
        containerColor: Color = Color.Unspecified,
        contentColor: Color = LocalContentColor.current,
        disabledContainerColor: Color = Color.Unspecified,
        disabledContentColor: Color =
            contentColor.copy(alpha = StandardIconButtonTokens.DisabledOpacity),
        checkedContainerColor: Color = Color.Unspecified,
        checkedContentColor: Color = Color.Unspecified
    ): IconToggleButtonColors =
        MaterialTheme.colorScheme
            .defaultIconToggleButtonColors(LocalContentColor.current)
            .copy(
                containerColor = containerColor,
                contentColor = contentColor,
                disabledContainerColor = disabledContainerColor,
                disabledContentColor = disabledContentColor,
                checkedContainerColor = checkedContainerColor,
                checkedContentColor = checkedContentColor,
            )

    internal fun ColorScheme.defaultIconToggleButtonColors(
        localContentColor: Color
    ): IconToggleButtonColors {
        return defaultIconToggleButtonColorsCached
            ?: run {
                IconToggleButtonColors(
                        containerColor = Color.Transparent,
                        contentColor = localContentColor,
                        disabledContainerColor = Color.Transparent,
                        disabledContentColor =
                            localContentColor.copy(
                                alpha = StandardIconButtonTokens.DisabledOpacity
                            ),
                        checkedContainerColor = Color.Transparent,
                        checkedContentColor = fromToken(StandardIconButtonTokens.SelectedColor)
                    )
                    .also { defaultIconToggleButtonColorsCached = it }
            }
    }

    /**
     * Creates a [IconToggleButtonColors] that represents the recommended high contrast colors used
     * in a [IconToggleButton]. See [iconToggleButtonColors] for default values that applies
     * [LocalContentColor] to the icon and down the UI tree.
     */
    @Composable
    fun iconToggleButtonVibrantColors(): IconToggleButtonColors =
        MaterialTheme.colorScheme.defaultIconToggleButtonVibrantColors()

    /**
     * Creates a [IconToggleButtonColors] that represents the recommended high contrast colors used
     * in a [IconToggleButton].
     *
     * See [iconToggleButtonColors] for default values that applies [LocalContentColor] to the icon
     * and down the UI tree.
     *
     * @param containerColor the container color of this icon button when enabled.
     * @param contentColor the content color of this icon button when enabled.
     * @param disabledContainerColor the container color of this icon button when not enabled.
     * @param disabledContentColor the content color of this icon button when not enabled.
     * @param checkedContainerColor the container color of this icon button when checked.
     * @param checkedContentColor the content color of this icon button when checked.
     */
    @Composable
    fun iconToggleButtonVibrantColors(
        containerColor: Color = Color.Unspecified,
        contentColor: Color = Color.Unspecified,
        disabledContainerColor: Color = Color.Unspecified,
        disabledContentColor: Color =
            contentColor.copy(alpha = StandardIconButtonTokens.DisabledOpacity),
        checkedContainerColor: Color = Color.Unspecified,
        checkedContentColor: Color = Color.Unspecified
    ): IconToggleButtonColors =
        MaterialTheme.colorScheme
            .defaultIconToggleButtonVibrantColors()
            .copy(
                containerColor = containerColor,
                contentColor = contentColor,
                disabledContainerColor = disabledContainerColor,
                disabledContentColor = disabledContentColor,
                checkedContainerColor = checkedContainerColor,
                checkedContentColor = checkedContentColor,
            )

    internal fun ColorScheme.defaultIconToggleButtonVibrantColors(): IconToggleButtonColors {
        return defaultIconToggleButtonVibrantColorsCached
            ?: run {
                IconToggleButtonColors(
                        containerColor = Color.Transparent,
                        contentColor = fromToken(StandardIconButtonTokens.UnselectedColor),
                        disabledContainerColor = Color.Transparent,
                        disabledContentColor =
                            fromToken(StandardIconButtonTokens.DisabledColor)
                                .copy(alpha = StandardIconButtonTokens.DisabledOpacity),
                        checkedContainerColor = Color.Transparent,
                        checkedContentColor = fromToken(StandardIconButtonTokens.SelectedColor)
                    )
                    .also { defaultIconToggleButtonVibrantColorsCached = it }
            }
    }

    /**
     * Creates a [IconButtonColors] that represents the default colors used in a [FilledIconButton].
     */
    @Composable
    fun filledIconButtonColors(): IconButtonColors =
        MaterialTheme.colorScheme.defaultFilledIconButtonColors

    /**
     * Creates a [IconButtonColors] that represents the default colors used in a [FilledIconButton].
     *
     * @param containerColor the container color of this icon button when enabled.
     * @param contentColor the content color of this icon button when enabled.
     * @param disabledContainerColor the container color of this icon button when not enabled.
     * @param disabledContentColor the content color of this icon button when not enabled.
     */
    @Composable
    fun filledIconButtonColors(
        containerColor: Color = Color.Unspecified,
        contentColor: Color = contentColorFor(containerColor),
        disabledContainerColor: Color = Color.Unspecified,
        disabledContentColor: Color = Color.Unspecified
    ): IconButtonColors =
        MaterialTheme.colorScheme.defaultFilledIconButtonColors.copy(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = disabledContainerColor,
            disabledContentColor = disabledContentColor,
        )

    internal val ColorScheme.defaultFilledIconButtonColors: IconButtonColors
        get() {
            return defaultFilledIconButtonColorsCached
                ?: IconButtonColors(
                        containerColor = fromToken(FilledIconButtonTokens.ContainerColor),
                        contentColor = fromToken(FilledIconButtonTokens.Color),
                        disabledContainerColor =
                            fromToken(FilledIconButtonTokens.DisabledContainerColor)
                                .copy(alpha = FilledIconButtonTokens.DisabledContainerOpacity),
                        disabledContentColor =
                            fromToken(FilledIconButtonTokens.DisabledColor)
                                .copy(alpha = FilledIconButtonTokens.DisabledOpacity)
                    )
                    .also { defaultFilledIconButtonColorsCached = it }
        }

    /**
     * Creates a [IconToggleButtonColors] that represents the default colors used in a
     * [FilledIconToggleButton].
     */
    @Composable
    fun filledIconToggleButtonColors(): IconToggleButtonColors =
        MaterialTheme.colorScheme.defaultFilledIconToggleButtonColors

    /**
     * Creates a [IconToggleButtonColors] that represents the default colors used in a
     * [FilledIconToggleButton].
     *
     * @param containerColor the container color of this icon button when enabled.
     * @param contentColor the content color of this icon button when enabled.
     * @param disabledContainerColor the container color of this icon button when not enabled.
     * @param disabledContentColor the content color of this icon button when not enabled.
     * @param checkedContainerColor the container color of this icon button when checked.
     * @param checkedContentColor the content color of this icon button when checked.
     */
    @Composable
    fun filledIconToggleButtonColors(
        containerColor: Color = Color.Unspecified,
        // TODO(b/228455081): Using contentColorFor here will return OnSurfaceVariant,
        //  while the token value is Primary.
        contentColor: Color = Color.Unspecified,
        disabledContainerColor: Color = Color.Unspecified,
        disabledContentColor: Color = Color.Unspecified,
        checkedContainerColor: Color = Color.Unspecified,
        checkedContentColor: Color = contentColorFor(checkedContainerColor)
    ): IconToggleButtonColors =
        MaterialTheme.colorScheme.defaultFilledIconToggleButtonColors.copy(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = disabledContainerColor,
            disabledContentColor = disabledContentColor,
            checkedContainerColor = checkedContainerColor,
            checkedContentColor = checkedContentColor,
        )

    internal val ColorScheme.defaultFilledIconToggleButtonColors: IconToggleButtonColors
        get() {
            return defaultFilledIconToggleButtonColorsCached
                ?: IconToggleButtonColors(
                        containerColor = fromToken(FilledIconButtonTokens.UnselectedContainerColor),
                        // TODO(b/228455081): Using contentColorFor here will return
                        // OnSurfaceVariant,
                        //  while the token value is Primary.
                        contentColor = fromToken(FilledIconButtonTokens.UnselectedColor),
                        disabledContainerColor =
                            fromToken(FilledIconButtonTokens.DisabledContainerColor)
                                .copy(alpha = FilledIconButtonTokens.DisabledContainerOpacity),
                        disabledContentColor =
                            fromToken(FilledIconButtonTokens.DisabledColor)
                                .copy(alpha = FilledIconButtonTokens.DisabledOpacity),
                        checkedContainerColor =
                            fromToken(FilledIconButtonTokens.SelectedContainerColor),
                        checkedContentColor = fromToken(FilledIconButtonTokens.SelectedColor)
                    )
                    .also { defaultFilledIconToggleButtonColorsCached = it }
        }

    /**
     * Creates a [IconButtonColors] that represents the default colors used in a
     * [FilledTonalIconButton].
     */
    @Composable
    fun filledTonalIconButtonColors(): IconButtonColors =
        MaterialTheme.colorScheme.defaultFilledTonalIconButtonColors

    /**
     * Creates a [IconButtonColors] that represents the default colors used in a
     * [FilledTonalIconButton].
     *
     * @param containerColor the container color of this icon button when enabled.
     * @param contentColor the content color of this icon button when enabled.
     * @param disabledContainerColor the container color of this icon button when not enabled.
     * @param disabledContentColor the content color of this icon button when not enabled.
     */
    @Composable
    fun filledTonalIconButtonColors(
        containerColor: Color = Color.Unspecified,
        contentColor: Color = contentColorFor(containerColor),
        disabledContainerColor: Color = Color.Unspecified,
        disabledContentColor: Color = Color.Unspecified
    ): IconButtonColors =
        MaterialTheme.colorScheme.defaultFilledTonalIconButtonColors.copy(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = disabledContainerColor,
            disabledContentColor = disabledContentColor,
        )

    internal val ColorScheme.defaultFilledTonalIconButtonColors: IconButtonColors
        get() {
            return defaultFilledTonalIconButtonColorsCached
                ?: IconButtonColors(
                        containerColor = fromToken(FilledTonalIconButtonTokens.ContainerColor),
                        contentColor = fromToken(FilledTonalIconButtonTokens.Color),
                        disabledContainerColor =
                            fromToken(FilledTonalIconButtonTokens.DisabledContainerColor)
                                .copy(alpha = FilledTonalIconButtonTokens.DisabledContainerOpacity),
                        disabledContentColor =
                            fromToken(FilledTonalIconButtonTokens.DisabledColor)
                                .copy(alpha = FilledTonalIconButtonTokens.DisabledOpacity)
                    )
                    .also { defaultFilledTonalIconButtonColorsCached = it }
        }

    /**
     * Creates a [IconToggleButtonColors] that represents the default colors used in a
     * [FilledTonalIconToggleButton].
     */
    @Composable
    fun filledTonalIconToggleButtonColors(): IconToggleButtonColors =
        MaterialTheme.colorScheme.defaultFilledTonalIconToggleButtonColors

    /**
     * Creates a [IconToggleButtonColors] that represents the default colors used in a
     * [FilledTonalIconToggleButton].
     *
     * @param containerColor the container color of this icon button when enabled.
     * @param contentColor the content color of this icon button when enabled.
     * @param disabledContainerColor the container color of this icon button when not enabled.
     * @param disabledContentColor the content color of this icon button when not enabled.
     * @param checkedContainerColor the container color of this icon button when checked.
     * @param checkedContentColor the content color of this icon button when checked.
     */
    @Composable
    fun filledTonalIconToggleButtonColors(
        containerColor: Color = Color.Unspecified,
        contentColor: Color = contentColorFor(containerColor),
        disabledContainerColor: Color = Color.Unspecified,
        disabledContentColor: Color = Color.Unspecified,
        checkedContainerColor: Color = Color.Unspecified,
        checkedContentColor: Color = contentColorFor(checkedContainerColor)
    ): IconToggleButtonColors =
        MaterialTheme.colorScheme.defaultFilledTonalIconToggleButtonColors.copy(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = disabledContainerColor,
            disabledContentColor = disabledContentColor,
            checkedContainerColor = checkedContainerColor,
            checkedContentColor = checkedContentColor,
        )

    internal val ColorScheme.defaultFilledTonalIconToggleButtonColors: IconToggleButtonColors
        get() {
            return defaultFilledTonalIconToggleButtonColorsCached
                ?: IconToggleButtonColors(
                        containerColor =
                            fromToken(FilledTonalIconButtonTokens.UnselectedContainerColor),
                        contentColor = fromToken(FilledTonalIconButtonTokens.UnselectedColor),
                        disabledContainerColor =
                            fromToken(FilledTonalIconButtonTokens.DisabledContainerColor)
                                .copy(alpha = FilledTonalIconButtonTokens.DisabledContainerOpacity),
                        disabledContentColor =
                            fromToken(FilledTonalIconButtonTokens.DisabledColor)
                                .copy(alpha = FilledTonalIconButtonTokens.DisabledOpacity),
                        checkedContainerColor =
                            fromToken(FilledTonalIconButtonTokens.SelectedContainerColor),
                        checkedContentColor = fromToken(FilledTonalIconButtonTokens.SelectedColor)
                    )
                    .also { defaultFilledTonalIconToggleButtonColorsCached = it }
        }

    /**
     * Creates a [IconButtonColors] that represents the default colors used in a
     * [OutlinedIconButton]. [LocalContentColor] will be applied to the icon and down the UI tree.
     *
     * See [outlinedIconButtonVibrantColors] for default values that applies the recommended high
     * contrast colors.
     */
    @Composable
    fun outlinedIconButtonColors(): IconButtonColors {
        val contentColor = LocalContentColor.current
        val colors = MaterialTheme.colorScheme.defaultOutlinedIconButtonColors(contentColor)
        if (colors.contentColor == contentColor) {
            return colors
        } else {
            return colors.copy(
                contentColor = contentColor,
                disabledContentColor =
                    contentColor.copy(alpha = OutlinedIconButtonTokens.DisabledOpacity)
            )
        }
    }

    /**
     * Creates a [IconButtonColors] that represents the default colors used in a
     * [OutlinedIconButton].
     *
     * See [outlinedIconButtonVibrantColors] for default values that applies the recommended high
     * contrast colors.
     *
     * @param containerColor the container color of this icon button when enabled.
     * @param contentColor the content color of this icon button when enabled.
     * @param disabledContainerColor the container color of this icon button when not enabled.
     * @param disabledContentColor the content color of this icon button when not enabled.
     */
    @Composable
    fun outlinedIconButtonColors(
        containerColor: Color = Color.Unspecified,
        contentColor: Color = LocalContentColor.current,
        disabledContainerColor: Color = Color.Unspecified,
        disabledContentColor: Color =
            contentColor.copy(alpha = OutlinedIconButtonTokens.DisabledOpacity)
    ): IconButtonColors =
        MaterialTheme.colorScheme
            .defaultOutlinedIconButtonColors(LocalContentColor.current)
            .copy(
                containerColor = containerColor,
                contentColor = contentColor,
                disabledContainerColor = disabledContainerColor,
                disabledContentColor = disabledContentColor,
            )

    internal fun ColorScheme.defaultOutlinedIconButtonColors(
        localContentColor: Color
    ): IconButtonColors {
        return defaultOutlinedIconButtonColorsCached
            ?: run {
                IconButtonColors(
                        containerColor = Color.Transparent,
                        contentColor = localContentColor,
                        disabledContainerColor = Color.Transparent,
                        disabledContentColor =
                            localContentColor.copy(alpha = OutlinedIconButtonTokens.DisabledOpacity)
                    )
                    .also { defaultOutlinedIconButtonColorsCached = it }
            }
    }

    /**
     * Creates a [IconButtonColors] that represents the default colors used in a
     * [OutlinedIconButton].
     *
     * See [outlinedIconButtonColors] for default values that applies [LocalContentColor] to the
     * icon and down the UI tree.
     */
    @Composable
    fun outlinedIconButtonVibrantColors(): IconButtonColors =
        MaterialTheme.colorScheme.defaultOutlinedIconButtonVibrantColors()

    /**
     * Creates a [IconButtonColors] that represents the default colors used in a
     * [OutlinedIconButton].
     *
     * See [outlinedIconButtonColors] for default values that applies [LocalContentColor] to the
     * icon and down the UI tree.
     *
     * @param containerColor the container color of this icon button when enabled.
     * @param contentColor the content color of this icon button when enabled.
     * @param disabledContainerColor the container color of this icon button when not enabled.
     * @param disabledContentColor the content color of this icon button when not enabled.
     */
    @Composable
    fun outlinedIconButtonVibrantColors(
        containerColor: Color = Color.Unspecified,
        contentColor: Color = Color.Unspecified,
        disabledContainerColor: Color = Color.Unspecified,
        disabledContentColor: Color =
            contentColor.copy(alpha = OutlinedIconButtonTokens.DisabledOpacity)
    ): IconButtonColors =
        MaterialTheme.colorScheme
            .defaultOutlinedIconButtonVibrantColors()
            .copy(
                containerColor = containerColor,
                contentColor = contentColor,
                disabledContainerColor = disabledContainerColor,
                disabledContentColor = disabledContentColor,
            )

    internal fun ColorScheme.defaultOutlinedIconButtonVibrantColors(): IconButtonColors {
        return defaultOutlinedIconButtonVibrantColorsCached
            ?: run {
                IconButtonColors(
                        containerColor = Color.Transparent,
                        contentColor = fromToken(OutlinedIconButtonTokens.Color),
                        disabledContainerColor = Color.Transparent,
                        disabledContentColor =
                            fromToken(OutlinedIconButtonTokens.DisabledColor)
                                .copy(alpha = OutlinedIconButtonTokens.DisabledOpacity)
                    )
                    .also { defaultOutlinedIconButtonVibrantColorsCached = it }
            }
    }

    /**
     * Creates a [IconToggleButtonColors] that represents the default colors used in a
     * [OutlinedIconToggleButton]. [LocalContentColor] will be applied to the icon and down the UI
     * tree.
     *
     * See [outlinedIconButtonVibrantColors] for default values that applies the recommended high
     * contrast colors.
     */
    @Composable
    fun outlinedIconToggleButtonColors(): IconToggleButtonColors {
        val contentColor = LocalContentColor.current
        val colors = MaterialTheme.colorScheme.defaultOutlinedIconToggleButtonColors(contentColor)
        if (colors.contentColor == contentColor) {
            return colors
        } else {
            return colors.copy(
                contentColor = contentColor,
                disabledContentColor =
                    contentColor.copy(alpha = OutlinedIconButtonTokens.DisabledOpacity)
            )
        }
    }

    /**
     * Creates a [IconToggleButtonColors] that represents the default colors used in a
     * [OutlinedIconToggleButton]. [LocalContentColor] will be applied to the icon and down the UI
     * tree.
     *
     * See [outlinedIconButtonVibrantColors] for default values that applies the recommended high
     * contrast colors.
     *
     * @param containerColor the container color of this icon button when enabled.
     * @param contentColor the content color of this icon button when enabled.
     * @param disabledContainerColor the container color of this icon button when not enabled.
     * @param disabledContentColor the content color of this icon button when not enabled.
     * @param checkedContainerColor the container color of this icon button when checked.
     * @param checkedContentColor the content color of this icon button when checked.
     */
    @Composable
    fun outlinedIconToggleButtonColors(
        containerColor: Color = Color.Unspecified,
        contentColor: Color = LocalContentColor.current,
        disabledContainerColor: Color = Color.Unspecified,
        disabledContentColor: Color =
            contentColor.copy(alpha = OutlinedIconButtonTokens.DisabledOpacity),
        checkedContainerColor: Color = Color.Unspecified,
        checkedContentColor: Color = contentColorFor(checkedContainerColor)
    ): IconToggleButtonColors =
        MaterialTheme.colorScheme
            .defaultOutlinedIconToggleButtonColors(LocalContentColor.current)
            .copy(
                containerColor = containerColor,
                contentColor = contentColor,
                disabledContainerColor = disabledContainerColor,
                disabledContentColor = disabledContentColor,
                checkedContainerColor = checkedContainerColor,
                checkedContentColor = checkedContentColor,
            )

    internal fun ColorScheme.defaultOutlinedIconToggleButtonColors(
        localContentColor: Color
    ): IconToggleButtonColors {
        return defaultIconToggleButtonColorsCached
            ?: run {
                IconToggleButtonColors(
                        containerColor = Color.Transparent,
                        contentColor = localContentColor,
                        disabledContainerColor = Color.Transparent,
                        disabledContentColor =
                            localContentColor.copy(
                                alpha = OutlinedIconButtonTokens.DisabledOpacity
                            ),
                        checkedContainerColor =
                            fromToken(OutlinedIconButtonTokens.SelectedContainerColor),
                        checkedContentColor =
                            contentColorFor(
                                fromToken(OutlinedIconButtonTokens.SelectedContainerColor)
                            )
                    )
                    .also { defaultOutlinedIconToggleButtonColorsCached = it }
            }
    }

    /**
     * Creates a [IconToggleButtonColors] that represents the default colors used in a
     * [OutlinedIconToggleButton].
     *
     * See [outlinedIconToggleButtonColors] for default values that applies [LocalContentColor] to
     * the icon and down the UI tree.
     */
    @Composable
    fun outlinedIconToggleButtonVibrantColors(): IconToggleButtonColors =
        MaterialTheme.colorScheme.defaultOutlinedIconToggleButtonVibrantColors()

    /**
     * Creates a [IconToggleButtonColors] that represents the default colors used in a
     * [OutlinedIconToggleButton].
     *
     * See [outlinedIconToggleButtonColors] for default values that applies [LocalContentColor] to
     * the icon and down the UI tree.
     *
     * @param containerColor the container color of this icon button when enabled.
     * @param contentColor the content color of this icon button when enabled.
     * @param disabledContainerColor the container color of this icon button when not enabled.
     * @param disabledContentColor the content color of this icon button when not enabled.
     * @param checkedContainerColor the container color of this icon button when checked.
     * @param checkedContentColor the content color of this icon button when checked.
     */
    @Composable
    fun outlinedIconToggleButtonVibrantColors(
        containerColor: Color = Color.Unspecified,
        contentColor: Color = Color.Unspecified,
        disabledContainerColor: Color = Color.Unspecified,
        disabledContentColor: Color =
            contentColor.copy(alpha = OutlinedIconButtonTokens.DisabledOpacity),
        checkedContainerColor: Color = Color.Unspecified,
        checkedContentColor: Color = contentColorFor(checkedContainerColor)
    ): IconToggleButtonColors =
        MaterialTheme.colorScheme
            .defaultOutlinedIconToggleButtonVibrantColors()
            .copy(
                containerColor = containerColor,
                contentColor = contentColor,
                disabledContainerColor = disabledContainerColor,
                disabledContentColor = disabledContentColor,
                checkedContainerColor = checkedContainerColor,
                checkedContentColor = checkedContentColor,
            )

    internal fun ColorScheme.defaultOutlinedIconToggleButtonVibrantColors():
        IconToggleButtonColors {
        return defaultOutlinedIconToggleButtonVibrantColorsCached
            ?: run {
                IconToggleButtonColors(
                        containerColor = Color.Transparent,
                        contentColor = fromToken(OutlinedIconButtonTokens.UnselectedColor),
                        disabledContainerColor = Color.Transparent,
                        disabledContentColor =
                            fromToken(OutlinedIconButtonTokens.DisabledColor)
                                .copy(alpha = OutlinedIconButtonTokens.DisabledOpacity),
                        checkedContainerColor =
                            fromToken(OutlinedIconButtonTokens.SelectedContainerColor),
                        checkedContentColor = fromToken(OutlinedIconButtonTokens.SelectedColor)
                    )
                    .also { defaultOutlinedIconToggleButtonColorsCached = it }
            }
    }

    /**
     * Represents the [BorderStroke] for an [OutlinedIconButton], depending on its [enabled] and
     * [checked] state. [LocalContentColor] will be used as the border color.
     *
     * See [outlinedIconToggleButtonVibrantBorder] for a [BorderStroke] that uses the spec
     * recommended color as the border color.
     *
     * @param enabled whether the icon button is enabled
     * @param checked whether the icon button is checked
     */
    @Composable
    fun outlinedIconToggleButtonBorder(enabled: Boolean, checked: Boolean): BorderStroke? {
        if (checked) {
            return null
        }
        return outlinedIconButtonBorder(enabled)
    }

    /**
     * Represents the [BorderStroke] for an [OutlinedIconButton], depending on its [enabled] and
     * [checked] state. The spec recommended color will be used as the border color.
     *
     * @param enabled whether the icon button is enabled
     * @param checked whether the icon button is checked
     */
    @Composable
    fun outlinedIconToggleButtonVibrantBorder(enabled: Boolean, checked: Boolean): BorderStroke? {
        if (checked) {
            return null
        }
        return outlinedIconButtonVibrantBorder(enabled)
    }

    /**
     * Represents the [BorderStroke] for an [OutlinedIconButton], depending on its [enabled] state.
     * [LocalContentColor] will be used as the border color.
     *
     * See [outlinedIconToggleButtonVibrantBorder] for a [BorderStroke] that uses the spec
     * recommended color as the border color.
     *
     * @param enabled whether the icon button is enabled
     */
    @Composable
    fun outlinedIconButtonBorder(enabled: Boolean): BorderStroke {
        val outlineColor = LocalContentColor.current
        val color: Color =
            if (enabled) {
                outlineColor
            } else {
                outlineColor.copy(alpha = OutlinedIconButtonTokens.DisabledContainerOpacity)
            }
        return remember(color) { BorderStroke(SmallIconButtonTokens.OutlinedOutlineWidth, color) }
    }

    /**
     * Represents the [BorderStroke] for an [OutlinedIconButton], depending on its [enabled] state.
     * The spec recommended color will be used as the border color.
     *
     * @param enabled whether the icon button is enabled
     */
    @Composable
    fun outlinedIconButtonVibrantBorder(enabled: Boolean): BorderStroke {
        val outlineColor = OutlinedIconButtonTokens.OutlineColor.value
        val color: Color =
            if (enabled) {
                outlineColor
            } else {
                outlineColor.copy(alpha = OutlinedIconButtonTokens.DisabledContainerOpacity)
            }
        return remember(color) { BorderStroke(SmallIconButtonTokens.OutlinedOutlineWidth, color) }
    }

    /** Default ripple shape for a standard icon button. */
    val standardShape: Shape
        @Composable get() = SmallIconButtonTokens.ContainerShapeRound.value

    /** Default shape for a filled icon button. */
    val filledShape: Shape
        @Composable get() = SmallIconButtonTokens.ContainerShapeRound.value

    /** Default shape for an outlined icon button. */
    val outlinedShape: Shape
        @Composable get() = SmallIconButtonTokens.ContainerShapeRound.value

    @Suppress("OPT_IN_MARKER_ON_WRONG_TARGET")
    @get:ExperimentalMaterial3ExpressiveApi
    @ExperimentalMaterial3ExpressiveApi
    /** Default round shape for any extra small icon button. */
    val xSmallRoundShape: Shape
        @Composable get() = XSmallIconButtonTokens.ContainerShapeRound.value

    @Suppress("OPT_IN_MARKER_ON_WRONG_TARGET")
    @get:ExperimentalMaterial3ExpressiveApi
    @ExperimentalMaterial3ExpressiveApi
    /** Default square shape for any extra small icon button. */
    val xSmallSquareShape: Shape
        @Composable get() = XSmallIconButtonTokens.ContainerShapeSquare.value

    @Suppress("OPT_IN_MARKER_ON_WRONG_TARGET")
    @get:ExperimentalMaterial3ExpressiveApi
    @ExperimentalMaterial3ExpressiveApi
    /** Default pressed shape for any extra small icon button. */
    val xSmallPressedShape: Shape
        @Composable get() = XSmallIconButtonTokens.PressedContainerShape.value

    @Suppress("OPT_IN_MARKER_ON_WRONG_TARGET")
    @get:ExperimentalMaterial3ExpressiveApi
    @ExperimentalMaterial3ExpressiveApi
    /** Default selected shape for any extra small icon button. */
    val xSmallSelectedRoundShape: Shape
        @Composable get() = XSmallIconButtonTokens.SelectedContainerShapeRound.value

    @Suppress("OPT_IN_MARKER_ON_WRONG_TARGET")
    @get:ExperimentalMaterial3ExpressiveApi
    @ExperimentalMaterial3ExpressiveApi
    /** Default selected shape for any extra small, square icon button. */
    val xSmallSelectedSquareShape: Shape
        @Composable get() = XSmallIconButtonTokens.SelectedContainerShapeSquare.value

    @Suppress("OPT_IN_MARKER_ON_WRONG_TARGET")
    @get:ExperimentalMaterial3ExpressiveApi
    @ExperimentalMaterial3ExpressiveApi
    /** Default shape for any small icon button. */
    val smallRoundShape: Shape
        @Composable get() = SmallIconButtonTokens.ContainerShapeRound.value

    @Suppress("OPT_IN_MARKER_ON_WRONG_TARGET")
    @get:ExperimentalMaterial3ExpressiveApi
    @ExperimentalMaterial3ExpressiveApi
    /** Default square shape for any small icon button. */
    val smallSquareShape: Shape
        @Composable get() = SmallIconButtonTokens.ContainerShapeSquare.value

    @Suppress("OPT_IN_MARKER_ON_WRONG_TARGET")
    @get:ExperimentalMaterial3ExpressiveApi
    @ExperimentalMaterial3ExpressiveApi
    /** Default pressed shape for any small icon button. */
    val smallPressedShape: Shape
        @Composable get() = SmallIconButtonTokens.PressedContainerShape.value

    @Suppress("OPT_IN_MARKER_ON_WRONG_TARGET")
    @get:ExperimentalMaterial3ExpressiveApi
    @ExperimentalMaterial3ExpressiveApi
    /** Default selected shape for any small icon button. */
    val smallSelectedRoundShape: Shape
        @Composable get() = SmallIconButtonTokens.SelectedContainerShapeRound.value

    @Suppress("OPT_IN_MARKER_ON_WRONG_TARGET")
    @get:ExperimentalMaterial3ExpressiveApi
    @ExperimentalMaterial3ExpressiveApi
    /** Default selected shape for any small, square icon button. */
    val SmallSelectedSquareShape: Shape
        @Composable get() = SmallIconButtonTokens.SelectedContainerShapeSquare.value

    @Suppress("OPT_IN_MARKER_ON_WRONG_TARGET")
    @get:ExperimentalMaterial3ExpressiveApi
    @ExperimentalMaterial3ExpressiveApi
    /** Default shape for any medium icon button. */
    val mediumRoundShape: Shape
        @Composable get() = MediumIconButtonTokens.ContainerShapeRound.value

    @Suppress("OPT_IN_MARKER_ON_WRONG_TARGET")
    @get:ExperimentalMaterial3ExpressiveApi
    @ExperimentalMaterial3ExpressiveApi
    /** Default shape for any medium icon button. */
    val mediumSquareShape: Shape
        @Composable get() = MediumIconButtonTokens.ContainerShapeSquare.value

    @Suppress("OPT_IN_MARKER_ON_WRONG_TARGET")
    @get:ExperimentalMaterial3ExpressiveApi
    @ExperimentalMaterial3ExpressiveApi
    /** Default pressed shape for any medium icon button. */
    val mediumPressedShape: Shape
        @Composable get() = MediumIconButtonTokens.PressedContainerShape.value

    @Suppress("OPT_IN_MARKER_ON_WRONG_TARGET")
    @get:ExperimentalMaterial3ExpressiveApi
    @ExperimentalMaterial3ExpressiveApi
    /** Default selected shape for any medium icon button. */
    val mediumSelectedRoundShape: Shape
        @Composable get() = MediumIconButtonTokens.SelectedContainerShapeRound.value

    @Suppress("OPT_IN_MARKER_ON_WRONG_TARGET")
    @get:ExperimentalMaterial3ExpressiveApi
    @ExperimentalMaterial3ExpressiveApi
    /** Default selected shape for any medium, square icon button. */
    val mediumSelectedSquareShape: Shape
        @Composable get() = MediumIconButtonTokens.SelectedContainerShapeSquare.value

    @Suppress("OPT_IN_MARKER_ON_WRONG_TARGET")
    @get:ExperimentalMaterial3ExpressiveApi
    @ExperimentalMaterial3ExpressiveApi
    /** Default shape for any large icon button. */
    val largeRoundShape: Shape
        @Composable get() = LargeIconButtonTokens.ContainerShapeRound.value

    @Suppress("OPT_IN_MARKER_ON_WRONG_TARGET")
    @get:ExperimentalMaterial3ExpressiveApi
    @ExperimentalMaterial3ExpressiveApi
    /** Default shape for any large icon button. */
    val largeSquareShape: Shape
        @Composable get() = LargeIconButtonTokens.ContainerShapeSquare.value

    @Suppress("OPT_IN_MARKER_ON_WRONG_TARGET")
    @get:ExperimentalMaterial3ExpressiveApi
    @ExperimentalMaterial3ExpressiveApi
    /** Default pressed shape for any large icon button. */
    val largePressedShape: Shape
        @Composable get() = LargeIconButtonTokens.PressedContainerShape.value

    @Suppress("OPT_IN_MARKER_ON_WRONG_TARGET")
    @get:ExperimentalMaterial3ExpressiveApi
    @ExperimentalMaterial3ExpressiveApi
    /** Default selected shape for any large icon button. */
    val largeSelectedRoundShape: Shape
        @Composable get() = LargeIconButtonTokens.SelectedContainerShapeRound.value

    @Suppress("OPT_IN_MARKER_ON_WRONG_TARGET")
    @get:ExperimentalMaterial3ExpressiveApi
    @ExperimentalMaterial3ExpressiveApi
    /** Default selected shape for any large, square icon button. */
    val largeSelectedSquareShape: Shape
        @Composable get() = LargeIconButtonTokens.SelectedContainerShapeSquare.value

    @Suppress("OPT_IN_MARKER_ON_WRONG_TARGET")
    @get:ExperimentalMaterial3ExpressiveApi
    @ExperimentalMaterial3ExpressiveApi
    /** Default shape for any xlarge icon button. */
    val xLargeRoundShape: Shape
        @Composable get() = XLargeIconButtonTokens.ContainerShapeRound.value

    @Suppress("OPT_IN_MARKER_ON_WRONG_TARGET")
    @get:ExperimentalMaterial3ExpressiveApi
    @ExperimentalMaterial3ExpressiveApi
    /** Default shape for any xlarge icon button. */
    val xLargeSquareShape: Shape
        @Composable get() = XLargeIconButtonTokens.ContainerShapeSquare.value

    @Suppress("OPT_IN_MARKER_ON_WRONG_TARGET")
    @get:ExperimentalMaterial3ExpressiveApi
    @ExperimentalMaterial3ExpressiveApi
    /** Default pressed shape for any extra large icon button. */
    val xLargePressedShape: Shape
        @Composable get() = XLargeIconButtonTokens.PressedContainerShape.value

    @Suppress("OPT_IN_MARKER_ON_WRONG_TARGET")
    @get:ExperimentalMaterial3ExpressiveApi
    @ExperimentalMaterial3ExpressiveApi
    /** Default selected shape for any extra large icon button. */
    val xLargeSelectedRoundShape: Shape
        @Composable get() = XLargeIconButtonTokens.SelectedContainerShapeRound.value

    @Suppress("OPT_IN_MARKER_ON_WRONG_TARGET")
    @get:ExperimentalMaterial3ExpressiveApi
    @ExperimentalMaterial3ExpressiveApi
    /** Default selected shape for any extra large, square icon button. */
    val xLargeSelectedSquareShape: Shape
        @Composable get() = XLargeIconButtonTokens.SelectedContainerShapeSquare.value

    /**
     * Creates a [IconButtonShapes] that correspond to the shapes in the default or pressed states.
     * Icon button will morph between these shapes as long as the shapes are all
     * [CornerBasedShape]s.
     *
     * @param shape the unchecked shape for [ButtonShapes]
     * @param pressedShape the unchecked shape for [ButtonShapes]
     */
    @ExperimentalMaterial3ExpressiveApi
    @Composable
    fun shapes(shape: Shape? = null, pressedShape: Shape? = null): IconButtonShapes =
        MaterialTheme.shapes.defaultIconButtonShapes.copy(
            shape = shape,
            pressedShape = pressedShape,
        )

    /**
     * Creates a [IconButtonShapes] that correspond to a default [IconButton] in the active and
     * pressed states. [IconButton] will morph between these shapes as long as the shapes are all
     * [CornerBasedShape]s.
     */
    @ExperimentalMaterial3ExpressiveApi
    @Composable
    fun shapes(): IconButtonShapes = MaterialTheme.shapes.defaultIconButtonShapes

    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    internal val Shapes.defaultIconButtonShapes: IconButtonShapes
        @Composable
        get() {
            return defaultIconButtonShapesCached
                ?: IconButtonShapes(
                        shape = smallRoundShape,
                        pressedShape = smallPressedShape,
                    )
                    .also { defaultIconButtonShapesCached = it }
        }

    /**
     * Creates a [IconToggleButtonShapes] that correspond to the shapes in the default, pressed, and
     * checked states. Icon button will morph between these shapes as long as the shapes are all
     * [CornerBasedShape]s.
     *
     * @param shape the active shape for [IconToggleButtonShapes]
     * @param pressedShape the pressed shape for [IconToggleButtonShapes]
     * @param checkedShape the checked shape for [IconToggleButtonShapes]
     */
    @ExperimentalMaterial3ExpressiveApi
    @Composable
    fun toggleableShapes(
        shape: Shape? = null,
        pressedShape: Shape? = null,
        checkedShape: Shape? = null
    ): IconToggleButtonShapes =
        MaterialTheme.shapes.defaultIconToggleButtonShapes.copy(
            shape = shape,
            pressedShape = pressedShape,
            checkedShape = checkedShape
        )

    /**
     * Creates a [ButtonShapes] that correspond to a default [IconToggleButton] in the active,
     * pressed and selected states. [IconToggleButton] will morph between these shapes as long as
     * the shapes are all [CornerBasedShape]s.
     */
    @ExperimentalMaterial3ExpressiveApi
    @Composable
    fun toggleableShapes(): IconToggleButtonShapes =
        MaterialTheme.shapes.defaultIconToggleButtonShapes

    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    internal val Shapes.defaultIconToggleButtonShapes: IconToggleButtonShapes
        @Composable
        get() {
            return defaultIconToggleButtonShapesCached
                ?: IconToggleButtonShapes(
                        shape = smallRoundShape,
                        pressedShape = smallPressedShape,
                        checkedShape = smallSelectedRoundShape
                    )
                    .also { defaultIconToggleButtonShapesCached = it }
        }

    @Suppress("OPT_IN_MARKER_ON_WRONG_TARGET")
    @get:ExperimentalMaterial3ExpressiveApi
    @ExperimentalMaterial3ExpressiveApi
    /** Default container for any extra small icon button. */
    val xSmallIconSize: Dp = XSmallIconButtonTokens.IconSize

    @Suppress("OPT_IN_MARKER_ON_WRONG_TARGET")
    @get:ExperimentalMaterial3ExpressiveApi
    @ExperimentalMaterial3ExpressiveApi
    /** Default size for any small icon button. */
    val smallIconSize: Dp = SmallIconButtonTokens.IconSize

    @Suppress("OPT_IN_MARKER_ON_WRONG_TARGET")
    @get:ExperimentalMaterial3ExpressiveApi
    @ExperimentalMaterial3ExpressiveApi
    /** Default container size for any medium icon button. */
    val mediumIconSize: Dp = MediumIconButtonTokens.IconSize

    @Suppress("OPT_IN_MARKER_ON_WRONG_TARGET")
    @get:ExperimentalMaterial3ExpressiveApi
    @ExperimentalMaterial3ExpressiveApi
    /** Default size for any large icon button. */
    val largeIconSize: Dp = LargeIconButtonTokens.IconSize

    /** Default size for any xlarge icon button. */
    @Suppress("OPT_IN_MARKER_ON_WRONG_TARGET")
    @get:ExperimentalMaterial3ExpressiveApi
    @ExperimentalMaterial3ExpressiveApi
    val xLargeIconSize: Dp = XLargeIconButtonTokens.IconSize

    /**
     * Default container size for any extra small icon button.
     *
     * @param widthOption the width of the container
     */
    @ExperimentalMaterial3ExpressiveApi
    fun xSmallContainerSize(
        widthOption: IconButtonWidthOption = IconButtonWidthOption.Uniform
    ): DpSize {
        val horizontalSpace =
            when (widthOption) {
                IconButtonWidthOption.Narrow ->
                    XSmallIconButtonTokens.NarrowLeadingSpace +
                        XSmallIconButtonTokens.NarrowTrailingSpace
                IconButtonWidthOption.Uniform ->
                    XSmallIconButtonTokens.UniformLeadingSpace +
                        XSmallIconButtonTokens.UniformLeadingSpace
                IconButtonWidthOption.Wide ->
                    XSmallIconButtonTokens.WideLeadingSpace +
                        XSmallIconButtonTokens.WideTrailingSpace
                else -> 0.dp
            }
        return DpSize(
            XSmallIconButtonTokens.IconSize + horizontalSpace,
            XSmallIconButtonTokens.ContainerHeight
        )
    }

    /**
     * Default container size for any small icon button.
     *
     * @param widthOption the width of the container
     */
    @ExperimentalMaterial3ExpressiveApi
    fun smallContainerSize(
        widthOption: IconButtonWidthOption = IconButtonWidthOption.Uniform
    ): DpSize {
        val horizontalSpace =
            when (widthOption) {
                IconButtonWidthOption.Narrow ->
                    SmallIconButtonTokens.NarrowLeadingSpace +
                        SmallIconButtonTokens.NarrowTrailingSpace
                IconButtonWidthOption.Uniform ->
                    SmallIconButtonTokens.UniformLeadingSpace +
                        SmallIconButtonTokens.UniformLeadingSpace
                IconButtonWidthOption.Wide ->
                    SmallIconButtonTokens.WideLeadingSpace + SmallIconButtonTokens.WideTrailingSpace
                else -> 0.dp
            }
        return DpSize(
            SmallIconButtonTokens.IconSize + horizontalSpace,
            SmallIconButtonTokens.ContainerHeight
        )
    }

    /**
     * Default container size for any medium icon button.
     *
     * @param widthOption the width of the container
     */
    @ExperimentalMaterial3ExpressiveApi
    fun mediumContainerSize(
        widthOption: IconButtonWidthOption = IconButtonWidthOption.Uniform
    ): DpSize {
        val horizontalSpace =
            when (widthOption) {
                IconButtonWidthOption.Narrow ->
                    MediumIconButtonTokens.NarrowLeadingSpace +
                        MediumIconButtonTokens.NarrowTrailingSpace
                IconButtonWidthOption.Uniform ->
                    MediumIconButtonTokens.UniformLeadingSpace +
                        MediumIconButtonTokens.UniformLeadingSpace
                IconButtonWidthOption.Wide ->
                    MediumIconButtonTokens.WideLeadingSpace +
                        MediumIconButtonTokens.WideTrailingSpace
                else -> 0.dp
            }
        return DpSize(
            MediumIconButtonTokens.IconSize + horizontalSpace,
            MediumIconButtonTokens.ContainerHeight
        )
    }

    /**
     * Default container size for any large icon button.
     *
     * @param widthOption the width of the container
     */
    @ExperimentalMaterial3ExpressiveApi
    fun largeContainerSize(
        widthOption: IconButtonWidthOption = IconButtonWidthOption.Uniform
    ): DpSize {
        val horizontalSpace =
            when (widthOption) {
                IconButtonWidthOption.Narrow ->
                    LargeIconButtonTokens.NarrowLeadingSpace +
                        LargeIconButtonTokens.NarrowTrailingSpace
                IconButtonWidthOption.Uniform ->
                    LargeIconButtonTokens.UniformLeadingSpace +
                        LargeIconButtonTokens.UniformLeadingSpace
                IconButtonWidthOption.Wide ->
                    LargeIconButtonTokens.WideLeadingSpace + LargeIconButtonTokens.WideTrailingSpace
                else -> 0.dp
            }
        return DpSize(
            LargeIconButtonTokens.IconSize + horizontalSpace,
            LargeIconButtonTokens.ContainerHeight
        )
    }

    /**
     * Default container size for any extra large icon button.
     *
     * @param widthOption the width of the container
     */
    @ExperimentalMaterial3ExpressiveApi
    fun xLargeContainerSize(
        widthOption: IconButtonWidthOption = IconButtonWidthOption.Uniform
    ): DpSize {
        val horizontalSpace =
            when (widthOption) {
                IconButtonWidthOption.Narrow ->
                    XLargeIconButtonTokens.NarrowLeadingSpace +
                        XLargeIconButtonTokens.NarrowTrailingSpace
                IconButtonWidthOption.Uniform ->
                    XLargeIconButtonTokens.UniformLeadingSpace +
                        XLargeIconButtonTokens.UniformLeadingSpace
                IconButtonWidthOption.Wide ->
                    XLargeIconButtonTokens.WideLeadingSpace +
                        XLargeIconButtonTokens.WideTrailingSpace
                else -> 0.dp
            }
        return DpSize(
            XLargeIconButtonTokens.IconSize + horizontalSpace,
            XLargeIconButtonTokens.ContainerHeight
        )
    }

    /** Class that describes the different supported widths of the [IconButton]. */
    @JvmInline
    value class IconButtonWidthOption private constructor(private val value: Int) {
        companion object {
            // TODO(b/342666275): update this kdoc with spec guidance
            /*
             * This configuration is recommended for small screens.
             */
            val Narrow = IconButtonWidthOption(0)

            /*
             * This configuration is recommended for medium width screens.
             */
            val Uniform = IconButtonWidthOption(1)

            /*
             * This configuration is recommended for wide screens.
             */
            val Wide = IconButtonWidthOption(2)
        }

        override fun toString() =
            when (this) {
                Narrow -> "Narrow"
                Uniform -> "Uniform"
                Wide -> "Wide"
                else -> "Unknown"
            }
    }
}
