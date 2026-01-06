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
import androidx.compose.remote.core.RcProfiles
import androidx.compose.remote.core.operations.layout.managers.BoxLayout
import androidx.compose.remote.core.operations.layout.managers.ColumnLayout
import androidx.compose.remote.core.operations.layout.managers.CoreText
import androidx.compose.remote.core.operations.layout.managers.RowLayout
import androidx.compose.remote.creation.RemoteComposeContext
import androidx.compose.remote.creation.RemoteComposeContextAndroid
import androidx.compose.remote.creation.modifiers.RecordingModifier
import androidx.compose.remote.creation.platform.AndroidxRcPlatformServices
import androidx.compose.remote.creation.sin
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

@Suppress("RestrictedApiAndroidX")
fun RcTextDemo8(): RemoteComposeContext {
    return RemoteComposeContextAndroid(
        600,
        600,
        "Demo",
        7,
        RcProfiles.PROFILE_ANDROIDX or RcProfiles.PROFILE_EXPERIMENTAL,
        AndroidxRcPlatformServices(),
    ) {
        root {
            row(
                Modifier.background(Color.GREEN).padding(8).fillMaxWidth(),
                vertical = RowLayout.CENTER,
            ) {
                column(Modifier.horizontalWeight(1f).background(Color.YELLOW)) {
                    text("New Arsenal Game", maxLines = 1, overflow = CoreText.OVERFLOW_ELLIPSIS)
                    text(
                        "Arsenal vs Bayern Munich",
                        fontSize = 64f,
                        maxLines = 3,
                        overflow = CoreText.OVERFLOW_ELLIPSIS,
                    )
                    text(
                        "UEFA Champions League Group Stage",
                        maxLines = 2,
                        overflow = CoreText.OVERFLOW_ELLIPSIS,
                    )
                    text(
                        "Wednesday 26th November",
                        maxLines = 1,
                        overflow = CoreText.OVERFLOW_ELLIPSIS,
                    )
                }
                column(
                    Modifier.size(130).background(Color.CYAN).padding(8),
                    ColumnLayout.CENTER,
                    ColumnLayout.CENTER,
                ) {
                    box(
                        Modifier.size(100).background(Color.YELLOW),
                        BoxLayout.CENTER,
                        BoxLayout.CENTER,
                    ) {
                        text("IMG")
                    }
                }
            }
        }
    }
}

@Suppress("RestrictedApiAndroidX")
fun RcTextDemo7(): RemoteComposeContext {
    return RemoteComposeContextAndroid(
        600,
        600,
        "Demo",
        7,
        RcProfiles.PROFILE_ANDROIDX or RcProfiles.PROFILE_EXPERIMENTAL,
        AndroidxRcPlatformServices(),
    ) {
        root {
            column(Modifier.background(Color.YELLOW).fillMaxSize()) {
                val content = "The quick brown Fox "
                val tween = (sin(ContinuousSec() % 3600f) + 1f) * 500f
                text(
                    content,
                    RecordingModifier().background(Color.LTGRAY),
                    fontWeight = tween.toFloat(),
                )
                text(
                    content + ": default",
                    RecordingModifier().background(Color.LTGRAY),
                    fontFamily = "default",
                )
                text(
                    content + ": sans-serif",
                    RecordingModifier().background(Color.LTGRAY),
                    fontFamily = "sans-serif",
                )
                text(
                    content + ": serif",
                    RecordingModifier().background(Color.LTGRAY),
                    fontFamily = "serif",
                )
                text(
                    content + ": monospace",
                    RecordingModifier().background(Color.LTGRAY),
                    fontFamily = "monospace",
                )
                text(
                    content + ": default",
                    RecordingModifier().background(Color.LTGRAY),
                    fontFamily = "default",
                    fontStyle = 1,
                )
                text(
                    content + ": sans-serif",
                    RecordingModifier().background(Color.LTGRAY),
                    fontFamily = "sans-serif",
                    fontStyle = 1,
                )
                text(
                    content + ": serif",
                    RecordingModifier().background(Color.LTGRAY),
                    fontFamily = "serif",
                    fontStyle = 1,
                )
                text(
                    content + ": monospace",
                    RecordingModifier().background(Color.LTGRAY),
                    fontFamily = "monospace",
                    fontStyle = 1,
                )
                text(
                    content + ": default",
                    RecordingModifier().background(Color.LTGRAY),
                    fontFamily = "default",
                    underline = true,
                )
                text(
                    content + ": default",
                    RecordingModifier().background(Color.LTGRAY),
                    fontFamily = "default",
                    strikethrough = true,
                )
                text(
                    content + ": default",
                    RecordingModifier().background(Color.LTGRAY),
                    fontFamily = "default",
                    fontWeight = 800f,
                    fontStyle = 1,
                )
                text(
                    content + ": sans-serif",
                    RecordingModifier().background(Color.LTGRAY),
                    fontFamily = "sans-serif",
                    fontWeight = 800f,
                    fontStyle = 1,
                )
                text(
                    content + ": serif",
                    RecordingModifier().background(Color.LTGRAY),
                    fontFamily = "serif",
                    fontWeight = 800f,
                    fontStyle = 1,
                )
                text(
                    content + ": monospace",
                    RecordingModifier().background(Color.LTGRAY),
                    fontFamily = "monospace",
                    fontWeight = 800f,
                    fontStyle = 1,
                )
            }
        }
    }
}

