/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.compose.ui.platform

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.net.Uri
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontSynthesis
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextGeometricTransform
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@SmallTest
@RunWith(AndroidJUnit4::class)
class AndroidClipboardManagerTest {

    @Test
    fun annotatedString_singleSpanStyle_convertToCharSequenceAndRecover() {
        val annotatedString = buildAnnotatedString {
            withStyle(SpanStyle(color = Color.Yellow)) {
                append("Hello ")
            }
            append("World")
        }

        assertEncodeAndDecode(annotatedString)
    }

    @Test
    fun annotatedString_multipleSpanStyle_convertToCharSequenceAndRecover() {
        val annotatedString = buildAnnotatedString {
            withStyle(SpanStyle(color = Color.Yellow)) {
                append("Hello")
            }
            append("World")
            withStyle(SpanStyle(letterSpacing = 0.4.sp)) {
                append("Hello")
            }
        }

        assertEncodeAndDecode(annotatedString)
    }

    @Test
    fun annotatedString_nestedSpanStyle_convertToCharSequenceAndRecover() {
        val annotatedString = buildAnnotatedString {
            withStyle(SpanStyle(letterSpacing = 0.4.sp)) {
                withStyle(SpanStyle(color = Color.Yellow)) {
                    append("Hello")
                }
                append("World")
            }
            append("Hello")
        }

        assertEncodeAndDecode(annotatedString)
    }

    @Test
    fun spanStyle_default_encodeAndDecode() {
        val spanStyle = SpanStyle()
        assertEncodeAndDecode(spanStyle)
    }

    @Test
    fun spanStyle_withColor_encodeAndDecode() {
        val spanStyle = SpanStyle(color = Color.Cyan)
        assertEncodeAndDecode(spanStyle)
    }

    @Test
    fun spanStyle_withFontSize_encodeAndDecode() {
        val spanStyle = SpanStyle(fontSize = 16.sp)
        assertEncodeAndDecode(spanStyle)
    }

    @Test
    fun spanStyle_withFontWeight_encodeAndDecode() {
        val spanStyle = SpanStyle(fontWeight = FontWeight.ExtraBold)
        assertEncodeAndDecode(spanStyle)
    }

    @Test
    fun spanStyle_withFontStyle_encodeAndDecode() {
        val spanStyle = SpanStyle(fontStyle = FontStyle.Italic)
        assertEncodeAndDecode(spanStyle)
    }

    @Test
    fun spanStyle_withFontSynthesis_encodeAndDecode() {
        val spanStyle = SpanStyle(fontSynthesis = FontSynthesis.Weight)
        assertEncodeAndDecode(spanStyle)
    }

    @Test
    fun spanStyle_withFontFeatureSettings_encodeAndDecode() {
        val spanStyle = SpanStyle(fontFeatureSettings = "smcp")
        assertEncodeAndDecode(spanStyle)
    }

    @Test
    fun spanStyle_withLetterSpacing_encodeAndDecode() {
        val spanStyle = SpanStyle(letterSpacing = 0.3.sp)
        assertEncodeAndDecode(spanStyle)
    }

    @Test
    fun spanStyle_withBaselineShift_encodeAndDecode() {
        val spanStyle = SpanStyle(baselineShift = BaselineShift.Subscript)
        assertEncodeAndDecode(spanStyle)
    }

    @Test
    fun spanStyle_withTextGeometricTransform_encodeAndDecode() {
        val spanStyle = SpanStyle(
            textGeometricTransform = TextGeometricTransform(
                scaleX = 1.5f,
                skewX = 0.3f
            )
        )
        assertEncodeAndDecode(spanStyle)
    }

    @Test
    fun spanStyle_withBackground_encodeAndDecode() {
        val spanStyle = SpanStyle(background = Color.Cyan)
        assertEncodeAndDecode(spanStyle)
    }

    @Test
    fun spanStyle_withTextDecoration_encodeAndDecode() {
        val spanStyle = SpanStyle(textDecoration = TextDecoration.Underline)
        assertEncodeAndDecode(spanStyle)
    }

    @Test
    fun spanStyle_withShadow_encodeAndDecode() {
        val spanStyle = SpanStyle(
            shadow = Shadow(
                color = Color.Cyan,
                offset = Offset(1f, 2f),
                blurRadius = 3f
            )
        )
        assertEncodeAndDecode(spanStyle)
    }

    @Test
    fun spanStyle_withEverything_encodeAndDecode() {
        val spanStyle = SpanStyle(
            color = Color.Cyan,
            fontSize = 16.sp,
            fontWeight = FontWeight.ExtraLight,
            fontSynthesis = FontSynthesis.Weight,
            fontFeatureSettings = "smcp",
            letterSpacing = 0.3.sp,
            baselineShift = BaselineShift.Superscript,
            textGeometricTransform = TextGeometricTransform(
                scaleX = 1.1f,
                skewX = 0.1f
            ),
            background = Color.Yellow,
            textDecoration = TextDecoration.LineThrough,
            shadow = Shadow(
                color = Color.Cyan,
                offset = Offset(1f, 2f),
                blurRadius = 3f
            )
        )
        assertEncodeAndDecode(spanStyle)
    }

    @Test
    fun getText_whenHasPrimary_butNoPrimary_returnsNull() {
        val clipboardManager = mock<ClipboardManager>()
        whenever(clipboardManager.hasPrimaryClip()).thenReturn(true)
        whenever(clipboardManager.primaryClip).thenReturn(null)
        val subject = AndroidClipboardManager(clipboardManager)

        assertThat(subject.getText()).isNull()
    }

