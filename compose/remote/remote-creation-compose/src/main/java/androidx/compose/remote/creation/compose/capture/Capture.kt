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
import androidx.compose.remote.core.Platform
import androidx.compose.remote.creation.RemoteComposeWriter
import androidx.compose.remote.creation.RemoteComposeWriterAndroid
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.state.AnimatedRemoteFloat
import androidx.compose.remote.creation.compose.state.BaseRemoteState
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.RemoteInt
import androidx.compose.remote.creation.profile.Profile
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Size

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public open class RemoteComposeCreationState {

    public val platform: Platform
    public var density: Float
    public val size: Size
    public val apiLevel: Int
    public val profiles: Int

    public val animCache: HashMap<Int, AnimatedRemoteFloat> = HashMap<Int, AnimatedRemoteFloat>()
    public val expressionCache: HashMap<Int, RemoteFloat> = HashMap<Int, RemoteFloat>()
    public val intExpressionCache: HashMap<Int, RemoteInt> = HashMap<Int, RemoteInt>()
    public var ready: Boolean = true
    public var document: RemoteComposeWriter
    public val remoteVariableToId: HashMap<BaseRemoteState, Int> = HashMap<BaseRemoteState, Int>()
    public val floatArrayCache: HashMap<BaseRemoteState, FloatArray> =
        HashMap<BaseRemoteState, FloatArray>()
    public val longArrayCache: HashMap<BaseRemoteState, LongArray> =
        HashMap<BaseRemoteState, LongArray>()

    public val namedState: HashMap<String, BaseRemoteState> = HashMap()

    public val time: MutableState<Long> = mutableStateOf(0L)

    public constructor(platform: Platform, density: Float, size: Size) {
        this.platform = platform
        this.density = density
        this.size = size
        this.apiLevel = CoreDocument.DOCUMENT_API_LEVEL
        this.profiles = 0
        document = RemoteComposeWriterAndroid(size.width.toInt(), size.height.toInt(), "", platform)
    }

    public constructor(
        platform: Platform,
        density: Float,
        size: Size,
        apiLevel: Int,
        profiles: Int,
    ) {
        this.platform = platform
        this.density = density
        this.size = size
        this.apiLevel = apiLevel
        this.profiles = profiles
        if (this.apiLevel == CoreDocument.DOCUMENT_API_LEVEL && this.profiles == 0) {
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

    public constructor(density: Float, size: Size, profile: Profile) {
        this.platform = profile.platform
        this.density = density
        this.size = size
        this.apiLevel = profile.apiLevel
        this.profiles = profile.operationsProfiles
        this.document = profile.create(size.width.toInt(), size.height.toInt(), "")
    }

    public open fun <T : BaseRemoteState> getOrCreateNamedState(
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
    RemoteComposeCreationState(platform = Platform.None, density = 1f, Size(1000f, 1000f)) {
    override fun <T : BaseRemoteState> getOrCreateNamedState(
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
    compositionLocalOf<RemoteComposeCreationState> { NoRemoteCompose() }

@RemoteComposable
@Composable
public inline fun <reified T : BaseRemoteState> rememberNamedState(
    name: String,
    domain: String,
    noinline function: () -> T,
): T {
    return LocalRemoteComposeCreationState.current.getOrCreateNamedState(
        T::class.java,
        name,
        domain,
        function,
    )
}
