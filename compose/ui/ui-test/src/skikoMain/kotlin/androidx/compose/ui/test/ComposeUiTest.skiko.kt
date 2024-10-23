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
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.draganddrop.DragAndDropTransferData
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asComposeCanvas
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.node.RootForTest
import androidx.compose.ui.platform.InfiniteAnimationPolicy
import androidx.compose.ui.platform.PlatformContext
import androidx.compose.ui.platform.PlatformDragAndDropManager
import androidx.compose.ui.platform.PlatformDragAndDropSource
import androidx.compose.ui.platform.WindowInfo
import androidx.compose.ui.scene.ComposeScene
import androidx.compose.ui.scene.CanvasLayersComposeScene
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.test.platform.makeSynchronizedObject
import androidx.compose.ui.test.platform.synchronized
import androidx.compose.ui.text.input.EditCommand
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.ImeOptions
import androidx.compose.ui.text.input.PlatformTextInputService
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import org.jetbrains.skia.IRect
import org.jetbrains.skia.Surface
import org.jetbrains.skiko.currentNanoTime

@ExperimentalTestApi
actual fun runComposeUiTest(effectContext: CoroutineContext, block: ComposeUiTest.() -> Unit) {
    SkikoComposeUiTest(effectContext = effectContext).runTest(block)
}

@ExperimentalTestApi
fun runSkikoComposeUiTest(
    size: Size = Size(1024.0f, 768.0f),
    density: Density = Density(1f),
    // TODO(https://github.com/JetBrains/compose-multiplatform/issues/2960) Support effectContext
    effectContext: CoroutineContext = EmptyCoroutineContext,
    block: SkikoComposeUiTest.() -> Unit
) {
    SkikoComposeUiTest(
        width = size.width.roundToInt(),
        height = size.height.roundToInt(),
        effectContext = effectContext,
        density = density
    ).runTest(block)
}

@InternalTestApi
@OptIn(InternalComposeUiApi::class, ExperimentalTestApi::class)
fun runInternalSkikoComposeUiTest(
    width: Int = 1024,
    height: Int = 768,
    density: Density = Density(1f),
    effectContext: CoroutineContext = EmptyCoroutineContext,
    semanticsOwnerListener: PlatformContext.SemanticsOwnerListener? = null,
    coroutineDispatcher: TestDispatcher = defaultTestDispatcher(),
    block: SkikoComposeUiTest.() -> Unit
) {
    SkikoComposeUiTest(
        width = width,
        height = height,
        effectContext = effectContext,
        density = density,
        semanticsOwnerListener = semanticsOwnerListener,
        coroutineDispatcher = coroutineDispatcher,
    ).runTest(block)
}

/**
 * How often to check idling resources.
 * Empirically checked that Android (espresso, really) tests approximately at this rate.
 */
private const val IDLING_RESOURCES_CHECK_INTERVAL_MS = 20L

/**
 * Returns the default [TestDispatcher] to use in tests.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@InternalTestApi
fun defaultTestDispatcher() = UnconfinedTestDispatcher()

/**
 * @param effectContext The [CoroutineContext] used to run the composition. The context for
 * `LaunchedEffect`s and `rememberCoroutineScope` will be derived from this context.
 */