@Suppress("RestrictedApiAndroidX")
fun RcTextDemo6(): RemoteComposeContext {
    return RemoteComposeContextAndroid(
        600,
        600,
        "Demo",
        7,
        RcProfiles.PROFILE_ANDROIDX or RcProfiles.PROFILE_EXPERIMENTAL,
        AndroidxRcPlatformServices(),
    ) {
        root {
            column(Modifier.background(Color.YELLOW).fillMaxSize()) {
                val content = "The quick brown Fox"
                val tween = (sin(ContinuousSec() % 3600f) + 1f) * 100f + 16f
                text(
                    content,
                    RecordingModifier().background(Color.LTGRAY),
                    fontSize = tween.toFloat(),
                    fontAxis = listOf("wght" to 1000f),
                )
            }
        }
    }
}

@Suppress("RestrictedApiAndroidX")
fun RcTextDemo5(): RemoteComposeContext {
    return RemoteComposeContextAndroid(
        600,
        600,
        "Demo",
        7,
        RcProfiles.PROFILE_ANDROIDX or RcProfiles.PROFILE_EXPERIMENTAL,
        AndroidxRcPlatformServices(),
    ) {
        val tween = (sin(ContinuousSec()) + 1f / 2f).toFloat()
        root {
            column(Modifier.background(Color.YELLOW).fillMaxSize()) {
                val fontName = "DancingScript-Regular"
                val content2 = "The quick brown fox jumps over the lazy dog"
                val c1 = 0xFFFF0000.toInt()
                val c2 = 0xFF0000FF.toInt()
                val color = addColorExpression(c1, c2, tween)
                text(
                    content2,
                    RecordingModifier().background(Color.LTGRAY).fillMaxWidth(),
                    fontFamily = fontName,
                    colorId = color.toInt(),
                    fontSize = 80f,
                    overflow = CoreText.OVERFLOW_ELLIPSIS,
                    maxLines = 1,
                )
                text(
                    content2,
                    RecordingModifier().background(Color.LTGRAY).fillMaxWidth(),
                    fontFamily = fontName,
                    fontSize = 80f,
                    overflow = CoreText.OVERFLOW_MIDDLE_ELLIPSIS,
                    maxLines = 1,
                )
                text(
                    content2,
                    RecordingModifier().background(Color.LTGRAY).fillMaxWidth(),
                    fontFamily = fontName,
                    fontSize = 80f,
                    overflow = CoreText.OVERFLOW_START_ELLIPSIS,
                    maxLines = 1,
                )
                val textId = createTextFromFloat(tween, 3, 3, 0)
                text(
                    textId,
                    RecordingModifier().background(Color.LTGRAY),
                    fontFamily = fontName,
                    fontSize = 0f,
                )
            }
        }
    }
}

@Suppress("RestrictedApiAndroidX")
fun RcTextDemo4(): RemoteComposeContext {
    return RemoteComposeContextAndroid(
        600,
        600,
        "Demo",
        7,
        RcProfiles.PROFILE_ANDROIDX or RcProfiles.PROFILE_EXPERIMENTAL,
        AndroidxRcPlatformServices(),
    ) {
        root {
            column(Modifier.background(Color.YELLOW).fillMaxSize()) {
                val content = "The quick brown Fox"
                val fontName2 = "RobotoFlex-Regular"
                val fontName = "DancingScript-Regular"
                text(
                    content,
                    RecordingModifier().background(Color.LTGRAY),
                    fontFamily = fontName2,
                    fontSize = 80f,
                )
                text(content, RecordingModifier().background(Color.LTGRAY), fontSize = 80f)
                text(
                    content,
                    RecordingModifier().background(Color.LTGRAY),
                    fontFamily = fontName,
                    fontSize = 80f,
                    fontAxis = listOf("wght" to 10f),
                )
                text(
                    content,
                    RecordingModifier().background(Color.LTGRAY),
                    fontFamily = fontName,
                    fontSize = 80f,
                    fontAxis = listOf("wght" to 600f),
                )
                text(
                    content,
                    RecordingModifier().background(Color.LTGRAY),
                    fontFamily = fontName2,
                    fontSize = 80f,
                )
                text(
                    content,
                    RecordingModifier().background(Color.LTGRAY),
                    fontFamily = fontName,
                    fontSize = 80f,
                    fontAxis = listOf("wght" to 1000f),
                )
            }
        }
    }
}

