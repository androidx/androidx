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

import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.operations.Header
import androidx.compose.remote.creation.RemoteComposeWriter.HTag
import androidx.compose.remote.creation.dsl.Modifier
import androidx.compose.remote.creation.dsl.RcColorValue
import androidx.compose.remote.creation.dsl.RcHorizontalPositioning
import androidx.compose.remote.creation.dsl.RcProfile
import androidx.compose.remote.creation.dsl.RcStrokeCap
import androidx.compose.remote.creation.dsl.RcTextAlign
import androidx.compose.remote.creation.dsl.RcVerticalPositioning
import androidx.compose.remote.creation.dsl.background
import androidx.compose.remote.creation.dsl.createRcBuffer
import androidx.compose.remote.creation.dsl.fillMaxSize
import androidx.compose.remote.creation.dsl.onClick
import androidx.compose.remote.creation.dsl.padding
import androidx.compose.remote.creation.dsl.rsp
import androidx.compose.remote.creation.dsl.setStrokeCap
import androidx.compose.remote.creation.dsl.size
import androidx.compose.remote.creation.profile.RcPlatformProfiles
import androidx.compose.remote.player.core.RemoteDocument
import androidx.compose.remote.tooling.preview.RemoteDocumentPreview
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import kotlin.math.PI

@Suppress("RestrictedApiAndroidX")
@Composable
@Preview
fun RcDslDemoPreview() {
    RemoteDocumentPreview(RemoteDocument(dslDemo()))
}

/** A simple demo using the new RemoteCompose DSL. */
@Suppress("RestrictedApiAndroidX")
fun dslDemo(): ByteArray {
    return createRcBuffer(
        RcProfile(RcPlatformProfiles.ANDROIDX),
        HTag(Header.DOC_DENSITY_BEHAVIOR, CoreDocument.DENSITY_BEHAVIOR_DP),
    ) {
        val count = remoteNamedFloat("count", 0f)
        val textVar = remoteNamedText("message", "Click me!")

        Column(
            modifier = Modifier.fillMaxSize().background(0xFFEEEEEE).padding(20f),
            horizontal = RcHorizontalPositioning.Center,
        ) {
            Text(
                "Hello from New DSL!",
                fontSize = 24.rsp,
                color = 0xFF333333.toInt(),
                textAlign = RcTextAlign.Center,
            )

            Box(
                modifier =
                    Modifier.size(200f, 100f).background(0xFFFF0000).padding(10f).onClick {
                        setValue(count, 1f)
                        setValue(textVar, "Button Clicked!")
                    }
            ) {
                Text("Click Box", color = 0xFFFFFFFF, textAlign = RcTextAlign.Center)
            }

            Text(textVar, fontSize = 36.rsp)
            Row(
                modifier = Modifier.size(300f, 100f).background(0xFF00FF00),
                vertical = RcVerticalPositioning.Center,
            ) {
                Text("Row Item 1")
                Text("Row Item 2", modifier = Modifier.padding(start = 20f))
            }

            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = width
                val h = height
                paint.setColor(0xFFFF0000.toInt()).commit()
                drawLine(0.rf, 0.rf, w, h)
                drawLine(0.rf, h, w, 0.rf)
                drawCircle(w / 2f, h / 2f, (w / 4f))
            }
        }
    }
}

@Suppress("RestrictedApiAndroidX")
@Composable
@Preview
fun RcDslTheme1Preview() {
    RemoteDocumentPreview(RemoteDocument(dslTheme1()))
}

@Suppress("RestrictedApiAndroidX")
@Composable
@Preview
fun RcDslTheme2Preview() {
    RemoteDocumentPreview(RemoteDocument(dslTheme2()))
}

@Suppress("RestrictedApiAndroidX")
fun dslTheme1(): ByteArray {
    return createRcBuffer(RcProfile(RcPlatformProfiles.ANDROIDX), experimental = true) {
        val color1 =
            remoteThemedColor(
                light = "color.system_neutral1_10",
                lightDefault = 0xFF00FF00.toInt(),
                dark = "color.system_neutral1_800",
                darkDefault = 0xFFFF0000.toInt(),
            )
        val backgroundColor =
            remoteThemedColor(
                light = "color.system_neutral1_900",
                lightDefault = 0xFFFFCCAA.toInt(),
                dark = "color.system_neutral1_0",
                darkDefault = 0xFF553322.toInt(),
            )

        Column(
            modifier = Modifier.background(backgroundColor).fillMaxSize(),
            horizontal = RcHorizontalPositioning.Center,
        ) {
            Box(modifier = Modifier.background(color1).size(200f, 200f).padding(0f, 0f, 0f, 12f))
            Text("hello", color = color1, fontSize = 100.rsp)
        }
    }
}

