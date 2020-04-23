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

import androidx.recyclerview.widget.ListUpdateCallback

class ListUpdateCallbackFake : ListUpdateCallback {
    var interactions = 0
    val onInsertedEvents = mutableListOf<OnInsertedEvent>()
    val onRemovedEvents = mutableListOf<OnRemovedEvent>()
    val onMovedEvents = mutableListOf<OnMovedEvent>()
    val onChangedEvents = mutableListOf<OnChangedEvent>()

    override fun onInserted(position: Int, count: Int) {
        interactions++
        onInsertedEvents.add(OnInsertedEvent(position, count))
    }

    override fun onRemoved(position: Int, count: Int) {
        interactions++
        onRemovedEvents.add(OnRemovedEvent(position, count))
    }

    override fun onMoved(fromPosition: Int, toPosition: Int) {
        interactions++
        onMovedEvents.add(OnMovedEvent(fromPosition, toPosition))
    }

    override fun onChanged(position: Int, count: Int, payload: Any?) {
        interactions++
        onChangedEvents.add(OnChangedEvent(position, count, payload))
    }

    data class OnInsertedEvent(val position: Int, val count: Int)
    data class OnRemovedEvent(val position: Int, val count: Int)
    data class OnMovedEvent(val fromPosition: Int, val toPosition: Int)
    data class OnChangedEvent(val position: Int, val count: Int, val payload: Any?)
}