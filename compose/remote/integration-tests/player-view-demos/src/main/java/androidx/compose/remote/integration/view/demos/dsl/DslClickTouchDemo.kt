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

import androidx.compose.remote.core.operations.Header
import androidx.compose.remote.creation.dsl.*
import androidx.compose.remote.creation.profile.RcPlatformProfiles
import androidx.compose.remote.player.core.RemoteDocument
import androidx.compose.remote.tooling.preview.RemoteDocumentPreview
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

/**
 * Creates an interactive showcase testing clicks, multi-clicks, touch lifecycles, and ripples.
 *
 * Demonstrates single click, double click, long press, touch up/down/cancel, player-side touch
 * expressions within a movement threshold, and ripple effects across static and scrollable layouts.
 *
 * @param unifiedPointerInput whether to enable the unified pointer input feature flag
 * @return serialized RemoteCompose document as a byte array
 */
@JvmOverloads
@Suppress("RestrictedApiAndroidX")
public fun dslClickTouchDemo(unifiedPointerInput: Boolean = true): ByteArray {
    return createRcBuffer(
        RcProfile(RcPlatformProfiles.ANDROIDX, isExperimental = true),
        RcHeaderTag.densityBehavior(RcDensityBehavior.Dp),
        RcHeaderTag.docWidth(420),
        RcHeaderTag.docHeight(840),
        RcHeaderTag.raw(Header.FEATURE_UNIFIED_POINTER_INPUT, if (unifiedPointerInput) 1 else 0),
    ) {
        val currentTab = remoteNamedInteger("currentTab", 0)

        Column(
            modifier = Modifier.fillMaxSize().background(0xFFF8FAFC.toInt()),
            vertical = RcColumnVerticalPositioning.Top,
        ) {
            Row { HeaderBar(currentTab) }
            Row {
                StateLayout(stateIndex = currentTab, modifier = Modifier.fillMaxSize()) {
                    TouchDemoView(
                        title = "Static View (No Scroll)",
                        titleColor = 0xFF1D4ED8.toInt(),
                        isScrollable = false,
                    )
                    TouchDemoView(
                        title = "Scrollable View (Drag to cancel)",
                        titleColor = 0xFF047857.toInt(),
                        isScrollable = true,
                    )
                }
            }
        }
    }
}

/** Header and Navigation Bar. */
@Suppress("RestrictedApiAndroidX")
private fun RcScope.HeaderBar(currentTab: RcInteger) {
    Column(
        modifier =
            Modifier.fillMaxWidth().background(0xFFFFFFFF.toInt()).padding(16f, 12f, 16f, 12f),
        horizontal = RcHorizontalPositioning.Center,
    ) {
        Text(
            text = "Touch Input Showcase",
            fontSize = 20.rsp,
            fontWeight = RcFontWeight.Bold,
            color = 0xFF0F172A.toInt(),
            textAlign = RcTextAlign.Center,
        )
        Text(
            text = "Testing click, multi-click, touch Up/Down/Cancel, expressions & ripples",
            fontSize = 10.rsp,
            color = 0xFF64748B.toInt(),
            textAlign = RcTextAlign.Center,
            modifier = Modifier.padding(top = 2f, bottom = 6f),
        )

        // Row of Buttons.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontal = RcRowHorizontalPositioning.SpaceBetween,
            vertical = RcVerticalPositioning.Center,
        ) {
            Box(
                modifier =
                    Modifier.horizontalWeight(1f)
                        .height(34f)
                        .clip(RcShape.roundedCorner(6f))
                        .background(0xFFF1F5F9.toInt())
                        .border(
                            1.5f,
                            6f,
                            0xFFCBD5E1.rcColor(),
                            RcBorderShape.RoundedRectangle,
                        )
                        .onClick { setValue(currentTab, 0) },
                horizontal = RcHorizontalPositioning.Center,
                vertical = RcVerticalPositioning.Center,
            ) {
                Text("Static", color = 0xFF1E293B.toInt(), fontSize = 12.rsp)
            }

            Spacer(Modifier.width(10f))

            Box(
                modifier =
                    Modifier.horizontalWeight(1f)
                        .height(34f)
                        .clip(RcShape.roundedCorner(6f))
                        .background(0xFFF1F5F9.toInt())
                        .border(
                            1.5f,
                            6f,
                            0xFFCBD5E1.rcColor(),
                            RcBorderShape.RoundedRectangle,
                        )
                        .onClick { setValue(currentTab, 1) },
                horizontal = RcHorizontalPositioning.Center,
                vertical = RcVerticalPositioning.Center,
            ) {
                Text("Scroll", color = 0xFF1E293B.toInt(), fontSize = 12.rsp)
            }
        }
    }
}

