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

/**
 * An interface to information to an arena.
 *
 * A given [GestureArenaMember] can have multiple entries in multiple arenas
 * with different pointer ids.
 */
// TODO(Migration/shepshapard): Had to break the GestureArenaEntry in Flutter apart into an
// interface and an Impl because in Dart any class can be used as an interface and in this case,
// Flutter was doing just that.
class GestureArenaEntryImpl internal constructor(
    private val _arena: GestureArenaManager,
    private val _pointer: Int,
    private val _member: GestureArenaMember
) : GestureArenaEntry {

    /**
     * Call this member to claim victory (with accepted) or admit defeat (with rejected).
     *
     * It's fine to attempt to resolve a gesture recognizer for an arena that is
     * already resolved.
     */
    override fun resolve(disposition: GestureDisposition) {
        _arena._resolve(_pointer, _member, disposition)
    }
}