@ExperimentalTestApi
@OptIn(InternalTestApi::class, InternalComposeUiApi::class)
class SkikoComposeUiTest @InternalTestApi constructor(
    width: Int = 1024,
    height: Int = 768,
    // TODO(https://github.com/JetBrains/compose-multiplatform/issues/2960) Support effectContext
    effectContext: CoroutineContext = EmptyCoroutineContext,
    override val density: Density = Density(1f),
    private val semanticsOwnerListener: PlatformContext.SemanticsOwnerListener?,
    coroutineDispatcher: TestDispatcher = defaultTestDispatcher(),
) : ComposeUiTest {
    init {
        require(effectContext == EmptyCoroutineContext) {
            "The argument effectContext isn't supported yet. " +
                "Follow https://github.com/JetBrains/compose-multiplatform/issues/2960"
        }
    }

    constructor(
        width: Int = 1024,
        height: Int = 768,
        effectContext: CoroutineContext = EmptyCoroutineContext,
        density: Density = Density(1f),
    ) : this (
        width = width,
        height = height,
        effectContext = effectContext,
        density = density,
        semanticsOwnerListener = null,
    )

    private class Session(
        var imeOptions: ImeOptions,
        var onEditCommand: (List<EditCommand>) -> Unit,
        var onImeActionPerformed: (ImeAction) -> Unit,
    )

    private val composeRootRegistry = ComposeRootRegistry()

    private val testScope = TestScope(coroutineDispatcher)
    override val mainClock: MainTestClock = MainTestClockImpl(
        testScheduler = coroutineDispatcher.scheduler,
        frameDelayMillis = FRAME_DELAY_MILLIS
    )
    private val uncaughtExceptionHandler = UncaughtExceptionHandler()
    private val infiniteAnimationPolicy = object : InfiniteAnimationPolicy {
        override suspend fun <R> onInfiniteOperation(block: suspend () -> R): R {
            if (mainClock.autoAdvance) {
                throw CancellationException("Infinite animations are disabled on tests")
            }
            return block()
        }
    }
    private val coroutineContext =
        coroutineDispatcher + uncaughtExceptionHandler + infiniteAnimationPolicy
    private val surface = Surface.makeRasterN32Premul(width, height)
    private val size = IntSize(width, height)

    @InternalComposeUiApi
    lateinit var scene: ComposeScene
        @InternalTestApi
        set

    private val testOwner = SkikoTestOwner()
    private val testContext = TestContext(testOwner)

    private val idlingResources = mutableSetOf<IdlingResource>()
    private val idlingResourcesLock = makeSynchronizedObject(idlingResources)

    fun <R> runTest(block: SkikoComposeUiTest.() -> R): R {
        return composeRootRegistry.withRegistry {
            withScene {
                withRenderLoop {
                    block()
                }
            }
        }
    }

    private fun <R> withScene(block: () -> R): R {
        scene = runOnUiThread(::createUi)
        try {
            return block()
        } finally {
            // Close the scene before calling testScope.runTest so that all the coroutines are
            // cancelled when we call it.
            runOnUiThread(scene::close)
            // call runTest instead of deprecated cleanupTestCoroutines()
            testScope.runTest { }
            uncaughtExceptionHandler.throwUncaught()
        }
    }

    private inline fun <R> withRenderLoop(block: () -> R): R {
        val scope = CoroutineScope(coroutineContext)
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
        scene.render(
            surface.canvas.asComposeCanvas(),
            timeMillis * NanoSecondsPerMilliSecond
        )
    }

    private fun createUi() = CanvasLayersComposeScene(
        density = density,
        size = size,
        coroutineContext = coroutineContext,
        platformContext = TestContext(),
        invalidate = { }
    )

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
        // TODO: consider adding a timeout to avoid an infinite loop?
        // always check even if we are idle
        uncaughtExceptionHandler.throwUncaught()
        while (!isIdle()) {
            advanceIfNeededAndRenderNextFrame()
            uncaughtExceptionHandler.throwUncaught()
            if (!areAllResourcesIdle()) {
                sleep(IDLING_RESOURCES_CHECK_INTERVAL_MS)
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

    override fun registerIdlingResource(idlingResource: IdlingResource) {
        synchronized(idlingResourcesLock) {
            idlingResources.add(idlingResource)
        }
    }

    override fun unregisterIdlingResource(idlingResource: IdlingResource) {
        synchronized(idlingResourcesLock) {
            idlingResources.remove(idlingResource)
        }
    }

    private fun areAllResourcesIdle() = synchronized(idlingResourcesLock) {
        idlingResources.all { it.isIdleNow }
    }

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

    override fun enableAccessibilityChecks() {
        // TODO(CMP-6719): Implement accessibility checks
    }

    override fun disableAccessibilityChecks() {
        // TODO(CMP-6719): Implement accessibility checks
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
        val iRect = IRect.makeLTRB(rect.left.toInt(), rect.top.toInt(), rect.right.toInt(), rect.bottom.toInt())
        val image = surface.makeImageSnapshot(iRect)
        return image!!.toComposeImageBitmap()
    }

    fun SemanticsNodeInteraction.captureToImage(): ImageBitmap {
        return captureToImage(fetchSemanticsNode())
    }

    @OptIn(InternalComposeUiApi::class)
    internal inner class SkikoTestOwner : TestOwner {
        override fun <T> runOnUiThread(action: () -> T): T {
            return this@SkikoComposeUiTest.runOnUiThread(action)
        }

        override fun getRoots(atLeastOneRootExpected: Boolean): Set<RootForTest> {
            waitForIdle()
            return composeRootRegistry.getComposeRoots()
        }

        override val mainClock get() =
            this@SkikoComposeUiTest.mainClock

        fun captureToImage(semanticsNode: SemanticsNode): ImageBitmap =
            this@SkikoComposeUiTest.captureToImage(semanticsNode)
    }

    private inner class TestWindowInfo : WindowInfo {
        override val isWindowFocused: Boolean
            get() = true

        @ExperimentalComposeUiApi
        override val containerSize: IntSize
            get() = size
    }

    private inner class TestTextInputService : PlatformTextInputService {
        var session: Session? = null

        override fun startInput(
            value: TextFieldValue,
            imeOptions: ImeOptions,
            onEditCommand: (List<EditCommand>) -> Unit,
            onImeActionPerformed: (ImeAction) -> Unit
        ) {
            session = Session(
                imeOptions = imeOptions,
                onEditCommand = onEditCommand,
                onImeActionPerformed = onImeActionPerformed
            )
        }

        override fun stopInput() {
            session = null
        }

        override fun showSoftwareKeyboard() = Unit
        override fun hideSoftwareKeyboard() = Unit
        override fun updateState(oldValue: TextFieldValue?, newValue: TextFieldValue) = Unit
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

    private inner class TestContext : PlatformContext by PlatformContext.Empty {
        override val windowInfo: WindowInfo = TestWindowInfo()

        override val textInputService = TestTextInputService()

        override val rootForTestListener: PlatformContext.RootForTestListener
            get() = composeRootRegistry

        override val semanticsOwnerListener: PlatformContext.SemanticsOwnerListener?
            get() = this@SkikoComposeUiTest.semanticsOwnerListener

        override val dragAndDropManager: PlatformDragAndDropManager = TestDragAndDropManager()
    }
}

@ExperimentalTestApi
actual sealed interface ComposeUiTest : SemanticsNodeInteractionsProvider {
    actual val density: Density
    actual val mainClock: MainTestClock
    actual fun <T> runOnUiThread(action: () -> T): T
    actual fun <T> runOnIdle(action: () -> T): T
    actual fun waitForIdle()
    actual suspend fun awaitIdle()
    actual fun waitUntil(
        conditionDescription: String?,
        timeoutMillis: Long,
        condition: () -> Boolean
    )
    actual fun registerIdlingResource(idlingResource: IdlingResource)
    actual fun unregisterIdlingResource(idlingResource: IdlingResource)
    actual fun setContent(composable: @Composable () -> Unit)

    actual fun enableAccessibilityChecks()
    actual fun disableAccessibilityChecks()
}

private const val FRAME_DELAY_MILLIS = 16L
