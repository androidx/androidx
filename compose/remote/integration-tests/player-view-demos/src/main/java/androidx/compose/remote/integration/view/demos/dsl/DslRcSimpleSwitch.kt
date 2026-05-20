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
import androidx.compose.remote.creation.dsl.RcHorizontalPositioning
import androidx.compose.remote.creation.dsl.RcProfile
import androidx.compose.remote.creation.dsl.RcVerticalPositioning
import androidx.compose.remote.creation.dsl.background
import androidx.compose.remote.creation.dsl.clip
import androidx.compose.remote.creation.dsl.createRcBuffer
import androidx.compose.remote.creation.dsl.fillMaxSize
import androidx.compose.remote.creation.dsl.height
import androidx.compose.remote.creation.dsl.onClick
import androidx.compose.remote.creation.dsl.padding
import androidx.compose.remote.creation.dsl.rsp
import androidx.compose.remote.creation.dsl.size
import androidx.compose.remote.creation.dsl.width
import androidx.compose.remote.creation.modifiers.RoundedRectShape
import androidx.compose.remote.creation.profile.RcPlatformProfiles

/**
 * DSL conversion of `examples/RcSimpleSwitchDemo.kt`.
 *
 * **Showcases the new RcInteger arithmetic (item B5):** The original built the toggle expression
 * with raw opcodes — `integerExpression(checked, 1L, L_ADD, 2, L_MOD)`. In the DSL it's idiomatic
 * Kotlin: `(checked + 1) % 2`.
 *
 * Also showcases:
 * - `StateLayout(stateIndex: RcInteger)` for switch on/off rendering
 * - `remoteArrayOf("OFF", "ON")` returning `RcTextList`
 * - `textLookup(RcTextList, RcInteger)` for label-by-state-index
 * - Typed `onClick { setValue(target, expression) }` (B7)
 */
@Suppress("RestrictedApiAndroidX")
public fun dslRcSimpleSwitchDemo(): ByteArray {
    return createRcBuffer(RcProfile(RcPlatformProfiles.ANDROIDX), experimental = true) {
        val checked = remoteNamedInteger("checked", 0)

        Column(
            modifier = Modifier.fillMaxSize().padding(20f),
            horizontal = RcHorizontalPositioning.Center,
        ) {
            Text("Simple Switch Demo", fontSize = 30.rsp)

            Box(modifier = Modifier.size(20f)) {}

            Box(modifier = Modifier.padding(4f).onClick { setValue(checked, (checked + 1) % 2) }) {
                StateLayout(stateIndex = checked, modifier = Modifier) {
                    // OFF state (knob on the left, gray track).
                    Box(
                        modifier =
                            Modifier.width(60f)
                                .height(36f)
                                .clip(RoundedRectShape(20f, 20f, 20f, 20f))
                                .background(0xFF888888.toInt())
                                .padding(8f),
                        horizontal = RcHorizontalPositioning.Start,
                        vertical = RcVerticalPositioning.Center,
                    ) {
                        Box(
                            modifier =
                                Modifier.size(20f)
                                    .clip(RoundedRectShape(10f, 10f, 10f, 10f))
                                    .background(0xFFFFFFFF.toInt())
                        ) {}
                    }

                    // ON state (knob on the right, blue track).
                    Box(
                        modifier =
                            Modifier.width(60f)
                                .height(36f)
                                .clip(RoundedRectShape(20f, 20f, 20f, 20f))
                                .background(0xFF0000FF.toInt())
                                .padding(2f),
                        horizontal = RcHorizontalPositioning.End,
                        vertical = RcVerticalPositioning.Center,
                    ) {
                        Box(
                            modifier =
                                Modifier.size(32f)
                                    .clip(RoundedRectShape(16f, 16f, 16f, 16f))
                                    .background(0xFFFFFFFF.toInt())
                        ) {}
                    }
                }
            }

            Box(modifier = Modifier.size(20f)) {}

            // Label that follows the switch state.
            val labels = remoteArrayOf("OFF", "ON")
            Text(textLookup(labels, checked), fontSize = 24.rsp)
        }
    }
}
