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

package androidx.compose.remote.integration.view.demos.examples

import android.graphics.Color
import androidx.compose.remote.core.RcProfiles
import androidx.compose.remote.core.operations.Header
import androidx.compose.remote.creation.RemoteComposeContext
import androidx.compose.remote.creation.RemoteComposeContextAndroid
import androidx.compose.remote.creation.RemoteComposeWriter
import androidx.compose.remote.creation.min
import androidx.compose.remote.creation.platform.AndroidxRcPlatformServices
import androidx.compose.remote.creation.windowHeight
import androidx.compose.remote.creation.windowWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

@Composable
@Preview
fun RcRatioPreview() {
    RemoteDocPreview(RcRatio())
}

@Suppress("RestrictedApiAndroidX")
fun RcRatio(): RemoteComposeContext {
    return RemoteComposeContextAndroid(
        AndroidxRcPlatformServices(),
        7,
        RemoteComposeWriter.HTag(
            Header.DOC_PROFILES,
            RcProfiles.PROFILE_ANDROIDX or RcProfiles.PROFILE_EXPERIMENTAL,
        ),
    ) {
        val size = min(writer.windowWidth(), writer.windowHeight()).toFloat()
        root {
            // on fillMaxSize in col/row, computeMeasure not taken into account
            // on box, somehow needed to get things working
            column(Modifier.fillMaxSize().background(Color.YELLOW)) {
                // val size = min(ComponentWidth(), ComponentHeight()).toFloat()
                canvas(Modifier.size(size).background(Color.RED)) {
                    val w = ComponentWidth()
                    val h = ComponentHeight()
                    painter.setColor(Color.GREEN).commit()
                    drawLine(0f, 0f, w, h)
                    drawLine(0f, h, w, 0f)
                }
            }
        }
    }
}
