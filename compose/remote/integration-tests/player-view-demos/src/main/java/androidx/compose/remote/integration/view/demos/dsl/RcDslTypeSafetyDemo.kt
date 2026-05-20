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
import androidx.compose.remote.creation.dsl.RcAnimationCurve
import androidx.compose.remote.creation.dsl.RcBorderShape
import androidx.compose.remote.creation.dsl.RcColumnVerticalPositioning
import androidx.compose.remote.creation.dsl.RcConditionOp
import androidx.compose.remote.creation.dsl.RcHaptic
import androidx.compose.remote.creation.dsl.RcHorizontalPositioning
import androidx.compose.remote.creation.dsl.RcPoint
import androidx.compose.remote.creation.dsl.RcProfile
import androidx.compose.remote.creation.dsl.RcRect
import androidx.compose.remote.creation.dsl.RcRowHorizontalPositioning
import androidx.compose.remote.creation.dsl.RcSkipKind
import androidx.compose.remote.creation.dsl.RcTextAlign
import androidx.compose.remote.creation.dsl.RcTextFromFloatSpec
import androidx.compose.remote.creation.dsl.RcVerticalPositioning
import androidx.compose.remote.creation.dsl.RcWeight
import androidx.compose.remote.creation.dsl.background
import androidx.compose.remote.creation.dsl.border
import androidx.compose.remote.creation.dsl.createRcBuffer
import androidx.compose.remote.creation.dsl.fillMaxSize
import androidx.compose.remote.creation.dsl.fillMaxWidth
import androidx.compose.remote.creation.dsl.height
import androidx.compose.remote.creation.dsl.padding
import androidx.compose.remote.creation.dsl.rcAt
import androidx.compose.remote.creation.dsl.rcColor
import androidx.compose.remote.creation.dsl.rdp
import androidx.compose.remote.creation.dsl.rsp
import androidx.compose.remote.creation.dsl.size
import androidx.compose.remote.creation.profile.RcPlatformProfiles

/**
 * A demo that exercises the type-safe API surface introduced by the type-safety pass.
 *
 * Each panel below uses a feature that previously required a raw `Int`/`Short`/`Byte` opcode or an
 * `Any` parameter, and is now expressed as a typed value class or enum.
 *
 * Panels:
 * 1. Geometry — [RcRect.ltrb], [RcRect.centered], [RcPoint] via `rcAt`.
 * 2. Border shape — [RcBorderShape.Rectangle] / `Circle` / `RoundedRectangle`.
 * 3. Conditional — [RcConditionOp] gates a label on a time predicate.
 * 4. Formatted float — [RcTextFromFloatSpec] for grouping + decimal padding.
 * 5. Haptic on click — [RcHaptic.Confirm] via [performHaptic].
 * 6. Animation curve — [RcAnimationCurve.EaseOutBounce] driving a wobble.
 * 7. Skip block — [RcSkipKind.IfApiLessThan] hides experimental content on old players.
 */
