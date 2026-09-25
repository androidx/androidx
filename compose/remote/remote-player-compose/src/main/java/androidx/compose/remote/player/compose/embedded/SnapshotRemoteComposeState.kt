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
import androidx.compose.remote.core.operations.utilities.ArrayAccess
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
    private val overriddenFloats: SnapshotStateMap<Int, Boolean> = mutableStateMapOf()
    private val overriddenIntegers: SnapshotStateMap<Int, Boolean> = mutableStateMapOf()
    private val overriddenColors: SnapshotStateMap<Int, Boolean> = mutableStateMapOf()
    private val overriddenData: SnapshotStateMap<Int, Boolean> = mutableStateMapOf()

    /**
     * Re-entrancy depth for [withOpCountReset], matching the lifecycle of `RemoteContext.mOpCount`.
     */
    internal var opCountDepth: Int = 0

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
        colors[id] = super.getColor(id)
    }

    override fun updateFloat(id: Int, value: Float) {
        val old = floats[id]
        super.updateFloat(id, value)
        val new = super.getFloat(id)
        if (new != old) {
            floats[id] = new
            colors[id] = super.getColor(id)
        }
    }

    override fun overrideFloat(id: Int, value: Float) {
        val old = floats[id]
        super.overrideFloat(id, value)
        overriddenFloats[id] = true
        val new = super.getFloat(id)
        if (new != old) {
            floats[id] = new
            colors[id] = super.getColor(id)
        }
    }

    override fun clearFloatOverride(id: Int) {
        super.clearFloatOverride(id)
        overriddenFloats[id] = false
        floats[id] = super.getFloat(id)
        colors[id] = super.getColor(id)
    }

    /** Whether a host/action override should take precedence over the id's authored expression. */
    internal fun isFloatOverridden(id: Int): Boolean = overriddenFloats[id] == true

    // --- Integer ---
    override fun getInteger(id: Int): Int {
        if (id in integers) {
            return integers[id] ?: 0
        }
        // IDs below START_ID (42) are reserved for built-in system variables and literal enum
        // constants (e.g. Component.Visibility.VISIBLE = 1). Explicit integer updates are already
        // stored in `integers`, whereas `super.getInteger(id)` reads `mIntegerMap` which is also
        // mutated by `super.updateFloat`/`super.overrideFloat` for system float variables.
        if (id in 0 until RemoteComposeState.START_ID) {
            return id
        }
        integers[id] = super.getInteger(id)
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
        overriddenIntegers[id] = true
        val new = super.getInteger(id)
        if (new != old) {
            integers[id] = new
            floats[id] = super.getFloat(id)
            colors[id] = super.getColor(id)
        }
    }

    override fun clearIntegerOverride(id: Int) {
        super.clearIntegerOverride(id)
        overriddenIntegers[id] = false
        integers[id] = super.getInteger(id)
        floats[id] = super.getFloat(id)
        colors[id] = super.getColor(id)
    }

    /** Whether an integer override should take precedence over the id's authored expression. */
    internal fun isIntegerOverridden(id: Int): Boolean = overriddenIntegers[id] == true

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
        overriddenColors[id] = true
        val new = super.getColor(id)
        if (new != old) {
            colors[id] = new
            floats[id] = super.getFloat(id)
            integers[id] = super.getInteger(id)
        }
    }

    override fun updateColor(id: Int, color: Int) {
        val old = colors[id]
        super.updateColor(id, color)
        val new = super.getColor(id)
        if (new != old) {
            colors[id] = new
            floats[id] = super.getFloat(id)
            integers[id] = super.getInteger(id)
        }
    }

    override fun clearColorOverride() {
        super.clearColorOverride()
        overriddenColors.clear()
        for (id in colors.keys) {
            colors[id] = super.getColor(id)
        }
    }

    /** Whether a color override should take precedence over the id's authored expression. */
    internal fun isColorOverridden(id: Int): Boolean = overriddenColors[id] == true

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

    override fun updateData(id: Int, item: Any) {
        // Mirror into the snapshot-backed map like cacheData/overrideData: loadText (and any
        // other update of an *existing* data id) goes through here, and without the mirror the
        // stale snapshot entry keeps being served and nothing recomposes — the update would not
        // be visible until some unrelated write refreshed the id.
        val old = data[id]
        super.updateData(id, item)
        val new = super.getFromId(id)
        if (new != old && new != null) {
            data[id] = new
        }
    }

    override fun updateObject(id: Int, item: Any) {
        super.updateObject(id, item)
        objects[id] = item
    }

    override fun overrideData(id: Int, item: Any) {
        super.overrideData(id, item)
        overriddenData[id] = true
        data[id] = item
    }

    override fun clearDataOverride(id: Int) {
        super.clearDataOverride(id)
        overriddenData[id] = false
        val item = super.getFromId(id)
        if (item != null) {
            data[id] = item
        } else {
            data.remove(id)
        }
    }

    /** Whether a data override should take precedence over the id's authored expression. */
    internal fun isDataOverridden(id: Int): Boolean = overriddenData[id] == true

    // --- Collections ---
    private val collectionVersions: SnapshotStateMap<Int, Int> = mutableStateMapOf()

    override fun addCollection(
        id: Int,
        collection: ArrayAccess,
    ) {
        super.addCollection(id, collection)
        val maskedId = id and 0xFFFFF
        collectionVersions[maskedId] = (collectionVersions[maskedId] ?: 0) + 1
    }

    override fun getFloatValue(id: Int, index: Int): Float {
        val maskedId = id and 0xFFFFF
        collectionVersions[maskedId]
        return super.getFloatValue(id, index)
    }

    override fun getFloats(id: Int): FloatArray? {
        val maskedId = id and 0xFFFFF
        collectionVersions[maskedId]
        return super.getFloats(id)
    }

    override fun getDynamicFloats(id: Int): FloatArray? {
        val maskedId = id and 0xFFFFF
        collectionVersions[maskedId]
        return super.getDynamicFloats(id)
    }

    override fun getArray(id: Int): ArrayAccess? {
        val maskedId = id and 0xFFFFF
        collectionVersions[maskedId]
        return super.getArray(id)
    }

    override fun getListLength(id: Int): Int {
        val maskedId = id and 0xFFFFF
        collectionVersions[maskedId]
        return super.getListLength(id)
    }

    override fun getId(listId: Int, index: Int): Int {
        val maskedId = listId and 0xFFFFF
        collectionVersions[maskedId]
        return super.getId(listId, index)
    }

    override fun markVariableDirty(id: Int) {
        super.markVariableDirty(id)
        val maskedId = id and 0xFFFFF
        collectionVersions[maskedId] = (collectionVersions[maskedId] ?: 0) + 1
    }
}
