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

package androidx.camera.core.internal.compat.workaround

import android.os.Build
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.ParameterizedRobolectricTestRunner.Parameters
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument
import org.robolectric.util.ReflectionHelpers

/**
 * Unit test for [CaptureFailedRetryEnabler]
 */
@SmallTest
@RunWith(ParameterizedRobolectricTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class CaptureFailedRetryEnablerTest(
    private val brand: String,
    private val model: String,
    private val expectedResult: Int
) {

    companion object {
        @JvmStatic
        @Parameters(name = "brand={0}, model={1}, expectedResult={2}")
        fun data() = mutableListOf<Array<Any?>>().apply {
            add(arrayOf("SAMSUNG", "SM-G981U1", 1))
            add(arrayOf("samsung", "sm-g981u1", 1))
            add(arrayOf("samsung", "sm-g981u10", 0))
            add(arrayOf("samsung", "sm-g981u", 0))
            add(arrayOf("fakeBrand", "sm-g981u1", 0))
            add(arrayOf("", "", 0))
        }
    }

    @Before
    fun setup() {
        ReflectionHelpers.setStaticField(Build::class.java, "BRAND", brand)
        ReflectionHelpers.setStaticField(Build::class.java, "MODEL", model)
    }

    @Test
    fun shouldRetry() {
        assertThat(CaptureFailedRetryEnabler().retryCount).isEqualTo(expectedResult)
    }
}
