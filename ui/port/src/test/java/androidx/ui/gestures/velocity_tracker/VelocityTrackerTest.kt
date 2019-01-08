/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.ui.gestures.velocity_tracker

import androidx.ui.engine.geometry.Offset
import androidx.ui.gestures.events.PointerDownEvent
import androidx.ui.gestures.events.PointerMoveEvent
import androidx.ui.gestures.events.PointerUpEvent
import androidx.ui.matchers.HasOneLineDescription
import androidx.ui.matchers.MoreOrLessEquals
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.not
import org.junit.Assert.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class VelocityTrackerTest {

    // TODO(Migration/shepshapard): This test needs to be broken up into smaller tests
    // that make edge cases clearer.  Right now its just a bunch of inputs and outputs
    // and its impossible for the reader to know how well different cases are being tested.
    @Test
    fun `Velocity tracker gives expected results`() {

        val expected: List<Offset> = listOf(
            Offset(219.59280094228163f, 1304.701682306001f),
            Offset(355.71046950050845f, 967.2112857054104f),
            Offset(12.657970884022308f, -36.90447839251946f),
            Offset(714.1399654786744f, -2561.534447931869f),
            Offset(-19.668121066218564f, -2910.105747052462f),
            Offset(646.8690114934209f, 2976.977762577527f),
            Offset(396.6988447819592f, 2106.225572911095f),
            Offset(298.31594440044495f, -3660.8315955215294f),
            Offset(-1.7334232785165882f, -3288.13174127454f),
            Offset(384.6361280392334f, -2645.6612524779835f),
            Offset(176.37900397918557f, 2711.2542876273264f),
            Offset(396.9328560260098f, 4280.651578291764f),
            Offset(-71.51939428321249f, 3716.7385187526947f)
        )

        val tracker = VelocityTracker()
        var i = 0
        velocityEventData.forEach {
            if (it is PointerDownEvent || it is PointerMoveEvent) {
                tracker.addPosition(it.timeStamp, it.position)
            }
            if (it is PointerUpEvent) {
                _checkVelocity(tracker.getVelocity(), expected[i])
                i += 1
            }
        }
    }

    @Test
    fun `Velocity control test`() {
        val velocity1 = Velocity(pixelsPerSecond = Offset(7.0f, 0.0f))
        val velocity2 = Velocity(pixelsPerSecond = Offset(12.0f, 0.0f))
        assertThat(velocity1, `is`(equalTo(Velocity(pixelsPerSecond = Offset(7.0f, 0.0f)))))
        assertThat(velocity1, `is`(not(equalTo(velocity2))))
        assertThat(
            velocity2 - velocity1,
            `is`(equalTo(Velocity(pixelsPerSecond = Offset(5.0f, 0.0f))))
        )
        assertThat((-velocity1).pixelsPerSecond, `is`(equalTo(Offset(-7.0f, 0.0f))))
        assertThat(
            velocity1 + velocity2,
            `is`(equalTo(Velocity(pixelsPerSecond = Offset(19.0f, 0.0f))))
        )
        assertThat(velocity1.hashCode(), `is`(not(equalTo(velocity2.hashCode()))))
        assertThat(velocity1, HasOneLineDescription)
    }

    @Test
    fun `Interrupted velocity estimation`() {
        // Regression test for https://github.com/flutter/flutter/pull/7510
        val tracker = VelocityTracker()
        interruptedVelocityEventData.forEach {
            if (it is PointerDownEvent || it is PointerMoveEvent)
                tracker.addPosition(it.timeStamp, it.position)
            if (it is PointerUpEvent) {
                _checkVelocity(tracker.getVelocity(), Offset(649.48932102748f, 3890.30505589076f))
            }
        }
    }

    @Test
    fun `No data velocity estimation`() {
        val tracker = VelocityTracker()
        assertThat(tracker.getVelocity(), `is`(equalTo(Velocity.zero)))
    }

    private fun _checkVelocity(actual: Velocity, expected: Offset) {
        assertThat(actual.pixelsPerSecond.dx, MoreOrLessEquals(expected.dx, 0.1f))
        assertThat(actual.pixelsPerSecond.dy, MoreOrLessEquals(expected.dy, 0.1f))
    }
}