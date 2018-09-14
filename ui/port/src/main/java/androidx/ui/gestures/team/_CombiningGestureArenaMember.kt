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

package androidx.ui.gestures.team

import androidx.ui.gestures.arena.GestureArenaEntry
import androidx.ui.gestures.arena.GestureArenaMember
import androidx.ui.gestures.arena.GestureDisposition

// TODO(Migration/shepshapard): Need tests, which are dependent on OneSequenceGestureRecognizer
internal class _CombiningGestureArenaMember(
    private val _owner: GestureArenaTeam,
    private val _pointer: Int
) : GestureArenaMember {
    private val _members: MutableList<GestureArenaMember> = mutableListOf()

    private var _resolved: Boolean = false
    private var _winner: GestureArenaMember? = null
    private var _entry: GestureArenaEntry? = null

    override fun acceptGesture(pointer: Int) {
        assert(_pointer == pointer)
        assert(_winner != null || _members.isNotEmpty())
        _close()
        val winner = _winner ?: _members[0]
        _members.forEach {
            if (it != winner) {
                it.rejectGesture(_pointer)
            }
        }
        winner.acceptGesture(pointer)
    }

    override fun rejectGesture(pointer: Int) {
        assert(_pointer == pointer)
        _close()
        _members.forEach {
            it.rejectGesture(pointer)
        }
    }

    fun _close() {
        assert(!_resolved)
        _resolved = true
        val combiner: _CombiningGestureArenaMember? = _owner._combiners.remove(_pointer)
        assert(combiner == this)
    }

    fun _add(pointer: Int, member: GestureArenaMember): GestureArenaEntry {
        assert(!_resolved)
        assert(_pointer == pointer)
        _members.add(member)
        // TODO(Migration/shepshapard): GestureBinding needed...
        // _entry = _entry ?: GestureBinding.instance.gestureArena.add(pointer, this)
        return _CombiningGestureArenaEntry(this, member)
    }

    internal fun _resolve(member: GestureArenaMember, disposition: GestureDisposition) {
        if (_resolved)
            return
        if (disposition == GestureDisposition.rejected) {
            _members.remove(member)
            member.rejectGesture(_pointer)
            if (_members.isEmpty())
                _entry!!.resolve(disposition)
        } else {
            assert(disposition == GestureDisposition.accepted)
            _winner = _winner ?: member
            _entry!!.resolve(disposition)
        }
    }
}