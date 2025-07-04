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
package androidx.compose.foundation.layout

import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.ui.ComposeUiFlags
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.WindowInsetsRulers.Companion.NavigationBars
import androidx.compose.ui.layout.WindowInsetsRulers.Companion.StatusBars
import androidx.compose.ui.layout.WindowInsetsRulers.Companion.SystemBars
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.round
import androidx.core.graphics.Insets
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsCompat.Type
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import org.junit.Assume
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@OptIn(ExperimentalComposeUiApi::class)
@MediumTest
@SdkSuppress(minSdkVersion = 30)
@RunWith(JUnit4::class)
class FitOutsideTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    @Before
    fun setup() {
        rule.runOnUiThread { rule.activity.enableEdgeToEdge() }
    }

    @Test
    fun testFitOutsideLeft() {
        Assume.assumeTrue(ComposeUiFlags.areWindowInsetsRulersEnabled)
        lateinit var outsideCoordinates: LayoutCoordinates
        lateinit var insideCoordinates: LayoutCoordinates
        lateinit var view: View
        rule.setContent {
            view = LocalView.current.parent as View
            Box(Modifier.fillMaxSize().onPlaced { outsideCoordinates = it }) {
                Box(
                    Modifier.fillMaxSize().fitOutside(SystemBars.current).onPlaced {
                        insideCoordinates = it
                    }
                )
            }
        }
        rule.runOnIdle { view.onApplyWindowInsets(createInsets(10, 20, 30, 50).toWindowInsets()) }

        rule.runOnIdle {
            assertThat(insideCoordinates.positionInRoot().round()).isEqualTo(IntOffset(0, 0))
            assertThat(insideCoordinates.size.width).isEqualTo(10)
            assertThat(insideCoordinates.size.height).isEqualTo(outsideCoordinates.size.height)
        }
    }

    @Test
    fun testFitOutsideTop() {
        Assume.assumeTrue(ComposeUiFlags.areWindowInsetsRulersEnabled)
        lateinit var outsideCoordinates: LayoutCoordinates
        lateinit var insideCoordinates: LayoutCoordinates
        lateinit var view: View
        rule.setContent {
            view = LocalView.current.parent as View
            Box(Modifier.fillMaxSize().onPlaced { outsideCoordinates = it }) {
                Box(
                    Modifier.fillMaxSize().fitOutside(SystemBars.current).onPlaced {
                        insideCoordinates = it
                    }
                )
            }
        }
        rule.runOnIdle { view.onApplyWindowInsets(createInsets(0, 20, 30, 50).toWindowInsets()) }

        rule.runOnIdle {
            assertThat(insideCoordinates.positionInRoot().round()).isEqualTo(IntOffset(0, 0))
            assertThat(insideCoordinates.size.width).isEqualTo(outsideCoordinates.size.width)
            assertThat(insideCoordinates.size.height).isEqualTo(20)
        }
    }

    @Test
    fun testFitOutsideRight() {
        Assume.assumeTrue(ComposeUiFlags.areWindowInsetsRulersEnabled)
        lateinit var outsideCoordinates: LayoutCoordinates
        lateinit var insideCoordinates: LayoutCoordinates
        lateinit var view: View
        rule.setContent {
            view = LocalView.current.parent as View
            Box(Modifier.fillMaxSize().onPlaced { outsideCoordinates = it }) {
                Box(
                    Modifier.fillMaxSize().fitOutside(SystemBars.current).onPlaced {
                        insideCoordinates = it
                    }
                )
            }
        }
        rule.runOnIdle { view.onApplyWindowInsets(createInsets(0, 0, 30, 50).toWindowInsets()) }

        rule.runOnIdle {
            assertThat(insideCoordinates.positionInRoot().round())
                .isEqualTo(IntOffset(outsideCoordinates.size.width - 30, 0))
            assertThat(insideCoordinates.size.width).isEqualTo(30)
            assertThat(insideCoordinates.size.height).isEqualTo(outsideCoordinates.size.height)
        }
    }

    @Test
    fun testFitOutsideBottom() {
        Assume.assumeTrue(ComposeUiFlags.areWindowInsetsRulersEnabled)
        lateinit var outsideCoordinates: LayoutCoordinates
        lateinit var insideCoordinates: LayoutCoordinates
        lateinit var view: View
        rule.setContent {
            view = LocalView.current.parent as View
            Box(Modifier.fillMaxSize().onPlaced { outsideCoordinates = it }) {
                Box(
                    Modifier.fillMaxSize().fitOutside(SystemBars.current).onPlaced {
                        insideCoordinates = it
                    }
                )
            }
        }
        rule.runOnIdle { view.onApplyWindowInsets(createInsets(0, 0, 0, 50).toWindowInsets()) }

        rule.runOnIdle {
            assertThat(insideCoordinates.positionInRoot().round())
                .isEqualTo(IntOffset(0, outsideCoordinates.size.height - 50))
            assertThat(insideCoordinates.size.width).isEqualTo(outsideCoordinates.size.width)
            assertThat(insideCoordinates.size.height).isEqualTo(50)
        }
    }

    @Test
    fun testFitOutsideNoInsets() {
        Assume.assumeTrue(ComposeUiFlags.areWindowInsetsRulersEnabled)
        lateinit var insideCoordinates: LayoutCoordinates
        lateinit var view: View
        rule.setContent {
            view = LocalView.current.parent as View
            Box(Modifier.fillMaxSize()) {
                Box(
                    Modifier.fillMaxSize().fitOutside(NavigationBars.current).onPlaced {
                        insideCoordinates = it
                    }
                )
            }
        }
        rule.runOnIdle { view.onApplyWindowInsets(createInsets(0, 0, 0, 0).toWindowInsets()) }

        rule.runOnIdle { assertThat(insideCoordinates.size).isEqualTo(IntSize.Zero) }
    }

    @Test
    fun testFitOutsideNoBounds() {
        Assume.assumeTrue(ComposeUiFlags.areWindowInsetsRulersEnabled)
        lateinit var insideCoordinates: LayoutCoordinates
        rule.setContent {
            Box(Modifier.fillMaxSize()) {
                val size = with(LocalDensity.current) { 3000.toDp() }
                Box(
                    Modifier.layout { measurable, constraints ->
                            val placeable = measurable.measure(Constraints())
                            layout(constraints.maxWidth, constraints.maxHeight) {
                                placeable.place(0, 0)
                            }
                        }
                        .onPlaced { insideCoordinates = it }
                        .fitOutside(StatusBars.current)
                        .size(size)
                )
            }
        }

        rule.runOnIdle { assertThat(insideCoordinates.size).isEqualTo(IntSize.Zero) }
    }

    private fun createInsets(left: Int, top: Int, right: Int, bottom: Int): WindowInsetsCompat {
        val builder = WindowInsetsCompat.Builder()

        builder.setInsets(Type.statusBars(), Insets.of(0, top, 0, 0))
        builder.setInsetsIgnoringVisibility(Type.statusBars(), Insets.of(0, top, 0, 0))
        builder.setVisible(Type.statusBars(), top != 0)

        builder.setInsets(Type.navigationBars(), Insets.of(left, 0, right, bottom))
        builder.setInsetsIgnoringVisibility(
            Type.navigationBars(),
            Insets.of(left, 0, right, bottom),
        )
        builder.setVisible(Type.navigationBars(), left != 0 || right != 0 || bottom != 0)

        return builder.build()
    }
}
