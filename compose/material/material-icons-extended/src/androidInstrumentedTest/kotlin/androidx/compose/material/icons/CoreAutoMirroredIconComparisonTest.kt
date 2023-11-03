/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.material.icons

import android.os.Build
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import kotlin.reflect.KProperty0
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * Test to ensure equality (both structurally, and visually) between programmatically generated core
 * Material [androidx.compose.material.icons.Icons.AutoMirrored] and their XML source.
 */
@Suppress("unused")
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
@LargeTest
@RunWith(Parameterized::class)
class CoreAutoMirroredIconComparisonTest(
    private val iconSublist: List<Pair<KProperty0<ImageVector>, String>>,
    private val debugParameterName: String
) : BaseIconComparisonTest() {

    companion object {

        @JvmStatic
        @Parameterized.Parameters(name = "{1}")
        fun initIconSublist(): Array<Array<Any>> {
            return arrayOf(arrayOf(AllCoreAutoMirroredIcons, "1 of 1"))
        }
    }

    @Test
    fun compareImageVectors() {
        compareImageVectors(iconSublist)
    }
}
