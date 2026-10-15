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

package androidx.compose.remote.player.compose.embedded.modifier

import androidx.compose.remote.core.Operation
import androidx.compose.remote.core.WireBuffer
import androidx.compose.remote.core.operations.Utils
import androidx.compose.remote.core.operations.layout.modifiers.DimensionInModifierOperation
import androidx.compose.remote.core.operations.layout.modifiers.GraphicsLayerModifierOperation
import androidx.compose.remote.core.operations.layout.modifiers.OffsetModifierOperation
import androidx.compose.remote.core.operations.layout.modifiers.PaddingModifierOperation
import androidx.compose.remote.core.operations.layout.modifiers.WidthInModifierOperation
import org.junit.Assert.assertEquals
import org.junit.Test

class LayoutValueSourceTest {

    @Test
    fun offsetKeepsVariableIdsAfterCoreResolvesIt() {
        val op = OffsetModifierOperation(Utils.asNan(41), Utils.asNan(42))
        setFloat(op, "mXValue", 18f)
        setFloat(op, "mYValue", 24f)

        assertEquals(41, Utils.idFromNan(op.rawX))
        assertEquals(42, Utils.idFromNan(op.rawY))
        assertEquals(18f, op.x, 0f)
        assertEquals(24f, op.y, 0f)
    }

    @Test
    fun graphicsLayerKeepsTheVariableSourceInsteadOfItsCurrentValue() {
        val buffer = WireBuffer()
        val values = HashMap<Int, Any>()
        values[GraphicsLayerModifierOperation.ALPHA] = Utils.asNan(43)
        GraphicsLayerModifierOperation.apply(buffer, values)

        buffer.setIndex(1) // skip opcode byte
        val operations = ArrayList<Operation>()
        GraphicsLayerModifierOperation.read(buffer, operations)
        val operation = operations[0] as GraphicsLayerModifierOperation

        val source = operation.values[GraphicsLayerModifierOperation.ALPHA].source

        assertEquals(43, Utils.idFromNan(source))
    }

    @Test
    fun dimensionInKeepsVariableIdsAfterCoreResolvesIt() {
        val maxSource = Utils.asNan(42)
        val op = WidthInModifierOperation(12f, maxSource)
        setResolved(op, 24f, 0f)

        assertEquals(12f, op.rawMin, 0f)
        assertEquals(42, Utils.idFromNan(op.rawMax))
        assertEquals(24f, op.min, 0f)
        assertEquals(0f, op.max, 0f)
    }

    @Test
    fun paddingKeepsVariableIdsAfterCoreResolvesIt() {
        val op = PaddingModifierOperation(Utils.asNan(10), 5f, Utils.asNan(20), 15f)
        setFloat(op, "mLeftValue", 30f)
        setFloat(op, "mTopValue", 15f)
        setFloat(op, "mRightValue", 60f)
        setFloat(op, "mBottomValue", 45f)

        assertEquals(10, Utils.idFromNan(op.rawLeft))
        assertEquals(5f, op.rawTop, 0f)
        assertEquals(20, Utils.idFromNan(op.rawRight))
        assertEquals(15f, op.rawBottom, 0f)
        assertEquals(30f, op.left, 0f)
        assertEquals(15f, op.top, 0f)
        assertEquals(60f, op.right, 0f)
        assertEquals(45f, op.bottom, 0f)
    }

    private fun setFloat(target: Any, name: String, value: Float) {
        target.javaClass
            .getDeclaredField(name)
            .apply { isAccessible = true }
            .setFloat(target, value)
    }

    private fun setResolved(op: WidthInModifierOperation, min: Float, max: Float) {
        for ((name, value) in listOf("mV1" to min, "mV2" to max)) {
            DimensionInModifierOperation::class
                .java
                .getDeclaredField(name)
                .apply { isAccessible = true }
                .setFloat(op, value)
        }
    }
}
