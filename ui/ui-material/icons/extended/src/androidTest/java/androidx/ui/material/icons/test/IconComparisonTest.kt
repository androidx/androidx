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

package androidx.ui.material.icons.test

import android.graphics.Bitmap
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.compose.Composable
import androidx.compose.Composition
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.ui.core.Alignment
import androidx.ui.core.ContextAmbient
import androidx.ui.core.DensityAmbient
import androidx.ui.core.Modifier
import androidx.ui.core.paint
import androidx.ui.core.setContent
import androidx.ui.core.testTag
import androidx.ui.foundation.Box
import androidx.ui.graphics.Color
import androidx.ui.graphics.ColorFilter
import androidx.ui.graphics.vector.VectorAsset
import androidx.ui.graphics.vector.VectorPainter
import androidx.ui.layout.Row
import androidx.ui.layout.Stack
import androidx.ui.layout.preferredSize
import androidx.ui.res.vectorResource
import androidx.ui.test.android.AndroidComposeTestRule
import androidx.ui.test.captureToBitmap
import androidx.ui.test.findByTag
import androidx.ui.test.runOnUiThread
import androidx.ui.test.waitForIdle
import androidx.ui.unit.ipx
import com.google.common.truth.Truth
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.reflect.KProperty0
import kotlin.reflect.jvm.javaGetter

const val ProgrammaticTestTag = "programmatic"
const val XmlTestTag = "Xml"

/**
 * Test to ensure equality (both structurally, and visually) between programmatically generated
 * Material [androidx.ui.material.icons.Icons] and their XML source.
 */
@Suppress("unused")
@LargeTest
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
@RunWith(Parameterized::class)
class IconComparisonTest(
    private val iconSublist: List<Pair<KProperty0<VectorAsset>, String>>,
    private val debugParameterName: String
) {

    companion object {
        /**
         * Arbitrarily split [AllIcons] into equal parts. This is needed as one test with the
         * whole of [AllIcons] will exceed the timeout allowed for a test in CI, so we split it
         * up to stay under the limit.
         *
         * Additionally, we run large batches of comparisons per method, instead of one icon per
         * method, so that we can re-use the same Activity instance between test runs. Most of the
         * cost of a simple test like this is in Activity instantiation so re-using the same
         * activity reduces time to run this test ~tenfold.
         */
        @JvmStatic
        @Parameterized.Parameters(name = "{1}")
        fun initIconSublist(): Array<Array<Any>> {
            val numberOfChunks = 4
            val subLists = AllIcons.chunked(AllIcons.size / numberOfChunks)
            return subLists.mapIndexed { index, list ->
                arrayOf(list, "${index + 1}of$numberOfChunks")
            }.toTypedArray()
        }
    }

    @get:Rule
    val composeTestRule = AndroidComposeTestRule<ComponentActivity>()

    @Test
    fun compareVectorAssets() {
        iconSublist.forEach { (property, drawableName) ->
            var xmlVector: VectorAsset? = null
            val programmaticVector = property.get()
            var composition: Composition? = null

            runOnUiThread {
                composition = composeTestRule.activityTestRule.activity.setContent {
                    xmlVector = drawableName.toVectorAsset()
                    DrawVectors(programmaticVector, xmlVector!!)
                }
            }

            waitForIdle()

            val iconName = property.javaGetter!!.declaringClass.canonicalName!!

            assertVectorAssetsAreEqual(xmlVector!!, programmaticVector, iconName)

            assertBitmapsAreEqual(
                findByTag(XmlTestTag).captureToBitmap(),
                findByTag(ProgrammaticTestTag).captureToBitmap(),
                iconName
            )

            // Dispose between composing each pair of icons to ensure correctness
            runOnUiThread {
                composition?.dispose()
            }
        }
    }
}

/**
 * @return the [VectorAsset] matching the drawable with [this] name.
 */
@Composable
private fun String.toVectorAsset(): VectorAsset {
    val context = ContextAmbient.current
    val resId = context.resources.getIdentifier(this, "drawable", context.packageName)
    return vectorResource(resId)
}

/**
 * Compares two [VectorAsset]s and ensures that they are deeply equal, comparing all children
 * recursively.
 */
private fun assertVectorAssetsAreEqual(
    xmlVector: VectorAsset,
    programmaticVector: VectorAsset,
    iconName: String
) {
    try {
        Truth.assertThat(programmaticVector).isEqualTo(xmlVector)
    } catch (e: AssertionError) {
        val message = "VectorAsset comparison failed for $iconName\n" + e.localizedMessage
        throw AssertionError(message, e)
    }
}

/**
 * Compares each pixel in two bitmaps, asserting they are equal.
 */
private fun assertBitmapsAreEqual(xmlBitmap: Bitmap, programmaticBitmap: Bitmap, iconName: String) {
    try {
        Truth.assertThat(programmaticBitmap.width).isEqualTo(xmlBitmap.width)
        Truth.assertThat(programmaticBitmap.height).isEqualTo(xmlBitmap.height)

        val xmlPixelArray = with(xmlBitmap) {
            val pixels = IntArray(width * height)
            getPixels(pixels, 0, width, 0, 0, width, height)
            pixels
        }

        val programmaticPixelArray = with(programmaticBitmap) {
            val pixels = IntArray(width * height)
            getPixels(pixels, 0, width, 0, 0, width, height)
            pixels
        }

        Truth.assertThat(programmaticPixelArray).isEqualTo(xmlPixelArray)
    } catch (e: AssertionError) {
        val message = "Bitmap comparison failed for $iconName\n" + e.localizedMessage
        throw AssertionError(message, e)
    }
}

/**
 * Renders both vectors in a column using the corresponding [ProgrammaticTestTag] and
 * [XmlTestTag] for [programmaticVector] and [xmlVector].
 */
@Composable
private fun DrawVectors(programmaticVector: VectorAsset, xmlVector: VectorAsset) {
    Stack {
        // Ideally these icons would be 24 dp, but due to density changes across devices we test
        // against in CI, on some devices using DP here causes there to be anti-aliasing issues.
        // Using ipx directly ensures that we will always have a consistent layout / drawing
        // story, so anti-aliasing should be identical.
        val layoutSize = with(DensityAmbient.current) {
            Modifier.preferredSize(72.ipx.toDp())
        }
        Row(Modifier.gravity(Alignment.Center)) {
            Box(
                modifier = layoutSize.paint(
                    VectorPainter(programmaticVector),
                    colorFilter = ColorFilter.tint(Color.Red)
                ).testTag(ProgrammaticTestTag)
            )
            Box(
                modifier = layoutSize.paint(
                    VectorPainter(xmlVector),
                    colorFilter = ColorFilter.tint(Color.Red)
                ).testTag(XmlTestTag)
            )
        }
    }
}
