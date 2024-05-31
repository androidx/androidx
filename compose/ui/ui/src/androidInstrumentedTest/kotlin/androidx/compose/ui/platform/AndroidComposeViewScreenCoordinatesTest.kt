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

package androidx.compose.ui.platform

import android.content.Context
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.WindowManager.LayoutParams
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.background
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionOnScreen
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.findViewTreeSavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class AndroidComposeViewScreenCoordinatesTest {

    @get:Rule val rule = createComposeRule()

    private lateinit var windowManager: WindowManager
    private lateinit var view: TestView

    @Before
    fun setUp() {
        rule.setContent {
            val hostView = LocalView.current
            DisposableEffect(Unit) {
                // Create a new window so we can control its position.
                windowManager =
                    hostView.context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
                view = TestView(hostView)
                @Suppress("DEPRECATION")
                val layoutParams =
                    LayoutParams().also {
                        it.x = 0
                        it.y = 0
                        it.width = LayoutParams.WRAP_CONTENT
                        it.height = LayoutParams.WRAP_CONTENT
                        it.type = LayoutParams.TYPE_APPLICATION
                        // Fullscreen to avoid accounting for system decorations.
                        it.flags =
                            LayoutParams.FLAG_LAYOUT_NO_LIMITS or LayoutParams.FLAG_FULLSCREEN
                        it.gravity = Gravity.LEFT or Gravity.TOP
                    }
                windowManager.addView(view, layoutParams)

                onDispose { windowManager.removeView(view) }
            }
        }
    }

    @Test
    fun positionOnScreen_withNoComposableOffset() {
        rule.runOnIdle {
            updateLayoutParams {
                it.x = 10
                it.y = 20
            }
        }

        rule.waitUntil {
            val coordinates = assertNotNull(view.coordinates)
            coordinates.positionOnScreen() == view.locationOnScreen
        }
    }

    @Test
    fun positionOnScreen_withComposableOffset() {
        rule.runOnIdle {
            updateLayoutParams {
                it.x = 10
                it.y = 20
            }
            view.innerOffset = IntOffset(30, 40)
        }

        rule.waitUntil {
            val coordinates = assertNotNull(view.coordinates)
            coordinates.positionOnScreen() == view.locationOnScreen
        }
    }

    @Test
    fun positionOnScreen_changesAfterUpdate() {
        rule.runOnIdle {
            updateLayoutParams {
                it.x = 10
                it.y = 20
            }
        }

        rule.waitUntil {
            val coordinates = assertNotNull(view.coordinates)
            coordinates.positionOnScreen() == view.locationOnScreen
        }

        rule.runOnIdle {
            updateLayoutParams {
                it.x = 30
                it.y = 40
            }
        }

        rule.waitUntil {
            val coordinates = assertNotNull(view.coordinates)
            coordinates.positionOnScreen() == view.locationOnScreen
        }
    }

    @Test
    fun screenToLocal_withNoComposableOffset() {
        rule.runOnIdle {
            updateLayoutParams {
                it.x = 10
                it.y = 20
            }
        }

        rule.runOnIdle {
            val coordinates = assertNotNull(view.coordinates)
            assertThat(coordinates.screenToLocal(view.locationOnScreen)).isEqualTo(Offset.Zero)
        }
    }

    @Test
    fun screenToLocal_withComposableOffset() {
        rule.runOnIdle {
            updateLayoutParams {
                it.x = 10
                it.y = 20
            }
            view.innerOffset = IntOffset(30, 40)
        }

        rule.runOnIdle {
            val coordinates = assertNotNull(view.coordinates)
            assertThat(coordinates.screenToLocal(view.locationOnScreen))
                .isEqualTo(Offset(-30f, -40f))
        }
    }

    private val View.locationOnScreen: Offset
        get() {
            val array = IntArray(2)
            getLocationOnScreen(array)
            return Offset(array[0].toFloat(), array[1].toFloat())
        }

    @Test
    fun transformToScreen_fromIdentity_withNoComposableOffset() {
        val matrix = Matrix()
        rule.runOnIdle {
            updateLayoutParams {
                it.x = 10
                it.y = 20
            }
        }

        rule.runOnIdle {
            val coordinates = assertNotNull(view.coordinates)
            coordinates.transformToScreen(matrix)
            assertThat(matrix.map(Offset.Zero)).isEqualTo(view.locationOnScreen + Offset.Zero)
        }
    }

    @Test
    fun transformToScreen_fromIdentity_withComposableOffset() {
        val matrix = Matrix()
        rule.runOnIdle {
            updateLayoutParams {
                it.x = 10
                it.y = 20
            }
            view.innerOffset = IntOffset(30, 40)
        }

        rule.runOnIdle {
            val coordinates = assertNotNull(view.coordinates)
            coordinates.transformToScreen(matrix)
            assertThat(matrix.map(Offset.Zero)).isEqualTo(view.locationOnScreen + Offset(30f, 40f))
        }
    }

    @Test
    fun transformToScreen_changesAfterUpdate() {
        val matrix = Matrix()
        rule.runOnIdle {
            updateLayoutParams {
                it.x = 10
                it.y = 20
            }
        }

        rule.runOnIdle {
            val coordinates = assertNotNull(view.coordinates)
            coordinates.transformToScreen(matrix)
            assertThat(matrix.map(Offset.Zero)).isEqualTo(view.locationOnScreen + Offset.Zero)

            updateLayoutParams {
                it.x = 30
                it.y = 40
            }
        }

        rule.runOnIdle {
            val coordinates = assertNotNull(view.coordinates)
            matrix.reset()
            coordinates.transformToScreen(matrix)
            assertThat(matrix.map(Offset.Zero)).isEqualTo(view.locationOnScreen + Offset.Zero)
        }
    }

    @Test
    fun transformToScreen_fromTransformedMatrix_includesExistingTransformation() {
        val matrix = Matrix()
        rule.runOnIdle {
            updateLayoutParams {
                it.x = 10
                it.y = 20
            }
        }
        matrix.translate(30f, 40f)

        rule.runOnIdle {
            val coordinates = assertNotNull(view.coordinates)
            coordinates.transformToScreen(matrix)
            assertThat(matrix.map(Offset.Zero)).isEqualTo(view.locationOnScreen + Offset(30f, 40f))
        }
    }

    private fun updateLayoutParams(block: (LayoutParams) -> Unit) {
        view.updateLayoutParams(block)
        windowManager.updateViewLayout(view, view.layoutParams)
    }

    private class TestView(hostView: View) : AbstractComposeView(hostView.context) {
        var coordinates: LayoutCoordinates? = null
        var innerOffset by mutableStateOf(IntOffset.Zero)

        init {
            setViewTreeLifecycleOwner(hostView.findViewTreeLifecycleOwner())
            setViewTreeSavedStateRegistryOwner(hostView.findViewTreeSavedStateRegistryOwner())
        }

        @Composable
        override fun Content() {
            Box(
                Modifier.background(Color.Blue)
                    .layout { measurable, _ ->
                        val placeable = measurable.measure(Constraints.fixed(10, 10))
                        layout(
                            width = innerOffset.x + placeable.width,
                            height = innerOffset.y + placeable.height
                        ) {
                            placeable.place(innerOffset)
                        }
                    }
                    .background(Color.Red)
                    .onGloballyPositioned { coordinates = it }
            )
        }
    }
}
