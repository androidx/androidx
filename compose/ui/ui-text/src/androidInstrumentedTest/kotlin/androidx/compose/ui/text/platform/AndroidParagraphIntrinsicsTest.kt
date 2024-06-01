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

package androidx.compose.ui.text.platform

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.text.EmojiSupportMatch
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.compose.ui.unit.Density
import androidx.emoji2.text.EmojiCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
@SmallTest
class AndroidParagraphIntrinsicsTest {

    val context = InstrumentationRegistry.getInstrumentation().context

    @After
    fun cleanup() {
        EmojiCompat.reset(null)
        EmojiCompatStatus.setDelegateForTesting(null)
    }

    @Test
    fun whenEmojiCompatLoads_hasStaleFontsIsTrue() {
        val fontState = mutableStateOf(false)
        EmojiCompatStatus.setDelegateForTesting(
            object : EmojiCompatStatusDelegate {
                override val fontLoaded: State<Boolean>
                    get() = fontState
            }
        )

        val subject =
            ActualParagraphIntrinsics(
                "text",
                TextStyle.Default,
                listOf(),
                listOf(),
                Density(1f),
                createFontFamilyResolver(context)
            )

        assertThat(subject.hasStaleResolvedFonts).isFalse()
        fontState.value = true
        assertThat(subject.hasStaleResolvedFonts).isTrue()
    }

    @Test
    fun whenStyleSaysNoemojiCompat_NoEmojiCompat() {
        val fontState = mutableStateOf(false)
        EmojiCompatStatus.setDelegateForTesting(
            object : EmojiCompatStatusDelegate {
                override val fontLoaded: State<Boolean>
                    get() = fontState
            }
        )

        val style =
            TextStyle(platformStyle = PlatformTextStyle(emojiSupportMatch = EmojiSupportMatch.None))
        val subject =
            ActualParagraphIntrinsics(
                "text",
                style,
                listOf(),
                listOf(),
                Density(1f),
                createFontFamilyResolver(context)
            )
        fontState.value = true
        assertThat(subject.hasStaleResolvedFonts).isFalse()
    }

    @Test
    fun whenReplaceall_replaceAll() {
        // sorry mocks - every obvious way to make this properly testable involves an allocation in
        // prod code :(
        val mock = mock(EmojiCompat::class.java)
        whenever(mock.process(ArgumentMatchers.anyString(), anyInt(), anyInt(), anyInt(), anyInt()))
            .thenReturn("")
        EmojiCompat.reset(mock)

        EmojiCompatStatus.setDelegateForTesting(
            object : EmojiCompatStatusDelegate {
                override val fontLoaded: State<Boolean>
                    get() = mutableStateOf(true)
            }
        )

        val style =
            TextStyle(platformStyle = PlatformTextStyle(emojiSupportMatch = EmojiSupportMatch.All))
        ActualParagraphIntrinsics(
            "text",
            style,
            listOf(),
            listOf(),
            Density(1f),
            createFontFamilyResolver(context)
        )

        verify(mock)
            .process(
                eq("text"),
                eq(0),
                eq("text".length),
                eq(Int.MAX_VALUE),
                eq(EmojiCompat.REPLACE_STRATEGY_ALL)
            )
    }

    @Test
    fun whenDefaultStrategy_doesDefault() {
        // sorry mocks - every obvious way to make this properly testable involves an allocation in
        // prod code :(
        val mock = mock(EmojiCompat::class.java)
        whenever(mock.process(ArgumentMatchers.anyString(), anyInt(), anyInt(), anyInt(), anyInt()))
            .thenReturn("")
        EmojiCompat.reset(mock)

        EmojiCompatStatus.setDelegateForTesting(
            object : EmojiCompatStatusDelegate {
                override val fontLoaded: State<Boolean>
                    get() = mutableStateOf(true)
            }
        )

        val style =
            TextStyle(
                platformStyle = PlatformTextStyle(emojiSupportMatch = EmojiSupportMatch.Default)
            )
        ActualParagraphIntrinsics(
            "text",
            style,
            listOf(),
            listOf(),
            Density(1f),
            createFontFamilyResolver(context)
        )

        verify(mock)
            .process(
                eq("text"),
                eq(0),
                eq("text".length),
                eq(Int.MAX_VALUE),
                eq(EmojiCompat.REPLACE_STRATEGY_DEFAULT)
            )
    }
}
