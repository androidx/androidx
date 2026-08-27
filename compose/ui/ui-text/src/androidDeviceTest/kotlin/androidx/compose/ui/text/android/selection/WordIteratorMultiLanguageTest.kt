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

package androidx.compose.ui.text.android.selection

import androidx.emoji2.bundled.BundledEmojiCompatConfig
import androidx.emoji2.text.EmojiCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import java.text.BreakIterator
import java.util.Locale
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Multi-language test suite for [WordIterator] validating platform [BreakIterator] parity across
 * global languages and script systems, and [EmojiCompat] integration.
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class WordIteratorMultiLanguageTest {

    companion object {
        private val context = InstrumentationRegistry.getInstrumentation().targetContext

        @BeforeClass
        @JvmStatic
        fun setup() {
            EmojiCompat.reset(null)
            val latch = CountDownLatch(1)
            val config =
                @Suppress("DEPRECATION")
                BundledEmojiCompatConfig(context)
                    .setReplaceAll(true)
                    .registerInitCallback(
                        object : EmojiCompat.InitCallback() {
                            override fun onInitialized() {
                                latch.countDown()
                            }

                            override fun onFailed(throwable: Throwable?) {
                                latch.countDown()
                            }
                        }
                    )
            EmojiCompat.init(config)
            assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue()
        }

        @AfterClass
        @JvmStatic
        fun clean() {
            EmojiCompat.reset(null)
        }
    }

    private fun assertWordBoundaries(text: String, locale: Locale) {
        val wordIterator = WordIterator(text, 0, text.length, locale)
        val platformIterator = BreakIterator.getWordInstance(locale)
        platformIterator.setText(text)

        // 1. Verify forward boundary parity across all positions
        var current = 0
        while (current < text.length) {
            val expected = platformIterator.following(current)
            val actual = wordIterator.nextBoundary(current)
            assertWithMessage("nextBoundary at $current in locale '$locale' for '$text'")
                .that(actual)
                .isEqualTo(expected)
            if (actual == BreakIterator.DONE) break
            current = actual
        }

        // 2. Verify backward boundary parity across all positions
        current = text.length
        while (current > 0) {
            val expected = platformIterator.preceding(current)
            val actual = wordIterator.prevBoundary(current)
            assertWithMessage("prevBoundary at $current in locale '$locale' for '$text'")
                .that(actual)
                .isEqualTo(expected)
            if (actual == BreakIterator.DONE) break
            current = actual
        }
    }

    // -------------------------------------------------------------------------
    // Language & Script Platform Parity Tests
    // -------------------------------------------------------------------------

    @Test
    fun english_spacedLatinWords_matchesPlatformParity() {
        assertWordBoundaries(
            text = "The quick brown fox jumps over the lazy dog",
            locale = Locale.ENGLISH,
        )
    }

    @Test
    fun spanish_latinAccents_matchesPlatformParity() {
        assertWordBoundaries(
            text = "Hola mundo feliz, ¿cómo estás hoy niño?",
            locale = Locale.forLanguageTag("es"),
        )
    }

    @Test
    fun japanese_kanjiAndHiragana_matchesPlatformParity() {
        assertWordBoundaries(text = "吾輩は猫である。名前はまだ無い。", locale = Locale.JAPANESE)
    }

    @Test
    fun japanese_katakanaCompound_matchesPlatformParity() {
        assertWordBoundaries(text = "スマートフォンの画面とアプリケーション", locale = Locale.JAPANESE)
    }

    @Test
    fun german_latinCompounds_matchesPlatformParity() {
        assertWordBoundaries(
            text = "Guten Tag Welt und Donaudampfschiffahrtsgesellschaft",
            locale = Locale.GERMAN,
        )
    }

    @Test
    fun french_latinElisions_matchesPlatformParity() {
        assertWordBoundaries(
            text = "Bonjour le monde et l'arbre de la forêt",
            locale = Locale.FRENCH,
        )
    }

    @Test
    fun portuguese_latinNasalAccents_matchesPlatformParity() {
        assertWordBoundaries(
            text = "Olá mundo maravilhoso e coração de mãe",
            locale = Locale.forLanguageTag("pt-BR"),
        )
    }

    @Test
    fun italian_latinWords_matchesPlatformParity() {
        assertWordBoundaries(
            text = "Ciao mondo bellissimo e buongiorno a tutti",
            locale = Locale.ITALIAN,
        )
    }

    @Test
    fun russian_cyrillicScript_matchesPlatformParity() {
        assertWordBoundaries(
            text = "Привет мир сегодня и быстрый поиск слов",
            locale = Locale.forLanguageTag("ru"),
        )
    }

    @Test
    fun korean_hangulSyllables_matchesPlatformParity() {
        assertWordBoundaries(text = "한글 맞춤법과 단어 분리 테스트", locale = Locale.KOREAN)
    }

    @Test
    fun chineseSimplified_hanziCompounds_matchesPlatformParity() {
        assertWordBoundaries(text = "机器学习正在改变世界开发体验极佳", locale = Locale.SIMPLIFIED_CHINESE)
    }

    @Test
    fun chineseTraditional_hanziCompounds_matchesPlatformParity() {
        assertWordBoundaries(text = "繁體中文排版測試效能與體驗並重", locale = Locale.TRADITIONAL_CHINESE)
    }

    @Test
    fun arabic_rightToLeftScript_matchesPlatformParity() {
        assertWordBoundaries(
            text = "مرحبا بالعالم الجميل واللغة العربية عريقة",
            locale = Locale.forLanguageTag("ar"),
        )
    }

    @Test
    fun hindi_devanagariScript_matchesPlatformParity() {
        assertWordBoundaries(
            text = "नमस्ते दुनिया भारत का राष्ट्रीय फूल कमल है",
            locale = Locale.forLanguageTag("hi"),
        )
    }

    @Test
    fun indonesian_latinWords_matchesPlatformParity() {
        assertWordBoundaries(
            text = "Selamat pagi dunia dan selamat datang",
            locale = Locale.forLanguageTag("id"),
        )
    }

    @Test
    fun turkish_latinDottedI_matchesPlatformParity() {
        assertWordBoundaries(
            text = "Merhaba dünya güzel ve İstanbul şehri",
            locale = Locale.forLanguageTag("tr"),
        )
    }

    @Test
    fun polish_latinOgonek_matchesPlatformParity() {
        assertWordBoundaries(
            text = "Witaj piękny świecie i dobry wieczór",
            locale = Locale.forLanguageTag("pl"),
        )
    }

    @Test
    fun thai_unspacedScript_matchesPlatformParity() {
        assertWordBoundaries(
            text = "ภาษาไทยเป็นภาษาที่สวยงามราคาห้าร้อยบาท",
            locale = Locale.forLanguageTag("th"),
        )
    }

    @Test
    fun vietnamese_latinCombiningMarks_matchesPlatformParity() {
        assertWordBoundaries(
            text = "Tiếng Việt có dấu thanh điệu rất phong phú",
            locale = Locale.forLanguageTag("vi"),
        )
    }

    @Test
    fun dutch_latinDigraph_matchesPlatformParity() {
        assertWordBoundaries(
            text = "Hallo mooie wereld en goede morgen",
            locale = Locale.forLanguageTag("nl"),
        )
    }

    @Test
    fun swedish_latinRingA_matchesPlatformParity() {
        assertWordBoundaries(
            text = "Hej vackra värld och god morgon Sverige",
            locale = Locale.forLanguageTag("sv"),
        )
    }

    @Test
    fun danish_latinOslash_matchesPlatformParity() {
        assertWordBoundaries(
            text = "Hej smukke verden og god dag Danmark",
            locale = Locale.forLanguageTag("da"),
        )
    }

    @Test
    fun norwegian_latinWords_matchesPlatformParity() {
        assertWordBoundaries(
            text = "Hei vakre verden og god morgen Norge",
            locale = Locale.forLanguageTag("no"),
        )
    }

    @Test
    fun finnish_latinDoubleVowels_matchesPlatformParity() {
        assertWordBoundaries(
            text = "Hei kaunis maailma ja hyvää huomenta",
            locale = Locale.forLanguageTag("fi"),
        )
    }

    @Test
    fun hebrew_rightToLeftScript_matchesPlatformParity() {
        assertWordBoundaries(
            text = "שלום עולם יפה ויום נפלא לכולם",
            locale = Locale.forLanguageTag("he"),
        )
    }

    @Test
    fun greek_accentedScript_matchesPlatformParity() {
        assertWordBoundaries(
            text = "Γειά σου κόσμε η ελληνική γλώσσα είναι όμορφη",
            locale = Locale.forLanguageTag("el"),
        )
    }

    @Test
    fun czech_latinHacek_matchesPlatformParity() {
        assertWordBoundaries(
            text = "Ahoj krásný světe a dobrý den Česko",
            locale = Locale.forLanguageTag("cs"),
        )
    }

    @Test
    fun hungarian_latinDoubleAcute_matchesPlatformParity() {
        assertWordBoundaries(
            text = "Helló szép világ és jó reggelt Magyarország",
            locale = Locale.forLanguageTag("hu"),
        )
    }

    @Test
    fun romanian_latinCommaBelow_matchesPlatformParity() {
        assertWordBoundaries(
            text = "Bună ziua lume și o zi frumoasă",
            locale = Locale.forLanguageTag("ro"),
        )
    }

    @Test
    fun ukrainian_cyrillicScript_matchesPlatformParity() {
        assertWordBoundaries(
            text = "Привіт прекрасний світ і доброго ранку Україно",
            locale = Locale.forLanguageTag("uk"),
        )
    }

    @Test
    fun tagalog_latinWords_matchesPlatformParity() {
        assertWordBoundaries(
            text = "Kamusta magandang mundo at magandang araw Pilipinas",
            locale = Locale.forLanguageTag("fil"),
        )
    }

    @Test
    fun malay_latinWords_matchesPlatformParity() {
        assertWordBoundaries(
            text = "Selamat pagi kawan dan salam sejahtera Malaysia",
            locale = Locale.forLanguageTag("ms"),
        )
    }

    @Test
    fun bengali_brahmicScript_matchesPlatformParity() {
        assertWordBoundaries(
            text = "বাংলা সুন্দর ভাষা আমাদের মাতৃভাষা ও গর্ব",
            locale = Locale.forLanguageTag("bn"),
        )
    }

    @Test
    fun tamil_dravidianScript_matchesPlatformParity() {
        assertWordBoundaries(
            text = "வணக்கம் நல்ல உலகம் தமிழ் மொழி மிக பழமையானது",
            locale = Locale.forLanguageTag("ta"),
        )
    }

    @Test
    fun telugu_dravidianScript_matchesPlatformParity() {
        assertWordBoundaries(
            text = "నమస్కారం మంచి ప్రపంచం తెలుగు భాష చాలా మధురమైనది",
            locale = Locale.forLanguageTag("te"),
        )
    }

    @Test
    fun marathi_devanagariScript_matchesPlatformParity() {
        assertWordBoundaries(
            text = "नमस्कार सुंदर जग आणि महाराष्ट्र राज्य",
            locale = Locale.forLanguageTag("mr"),
        )
    }

    @Test
    fun gujarati_gujaratiScript_matchesPlatformParity() {
        assertWordBoundaries(
            text = "નમસ્તે સુંદર દુનિયા અને ગુજરાત રાજ્ય",
            locale = Locale.forLanguageTag("gu"),
        )
    }

    @Test
    fun kannada_dravidianScript_matchesPlatformParity() {
        assertWordBoundaries(
            text = "ನಮಸ್ಕಾರ ಸುಂದರ ಜಗತ್ತು ಕನ್ನಡ ಭಾಷೆ ಅದ್ಭುತವಾಗಿದೆ",
            locale = Locale.forLanguageTag("kn"),
        )
    }

    @Test
    fun malayalam_dravidianScript_matchesPlatformParity() {
        assertWordBoundaries(
            text = "നമസ്കാരം നല്ല ലോകം മലയാള ഭാഷ സുന്ദരമാണ്",
            locale = Locale.forLanguageTag("ml"),
        )
    }

    @Test
    fun urdu_rightToLeftNastaliq_matchesPlatformParity() {
        assertWordBoundaries(
            text = "خوش آمدید پیاری دنیا اور خوبصورت پاکستان",
            locale = Locale.forLanguageTag("ur"),
        )
    }

    @Test
    fun persian_rightToLeftScript_matchesPlatformParity() {
        assertWordBoundaries(
            text = "سلام دنیای زیبا و فرهنگ کهن ایران",
            locale = Locale.forLanguageTag("fa"),
        )
    }

    @Test
    fun amharic_geezEthiopicScript_matchesPlatformParity() {
        assertWordBoundaries(text = "ሰላም ዓለም ዛሬ እና ውብ ኢትዮጵያ", locale = Locale.forLanguageTag("am"))
    }

    @Test
    fun swahili_africanBantuLatin_matchesPlatformParity() {
        assertWordBoundaries(
            text = "Habari ya dunia na karibu sana Afrika Mashariki",
            locale = Locale.forLanguageTag("sw"),
        )
    }

    @Test
    fun zulu_africanBantuLatin_matchesPlatformParity() {
        assertWordBoundaries(
            text = "Sawubona mhlaba omuhle naseNingizimu Afrika",
            locale = Locale.forLanguageTag("zu"),
        )
    }

    @Test
    fun afrikaans_latinWords_matchesPlatformParity() {
        assertWordBoundaries(
            text = "Hallo wonderlike wêreld en goeie more Suid Afrika",
            locale = Locale.forLanguageTag("af"),
        )
    }

    @Test
    fun hausa_latinHookedLetters_matchesPlatformParity() {
        assertWordBoundaries(
            text = "Sannu da duniya da fatan alheri Najeriya",
            locale = Locale.forLanguageTag("ha"),
        )
    }

    @Test
    fun nepali_devanagariScript_matchesPlatformParity() {
        assertWordBoundaries(
            text = "नमस्ते सुन्दर संसार र सुन्दर नेपाल",
            locale = Locale.forLanguageTag("ne"),
        )
    }

    @Test
    fun sinhala_sinhalaScript_matchesPlatformParity() {
        assertWordBoundaries(
            text = "ආයුබෝවන් ලස්සන ලෝකය සහ ශ්‍රී ලංකාව",
            locale = Locale.forLanguageTag("si"),
        )
    }

    @Test
    fun punjabi_gurmukhiScript_matchesPlatformParity() {
        assertWordBoundaries(
            text = "ਸਤਿ ਸ੍ਰੀ ਅਕਾਲ ਸੋਹਣੀ ਦੁਨੀਆ ਅਤੇ ਪੰਜਾਬ",
            locale = Locale.forLanguageTag("pa"),
        )
    }

    @Test
    fun odia_odiaScript_matchesPlatformParity() {
        assertWordBoundaries(
            text = "ନମସ୍କାର ଭଲ ପୃଥିବୀ ଏବଂ ଓଡ଼ିଶା ରାଜ୍ୟ",
            locale = Locale.forLanguageTag("or"),
        )
    }

    @Test
    fun georgian_mkhedruliScript_matchesPlatformParity() {
        assertWordBoundaries(
            text = "გამარჯობა ლამაზო სამყარო და საქართველო",
            locale = Locale.forLanguageTag("ka"),
        )
    }

    @Test
    fun armenian_armenianScript_matchesPlatformParity() {
        assertWordBoundaries(
            text = "Բարև գեղեցիկ աշխարհ և Հայաստան",
            locale = Locale.forLanguageTag("hy"),
        )
    }

    @Test
    fun kazakh_cyrillicScript_matchesPlatformParity() {
        assertWordBoundaries(
            text = "Сәлем тамаша әлем және Қазақстан",
            locale = Locale.forLanguageTag("kk"),
        )
    }

    @Test
    fun uzbek_latinScript_matchesPlatformParity() {
        assertWordBoundaries(
            text = "Salom gozal dunyo va Ozbekiston yurti",
            locale = Locale.forLanguageTag("uz"),
        )
    }

    @Test
    fun azerbaijani_latinScript_matchesPlatformParity() {
        assertWordBoundaries(
            text = "Salam gözəl dünya və gözəl Azərbaycan",
            locale = Locale.forLanguageTag("az"),
        )
    }

    @Test
    fun mongolian_cyrillicScript_matchesPlatformParity() {
        assertWordBoundaries(
            text = "Сайн байна уу сайхан ертөнц Монгол улс",
            locale = Locale.forLanguageTag("mn"),
        )
    }

    @Test
    fun catalan_latinMiddleDot_matchesPlatformParity() {
        assertWordBoundaries(
            text = "Hola món bonic i bon dia Catalunya",
            locale = Locale.forLanguageTag("ca"),
        )
    }

    @Test
    fun basque_latinWords_matchesPlatformParity() {
        assertWordBoundaries(
            text = "Kaixo mundu eder eta egun on Euskal Herria",
            locale = Locale.forLanguageTag("eu"),
        )
    }

    @Test
    fun galician_latinWords_matchesPlatformParity() {
        assertWordBoundaries(
            text = "Ola mundo fermoso e bo día Galicia",
            locale = Locale.forLanguageTag("gl"),
        )
    }

    @Test
    fun croatian_latinWords_matchesPlatformParity() {
        assertWordBoundaries(
            text = "Pozdrav lijepi svijete i dobar dan Hrvatska",
            locale = Locale.forLanguageTag("hr"),
        )
    }

    @Test
    fun serbian_cyrillicScript_matchesPlatformParity() {
        assertWordBoundaries(
            text = "Здраво лепи свете и добар дан Србијо",
            locale = Locale.forLanguageTag("sr"),
        )
    }

    @Test
    fun slovak_latinWords_matchesPlatformParity() {
        assertWordBoundaries(
            text = "Ahoj krásny svet a pekný deň Slovensko",
            locale = Locale.forLanguageTag("sk"),
        )
    }

    @Test
    fun slovenian_latinWords_matchesPlatformParity() {
        assertWordBoundaries(
            text = "Pozdravljen lep svet in dober dan Slovenija",
            locale = Locale.forLanguageTag("sl"),
        )
    }

    @Test
    fun bulgarian_cyrillicScript_matchesPlatformParity() {
        assertWordBoundaries(
            text = "Здравей красив свят и добър ден България",
            locale = Locale.forLanguageTag("bg"),
        )
    }

    @Test
    fun lithuanian_latinOgonek_matchesPlatformParity() {
        assertWordBoundaries(
            text = "Labas gražus pasauli ir laba diena Lietuva",
            locale = Locale.forLanguageTag("lt"),
        )
    }

    @Test
    fun latvian_latinMacron_matchesPlatformParity() {
        assertWordBoundaries(
            text = "Sveika skaistā pasaule un labdien Latvija",
            locale = Locale.forLanguageTag("lv"),
        )
    }

    @Test
    fun estonian_latinUmlauts_matchesPlatformParity() {
        assertWordBoundaries(
            text = "Tere ilus maailm ja tere päevast Eesti",
            locale = Locale.forLanguageTag("et"),
        )
    }

    @Test
    fun icelandic_latinThornAndEth_matchesPlatformParity() {
        assertWordBoundaries(
            text = "Halló fallegi heimur og góðan daginn Ísland",
            locale = Locale.forLanguageTag("is"),
        )
    }

    @Test
    fun welsh_celticLatinWords_matchesPlatformParity() {
        assertWordBoundaries(
            text = "Helo fyd hardd a bore da Cymru",
            locale = Locale.forLanguageTag("cy"),
        )
    }

    @Test
    fun irish_celticLatinWords_matchesPlatformParity() {
        assertWordBoundaries(
            text = "Dia dhuit a domhan agus maidin mhaith Éire",
            locale = Locale.forLanguageTag("ga"),
        )
    }

    @Test
    fun javanese_austronesianLatin_matchesPlatformParity() {
        assertWordBoundaries(
            text = "Sugeng enjang sedulur lan sugeng rawuh Indonesia",
            locale = Locale.forLanguageTag("jv"),
        )
    }

    @Test
    fun sundanese_austronesianLatin_matchesPlatformParity() {
        assertWordBoundaries(
            text = "Wilujeng enjing sadayana mugia salawasna rahayu",
            locale = Locale.forLanguageTag("su"),
        )
    }

    @Test
    fun maori_polynesianLatinMacron_matchesPlatformParity() {
        assertWordBoundaries(
            text = "Kia ora e te ao me te ata pai Aotearoa",
            locale = Locale.forLanguageTag("mi"),
        )
    }

    @Test
    fun hawaiian_polynesianLatinOkina_matchesPlatformParity() {
        assertWordBoundaries(
            text = "Aloha e ke ao a me ke kakahiaka maika'i Hawaii",
            locale = Locale.forLanguageTag("haw"),
        )
    }

    @Test
    fun lao_unspacedScript_matchesPlatformParity() {
        assertWordBoundaries(
            text = "ພາສາລາວເປັນພາສາທາງການຂອງປະເທດລາວ",
            locale = Locale.forLanguageTag("lo"),
        )
    }

    @Test
    fun burmese_unspacedScript_matchesPlatformParity() {
        assertWordBoundaries(
            text = "မြန်မာစာသည် မြန်မာနိုင်ငံ၏ ရုံးသုံးဘာသာဖြစ်သည်",
            locale = Locale.forLanguageTag("my"),
        )
    }

    @Test
    fun khmer_unspacedScript_matchesPlatformParity() {
        assertWordBoundaries(
            text = "ភាសាខ្មែរជាភាសាផ្លូវការនៃប្រទេសកម្ពុជា",
            locale = Locale.forLanguageTag("km"),
        )
    }

    // -------------------------------------------------------------------------
    // Script-specific EmojiCompat integration tests
    // -------------------------------------------------------------------------

    @Test
    fun emoji_japaneseKanjiWithAppendedEmoji_bridgesAsSingleWord() {
        val text = "猫🐱"
        val iterator = WordIterator(text, 0, text.length, Locale.JAPANESE)
        assertThat(iterator.getNextWordEndOnTwoWordBoundary(0)).isEqualTo(text.length)
        assertThat(iterator.getPrevWordBeginningOnTwoWordsBoundary(text.length)).isEqualTo(0)
    }

    @Test
    fun emoji_chineseHanziWithAppendedEmoji_bridgesAsSingleWord() {
        val text = "开发🚀"
        val iterator = WordIterator(text, 0, text.length, Locale.SIMPLIFIED_CHINESE)
        assertThat(iterator.getNextWordEndOnTwoWordBoundary(0)).isEqualTo(text.length)
        assertThat(iterator.getPrevWordBeginningOnTwoWordsBoundary(text.length)).isEqualTo(0)
    }

    @Test
    fun emoji_thaiWordWithEmoji_segmentsWordAndEmoji() {
        val text = "สวัสดี🙏"
        val iterator = WordIterator(text, 0, text.length, Locale.forLanguageTag("th"))
        // Thai word "สวัสดี" ends at index 6
        assertThat(iterator.getNextWordEndOnTwoWordBoundary(0)).isEqualTo(6)
        // Emoji "🙏" spans 6..8
        assertThat(iterator.getNextWordEndOnTwoWordBoundary(6)).isEqualTo(8)
    }

    @Test
    fun emoji_arabicRtlWithAppendedEmoji_bridgesAsSingleWord() {
        val text = "مبروك🎉"
        val iterator = WordIterator(text, 0, text.length, Locale.forLanguageTag("ar"))
        assertThat(iterator.getNextWordEndOnTwoWordBoundary(0)).isEqualTo(text.length)
        assertThat(iterator.getPrevWordBeginningOnTwoWordsBoundary(text.length)).isEqualTo(0)
    }

    @Test
    fun emoji_zwjFamilySequence_treatedAsSingleWord() {
        val zwjEmoji = "👨‍👩‍👧‍👦"
        val text = "family $zwjEmoji photo"
        val iterator = WordIterator(text, 0, text.length, Locale.ENGLISH)
        val emojiStart = 7
        val emojiEnd = 7 + zwjEmoji.length

        assertThat(iterator.getNextWordEndOnTwoWordBoundary(emojiStart)).isEqualTo(emojiEnd)
        assertThat(iterator.getPrevWordBeginningOnTwoWordsBoundary(emojiEnd)).isEqualTo(emojiStart)
    }

    @Test
    fun emoji_skinToneModifierSequence_treatedAsSingleWord() {
        val modifierEmoji = "👍🏽"
        val text = "vote $modifierEmoji yes"
        val iterator = WordIterator(text, 0, text.length, Locale.ENGLISH)
        val emojiStart = 5
        val emojiEnd = 5 + modifierEmoji.length

        assertThat(iterator.getNextWordEndOnTwoWordBoundary(emojiStart)).isEqualTo(emojiEnd)
        assertThat(iterator.getPrevWordBeginningOnTwoWordsBoundary(emojiEnd)).isEqualTo(emojiStart)
    }

    @Test
    fun emoji_regionalIndicatorFlagSequence_treatedAsSingleWord() {
        val flagEmoji = "🇯🇵"
        val text = "visit $flagEmoji now"
        val iterator = WordIterator(text, 0, text.length, Locale.JAPANESE)
        val emojiStart = 6
        val emojiEnd = 6 + flagEmoji.length

        assertThat(iterator.getNextWordEndOnTwoWordBoundary(emojiStart)).isEqualTo(emojiEnd)
        assertThat(iterator.getPrevWordBeginningOnTwoWordsBoundary(emojiEnd)).isEqualTo(emojiStart)
    }

    @Test
    fun emoji_keycapSequence_treatedAsSingleWord() {
        val keycapEmoji = "1️⃣"
        val text = "step $keycapEmoji done"
        val iterator = WordIterator(text, 0, text.length, Locale.ENGLISH)
        val emojiStart = 5
        val emojiEnd = 5 + keycapEmoji.length

        assertThat(iterator.getNextWordEndOnTwoWordBoundary(emojiStart)).isEqualTo(emojiEnd)
        assertThat(iterator.getPrevWordBeginningOnTwoWordsBoundary(emojiEnd)).isEqualTo(emojiStart)
    }

    // -------------------------------------------------------------------------
    // Punctuation tests
    // -------------------------------------------------------------------------

    @Test
    fun punctuation_cjkQuotesAndBrackets_isolatedCorrectly() {
        val text = "「こんにちは」『世界』"
        val iterator = WordIterator(text, 0, text.length, Locale.JAPANESE)

        assertThat(iterator.isOnPunctuation(0)).isTrue()
        assertThat(iterator.getPunctuationBeginning(0)).isEqualTo(0)
        assertThat(iterator.getPunctuationEnd(0)).isEqualTo(1)
    }

    @Test
    fun punctuation_fullwidthStopsAndCommas_isolatedCorrectly() {
        val text = "あ、い。う！"
        val iterator = WordIterator(text, 0, text.length, Locale.JAPANESE)

        assertThat(iterator.isOnPunctuation(1)).isTrue()
        assertThat(iterator.isAfterPunctuation(2)).isTrue()
    }
}
