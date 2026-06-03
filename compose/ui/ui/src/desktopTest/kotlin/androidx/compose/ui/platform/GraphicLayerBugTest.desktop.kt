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

package androidx.compose.ui.platform

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asComposeCanvas
import androidx.compose.ui.scene.CanvasLayersComposeScene
import androidx.compose.ui.scene.ComposeScene
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking
import org.jetbrains.skia.Surface
import org.jetbrains.skiko.MainUIDispatcher
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

/**
 * Collection of tests for encountered bugs
 */
@RunWith(JUnit4::class)
class GraphicLayerBugDesktopTest {
    // https://youtrack.jetbrains.com/issue/CMP-6729/iOS-Runtime-crash.-Unsupported-concurrent-change-during-composition
    // sendApplyNotifications can be called anywhere. When it was called inside composition, it triggers wrongly written observers
    @Test
    fun `no crash when sendApplyNotifications performed in composition`() {
        runLayerSceneTest { scene, frameDispatcher ->
            val canvas = Surface.makeRasterN32Premul(100, 100).canvas

            var triggerApplySnapshot by mutableStateOf(false)

            scene.setContent {
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                    CircularProgressIndicator()
                }

                if (triggerApplySnapshot) {
                    Snapshot.sendApplyNotifications()
                }
            }

            repeat(10) {
                frameDispatcher.performFrame(it * 100L)
                scene.measureAndLayout()
                scene.draw(canvas.asComposeCanvas())
            }

            triggerApplySnapshot = true

            repeat(10) {
                frameDispatcher.performFrame(1000 + it * 100L)
                scene.measureAndLayout()
                scene.draw(canvas.asComposeCanvas())
            }
        }
    }

    private fun runLayerSceneTest(body: CoroutineScope.(ComposeScene, FrameRecomposer) -> Unit) {
        var coroutineException: Throwable? = null

        // catching recomposition exceptions this way because of https://youtrack.jetbrains.com/issue/CMP-6734/ComposeScene-doesnt-catch-exceptions-during-recomposition
        // we catch exceptions this way with real ComposeWindow and the other testing method (runComposeUiTest)
        runBlocking(
            MainUIDispatcher + CoroutineExceptionHandler { _, throwable ->
                coroutineException = throwable
            }
        ) {
            val frameRecomposer = FrameRecomposer(coroutineContext)
            val scene = CanvasLayersComposeScene(
                frameRecomposer = frameRecomposer,
            )
            try {
                body(scene, frameRecomposer)
            } finally {
                scene.close()
                frameRecomposer.close()
            }
        }

        if (coroutineException != null) {
            throw coroutineException
        }
    }
}
