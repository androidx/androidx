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
import androidx.compose.remote.creation.profile.RcPlatformProfiles
import androidx.compose.remote.player.core.RemoteDocument
import androidx.compose.remote.tooling.preview.RemoteDocPreview
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import kotlin.math.sin

@Suppress("RestrictedApiAndroidX")
@Composable
@Preview
fun DslDemoGraphsPreview() {
    RemoteDocPreview(RemoteDocument(dslDemoGraphs()))
}

@Suppress("RestrictedApiAndroidX")
@Composable
@Preview
fun DslDemoGraphs2Preview() {
    RemoteDocPreview(RemoteDocument(dslDemoGraphs2()))
}

@Suppress("RestrictedApiAndroidX")
fun dslDemoGraphs(): ByteArray {
    val data = FloatArray(32) { x -> sin(x / 3.14f) + 0.5f }

    return createRcBuffer(RcProfile(RcPlatformProfiles.ANDROIDX), experimental = true) {
        val values = remoteFloatArray(data)

        Box(modifier = Modifier.fillMaxSize()) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = componentWidth()
                val h = componentHeight()

                val plot = DataPlot(values)
                rcPlotXY(10f.rf, 10f.rf, w - 10f.rf, h - 10f.rf, plot)
            }
        }
    }
}

@Suppress("RestrictedApiAndroidX")
fun dslDemoGraphs2(): ByteArray {
    return createRcBuffer(RcProfile(RcPlatformProfiles.ANDROIDX), experimental = true) {
        Box(modifier = Modifier.fillMaxSize()) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = componentWidth()
                val h = componentHeight()
                beginGlobal()

                val scale = abs((sin(continuousSeconds()) + 1.5f.rf) * 10f.rf).flush()
                val equ = rFun { x ->
                    min(scale, 15f.rf) * sin(x * 0.3f.rf + continuousSeconds()) * sin(x * 7f.rf)
                }

                val function = FunctionPlot(equ, (-10f).rf, 10f.rf, (-1f).rf * scale, scale)
                rcPlotXY(10f.rf, 10f.rf, w, h, function)
            }
        }
    }
}
