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
@file:OptIn(ExperimentalRemoteCreationComposeApi::class)

package androidx.compose.remote.creation.compose.capture

import androidx.annotation.RestrictTo
import androidx.collection.MutableIntObjectMap
import androidx.collection.MutableObjectIntMap
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.RcPlatformServices
import androidx.compose.remote.core.operations.Utils
import androidx.compose.remote.creation.CreationDisplayInfo
import androidx.compose.remote.creation.RemoteComposeWriter
import androidx.compose.remote.creation.RemoteComposeWriterAndroid
import androidx.compose.remote.creation.compose.ExperimentalRemoteCreationComposeApi
import androidx.compose.remote.creation.compose.state.AnimatedRemoteFloat
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.RemoteInt
import androidx.compose.remote.creation.compose.state.RemoteState
import androidx.compose.remote.creation.compose.state.RemoteStateScope
import androidx.compose.remote.creation.profile.Profile
import androidx.compose.remote.creation.profile.RcPlatformProfiles
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.ui.geometry.Size

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public open class RemoteComposeCreationState : RemoteStateScope {

    override val creationState: RemoteComposeCreationState
        get() = this

    public val creationDisplayInfo: CreationDisplayInfo
    public val profile: Profile
    public override lateinit var remoteDensity: RemoteDensity

    public val animCache: MutableIntObjectMap<AnimatedRemoteFloat> = MutableIntObjectMap()
    public val expressionCache: MutableIntObjectMap<RemoteFloat> = MutableIntObjectMap()
    public val intExpressionCache: MutableIntObjectMap<RemoteInt> = MutableIntObjectMap()
    public var ready: Boolean = true
    override lateinit var document: RemoteComposeWriter
    public val remoteVariableToId: MutableObjectIntMap<RemoteState<*>> = MutableObjectIntMap()
    public val floatArrayCache: HashMap<RemoteState<*>, FloatArray> = HashMap()
    public val longArrayCache: HashMap<RemoteState<*>, LongArray> = HashMap()

    public val namedState: HashMap<String, RemoteState<*>> = HashMap()

    public val time: MutableState<Long> = mutableLongStateOf(0L)
    private val textFromFloatCache = MutableObjectIntMap<TextFromFloatParams>()

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
        this.remoteDensity = RemoteDensity.from(creationDisplayInfo)
    }

    public constructor(
        creationDisplayInfo: CreationDisplayInfo,
        profile: Profile,
        writerEvents: WriterEvents?,
        remoteDensity: RemoteDensity = RemoteDensity.from(creationDisplayInfo),
    ) {
        this.creationDisplayInfo = creationDisplayInfo
        this.profile = profile
        document = profile.create(creationDisplayInfo, writerEvents) as RemoteComposeWriterAndroid
        this.remoteDensity = remoteDensity
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
        this.creationDisplayInfo = CreationDisplayInfo(size.width.toInt(), size.height.toInt(), 160)
        document = RemoteComposeWriterAndroid(size.width.toInt(), size.height.toInt(), "", platform)
        this.remoteDensity = RemoteDensity.from(creationDisplayInfo)
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
        this.creationDisplayInfo = CreationDisplayInfo(size.width.toInt(), size.height.toInt(), 160)
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
        this.remoteDensity = RemoteDensity.from(creationDisplayInfo)
    }

    public constructor(
        creationDisplayInfo: CreationDisplayInfo,
        profile: Profile,
        writer: RemoteComposeWriter,
    ) {
        this.creationDisplayInfo = creationDisplayInfo
        this.profile = profile
        this.document = writer
        this.remoteDensity = RemoteDensity.from(creationDisplayInfo)
    }

    public constructor(size: Size, profile: Profile) {
        this.profile = profile
        this.creationDisplayInfo = CreationDisplayInfo(size.width.toInt(), size.height.toInt(), 160)
        this.document = profile.create(creationDisplayInfo, null)
        this.remoteDensity = RemoteDensity.from(creationDisplayInfo)
    }

    public open fun <T : RemoteState<*>> getOrCreateNamedState(
        type: Class<T>,
        name: String,
        domain: String,
        function: () -> T,
    ): T {
        return type.cast(namedState.getOrPut("$domain:$name", function))!!
    }

    internal data class TextFromFloatParams(
        val id: Int,
        val before: Int,
        val after: Int,
        val flags: Int,
    )

    internal fun createTextFromFloat(params: TextFromFloatParams): Int =
        textFromFloatCache.getOrPut(params) {
            document.createTextFromFloat(
                Utils.asNan(params.id),
                params.before,
                params.after,
                params.flags,
            )
        }
}

// Density and Size should be taken from Compose in this mode
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class NoRemoteCompose :
    RemoteComposeCreationState(CreationDisplayInfo(1, 1, 160), null, RcPlatformProfiles.ANDROIDX) {
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
