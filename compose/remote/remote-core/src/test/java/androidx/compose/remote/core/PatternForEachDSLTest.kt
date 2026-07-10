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

package androidx.compose.remote.core

import androidx.compose.remote.core.operations.DrawRect
import androidx.compose.remote.core.operations.Utils
import androidx.compose.remote.creation.RemoteComposeContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PatternForEachDSLTest {

    @Suppress("RestrictedApiAndroidX")
    @Test
    fun testMacroForEachDSL() {
        val context =
            RemoteComposeContext(
                100,
                100,
                "Test",
                8,
                RcProfiles.PROFILE_ANDROIDX or RcProfiles.PROFILE_EXPERIMENTAL,
                RcPlatformServices.None,
            ) {
                val collectionParam = definePatternParameter("items")
                val macroId =
                    definePattern("Repeater", collectionParam) {
                        val itemId = localId()
                        patternForEach(collectionParam, itemId) {
                            // Draw a rect with x coordinate being the item ID
                            drawRect(floatId(itemId), 0f, 10f, 10f)
                        }
                    }

                val myData = addDataListIds(intArrayOf(501, 502))
                root { inflatePattern(macroId, myData) {} }
            }

        val doc = CoreDocument()
        doc.initFromBuffer(context.buffer)

        val root =
            doc.operations.find {
                it is androidx.compose.remote.core.operations.layout.RootLayoutComponent
            } as androidx.compose.remote.core.operations.layout.RootLayoutComponent

        val expanded = root.list
        assertEquals(2, expanded.size)

        assertTrue(expanded[0] is DrawRect)
        assertEquals(501, Utils.idFromNan((expanded[0] as DrawRect).x1))

        assertTrue(expanded[1] is DrawRect)
        assertEquals(502, Utils.idFromNan((expanded[1] as DrawRect).x1))
    }
}
