/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.ui.test

import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.draganddrop.DragAndDropTransferData
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asComposeCanvas
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.node.RootForTest
import androidx.compose.ui.platform.DefaultArchitectureComponentsOwner
import androidx.compose.ui.platform.InfiniteAnimationPolicy
import androidx.compose.ui.platform.PlatformContext
import androidx.compose.ui.platform.PlatformDragAndDropManager
import androidx.compose.ui.platform.PlatformDragAndDropSource
import androidx.compose.ui.platform.PlatformTextInputMethodRequest
import androidx.compose.ui.platform.PlatformWindowInsets
import androidx.compose.ui.platform.WindowInfo
import androidx.compose.ui.scene.CanvasLayersComposeScene
import androidx.compose.ui.scene.ComposeScene
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.toSize
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.enableSavedStateHandles
import kotlin.coroutines.ContinuationInterceptor
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.roundToInt
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestResult
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import org.jetbrains.skia.Color
import org.jetbrains.skia.Surface
import org.jetbrains.skiko.currentNanoTime

@Deprecated(
    message =
        "Use `androidx.compose.ui.test.v2.runComposeUiTest` instead. The v2 APIs use " +
            "`StandardTestDispatcher` by default to better simulate production behavior where " +
            "coroutines are queued rather than executed immediately.",
    level = DeprecationLevel.WARNING,
)
@ExperimentalTestApi
actual fun runComposeUiTest(
    effectContext: CoroutineContext,
    runTestContext: CoroutineContext,
    testTimeout: Duration,
    block: suspend ComposeUiTest.() -> Unit
): TestResult {
    return runSkikoComposeUiTest(
        effectContext = effectContext,
        runTestContext = runTestContext,
        testTimeout = testTimeout,
    ) {
        block()
    }
}

@OptIn(InternalComposeUiApi::class, InternalTestApi::class)
@Deprecated(
    message =
        "Use `androidx.compose.ui.test.v2.runSkikoComposeUiTest` instead. The v2 APIs use " +
            "`StandardTestDispatcher` by default to better simulate production behavior where " +
            "coroutines are queued rather than executed immediately.",
    level = DeprecationLevel.WARNING,
)
@ExperimentalTestApi
fun runSkikoComposeUiTest(
    size: Size = Size(1024.0f, 768.0f),
    density: Density = Density(1f),
    effectContext: CoroutineContext = EmptyCoroutineContext,
    runTestContext: CoroutineContext = EmptyCoroutineContext,
    testTimeout: Duration = Duration.INFINITE,
    block: suspend SkikoComposeUiTest.() -> Unit
): TestResult {
    return SkikoComposeUiTest(
        width = size.width.roundToInt(),
        height = size.height.roundToInt(),
        effectContext = effectContext,
        testTimeout = testTimeout,
        runTestContext = runTestContext,
        density = density,
        semanticsOwnerListener = null,
        windowInsets = null,
        useStandardTestDispatcherForComposition = false,
    ).runTest(block)
}

/**
 * How often to check idling resources.
 * Empirically checked that Android (espresso, really) tests approximately at this rate.
 */
private const val IDLING_RESOURCES_CHECK_INTERVAL_MS = 20L

/**
 * @param effectContext The [CoroutineContext] used to run the composition. The context for
 * `LaunchedEffect`s and `rememberCoroutineScope` will be derived from this context.
 */
