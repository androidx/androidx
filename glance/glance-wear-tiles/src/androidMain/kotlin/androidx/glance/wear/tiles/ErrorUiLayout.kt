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
import androidx.wear.tiles.ColorBuilders.argb
import androidx.wear.tiles.DimensionBuilders.expand
import androidx.wear.tiles.DimensionBuilders.sp
import androidx.wear.tiles.LayoutElementBuilders.LayoutElement
import androidx.wear.tiles.LayoutElementBuilders.Box
import androidx.wear.tiles.LayoutElementBuilders.Column
import androidx.wear.tiles.LayoutElementBuilders.FONT_WEIGHT_BOLD
import androidx.wear.tiles.LayoutElementBuilders.FontStyle
import androidx.wear.tiles.LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER
import androidx.wear.tiles.LayoutElementBuilders.VERTICAL_ALIGN_CENTER
import androidx.wear.tiles.LayoutElementBuilders.TEXT_ALIGN_CENTER
import androidx.wear.tiles.LayoutElementBuilders.Text
import androidx.wear.tiles.ModifiersBuilders.Background
import androidx.wear.tiles.ModifiersBuilders.Modifiers

internal fun errorUiLayout(): LayoutElement =
    Box.Builder()
        .setWidth(expand())
        .setHeight(expand())
        .setHorizontalAlignment(HORIZONTAL_ALIGN_CENTER)
        .setVerticalAlignment(VERTICAL_ALIGN_CENTER)
        .addContent(
            Column.Builder()
                .setWidth(expand())
                .setHorizontalAlignment(HORIZONTAL_ALIGN_CENTER)
                .setModifiers(
                    Modifiers.Builder()
                        .setBackground(
                            Background.Builder()
                                .setColor(argb(Color.DarkGray.toArgb()))
                                .build()
                        )
                        .build()
                )
                .addContent(
                    Text.Builder()
                        .setText("Glance Wear Tile Error")
                        .setFontStyle(
                            FontStyle.Builder()
                                .setSize(sp(18.toFloat()))
                                .setWeight(FONT_WEIGHT_BOLD)
                                .build()
                        )
                        .build()
                )
                .addContent(
                    Text.Builder()
                        .setText(
                            "Check the exact error using \"adb logcat\"," +
                                " searching for $GlanceWearTileTag"
                        )
                        .setMaxLines(6)
                        .setMultilineAlignment(TEXT_ALIGN_CENTER)
                        .setFontStyle(
                            FontStyle.Builder()
                                .setSize(sp(14.toFloat()))
                                .build()
                        )
                        .build()
                )
                .build()
        )
        .build()
