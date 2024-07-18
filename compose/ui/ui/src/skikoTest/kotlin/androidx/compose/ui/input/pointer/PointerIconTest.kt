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

package androidx.compose.ui.input.pointer

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.assertThat
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.asComposeCanvas
import androidx.compose.ui.isEqualTo
import androidx.compose.ui.platform.LocalPointerIconService
import androidx.compose.ui.platform.PlatformContext
import androidx.compose.ui.scene.ComposeScene
import androidx.compose.ui.scene.ComposeSceneContext
import androidx.compose.ui.scene.PlatformLayersComposeScene
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runSkikoComposeUiTest
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.use
import kotlin.coroutines.CoroutineContext
import kotlin.test.Test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.jetbrains.skia.Surface
import org.jetbrains.skiko.FrameDispatcher

class PointerIconTest {
    private val iconService = object : PointerIconService {
        private var current: PointerIcon = PointerIcon.Default

        override fun getIcon(): PointerIcon {
            return current
        }

        override fun setIcon(value: PointerIcon?) {
            current = value ?: PointerIcon.Default
        }
    }

    @Test
    fun basicTest() = ImageComposeScene(
        width = 100, height = 100
    ).use { scene ->
        scene.setContent {
            CompositionLocalProvider(
                LocalPointerIconService provides iconService
            ) {
                Box(
                    modifier = Modifier
                        .size(30.dp, 30.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .pointerHoverIcon(PointerIcon.Text)
                            .size(10.dp, 10.dp)
                    )
                }
            }
        }

        scene.sendPointerEvent(PointerEventType.Move, Offset(5f, 5f))
        assertThat(iconService.getIcon()).isEqualTo(PointerIcon.Text)
    }

    @Test
    fun commitsToComponent() {
        val iconContext = IconPlatformContext()
        val size = IntSize(100, 100)
        val scene = SingleLayerComposeScene(
            platformContext = iconContext,
        )

        try {
            scene.size = size
            scene.setContent {
                Box(
                    modifier = Modifier
                        .size(30.dp, 30.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .pointerHoverIcon(PointerIcon.Text)
                            .size(10.dp, 10.dp)
                    )
                }
            }

            scene.sendPointerEvent(PointerEventType.Move, Offset(5f, 5f))
            assertThat(iconContext._pointerIcon).isEqualTo(PointerIcon.Text)
        } finally {
            scene.close()
        }
    }

    @Test
    fun preservedIfSameEventDispatchedTwice() {
        val iconContext = IconPlatformContext()
        val size = IntSize(100, 100)
        val scene = SingleLayerComposeScene(
            platformContext = iconContext,
        )

        try {
            scene.size = size
            scene.setContent {
                Box(
                    modifier = Modifier
                        .size(30.dp, 30.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .pointerHoverIcon(PointerIcon.Text)
                            .size(10.dp, 10.dp)
                    )
                }
            }

            scene.sendPointerEvent(PointerEventType.Move, Offset(5f, 5f))
            scene.sendPointerEvent(PointerEventType.Move, Offset(5f, 5f))
            assertThat(iconContext._pointerIcon).isEqualTo(PointerIcon.Text)
        } finally {
            scene.close()
        }
    }

    @Test
    fun parentWins() = ImageComposeScene(
        width = 100, height = 100
    ).use { scene ->
        scene.setContent {
            CompositionLocalProvider(
                LocalPointerIconService provides iconService
            ) {
                Box(
                    modifier = Modifier
                        .pointerHoverIcon(PointerIcon.Hand, true)
                        .size(30.dp, 30.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .pointerHoverIcon(PointerIcon.Text)
                            .size(10.dp, 10.dp)
                    )
                }
            }
        }

        // skip one frame, so Compose will properly calculate Boxes hierarchy
        // and detect overrideDescendants
        scene.render()
        scene.sendPointerEvent(PointerEventType.Move, Offset(5f, 5f))
        assertThat(iconService.getIcon()).isEqualTo(PointerIcon.Hand)

        scene.sendPointerEvent(PointerEventType.Move, Offset(15f, 15f))
        assertThat(iconService.getIcon()).isEqualTo(PointerIcon.Hand)
    }

    @Test
    fun childWins() = ImageComposeScene(
        width = 100, height = 100
    ).use { scene ->
        scene.setContent {
            CompositionLocalProvider(
                LocalPointerIconService provides iconService
            ) {
                Box(
                    modifier = Modifier
                        .pointerHoverIcon(PointerIcon.Hand)
                        .size(30.dp, 30.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .pointerHoverIcon(PointerIcon.Text)
                            .size(10.dp, 10.dp)
                    )
                }
            }
        }

        scene.sendPointerEvent(PointerEventType.Move, Offset(5f, 5f))
        assertThat(iconService.getIcon()).isEqualTo(PointerIcon.Text)

        scene.sendPointerEvent(PointerEventType.Move, Offset(15f, 15f))
        assertThat(iconService.getIcon()).isEqualTo(PointerIcon.Hand)
    }

