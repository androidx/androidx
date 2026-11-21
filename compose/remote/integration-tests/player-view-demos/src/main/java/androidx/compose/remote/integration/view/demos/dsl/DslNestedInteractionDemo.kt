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
 * Creates a nested interaction and event bubbling test suite document.
 *
 * Covers discrete multi-click combinations, raw touch lifecycle and player expressions, mixed modes
 * (discrete, raw, ripple), and deep hierarchies with complex gestures.
 *
 * @param unifiedPointerInput whether to enable the unified pointer input feature flag
 * @return serialized RemoteCompose document as a byte array
 */
@JvmOverloads
@Suppress("RestrictedApiAndroidX")
public fun dslNestedInteractionDemo(unifiedPointerInput: Boolean = true): ByteArray {
    return createRcBuffer(
        RcProfile(RcPlatformProfiles.ANDROIDX),
        experimental = true,
        RcHeaderTag.densityBehavior(RcDensityBehavior.Dp),
        RcHeaderTag.docWidth(420),
        RcHeaderTag.docHeight(840),
        RcHeaderTag.raw(Header.FEATURE_UNIFIED_POINTER_INPUT, if (unifiedPointerInput) 1 else 0),
    ) {
        val currentTab = remoteNamedInteger("nestedTab", 0)
        val threshold = 50f.rf

        Column(
            modifier = Modifier.fillMaxSize().background(0xFFF8FAFC.toInt()),
            vertical = RcColumnVerticalPositioning.Top,
        ) {
            Row { NestedHeaderBar(currentTab) }
            Row {
                StateLayout(stateIndex = currentTab, modifier = Modifier.fillMaxSize()) {
                    MultiClickTab()
                    RawTouchExprTab(threshold)
                    MixedModesTab(threshold)
                    DeepHierarchiesTab()
                }
            }
        }
    }
}

/** Header with 4 buttons. */
@Suppress("RestrictedApiAndroidX")
private fun RcScope.NestedHeaderBar(currentTab: RcInteger) {
    Column(
        modifier =
            Modifier.fillMaxWidth().background(0xFFFFFFFF.toInt()).padding(14f, 10f, 14f, 10f),
        horizontal = RcHorizontalPositioning.Center,
    ) {
        Text(
            text = "Nested Touch Input Showcase",
            fontSize = 20.rsp,
            fontWeight = RcFontWeight.Bold,
            color = 0xFF0F172A.toInt(),
            textAlign = RcTextAlign.Center,
        )
        Text(
            text = "Event bubbling, precedence, gesture conflicts & raw pointer lifecycles",
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
                        .border(1.5f, 6f, 0xFFCBD5E1.rcColor(), RcBorderShape.RoundedRectangle)
                        .onClick { setValue(currentTab, 0) },
                horizontal = RcHorizontalPositioning.Center,
                vertical = RcVerticalPositioning.Center,
            ) {
                Text("1. Multi", color = 0xFF1E293B.toInt(), fontSize = 12.rsp)
            }

            Spacer(Modifier.width(4f))

            Box(
                modifier =
                    Modifier.horizontalWeight(1f)
                        .height(34f)
                        .clip(RcShape.roundedCorner(6f))
                        .background(0xFFF1F5F9.toInt())
                        .border(1.5f, 6f, 0xFFCBD5E1.rcColor(), RcBorderShape.RoundedRectangle)
                        .onClick { setValue(currentTab, 1) },
                horizontal = RcHorizontalPositioning.Center,
                vertical = RcVerticalPositioning.Center,
            ) {
                Text("2. Raw/Expr", color = 0xFF1E293B.toInt(), fontSize = 12.rsp)
            }

            Spacer(Modifier.width(4f))

            Box(
                modifier =
                    Modifier.horizontalWeight(1f)
                        .height(34f)
                        .clip(RcShape.roundedCorner(6f))
                        .background(0xFFF1F5F9.toInt())
                        .border(1.5f, 6f, 0xFFCBD5E1.rcColor(), RcBorderShape.RoundedRectangle)
                        .onClick { setValue(currentTab, 2) },
                horizontal = RcHorizontalPositioning.Center,
                vertical = RcVerticalPositioning.Center,
            ) {
                Text("3. Mixed", color = 0xFF1E293B.toInt(), fontSize = 12.rsp)
            }

            Spacer(Modifier.width(4f))

            Box(
                modifier =
                    Modifier.horizontalWeight(1f)
                        .height(34f)
                        .clip(RcShape.roundedCorner(6f))
                        .background(0xFFF1F5F9.toInt())
                        .border(1.5f, 6f, 0xFFCBD5E1.rcColor(), RcBorderShape.RoundedRectangle)
                        .onClick { setValue(currentTab, 3) },
                horizontal = RcHorizontalPositioning.Center,
                vertical = RcVerticalPositioning.Center,
            ) {
                Text("4. Deep", color = 0xFF1E293B.toInt(), fontSize = 12.rsp)
            }
        }
    }
}

