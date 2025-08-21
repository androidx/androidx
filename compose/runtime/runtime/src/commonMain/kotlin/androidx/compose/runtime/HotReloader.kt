/*
 * Copyright 2023 The Android Open Source Project
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

@file:OptIn(InternalComposeApi::class)

package androidx.compose.runtime

import androidx.annotation.RestrictTo
import androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP
import kotlin.jvm.JvmName

/**
 * Apply Code Changes will invoke the two functions before and after a code swap.
 *
 * This forces the whole view hierarchy to be redrawn to invoke any code change that was introduce
 * in the code swap.
 *
 * All these are private as within JVMTI / JNI accessibility is mostly a formality.
 */
private class HotReloader {
    companion object {
        // Called before Dex Code Swap
        @Suppress("UNUSED_PARAMETER")
        private fun saveStateAndDispose(context: Any): Any {
            return Recomposer.saveStateAndDisposeForHotReload()
        }

        // Called after Dex Code Swap
        private fun loadStateAndCompose(token: Any) {
            Recomposer.loadStateAndComposeForHotReload(token)
        }

        @TestOnly
        internal fun simulateHotReload(context: Any) {
            loadStateAndCompose(saveStateAndDispose(context))
        }

        @TestOnly
        // b/426871325: required for LiveEdit compatibility
        @JvmName("invalidateGroupsWithKey")
        internal fun invalidateGroupsWithKey(key: Int) {
            return Recomposer.invalidateGroupsWithKey(key)
        }

        @TestOnly
        // b/426871325: required for LiveEdit compatibility
        @JvmName("getCurrentErrors")
        internal fun getCurrentErrors(): List<RecomposerErrorInfo> {
            return Recomposer.getCurrentErrors()
        }

        @TestOnly
        // b/426871325: required for LiveEdit compatibility
        @JvmName("clearErrors")
        internal fun clearErrors() {
            return Recomposer.clearErrors()
        }
    }
}

/**
 * Simulates hot reload of all current compositions by disposing all composed content and restarting
 * compositions. Calling this method switches recomposer into hot reload mode. Test-only API, not
 * for use in production.
 *
 * @param context context for disposal.
 */
@TestOnly public fun simulateHotReload(context: Any): Unit = HotReloader.simulateHotReload(context)

/**
 * Invalidates composed groups with the given key. Calling this method switches recomposer into hot
 * reload mode. Test-only API, not for use in production.
 *
 * @param key group key to invalidate.
 */
@TestOnly
public fun invalidateGroupsWithKey(key: Int): Unit = HotReloader.invalidateGroupsWithKey(key)

/** Disables hot reload mode in recomposer. Test-only API, not for use in production. */
@TestOnly public fun disableHotReloadMode(): Unit = Recomposer.setHotReloadEnabled(false)

/**
 * Get list of errors captured in composition. This list is only available when recomposer is in hot
 * reload mode. Test-only API, not for use in production.
 *
 * @return pair of error and whether the error is recoverable.
 */
@Deprecated(
    "currentCompositionErrors only reports errors that extend from Exception. This method is " +
        "unsupported outside of Compose runtime tests. Internally, getCurrentCompositionErrors " +
        "should be used instead."
)
@TestOnly
@Suppress("ListIterator")
public fun currentCompositionErrors(): List<Pair<Exception, Boolean>> =
    getCurrentCompositionErrors().mapNotNull { (cause, recoverable) ->
        (cause as? Exception ?: return@mapNotNull null) to recoverable
    }

/**
 * Get list of errors captured in composition. This list is only available when recomposer is in hot
 * reload mode. Test-only API, not for use in production.
 *
 * @return pair of error and whether the error is recoverable.
 */
// suppressing for test-only api
@Suppress("ListIterator")
@RestrictTo(LIBRARY_GROUP)
@TestOnly
public fun getCurrentCompositionErrors(): List<Pair<Throwable, Boolean>> =
    HotReloader.getCurrentErrors().map { it.cause to it.recoverable }

/**
 * Clears current composition errors in hot reload mode. Test-only API, not for use in production.
 */
@TestOnly public fun clearCompositionErrors(): Unit = HotReloader.clearErrors()
