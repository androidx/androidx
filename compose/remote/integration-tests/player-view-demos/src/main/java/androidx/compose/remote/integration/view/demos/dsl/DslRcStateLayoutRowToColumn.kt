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
import androidx.compose.remote.creation.dsl.RcColumnVerticalPositioning
import androidx.compose.remote.creation.dsl.RcHorizontalPositioning
import androidx.compose.remote.creation.dsl.RcProfile
import androidx.compose.remote.creation.dsl.RcRowHorizontalPositioning
import androidx.compose.remote.creation.dsl.RcVerticalPositioning
import androidx.compose.remote.creation.dsl.animationSpec
import androidx.compose.remote.creation.dsl.background
import androidx.compose.remote.creation.dsl.clip
import androidx.compose.remote.creation.dsl.createRcBuffer
import androidx.compose.remote.creation.dsl.fillMaxSize
import androidx.compose.remote.creation.dsl.fillMaxWidth
import androidx.compose.remote.creation.dsl.height
import androidx.compose.remote.creation.dsl.horizontalWeight
import androidx.compose.remote.creation.dsl.onClick
import androidx.compose.remote.creation.dsl.padding
import androidx.compose.remote.creation.dsl.rsp
import androidx.compose.remote.creation.dsl.size
import androidx.compose.remote.creation.dsl.width
import androidx.compose.remote.creation.modifiers.RoundedRectShape
import androidx.compose.remote.creation.profile.RcPlatformProfiles

/**
 * Kotlin DSL (androidx.compose.remote.creation.dsl) implementation of StateLayout Row to Column
 * demo.
 */
@Suppress("RestrictedApiAndroidX")
public fun dslRcStateLayoutRowToColumnDemo(): ByteArray {
    return createRcBuffer(
        RcProfile(RcPlatformProfiles.ANDROIDX),
        HTag(Header.DOC_DENSITY_BEHAVIOR, CoreDocument.DENSITY_BEHAVIOR_DP),
        experimental = true,
    ) {
        val stateVar = remoteNamedInteger("state", 0)

        Column(
            modifier = Modifier.fillMaxSize().padding(16f),
            horizontal = RcHorizontalPositioning.Center,
            vertical = RcColumnVerticalPositioning.Center,
        ) {
            StateLayout(stateIndex = stateVar, modifier = Modifier.fillMaxWidth().height(260f)) {
                // State 0: 3 boxes in a Row (fillMaxWidth)
                Row(
                    modifier = Modifier.fillMaxWidth().height(260f).background(0xFFD3D3D3.toInt()),
                    horizontal = RcRowHorizontalPositioning.SpaceEvenly,
                    vertical = RcVerticalPositioning.Center,
                ) {
                    Box(
                        modifier =
                            Modifier.animationSpec(100).size(60f).background(0xFFFF0000.toInt())
                    )
                    Box(
                        modifier =
                            Modifier.animationSpec(101)
                                .size(60f)
                                .horizontalWeight(1f)
                                .background(0xFF00FF00.toInt())
                    )
                    Box(
                        modifier =
                            Modifier.animationSpec(102).size(60f).background(0xFF0000FF.toInt())
                    )
                }

                // State 1: 3 boxes in a Column (fillMaxHeight)
                Column(
                    modifier = Modifier.fillMaxWidth().height(260f).background(0xFFD3D3D3.toInt()),
                    horizontal = RcHorizontalPositioning.Center,
                    vertical = RcColumnVerticalPositioning.SpaceEvenly,
                ) {
                    Box(
                        modifier =
                            Modifier.animationSpec(100).size(60f).background(0xFFFF0000.toInt())
                    )
                    Box(
                        modifier =
                            Modifier.animationSpec(101).size(120f).background(0xFF00FF00.toInt())
                    )
                    Box(
                        modifier =
                            Modifier.animationSpec(102).size(60f).background(0xFF0000FF.toInt())
                    )
                }
            }

            Spacer(Modifier.size(16f))

            // Interactive toggle button underneath
            Box(
                modifier =
                    Modifier.width(160f)
                        .height(48f)
                        .clip(RoundedRectShape(12f, 12f, 12f, 12f))
                        .background(0xFFA9A9A9.toInt())
                        .onClick { setValue(stateVar, (stateVar + 1) % 2) },
                horizontal = RcHorizontalPositioning.Center,
                vertical = RcVerticalPositioning.Center,
            ) {
                Text("Toggle State", color = 0xFFFFFFFF.toInt(), fontSize = 18.rsp)
            }
        }
    }
}
