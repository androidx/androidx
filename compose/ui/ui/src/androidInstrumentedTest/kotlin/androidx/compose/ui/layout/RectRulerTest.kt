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
package androidx.compose.ui.layout

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// There is a bug on x86 L emulators where 35f == NaN is true
@MediumTest
@RunWith(AndroidJUnit4::class)
class RectRulerTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun defaultRectRulers() {
        val rectRulers = RectRulers()
        lateinit var rect: Rect
        rule.setContent {
            Box(
                Modifier.fillMaxSize()
                    .layout { m, c ->
                        val p = m.measure(c)
                        layout(
                            p.width,
                            p.height,
                            rulers = {
                                rectRulers.left provides 10f
                                rectRulers.top provides 20f
                                rectRulers.right provides 30f
                                rectRulers.bottom provides 40f
                            },
                        ) {
                            p.place(0, 0)
                        }
                    }
                    .layout { m, c ->
                        val p = m.measure(c)
                        layout(p.width, p.height) {
                            rect =
                                Rect(
                                    rectRulers.left.current(0f),
                                    rectRulers.top.current(0f),
                                    rectRulers.right.current(0f),
                                    rectRulers.bottom.current(0f),
                                )
                            p.place(0, 0)
                        }
                    }
            )
        }
        rule.runOnIdle {
            assertThat(rect.left).isEqualTo(10f)
            assertThat(rect.top).isEqualTo(20f)
            assertThat(rect.right).isEqualTo(30f)
            assertThat(rect.bottom).isEqualTo(40f)
        }
    }

    @Test
    fun innerRectRulers() {
        val rectRulers1 = RectRulers()
        val rectRulers2 = RectRulers()
        val mergedRulers = RectRulers.innermostOf(rectRulers1, rectRulers2)
        lateinit var rect: Rect
        rule.setContent {
            Box(
                Modifier.fillMaxSize()
                    .layout { m, c ->
                        val p = m.measure(c)
                        layout(
                            p.width,
                            p.height,
                            rulers = {
                                rectRulers1.left provides 10f
                                rectRulers1.top provides 20f
                                rectRulers1.right provides 30f
                                rectRulers1.bottom provides 40f

                                rectRulers2.left provides 1f
                                rectRulers2.top provides 55f
                                rectRulers2.right provides 25f
                                rectRulers2.bottom provides 100f
                            },
                        ) {
                            p.place(0, 0)
                        }
                    }
                    .layout { m, c ->
                        val p = m.measure(c)
                        layout(p.width, p.height) {
                            rect =
                                Rect(
                                    mergedRulers.left.current(0f),
                                    mergedRulers.top.current(0f),
                                    mergedRulers.right.current(0f),
                                    mergedRulers.bottom.current(0f),
                                )
                            p.place(0, 0)
                        }
                    }
            )
        }
        rule.runOnIdle {
            assertThat(rect.left).isEqualTo(10f)
            assertThat(rect.top).isEqualTo(55f)
            assertThat(rect.right).isEqualTo(25f)
            assertThat(rect.bottom).isEqualTo(40f)
        }
    }

    @Test
    fun outerRectRulers() {
        val rectRulers1 = RectRulers()
        val rectRulers2 = RectRulers()
        val mergedRulers = RectRulers.outermostOf(rectRulers1, rectRulers2)
        lateinit var rect: Rect
        rule.setContent {
            Box(
                Modifier.fillMaxSize()
                    .layout { m, c ->
                        val p = m.measure(c)
                        layout(
                            p.width,
                            p.height,
                            rulers = {
                                rectRulers1.left provides 10f
                                rectRulers1.top provides 20f
                                rectRulers1.right provides 30f
                                rectRulers1.bottom provides 40f

                                rectRulers2.left provides 1f
                                rectRulers2.top provides 55f
                                rectRulers2.right provides 25f
                                rectRulers2.bottom provides 100f
                            },
                        ) {
                            p.place(0, 0)
                        }
                    }
                    .layout { m, c ->
                        val p = m.measure(c)
                        layout(p.width, p.height) {
                            rect =
                                Rect(
                                    mergedRulers.left.current(0f),
                                    mergedRulers.top.current(0f),
                                    mergedRulers.right.current(0f),
                                    mergedRulers.bottom.current(0f),
                                )
                            p.place(0, 0)
                        }
                    }
            )
        }
        rule.runOnIdle {
            assertThat(rect.left).isEqualTo(1f)
            assertThat(rect.top).isEqualTo(20f)
            assertThat(rect.right).isEqualTo(30f)
            assertThat(rect.bottom).isEqualTo(100f)
        }
    }
}
