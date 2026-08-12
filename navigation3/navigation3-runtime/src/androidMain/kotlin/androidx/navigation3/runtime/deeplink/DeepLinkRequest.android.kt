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
import androidx.savedstate.SavedState

/**
 * Creates a [DeepLinkRequest] with an [Intent].
 *
 * The returned [DeepLinkRequest] will be populated with the following data:
 * 1. [DeepLinkRequest.uri] will be [Intent.getData] if not null
 * 2. [DeepLinkRequest.extras] will contain any non-null information from [Intent.getType],
 *    [Intent.getAction], and [Intent.getExtras].
 *
 * @param intent The Intent with the metadata to construct a DeepLinkRequest
 * @param extras The [RequestExtras] holding key-value pairs of `RequestExtrasKey<T>` to `T` to
 *   provide extra information to the [DeepLinkRequest]. If [extras] contains an Action stored under
 *   [ActionExtrasKey] or a MimeType stored under [MimeTypeExtrasKey], their values will override
 *   any Action or MimeType stored in the [intent].
 * @return a [DeepLinkRequest] instance
 */
public operator fun DeepLinkRequest.Companion.invoke(
    intent: Intent,
    extras: RequestExtras = emptyRequestExtras(),
): DeepLinkRequest {
    val uri = intent.data?.toString()?.let { DeepLinkUri(it) }
    var combinedExtras = emptyRequestExtras()
    intent.extras?.let { combinedExtras += requestExtras { put(IntentExtrasKey, it) } }
    intent.type?.let { combinedExtras += DeepLinkRequest.mimeTypeExtra(it) }
    intent.action?.let { combinedExtras += DeepLinkRequest.actionExtra(it) }
    combinedExtras += extras
    return DeepLinkRequest(uri, combinedExtras)
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

/**
 * The [RequestExtrasKey] for the Action stored inside the [RequestExtras] returned by
 * [actionExtra].
 */
public val DeepLinkRequest.Companion.ActionExtrasKey: RequestExtrasKey<String>
    get() = ActionKey

private object ActionKey : RequestExtrasKey<String>

/**
 * Returns a [RequestExtras] that stores the provided [actionExtra] with the key [ActionExtrasKey].
 *
 * The value can be retrieved via extras[[ActionExtrasKey]].
 */
public fun DeepLinkRequest.Companion.actionExtra(action: String): RequestExtras = requestExtras {
    put(ActionExtrasKey, action)
}

/**
 * The [RequestExtrasKey] for the [Intent.extras] stored in [DeepLinkRequest.extras] when the
 * Request is created with [DeepLinkRequest.Companion.invoke] factory method.
 *
 * The [android.os.Bundle] is stored as a [SavedState].
 */
public val DeepLinkRequest.Companion.IntentExtrasKey: RequestExtrasKey<SavedState>
    get() = IntentKey

private object IntentKey : RequestExtrasKey<SavedState>