@Suppress("RestrictedApiAndroidX")
fun dslTheme2(): ByteArray {
    return createRcBuffer(RcProfile(RcPlatformProfiles.ANDROIDX), experimental = true) {
        val color1 =
            remoteThemedColor(
                light = "color.system_neutral1_10",
                lightDefault = 0xFF00FF00.toInt(),
                dark = "color.system_neutral1_800",
                darkDefault = 0xFFFF0000.toInt(),
            )
        val backgroundColor =
            remoteThemedColor(
                light = "color.system_neutral1_900",
                lightDefault = 0xFFFFCCAA.toInt(),
                dark = "color.system_neutral1_0",
                darkDefault = 0xFF553322.toInt(),
            )

        Column(
            modifier = Modifier.background(backgroundColor).fillMaxSize(),
            horizontal = RcHorizontalPositioning.Center,
        ) {
            Box(modifier = Modifier.background(color1).size(200f, 200f).padding(0f, 0f, 0f, 12f))
            Text("hello", color = color1, fontSize = 100.rsp)
        }
    }
}

@Suppress("RestrictedApiAndroidX")
@Composable
@Preview
fun RcDslSimpleDemoPreview() {
    RemoteDocumentPreview(RemoteDocument(dslSimpleDemo()))
}

@Suppress("RestrictedApiAndroidX")
@Composable
@Preview
fun RcDslSimpleClockPreview() {
    RemoteDocumentPreview(RemoteDocument(dslSimpleClock()))
}

@Suppress("RestrictedApiAndroidX")
fun dslSimpleDemo(): ByteArray {
    return createRcBuffer(RcProfile(RcPlatformProfiles.ANDROIDX)) {
        Box(modifier = Modifier.fillMaxSize()) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = componentWidth()
                val h = componentHeight()
                val cx = w / 2f
                val cy = h / 2f
                val rad = min(cx, cy)

                applyPaint { setColor(RcColorValue(0xFF000000.toInt())) }
                drawRoundRect(0.rf, 0.rf, w, h, rad, rad)

                applyPaint {
                    setColor(RcColorValue(0xFFFFFFFF.toInt()))
                    setStrokeWidth(8f.rf + 3f)
                }

                val angle = seconds() * (2 * PI.toFloat() / 60f)

                drawLine(cx, cy, cx + rad * sin(angle), cy - rad * cos(angle))
            }
        }
    }
}

@Suppress("RestrictedApiAndroidX")
fun dslSimpleClock(): ByteArray {
    return createRcBuffer(RcProfile(RcPlatformProfiles.ANDROIDX)) {
        Box(modifier = Modifier.fillMaxSize()) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = componentWidth()
                val h = componentHeight()
                val cx = w / 2f
                val cy = h / 2f
                val rad = min(cx, cy)

                applyPaint {
                    setColor(RcColorValue(0xFF0000FF.toInt())) // Blue
                }
                drawRoundRect(0.rf, 0.rf, w, h, rad / 4f, rad / 4f)

                applyPaint {
                    setColor(RcColorValue(0xFF888888.toInt())) // Gray
                    setStrokeWidth(32f)
                    setStrokeCap(RcStrokeCap.Round)
                }

                save {
                    rotate(minutes() * 6f, cx, cy)
                    drawLine(cx, cy, cx, cy - rad * 0.8f)
                }
                save {
                    rotate(hour() * 30f, cx, cy)
                    drawLine(cx, cy, cx, cy - rad / 2f)
                }

                applyPaint {
                    setColor(RcColorValue(0xFFFFFFFF.toInt())) // White
                    setStrokeWidth(4f)
                }
                drawLine(
                    cx,
                    cy,
                    w / 2f + rad * sin(seconds() * (2 * PI.toFloat() / 60f)),
                    h / 2f - rad * cos(seconds() * (2 * PI.toFloat() / 60f)),
                )
            }
        }
    }
}
