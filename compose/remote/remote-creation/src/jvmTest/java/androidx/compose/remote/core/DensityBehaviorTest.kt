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

import androidx.compose.remote.core.layout.TestOperation
import androidx.compose.remote.core.layout.TestParameters
import androidx.compose.remote.core.operations.Header
import androidx.compose.remote.creation.RemoteComposeContext
import androidx.compose.remote.creation.RemoteComposeWriter
import org.junit.Test

class DensityBehaviorTest : BaseLayoutTest() {

    init {
        GENERATE_GOLD_FILES = false
    }

    @Test
    fun testDensityBehaviorDp() {
        val density = 2.5f
        val paddingDp = 100
        val expectedPx = paddingDp * density
        val ops =
            arrayListOf<TestOperation>(
                TestLayout { box(Modifier.componentId(1).padding(paddingDp)) },
                validateSize(1, expectedPx * 2, expectedPx * 2),
            )

        checkLayoutWithDensityBehavior(
            2000,
            2000,
            8,
            RcProfiles.PROFILE_ANDROIDX,
            "DensityBehaviorDp",
            ops,
            density,
            CoreDocument.DENSITY_BEHAVIOR_DP,
        )
    }

    @Test
    fun testDensityBehaviorPixels() {
        val density = 2.5f
        val paddingPx = 100
        val expectedPx = paddingPx.toFloat()
        val ops =
            arrayListOf<TestOperation>(
                TestLayout { box(Modifier.componentId(1).padding(paddingPx)) },
                validateSize(1, expectedPx * 2, expectedPx * 2),
            )

        checkLayoutWithDensityBehavior(
            2000,
            2000,
            8,
            RcProfiles.PROFILE_ANDROIDX,
            "DensityBehaviorPixels",
            ops,
            density,
            CoreDocument.DENSITY_BEHAVIOR_PIXELS,
        )
    }

    private fun checkLayoutWithDensityBehavior(
        w: Int,
        h: Int,
        apiLevel: Int,
        profile: Int,
        description: String,
        ops: ArrayList<TestOperation>,
        density: Float,
        behavior: Int,
    ) {
        if (ops.size == 0 || ops[0] !is TestLayout) return
        val function = (ops[0] as TestLayout).layout
        val testParameters =
            TestParameters(name.getMethodName(), GENERATE_GOLD_FILES, TestClock(1234))

        val tags =
            arrayOf(
                RemoteComposeWriter.hTag(Header.DOC_WIDTH, w),
                RemoteComposeWriter.hTag(Header.DOC_HEIGHT, h),
                RemoteComposeWriter.hTag(Header.DOC_DENSITY_AT_GENERATION, density),
                RemoteComposeWriter.hTag(Header.DOC_DENSITY_BEHAVIOR, behavior),
                RemoteComposeWriter.hTag(Header.DOC_PROFILES, profile),
            )

        val writer =
            RemoteComposeContext(*tags, platform = platform) { root { function.invoke(this) } }
                .writer

        play(writer, ops, testParameters)
    }
}
