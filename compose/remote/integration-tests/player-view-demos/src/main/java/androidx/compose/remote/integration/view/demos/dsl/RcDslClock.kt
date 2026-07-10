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

import androidx.compose.remote.creation.dsl.Modifier
import androidx.compose.remote.creation.dsl.RcColor
import androidx.compose.remote.creation.dsl.RcPaintStyle
import androidx.compose.remote.creation.dsl.RcPathType
import androidx.compose.remote.creation.dsl.RcProfile
import androidx.compose.remote.creation.dsl.RcScope
import androidx.compose.remote.creation.dsl.RcStrokeCap
import androidx.compose.remote.creation.dsl.createRcBuffer
import androidx.compose.remote.creation.dsl.fillMaxSize
import androidx.compose.remote.creation.dsl.setStrokeCap
import androidx.compose.remote.creation.dsl.setStyle
import androidx.compose.remote.creation.profile.RcPlatformProfiles
import androidx.compose.remote.player.core.RemoteDocument
import androidx.compose.remote.tooling.preview.RemoteDocumentPreview
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import kotlin.math.PI

@Suppress("RestrictedApiAndroidX")
@Composable
@Preview
fun RcDslClockPreview() {
    RemoteDocumentPreview(RemoteDocument(dslClock()))
}

/** Reimplementation of MClock using the new type-safe DSL. */
@Suppress("RestrictedApiAndroidX")
fun dslClock(): ByteArray {
    return createRcBuffer(RcProfile(RcPlatformProfiles.ANDROIDX), experimental = true) {
        val color = RcDslClockColors(this)

        val days = remoteArrayOf("Mon ", "Tue ", "Wed ", "Thu ", "Fri ", "Sat ", "Sun ")
        val day = days[dayOfWeek() - 1f]
        val dom = dayOfMonth().format(2, 0)
        val date = day + ":" + dom

        Box(Modifier.fillMaxSize()) {
            Canvas(Modifier.fillMaxSize()) {
                val minX = 0f
                val maxX = PI.toFloat() * 2f
                val w = componentWidth()
                val h = componentHeight()
                val cx = (w / 2f)
                val cy = (h / 2f)
                val rad = min(cx, cy)
                val strokeWidth = (rad / 6f)

                val equ = rFun { x -> rad * (0.97f.rf + 0.03f.rf * cos(x * 12f)) }

                val textSize = rad / 5f
                applyPaint {
                    setColor(color.background)
                    setTextSize(textSize)
                }
                val path =
                    remotePolarPath(
                        equ,
                        minX,
                        maxX,
                        64,
                        centerX = cx,
                        centerY = cy,
                        RcPathType.Spline,
                    )
                drawPath(path)

                applyPaint {
                    setColor(color.hourHand)
                    setStrokeWidth(strokeWidth)
                    setStrokeCap(RcStrokeCap.Round)
                }

                val hrHand = (hour() + (minutes() % 60f) / 60f) * 30f
                save {
                    rotate(hrHand, cx, cy)
                    drawLine(cx, cy, cx, cy - rad / 3f)
                }

                applyPaint {
                    setColor(color.minuteHand)
                    setStrokeWidth(strokeWidth)
                    setStrokeCap(RcStrokeCap.Round)
                }
                save {
                    rotate(minutes() * 6f, cx, cy)
                    drawLine(cx, cy, cx, cy - rad * 0.6f)
                }

                val textPath =
                    remotePolarPath(
                        rFun { rad * 0.7f },
                        minX,
                        maxX,
                        64,
                        centerX = cx,
                        centerY = cy,
                        RcPathType.Spline,
                    )

                save {
                    rotate(seconds() * 6f, cx, cy)
                    val radius = rad * 0.1f
                    applyPaint {
                        setStyle(RcPaintStyle.Fill)
                        setColor(color.dot)
                    }
                    drawCircle(cx, cy - rad + (2f.rf * radius), radius)

                    rotate(70.rf, cx, cy)
                    applyPaint { setColor(color.text) }
                    drawTextOnPath(date, textPath, 0.rf, 0.rf)
                }

                val versionId = remoteText("v1.2")
                drawTextAnchored(versionId, cx, (cy + h) / 2f, 0.5f.rf, 0.5f.rf)
            }
        }
    }
}

@Suppress("RestrictedApiAndroidX")
private class RcDslClockColors(scope: RcScope) {
    var background: RcColor = RcColor(0)
    var hourHand: RcColor = RcColor(0)
    var minuteHand: RcColor = RcColor(0)
    var text: RcColor = RcColor(0)
    var dot: RcColor = RcColor(0)

    init {
        scope.apply {
            Global {
                background =
                    remoteThemedColor(
                        light = "color.system_accent2_50",
                        lightDefault = 0xFF113311.toInt(),
                        dark = "color.system_accent2_800",
                        darkDefault = 0xFFFF9966.toInt(),
                    )
                minuteHand =
                    remoteThemedColor(
                        light = "color.system_accent1_500",
                        lightDefault = 0xFF113311.toInt(),
                        dark = "color.system_accent1_100",
                        darkDefault = 0xFFFF9966.toInt(),
                    )
                text =
                    remoteThemedColor(
                        light = "color.system_on_surface_light",
                        lightDefault = 0xFF113311.toInt(),
                        dark = "color.system_on_surface_dark",
                        darkDefault = 0xFFFF9966.toInt(),
                    )
                dot =
                    remoteThemedColor(
                        light = "color.system_accent3_500",
                        lightDefault = 0xFF113311.toInt(),
                        dark = "color.system_accent3_100",
                        darkDefault = 0xFFFF9966.toInt(),
                    )
                hourHand =
                    remoteThemedColor(
                        light = "color.system_accent2_700",
                        lightDefault = 0xFF113311.toInt(),
                        dark = "color.system_accent2_400",
                        darkDefault = 0xFFFF9966.toInt(),
                    )
            }
        }
    }
}
