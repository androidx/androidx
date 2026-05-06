/*
 * Copyright 2025 The Android Open Source Project
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
package androidx.compose.remote.player.compose.context

import androidx.compose.remote.core.RemoteClock
import androidx.compose.remote.core.RemoteContext
import androidx.compose.remote.core.VariableSupport
import androidx.compose.remote.core.operations.FloatExpression
import androidx.compose.remote.core.operations.ShaderData
import androidx.compose.remote.core.operations.utilities.ArrayAccess
import androidx.compose.remote.core.operations.utilities.DataMap
import androidx.compose.remote.core.types.LongConstant
import androidx.compose.remote.player.core.platform.BitmapLoader
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType

/**
 * An implementation of [RemoteContext] for
 * [androidx.compose.remote.player.compose.impl.RemoteComposePlayer].
 */
internal class ComposeRemoteContext(clock: RemoteClock) : RemoteContext(clock) {
    private lateinit var haptic: HapticFeedback
    private var varNameHashMap: HashMap<String, VarName?> = HashMap<String, VarName?>()

    public var a11yAnimationEnabled = true

    private var bitmapLoader: BitmapLoader = BitmapLoader.UNSUPPORTED

    /**
     * Sets the BitmapLoader to be used by the RemoteContext for loading bitmaps from URLs. This is
     * useful when you want to provide a custom way of loading bitmaps, for example, from a network
     * cache or a local file system.
     *
     * @param bitmapLoader The BitmapLoader to be used.
     */
    fun setBitmapLoader(bitmapLoader: BitmapLoader) {
        this.bitmapLoader = bitmapLoader
    }

    override fun loadPathData(instanceId: Int, winding: Int, floatPath: FloatArray) {
        mRemoteComposeState.putPathData(instanceId, floatPath)
        mRemoteComposeState.putPathWinding(instanceId, winding)
    }

    override fun getPathData(instanceId: Int): FloatArray? {
        return mRemoteComposeState.getPathData(instanceId)
    }

    override fun loadVariableName(varName: String, varId: Int, varType: Int) {
        varNameHashMap.put(varName, VarName(varName, varId, varType))
    }

    override fun loadColor(id: Int, color: Int) {
        mRemoteComposeState.updateColor(id, color)
    }

    override fun setNamedColorOverride(colorName: String, color: Int) {
        val id = varNameHashMap[colorName]!!.id
        mRemoteComposeState.overrideColor(id, color)
    }

    override fun setNamedStringOverride(stringName: String, value: String) {
        if (varNameHashMap[stringName] != null) {
            val id = varNameHashMap[stringName]!!.id
            overrideText(id, value)
        }
    }

    fun clearDataOverride(id: Int) {
        mRemoteComposeState.clearDataOverride(id)
    }

    fun overrideInt(id: Int, value: Int) {
        mRemoteComposeState.overrideInteger(id, value)
    }

    fun clearIntegerOverride(id: Int) {
        mRemoteComposeState.clearIntegerOverride(id)
    }

    fun clearFloatOverride(id: Int) {
        mRemoteComposeState.clearFloatOverride(id)
    }

    fun overrideData(id: Int, value: Any?) {
        mRemoteComposeState.overrideData(id, value!!)
    }

    override fun clearNamedStringOverride(stringName: String) {
        if (varNameHashMap[stringName] != null) {
            val id = varNameHashMap[stringName]!!.id
            clearDataOverride(id)
        }
        varNameHashMap[stringName] = null
    }

    override fun setNamedBooleanOverride(booleanName: String, value: Boolean) {
        setNamedIntegerOverride(booleanName, if (value) 1 else 0)
    }

    override fun clearNamedBooleanOverride(booleanName: String) {
        clearNamedIntegerOverride(booleanName)
    }

    override fun setNamedIntegerOverride(integerName: String, value: Int) {
        if (varNameHashMap[integerName] != null) {
            val id = varNameHashMap[integerName]!!.id
            overrideInt(id, value)
        }
    }

    override fun clearNamedIntegerOverride(integerName: String) {
        if (varNameHashMap[integerName] != null) {
            val id = varNameHashMap[integerName]!!.id
            clearIntegerOverride(id)
        }
        varNameHashMap[integerName] = null
    }

