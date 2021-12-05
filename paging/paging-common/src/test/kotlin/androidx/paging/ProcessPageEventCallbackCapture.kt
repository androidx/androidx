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

import androidx.paging.PagePresenter.ProcessPageEventCallback

sealed class PresenterEvent
data class ChangeEvent(val position: Int, val count: Int) : PresenterEvent()
data class InsertEvent(val position: Int, val count: Int) : PresenterEvent()
data class RemoveEvent(val position: Int, val count: Int) : PresenterEvent()
data class StateEvent(
    val loadType: LoadType,
    val fromMediator: Boolean,
    val loadState: LoadState
) : PresenterEvent()
data class CombinedStateEvent(
    val source: LoadStates,
    val mediator: LoadStates?,
) : PresenterEvent()

class ProcessPageEventCallbackCapture : ProcessPageEventCallback {
    private val list = mutableListOf<PresenterEvent>()
    fun getAllAndClear() = list.getAllAndClear()

    override fun onChanged(position: Int, count: Int) {
        if (count != 0) {
            list.add(ChangeEvent(position, count))
        }
    }

    override fun onInserted(position: Int, count: Int) {
        if (count != 0) {
            list.add(InsertEvent(position, count))
        }
    }

    override fun onRemoved(position: Int, count: Int) {
        if (count != 0) {
            list.add(RemoveEvent(position, count))
        }
    }

    override fun onStateUpdate(loadType: LoadType, fromMediator: Boolean, loadState: LoadState) {
        list.add(StateEvent(loadType, fromMediator, loadState))
    }

    override fun onStateUpdate(source: LoadStates, mediator: LoadStates?) {
        list.add(CombinedStateEvent(source, mediator))
    }
}