    @Test
    fun whenHoveredShouldCommitWithoutMoveWhenIconChanges() = runTest(StandardTestDispatcher()) {
        val iconContext = IconPlatformContext()
        val size = IntSize(100, 100)
        val surface = Surface.makeRasterN32Premul(size.width, size.height)
        lateinit var scene: ComposeScene

        val frameDispatcher = FrameDispatcher(coroutineContext) {
            scene.render(surface.canvas.asComposeCanvas(), 1)
        }
        scene = SingleLayerComposeScene(
            coroutineContext = coroutineContext,
            platformContext = iconContext,
            invalidate = {
                frameDispatcher.scheduleFrame()
            }
        )
        val iconState = mutableStateOf(PointerIcon.Text)

        val recomposeChannel = Channel<Int>(Channel.CONFLATED) // helps with waiting for recomposition
        var count = 0
        try {
            scene.size = size
            scene.setContent {
                Box(
                    modifier = Modifier.pointerHoverIcon(iconState.value).size(30.dp, 30.dp)
                )
                recomposeChannel.trySend(++count)
            }
            scene.sendPointerEvent(PointerEventType.Move, Offset(5f, 5f))
            assertThat(recomposeChannel.receive()).isEqualTo(1)
            assertThat(iconContext._pointerIcon).isEqualTo(PointerIcon.Text)

            // No move, but change should be applied anyway
            iconState.value = PointerIcon.Crosshair
            assertThat(recomposeChannel.receive()).isEqualTo(2)
            assertThat(iconContext._pointerIcon).isEqualTo(PointerIcon.Crosshair)
        } finally {
            scene.close()
            frameDispatcher.cancel()
        }
    }

    @Test
    fun whenNotHoveredShouldNeverCommit() = runTest(StandardTestDispatcher()) {
        val iconContext = IconPlatformContext()
        val size = IntSize(100, 100)
        val surface = Surface.makeRasterN32Premul(size.width, size.height)
        lateinit var scene: ComposeScene

        val frameDispatcher = FrameDispatcher(coroutineContext) {
            scene.render(surface.canvas.asComposeCanvas(), 1)
        }
        scene = SingleLayerComposeScene(
            coroutineContext = coroutineContext,
            platformContext = iconContext,
            invalidate = {
                frameDispatcher.scheduleFrame()
            }
        )

        val iconState = mutableStateOf(PointerIcon.Text)

        val recomposeChannel = Channel<Int>(Channel.CONFLATED) // helps with waiting for recomposition
        var count = 0
        try {
            scene.size = size
            scene.setContent {
                Box(
                    modifier = Modifier.size(100.dp, 100.dp).pointerHoverIcon(PointerIcon.Default)
                ) {
                    Box(
                        modifier = Modifier.pointerHoverIcon(iconState.value).size(30.dp, 30.dp)
                    )
                }
                recomposeChannel.trySend(++count)
            }
            assertThat(recomposeChannel.receive()).isEqualTo(1)
            assertThat(iconContext._pointerIcon).isEqualTo(null)

            // No move, not hovered. No pointer icon change expected
            iconState.value = PointerIcon.Crosshair
            assertThat(recomposeChannel.receive()).isEqualTo(2)
            assertThat(iconContext._pointerIcon).isEqualTo(null)

            // Move, but not hovered. Pointer Icon should be Default
            scene.sendPointerEvent(PointerEventType.Move, Offset(90f, 95f))
            assertThat(iconContext._pointerIcon).isEqualTo(PointerIcon.Default)
        } finally {
            scene.close()
            frameDispatcher.cancel()
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun resetsToDefault() = runSkikoComposeUiTest(Size(width = 100f, height = 100f)) {
        var show by mutableStateOf(true)
        setContent {
            CompositionLocalProvider(LocalPointerIconService provides iconService) {
                Box(Modifier.fillMaxSize()) {
                    if (show) {
                        Box(
                            modifier = Modifier
                                .pointerHoverIcon(PointerIcon.Text)
                                .size(10.dp, 10.dp)
                        )
                    }
                }
            }
        }

        assertThat(iconService.getIcon()).isEqualTo(PointerIcon.Default)

        scene.sendPointerEvent(PointerEventType.Move, Offset(5f, 5f))
        assertThat(iconService.getIcon()).isEqualTo(PointerIcon.Text)

        show = false
        waitForIdle()
        assertThat(iconService.getIcon()).isEqualTo(PointerIcon.Default)
    }


    private class IconPlatformContext : PlatformContext by PlatformContext.Empty {
        @Suppress("PropertyName")
        var _pointerIcon: PointerIcon? = null

        override fun setPointerIcon(pointerIcon: PointerIcon) {
            this._pointerIcon = pointerIcon
        }
    }
}

private fun SingleLayerComposeScene(
    coroutineContext: CoroutineContext = Dispatchers.Unconfined,
    platformContext: PlatformContext,
    invalidate: () -> Unit = {},
) = PlatformLayersComposeScene(
    coroutineContext = coroutineContext,
    composeSceneContext = object : ComposeSceneContext {
        override val platformContext get() = platformContext
    },
    invalidate = invalidate
)
