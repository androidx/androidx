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
@file:JvmName("RcProfiles")

package androidx.compose.remote.creation.dsl

import androidx.annotation.IntRange
import androidx.annotation.RestrictTo
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.RcProfiles as CoreRcProfiles
import androidx.compose.remote.creation.profile.Profile

internal expect fun createPlatformProfile(
    docApiLevel: Int,
    profileFlags: Int,
    supportedOperations: IntArray?,
): Profile

private val defaultAndroidXProfile: RcProfile by lazy {
    RcProfile(
        createPlatformProfile(
            CoreDocument.DOCUMENT_API_LEVEL,
            CoreRcProfiles.PROFILE_ANDROIDX,
            null,
        )
    )
}

private val androidX7Profile: RcProfile by lazy {
    RcProfile(createPlatformProfile(7, CoreRcProfiles.PROFILE_ANDROIDX, null))
}

private val androidX8Profile: RcProfile by lazy {
    RcProfile(createPlatformProfile(8, CoreRcProfiles.PROFILE_ANDROIDX, null))
}

private val androidX9Profile: RcProfile by lazy {
    RcProfile(createPlatformProfile(9, CoreRcProfiles.PROFILE_ANDROIDX, null))
}

/**
 * Default [RcProfile] targeting the AndroidX RemoteCompose player at the latest document API level
 * with platform bitmap and path services enabled.
 */
@get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public val RcProfile.Companion.AndroidX: RcProfile
    get() = defaultAndroidXProfile

/** [RcProfile] targeting AndroidX RemoteCompose player API level 7. */
@get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public val RcProfile.Companion.AndroidX7: RcProfile
    get() = androidX7Profile

/** [RcProfile] targeting AndroidX RemoteCompose player API level 8. */
@get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public val RcProfile.Companion.AndroidX8: RcProfile
    get() = androidX8Profile

/** [RcProfile] targeting AndroidX RemoteCompose player API level 9. */
@get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public val RcProfile.Companion.AndroidX9: RcProfile
    get() = androidX9Profile

/**
 * Creates a platform-configured [RcProfile] for Android or JVM RemoteCompose document creation.
 *
 * @param docApiLevel document API level supported by the target player
 * @param profileFlags operation profile category flags
 * @param supportedOperations explicit allowlist of operation IDs, or `null` to allow all operations
 *   defined by [docApiLevel] and [profileFlags]
 * @param isExperimental whether experimental document operations are enabled
 * @return configured [RcProfile] backed by platform services
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun RcProfile(
    @IntRange(from = 1) docApiLevel: Int = CoreDocument.DOCUMENT_API_LEVEL,
    profileFlags: RcProfileFlags = RcProfileFlags.AndroidX,
    supportedOperations: IntArray? = null,
    isExperimental: Boolean = false,
): RcProfile =
    RcProfile(
        createPlatformProfile(docApiLevel, profileFlags.bits, supportedOperations),
        isExperimental,
    )
