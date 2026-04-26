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

import androidx.compose.remote.core.layout.CaptureComponentTree
import androidx.compose.remote.core.layout.TestOperation
import org.junit.Test

class PatternForEachLayoutTest : BaseLayoutTest() {

    init {
        GENERATE_GOLD_FILES = false
    }

    @Test
    fun testMacroForEachExpansionInLayout() {
        val ops =
            arrayListOf<TestOperation>(
                TestLayout {
                    // 1. Define a template that iterates over a collection
                    val collectionParam = definePatternParameter("items")
                    val macroId =
                        definePattern("ItemRepeater", collectionParam) {
                            column {
                                val itemId = localId()
                                patternForEach(collectionParam, itemId) {
                                    box(Modifier.size(100)) {
                                        // itemId will be the actual ID from the collection
                                        // We use it as text just for verification
                                        text(itemId)
                                    }
                                }
                            }
                        }

                    root {
                        column {
                            // 2. Data collection
                            val myData = addDataListIds(intArrayOf(textId("A"), textId("B")))

                            // 3. Call macro
                            inflatePattern(macroId, myData) {}
                        }
                    }
                },
                CaptureComponentTree(),
            )

        checkLayout(
            1000,
            1000,
            8,
            RcProfiles.PROFILE_ANDROIDX or RcProfiles.PROFILE_EXPERIMENTAL,
            "MacroForEachExpansion",
            ops,
            TestClock(1234),
        )
    }
}
