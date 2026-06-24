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

/**
 * Creates a [DeepLinkRequest] with an [Intent].
 *
 * @param intent The Intent with the metadata to add to the DeepLinkRequest
 * @return a [DeepLinkRequest] instance
 */
public fun DeepLinkRequest.Companion.fromIntent(intent: Intent): DeepLinkRequest =
    DeepLinkRequest(uri = intent.data)

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
