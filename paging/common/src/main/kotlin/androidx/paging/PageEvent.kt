/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.paging

/**
 * Events in the stream from paging fetch logic to UI.
 *
 * Every event sent to the UI is a PageEvent, and will be processed atomically.
 *
 * TODO:
 *  - transformations
 */
internal sealed class PageEvent<T : Any> {
    data class Insert<T : Any>(
        val loadType: LoadType,
        val pages: List<TransformedPage<T>>,
        val placeholdersStart: Int,
        val placeholdersEnd: Int
    ) : PageEvent<T>() {
        init {
            require(placeholdersStart >= 0) {
                "Invalid placeholdersBefore $placeholdersStart"
            }
            require(placeholdersEnd >= 0) {
                "Invalid placeholdersAfter $placeholdersEnd"
            }
        }
    }

    data class Drop<T : Any>(
        val loadType: LoadType,
        val count: Int,
        val placeholdersRemaining: Int
    ) : PageEvent<T>() {
        init {
            require(loadType != LoadType.REFRESH) { "Drop must be START or END" }
            require(count >= 0) { "Invalid count $count" }
            require(placeholdersRemaining >= 0) {
                "Invalid placeholdersRemaining $placeholdersRemaining"
            }
        }
    }

    data class StateUpdate<T : Any>(
        val loadType: LoadType,
        val loadState: LoadState
    ) : PageEvent<T>()
}