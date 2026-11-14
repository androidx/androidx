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

package androidx.compose.remote.creation.dsl

import androidx.annotation.RestrictTo
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.RcPlatformServices
import androidx.compose.remote.core.RcProfiles
import androidx.compose.remote.creation.RemoteComposeWriter
import androidx.compose.remote.creation.profile.Profile
import androidx.compose.remote.creation.profile.Profile.SupportedOperationsProvider
import androidx.compose.remote.creation.profile.RemoteComposeWriterFactory

/**
 * Bit-flag set selecting the operation profile categories enabled in an [RcProfile]. Combine
 * independent flags with [or].
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@JvmInline
public value class RcProfileFlags
internal constructor(@get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) public val bits: Int) {
    /** Combines this profile flag set with [other]. */
    public infix fun or(other: RcProfileFlags): RcProfileFlags = RcProfileFlags(bits or other.bits)

    /** Returns `true` if every flag in [other] is present in this set. */
    public operator fun contains(other: RcProfileFlags): Boolean =
        (bits and other.bits) == other.bits

    public companion object {
        /** Standard baseline operations supported across RemoteCompose players. */
        public val Baseline: RcProfileFlags = RcProfileFlags(RcProfiles.PROFILE_BASELINE)

        /** Extended operations supported by the AndroidX RemoteCompose player. */
        public val AndroidX: RcProfileFlags = RcProfileFlags(RcProfiles.PROFILE_ANDROIDX)

        /** Extended operations supported by the Android platform launcher widget player. */
        public val Widgets: RcProfileFlags = RcProfileFlags(RcProfiles.PROFILE_WIDGETS)
    }
}

/**
 * Configures target player capabilities, API level, and platform services for a RemoteCompose
 * document created via [createRcBuffer] or [createRawRcBuffer].
 *
 * @param profile underlying [Profile] containing document API level and supported operations
 * @param isExperimental whether experimental document operations are enabled
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class RcProfile(
    public val profile: Profile,
    public val isExperimental: Boolean = false,
) {
    /** Document API level supported by this profile. */
    public val docApiLevel: Int
        get() = profile.apiLevel

    /** Operation profile flags enabled on this profile. */
    public val profileFlags: RcProfileFlags
        get() = RcProfileFlags(profile.operationsProfiles)

    /** Platform services implementation associated with this profile. */
    internal val platform: RcPlatformServices
        get() = profile.platform

    public companion object {
        /** Platform-independent baseline [RcProfile] at the current document API level. */
        public val CoreBaseline: RcProfile = core(profileFlags = RcProfileFlags.Baseline)

        /** Platform-independent AndroidX [RcProfile] at the current document API level. */
        public val CoreAndroidX: RcProfile = core(profileFlags = RcProfileFlags.AndroidX)

        /**
         * Creates a platform-independent [RcProfile] for headless or server-side document encoding.
         *
         * @param docApiLevel document API level supported by the target player
         * @param profileFlags operation profile category flags
         * @param supportedOperations explicit allowlist of operation IDs, or `null` to allow all
         *   operations defined by [docApiLevel] and [profileFlags]
         * @param isExperimental whether experimental document operations are enabled
         * @return configured platform-independent [RcProfile]
         */
        public fun core(
            docApiLevel: Int = CoreDocument.DOCUMENT_API_LEVEL,
            profileFlags: RcProfileFlags = RcProfileFlags.AndroidX,
            supportedOperations: IntArray? = null,
            isExperimental: Boolean = false,
        ): RcProfile {
            val factory = RemoteComposeWriterFactory { displayInfo, p, callback ->
                RemoteComposeWriter(displayInfo, null, p, callback)
            }
            val underlying =
                if (supportedOperations != null) {
                    @Suppress("PrimitiveInCollection")
                    val ops = HashSet<Int>(supportedOperations.size)
                    for (op in supportedOperations) {
                        ops.add(op)
                    }
                    Profile(
                        docApiLevel,
                        profileFlags.bits,
                        RcPlatformServices.None,
                        SupportedOperationsProvider { ops },
                        factory,
                    )
                } else {
                    Profile(docApiLevel, profileFlags.bits, RcPlatformServices.None, factory)
                }
            return RcProfile(underlying, isExperimental)
        }
    }
}
