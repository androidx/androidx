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

package androidx.ui.core.gesture.util

import androidx.ui.core.PointerInputData
import androidx.ui.core.Px
import androidx.ui.core.PxPosition
import androidx.ui.core.Uptime
import androidx.ui.core.Velocity
import androidx.ui.core.milliseconds
import androidx.ui.core.px
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class VelocityTrackerTest {

    // TODO(shepshapard): This test needs to be broken up into smaller tests
    // that make edge cases clearer.  Right now its just a bunch of inputs and outputs
    // and its impossible for the reader to know how well different cases are being tested.
    @Test
    fun calculateVelocity_returnsExpectedValues() {

        val expected = listOf(
            Pair(219.59280094228163f, 1304.701682306001f),
            Pair(355.71046950050845f, 967.2112857054104f),
            Pair(12.657970884022308f, -36.90447839251946f),
            Pair(714.1399654786744f, -2561.534447931869f),
            Pair(-19.668121066218564f, -2910.105747052462f),
            Pair(646.8690114934209f, 2976.977762577527f),
            Pair(396.6988447819592f, 2106.225572911095f),
            Pair(298.31594440044495f, -3660.8315955215294f),
            Pair(-1.7334232785165882f, -3288.13174127454f),
            Pair(384.6361280392334f, -2645.6612524779835f),
            Pair(176.37900397918557f, 2711.2542876273264f),
            Pair(396.9328560260098f, 4280.651578291764f),
            Pair(-71.51939428321249f, 3716.7385187526947f)
        )

        val tracker = VelocityTracker()
        var i = 0
        velocityEventData.forEach {
            if (it.down) {
                tracker.addPosition(it.uptime!!, it.position!!)
            } else {
                checkVelocity(tracker.calculateVelocity(), expected[i].first, expected[i].second)
                tracker.resetTracking()
                i += 1
            }
        }
    }

    @Test
    fun calculateVelocity_gapOf40MillisecondsInPositions_positionsAfterGapIgnored() {
        val tracker = VelocityTracker()
        interruptedVelocityEventData.forEach {
            if (it.down) {
                tracker.addPosition(it.uptime!!, it.position!!)
            } else {
                checkVelocity(
                    tracker.calculateVelocity(),
                    649.48932102748f,
                    3890.30505589076f)
                tracker.resetTracking()
            }
        }
    }

    @Test
    fun calculateVelocity_noData_returnsZero() {
        val tracker = VelocityTracker()
        assertThat(tracker.calculateVelocity()).isEqualTo(Velocity.Zero)
    }

    @Test
    fun calculateVelocity_onePosition_returnsZero() {
        val tracker = VelocityTracker()
        tracker.addPosition(
            velocityEventData[0].uptime!!,
            velocityEventData[0].position!!
        )
        assertThat(tracker.calculateVelocity()).isEqualTo(Velocity.Zero)
    }

    @Test
    fun resetTracking_resetsTracking() {
        val tracker = VelocityTracker()
        tracker.addPosition(
            velocityEventData[0].uptime!!,
            velocityEventData[0].position!!
        )

        tracker.resetTracking()

        assertThat(tracker.calculateVelocity()).isEqualTo(Velocity.Zero)
    }

    private fun checkVelocity(actual: Velocity, expectedDx: Float, expectedDy: Float) {
        assertThat(actual.pixelsPerSecond.x.value).isWithin(0.1f).of(expectedDx)
        assertThat(actual.pixelsPerSecond.y.value).isWithin(0.1f).of(expectedDy)
    }
}

/**
 * This extracts the inline PxPosition to a separate function so that velocityEventData
 * creation doesn't make the function too long for dex.
 */
private fun createPxPosition(width: Px, height: Px) = PxPosition(width, height)

