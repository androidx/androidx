/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.material3

import kotlin.test.Test


@OptIn(ExperimentalMaterial3Api::class)
internal class PlatformDateFormatTest {

    @Test
    fun hourCycleLocalization() {
        var locale = calendarLocale("en","US")
        assertThat(PlatformDateFormat.is24HourFormat(locale)).isEqualTo(false)

        locale = calendarLocale("en","CA")
        assertThat(PlatformDateFormat.is24HourFormat(locale)).isEqualTo(false)

        locale = calendarLocale("ar","EG")
        assertThat(PlatformDateFormat.is24HourFormat(locale)).isEqualTo(false)

        locale = calendarLocale("ko","KR")
        assertThat(PlatformDateFormat.is24HourFormat(locale)).isEqualTo(false)

        locale = calendarLocale("en","GB")
        assertThat(PlatformDateFormat.is24HourFormat(locale)).isEqualTo(true)

        locale = calendarLocale("ru","RU")
        assertThat(PlatformDateFormat.is24HourFormat(locale)).isEqualTo(true)

        locale = calendarLocale("de","DE")
        assertThat(PlatformDateFormat.is24HourFormat(locale)).isEqualTo(true)
    }

}
