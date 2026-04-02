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

import androidx.compose.remote.creation.Rc
import androidx.compose.remote.creation.dsl.Modifier
import androidx.compose.remote.creation.dsl.RcColor
import androidx.compose.remote.creation.dsl.RcProfile
import androidx.compose.remote.creation.dsl.RcScope
import androidx.compose.remote.creation.dsl.createRcBuffer
import androidx.compose.remote.creation.dsl.fillMaxSize
import androidx.compose.remote.creation.profile.RcPlatformProfiles
import androidx.compose.remote.player.core.RemoteDocument
import androidx.compose.remote.tooling.preview.RemoteDocPreview
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import kotlin.math.PI

@Suppress("RestrictedApiAndroidX")
@Composable
@Preview
fun RcDslClockPreview() {
    RemoteDocPreview(RemoteDocument(RcDslClock()))
}

/** Reimplementation of MClock using the new type-safe DSL. */
@Suppress("RestrictedApiAndroidX")
fun RcDslClock(): ByteArray {
    return createRcBuffer(RcProfile(RcPlatformProfiles.ANDROIDX), experimental = true) {
        val color = RcDslClockColors(this)

        val dayNamesId = addStringList("Mon ", "Tue ", "Wed ", "Thu ", "Fri ", "Sat ", "Sun ")
        val day = textLookup(dayNamesId, (DayOfWeek() - 1f))
        val dom = DayOfMonth().format(2, 0, 0)
        val date = textMerge(day, dom)

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
                    addPolarPathExpression(
                        equ,
                        minX,
                        maxX,
                        64,
                        centerX = cx,
                        centerY = cy,
                        Rc.PathExpression.SPLINE_PATH,
                    )
                drawPath(path)

                applyPaint {
                    setColor(color.hourHand)
                    setStrokeWidth(strokeWidth)
                    setStrokeCap(1) // ROUND
                }

                val hrHand = (Hour() + (Minutes() % 60f) / 60f) * 30f
                save {
                    rotate(hrHand, cx, cy)
                    drawLine(cx, cy, cx, cy - rad / 3f)
                }

                applyPaint {
                    setColor(color.minuteHand)
                    setStrokeWidth(strokeWidth)
                    setStrokeCap(1) // ROUND
                }
                save {
                    rotate(Minutes() * 6f, cx, cy)
                    drawLine(cx, cy, cx, cy - rad * 0.6f)
                }

                val textPath =
                    addPolarPathExpression(
                        rFun { rad * 0.7f },
                        minX,
                        maxX,
                        64,
                        centerX = cx,
                        centerY = cy,
                        Rc.PathExpression.SPLINE_PATH,
                    )

                save {
                    rotate(Seconds() * 6f, cx, cy)
                    val radius = rad * 0.1f
                    applyPaint {
                        setStyle(0) // FILL
                        setColor(color.dot)
                    }
                    drawCircle(cx, cy - rad + (2f.rf * radius), radius)

                    rotate(70.rf, cx, cy)
                    applyPaint { setColor(color.text) }
                    drawTextOnPath(date, textPath, 0.rf, 0.rf)
                }

                val versionId = addText("v1.2")
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
                    addThemedColor(
                        light = "color.system_accent2_50",
                        lightDefault = 0xFF113311.toInt(),
                        dark = "color.system_accent2_800",
                        darkDefault = 0xFFFF9966.toInt(),
                    )
                minuteHand =
                    addThemedColor(
                        light = "color.system_accent1_500",
                        lightDefault = 0xFF113311.toInt(),
                        dark = "color.system_accent1_100",
                        darkDefault = 0xFFFF9966.toInt(),
                    )
                text =
                    addThemedColor(
                        light = "color.system_on_surface_light",
                        lightDefault = 0xFF113311.toInt(),
                        dark = "color.system_on_surface_dark",
                        darkDefault = 0xFFFF9966.toInt(),
                    )
                dot =
                    addThemedColor(
                        light = "color.system_accent3_500",
                        lightDefault = 0xFF113311.toInt(),
                        dark = "color.system_accent3_100",
                        darkDefault = 0xFFFF9966.toInt(),
                    )
                hourHand =
                    addThemedColor(
                        light = "color.system_accent2_700",
                        lightDefault = 0xFF113311.toInt(),
                        dark = "color.system_accent2_400",
                        darkDefault = 0xFFFF9966.toInt(),
                    )
            }
        }
    }
}