/** Tab 1 - Multi-Click Combinations. */
@Suppress("RestrictedApiAndroidX")
private fun RcScope.MultiClickTab() {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll().padding(14f, 10f, 14f, 14f),
        horizontal = RcHorizontalPositioning.Center,
        vertical = RcColumnVerticalPositioning.Top,
    ) {
        Text(
            text = "1. Multi-Click Combinations",
            fontWeight = RcFontWeight.Bold,
            fontSize = 16.rsp,
            color = 0xFF1D4ED8.toInt(),
            modifier = Modifier.padding(bottom = 10f),
        )

        // 1. Single in Single
        val card1OuterClick = remoteNamedFloat("card1_outerClick", 0f)
        val card1ChildAClick = remoteNamedFloat("card1_childAClick", 0f)
        val card1ChildBClick = remoteNamedFloat("card1_childBClick", 0f)
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 12f)) {
            Text(
                "1. Single Click in Single Click (Outer + Inner)",
                color = 0xFF0F172A.toInt(),
                fontSize = 13.rsp,
            )
            Box(
                modifier =
                    Modifier.fillMaxWidth()
                        .clip(RcShape.roundedCorner(8f))
                        .background(0xFFDBEAFE.toInt())
                        .border(1.5f, 8f, 0xFF3B82F6.rcColor(), RcBorderShape.RoundedRectangle)
                        .onClick { setValue(card1OuterClick, card1OuterClick + 1f) }
                        .padding(12f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontal = RcRowHorizontalPositioning.SpaceBetween,
                    vertical = RcVerticalPositioning.Center,
                ) {
                    Box(
                        modifier =
                            Modifier.horizontalWeight(1f)
                                .height(44f)
                                .clip(RcShape.roundedCorner(6f))
                                .background(0xFF1D4ED8.toInt())
                                .onClick { setValue(card1ChildAClick, card1ChildAClick + 1f) },
                        horizontal = RcHorizontalPositioning.Center,
                        vertical = RcVerticalPositioning.Center,
                    ) {
                        Text("Child A (Click)", color = 0xFFFFFFFF.toInt(), fontSize = 12.rsp)
                    }
                    Box(
                        modifier = Modifier.width(40f).height(44f),
                        horizontal = RcHorizontalPositioning.Center,
                        vertical = RcVerticalPositioning.Center,
                    ) {
                        Text("Gap", color = 0xFF1E3A8A.toInt(), fontSize = 10.rsp)
                    }
                    Box(
                        modifier =
                            Modifier.horizontalWeight(1f)
                                .height(44f)
                                .clip(RcShape.roundedCorner(6f))
                                .background(0xFF0F766E.toInt())
                                .onClick { setValue(card1ChildBClick, card1ChildBClick + 1f) },
                        horizontal = RcHorizontalPositioning.Center,
                        vertical = RcVerticalPositioning.Center,
                    ) {
                        Text("Child B (Click)", color = 0xFFFFFFFF.toInt(), fontSize = 12.rsp)
                    }
                }
            }
            Text(
                text =
                    remoteText("Outer: ") +
                        createTextFromFloat(card1OuterClick, 2, 0) +
                        remoteText(" | A: ") +
                        createTextFromFloat(card1ChildAClick, 2, 0) +
                        remoteText(" | B: ") +
                        createTextFromFloat(card1ChildBClick, 2, 0),
                color = 0xFF1E3A8A.toInt(),
                fontSize = 11.rsp,
                modifier = Modifier.padding(top = 2f),
            )
        }

        // 2. Double in Single
        val card2OuterClick = remoteNamedFloat("card2_outerClick", 0f)
        val card2ChildADouble = remoteNamedFloat("card2_childADouble", 0f)
        val card2ChildBDouble = remoteNamedFloat("card2_childBDouble", 0f)
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 12f)) {
            Text(
                "2. Double Click in Single Click (Outer Single + Inner Dbl)",
                color = 0xFF0F172A.toInt(),
                fontSize = 13.rsp,
            )
            Box(
                modifier =
                    Modifier.fillMaxWidth()
                        .clip(RcShape.roundedCorner(8f))
                        .background(0xFFEDE9FE.toInt())
                        .border(1.5f, 8f, 0xFF8B5CF6.rcColor(), RcBorderShape.RoundedRectangle)
                        .onClick { setValue(card2OuterClick, card2OuterClick + 1f) }
                        .padding(12f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontal = RcRowHorizontalPositioning.SpaceBetween,
                    vertical = RcVerticalPositioning.Center,
                ) {
                    Box(
                        modifier =
                            Modifier.horizontalWeight(1f)
                                .height(44f)
                                .clip(RcShape.roundedCorner(6f))
                                .background(0xFF6D28D9.toInt())
                                .onDoubleClick {
                                    setValue(card2ChildADouble, card2ChildADouble + 1f)
                                },
                        horizontal = RcHorizontalPositioning.Center,
                        vertical = RcVerticalPositioning.Center,
                    ) {
                        Text("Child A (Dbl)", color = 0xFFFFFFFF.toInt(), fontSize = 12.rsp)
                    }
                    Box(
                        modifier = Modifier.width(40f).height(44f),
                        horizontal = RcHorizontalPositioning.Center,
                        vertical = RcVerticalPositioning.Center,
                    ) {
                        Text("Gap", color = 0xFF4C1D95.toInt(), fontSize = 10.rsp)
                    }
                    Box(
                        modifier =
                            Modifier.horizontalWeight(1f)
                                .height(44f)
                                .clip(RcShape.roundedCorner(6f))
                                .background(0xFF6D28D9.toInt())
                                .onDoubleClick {
                                    setValue(card2ChildBDouble, card2ChildBDouble + 1f)
                                },
                        horizontal = RcHorizontalPositioning.Center,
                        vertical = RcVerticalPositioning.Center,
                    ) {
                        Text("Child B (Dbl)", color = 0xFFFFFFFF.toInt(), fontSize = 12.rsp)
                    }
                }
            }
            Text(
                text =
                    remoteText("Outer Single: ") +
                        createTextFromFloat(card2OuterClick, 2, 0) +
                        remoteText(" | A Dbl: ") +
                        createTextFromFloat(card2ChildADouble, 2, 0) +
                        remoteText(" | B Dbl: ") +
                        createTextFromFloat(card2ChildBDouble, 2, 0),
                color = 0xFF4C1D95.toInt(),
                fontSize = 11.rsp,
                modifier = Modifier.padding(top = 2f),
            )
        }

        // 3. Single in Double
        val card3OuterDouble = remoteNamedFloat("card3_outerDouble", 0f)
        val card3ChildAClick = remoteNamedFloat("card3_childAClick", 0f)
        val card3ChildBClick = remoteNamedFloat("card3_childBClick", 0f)
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 12f)) {
            Text(
                "3. Single Click in Double Click (Outer Dbl + Inner Single)",
                color = 0xFF0F172A.toInt(),
                fontSize = 13.rsp,
            )
            Box(
                modifier =
                    Modifier.fillMaxWidth()
                        .clip(RcShape.roundedCorner(8f))
                        .background(0xFFEDE9FE.toInt())
                        .border(1.5f, 8f, 0xFF8B5CF6.rcColor(), RcBorderShape.RoundedRectangle)
                        .onDoubleClick { setValue(card3OuterDouble, card3OuterDouble + 1f) }
                        .padding(12f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontal = RcRowHorizontalPositioning.SpaceBetween,
                    vertical = RcVerticalPositioning.Center,
                ) {
                    Box(
                        modifier =
                            Modifier.horizontalWeight(1f)
                                .height(44f)
                                .clip(RcShape.roundedCorner(6f))
                                .background(0xFF1D4ED8.toInt())
                                .onClick { setValue(card3ChildAClick, card3ChildAClick + 1f) },
                        horizontal = RcHorizontalPositioning.Center,
                        vertical = RcVerticalPositioning.Center,
                    ) {
                        Text("Child A (Click)", color = 0xFFFFFFFF.toInt(), fontSize = 12.rsp)
                    }
                    Box(
                        modifier = Modifier.width(40f).height(44f),
                        horizontal = RcHorizontalPositioning.Center,
                        vertical = RcVerticalPositioning.Center,
                    ) {
                        Text("Gap", color = 0xFF4C1D95.toInt(), fontSize = 10.rsp)
                    }
                    Box(
                        modifier =
                            Modifier.horizontalWeight(1f)
                                .height(44f)
                                .clip(RcShape.roundedCorner(6f))
                                .background(0xFF1D4ED8.toInt())
                                .onClick { setValue(card3ChildBClick, card3ChildBClick + 1f) },
                        horizontal = RcHorizontalPositioning.Center,
                        vertical = RcVerticalPositioning.Center,
                    ) {
                        Text("Child B (Click)", color = 0xFFFFFFFF.toInt(), fontSize = 12.rsp)
                    }
                }
            }
            Text(
                text =
                    remoteText("Outer Dbl: ") +
                        createTextFromFloat(card3OuterDouble, 2, 0) +
                        remoteText(" | A Click: ") +
                        createTextFromFloat(card3ChildAClick, 2, 0) +
                        remoteText(" | B Click: ") +
                        createTextFromFloat(card3ChildBClick, 2, 0),
                color = 0xFF4C1D95.toInt(),
                fontSize = 11.rsp,
                modifier = Modifier.padding(top = 2f),
            )
        }

        // 4. Long in Single
        val card4OuterClick = remoteNamedFloat("card4_outerClick", 0f)
        val card4ChildALong = remoteNamedFloat("card4_childALong", 0f)
        val card4ChildBLong = remoteNamedFloat("card4_childBLong", 0f)
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 12f)) {
            Text(
                "4. Long Press in Single Click (Outer Single + Inner Long)",
                color = 0xFF0F172A.toInt(),
                fontSize = 13.rsp,
            )
            Box(
                modifier =
                    Modifier.fillMaxWidth()
                        .clip(RcShape.roundedCorner(8f))
                        .background(0xFFFEF3C7.toInt())
                        .border(1.5f, 8f, 0xFFF59E0B.rcColor(), RcBorderShape.RoundedRectangle)
                        .onClick { setValue(card4OuterClick, card4OuterClick + 1f) }
                        .padding(12f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontal = RcRowHorizontalPositioning.SpaceBetween,
                    vertical = RcVerticalPositioning.Center,
                ) {
                    Box(
                        modifier =
                            Modifier.horizontalWeight(1f)
                                .height(44f)
                                .clip(RcShape.roundedCorner(6f))
                                .background(0xFFC2410C.toInt())
                                .onLongClick { setValue(card4ChildALong, card4ChildALong + 1f) },
                        horizontal = RcHorizontalPositioning.Center,
                        vertical = RcVerticalPositioning.Center,
                    ) {
                        Text("Child A (Long)", color = 0xFFFFFFFF.toInt(), fontSize = 12.rsp)
                    }
                    Box(
                        modifier = Modifier.width(40f).height(44f),
                        horizontal = RcHorizontalPositioning.Center,
                        vertical = RcVerticalPositioning.Center,
                    ) {
                        Text("Gap", color = 0xFF78350F.toInt(), fontSize = 10.rsp)
                    }
                    Box(
                        modifier =
                            Modifier.horizontalWeight(1f)
                                .height(44f)
                                .clip(RcShape.roundedCorner(6f))
                                .background(0xFFC2410C.toInt())
                                .onLongClick { setValue(card4ChildBLong, card4ChildBLong + 1f) },
                        horizontal = RcHorizontalPositioning.Center,
                        vertical = RcVerticalPositioning.Center,
                    ) {
                        Text("Child B (Long)", color = 0xFFFFFFFF.toInt(), fontSize = 12.rsp)
                    }
                }
            }
            Text(
                text =
                    remoteText("Outer Single: ") +
                        createTextFromFloat(card4OuterClick, 2, 0) +
                        remoteText(" | A Long: ") +
                        createTextFromFloat(card4ChildALong, 2, 0) +
                        remoteText(" | B Long: ") +
                        createTextFromFloat(card4ChildBLong, 2, 0),
                color = 0xFF78350F.toInt(),
                fontSize = 11.rsp,
                modifier = Modifier.padding(top = 2f),
            )
        }

        // 5. Single in Long
        val card5OuterLong = remoteNamedFloat("card5_outerLong", 0f)
        val card5ChildAClick = remoteNamedFloat("card5_childAClick", 0f)
        val card5ChildBClick = remoteNamedFloat("card5_childBClick", 0f)
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 12f)) {
            Text(
                "5. Single Click in Long Press (Outer Long + Inner Single)",
                color = 0xFF0F172A.toInt(),
                fontSize = 13.rsp,
            )
            Box(
                modifier =
                    Modifier.fillMaxWidth()
                        .clip(RcShape.roundedCorner(8f))
                        .background(0xFFFFEDD5.toInt())
                        .border(1.5f, 8f, 0xFFF97316.rcColor(), RcBorderShape.RoundedRectangle)
                        .onLongClick { setValue(card5OuterLong, card5OuterLong + 1f) }
                        .padding(12f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontal = RcRowHorizontalPositioning.SpaceBetween,
                    vertical = RcVerticalPositioning.Center,
                ) {
                    Box(
                        modifier =
                            Modifier.horizontalWeight(1f)
                                .height(44f)
                                .clip(RcShape.roundedCorner(6f))
                                .background(0xFF1D4ED8.toInt())
                                .onClick { setValue(card5ChildAClick, card5ChildAClick + 1f) },
                        horizontal = RcHorizontalPositioning.Center,
                        vertical = RcVerticalPositioning.Center,
                    ) {
                        Text("Child A (Click)", color = 0xFFFFFFFF.toInt(), fontSize = 12.rsp)
                    }
                    Box(
                        modifier = Modifier.width(40f).height(44f),
                        horizontal = RcHorizontalPositioning.Center,
                        vertical = RcVerticalPositioning.Center,
                    ) {
                        Text("Gap", color = 0xFF7C2D12.toInt(), fontSize = 10.rsp)
                    }
                    Box(
                        modifier =
                            Modifier.horizontalWeight(1f)
                                .height(44f)
                                .clip(RcShape.roundedCorner(6f))
                                .background(0xFF1D4ED8.toInt())
                                .onClick { setValue(card5ChildBClick, card5ChildBClick + 1f) },
                        horizontal = RcHorizontalPositioning.Center,
                        vertical = RcVerticalPositioning.Center,
                    ) {
                        Text("Child B (Click)", color = 0xFFFFFFFF.toInt(), fontSize = 12.rsp)
                    }
                }
            }
            Text(
                text =
                    remoteText("Outer Long: ") +
                        createTextFromFloat(card5OuterLong, 2, 0) +
                        remoteText(" | A Click: ") +
                        createTextFromFloat(card5ChildAClick, 2, 0) +
                        remoteText(" | B Click: ") +
                        createTextFromFloat(card5ChildBClick, 2, 0),
                color = 0xFF7C2D12.toInt(),
                fontSize = 11.rsp,
                modifier = Modifier.padding(top = 2f),
            )
        }

        // 6. Double in Long
        val card6OuterLong = remoteNamedFloat("card6_outerLong", 0f)
        val card6ChildADouble = remoteNamedFloat("card6_childADouble", 0f)
        val card6ChildBDouble = remoteNamedFloat("card6_childBDouble", 0f)
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 12f)) {
            Text(
                "6. Double Click in Long Press (Outer Long + Inner Dbl)",
                color = 0xFF0F172A.toInt(),
                fontSize = 13.rsp,
            )
            Box(
                modifier =
                    Modifier.fillMaxWidth()
                        .clip(RcShape.roundedCorner(8f))
                        .background(0xFFFFE4E6.toInt())
                        .border(1.5f, 8f, 0xFFFB7185.rcColor(), RcBorderShape.RoundedRectangle)
                        .onLongClick { setValue(card6OuterLong, card6OuterLong + 1f) }
                        .padding(12f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontal = RcRowHorizontalPositioning.SpaceBetween,
                    vertical = RcVerticalPositioning.Center,
                ) {
                    Box(
                        modifier =
                            Modifier.horizontalWeight(1f)
                                .height(44f)
                                .clip(RcShape.roundedCorner(6f))
                                .background(0xFF6D28D9.toInt())
                                .onDoubleClick {
                                    setValue(card6ChildADouble, card6ChildADouble + 1f)
                                },
                        horizontal = RcHorizontalPositioning.Center,
                        vertical = RcVerticalPositioning.Center,
                    ) {
                        Text("Child A (Dbl)", color = 0xFFFFFFFF.toInt(), fontSize = 12.rsp)
                    }
                    Box(
                        modifier = Modifier.width(40f).height(44f),
                        horizontal = RcHorizontalPositioning.Center,
                        vertical = RcVerticalPositioning.Center,
                    ) {
                        Text("Gap", color = 0xFF881337.toInt(), fontSize = 10.rsp)
                    }
                    Box(
                        modifier =
                            Modifier.horizontalWeight(1f)
                                .height(44f)
                                .clip(RcShape.roundedCorner(6f))
                                .background(0xFF6D28D9.toInt())
                                .onDoubleClick {
                                    setValue(card6ChildBDouble, card6ChildBDouble + 1f)
                                },
                        horizontal = RcHorizontalPositioning.Center,
                        vertical = RcVerticalPositioning.Center,
                    ) {
                        Text("Child B (Dbl)", color = 0xFFFFFFFF.toInt(), fontSize = 12.rsp)
                    }
                }
            }
            Text(
                text =
                    remoteText("Outer Long: ") +
                        createTextFromFloat(card6OuterLong, 2, 0) +
                        remoteText(" | A Dbl: ") +
                        createTextFromFloat(card6ChildADouble, 2, 0) +
                        remoteText(" | B Dbl: ") +
                        createTextFromFloat(card6ChildBDouble, 2, 0),
                color = 0xFF881337.toInt(),
                fontSize = 11.rsp,
                modifier = Modifier.padding(top = 2f),
            )
        }

        // 7. Combined Target in Single Click
        val card7OuterClick = remoteNamedFloat("card7_outerClick", 0f)
        val card7InnerSingle = remoteNamedFloat("card7_innerSingle", 0f)
        val card7InnerDouble = remoteNamedFloat("card7_innerDouble", 0f)
        val card7InnerLong = remoteNamedFloat("card7_innerLong", 0f)
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 12f)) {
            Text(
                "7. Combined Target in Single Click (Outer Single + Inner S/D/L)",
                color = 0xFF0F172A.toInt(),
                fontSize = 13.rsp,
            )
            Box(
                modifier =
                    Modifier.fillMaxWidth()
                        .clip(RcShape.roundedCorner(8f))
                        .background(0xFFDBEAFE.toInt())
                        .border(1.5f, 8f, 0xFF3B82F6.rcColor(), RcBorderShape.RoundedRectangle)
                        .onClick { setValue(card7OuterClick, card7OuterClick + 1f) }
                        .padding(12f)
            ) {
                Box(
                    modifier =
                        Modifier.fillMaxWidth()
                            .height(48f)
                            .clip(RcShape.roundedCorner(6f))
                            .background(0xFF6D28D9.toInt())
                            .onClick { setValue(card7InnerSingle, card7InnerSingle + 1f) }
                            .onDoubleClick { setValue(card7InnerDouble, card7InnerDouble + 1f) }
                            .onLongClick { setValue(card7InnerLong, card7InnerLong + 1f) },
                    horizontal = RcHorizontalPositioning.Center,
                    vertical = RcVerticalPositioning.Center,
                ) {
                    Text(
                        "Inner: Single + Double + Long Target",
                        color = 0xFFFFFFFF.toInt(),
                        fontSize = 12.rsp,
                    )
                }
            }
            Text(
                text =
                    remoteText("Outer (Click): ") +
                        createTextFromFloat(card7OuterClick, 2, 0) +
                        remoteText(" | Inner (S/D/L): ") +
                        createTextFromFloat(card7InnerSingle, 2, 0) +
                        remoteText("/") +
                        createTextFromFloat(card7InnerDouble, 2, 0) +
                        remoteText("/") +
                        createTextFromFloat(card7InnerLong, 2, 0),
                color = 0xFF1E3A8A.toInt(),
                fontSize = 11.rsp,
                modifier = Modifier.padding(top = 2f),
            )
        }

        // 8. Single Click in Combined Target
        val card8OuterSingle = remoteNamedFloat("card8_outerSingle", 0f)
        val card8OuterDouble = remoteNamedFloat("card8_outerDouble", 0f)
        val card8OuterLong = remoteNamedFloat("card8_outerLong", 0f)
        val card8InnerSingle = remoteNamedFloat("card8_innerSingle", 0f)
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 12f)) {
            Text(
                "8. Single Click in Combined Target (Outer S/D/L + Inner Single)",
                color = 0xFF0F172A.toInt(),
                fontSize = 13.rsp,
            )
            Box(
                modifier =
                    Modifier.fillMaxWidth()
                        .clip(RcShape.roundedCorner(8f))
                        .background(0xFFEDE9FE.toInt())
                        .border(1.5f, 8f, 0xFF8B5CF6.rcColor(), RcBorderShape.RoundedRectangle)
                        .onClick { setValue(card8OuterSingle, card8OuterSingle + 1f) }
                        .onDoubleClick { setValue(card8OuterDouble, card8OuterDouble + 1f) }
                        .onLongClick { setValue(card8OuterLong, card8OuterLong + 1f) }
                        .padding(12f)
            ) {
                Box(
                    modifier =
                        Modifier.fillMaxWidth()
                            .height(44f)
                            .clip(RcShape.roundedCorner(6f))
                            .background(0xFF1D4ED8.toInt())
                            .onClick { setValue(card8InnerSingle, card8InnerSingle + 1f) },
                    horizontal = RcHorizontalPositioning.Center,
                    vertical = RcVerticalPositioning.Center,
                ) {
                    Text(
                        "Inner: Single Click Only",
                        color = 0xFFFFFFFF.toInt(),
                        fontSize = 12.rsp,
                    )
                }
            }
            Text(
                text =
                    remoteText("Outer (S/D/L): ") +
                        createTextFromFloat(card8OuterSingle, 2, 0) +
                        remoteText("/") +
                        createTextFromFloat(card8OuterDouble, 2, 0) +
                        remoteText("/") +
                        createTextFromFloat(card8OuterLong, 2, 0) +
                        remoteText(" | Inner Single: ") +
                        createTextFromFloat(card8InnerSingle, 2, 0),
                color = 0xFF4C1D95.toInt(),
                fontSize = 11.rsp,
                modifier = Modifier.padding(top = 2f),
            )
        }

        Spacer(Modifier.height(40f))
    }
}

