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

// / Represents an object participating in an arena.
// /
// / Receives callbacks from the GestureArena to notify the object when it wins
// / or loses a gesture negotiation. Exactly one of [acceptGesture] or
// / [rejectGesture] will be called for each arena this member was added to,
// / regardless of what caused the arena to be resolved. For example, if a
// / member resolves the arena itself, that member still receives an
// / [acceptGesture] callback.
interface GestureArenaMember {
    // / Called when this member wins the arena for the given pointer id.
    fun acceptGesture(pointer: Int)

    // / Called when this member loses the arena for the given pointer id.
    fun rejectGesture(pointer: Int)
}