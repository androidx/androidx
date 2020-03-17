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

import android.app.Activity
import android.graphics.Bitmap
import android.os.Build
import androidx.compose.Composable
import androidx.compose.Composition
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.rule.ActivityTestRule
import androidx.ui.core.ContextAmbient
import androidx.ui.core.DensityAmbient
import androidx.ui.core.TestTag
import androidx.ui.core.setContent
import androidx.ui.foundation.Box
import androidx.ui.graphics.Color
import androidx.ui.graphics.vector.VectorAsset
import androidx.ui.graphics.vector.drawVector
import androidx.ui.layout.Center
import androidx.ui.layout.Column
import androidx.ui.layout.LayoutSize
import androidx.ui.res.vectorResource
import androidx.ui.semantics.Semantics
import androidx.ui.test.captureToBitmap
import androidx.ui.test.findByTag
import androidx.ui.unit.ipx
import com.google.common.truth.Truth
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import kotlin.reflect.jvm.javaGetter

const val ProgrammaticTestTag = "programmatic"
const val XmlTestTag = "Xml"

/**
 * Test to ensure equality (both structurally, and visually) between programmatically generated
 * Material [androidx.ui.material.icons.Icons] and their XML source.
 */
@LargeTest
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
@RunWith(JUnit4::class)
class IconComparisonTest {

    /**
     * Running all comparisons inside one test method instead of using a Parameterized test
     * runner so we can re-use the same Activity instance between test runs. Most of the cost of a
     * simple test like this is in Activity instantiation so re-using the same activity reduces time
     * to run this test ~tenfold.
     */
    @get:Rule
    val activityTestRule = ActivityTestRule(Activity::class.java)

    @Test
    fun compareVectorAssets() {
        AllIcons.forEach { (property, drawableName) ->
            var xmlVector: VectorAsset? = null
            val programmaticVector = property.get()
            var composition: Composition? = null

            activityTestRule.runOnUiThread {
                composition = activityTestRule.activity.setContent {
                    xmlVector = drawableName.toVectorAsset()
                    DrawVectors(programmaticVector, xmlVector!!)
                }
            }

            val iconName = property.javaGetter!!.declaringClass.canonicalName!!

            assertVectorAssetsAreEqual(xmlVector!!, programmaticVector, iconName)

            assertBitmapsAreEqual(
                findByTag(XmlTestTag).captureToBitmap(),
                findByTag(ProgrammaticTestTag).captureToBitmap(),
                iconName
            )

            // Dispose between composing each pair of icons to ensure correctness
            activityTestRule.runOnUiThread {
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
    Center {
        // Ideally these icons would be 24 dp, but due to density changes across devices we test
        // against in CI, on some devices using DP here causes there to be anti-aliasing issues.
        // Using ipx directly ensures that we will always have a consistent layout / drawing
        // story, so anti-aliasing should be identical.
        val layoutSize = with(DensityAmbient.current) {
            LayoutSize(72.ipx.toDp())
        }
        Column {
            TestTag(ProgrammaticTestTag) {
                Semantics(container = true) {
                    Box(
                        modifier = layoutSize +
                                drawVector(vectorImage = programmaticVector, tintColor = Color.Red)
                    )
                }
            }
            TestTag(XmlTestTag) {
                Semantics(container = true) {
                    Box(
                        modifier = layoutSize +
                                drawVector(vectorImage = xmlVector, tintColor = Color.Red)
                    )
                }
            }
        }
    }
}
