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

package androidx.camera.viewfinder.compose

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.os.Build
import android.util.Log
import android.util.Size
import android.view.Surface
import androidx.annotation.RequiresApi
import androidx.camera.testing.impl.AndroidUtil
import androidx.camera.viewfinder.core.FrameRenderedListener
import androidx.camera.viewfinder.core.ImplementationMode
import androidx.camera.viewfinder.core.TransformationInfo
import androidx.camera.viewfinder.core.TransformationMode
import androidx.camera.viewfinder.core.ViewfinderSurfaceRequest
import androidx.camera.viewfinder.core.ViewfinderSurfaceSessionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.AndroidUiDispatcher
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import com.google.common.truth.TruthJUnit.assume
import java.util.concurrent.Executor
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.abs
import kotlin.math.cos
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.yield
import org.junit.After
import org.junit.Assume.assumeFalse
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@MediumTest
@RunWith(Parameterized::class)
class ViewfinderTest(private val implementationMode: ImplementationMode) {

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "implementationMode = {0}")
        fun data(): Array<ImplementationMode> =
            arrayOf(ImplementationMode.EXTERNAL, ImplementationMode.EMBEDDED)
    }

    @get:Rule val rule = createComposeRule()

    @After
    fun tearDown() {
        // Force GC to trigger finalizers and catch leaks/crashes early
        Runtime.getRuntime().gc()
        System.runFinalization()
    }

    @Test
    fun coordinatesTransformationSameSizeNoRotation(): Unit = runBlocking {
        val coordinateTransformer = MutableCoordinateTransformer()
        val surfaceSessionInitialized = CompletableDeferred<Unit>()

        rule.setContent {
            with(LocalDensity.current) {
                TestViewfinder(
                    modifier = Modifier.requiredSize(540.toDp(), 960.toDp()),
                    coordinateTransformer = coordinateTransformer,
                ) {
                    onSurfaceSession { surfaceSessionInitialized.complete(Unit) }
                }
            }
        }

        withTimeout(5.seconds) { surfaceSessionInitialized.await() }
        rule.awaitIdle()

        val expectedMatrix =
            Matrix(
                values =
                    floatArrayOf(1f, 0f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 1f)
            )

        assertThat(coordinateTransformer.transformMatrix.values).isEqualTo(expectedMatrix.values)
    }

    @Test
    fun coordinatesTransformationSameSizeWithHalfCrop(): Unit = runBlocking {
        // Viewfinder size: 1080x1920
        // Surface size: 1080x1920
        // Crop rect size: 540x960

        val coordinateTransformer = MutableCoordinateTransformer()
        val matrixValuesInOnSurfaceSession = CompletableDeferred<FloatArray>()

        rule.setContent {
            with(LocalDensity.current) {
                TestViewfinder(
                    modifier = Modifier.requiredSize(540.toDp(), 960.toDp()),
                    transformationInfo =
                        TransformationInfo(
                            sourceRotation = 0,
                            isSourceMirroredHorizontally = false,
                            isSourceMirroredVertically = false,
                            cropRectLeft = 0f,
                            cropRectTop = 0f,
                            cropRectRight = 270f,
                            cropRectBottom = 480f,
                        ),
                    coordinateTransformer = coordinateTransformer,
                ) {
                    onSurfaceSession {
                        matrixValuesInOnSurfaceSession.complete(
                            coordinateTransformer.transformMatrix.values.copyOf()
                        )
                    }
                }
            }
        }

        val sessionMatrixValues = withTimeout(5.seconds) { matrixValuesInOnSurfaceSession.await() }
        rule.awaitIdle()

        val expectedMatrix =
            Matrix(
                values =
                    floatArrayOf(0.5f, 0f, 0f, 0f, 0f, 0.5f, 0f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 1f)
            )

        assertThat(coordinateTransformer.transformMatrix.values).isEqualTo(expectedMatrix.values)
        assertThat(sessionMatrixValues).isEqualTo(expectedMatrix.values)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun preAppliedTransformation_rendersUprightAcrossDynamicRotations(): Unit = runBlocking {
        assumeFalse(
            "Skipping test due to SurfaceTexture crash on Emulator API 36",
            AndroidUtil.isEmulator(36),
        )
        assume()
            .withMessage(
                "SurfaceControl is required to rotate PRE_APPLIED content in EXTERNAL mode."
            )
            .that(
                implementationMode == ImplementationMode.EMBEDDED ||
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
            )
            .isTrue()
        val surfaceWidth = 200
        val surfaceHeight = 400
        val viewfinderSizePx = 200
        val coordinateTransformer = MutableCoordinateTransformer()
        val transformationInfoState =
            mutableStateOf(
                TransformationInfo(
                    sourceRotation = 0,
                    transformationMode = TransformationMode.PRE_APPLIED,
                )
            )

        testWithSessionInternal(
            scopeProvider = { it },
            withRenderAnimation = false,
            composeContent = { onInit ->
                val transformationInfo by remember { transformationInfoState }
                with(LocalDensity.current) {
                    TestViewfinder(
                        modifier =
                            Modifier.size(viewfinderSizePx.toDp(), viewfinderSizePx.toDp())
                                .testTag(VIEWFINDER_TEST_TAG),
                        surfaceRequest =
                            remember {
                                ViewfinderSurfaceRequest(
                                    width = surfaceWidth,
                                    height = surfaceHeight,
                                    implementationMode = implementationMode,
                                )
                            },
                        transformationInfo = transformationInfo,
                        coordinateTransformer = coordinateTransformer,
                        onInit = onInit,
                    )
                }
            },
        ) {
            val surface = awaitSurfaceSession().surface

            // Exercise initial 0°, 90° steps, and a 180° flip (270° -> 90°).
            for (rotation in listOf(0, 90, 180, 270, 90, 0)) {
                withContext(AndroidUiDispatcher.Main) {
                    transformationInfoState.value =
                        TransformationInfo(
                            sourceRotation = rotation,
                            transformationMode = TransformationMode.PRE_APPLIED,
                        )
                }
                rule.awaitIdle()

                // Verify that the center of the Viewfinder maps to the center of the Surface, and
                // a point in the top-left of the Viewfinder maps to the expected pre-rotated
                // quadrant of the Surface.
                val centerInSurface =
                    with(coordinateTransformer) {
                        Offset(viewfinderSizePx / 2f, viewfinderSizePx / 2f).transform()
                    }
                assertThat(centerInSurface.x).isWithin(1f).of(surfaceWidth / 2f)
                assertThat(centerInSurface.y).isWithin(1f).of(surfaceHeight / 2f)

                val topLeftInSurface =
                    with(coordinateTransformer) {
                        Offset(viewfinderSizePx / 4f, viewfinderSizePx / 4f).transform()
                    }
                assertThat(topLeftInSurface.x > surfaceWidth / 2f)
                    .isEqualTo(rotation == 180 || rotation == 270)
                assertThat(topLeftInSurface.y > surfaceHeight / 2f)
                    .isEqualTo(rotation == 90 || rotation == 180)

                assertRendersUpright(surface, surfaceWidth, surfaceHeight, rotation)
            }
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun preAppliedTransformation_withInitialRotation_rendersUprightBeforeAndAfterMove(): Unit =
        runBlocking {
            assumeFalse(
                "Skipping test due to SurfaceTexture crash on Emulator API 36",
                AndroidUtil.isEmulator(36),
            )
            assume()
                .withMessage(
                    "SurfaceControl is required to rotate PRE_APPLIED content in EXTERNAL mode."
                )
                .that(
                    implementationMode == ImplementationMode.EMBEDDED ||
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
                )
                .isTrue()

            val surfaceWidth = 200
            val surfaceHeight = 400
            val viewfinderSizePx = 200
            val sourceRotation = 90

            testMovableWithSession(
                withRenderAnimation = false,
                composeContent = { onInit ->
                    with(LocalDensity.current) {
                        TestViewfinder(
                            modifier =
                                Modifier.size(viewfinderSizePx.toDp(), viewfinderSizePx.toDp())
                                    .testTag(VIEWFINDER_TEST_TAG),
                            surfaceRequest =
                                remember {
                                    ViewfinderSurfaceRequest(
                                        width = surfaceWidth,
                                        height = surfaceHeight,
                                        implementationMode = implementationMode,
                                    )
                                },
                            transformationInfo =
                                TransformationInfo(
                                    sourceRotation = sourceRotation,
                                    transformationMode = TransformationMode.PRE_APPLIED,
                                ),
                            onInit = onInit,
                        )
                    }
                },
            ) {
                val surface = awaitSurfaceSession().surface

                // The rotation is applied to a surface that is created with a non-zero rotation.
                assertRendersUpright(surface, surfaceWidth, surfaceHeight, sourceRotation)

                // Moving the viewfinder detaches and reattaches its surface. The rotation must be
                // preserved.
                moveViewfinder()
                rule.awaitIdle()

                assertThat(surface.isValid).isTrue()
                assertRendersUpright(surface, surfaceWidth, surfaceHeight, sourceRotation)
            }
        }

    /**
     * Draws quadrants pre-rotated for [sourceRotation] to [surface] until the viewfinder shows them
     * upright, and fails if it doesn't within a timeout.
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private suspend fun assertRendersUpright(
        surface: Surface,
        surfaceWidth: Int,
        surfaceHeight: Int,
        sourceRotation: Int,
    ) {
        // Poll briefly to allow SurfaceFlinger / TextureView to composite the updated buffer and
        // transform.
        var lastColors: List<Color> = emptyList()
        val matched =
            withTimeoutOrNull(3.seconds) {
                while (true) {
                    withContext(AndroidUiDispatcher.Main) {
                        surface.drawPreRotatedQuadrants(
                            width = surfaceWidth,
                            height = surfaceHeight,
                            sourceRotation = sourceRotation,
                        )
                    }
                    rule.awaitIdle()
                    delay(50)
                    val pixelMap =
                        rule.onNodeWithTag(VIEWFINDER_TEST_TAG).captureToImage().toPixelMap()
                    val tl = pixelMap[pixelMap.width / 4, pixelMap.height / 4]
                    val tr = pixelMap[(3 * pixelMap.width) / 4, pixelMap.height / 4]
                    val bl = pixelMap[pixelMap.width / 4, (3 * pixelMap.height) / 4]
                    val br = pixelMap[(3 * pixelMap.width) / 4, (3 * pixelMap.height) / 4]
                    lastColors = listOf(tl, tr, bl, br)
                    if (
                        tl.isCloseTo(Color.Red) &&
                            tr.isCloseTo(Color.Green) &&
                            bl.isCloseTo(Color.Blue) &&
                            br.isCloseTo(Color.Yellow)
                    ) {
                        break
                    }
                }
                true
            } ?: false

        assertWithMessage(
                "Quadrant colors at sourceRotation=$sourceRotation (TL, TR, BL, BR)=$lastColors"
            )
            .that(matched)
            .isTrue()
    }

    @Test
    fun canRetrieveSurface() = runBlocking {
        // Disable render animation so we aren't competing to lock the canvas
        testWithSession(withRenderAnimation = false) {
            val surface = awaitSurfaceSession().surface
            surface.lockHardwareCanvas().apply {
                try {
                    assertThat(Size(width, height))
                        .isEqualTo(ViewfinderTestParams.Default.sourceResolution)
                } finally {
                    surface.unlockCanvasAndPost(this)
                }
            }
        }
    }

    @Test
    fun verifySurfacesAreReleased_surfaceRequestReleased_thenComposableDestroyed(): Unit =
        runBlocking {
            assumeFalse(
                "Skipping test due to SurfaceTexture crash on Emulator API 36",
                AndroidUtil.isEmulator(36),
            )
            testHideableWithSession {
                val surface = awaitSurfaceSession().surface
                assertThat(surface.isValid).isTrue()

                allowNextSessionCompletion()
                rule.awaitIdleWithPausedRendering()
                assertThat(surface.isValid).isTrue()

                hideViewfinder()
                rule.awaitIdleWithPausedRendering()
                assertThat(surface.isValid).isFalse()
            }
        }

    @Test
    fun verifySurfacesAreReleased_composableDestroyed_thenSurfaceRequestReleased(): Unit =
        runBlocking {
            assumeFalse(
                "Skipping test due to SurfaceTexture crash on Emulator API 36",
                AndroidUtil.isEmulator(36),
            )
            assume()
                .withMessage(
                    "EXTERNAL implamentation on API < 29 is not yet able to delay surface destruction by the Viewfinder."
                )
                .that(
                    Build.VERSION.SDK_INT >= 29 || implementationMode != ImplementationMode.EXTERNAL
                )
                .isTrue()
            testHideableWithSession {
                val surface = awaitSurfaceSession().surface
                assertThat(surface.isValid).isTrue()

                hideViewfinder()
                rule.awaitIdleWithPausedRendering()
                assertThat(surface.isValid).isTrue()

                allowNextSessionCompletion()
                rule.awaitIdleWithPausedRendering()
                assertThat(surface.isValid).isFalse()
            }
        }

    @Test
    fun movableContentOf_afterMove_validSurfaceIsAvailable(): Unit = runBlocking {
        testMovableWithSession {
            val surfaceSession = awaitSurfaceSession()
            assertThat(surfaceSession.surface.isValid).isTrue()
            rule.awaitIdleWithPausedRendering()

            moveViewfinder()

            rule.awaitIdleWithPausedRendering()

            when (implementationMode) {
                ImplementationMode.EMBEDDED -> assertThat(surfaceSession.surface.isValid).isTrue()
                ImplementationMode.EXTERNAL ->
                    if (Build.VERSION.SDK_INT >= 29) {
                        assertThat(surfaceSession.surface.isValid).isTrue()
                    } else {
                        // A new surface would need to be created on API 28 and lower, wait for
                        // the new surface session
                        allowNextSessionCompletion()
                        rule.awaitIdle()
                        val newSurfaceSession =
                            withTimeoutOrNull(5.seconds) {
                                awaitSurfaceSession { it !== surfaceSession }
                            }
                        assertThat(newSurfaceSession).isNotNull()
                        assertThat(newSurfaceSession!!.surface.isValid).isTrue()
                    }
            }
        }
    }

    @Test
    fun viewfinderInPagerWithDefaultOffscreenPageCount_afterMoveOffThenOnScreen_validSurfaceIsAvailable():
        Unit =
        runTest(rule.mainClock.scheduler) {
            assumeFalse(
                "Test fails on cuttlefish b/467137284",
                Build.MODEL.contains("Cuttlefish", ignoreCase = true),
            )
            testPageableWithSession(beyondViewportPageCount = 0) {
                val firstSurfaceSession = awaitSurfaceSession()
                assertThat(firstSurfaceSession.surface.isValid).isTrue()

                scrollToPage(1)

                rule.awaitIdleWithPausedRendering()
                // Moving off screen will remove the View from the composition, so the session
                // should
                // be completed.
                allowNextSessionCompletion()

                scrollToPage(0)
                rule.awaitIdleWithPausedRendering()
                val secondSurfaceSession = awaitSurfaceSession()

                assertThat(secondSurfaceSession.surface.isValid).isTrue()
            }
        }

    @Test
    fun viewfinderInPagerWithOneOffscreenPageCount_afterMoveOffThenOnScreen_validSurfaceIsAvailable():
        Unit =
        runTest(rule.mainClock.scheduler) {
            // When the beyondViewportPageCount keeps the underlying View alive, we don't expect
            // the session to be recreated since the composable is never removed from the
            // composition.
            testPageableWithSession(beyondViewportPageCount = 1) {
                val surfaceSession = awaitSurfaceSession()
                assertThat(surfaceSession.surface.isValid).isTrue()

                scrollToPage(1)
                rule.awaitIdleWithPausedRendering()

                scrollToPage(0)
                rule.awaitIdleWithPausedRendering()

                assertThat(surfaceSession.surface.isValid).isTrue()
            }
        }

    @Test
    fun frameRenderedListener_isInvokedWhenSurfaceIsDrawn(): Unit =
        runTest(rule.mainClock.scheduler) {
            assumeFalse(
                "FrameRenderedListener may not be invoked on emulator environments due to GL/rendering limitations or cause crashes",
                AndroidUtil.isEmulator(),
            )
            assumeTrue(implementationMode == ImplementationMode.EMBEDDED)

            testWithSession(withRenderAnimation = true) {
                val surfaceSession = awaitSurfaceSession()
                val listenerInvokedCount = AtomicInteger(0)
                val firstFrameRendered = CompletableDeferred<Unit>()
                val listener = FrameRenderedListener { _ ->
                    listenerInvokedCount.incrementAndGet()
                    firstFrameRendered.complete(Unit)
                }
                val executor = Executor { it.run() }

                surfaceSession.addFrameRenderedListener(executor, listener)

                // runTest uses virtual time; wait in real time so TextureView can draw.
                withContext(Dispatchers.Default) {
                    withTimeoutOrNull(2.seconds) { firstFrameRendered.await() }
                }

                assertThat(listenerInvokedCount.get()).isGreaterThan(0)

                // Remove on the main thread, where onSurfaceTextureUpdated dispatches, so no
                // in-flight dispatch can race with the removal.
                val countAfterRemoval =
                    withContext(AndroidUiDispatcher.Main) {
                        surfaceSession.removeFrameRenderedListener(listener)
                        listenerInvokedCount.get()
                    }

                // Wait a bit in real time to ensure no more invocations happen
                withContext(Dispatchers.Default) { delay(500) }
                assertThat(listenerInvokedCount.get()).isEqualTo(countAfterRemoval)
            }
        }

    private interface SessionTestScope {

        suspend fun awaitSurfaceSession(
            predicate: ((ViewfinderSurfaceSessionScope) -> Boolean)? = null
        ): ViewfinderSurfaceSessionScope

        fun allowNextSessionCompletion()

        suspend fun ComposeTestRule.awaitIdleWithPausedRendering()
    }

    private interface HideableSessionTestScope : SessionTestScope {
        fun hideViewfinder()
    }

    private interface MovableSessionTestScope : SessionTestScope {
        fun moveViewfinder()
    }

    private interface PageableSessionTestScope : SessionTestScope {
        suspend fun scrollToPage(page: Int)
    }

    private suspend inline fun <T : SessionTestScope> testWithSessionInternal(
        crossinline scopeProvider: (SessionTestScope) -> T,
        crossinline composeContent: @Composable (onInit: ViewfinderInitScope.() -> Unit) -> Unit,
        withRenderAnimation: Boolean = true,
        crossinline block: suspend T.() -> Unit,
    ) {
        var renderJob: Job? = null
        val surfaceSessionFlow = MutableStateFlow<ViewfinderSurfaceSessionScope?>(null)
        val sessionCompleteCount = MutableStateFlow(0)
        val paused = MutableStateFlow(!withRenderAnimation)

        var numSessions = 0
        val onInit: ViewfinderInitScope.() -> Unit = {
            onSurfaceSession {
                numSessions++
                surfaceSessionFlow.value = this@onSurfaceSession

                renderJob =
                    launch(AndroidUiDispatcher.Main) {
                        val w = request.width
                        val h = request.height

                        // Render loop
                        var initialTime: Long? = null
                        paused.collectLatest { paused ->
                            while (!paused) {
                                withFrameNanos { time ->
                                    if (initialTime == null) {
                                        initialTime = time
                                    }
                                    surface.tryDrawWithCanvas(Rect(0, 0, w, h)) {
                                        val timeMs = (time - initialTime) / 1_000_000L
                                        val t =
                                            0.5f - 0.5f * cos(Math.PI.toFloat() * timeMs / 1_000.0f)
                                        drawColor(lerp(Color.Blue, Color.Yellow, t).toArgb())
                                    }
                                }
                            }
                        }
                    }

                withContext(NonCancellable) { sessionCompleteCount.first { it >= numSessions } }
            }
        }

        rule.setContent { composeContent(onInit) }

        val baseSessionTestScope =
            BaseSessionTestScope(surfaceSessionFlow, sessionCompleteCount, paused)

        val specificSessionTestScope = scopeProvider(baseSessionTestScope)

        try {
            block.invoke(specificSessionTestScope)
        } finally {
            sessionCompleteCount.value = Int.MAX_VALUE
            renderJob?.cancel()
            renderJob?.join()
            rule.awaitIdle()
        }
    }

    private suspend inline fun testPageableWithSession(
        beyondViewportPageCount: Int,
        crossinline block: suspend PageableSessionTestScope.() -> Unit,
    ) {
        val selectedPageFlow = MutableStateFlow(0)
        val settledPageFlow = MutableStateFlow(0)
        testWithSessionInternal(
            scopeProvider = { baseScope ->
                object : SessionTestScope by baseScope, PageableSessionTestScope {
                    override suspend fun scrollToPage(page: Int) {
                        selectedPageFlow.value = page

                        // Manually advance the frame clock and yield until the page settles
                        while (settledPageFlow.value != page) {
                            rule.mainClock.advanceTimeByFrame()
                            yield()
                        }
                    }
                }
            },
            composeContent = { onInit ->
                val pagerState = rememberPagerState(pageCount = { 3 })

                HorizontalPager(
                    modifier = Modifier.fillMaxSize(),
                    state = pagerState,
                    beyondViewportPageCount = beyondViewportPageCount,
                ) { page ->
                    when (page) {
                        0 -> TestViewfinder(onInit = onInit)
                        1 -> Box(modifier = Modifier.fillMaxSize().background(Color.Red))
                    }
                }

                LaunchedEffect(Unit) {
                    selectedPageFlow.collectLatest { selectedPage ->
                        withContext(AndroidUiDispatcher.Main) {
                            pagerState.animateScrollToPage(selectedPage)
                        }

                        // Wait for the page to settle
                        snapshotFlow { pagerState.settledPage }.first { it == selectedPage }

                        settledPageFlow.value = selectedPage
                    }
                }
            },
            block = block,
        )
    }

    /**
     * Runs [block] with a viewfinder inside `movableContentOf` that [MovableSessionTestScope] can
     * move.
     *
     * @param composeContent The viewfinder content to move, or `null` for a default
     *   [TestViewfinder].
     */
    private suspend fun testMovableWithSession(
        withRenderAnimation: Boolean = true,
        composeContent: (@Composable (onInit: ViewfinderInitScope.() -> Unit) -> Unit)? = null,
        block: suspend MovableSessionTestScope.() -> Unit,
    ) {
        val moveViewfinderState = mutableStateOf(false)

        testWithSessionInternal(
            scopeProvider = { baseScope ->
                object : SessionTestScope by baseScope, MovableSessionTestScope {
                    override fun moveViewfinder() {
                        moveViewfinderState.value = true
                    }
                }
            },
            withRenderAnimation = withRenderAnimation,
            composeContent = { onInit ->
                var moveView by remember { moveViewfinderState }
                val content = remember {
                    movableContentOf {
                        if (composeContent != null) {
                            composeContent(onInit)
                        } else {
                            TestViewfinder(onInit = onInit)
                        }
                    }
                }

                Column {
                    if (moveView) {
                        content()
                    } else {
                        content()
                    }
                }
            },
            block = block,
        )
    }

    private suspend fun testWithSession(
        withRenderAnimation: Boolean = true,
        block: suspend SessionTestScope.() -> Unit,
    ) {
        testWithSessionInternal(
            scopeProvider = { it },
            composeContent = { onInit -> TestViewfinder(onInit = onInit) },
            block = block,
            withRenderAnimation = withRenderAnimation,
        )
    }

    private suspend fun testHideableWithSession(
        block: suspend HideableSessionTestScope.() -> Unit
    ) {
        val showViewfinderState = mutableStateOf(true)

        testWithSessionInternal(
            scopeProvider = { baseScope ->
                object : SessionTestScope by baseScope, HideableSessionTestScope {
                    override fun hideViewfinder() {
                        showViewfinderState.value = false
                    }
                }
            },
            composeContent = { onInit ->
                val showView by remember { showViewfinderState }
                if (showView) {
                    TestViewfinder(onInit = onInit)
                }
            },
            block = block,
        )
    }

    class BaseSessionTestScope(
        val surfaceFlow: StateFlow<ViewfinderSurfaceSessionScope?>,
        val sessionCompletionCount: MutableStateFlow<Int>,
        val paused: MutableStateFlow<Boolean>,
    ) : SessionTestScope {
        override suspend fun awaitSurfaceSession(
            predicate: ((ViewfinderSurfaceSessionScope) -> Boolean)?
        ): ViewfinderSurfaceSessionScope {
            return surfaceFlow.filterNotNull().run {
                if (predicate != null) {
                    this.first(predicate)
                } else {
                    this.first()
                }
            }
        }

        override fun allowNextSessionCompletion() {
            sessionCompletionCount.update { it + 1 }
        }

        override suspend fun ComposeTestRule.awaitIdleWithPausedRendering() {
            val oldPaused = paused.getAndUpdate { true }
            try {
                awaitIdle()
            } finally {
                paused.value = oldPaused
            }
        }
    }

    @Composable
    fun TestViewfinder(
        modifier: Modifier = Modifier,
        transformationInfo: TransformationInfo = ViewfinderTestParams.Default.transformationInfo,
        surfaceRequest: ViewfinderSurfaceRequest = remember {
            ViewfinderSurfaceRequest(
                width = ViewfinderTestParams.Default.sourceResolution.width,
                height = ViewfinderTestParams.Default.sourceResolution.height,
                implementationMode = implementationMode,
            )
        },
        coordinateTransformer: MutableCoordinateTransformer? = null,
        onInit: ViewfinderInitScope.() -> Unit,
    ) {
        Viewfinder(
            modifier = modifier.fillMaxSize(),
            surfaceRequest = surfaceRequest,
            transformationInfo = transformationInfo,
            coordinateTransformer = coordinateTransformer,
            onInit = onInit,
        )
    }
}

private const val TAG = "ViewfinderTest"
private const val VIEWFINDER_TEST_TAG = "ViewfinderTestTag"

private fun Surface.drawPreRotatedQuadrants(width: Int, height: Int, sourceRotation: Int) {
    // Colors of the 4 quadrants when the image is viewed upright on screen:
    // Top-Left = Red, Top-Right = Green, Bottom-Left = Blue, Bottom-Right = Yellow.
    // The buffer is pre-rotated counter-clockwise by sourceRotation so that rotating the buffer
    // clockwise by sourceRotation restores the upright orientation.
    val (topLeftColor, topRightColor, bottomLeftColor, bottomRightColor) =
        when (sourceRotation) {
            0 -> listOf(Color.Red, Color.Green, Color.Blue, Color.Yellow)
            90 -> listOf(Color.Green, Color.Yellow, Color.Red, Color.Blue)
            180 -> listOf(Color.Yellow, Color.Blue, Color.Green, Color.Red)
            270 -> listOf(Color.Blue, Color.Red, Color.Yellow, Color.Green)
            else -> throw IllegalArgumentException("Unexpected rotation: $sourceRotation")
        }
    val paint = Paint().apply { style = Paint.Style.FILL }
    val halfW = width / 2f
    val halfH = height / 2f
    val canvas = lockHardwareCanvas()
    try {
        for ((color, rect) in
            listOf(
                topLeftColor to RectF(0f, 0f, halfW, halfH),
                topRightColor to RectF(halfW, 0f, width.toFloat(), halfH),
                bottomLeftColor to RectF(0f, halfH, halfW, height.toFloat()),
                bottomRightColor to RectF(halfW, halfH, width.toFloat(), height.toFloat()),
            )) {
            paint.color = color.toArgb()
            canvas.drawRect(rect, paint)
        }
    } finally {
        unlockCanvasAndPost(canvas)
    }
}

private fun Color.isCloseTo(expected: Color, tolerance: Float = 0.1f): Boolean =
    abs(red - expected.red) <= tolerance &&
        abs(green - expected.green) <= tolerance &&
        abs(blue - expected.blue) <= tolerance

private inline fun Surface.tryDrawWithCanvas(
    inOutDirty: Rect,
    crossinline block: Canvas.() -> Unit,
) {
    try {
        lockCanvas(inOutDirty).apply {
            block()
            unlockCanvasAndPost(this)
        }
    } catch (e: IllegalStateException) {
        Log.e(TAG, "Unable to draw to canvas", e)
    }
}
