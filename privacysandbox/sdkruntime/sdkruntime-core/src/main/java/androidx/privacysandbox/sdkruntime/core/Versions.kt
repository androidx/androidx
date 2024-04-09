/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.privacysandbox.sdkruntime.core

import androidx.annotation.Keep
import androidx.annotation.RestrictTo
import androidx.privacysandbox.sdkruntime.core.internal.ClientApiVersion
import org.jetbrains.annotations.TestOnly

/**
 * Store internal API version (for Client-Core communication).
 *
 * DO NOT CHANGE THIS CLASS.
 * Methods invoked via reflection from previously released versions of sdkruntime-client.
 */
@Suppress("unused")
@Keep
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object Versions {

    @JvmField
    val API_VERSION = ClientApiVersion.CURRENT_VERSION.apiLevel

    @JvmField
    var CLIENT_VERSION: Int? = null

    @JvmStatic
    fun handShake(clientVersion: Int): Int {
        CLIENT_VERSION = clientVersion
        return API_VERSION
    }

    @TestOnly
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    fun resetClientVersion() {
        CLIENT_VERSION = null
    }
}