@Suppress("RestrictedApiAndroidX")
public fun typeSafetyDemo(): ByteArray {
    return createRcBuffer(RcProfile(RcPlatformProfiles.ANDROIDX), experimental = true) {
        Column(
            modifier = Modifier.fillMaxSize().background(0xFFF7F7F7.toInt()).padding(24.rdp),
            horizontal = RcHorizontalPositioning.Center,
            vertical = RcColumnVerticalPositioning.Top,
        ) {
            // Header — typed font weight.
            Text(
                text = "Type Safety Showcase",
                weight = RcWeight.Bold,
                modifier = Modifier.fillMaxWidth(),
                fontSize = 48.rsp,
                textAlign = RcTextAlign.Center,
            )

            Spacer(Modifier.height(16.rdp))

            // ----------------------------------------------------------------------------
            // Panel 1: geometry — RcRect.ltrb / RcRect.centered / RcPoint via `rcAt`.
            // ----------------------------------------------------------------------------
            sectionLabel("1. Geometry: RcRect + RcPoint")
            Canvas(modifier = Modifier.size(560.rdp, 200.rdp).background(0xFFFFFFFF.toInt())) {
                applyPaint { setColor(0xFF1976D2.toInt()) }
                drawRect(RcRect.ltrb(20f, 20f, 200f, 180f))

                applyPaint { setColor(0xFF388E3C.toInt()) }
                drawRoundRect(RcRect.xywh(220f, 20f, 160f, 160f), 16f, 16f)

                applyPaint { setColor(0xFFFB8C00.toInt()) }
                drawCircle(center = (480f rcAt 100f), radius = 60f)
            }

            Spacer(Modifier.height(16.rdp))

            // ----------------------------------------------------------------------------
            // Panel 2: typed border shapes via RcBorderShape.
            // ----------------------------------------------------------------------------
            sectionLabel("2. Typed border shape (RcBorderShape)")
            Row(
                horizontal = RcRowHorizontalPositioning.SpaceBetween,
                vertical = RcVerticalPositioning.Center,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Box(
                    modifier =
                        Modifier.size(140.rdp)
                            .border(
                                width = 4f,
                                roundedCorner = 0f,
                                color = 0xFF1976D2.rcColor(),
                                shape = RcBorderShape.Rectangle,
                            )
                ) {}
                Box(
                    modifier =
                        Modifier.size(140.rdp)
                            .border(
                                width = 4f,
                                roundedCorner = 0f,
                                color = 0xFF388E3C.rcColor(),
                                shape = RcBorderShape.Circle,
                            )
                ) {}
                Box(
                    modifier =
                        Modifier.size(140.rdp)
                            .border(
                                width = 4f,
                                roundedCorner = 24f,
                                color = 0xFFFB8C00.rcColor(),
                                shape = RcBorderShape.RoundedRectangle,
                            )
                ) {}
            }

            Spacer(Modifier.height(16.rdp))

            // ----------------------------------------------------------------------------
            // Panel 3: conditional gating via RcConditionOp.
            // The label is drawn only when the device clock's seconds count is below 30.
            // ----------------------------------------------------------------------------
            sectionLabel("3. Conditional gating (RcConditionOp.Lt)")
            Box(modifier = Modifier.size(560.rdp, 70.rdp).background(0xFFE3F2FD.toInt())) {
                conditionalOperations(RcConditionOp.Lt, seconds(), 30f.rf) {
                    Text(
                        text = "Visible while seconds < 30",
                        weight = RcWeight.SemiBold,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = RcTextAlign.Center,
                        fontSize = 28.rsp,
                    )
                }
            }

            Spacer(Modifier.height(16.rdp))

            // ----------------------------------------------------------------------------
            // Panel 4: typed format spec for live float-to-text.
            // ----------------------------------------------------------------------------
            sectionLabel("4. Formatted float (RcTextFromFloatSpec)")
            val grouped =
                createTextFromFloat(
                    value = continuousSeconds() * 1000f,
                    whole = 8,
                    decimal = 2,
                    spec =
                        RcTextFromFloatSpec.of(
                            grouping = RcTextFromFloatSpec.Grouping.By3,
                            padPre = RcTextFromFloatSpec.PadPre.Space,
                            options = RcTextFromFloatSpec.Options.Rounding,
                        ),
                )
            Box(modifier = Modifier.size(560.rdp, 70.rdp).background(0xFFFFFFFF.toInt())) {
                Text(
                    text = grouped,
                    weight = RcWeight.Medium,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = RcTextAlign.Center,
                    fontSize = 32.rsp,
                )
            }

            Spacer(Modifier.height(16.rdp))

            // ----------------------------------------------------------------------------
            // Panel 5: typed haptic emitted at doc-load. `performHaptic` is a writer-level
            // op exposed on RcScope; it doesn't fit inside an onClick `RcActionScope`
            // lambda, which only accepts the typed setValue / hostAction surface.
            // ----------------------------------------------------------------------------
            sectionLabel("5. Typed haptic (RcHaptic.SegmentTick)")
            performHaptic(RcHaptic.SegmentTick)
            Box(modifier = Modifier.size(560.rdp, 80.rdp).background(0xFFC8E6C9.toInt())) {
                Text(
                    text = "Haptic 'SegmentTick' fires at doc load",
                    weight = RcWeight.SemiBold,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = RcTextAlign.Center,
                    fontSize = 28.rsp,
                )
            }

            Spacer(Modifier.height(16.rdp))

            // ----------------------------------------------------------------------------
            // Panel 6: animated bar driven by an easing curve. The curve type is typed
            // via RcAnimationCurve.EaseOutBounce. Note that RcRect is a Float-only struct
            // — animated draws still use the raw RcFloat-receiver overloads.
            // ----------------------------------------------------------------------------
            sectionLabel("6. Animated bar (RcAnimationCurve.EaseOutBounce)")
            Canvas(modifier = Modifier.size(560.rdp, 80.rdp).background(0xFFFFFFFF.toInt())) {
                applyPaint { setColor(0xFF6A1B9A.toInt()) }
                val wobble = remoteFloat(0f).anim(2f, RcAnimationCurve.EaseOutBounce)
                drawRect(left = wobble, top = 10f.rf, right = wobble + 80f, bottom = 70f.rf)
            }

            Spacer(Modifier.height(16.rdp))

            // ----------------------------------------------------------------------------
            // Panel 7: api-level skip — hide a "preview only" badge from older players.
            // ----------------------------------------------------------------------------
            sectionLabel("7. Skip on API mismatch (RcSkipKind.IfApiLessThan)")
            Box(modifier = Modifier.size(560.rdp, 60.rdp).background(0xFFFFF9C4.toInt())) {
                skip(RcSkipKind.IfApiLessThan, value = Int.MAX_VALUE) {
                    // This block is always skipped on real players (since no API >= MAX_VALUE),
                    // but compiles and serializes cleanly — proves the typed API wires up.
                    Text(
                        text = "Future-only badge",
                        weight = RcWeight.Bold,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = RcTextAlign.Center,
                        fontSize = 24.rsp,
                    )
                }
                Text(
                    text = "Always-visible body",
                    weight = RcWeight.Normal,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = RcTextAlign.Center,
                    fontSize = 24.rsp,
                )
            }
        }
    }
}

