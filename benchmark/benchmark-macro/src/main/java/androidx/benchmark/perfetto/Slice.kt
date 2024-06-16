/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.benchmark.perfetto

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class Slice(val name: String, val ts: Long, val dur: Long) {
    val endTs: Long = ts + dur

    val frameId: Int?

    init {
        val firstSpaceIndex = name.indexOf(" ")
        frameId =
            if (firstSpaceIndex >= 0) {
                // if see a space, check for id from end of first space to next space (or end of
                // String)
                val secondSpaceIndex = name.indexOf(" ", firstSpaceIndex + 1)
                val endFrameIdIndex = if (secondSpaceIndex < 0) name.length else secondSpaceIndex
                name.substring(firstSpaceIndex + 1, endFrameIdIndex).toIntOrNull()
            } else {
                null
            }
    }

    fun contains(targetTs: Long): Boolean {
        return targetTs >= ts && targetTs <= (ts + dur)
    }
}

/**
 * Convenient function to immediately retrieve a list of slices. Note that this method is provided
 * for convenience.
 */
internal fun Sequence<Row>.toSlices(): List<Slice> =
    map { Slice(name = it.string("name"), ts = it.long("ts"), dur = it.long("dur")) }.toList()
