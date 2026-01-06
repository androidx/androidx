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

package androidx.compose.remote.integration.view.demos.examples

import android.graphics.Color
import android.graphics.fonts.Font
import android.graphics.fonts.SystemFonts
import androidx.compose.remote.core.RcProfiles
import androidx.compose.remote.core.operations.layout.managers.BoxLayout
import androidx.compose.remote.core.operations.layout.managers.ColumnLayout
import androidx.compose.remote.core.operations.layout.managers.CoreText
import androidx.compose.remote.creation.RFloat
import androidx.compose.remote.creation.RemoteComposeContext
import androidx.compose.remote.creation.RemoteComposeContextAndroid
import androidx.compose.remote.creation.computePosition
import androidx.compose.remote.creation.minus
import androidx.compose.remote.creation.modifiers.RectShape
import androidx.compose.remote.creation.platform.AndroidxRcPlatformServices

@Suppress("RestrictedApiAndroidX")
fun RcCanvasComponents1(): RemoteComposeContext {
    return RemoteComposeContextAndroid(
        600,
        600,
        "Demo",
        8,
        RcProfiles.PROFILE_ANDROIDX or RcProfiles.PROFILE_EXPERIMENTAL,
        AndroidxRcPlatformServices(),
    ) {
        root {
            column(Modifier.fillMaxSize().background(Color.YELLOW).padding(16)) {
                canvas(Modifier.fillMaxSize().background(Color.BLUE)) {
                    val w = ComponentWidth()
                    val h = ComponentHeight()
                    painter.setColor(Color.RED).setStrokeWidth(4f).commit()
                    drawLine(0f, 0f, w, h)
                    drawLine(0f, h, w, 0f)
                    box(
                        Modifier.background(Color.CYAN).size(300, 200).computePosition {
                            x = w / 2f - RFloat(width.toFloat()) / 2f
                            y = h / 2f - RFloat(height.toFloat()) / 2f
                        }
                    ) {
                        text(
                            "Hello, World!",
                            autosize = true,
                            textAlign = CoreText.TEXT_ALIGN_CENTER,
                        )
                    }
                }
            }
        }
    }
}

@Suppress("RestrictedApiAndroidX")
fun RcCanvasComponents2(): RemoteComposeContext {
    return RemoteComposeContextAndroid(
        600,
        600,
        "Demo",
        8,
        RcProfiles.PROFILE_ANDROIDX or RcProfiles.PROFILE_EXPERIMENTAL,
        AndroidxRcPlatformServices(),
    ) {
        val position = RFloat(writer, 0f)
        root {
            column(
                Modifier.fillMaxSize()
                    .background(Color.YELLOW)
                    .padding(32)
                    .verticalScroll(position.toFloat())
            ) {
                canvas(Modifier.fillMaxWidth().height(2000).background(Color.BLUE)) {
                    val w = ComponentWidth()
                    val h = ComponentHeight()
                    painter.setColor(Color.RED).setStrokeWidth(4f).commit()
                    drawLine(0f, 0f, w, h)
                    drawLine(0f, h, w, 0f)
                    box(
                        Modifier.background(Color.CYAN).size(300, 200).computePosition {
                            x = w / 2f - RFloat(width.toFloat()) / 2f
                            y = h / 2f - RFloat(height.toFloat()) / 2f
                        }
                    ) {
                        text(
                            "Hello, World!",
                            autosize = true,
                            textAlign = CoreText.TEXT_ALIGN_CENTER,
                        )
                    }
                    val textId = writer.createTextFromFloat(position.toFloat(), 2, 1, 0)
                    text(
                        textId,
                        Modifier.padding(16).computePosition { y = position },
                        color = Color.WHITE,
                        fontSize = 64f,
                    )
                }
            }
        }
    }
}

fun getFonts(): Array<Font> {
    val fList = SystemFonts.getAvailableFonts().toTypedArray()
    fList.forEach { font -> println(font.file?.name) }
    return fList
}

