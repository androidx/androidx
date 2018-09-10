/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.ui.rendering

import androidx.ui.foundation.diagnostics.DiagnosticLevel
import androidx.ui.matchers.EqualsIgnoringHashCodes
import androidx.ui.matchers.HasGoodToStringDeep
import androidx.ui.matchers.MoreOrLessEquals
import androidx.ui.painting.BlendMode
import androidx.ui.rendering.box.BoxConstraints
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThat
import org.junit.Test

class ImageTest {

    @Test
    fun `Image sizing`() {
        val squareImage = mockImage(10, 10)
        val wideImage = mockImage(20, 10)
        val tallImage = mockImage(10, 20)

        var image = RenderImage(image = squareImage)
        layout(image,
                constraints = BoxConstraints(
                minWidth = 25.0,
        minHeight = 25.0,
        maxWidth = 100.0,
        maxHeight = 100.0))
        assertThat(image.size.width, MoreOrLessEquals(25.0))
        assertThat(image.size.height, MoreOrLessEquals(25.0))

        assertThat(image, HasGoodToStringDeep)
        assertThat(
                image.toStringDeep(minLevel = DiagnosticLevel.info),
                EqualsIgnoringHashCodes(
                "RenderImage#00000 relayoutBoundary=up2 NEEDS-PAINT\n" +
                "   parentData: <none> (can use size)\n" +
        "   constraints: BoxConstraints(25.0<=w<=100.0, 25.0<=h<=100.0)\n" +
        "   size: Size(25.0, 25.0)\n" +
        "   image: 10 * 10\n" +
        "   alignment: center\n"
        )
        )

        image = RenderImage(image = wideImage)
        layout(image,
                constraints = BoxConstraints(
                minWidth = 5.0,
        minHeight = 30.0,
        maxWidth = 100.0,
        maxHeight = 100.0))
        assertThat(image.size.width, MoreOrLessEquals(60.0))
        assertThat(image.size.height, MoreOrLessEquals(30.0))

        image = RenderImage(image = tallImage)
        layout(image,
                constraints = BoxConstraints(
                minWidth = 50.0,
        minHeight = 5.0,
        maxWidth = 75.0,
        maxHeight = 75.0))
        assertThat(image.size.width, MoreOrLessEquals(50.0))
        assertThat(image.size.height, MoreOrLessEquals(75.0))

        image = RenderImage(image = wideImage)
        layout(image,
                constraints = BoxConstraints(
                minWidth = 5.0,
        minHeight = 5.0,
        maxWidth = 100.0,
        maxHeight = 100.0))
        assertThat(image.size.width, MoreOrLessEquals(20.0))
        assertThat(image.size.height, MoreOrLessEquals(10.0))

        image = RenderImage(image = wideImage)
        layout(image,
                constraints = BoxConstraints(
                minWidth = 5.0,
        minHeight = 5.0,
        maxWidth = 16.0,
        maxHeight = 16.0))
        assertThat(image.size.width, MoreOrLessEquals(16.0))
        assertThat(image.size.height, MoreOrLessEquals(8.0))

        image = RenderImage(image = tallImage)
        layout(image,
                constraints = BoxConstraints(
                minWidth = 5.0,
        minHeight = 5.0,
        maxWidth = 16.0,
        maxHeight = 16.0))
        assertThat(image.size.width, MoreOrLessEquals(8.0))
        assertThat(image.size.height, MoreOrLessEquals(16.0))

        image = RenderImage(image = squareImage)
        layout(image,
                constraints = BoxConstraints(
                minWidth = 4.0,
        minHeight = 4.0,
        maxWidth = 8.0,
        maxHeight = 8.0))
        assertThat(image.size.width, MoreOrLessEquals(8.0))
        assertThat(image.size.height, MoreOrLessEquals(8.0))

        image = RenderImage(image = wideImage)
        layout(image,
                constraints = BoxConstraints(
                minWidth = 20.0,
        minHeight = 20.0,
        maxWidth = 30.0,
        maxHeight = 30.0))
        assertThat(image.size.width, MoreOrLessEquals(30.0))
        assertThat(image.size.height, MoreOrLessEquals(20.0))

        image = RenderImage(image = tallImage)
        layout(image,
                constraints = BoxConstraints(
                minWidth = 20.0,
        minHeight = 20.0,
        maxWidth = 30.0,
        maxHeight = 30.0))
        assertThat(image.size.width, MoreOrLessEquals(20.0))
        assertThat(image.size.height, MoreOrLessEquals(30.0))
    }

    @Test
    fun `Null image sizing`() {
        var image = RenderImage()
        layout(image,
                constraints = BoxConstraints(
                minWidth = 25.0,
        minHeight = 25.0,
        maxWidth = 100.0,
        maxHeight = 100.0))
        assertThat(image.size.width, MoreOrLessEquals(25.0))
        assertThat(image.size.height, MoreOrLessEquals(25.0))

        image = RenderImage(width = 50.0)
        layout(image,
                constraints = BoxConstraints(
                minWidth = 25.0,
        minHeight = 25.0,
        maxWidth = 100.0,
        maxHeight = 100.0))
        assertThat(image.size.width, MoreOrLessEquals(50.0))
        assertThat(image.size.height, MoreOrLessEquals(25.0))

        image = RenderImage(height = 50.0)
        layout(image,
                constraints = BoxConstraints(
                minWidth = 25.0,
        minHeight = 25.0,
        maxWidth = 100.0,
        maxHeight = 100.0))
        assertThat(image.size.width, MoreOrLessEquals(25.0))
        assertThat(image.size.height, MoreOrLessEquals(50.0))

        image = RenderImage(width = 100.0, height = 100.0)
        layout(image,
                constraints = BoxConstraints(
                minWidth = 25.0,
        minHeight = 25.0,
        maxWidth = 75.0,
        maxHeight = 75.0))
        assertThat(image.size.width, MoreOrLessEquals(75.0))
        assertThat(image.size.height, MoreOrLessEquals(75.0))
    }

    @Test
    fun `update image colorBlendMode`() {
        val image = RenderImage()
        assertNull(image.colorBlendMode)
        image.colorBlendMode = BlendMode.color
        assertEquals(image.colorBlendMode, BlendMode.color)
    }
}