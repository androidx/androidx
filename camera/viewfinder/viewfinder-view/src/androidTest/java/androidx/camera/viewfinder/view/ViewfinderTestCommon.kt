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

package androidx.camera.viewfinder.view

import android.view.ViewGroup
import androidx.camera.viewfinder.core.TransformationInfo
import androidx.test.core.app.ActivityScenario
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking

const val ANY_WIDTH = 640
const val ANY_HEIGHT = 480

val ANY_TRANSFORMATION_INFO =
    TransformationInfo(
        sourceRotation = 0,
        cropRectLeft = 0f,
        cropRectTop = 0f,
        cropRectRight = ANY_WIDTH.toFloat(),
        cropRectBottom = ANY_HEIGHT.toFloat(),
        isSourceMirroredHorizontally = false,
        isSourceMirroredVertically = false,
    )

val REQUEST_TIMEOUT = 2.seconds

inline fun runViewfinderTest(
    viewfinderInitiallyAttached: Boolean = true,
    crossinline block: suspend ViewfinderTestScope.() -> Unit,
) {
    runBlocking {
        ActivityScenario.launch(FakeActivity::class.java).use { scenario ->
            var viewfinderTestScope: ViewfinderTestScope? = null
            scenario.onActivity { activity ->
                val viewfinderView = ViewfinderView(activity)
                if (viewfinderInitiallyAttached) {
                    activity.setContentView(viewfinderView)
                }
                viewfinderTestScope =
                    ViewfinderTestScope(
                        viewfinder = viewfinderView,
                        activityScenario = scenario,
                        coroutineContext = coroutineContext,
                        viewfinderInitiallyAttached,
                    )
            }
            val testScope =
                viewfinderTestScope ?: throw AssertionError("Unable to create ViewfinderTestScope")

            with(testScope) { block() }
        }
    }
}

data class ViewfinderTestScope(
    val viewfinder: ViewfinderView,
    private val activityScenario: ActivityScenario<FakeActivity>,
    override val coroutineContext: CoroutineContext,
    private var attached: Boolean = false,
) : CoroutineScope {
    fun detachViewfinder() {
        if (attached) {
            activityScenario.onActivity {
                (viewfinder.parent as? ViewGroup)?.removeView(viewfinder)
            }
            attached = false
        }
    }
}
