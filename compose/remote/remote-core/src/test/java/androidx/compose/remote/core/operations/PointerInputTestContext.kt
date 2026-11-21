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

package androidx.compose.remote.core.operations

import androidx.compose.remote.core.RemoteClock
import androidx.compose.remote.core.RemoteContext
import androidx.compose.remote.core.VariableSupport
import androidx.compose.remote.core.operations.utilities.ArrayAccess
import androidx.compose.remote.core.operations.utilities.DataMap

/**
 * Minimal [RemoteContext] for unit testing pointer input operations. Stores floats in a map and
 * records repaint requests and haptic effects.
 */
internal open class PointerInputTestContext(clock: RemoteClock = FakeClock()) :
    RemoteContext(clock) {
    val floatMap = HashMap<Int, Float>()
    var repaintRequested = false
    var hapticEffects = 0
    val hapticTypes = mutableListOf<Int>()

    override fun loadFloat(id: Int, value: Float) {
        floatMap[id] = value
    }

    override fun overrideFloat(id: Int, value: Float) {
        floatMap[id] = value
    }

    override fun getFloat(id: Int): Float = floatMap[id] ?: 0f

    override fun needsRepaint() {
        repaintRequested = true
    }

    override fun loadPathData(instanceId: Int, winding: Int, floatPath: FloatArray) {}

    override fun getPathData(instanceId: Int): FloatArray? = null

    override fun loadVariableName(varName: String, varId: Int, varType: Int) {}

    override fun loadColor(id: Int, color: Int) {}

    override fun setNamedColorOverride(colorName: String, color: Int) {}

    override fun setNamedStringOverride(stringName: String, value: String) {}

    override fun clearNamedStringOverride(stringName: String) {}

    override fun setNamedBooleanOverride(booleanName: String, value: Boolean) {}

    override fun clearNamedBooleanOverride(booleanName: String) {}

    override fun setNamedIntegerOverride(integerName: String, value: Int) {}

    override fun clearNamedIntegerOverride(integerName: String) {}

    override fun setNamedFloatOverride(floatName: String, value: Float) {}

    override fun clearNamedFloatOverride(floatName: String) {}

    override fun setNamedLong(name: String, value: Long) {}

    override fun setNamedDataOverride(dataName: String, value: Any) {}

    override fun clearNamedDataOverride(dataName: String) {}

    override fun addCollection(id: Int, collection: ArrayAccess) {}

    override fun putDataMap(id: Int, map: DataMap) {}

    override fun getDataMap(id: Int): DataMap? = null

    override fun runAction(id: Int, metadata: String) {}

    override fun runNamedAction(id: Int, value: Any?) {}

    override fun putObject(id: Int, value: Any) {}

    override fun getObject(id: Int): Any? = null

    override fun hapticEffect(type: Int) {
        hapticEffects++
        hapticTypes.add(type)
    }

    override fun loadBitmap(
        imageId: Int,
        encoding: Short,
        type: Short,
        width: Int,
        height: Int,
        bitmap: ByteArray,
    ) {}

    override fun loadText(id: Int, text: String) {}

    override fun getText(id: Int): String? = null

    override fun loadInteger(id: Int, value: Int) {}

    override fun overrideInteger(id: Int, value: Int) {}

    override fun overrideText(id: Int, valueId: Int) {}

    override fun loadAnimatedFloat(id: Int, animatedFloat: FloatExpression) {}

    override fun loadShader(id: Int, value: ShaderData) {}

    override fun getInteger(id: Int): Int = 0

    override fun getLong(id: Int): Long = 0L

    override fun getColor(id: Int): Int = 0

    override fun listensTo(id: Int, variableSupport: VariableSupport) {}

    override fun updateOps(): Int = 0

    override fun getShader(id: Int): ShaderData? = null

    override fun addClickArea(
        id: Int,
        contentDescriptionId: Int,
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        metadataId: Int,
    ) {}
}

/** A [RemoteClock] whose time only advances when [advance] is called. */
internal class FakeClock(var nowMillis: Long = 1_000_000L) : RemoteClock {
    fun advance(millis: Long) {
        nowMillis += millis
    }

    override fun millis(): Long = nowMillis

    override fun nanoTime(): Long = nowMillis * 1_000_000L

    override fun getZoneId(): String = RemoteClock.SYSTEM.zoneId

    override fun snapshot(millis: Long?): RemoteClock.TimeSnapshot =
        RemoteClock.SYSTEM.snapshot(millis ?: nowMillis)
}
