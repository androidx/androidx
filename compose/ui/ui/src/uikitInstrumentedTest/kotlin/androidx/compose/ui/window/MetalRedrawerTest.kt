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

package androidx.compose.ui.window

import androidx.compose.ui.viewinterop.UIKitInteropMutableTransaction
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCAction
import platform.CoreGraphics.CGSizeMake
import platform.Foundation.NSDate
import platform.Foundation.NSRunLoop
import platform.Foundation.NSRunLoopCommonModes
import platform.Foundation.runUntilDate
import platform.Foundation.timeIntervalSinceDate
import platform.QuartzCore.CADisplayLink
import platform.QuartzCore.CAMetalLayer
import platform.darwin.NSObject
import platform.darwin.sel_registerName

class MetalRedrawerTest {
    @Test
    fun testShouldNotRenderWhenInitialized() {
        var rendersCount = 0
        val metalRedrawer = makeMetalRedrawer(onRender = { rendersCount++ })
        val displayLinkListener = TestDisplayLinkListener()

        metalRedrawer.isActive = true
        displayLinkListener.await(frames = 10)

        assertEquals(0, rendersCount)
    }

    @Test
    fun testShouldRenderWhenMakeNeedsRedraw() {
        var rendersCount = 0
        val metalRedrawer = makeMetalRedrawer(onRender = { rendersCount++ })
        val displayLinkListener = TestDisplayLinkListener()

        metalRedrawer.isActive = true
        metalRedrawer.setNeedsRedraw()

        displayLinkListener.await(frames = 10)
        assertEquals(2, rendersCount)
    }

    @Test
    fun testShouldNotRenderWhenNotActive() {
        var rendersCount = 0
        val metalRedrawer = makeMetalRedrawer(onRender = { rendersCount++ })
        val displayLinkListener = TestDisplayLinkListener()

        metalRedrawer.isActive = false
        metalRedrawer.setNeedsRedraw()

        displayLinkListener.await(frames = 10)
        assertEquals(0, rendersCount)
    }

    @Test
    fun testShouldRenderWhenBecomesActive() {
        var rendersCount = 0
        val metalRedrawer = makeMetalRedrawer(onRender = { rendersCount++ })
        val displayLinkListener = TestDisplayLinkListener()

        metalRedrawer.isActive = false
        displayLinkListener.await(frames = 2)

        assertEquals(0, rendersCount)

        metalRedrawer.isActive = true
        displayLinkListener.await(frames = 10)

        assertEquals(2, rendersCount)
    }

    @Test
    fun testOngoingInteractionEventsCountShouldNotStartRendering() {
        var rendersCount = 0
        val metalRedrawer = makeMetalRedrawer(onRender = { rendersCount++ })
        val displayLinkListener = TestDisplayLinkListener()

        metalRedrawer.ongoingInteractionEventsCount = 5
        displayLinkListener.await(frames = 10)
        assertEquals(0, rendersCount)
    }

    @Test
    fun testOngoingInteractionEventsCountShouldRenderWhenNeedsRedraw() {
        var rendersCount = 0
        val metalRedrawer = makeMetalRedrawer(onRender = { rendersCount++ })
        val displayLinkListener = TestDisplayLinkListener()

        metalRedrawer.isActive = true
        repeat(10) {
            metalRedrawer.setNeedsRedraw()
            displayLinkListener.await(frames = 1)
        }

        assertTrue(rendersCount > 5, "Should be significant number of frames rendered, but got $rendersCount")
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun makeMetalRedrawer(onRender: () -> Unit): MetalRedrawer {
        val transaction = UIKitInteropMutableTransaction(isInteropActive = false)
        val metalLayer = CAMetalLayer()
        metalLayer.setDrawableSize(CGSizeMake(100.0, 100.0))
        val metalRedrawer = MetalRedrawer(
            metalLayer = metalLayer,
            retrieveInteropTransaction = { transaction },
            useSeparateRenderThreadWhenPossible = false,
            render = { _, _ -> onRender() }
        )
        return metalRedrawer
    }
}

internal class TestDisplayLinkListener {
    private var displayLink: CADisplayLink? = null

    var framesCountSinceStart = 0
    @OptIn(ExperimentalForeignApi::class)
    fun start() {
        displayLink = CADisplayLink.displayLinkWithTarget(
            target = object : NSObject() {
                @OptIn(BetaInteropApi::class)
                @Suppress("unused")
                @ObjCAction
                fun onTick() {
                    framesCountSinceStart++
                }
            },
            selector = sel_registerName("onTick")
        )
        displayLink?.addToRunLoop(NSRunLoop.mainRunLoop, NSRunLoopCommonModes)
    }

    fun await(frames: Int = 1) {
        invalidate()
        framesCountSinceStart = 0
        start()
        val startDate = NSDate()
        while (framesCountSinceStart < frames) {
            if (NSDate().timeIntervalSinceDate(startDate) > 5.0) {
                invalidate()
                error("Timeout reached")
            }
            NSRunLoop.mainRunLoop.runUntilDate(NSDate(0.001))
        }
        invalidate()
    }

    fun invalidate() {
        displayLink?.invalidate()
        displayLink = null
    }
}
