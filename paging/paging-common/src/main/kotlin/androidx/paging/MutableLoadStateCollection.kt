/*
 * Copyright 2021 The Android Open Source Project
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

import androidx.paging.LoadState.NotLoading

internal class MutableLoadStateCollection {
    var refresh: LoadState = NotLoading.Incomplete
    var prepend: LoadState = NotLoading.Incomplete
    var append: LoadState = NotLoading.Incomplete

    fun snapshot() = LoadStates(
        refresh = refresh,
        prepend = prepend,
        append = append,
    )

    fun get(loadType: LoadType) = when (loadType) {
        LoadType.REFRESH -> refresh
        LoadType.APPEND -> append
        LoadType.PREPEND -> prepend
    }

    fun set(type: LoadType, state: LoadState) = when (type) {
        LoadType.REFRESH -> refresh = state
        LoadType.APPEND -> append = state
        LoadType.PREPEND -> prepend = state
    }

    fun set(states: LoadStates) {
        refresh = states.refresh
        append = states.append
        prepend = states.prepend
    }
}