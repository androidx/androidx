/*
 * Copyright 2020 The Android Open Source Project
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

import androidx.paging.RemoteMediator.InitializeAction.SKIP_INITIAL_REFRESH
import kotlinx.coroutines.delay

open class RemoteMediatorMock(private val loadDelay: Long = 0) : RemoteMediator<Int, Int>() {
    val loadEvents = mutableListOf<LoadEvent<Int, Int>>()
    private val _newLoadEvents = mutableListOf<LoadEvent<Int, Int>>()
    val newLoadEvents: List<LoadEvent<Int, Int>>
        get() {
            val result = _newLoadEvents.toList()
            _newLoadEvents.clear()
            return result
        }
    val initializeEvents = mutableListOf<Unit>()

    var initializeResult = SKIP_INITIAL_REFRESH

    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, Int>
    ): MediatorResult {
        loadEvents.add(LoadEvent(loadType, state))
        _newLoadEvents.add(LoadEvent(loadType, state))

        delay(loadDelay)

        return MediatorResult.Success(false)
    }

    override suspend fun initialize(): InitializeAction {
        initializeEvents.add(Unit)
        return initializeResult
    }

    data class LoadEvent<Key : Any, Value : Any>(
        val loadType: LoadType,
        val state: PagingState<Key, Value>?
    )
}
