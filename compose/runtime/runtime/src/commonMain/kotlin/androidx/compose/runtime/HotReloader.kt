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
        internal fun invalidateGroupsWithKey(key: Int) {
            return Recomposer.invalidateGroupsWithKey(key)
        }

        @TestOnly
        internal fun getCurrentErrors(): List<RecomposerErrorInfo> {
            return Recomposer.getCurrentErrors()
        }

        @TestOnly
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
@TestOnly fun simulateHotReload(context: Any) = HotReloader.simulateHotReload(context)

/**
 * Invalidates composed groups with the given key. Calling this method switches recomposer into hot
 * reload mode. Test-only API, not for use in production.
 *
 * @param key group key to invalidate.
 */
@TestOnly fun invalidateGroupsWithKey(key: Int) = HotReloader.invalidateGroupsWithKey(key)

/**
 * Get list of errors captured in composition. This list is only available when recomposer is in hot
 * reload mode. Test-only API, not for use in production.
 *
 * @return pair of error and whether the error is recoverable.
 */
// suppressing for test-only api
@Suppress("ListIterator")
@TestOnly
fun currentCompositionErrors(): List<Pair<Exception, Boolean>> =
    HotReloader.getCurrentErrors().map { it.cause to it.recoverable }

/**
 * Clears current composition errors in hot reload mode. Test-only API, not for use in production.
 */
@TestOnly fun clearCompositionErrors() = HotReloader.clearErrors()
