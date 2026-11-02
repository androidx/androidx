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
import androidx.compose.remote.creation.RemoteComposeWriter
import androidx.compose.remote.creation.RemoteComposeWriterAndroid
import androidx.compose.remote.creation.compose.layout.RemoteComponentToken
import androidx.compose.remote.creation.compose.layout.RemoteLayoutDirection
import androidx.compose.remote.creation.compose.layout.toComposeLayoutDirection
import androidx.compose.remote.creation.compose.layout.toRemoteLayoutDirection
import androidx.compose.remote.creation.compose.state.RemoteStateScope
import androidx.compose.remote.creation.profile.Profile
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.unit.LayoutDirection

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
    public final override var remoteLayoutDirection: RemoteLayoutDirection
    public final override val layoutDirection: LayoutDirection
        get() = remoteLayoutDirection.toComposeLayoutDirection()

    public final override val densityBehavior: RemoteDensityBehavior
    internal var platformImageProvider: PlatformImageProvider = AndroidPlatformImageProvider

    public val time: MutableState<Long> = mutableLongStateOf(0L)

    public val platform: RcPlatformServices
        get() = profile.platform

    public constructor(
        creationDisplayInfo: RemoteCreationDisplayInfo,
        profile: Profile,
        writerCallback: Any?,
        remoteDensity: RemoteDensity = RemoteDensity.from(creationDisplayInfo),
        layoutDirection: RemoteLayoutDirection = RemoteLayoutDirection.Ltr,
        platformImageProvider: PlatformImageProvider = AndroidPlatformImageProvider,
    ) : super() {
        this.creationDisplayInfo = creationDisplayInfo
        this.profile = profile
        document = profile.create(creationDisplayInfo.toCreationDisplayInfo(), writerCallback)
        this.remoteDensity = remoteDensity
        this.remoteLayoutDirection = layoutDirection
        this.densityBehavior = creationDisplayInfo.densityBehavior
        this.platformImageProvider = platformImageProvider
    }

    public constructor(
        creationDisplayInfo: RemoteCreationDisplayInfo,
        profile: Profile,
        writerCallback: Any?,
        remoteDensity: RemoteDensity = RemoteDensity.from(creationDisplayInfo),
        layoutDirection: LayoutDirection,
        platformImageProvider: PlatformImageProvider = AndroidPlatformImageProvider,
    ) : this(
        creationDisplayInfo,
        profile,
        writerCallback,
        remoteDensity,
        layoutDirection.toRemoteLayoutDirection(),
        platformImageProvider,
    )

    public constructor(
        creationDisplayInfo: RemoteCreationDisplayInfo,
        contentDescription: String?,
        profile: Profile,
        platformImageProvider: PlatformImageProvider = AndroidPlatformImageProvider,
    ) : super() {
        this.creationDisplayInfo = creationDisplayInfo
        this.profile = profile
        document = profile.create(creationDisplayInfo.toCreationDisplayInfo(), null)
        this.remoteDensity = RemoteDensity.from(creationDisplayInfo)
        this.remoteLayoutDirection = RemoteLayoutDirection.Ltr
        this.densityBehavior = creationDisplayInfo.densityBehavior
        this.platformImageProvider = platformImageProvider
    }

    public constructor(platform: RcPlatformServices, size: Size) : super() {
        this.profile =
            Profile(
                CoreDocument.DOCUMENT_API_LEVEL,
                0,
                platform,
                { creationDisplayInfo, profile, callback ->
                    RemoteComposeWriterAndroid(creationDisplayInfo, null, profile, callback)
                },
            )
        this.creationDisplayInfo =
            RemoteCreationDisplayInfo(size.width.toInt(), size.height.toInt(), 160, 1.0f)
        document = RemoteComposeWriterAndroid(size.width.toInt(), size.height.toInt(), "", platform)
        this.remoteDensity = RemoteDensity.from(creationDisplayInfo)
        this.remoteLayoutDirection = RemoteLayoutDirection.Ltr
        this.densityBehavior = creationDisplayInfo.densityBehavior
    }

    public constructor(
        platform: RcPlatformServices,
        size: Size,
        apiLevel: Int,
        profiles: Int,
    ) : super() {
        this.profile =
            Profile(
                apiLevel,
                profiles,
                platform,
                { creationDisplayInfo, profile, callback ->
                    RemoteComposeWriterAndroid(creationDisplayInfo, null, profile, callback)
                },
            )
        this.creationDisplayInfo =
            RemoteCreationDisplayInfo(size.width.toInt(), size.height.toInt(), 160, 1f)
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
        this.remoteLayoutDirection = RemoteLayoutDirection.Ltr
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
        this.remoteLayoutDirection = RemoteLayoutDirection.Ltr
        this.densityBehavior = creationDisplayInfo.densityBehavior
    }

    public constructor(size: Size, profile: Profile) : super() {
        this.profile = profile
        this.creationDisplayInfo =
            RemoteCreationDisplayInfo(size.width.toInt(), size.height.toInt(), 160, 1.0f)
        this.document = profile.create(creationDisplayInfo.toCreationDisplayInfo(), null)
        this.remoteDensity = RemoteDensity.from(creationDisplayInfo)
        this.remoteLayoutDirection = RemoteLayoutDirection.Ltr
        this.densityBehavior = creationDisplayInfo.densityBehavior
    }
}

