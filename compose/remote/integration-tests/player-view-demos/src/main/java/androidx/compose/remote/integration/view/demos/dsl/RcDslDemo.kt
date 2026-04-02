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
import androidx.compose.remote.creation.dsl.RcHorizontalPositioning
import androidx.compose.remote.creation.dsl.RcProfile
import androidx.compose.remote.creation.dsl.RcVerticalPositioning
import androidx.compose.remote.creation.dsl.background
import androidx.compose.remote.creation.dsl.createRcBuffer
import androidx.compose.remote.creation.dsl.fillMaxSize
import androidx.compose.remote.creation.dsl.onClick
import androidx.compose.remote.creation.dsl.padding
import androidx.compose.remote.creation.dsl.rsp
import androidx.compose.remote.creation.dsl.size
import androidx.compose.remote.creation.profile.RcPlatformProfiles
import androidx.compose.remote.player.core.RemoteDocument
import androidx.compose.remote.tooling.preview.RemoteDocPreview
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

@Suppress("RestrictedApiAndroidX")
@Composable
@Preview
fun RcDslDemoPreview() {
    RemoteDocPreview(RemoteDocument(RcDslDemo()))
}

/** A simple demo using the new RemoteCompose DSL. */
@Suppress("RestrictedApiAndroidX")
fun RcDslDemo(): ByteArray {
    return createRcBuffer(
        RcProfile(RcPlatformProfiles.ANDROIDX),
        HTag(Header.DOC_DENSITY_BEHAVIOR, CoreDocument.DENSITY_BEHAVIOR_DP),
    ) {
        val count = addNamedFloat("count", 0f)
        val textVar = addNamedText("message", "Click me!")

        Column(
            modifier = Modifier.fillMaxSize().background(0xFFEEEEEE).padding(20f),
            horizontal = RcHorizontalPositioning.Center,
        ) {
            Text("Hello from New DSL!", fontSize = 24.rsp, color = 0xFF333333.toInt())

            Box(
                modifier =
                    Modifier.size(200f, 100f).background(0xFFFF0000).padding(10f).onClick {
                        setValue(count, 1f)
                        setValue(textVar, "Button Clicked!")
                    }
            ) {
                Text("Click Box", color = 0xFFFFFFFF)
            }

            Text(textVar, fontSize = 36.rsp)
            Row(
                modifier = Modifier.size(300f, 100f).background(0xFF00FF00),
                vertical = RcVerticalPositioning.Center,
            ) {
                Text("Row Item 1")
                Text("Row Item 2", modifier = Modifier.padding(start = 20f))
            }

            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = width
                val h = height
                paint.setColor(0xFFFF0000.toInt()).commit()
                drawLine(0.rf, 0.rf, w, h)
                drawLine(0.rf, h, w, 0.rf)
                drawCircle(w / 2f, h / 2f, (w / 4f))
            }
        }
    }
}
