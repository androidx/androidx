/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.compose.runtime.composer

internal enum class InvalidationResult {
    /**
     * The invalidation was ignored because the associated recompose scope is no longer part of the
     * composition or has yet to be entered in the composition. This could occur for invalidations
     * called on scopes that are no longer part of composition or if the scope was invalidated
     * before [androidx.compose.runtime.ControlledComposition.applyChanges] was called that will
     * enter the scope into the composition.
     */
    IGNORED,

    /**
     * The composition is not currently composing and the invalidation was recorded for a future
     * composition. A recomposition requested to be scheduled.
     */
    SCHEDULED,

    /**
     * The composition that owns the recompose scope is actively composing but the scope has already
     * been composed or is in the process of composing. The invalidation is treated as SCHEDULED
     * above.
     */
    DEFERRED,

    /**
     * The composition that owns the recompose scope is actively composing and the invalidated scope
     * has not been composed yet but will be recomposed before the composition completes. A new
     * recomposition was not scheduled for this invalidation.
     */
    IMMINENT,
}
