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
import androidx.compose.remote.creation.dsl.RcProfile
import androidx.compose.remote.creation.dsl.RcRowHorizontalPositioning
import androidx.compose.remote.creation.dsl.RcVerticalPositioning
import androidx.compose.remote.creation.dsl.alignByBaseline
import androidx.compose.remote.creation.dsl.background
import androidx.compose.remote.creation.dsl.createRcBuffer
import androidx.compose.remote.creation.dsl.fillMaxSize
import androidx.compose.remote.creation.dsl.rsp
import androidx.compose.remote.creation.profile.RcPlatformProfiles

/**
 * DSL conversion of `examples/Text.kt` `RcTextDemo()`.
 *
 * Three [Row]s stacked z-order inside a [Box], each with its own vertical alignment
 * (Top/Center/Bottom) but using `alignByBaseline()` on each text so the differently-sized words
 * share a baseline within their row.
 *
 * Demonstrates:
 * - `Modifier.alignByBaseline()` in a row layout (B6 typed modifier).
 * - Typed `RcRowHorizontalPositioning.SpaceEvenly` and `RcVerticalPositioning.{Top,Center,Bottom}`.
 *
 * The autosize/min-max/overflow/hyphenation variants (`RcTextDemo2`, `RcTextDemo2b`) are skipped —
 * the DSL's `Text(...)` doesn't expose those parameters yet (see G17 in the cleanup ledger).
 */
@Suppress("RestrictedApiAndroidX")
public fun dslRcTextDemo(): ByteArray {
    return createRcBuffer(
        RcProfile(RcPlatformProfiles.ANDROIDX),
        RemoteComposeWriter.hTag(Header.DOC_WIDTH, 600),
        RemoteComposeWriter.hTag(Header.DOC_HEIGHT, 600),
        RemoteComposeWriter.hTag(Header.DOC_CONTENT_DESCRIPTION, "Demo"),
        experimental = true,
    ) {
        Box(modifier = Modifier.fillMaxSize().background(0xFFFFFF00.toInt())) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontal = RcRowHorizontalPositioning.SpaceEvenly,
                vertical = RcVerticalPositioning.Top,
            ) {
                Text("Hello", modifier = Modifier.alignByBaseline())
                Text("World", modifier = Modifier.alignByBaseline(), fontSize = 100.rsp)
                Text("the", modifier = Modifier.alignByBaseline(), fontSize = 12.rsp)
                Text("quick", modifier = Modifier.alignByBaseline(), fontSize = 64.rsp)
                Text("brown", modifier = Modifier.alignByBaseline(), fontSize = 72.rsp)
                Text("fox", modifier = Modifier.alignByBaseline())
            }
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontal = RcRowHorizontalPositioning.SpaceEvenly,
                vertical = RcVerticalPositioning.Center,
            ) {
                Text("Hello", modifier = Modifier.alignByBaseline())
                Text("World", modifier = Modifier.alignByBaseline(), fontSize = 100.rsp)
                Text("the", modifier = Modifier.alignByBaseline(), fontSize = 12.rsp)
                Text("quick", modifier = Modifier.alignByBaseline(), fontSize = 64.rsp)
                Text("brown", modifier = Modifier.alignByBaseline(), fontSize = 72.rsp)
                Text("fox", modifier = Modifier.alignByBaseline())
            }
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontal = RcRowHorizontalPositioning.SpaceEvenly,
                vertical = RcVerticalPositioning.Bottom,
            ) {
                Text("Hello", modifier = Modifier.alignByBaseline())
                Text("World", modifier = Modifier.alignByBaseline(), fontSize = 100.rsp)
                Text("the", modifier = Modifier.alignByBaseline(), fontSize = 12.rsp)
                Text("quick", modifier = Modifier.alignByBaseline(), fontSize = 64.rsp)
                Text("brown", modifier = Modifier.alignByBaseline(), fontSize = 72.rsp)
                Text("fox", modifier = Modifier.alignByBaseline())
            }
        }
    }
}
