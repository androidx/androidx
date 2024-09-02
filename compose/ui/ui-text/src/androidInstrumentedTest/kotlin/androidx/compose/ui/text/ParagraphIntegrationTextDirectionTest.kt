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

package androidx.compose.ui.text

import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.style.ResolvedTextDirection
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import java.util.Locale
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class ParagraphIntegrationTextDirectionTest {

    private lateinit var defaultLocale: Locale
    private val context = InstrumentationRegistry.getInstrumentation().context
    private val defaultDensity = Density(density = 1f)
    private val resourceLoader = UncachedFontFamilyResolver(context)
    private val ltrLocaleList = LocaleList("en")
    private val rtlLocaleList = LocaleList("ar")
    private val rtlLocale = Locale("ar")
    private val ltrLocale = Locale.ENGLISH

    @Before
    fun before() {
        defaultLocale = Locale.getDefault()
    }

    @After
    fun after() {
        Locale.setDefault(defaultLocale)
    }

    @Test
    fun unspecifiedTextDirection_withLtrLocale_resolvesToLtr() {
        Locale.setDefault(ltrLocale)

        val paragraph =
            Paragraph(
                text = "",
                style = TextStyle(textDirection = TextDirection.Unspecified),
                constraints = Constraints(),
                density = defaultDensity,
                fontFamilyResolver = resourceLoader,
                overflow = TextOverflow.Clip
            )

        assertThat(paragraph.getParagraphDirection(0)).isEqualTo(ResolvedTextDirection.Ltr)
    }

    @Test
    fun unspecifiedTextDirection_withRtlLocale_resolvesToRtl() {
        Locale.setDefault(rtlLocale)

        val paragraph =
            Paragraph(
                text = "",
                style = TextStyle(textDirection = TextDirection.Unspecified),
                constraints = Constraints(),
                density = defaultDensity,
                fontFamilyResolver = resourceLoader,
                overflow = TextOverflow.Clip
            )

        assertThat(paragraph.getParagraphDirection(0)).isEqualTo(ResolvedTextDirection.Rtl)
    }

    @Test
    fun unspecifiedTextDirection_withLtrLocaleList_resolvesToLtr() {
        val paragraph =
            Paragraph(
                text = "",
                style =
                    TextStyle(
                        textDirection = TextDirection.Unspecified,
                        localeList = ltrLocaleList
                    ),
                constraints = Constraints(),
                density = defaultDensity,
                fontFamilyResolver = resourceLoader,
                overflow = TextOverflow.Clip
            )

        assertThat(paragraph.getParagraphDirection(0)).isEqualTo(ResolvedTextDirection.Ltr)
    }

    @Test
    fun unspecifiedTextDirection_withRtlLocaleList_resolvesToRtl() {
        val paragraph =
            Paragraph(
                text = "",
                style =
                    TextStyle(
                        textDirection = TextDirection.Unspecified,
                        localeList = rtlLocaleList
                    ),
                constraints = Constraints(),
                density = defaultDensity,
                fontFamilyResolver = resourceLoader,
                overflow = TextOverflow.Clip
            )

        assertThat(paragraph.getParagraphDirection(0)).isEqualTo(ResolvedTextDirection.Rtl)
    }
}
