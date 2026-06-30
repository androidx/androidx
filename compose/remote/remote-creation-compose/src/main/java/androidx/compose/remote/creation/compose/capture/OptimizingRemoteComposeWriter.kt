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

import androidx.annotation.RestrictTo
import androidx.compose.remote.core.RemoteComposeBuffer
import androidx.compose.remote.core.operations.Header
import androidx.compose.remote.creation.CreationDisplayInfo
import androidx.compose.remote.creation.RemoteComposeWriter
import androidx.compose.remote.creation.RemoteComposeWriterAndroid
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.profile.Profile

/**
 * A [RemoteComposeWriter] implementation that intercepts [encodeToByteArray] to execute an
 * optimization pipeline on the underlying [OptimizingRemoteComposeBuffer] before serialization.
 *
 * This writer is intended to be used in conjunction with [OptimizingRemoteComposeBuffer]. To
 * trigger the optimization and flush the recorded operations to the binary buffer, either
 * [encodeToByteArray] must be called on this writer, or
 * [OptimizingRemoteComposeBuffer.optimizeAndFlush] must be called directly on the buffer.
 *
 * This writer requires a [RemoteComposeCreationState] to resolve [RemoteFloat] expressions to their
 * corresponding IDs during the optimization and flush phase.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class OptimizingRemoteComposeWriter
internal constructor(
    profile: Profile,
    buffer: RemoteComposeBuffer,
    private val mCallback: Any?,
    internal var creationState: RemoteComposeCreationState? = null,
    vararg tags: RemoteComposeWriter.HTag,
) : RemoteComposeWriterAndroid(profile, buffer, *tags) {

    public constructor(
        creationDisplayInfo: CreationDisplayInfo,
        writerCallback: Any?,
        profile: Profile,
        buffer: RemoteComposeBuffer,
    ) : this(
        profile,
        buffer,
        writerCallback,
        null,
        RemoteComposeWriter.hTag(Header.DOC_WIDTH, creationDisplayInfo.width),
        RemoteComposeWriter.hTag(Header.DOC_HEIGHT, creationDisplayInfo.height),
        RemoteComposeWriter.hTag(Header.DOC_PROFILES, profile.operationsProfiles),
        RemoteComposeWriter.hTag(Header.DOC_DENSITY_BEHAVIOR, creationDisplayInfo.densityBehavior),
    )

    public constructor(
        profile: Profile,
        buffer: RemoteComposeBuffer,
        vararg tags: RemoteComposeWriter.HTag,
    ) : this(profile, buffer, null, null, *tags)

    override fun getWriterCallback(): Any? {
        return mCallback
    }

    override fun encodeToByteArray(): ByteArray {
        (buffer as OptimizingRemoteComposeBuffer).optimizeAndFlush(
            checkNotNull(creationState) { "creationState not initialized" }
        )
        return super.encodeToByteArray()
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun Profile.withOptimizingWriter(): Profile {
    val originalFactory = this.profileFactory
    return Profile(
        this.apiLevel,
        this.operationsProfiles,
        this.platform,
        { creationDisplayInfo, profile, callback ->
            val originalWriter = originalFactory.create(creationDisplayInfo, profile, callback)
            if (originalWriter is OptimizingRemoteComposeWriter) {
                originalWriter
            } else {
                val buffer = OptimizingRemoteComposeBuffer(profile.apiLevel)
                OptimizingRemoteComposeWriter(creationDisplayInfo, callback, profile, buffer)
            }
        },
    )
}
