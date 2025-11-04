/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.compose.ui.node

import androidx.compose.ui.FrameRateCategory
import androidx.compose.ui.graphics.asComposeCanvas
import androidx.compose.ui.platform.PlatformContext
import androidx.compose.ui.scene.ComposeSceneInputHandler
import androidx.compose.ui.scene.PointerEventResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail
import kotlinx.coroutines.test.runTest
import org.jetbrains.skia.Surface

class VoteFrameRateTest {

    @Test
    fun testNoVotedFrameRate() = runTest {
        val surface = Surface.makeRasterN32Premul(100, 100)
        val owner = RootNodeOwner(
            platformContext = object : PlatformContext.Empty() {
                override fun voteFrameRate(frameRate: Float, frameRateCategory: Float) {
                    fail("voteFrameRate should not be called")
                }
            }
        )

        owner.draw(surface.canvas.asComposeCanvas())
    }

    @Test
    fun testKeepVotedFrameRate() = runTest {
        var votedFrameRate = Float.NaN
        var votedFrameRateCategory = 0f
        var voteFrameRateCount = 0

        val surface = Surface.makeRasterN32Premul(100, 100)
        val owner = RootNodeOwner(
            platformContext = object : PlatformContext.Empty() {
                override fun voteFrameRate(frameRate: Float, frameRateCategory: Float) {
                    voteFrameRateCount++
                    votedFrameRate = frameRate
                    votedFrameRateCategory = frameRateCategory
                }
            }
        )

        owner.owner.voteFrameRate(120f)
        owner.draw(surface.canvas.asComposeCanvas())

        assertEquals(120f, votedFrameRate)
        assertEquals(0f, votedFrameRateCategory)
        assertEquals(1, voteFrameRateCount)

        owner.draw(surface.canvas.asComposeCanvas())

        assertEquals(120f, votedFrameRate)
        assertEquals(0f, votedFrameRateCategory)
        assertEquals(1, voteFrameRateCount)
    }

    @Test
    fun testKeepVotedFrameRateCategory() = runTest {
        var votedFrameRate = Float.NaN
        var votedFrameRateCategory = 0f
        var voteFrameRateCount = 0

        val surface = Surface.makeRasterN32Premul(100, 100)
        val owner = RootNodeOwner(
            platformContext = object : PlatformContext.Empty() {
                override fun voteFrameRate(frameRate: Float, frameRateCategory: Float) {
                    voteFrameRateCount++
                    votedFrameRate = frameRate
                    votedFrameRateCategory = frameRateCategory
                }
            }
        )

        owner.owner.voteFrameRate(FrameRateCategory.Normal.value)
        owner.draw(surface.canvas.asComposeCanvas())

        assertTrue(votedFrameRate.isNaN())
        assertEquals(FrameRateCategory.Normal.value, votedFrameRateCategory)
        assertEquals(1, voteFrameRateCount)

        owner.draw(surface.canvas.asComposeCanvas())

        assertTrue(votedFrameRate.isNaN())
        assertEquals(FrameRateCategory.Normal.value, votedFrameRateCategory)
        assertEquals(1, voteFrameRateCount)
    }

    @Test
    fun testVoteFrameRateChangeToHigher() = runTest {
        var votedFrameRate = Float.NaN
        var votedFrameRateCategory = 0f

        val surface = Surface.makeRasterN32Premul(100, 100)
        val owner = RootNodeOwner(
            platformContext = object : PlatformContext.Empty() {
                override fun voteFrameRate(frameRate: Float, frameRateCategory: Float) {
                    votedFrameRate = frameRate
                    votedFrameRateCategory = frameRateCategory
                }
            }
        )

        owner.owner.voteFrameRate(60f)
        owner.owner.voteFrameRate(120f)
        owner.draw(surface.canvas.asComposeCanvas())

        assertEquals(120f, votedFrameRate)
        assertEquals(0f, votedFrameRateCategory)
    }

    @Test
    fun testVoteFrameRateChangeToLower() = runTest {
        var votedFrameRate = Float.NaN
        var votedFrameRateCategory = 0f

        val surface = Surface.makeRasterN32Premul(100, 100)
        val owner = RootNodeOwner(
            platformContext = object : PlatformContext.Empty() {
                override fun voteFrameRate(frameRate: Float, frameRateCategory: Float) {
                    votedFrameRate = frameRate
                    votedFrameRateCategory = frameRateCategory
                }
            }
        )

        owner.owner.voteFrameRate(120f)
        owner.owner.voteFrameRate(60f)
        owner.draw(surface.canvas.asComposeCanvas())

        assertEquals(120f, votedFrameRate)
        assertEquals(0f, votedFrameRateCategory)
    }

