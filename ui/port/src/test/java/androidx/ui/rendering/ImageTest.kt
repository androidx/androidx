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

import androidx.ui.async.Timer
import androidx.ui.foundation.diagnostics.DiagnosticLevel
import androidx.ui.matchers.EqualsIgnoringHashCodes
import androidx.ui.matchers.HasGoodToStringDeep
import androidx.ui.matchers.MoreOrLessEquals
import androidx.ui.painting.BlendMode
import androidx.ui.rendering.box.BoxConstraints
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.test.TestCoroutineContext
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.Test

class ImageTest {

    private lateinit var job: Job

    @Before
    fun setup() {
        job = Job()
        Timer.scope = CoroutineScope(TestCoroutineContext() + job)
    }

    @After
    fun teardown() {
        job.cancel()
    }

    @Test
    fun `Image sizing`() {
        val squareImage = mockImage(10, 10)
        val wideImage = mockImage(20, 10)
        val tallImage = mockImage(10, 20)

        var image = RenderImage(image = squareImage)
        layout(image,
                constraints = BoxConstraints(
                minWidth = 25.0f,
        minHeight = 25.0f,
        maxWidth = 100.0f,
        maxHeight = 100.0f))
        assertThat(image.size.width, MoreOrLessEquals(25.0f))
        assertThat(image.size.height, MoreOrLessEquals(25.0f))

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
                minWidth = 5.0f,
        minHeight = 30.0f,
        maxWidth = 100.0f,
        maxHeight = 100.0f))
        assertThat(image.size.width, MoreOrLessEquals(60.0f))
        assertThat(image.size.height, MoreOrLessEquals(30.0f))

        image = RenderImage(image = tallImage)
        layout(image,
                constraints = BoxConstraints(
                minWidth = 50.0f,
        minHeight = 5.0f,
        maxWidth = 75.0f,
        maxHeight = 75.0f))
        assertThat(image.size.width, MoreOrLessEquals(50.0f))
        assertThat(image.size.height, MoreOrLessEquals(75.0f))

        image = RenderImage(image = wideImage)
        layout(image,
                constraints = BoxConstraints(
                minWidth = 5.0f,
        minHeight = 5.0f,
        maxWidth = 100.0f,
        maxHeight = 100.0f))
        assertThat(image.size.width, MoreOrLessEquals(20.0f))
        assertThat(image.size.height, MoreOrLessEquals(10.0f))

        image = RenderImage(image = wideImage)
        layout(image,
                constraints = BoxConstraints(
                minWidth = 5.0f,
        minHeight = 5.0f,
        maxWidth = 16.0f,
        maxHeight = 16.0f))
        assertThat(image.size.width, MoreOrLessEquals(16.0f))
        assertThat(image.size.height, MoreOrLessEquals(8.0f))

        image = RenderImage(image = tallImage)
        layout(image,
                constraints = BoxConstraints(
                minWidth = 5.0f,
        minHeight = 5.0f,
        maxWidth = 16.0f,
        maxHeight = 16.0f))
        assertThat(image.size.width, MoreOrLessEquals(8.0f))
        assertThat(image.size.height, MoreOrLessEquals(16.0f))

        image = RenderImage(image = squareImage)
        layout(image,
                constraints = BoxConstraints(
                minWidth = 4.0f,
        minHeight = 4.0f,
        maxWidth = 8.0f,
        maxHeight = 8.0f))
        assertThat(image.size.width, MoreOrLessEquals(8.0f))
        assertThat(image.size.height, MoreOrLessEquals(8.0f))

        image = RenderImage(image = wideImage)
        layout(image,
                constraints = BoxConstraints(
                minWidth = 20.0f,
        minHeight = 20.0f,
        maxWidth = 30.0f,
        maxHeight = 30.0f))
        assertThat(image.size.width, MoreOrLessEquals(30.0f))
        assertThat(image.size.height, MoreOrLessEquals(20.0f))

        image = RenderImage(image = tallImage)
        layout(image,
                constraints = BoxConstraints(
                minWidth = 20.0f,
        minHeight = 20.0f,
        maxWidth = 30.0f,
        maxHeight = 30.0f))
        assertThat(image.size.width, MoreOrLessEquals(20.0f))
        assertThat(image.size.height, MoreOrLessEquals(30.0f))
    }

    @Test
    fun `Null image sizing`() {
        var image = RenderImage()
        layout(image,
                constraints = BoxConstraints(
                minWidth = 25.0f,
        minHeight = 25.0f,
        maxWidth = 100.0f,
        maxHeight = 100.0f))
        assertThat(image.size.width, MoreOrLessEquals(25.0f))
        assertThat(image.size.height, MoreOrLessEquals(25.0f))

        image = RenderImage(width = 50.0f)
        layout(image,
                constraints = BoxConstraints(
                minWidth = 25.0f,
        minHeight = 25.0f,
        maxWidth = 100.0f,
        maxHeight = 100.0f))
        assertThat(image.size.width, MoreOrLessEquals(50.0f))
        assertThat(image.size.height, MoreOrLessEquals(25.0f))

        image = RenderImage(height = 50.0f)
        layout(image,
                constraints = BoxConstraints(
                minWidth = 25.0f,
        minHeight = 25.0f,
        maxWidth = 100.0f,
        maxHeight = 100.0f))
        assertThat(image.size.width, MoreOrLessEquals(25.0f))
        assertThat(image.size.height, MoreOrLessEquals(50.0f))

        image = RenderImage(width = 100.0f, height = 100.0f)
        layout(image,
                constraints = BoxConstraints(
                minWidth = 25.0f,
        minHeight = 25.0f,
        maxWidth = 75.0f,
        maxHeight = 75.0f))
        assertThat(image.size.width, MoreOrLessEquals(75.0f))
        assertThat(image.size.height, MoreOrLessEquals(75.0f))
    }

    @Test
    fun `update image colorBlendMode`() {
        val image = RenderImage()
        assertNull(image.colorBlendMode)
        image.colorBlendMode = BlendMode.color
        assertEquals(image.colorBlendMode, BlendMode.color)
    }
}