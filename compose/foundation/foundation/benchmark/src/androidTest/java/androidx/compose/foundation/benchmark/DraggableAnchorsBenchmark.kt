/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.compose.foundation.benchmark

import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private enum class DraggableAnchorsSampleValue {
    Start,
    HalfStart,
    Center,
    HalfEnd,
    End
}

@RunWith(AndroidJUnit4::class)
@Suppress("unused")
@MediumTest
class DraggableAnchorsBenchmark {

    @get:Rule val benchmarkRule = BenchmarkRule()

    @Test
    fun constructAnchors() {
        benchmarkRule.measureRepeated {
            DraggableAnchors {
                DraggableAnchorsSampleValue.Start at 0f
                DraggableAnchorsSampleValue.HalfStart at 100f
                DraggableAnchorsSampleValue.Center at 200f
                DraggableAnchorsSampleValue.HalfEnd at 300f
                DraggableAnchorsSampleValue.End at 400f
            }
        }
    }

    @Test
    fun positionOf() {
        val anchors = DraggableAnchors {
            DraggableAnchorsSampleValue.Start at 0f
            DraggableAnchorsSampleValue.HalfStart at 100f
            DraggableAnchorsSampleValue.Center at 200f
            DraggableAnchorsSampleValue.HalfEnd at 300f
            DraggableAnchorsSampleValue.End at 400f
        }
        benchmarkRule.measureRepeated { anchors.positionOf(DraggableAnchorsSampleValue.Center) }
    }

    @Test
    fun findClosestUpwards() {
        val anchors = DraggableAnchors {
            DraggableAnchorsSampleValue.Start at 0f
            DraggableAnchorsSampleValue.HalfStart at 100f
            DraggableAnchorsSampleValue.Center at 200f
            DraggableAnchorsSampleValue.HalfEnd at 300f
            DraggableAnchorsSampleValue.End at 400f
        }
        benchmarkRule.measureRepeated { anchors.closestAnchor(250f, searchUpwards = true) }
    }

    @Test
    fun findClosestDownwards() {
        val anchors = DraggableAnchors {
            DraggableAnchorsSampleValue.Start at 0f
            DraggableAnchorsSampleValue.HalfStart at 100f
            DraggableAnchorsSampleValue.Center at 200f
            DraggableAnchorsSampleValue.HalfEnd at 300f
            DraggableAnchorsSampleValue.End at 400f
        }
        benchmarkRule.measureRepeated { anchors.closestAnchor(250f, searchUpwards = false) }
    }

    @Test
    fun findClosestNoDirection() {
        val anchors = DraggableAnchors {
            DraggableAnchorsSampleValue.Start at 0f
            DraggableAnchorsSampleValue.HalfStart at 100f
            DraggableAnchorsSampleValue.Center at 200f
            DraggableAnchorsSampleValue.HalfEnd at 300f
            DraggableAnchorsSampleValue.End at 400f
        }
        benchmarkRule.measureRepeated { anchors.closestAnchor(250f) }
    }

    @Test
    fun hasPositionFor() {
        val anchors = DraggableAnchors {
            DraggableAnchorsSampleValue.Start at 0f
            DraggableAnchorsSampleValue.HalfStart at 100f
            DraggableAnchorsSampleValue.Center at 200f
            DraggableAnchorsSampleValue.HalfEnd at 300f
            DraggableAnchorsSampleValue.End at 400f
        }
        benchmarkRule.measureRepeated { anchors.hasPositionFor(DraggableAnchorsSampleValue.Center) }
    }

    @Test
    fun minPosition() {
        val anchors = DraggableAnchors {
            DraggableAnchorsSampleValue.Start at 0f
            DraggableAnchorsSampleValue.HalfStart at 100f
            DraggableAnchorsSampleValue.Center at 200f
            DraggableAnchorsSampleValue.HalfEnd at 300f
            DraggableAnchorsSampleValue.End at 400f
        }
        benchmarkRule.measureRepeated { anchors.minPosition() }
    }

    @Test
    fun maxPosition() {
        val anchors = DraggableAnchors {
            DraggableAnchorsSampleValue.Start at 0f
            DraggableAnchorsSampleValue.HalfStart at 100f
            DraggableAnchorsSampleValue.Center at 200f
            DraggableAnchorsSampleValue.HalfEnd at 300f
            DraggableAnchorsSampleValue.End at 400f
        }
        benchmarkRule.measureRepeated { anchors.maxPosition() }
    }
}