    @Test
    fun hasText_whenHasPrimaryClipDescription_Text_returnsTrue() {
        val clipboardManager = mock<ClipboardManager>()
        val clipDescription = mock<ClipDescription>()
        whenever(clipboardManager.primaryClipDescription).thenReturn(clipDescription)
        whenever(clipDescription.hasMimeType("text/*")).thenReturn(true)
        val subject = AndroidClipboardManager(clipboardManager)

        assertThat(subject.hasText()).isTrue()
    }

    @Test
    fun hasText_whenHasPrimaryClipDescription_notText_returnsFalse() {
        val clipboardManager = mock<ClipboardManager>()
        val clipDescription = mock<ClipDescription>()
        whenever(clipboardManager.primaryClipDescription).thenReturn(clipDescription)
        whenever(clipDescription.hasMimeType("text/*")).thenReturn(false)
        val subject = AndroidClipboardManager(clipboardManager)

        assertThat(subject.hasText()).isFalse()
    }

    @Test
    fun hasText_whenNoPrimaryClipDescription_returnsFalse() {
        val clipboardManager = mock<ClipboardManager>()
        whenever(clipboardManager.primaryClipDescription).thenReturn(null)
        val subject = AndroidClipboardManager(clipboardManager)

        assertThat(subject.hasText()).isFalse()
    }

    @Test
    fun getPrimaryClipEntry_returnsClipData() {
        val clipboardManager = mock<ClipboardManager>()
        val clipData = mock<ClipData>()
        whenever(clipboardManager.primaryClip).thenReturn(clipData)
        val subject = AndroidClipboardManager(clipboardManager)

        assertThat(subject.getClip()?.clipData).isSameInstanceAs(clipData)
    }

    @Test
    fun getPrimaryClipDescription_returnsClipDescription() {
        val clipboardManager = mock<ClipboardManager>()
        val clipDescription = mock<ClipDescription>()
        whenever(clipboardManager.primaryClipDescription).thenReturn(clipDescription)
        val subject = AndroidClipboardManager(clipboardManager)

        assertThat(subject.getClipMetadata()?.clipDescription).isSameInstanceAs(clipDescription)
        verify(clipboardManager, never()).primaryClip
    }

    @Test
    fun hasPrimaryClipEntry_returnsHasClipData() {
        val clipboardManager = mock<ClipboardManager>()
        whenever(clipboardManager.hasPrimaryClip()).thenReturn(true)
        val subject = AndroidClipboardManager(clipboardManager)

        assertThat(subject.hasClip()).isEqualTo(true)

        whenever(clipboardManager.hasPrimaryClip()).thenReturn(false)

        assertThat(subject.hasClip()).isEqualTo(false)

        verify(clipboardManager, never()).primaryClip
        verify(clipboardManager, never()).primaryClipDescription
    }

    @Test
    fun setPrimaryClip_callsSetPrimaryClip() {
        val clipboardManager = mock<ClipboardManager>()
        val clipData = mock<ClipData>()
        val subject = AndroidClipboardManager(clipboardManager)

        subject.setClip(clipData.toClipEntry())

        verify(clipboardManager, times(1)).setPrimaryClip(clipData)
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Test
    fun firstUriOrNull_returnsFirstItem_ifNotNull() {
        val uri = Uri.parse("http://example.com")
        val clipData = mock<ClipData> {
            on { itemCount } doReturn 2
            on { getItemAt(0) } doReturn ClipData.Item(uri)
            on { getItemAt(1) } doReturn ClipData.Item("Hello")
        }

        assertThat(clipData.toClipEntry().firstUriOrNull()).isEqualTo(uri)
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Test
    fun firstUriOrNull_returnsSecondItem_ifFirstIsNull() {
        val uri = Uri.parse("http://example.com")
        val clipData = mock<ClipData> {
            on { itemCount } doReturn 2
            on { getItemAt(0) } doReturn ClipData.Item("Hello")
            on { getItemAt(1) } doReturn ClipData.Item(uri)
        }

        assertThat(clipData.toClipEntry().firstUriOrNull()).isEqualTo(uri)
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Test
    fun firstUriOrNull_returnsNull_ifNoUri() {
        val clipData = mock<ClipData> {
            on { itemCount } doReturn 2
            on { getItemAt(0) } doReturn ClipData.Item("Hello")
            on { getItemAt(1) } doReturn ClipData.Item("World")
        }

        assertThat(clipData.toClipEntry().firstUriOrNull()).isNull()
    }

    private fun assertEncodeAndDecode(spanStyle: SpanStyle) {
        val encoder = EncodeHelper()
        encoder.encode(spanStyle)
        val encodedString = encoder.encodedString()

        val decodeHelper = DecodeHelper(encodedString)
        val decodedSpanStyle = decodeHelper.decodeSpanStyle()

        assertThat(spanStyle).isEqualTo(decodedSpanStyle)
    }

    private fun assertEncodeAndDecode(annotatedString: AnnotatedString) {
        val charSequence = annotatedString.convertToCharSequence()
        val recoveredAnnotatedString = charSequence.convertToAnnotatedString()

        assertThat(annotatedString).isEqualTo(recoveredAnnotatedString)
    }
}
