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

package androidx.compose.remote.creation.compose.capture

import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.RcPlatformServices
import androidx.compose.remote.creation.RemoteComposeWriterAndroid
import androidx.compose.remote.creation.platform.AndroidxRcPlatformServices
import androidx.compose.remote.creation.profile.Profile

internal fun createTestCreationState(
    width: Int = 1,
    height: Int = 1,
    platform: RcPlatformServices = AndroidxRcPlatformServices(),
    apiLevel: Int = CoreDocument.DOCUMENT_API_LEVEL,
    profiles: Int = 0,
): RemoteComposeCaptureState {
    val displayInfo = RemoteCreationDisplayInfo(width, height, 160, 1f)
    val profile =
        Profile(
            apiLevel,
            profiles,
            platform,
            { dInfo, p, callback -> RemoteComposeWriterAndroid(dInfo, null, p, callback) },
        )
    val document =
        if (apiLevel == CoreDocument.DOCUMENT_API_LEVEL && profiles == 0) {
            RemoteComposeWriterAndroid(width, height, "", platform)
        } else {
            RemoteComposeWriterAndroid(width, height, "", apiLevel, profiles, platform)
        }
    return RemoteComposeCreationState(
        creationDisplayInfo = displayInfo,
        profile = profile,
        writer = document,
    )
}

internal fun createTestCreationState(
    width: Int,
    height: Int,
    profile: Profile,
): RemoteComposeCaptureState {
    val displayInfo = RemoteCreationDisplayInfo(width, height, 160, 1f)
    val document = profile.create(displayInfo.toCreationDisplayInfo(), null)
    return RemoteComposeCreationState(
        creationDisplayInfo = displayInfo,
        profile = profile,
        writer = document,
    )
}
