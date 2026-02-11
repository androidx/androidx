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
import androidx.compose.remote.core.operations.layout.managers.RowLayout
import androidx.compose.remote.creation.RemoteComposeContext
import androidx.compose.remote.creation.RemoteComposeContextAndroid
import androidx.compose.remote.creation.RemoteComposeWriter
import androidx.compose.remote.creation.platform.AndroidxRcPlatformServices
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

@Composable
@Preview
fun RcFlowPreview() {
    RemoteDocPreview(RcFlow())
}

@Suppress("RestrictedApiAndroidX")
fun RcFlow(): RemoteComposeContext {
    return RemoteComposeContextAndroid(
        AndroidxRcPlatformServices(),
        7,
        RemoteComposeWriter.HTag(
            Header.DOC_PROFILES,
            RcProfiles.PROFILE_ANDROIDX or RcProfiles.PROFILE_EXPERIMENTAL,
        ),
        RemoteComposeWriter.HTag(Header.FEATURE_MEASURE_VERSION, 2),
    ) {
        root {
            column(Modifier.fillMaxSize().background(Color.WHITE).padding(60)) {
                flow(
                    Modifier.fillMaxWidth().background(Color.DKGRAY),
                    RowLayout.SPACE_EVENLY,
                    RowLayout.CENTER,
                ) {
                    box(
                        Modifier.background(Color.YELLOW)
                            .height(300)
                            .horizontalWeight(1f)
                            .widthIn(0f, 200f)
                    )
                    box(Modifier.background(Color.RED).size(300))
                    box(Modifier.background(Color.GREEN).size(300))
                    box(Modifier.background(Color.RED).size(200))
                }
            }
        }
    }
}
