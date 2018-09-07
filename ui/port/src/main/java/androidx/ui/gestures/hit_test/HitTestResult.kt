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

package androidx.ui.gestures.hit_test

// /// The result of performing a hit test.
class HitTestResult(
    path: MutableList<HitTestEntry> = mutableListOf()
) {
    private val _path: MutableList<HitTestEntry> = path

    // /// An unmodifiable list of [HitTestEntry] objects recorded during the hit test.
    // ///
    // /// The first entry in the path is the most specific, typically the one at
    // /// the leaf of tree being hit tested. Event propagation starts with the most
    // /// specific (i.e., first) entry and proceeds in order through the path.
    val path: List<HitTestEntry> = _path

    // /// Add a [HitTestEntry] to the path.
    // ///
    // /// The new entry is added at the end of the path, which means entries should
    // /// be added in order from most specific to least specific, typically during an
    // /// upward walk of the tree being hit tested.
    fun add(entry: HitTestEntry) = _path.add(entry)

    override fun toString(): String =
        "HitTestResult(${if (_path.isEmpty()) "<empty path>" else _path.joinToString(", ")})"
}