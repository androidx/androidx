/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.animation

import junit.framework.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class TypeConverterTest {
    @Test
    fun testFloatToVectorConverter() {
        verifyFloatConverter(FloatToVectorConverter)
        verifyFloatConverter(FloatPropKey().typeConverter)
        val holder = object : FloatValueHolder {
            override var value: Float = 0.0f
        }
        verifyFloatConverter(holder.typeConverter)
    }

    @Test
    fun testIntToVectorConverter() {
        assertEquals(100f, IntToVectorConverter.convertToVector(100).value)
        assertEquals(5, IntToVectorConverter.convertFromVector(AnimationVector1D(5f)))

        assertEquals(30f, IntPropKey().typeConverter.convertToVector(30).value)
        assertEquals(22, IntPropKey().typeConverter.convertFromVector(AnimationVector1D(22f)))
    }

    private fun verifyFloatConverter(converter: TwoWayConverter<Float, AnimationVector1D>) {
        assertEquals(15f, converter.convertToVector(15f).value)
        assertEquals(5f, converter.convertFromVector(AnimationVector1D(5f)))
    }
}