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

package androidx.compose.remote.player.compose.embedded

import androidx.compose.remote.core.RcProfiles
import androidx.compose.remote.creation.CreationDisplayInfo
import androidx.compose.remote.creation.RemoteComposeWriterAndroid
import androidx.compose.remote.creation.platform.AndroidxRcPlatformServices
import androidx.compose.remote.creation.profile.Profile
import androidx.compose.remote.creation.profile.WidgetsProfileWriterV6

/**
 * Copies of `RcPlatformProfiles.WIDGETS_V6` and `WIDGETS_V7` that pass the writer callback through,
 * which the originals drop.
 *
 * TODO: Remove once `RcPlatformProfiles` passes the writer callback through.
 */
internal object LauncherWidgetTestProfiles {
    val WIDGETS_V6: Profile =
        Profile(6, 0, AndroidxRcPlatformServices()) { creationDisplayInfo, profile, callback ->
            CallbackWidgetsProfileWriterV6(creationDisplayInfo, profile, callback)
        }

    val WIDGETS_V7: Profile =
        Profile(7, RcProfiles.PROFILE_WIDGETS, AndroidxRcPlatformServices()) {
            creationDisplayInfo,
            profile,
            callback ->
            RemoteComposeWriterAndroid(creationDisplayInfo, null, profile, callback)
        }

    private class CallbackWidgetsProfileWriterV6(
        creationDisplayInfo: CreationDisplayInfo,
        profile: Profile,
        private val callback: Any?,
    ) : WidgetsProfileWriterV6(creationDisplayInfo, null, profile) {
        override fun getWriterCallback(): Any? = callback
    }
}
