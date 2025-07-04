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
import android.graphics.Rect
import android.os.Build
import android.util.Log
import android.util.Size
import android.view.Surface
import androidx.camera.viewfinder.core.ImplementationMode
import androidx.camera.viewfinder.core.TransformationInfo
import androidx.camera.viewfinder.core.ViewfinderSurfaceRequest
import androidx.camera.viewfinder.core.ViewfinderSurfaceSessionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.AndroidUiDispatcher
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.TruthJUnit.assume
import kotlin.math.cos
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
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

    @Test
    fun coordinatesTransformationSameSizeNoRotation(): Unit = runBlocking {
        val coordinateTransformer = MutableCoordinateTransformer()

        rule.setContent {
            with(LocalDensity.current) {
                TestViewfinder(
                    modifier = Modifier.size(540.toDp(), 960.toDp()),
                    coordinateTransformer = coordinateTransformer,
                ) {}
            }
        }

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

        rule.setContent {
            with(LocalDensity.current) {
                TestViewfinder(
                    modifier = Modifier.size(540.toDp(), 960.toDp()),
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
                ) {}
            }
        }

        val expectedMatrix =
            Matrix(
                values =
                    floatArrayOf(0.5f, 0f, 0f, 0f, 0f, 0.5f, 0f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 1f)
            )

        assertThat(coordinateTransformer.transformMatrix.values).isEqualTo(expectedMatrix.values)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.M) // Needed for Surface.lockHardwareCanvas()
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
        Unit = runBlocking {
        testPageableWithSession(beyondViewportPageCount = 0) {
            val firstSurfaceSession = awaitSurfaceSession()
            assertThat(firstSurfaceSession.surface.isValid).isTrue()

            scrollToPage(1)

            rule.awaitIdleWithPausedRendering()
            // Moving off screen will remove the View from the composition, so the session should
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
        Unit = runBlocking {
        // When the beyondViewportPageCount keeps the underlying View alive, we don't expect
        // the session to be recreated since the composable is never removed from the composition.
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
        val surfaceSessionFlow = MutableStateFlow<ViewfinderSurfaceSessionScope?>(null)
        val sessionCompleteCount = MutableStateFlow(0)
        val paused = MutableStateFlow(!withRenderAnimation)

        var numSessions = 0
        val onInit: ViewfinderInitScope.() -> Unit = {
            onSurfaceSession {
                numSessions++
                surfaceSessionFlow.value = this@onSurfaceSession

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
                                    val t = 0.5f - 0.5f * cos(Math.PI.toFloat() * timeMs / 1_000.0f)
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
                        settledPageFlow.first { it == page }
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

    private suspend fun testMovableWithSession(block: suspend MovableSessionTestScope.() -> Unit) {
        val moveViewfinderState = mutableStateOf(false)

        testWithSessionInternal(
            scopeProvider = { baseScope ->
                object : SessionTestScope by baseScope, MovableSessionTestScope {
                    override fun moveViewfinder() {
                        moveViewfinderState.value = true
                    }
                }
            },
            composeContent = { onInit ->
                var moveView by remember { moveViewfinderState }
                val content = remember { movableContentOf { TestViewfinder(onInit = onInit) } }

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
