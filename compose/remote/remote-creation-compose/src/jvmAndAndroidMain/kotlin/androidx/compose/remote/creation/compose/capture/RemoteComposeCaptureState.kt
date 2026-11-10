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
@file:JvmName("RemoteComposeCaptureStateKt")

package androidx.compose.remote.creation.compose.capture

import androidx.annotation.RestrictTo
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.RcPlatformServices
import androidx.compose.remote.creation.RemoteComposeWriter
import androidx.compose.remote.creation.compose.layout.RemoteComponentToken
import androidx.compose.remote.creation.compose.layout.RemoteLayoutDirection
import androidx.compose.remote.creation.compose.state.RemoteStateScope
import androidx.compose.remote.creation.profile.Profile
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableLongStateOf

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public open class RemoteComposeCaptureState : RemoteComposeCreationState, RemoteStateScope {

    override val parentScope: RemoteComposeCaptureState
        get() = this

    public override fun resolveComponentToken(): RemoteComponentToken? =
        super<RemoteComposeCreationState>.resolveComponentToken()

    public override var document: RemoteComposeWriter
        get() = super<RemoteComposeCreationState>.document
        set(value) {
            super<RemoteComposeCreationState>.document = value
        }

    public val creationDisplayInfo: RemoteCreationDisplayInfo
    public val profile: Profile
    public final override var layoutDirection: RemoteLayoutDirection

    public final override val densityBehavior: RemoteDensityBehavior

    public val time: MutableState<Long> = mutableLongStateOf(0L)

    public val platform: RcPlatformServices
        get() = profile.platform

    public constructor(
        creationDisplayInfo: RemoteCreationDisplayInfo,
        profile: Profile,
        writerCallback: Any?,
        remoteDensity: RemoteDensity = RemoteDensity.from(creationDisplayInfo),
        layoutDirection: RemoteLayoutDirection = RemoteLayoutDirection.Ltr,
    ) : super() {
        this.creationDisplayInfo = creationDisplayInfo
        this.profile = profile
        document = profile.create(creationDisplayInfo.toCreationDisplayInfo(), writerCallback)
        this.remoteDensity = remoteDensity
        this.layoutDirection = layoutDirection
        this.densityBehavior = creationDisplayInfo.densityBehavior
    }

    public constructor(
        creationDisplayInfo: RemoteCreationDisplayInfo,
        contentDescription: String?,
        profile: Profile,
    ) : super() {
        this.creationDisplayInfo = creationDisplayInfo
        this.profile = profile
        document = profile.create(creationDisplayInfo.toCreationDisplayInfo(), null)
        this.remoteDensity = RemoteDensity.from(creationDisplayInfo)
        this.layoutDirection = RemoteLayoutDirection.Ltr
        this.densityBehavior = creationDisplayInfo.densityBehavior
    }

    public constructor(
        creationDisplayInfo: RemoteCreationDisplayInfo,
        profile: Profile,
        writer: RemoteComposeWriter,
    ) : super() {
        this.creationDisplayInfo = creationDisplayInfo
        this.profile = profile
        this.document = writer
        this.remoteDensity = RemoteDensity.from(creationDisplayInfo)
        this.layoutDirection = RemoteLayoutDirection.Ltr
        this.densityBehavior = creationDisplayInfo.densityBehavior
    }
}

@get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public val RemoteComposeCreationState.creationDisplayInfo: RemoteCreationDisplayInfo
    get() = (this as RemoteComposeCaptureState).creationDisplayInfo

@get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public val RemoteComposeCreationState.profile: Profile
    get() = (this as RemoteComposeCaptureState).profile

@get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public val RemoteComposeCreationState.platform: RcPlatformServices
    get() = profile.platform

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun RemoteComposeCreationState(
    creationDisplayInfo: RemoteCreationDisplayInfo,
    profile: Profile,
    writerCallback: Any?,
    remoteDensity: RemoteDensity = RemoteDensity.from(creationDisplayInfo),
    layoutDirection: RemoteLayoutDirection = RemoteLayoutDirection.Ltr,
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
    contentDescription: String?,
    profile: Profile,
): RemoteComposeCaptureState =
    RemoteComposeCaptureState(
        creationDisplayInfo,
        contentDescription,
        profile,
    )

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun RemoteComposeCreationState(
    creationDisplayInfo: RemoteCreationDisplayInfo,
    profile: Profile,
    writer: RemoteComposeWriter,
): RemoteComposeCaptureState = RemoteComposeCaptureState(creationDisplayInfo, profile, writer)

// Density and Size should be taken from Compose in this mode
internal class NoRemoteCompose :
    RemoteComposeCaptureState(
        creationDisplayInfo = RemoteCreationDisplayInfo(1, 1, 160, 1.0f),
        contentDescription = null,
        profile =
            Profile(
                CoreDocument.DOCUMENT_API_LEVEL,
                0,
                RcPlatformServices.None,
                { creationDisplayInfo, profile, callback ->
                    RemoteComposeWriter(creationDisplayInfo, null, profile, callback)
                },
            ),
    )

@get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public val LocalRemoteComposeCreationState: ProvidableCompositionLocal<RemoteComposeCaptureState> =
    compositionLocalOf {
        NoRemoteCompose()
    }
