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

package androidx.compose.remote.creation.dsl

import androidx.annotation.RestrictTo
import androidx.compose.remote.core.RcProfiles
import androidx.compose.remote.creation.RemoteComposeWriter
import androidx.compose.remote.creation.RemoteComposeWriter.HTag

private fun createRcBufferInternal(
    profile: RcProfile,
    vararg tags: HTag,
    experimental: Boolean = false,
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

    val finalTags =
        if (isExperimental) {
            var profileTagFound = false
            val modifiedTags =
                tags
                    .map { tag ->
                        if (
                            tag.tag == androidx.compose.remote.core.operations.Header.DOC_PROFILES
                        ) {
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
                        androidx.compose.remote.core.operations.Header.DOC_PROFILES,
                        profile.profile.operationsProfiles or RcProfiles.PROFILE_EXPERIMENTAL,
                    )
                )
            }
            modifiedTags.toTypedArray()
        } else {
            tags
        }

    val writer = RemoteComposeWriter(finalProfile, *finalTags)
    val scope = RcScopeImpl(writer)

    contentExecution(writer, scope)

    val buffer = writer.buffer()
    val size = writer.bufferSize()
    return buffer.copyOfRange(0, size)
}

/** Top-level builder for creating a serialized RemoteCompose document. It will also create root */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun createRcBuffer(
    profile: RcProfile,
    vararg tags: HTag,
    experimental: Boolean = false,
    content: RcScope.() -> Unit,
): ByteArray =
    createRcBufferInternal(profile, *tags, experimental = experimental) { writer, scope ->
        writer.root { scope.content() }
    }

/** Top-level builder for creating a serialized RemoteCompose document. without creating root */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun createRawRcBuffer(
    profile: RcProfile,
    vararg tags: HTag,
    experimental: Boolean = false,
    content: RcScope.() -> Unit,
): ByteArray =
    createRcBufferInternal(profile, *tags, experimental = experimental) { _, scope ->
        scope.content()
    }
