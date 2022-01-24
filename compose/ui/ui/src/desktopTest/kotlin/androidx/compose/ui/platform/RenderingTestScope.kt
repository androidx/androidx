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
import androidx.compose.ui.ComposeScene
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.swing.Swing
import kotlinx.coroutines.yield
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Surface
import org.jetbrains.skiko.FrameDispatcher
import kotlin.coroutines.CoroutineContext

internal fun renderingTest(
    width: Int,
    height: Int,
    context: CoroutineContext = Dispatchers.Swing,
    block: suspend RenderingTestScope.() -> Unit
) = runBlocking(context) {
    val scope = RenderingTestScope(width, height, context)
    try {
        scope.block()
    } finally {
        scope.dispose()
    }
}

@OptIn(ExperimentalComposeUiApi::class)
internal class RenderingTestScope(
    val width: Int,
    val height: Int,
    coroutineContext: CoroutineContext
) {
    var currentTimeMillis = 0L

    private val frameDispatcher = FrameDispatcher(coroutineContext) {
        onRender(currentTimeMillis * 1_000_000)
    }

    val surface: Surface = Surface.makeRasterN32Premul(width, height)
    val canvas: Canvas = surface.canvas
    val scene = ComposeScene(
        coroutineContext = coroutineContext,
        invalidate = frameDispatcher::scheduleFrame
    ).apply {
        constraints = Constraints(maxWidth = width, maxHeight = height)
    }

    var density: Float
        get() = scene.density.density
        set(value) {
            scene.density = Density(value, scene.density.fontScale)
        }

    fun dispose() {
        scene.close()
        frameDispatcher.cancel()
    }

    private var onRender = CompletableDeferred<Unit>()

    fun setContent(content: @Composable () -> Unit) {
        scene.setContent {
            content()
        }
    }

    private fun onRender(timeNanos: Long) {
        canvas.clear(Color.Transparent.toArgb())
        scene.render(canvas, timeNanos)
        onRender.complete(Unit)
    }

    suspend fun awaitNextRender() {
        onRender = CompletableDeferred()
        onRender.await()
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