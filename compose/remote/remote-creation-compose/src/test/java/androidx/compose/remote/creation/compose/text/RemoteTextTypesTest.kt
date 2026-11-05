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

package androidx.compose.remote.creation.compose.text

import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.rf
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class RemoteTextTypesTest {

    @Test
    fun remoteTextAlign_constantsAndToString() {
        assertThat(RemoteTextAlign.Unspecified.toString()).isEqualTo("RemoteTextAlign.Unspecified")
        assertThat(RemoteTextAlign.Left.toString()).isEqualTo("RemoteTextAlign.Left")
        assertThat(RemoteTextAlign.Right.toString()).isEqualTo("RemoteTextAlign.Right")
        assertThat(RemoteTextAlign.Center.toString()).isEqualTo("RemoteTextAlign.Center")
        assertThat(RemoteTextAlign.Justify.toString()).isEqualTo("RemoteTextAlign.Justify")
        assertThat(RemoteTextAlign.Start.toString()).isEqualTo("RemoteTextAlign.Start")
        assertThat(RemoteTextAlign.End.toString()).isEqualTo("RemoteTextAlign.End")
        assertThat(RemoteTextAlign(-1).toString()).isEqualTo("Invalid(-1)")

        assertThat(RemoteTextAlign.Unspecified.value).isEqualTo(Int.MIN_VALUE)
    }

    @Test
    fun remoteLineBreak_constantsAndToString() {
        assertThat(RemoteLineBreak.Unspecified.toString()).isEqualTo("RemoteLineBreak.Unspecified")
        assertThat(RemoteLineBreak.Simple.toString()).isEqualTo("RemoteLineBreak.Simple")
        assertThat(RemoteLineBreak.Paragraph.toString()).isEqualTo("RemoteLineBreak.Paragraph")
        assertThat(RemoteLineBreak.Heading.toString()).isEqualTo("RemoteLineBreak.Heading")
        assertThat(RemoteLineBreak(-1).toString()).isEqualTo("Invalid(-1)")

        assertThat(RemoteLineBreak.Unspecified.value).isEqualTo(Int.MIN_VALUE)
    }

    @Test
    fun remoteHyphens_constantsAndToString() {
        assertThat(RemoteHyphens.Unspecified.toString()).isEqualTo("RemoteHyphens.Unspecified")
        assertThat(RemoteHyphens.None.toString()).isEqualTo("RemoteHyphens.None")
        assertThat(RemoteHyphens.Auto.toString()).isEqualTo("RemoteHyphens.Auto")
        assertThat(RemoteHyphens(-1).toString()).isEqualTo("Invalid(-1)")

        assertThat(RemoteHyphens.Unspecified.value).isEqualTo(Int.MIN_VALUE)
    }

    @Test
    fun remoteFontStyle_constantsAndToString() {
        assertThat(RemoteFontStyle.Normal.toString()).isEqualTo("RemoteFontStyle.Normal")
        assertThat(RemoteFontStyle.Italic.toString()).isEqualTo("RemoteFontStyle.Italic")
        assertThat(RemoteFontStyle(-1).toString()).isEqualTo("Invalid(-1)")
    }

    @Test
    fun remoteTextDecoration_operationsAndToString() {
        assertThat(RemoteTextDecoration.None.toString()).isEqualTo("RemoteTextDecoration.None")
        assertThat(RemoteTextDecoration.Underline.toString())
            .isEqualTo("RemoteTextDecoration.Underline")
        assertThat(RemoteTextDecoration.LineThrough.toString())
            .isEqualTo("RemoteTextDecoration.LineThrough")

        val combined = RemoteTextDecoration.Underline + RemoteTextDecoration.LineThrough
        assertThat(combined.toString()).isEqualTo("RemoteTextDecoration[Underline, LineThrough]")
        assertThat(combined.contains(RemoteTextDecoration.Underline)).isTrue()
        assertThat(combined.contains(RemoteTextDecoration.LineThrough)).isTrue()
        assertThat(RemoteTextDecoration.Underline.contains(RemoteTextDecoration.LineThrough))
            .isFalse()

        val custom = RemoteTextDecoration(4)
        assertThat(custom.toString()).isEqualTo("RemoteTextDecoration[]")
    }

    @Test
    fun remoteFontWeight_constructorsAndProperties() {
        val intWeight = RemoteFontWeight(400)
        val rfWeight = RemoteFontWeight(400.rf)
        val dynamicWeight = RemoteFontWeight(RemoteFloat.createNamedRemoteFloat("w", 400f))

        assertThat(intWeight).isEqualTo(rfWeight)
        assertThat(intWeight.hashCode()).isEqualTo(rfWeight.hashCode())
        assertThat(intWeight.weight.constantValue).isEqualTo(400f)
        assertThat(intWeight.toString()).isEqualTo("RemoteFontWeight(${intWeight.weight})")

        assertThat(dynamicWeight).isNotEqualTo(intWeight)
        assertThat(intWeight).isNotEqualTo("other")

        assertThat(RemoteFontWeight.Thin).isEqualTo(RemoteFontWeight.W100)
        assertThat(RemoteFontWeight.ExtraLight).isEqualTo(RemoteFontWeight.W200)
        assertThat(RemoteFontWeight.Light).isEqualTo(RemoteFontWeight.W300)
        assertThat(RemoteFontWeight.Normal).isEqualTo(RemoteFontWeight.W400)
        assertThat(RemoteFontWeight.Medium).isEqualTo(RemoteFontWeight.W500)
        assertThat(RemoteFontWeight.SemiBold).isEqualTo(RemoteFontWeight.W600)
        assertThat(RemoteFontWeight.Bold).isEqualTo(RemoteFontWeight.W700)
        assertThat(RemoteFontWeight.ExtraBold).isEqualTo(RemoteFontWeight.W800)
        assertThat(RemoteFontWeight.Black).isEqualTo(RemoteFontWeight.W900)

        assertThat(RemoteFontWeight.W100.weight.constantValue).isEqualTo(100f)
        assertThat(RemoteFontWeight.W900.weight.constantValue).isEqualTo(900f)
    }
}
