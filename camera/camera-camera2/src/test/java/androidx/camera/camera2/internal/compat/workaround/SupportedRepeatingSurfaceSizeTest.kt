/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.camera.camera2.internal.compat.workaround

import android.os.Build
import android.util.Size
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument
import org.robolectric.util.ReflectionHelpers

@RunWith(ParameterizedRobolectricTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class SupportedRepeatingSurfaceSizeTest(
    private val brand: String,
    private val model: String,
    private val result_sizes: Array<Size>,
) {
    companion object {
        private val input_sizes =
            arrayOf(Size(176, 144), Size(208, 144), Size(320, 240), Size(352, 288), Size(400, 400))

        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "brand={0}, model={1}")
        fun data() = mutableListOf<Array<Any?>>().apply {
            add(
                arrayOf(
                    "Huawei",
                    "mha-l29",
                    arrayOf(Size(320, 240), Size(352, 288), Size(400, 400))
                )
            )
            add(arrayOf("Huawei", "Not_mha-l29", input_sizes))
            add(arrayOf("Not_Huawei", "mha-l29", input_sizes))
            add(arrayOf("Not_Huawei", "Not_mha-l29", input_sizes))
        }
    }

    @Before
    fun setup() {
        ReflectionHelpers.setStaticField(Build::class.java, "BRAND", brand)
        ReflectionHelpers.setStaticField(Build::class.java, "MODEL", model)
    }

    @Test
    fun getSurfaceSizes() {
        assertThat(
            SupportedRepeatingSurfaceSize().getSupportedSizes(input_sizes).asList()
        ).containsExactlyElementsIn(result_sizes)
    }
}