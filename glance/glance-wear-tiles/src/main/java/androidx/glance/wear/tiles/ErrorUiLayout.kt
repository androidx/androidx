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

package androidx.glance.wear.tiles

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

@Suppress("deprecation") // For backwards compatibility.
internal fun errorUiLayout(): androidx.wear.tiles.LayoutElementBuilders.LayoutElement =
    androidx.wear.tiles.LayoutElementBuilders.Box.Builder()
        .setWidth(androidx.wear.tiles.DimensionBuilders.expand())
        .setHeight(androidx.wear.tiles.DimensionBuilders.expand())
        .setHorizontalAlignment(androidx.wear.tiles.LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
        .setVerticalAlignment(androidx.wear.tiles.LayoutElementBuilders.VERTICAL_ALIGN_CENTER)
        .addContent(
            androidx.wear.tiles.LayoutElementBuilders.Column.Builder()
                .setWidth(androidx.wear.tiles.DimensionBuilders.expand())
                .setHorizontalAlignment(
                    androidx.wear.tiles.LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
                .setModifiers(
                    androidx.wear.tiles.ModifiersBuilders.Modifiers.Builder()
                        .setBackground(
                            androidx.wear.tiles.ModifiersBuilders.Background.Builder()
                                .setColor(
                                    androidx.wear.tiles.ColorBuilders.argb(Color.DarkGray.toArgb()))
                                .build()
                        )
                        .build()
                )
                .addContent(
                    androidx.wear.tiles.LayoutElementBuilders.Text.Builder()
                        .setText("Glance Wear Tile Error")
                        .setFontStyle(
                            androidx.wear.tiles.LayoutElementBuilders.FontStyle.Builder()
                                .setSize(androidx.wear.tiles.DimensionBuilders.sp(18.toFloat()))
                                .setWeight(
                                    androidx.wear.tiles.LayoutElementBuilders.FONT_WEIGHT_BOLD)
                                .build()
                        )
                        .build()
                )
                .addContent(
                    androidx.wear.tiles.LayoutElementBuilders.Text.Builder()
                        .setText(
                            "Check the exact error using \"adb logcat\"," +
                                " searching for $GlanceWearTileTag"
                        )
                        .setMaxLines(6)
                        .setMultilineAlignment(
                            androidx.wear.tiles.LayoutElementBuilders.TEXT_ALIGN_CENTER)
                        .setFontStyle(
                            androidx.wear.tiles.LayoutElementBuilders.FontStyle.Builder()
                                .setSize(androidx.wear.tiles.DimensionBuilders.sp(14.toFloat()))
                                .build()
                        )
                        .build()
                )
                .build()
        )
        .build()
