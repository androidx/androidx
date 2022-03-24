/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.benchmark.macro

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class FrameStatsResultTest {
    @Test
    fun parse() {
        assertEquals(
            listOf(
                FrameStatsResult(
                    uniqueName = "com.pkg/com.pkg.MyActivity1/android.view.ViewRootImpl@ade24ea",
                    lastFrameNs = 4211995467212,
                    lastLaunchNs = 3211995467212
                ),
                FrameStatsResult(
                    uniqueName = "com.pkg/com.pkg.MyActivity2/android.view.ViewRootImpl@e8a2229b",
                    lastFrameNs = 6117484488193,
                    lastLaunchNs = 5117484488193
                )
            ),
            FrameStatsResult.parse(
                """
                    com.pkg/com.pkg.MyActivity1/android.view.ViewRootImpl@ade24ea (visibility=8)
                    Window: com.pkg/com.pkg.MyActivity1
                    ---PROFILEDATA---
                    Flags,IntendedVsync,Vsync,
                    1,3211995467212,3212028800544,
                    0,4211995467212,2212028800544,
                    ---PROFILEDATA---

                    // this should be ignored
                    1,9999999999999,9999999999999,
                    
                    com.pkg/com.pkg.MyActivity2/android.view.ViewRootImpl@e8a2229b (visibility=8)
                    Window: com.pkg/com.pkg.MyActivity2

                    ---PROFILEDATA---
                    Flags,IntendedVsync,Vsync,
                    1,5117484488193,5117484488194,
                    0,6117484488193,5117484488194,
                    ---PROFILEDATA---

                    // this should be ignored too
                    com.pkg/com.pkg.MyActivity3/android.view.ViewRootImpl@8a8ebbbc (visibility=8)
                    Window: com.pkg/com.pkg.MyActivity3
                """.trimIndent()
            )
        )
    }
}
