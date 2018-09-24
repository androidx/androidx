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

// / A group of [GestureArenaMember] objects that are competing as a unit in the
// / [GestureArenaManager].
// /
// / Normally, a recognizer competes directly in the [GestureArenaManager] to
// / recognize a sequence of pointer events as a gesture. With a
// / [GestureArenaTeam], recognizers can compete in the arena in a group with
// / other recognizers.
// /
// / When gesture recognizers are in a team together, then once there are no
// / other competing gestures in the arena, the first gesture to have been added
// / to the team automatically wins, instead of the gestures continuing to
// / compete against each other.
// /
// / For example, [Slider] uses this to support both a
// / [HorizontalDragGestureRecognizer] and a [TapGestureRecognizer], but without
// / the drag recognizer having to wait until the user has dragged outside the
// / slop region of the tap gesture before triggering. Since they compete as a
// / team, as soon as any other recognizers are out of the arena, the drag
// / recognizer wins, even if the user has not actually dragged yet. On the other
// / hand, if the tap can win outright, before the other recognizers are taken
// / out of the arena (e.g. if the slider is in a vertical scrolling list and the
// / user places their finger on the touch surface then lifts it, so that neither
// / the horizontal nor vertical drag recognizers can claim victory) the tap
// / recognizer still actually wins, despite being in the team.
// /
// / To assign a gesture recognizer to a team, set
// / [OneSequenceGestureRecognizer.team] to an instance of [GestureArenaTeam].
// TODO(Migration/shepshapard): Need tests, which are dependent on HorizontalDragGestureRecognizer
// and VerticalDragGestureRecognizer
class GestureArenaTeam {
    internal val _combiners: MutableMap<Int, _CombiningGestureArenaMember> = mutableMapOf()

    // / Adds a new member to the arena on behalf of this team.
    // /
    // / Used by [GestureRecognizer] subclasses that wish to compete in the arena
    // / using this team.
    // /
    // / To assign a gesture recognizer to a team, see
    // / [OneSequenceGestureRecognizer.team].
    fun add(pointer: Int, member: GestureArenaMember): GestureArenaEntry {
        val combiner: _CombiningGestureArenaMember = _combiners.getOrPut(pointer) {
            _CombiningGestureArenaMember(this, pointer)
        }
        return combiner._add(pointer, member)
    }
}