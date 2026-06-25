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

package androidx.navigation3.runtime.deeplink

import android.content.Intent
import androidx.savedstate.read

/**
 * Creates a [DeepLinkRequest] with an [Intent].
 *
 * The returned [DeepLinkRequest] will be populated with the following data:
 * 1. [DeepLinkRequest.uri] will be [Intent.getData] if not null
 * 2. [DeepLinkRequest.extras] will contain any non-null information from [Intent.getType],
 *    [Intent.getAction], and [Intent.getExtras].
 *
 * @param intent The Intent with the metadata to construct a DeepLinkRequest
 * @param extras The map holding pairs of [String] to [Any] to provide extra information to the
 *   [DeepLinkRequest], such a mimeType. If stored under the same key, all information inside the
 *   [extras] map will take precedence over the information in the [intent].
 * @return a [DeepLinkRequest] instance
 */
public operator fun DeepLinkRequest.Companion.invoke(
    intent: Intent,
    extras: Map<String, Any> = emptyMap(),
): DeepLinkRequest {
    val uri = intent.data?.toString()?.let { DeepLinkUri(it) }
    val intentExtra = buildMap {
        intent.extras?.read {
            val extrasMap = toMap()
            for ((key, value) in extrasMap) {
                if (value != null) put(key, value)
            }
        }
    }
    val mimeExtra =
        if (intent.type != null) DeepLinkRequest.mimeTypeExtra(intent.type!!) else emptyMap()
    val actionExtra =
        if (intent.action != null) DeepLinkRequest.actionExtra(intent.action!!) else emptyMap()
    return DeepLinkRequest(uri, mimeExtra + actionExtra + intentExtra + extras)
}

/**
 * Creates a [DeepLinkMatcher.Filter] that filters a [DeepLinkRequest] with the [action]. Matching
 * is case-insensitive.
 *
 * @param action the action the filter by
 * @return true if the [DeepLinkRequest]'s Action exactly matches the [action], false if the
 *   request's Action does not match or if the request does not provide any Action.
 */
public fun DeepLinkMatcher.Companion.actionFilter(action: String): DeepLinkMatcher.Filter =
    DeepLinkMatcher.Filter { request ->
        val requestedAction = request.extras[DeepLinkRequest.ActionExtrasKey]
        action.equals(requestedAction, true)
    }

/** The key of the Action stored inside the map returned by [actionExtra]. */
public val DeepLinkRequest.Companion.ActionExtrasKey: RequestExtrasKey<String>
    get() = ActionKey

private object ActionKey : RequestExtrasKey<String>

/**
 * Returns a Map<String, Any> that stores the provided [actionExtra] with the key [ActionExtrasKey].
 *
 * The value can be retrieved via map.get([ActionExtrasKey]).
 */
public fun DeepLinkRequest.Companion.actionExtra(action: String): Map<String, Any> = requestExtras {
    put(ActionExtrasKey, action)
}
