/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.ui.core

import com.google.common.truth.Truth
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class VelocityTest {

    private val velocity1 = Velocity(pixelsPerSecond = PxPosition(3.px, -7.px))
    private val velocity2 = Velocity(pixelsPerSecond = PxPosition(5.px, 13.px))

    @Test
    fun operatorUnaryMinus() {
        Truth.assertThat(-velocity1)
            .isEqualTo(Velocity(pixelsPerSecond = PxPosition(-3.px, 7.px)))
        Truth.assertThat(-velocity2)
            .isEqualTo(Velocity(pixelsPerSecond = PxPosition(-5.px, (-13).px)))
    }

    @Test
    fun operatorPlus() {
        Truth.assertThat(velocity2 + velocity1)
            .isEqualTo(Velocity(pixelsPerSecond = PxPosition(8.px, 6.px)))
        Truth.assertThat(velocity1 + velocity2)
            .isEqualTo(Velocity(pixelsPerSecond = PxPosition(8.px, 6.px)))
    }

    @Test
    fun operatorMinus() {
        Truth.assertThat(velocity1 - velocity2)
            .isEqualTo(Velocity(pixelsPerSecond = PxPosition(-2.px, (-20).px)))
        Truth.assertThat(velocity2 - velocity1)
            .isEqualTo(Velocity(pixelsPerSecond = PxPosition(2.px, 20.px)))
    }
}