/** Unified view rendering the 10 touch buttons, supporting both static and scrollable layouts. */
@Suppress("RestrictedApiAndroidX")
private fun RcScope.TouchDemoView(
    title: String,
    titleColor: Int,
    isScrollable: Boolean,
) {
    val colModifier =
        if (isScrollable) {
            Modifier.fillMaxSize().verticalScroll().padding(16f)
        } else {
            Modifier.fillMaxSize().padding(16f)
        }

    Column(
        modifier = colModifier,
        horizontal = RcHorizontalPositioning.Center,
        vertical = RcColumnVerticalPositioning.Top,
    ) {
        Text(
            text = title,
            fontWeight = RcFontWeight.Bold,
            fontSize = 16.rsp,
            color = titleColor,
            modifier = Modifier.padding(bottom = 8f),
        )

        // Button 1: Single Click (Soft Sky Blue)
        val button1Click = remoteNamedFloat("button1_click", 0f)
        Box(
            modifier =
                Modifier.fillMaxWidth()
                    .height(40f)
                    .clip(RcShape.roundedCorner(10f))
                    .background(0xFFDBEAFE.toInt())
                    .border(1.5f, 10f, 0xFF3B82F6.rcColor(), RcBorderShape.RoundedRectangle)
                    .onClick { setValue(button1Click, button1Click + 1f) }
                    .padding(12f),
            horizontal = RcHorizontalPositioning.Start,
            vertical = RcVerticalPositioning.Center,
        ) {
            Text(
                text =
                    remoteText("1. Single Click  |  Clicks: ") +
                        createTextFromFloat(button1Click, 3, 0),
                color = 0xFF1E3A8A.toInt(),
                fontSize = 13.rsp,
            )
        }

        Spacer(Modifier.height(6f))

        // Button 2: Double Click (Soft Indigo)
        val button2DoubleClick = remoteNamedFloat("button2_doubleClick", 0f)
        Box(
            modifier =
                Modifier.fillMaxWidth()
                    .height(40f)
                    .clip(RcShape.roundedCorner(10f))
                    .background(0xFFE0E7FF.toInt())
                    .border(1.5f, 10f, 0xFF6366F1.rcColor(), RcBorderShape.RoundedRectangle)
                    .onDoubleClick { setValue(button2DoubleClick, button2DoubleClick + 1f) }
                    .padding(12f),
            horizontal = RcHorizontalPositioning.Start,
            vertical = RcVerticalPositioning.Center,
        ) {
            Text(
                text =
                    remoteText("2. Double Click  |  Double: ") +
                        createTextFromFloat(button2DoubleClick, 3, 0),
                color = 0xFF312E81.toInt(),
                fontSize = 13.rsp,
            )
        }

        Spacer(Modifier.height(6f))

        // Button 3: Long Press (Soft Amber)
        val button3LongClick = remoteNamedFloat("button3_longClick", 0f)
        Box(
            modifier =
                Modifier.fillMaxWidth()
                    .height(40f)
                    .clip(RcShape.roundedCorner(10f))
                    .background(0xFFFEF3C7.toInt())
                    .border(1.5f, 10f, 0xFFF59E0B.rcColor(), RcBorderShape.RoundedRectangle)
                    .onLongClick { setValue(button3LongClick, button3LongClick + 1f) }
                    .padding(12f),
            horizontal = RcHorizontalPositioning.Start,
            vertical = RcVerticalPositioning.Center,
        ) {
            Text(
                text =
                    remoteText("3. Long Press  |  Long: ") +
                        createTextFromFloat(button3LongClick, 3, 0),
                color = 0xFF78350F.toInt(),
                fontSize = 13.rsp,
            )
        }

        Spacer(Modifier.height(6f))

        // Button 4: Combined Multi-Click (Soft Purple)
        val button4Single = remoteNamedFloat("button4_single", 0f)
        val button4Double = remoteNamedFloat("button4_double", 0f)
        val button4Long = remoteNamedFloat("button4_long", 0f)
        Box(
            modifier =
                Modifier.fillMaxWidth()
                    .height(50f)
                    .clip(RcShape.roundedCorner(10f))
                    .background(0xFFF3E8FF.toInt())
                    .border(1.5f, 10f, 0xFFA855F7.rcColor(), RcBorderShape.RoundedRectangle)
                    .onClick { setValue(button4Single, button4Single + 1f) }
                    .onDoubleClick { setValue(button4Double, button4Double + 1f) }
                    .onLongClick { setValue(button4Long, button4Long + 1f) }
                    .padding(12f, 6f, 12f, 6f),
            horizontal = RcHorizontalPositioning.Start,
            vertical = RcVerticalPositioning.Center,
        ) {
            Column {
                Text(
                    "4. Combined Target (Click + Dbl + Long)",
                    color = 0xFF6B21A8.toInt(),
                    fontSize = 12.rsp,
                )
                Text(
                    text =
                        remoteText("Single: ") +
                            createTextFromFloat(button4Single, 2, 0) +
                            remoteText(" | Dbl: ") +
                            createTextFromFloat(button4Double, 2, 0) +
                            remoteText(" | Long: ") +
                            createTextFromFloat(button4Long, 2, 0),
                    color = 0xFF3B0764.toInt(),
                    fontSize = 11.rsp,
                )
            }
        }

        Spacer(Modifier.height(6f))

        // Button 5: Touch Lifecycle (Soft Emerald)
        val button5Down = remoteNamedFloat("button5_down", 0f)
        val button5Up = remoteNamedFloat("button5_up", 0f)
        val button5Cancel = remoteNamedFloat("button5_cancel", 0f)
        Box(
            modifier =
                Modifier.fillMaxWidth()
                    .height(50f)
                    .clip(RcShape.roundedCorner(10f))
                    .background(0xFFDCFCE7.toInt())
                    .border(1.5f, 10f, 0xFF22C55E.rcColor(), RcBorderShape.RoundedRectangle)
                    .onTouchDown { setValue(button5Down, button5Down + 1f) }
                    .onTouchUp { setValue(button5Up, button5Up + 1f) }
                    .onTouchCancel { setValue(button5Cancel, button5Cancel + 1f) }
                    .padding(12f, 6f, 12f, 6f),
            horizontal = RcHorizontalPositioning.Start,
            vertical = RcVerticalPositioning.Center,
        ) {
            Column {
                Text(
                    "5. Touch Lifecycle (Down / Up / Cancel)",
                    color = 0xFF15803D.toInt(),
                    fontSize = 12.rsp,
                )
                Text(
                    text =
                        remoteText("Down: ") +
                            createTextFromFloat(button5Down, 2, 0) +
                            remoteText(" | Up: ") +
                            createTextFromFloat(button5Up, 2, 0) +
                            remoteText(" | Cancel: ") +
                            createTextFromFloat(button5Cancel, 2, 0),
                    color = 0xFF064E3B.toInt(),
                    fontSize = 11.rsp,
                )
            }
        }

        Spacer(Modifier.height(6f))

        // Button 6: Slider with Touch Expression (Soft Warm Orange)
        Box(
            modifier =
                Modifier.fillMaxWidth()
                    .height(40f)
                    .clip(RcShape.roundedCorner(10f))
                    .background(0xFFFFEDD5.toInt())
                    .border(1.5f, 10f, 0xFFF97316.rcColor(), RcBorderShape.RoundedRectangle)
                    .padding(12f, 6f, 12f, 6f),
            horizontal = RcHorizontalPositioning.Start,
            vertical = RcVerticalPositioning.Center,
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "6. Slider (onTouch expression)",
                    color = 0xFF9A3412.toInt(),
                    fontSize = 12.rsp,
                )
                Canvas(
                    modifier = Modifier.fillMaxWidth().height(28f).clip(RcShape.roundedCorner(6f))
                ) {
                    val w = width
                    val cy = 14f
                    val left = 16f
                    val right = w.toFloat() - 16f

                    val pos =
                        addTouch(
                            defaultValue = left,
                            range = left..right,
                            stopMode = RcTouchStopMode.Instantly,
                            expression = touchPosX(),
                        )

                    // Background track
                    paint {
                        color(0xFFFDBA74.toInt())
                        style(RcPaintStyle.Stroke)
                        strokeWidth(6f)
                        strokeCap(RcStrokeCap.Round)
                    }
                    drawLine(left.rf, cy.rf, right.rf, cy.rf)

                    // Active track fill
                    paint {
                        color(0xFFEA580C.toInt())
                        style(RcPaintStyle.Stroke)
                        strokeWidth(6f)
                        strokeCap(RcStrokeCap.Round)
                    }
                    drawLine(left.rf, cy.rf, pos, cy.rf)

                    // Slider thumb knob
                    paint {
                        color(0xFFEA580C.toInt())
                        style(RcPaintStyle.Fill)
                    }
                    drawCircle(pos, cy.rf, 8f.rf)
                }
            }
        }

        Spacer(Modifier.height(6f))

        // Button 7: Vertical Slider with Touch Expression (Soft Warm Orange)
        Box(
            modifier =
                Modifier.fillMaxWidth()
                    .height(100f)
                    .clip(RcShape.roundedCorner(10f))
                    .background(0xFFFFEDD5.toInt())
                    .border(1.5f, 10f, 0xFFF97316.rcColor(), RcBorderShape.RoundedRectangle)
                    .padding(12f, 6f, 12f, 6f),
            horizontal = RcHorizontalPositioning.Start,
            vertical = RcVerticalPositioning.Center,
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "7. Vertical Slider (onTouch Y expression)",
                    color = 0xFF9A3412.toInt(),
                    fontSize = 12.rsp,
                )
                Canvas(
                    modifier =
                        Modifier.fillMaxWidth().fillMaxHeight().clip(RcShape.roundedCorner(6f))
                ) {
                    val h = height
                    val cx = 24f
                    val top = 12f
                    val bottom = h.toFloat() - 12f

                    val pos =
                        addTouch(
                            defaultValue = bottom,
                            range = top..bottom,
                            stopMode = RcTouchStopMode.Instantly,
                            expression = touchPosY(),
                        )

                    // Background track
                    paint {
                        color(0xFFFDBA74.toInt())
                        style(RcPaintStyle.Stroke)
                        strokeWidth(6f)
                        strokeCap(RcStrokeCap.Round)
                    }
                    drawLine(cx.rf, top.rf, cx.rf, bottom.rf)

                    // Active track fill (from bottom up to pos)
                    paint {
                        color(0xFFEA580C.toInt())
                        style(RcPaintStyle.Stroke)
                        strokeWidth(6f)
                        strokeCap(RcStrokeCap.Round)
                    }
                    drawLine(cx.rf, bottom.rf, cx.rf, pos)

                    // Slider thumb knob
                    paint {
                        color(0xFFEA580C.toInt())
                        style(RcPaintStyle.Fill)
                    }
                    drawCircle(cx.rf, pos, 8f.rf)
                }
            }
        }

        Spacer(Modifier.height(6f))

        // Button 8: Ripple Only (No Click Handler)
        Box(
            modifier =
                Modifier.fillMaxWidth()
                    .height(40f)
                    .clip(RcShape.roundedCorner(10f))
                    .background(0xFFF1F5F9.toInt())
                    .border(1.5f, 10f, 0xFFCBD5E1.rcColor(), RcBorderShape.RoundedRectangle)
                    .ripple()
                    .padding(12f),
            horizontal = RcHorizontalPositioning.Start,
            vertical = RcVerticalPositioning.Center,
        ) {
            Text(
                text = "8. Ripple Only (No Click Handler)",
                color = 0xFF475569.toInt(),
                fontSize = 13.rsp,
            )
        }

        Spacer(Modifier.height(6f))

        // Button 9: Ripple + Single Click
        val button9RippleClick = remoteNamedFloat("button9_rippleClick", 0f)
        Box(
            modifier =
                Modifier.fillMaxWidth()
                    .height(40f)
                    .clip(RcShape.roundedCorner(10f))
                    .background(0xFFDBEAFE.toInt())
                    .border(1.5f, 10f, 0xFF3B82F6.rcColor(), RcBorderShape.RoundedRectangle)
                    .ripple()
                    .onClick { setValue(button9RippleClick, button9RippleClick + 1f) }
                    .padding(12f),
            horizontal = RcHorizontalPositioning.Start,
            vertical = RcVerticalPositioning.Center,
        ) {
            Text(
                text =
                    remoteText("9. Ripple + Click  |  Clicks: ") +
                        createTextFromFloat(button9RippleClick, 3, 0),
                color = 0xFF1E3A8A.toInt(),
                fontSize = 13.rsp,
            )
        }

        Spacer(Modifier.height(6f))

        // Button 10: Ripple + Double Click
        val button10RippleDbl = remoteNamedFloat("button10_rippleDbl", 0f)
        Box(
            modifier =
                Modifier.fillMaxWidth()
                    .height(40f)
                    .clip(RcShape.roundedCorner(10f))
                    .background(0xFFE0E7FF.toInt())
                    .border(1.5f, 10f, 0xFF6366F1.rcColor(), RcBorderShape.RoundedRectangle)
                    .ripple()
                    .onDoubleClick { setValue(button10RippleDbl, button10RippleDbl + 1f) }
                    .padding(12f),
            horizontal = RcHorizontalPositioning.Start,
            vertical = RcVerticalPositioning.Center,
        ) {
            Text(
                text =
                    remoteText("10. Ripple + Double Click  |  Double: ") +
                        createTextFromFloat(button10RippleDbl, 3, 0),
                color = 0xFF312E81.toInt(),
                fontSize = 13.rsp,
            )
        }

        Spacer(Modifier.height(6f))

        // Button 11: Ripple + Long Click
        val button11RippleLong = remoteNamedFloat("button11_rippleLong", 0f)
        Box(
            modifier =
                Modifier.fillMaxWidth()
                    .height(40f)
                    .clip(RcShape.roundedCorner(10f))
                    .background(0xFFFEF3C7.toInt())
                    .border(1.5f, 10f, 0xFFF59E0B.rcColor(), RcBorderShape.RoundedRectangle)
                    .ripple()
                    .onLongClick { setValue(button11RippleLong, button11RippleLong + 1f) }
                    .padding(12f),
            horizontal = RcHorizontalPositioning.Start,
            vertical = RcVerticalPositioning.Center,
        ) {
            Text(
                text =
                    remoteText("11. Ripple + Long Click  |  Long: ") +
                        createTextFromFloat(button11RippleLong, 3, 0),
                color = 0xFF78350F.toInt(),
                fontSize = 13.rsp,
            )
        }

        if (isScrollable) {
            Spacer(Modifier.height(14f))

            // Bottom Scroll Travel Buffer
            Box(
                modifier =
                    Modifier.fillMaxWidth()
                        .height(300f)
                        .clip(RcShape.roundedCorner(8f))
                        .background(0xFFF1F5F9.toInt())
                        .border(
                            1f,
                            8f,
                            0xFFCBD5E1.rcColor(),
                            RcBorderShape.RoundedRectangle,
                        ),
                horizontal = RcHorizontalPositioning.Center,
                vertical = RcVerticalPositioning.Center,
            ) {
                Text(
                    "↕ Scroll Travel Buffer (Drag upwards to scroll) ↕",
                    color = 0xFF64748B.toInt(),
                    fontSize = 13.rsp,
                )
            }
        }

        Spacer(Modifier.height(20f))
    }
}

@Suppress("RestrictedApiAndroidX")
@Composable
@Preview
public fun DslClickTouchDemoPreview() {
    RemoteDocumentPreview(RemoteDocument(dslClickTouchDemo()))
}
