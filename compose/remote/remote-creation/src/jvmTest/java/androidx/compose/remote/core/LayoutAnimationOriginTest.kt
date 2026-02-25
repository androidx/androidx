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

package androidx.compose.remote.core

import androidx.compose.remote.core.layout.CaptureAnimatedState
import androidx.compose.remote.core.layout.Color
import androidx.compose.remote.core.layout.ResizeWithOrigin
import androidx.compose.remote.core.layout.TestOperation
import org.junit.Test

class LayoutAnimationOriginTest : BaseLayoutTest() {

    init {
        GENERATE_GOLD_FILES = false
    }

    @Test
    fun testOriginAwareResize() {
        val ops =
            arrayListOf<TestOperation>(
                TestLayout {
                    column(Modifier.fillMaxSize()) {
                        // A simple box at (0,0) inside the column
                        box(Modifier.size(100, 100).background(Color.RED)) { text("Box") }
                    }
                },
                // 1. Initial state: width 500, height 500, origin (0,0)
                // The box should be at (0,0)

                // 2. Resize from left: new width 600, new origin (100, 0)
                // This means the player moved 100px to the right on screen,
                // and expanded 100px to the left.
                // The box should now be at (100, 0) locally to stay at the same screen position.
                // We want to animate this transition.
                ResizeWithOrigin(600, 500, 100f, 0f),

                // Capture at start of animation (t=0)
                // It should have mX = -100 (compensating for origin jump)
                CaptureAnimatedState(0),

                // Capture at mid animation (t=300ms, default duration is 600ms)
                // It should have mX = -50
                CaptureAnimatedState(300),

                // Capture at end of animation (t=600ms)
                // It should have mX = 0
                CaptureAnimatedState(300),
            )
        checkLayout(
            500,
            500,
            7,
            RcProfiles.PROFILE_ANDROIDX or RcProfiles.PROFILE_EXPERIMENTAL,
            "OriginAwareResize",
            ops,
            TestClock(1234),
            true,
        )
    }
}