public fun RemoteComposeCreationState.addBitmap(image: ImageBitmap): Int =
    (this as? RemoteComposeCaptureState)?.platformImageProvider?.addBitmap(document, image)
        ?: document.addBitmap(image.asAndroidBitmap())

public fun RemoteComposeCreationState.addNamedBitmap(name: String, image: ImageBitmap): Int =
    (this as? RemoteComposeCaptureState)
        ?.platformImageProvider
        ?.addNamedBitmap(
            document,
            name,
            image,
        ) ?: document.addNamedBitmap(name, image.asAndroidBitmap())

public val RemoteComposeCreationState.creationDisplayInfo: RemoteCreationDisplayInfo
    get() = (this as RemoteComposeCaptureState).creationDisplayInfo

public val RemoteComposeCreationState.profile: Profile
    get() = (this as RemoteComposeCaptureState).profile

public val RemoteComposeCreationState.platform: RcPlatformServices
    get() = profile.platform

public fun RemoteComposeCreationState(
    creationDisplayInfo: RemoteCreationDisplayInfo,
    profile: Profile,
    writerCallback: Any?,
    remoteDensity: RemoteDensity = RemoteDensity.from(creationDisplayInfo),
    layoutDirection: RemoteLayoutDirection = RemoteLayoutDirection.Ltr,
    platformImageProvider: PlatformImageProvider = AndroidPlatformImageProvider,
): RemoteComposeCaptureState =
    RemoteComposeCaptureState(
        creationDisplayInfo,
        profile,
        writerCallback,
        remoteDensity,
        layoutDirection,
        platformImageProvider,
    )

public fun RemoteComposeCreationState(
    creationDisplayInfo: RemoteCreationDisplayInfo,
    profile: Profile,
    writerCallback: Any?,
    remoteDensity: RemoteDensity = RemoteDensity.from(creationDisplayInfo),
    layoutDirection: LayoutDirection,
    platformImageProvider: PlatformImageProvider = AndroidPlatformImageProvider,
): RemoteComposeCaptureState =
    RemoteComposeCaptureState(
        creationDisplayInfo,
        profile,
        writerCallback,
        remoteDensity,
        layoutDirection,
        platformImageProvider,
    )

public fun RemoteComposeCreationState(
    creationDisplayInfo: RemoteCreationDisplayInfo,
    profile: Profile,
    writerEvents: RemoteWriterEvents?,
    remoteDensity: RemoteDensity = RemoteDensity.from(creationDisplayInfo),
    layoutDirection: LayoutDirection,
    platformImageProvider: PlatformImageProvider = AndroidPlatformImageProvider,
): RemoteComposeCaptureState =
    RemoteComposeCaptureState(
        creationDisplayInfo,
        profile,
        writerEvents,
        remoteDensity,
        layoutDirection,
        platformImageProvider,
    )

public fun RemoteComposeCreationState(
    creationDisplayInfo: RemoteCreationDisplayInfo,
    contentDescription: String?,
    profile: Profile,
    platformImageProvider: PlatformImageProvider = AndroidPlatformImageProvider,
): RemoteComposeCaptureState =
    RemoteComposeCaptureState(
        creationDisplayInfo,
        contentDescription,
        profile,
        platformImageProvider,
    )

public fun RemoteComposeCreationState(
    platform: RcPlatformServices,
    size: Size,
): RemoteComposeCaptureState = RemoteComposeCaptureState(platform, size)

public fun RemoteComposeCreationState(
    platform: RcPlatformServices,
    size: Size,
    apiLevel: Int,
    profiles: Int,
): RemoteComposeCaptureState = RemoteComposeCaptureState(platform, size, apiLevel, profiles)

public fun RemoteComposeCreationState(
    creationDisplayInfo: RemoteCreationDisplayInfo,
    profile: Profile,
    writer: RemoteComposeWriter,
): RemoteComposeCaptureState = RemoteComposeCaptureState(creationDisplayInfo, profile, writer)

public fun RemoteComposeCreationState(
    size: Size,
    profile: Profile,
): RemoteComposeCaptureState = RemoteComposeCaptureState(size, profile)

// Density and Size should be taken from Compose in this mode
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class NoRemoteCompose :
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
        platformImageProvider = NoOpPlatformImageProvider,
    )

public val LocalRemoteComposeCreationState: ProvidableCompositionLocal<RemoteComposeCaptureState> =
    compositionLocalOf {
        NoRemoteCompose()
    }

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface PlatformImageProvider {
    public fun addBitmap(document: RemoteComposeWriter, image: ImageBitmap): Int

    public fun addNamedBitmap(document: RemoteComposeWriter, name: String, image: ImageBitmap): Int
}

internal object NoOpPlatformImageProvider : PlatformImageProvider {
    override fun addBitmap(document: RemoteComposeWriter, image: ImageBitmap): Int = -1

    override fun addNamedBitmap(
        document: RemoteComposeWriter,
        name: String,
        image: ImageBitmap,
    ): Int = -1
}

internal object AndroidPlatformImageProvider : PlatformImageProvider {
    override fun addBitmap(document: RemoteComposeWriter, image: ImageBitmap): Int =
        document.addBitmap(image.asAndroidBitmap())

    override fun addNamedBitmap(
        document: RemoteComposeWriter,
        name: String,
        image: ImageBitmap,
    ): Int = document.addNamedBitmap(name, image.asAndroidBitmap())
}
