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

package androidx.compose.remote.integration.demos.layout

import androidx.compose.remote.core.RcProfiles
import androidx.compose.remote.creation.compose.layout.RemoteAlignment
import androidx.compose.remote.creation.compose.layout.RemoteArrangement
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteFlowRow
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.background
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.RemoteDp
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.profile.Profile
import androidx.compose.remote.creation.profile.RcPlatformProfiles
import androidx.compose.remote.integration.demos.common.RemoteDemo
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Suppress("RestrictedApiAndroidX")
private val experimentalProfile =
    Profile(
        RcPlatformProfiles.ANDROIDX.apiLevel,
        RcPlatformProfiles.ANDROIDX.operationsProfiles or RcProfiles.PROFILE_EXPERIMENTAL,
        RcPlatformProfiles.ANDROIDX.platform,
        RcPlatformProfiles.ANDROIDX.profileFactory,
    )

@Suppress("RestrictedApiAndroidX")
@Composable
fun RemoteFlowRowDemo() {
    RemoteDemo(profile = experimentalProfile) {
        RemoteFlowRow(
            modifier =
                RemoteModifier.size(RemoteDp(200.dp)).background(RemoteColor(Color.LightGray)),
            maxItemsInEachRow = 3,
            maxLines = 3,
            horizontalArrangement =
                RemoteArrangement.spacedBy(
                    space = 8.rdp,
                    alignment = RemoteAlignment.CenterHorizontally,
                ),
        ) {
            repeat(15) { index ->
                val color = if (index % 2 == 0) Color(0xFF6200EE) else Color(0xFF03DAC6)
                RemoteBox(
                    modifier = RemoteModifier.size(RemoteDp(30.dp)).background(RemoteColor(color))
                )
            }
        }
    }
}