/** Tab 2 - Click & Touch Expressions. */
@Suppress("RestrictedApiAndroidX")
private fun RcScope.RawTouchExprTab(threshold: RcFloat) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll().padding(14f, 10f, 14f, 14f),
        horizontal = RcHorizontalPositioning.Center,
        vertical = RcColumnVerticalPositioning.Top,
    ) {
        Text(
            text = "2. Raw Touch & Expressions",
            fontWeight = RcFontWeight.Bold,
            fontSize = 16.rsp,
            color = 0xFF047857.toInt(),
            modifier = Modifier.padding(bottom = 10f),
        )

        // 1. Raw in Raw
        val raw1OuterDown = remoteNamedFloat("raw1_outerDown", 0f)
        val raw1OuterUp = remoteNamedFloat("raw1_outerUp", 0f)
        val raw1ChildDown = remoteNamedFloat("raw1_childDown", 0f)
        val raw1ChildUp = remoteNamedFloat("raw1_childUp", 0f)
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 12f)) {
            Text(
                "1. Raw in Raw (Outer D/U + Inner D/U)",
                color = 0xFF0F172A.toInt(),
                fontSize = 13.rsp,
            )
            Box(
                modifier =
                    Modifier.fillMaxWidth()
                        .clip(RcShape.roundedCorner(8f))
                        .background(0xFFF1F5F9.toInt())
                        .border(1.5f, 8f, 0xFF94A3B8.rcColor(), RcBorderShape.RoundedRectangle)
                        .onTouchDown { setValue(raw1OuterDown, raw1OuterDown + 1f) }
                        .onTouchUp { setValue(raw1OuterUp, raw1OuterUp + 1f) }
                        .padding(12f)
            ) {
                Box(
                    modifier =
                        Modifier.fillMaxWidth()
                            .height(44f)
                            .clip(RcShape.roundedCorner(6f))
                            .background(0xFF2563EB.toInt())
                            .onTouchDown { setValue(raw1ChildDown, raw1ChildDown + 1f) }
                            .onTouchUp { setValue(raw1ChildUp, raw1ChildUp + 1f) },
                    horizontal = RcHorizontalPositioning.Center,
                    vertical = RcVerticalPositioning.Center,
                ) {
                    Text("Child (D/U)", color = 0xFFFFFFFF.toInt(), fontSize = 12.rsp)
                }
            }
            Text(
                text =
                    remoteText("Outer: ") +
                        createTextFromFloat(raw1OuterDown, 2, 0) +
                        remoteText("/") +
                        createTextFromFloat(raw1OuterUp, 2, 0) +
                        remoteText(" | Child: ") +
                        createTextFromFloat(raw1ChildDown, 2, 0) +
                        remoteText("/") +
                        createTextFromFloat(raw1ChildUp, 2, 0),
                color = 0xFF475569.toInt(),
                fontSize = 11.rsp,
                modifier = Modifier.padding(top = 2f),
            )
        }

        // 2. Down-only Child vs Up-only Parent
        val raw2OuterUp = remoteNamedFloat("raw2_outerUp", 0f)
        val raw2ChildDown = remoteNamedFloat("raw2_childDown", 0f)
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 12f)) {
            Text(
                "2. Down-only Child vs Up-only Parent",
                color = 0xFF0F172A.toInt(),
                fontSize = 13.rsp,
            )
            Box(
                modifier =
                    Modifier.fillMaxWidth()
                        .clip(RcShape.roundedCorner(8f))
                        .background(0xFFF1F5F9.toInt())
                        .border(1.5f, 8f, 0xFF94A3B8.rcColor(), RcBorderShape.RoundedRectangle)
                        .onTouchUp { setValue(raw2OuterUp, raw2OuterUp + 1f) }
                        .padding(12f)
            ) {
                Box(
                    modifier =
                        Modifier.fillMaxWidth()
                            .height(44f)
                            .clip(RcShape.roundedCorner(6f))
                            .background(0xFF0D9488.toInt())
                            .onTouchDown { setValue(raw2ChildDown, raw2ChildDown + 1f) },
                    horizontal = RcHorizontalPositioning.Center,
                    vertical = RcVerticalPositioning.Center,
                ) {
                    Text("Child (Down Only)", color = 0xFFFFFFFF.toInt(), fontSize = 12.rsp)
                }
            }
            Text(
                text =
                    remoteText("Outer Up: ") +
                        createTextFromFloat(raw2OuterUp, 2, 0) +
                        remoteText(" | Child Down: ") +
                        createTextFromFloat(raw2ChildDown, 2, 0),
                color = 0xFF475569.toInt(),
                fontSize = 11.rsp,
                modifier = Modifier.padding(top = 2f),
            )
        }

        // 3. Touch Expression in Raw Touch
        val raw3OuterDown = remoteNamedFloat("raw3_outerDown", 0f)
        val raw3OuterUp = remoteNamedFloat("raw3_outerUp", 0f)
        val raw3ChildClick = remoteNamedFloat("raw3_childClick", 0f)
        val raw3ChildStartX = remoteNamedFloat("raw3_childStartX", 0f)
        val raw3ChildStartY = remoteNamedFloat("raw3_childStartY", 0f)
        val raw3ChildDist = remoteNamedFloat("raw3_childDist", 0f)
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 12f)) {
            Text(
                "3. Touch Expression in Raw Touch (Outer D/U + Inner Expr)",
                color = 0xFF0F172A.toInt(),
                fontSize = 13.rsp,
            )
            Box(
                modifier =
                    Modifier.fillMaxWidth()
                        .clip(RcShape.roundedCorner(8f))
                        .background(0xFFF1F5F9.toInt())
                        .border(1.5f, 8f, 0xFF94A3B8.rcColor(), RcBorderShape.RoundedRectangle)
                        .onTouchDown { setValue(raw3OuterDown, raw3OuterDown + 1f) }
                        .onTouchUp { setValue(raw3OuterUp, raw3OuterUp + 1f) }
                        .padding(12f)
            ) {
                Box(
                    modifier =
                        Modifier.fillMaxWidth()
                            .height(44f)
                            .clip(RcShape.roundedCorner(6f))
                            .background(0xFF16A34A.toInt())
                            .onTouchDown {
                                setValue(raw3ChildStartX, touchPosX())
                                setValue(raw3ChildStartY, touchPosY())
                            }
                            .onTouchUp {
                                val d =
                                    hypot(
                                        touchPosX() - raw3ChildStartX,
                                        touchPosY() - raw3ChildStartY,
                                    )
                                setValue(raw3ChildDist, d)
                                setValue(
                                    raw3ChildClick,
                                    ifElse(
                                        threshold - d,
                                        raw3ChildClick + 1f,
                                        raw3ChildClick,
                                    ),
                                )
                            }
                            .padding(12f),
                    horizontal = RcHorizontalPositioning.Center,
                    vertical = RcVerticalPositioning.Center,
                ) {
                    Text(
                        "Child (Expr if Δ ≤ 50px)",
                        color = 0xFFFFFFFF.toInt(),
                        fontSize = 12.rsp,
                    )
                }
            }
            Text(
                text =
                    remoteText("Outer: ") +
                        createTextFromFloat(raw3OuterDown, 2, 0) +
                        remoteText("/") +
                        createTextFromFloat(raw3OuterUp, 2, 0) +
                        remoteText(" | Child Expr: ") +
                        createTextFromFloat(raw3ChildClick, 2, 0) +
                        remoteText(" (Δ: ") +
                        createTextFromFloat(raw3ChildDist, 3, 1) +
                        remoteText("px)"),
                color = 0xFF475569.toInt(),
                fontSize = 11.rsp,
                modifier = Modifier.padding(top = 2f),
            )
        }

        // 4. Raw Touch in Touch Expression
        val raw4OuterClick = remoteNamedFloat("raw4_outerClick", 0f)
        val raw4OuterStartX = remoteNamedFloat("raw4_outerStartX", 0f)
        val raw4OuterStartY = remoteNamedFloat("raw4_outerStartY", 0f)
        val raw4OuterDist = remoteNamedFloat("raw4_outerDist", 0f)
        val raw4ChildDown = remoteNamedFloat("raw4_childDown", 0f)
        val raw4ChildUp = remoteNamedFloat("raw4_childUp", 0f)
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 12f)) {
            Text(
                "4. Raw Touch in Touch Expression (Outer Expr + Inner D/U)",
                color = 0xFF0F172A.toInt(),
                fontSize = 13.rsp,
            )
            Box(
                modifier =
                    Modifier.fillMaxWidth()
                        .clip(RcShape.roundedCorner(8f))
                        .background(0xFFDCFCE7.toInt())
                        .border(1.5f, 8f, 0xFF22C55E.rcColor(), RcBorderShape.RoundedRectangle)
                        .onTouchDown {
                            setValue(raw4OuterStartX, touchPosX())
                            setValue(raw4OuterStartY, touchPosY())
                        }
                        .onTouchUp {
                            val d =
                                hypot(
                                    touchPosX() - raw4OuterStartX,
                                    touchPosY() - raw4OuterStartY,
                                )
                            setValue(raw4OuterDist, d)
                            setValue(
                                raw4OuterClick,
                                ifElse(
                                    threshold - d,
                                    raw4OuterClick + 1f,
                                    raw4OuterClick,
                                ),
                            )
                        }
                        .padding(12f)
            ) {
                Box(
                    modifier =
                        Modifier.fillMaxWidth()
                            .height(44f)
                            .clip(RcShape.roundedCorner(6f))
                            .background(0xFF2563EB.toInt())
                            .onTouchDown { setValue(raw4ChildDown, raw4ChildDown + 1f) }
                            .onTouchUp { setValue(raw4ChildUp, raw4ChildUp + 1f) },
                    horizontal = RcHorizontalPositioning.Center,
                    vertical = RcVerticalPositioning.Center,
                ) {
                    Text("Child (D/U)", color = 0xFFFFFFFF.toInt(), fontSize = 12.rsp)
                }
            }
            Text(
                text =
                    remoteText("Outer Expr: ") +
                        createTextFromFloat(raw4OuterClick, 2, 0) +
                        remoteText(" (Δ: ") +
                        createTextFromFloat(raw4OuterDist, 3, 1) +
                        remoteText("px) | Child: ") +
                        createTextFromFloat(raw4ChildDown, 2, 0) +
                        remoteText("/") +
                        createTextFromFloat(raw4ChildUp, 2, 0),
                color = 0xFF475569.toInt(),
                fontSize = 11.rsp,
                modifier = Modifier.padding(top = 2f),
            )
        }

        // 5. Dual Expressions
        val raw5OuterClick = remoteNamedFloat("raw5_outerClick", 0f)
        val raw5OuterStartX = remoteNamedFloat("raw5_outerStartX", 0f)
        val raw5OuterStartY = remoteNamedFloat("raw5_outerStartY", 0f)
        val raw5OuterDist = remoteNamedFloat("raw5_outerDist", 0f)
        val raw5ChildClick = remoteNamedFloat("raw5_childClick", 0f)
        val raw5ChildStartX = remoteNamedFloat("raw5_childStartX", 0f)
        val raw5ChildStartY = remoteNamedFloat("raw5_childStartY", 0f)
        val raw5ChildDist = remoteNamedFloat("raw5_childDist", 0f)
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 12f)) {
            Text(
                "5. Dual Expressions (Both Outer and Inner check Δ ≤ 50px)",
                color = 0xFF0F172A.toInt(),
                fontSize = 13.rsp,
            )
            Box(
                modifier =
                    Modifier.fillMaxWidth()
                        .clip(RcShape.roundedCorner(8f))
                        .background(0xFFDCFCE7.toInt())
                        .border(1.5f, 8f, 0xFF22C55E.rcColor(), RcBorderShape.RoundedRectangle)
                        .onTouchDown {
                            setValue(raw5OuterStartX, touchPosX())
                            setValue(raw5OuterStartY, touchPosY())
                        }
                        .onTouchUp {
                            val d =
                                hypot(
                                    touchPosX() - raw5OuterStartX,
                                    touchPosY() - raw5OuterStartY,
                                )
                            setValue(raw5OuterDist, d)
                            setValue(
                                raw5OuterClick,
                                ifElse(
                                    threshold - d,
                                    raw5OuterClick + 1f,
                                    raw5OuterClick,
                                ),
                            )
                        }
                        .padding(12f)
            ) {
                Box(
                    modifier =
                        Modifier.fillMaxWidth()
                            .height(44f)
                            .clip(RcShape.roundedCorner(6f))
                            .background(0xFF16A34A.toInt())
                            .onTouchDown {
                                setValue(raw5ChildStartX, touchPosX())
                                setValue(raw5ChildStartY, touchPosY())
                            }
                            .onTouchUp {
                                val d =
                                    hypot(
                                        touchPosX() - raw5ChildStartX,
                                        touchPosY() - raw5ChildStartY,
                                    )
                                setValue(raw5ChildDist, d)
                                setValue(
                                    raw5ChildClick,
                                    ifElse(
                                        threshold - d,
                                        raw5ChildClick + 1f,
                                        raw5ChildClick,
                                    ),
                                )
                            }
                            .padding(12f),
                    horizontal = RcHorizontalPositioning.Center,
                    vertical = RcVerticalPositioning.Center,
                ) {
                    Text("Child (Expr)", color = 0xFFFFFFFF.toInt(), fontSize = 12.rsp)
                }
            }
            Text(
                text =
                    remoteText("Outer Expr: ") +
                        createTextFromFloat(raw5OuterClick, 2, 0) +
                        remoteText(" | Child Expr: ") +
                        createTextFromFloat(raw5ChildClick, 2, 0),
                color = 0xFF475569.toInt(),
                fontSize = 11.rsp,
                modifier = Modifier.padding(top = 2f),
            )
        }

        // 6. Cross-Boundary Departure
        val raw6OuterDown = remoteNamedFloat("raw6_outerDown", 0f)
        val raw6OuterUp = remoteNamedFloat("raw6_outerUp", 0f)
        val raw6ChildDown = remoteNamedFloat("raw6_childDown", 0f)
        val raw6ChildUp = remoteNamedFloat("raw6_childUp", 0f)
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 12f)) {
            Text(
                "6. Cross-Boundary Drag Departure (Down in Child -> Drag to Outer)",
                color = 0xFF0F172A.toInt(),
                fontSize = 13.rsp,
            )
            Box(
                modifier =
                    Modifier.fillMaxWidth()
                        .clip(RcShape.roundedCorner(8f))
                        .background(0xFFFEE2E2.toInt())
                        .border(1.5f, 8f, 0xFFEF4444.rcColor(), RcBorderShape.RoundedRectangle)
                        .onTouchDown { setValue(raw6OuterDown, raw6OuterDown + 1f) }
                        .onTouchUp { setValue(raw6OuterUp, raw6OuterUp + 1f) }
                        .padding(12f),
                horizontal = RcHorizontalPositioning.Center,
                vertical = RcVerticalPositioning.Center,
            ) {
                Box(
                    modifier =
                        Modifier.fillMaxWidth(0.7f)
                            .height(44f)
                            .clip(RcShape.roundedCorner(6f))
                            .background(0xFFDC2626.toInt())
                            .onTouchDown { setValue(raw6ChildDown, raw6ChildDown + 1f) }
                            .onTouchUp { setValue(raw6ChildUp, raw6ChildUp + 1f) },
                    horizontal = RcHorizontalPositioning.Center,
                    vertical = RcVerticalPositioning.Center,
                ) {
                    Text(
                        "Press Here -> Drag Out -> Release",
                        color = 0xFFFFFFFF.toInt(),
                        fontSize = 10.rsp,
                    )
                }
            }
            Text(
                text =
                    remoteText("Outer (D/U): ") +
                        createTextFromFloat(raw6OuterDown, 2, 0) +
                        remoteText("/") +
                        createTextFromFloat(raw6OuterUp, 2, 0) +
                        remoteText(" | Child (D/U): ") +
                        createTextFromFloat(raw6ChildDown, 2, 0) +
                        remoteText("/") +
                        createTextFromFloat(raw6ChildUp, 2, 0),
                color = 0xFF475569.toInt(),
                fontSize = 11.rsp,
                modifier = Modifier.padding(top = 2f),
            )
        }

        // 7. Cross-Boundary Arrival
        val raw7OuterDown = remoteNamedFloat("raw7_outerDown", 0f)
        val raw7OuterUp = remoteNamedFloat("raw7_outerUp", 0f)
        val raw7ChildDown = remoteNamedFloat("raw7_childDown", 0f)
        val raw7ChildUp = remoteNamedFloat("raw7_childUp", 0f)
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 12f)) {
            Text(
                "7. Cross-Boundary Drag Arrival (Down in Outer -> Drag to Child)",
                color = 0xFF0F172A.toInt(),
                fontSize = 13.rsp,
            )
            Box(
                modifier =
                    Modifier.fillMaxWidth()
                        .clip(RcShape.roundedCorner(8f))
                        .background(0xFFFEE2E2.toInt())
                        .border(1.5f, 8f, 0xFFEF4444.rcColor(), RcBorderShape.RoundedRectangle)
                        .onTouchDown { setValue(raw7OuterDown, raw7OuterDown + 1f) }
                        .onTouchUp { setValue(raw7OuterUp, raw7OuterUp + 1f) }
                        .padding(12f),
                horizontal = RcHorizontalPositioning.Center,
                vertical = RcVerticalPositioning.Center,
            ) {
                Box(
                    modifier =
                        Modifier.fillMaxWidth(0.7f)
                            .height(44f)
                            .clip(RcShape.roundedCorner(6f))
                            .background(0xFF0D9488.toInt())
                            .onTouchDown { setValue(raw7ChildDown, raw7ChildDown + 1f) }
                            .onTouchUp { setValue(raw7ChildUp, raw7ChildUp + 1f) },
                    horizontal = RcHorizontalPositioning.Center,
                    vertical = RcVerticalPositioning.Center,
                ) {
                    Text(
                        "Release Here after Down Outside",
                        color = 0xFFFFFFFF.toInt(),
                        fontSize = 10.rsp,
                    )
                }
            }
            Text(
                text =
                    remoteText("Outer (D/U): ") +
                        createTextFromFloat(raw7OuterDown, 2, 0) +
                        remoteText("/") +
                        createTextFromFloat(raw7OuterUp, 2, 0) +
                        remoteText(" | Child (D/U): ") +
                        createTextFromFloat(raw7ChildDown, 2, 0) +
                        remoteText("/") +
                        createTextFromFloat(raw7ChildUp, 2, 0),
                color = 0xFF475569.toInt(),
                fontSize = 11.rsp,
                modifier = Modifier.padding(top = 2f),
            )
        }

        Spacer(Modifier.height(40f))
    }
}

