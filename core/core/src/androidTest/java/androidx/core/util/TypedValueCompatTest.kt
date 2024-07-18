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
package androidx.core.util

import android.util.DisplayMetrics
import android.util.TypedValue
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock

@RunWith(AndroidJUnit4::class)
@SmallTest
class TypedValueCompatTest {
    @Test
    fun invalidUnitThrows() {
        val metrics: DisplayMetrics = mock(DisplayMetrics::class.java)
        val fontScale = 2f
        metrics.density = 1f
        metrics.xdpi = 2f
        metrics.scaledDensity = fontScale * metrics.density

        assertThrows(IllegalArgumentException::class.java) {
            TypedValueCompat.deriveDimension(TypedValue.COMPLEX_UNIT_MM + 1, 23f, metrics)
        }
    }

    @Test
    fun density0_deriveDoesNotCrash() {
        val metrics: DisplayMetrics = mock(DisplayMetrics::class.java)
        metrics.density = 0f
        metrics.xdpi = 0f
        metrics.scaledDensity = 0f

        listOf(
                TypedValue.COMPLEX_UNIT_DIP,
                TypedValue.COMPLEX_UNIT_SP,
                TypedValue.COMPLEX_UNIT_PT,
                TypedValue.COMPLEX_UNIT_IN,
                TypedValue.COMPLEX_UNIT_MM
            )
            .forEach { dimenType ->
                assertThat(TypedValueCompat.deriveDimension(dimenType, 23f, metrics)).isEqualTo(0)
            }
    }

    @Test
    fun scaledDensity0_deriveSpDoesNotCrash() {
        val metrics: DisplayMetrics = mock(DisplayMetrics::class.java)
        metrics.density = 1f
        metrics.xdpi = 2f
        metrics.scaledDensity = 0f

        assertThat(TypedValueCompat.deriveDimension(TypedValue.COMPLEX_UNIT_SP, 23f, metrics))
            .isEqualTo(0)
    }

    @SdkSuppress(minSdkVersion = 34)
    @Test
    fun deriveDimensionMatchesRealVersion() {
        val metrics: DisplayMetrics = mock(DisplayMetrics::class.java)
        metrics.density = 1f
        metrics.xdpi = 2f
        metrics.scaledDensity = 2f

        listOf(
                TypedValue.COMPLEX_UNIT_PX,
                TypedValue.COMPLEX_UNIT_DIP,
                TypedValue.COMPLEX_UNIT_SP,
                TypedValue.COMPLEX_UNIT_PT,
                TypedValue.COMPLEX_UNIT_IN,
                TypedValue.COMPLEX_UNIT_MM
            )
            .forEach { dimenType ->
                for (i: Int in -1000 until 1000) {
                    assertThat(TypedValueCompat.deriveDimension(dimenType, i.toFloat(), metrics))
                        .isWithin(0.05f)
                        .of(TypedValue.deriveDimension(dimenType, i.toFloat(), metrics))
                }
            }
    }

    @Test
    fun eachUnitType_roundTripIsEqual() {
        val metrics: DisplayMetrics = mock(DisplayMetrics::class.java)
        metrics.density = 1f
        metrics.xdpi = 2f
        metrics.scaledDensity = 2f

        listOf(
                TypedValue.COMPLEX_UNIT_PX,
                TypedValue.COMPLEX_UNIT_DIP,
                TypedValue.COMPLEX_UNIT_SP,
                TypedValue.COMPLEX_UNIT_PT,
                TypedValue.COMPLEX_UNIT_IN,
                TypedValue.COMPLEX_UNIT_MM
            )
            .forEach { dimenType ->
                for (i: Int in -10000 until 10000) {
                    assertRoundTripIsEqual(i.toFloat(), dimenType, metrics)
                    assertRoundTripIsEqual(i - .1f, dimenType, metrics)
                    assertRoundTripIsEqual(i + .5f, dimenType, metrics)
                }
            }
    }

    @Test
    fun convenienceFunctionsCallCorrectAliases() {
        val metrics: DisplayMetrics = mock(DisplayMetrics::class.java)
        metrics.density = 1f
        metrics.xdpi = 2f
        metrics.scaledDensity = 2f

        assertThat(TypedValueCompat.pxToDp(20f, metrics))
            .isWithin(0.05f)
            .of(TypedValueCompat.deriveDimension(TypedValue.COMPLEX_UNIT_DIP, 20f, metrics))
        assertThat(TypedValueCompat.pxToSp(20f, metrics))
            .isWithin(0.05f)
            .of(TypedValueCompat.deriveDimension(TypedValue.COMPLEX_UNIT_SP, 20f, metrics))
        assertThat(TypedValueCompat.dpToPx(20f, metrics))
            .isWithin(0.05f)
            .of(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 20f, metrics))
        assertThat(TypedValueCompat.spToPx(20f, metrics))
            .isWithin(0.05f)
            .of(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 20f, metrics))
    }

    private fun assertRoundTripIsEqual(
        dimenValueToTest: Float,
        dimenType: Int,
        metrics: DisplayMetrics,
    ) {
        val actualPx = TypedValue.applyDimension(dimenType, dimenValueToTest, metrics)
        val actualDimenValue = TypedValueCompat.deriveDimension(dimenType, actualPx, metrics)
        assertWithMessage(
                "TypedValue.applyDimension for type %s on %s = %s should equal " +
                    "TypedValueCompat.deriveDimension of %s",
                dimenType,
                dimenValueToTest,
                actualPx,
                actualDimenValue
            )
            .that(dimenValueToTest)
            .isWithin(0.05f)
            .of(actualDimenValue)
    }
}
