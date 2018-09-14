/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.ui.gestures.arena

internal class _GestureArena {
    val members: MutableList<GestureArenaMember> = mutableListOf()
    var isOpen = true
    var isHeld = false
    var hasPendingSweep = false

    // / If a gesture attempts to win while the arena is still open, it becomes the
    // / "eager winner". We look for an eager winner when closing the arena to new
    // / participants, and if there is one, we resolve the arena in its favor at
    // / that time.
    var eagerWinner: GestureArenaMember? = null

    fun add(member: GestureArenaMember) {
        assert(isOpen)
        members.add(member)
    }

    override fun toString(): String {
        val buffer = StringBuffer()
        if (members.isEmpty()) {
            buffer.append("<empty")
        } else {
            buffer.append(members.map {
                if (it == eagerWinner)
                    return "$it (eager winner)"
                return "$it"
            }.joinToString(", "))
        }
        if (isOpen)
            buffer.append(" [open")
        if (isHeld)
            buffer.append(" [held")
        if (hasPendingSweep)
            buffer.append(" [hasPendingSweep]")
        return buffer.toString()
    }
}