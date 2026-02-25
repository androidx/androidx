/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.glance.appwidget.remotecompose

import androidx.compose.remote.creation.ExperimentalRemoteCreationApi
import androidx.compose.remote.creation.profile.Profile
import androidx.compose.remote.creation.profile.RcPlatformProfiles

internal object RemoteComposeConstants {

    object Text {
        const val DefaultWeight = 1f
        const val DefaultFontSize = 14f // TODO arbitrary choice of default font size
    }

    const val RemoteComposeVersion = 6

    @OptIn(ExperimentalRemoteCreationApi::class)
    @Suppress("RestrictedApiAndroidX")
    val GlanceRemoteComposeProfile: Profile
        get() = RcPlatformProfiles.WIDGETS_V6

    internal val DebugRemoteCompose = true // TODO: change to false before release b/483748434
}
