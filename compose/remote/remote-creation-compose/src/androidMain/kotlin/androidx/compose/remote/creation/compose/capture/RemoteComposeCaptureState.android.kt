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

@file:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@file:JvmName("RemoteComposeCaptureStateKt")
@file:JvmMultifileClass

package androidx.compose.remote.creation.compose.capture

import androidx.annotation.RestrictTo
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.RcPlatformServices
import androidx.compose.remote.creation.RemoteComposeWriterAndroid
import androidx.compose.remote.creation.compose.layout.toRemoteLayoutDirection
import androidx.compose.remote.creation.profile.Profile
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.LayoutDirection

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun RemoteComposeCaptureState(
    creationDisplayInfo: RemoteCreationDisplayInfo,
    profile: Profile,
    writerCallback: Any?,
    remoteDensity: RemoteDensity = RemoteDensity.from(creationDisplayInfo),
    layoutDirection: LayoutDirection,
): RemoteComposeCaptureState =
    RemoteComposeCaptureState(
        creationDisplayInfo = creationDisplayInfo,
        profile = profile,
        writerCallback = writerCallback,
        remoteDensity = remoteDensity,
        layoutDirection = layoutDirection.toRemoteLayoutDirection(),
    )

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun RemoteComposeCaptureState(
    platform: RcPlatformServices,
    size: Size,
): RemoteComposeCaptureState {
    val creationDisplayInfo =
        RemoteCreationDisplayInfo(size.width.toInt(), size.height.toInt(), 160, 1.0f)
    val profile =
        Profile(
            CoreDocument.DOCUMENT_API_LEVEL,
            0,
            platform,
            { displayInfo, p, callback ->
                RemoteComposeWriterAndroid(displayInfo, null, p, callback)
            },
        )
    val document = RemoteComposeWriterAndroid(size.width.toInt(), size.height.toInt(), "", platform)
    return RemoteComposeCaptureState(
        creationDisplayInfo = creationDisplayInfo,
        profile = profile,
        writer = document,
    )
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun RemoteComposeCaptureState(
    platform: RcPlatformServices,
    size: Size,
    apiLevel: Int,
    profiles: Int,
): RemoteComposeCaptureState {
    val creationDisplayInfo =
        RemoteCreationDisplayInfo(size.width.toInt(), size.height.toInt(), 160, 1f)
    val profile =
        Profile(
            apiLevel,
            profiles,
            platform,
            { displayInfo, p, callback ->
                RemoteComposeWriterAndroid(displayInfo, null, p, callback)
            },
        )
    val document =
        if (apiLevel == CoreDocument.DOCUMENT_API_LEVEL && profiles == 0) {
            RemoteComposeWriterAndroid(size.width.toInt(), size.height.toInt(), "", platform)
        } else {
            RemoteComposeWriterAndroid(
                size.width.toInt(),
                size.height.toInt(),
                "",
                apiLevel,
                profiles,
                platform,
            )
        }
    return RemoteComposeCaptureState(
        creationDisplayInfo = creationDisplayInfo,
        profile = profile,
        writer = document,
    )
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun RemoteComposeCaptureState(size: Size, profile: Profile): RemoteComposeCaptureState {
    val creationDisplayInfo =
        RemoteCreationDisplayInfo(size.width.toInt(), size.height.toInt(), 160, 1.0f)
    val document = profile.create(creationDisplayInfo.toCreationDisplayInfo(), null)
    return RemoteComposeCaptureState(
        creationDisplayInfo = creationDisplayInfo,
        profile = profile,
        writer = document,
    )
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun RemoteComposeCreationState(
    creationDisplayInfo: RemoteCreationDisplayInfo,
    profile: Profile,
    writerCallback: Any?,
    remoteDensity: RemoteDensity = RemoteDensity.from(creationDisplayInfo),
    layoutDirection: LayoutDirection,
): RemoteComposeCaptureState =
    RemoteComposeCaptureState(
        creationDisplayInfo,
        profile,
        writerCallback,
        remoteDensity,
        layoutDirection,
    )

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun RemoteComposeCreationState(
    creationDisplayInfo: RemoteCreationDisplayInfo,
    profile: Profile,
    writerEvents: RemoteWriterEvents?,
    remoteDensity: RemoteDensity = RemoteDensity.from(creationDisplayInfo),
    layoutDirection: LayoutDirection,
): RemoteComposeCaptureState =
    RemoteComposeCaptureState(
        creationDisplayInfo,
        profile,
        writerEvents,
        remoteDensity,
        layoutDirection,
    )

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun RemoteComposeCreationState(
    platform: RcPlatformServices,
    size: Size,
): RemoteComposeCaptureState = RemoteComposeCaptureState(platform, size)

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun RemoteComposeCreationState(
    platform: RcPlatformServices,
    size: Size,
    apiLevel: Int,
    profiles: Int,
): RemoteComposeCaptureState = RemoteComposeCaptureState(platform, size, apiLevel, profiles)

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun RemoteComposeCreationState(
    size: Size,
    profile: Profile,
): RemoteComposeCaptureState = RemoteComposeCaptureState(size, profile)
