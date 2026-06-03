/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.compose.ui.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeCanvas
import androidx.compose.ui.graphics.skiaCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.scene.CanvasLayersComposeScene
import androidx.compose.ui.scene.SingleComposeSceneRenderingScope
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.jetbrains.skia.Surface
import org.jetbrains.skiko.FrameDispatcher
import org.jetbrains.skiko.MainUIDispatcher

internal fun renderingTest(
    width: Int,
    height: Int,
    context: CoroutineContext = MainUIDispatcher,
    block: suspend RenderingTestScope.() -> Unit
) = runBlocking(MainUIDispatcher) {
    val scope = RenderingTestScope(width, height, context)
    try {
        scope.block()
    } finally {
        scope.dispose()
    }
}

internal class RenderingTestScope(
    val width: Int,
    val height: Int,
    coroutineContext: CoroutineContext
) {
    var currentTimeMillis = 0L

    private val frameDispatcher = FrameDispatcher(coroutineContext) {
        onRender(currentTimeMillis * 1_000_000)
    }
    private val frameRecomposer = FrameRecomposer(coroutineContext, frameDispatcher::scheduleFrame)
    private val sceneRenderingScope = SingleComposeSceneRenderingScope(frameDispatcher::scheduleFrame)

    val surface: Surface = Surface.makeRasterN32Premul(width, height)
    private val canvas = surface.canvas.asComposeCanvas()
    val scene = CanvasLayersComposeScene(
        frameRecomposer = frameRecomposer,
        invalidateLayout = sceneRenderingScope::onSceneInvalidation,
        invalidateDraw = sceneRenderingScope::onSceneInvalidation,
    ).apply {
        size = IntSize(width = width, height = height)
    }

    var density: Float
        get() = scene.density.density
        set(value) {
            scene.density = Density(value, scene.density.fontScale)
        }

    fun dispose() {
        scene.close()
        frameRecomposer.close()
        frameDispatcher.cancel()
    }

    private var onRender = CompletableDeferred<Unit>()

    fun setContent(content: @Composable () -> Unit) {
        scene.setContent(content = content)
    }

    private fun onRender(timeNanos: Long) {
        canvas.skiaCanvas.clear(Color.Transparent.toArgb())
        with(sceneRenderingScope) {
            scene.render(frameRecomposer, canvas, timeNanos)
        }
        onRender.complete(Unit)
    }

    suspend fun awaitNextRender() {
        onRender = CompletableDeferred()
        onRender.await()
    }

    suspend fun skipRenders() {
        repeat(1000) {
            yield()
        }
    }

    suspend fun hasRenders(): Boolean {
        onRender = CompletableDeferred()
        // repeat multiple times because rendering can be dispatched on the next frames
        repeat(10) {
            yield()
        }
        return onRender.isCompleted
    }
}
