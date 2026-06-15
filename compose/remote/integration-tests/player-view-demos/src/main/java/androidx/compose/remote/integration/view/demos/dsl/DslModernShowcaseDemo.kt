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

import androidx.compose.remote.creation.dsl.*
import androidx.compose.remote.creation.modifiers.RoundedRectShape
import androidx.compose.remote.creation.profile.RcPlatformProfiles
import androidx.compose.remote.player.core.RemoteDocument
import androidx.compose.remote.tooling.preview.RemoteDocumentPreview
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

/**
 * A comprehensive Integration Demo rigorously presenting the definitive collection of modern
 * RemoteCompose DSL additions:
 * 1. Macro Template Definition and Dynamic Inflation (`defineMacro`, `inflate`).
 * 2. Short-Circuit/RPN Logical Conditionals (`ifTrue`, `gt`, `lt`).
 * 3. Text Manipulation DSL (`merge`, `subtext`, `length`).
 * 4. `Modifier.graphicsLayer { ... }` builder lambdas.
 * 5. Layout Container Parity (`FitBox`, `Flow`, `StateLayout`, `Icon`).
 */
@Suppress("RestrictedApiAndroidX")
public fun dslModernShowcaseDemo(): ByteArray {
    return createRcBuffer(RcProfile(RcPlatformProfiles.ANDROIDX), experimental = true) {
        Column(
            modifier = Modifier.fillMaxSize().background(0xFF0F172A.toInt()).padding(24.rdp),
            horizontal = RcHorizontalPositioning.Center,
            vertical = RcColumnVerticalPositioning.Top,
        ) {
            // Header Title
            Text(
                text = "Premium Kotlin DSL Showcase",
                weight = RcWeight.Bold,
                modifier = Modifier.fillMaxWidth(),
                fontSize = 38.rsp,
                textAlign = RcTextAlign.Center,
                color = 0xFFF8FAFC.toInt(),
            )
            Spacer(Modifier.height(8.rdp))
            Text(
                text = "Fully Architected Modern Type-Safe DSL",
                weight = RcWeight.Normal,
                modifier = Modifier.fillMaxWidth(),
                fontSize = 22.rsp,
                textAlign = RcTextAlign.Center,
                color = 0xFF94A3B8.toInt(),
            )
            Spacer(Modifier.height(24.rdp))

            // ----------------------------------------------------------------------------
            // Feature 1: Reusable Remote Macro Templates
            // ----------------------------------------------------------------------------
            val bannerMacro =
                defineMacro("BannerCard", listOf("title", "bgColor")) { args ->
                    Box(
                        modifier =
                            Modifier.fillMaxWidth()
                                .height(100.rdp)
                                .clip(RoundedRectShape(24f, 24f, 24f, 24f))
                    ) {
                        Canvas(Modifier.fillMaxSize()) {
                            val w = componentWidth()
                            val h = componentHeight()
                            args["bgColor"]?.insertArgument()
                            drawRect(0.rf, 0.rf, w, h)

                            paint {
                                color(0xFFFFFFFF.toInt())
                                textSize(64f)
                            }
                            val titleText = RcText(args["title"]!!.paramId)
                            drawTextAnchored(titleText, w / 2f, h / 2f, 0.rf, 0.rf, 0)
                        }
                    }
                }

            // Inflate macro templates live!
            val helloId = remoteText("Reusable Macro Inflation 1")
            bannerMacro.inflate(mapOf("title" to helloId, "bgColor" to 0xFF3B82F6.toInt()))
            Spacer(Modifier.height(12.rdp))
            val worldId = remoteText("Reusable Macro Inflation 2")
            bannerMacro.inflate(mapOf("title" to worldId, "bgColor" to 0xFF10B981.toInt()))

            Spacer(Modifier.height(24.rdp))

            // ----------------------------------------------------------------------------
            // Feature 2: Advanced RPN Logic & Conditionals (`ifTrue`)
            // ----------------------------------------------------------------------------
            Box(
                modifier =
                    Modifier.fillMaxWidth()
                        .height(120.rdp)
                        .clip(RoundedRectShape(24f, 24f, 24f, 24f))
                        .background(0xFF1E293B.toInt())
            ) {
                Canvas(Modifier.fillMaxSize()) {
                    val w = componentWidth()
                    val sec = continuousSeconds() % 10f
                    val progressX = (sec / 10f) * (w - 100f)

                    paint { color(0xFF6366F1.toInt()) }
                    drawRoundRect(progressX, 20f.rf, progressX + 100f, 100f.rf, 20f.rf, 20f.rf)

                    paint {
                        color(0xFFFFFFFF.toInt())
                        textSize(48f)
                    }
                    ifTrue(sec lt 5f.rf) {
                        val txtLeft = remoteText("Status: < 5s")
                        drawTextAnchored(txtLeft, w / 2f, 60f.rf, 0f.rf, 0f.rf, 0)
                    }
                    ifTrue(sec gte 5f.rf) {
                        val txtRight = remoteText("Status: >= 5s")
                        drawTextAnchored(txtRight, w / 2f, 60f.rf, 0f.rf, 0f.rf, 0)
                    }
                }
            }

            Spacer(Modifier.height(24.rdp))

            // ----------------------------------------------------------------------------
            // Feature 3: String Manipulation DSL (`merge`, `subtext`, `length`)
            // ----------------------------------------------------------------------------
            Row(
                Modifier.fillMaxWidth().height(80.rdp),
                vertical = RcVerticalPositioning.Center,
                horizontal = RcRowHorizontalPositioning.SpaceBetween,
            ) {
                val baseTxt = remoteText("DeepMind")
                val extTxt = remoteText(" Support")
                val merged = baseTxt merge extTxt
                Text(
                    text = merged,
                    weight = RcWeight.Bold,
                    fontSize = 42.rsp,
                    color = 0xFFF59E0B.toInt(),
                )

                val sub = baseTxt.subtext(0f, 4f)
                Text(
                    text = sub,
                    weight = RcWeight.SemiBold,
                    fontSize = 42.rsp,
                    color = 0xFFEC4899.toInt(),
                )
            }

            Spacer(Modifier.height(24.rdp))

            // ----------------------------------------------------------------------------
            // Feature 4: `Modifier.graphicsLayer` & Parity Containers
            // ----------------------------------------------------------------------------
            Flow(
                modifier =
                    Modifier.fillMaxWidth().graphicsLayer {
                        scaleX = 1.05f
                        scaleY = 1.05f
                        alpha = 0.95f
                    }
            ) {
                FitBox(
                    Modifier.width(180.rdp)
                        .height(80.rdp)
                        .background(0xFF8B5CF6.toInt())
                        .clip(RoundedRectShape(16f, 16f, 16f, 16f))
                ) {
                    Text(
                        text = "FitBox Parity",
                        weight = RcWeight.Bold,
                        color = 0xFFFFFFFF.toInt(),
                        fontSize = 32.rsp,
                    )
                }
            }
        }
    }
}

@Suppress("RestrictedApiAndroidX")
@Composable
@Preview
private fun DslModernShowcaseDemoPreview() {
    RemoteDocumentPreview(RemoteDocument(dslModernShowcaseDemo()))
}