    @Test
    fun testVoteFrameRateAndCategory() = runTest {
        var votedFrameRate = Float.NaN
        var votedFrameRateCategory = 0f

        val surface = Surface.makeRasterN32Premul(100, 100)
        val owner = RootNodeOwner(
            platformContext = object : PlatformContext.Empty() {
                override fun voteFrameRate(frameRate: Float, frameRateCategory: Float) {
                    votedFrameRate = frameRate
                    votedFrameRateCategory = frameRateCategory
                }
            }
        )

        owner.owner.voteFrameRate(5f)
        owner.owner.voteFrameRate(FrameRateCategory.High.value)
        owner.draw(surface.canvas.asComposeCanvas())

        assertEquals(5f, votedFrameRate)
        assertEquals(FrameRateCategory.High.value, votedFrameRateCategory)
    }

    @Test
    fun testVoteFrameRateCategoryAndThenHigher() = runTest {
        var votedFrameRate = Float.NaN
        var votedFrameRateCategory = 0f

        val surface = Surface.makeRasterN32Premul(100, 100)
        val owner = RootNodeOwner(
            platformContext = object : PlatformContext.Empty() {
                override fun voteFrameRate(frameRate: Float, frameRateCategory: Float) {
                    votedFrameRate = frameRate
                    votedFrameRateCategory = frameRateCategory
                }
            }
        )

        owner.owner.voteFrameRate(FrameRateCategory.Normal.value)
        owner.owner.voteFrameRate(FrameRateCategory.High.value)
        owner.draw(surface.canvas.asComposeCanvas())

        assertTrue(votedFrameRate.isNaN())
        assertEquals(FrameRateCategory.High.value, votedFrameRateCategory)
    }

    @Test
    fun testVoteFrameRateCategoryAndThenLower() = runTest {
        var votedFrameRate = Float.NaN
        var votedFrameRateCategory = 0f

        val surface = Surface.makeRasterN32Premul(100, 100)
        val owner = RootNodeOwner(
            platformContext = object : PlatformContext.Empty() {
                override fun voteFrameRate(frameRate: Float, frameRateCategory: Float) {
                    votedFrameRate = frameRate
                    votedFrameRateCategory = frameRateCategory
                }
            }
        )

        owner.owner.voteFrameRate(FrameRateCategory.High.value)
        owner.owner.voteFrameRate(FrameRateCategory.Normal.value)
        owner.draw(surface.canvas.asComposeCanvas())

        assertTrue(votedFrameRate.isNaN())
        assertEquals(FrameRateCategory.High.value, votedFrameRateCategory)
    }

    @Test
    fun testVoteFrameRateCategoryAndThenDefault() = runTest {
        var votedFrameRate = Float.NaN
        var votedFrameRateCategory = 0f

        val surface = Surface.makeRasterN32Premul(100, 100)
        val owner = RootNodeOwner(
            platformContext = object : PlatformContext.Empty() {
                override fun voteFrameRate(frameRate: Float, frameRateCategory: Float) {
                    votedFrameRate = frameRate
                    votedFrameRateCategory = frameRateCategory
                }
            }
        )

        owner.owner.voteFrameRate(FrameRateCategory.Normal.value)
        owner.owner.voteFrameRate(FrameRateCategory.Default.value)
        owner.draw(surface.canvas.asComposeCanvas())

        assertTrue(votedFrameRate.isNaN())
        assertEquals(FrameRateCategory.Normal.value, votedFrameRateCategory)
    }

    @Test
    fun testVoteFrameRateResetOnDraw() = runTest {
        var votedFrameRate = Float.NaN
        var votedFrameRateCategory = 0f

        val surface = Surface.makeRasterN32Premul(100, 100)
        val owner = RootNodeOwner(
            platformContext = object : PlatformContext.Empty() {
                override fun voteFrameRate(frameRate: Float, frameRateCategory: Float) {
                    votedFrameRate = frameRate
                    votedFrameRateCategory = frameRateCategory
                }
            }
        )

        owner.owner.voteFrameRate(60f)
        owner.draw(surface.canvas.asComposeCanvas())

        assertEquals(60f, votedFrameRate)
        assertEquals(0f, votedFrameRateCategory)

        owner.owner.voteFrameRate(30f)
        owner.draw(surface.canvas.asComposeCanvas())

        assertEquals(30f, votedFrameRate)
        assertEquals(0f, votedFrameRateCategory)
    }

