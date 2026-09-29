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

package androidx.xr.scenecore

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertFailsWith
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PointSourceParamsTest {

    @Test
    fun builder_defaultValues() {
        val params = PointSourceParams.Builder().build()
        assertThat(params.distanceAttenuation).isEqualTo(DistanceAttenuation.Auto)
        assertThat(params.directivityBalance).isEqualTo(0.0f)
        assertThat(params.directivitySharpness).isEqualTo(1.0f)
        assertThat(params.spread).isEqualTo(0.0f)
    }

    @Test
    fun builder_setDistanceAttenuation() {
        val distanceAttenuation = DistanceAttenuation.None
        val params = PointSourceParams.Builder().setDistanceAttenuation(distanceAttenuation).build()
        assertThat(params.distanceAttenuation).isEqualTo(distanceAttenuation)
    }

    @Test
    fun builder_setCustomDistanceAttenuation() {
        val distanceAttenuation =
            DistanceAttenuation.Custom(
                minDistance = 0.5f,
                maxDistance = 10.0f,
                gainAtMaxDistance = 0.5f,
                rolloffFactor = 2.0f,
            )
        val params = PointSourceParams.Builder().setDistanceAttenuation(distanceAttenuation).build()
        assertThat(params.distanceAttenuation).isEqualTo(distanceAttenuation)
    }

    @Test
    fun builder_setDirectivity() {
        val params =
            PointSourceParams.Builder().setDirectivity(balance = 0.5f, sharpness = 2.0f).build()
        assertThat(params.directivityBalance).isEqualTo(0.5f)
        assertThat(params.directivitySharpness).isEqualTo(2.0f)
    }

    @Test
    fun builder_setSpread() {
        val params = PointSourceParams.Builder().setSpread(180.0f).build()
        assertThat(params.spread).isEqualTo(180.0f)
    }

    @Test
    fun builder_invalidDirectivityBalance_throwsException() {
        assertFailsWith<IllegalArgumentException> {
            PointSourceParams.Builder().setDirectivity(balance = -0.1f, sharpness = 1.0f)
        }
        assertFailsWith<IllegalArgumentException> {
            PointSourceParams.Builder().setDirectivity(balance = 1.1f, sharpness = 1.0f)
        }
    }

    @Test
    fun builder_invalidDirectivitySharpness_throwsException() {
        assertFailsWith<IllegalArgumentException> {
            PointSourceParams.Builder().setDirectivity(balance = 0.5f, sharpness = 0.9f)
        }
    }

    @Test
    fun builder_invalidSpread_throwsException() {
        assertFailsWith<IllegalArgumentException> { PointSourceParams.Builder().setSpread(-0.1f) }
        assertFailsWith<IllegalArgumentException> { PointSourceParams.Builder().setSpread(360.1f) }
    }

    @Test
    fun builder_copyConstructor() {
        val originalParams =
            PointSourceParams.Builder()
                .setDistanceAttenuation(DistanceAttenuation.None)
                .setDirectivity(balance = 0.5f, sharpness = 2.0f)
                .setSpread(180.0f)
                .build()

        val copiedParams = PointSourceParams.Builder(originalParams).build()

        assertThat(copiedParams).isEqualTo(originalParams)
        assertThat(copiedParams).isNotSameInstanceAs(originalParams)
    }

    @Test
    fun equalsAndHashCode() {
        val params1 =
            PointSourceParams.Builder()
                .setDistanceAttenuation(DistanceAttenuation.None)
                .setDirectivity(balance = 0.5f, sharpness = 2.0f)
                .setSpread(180.0f)
                .build()

        val params2 =
            PointSourceParams.Builder()
                .setDistanceAttenuation(DistanceAttenuation.None)
                .setDirectivity(balance = 0.5f, sharpness = 2.0f)
                .setSpread(180.0f)
                .build()

        val params3 =
            PointSourceParams.Builder()
                .setDistanceAttenuation(DistanceAttenuation.Auto)
                .setDirectivity(balance = 0.5f, sharpness = 2.0f)
                .setSpread(180.0f)
                .build()

        assertThat(params1).isEqualTo(params2)
        assertThat(params1.hashCode()).isEqualTo(params2.hashCode())

        assertThat(params1).isNotEqualTo(params3)
        assertThat(params1.hashCode()).isNotEqualTo(params3.hashCode())
    }

    @Test
    fun testToString() {
        val params = PointSourceParams.Builder().build()
        val toString = params.toString()
        assertThat(toString).contains("distanceAttenuation")
        assertThat(toString).contains("directivityBalance")
        assertThat(toString).contains("directivitySharpness")
        assertThat(toString).contains("spread")
    }
}