@Suppress("RestrictedApiAndroidX")
fun RcTextDemo3(): RemoteComposeContext {
    return RemoteComposeContextAndroid(
        600,
        600,
        "Demo",
        7,
        RcProfiles.PROFILE_ANDROIDX or RcProfiles.PROFILE_EXPERIMENTAL,
        AndroidxRcPlatformServices(),
    ) {
        root {
            column {
                row {
                    val content =
                        "Lorem ipsum dolor sit amet, consectetur" +
                            " adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna" +
                            " aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris" +
                            " nisi ut aliquip ex ea commodo consequat."
                    column(Modifier.fillMaxSize().background(Color.YELLOW).horizontalWeight(1f)) {
                        text(content, autosize = false)
                    }
                    column(Modifier.fillMaxSize().background(Color.CYAN).horizontalWeight(1f)) {
                        text(
                            content,
                            underline = true,
                            letterSpacing = 0.1f,
                            lineHeightMultiplier = 1.2f,
                            autosize = true,
                        )
                    }
                }
            }
        }
    }
}

@Suppress("RestrictedApiAndroidX")
fun RcTextDemo3b(): RemoteComposeContext {
    return RemoteComposeContextAndroid(
        600,
        600,
        "Demo",
        7,
        RcProfiles.PROFILE_ANDROIDX or RcProfiles.PROFILE_EXPERIMENTAL,
        AndroidxRcPlatformServices(),
    ) {
        root {
            column(Modifier.padding(8)) {
                val content =
                    "Lorem ipsum dolor sit amet, consectetur" +
                        " adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna" +
                        " aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris" +
                        " nisi ut aliquip ex ea commodo consequat."
                row(Modifier.background(Color.BLACK).padding(8)) {
                    text("Left Alignment", Modifier.horizontalWeight(1f), color = Color.WHITE)
                    text("Basic Justification", Modifier.horizontalWeight(1f), color = Color.WHITE)
                }
                row(Modifier.padding(8)) {
                    column(Modifier.background(Color.YELLOW).horizontalWeight(1f)) { text(content) }
                    box(Modifier.size(16))
                    column(Modifier.background(Color.CYAN).horizontalWeight(1f)) {
                        text(content, textAlign = CoreText.TEXT_ALIGN_JUSTIFY)
                    }
                }
                row(Modifier.background(Color.BLACK).padding(8)) {
                    text("Center Alignment", Modifier.horizontalWeight(1f), color = Color.WHITE)
                    text(
                        "Line Break & Justification",
                        Modifier.horizontalWeight(1f),
                        color = Color.WHITE,
                    )
                }
                row(Modifier.padding(8)) {
                    column(Modifier.background(Color.YELLOW).horizontalWeight(1f)) {
                        text(content, textAlign = CoreText.TEXT_ALIGN_CENTER)
                    }
                    box(Modifier.size(16))
                    column(Modifier.background(Color.CYAN).horizontalWeight(1f)) {
                        text(
                            content,
                            textAlign = CoreText.TEXT_ALIGN_JUSTIFY,
                            lineBreakStrategy = CoreText.BREAK_STRATEGY_HIGH_QUALITY,
                            justificationMode = CoreText.JUSTIFICATION_MODE_INTER_CHARACTER,
                        )
                    }
                }
                row(Modifier.background(Color.BLACK).padding(8)) {
                    text("Right Alignment", Modifier.horizontalWeight(1f), color = Color.WHITE)
                    text(
                        "Line Break & Hyphenation",
                        Modifier.horizontalWeight(1f),
                        color = Color.WHITE,
                    )
                }
                row(Modifier.padding(8)) {
                    column(Modifier.background(Color.YELLOW).horizontalWeight(1f)) {
                        text(content, textAlign = CoreText.TEXT_ALIGN_RIGHT)
                    }
                    box(Modifier.size(16))
                    column(Modifier.background(Color.CYAN).horizontalWeight(1f)) {
                        text(
                            content,
                            textAlign = CoreText.TEXT_ALIGN_JUSTIFY,
                            hyphenationFrequency = CoreText.HYPHENATION_FREQUENCY_FULL,
                            lineBreakStrategy = CoreText.BREAK_STRATEGY_HIGH_QUALITY,
                            justificationMode = CoreText.JUSTIFICATION_MODE_INTER_CHARACTER,
                        )
                    }
                }
            }
        }
    }
}

