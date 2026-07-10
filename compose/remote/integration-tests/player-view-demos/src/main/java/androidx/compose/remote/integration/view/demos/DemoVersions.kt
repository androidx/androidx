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
@file:Suppress("RestrictedApiAndroidX")

package androidx.compose.remote.integration.view.demos

import androidx.compose.remote.core.RcProfiles
import androidx.compose.remote.creation.CreationDisplayInfo
import androidx.compose.remote.creation.RemoteComposeWriterAndroid
import androidx.compose.remote.creation.platform.AndroidxRcPlatformServices
import androidx.compose.remote.creation.profile.Profile
import androidx.compose.remote.creation.profile.RcPlatformProfiles

object DemoVersions {
    val Baklava = 6
    val CinnamonBun = 7
    val CurrentStable = CinnamonBun

    val AndroidXExperimentalCinnamonBun: Profile =
        Profile(
            CinnamonBun,
            RcProfiles.PROFILE_ANDROIDX or RcProfiles.PROFILE_EXPERIMENTAL,
            AndroidxRcPlatformServices(),
        ) { creationDisplayInfo: CreationDisplayInfo?, profile: Profile?, callback: Any? ->
            RemoteComposeWriterAndroid(creationDisplayInfo!!, null, profile!!, callback)
        }

    val AndroidXCinnamonBun: Profile =
        Profile(CinnamonBun, RcProfiles.PROFILE_ANDROIDX, AndroidxRcPlatformServices()) {
            creationDisplayInfo: CreationDisplayInfo?,
            profile: Profile?,
            callback: Any? ->
            RemoteComposeWriterAndroid(creationDisplayInfo!!, null, profile!!, callback)
        }

    val AndroidXBaklava: Profile =
        Profile(Baklava, RcProfiles.PROFILE_ANDROIDX, AndroidxRcPlatformServices()) {
            creationDisplayInfo: CreationDisplayInfo?,
            profile: Profile?,
            callback: Any? ->
            RemoteComposeWriterAndroid(creationDisplayInfo!!, null, profile!!, callback)
        }

    val BaselineCinnamonBun: Profile =
        Profile(CinnamonBun, RcProfiles.PROFILE_BASELINE, AndroidxRcPlatformServices()) {
            creationDisplayInfo: CreationDisplayInfo?,
            profile: Profile?,
            callback: Any? ->
            RemoteComposeWriterAndroid(creationDisplayInfo!!, null, profile!!, callback)
        }

    val AndroidXCurrent: Profile = RcPlatformProfiles.ANDROIDX
}