/** Tab 3 - Mixed Mode. */
@Suppress("RestrictedApiAndroidX")
private fun RcScope.MixedModesTab(threshold: RcFloat) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll().padding(14f, 10f, 14f, 14f),
        horizontal = RcHorizontalPositioning.Center,
        vertical = RcColumnVerticalPositioning.Top,
    ) {
        Text(
            text = "3. Mixed Modes (Discrete, Raw & Ripple)",
            fontWeight = RcFontWeight.Bold,
            fontSize = 16.rsp,
            color = 0xFFC2410C.toInt(),
            modifier = Modifier.padding(bottom = 10f),
        )

        // 1. Raw Down/Up in Single Click
        val mixed1OuterClick = remoteNamedFloat("mixed1_outerClick", 0f)
        val mixed1ChildDown = remoteNamedFloat("mixed1_childDown", 0f)
        val mixed1ChildUp = remoteNamedFloat("mixed1_childUp", 0f)
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 12f)) {
            Text(
                "1. Raw Down/Up in Single Click (Outer Click + Inner D/U)",
                color = 0xFF0F172A.toInt(),
                fontSize = 13.rsp,
            )
            Box(
                modifier =
                    Modifier.fillMaxWidth()
                        .clip(RcShape.roundedCorner(8f))
                        .background(0xFFEFF6FF.toInt())
                        .border(1.5f, 8f, 0xFF3B82F6.rcColor(), RcBorderShape.RoundedRectangle)
                        .onClick { setValue(mixed1OuterClick, mixed1OuterClick + 1f) }
                        .padding(12f)
            ) {
                Box(
                    modifier =
                        Modifier.fillMaxWidth()
                            .height(44f)
                            .clip(RcShape.roundedCorner(6f))
                            .background(0xFF0D9488.toInt())
                            .onTouchDown { setValue(mixed1ChildDown, mixed1ChildDown + 1f) }
                            .onTouchUp { setValue(mixed1ChildUp, mixed1ChildUp + 1f) },
                    horizontal = RcHorizontalPositioning.Center,
                    vertical = RcVerticalPositioning.Center,
                ) {
                    Text("Child (D/U)", color = 0xFFFFFFFF.toInt(), fontSize = 12.rsp)
                }
            }
            Text(
                text =
                    remoteText("Outer Click: ") +
                        createTextFromFloat(mixed1OuterClick, 2, 0) +
                        remoteText(" | Child D/U: ") +
                        createTextFromFloat(mixed1ChildDown, 2, 0) +
                        remoteText("/") +
                        createTextFromFloat(mixed1ChildUp, 2, 0),
                color = 0xFF475569.toInt(),
                fontSize = 11.rsp,
                modifier = Modifier.padding(top = 2f),
            )
        }

        // 2. Single Click in Raw Down/Up
        val mixed2OuterDown = remoteNamedFloat("mixed2_outerDown", 0f)
        val mixed2OuterUp = remoteNamedFloat("mixed2_outerUp", 0f)
        val mixed2ChildClick = remoteNamedFloat("mixed2_childClick", 0f)
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 12f)) {
            Text(
                "2. Single Click in Raw Down/Up (Outer D/U + Inner Click)",
                color = 0xFF0F172A.toInt(),
                fontSize = 13.rsp,
            )
            Box(
                modifier =
                    Modifier.fillMaxWidth()
                        .clip(RcShape.roundedCorner(8f))
                        .background(0xFFF1F5F9.toInt())
                        .border(1.5f, 8f, 0xFF94A3B8.rcColor(), RcBorderShape.RoundedRectangle)
                        .onTouchDown { setValue(mixed2OuterDown, mixed2OuterDown + 1f) }
                        .onTouchUp { setValue(mixed2OuterUp, mixed2OuterUp + 1f) }
                        .padding(12f)
            ) {
                Box(
                    modifier =
                        Modifier.fillMaxWidth()
                            .height(44f)
                            .clip(RcShape.roundedCorner(6f))
                            .background(0xFF2563EB.toInt())
                            .onClick { setValue(mixed2ChildClick, mixed2ChildClick + 1f) },
                    horizontal = RcHorizontalPositioning.Center,
                    vertical = RcVerticalPositioning.Center,
                ) {
                    Text("Child (Click)", color = 0xFFFFFFFF.toInt(), fontSize = 12.rsp)
                }
            }
            Text(
                text =
                    remoteText("Outer D/U: ") +
                        createTextFromFloat(mixed2OuterDown, 2, 0) +
                        remoteText("/") +
                        createTextFromFloat(mixed2OuterUp, 2, 0) +
                        remoteText(" | Child Click: ") +
                        createTextFromFloat(mixed2ChildClick, 2, 0),
                color = 0xFF475569.toInt(),
                fontSize = 11.rsp,
                modifier = Modifier.padding(top = 2f),
            )
        }

        // 3. Long Click in Raw Down/Up
        val mixed3OuterDown = remoteNamedFloat("mixed3_outerDown", 0f)
        val mixed3OuterUp = remoteNamedFloat("mixed3_outerUp", 0f)
        val mixed3ChildLong = remoteNamedFloat("mixed3_childLong", 0f)
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 12f)) {
            Text(
                "3. Long Click in Raw Down/Up (Outer D/U + Inner Long)",
                color = 0xFF0F172A.toInt(),
                fontSize = 13.rsp,
            )
            Box(
                modifier =
                    Modifier.fillMaxWidth()
                        .clip(RcShape.roundedCorner(8f))
                        .background(0xFFF1F5F9.toInt())
                        .border(1.5f, 8f, 0xFF94A3B8.rcColor(), RcBorderShape.RoundedRectangle)
                        .onTouchDown { setValue(mixed3OuterDown, mixed3OuterDown + 1f) }
                        .onTouchUp { setValue(mixed3OuterUp, mixed3OuterUp + 1f) }
                        .padding(12f)
            ) {
                Box(
                    modifier =
                        Modifier.fillMaxWidth()
                            .height(44f)
                            .clip(RcShape.roundedCorner(6f))
                            .background(0xFFEA580C.toInt())
                            .onLongClick { setValue(mixed3ChildLong, mixed3ChildLong + 1f) },
                    horizontal = RcHorizontalPositioning.Center,
                    vertical = RcVerticalPositioning.Center,
                ) {
                    Text("Child (Long)", color = 0xFFFFFFFF.toInt(), fontSize = 12.rsp)
                }
            }
            Text(
                text =
                    remoteText("Outer D/U: ") +
                        createTextFromFloat(mixed3OuterDown, 2, 0) +
                        remoteText("/") +
                        createTextFromFloat(mixed3OuterUp, 2, 0) +
                        remoteText(" | Child Long: ") +
                        createTextFromFloat(mixed3ChildLong, 2, 0),
                color = 0xFF475569.toInt(),
                fontSize = 11.rsp,
                modifier = Modifier.padding(top = 2f),
            )
        }

        // 4. Touch Expression in Double Click
        val mixed4OuterDouble = remoteNamedFloat("mixed4_outerDouble", 0f)
        val mixed4ChildClick = remoteNamedFloat("mixed4_childClick", 0f)
        val mixed4ChildStartX = remoteNamedFloat("mixed4_childStartX", 0f)
        val mixed4ChildStartY = remoteNamedFloat("mixed4_childStartY", 0f)
        val mixed4ChildDist = remoteNamedFloat("mixed4_childDist", 0f)
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 12f)) {
            Text(
                "4. Touch Expression in Double Click (Outer Dbl + Inner Expr)",
                color = 0xFF0F172A.toInt(),
                fontSize = 13.rsp,
            )
            Box(
                modifier =
                    Modifier.fillMaxWidth()
                        .clip(RcShape.roundedCorner(8f))
                        .background(0xFFF5F3FF.toInt())
                        .border(1.5f, 8f, 0xFF8B5CF6.rcColor(), RcBorderShape.RoundedRectangle)
                        .onDoubleClick { setValue(mixed4OuterDouble, mixed4OuterDouble + 1f) }
                        .padding(12f)
            ) {
                Box(
                    modifier =
                        Modifier.fillMaxWidth()
                            .height(44f)
                            .clip(RcShape.roundedCorner(6f))
                            .background(0xFF16A34A.toInt())
                            .onTouchDown {
                                setValue(mixed4ChildStartX, touchPosX())
                                setValue(mixed4ChildStartY, touchPosY())
                            }
                            .onTouchUp {
                                val d =
                                    hypot(
                                        touchPosX() - mixed4ChildStartX,
                                        touchPosY() - mixed4ChildStartY,
                                    )
                                setValue(mixed4ChildDist, d)
                                setValue(
                                    mixed4ChildClick,
                                    ifElse(
                                        threshold - d,
                                        mixed4ChildClick + 1f,
                                        mixed4ChildClick,
                                    ),
                                )
                            }
                            .padding(12f),
                    horizontal = RcHorizontalPositioning.Center,
                    vertical = RcVerticalPositioning.Center,
                ) {
                    Text(
                        "Child (Expr if Δ ≤ 50px)",
                        color = 0xFFFFFFFF.toInt(),
                        fontSize = 12.rsp,
                    )
                }
            }
            Text(
                text =
                    remoteText("Outer Dbl: ") +
                        createTextFromFloat(mixed4OuterDouble, 2, 0) +
                        remoteText(" | Child Expr: ") +
                        createTextFromFloat(mixed4ChildClick, 2, 0) +
                        remoteText(" (Δ: ") +
                        createTextFromFloat(mixed4ChildDist, 3, 1) +
                        remoteText("px)"),
                color = 0xFF475569.toInt(),
                fontSize = 11.rsp,
                modifier = Modifier.padding(top = 2f),
            )
        }

        // 5. Mismatched Siblings
        val mixed5OuterClick = remoteNamedFloat("mixed5_outerClick", 0f)
        val mixed5ChildAClick = remoteNamedFloat("mixed5_childAClick", 0f)
        val mixed5ChildBClick = remoteNamedFloat("mixed5_childBClick", 0f)
        val mixed5ChildBStartX = remoteNamedFloat("mixed5_childBStartX", 0f)
        val mixed5ChildBStartY = remoteNamedFloat("mixed5_childBStartY", 0f)
        val mixed5ChildBDist = remoteNamedFloat("mixed5_childBDist", 0f)
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 12f)) {
            Text(
                "5. Mismatched Siblings (A: Click, B: Touch Expr)",
                color = 0xFF0F172A.toInt(),
                fontSize = 13.rsp,
            )
            Box(
                modifier =
                    Modifier.fillMaxWidth()
                        .clip(RcShape.roundedCorner(8f))
                        .background(0xFFEFF6FF.toInt())
                        .border(1.5f, 8f, 0xFF3B82F6.rcColor(), RcBorderShape.RoundedRectangle)
                        .onClick { setValue(mixed5OuterClick, mixed5OuterClick + 1f) }
                        .padding(12f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontal = RcRowHorizontalPositioning.SpaceBetween,
                    vertical = RcVerticalPositioning.Center,
                ) {
                    Box(
                        modifier =
                            Modifier.horizontalWeight(1f)
                                .height(44f)
                                .clip(RcShape.roundedCorner(6f))
                                .background(0xFF2563EB.toInt())
                                .onClick { setValue(mixed5ChildAClick, mixed5ChildAClick + 1f) },
                        horizontal = RcHorizontalPositioning.Center,
                        vertical = RcVerticalPositioning.Center,
                    ) {
                        Text("Child A (Click)", color = 0xFFFFFFFF.toInt(), fontSize = 12.rsp)
                    }
                    Box(
                        modifier = Modifier.width(40f).height(44f),
                        horizontal = RcHorizontalPositioning.Center,
                        vertical = RcVerticalPositioning.Center,
                    ) {
                        Text("Gap", color = 0xFF64748B.toInt(), fontSize = 10.rsp)
                    }
                    Box(
                        modifier =
                            Modifier.horizontalWeight(1f)
                                .height(44f)
                                .clip(RcShape.roundedCorner(6f))
                                .background(0xFFEA580C.toInt())
                                .onTouchDown {
                                    setValue(mixed5ChildBStartX, touchPosX())
                                    setValue(mixed5ChildBStartY, touchPosY())
                                }
                                .onTouchUp {
                                    val d =
                                        hypot(
                                            touchPosX() - mixed5ChildBStartX,
                                            touchPosY() - mixed5ChildBStartY,
                                        )
                                    setValue(mixed5ChildBDist, d)
                                    setValue(
                                        mixed5ChildBClick,
                                        ifElse(
                                            threshold - d,
                                            mixed5ChildBClick + 1f,
                                            mixed5ChildBClick,
                                        ),
                                    )
                                },
                        horizontal = RcHorizontalPositioning.Center,
                        vertical = RcVerticalPositioning.Center,
                    ) {
                        Text("Child B (Expr)", color = 0xFFFFFFFF.toInt(), fontSize = 12.rsp)
                    }
                }
            }
            Text(
                text =
                    remoteText("Outer: ") +
                        createTextFromFloat(mixed5OuterClick, 2, 0) +
                        remoteText(" | A (Click): ") +
                        createTextFromFloat(mixed5ChildAClick, 2, 0) +
                        remoteText(" | B (Expr): ") +
                        createTextFromFloat(mixed5ChildBClick, 2, 0),
                color = 0xFF475569.toInt(),
                fontSize = 11.rsp,
                modifier = Modifier.padding(top = 2f),
            )
        }

        // 6. Hole / Passthrough Sibling
        val mixed6OuterClick = remoteNamedFloat("mixed6_outerClick", 0f)
        val mixed6ChildAClick = remoteNamedFloat("mixed6_childAClick", 0f)
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 12f)) {
            Text(
                "6. Hole / Passthrough Sibling (A: Click, B: No Handlers)",
                color = 0xFF0F172A.toInt(),
                fontSize = 13.rsp,
            )
            Box(
                modifier =
                    Modifier.fillMaxWidth()
                        .clip(RcShape.roundedCorner(8f))
                        .background(0xFFEFF6FF.toInt())
                        .border(1.5f, 8f, 0xFF3B82F6.rcColor(), RcBorderShape.RoundedRectangle)
                        .onClick { setValue(mixed6OuterClick, mixed6OuterClick + 1f) }
                        .padding(12f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontal = RcRowHorizontalPositioning.SpaceBetween,
                    vertical = RcVerticalPositioning.Center,
                ) {
                    Box(
                        modifier =
                            Modifier.horizontalWeight(1f)
                                .height(44f)
                                .clip(RcShape.roundedCorner(6f))
                                .background(0xFF2563EB.toInt())
                                .onClick { setValue(mixed6ChildAClick, mixed6ChildAClick + 1f) },
                        horizontal = RcHorizontalPositioning.Center,
                        vertical = RcVerticalPositioning.Center,
                    ) {
                        Text("Child A (Click)", color = 0xFFFFFFFF.toInt(), fontSize = 12.rsp)
                    }
                    Box(
                        modifier = Modifier.width(40f).height(44f),
                        horizontal = RcHorizontalPositioning.Center,
                        vertical = RcVerticalPositioning.Center,
                    ) {
                        Text("Gap", color = 0xFF64748B.toInt(), fontSize = 10.rsp)
                    }
                    Box(
                        modifier =
                            Modifier.horizontalWeight(1f)
                                .height(44f)
                                .clip(RcShape.roundedCorner(6f))
                                .background(0xFFE2E8F0.toInt()),
                        horizontal = RcHorizontalPositioning.Center,
                        vertical = RcVerticalPositioning.Center,
                    ) {
                        Text("Child B (No Op)", color = 0xFF64748B.toInt(), fontSize = 12.rsp)
                    }
                }
            }
            Text(
                text =
                    remoteText("Outer Click: ") +
                        createTextFromFloat(mixed6OuterClick, 2, 0) +
                        remoteText(" | A (Click): ") +
                        createTextFromFloat(mixed6ChildAClick, 2, 0) +
                        remoteText(" | B: (Bubbles to Outer)"),
                color = 0xFF475569.toInt(),
                fontSize = 11.rsp,
                modifier = Modifier.padding(top = 2f),
            )
        }

        // 7. Ripple Outer + Click Inner
        val mixed7ChildAClick = remoteNamedFloat("mixed7_childAClick", 0f)
        val mixed7ChildBClick = remoteNamedFloat("mixed7_childBClick", 0f)
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 12f)) {
            Text(
                "7. Ripple Outer (No Click) + Click Inner",
                color = 0xFF0F172A.toInt(),
                fontSize = 13.rsp,
            )
            Box(
                modifier =
                    Modifier.fillMaxWidth()
                        .clip(RcShape.roundedCorner(8f))
                        .background(0xFFDBEAFE.toInt())
                        .border(1.5f, 8f, 0xFF3B82F6.rcColor(), RcBorderShape.RoundedRectangle)
                        .ripple()
                        .padding(12f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontal = RcRowHorizontalPositioning.SpaceBetween,
                    vertical = RcVerticalPositioning.Center,
                ) {
                    Box(
                        modifier =
                            Modifier.horizontalWeight(1f)
                                .height(44f)
                                .clip(RcShape.roundedCorner(6f))
                                .background(0xFF1D4ED8.toInt())
                                .onClick { setValue(mixed7ChildAClick, mixed7ChildAClick + 1f) },
                        horizontal = RcHorizontalPositioning.Center,
                        vertical = RcVerticalPositioning.Center,
                    ) {
                        Text("Child A (Click)", color = 0xFFFFFFFF.toInt(), fontSize = 12.rsp)
                    }
                    Box(
                        modifier = Modifier.width(60f).height(44f),
                        horizontal = RcHorizontalPositioning.Center,
                        vertical = RcVerticalPositioning.Center,
                    ) {
                        Text("Gap (Ripple)", color = 0xFF1E3A8A.toInt(), fontSize = 10.rsp)
                    }
                    Box(
                        modifier =
                            Modifier.horizontalWeight(1f)
                                .height(44f)
                                .clip(RcShape.roundedCorner(6f))
                                .background(0xFF0F766E.toInt())
                                .onClick { setValue(mixed7ChildBClick, mixed7ChildBClick + 1f) },
                        horizontal = RcHorizontalPositioning.Center,
                        vertical = RcVerticalPositioning.Center,
                    ) {
                        Text("Child B (Click)", color = 0xFFFFFFFF.toInt(), fontSize = 12.rsp)
                    }
                }
            }
            Text(
                text =
                    remoteText("Outer: Ripple Only (No Click) | A: ") +
                        createTextFromFloat(mixed7ChildAClick, 2, 0) +
                        remoteText(" | B: ") +
                        createTextFromFloat(mixed7ChildBClick, 2, 0),
                color = 0xFF1E3A8A.toInt(),
                fontSize = 11.rsp,
                modifier = Modifier.padding(top = 2f),
            )
        }

        // 8. Click Outer + Ripple Inner
        val mixed8OuterClick = remoteNamedFloat("mixed8_outerClick", 0f)
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 12f)) {
            Text(
                "8. Click Outer + Ripple Inner (No Child Click)",
                color = 0xFF0F172A.toInt(),
                fontSize = 13.rsp,
            )
            Box(
                modifier =
                    Modifier.fillMaxWidth()
                        .clip(RcShape.roundedCorner(8f))
                        .background(0xFFEDE9FE.toInt())
                        .border(1.5f, 8f, 0xFF8B5CF6.rcColor(), RcBorderShape.RoundedRectangle)
                        .onClick { setValue(mixed8OuterClick, mixed8OuterClick + 1f) }
                        .padding(12f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontal = RcRowHorizontalPositioning.SpaceBetween,
                    vertical = RcVerticalPositioning.Center,
                ) {
                    Box(
                        modifier =
                            Modifier.horizontalWeight(1f)
                                .height(44f)
                                .clip(RcShape.roundedCorner(6f))
                                .background(0xFF6D28D9.toInt())
                                .ripple(),
                        horizontal = RcHorizontalPositioning.Center,
                        vertical = RcVerticalPositioning.Center,
                    ) {
                        Text("Child A (Ripple)", color = 0xFFFFFFFF.toInt(), fontSize = 12.rsp)
                    }
                    Box(
                        modifier = Modifier.width(40f).height(44f),
                        horizontal = RcHorizontalPositioning.Center,
                        vertical = RcVerticalPositioning.Center,
                    ) {
                        Text("Gap", color = 0xFF4C1D95.toInt(), fontSize = 10.rsp)
                    }
                    Box(
                        modifier =
                            Modifier.horizontalWeight(1f)
                                .height(44f)
                                .clip(RcShape.roundedCorner(6f))
                                .background(0xFF6D28D9.toInt())
                                .ripple(),
                        horizontal = RcHorizontalPositioning.Center,
                        vertical = RcVerticalPositioning.Center,
                    ) {
                        Text("Child B (Ripple)", color = 0xFFFFFFFF.toInt(), fontSize = 12.rsp)
                    }
                }
            }
            Text(
                text =
                    remoteText("Outer Clicks: ") +
                        createTextFromFloat(mixed8OuterClick, 2, 0) +
                        remoteText(" (Children show ripple & test click bubbling)"),
                color = 0xFF4C1D95.toInt(),
                fontSize = 11.rsp,
                modifier = Modifier.padding(top = 2f),
            )
        }

        // 9. Ripple in Ripple
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 12f)) {
            Text(
                "9. Ripple in Ripple (Outer Ripple + Inner Ripple)",
                color = 0xFF0F172A.toInt(),
                fontSize = 13.rsp,
            )
            Box(
                modifier =
                    Modifier.fillMaxWidth()
                        .clip(RcShape.roundedCorner(8f))
                        .background(0xFFFEF3C7.toInt())
                        .border(1.5f, 8f, 0xFFF59E0B.rcColor(), RcBorderShape.RoundedRectangle)
                        .ripple()
                        .padding(12f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontal = RcRowHorizontalPositioning.SpaceBetween,
                    vertical = RcVerticalPositioning.Center,
                ) {
                    Box(
                        modifier =
                            Modifier.horizontalWeight(1f)
                                .height(44f)
                                .clip(RcShape.roundedCorner(6f))
                                .background(0xFFC2410C.toInt())
                                .ripple(),
                        horizontal = RcHorizontalPositioning.Center,
                        vertical = RcVerticalPositioning.Center,
                    ) {
                        Text("Child A (Ripple)", color = 0xFFFFFFFF.toInt(), fontSize = 12.rsp)
                    }
                    Box(
                        modifier = Modifier.width(60f).height(44f),
                        horizontal = RcHorizontalPositioning.Center,
                        vertical = RcVerticalPositioning.Center,
                    ) {
                        Text("Gap (Outer)", color = 0xFF78350F.toInt(), fontSize = 10.rsp)
                    }
                    Box(
                        modifier =
                            Modifier.horizontalWeight(1f)
                                .height(44f)
                                .clip(RcShape.roundedCorner(6f))
                                .background(0xFFC2410C.toInt())
                                .ripple(),
                        horizontal = RcHorizontalPositioning.Center,
                        vertical = RcVerticalPositioning.Center,
                    ) {
                        Text("Child B (Ripple)", color = 0xFFFFFFFF.toInt(), fontSize = 12.rsp)
                    }
                }
            }
            Text(
                text = "Both Outer and Inner components have Ripple modifiers only",
                color = 0xFF78350F.toInt(),
                fontSize = 11.rsp,
                modifier = Modifier.padding(top = 2f),
            )
        }

        // 10. Sibling with Ripple vs Sibling without Ripple
        val mixed10OuterClick = remoteNamedFloat("mixed10_outerClick", 0f)
        val mixed10ChildARippleClick = remoteNamedFloat("mixed10_childARippleClick", 0f)
        val mixed10ChildBClick = remoteNamedFloat("mixed10_childBClick", 0f)
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 12f)) {
            Text(
                "10. Sibling with Ripple vs Sibling without Ripple",
                color = 0xFF0F172A.toInt(),
                fontSize = 13.rsp,
            )
            Box(
                modifier =
                    Modifier.fillMaxWidth()
                        .clip(RcShape.roundedCorner(8f))
                        .background(0xFFEFF6FF.toInt())
                        .border(1.5f, 8f, 0xFF3B82F6.rcColor(), RcBorderShape.RoundedRectangle)
                        .onClick { setValue(mixed10OuterClick, mixed10OuterClick + 1f) }
                        .padding(12f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontal = RcRowHorizontalPositioning.SpaceBetween,
                    vertical = RcVerticalPositioning.Center,
                ) {
                    Box(
                        modifier =
                            Modifier.horizontalWeight(1f)
                                .height(44f)
                                .clip(RcShape.roundedCorner(6f))
                                .background(0xFF2563EB.toInt())
                                .ripple()
                                .onClick {
                                    setValue(
                                        mixed10ChildARippleClick,
                                        mixed10ChildARippleClick + 1f,
                                    )
                                },
                        horizontal = RcHorizontalPositioning.Center,
                        vertical = RcVerticalPositioning.Center,
                    ) {
                        Text("A: Click + Ripple", color = 0xFFFFFFFF.toInt(), fontSize = 11.rsp)
                    }
                    Box(
                        modifier = Modifier.width(40f).height(44f),
                        horizontal = RcHorizontalPositioning.Center,
                        vertical = RcVerticalPositioning.Center,
                    ) {
                        Text("Gap", color = 0xFF64748B.toInt(), fontSize = 10.rsp)
                    }
                    Box(
                        modifier =
                            Modifier.horizontalWeight(1f)
                                .height(44f)
                                .clip(RcShape.roundedCorner(6f))
                                .background(0xFF0F766E.toInt())
                                .onClick { setValue(mixed10ChildBClick, mixed10ChildBClick + 1f) },
                        horizontal = RcHorizontalPositioning.Center,
                        vertical = RcVerticalPositioning.Center,
                    ) {
                        Text("B: Click Only", color = 0xFFFFFFFF.toInt(), fontSize = 11.rsp)
                    }
                }
            }
            Text(
                text =
                    remoteText("Outer: ") +
                        createTextFromFloat(mixed10OuterClick, 2, 0) +
                        remoteText(" | A (Ripple): ") +
                        createTextFromFloat(mixed10ChildARippleClick, 2, 0) +
                        remoteText(" | B (No Ripple): ") +
                        createTextFromFloat(mixed10ChildBClick, 2, 0),
                color = 0xFF475569.toInt(),
                fontSize = 11.rsp,
                modifier = Modifier.padding(top = 2f),
            )
        }

        Spacer(Modifier.height(40f))
    }
}