val velocityEventData: List<PointerInputData> = listOf(
    PointerInputData(
        uptime = Uptime.Boot + 216690896.milliseconds,
        down = true,
        position = createPxPosition(270.px, 538.2857055664062.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216690906.milliseconds,
        down = true,
        position = createPxPosition(270.px, 538.2857055664062.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216690951.milliseconds,
        down = true,
        position = createPxPosition(270.px, 530.8571166992188.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216690959.milliseconds,
        down = true,
        position = createPxPosition(270.px, 526.8571166992188.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216690967.milliseconds,
        down = true,
        position = createPxPosition(270.px, 521.4285888671875.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216690975.milliseconds,
        down = true,
        position = createPxPosition(270.px, 515.4285888671875.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216690983.milliseconds,
        down = true,
        position = createPxPosition(270.px, 506.8571472167969.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216690991.milliseconds,
        down = true,
        position = createPxPosition(268.8571472167969.px, 496.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216690998.milliseconds,
        down = true,
        position = createPxPosition(267.4285583496094.px, 483.1428527832031.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216691006.milliseconds,
        down = true,
        position = createPxPosition(266.28570556640625.px, 469.71429443359375.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216691014.milliseconds,
        down = true,
        position = createPxPosition(265.4285583496094.px, 456.8571472167969.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216691021.milliseconds,
        down = true,
        position = createPxPosition(264.28570556640625.px, 443.71429443359375.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216691029.milliseconds,
        down = true,
        position = createPxPosition(264.px, 431.71429443359375.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216691036.milliseconds,
        down = true,
        position = createPxPosition(263.4285583496094.px, 421.1428527832031.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216691044.milliseconds,
        down = true,
        position = createPxPosition(263.4285583496094.px, 412.5714416503906.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216691052.milliseconds,
        down = true,
        position = createPxPosition(263.4285583496094.px, 404.5714416503906.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216691060.milliseconds,
        down = true,
        position = createPxPosition(263.4285583496094.px, 396.5714416503906.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216691068.milliseconds,
        down = true,
        position = createPxPosition(264.5714416503906.px, 390.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216691075.milliseconds,
        down = true,
        position = createPxPosition(265.1428527832031.px, 384.8571472167969.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216691083.milliseconds,
        down = true,
        position = createPxPosition(266.px, 380.28570556640625.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216691091.milliseconds,
        down = true,
        position = createPxPosition(266.5714416503906.px, 376.28570556640625.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216691098.milliseconds,
        down = true,
        position = createPxPosition(267.1428527832031.px, 373.1428527832031.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216691106.milliseconds,
        down = true,
        position = createPxPosition(267.71429443359375.px, 370.28570556640625.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216691114.milliseconds,
        down = true,
        position = createPxPosition(268.28570556640625.px, 367.71429443359375.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216691121.milliseconds,
        down = true,
        position = createPxPosition(268.5714416503906.px, 366.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216691130.milliseconds,
        down = true,
        position = createPxPosition(268.8571472167969.px, 364.5714416503906.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216691137.milliseconds,
        down = true,
        position = createPxPosition(269.1428527832031.px, 363.71429443359375.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216691145.milliseconds,
        down = true,
        position = createPxPosition(269.1428527832031.px, 362.8571472167969.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216691153.milliseconds,
        down = true,
        position = createPxPosition(269.4285583496094.px, 362.8571472167969.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216691168.milliseconds,
        down = true,
        position = createPxPosition(268.5714416503906.px, 365.4285583496094.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216691176.milliseconds,
        down = true,
        position = createPxPosition(267.1428527832031.px, 370.28570556640625.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216691183.milliseconds,
        down = true,
        position = createPxPosition(265.4285583496094.px, 376.8571472167969.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216691191.milliseconds,
        down = true,
        position = createPxPosition(263.1428527832031.px, 385.71429443359375.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216691199.milliseconds,
        down = true,
        position = createPxPosition(261.4285583496094.px, 396.5714416503906.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216691207.milliseconds,
        down = true,
        position = createPxPosition(259.71429443359375.px, 408.5714416503906.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216691215.milliseconds,
        down = true,
        position = createPxPosition(258.28570556640625.px, 419.4285583496094.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216691222.milliseconds,
        down = true,
        position = createPxPosition(257.4285583496094.px, 428.5714416503906.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216691230.milliseconds,
        down = true,
        position = createPxPosition(256.28570556640625.px, 436.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216691238.milliseconds,
        down = true,
        position = createPxPosition(255.7142791748047.px, 442.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216691245.milliseconds,
        down = true,
        position = createPxPosition(255.14285278320312.px, 447.71429443359375.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216691253.milliseconds,
        down = true,
        position = createPxPosition(254.85714721679688.px, 453.1428527832031.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216691261.milliseconds,
        down = true,
        position = createPxPosition(254.57142639160156.px, 458.5714416503906.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216691268.milliseconds,
        down = true,
        position = createPxPosition(254.2857208251953.px, 463.71429443359375.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216691276.milliseconds,
        down = true,
        position = createPxPosition(254.2857208251953.px, 470.28570556640625.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216691284.milliseconds,
        down = true,
        position = createPxPosition(254.2857208251953.px, 477.71429443359375.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216691292.milliseconds,
        down = true,
        position = createPxPosition(255.7142791748047.px, 487.1428527832031.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216691300.milliseconds,
        down = true,
        position = createPxPosition(256.8571472167969.px, 498.5714416503906.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216691307.milliseconds,
        down = true,
        position = createPxPosition(258.28570556640625.px, 507.71429443359375.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216691315.milliseconds,
        down = true,
        position = createPxPosition(259.4285583496094.px, 516.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216691323.milliseconds,
        down = true,
        position = createPxPosition(260.28570556640625.px, 521.7142944335938.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216691338.milliseconds,
        down = false,
        position = createPxPosition(260.28570556640625.px, 521.7142944335938.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216691573.milliseconds,
        down = true,
        position = createPxPosition(266.px, 327.4285583496094.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216691588.milliseconds,
        down = true,
        position = createPxPosition(266.px, 327.4285583496094.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216691626.milliseconds,
        down = true,
        position = createPxPosition(261.1428527832031.px, 337.1428527832031.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216691634.milliseconds,
        down = true,
        position = createPxPosition(258.28570556640625.px, 343.1428527832031.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216691642.milliseconds,
        down = true,
        position = createPxPosition(254.57142639160156.px, 354.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216691650.milliseconds,
        down = true,
        position = createPxPosition(250.2857208251953.px, 368.28570556640625.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216691657.milliseconds,
        down = true,
        position = createPxPosition(247.42857360839844.px, 382.8571472167969.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216691665.milliseconds,
        down = true,
        position = createPxPosition(245.14285278320312.px, 397.4285583496094.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216691673.milliseconds,
        down = true,
        position = createPxPosition(243.14285278320312.px, 411.71429443359375.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216691680.milliseconds,
        down = true,
        position = createPxPosition(242.2857208251953.px, 426.28570556640625.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216691688.milliseconds,
        down = true,
        position = createPxPosition(241.7142791748047.px, 440.5714416503906.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216691696.milliseconds,
        down = true,
        position = createPxPosition(241.7142791748047.px, 454.5714416503906.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216691703.milliseconds,
        down = true,
        position = createPxPosition(242.57142639160156.px, 467.71429443359375.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216691712.milliseconds,
        down = true,
        position = createPxPosition(243.42857360839844.px, 477.4285583496094.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216691720.milliseconds,
        down = true,
        position = createPxPosition(244.85714721679688.px, 485.71429443359375.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216691727.milliseconds,
        down = true,
        position = createPxPosition(246.2857208251953.px, 493.1428527832031.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216691735.milliseconds,
        down = true,
        position = createPxPosition(248.px, 499.71429443359375.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216691750.milliseconds,
        down = false,
        position = createPxPosition(248.px, 499.71429443359375.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216692255.milliseconds,
        down = true,
        position = createPxPosition(249.42857360839844.px, 351.4285583496094.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216692270.milliseconds,
        down = true,
        position = createPxPosition(249.42857360839844.px, 351.4285583496094.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216692309.milliseconds,
        down = true,
        position = createPxPosition(246.2857208251953.px, 361.71429443359375.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216692317.milliseconds,
        down = true,
        position = createPxPosition(244.px, 368.5714416503906.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216692325.milliseconds,
        down = true,
        position = createPxPosition(241.42857360839844.px, 377.71429443359375.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216692333.milliseconds,
        down = true,
        position = createPxPosition(237.7142791748047.px, 391.71429443359375.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216692340.milliseconds,
        down = true,
        position = createPxPosition(235.14285278320312.px, 406.5714416503906.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216692348.milliseconds,
        down = true,
        position = createPxPosition(232.57142639160156.px, 421.4285583496094.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216692356.milliseconds,
        down = true,
        position = createPxPosition(230.2857208251953.px, 436.5714416503906.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216692363.milliseconds,
        down = true,
        position = createPxPosition(228.2857208251953.px, 451.71429443359375.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216692371.milliseconds,
        down = true,
        position = createPxPosition(227.42857360839844.px, 466.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216692378.milliseconds,
        down = true,
        position = createPxPosition(226.2857208251953.px, 479.71429443359375.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216692387.milliseconds,
        down = true,
        position = createPxPosition(225.7142791748047.px, 491.71429443359375.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216692395.milliseconds,
        down = true,
        position = createPxPosition(225.14285278320312.px, 501.71429443359375.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216692402.milliseconds,
        down = true,
        position = createPxPosition(224.85714721679688.px, 509.1428527832031.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216692410.milliseconds,
        down = true,
        position = createPxPosition(224.57142639160156.px, 514.8571166992188.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216692418.milliseconds,
        down = true,
        position = createPxPosition(224.2857208251953.px, 519.4285888671875.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216692425.milliseconds,
        down = true,
        position = createPxPosition(224.px, 523.4285888671875.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216692433.milliseconds,
        down = true,
        position = createPxPosition(224.px, 527.1428833007812.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216692441.milliseconds,
        down = true,
        position = createPxPosition(224.px, 530.5714111328125.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216692448.milliseconds,
        down = true,
        position = createPxPosition(224.px, 533.1428833007812.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216692456.milliseconds,
        down = true,
        position = createPxPosition(224.px, 535.4285888671875.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216692464.milliseconds,
        down = true,
        position = createPxPosition(223.7142791748047.px, 536.8571166992188.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216692472.milliseconds,
        down = true,
        position = createPxPosition(223.7142791748047.px, 538.2857055664062.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216692487.milliseconds,
        down = false,
        position = createPxPosition(223.7142791748047.px, 538.2857055664062.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216692678.milliseconds,
        down = true,
        position = createPxPosition(221.42857360839844.px, 526.2857055664062.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216692701.milliseconds,
        down = true,
        position = createPxPosition(220.57142639160156.px, 514.8571166992188.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216692708.milliseconds,
        down = true,
        position = createPxPosition(220.2857208251953.px, 508.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216692716.milliseconds,
        down = true,
        position = createPxPosition(220.2857208251953.px, 498.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216692724.milliseconds,
        down = true,
        position = createPxPosition(221.14285278320312.px, 484.28570556640625.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216692732.milliseconds,
        down = true,
        position = createPxPosition(221.7142791748047.px, 469.4285583496094.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216692740.milliseconds,
        down = true,
        position = createPxPosition(223.42857360839844.px, 453.1428527832031.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216692748.milliseconds,
        down = true,
        position = createPxPosition(225.7142791748047.px, 436.28570556640625.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216692755.milliseconds,
        down = true,
        position = createPxPosition(229.14285278320312.px, 418.28570556640625.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216692763.milliseconds,
        down = true,
        position = createPxPosition(232.85714721679688.px, 400.28570556640625.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216692770.milliseconds,
        down = true,
        position = createPxPosition(236.85714721679688.px, 382.5714416503906.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216692778.milliseconds,
        down = true,
        position = createPxPosition(241.14285278320312.px, 366.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216692786.milliseconds,
        down = true,
        position = createPxPosition(244.85714721679688.px, 350.28570556640625.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216692793.milliseconds,
        down = true,
        position = createPxPosition(249.14285278320312.px, 335.4285583496094.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216692809.milliseconds,
        down = false,
        position = createPxPosition(249.14285278320312.px, 335.4285583496094.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216693222.milliseconds,
        down = true,
        position = createPxPosition(224.px, 545.4285888671875.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216693245.milliseconds,
        down = true,
        position = createPxPosition(224.px, 545.4285888671875.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216693275.milliseconds,
        down = true,
        position = createPxPosition(222.85714721679688.px, 535.1428833007812.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216693284.milliseconds,
        down = true,
        position = createPxPosition(222.85714721679688.px, 528.8571166992188.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216693291.milliseconds,
        down = true,
        position = createPxPosition(222.2857208251953.px, 518.5714111328125.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216693299.milliseconds,
        down = true,
        position = createPxPosition(222.px, 503.4285583496094.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216693307.milliseconds,
        down = true,
        position = createPxPosition(222.px, 485.4285583496094.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216693314.milliseconds,
        down = true,
        position = createPxPosition(221.7142791748047.px, 464.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216693322.milliseconds,
        down = true,
        position = createPxPosition(222.2857208251953.px, 440.28570556640625.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216693337.milliseconds,
        down = false,
        position = createPxPosition(222.2857208251953.px, 440.28570556640625.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216693985.milliseconds,
        down = true,
        position = createPxPosition(208.px, 544.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216694047.milliseconds,
        down = true,
        position = createPxPosition(208.57142639160156.px, 532.2857055664062.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216694054.milliseconds,
        down = true,
        position = createPxPosition(208.85714721679688.px, 525.7142944335938.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216694062.milliseconds,
        down = true,
        position = createPxPosition(208.85714721679688.px, 515.1428833007812.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216694070.milliseconds,
        down = true,
        position = createPxPosition(208.px, 501.4285583496094.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216694077.milliseconds,
        down = true,
        position = createPxPosition(207.42857360839844.px, 487.1428527832031.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216694085.milliseconds,
        down = true,
        position = createPxPosition(206.57142639160156.px, 472.8571472167969.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216694092.milliseconds,
        down = true,
        position = createPxPosition(206.57142639160156.px, 458.8571472167969.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216694100.milliseconds,
        down = true,
        position = createPxPosition(206.57142639160156.px, 446.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216694108.milliseconds,
        down = true,
        position = createPxPosition(206.57142639160156.px, 434.28570556640625.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216694116.milliseconds,
        down = true,
        position = createPxPosition(207.14285278320312.px, 423.71429443359375.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216694124.milliseconds,
        down = true,
        position = createPxPosition(208.57142639160156.px, 412.8571472167969.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216694131.milliseconds,
        down = true,
        position = createPxPosition(209.7142791748047.px, 402.28570556640625.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216694139.milliseconds,
        down = true,
        position = createPxPosition(211.7142791748047.px, 393.1428527832031.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216694147.milliseconds,
        down = true,
        position = createPxPosition(213.42857360839844.px, 385.1428527832031.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216694154.milliseconds,
        down = true,
        position = createPxPosition(215.42857360839844.px, 378.28570556640625.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216694162.milliseconds,
        down = true,
        position = createPxPosition(217.42857360839844.px, 371.71429443359375.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216694169.milliseconds,
        down = true,
        position = createPxPosition(219.42857360839844.px, 366.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216694177.milliseconds,
        down = true,
        position = createPxPosition(221.42857360839844.px, 360.8571472167969.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216694185.milliseconds,
        down = true,
        position = createPxPosition(223.42857360839844.px, 356.5714416503906.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216694193.milliseconds,
        down = true,
        position = createPxPosition(225.14285278320312.px, 352.28570556640625.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216694201.milliseconds,
        down = true,
        position = createPxPosition(226.85714721679688.px, 348.5714416503906.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216694209.milliseconds,
        down = true,
        position = createPxPosition(228.2857208251953.px, 346.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216694216.milliseconds,
        down = true,
        position = createPxPosition(229.14285278320312.px, 343.71429443359375.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216694224.milliseconds,
        down = true,
        position = createPxPosition(230.px, 342.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216694232.milliseconds,
        down = true,
        position = createPxPosition(230.57142639160156.px, 340.5714416503906.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216694239.milliseconds,
        down = true,
        position = createPxPosition(230.85714721679688.px, 339.71429443359375.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216694247.milliseconds,
        down = true,
        position = createPxPosition(230.85714721679688.px, 339.4285583496094.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216694262.milliseconds,
        down = true,
        position = createPxPosition(230.2857208251953.px, 342.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216694270.milliseconds,
        down = true,
        position = createPxPosition(228.85714721679688.px, 346.28570556640625.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216694278.milliseconds,
        down = true,
        position = createPxPosition(227.14285278320312.px, 352.5714416503906.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216694286.milliseconds,
        down = true,
        position = createPxPosition(225.42857360839844.px, 359.4285583496094.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216694294.milliseconds,
        down = true,
        position = createPxPosition(223.7142791748047.px, 367.71429443359375.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216694301.milliseconds,
        down = true,
        position = createPxPosition(222.57142639160156.px, 376.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216694309.milliseconds,
        down = true,
        position = createPxPosition(221.42857360839844.px, 384.28570556640625.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216694317.milliseconds,
        down = true,
        position = createPxPosition(220.85714721679688.px, 392.28570556640625.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216694324.milliseconds,
        down = true,
        position = createPxPosition(220.px, 400.5714416503906.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216694332.milliseconds,
        down = true,
        position = createPxPosition(219.14285278320312.px, 409.71429443359375.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216694339.milliseconds,
        down = true,
        position = createPxPosition(218.85714721679688.px, 419.1428527832031.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216694348.milliseconds,
        down = true,
        position = createPxPosition(218.2857208251953.px, 428.8571472167969.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216694356.milliseconds,
        down = true,
        position = createPxPosition(218.2857208251953.px, 438.8571472167969.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216694363.milliseconds,
        down = true,
        position = createPxPosition(218.2857208251953.px, 447.71429443359375.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216694371.milliseconds,
        down = true,
        position = createPxPosition(218.2857208251953.px, 455.71429443359375.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216694379.milliseconds,
        down = true,
        position = createPxPosition(219.14285278320312.px, 462.8571472167969.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216694386.milliseconds,
        down = true,
        position = createPxPosition(220.px, 469.4285583496094.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216694394.milliseconds,
        down = true,
        position = createPxPosition(221.14285278320312.px, 475.4285583496094.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216694401.milliseconds,
        down = true,
        position = createPxPosition(222.px, 480.5714416503906.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216694409.milliseconds,
        down = true,
        position = createPxPosition(222.85714721679688.px, 485.4285583496094.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216694417.milliseconds,
        down = true,
        position = createPxPosition(224.px, 489.71429443359375.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216694425.milliseconds,
        down = true,
        position = createPxPosition(224.85714721679688.px, 492.8571472167969.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216694433.milliseconds,
        down = true,
        position = createPxPosition(225.42857360839844.px, 495.4285583496094.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216694440.milliseconds,
        down = true,
        position = createPxPosition(226.px, 497.1428527832031.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216694448.milliseconds,
        down = true,
        position = createPxPosition(226.2857208251953.px, 498.28570556640625.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216694456.milliseconds,
        down = true,
        position = createPxPosition(226.2857208251953.px, 498.8571472167969.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216694471.milliseconds,
        down = true,
        position = createPxPosition(226.2857208251953.px, 498.28570556640625.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216694479.milliseconds,
        down = true,
        position = createPxPosition(226.2857208251953.px, 496.5714416503906.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216694486.milliseconds,
        down = true,
        position = createPxPosition(226.2857208251953.px, 493.71429443359375.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216694494.milliseconds,
        down = true,
        position = createPxPosition(226.2857208251953.px, 490.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216694502.milliseconds,
        down = true,
        position = createPxPosition(226.2857208251953.px, 486.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216694510.milliseconds,
        down = true,
        position = createPxPosition(226.2857208251953.px, 480.5714416503906.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216694518.milliseconds,
        down = true,
        position = createPxPosition(226.2857208251953.px, 475.71429443359375.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216694525.milliseconds,
        down = true,
        position = createPxPosition(226.2857208251953.px, 468.8571472167969.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216694533.milliseconds,
        down = true,
        position = createPxPosition(226.2857208251953.px, 461.4285583496094.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216694541.milliseconds,
        down = true,
        position = createPxPosition(226.2857208251953.px, 452.5714416503906.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216694548.milliseconds,
        down = true,
        position = createPxPosition(226.57142639160156.px, 442.28570556640625.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216694556.milliseconds,
        down = true,
        position = createPxPosition(226.57142639160156.px, 432.28570556640625.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216694564.milliseconds,
        down = true,
        position = createPxPosition(226.85714721679688.px, 423.4285583496094.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216694571.milliseconds,
        down = true,
        position = createPxPosition(227.42857360839844.px, 416.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216694580.milliseconds,
        down = true,
        position = createPxPosition(227.7142791748047.px, 410.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216694587.milliseconds,
        down = true,
        position = createPxPosition(228.2857208251953.px, 404.28570556640625.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216694595.milliseconds,
        down = true,
        position = createPxPosition(228.85714721679688.px, 399.71429443359375.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216694603.milliseconds,
        down = true,
        position = createPxPosition(229.14285278320312.px, 395.4285583496094.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216694610.milliseconds,
        down = true,
        position = createPxPosition(229.42857360839844.px, 392.28570556640625.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216694618.milliseconds,
        down = true,
        position = createPxPosition(229.7142791748047.px, 390.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216694625.milliseconds,
        down = true,
        position = createPxPosition(229.7142791748047.px, 388.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216694633.milliseconds,
        down = true,
        position = createPxPosition(229.7142791748047.px, 386.8571472167969.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216694641.milliseconds,
        down = true,
        position = createPxPosition(229.7142791748047.px, 386.28570556640625.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216694648.milliseconds,
        down = true,
        position = createPxPosition(229.7142791748047.px, 386.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216694657.milliseconds,
        down = true,
        position = createPxPosition(228.85714721679688.px, 386.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216694665.milliseconds,
        down = true,
        position = createPxPosition(228.px, 388.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216694672.milliseconds,
        down = true,
        position = createPxPosition(226.px, 392.5714416503906.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216694680.milliseconds,
        down = true,
        position = createPxPosition(224.px, 397.71429443359375.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216694688.milliseconds,
        down = true,
        position = createPxPosition(222.px, 404.28570556640625.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216694695.milliseconds,
        down = true,
        position = createPxPosition(219.7142791748047.px, 411.1428527832031.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216694703.milliseconds,
        down = true,
        position = createPxPosition(218.2857208251953.px, 418.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216694710.milliseconds,
        down = true,
        position = createPxPosition(217.14285278320312.px, 425.4285583496094.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216694718.milliseconds,
        down = true,
        position = createPxPosition(215.7142791748047.px, 433.4285583496094.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216694726.milliseconds,
        down = true,
        position = createPxPosition(214.85714721679688.px, 442.28570556640625.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216694734.milliseconds,
        down = true,
        position = createPxPosition(214.px, 454.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216694742.milliseconds,
        down = true,
        position = createPxPosition(214.px, 469.4285583496094.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216694749.milliseconds,
        down = true,
        position = createPxPosition(215.42857360839844.px, 485.4285583496094.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216694757.milliseconds,
        down = true,
        position = createPxPosition(217.7142791748047.px, 502.8571472167969.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216694765.milliseconds,
        down = true,
        position = createPxPosition(221.14285278320312.px, 521.4285888671875.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216694772.milliseconds,
        down = true,
        position = createPxPosition(224.57142639160156.px, 541.1428833007812.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216694780.milliseconds,
        down = true,
        position = createPxPosition(229.14285278320312.px, 561.1428833007812.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216694788.milliseconds,
        down = true,
        position = createPxPosition(233.42857360839844.px, 578.8571166992188.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216694802.milliseconds,
        down = false,
        position = createPxPosition(233.42857360839844.px, 578.8571166992188.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216695344.milliseconds,
        down = true,
        position = createPxPosition(253.42857360839844.px, 310.5714416503906.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216695352.milliseconds,
        down = true,
        position = createPxPosition(253.42857360839844.px, 310.5714416503906.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216695359.milliseconds,
        down = true,
        position = createPxPosition(252.85714721679688.px, 318.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216695367.milliseconds,
        down = true,
        position = createPxPosition(251.14285278320312.px, 322.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216695375.milliseconds,
        down = true,
        position = createPxPosition(248.85714721679688.px, 327.1428527832031.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216695382.milliseconds,
        down = true,
        position = createPxPosition(246.px, 334.8571472167969.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216695390.milliseconds,
        down = true,
        position = createPxPosition(242.57142639160156.px, 344.5714416503906.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216695397.milliseconds,
        down = true,
        position = createPxPosition(238.85714721679688.px, 357.4285583496094.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216695406.milliseconds,
        down = true,
        position = createPxPosition(235.7142791748047.px, 371.71429443359375.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216695414.milliseconds,
        down = true,
        position = createPxPosition(232.2857208251953.px, 386.8571472167969.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216695421.milliseconds,
        down = true,
        position = createPxPosition(229.42857360839844.px, 402.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216695429.milliseconds,
        down = true,
        position = createPxPosition(227.42857360839844.px, 416.8571472167969.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216695437.milliseconds,
        down = true,
        position = createPxPosition(226.2857208251953.px, 431.4285583496094.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216695444.milliseconds,
        down = true,
        position = createPxPosition(226.2857208251953.px, 446.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216695452.milliseconds,
        down = true,
        position = createPxPosition(227.7142791748047.px, 460.28570556640625.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216695459.milliseconds,
        down = true,
        position = createPxPosition(230.px, 475.1428527832031.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216695467.milliseconds,
        down = true,
        position = createPxPosition(232.2857208251953.px, 489.71429443359375.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216695475.milliseconds,
        down = true,
        position = createPxPosition(235.7142791748047.px, 504.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216695490.milliseconds,
        down = false,
        position = createPxPosition(235.7142791748047.px, 504.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216695885.milliseconds,
        down = true,
        position = createPxPosition(238.85714721679688.px, 524.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216695908.milliseconds,
        down = true,
        position = createPxPosition(236.2857208251953.px, 515.7142944335938.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216695916.milliseconds,
        down = true,
        position = createPxPosition(234.85714721679688.px, 509.1428527832031.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216695924.milliseconds,
        down = true,
        position = createPxPosition(232.57142639160156.px, 498.5714416503906.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216695931.milliseconds,
        down = true,
        position = createPxPosition(230.57142639160156.px, 483.71429443359375.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216695939.milliseconds,
        down = true,
        position = createPxPosition(229.14285278320312.px, 466.5714416503906.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216695947.milliseconds,
        down = true,
        position = createPxPosition(229.14285278320312.px, 446.5714416503906.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216695955.milliseconds,
        down = true,
        position = createPxPosition(230.57142639160156.px, 424.8571472167969.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216695963.milliseconds,
        down = true,
        position = createPxPosition(232.57142639160156.px, 402.28570556640625.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216695970.milliseconds,
        down = true,
        position = createPxPosition(235.14285278320312.px, 380.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216695978.milliseconds,
        down = true,
        position = createPxPosition(238.57142639160156.px, 359.4285583496094.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216695993.milliseconds,
        down = false,
        position = createPxPosition(238.57142639160156.px, 359.4285583496094.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216696429.milliseconds,
        down = true,
        position = createPxPosition(238.2857208251953.px, 568.5714111328125.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216696459.milliseconds,
        down = true,
        position = createPxPosition(234.px, 560.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216696467.milliseconds,
        down = true,
        position = createPxPosition(231.42857360839844.px, 553.1428833007812.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216696475.milliseconds,
        down = true,
        position = createPxPosition(228.2857208251953.px, 543.1428833007812.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216696483.milliseconds,
        down = true,
        position = createPxPosition(225.42857360839844.px, 528.8571166992188.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216696491.milliseconds,
        down = true,
        position = createPxPosition(223.14285278320312.px, 512.2857055664062.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216696498.milliseconds,
        down = true,
        position = createPxPosition(222.px, 495.4285583496094.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216696506.milliseconds,
        down = true,
        position = createPxPosition(221.7142791748047.px, 477.4285583496094.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216696514.milliseconds,
        down = true,
        position = createPxPosition(221.7142791748047.px, 458.28570556640625.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216696521.milliseconds,
        down = true,
        position = createPxPosition(223.14285278320312.px, 438.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216696529.milliseconds,
        down = true,
        position = createPxPosition(224.2857208251953.px, 416.28570556640625.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216696544.milliseconds,
        down = false,
        position = createPxPosition(224.2857208251953.px, 416.28570556640625.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216696974.milliseconds,
        down = true,
        position = createPxPosition(218.57142639160156.px, 530.5714111328125.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216697012.milliseconds,
        down = true,
        position = createPxPosition(220.2857208251953.px, 522.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216697020.milliseconds,
        down = true,
        position = createPxPosition(221.14285278320312.px, 517.7142944335938.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216697028.milliseconds,
        down = true,
        position = createPxPosition(222.2857208251953.px, 511.71429443359375.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216697036.milliseconds,
        down = true,
        position = createPxPosition(224.px, 504.28570556640625.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216697044.milliseconds,
        down = true,
        position = createPxPosition(227.14285278320312.px, 490.5714416503906.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216697052.milliseconds,
        down = true,
        position = createPxPosition(229.42857360839844.px, 474.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216697059.milliseconds,
        down = true,
        position = createPxPosition(231.42857360839844.px, 454.5714416503906.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216697067.milliseconds,
        down = true,
        position = createPxPosition(233.7142791748047.px, 431.1428527832031.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216697082.milliseconds,
        down = false,
        position = createPxPosition(233.7142791748047.px, 431.1428527832031.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216697435.milliseconds,
        down = true,
        position = createPxPosition(257.1428527832031.px, 285.1428527832031.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216697465.milliseconds,
        down = true,
        position = createPxPosition(251.7142791748047.px, 296.8571472167969.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216697473.milliseconds,
        down = true,
        position = createPxPosition(248.2857208251953.px, 304.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216697481.milliseconds,
        down = true,
        position = createPxPosition(244.57142639160156.px, 314.8571472167969.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216697489.milliseconds,
        down = true,
        position = createPxPosition(240.2857208251953.px, 329.1428527832031.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216697497.milliseconds,
        down = true,
        position = createPxPosition(236.85714721679688.px, 345.1428527832031.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216697505.milliseconds,
        down = true,
        position = createPxPosition(233.7142791748047.px, 361.4285583496094.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216697512.milliseconds,
        down = true,
        position = createPxPosition(231.14285278320312.px, 378.28570556640625.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216697520.milliseconds,
        down = true,
        position = createPxPosition(229.42857360839844.px, 395.4285583496094.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216697528.milliseconds,
        down = true,
        position = createPxPosition(229.42857360839844.px, 412.8571472167969.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216697535.milliseconds,
        down = true,
        position = createPxPosition(230.85714721679688.px, 430.8571472167969.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216697543.milliseconds,
        down = true,
        position = createPxPosition(233.42857360839844.px, 449.71429443359375.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216697558.milliseconds,
        down = false,
        position = createPxPosition(233.42857360839844.px, 449.71429443359375.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216697749.milliseconds,
        down = true,
        position = createPxPosition(246.px, 311.4285583496094.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216697780.milliseconds,
        down = true,
        position = createPxPosition(244.57142639160156.px, 318.28570556640625.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216697787.milliseconds,
        down = true,
        position = createPxPosition(243.14285278320312.px, 325.4285583496094.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216697795.milliseconds,
        down = true,
        position = createPxPosition(241.42857360839844.px, 336.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216697803.milliseconds,
        down = true,
        position = createPxPosition(239.7142791748047.px, 351.1428527832031.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216697811.milliseconds,
        down = true,
        position = createPxPosition(238.2857208251953.px, 368.5714416503906.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216697819.milliseconds,
        down = true,
        position = createPxPosition(238.px, 389.4285583496094.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216697826.milliseconds,
        down = true,
        position = createPxPosition(239.14285278320312.px, 412.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216697834.milliseconds,
        down = true,
        position = createPxPosition(242.2857208251953.px, 438.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216697842.milliseconds,
        down = true,
        position = createPxPosition(247.42857360839844.px, 466.8571472167969.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216697849.milliseconds,
        down = true,
        position = createPxPosition(254.2857208251953.px, 497.71429443359375.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216697864.milliseconds,
        down = false,
        position = createPxPosition(254.2857208251953.px, 497.71429443359375.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216698321.milliseconds,
        down = true,
        position = createPxPosition(250.px, 306.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216698328.milliseconds,
        down = true,
        position = createPxPosition(250.px, 306.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216698344.milliseconds,
        down = true,
        position = createPxPosition(249.14285278320312.px, 314.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216698351.milliseconds,
        down = true,
        position = createPxPosition(247.42857360839844.px, 319.4285583496094.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216698359.milliseconds,
        down = true,
        position = createPxPosition(245.14285278320312.px, 326.8571472167969.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216698366.milliseconds,
        down = true,
        position = createPxPosition(241.7142791748047.px, 339.4285583496094.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216698374.milliseconds,
        down = true,
        position = createPxPosition(238.57142639160156.px, 355.71429443359375.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216698382.milliseconds,
        down = true,
        position = createPxPosition(236.2857208251953.px, 374.28570556640625.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216698390.milliseconds,
        down = true,
        position = createPxPosition(235.14285278320312.px, 396.5714416503906.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216698398.milliseconds,
        down = true,
        position = createPxPosition(236.57142639160156.px, 421.4285583496094.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216698406.milliseconds,
        down = true,
        position = createPxPosition(241.14285278320312.px, 451.4285583496094.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216698421.milliseconds,
        down = false,
        position = createPxPosition(241.14285278320312.px, 451.4285583496094.px)
    )
)

val interruptedVelocityEventData: List<PointerInputData> = listOf(
    PointerInputData(
        uptime = Uptime.Boot + 216698321.milliseconds,
        down = true,
        position = createPxPosition(250.px, 306.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216698328.milliseconds,
        down = true,
        position = createPxPosition(250.px, 306.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216698344.milliseconds,
        down = true,
        position = createPxPosition(249.14285278320312.px, 314.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216698351.milliseconds,
        down = true,
        position = createPxPosition(247.42857360839844.px, 319.4285583496094.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216698359.milliseconds,
        down = true,
        position = createPxPosition(245.14285278320312.px, 326.8571472167969.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + 216698366.milliseconds,
        down = true,
        position = createPxPosition(241.7142791748047.px, 339.4285583496094.px)
    ),

// The pointer "stops" here because we've introduced a 40+ms gap
// in the move event stream. See kAssumePointerMoveStoppedMilliseconds
// in velocity_tracker.dart.

    PointerInputData(
        uptime = Uptime.Boot + (216698374 + 40).milliseconds,
        down = true,
        position = createPxPosition(238.57142639160156.px, 355.71429443359375.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + (216698382 + 40).milliseconds,
        down = true,
        position = createPxPosition(236.2857208251953.px, 374.28570556640625.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + (216698390 + 40).milliseconds,
        down = true,
        position = createPxPosition(235.14285278320312.px, 396.5714416503906.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + (216698398 + 40).milliseconds,
        down = true,
        position = createPxPosition(236.57142639160156.px, 421.4285583496094.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + (216698406 + 40).milliseconds,
        down = true,
        position = createPxPosition(241.14285278320312.px, 451.4285583496094.px)
    ),
    PointerInputData(
        uptime = Uptime.Boot + (216698421 + 40).milliseconds,
        down = false,
        position = createPxPosition(241.14285278320312.px, 451.4285583496094.px)
    )
)