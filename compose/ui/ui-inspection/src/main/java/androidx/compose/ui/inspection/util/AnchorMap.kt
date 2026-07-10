/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.compose.ui.inspection.util

import androidx.collection.mutableIntObjectMapOf

const val NO_ANCHOR_ID = 0

/** A map of anchors with a unique id generator. */
class AnchorMap {
    private val anchorLookup = mutableIntObjectMapOf<Any>()
    private val dataLookup = mutableMapOf<Any, AnchorData>()

    /** Return a unique id for the specified [anchor] instance. */
    operator fun get(anchor: Any?, key: Int): Int =
        anchor?.let { dataLookup.getOrPut(it) { generateUniqueId(it, key) } }?.id ?: NO_ANCHOR_ID

    /** Return the anchor associated with a given unique anchor [id]. */
    operator fun get(id: Int): Any? = anchorLookup[id]

    /** Return the key associated with a given anchor. */
    fun getKey(anchor: Any?): Int = anchor?.let { dataLookup[anchor] }?.key ?: 0

    private fun generateUniqueId(anchor: Any, key: Int): AnchorData {
        var id = anchor.hashCode()
        while (id == NO_ANCHOR_ID || anchorLookup.containsKey(id)) {
            id++
        }
        val data = AnchorData(id, key)
        anchorLookup[id] = anchor
        dataLookup[anchor] = data
        return data
    }

    /**
     * Data stored by anchor instance:
     *
     * @param id the generated unique id for this anchor
     * @param key the key of the CompositionGroup that is also used in CompositionTracer
     */
    private class AnchorData(val id: Int, val key: Int)
}
