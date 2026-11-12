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

package androidx.compose.remote.integration.view.demos.dsl

import android.graphics.Color
import androidx.compose.remote.core.RcProfiles
import androidx.compose.remote.core.operations.Utils
import androidx.compose.remote.core.operations.layout.managers.BoxLayout
import androidx.compose.remote.core.operations.layout.managers.ColumnLayout
import androidx.compose.remote.core.operations.layout.managers.RowLayout
import androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression.ADD
import androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression.SUB
import androidx.compose.remote.creation.RemoteComposeContextAndroid
import androidx.compose.remote.creation.RemoteComposeWriter
import androidx.compose.remote.creation.actions.ValueFloatExpressionChange
import androidx.compose.remote.creation.modifiers.RoundedRectShape
import androidx.compose.remote.creation.platform.AndroidxRcPlatformServices

private const val ITEM_COUNT = 20
private const val ITEM_WIDTH = 180
private const val SCROLL_STEP = 3f * ITEM_WIDTH

/**
 * Horizontal twin of [demoControlledScroll]: a row of 20 numbers with two buttons below it that
 * scroll the row left and right through actions on the scroll position variable (`position ± 3
 * items`), without touching the row; the row stays draggable by finger as well.
 */
@Suppress("RestrictedApiAndroidX")
fun demoControlledHorizontalScroll(): RemoteComposeWriter {
    return RemoteComposeContextAndroid(
            width = 1000,
            height = 1400,
            contentDescription = "Controlled horizontal scroll demo",
            apiLevel = 7,
            profiles = RcProfiles.PROFILE_ANDROIDX or RcProfiles.PROFILE_EXPERIMENTAL,
            platform = AndroidxRcPlatformServices(),
        ) {
            val position = addNamedFloat("scrollPosition", 0f)
            val positionId = Utils.idFromNan(position)
            val scrollLeft = Utils.idFromNan(floatExpression(position, SCROLL_STEP, SUB))
            val scrollRight = Utils.idFromNan(floatExpression(position, SCROLL_STEP, ADD))

            root {
                column(
                    Modifier.fillMaxSize().background(0xFF0F172A.toInt()).padding(24),
                    horizontal = ColumnLayout.CENTER,
                    vertical = ColumnLayout.TOP,
                ) {
                    // The scrolling row: 20 numbered columns
                    row(
                        Modifier.fillMaxWidth()
                            .verticalWeight(1f)
                            .clip(RoundedRectShape(20f, 20f, 20f, 20f))
                            .background(0xFF1E293B.toInt())
                            .horizontalScroll(position),
                        horizontal = RowLayout.START,
                        vertical = RowLayout.CENTER,
                    ) {
                        for (i in 1..ITEM_COUNT) {
                            box(
                                Modifier.width(ITEM_WIDTH)
                                    .fillMaxHeight()
                                    .background(
                                        if (i % 2 == 0) 0xFF1E3A5F.toInt() else 0xFF172554.toInt()
                                    ),
                                horizontal = BoxLayout.CENTER,
                                vertical = BoxLayout.CENTER,
                            ) {
                                text(
                                    "$i",
                                    fontSize = 56f,
                                    fontWeight = 700f,
                                    color = Color.WHITE,
                                )
                            }
                        }
                    }

                    // Left / Right buttons
                    row(
                        Modifier.fillMaxWidth().height(120).padding(0, 24, 0, 0),
                        horizontal = RowLayout.SPACE_BETWEEN,
                        vertical = RowLayout.CENTER,
                    ) {
                        val buttons =
                            listOf(
                                "\u25C0 Left" to ValueFloatExpressionChange(positionId, scrollLeft),
                                "\u25B6 Right" to
                                    ValueFloatExpressionChange(positionId, scrollRight),
                            )
                        for ((label, action) in buttons) {
                            box(
                                Modifier.horizontalWeight(1f)
                                    .fillMaxHeight()
                                    .padding(12, 0, 12, 0)
                                    .clip(RoundedRectShape(20f, 20f, 20f, 20f))
                                    .background(0xFF334155.toInt())
                                    .onClick(action),
                                horizontal = BoxLayout.CENTER,
                                vertical = BoxLayout.CENTER,
                            ) {
                                text(label, fontSize = 40f, fontWeight = 700f, color = Color.WHITE)
                            }
                        }
                    }
                }
            }
        }
        .writer
}
