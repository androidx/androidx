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
    private val minAppWidgetSize = DpSize(50.dp, 100.dp)

    @Test
    fun sizeModeSingle_usesMinAppWidgetSize() = runTest {
        val root = runTestingComposition {
            ForEachSize(SizeMode.Single, minAppWidgetSize) {
                val size = LocalSize.current
                Text(size.toSizeString())
            }
        }

        val sizeBox = assertIs<EmittableSizeBox>(root.children.single())
        assertThat(sizeBox.size).isEqualTo(minAppWidgetSize)
        assertThat(sizeBox.sizeMode).isEqualTo(SizeMode.Single)
        val text = assertIs<EmittableText>(sizeBox.children.single())
        assertThat(text.text).isEqualTo(minAppWidgetSize.toSizeString())
    }

    @Config(maxSdk = 30)
    @Test
    fun sizeModeExact_onlyMinMaxSizes_usesOrientationSizesDerivedFromMinMax() = runTest {
        val displaySizes = listOf(
            DpSize(100.dp, 50.dp),
            DpSize(50.dp, 100.dp),
            DpSize(75.dp, 75.dp),
        )
        // Following utility function populates only
        // AppWidgetManager.OPTION_APPWIDGET_{MIN,MAX}_{HEIGHT,WIDTH} to mimic Pre-S behavior, so
        // actual possible sizes aren't available.
        val options = optionsBundleOf(displaySizes)

        val root = runTestingComposition {
            CompositionLocalProvider(LocalAppWidgetOptions provides options) {
                ForEachSize(SizeMode.Exact, minAppWidgetSize) {
                    val size = LocalSize.current
                    Text(size.toSizeString())
                }
            }
        }

        // On Pre-S, since AppWidgetManager.OPTION_APPWIDGET_SIZES isn't available, we use
        // AppWidgetManager.OPTION_APPWIDGET_{MIN,MAX}_{HEIGHT,WIDTH} to find the landscape and
        // portrait sizes.
        assertThat(root.children).hasSize(2)
        val maxWidthMinHeightSize = DpSize(100.dp, 50.dp) // Landscape
        val sizeBox1 = assertIs<EmittableSizeBox>(root.children[0])
        assertThat(sizeBox1.size).isEqualTo(maxWidthMinHeightSize)
        assertThat(sizeBox1.sizeMode).isEqualTo(SizeMode.Exact)
        val text1 = assertIs<EmittableText>(sizeBox1.children.single())
        assertThat(text1.text).isEqualTo(maxWidthMinHeightSize.toSizeString())

        val minWidthMaxHeightSize = DpSize(50.dp, 100.dp) // Portrait
        val sizeBox2 = assertIs<EmittableSizeBox>(root.children[1])
        assertThat(sizeBox2.size).isEqualTo(minWidthMaxHeightSize)
        assertThat(sizeBox2.sizeMode).isEqualTo(SizeMode.Exact)
        val text2 = assertIs<EmittableText>(sizeBox2.children.single())
        assertThat(text2.text).isEqualTo(minWidthMaxHeightSize.toSizeString())
    }

    @Config(minSdk = 31)
    @Test
    fun sizeModeExact_possibleSizesAvailable_usesEachDistinctPossibleSize() {
        runTest {
            val displaySizes = listOf(
                DpSize(100.dp, 50.dp), // duplicate for testing
                DpSize(50.dp, 100.dp),
                DpSize(75.dp, 75.dp),
                DpSize(100.dp, 50.dp),
            )
            val distinctDisplaySizes = displaySizes.distinct() // distinct maintains order.
            // In S+, following utility function populates
            // AppWidgetManager.OPTION_APPWIDGET_OPTIONS with given sizes.
            val options = optionsBundleOf(displaySizes)

            val root = runTestingComposition {
                CompositionLocalProvider(LocalAppWidgetOptions provides options) {
                    ForEachSize(SizeMode.Exact, minAppWidgetSize) {
                        val size = LocalSize.current
                        Text(size.toSizeString())
                    }
                }
            }

            // On S+, AppWidgetManager.OPTION_APPWIDGET_SIZES is available so we create a SizeBox
            // for each size.
            assertThat(root.children).hasSize(distinctDisplaySizes.size)
            distinctDisplaySizes.forEachIndexed { index, dpSize ->
                val sizeBox = assertIs<EmittableSizeBox>(root.children[index])
                assertThat(sizeBox.size).isEqualTo(dpSize)
                assertThat(sizeBox.sizeMode).isEqualTo(SizeMode.Exact)
                val text = assertIs<EmittableText>(sizeBox.children.single())
                assertThat(text.text).isEqualTo(dpSize.toSizeString())
            }
        }
    }

    @Test
    fun sizeModeExact_emptySizes_usesMinAppWidgetSize() = runTest {
        val options = Bundle()

        val root = runTestingComposition {
            CompositionLocalProvider(LocalAppWidgetOptions provides options) {
                ForEachSize(SizeMode.Exact, minAppWidgetSize) {
                    val size = LocalSize.current
                    Text(size.toSizeString())
                }
            }
        }

        // When no sizes are available, a single SizeBox for minSize should be created
        assertThat(root.children).hasSize(1)
        val sizeBox = assertIs<EmittableSizeBox>(root.children[0])
        assertThat(sizeBox.size).isEqualTo(minAppWidgetSize)
        assertThat(sizeBox.sizeMode).isEqualTo(SizeMode.Exact)
        val text = assertIs<EmittableText>(sizeBox.children.single())
        assertThat(text.text).isEqualTo(minAppWidgetSize.toSizeString())
    }

    @Config(maxSdk = 30)
    @Test
    fun sizeModeResponsive_onlyMinMaxSizes_usesBestFitsFromInputResponsiveSizes() {
        runTest {
            val displaySizes = listOf(
                DpSize(100.dp, 50.dp),
                DpSize(50.dp, 100.dp),
                DpSize(75.dp, 75.dp),
            )
            val responsiveSizes = setOf(
                DpSize(99.dp, 49.dp),
                DpSize(49.dp, 99.dp),
                DpSize(75.dp, 75.dp),
            )
            // Following utility function populates only
            // AppWidgetManager.OPTION_APPWIDGET_{MIN,MAX}_{HEIGHT,WIDTH} to mimic Pre-S behavior,
            // so actual possible sizes aren't available.
            val options = optionsBundleOf(displaySizes)
            val sizeMode = SizeMode.Responsive(responsiveSizes)

            val root = runTestingComposition {
                CompositionLocalProvider(LocalAppWidgetOptions provides options) {
                    ForEachSize(sizeMode, minAppWidgetSize) {
                        val size = LocalSize.current
                        Text(size.toSizeString())
                    }
                }
            }

            // On Pre-S, we extract orientation sizes from
            // AppWidgetManager.OPTION_APPWIDGET_{MIN,MAX}_{HEIGHT,WIDTH} to find the landscape and
            // portrait sizes, then find which responsive size fits best for each.
            assertThat(root.children).hasSize(2)
            val bestLandscapeFit = DpSize(99.dp, 49.dp)
            val sizeBox1 = assertIs<EmittableSizeBox>(root.children[0])
            assertThat(sizeBox1.size).isEqualTo(bestLandscapeFit)
            assertThat(sizeBox1.sizeMode).isEqualTo(sizeMode)
            val text1 = assertIs<EmittableText>(sizeBox1.children.single())
            assertThat(text1.text).isEqualTo(bestLandscapeFit.toSizeString())

            val bestPortraitFit = DpSize(49.dp, 99.dp)
            val sizeBox2 = assertIs<EmittableSizeBox>(root.children[1])
            assertThat(sizeBox2.size).isEqualTo(bestPortraitFit)
            assertThat(sizeBox2.sizeMode).isEqualTo(sizeMode)
            val text2 = assertIs<EmittableText>(sizeBox2.children.single())
            assertThat(text2.text).isEqualTo(bestPortraitFit.toSizeString())
        }
    }

    @Config(maxSdk = 30)
    @Test
    fun responsive_onlyMinMaxSizesAndNoFit_usesMinFromInputResponsiveSizes() = runTest {
        val displaySizes = listOf(
            DpSize(100.dp, 50.dp),
            DpSize(50.dp, 100.dp),
        )
        val responsiveSizes = setOf(
            DpSize(200.dp, 200.dp),
            DpSize(300.dp, 300.dp),
            DpSize(75.dp, 75.dp),
        )
        // Following utility function populates only
        // AppWidgetManager.OPTION_APPWIDGET_{MIN,MAX}_{HEIGHT,WIDTH} to mimic Pre-S behavior,
        // so actual possible sizes aren't available.
        val options = optionsBundleOf(displaySizes)
        val sizeMode = SizeMode.Responsive(responsiveSizes)
        val minResponsiveSize = DpSize(75.dp, 75.dp)

        val root = runTestingComposition {
            CompositionLocalProvider(LocalAppWidgetOptions provides options) {
                ForEachSize(sizeMode, minResponsiveSize) {
                    val size = LocalSize.current
                    Text(size.toSizeString())
                }
            }
        }

        // On Pre-S, we extract orientation sizes from
        // AppWidgetManager.OPTION_APPWIDGET_{MIN,MAX}_{HEIGHT,WIDTH} to find the landscape and
        // portrait sizes, then find which responsive size fits best for each. If none fits, then we
        // use the smallest size for both landscape and portrait - and since same size is used for
        // both, we effectively compose for single size.
        assertThat(root.children).hasSize(1)
        val sizeBox = assertIs<EmittableSizeBox>(root.children[0])
        assertThat(sizeBox.size).isEqualTo(minResponsiveSize)
        assertThat(sizeBox.sizeMode).isEqualTo(sizeMode)
        val text = assertIs<EmittableText>(sizeBox.children.single())
        assertThat(text.text).isEqualTo(minResponsiveSize.toSizeString())
    }

    @Config(minSdk = 31)
    @Test
    fun sizeModeResponsive_usesEachResponsiveInputSize() {
        runTest {
            val responsiveSizes = setOf(
                DpSize(100.dp, 50.dp),
                DpSize(50.dp, 100.dp),
                DpSize(75.dp, 75.dp),
            )
            val sizeMode = SizeMode.Responsive(responsiveSizes)

            val root = runTestingComposition {
                ForEachSize(sizeMode, minAppWidgetSize) {
                    val size = LocalSize.current
                    Text(size.toSizeString())
                }
            }

            // On S, we create a SizeBox for each given size.
            assertThat(root.children).hasSize(responsiveSizes.size)
            responsiveSizes.forEachIndexed { index, dpSize ->
                val sizeBox = assertIs<EmittableSizeBox>(root.children[index])
                assertThat(sizeBox.size).isEqualTo(dpSize)
                assertThat(sizeBox.sizeMode).isEqualTo(sizeMode)
                val text = assertIs<EmittableText>(sizeBox.children.single())
                assertThat(text.text).isEqualTo(dpSize.toSizeString())
            }
        }
    }
}