    override fun setNamedFloatOverride(floatName: String, value: Float) {
        if (varNameHashMap[floatName] != null) {
            val id = varNameHashMap[floatName]!!.id
            overrideFloat(id, value)
        }
    }

    override fun clearNamedFloatOverride(floatName: String) {
        if (varNameHashMap[floatName] != null) {
            val id = varNameHashMap[floatName]!!.id
            clearFloatOverride(id)
        }
        varNameHashMap[floatName] = null
    }

    override fun setNamedLong(name: String, value: Long) {
        val entry = varNameHashMap[name]
        if (entry != null) {
            val id = entry.id
            val longConstant = mRemoteComposeState.getObject(id) as LongConstant?
            longConstant!!.value = value
        }
    }

    override fun setNamedDataOverride(dataName: String, value: Any) {
        if (varNameHashMap[dataName] != null) {
            val id = varNameHashMap[dataName]!!.id
            overrideData(id, value)
        }
    }

    override fun clearNamedDataOverride(dataName: String) {
        if (varNameHashMap[dataName] != null) {
            val id = varNameHashMap[dataName]!!.id
            clearDataOverride(id)
        }
        varNameHashMap[dataName] = null
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
        mDocument.runNamedAction(text!!, value)
    }

    override fun putObject(id: Int, value: Any) {
        mRemoteComposeState.updateObject(id, value)
    }

    override fun getObject(id: Int): Any? {
        return mRemoteComposeState.getObject(id)
    }

    override fun hapticEffect(type: Int) {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    override fun loadBitmap(
        imageId: Int,
        encoding: Short,
        type: Short,
        width: Int,
        height: Int,
        bitmap: ByteArray,
    ) {
        if (!mRemoteComposeState.containsId(imageId)) {
            val image =
                androidx.compose.remote.player.core.platform.RemoteBitmapDecoder.decodeBitmap(
                    imageId,
                    encoding,
                    type,
                    width,
                    height,
                    bitmap,
                    bitmapLoader,
                )
            if (image != null) {
                mRemoteComposeState.cacheData(imageId, image)
            }
        }
    }

    override fun loadText(id: Int, text: String) {
        if (!mRemoteComposeState.containsId(id)) {
            mRemoteComposeState.cacheData(id, text)
        } else {
            mRemoteComposeState.updateData(id, text)
        }
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

    override fun overrideInteger(id: Int, value: Int) {
        mRemoteComposeState.overrideInteger(id, value)
    }

    fun overrideText(id: Int, text: String?) {
        mRemoteComposeState.overrideData(id, text!!)
    }

    override fun overrideText(id: Int, valueId: Int) {
        val text = getText(valueId)
        overrideText(id, text)
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

    override fun getInteger(id: Int): Int {
        return mRemoteComposeState.getInteger(id)
    }

    override fun getLong(id: Int): Long {
        return (mRemoteComposeState.getObject(id) as LongConstant?)!!.value
    }

    override fun getColor(id: Int): Int {
        return mRemoteComposeState.getColor(id)
    }

    override fun listensTo(id: Int, variableSupport: VariableSupport) {
        mRemoteComposeState.listenToVar(id, variableSupport)
    }

    override fun updateOps(): Int {
        return mRemoteComposeState.getOpsToUpdate(this, currentTime)
    }

    override fun getShader(id: Int): ShaderData? {
        return mRemoteComposeState.getFromId(id) as ShaderData?
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
        val contentDescription = mRemoteComposeState.getFromId(contentDescriptionId) as String?
        val metadata = mRemoteComposeState.getFromId(metadataId) as String?
        mDocument.addClickArea(id, contentDescription, left, top, right, bottom, metadata)
    }

    fun setHaptic(haptic: HapticFeedback) {
        this@ComposeRemoteContext.haptic = haptic
    }

    override fun isAnimationEnabled(): Boolean =
        if (a11yAnimationEnabled) {
            super.isAnimationEnabled()
        } else {
            false
        }
}

private data class VarName(val name: String, val id: Int, val type: Int)