@ExperimentalTestApi
@OptIn(InternalTestApi::class, InternalComposeUiApi::class)
open class SkikoComposeUiTest @InternalTestApi constructor(
    width: Int = 1024,
    height: Int = 768,
    private val effectContext: CoroutineContext = EmptyCoroutineContext,
    private val runTestContext: CoroutineContext = EmptyCoroutineContext,
    private val testTimeout: Duration = Duration.INFINITE,
    override val density: Density = Density(1f),
    private val semanticsOwnerListener: PlatformContext.SemanticsOwnerListener?,
    private val windowInsets: PlatformWindowInsets?,
    private val useStandardTestDispatcherForComposition: Boolean,
) : ComposeUiTest {
    constructor(
        width: Int = 1024,
        height: Int = 768,
        effectContext: CoroutineContext = EmptyCoroutineContext,
        density: Density = Density(1f),
    ) : this(
        width = width,
        height = height,
        effectContext = effectContext,
        density = density,
        semanticsOwnerListener = null,
        windowInsets = null,
        useStandardTestDispatcherForComposition = true,
    )

    constructor(
        width: Int = 1024,
        height: Int = 768,
        effectContext: CoroutineContext = EmptyCoroutineContext,
        runTestContext: CoroutineContext = EmptyCoroutineContext,
        testTimeout: Duration = Duration.INFINITE,
        density: Density = Density(1f)
    ) : this(
        width = width,
        height = height,
        effectContext = effectContext,
        runTestContext = runTestContext,
        testTimeout = testTimeout,
        density = density,
        semanticsOwnerListener = null,
        windowInsets = null,
        useStandardTestDispatcherForComposition = true,
    )

    private val composeRootRegistry = ComposeRootRegistry()

    private val customTestDispatcher: TestDispatcher? =
        effectContext[ContinuationInterceptor] as? TestDispatcher

    /**
     * We can only accept a TestDispatcher here because we need to access its scheduler. Use the
     * TestDispatcher if it is provided in the effectContext Otherwise, use the
     * TestCoroutineScheduler if it is provided
     */
    private val compositionCoroutineDispatcher: TestDispatcher =
        customTestDispatcher
            ?: effectContext.createDefaultTestDispatcher(useStandardTestDispatcherForComposition)

    private val mainClockImpl = MainTestClockImpl(
        scheduler = compositionCoroutineDispatcher.scheduler,
        frameDelayMillis = FRAME_DELAY_MILLIS,
        isStandardTestDispatcherSupportEnabled = useStandardTestDispatcherForComposition
    )
    override val mainClock: MainTestClock
        get() = mainClockImpl

    private val uncaughtExceptionHandler = UncaughtExceptionHandler(
        effectContext[CoroutineExceptionHandler]
    )
    private val infiniteAnimationPolicy = object : InfiniteAnimationPolicy {
        override suspend fun <R> onInfiniteOperation(block: suspend () -> R): R {
            if (mainClock.autoAdvance) {
                throw CancellationException("Infinite animations are disabled on tests")
            }
            return block()
        }
    }

    private val recomposerCoroutineScope = CoroutineScope(
        effectContext +
            compositionCoroutineDispatcher +
            infiniteAnimationPolicy +
            uncaughtExceptionHandler +
            Job()
    )

    private val surface = Surface.makeRasterN32Premul(width, height)
    private val size = IntSize(width, height)

    @InternalComposeUiApi
    lateinit var scene: ComposeScene
        @InternalTestApi
        set

    private val architectureComponentsOwner =
        DefaultArchitectureComponentsOwner(enforceMainThread = false)
    private val testOwner = SkikoTestOwner()
    private val testContext = TestContext(testOwner)

    fun runTest(
        block: suspend SkikoComposeUiTest.() -> Unit
    ): TestResult {
        @OptIn(ExperimentalStdlibApi::class)
        // We don't restrict the type to be TestDispatcher, because it's not a requirement from
        // ComposeUiTest perspective.
        // Below we call `kotlinx.coroutines.test.runTest`, and it will throw an exception
        // if the CoroutineDispatcher can't be used for tests (it expects a TestDispatcher).
        val testDispatcher: CoroutineDispatcher =
            runTestContext[CoroutineDispatcher] ?: StandardTestDispatcher()

        val combinedRunTestCoroutineContext =
            recomposerCoroutineScope.coroutineContext
                .minusKey(CoroutineExceptionHandler.Key)
                .minusKey(Job.Key)
                .minusKey(TestCoroutineScheduler.Key)
                .plus(runTestContext)
                .plus(testDispatcher)

        // Note: on web this call returns immediately (it returns a Promise),
        return runTest(
            timeout = testTimeout,
            context = combinedRunTestCoroutineContext
        ) {
            composeRootRegistry.withRegistry {
                withScene {
                    withRenderLoop {
                        // MonotonicFrameClock is necessary. See the CL for details:
                        // https://android-review.googlesource.com/c/platform/frameworks/support/+/3284298
                        // > Anything that might result in animation may require the MonotonicFrameClock,
                        // > and to get the timing right it should be the clock provided by the Recomposer's effect context
                        // It's covered by SkikoComposeUiTestTest.canDriveAnimationsFromTest.
                        scene.withMonotonicFrameClock {
                            block()
                        }
                    }
                }
            }
        }
    }

    private inline fun <R> withScene(block: () -> R): R {
        runOnUiThread(::createScene)
        try {
            return block()
        } finally {
            runOnUiThread(::closeScene)
            // After the scene is closed, run all left foreground TestDispatchEvent.
            // They might've been added outside the runTest call, using the provided coroutineDispatcher:
            compositionCoroutineDispatcher.scheduler.advanceUntilIdle()
            uncaughtExceptionHandler.throwUncaught()
        }
    }

    private inline fun <R> withRenderLoop(block: () -> R): R {
        val scope = recomposerCoroutineScope
        return try {
            scope.launch {
                while (isActive) {
                    delay(FRAME_DELAY_MILLIS)
                    runOnUiThread {
                        render(mainClock.currentTime)
                    }
                }
            }
            block()
        } finally {
            scope.cancel("Run completed")
        }
    }

    /**
     * Render the scene at the given time.
     */
    private fun render(timeMillis: Long) {
        surface.canvas.clear(Color.TRANSPARENT)
        scene.render(
            surface.canvas.asComposeCanvas(),
            timeMillis * NanoSecondsPerMilliSecond
        )
    }

    private fun createScene() {
        scene = CanvasLayersComposeScene(
            density = density,
            size = size,
            coroutineContext = recomposerCoroutineScope.coroutineContext,
            platformContext = TestContext(),
            invalidate = { }
        )
        architectureComponentsOwner.enableSavedStateHandles()
        architectureComponentsOwner.lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    private fun closeScene() {
        architectureComponentsOwner.lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        scene.close()
    }

    private fun advanceIfNeededAndRenderNextFrame() {
        if (mainClock.autoAdvance) {
            mainClock.advanceTimeByFrame()
            // The rendering is done by withRenderLoop
        } else {
            runOnUiThread {
                render(mainClock.currentTime)
            }
        }
    }

    @OptIn(InternalComposeUiApi::class)
    private fun isIdle(): Boolean {
        if (composeRootRegistry.getComposeRoots().any { it.hasPendingMeasureOrLayout }) {
            return false
        }

        if (!mainClock.autoAdvance) {
            return true
        }

        return !Snapshot.current.hasPendingChanges()
            && !Snapshot.isApplyObserverNotificationPending
            && !scene.hasInvalidations()
            && areAllResourcesIdle()
    }

    override fun waitForIdle() {
        val startedAt = currentNanoTime().toDuration(DurationUnit.NANOSECONDS)
        var lastReportedElapsedSeconds = 0L
        // always check even if we are idle
        uncaughtExceptionHandler.throwUncaught()
        while (!isIdle()) {
            advanceIfNeededAndRenderNextFrame()
            uncaughtExceptionHandler.throwUncaught()
            if (!areAllResourcesIdle()) {
                sleep(IDLING_RESOURCES_CHECK_INTERVAL_MS)
            }
            val currentTime = currentNanoTime().toDuration(DurationUnit.NANOSECONDS)
            val elapsedSeconds = (currentTime - startedAt).inWholeSeconds
            if (elapsedSeconds > lastReportedElapsedSeconds) {
                println("Suspicious! waitForIdle has not finished after $elapsedSeconds seconds.")
                lastReportedElapsedSeconds = elapsedSeconds
            }
        }
    }

    override suspend fun awaitIdle() {
        // always check even if we are idle
        uncaughtExceptionHandler.throwUncaught()
        while (!isIdle()) {
            advanceIfNeededAndRenderNextFrame()
            uncaughtExceptionHandler.throwUncaught()
            if (!areAllResourcesIdle()) {
                delay(IDLING_RESOURCES_CHECK_INTERVAL_MS)
            }
            yield()
        }
    }

    override fun <T> runOnUiThread(action: () -> T): T {
        return androidx.compose.ui.test.runOnUiThread(action)
    }

    override fun <T> runOnIdle(action: () -> T): T {
        waitForIdle()
        return runOnUiThread(action)
    }

    override fun <T> runWithoutImplicitWait(block: () -> T): T {
        // TODO https://youtrack.jetbrains.com/issue/CMP-10244/ui-test.-Implement-runWithoutImplicitWait
        throw NotImplementedError("runWithoutImplicitWait is not implemented.")
    }

    override fun waitUntil(
        conditionDescription: String?,
        timeoutMillis: Long,
        condition: () -> Boolean
    ) {
        val startTime = currentNanoTime()
        val timeoutNanos = timeoutMillis * NanoSecondsPerMilliSecond
        while (!condition()) {
            advanceIfNeededAndRenderNextFrame()
            if (currentNanoTime() - startTime > timeoutNanos) {
                throw ComposeTimeoutException(
                    buildWaitUntilTimeoutMessage(timeoutMillis, conditionDescription)
                )
            }
        }

        // TODO Add a wait function variant without conditions (timeout exceptions)
    }

    // Only DesktopComposeUiTest supports IdlingResource registration,
    // so by default SkikoComposeUiTest doesn't expect any IdlingResource
    protected open fun areAllResourcesIdle() = true

    override fun setContent(composable: @Composable () -> Unit) {
        if (isOnUiThread()) {
            scene.setContent(content = composable)
        } else {
            runOnUiThread {
                scene.setContent(content = composable)
            }

            // Only wait for idleness if not on the UI thread. If we are on the UI thread, the
            // caller clearly wants to keep tight control over execution order, so don't go
            // executing future tasks on the main thread.
            waitForIdle()
        }
    }

    override fun hasPendingWork(): Boolean {
        return !isIdle()
    }

    override fun onNode(
        matcher: SemanticsMatcher,
        useUnmergedTree: Boolean
    ): SemanticsNodeInteraction {
        return SemanticsNodeInteraction(testContext, useUnmergedTree, matcher)
    }

    override fun onAllNodes(
        matcher: SemanticsMatcher,
        useUnmergedTree: Boolean
    ): SemanticsNodeInteractionCollection {
        return SemanticsNodeInteractionCollection(testContext, useUnmergedTree, matcher)
    }

    fun captureToImage(): ImageBitmap {
        waitForIdle()
        return surface.makeImageSnapshot().toComposeImageBitmap()
    }

    fun captureToImage(semanticsNode: SemanticsNode): ImageBitmap {
        val rect = semanticsNode.boundsInWindow
        val image = surface.makeImageSnapshot(
            rect.left.toInt(),
            rect.top.toInt(),
            rect.right.toInt(),
            rect.bottom.toInt()
        )
        return image!!.toComposeImageBitmap()
    }

    fun SemanticsNodeInteraction.captureToImage(): ImageBitmap {
        return captureToImage(fetchSemanticsNode())
    }

    @OptIn(InternalComposeUiApi::class)
    internal inner class SkikoTestOwner : TestOwner {
        override var isImplicitWaitSuppressed: Boolean = false

        override val mainClock
            get() = mainClockImpl

        override fun <T> runOnUiThread(action: () -> T): T {
            return this@SkikoComposeUiTest.runOnUiThread(action)
        }

        override fun getRoots(atLeastOneRootExpected: Boolean): Set<RootForTest> {
            if (!isImplicitWaitSuppressed) {
                waitForIdle()
            }
            return composeRootRegistry.getComposeRoots()
        }

        override fun runCurrent() {
            mainClockImpl.runCurrent()
        }

        fun captureToImage(semanticsNode: SemanticsNode): ImageBitmap =
            this@SkikoComposeUiTest.captureToImage(semanticsNode)
    }

    private inner class TestWindowInfo : WindowInfo {
        override val isWindowFocused: Boolean
            get() = true

        override val containerSize: IntSize
            get() = size

        override val containerDpSize: DpSize
            get() = with(density) { size.toSize().toDpSize() }
    }

    private inner class TestDragAndDropManager : PlatformDragAndDropManager {
        override val isRequestDragAndDropTransferRequired: Boolean
            get() = true

        override fun requestDragAndDropTransfer(
            source: PlatformDragAndDropSource,
            offset: Offset
        ) {
            var isTransferStarted = false
            val startTransferScope = object : PlatformDragAndDropSource.StartTransferScope {
                override fun startDragAndDropTransfer(
                    transferData: DragAndDropTransferData,
                    decorationSize: Size,
                    drawDragDecoration: DrawScope.() -> Unit
                ): Boolean {
                    isTransferStarted = true
                    return true
                }
            }
            with(source) {
                startTransferScope.startDragAndDropTransfer(offset) {
                    isTransferStarted
                }
            }
        }
    }

    private inner class TestContext : PlatformContext by PlatformContext.Empty() {
        override val windowInfo: WindowInfo = TestWindowInfo()
        override val architectureComponentsOwner get() = this@SkikoComposeUiTest.architectureComponentsOwner
        override val rootForTestListener: PlatformContext.RootForTestListener
            get() = composeRootRegistry

        override val semanticsOwnerListener: PlatformContext.SemanticsOwnerListener?
            get() = this@SkikoComposeUiTest.semanticsOwnerListener
        override val windowInsets: PlatformWindowInsets
            get() = this@SkikoComposeUiTest.windowInsets ?: super.windowInsets

        override val dragAndDropManager: PlatformDragAndDropManager = TestDragAndDropManager()

        override suspend fun startInputMethod(request: PlatformTextInputMethodRequest): Nothing {
            awaitCancellation()
        }
    }
}

private const val FRAME_DELAY_MILLIS = 16L

@OptIn(ExperimentalCoroutinesApi::class)
private fun CoroutineContext.createDefaultTestDispatcher(
    useStandardTestDispatcher: Boolean
): TestDispatcher {
    if (useStandardTestDispatcher) {
        return StandardTestDispatcher(this[TestCoroutineScheduler])
    }
    return UnconfinedTestDispatcher(this[TestCoroutineScheduler])
}