    @Test
    fun testVoteFrameRateCategoryResetOnDraw() = runTest {
        var votedFrameRate = Float.NaN
        var votedFrameRateCategory = 0f

        val surface = Surface.makeRasterN32Premul(100, 100)
        val owner = RootNodeOwner(
            platformContext = object : PlatformContext.Empty() {
                override fun voteFrameRate(frameRate: Float, frameRateCategory: Float) {
                    votedFrameRate = frameRate
                    votedFrameRateCategory = frameRateCategory
                }
            }
        )

        owner.owner.voteFrameRate(FrameRateCategory.High.value)
        owner.draw(surface.canvas.asComposeCanvas())

        assertTrue(votedFrameRate.isNaN())
        assertEquals(FrameRateCategory.High.value, votedFrameRateCategory)

        owner.owner.voteFrameRate(FrameRateCategory.Default.value)
        owner.draw(surface.canvas.asComposeCanvas())

        assertTrue(votedFrameRate.isNaN())
        assertEquals(FrameRateCategory.Default.value, votedFrameRateCategory)
    }

    @Test
    fun testVoteFrameRateZeroIgnored() = runTest {
        var votedFrameRate = Float.NaN
        var votedFrameRateCategory = 0f

        val surface = Surface.makeRasterN32Premul(100, 100)
        val owner = RootNodeOwner(
            platformContext = object : PlatformContext.Empty() {
                override fun voteFrameRate(frameRate: Float, frameRateCategory: Float) {
                    votedFrameRate = frameRate
                    votedFrameRateCategory = frameRateCategory
                }
            }
        )

        owner.owner.voteFrameRate(0f)
        owner.draw(surface.canvas.asComposeCanvas())

        assertTrue(votedFrameRate.isNaN())
        assertEquals(0f, votedFrameRateCategory)
    }

    @Test
    fun testVoteFrameRateCombined() = runTest {
        var votedFrameRate = Float.NaN
        var votedFrameRateCategory = 0f

        val surface = Surface.makeRasterN32Premul(100, 100)
        val owner = RootNodeOwner(
            platformContext = object : PlatformContext.Empty() {
                override fun voteFrameRate(frameRate: Float, frameRateCategory: Float) {
                    votedFrameRate = frameRate
                    votedFrameRateCategory = frameRateCategory
                }
            }
        )

        owner.owner.voteFrameRate(FrameRateCategory.High.value)
        owner.owner.voteFrameRate(60f)
        owner.owner.voteFrameRate(30f)
        owner.owner.voteFrameRate(FrameRateCategory.Default.value)
        owner.draw(surface.canvas.asComposeCanvas())

        assertEquals(60f, votedFrameRate)
        assertEquals(FrameRateCategory.High.value, votedFrameRateCategory)

        owner.owner.voteFrameRate(FrameRateCategory.Normal.value)
        owner.owner.voteFrameRate(5f)
        owner.draw(surface.canvas.asComposeCanvas())

        assertEquals(5f, votedFrameRate)
        assertEquals(FrameRateCategory.Normal.value, votedFrameRateCategory)
    }

    @Test
    fun testVoteFrameRateCategoryDefault() = runTest {
        var votedFrameRate = Float.NaN
        var votedFrameRateCategory = 0f

        val surface = Surface.makeRasterN32Premul(100, 100)
        val owner = RootNodeOwner(
            platformContext = object : PlatformContext.Empty() {
                override fun voteFrameRate(frameRate: Float, frameRateCategory: Float) {
                    votedFrameRate = frameRate
                    votedFrameRateCategory = frameRateCategory
                }
            }
        )

        owner.owner.voteFrameRate(FrameRateCategory.Default.value)
        owner.draw(surface.canvas.asComposeCanvas())

        assertTrue(votedFrameRate.isNaN())
        assertTrue(votedFrameRateCategory.isNaN())
    }
}

private fun RootNodeOwner(
    platformContext: PlatformContext = PlatformContext.Empty()
) = RootNodeOwner(
    density = Density(1f),
    layoutDirection = LayoutDirection.Ltr,
    size = null,
    coroutineContext = EmptyCoroutineContext,
    platformContext = platformContext,
    snapshotInvalidationTracker = SnapshotInvalidationTracker {},
    inputHandler = ComposeSceneInputHandler(
        prepareForPointerInputEvent = {},
        processPointerInputEvent = { PointerEventResult(false) },
        cancelPointerInput = {},
        processKeyEvent = { false },
    )
)