@Suppress("RestrictedApiAndroidX")
fun RcTextDemo3c(): RemoteComposeContext {
    return RemoteComposeContextAndroid(
        600,
        600,
        "Demo",
        7,
        RcProfiles.PROFILE_ANDROIDX or RcProfiles.PROFILE_EXPERIMENTAL,
        AndroidxRcPlatformServices(),
    ) {
        root {
            column {
                row {
                    val content =
                        "Lorem ipsum dolor sit amet, consectetur" +
                            " adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna" +
                            " aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris" +
                            " nisi ut aliquip ex ea commodo consequat."
                    column(Modifier.fillMaxSize().background(Color.CYAN).horizontalWeight(1f)) {
                        text(content, maxLines = 5, overflow = CoreText.OVERFLOW_ELLIPSIS)
                    }
                }
            }
        }
    }
}

@Suppress("RestrictedApiAndroidX")
fun RcTextDemo2(): RemoteComposeContext {
    return RemoteComposeContextAndroid(
        600,
        600,
        "Demo",
        7,
        RcProfiles.PROFILE_ANDROIDX or RcProfiles.PROFILE_EXPERIMENTAL,
        AndroidxRcPlatformServices(),
    ) {
        root {
            column(Modifier.fillMaxSize().background(Color.YELLOW)) {
                text("Title with fixed size")
                text(
                    "Resizable Hello World!",
                    modifier = Modifier.verticalWeight(1f).fillMaxWidth().background(Color.GREEN),
                    autosize = true,
                    hyphenationFrequency = CoreText.HYPHENATION_FREQUENCY_NONE,
                )
            }
        }
    }
}

@Suppress("RestrictedApiAndroidX")
fun RcTextDemo(): RemoteComposeContext {
    return RemoteComposeContextAndroid(
        600,
        600,
        "Demo",
        7,
        RcProfiles.PROFILE_ANDROIDX or RcProfiles.PROFILE_EXPERIMENTAL,
        AndroidxRcPlatformServices(),
    ) {
        root {
            box(Modifier.fillMaxSize().background(Color.YELLOW)) {
                row(
                    Modifier.fillMaxSize(),
                    horizontal = RowLayout.SPACE_EVENLY,
                    vertical = RowLayout.TOP,
                ) {
                    text("Hello", modifier = Modifier.alignByBaseline())
                    text("World", modifier = Modifier.alignByBaseline(), fontSize = 100f)
                    text("the", modifier = Modifier.alignByBaseline(), fontSize = 12f)
                    text("quick", modifier = Modifier.alignByBaseline(), fontSize = 64f)
                    text("brown", modifier = Modifier.alignByBaseline(), fontSize = 72f)
                    text("fox", modifier = Modifier.alignByBaseline())
                }
                row(
                    Modifier.fillMaxSize(),
                    horizontal = RowLayout.SPACE_EVENLY,
                    vertical = RowLayout.CENTER,
                ) {
                    text("Hello", modifier = Modifier.alignByBaseline())
                    text("World", modifier = Modifier.alignByBaseline(), fontSize = 100f)
                    text("the", modifier = Modifier.alignByBaseline(), fontSize = 12f)
                    text("quick", modifier = Modifier.alignByBaseline(), fontSize = 64f)
                    text("brown", modifier = Modifier.alignByBaseline(), fontSize = 72f)
                    text("fox", modifier = Modifier.alignByBaseline())
                }
                row(
                    Modifier.fillMaxSize(),
                    horizontal = RowLayout.SPACE_EVENLY,
                    vertical = RowLayout.BOTTOM,
                ) {
                    text("Hello", modifier = Modifier.alignByBaseline())
                    text("World", modifier = Modifier.alignByBaseline(), fontSize = 100f)
                    text("the", modifier = Modifier.alignByBaseline(), fontSize = 12f)
                    text("quick", modifier = Modifier.alignByBaseline(), fontSize = 64f)
                    text("brown", modifier = Modifier.alignByBaseline(), fontSize = 72f)
                    text("fox", modifier = Modifier.alignByBaseline())
                }
            }
        }
    }
}

@Preview @Composable fun RcTextDemoPreview() = RemoteDocPreview(RcTextDemo())

@Preview @Composable fun RcTextDemo2Preview() = RemoteDocPreview(RcTextDemo2())

@Preview @Composable fun RcTextDemo3Preview() = RemoteDocPreview(RcTextDemo3())

@Preview(group = "alignment")
@Composable
fun RcTextDemo3bPreview() = RemoteDocPreview(RcTextDemo3b())

@Preview @Composable fun RcTextDemo4Preview() = RemoteDocPreview(RcTextDemo4())

@Preview @Composable fun RcTextDemo5Preview() = RemoteDocPreview(RcTextDemo5())

@Preview @Composable fun RcTextDemo6Preview() = RemoteDocPreview(RcTextDemo6())

@Preview @Composable fun RcTextDemo7Preview() = RemoteDocPreview(RcTextDemo7())

@Preview @Composable fun RcTextDemo8Preview() = RemoteDocPreview(RcTextDemo8())
