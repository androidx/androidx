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

package androidx.compose.ui.platform

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest

class NotoFontDownloaderTest {

    @Test
    fun emptyCodepoints_returnsEmptyList() = runTest {
        val downloader = NotoFontDownloader()
        val result = downloader.downloadFallbackFont(emptySet())
        assertEquals(emptyList(), result)
        assertTrue(downloader.getCodepointsWithNoKnownFont().isEmpty())
    }

    @Test
    fun codepointAboveMaxUnicode_isIgnored() = runTest {
        // 0x110000 is one above MAX_CODE_POINT (0x10FFFF); must not trigger network
        val downloader = NotoFontDownloader()
        val result = downloader.downloadFallbackFont(setOf(0x110000))
        assertEquals(emptyList(), result)
        assertTrue(downloader.getCodepointsWithNoKnownFont().isEmpty())
    }

    @Test
    fun codepointWithNoCoverage_isRememberedAndSkippedOnSubsequentCall() = runTest {
        // Private Use Area codepoints (0xE000–0xF8FF) have no Noto font coverage
        val pua = 0xE000
        val downloader = NotoFontDownloader()

        val first = downloader.downloadFallbackFont(setOf(pua))
        assertEquals(emptyList(), first, "PUA codepoint has no Noto coverage")
        assertContentEquals(listOf(pua), downloader.getCodepointsWithNoKnownFont())

        // A second call with the same codepoint should be a no-op (already remembered)
        val second = downloader.downloadFallbackFont(setOf(pua))
        assertEquals(emptyList(), second)
        assertContentEquals(listOf(pua), downloader.getCodepointsWithNoKnownFont())
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun mixOfKnownAndUnknownCodepoints_onlyUnknownTracked() = runTest {
        val codepoints = setOf(0xF000, 0x110000)
        val downloader = NotoFontDownloader()
        // Call with a mix; the PUA codepoint has no coverage and will be remembered.
        // The valid codepoint (e.g. CJK) would trigger a network call — skip for unit test.
        // Here we just verify that a PUA-only set returns empty without crashing.
        val result = downloader.downloadFallbackFont(codepoints)
        assertEquals(emptyList(), result)
        assertContentEquals(listOf(0xF000), downloader.getCodepointsWithNoKnownFont())
    }

    @Test
    fun koreanHangulSyllables_coveredByKoreanFont_KoBrowser() {
        val downloader = NotoFontDownloader()
        val cases = mapOf(
            0xACA8 to "겨",  // multi-CJK coverage: HK + KR + SC + TC
            0xACAF to "겼",  // single coverage: KR only
            0xACF0 to "곰",  // multi-CJK coverage: HK + KR + TC
        )
        val forDownload = downloader.getFontsToDownload(cases.keys, language = "ko")
        val names = forDownload.map { it.font.name }
        assertTrue(names.isNotEmpty(), "No fonts found for '${cases.values.joinToString()}'")
        assertTrue(
            names.all { it.startsWith("Noto Sans KR") },
            "Expected 'Noto Sans KR' fonts for '${cases.values.joinToString()}', got: $names"
        )
        assertTrue(downloader.getCodepointsWithNoKnownFont().isEmpty(),
            "Expected no codepoints with no known font, got: ${downloader.getCodepointsWithNoKnownFont()}"
        )
    }

    @Test
    fun koreanHangulSyllables_coveredByKoreanFont_EnBrowser() {
        val downloader = NotoFontDownloader()
        val cases = mapOf(
            0xACA8 to "겨",  // multi-CJK coverage: HK + KR + SC + TC
            0xACAF to "겼",  // single coverage: KR only
            0xACF0 to "곰",  // multi-CJK coverage: HK + KR + TC
        )
        val forDownload = downloader.getFontsToDownload(cases.keys, language = "en")
        val names = forDownload.map { it.font.name }
        assertTrue(names.isNotEmpty(), "No fonts found for '${cases.values.joinToString()}'")
        assertTrue(downloader.getCodepointsWithNoKnownFont().isEmpty(),
            "Expected no codepoints with no known font, got: ${downloader.getCodepointsWithNoKnownFont()}"
        )
    }

    // --- Script-to-font resolution tests ---

    @Test
    fun arabicScript_resolvesToNotoSansArabic() {
        val downloader = NotoFontDownloader()
        // U+0639 ع Arabic Letter Ain, U+0641 ف Arabic Letter Fa
        val fonts = downloader.getFontsToDownload(setOf(0x0639, 0x0641), language = "en")
        val names = fonts.map { it.font.name }
        assertTrue(names.isNotEmpty(), "No fonts found for Arabic script")
        assertTrue(names.all { it.startsWith("Noto Sans Arabic") }, "Expected Noto Sans Arabic, got: $names")
        assertTrue(downloader.getCodepointsWithNoKnownFont().isEmpty())
    }

    @Test
    fun hebrewScript_resolvesToNotoSansHebrew() {
        val downloader = NotoFontDownloader()
        // U+05E9 ש Hebrew Letter Shin, U+05D0 א Hebrew Letter Alef
        val fonts = downloader.getFontsToDownload(setOf(0x05E9, 0x05D0), language = "en")
        val names = fonts.map { it.font.name }
        assertTrue(names.isNotEmpty(), "No fonts found for Hebrew script")
        assertTrue(names.all { it.startsWith("Noto Sans Hebrew") }, "Expected Noto Sans Hebrew, got: $names")
        assertTrue(downloader.getCodepointsWithNoKnownFont().isEmpty())
    }

    @Test
    fun thaiScript_resolvesToNotoSansThai() {
        val downloader = NotoFontDownloader()
        // U+0E01 ก Thai Letter Ko Kai, U+0E2A ส Thai Letter So Sua
        val fonts = downloader.getFontsToDownload(setOf(0x0E01, 0x0E2A), language = "en")
        val names = fonts.map { it.font.name }
        assertTrue(names.isNotEmpty(), "No fonts found for Thai script")
        assertTrue(names.all { it.startsWith("Noto Sans Thai") }, "Expected Noto Sans Thai, got: $names")
        assertTrue(downloader.getCodepointsWithNoKnownFont().isEmpty())
    }

    @Test
    fun devanagariScript_resolvesToNotoSans() {
        val downloader = NotoFontDownloader()
        // U+0905 अ Devanagari Letter A, U+0915 क Devanagari Letter Ka
        // These codepoints are covered by the base Noto Sans woff2 subset, not a separate Devanagari font
        val fonts = downloader.getFontsToDownload(setOf(0x0905, 0x0915), language = "en")
        val names = fonts.map { it.font.name }
        assertTrue(names.isNotEmpty(), "No fonts found for Devanagari script")
        assertTrue(names.all { it.startsWith("Noto Sans") }, "Expected Noto Sans*, got: $names")
        assertTrue(downloader.getCodepointsWithNoKnownFont().isEmpty())
    }

    @Test
    fun bengaliScript_resolvesToNotoSansBengali() {
        val downloader = NotoFontDownloader()
        // U+0985 অ Bengali Letter A, U+0995 ক Bengali Letter Ka
        val fonts = downloader.getFontsToDownload(setOf(0x0985, 0x0995), language = "en")
        val names = fonts.map { it.font.name }
        assertTrue(names.isNotEmpty(), "No fonts found for Bengali script")
        assertTrue(names.all { it.startsWith("Noto Sans Bengali") }, "Expected Noto Sans Bengali, got: $names")
        assertTrue(downloader.getCodepointsWithNoKnownFont().isEmpty())
    }

    @Test
    fun emoji_resolvesToNotoColorEmoji() {
        val downloader = NotoFontDownloader()
        // U+1F600 😀 Grinning Face, U+1F389 🎉 Party Popper
        val fonts = downloader.getFontsToDownload(setOf(0x1F600, 0x1F389), language = "en")
        val names = fonts.map { it.font.name }
        assertTrue(names.isNotEmpty(), "No fonts found for emoji")
        assertTrue(names.all { it.startsWith("Noto Color Emoji") }, "Expected Noto Color Emoji, got: $names")
        assertTrue(downloader.getCodepointsWithNoKnownFont().isEmpty())
    }

    @Test
    fun cardSuitSymbols_resolvesToNotoColorEmoji() {
        val downloader = NotoFontDownloader()
        // U+2660 ♠ Black Spade Suit, U+2663 ♣ Black Club Suit
        // These have emoji presentations, so Noto Color Emoji wins over Noto Sans Symbols
        val fonts = downloader.getFontsToDownload(setOf(0x2660, 0x2663), language = "en")
        val names = fonts.map { it.font.name }
        assertTrue(names.isNotEmpty(), "No fonts found for card suit symbols")
        assertTrue(names.all { it.startsWith("Noto Color Emoji") }, "Expected Noto Color Emoji, got: $names")
        assertTrue(downloader.getCodepointsWithNoKnownFont().isEmpty())
    }

    @Test
    fun boxDrawingCharacters_resolvesToNotoSansHK() {
        val downloader = NotoFontDownloader()
        // U+2500 ─ Box Drawings Light Horizontal, U+2502 │ Box Drawings Light Vertical
        // Box drawing characters are included in Noto Sans HK sub-fonts
        val fonts = downloader.getFontsToDownload(setOf(0x2500, 0x2502), language = "en")
        val names = fonts.map { it.font.name }
        assertTrue(names.isNotEmpty(), "No fonts found for box drawing characters")
        assertTrue(names.all { it.startsWith("Noto Sans HK") }, "Expected Noto Sans HK, got: $names")
        assertTrue(downloader.getCodepointsWithNoKnownFont().isEmpty())
    }

    // --- CJK language selection tests ---

    @Test
    fun japaneseHiragana_resolvesToNotoSansJP() {
        val downloader = NotoFontDownloader()
        // U+3042 あ Hiragana Letter A — only covered by Noto Sans JP
        val fonts = downloader.getFontsToDownload(setOf(0x3042), language = "ja")
        val names = fonts.map { it.font.name }
        assertTrue(names.isNotEmpty(), "No fonts found for Hiragana")
        assertTrue(names.all { it.startsWith("Noto Sans JP") }, "Expected Noto Sans JP, got: $names")
        assertTrue(downloader.getCodepointsWithNoKnownFont().isEmpty())
    }

    @Test
    fun cjkCharacter_resolvesToNotoSansSC_forZhCN() {
        val downloader = NotoFontDownloader()
        // U+5B57 字 CJK Unified Ideograph — covered by SC + TC + HK + JP; zh-CN picks SC
        val fonts = downloader.getFontsToDownload(setOf(0x5B57), language = "zh-CN")
        val names = fonts.map { it.font.name }
        assertTrue(names.isNotEmpty(), "No fonts found for CJK character (zh-CN)")
        assertTrue(names.all { it.startsWith("Noto Sans SC") }, "Expected Noto Sans SC, got: $names")
        assertTrue(downloader.getCodepointsWithNoKnownFont().isEmpty())
    }

    @Test
    fun cjkCharacter_resolvesToNotoSansSC_forZhHans() {
        val downloader = NotoFontDownloader()
        val fonts = downloader.getFontsToDownload(setOf(0x5B57), language = "zh-Hans")
        val names = fonts.map { it.font.name }
        assertTrue(names.isNotEmpty(), "No fonts found for CJK character (zh-Hans)")
        assertTrue(names.all { it.startsWith("Noto Sans SC") }, "Expected Noto Sans SC, got: $names")
        assertTrue(downloader.getCodepointsWithNoKnownFont().isEmpty())
    }

    @Test
    fun cjkCharacter_resolvesToNotoSansTC_forZhTW() {
        val downloader = NotoFontDownloader()
        // zh-TW picks Traditional Chinese
        val fonts = downloader.getFontsToDownload(setOf(0x5B57), language = "zh-TW")
        val names = fonts.map { it.font.name }
        assertTrue(names.isNotEmpty(), "No fonts found for CJK character (zh-TW)")
        assertTrue(names.all { it.startsWith("Noto Sans TC") }, "Expected Noto Sans TC, got: $names")
        assertTrue(downloader.getCodepointsWithNoKnownFont().isEmpty())
    }

    @Test
    fun cjkCharacter_resolvesToNotoSansHK_forZhHK() {
        val downloader = NotoFontDownloader()
        val fonts = downloader.getFontsToDownload(setOf(0x5B57), language = "zh-HK")
        val names = fonts.map { it.font.name }
        assertTrue(names.isNotEmpty(), "No fonts found for CJK character (zh-HK)")
        assertTrue(names.all { it.startsWith("Noto Sans HK") }, "Expected Noto Sans HK, got: $names")
        assertTrue(downloader.getCodepointsWithNoKnownFont().isEmpty())
    }

    @Test
    fun cjkCharacter_resolvesToNotoSansJP_forJa() {
        val downloader = NotoFontDownloader()
        val fonts = downloader.getFontsToDownload(setOf(0x5B57), language = "ja")
        val names = fonts.map { it.font.name }
        assertTrue(names.isNotEmpty(), "No fonts found for CJK character (ja)")
        assertTrue(names.all { it.startsWith("Noto Sans JP") }, "Expected Noto Sans JP, got: $names")
        assertTrue(downloader.getCodepointsWithNoKnownFont().isEmpty())
    }
}
