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
@file:JvmName("RcDocumentBuilders")

package androidx.compose.remote.creation.dsl

import androidx.annotation.RestrictTo
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.RcProfiles
import androidx.compose.remote.core.operations.Header
import androidx.compose.remote.creation.RemoteComposeWriter
import androidx.compose.remote.creation.RemoteComposeWriter.HTag

/**
 * Controls how coordinate and dimension values in a RemoteCompose document are interpreted by the
 * player relative to display density.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@JvmInline
public value class RcDensityBehavior internal constructor(internal val value: Int) {
    public companion object {
        /** Values are interpreted as raw pixels, with no display density scaling applied. */
        public val Pixels: RcDensityBehavior =
            RcDensityBehavior(CoreDocument.DENSITY_BEHAVIOR_PIXELS)

        /** Values are interpreted as density-independent pixels (dp) and scaled by density. */
        public val Dp: RcDensityBehavior = RcDensityBehavior(CoreDocument.DENSITY_BEHAVIOR_DP)
    }
}

/**
 * Metadata property written into the header of a serialized RemoteCompose document created via
 * [createRcBuffer] or [createRawRcBuffer].
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class RcHeaderTag internal constructor(internal val tag: Short, internal val value: Any) {
    public companion object {
        /** Sets the intrinsic document width in pixels or dp (per [densityBehavior]). */
        public fun docWidth(width: Int): RcHeaderTag = RcHeaderTag(Header.DOC_WIDTH, width)

        /** Sets the intrinsic document height in pixels or dp (per [densityBehavior]). */
        public fun docHeight(height: Int): RcHeaderTag = RcHeaderTag(Header.DOC_HEIGHT, height)

        /** Sets the target playback frame rate in frames per second. */
        public fun desiredFps(fps: Int): RcHeaderTag = RcHeaderTag(Header.DOC_DESIRED_FPS, fps)

        /** Sets the accessibility content description for the document. */
        public fun contentDescription(description: String): RcHeaderTag =
            RcHeaderTag(Header.DOC_CONTENT_DESCRIPTION, description)

        /** Configures whether coordinates and dimensions are interpreted as pixels or dp. */
        public fun densityBehavior(behavior: RcDensityBehavior): RcHeaderTag =
            RcHeaderTag(Header.DOC_DENSITY_BEHAVIOR, behavior.value)

        /** Enables or disables DEFLATE stream compression for document operations. */
        public fun compress(enabled: Boolean = true): RcHeaderTag =
            RcHeaderTag(
                Header.COMPRESS,
                if (enabled) Header.COMPRESSION_DEFLATE else Header.COMPRESSION_NONE,
            )

        /** Sets the player debug verbosity level (`0` disables debug output). */
        public fun debug(level: Int): RcHeaderTag = RcHeaderTag(Header.DEBUG, level)

        /** Creates a raw header tag from a wire property ID and value. */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public fun raw(tag: Short, value: Any): RcHeaderTag = RcHeaderTag(tag, value)
    }
}

private fun createRcBufferInternal(
    profile: RcProfile,
    experimental: Boolean = false,
    vararg tags: RcHeaderTag,
    contentExecution: (RemoteComposeWriter, RcScope) -> Unit,
): ByteArray {
    val isExperimental = experimental || profile.experimental
    val finalProfile =
        if (isExperimental) {
            androidx.compose.remote.creation.profile.Profile(
                profile.profile.apiLevel,
                profile.profile.operationsProfiles or RcProfiles.PROFILE_EXPERIMENTAL,
                profile.profile.platform,
                profile.profile.profileFactory,
            )
        } else {
            profile.profile
        }

    val rawTags = Array(tags.size) { HTag(tags[it].tag, tags[it].value) }
    val finalTags =
        if (isExperimental) {
            var profileTagFound = false
            val modifiedTags =
                rawTags
                    .map { tag ->
                        if (tag.tag == Header.DOC_PROFILES) {
                            profileTagFound = true
                            HTag(tag.tag, (tag.value as Int) or RcProfiles.PROFILE_EXPERIMENTAL)
                        } else {
                            tag
                        }
                    }
                    .toMutableList()

            if (!profileTagFound) {
                modifiedTags.add(
                    HTag(
                        Header.DOC_PROFILES,
                        profile.profile.operationsProfiles or RcProfiles.PROFILE_EXPERIMENTAL,
                    )
                )
            }
            modifiedTags.toTypedArray()
        } else {
            rawTags
        }

    val writer = RemoteComposeWriter(finalProfile, *finalTags)
    val scope = RcScopeImpl(writer)

    contentExecution(writer, scope)

    return writer.encodeToByteArray()
}

/**
 * Creates a serialized RemoteCompose document wrapped in a root layout container for the given
 * [profile].
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun createRcBuffer(
    profile: RcProfile,
    vararg tags: RcHeaderTag,
    content: RcScope.() -> Unit,
): ByteArray =
    createRcBufferInternal(profile, experimental = false, *tags) { writer, scope ->
        writer.root { scope.content() }
    }

/**
 * Creates a serialized RemoteCompose document wrapped in a root layout container for the given
 * [profile], with an explicit [experimental] flag.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun createRcBuffer(
    profile: RcProfile,
    experimental: Boolean,
    vararg tags: RcHeaderTag,
    content: RcScope.() -> Unit,
): ByteArray =
    createRcBufferInternal(profile, experimental, *tags) { writer, scope ->
        writer.root { scope.content() }
    }

/**
 * Creates a serialized RemoteCompose document for the given [profile] without wrapping [content] in
 * a root layout container.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun createRawRcBuffer(
    profile: RcProfile,
    vararg tags: RcHeaderTag,
    content: RcCanvasScope.() -> Unit,
): ByteArray =
    createRcBufferInternal(profile, experimental = false, *tags) { writer, _ ->
        RcCanvasScopeImpl(writer).content()
    }

/**
 * Creates a serialized RemoteCompose document for the given [profile] with an explicit
 * [experimental] flag, without wrapping [content] in a root layout container.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun createRawRcBuffer(
    profile: RcProfile,
    experimental: Boolean,
    vararg tags: RcHeaderTag,
    content: RcCanvasScope.() -> Unit,
): ByteArray =
    createRcBufferInternal(profile, experimental, *tags) { writer, _ ->
        RcCanvasScopeImpl(writer).content()
    }

/**
 * Executes [RcCanvasScope] drawing commands against an existing [writer], bridging Compose frontend
 * canvas blocks to the procedural DSL.
 *
 * ```
 * RemoteCanvas(modifier = RemoteModifier.fillMaxSize()) {
 *     RcCanvas(remoteComposeCreationState.document) {
 *         // DSL drawing commands
 *     }
 * }
 * ```
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun RcCanvas(writer: RemoteComposeWriter, content: RcCanvasScope.() -> Unit) {
    RcCanvasScopeImpl(writer).content()
}