@Suppress("RestrictedApiAndroidX")
fun RcCanvasComponents3(): RemoteComposeContext {
    return RemoteComposeContextAndroid(
        600,
        600,
        "Demo",
        8,
        RcProfiles.PROFILE_ANDROIDX or RcProfiles.PROFILE_EXPERIMENTAL,
        AndroidxRcPlatformServices(),
    ) {
        root {
            column {
                text(
                    "Hello World",
                    fontSize = 64f,
                    modifier = Modifier.background(Color.BLUE),
                    color = Color.WHITE,
                )
                column(
                    Modifier.fillMaxSize()
                        .background(Color.RED)
                        .padding(32)
                        .background(Color.YELLOW)
                        .verticalScroll()
                ) {
                    val fonts = getFonts()
                    val maxFonts = 20
                    var current = 0
                    for (font in fonts) {
                        text(
                            "${font.file?.name}",
                            fontSize = 64f,
                            fontFamily = "${font.file?.name}",
                            modifier = Modifier.fillMaxWidth(),
                        )
                        current++
                        if (current > maxFonts) {
                            break
                        }
                    }
                }
            }
        }
    }
}

@Suppress("RestrictedApiAndroidX")
fun RcCanvasComponents4(): RemoteComposeContext {
    return RemoteComposeContextAndroid(
        600,
        600,
        "Demo",
        8,
        RcProfiles.PROFILE_ANDROIDX or RcProfiles.PROFILE_EXPERIMENTAL,
        AndroidxRcPlatformServices(),
    ) {
        root {
            column {
                text(
                    "Hello World",
                    fontSize = 64f,
                    modifier = Modifier.background(Color.BLUE),
                    color = Color.WHITE,
                )
                row(
                    Modifier.fillMaxSize()
                        .background(Color.RED)
                        .padding(32)
                        .background(Color.YELLOW)
                        .horizontalScroll()
                ) {
                    for (i in 0..100) {
                        text(" $i ", fontSize = 64f)
                    }
                }
            }
        }
    }
}

@Suppress("RestrictedApiAndroidX")
fun RcCanvasComponents5(): RemoteComposeContext {
    return RemoteComposeContextAndroid(
        600,
        600,
        "Demo",
        8,
        RcProfiles.PROFILE_ANDROIDX or RcProfiles.PROFILE_EXPERIMENTAL,
        AndroidxRcPlatformServices(),
    ) {
        val scrollPosition = RFloat(writer, 0f)
        root {
            column {
                text(
                    "Hello World",
                    fontSize = 64f,
                    modifier = Modifier.background(Color.BLUE),
                    color = Color.WHITE,
                )
                row {
                    column(
                        Modifier.horizontalWeight(1f)
                            .background(Color.RED)
                            .padding(32)
                            .fillMaxHeight()
                            .background(Color.YELLOW)
                            .verticalScroll(),
                        horizontal = ColumnLayout.CENTER,
                    ) {
                        for (i in 0..100) {
                            text(" $i ", fontSize = 64f, textAlign = CoreText.TEXT_ALIGN_CENTER)
                        }
                    }
                    column(
                        Modifier.horizontalWeight(1f)
                            .background(Color.RED)
                            .padding(32)
                            .fillMaxHeight()
                            .background(Color.YELLOW)
                            .verticalScroll(scrollPosition.toFloat()),
                        horizontal = ColumnLayout.CENTER,
                    ) {
                        for (i in 0..100) {
                            box(
                                Modifier.size(200, 60)
                                    .background(Color.RED)
                                    .border(2f, 0f, Color.BLUE, 0)
                                    .clip(RectShape(8f, 8f, 8f, 8f)),
                                horizontal = BoxLayout.CENTER,
                                vertical = BoxLayout.CENTER,
                            ) {
                                text(" $i ", fontSize = 64f)
                            }
                        }
                    }
                }
            }
        }
    }
}