/** Tab 4 - Deep Hierarchies. */
@Suppress("RestrictedApiAndroidX")
private fun RcScope.DeepHierarchiesTab() {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll().padding(14f, 10f, 14f, 14f),
        horizontal = RcHorizontalPositioning.Center,
        vertical = RcColumnVerticalPositioning.Top,
    ) {
        Text(
            text = "4. Deep & Complex Hierarchies",
            fontWeight = RcFontWeight.Bold,
            fontSize = 16.rsp,
            color = 0xFF7C3AED.toInt(),
            modifier = Modifier.padding(bottom = 10f),
        )

        // 1. 3-Level Hierarchy (All Click)
        val deep1GrandparentClick = remoteNamedFloat("deep1_grandparentClick", 0f)
        val deep1ParentClick = remoteNamedFloat("deep1_parentClick", 0f)
        val deep1ChildClick = remoteNamedFloat("deep1_childClick", 0f)
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 12f)) {
            Text(
                "1. 3-Level Hierarchy (Grandparent -> Parent -> Child Click)",
                color = 0xFF0F172A.toInt(),
                fontSize = 13.rsp,
            )
            Box(
                modifier =
                    Modifier.fillMaxWidth()
                        .clip(RcShape.roundedCorner(8f))
                        .background(0xFFEFF6FF.toInt())
                        .border(1.5f, 8f, 0xFF93C5FD.rcColor(), RcBorderShape.RoundedRectangle)
                        .onClick { setValue(deep1GrandparentClick, deep1GrandparentClick + 1f) }
                        .padding(14f),
                horizontal = RcHorizontalPositioning.Center,
                vertical = RcVerticalPositioning.Center,
            ) {
                Box(
                    modifier =
                        Modifier.fillMaxWidth(0.85f)
                            .clip(RcShape.roundedCorner(6f))
                            .background(0xFFDBEAFE.toInt())
                            .border(
                                1.5f,
                                6f,
                                0xFF3B82F6.rcColor(),
                                RcBorderShape.RoundedRectangle,
                            )
                            .onClick { setValue(deep1ParentClick, deep1ParentClick + 1f) }
                            .padding(12f),
                    horizontal = RcHorizontalPositioning.Center,
                    vertical = RcVerticalPositioning.Center,
                ) {
                    Box(
                        modifier =
                            Modifier.fillMaxWidth(0.7f)
                                .height(36f)
                                .clip(RcShape.roundedCorner(4f))
                                .background(0xFF1D4ED8.toInt())
                                .onClick { setValue(deep1ChildClick, deep1ChildClick + 1f) },
                        horizontal = RcHorizontalPositioning.Center,
                        vertical = RcVerticalPositioning.Center,
                    ) {
                        Text(
                            "Innermost Child",
                            color = 0xFFFFFFFF.toInt(),
                            fontSize = 11.rsp,
                        )
                    }
                }
            }
            Text(
                text =
                    remoteText("Grandparent: ") +
                        createTextFromFloat(deep1GrandparentClick, 2, 0) +
                        remoteText(" | Parent: ") +
                        createTextFromFloat(deep1ParentClick, 2, 0) +
                        remoteText(" | Child: ") +
                        createTextFromFloat(deep1ChildClick, 2, 0),
                color = 0xFF475569.toInt(),
                fontSize = 11.rsp,
                modifier = Modifier.padding(top = 2f),
            )
        }

        // 2. 3-Level Mixed Modes
        val deep2GrandparentClick = remoteNamedFloat("deep2_grandparentClick", 0f)
        val deep2ParentDown = remoteNamedFloat("deep2_parentDown", 0f)
        val deep2ParentUp = remoteNamedFloat("deep2_parentUp", 0f)
        val deep2ChildDouble = remoteNamedFloat("deep2_childDouble", 0f)
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 12f)) {
            Text(
                "2. 3-Level Mixed Modes (Grand: Click, Parent: D/U, Child: Dbl)",
                color = 0xFF0F172A.toInt(),
                fontSize = 13.rsp,
            )
            Box(
                modifier =
                    Modifier.fillMaxWidth()
                        .clip(RcShape.roundedCorner(8f))
                        .background(0xFFEFF6FF.toInt())
                        .border(1.5f, 8f, 0xFF93C5FD.rcColor(), RcBorderShape.RoundedRectangle)
                        .onClick { setValue(deep2GrandparentClick, deep2GrandparentClick + 1f) }
                        .padding(14f),
                horizontal = RcHorizontalPositioning.Center,
                vertical = RcVerticalPositioning.Center,
            ) {
                Box(
                    modifier =
                        Modifier.fillMaxWidth(0.85f)
                            .clip(RcShape.roundedCorner(6f))
                            .background(0xFFF1F5F9.toInt())
                            .border(
                                1.5f,
                                6f,
                                0xFF94A3B8.rcColor(),
                                RcBorderShape.RoundedRectangle,
                            )
                            .onTouchDown { setValue(deep2ParentDown, deep2ParentDown + 1f) }
                            .onTouchUp { setValue(deep2ParentUp, deep2ParentUp + 1f) }
                            .padding(12f),
                    horizontal = RcHorizontalPositioning.Center,
                    vertical = RcVerticalPositioning.Center,
                ) {
                    Box(
                        modifier =
                            Modifier.fillMaxWidth(0.7f)
                                .height(36f)
                                .clip(RcShape.roundedCorner(4f))
                                .background(0xFF7C3AED.toInt())
                                .onDoubleClick {
                                    setValue(deep2ChildDouble, deep2ChildDouble + 1f)
                                },
                        horizontal = RcHorizontalPositioning.Center,
                        vertical = RcVerticalPositioning.Center,
                    ) {
                        Text(
                            "Child (Dbl)",
                            color = 0xFFFFFFFF.toInt(),
                            fontSize = 11.rsp,
                        )
                    }
                }
            }
            Text(
                text =
                    remoteText("Grand (Click): ") +
                        createTextFromFloat(deep2GrandparentClick, 2, 0) +
                        remoteText(" | Parent (D/U): ") +
                        createTextFromFloat(deep2ParentDown, 2, 0) +
                        remoteText("/") +
                        createTextFromFloat(deep2ParentUp, 2, 0) +
                        remoteText(" | Child (Dbl): ") +
                        createTextFromFloat(deep2ChildDouble, 2, 0),
                color = 0xFF475569.toInt(),
                fontSize = 11.rsp,
                modifier = Modifier.padding(top = 2f),
            )
        }

        // 3. Draggable Slider in Clickable Card
        val deep3OuterClick = remoteNamedFloat("deep3_outerClick", 0f)
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 12f)) {
            Text(
                "3. Continuous Slider inside Clickable Card",
                color = 0xFF0F172A.toInt(),
                fontSize = 13.rsp,
            )
            Box(
                modifier =
                    Modifier.fillMaxWidth()
                        .clip(RcShape.roundedCorner(8f))
                        .background(0xFFFAF5FF.toInt())
                        .border(1.5f, 8f, 0xFFC4B5FD.rcColor(), RcBorderShape.RoundedRectangle)
                        .onClick { setValue(deep3OuterClick, deep3OuterClick + 1f) }
                        .padding(12f)
            ) {
                Column(horizontal = RcHorizontalPositioning.Center) {
                    Text(
                        "Outer Card (Clickable) - Drag Slider Below",
                        color = 0xFF64748B.toInt(),
                        fontSize = 11.rsp,
                    )
                    Spacer(Modifier.height(8f))
                    Canvas(
                        modifier =
                            Modifier.fillMaxWidth()
                                .height(48f)
                                .clip(RcShape.roundedCorner(6f))
                                .background(0xFFF1F5F9.toInt())
                    ) {
                        val w = width
                        val cy = 24f
                        val left = 30f
                        val right = w - 30f

                        val pos = addTouch(150f, 30f..450f, RcTouchStopMode.Gently, touchPosX())

                        paint {
                            color(0xFFCBD5E1.toInt())
                            style(RcPaintStyle.Stroke)
                            strokeWidth(8f)
                            strokeCap(RcStrokeCap.Round)
                        }
                        drawLine(left.rf, cy.rf, right, cy.rf)

                        paint {
                            color(0xFF7C3AED.toInt())
                            style(RcPaintStyle.Fill)
                        }
                        drawCircle(pos, cy.rf, 14f.rf)
                    }
                }
            }
            Text(
                text =
                    remoteText("Outer Clicks (Must stay 0 during drag): ") +
                        createTextFromFloat(deep3OuterClick, 2, 0),
                color = 0xFF475569.toInt(),
                fontSize = 11.rsp,
                modifier = Modifier.padding(top = 2f),
            )
        }

        // 4. Multi-Click Hierarchy Conflict
        val deep4OuterSingle = remoteNamedFloat("deep4_outerSingle", 0f)
        val deep4OuterDouble = remoteNamedFloat("deep4_outerDouble", 0f)
        val deep4OuterLong = remoteNamedFloat("deep4_outerLong", 0f)
        val deep4ChildAClick = remoteNamedFloat("deep4_childAClick", 0f)
        val deep4ChildBLong = remoteNamedFloat("deep4_childBLong", 0f)
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 12f)) {
            Text(
                "4. Multi-Click Hierarchy Conflict",
                color = 0xFF0F172A.toInt(),
                fontSize = 13.rsp,
            )
            Box(
                modifier =
                    Modifier.fillMaxWidth()
                        .clip(RcShape.roundedCorner(8f))
                        .background(0xFFEFF6FF.toInt())
                        .border(1.5f, 8f, 0xFF93C5FD.rcColor(), RcBorderShape.RoundedRectangle)
                        .onClick { setValue(deep4OuterSingle, deep4OuterSingle + 1f) }
                        .onDoubleClick { setValue(deep4OuterDouble, deep4OuterDouble + 1f) }
                        .onLongClick { setValue(deep4OuterLong, deep4OuterLong + 1f) }
                        .padding(12f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontal = RcRowHorizontalPositioning.SpaceBetween,
                    vertical = RcVerticalPositioning.Center,
                ) {
                    Box(
                        modifier =
                            Modifier.horizontalWeight(1f)
                                .height(44f)
                                .clip(RcShape.roundedCorner(6f))
                                .background(0xFF2563EB.toInt())
                                .onClick { setValue(deep4ChildAClick, deep4ChildAClick + 1f) },
                        horizontal = RcHorizontalPositioning.Center,
                        vertical = RcVerticalPositioning.Center,
                    ) {
                        Text("A: Click Only", color = 0xFFFFFFFF.toInt(), fontSize = 11.rsp)
                    }
                    Box(
                        modifier = Modifier.width(40f).height(44f),
                        horizontal = RcHorizontalPositioning.Center,
                        vertical = RcVerticalPositioning.Center,
                    ) {
                        Text("Gap", color = 0xFF64748B.toInt(), fontSize = 10.rsp)
                    }
                    Box(
                        modifier =
                            Modifier.horizontalWeight(1f)
                                .height(44f)
                                .clip(RcShape.roundedCorner(6f))
                                .background(0xFFEA580C.toInt())
                                .onLongClick { setValue(deep4ChildBLong, deep4ChildBLong + 1f) },
                        horizontal = RcHorizontalPositioning.Center,
                        vertical = RcVerticalPositioning.Center,
                    ) {
                        Text("B: Long Only", color = 0xFFFFFFFF.toInt(), fontSize = 11.rsp)
                    }
                }
            }
            Text(
                text =
                    remoteText("Outer (S/D/L): ") +
                        createTextFromFloat(deep4OuterSingle, 2, 0) +
                        remoteText("/") +
                        createTextFromFloat(deep4OuterDouble, 2, 0) +
                        remoteText("/") +
                        createTextFromFloat(deep4OuterLong, 2, 0) +
                        remoteText(" | A: ") +
                        createTextFromFloat(deep4ChildAClick, 2, 0) +
                        remoteText(" | B: ") +
                        createTextFromFloat(deep4ChildBLong, 2, 0),
                color = 0xFF475569.toInt(),
                fontSize = 11.rsp,
                modifier = Modifier.padding(top = 2f),
            )
        }

        // 5. Nested Touch Cancellation under Scroll
        val deep5OuterDown = remoteNamedFloat("deep5_outerDown", 0f)
        val deep5OuterUp = remoteNamedFloat("deep5_outerUp", 0f)
        val deep5OuterCancel = remoteNamedFloat("deep5_outerCancel", 0f)
        val deep5ChildDown = remoteNamedFloat("deep5_childDown", 0f)
        val deep5ChildUp = remoteNamedFloat("deep5_childUp", 0f)
        val deep5ChildCancel = remoteNamedFloat("deep5_childCancel", 0f)
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 12f)) {
            Text(
                "5. Touch Cancellation under Parent Scroll",
                color = 0xFF0F172A.toInt(),
                fontSize = 13.rsp,
            )
            Box(
                modifier =
                    Modifier.fillMaxWidth()
                        .clip(RcShape.roundedCorner(8f))
                        .background(0xFFECFDF5.toInt())
                        .border(1.5f, 8f, 0xFFA7F3D0.rcColor(), RcBorderShape.RoundedRectangle)
                        .onTouchDown { setValue(deep5OuterDown, deep5OuterDown + 1f) }
                        .onTouchUp { setValue(deep5OuterUp, deep5OuterUp + 1f) }
                        .onTouchCancel { setValue(deep5OuterCancel, deep5OuterCancel + 1f) }
                        .padding(12f),
                horizontal = RcHorizontalPositioning.Center,
                vertical = RcVerticalPositioning.Center,
            ) {
                Box(
                    modifier =
                        Modifier.fillMaxWidth(0.85f)
                            .height(44f)
                            .clip(RcShape.roundedCorner(6f))
                            .background(0xFF0D9488.toInt())
                            .onTouchDown { setValue(deep5ChildDown, deep5ChildDown + 1f) }
                            .onTouchUp { setValue(deep5ChildUp, deep5ChildUp + 1f) }
                            .onTouchCancel { setValue(deep5ChildCancel, deep5ChildCancel + 1f) },
                    horizontal = RcHorizontalPositioning.Center,
                    vertical = RcVerticalPositioning.Center,
                ) {
                    Text(
                        "Child (Press & Drag to Scroll)",
                        color = 0xFFFFFFFF.toInt(),
                        fontSize = 11.rsp,
                    )
                }
            }
            Text(
                text =
                    remoteText("Outer (D/U/C): ") +
                        createTextFromFloat(deep5OuterDown, 2, 0) +
                        remoteText("/") +
                        createTextFromFloat(deep5OuterUp, 2, 0) +
                        remoteText("/") +
                        createTextFromFloat(deep5OuterCancel, 2, 0) +
                        remoteText(" | Child: ") +
                        createTextFromFloat(deep5ChildDown, 2, 0) +
                        remoteText("/") +
                        createTextFromFloat(deep5ChildUp, 2, 0) +
                        remoteText("/") +
                        createTextFromFloat(deep5ChildCancel, 2, 0),
                color = 0xFF475569.toInt(),
                fontSize = 11.rsp,
                modifier = Modifier.padding(top = 2f),
            )
        }

        Spacer(Modifier.height(40f))
    }
}

@Suppress("RestrictedApiAndroidX")
@Composable
@Preview
public fun DslNestedInteractionDemoPreview() {
    RemoteDocumentPreview(RemoteDocument(dslNestedInteractionDemo()))
}
