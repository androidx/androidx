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

import androidx.ui.foundation.debugPrint
import androidx.ui.gestures.debugPrintGestureArenaDiagnostics

/**
 * The first member to accept or the last member to not to reject wins.
 *
 * See [https://flutter.io/gestures/#gesture-disambiguation] for more
 * information about the role this class plays in the gesture system.
 *
 * To debug problems with gestures, consider using
 * [debugPrintGestureArenaDiagnostics].
 */
class GestureArenaManager {
    private val _arenas = mutableMapOf<Int, _GestureArena>()

    /** Adds a new member (e.g., gesture recognizer) to the arena. */
    fun add(pointer: Int, member: GestureArenaMember): GestureArenaEntry {
        val state: _GestureArena = _arenas.getOrPut(pointer) {
            assert(_debugLogDiagnostic(pointer, "★ Opening new gesture arena."))
            _GestureArena()
        }
        state.add(member)
        assert(_debugLogDiagnostic(pointer, "Adding: $member"))
        return GestureArenaEntryImpl(this, pointer, member)
    }

    /**
     * Prevents new members from entering the arena.
     *
     * Called after the framework has finished dispatching the pointer down event.
     */
    fun close(pointer: Int) {
        val state: _GestureArena = _arenas[pointer]
            ?: return // This arena either never existed or has been resolved.

        state.isOpen = false
        assert(_debugLogDiagnostic(pointer, "Closing", state))
        _tryToResolveArena(pointer, state)
    }

    /**
     * Forces resolution of the arena, giving the win to the first member.
     *
     * Sweep is typically after all the other processing for a [PointerUpEvent]
     * have taken place. It ensures that multiple passive gestures do not cause a
     * stalemate that prevents the user from interacting with the app.
     *
     * Recognizers that wish to delay resolving an arena past [PointerUpEvent]
     * should call [hold] to delay sweep until [release] is called.
     *
     * See also:
     *
     *  * [hold]
     *  * [release]
     */
    fun sweep(pointer: Int) {
        val state: _GestureArena = _arenas[pointer]
            ?: return // This arena either never existed or has been resolved.
        assert(!state.isOpen)
        if (state.isHeld) {
            state.hasPendingSweep = true
            assert(_debugLogDiagnostic(pointer, "Delaying sweep", state))
            return // This arena is being held for a long-lived member.
        }
        assert(_debugLogDiagnostic(pointer, "Sweeping", state))
        _arenas.remove(pointer)
        if (state.members.isNotEmpty()) {
            // First member wins.
            assert(_debugLogDiagnostic(pointer, "Winner: ${state.members[0]}"))
            state.members[0].acceptGesture(pointer)
            // Give all the other members the bad news.
            for (i in 1 until state.members.size) {
                state.members[i].rejectGesture(pointer)
            }
        }
    }

    /**
     * Prevents the arena from being swept.
     *
     * Typically, a winner is chosen in an arena after all the other
     * [PointerUpEvent] processing by [sweep]. If a recognizer wishes to delay
     * resolving an arena past [PointerUpEvent], the recognizer can [hold] the
     * arena open using this function. To release such a hold and let the arena
     * resolve, call [release].
     *
     * See also:
     *
     *  * [sweep]
     *  * [release]
     */
    fun hold(pointer: Int) {
        val state = _arenas[pointer]
            ?: return // This arena either never existed or has been resolved.
        state.isHeld = true
        assert(_debugLogDiagnostic(pointer, "Holding", state))
    }

    /**
     * Releases a hold, allowing the arena to be swept.
     *
     * If a sweep was attempted on a held arena, the sweep will be done
     * on release.
     *
     * See also:
     *
     *  * [sweep]
     *  * [hold]
     */
    fun release(pointer: Int) {
        val state = _arenas[pointer]
            ?: return // This arena either never existed or has been resolved.
        state.isHeld = false
        assert(_debugLogDiagnostic(pointer, "Releasing", state))
        if (state.hasPendingSweep)
            sweep(pointer)
    }

    /**
     * Reject or accept a gesture recognizer.
     *
     * This is called by calling [GestureArenaEntry.resolve] on the object returned from [add].
     */
    internal fun _resolve(
        pointer: Int,
        member: GestureArenaMember,
        disposition: GestureDisposition
    ) {
        val state: _GestureArena = _arenas[pointer]
            ?: return // This arena has already resolved.
        assert(
            _debugLogDiagnostic(
                pointer,
                "${if (disposition == GestureDisposition.accepted) "Accepting" else "Rejecting"}" +
                        " : $member"
            )
        )
        assert(state.members.contains(member))
        if (disposition == GestureDisposition.rejected) {
            state.members.remove(member)
            member.rejectGesture(pointer)
            if (!state.isOpen)
                _tryToResolveArena(pointer, state)
        } else {
            assert(disposition == GestureDisposition.accepted)
            if (state.isOpen) {
                state.eagerWinner = state.eagerWinner ?: member
            } else {
                assert(_debugLogDiagnostic(pointer, "Self-declared winner: $member"))
                _resolveInFavorOf(pointer, state, member)
            }
        }
    }

    private fun _tryToResolveArena(pointer: Int, state: _GestureArena) {
        assert(_arenas[pointer] == state)
        assert(!state.isOpen)
        val eagerWinner = state.eagerWinner
        if (state.members.size == 1) {
            // TODO(Migration/shepshapard): need scheduleMicrotask? for now just calling
            // what's needed synchrounously.
            _resolveByDefault(pointer, state)
            // scheduleMicrotask(() => _resolveByDefault(pointer, state))
        } else if (state.members.isEmpty()) {
            _arenas.remove(pointer)
            assert(_debugLogDiagnostic(pointer, "Arena empty."))
        } else if (eagerWinner != null) {
            assert(_debugLogDiagnostic(pointer, "Eager winner: ${state.eagerWinner}"))
            _resolveInFavorOf(pointer, state, eagerWinner)
        }
    }

    private fun _resolveByDefault(pointer: Int, state: _GestureArena) {
        if (!_arenas.containsKey(pointer))
            return // Already resolved earlier.
        assert(_arenas[pointer] == state)
        assert(!state.isOpen)
        val members: List<GestureArenaMember> = state.members
        assert(members.size == 1)
        _arenas.remove(pointer)
        assert(_debugLogDiagnostic(pointer, "Default winner: ${state.members.get(0)}"))
        state.members.get(0).acceptGesture(pointer)
    }

    private fun _resolveInFavorOf(pointer: Int, state: _GestureArena, member: GestureArenaMember) {
        assert(state == _arenas[pointer])
        assert(state.eagerWinner == null || state.eagerWinner == member)
        assert(!state.isOpen)
        _arenas.remove(pointer)
        state.members.forEach {
            if (it != member)
                it.rejectGesture(pointer)
        }
        member.acceptGesture(pointer)
    }

    private fun _debugLogDiagnostic(
        pointer: Int,
        message: String,
        state: _GestureArena? = null
    ): Boolean {
        androidx.ui.assert {
            if (debugPrintGestureArenaDiagnostics) {
                val count: Int? = if (state != null) state.members.size else null
                val s: String = if (count != 1) "s" else ""
                debugPrint(
                    "Gesture arena ${pointer.toString().padEnd(4)}" +
                            " ❙ $message${if (count != null) " with $count member$s." else ""}"
                )
            }
            true
        }
        return true
    }
}