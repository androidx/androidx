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

package androidx.compose.foundation.internal

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import com.google.common.truth.Truth.assertThat
import java.awt.Image
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.StringSelection
import java.awt.datatransfer.Transferable
import java.awt.datatransfer.UnsupportedFlavorException
import java.awt.image.BufferedImage
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@OptIn(ExperimentalComposeUiApi::class)
@RunWith(JUnit4::class)
class ClipboardUtilsTest {

    @Test
    fun `should not say a ClipEntry contains plain text when it doesn't`() {
        val image = BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB)
        val clipEntry = ClipEntry(ImageTransferable(image))
        assertThat(clipEntry.hasText()).isFalse()
    }

    @Test
    fun `should say a ClipEntry contains plain text when it only contains plain text`() {
        val clipEntry = ClipEntry(StringSelection("Hello"))
        assertThat(clipEntry.hasText()).isTrue()
    }

    @Test
    fun `should say a ClipEntry contains plain text when it is an AnnotatedStringTransferable`() {
        val clipEntry = ClipEntry(AnnotatedStringTransferable(AnnotatedString("Hello")))
        assertThat(clipEntry.hasText()).isTrue()
    }

    @Test
    fun `should not say a ClipEntry contains an AnnotatedString when it doesn't contain any text`() {
        val image = BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB)
        val clipEntry = ClipEntry(ImageTransferable(image))
        assertThat(clipEntry.hasAnnotatedString()).isFalse()
    }

    @Test
    fun `should not say a ClipEntry contains an AnnotatedString when it contains only plain text`() {
        val clipEntry = ClipEntry(StringSelection("Hello"))
        assertThat(clipEntry.hasAnnotatedString()).isFalse()
    }

    @Test
    fun `should say a ClipEntry contains an AnnotatedString when it does`() {
        val clipEntry = ClipEntry(AnnotatedStringTransferable(AnnotatedString("Hello")))
        assertThat(clipEntry.hasAnnotatedString()).isTrue()
    }

    @Test
    fun `should return null when trying to read plain text from a ClipEntry with no text`() {
        val image = BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB)
        val clipEntry = ClipEntry(ImageTransferable(image))
        assertThat(runBlocking { clipEntry.readText() }).isNull()
    }

    @Test
    fun `should return the right plain text when trying to read plain text from a ClipEntry with only plain text`() {
        val clipEntry = ClipEntry(StringSelection("Hello"))
        val text = runBlocking { clipEntry.readText() }
        assertThat(text).isNotNull()
        assertThat(text).isEqualTo("Hello")
    }

    @Test
    fun `should return the right plain text when trying to read plain text from a ClipEntry with an AnnotatedString`() {
        val clipEntry = ClipEntry(AnnotatedStringTransferable(AnnotatedString("Hello")))
        val text = runBlocking { clipEntry.readText() }
        assertThat(text).isNotNull()
        assertThat(text).isEqualTo("Hello")
    }

    @Test
    fun `should return null when trying to read an AnnotatedString from a ClipEntry with no text`() {
        val image = BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB)
        val clipEntry = ClipEntry(ImageTransferable(image))
        assertThat(runBlocking { clipEntry.readAnnotatedString() }).isNull()
    }

    @Test
    fun `should return an AnnotatedString wrapping the plain text when trying to read an AnnotatedString from a ClipEntry with only plain text`() {
        val clipEntry = ClipEntry(StringSelection("Hello"))
        val annotatedString = runBlocking { clipEntry.readAnnotatedString() }
        assertThat(annotatedString).isNotNull()
        assertThat(annotatedString).isEqualTo(AnnotatedString("Hello"))
    }

    @Test
    fun `should return an AnnotatedString when trying to read an AnnotatedString from a ClipEntry with an AnnotatedString`() {
        val clipEntry = ClipEntry(AnnotatedStringTransferable(AnnotatedString("Hello")))
        assertThat(runBlocking { clipEntry.readAnnotatedString() }).isNotNull()
    }

    @Test
    fun `should retain annotations when transforming an AnnotatedString to a ClipEntry`() {
        val original = buildAnnotatedString {
            append("Hello, ")
            withStyle(SpanStyle(color = Color.Blue)) {
                append("World!")
            }
        }

        val clipEntry = original.toClipEntry()
        assertThat(clipEntry).isNotNull()
        assertThat(clipEntry!!.hasAnnotatedString()).isTrue()

        val clipEntryString = runBlocking { clipEntry.readAnnotatedString() }
        assertThat(clipEntryString).isNotNull()
        assertThat(clipEntryString!!).isEqualTo(original)
    }

    @Test
    fun `should allow obtaining the plain text when transforming an AnnotatedString to a ClipEntry`() {
        val original = buildAnnotatedString {
            append("Hello, ")
            withStyle(SpanStyle(color = Color.Blue)) {
                append("World!")
            }
        }

        val clipEntry = original.toClipEntry()
        assertThat(clipEntry).isNotNull()
        assertThat(clipEntry!!.hasText()).isTrue()

        val clipEntryString = runBlocking { clipEntry.readText() }
        assertThat(clipEntryString).isNotNull()
        assertThat(clipEntryString!!).isEqualTo(original.text)
    }
}

private class ImageTransferable(
    private val image: Image
) : Transferable {
    override fun getTransferDataFlavors(): Array<DataFlavor?> = supportedFlavors

    override fun isDataFlavorSupported(flavor: DataFlavor): Boolean =
        flavor in supportedFlavors

    override fun getTransferData(flavor: DataFlavor): Any =
        when (flavor) {
            DataFlavor.imageFlavor -> image
            else -> throw UnsupportedFlavorException(flavor)
        }

    companion object {
        private val supportedFlavors = arrayOf(DataFlavor.imageFlavor)
    }
}