/**
 * Small italic-ish section label used between panels. Centered in the column with a muted color and
 * SemiBold weight.
 */
@Suppress("RestrictedApiAndroidX")
private fun androidx.compose.remote.creation.dsl.RcScope.sectionLabel(text: String) {
    Text(
        text = text,
        weight = RcWeight.SemiBold,
        modifier = Modifier.fillMaxWidth().padding(top = 8f, bottom = 4f),
        fontSize = 22.rsp,
        textAlign = RcTextAlign.Start,
        color = 0xFF555555.toInt(),
    )
}

/**
 * A second demo: a single-panel "before vs after" showing both the raw and typed forms side-by-side
 * for the same drawing — useful for demoing API ergonomics.
 */
@Suppress("RestrictedApiAndroidX")
public fun typeSafetyBeforeAfter(): ByteArray {
    return createRcBuffer(RcProfile(RcPlatformProfiles.ANDROIDX), experimental = true) {
        Column(
            modifier = Modifier.fillMaxSize().background(0xFFFAFAFA.toInt()).padding(24.rdp),
            horizontal = RcHorizontalPositioning.Center,
            vertical = RcColumnVerticalPositioning.Top,
        ) {
            Text(
                text = "Before / After",
                weight = RcWeight.Bold,
                modifier = Modifier.fillMaxWidth(),
                textAlign = RcTextAlign.Center,
                fontSize = 48.rsp,
            )
            Spacer(Modifier.height(24.rdp))

            // Each row: a short caption + two equivalent canvases. The left canvas uses
            // raw 4-float drawing; the right uses RcRect / RcPoint. They render
            // identically — proving that the typed surface is a pure ergonomic refactor.
            row("Rectangle: 4 floats vs RcRect.ltrb") {
                Canvas(modifier = Modifier.size(220.rdp, 100.rdp).background(0xFFFFFFFF.toInt())) {
                    applyPaint { setColor(0xFF1976D2.toInt()) }
                    drawRect(20f, 20f, 200f, 80f)
                }
                Canvas(modifier = Modifier.size(220.rdp, 100.rdp).background(0xFFFFFFFF.toInt())) {
                    applyPaint { setColor(0xFF1976D2.toInt()) }
                    drawRect(RcRect.ltrb(20f, 20f, 200f, 80f))
                }
            }

            Spacer(Modifier.height(16.rdp))

            row("Circle: 3 floats vs RcPoint + radius") {
                Canvas(modifier = Modifier.size(220.rdp, 100.rdp).background(0xFFFFFFFF.toInt())) {
                    applyPaint { setColor(0xFF388E3C.toInt()) }
                    drawCircle(110f, 50f, 40f)
                }
                Canvas(modifier = Modifier.size(220.rdp, 100.rdp).background(0xFFFFFFFF.toInt())) {
                    applyPaint { setColor(0xFF388E3C.toInt()) }
                    drawCircle(center = 110f rcAt 50f, radius = 40f)
                }
            }
        }
    }
}

@Suppress("RestrictedApiAndroidX")
private fun androidx.compose.remote.creation.dsl.RcScope.row(
    caption: String,
    content: androidx.compose.remote.creation.dsl.RcRowScope.() -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = caption,
            weight = RcWeight.Medium,
            modifier = Modifier.fillMaxWidth().padding(bottom = 4f),
            textAlign = RcTextAlign.Start,
            fontSize = 22.rsp,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontal = RcRowHorizontalPositioning.SpaceBetween,
            content = content,
        )
    }
}
