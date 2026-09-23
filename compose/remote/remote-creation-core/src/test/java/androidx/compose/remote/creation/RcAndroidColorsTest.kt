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

package androidx.compose.remote.creation

import java.lang.reflect.Modifier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RcAndroidColorsTest {

    @Test
    fun testAllAndroidColorsIndicesContiguousAndUnique() {
        val fields = Rc.AndroidColors::class.java.declaredFields
        val colorConstants = mutableMapOf<String, Short>()
        val indexToName = mutableMapOf<Short, String>()

        for (field in fields) {
            if (
                Modifier.isPublic(field.modifiers) &&
                    Modifier.isStatic(field.modifiers) &&
                    field.type == Short::class.javaPrimitiveType
            ) {
                val name = field.name
                val value = field.getShort(null)
                colorConstants[name] = value

                val existing = indexToName.put(value, name)
                assertTrue(
                    "Duplicate index $value for fields $name and $existing",
                    existing == null,
                )
            }
        }

        assertEquals("Expected exactly 196 theme color constants", 196, colorConstants.size)

        for (i in 0 until 196) {
            val shortIdx = i.toShort()
            assertTrue("Missing color constant for index $i", indexToName.containsKey(shortIdx))
        }

        assertEquals(0.toShort(), Rc.AndroidColors.BACKGROUND_DARK)
        assertEquals(31.toShort(), Rc.AndroidColors.SYSTEM_ACCENT2_200)
        assertEquals(62.toShort(), Rc.AndroidColors.SYSTEM_ERROR_10)
        assertEquals(63.toShort(), Rc.AndroidColors.SYSTEM_ERROR_100)
        assertEquals(64.toShort(), Rc.AndroidColors.SYSTEM_ERROR_1000)
        assertEquals(78.toShort(), Rc.AndroidColors.SYSTEM_NEUTRAL1_0)
        assertEquals(92.toShort(), Rc.AndroidColors.SYSTEM_NEUTRAL2_10)
        assertEquals(195.toShort(), Rc.AndroidColors.TAB_INDICATOR_TEXT)
    }
}
