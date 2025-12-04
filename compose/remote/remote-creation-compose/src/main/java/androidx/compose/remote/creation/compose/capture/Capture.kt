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
@file:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)

package androidx.compose.remote.creation.compose.capture

import androidx.annotation.RestrictTo
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.RcPlatformServices
import androidx.compose.remote.creation.CreationDisplayInfo
import androidx.compose.remote.creation.RemoteComposeWriter
import androidx.compose.remote.creation.RemoteComposeWriterAndroid
import androidx.compose.remote.creation.compose.state.AnimatedRemoteFloat
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.RemoteInt
import androidx.compose.remote.creation.compose.state.RemoteState
import androidx.compose.remote.creation.profile.Profile
import androidx.compose.remote.creation.profile.RcPlatformProfiles
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Size

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public open class RemoteComposeCreationState {

    public val creationDisplayInfo: CreationDisplayInfo
    public val profile: Profile

    public val animCache: HashMap<Int, AnimatedRemoteFloat> = HashMap()
    public val expressionCache: HashMap<Int, RemoteFloat> = HashMap()
    public val intExpressionCache: HashMap<Int, RemoteInt> = HashMap()
    public var ready: Boolean = true
    public var document: RemoteComposeWriter
    public val remoteVariableToId: HashMap<RemoteState<*>, Int> = HashMap()
    public val floatArrayCache: HashMap<RemoteState<*>, FloatArray> = HashMap()
    public val longArrayCache: HashMap<RemoteState<*>, LongArray> = HashMap()

    public val namedState: HashMap<String, RemoteState<*>> = HashMap()

    public val time: MutableState<Long> = mutableStateOf(0L)

    public val platform: RcPlatformServices
        get() = profile.platform

    public constructor(
        creationDisplayInfo: CreationDisplayInfo,
        contentDescription: String?,
        profile: Profile,
    ) {
        this.creationDisplayInfo = creationDisplayInfo
        this.profile = profile
        document = profile.create(creationDisplayInfo, null) as RemoteComposeWriterAndroid
    }

    public constructor(
        creationDisplayInfo: CreationDisplayInfo,
        profile: Profile,
        writerCallback: WriterCallback?,
    ) {
        this.creationDisplayInfo = creationDisplayInfo
        this.profile = profile
        document = profile.create(creationDisplayInfo, writerCallback) as RemoteComposeWriterAndroid
    }

    public constructor(platform: RcPlatformServices, size: Size) {
        this.profile =
            Profile(
                CoreDocument.DOCUMENT_API_LEVEL,
                0,
                platform,
                { creationDisplayInfo, profile, callback ->
                    RemoteComposeWriterAndroid(creationDisplayInfo, null, profile, callback)
                },
            )
        this.creationDisplayInfo = CreationDisplayInfo(size.width.toInt(), size.height.toInt(), 1f)
        document = RemoteComposeWriterAndroid(size.width.toInt(), size.height.toInt(), "", platform)
    }

    public constructor(platform: RcPlatformServices, size: Size, apiLevel: Int, profiles: Int) {
        this.profile =
            Profile(
                apiLevel,
                profiles,
                platform,
                { creationDisplayInfo, profile, callback ->
                    RemoteComposeWriterAndroid(creationDisplayInfo, null, profile, callback)
                },
            )
        this.creationDisplayInfo = CreationDisplayInfo(size.width.toInt(), size.height.toInt(), 1f)
        if (apiLevel == CoreDocument.DOCUMENT_API_LEVEL && profiles == 0) {
            document =
                RemoteComposeWriterAndroid(size.width.toInt(), size.height.toInt(), "", platform)
        } else {
            document =
                RemoteComposeWriterAndroid(
                    size.width.toInt(),
                    size.height.toInt(),
                    "",
                    apiLevel,
                    profiles,
                    platform,
                )
        }
    }

    public constructor(
        creationDisplayInfo: CreationDisplayInfo,
        profile: Profile,
        document: RemoteComposeWriter,
    ) {
        this.creationDisplayInfo = creationDisplayInfo
        this.profile = profile
        this.document = document
    }

    public constructor(size: Size, profile: Profile) {
        this.profile = profile
        this.creationDisplayInfo = CreationDisplayInfo(size.width.toInt(), size.height.toInt(), 1f)
        this.document = profile.create(creationDisplayInfo, null)
    }

    public open fun <T : RemoteState<*>> getOrCreateNamedState(
        type: Class<T>,
        name: String,
        domain: String,
        function: () -> T,
    ): T {
        return type.cast(namedState.getOrPut("$domain:$name", function))!!
    }
}

// Density and Size should be taken from Compose in this mode
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class NoRemoteCompose :
    RemoteComposeCreationState(CreationDisplayInfo(1, 1, 1f), null, RcPlatformProfiles.ANDROIDX) {
    override fun <T : RemoteState<*>> getOrCreateNamedState(
        type: Class<T>,
        name: String,
        domain: String,
        function: () -> T,
    ): T {
        // no need to cache here
        return function()
    }
}

public val LocalRemoteComposeCreationState: ProvidableCompositionLocal<RemoteComposeCreationState> =
    compositionLocalOf {
        NoRemoteCompose()
    }
