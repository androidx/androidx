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

@file:Suppress("RestrictedApiAndroidX")

package androidx.compose.remote.player.compose.embedded

import androidx.compose.remote.core.RemoteClock
import androidx.compose.remote.core.RemoteComposeState
import androidx.compose.remote.core.RemoteContext
import androidx.compose.remote.core.TouchListener
import androidx.compose.remote.core.VariableSupport
import androidx.compose.remote.core.operations.FloatExpression
import androidx.compose.remote.core.operations.ShaderData
import androidx.compose.remote.core.operations.utilities.ArrayAccess
import androidx.compose.remote.core.operations.utilities.DataMap
import androidx.compose.remote.core.types.LongConstant
import androidx.compose.ui.util.fastForEach
import java.util.ArrayList
import java.util.HashMap
import java.util.NoSuchElementException

/**
 * A platform-neutral [RemoteContext] implementation backed by the core [RemoteComposeState] store.
 *
 * This provides the store-backed implementations of variable management, collections, and
 * operations without depending on Android platform classes.
 */
internal open class StoreBackedRemoteContext(clock: RemoteClock) : RemoteContext(clock) {

    private class VarName(val name: String, val id: Int, val type: Int)

    private val varNameHashMap = HashMap<String, ArrayList<VarName>>()

    override fun clearVariables() {
        varNameHashMap.clear()
    }

    fun getVariableId(name: String): Int {
        val list = varNameHashMap[name]
        if (list.isNullOrEmpty()) {
            throw NoSuchElementException("Variable $name not found")
        }
        return list[0].id
    }

    fun getStringVariableName(name: String): String? {
        val id = getVariableId(name)
        return getText(id)
    }

    override fun loadVariableName(varName: String, varId: Int, varType: Int) {
        val list = varNameHashMap.getOrPut(varName) { ArrayList() }
        for (i in list.indices) {
            if (list[i].id == varId) return
        }
        list.add(VarName(varName, varId, varType))
    }

    override fun loadPathData(instanceId: Int, winding: Int, floatPath: FloatArray) {
        mRemoteComposeState.putPathData(instanceId, floatPath)
        mRemoteComposeState.putPathWinding(instanceId, winding)
    }

    override fun getPathData(instanceId: Int): FloatArray? {
        return mRemoteComposeState.getPathData(instanceId)
    }

    override fun setNamedStringOverride(stringName: String, value: String) {
        varNameHashMap[stringName]?.fastForEach { overrideText(it.id, value) }
    }

    override fun clearNamedStringOverride(stringName: String) {
        varNameHashMap[stringName]?.fastForEach { clearDataOverride(it.id) }
    }

    override fun setNamedBooleanOverride(booleanName: String, value: Boolean) {
        setNamedIntegerOverride(booleanName, if (value) 1 else 0)
    }

    override fun clearNamedBooleanOverride(booleanName: String) {
        clearNamedIntegerOverride(booleanName)
    }

    override fun setNamedIntegerOverride(integerName: String, value: Int) {
        varNameHashMap[integerName]?.fastForEach { overrideInt(it.id, value) }
    }

    override fun clearNamedIntegerOverride(integerName: String) {
        varNameHashMap[integerName]?.fastForEach { clearIntegerOverride(it.id) }
    }

    override fun setNamedFloatOverride(floatName: String, value: Float) {
        varNameHashMap[floatName]?.fastForEach { overrideFloat(it.id, value) }
    }

    override fun clearNamedFloatOverride(floatName: String) {
        varNameHashMap[floatName]?.fastForEach { clearFloatOverride(it.id) }
    }

    override fun setNamedLong(name: String, value: Long) {
        varNameHashMap[name]?.fastForEach {
            val longConstant = mRemoteComposeState.getObject(it.id) as? LongConstant
            longConstant?.value = value
        }
    }

    override fun setNamedDataOverride(dataName: String, value: Any) {
        varNameHashMap[dataName]?.fastForEach { overrideData(it.id, value) }
    }

    override fun clearNamedDataOverride(dataName: String) {
        varNameHashMap[dataName]?.fastForEach { clearDataOverride(it.id) }
    }

    override fun setNamedColorOverride(colorName: String, color: Int) {
        varNameHashMap[colorName]?.fastForEach { mRemoteComposeState.overrideColor(it.id, color) }
    }

    override fun addCollection(id: Int, collection: ArrayAccess) {
        mRemoteComposeState.addCollection(id, collection)
    }

    override fun putDataMap(id: Int, map: DataMap) {
        mRemoteComposeState.putDataMap(id, map)
    }

    override fun getDataMap(id: Int): DataMap? {
        return mRemoteComposeState.getDataMap(id)
    }

