/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.glance.appwidget

import android.os.Bundle
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.LocalSize
import androidx.glance.text.EmittableText
import androidx.glance.text.Text
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertIs
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class SizeBoxTest {
    private val minSize = DpSize(50.dp, 100.dp)

    @Test
    fun sizeModeSingle() = runTest {
        val root = runTestingComposition {
            ForEachSize(SizeMode.Single, minSize) {
                val size = LocalSize.current
                Text("${size.width} x ${size.height}")
            }
        }
        val sizeBox = assertIs<EmittableSizeBox>(root.children.single())
        assertThat(sizeBox.size).isEqualTo(minSize)
        assertThat(sizeBox.sizeMode).isEqualTo(SizeMode.Single)
        val text = assertIs<EmittableText>(sizeBox.children.single())
        assertThat(text.text).isEqualTo("50.0.dp x 100.0.dp")
    }

    @Config(sdk = [30])
    @Test
    fun sizeModeExactPreS() = runTest {
        val options = optionsBundleOf(
            listOf(
                DpSize(100.dp, 50.dp),
                DpSize(50.dp, 100.dp),
                DpSize(75.dp, 75.dp),
            )
        )
        val root = runTestingComposition {
            CompositionLocalProvider(LocalAppWidgetOptions provides options) {
                ForEachSize(SizeMode.Exact, minSize) {
                    val size = LocalSize.current
                    Text("${size.width} x ${size.height}")
                }
            }
        }
        // On Pre-S, since AppWidgetManager.OPTION_APPWIDGET_SIZES isn't available, we use
        // AppWidgetManager.OPTION_APPWIDGET_{MIN,MAX}_{HEIGHT,WIDTH} to find the landscape and
        // portrait sizes.
        assertThat(root.children).hasSize(2)
        val sizeBox1 = assertIs<EmittableSizeBox>(root.children[0])
        assertThat(sizeBox1.size).isEqualTo(DpSize(100.dp, 50.dp))
        assertThat(sizeBox1.sizeMode).isEqualTo(SizeMode.Exact)
        val text1 = assertIs<EmittableText>(sizeBox1.children.single())
        assertThat(text1.text).isEqualTo("100.0.dp x 50.0.dp")

        val sizeBox2 = assertIs<EmittableSizeBox>(root.children[1])
        assertThat(sizeBox2.size).isEqualTo(DpSize(50.dp, 100.dp))
        assertThat(sizeBox2.sizeMode).isEqualTo(SizeMode.Exact)
        val text2 = assertIs<EmittableText>(sizeBox2.children.single())
        assertThat(text2.text).isEqualTo("50.0.dp x 100.0.dp")
    }

    @Config(sdk = [31])
    @Test
    fun sizeModeExactS() = runTest {
        val options = optionsBundleOf(
            listOf(
                DpSize(100.dp, 50.dp),
                DpSize(50.dp, 100.dp),
                DpSize(75.dp, 75.dp),
            )
        )
        val root = runTestingComposition {
            CompositionLocalProvider(LocalAppWidgetOptions provides options) {
                ForEachSize(SizeMode.Exact, minSize) {
                    val size = LocalSize.current
                    Text("${size.width} x ${size.height}")
                }
            }
        }
        // On S+, AppWidgetManager.OPTION_APPWIDGET_SIZES is available so we create a SizeBox for
        // each size.
        assertThat(root.children).hasSize(3)
        val sizeBox1 = assertIs<EmittableSizeBox>(root.children[0])
        assertThat(sizeBox1.size).isEqualTo(DpSize(100.dp, 50.dp))
        assertThat(sizeBox1.sizeMode).isEqualTo(SizeMode.Exact)
        val text1 = assertIs<EmittableText>(sizeBox1.children.single())
        assertThat(text1.text).isEqualTo("100.0.dp x 50.0.dp")

        val sizeBox2 = assertIs<EmittableSizeBox>(root.children[1])
        assertThat(sizeBox2.size).isEqualTo(DpSize(50.dp, 100.dp))
        assertThat(sizeBox2.sizeMode).isEqualTo(SizeMode.Exact)
        val text2 = assertIs<EmittableText>(sizeBox2.children.single())
        assertThat(text2.text).isEqualTo("50.0.dp x 100.0.dp")

        val sizeBox3 = assertIs<EmittableSizeBox>(root.children[2])
        assertThat(sizeBox3.size).isEqualTo(DpSize(75.dp, 75.dp))
        assertThat(sizeBox3.sizeMode).isEqualTo(SizeMode.Exact)
        val text3 = assertIs<EmittableText>(sizeBox3.children.single())
        assertThat(text3.text).isEqualTo("75.0.dp x 75.0.dp")
    }

    @Test
    fun sizeModeExactEmptySizes() = runTest {
        val options = Bundle()
        val root = runTestingComposition {
            CompositionLocalProvider(LocalAppWidgetOptions provides options) {
                ForEachSize(SizeMode.Exact, minSize) {
                    val size = LocalSize.current
                    Text("${size.width} x ${size.height}")
                }
            }
        }
        // When no sizes are available, a single SizeBox for minSize should be created
        assertThat(root.children).hasSize(1)
        val sizeBox1 = assertIs<EmittableSizeBox>(root.children[0])
        assertThat(sizeBox1.size).isEqualTo(minSize)
        assertThat(sizeBox1.sizeMode).isEqualTo(SizeMode.Exact)
        val text1 = assertIs<EmittableText>(sizeBox1.children.single())
        assertThat(text1.text).isEqualTo("50.0.dp x 100.0.dp")
    }

    @Config(sdk = [30])
    @Test
    fun sizeModeResponsivePreS() = runTest {
        val options = optionsBundleOf(
            listOf(
                DpSize(100.dp, 50.dp),
                DpSize(50.dp, 100.dp),
                DpSize(75.dp, 75.dp),
            )
        )
        val sizeMode = SizeMode.Responsive(
            setOf(
                DpSize(99.dp, 49.dp),
                DpSize(49.dp, 99.dp),
                DpSize(75.dp, 75.dp),
            )
        )
        val root = runTestingComposition {
            CompositionLocalProvider(LocalAppWidgetOptions provides options) {
                ForEachSize(sizeMode, minSize) {
                    val size = LocalSize.current
                    Text("${size.width} x ${size.height}")
                }
            }
        }
        // On Pre-S, we extract orientation sizes from
        // AppWidgetManager.OPTION_APPWIDGET_{MIN,MAX}_{HEIGHT,WIDTH} to find the landscape and
        // portrait sizes, then find which responsive size fits best for each.
        assertThat(root.children).hasSize(2)
        val sizeBox1 = assertIs<EmittableSizeBox>(root.children[0])
        assertThat(sizeBox1.size).isEqualTo(DpSize(99.dp, 49.dp))
        assertThat(sizeBox1.sizeMode).isEqualTo(sizeMode)
        val text1 = assertIs<EmittableText>(sizeBox1.children.single())
        assertThat(text1.text).isEqualTo("99.0.dp x 49.0.dp")

        val sizeBox2 = assertIs<EmittableSizeBox>(root.children[1])
        assertThat(sizeBox2.size).isEqualTo(DpSize(49.dp, 99.dp))
        assertThat(sizeBox2.sizeMode).isEqualTo(sizeMode)
        val text2 = assertIs<EmittableText>(sizeBox2.children.single())
        assertThat(text2.text).isEqualTo("49.0.dp x 99.0.dp")
    }

    @Config(sdk = [30])
    @Test
    fun sizeModeResponsiveUseSmallestSize() = runTest {
        val options = optionsBundleOf(
            listOf(
                DpSize(100.dp, 50.dp),
                DpSize(50.dp, 100.dp),
            )
        )
        val sizeMode = SizeMode.Responsive(
            setOf(
                DpSize(200.dp, 200.dp),
                DpSize(300.dp, 300.dp),
                DpSize(75.dp, 75.dp),
            )
        )
        val root = runTestingComposition {
            CompositionLocalProvider(LocalAppWidgetOptions provides options) {
                ForEachSize(sizeMode, minSize) {
                    val size = LocalSize.current
                    Text("${size.width} x ${size.height}")
                }
            }
        }
        // On Pre-S, we extract orientation sizes from
        // AppWidgetManager.OPTION_APPWIDGET_{MIN,MAX}_{HEIGHT,WIDTH} to find the landscape and
        // portrait sizes, then find which responsive size fits best for each. If none fits, then we
        // use the smallest size for both landscape and portrait.
        assertThat(root.children).hasSize(2)
        val sizeBox1 = assertIs<EmittableSizeBox>(root.children[0])
        assertThat(sizeBox1.size).isEqualTo(DpSize(75.dp, 75.dp))
        assertThat(sizeBox1.sizeMode).isEqualTo(sizeMode)
        val text1 = assertIs<EmittableText>(sizeBox1.children.single())
        assertThat(text1.text).isEqualTo("75.0.dp x 75.0.dp")

        val sizeBox2 = assertIs<EmittableSizeBox>(root.children[1])
        assertThat(sizeBox2.size).isEqualTo(DpSize(75.dp, 75.dp))
        assertThat(sizeBox2.sizeMode).isEqualTo(sizeMode)
        val text2 = assertIs<EmittableText>(sizeBox2.children.single())
        assertThat(text2.text).isEqualTo("75.0.dp x 75.0.dp")
    }

    @Config(sdk = [31])
    @Test
    fun sizeModeResponsiveS() = runTest {
        val sizeMode = SizeMode.Responsive(
            setOf(
                DpSize(100.dp, 50.dp),
                DpSize(50.dp, 100.dp),
                DpSize(75.dp, 75.dp),
            )
        )
        val root = runTestingComposition {
            ForEachSize(sizeMode, minSize) {
                val size = LocalSize.current
                Text("${size.width} x ${size.height}")
            }
        }
        // On S, we create a SizeBox for each given size.
        assertThat(root.children).hasSize(3)
        val sizeBox1 = assertIs<EmittableSizeBox>(root.children[0])
        assertThat(sizeBox1.size).isEqualTo(DpSize(100.dp, 50.dp))
        assertThat(sizeBox1.sizeMode).isEqualTo(sizeMode)
        val text1 = assertIs<EmittableText>(sizeBox1.children.single())
        assertThat(text1.text).isEqualTo("100.0.dp x 50.0.dp")

        val sizeBox2 = assertIs<EmittableSizeBox>(root.children[1])
        assertThat(sizeBox2.size).isEqualTo(DpSize(50.dp, 100.dp))
        assertThat(sizeBox2.sizeMode).isEqualTo(sizeMode)
        val text2 = assertIs<EmittableText>(sizeBox2.children.single())
        assertThat(text2.text).isEqualTo("50.0.dp x 100.0.dp")

        val sizeBox3 = assertIs<EmittableSizeBox>(root.children[2])
        assertThat(sizeBox3.size).isEqualTo(DpSize(75.dp, 75.dp))
        assertThat(sizeBox3.sizeMode).isEqualTo(sizeMode)
        val text3 = assertIs<EmittableText>(sizeBox3.children.single())
        assertThat(text3.text).isEqualTo("75.0.dp x 75.0.dp")
    }
}