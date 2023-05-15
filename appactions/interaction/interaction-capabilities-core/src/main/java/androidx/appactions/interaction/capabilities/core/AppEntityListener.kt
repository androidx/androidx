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
package androidx.appactions.interaction.capabilities.core

import androidx.annotation.RestrictTo
import androidx.concurrent.futures.await
import com.google.common.util.concurrent.ListenableFuture

/**
 * Handle grounding of ungrounded values.
 *
 * @suppress
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
interface AppEntityListenerBase<T> {
    /**
     * Given a search criteria, looks up the inventory during runtime, renders the search result
     * within the app's own UI and then returns it to the Assistant so that the task can be kept in
     * sync with the app UI.
     *
     * @return a structured search result represented by [EntitySearchResult] for the given
     *   [searchAction]
     */
    suspend fun lookupAndRender(searchAction: SearchAction<T>): EntitySearchResult<T> =
        lookupAndRenderAsync(searchAction).await()

    /**
     * Given a search criteria, looks up the inventory during runtime, renders the search result
     * within the app's own UI and then returns it to the Assistant so that the task can be kept in
     * sync with the app UI.
     *
     * @return a [ListenableFuture] of the structured search result represented by
     *   [EntitySearchResult] for the given [searchAction]
     */
    fun lookupAndRenderAsync(
        searchAction: SearchAction<T>,
    ): ListenableFuture<EntitySearchResult<T>> {
        throw NotImplementedError()
    }
}

/**
 * Similar to ValueListener, but also need to handle grounding of ungrounded values.
 *
 * @suppress
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
interface AppEntityListener<T> : ValueListener<T>, AppEntityListenerBase<T>

/**
 * Similar to ValueListener, but also need to handle grounding of ungrounded values.
 *
 * @suppress
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
interface AppEntityListListener<T> : ValueListener<List<T>>, AppEntityListenerBase<T>