    override fun runAction(id: Int, metadata: String) {
        mDocument.performClick(this, id, metadata)
    }

    override fun runNamedAction(id: Int, value: Any?) {
        val text = getText(id)
        if (text != null) {
            mDocument.runNamedAction(text, value)
        }
    }

    override fun loadBitmap(
        imageId: Int,
        encoding: Short,
        type: Short,
        width: Int,
        height: Int,
        data: ByteArray,
    ) {
        // No-op in platform-neutral base context
    }

    override fun loadText(id: Int, text: String) {
        if (!mRemoteComposeState.containsId(id)) {
            mRemoteComposeState.cacheData(id, text)
        } else {
            mRemoteComposeState.updateData(id, text)
        }
    }

    fun overrideText(id: Int, text: String) {
        mRemoteComposeState.overrideData(id, text)
    }

    fun overrideInt(id: Int, value: Int) {
        mRemoteComposeState.overrideInteger(id, value)
    }

    fun overrideData(id: Int, value: Any) {
        mRemoteComposeState.overrideData(id, value)
    }

    fun clearDataOverride(id: Int) {
        mRemoteComposeState.clearDataOverride(id)
    }

    fun clearIntegerOverride(id: Int) {
        mRemoteComposeState.clearIntegerOverride(id)
    }

    fun clearFloatOverride(id: Int) {
        mRemoteComposeState.clearFloatOverride(id)
    }

    override fun getText(id: Int): String? {
        return mRemoteComposeState.getFromId(id) as? String
    }

    override fun loadFloat(id: Int, value: Float) {
        mRemoteComposeState.updateFloat(id, value)
    }

    override fun overrideFloat(id: Int, value: Float) {
        mRemoteComposeState.overrideFloat(id, value)
    }

    override fun loadInteger(id: Int, value: Int) {
        mRemoteComposeState.updateInteger(id, value)
    }

    override fun markVariableDirty(id: Int) {
        mRemoteComposeState.markVariableDirty(id)
    }

    override fun overrideInteger(id: Int, value: Int) {
        mRemoteComposeState.overrideInteger(id, value)
    }

    override fun overrideText(id: Int, valueId: Int) {
        val text = getText(valueId)
        if (text != null) {
            overrideText(id, text)
        }
    }

    override fun loadColor(id: Int, color: Int) {
        mRemoteComposeState.updateColor(id, color)
    }

    override fun loadAnimatedFloat(id: Int, animatedFloat: FloatExpression) {
        mRemoteComposeState.cacheData(id, animatedFloat)
    }

    override fun loadShader(id: Int, value: ShaderData) {
        mRemoteComposeState.cacheData(id, value)
    }

    override fun getFloat(id: Int): Float {
        return mRemoteComposeState.getFloat(id)
    }

    override fun putObject(id: Int, value: Any) {
        mRemoteComposeState.updateObject(id, value)
    }

    override fun getObject(id: Int): Any? {
        return mRemoteComposeState.getObject(id)
    }

    override fun getInteger(id: Int): Int {
        return mRemoteComposeState.getInteger(id)
    }

    override fun getLong(id: Int): Long {
        return (mRemoteComposeState.getObject(id) as? LongConstant)?.value ?: 0L
    }

    override fun getColor(id: Int): Int {
        return mRemoteComposeState.getColor(id)
    }

    override fun listensTo(id: Int, variableSupport: VariableSupport) {
        mRemoteComposeState.listenToVar(id, variableSupport)
    }

    override fun getListeners(id: Int): ArrayList<VariableSupport>? {
        return mRemoteComposeState.getListeners(id)
    }

    override fun updateOps(): Int {
        return mRemoteComposeState.getOpsToUpdate(this, currentTime)
    }

    override fun getShader(id: Int): ShaderData? {
        return mRemoteComposeState.getFromId(id) as? ShaderData
    }

    override fun addTouchListener(touchExpression: TouchListener) {
        mDocument.addTouchListener(touchExpression)
    }

    override fun addClickArea(
        id: Int,
        contentDescriptionId: Int,
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        metadataId: Int,
    ) {
        val contentDescription = mRemoteComposeState.getFromId(contentDescriptionId) as? String
        val metadata = mRemoteComposeState.getFromId(metadataId) as? String
        mDocument.addClickArea(id, contentDescription, left, top, right, bottom, metadata)
    }

    override fun hapticEffect(type: Int) {
        mDocument.haptic(type)
    }

    override fun loadSound(soundId: Int, data: ByteArray) {
        mDocument.loadSound(soundId, data)
    }

    override fun playSound(soundId: Int) {
        mDocument.playSound(soundId)
    }
}
