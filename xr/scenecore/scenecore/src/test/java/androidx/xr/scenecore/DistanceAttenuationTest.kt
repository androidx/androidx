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
class DistanceAttenuationTest {
    @Test
    fun class_autoIsValid() {
        val auto = DistanceAttenuation.Auto
        assertThat(auto).isNotNull()
        assertThat(auto).isEqualTo(DistanceAttenuation.Auto)
        assertThat(auto.hashCode()).isEqualTo(DistanceAttenuation.Auto.hashCode())
    }

    @Test
    fun class_noneIsValid() {
        val none = DistanceAttenuation.None
        assertThat(none).isNotNull()
        assertThat(none).isEqualTo(DistanceAttenuation.None)
        assertThat(none.hashCode()).isEqualTo(DistanceAttenuation.None.hashCode())
    }

    @Test
    fun class_naturalIsValid() {
        val natural = DistanceAttenuation.Natural(minDistance = 0.5f)
        assertThat(natural).isNotNull()
        assertThat(natural.minDistance).isEqualTo(0.5f)
        assertThat(natural).isEqualTo(DistanceAttenuation.Natural(minDistance = 0.5f))
        assertThat(natural.hashCode())
            .isEqualTo(DistanceAttenuation.Natural(minDistance = 0.5f).hashCode())

        assertFailsWith<IllegalArgumentException> {
            DistanceAttenuation.Natural(minDistance = 0.0f)
        }
    }

    @Test
    fun class_linearIsValid() {
        val linear =
            DistanceAttenuation.Linear(
                minDistance = 0.5f,
                maxDistance = 10.0f,
                gainAtMaxDistance = 0.5f,
            )
        assertThat(linear).isNotNull()
        assertThat(linear.minDistance).isEqualTo(0.5f)
        assertThat(linear.maxDistance).isEqualTo(10.0f)
        assertThat(linear.gainAtMaxDistance).isEqualTo(0.5f)
        assertThat(linear)
            .isEqualTo(
                DistanceAttenuation.Linear(
                    minDistance = 0.5f,
                    maxDistance = 10.0f,
                    gainAtMaxDistance = 0.5f,
                )
            )
        assertThat(linear.hashCode())
            .isEqualTo(
                DistanceAttenuation.Linear(
                        minDistance = 0.5f,
                        maxDistance = 10.0f,
                        gainAtMaxDistance = 0.5f,
                    )
                    .hashCode()
            )

        assertFailsWith<IllegalArgumentException> {
            DistanceAttenuation.Linear(
                minDistance = 0.0f,
                maxDistance = 10.0f,
                gainAtMaxDistance = 0.5f,
            )
        }
        assertFailsWith<IllegalArgumentException> {
            DistanceAttenuation.Linear(
                minDistance = 0.5f,
                maxDistance = 0.1f,
                gainAtMaxDistance = 0.5f,
            )
        }
        assertFailsWith<IllegalArgumentException> {
            DistanceAttenuation.Linear(
                minDistance = 0.5f,
                maxDistance = 10.0f,
                gainAtMaxDistance = -0.1f,
            )
        }
        assertFailsWith<IllegalArgumentException> {
            DistanceAttenuation.Linear(
                minDistance = 0.5f,
                maxDistance = 10.0f,
                gainAtMaxDistance = 1.1f,
            )
        }
    }

    @Test
    fun class_customIsValid() {
        val custom =
            DistanceAttenuation.Custom(
                minDistance = 0.5f,
                maxDistance = 10.0f,
                gainAtMaxDistance = 0.5f,
                rolloffFactor = 2.0f,
            )
        assertThat(custom).isNotNull()
        assertThat(custom.minDistance).isEqualTo(0.5f)
        assertThat(custom.maxDistance).isEqualTo(10.0f)
        assertThat(custom.gainAtMaxDistance).isEqualTo(0.5f)
        assertThat(custom.rolloffFactor).isEqualTo(2.0f)
        assertThat(custom)
            .isEqualTo(
                DistanceAttenuation.Custom(
                    minDistance = 0.5f,
                    maxDistance = 10.0f,
                    gainAtMaxDistance = 0.5f,
                    rolloffFactor = 2.0f,
                )
            )
        assertThat(custom.hashCode())
            .isEqualTo(
                DistanceAttenuation.Custom(
                        minDistance = 0.5f,
                        maxDistance = 10.0f,
                        gainAtMaxDistance = 0.5f,
                        rolloffFactor = 2.0f,
                    )
                    .hashCode()
            )

        assertFailsWith<IllegalArgumentException> {
            DistanceAttenuation.Custom(
                minDistance = 0.0f,
                maxDistance = 10.0f,
                gainAtMaxDistance = 0.5f,
                rolloffFactor = 2.0f,
            )
        }
        assertFailsWith<IllegalArgumentException> {
            DistanceAttenuation.Custom(
                minDistance = 0.5f,
                maxDistance = 0.1f,
                gainAtMaxDistance = 0.5f,
                rolloffFactor = 2.0f,
            )
        }
        assertFailsWith<IllegalArgumentException> {
            DistanceAttenuation.Custom(
                minDistance = 0.5f,
                maxDistance = 10.0f,
                gainAtMaxDistance = -0.1f,
                rolloffFactor = 2.0f,
            )
        }
        assertFailsWith<IllegalArgumentException> {
            DistanceAttenuation.Custom(
                minDistance = 0.5f,
                maxDistance = 10.0f,
                gainAtMaxDistance = 1.1f,
                rolloffFactor = 2.0f,
            )
        }
    }
}
