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

package androidx.a2ui.model.protocol

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class A2uiClientDataModelTest {

    @Test
    fun toPayloadMap_formatsCorrectly() {
        val model =
            A2uiClientDataModel(
                surfaces =
                    mapOf("surface1" to mapOf("key" to "value"), "surface2" to listOf(1, 2, 3))
            )

        val expected =
            mapOf(
                "a2uiClientDataModel" to
                    mapOf(
                        "v0.9" to
                            mapOf(
                                "surfaces" to
                                    mapOf(
                                        "surface1" to mapOf("key" to "value"),
                                        "surface2" to listOf(1, 2, 3),
                                    )
                            )
                    )
            )

        assertEquals(expected, model.toPayloadMap())
    }

    @Test
    fun equalsAndHashCode() {
        val model1 = A2uiClientDataModel(surfaces = mapOf("s1" to "val1"))
        val model2 = A2uiClientDataModel(surfaces = mapOf("s1" to "val1"))
        val model3 = A2uiClientDataModel(surfaces = mapOf("s1" to "val2"))

        assertEquals(model1, model1)
        assertEquals(model1, model2)
        assertNotEquals(model1, model3)
        assertNotEquals(model1, null)
        assertNotEquals(model1, Any())

        assertEquals(model1.hashCode(), model2.hashCode())
        assertNotEquals(model1.hashCode(), model3.hashCode())
    }
}
