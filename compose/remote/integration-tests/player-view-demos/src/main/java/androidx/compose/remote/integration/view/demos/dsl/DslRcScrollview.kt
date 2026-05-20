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
import androidx.compose.remote.creation.RemoteComposeWriter
import androidx.compose.remote.creation.dsl.Modifier
import androidx.compose.remote.creation.dsl.RcBorderShape
import androidx.compose.remote.creation.dsl.RcColumnVerticalPositioning
import androidx.compose.remote.creation.dsl.RcHorizontalPositioning
import androidx.compose.remote.creation.dsl.RcProfile
import androidx.compose.remote.creation.dsl.background
import androidx.compose.remote.creation.dsl.border
import androidx.compose.remote.creation.dsl.createRcBuffer
import androidx.compose.remote.creation.dsl.fillMaxSize
import androidx.compose.remote.creation.dsl.fillParentMaxHeight
import androidx.compose.remote.creation.dsl.fillParentMaxSize
import androidx.compose.remote.creation.dsl.fillParentMaxWidth
import androidx.compose.remote.creation.dsl.padding
import androidx.compose.remote.creation.dsl.rcColor
import androidx.compose.remote.creation.dsl.verticalScroll
import androidx.compose.remote.creation.dsl.width
import androidx.compose.remote.creation.profile.RcPlatformProfiles

/**
 * DSL conversion of `examples/RcScrollview.kt`.
 *
 * Demonstrates `Modifier.verticalScroll()` + nested column with `SpaceEvenly` vertical
 * arrangement + `fillParentMax*` modifiers. Uses the typed `RcBorderShape.Rectangle` border.
 */
@Suppress("RestrictedApiAndroidX")
public fun dslRcScrollview(): ByteArray {
    return createRcBuffer(
        RcProfile(RcPlatformProfiles.ANDROIDX),
        RemoteComposeWriter.hTag(Header.FEATURE_MEASURE_VERSION, 2),
        experimental = true,
    ) {
        Column(modifier = Modifier.fillMaxSize().background(0xFFFFFFFF.toInt()).padding(60f)) {
            Column(
                modifier = Modifier.fillMaxSize().background(0xFF444444.toInt()).verticalScroll(),
                horizontal = RcHorizontalPositioning.Center,
                vertical = RcColumnVerticalPositioning.SpaceEvenly,
            ) {
                Box(
                    modifier =
                        Modifier.padding(8f)
                            .border(20.rf, 0.rf, 0xFF000000.rcColor(), RcBorderShape.Rectangle)
                            .background(0xFFFF0000.toInt())
                            .fillParentMaxHeight(0.5f)
                            .width(100f)
                ) {}
                Box(
                    modifier =
                        Modifier.padding(8f)
                            .border(20f, 0f, 0xFF000000.rcColor(), RcBorderShape.Rectangle)
                            .background(0xFF00FF00.toInt())
                            .fillParentMaxSize(0.5f)
                ) {}
                Box(
                    modifier =
                        Modifier.padding(8f)
                            .border(20f, 0f, 0xFF000000.rcColor(), RcBorderShape.Rectangle)
                            .background(0xFFFFFF00.toInt())
                            .fillParentMaxWidth(0.5f)
                            .fillParentMaxHeight()
                ) {}
                Box(
                    modifier =
                        Modifier.padding(8f)
                            .border(20f, 0f, 0xFF000000.rcColor(), RcBorderShape.Rectangle)
                            .background(0xFF0000FF.toInt())
                            .fillParentMaxHeight()
                            .width(100f)
                ) {}
            }
        }
    }
}
