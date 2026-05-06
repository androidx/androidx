/*
 * Copyright 2024 The Android Open Source Project
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

import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.RcProfiles
import androidx.compose.remote.core.RemoteClock
import androidx.compose.remote.core.RemoteComposeBuffer
import androidx.compose.remote.core.RemoteContext
import androidx.compose.remote.core.VariableSupport
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ReferencedOperationsTest {

    private class TestRemoteContext : RemoteContext(RemoteClock.SYSTEM) {
        private val objects = mutableMapOf<Int, Any>()

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

        override fun addCollection(
            id: Int,
            collection: androidx.compose.remote.core.operations.utilities.ArrayAccess,
        ) {}

        override fun putDataMap(
            id: Int,
            map: androidx.compose.remote.core.operations.utilities.DataMap,
        ) {}

        override fun getDataMap(
            id: Int
        ): androidx.compose.remote.core.operations.utilities.DataMap? = null

        override fun runAction(id: Int, metadata: String) {}

        override fun runNamedAction(id: Int, value: Any?) {}

        override fun putObject(id: Int, value: Any) {
            objects[id] = value
        }

        override fun getObject(id: Int): Any? = objects[id]

        override fun hapticEffect(type: Int) {}

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

        override fun loadFloat(id: Int, value: Float) {}

        override fun overrideFloat(id: Int, value: Float) {}

        override fun loadInteger(id: Int, value: Int) {}

        override fun overrideInteger(id: Int, value: Int) {}

        override fun overrideText(id: Int, valueId: Int) {}

        override fun loadAnimatedFloat(id: Int, animatedFloat: FloatExpression) {}

        override fun loadShader(id: Int, value: ShaderData) {}

        override fun getFloat(id: Int): Float = 0f

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

    @Test
    fun testReferencedOperations() {
        val buffer = RemoteComposeBuffer()
        val containerId = 100

        // Header
        // buffer.addHeader(shortArrayOf(), arrayOf())
        buffer.addHeader(
            shortArrayOf(Header.DOC_PROFILES),
            arrayOf(RcProfiles.PROFILE_ANDROIDX or RcProfiles.PROFILE_EXPERIMENTAL),
        )

        // Referenced Operations
        buffer.addReferencedOperations(containerId)
        buffer.addDrawRect(0f, 0f, 100f, 100f)
        buffer.addDrawCircle(50f, 50f, 50f)
        buffer.addContainerEnd()

        // Document init
        val doc = CoreDocument()
        doc.initFromBuffer(buffer)

        // Context init
        val context = TestRemoteContext()
        doc.initializeContext(context)

        // Reference the container
        val obj = context.getObject(containerId)
        assertThat(obj).isInstanceOf(ReferencedOperations::class.java)

        val namedOps = obj as ReferencedOperations
        assertThat(namedOps.id).isEqualTo(containerId)
        assertThat(namedOps.list).hasSize(2)
        assertThat(namedOps.list[0]).isInstanceOf(DrawRect::class.java)
        assertThat(namedOps.list[1]).isInstanceOf(DrawCircle::class.java)
    }
}
