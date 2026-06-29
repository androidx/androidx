/*
 * Copyright 2026 The Android Open Source Project
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

@file:Suppress("RestrictedApiAndroidX", "PrimitiveInCollection")

package androidx.compose.remote.player.compose.embedded

import androidx.compose.remote.core.RemoteComposeState
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.snapshots.SnapshotStateMap

/**
 * A [RemoteComposeState] whose reactive scalar caches (float / integer / color) are backed by
 * Compose [SnapshotStateMap]s instead of plain maps, by overriding the public getter/setter APIs.
 *
 * This makes Compose the single source of truth for those variables: a read through `getFloat` /
 * `getInteger` / `getColor` performed inside composition, layout, or draw is recorded as a snapshot
 * dependency, and a write (the core's `updateFloat`/`overrideFloat`/… as host actions, value
 * changes, or expression evaluation run) invalidates exactly those readers.
 */
internal class SnapshotRemoteComposeState : RemoteComposeState() {
    private val floats: SnapshotStateMap<Int, Float> = mutableStateMapOf()
    private val integers: SnapshotStateMap<Int, Int> = mutableStateMapOf()
    private val colors: SnapshotStateMap<Int, Int> = mutableStateMapOf()
    private val data: SnapshotStateMap<Int, Any> = mutableStateMapOf()
    private val objects: SnapshotStateMap<Int, Any> = mutableStateMapOf()

    // --- Float ---
    override fun getFloat(id: Int): Float {
        if (id !in floats) {
            floats[id] = super.getFloat(id)
        }
        return floats[id] ?: 0f
    }

    override fun cacheFloat(id: Int, item: Float) {
        super.cacheFloat(id, item)
        floats[id] = super.getFloat(id)
        integers[id] = super.getInteger(id)
        colors[id] = super.getColor(id)
    }

    override fun updateFloat(id: Int, value: Float) {
        val old = floats[id]
        super.updateFloat(id, value)
        val new = super.getFloat(id)
        if (new != old) {
            floats[id] = new
            integers[id] = super.getInteger(id)
            colors[id] = super.getColor(id)
        }
    }

    override fun overrideFloat(id: Int, value: Float) {
        val old = floats[id]
        super.overrideFloat(id, value)
        val new = super.getFloat(id)
        if (new != old) {
            floats[id] = new
            integers[id] = super.getInteger(id)
            colors[id] = super.getColor(id)
        }
    }

    // --- Integer ---
    override fun getInteger(id: Int): Int {
        if (id !in integers) {
            integers[id] = super.getInteger(id)
        }
        return integers[id] ?: 0
    }

    override fun updateInteger(id: Int, value: Int) {
        val old = integers[id]
        super.updateInteger(id, value)
        val new = super.getInteger(id)
        if (new != old) {
            integers[id] = new
            floats[id] = super.getFloat(id)
            colors[id] = super.getColor(id)
        }
    }

    override fun overrideInteger(id: Int, value: Int) {
        val old = integers[id]
        super.overrideInteger(id, value)
        val new = super.getInteger(id)
        if (new != old) {
            integers[id] = new
            floats[id] = super.getFloat(id)
            colors[id] = super.getColor(id)
        }
    }

    // --- Color ---
    override fun getColor(id: Int): Int {
        if (id !in colors) {
            colors[id] = super.getColor(id)
        }
        return colors[id] ?: 0
    }

    override fun overrideColor(id: Int, color: Int) {
        val old = colors[id]
        super.overrideColor(id, color)
        val new = super.getColor(id)
        if (new != old) {
            colors[id] = new
            floats[id] = super.getFloat(id)
            integers[id] = super.getInteger(id)
        }
    }

    // --- Data Object ---
    override fun getFromId(id: Int): Any? {
        if (id !in data) {
            val item = super.getFromId(id)
            if (item != null) {
                data[id] = item
            }
        }
        return data[id]
    }

    override fun getObject(id: Int): Any? {
        if (id !in objects) {
            val item = super.getObject(id)
            if (item != null) {
                objects[id] = item
            }
        }
        return objects[id]
    }

    override fun cacheData(id: Int, item: Any) {
        super.cacheData(id, item)
        data[id] = item
    }

    override fun updateObject(id: Int, item: Any) {
        super.updateObject(id, item)
        objects[id] = item
    }

    override fun overrideData(id: Int, item: Any) {
        super.overrideData(id, item)
        data[id] = item
    }
}
