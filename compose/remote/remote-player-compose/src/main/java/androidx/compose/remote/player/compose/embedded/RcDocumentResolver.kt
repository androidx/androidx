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

package androidx.compose.remote.player.compose.embedded

import androidx.annotation.RestrictTo
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf

/**
 * Pluggable resolver for embedded RemoteCompose documents referenced by URL in `RcDocLayout`.
 *
 * Resolving a URL to serialized RemoteCompose document bytes is the responsibility of the host
 * application (for example reading raw resources, app assets, local disk caches, or fetching from
 * the network). Returning `null` indicates that the URL cannot be resolved or is not permitted by
 * the host.
 *
 * [resolve] is a `suspend` function so hosts can perform off-main-thread I/O without blocking
 * composition.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Stable
public fun interface RcDocumentResolver {
    /**
     * Resolves the given [url] or document identifier into serialized RemoteCompose document bytes,
     * or `null` if unavailable.
     */
    public suspend fun resolve(url: String): ByteArray?
}

/**
 * The active [RcDocumentResolver] for resolving embedded RemoteCompose documents in [RcPlayer].
 * Inherited automatically by nested [RcPlayer] instances.
 */
@get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public val LocalRcDocumentResolver: ProvidableCompositionLocal<RcDocumentResolver?> =
    compositionLocalOf {
        null
    }
