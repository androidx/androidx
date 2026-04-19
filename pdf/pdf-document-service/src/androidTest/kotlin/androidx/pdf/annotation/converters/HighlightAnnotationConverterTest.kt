/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.pdf.annotation.converters

import android.graphics.Color
import android.graphics.RectF
import android.graphics.pdf.component.HighlightAnnotation as AospHighlightAnnotation
import android.os.Build
import androidx.annotation.RequiresExtension
import androidx.pdf.annotation.models.HighlightAnnotation
import androidx.pdf.utils.isAnnotationsFeatureAvailable
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RequiresExtension(extension = Build.VERSION_CODES.S, version = 18)
@RunWith(AndroidJUnit4::class)
class HighlightAnnotationConverterTest {
    private lateinit var highlightAnnotationConverter: HighlightAnnotationConverter
    private lateinit var aospHighlightAnnotationConverter: AospHighlightAnnotationConverter

    @Before
    fun setUp() {
        highlightAnnotationConverter = HighlightAnnotationConverter()
        aospHighlightAnnotationConverter = AospHighlightAnnotationConverter()
    }

    @Test
    fun test_convertJetpackHighlightToAospHighlight() {
        if (!isAnnotationsFeatureAvailable()) return

        val bounds = listOf(RectF(10f, 10f, 20f, 20f), RectF(30f, 30f, 40f, 40f))
        val color = Color.GREEN
        val highlightAnnotation = HighlightAnnotation(0, bounds, color)
        val aospHighlightAnnotation = highlightAnnotationConverter.convert(highlightAnnotation)
        assertThat(aospHighlightAnnotation.boundsList).isEqualTo(bounds)
        assertThat(aospHighlightAnnotation.color).isEqualTo(color)
    }

    @Test
    fun test_convertAospHighlightToJetpackHighlight() {
        if (!isAnnotationsFeatureAvailable()) return

        val bounds = listOf(RectF(10f, 10f, 20f, 20f), RectF(30f, 30f, 40f, 40f))
        val highlightColor = Color.GREEN
        val aospHighlightAnnotation = AospHighlightAnnotation(bounds)
        aospHighlightAnnotation.color = highlightColor
        val jetpackHighlightAnnotation =
            aospHighlightAnnotationConverter.convert(aospHighlightAnnotation, 0)
        assertThat(jetpackHighlightAnnotation.bounds).isEqualTo(bounds)
        assertThat(jetpackHighlightAnnotation.color).isEqualTo(highlightColor)
    }

    @Test
    fun test_convertAospHighlight_throwsExceptionOnInvalidType() {
        if (!isAnnotationsFeatureAvailable()) return

        val bounds = listOf(RectF(10f, 10f, 20f, 20f))
        val aospHighlightAnnotation = AospHighlightAnnotation(bounds)

        var exceptionThrown = false
        try {
            aospHighlightAnnotationConverter.convert(aospHighlightAnnotation, "invalid")
        } catch (e: IllegalArgumentException) {
            exceptionThrown = true
        }
        assertThat(exceptionThrown).isTrue()
    }
}
