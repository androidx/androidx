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
import androidx.ui.core.PxPosition
import androidx.ui.core.Velocity
import androidx.ui.core.millisecondsToTimestamp
import androidx.ui.core.px
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class VelocityTrackerTest {

    // TODO(Migration/shepshapard): This test needs to be broken up into smaller tests
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
                tracker.addPosition(it.timestamp!!, it.position!!)
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
                tracker.addPosition(it.timestamp!!, it.position!!)
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
            velocityEventData[0].timestamp!!,
            velocityEventData[0].position!!
        )
        assertThat(tracker.calculateVelocity()).isEqualTo(Velocity.Zero)
    }

    @Test
    fun resetTracking_resetsTracking() {
        val tracker = VelocityTracker()
        tracker.addPosition(
            velocityEventData[0].timestamp!!,
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

val velocityEventData: List<PointerInputData> = listOf(
    PointerInputData(
        timestamp = 216690896L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(270.px, 538.2857055664062.px)
    ),
    PointerInputData(
        timestamp = 216690906L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(270.px, 538.2857055664062.px)
    ),
    PointerInputData(
        timestamp = 216690951L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(270.px, 530.8571166992188.px)
    ),
    PointerInputData(
        timestamp = 216690959L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(270.px, 526.8571166992188.px)
    ),
    PointerInputData(
        timestamp = 216690967L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(270.px, 521.4285888671875.px)
    ),
    PointerInputData(
        timestamp = 216690975L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(270.px, 515.4285888671875.px)
    ),
    PointerInputData(
        timestamp = 216690983L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(270.px, 506.8571472167969.px)
    ),
    PointerInputData(
        timestamp = 216690991L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(268.8571472167969.px, 496.px)
    ),
    PointerInputData(
        timestamp = 216690998L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(267.4285583496094.px, 483.1428527832031.px)
    ),
    PointerInputData(
        timestamp = 216691006L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(266.28570556640625.px, 469.71429443359375.px)
    ),
    PointerInputData(
        timestamp = 216691014L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(265.4285583496094.px, 456.8571472167969.px)
    ),
    PointerInputData(
        timestamp = 216691021L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(264.28570556640625.px, 443.71429443359375.px)
    ),
    PointerInputData(
        timestamp = 216691029L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(264.px, 431.71429443359375.px)
    ),
    PointerInputData(
        timestamp = 216691036L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(263.4285583496094.px, 421.1428527832031.px)
    ),
    PointerInputData(
        timestamp = 216691044L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(263.4285583496094.px, 412.5714416503906.px)
    ),
    PointerInputData(
        timestamp = 216691052L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(263.4285583496094.px, 404.5714416503906.px)
    ),
    PointerInputData(
        timestamp = 216691060L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(263.4285583496094.px, 396.5714416503906.px)
    ),
    PointerInputData(
        timestamp = 216691068L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(264.5714416503906.px, 390.px)
    ),
    PointerInputData(
        timestamp = 216691075L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(265.1428527832031.px, 384.8571472167969.px)
    ),
    PointerInputData(
        timestamp = 216691083L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(266.px, 380.28570556640625.px)
    ),
    PointerInputData(
        timestamp = 216691091L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(266.5714416503906.px, 376.28570556640625.px)
    ),
    PointerInputData(
        timestamp = 216691098L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(267.1428527832031.px, 373.1428527832031.px)
    ),
    PointerInputData(
        timestamp = 216691106L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(267.71429443359375.px, 370.28570556640625.px)
    ),
    PointerInputData(
        timestamp = 216691114L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(268.28570556640625.px, 367.71429443359375.px)
    ),
    PointerInputData(
        timestamp = 216691121L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(268.5714416503906.px, 366.px)
    ),
    PointerInputData(
        timestamp = 216691130L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(268.8571472167969.px, 364.5714416503906.px)
    ),
    PointerInputData(
        timestamp = 216691137L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(269.1428527832031.px, 363.71429443359375.px)
    ),
    PointerInputData(
        timestamp = 216691145L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(269.1428527832031.px, 362.8571472167969.px)
    ),
    PointerInputData(
        timestamp = 216691153L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(269.4285583496094.px, 362.8571472167969.px)
    ),
    PointerInputData(
        timestamp = 216691168L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(268.5714416503906.px, 365.4285583496094.px)
    ),
    PointerInputData(
        timestamp = 216691176L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(267.1428527832031.px, 370.28570556640625.px)
    ),
    PointerInputData(
        timestamp = 216691183L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(265.4285583496094.px, 376.8571472167969.px)
    ),
    PointerInputData(
        timestamp = 216691191L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(263.1428527832031.px, 385.71429443359375.px)
    ),
    PointerInputData(
        timestamp = 216691199L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(261.4285583496094.px, 396.5714416503906.px)
    ),
    PointerInputData(
        timestamp = 216691207L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(259.71429443359375.px, 408.5714416503906.px)
    ),
    PointerInputData(
        timestamp = 216691215L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(258.28570556640625.px, 419.4285583496094.px)
    ),
    PointerInputData(
        timestamp = 216691222L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(257.4285583496094.px, 428.5714416503906.px)
    ),
    PointerInputData(
        timestamp = 216691230L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(256.28570556640625.px, 436.px)
    ),
    PointerInputData(
        timestamp = 216691238L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(255.7142791748047.px, 442.px)
    ),
    PointerInputData(
        timestamp = 216691245L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(255.14285278320312.px, 447.71429443359375.px)
    ),
    PointerInputData(
        timestamp = 216691253L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(254.85714721679688.px, 453.1428527832031.px)
    ),
    PointerInputData(
        timestamp = 216691261L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(254.57142639160156.px, 458.5714416503906.px)
    ),
    PointerInputData(
        timestamp = 216691268L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(254.2857208251953.px, 463.71429443359375.px)
    ),
    PointerInputData(
        timestamp = 216691276L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(254.2857208251953.px, 470.28570556640625.px)
    ),
    PointerInputData(
        timestamp = 216691284L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(254.2857208251953.px, 477.71429443359375.px)
    ),
    PointerInputData(
        timestamp = 216691292L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(255.7142791748047.px, 487.1428527832031.px)
    ),
    PointerInputData(
        timestamp = 216691300L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(256.8571472167969.px, 498.5714416503906.px)
    ),
    PointerInputData(
        timestamp = 216691307L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(258.28570556640625.px, 507.71429443359375.px)
    ),
    PointerInputData(
        timestamp = 216691315L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(259.4285583496094.px, 516.px)
    ),
    PointerInputData(
        timestamp = 216691323L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(260.28570556640625.px, 521.7142944335938.px)
    ),
    PointerInputData(
        timestamp = 216691338L.millisecondsToTimestamp(),
        down = false,
        position = PxPosition(260.28570556640625.px, 521.7142944335938.px)
    ),
    PointerInputData(
        timestamp = 216691573L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(266.px, 327.4285583496094.px)
    ),
    PointerInputData(
        timestamp = 216691588L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(266.px, 327.4285583496094.px)
    ),
    PointerInputData(
        timestamp = 216691626L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(261.1428527832031.px, 337.1428527832031.px)
    ),
    PointerInputData(
        timestamp = 216691634L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(258.28570556640625.px, 343.1428527832031.px)
    ),
    PointerInputData(
        timestamp = 216691642L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(254.57142639160156.px, 354.px)
    ),
    PointerInputData(
        timestamp = 216691650L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(250.2857208251953.px, 368.28570556640625.px)
    ),
    PointerInputData(
        timestamp = 216691657L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(247.42857360839844.px, 382.8571472167969.px)
    ),
    PointerInputData(
        timestamp = 216691665L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(245.14285278320312.px, 397.4285583496094.px)
    ),
    PointerInputData(
        timestamp = 216691673L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(243.14285278320312.px, 411.71429443359375.px)
    ),
    PointerInputData(
        timestamp = 216691680L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(242.2857208251953.px, 426.28570556640625.px)
    ),
    PointerInputData(
        timestamp = 216691688L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(241.7142791748047.px, 440.5714416503906.px)
    ),
    PointerInputData(
        timestamp = 216691696L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(241.7142791748047.px, 454.5714416503906.px)
    ),
    PointerInputData(
        timestamp = 216691703L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(242.57142639160156.px, 467.71429443359375.px)
    ),
    PointerInputData(
        timestamp = 216691712L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(243.42857360839844.px, 477.4285583496094.px)
    ),
    PointerInputData(
        timestamp = 216691720L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(244.85714721679688.px, 485.71429443359375.px)
    ),
    PointerInputData(
        timestamp = 216691727L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(246.2857208251953.px, 493.1428527832031.px)
    ),
    PointerInputData(
        timestamp = 216691735L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(248.px, 499.71429443359375.px)
    ),
    PointerInputData(
        timestamp = 216691750L.millisecondsToTimestamp(),
        down = false,
        position = PxPosition(248.px, 499.71429443359375.px)
    ),
    PointerInputData(
        timestamp = 216692255L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(249.42857360839844.px, 351.4285583496094.px)
    ),
    PointerInputData(
        timestamp = 216692270L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(249.42857360839844.px, 351.4285583496094.px)
    ),
    PointerInputData(
        timestamp = 216692309L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(246.2857208251953.px, 361.71429443359375.px)
    ),
    PointerInputData(
        timestamp = 216692317L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(244.px, 368.5714416503906.px)
    ),
    PointerInputData(
        timestamp = 216692325L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(241.42857360839844.px, 377.71429443359375.px)
    ),
    PointerInputData(
        timestamp = 216692333L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(237.7142791748047.px, 391.71429443359375.px)
    ),
    PointerInputData(
        timestamp = 216692340L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(235.14285278320312.px, 406.5714416503906.px)
    ),
    PointerInputData(
        timestamp = 216692348L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(232.57142639160156.px, 421.4285583496094.px)
    ),
    PointerInputData(
        timestamp = 216692356L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(230.2857208251953.px, 436.5714416503906.px)
    ),
    PointerInputData(
        timestamp = 216692363L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(228.2857208251953.px, 451.71429443359375.px)
    ),
    PointerInputData(
        timestamp = 216692371L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(227.42857360839844.px, 466.px)
    ),
    PointerInputData(
        timestamp = 216692378L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(226.2857208251953.px, 479.71429443359375.px)
    ),
    PointerInputData(
        timestamp = 216692387L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(225.7142791748047.px, 491.71429443359375.px)
    ),
    PointerInputData(
        timestamp = 216692395L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(225.14285278320312.px, 501.71429443359375.px)
    ),
    PointerInputData(
        timestamp = 216692402L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(224.85714721679688.px, 509.1428527832031.px)
    ),
    PointerInputData(
        timestamp = 216692410L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(224.57142639160156.px, 514.8571166992188.px)
    ),
    PointerInputData(
        timestamp = 216692418L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(224.2857208251953.px, 519.4285888671875.px)
    ),
    PointerInputData(
        timestamp = 216692425L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(224.px, 523.4285888671875.px)
    ),
    PointerInputData(
        timestamp = 216692433L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(224.px, 527.1428833007812.px)
    ),
    PointerInputData(
        timestamp = 216692441L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(224.px, 530.5714111328125.px)
    ),
    PointerInputData(
        timestamp = 216692448L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(224.px, 533.1428833007812.px)
    ),
    PointerInputData(
        timestamp = 216692456L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(224.px, 535.4285888671875.px)
    ),
    PointerInputData(
        timestamp = 216692464L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(223.7142791748047.px, 536.8571166992188.px)
    ),
    PointerInputData(
        timestamp = 216692472L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(223.7142791748047.px, 538.2857055664062.px)
    ),
    PointerInputData(
        timestamp = 216692487L.millisecondsToTimestamp(),
        down = false,
        position = PxPosition(223.7142791748047.px, 538.2857055664062.px)
    ),
    PointerInputData(
        timestamp = 216692678L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(221.42857360839844.px, 526.2857055664062.px)
    ),
    PointerInputData(
        timestamp = 216692701L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(220.57142639160156.px, 514.8571166992188.px)
    ),
    PointerInputData(
        timestamp = 216692708L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(220.2857208251953.px, 508.px)
    ),
    PointerInputData(
        timestamp = 216692716L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(220.2857208251953.px, 498.px)
    ),
    PointerInputData(
        timestamp = 216692724L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(221.14285278320312.px, 484.28570556640625.px)
    ),
    PointerInputData(
        timestamp = 216692732L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(221.7142791748047.px, 469.4285583496094.px)
    ),
    PointerInputData(
        timestamp = 216692740L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(223.42857360839844.px, 453.1428527832031.px)
    ),
    PointerInputData(
        timestamp = 216692748L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(225.7142791748047.px, 436.28570556640625.px)
    ),
    PointerInputData(
        timestamp = 216692755L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(229.14285278320312.px, 418.28570556640625.px)
    ),
    PointerInputData(
        timestamp = 216692763L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(232.85714721679688.px, 400.28570556640625.px)
    ),
    PointerInputData(
        timestamp = 216692770L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(236.85714721679688.px, 382.5714416503906.px)
    ),
    PointerInputData(
        timestamp = 216692778L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(241.14285278320312.px, 366.px)
    ),
    PointerInputData(
        timestamp = 216692786L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(244.85714721679688.px, 350.28570556640625.px)
    ),
    PointerInputData(
        timestamp = 216692793L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(249.14285278320312.px, 335.4285583496094.px)
    ),
    PointerInputData(
        timestamp = 216692809L.millisecondsToTimestamp(),
        down = false,
        position = PxPosition(249.14285278320312.px, 335.4285583496094.px)
    ),
    PointerInputData(
        timestamp = 216693222L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(224.px, 545.4285888671875.px)
    ),
    PointerInputData(
        timestamp = 216693245L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(224.px, 545.4285888671875.px)
    ),
    PointerInputData(
        timestamp = 216693275L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(222.85714721679688.px, 535.1428833007812.px)
    ),
    PointerInputData(
        timestamp = 216693284L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(222.85714721679688.px, 528.8571166992188.px)
    ),
    PointerInputData(
        timestamp = 216693291L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(222.2857208251953.px, 518.5714111328125.px)
    ),
    PointerInputData(
        timestamp = 216693299L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(222.px, 503.4285583496094.px)
    ),
    PointerInputData(
        timestamp = 216693307L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(222.px, 485.4285583496094.px)
    ),
    PointerInputData(
        timestamp = 216693314L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(221.7142791748047.px, 464.px)
    ),
    PointerInputData(
        timestamp = 216693322L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(222.2857208251953.px, 440.28570556640625.px)
    ),
    PointerInputData(
        timestamp = 216693337L.millisecondsToTimestamp(),
        down = false,
        position = PxPosition(222.2857208251953.px, 440.28570556640625.px)
    ),
    PointerInputData(
        timestamp = 216693985L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(208.px, 544.px)
    ),
    PointerInputData(
        timestamp = 216694047L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(208.57142639160156.px, 532.2857055664062.px)
    ),
    PointerInputData(
        timestamp = 216694054L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(208.85714721679688.px, 525.7142944335938.px)
    ),
    PointerInputData(
        timestamp = 216694062L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(208.85714721679688.px, 515.1428833007812.px)
    ),
    PointerInputData(
        timestamp = 216694070L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(208.px, 501.4285583496094.px)
    ),
    PointerInputData(
        timestamp = 216694077L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(207.42857360839844.px, 487.1428527832031.px)
    ),
    PointerInputData(
        timestamp = 216694085L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(206.57142639160156.px, 472.8571472167969.px)
    ),
    PointerInputData(
        timestamp = 216694092L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(206.57142639160156.px, 458.8571472167969.px)
    ),
    PointerInputData(
        timestamp = 216694100L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(206.57142639160156.px, 446.px)
    ),
    PointerInputData(
        timestamp = 216694108L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(206.57142639160156.px, 434.28570556640625.px)
    ),
    PointerInputData(
        timestamp = 216694116L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(207.14285278320312.px, 423.71429443359375.px)
    ),
    PointerInputData(
        timestamp = 216694124L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(208.57142639160156.px, 412.8571472167969.px)
    ),
    PointerInputData(
        timestamp = 216694131L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(209.7142791748047.px, 402.28570556640625.px)
    ),
    PointerInputData(
        timestamp = 216694139L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(211.7142791748047.px, 393.1428527832031.px)
    ),
    PointerInputData(
        timestamp = 216694147L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(213.42857360839844.px, 385.1428527832031.px)
    ),
    PointerInputData(
        timestamp = 216694154L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(215.42857360839844.px, 378.28570556640625.px)
    ),
    PointerInputData(
        timestamp = 216694162L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(217.42857360839844.px, 371.71429443359375.px)
    ),
    PointerInputData(
        timestamp = 216694169L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(219.42857360839844.px, 366.px)
    ),
    PointerInputData(
        timestamp = 216694177L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(221.42857360839844.px, 360.8571472167969.px)
    ),
    PointerInputData(
        timestamp = 216694185L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(223.42857360839844.px, 356.5714416503906.px)
    ),
    PointerInputData(
        timestamp = 216694193L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(225.14285278320312.px, 352.28570556640625.px)
    ),
    PointerInputData(
        timestamp = 216694201L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(226.85714721679688.px, 348.5714416503906.px)
    ),
    PointerInputData(
        timestamp = 216694209L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(228.2857208251953.px, 346.px)
    ),
    PointerInputData(
        timestamp = 216694216L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(229.14285278320312.px, 343.71429443359375.px)
    ),
    PointerInputData(
        timestamp = 216694224L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(230.px, 342.px)
    ),
    PointerInputData(
        timestamp = 216694232L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(230.57142639160156.px, 340.5714416503906.px)
    ),
    PointerInputData(
        timestamp = 216694239L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(230.85714721679688.px, 339.71429443359375.px)
    ),
    PointerInputData(
        timestamp = 216694247L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(230.85714721679688.px, 339.4285583496094.px)
    ),
    PointerInputData(
        timestamp = 216694262L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(230.2857208251953.px, 342.px)
    ),
    PointerInputData(
        timestamp = 216694270L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(228.85714721679688.px, 346.28570556640625.px)
    ),
    PointerInputData(
        timestamp = 216694278L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(227.14285278320312.px, 352.5714416503906.px)
    ),
    PointerInputData(
        timestamp = 216694286L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(225.42857360839844.px, 359.4285583496094.px)
    ),
    PointerInputData(
        timestamp = 216694294L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(223.7142791748047.px, 367.71429443359375.px)
    ),
    PointerInputData(
        timestamp = 216694301L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(222.57142639160156.px, 376.px)
    ),
    PointerInputData(
        timestamp = 216694309L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(221.42857360839844.px, 384.28570556640625.px)
    ),
    PointerInputData(
        timestamp = 216694317L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(220.85714721679688.px, 392.28570556640625.px)
    ),
    PointerInputData(
        timestamp = 216694324L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(220.px, 400.5714416503906.px)
    ),
    PointerInputData(
        timestamp = 216694332L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(219.14285278320312.px, 409.71429443359375.px)
    ),
    PointerInputData(
        timestamp = 216694339L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(218.85714721679688.px, 419.1428527832031.px)
    ),
    PointerInputData(
        timestamp = 216694348L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(218.2857208251953.px, 428.8571472167969.px)
    ),
    PointerInputData(
        timestamp = 216694356L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(218.2857208251953.px, 438.8571472167969.px)
    ),
    PointerInputData(
        timestamp = 216694363L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(218.2857208251953.px, 447.71429443359375.px)
    ),
    PointerInputData(
        timestamp = 216694371L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(218.2857208251953.px, 455.71429443359375.px)
    ),
    PointerInputData(
        timestamp = 216694379L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(219.14285278320312.px, 462.8571472167969.px)
    ),
    PointerInputData(
        timestamp = 216694386L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(220.px, 469.4285583496094.px)
    ),
    PointerInputData(
        timestamp = 216694394L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(221.14285278320312.px, 475.4285583496094.px)
    ),
    PointerInputData(
        timestamp = 216694401L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(222.px, 480.5714416503906.px)
    ),
    PointerInputData(
        timestamp = 216694409L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(222.85714721679688.px, 485.4285583496094.px)
    ),
    PointerInputData(
        timestamp = 216694417L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(224.px, 489.71429443359375.px)
    ),
    PointerInputData(
        timestamp = 216694425L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(224.85714721679688.px, 492.8571472167969.px)
    ),
    PointerInputData(
        timestamp = 216694433L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(225.42857360839844.px, 495.4285583496094.px)
    ),
    PointerInputData(
        timestamp = 216694440L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(226.px, 497.1428527832031.px)
    ),
    PointerInputData(
        timestamp = 216694448L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(226.2857208251953.px, 498.28570556640625.px)
    ),
    PointerInputData(
        timestamp = 216694456L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(226.2857208251953.px, 498.8571472167969.px)
    ),
    PointerInputData(
        timestamp = 216694471L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(226.2857208251953.px, 498.28570556640625.px)
    ),
    PointerInputData(
        timestamp = 216694479L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(226.2857208251953.px, 496.5714416503906.px)
    ),
    PointerInputData(
        timestamp = 216694486L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(226.2857208251953.px, 493.71429443359375.px)
    ),
    PointerInputData(
        timestamp = 216694494L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(226.2857208251953.px, 490.px)
    ),
    PointerInputData(
        timestamp = 216694502L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(226.2857208251953.px, 486.px)
    ),
    PointerInputData(
        timestamp = 216694510L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(226.2857208251953.px, 480.5714416503906.px)
    ),
    PointerInputData(
        timestamp = 216694518L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(226.2857208251953.px, 475.71429443359375.px)
    ),
    PointerInputData(
        timestamp = 216694525L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(226.2857208251953.px, 468.8571472167969.px)
    ),
    PointerInputData(
        timestamp = 216694533L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(226.2857208251953.px, 461.4285583496094.px)
    ),
    PointerInputData(
        timestamp = 216694541L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(226.2857208251953.px, 452.5714416503906.px)
    ),
    PointerInputData(
        timestamp = 216694548L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(226.57142639160156.px, 442.28570556640625.px)
    ),
    PointerInputData(
        timestamp = 216694556L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(226.57142639160156.px, 432.28570556640625.px)
    ),
    PointerInputData(
        timestamp = 216694564L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(226.85714721679688.px, 423.4285583496094.px)
    ),
    PointerInputData(
        timestamp = 216694571L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(227.42857360839844.px, 416.px)
    ),
    PointerInputData(
        timestamp = 216694580L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(227.7142791748047.px, 410.px)
    ),
    PointerInputData(
        timestamp = 216694587L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(228.2857208251953.px, 404.28570556640625.px)
    ),
    PointerInputData(
        timestamp = 216694595L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(228.85714721679688.px, 399.71429443359375.px)
    ),
    PointerInputData(
        timestamp = 216694603L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(229.14285278320312.px, 395.4285583496094.px)
    ),
    PointerInputData(
        timestamp = 216694610L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(229.42857360839844.px, 392.28570556640625.px)
    ),
    PointerInputData(
        timestamp = 216694618L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(229.7142791748047.px, 390.px)
    ),
    PointerInputData(
        timestamp = 216694625L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(229.7142791748047.px, 388.px)
    ),
    PointerInputData(
        timestamp = 216694633L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(229.7142791748047.px, 386.8571472167969.px)
    ),
    PointerInputData(
        timestamp = 216694641L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(229.7142791748047.px, 386.28570556640625.px)
    ),
    PointerInputData(
        timestamp = 216694648L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(229.7142791748047.px, 386.px)
    ),
    PointerInputData(
        timestamp = 216694657L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(228.85714721679688.px, 386.px)
    ),
    PointerInputData(
        timestamp = 216694665L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(228.px, 388.px)
    ),
    PointerInputData(
        timestamp = 216694672L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(226.px, 392.5714416503906.px)
    ),
    PointerInputData(
        timestamp = 216694680L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(224.px, 397.71429443359375.px)
    ),
    PointerInputData(
        timestamp = 216694688L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(222.px, 404.28570556640625.px)
    ),
    PointerInputData(
        timestamp = 216694695L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(219.7142791748047.px, 411.1428527832031.px)
    ),
    PointerInputData(
        timestamp = 216694703L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(218.2857208251953.px, 418.px)
    ),
    PointerInputData(
        timestamp = 216694710L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(217.14285278320312.px, 425.4285583496094.px)
    ),
    PointerInputData(
        timestamp = 216694718L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(215.7142791748047.px, 433.4285583496094.px)
    ),
    PointerInputData(
        timestamp = 216694726L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(214.85714721679688.px, 442.28570556640625.px)
    ),
    PointerInputData(
        timestamp = 216694734L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(214.px, 454.px)
    ),
    PointerInputData(
        timestamp = 216694742L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(214.px, 469.4285583496094.px)
    ),
    PointerInputData(
        timestamp = 216694749L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(215.42857360839844.px, 485.4285583496094.px)
    ),
    PointerInputData(
        timestamp = 216694757L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(217.7142791748047.px, 502.8571472167969.px)
    ),
    PointerInputData(
        timestamp = 216694765L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(221.14285278320312.px, 521.4285888671875.px)
    ),
    PointerInputData(
        timestamp = 216694772L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(224.57142639160156.px, 541.1428833007812.px)
    ),
    PointerInputData(
        timestamp = 216694780L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(229.14285278320312.px, 561.1428833007812.px)
    ),
    PointerInputData(
        timestamp = 216694788L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(233.42857360839844.px, 578.8571166992188.px)
    ),
    PointerInputData(
        timestamp = 216694802L.millisecondsToTimestamp(),
        down = false,
        position = PxPosition(233.42857360839844.px, 578.8571166992188.px)
    ),
    PointerInputData(
        timestamp = 216695344L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(253.42857360839844.px, 310.5714416503906.px)
    ),
    PointerInputData(
        timestamp = 216695352L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(253.42857360839844.px, 310.5714416503906.px)
    ),
    PointerInputData(
        timestamp = 216695359L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(252.85714721679688.px, 318.px)
    ),
    PointerInputData(
        timestamp = 216695367L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(251.14285278320312.px, 322.px)
    ),
    PointerInputData(
        timestamp = 216695375L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(248.85714721679688.px, 327.1428527832031.px)
    ),
    PointerInputData(
        timestamp = 216695382L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(246.px, 334.8571472167969.px)
    ),
    PointerInputData(
        timestamp = 216695390L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(242.57142639160156.px, 344.5714416503906.px)
    ),
    PointerInputData(
        timestamp = 216695397L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(238.85714721679688.px, 357.4285583496094.px)
    ),
    PointerInputData(
        timestamp = 216695406L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(235.7142791748047.px, 371.71429443359375.px)
    ),
    PointerInputData(
        timestamp = 216695414L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(232.2857208251953.px, 386.8571472167969.px)
    ),
    PointerInputData(
        timestamp = 216695421L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(229.42857360839844.px, 402.px)
    ),
    PointerInputData(
        timestamp = 216695429L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(227.42857360839844.px, 416.8571472167969.px)
    ),
    PointerInputData(
        timestamp = 216695437L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(226.2857208251953.px, 431.4285583496094.px)
    ),
    PointerInputData(
        timestamp = 216695444L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(226.2857208251953.px, 446.px)
    ),
    PointerInputData(
        timestamp = 216695452L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(227.7142791748047.px, 460.28570556640625.px)
    ),
    PointerInputData(
        timestamp = 216695459L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(230.px, 475.1428527832031.px)
    ),
    PointerInputData(
        timestamp = 216695467L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(232.2857208251953.px, 489.71429443359375.px)
    ),
    PointerInputData(
        timestamp = 216695475L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(235.7142791748047.px, 504.px)
    ),
    PointerInputData(
        timestamp = 216695490L.millisecondsToTimestamp(),
        down = false,
        position = PxPosition(235.7142791748047.px, 504.px)
    ),
    PointerInputData(
        timestamp = 216695885L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(238.85714721679688.px, 524.px)
    ),
    PointerInputData(
        timestamp = 216695908L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(236.2857208251953.px, 515.7142944335938.px)
    ),
    PointerInputData(
        timestamp = 216695916L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(234.85714721679688.px, 509.1428527832031.px)
    ),
    PointerInputData(
        timestamp = 216695924L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(232.57142639160156.px, 498.5714416503906.px)
    ),
    PointerInputData(
        timestamp = 216695931L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(230.57142639160156.px, 483.71429443359375.px)
    ),
    PointerInputData(
        timestamp = 216695939L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(229.14285278320312.px, 466.5714416503906.px)
    ),
    PointerInputData(
        timestamp = 216695947L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(229.14285278320312.px, 446.5714416503906.px)
    ),
    PointerInputData(
        timestamp = 216695955L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(230.57142639160156.px, 424.8571472167969.px)
    ),
    PointerInputData(
        timestamp = 216695963L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(232.57142639160156.px, 402.28570556640625.px)
    ),
    PointerInputData(
        timestamp = 216695970L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(235.14285278320312.px, 380.px)
    ),
    PointerInputData(
        timestamp = 216695978L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(238.57142639160156.px, 359.4285583496094.px)
    ),
    PointerInputData(
        timestamp = 216695993L.millisecondsToTimestamp(),
        down = false,
        position = PxPosition(238.57142639160156.px, 359.4285583496094.px)
    ),
    PointerInputData(
        timestamp = 216696429L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(238.2857208251953.px, 568.5714111328125.px)
    ),
    PointerInputData(
        timestamp = 216696459L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(234.px, 560.px)
    ),
    PointerInputData(
        timestamp = 216696467L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(231.42857360839844.px, 553.1428833007812.px)
    ),
    PointerInputData(
        timestamp = 216696475L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(228.2857208251953.px, 543.1428833007812.px)
    ),
    PointerInputData(
        timestamp = 216696483L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(225.42857360839844.px, 528.8571166992188.px)
    ),
    PointerInputData(
        timestamp = 216696491L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(223.14285278320312.px, 512.2857055664062.px)
    ),
    PointerInputData(
        timestamp = 216696498L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(222.px, 495.4285583496094.px)
    ),
    PointerInputData(
        timestamp = 216696506L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(221.7142791748047.px, 477.4285583496094.px)
    ),
    PointerInputData(
        timestamp = 216696514L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(221.7142791748047.px, 458.28570556640625.px)
    ),
    PointerInputData(
        timestamp = 216696521L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(223.14285278320312.px, 438.px)
    ),
    PointerInputData(
        timestamp = 216696529L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(224.2857208251953.px, 416.28570556640625.px)
    ),
    PointerInputData(
        timestamp = 216696544L.millisecondsToTimestamp(),
        down = false,
        position = PxPosition(224.2857208251953.px, 416.28570556640625.px)
    ),
    PointerInputData(
        timestamp = 216696974L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(218.57142639160156.px, 530.5714111328125.px)
    ),
    PointerInputData(
        timestamp = 216697012L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(220.2857208251953.px, 522.px)
    ),
    PointerInputData(
        timestamp = 216697020L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(221.14285278320312.px, 517.7142944335938.px)
    ),
    PointerInputData(
        timestamp = 216697028L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(222.2857208251953.px, 511.71429443359375.px)
    ),
    PointerInputData(
        timestamp = 216697036L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(224.px, 504.28570556640625.px)
    ),
    PointerInputData(
        timestamp = 216697044L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(227.14285278320312.px, 490.5714416503906.px)
    ),
    PointerInputData(
        timestamp = 216697052L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(229.42857360839844.px, 474.px)
    ),
    PointerInputData(
        timestamp = 216697059L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(231.42857360839844.px, 454.5714416503906.px)
    ),
    PointerInputData(
        timestamp = 216697067L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(233.7142791748047.px, 431.1428527832031.px)
    ),
    PointerInputData(
        timestamp = 216697082L.millisecondsToTimestamp(),
        down = false,
        position = PxPosition(233.7142791748047.px, 431.1428527832031.px)
    ),
    PointerInputData(
        timestamp = 216697435L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(257.1428527832031.px, 285.1428527832031.px)
    ),
    PointerInputData(
        timestamp = 216697465L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(251.7142791748047.px, 296.8571472167969.px)
    ),
    PointerInputData(
        timestamp = 216697473L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(248.2857208251953.px, 304.px)
    ),
    PointerInputData(
        timestamp = 216697481L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(244.57142639160156.px, 314.8571472167969.px)
    ),
    PointerInputData(
        timestamp = 216697489L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(240.2857208251953.px, 329.1428527832031.px)
    ),
    PointerInputData(
        timestamp = 216697497L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(236.85714721679688.px, 345.1428527832031.px)
    ),
    PointerInputData(
        timestamp = 216697505L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(233.7142791748047.px, 361.4285583496094.px)
    ),
    PointerInputData(
        timestamp = 216697512L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(231.14285278320312.px, 378.28570556640625.px)
    ),
    PointerInputData(
        timestamp = 216697520L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(229.42857360839844.px, 395.4285583496094.px)
    ),
    PointerInputData(
        timestamp = 216697528L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(229.42857360839844.px, 412.8571472167969.px)
    ),
    PointerInputData(
        timestamp = 216697535L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(230.85714721679688.px, 430.8571472167969.px)
    ),
    PointerInputData(
        timestamp = 216697543L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(233.42857360839844.px, 449.71429443359375.px)
    ),
    PointerInputData(
        timestamp = 216697558L.millisecondsToTimestamp(),
        down = false,
        position = PxPosition(233.42857360839844.px, 449.71429443359375.px)
    ),
    PointerInputData(
        timestamp = 216697749L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(246.px, 311.4285583496094.px)
    ),
    PointerInputData(
        timestamp = 216697780L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(244.57142639160156.px, 318.28570556640625.px)
    ),
    PointerInputData(
        timestamp = 216697787L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(243.14285278320312.px, 325.4285583496094.px)
    ),
    PointerInputData(
        timestamp = 216697795L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(241.42857360839844.px, 336.px)
    ),
    PointerInputData(
        timestamp = 216697803L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(239.7142791748047.px, 351.1428527832031.px)
    ),
    PointerInputData(
        timestamp = 216697811L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(238.2857208251953.px, 368.5714416503906.px)
    ),
    PointerInputData(
        timestamp = 216697819L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(238.px, 389.4285583496094.px)
    ),
    PointerInputData(
        timestamp = 216697826L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(239.14285278320312.px, 412.px)
    ),
    PointerInputData(
        timestamp = 216697834L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(242.2857208251953.px, 438.px)
    ),
    PointerInputData(
        timestamp = 216697842L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(247.42857360839844.px, 466.8571472167969.px)
    ),
    PointerInputData(
        timestamp = 216697849L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(254.2857208251953.px, 497.71429443359375.px)
    ),
    PointerInputData(
        timestamp = 216697864L.millisecondsToTimestamp(),
        down = false,
        position = PxPosition(254.2857208251953.px, 497.71429443359375.px)
    ),
    PointerInputData(
        timestamp = 216698321L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(250.px, 306.px)
    ),
    PointerInputData(
        timestamp = 216698328L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(250.px, 306.px)
    ),
    PointerInputData(
        timestamp = 216698344L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(249.14285278320312.px, 314.px)
    ),
    PointerInputData(
        timestamp = 216698351L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(247.42857360839844.px, 319.4285583496094.px)
    ),
    PointerInputData(
        timestamp = 216698359L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(245.14285278320312.px, 326.8571472167969.px)
    ),
    PointerInputData(
        timestamp = 216698366L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(241.7142791748047.px, 339.4285583496094.px)
    ),
    PointerInputData(
        timestamp = 216698374L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(238.57142639160156.px, 355.71429443359375.px)
    ),
    PointerInputData(
        timestamp = 216698382L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(236.2857208251953.px, 374.28570556640625.px)
    ),
    PointerInputData(
        timestamp = 216698390L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(235.14285278320312.px, 396.5714416503906.px)
    ),
    PointerInputData(
        timestamp = 216698398L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(236.57142639160156.px, 421.4285583496094.px)
    ),
    PointerInputData(
        timestamp = 216698406L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(241.14285278320312.px, 451.4285583496094.px)
    ),
    PointerInputData(
        timestamp = 216698421L.millisecondsToTimestamp(),
        down = false,
        position = PxPosition(241.14285278320312.px, 451.4285583496094.px)
    )
)

val interruptedVelocityEventData: List<PointerInputData> = listOf(
    PointerInputData(
        timestamp = 216698321L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(250.px, 306.px)
    ),
    PointerInputData(
        timestamp = 216698328L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(250.px, 306.px)
    ),
    PointerInputData(
        timestamp = 216698344L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(249.14285278320312.px, 314.px)
    ),
    PointerInputData(
        timestamp = 216698351L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(247.42857360839844.px, 319.4285583496094.px)
    ),
    PointerInputData(
        timestamp = 216698359L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(245.14285278320312.px, 326.8571472167969.px)
    ),
    PointerInputData(
        timestamp = 216698366L.millisecondsToTimestamp(),
        down = true,
        position = PxPosition(241.7142791748047.px, 339.4285583496094.px)
    ),

// The pointer "stops" here because we've introduced a 40+ms gap
// in the move event stream. See kAssumePointerMoveStoppedMilliseconds
// in velocity_tracker.dart.

    PointerInputData(
        timestamp = (216698374 + 40).toLong().millisecondsToTimestamp(),
        down = true,
        position = PxPosition(238.57142639160156.px, 355.71429443359375.px)
    ),
    PointerInputData(
        timestamp = (216698382 + 40).toLong().millisecondsToTimestamp(),
        down = true,
        position = PxPosition(236.2857208251953.px, 374.28570556640625.px)
    ),
    PointerInputData(
        timestamp = (216698390 + 40).toLong().millisecondsToTimestamp(),
        down = true,
        position = PxPosition(235.14285278320312.px, 396.5714416503906.px)
    ),
    PointerInputData(
        timestamp = (216698398 + 40).toLong().millisecondsToTimestamp(),
        down = true,
        position = PxPosition(236.57142639160156.px, 421.4285583496094.px)
    ),
    PointerInputData(
        timestamp = (216698406 + 40).toLong().millisecondsToTimestamp(),
        down = true,
        position = PxPosition(241.14285278320312.px, 451.4285583496094.px)
    ),
    PointerInputData(
        timestamp = (216698421 + 40).toLong().millisecondsToTimestamp(),
        down = false,
        position = PxPosition(241.14285278320312.px, 451.4285583496094.px)
    )
)