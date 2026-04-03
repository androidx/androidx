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

package androidx.compose.material3

import android.os.Build
import androidx.compose.material3.internal.CalendarDate
import androidx.compose.material3.internal.CalendarModel
import androidx.compose.material3.internal.CalendarModelImpl
import androidx.compose.material3.internal.DaysInWeek
import androidx.compose.material3.internal.LegacyCalendarModelImpl
import androidx.compose.material3.internal.formatWithSkeleton
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import java.util.Locale
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@MediumTest
@RunWith(Parameterized::class)
internal class CalendarModelTest(private val model: CalendarModel) {

    private lateinit var defaultLocale: Locale

    @Before
    fun before() {
        // Using the JVM Locale.getDefault() for testing purposes only.
        defaultLocale = Locale.getDefault()
    }

    @After
    fun after() {
        Locale.setDefault(defaultLocale)
    }

    @Test
    fun dateCreation() {
        val date = model.getCanonicalDate(January2022Millis) // 1/1/2022
        assertThat(date.year).isEqualTo(2022)
        assertThat(date.month).isEqualTo(1)
        assertThat(date.dayOfMonth).isEqualTo(1)
        assertThat(date.utcTimeMillis).isEqualTo(January2022Millis)
    }

    @Test
    fun dateCreation_withRounding() {
        val date = model.getCanonicalDate(January2022Millis + 30000) // 1/1/2022 + 30000 millis
        assertThat(date.year).isEqualTo(2022)
        assertThat(date.month).isEqualTo(1)
        assertThat(date.dayOfMonth).isEqualTo(1)
        // Check that the milliseconds represent the start of the day.
        assertThat(date.utcTimeMillis).isEqualTo(January2022Millis)
    }

    @Test
    fun dateRestore() {
        val date =
            CalendarDate(year = 2022, month = 1, dayOfMonth = 1, utcTimeMillis = January2022Millis)
        assertThat(model.getCanonicalDate(date.utcTimeMillis)).isEqualTo(date)
    }

    @Test
    fun monthCreation() {
        val date =
            CalendarDate(year = 2022, month = 1, dayOfMonth = 1, utcTimeMillis = January2022Millis)
        val monthFromDate = model.getMonth(date)
        val monthFromMilli = model.getMonth(January2022Millis)
        val monthFromYearMonth = model.getMonth(year = 2022, month = 1)
        assertThat(monthFromDate).isEqualTo(monthFromMilli)
        assertThat(monthFromDate).isEqualTo(monthFromYearMonth)
    }

    @Test
    fun monthCreation_withRounding() {
        val date =
            CalendarDate(year = 2022, month = 1, dayOfMonth = 1, utcTimeMillis = January2022Millis)
        val monthFromDate = model.getMonth(date)
        val monthFromMilli = model.getMonth(January2022Millis + 10000)
        assertThat(monthFromDate).isEqualTo(monthFromMilli)
    }

    @Test
    fun monthRestore() {
        val month = model.getMonth(year = 1999, month = 12)
        assertThat(model.getMonth(month.startUtcTimeMillis)).isEqualTo(month)
    }

    @Test
    fun plusMinusMonth() {
        val month = model.getMonth(January2022Millis) // 1/1/2022
        val expectedNextMonth = model.getMonth(month.endUtcTimeMillis + 1) // 2/1/2022
        val plusMonth = model.plusMonths(from = month, addedMonthsCount = 1)
        assertThat(plusMonth).isEqualTo(expectedNextMonth)
        assertThat(model.minusMonths(from = plusMonth, subtractedMonthsCount = 1)).isEqualTo(month)
    }

    @Test
    fun parseDate() {
        val expectedDate =
            CalendarDate(year = 2022, month = 1, dayOfMonth = 1, utcTimeMillis = January2022Millis)
        val parsedDate = model.parse("1/1/2022", "M/d/yyyy", Locale.ENGLISH)
        assertThat(parsedDate).isEqualTo(expectedDate)
    }

    @Test
    fun parseDate_Arabic() {
        val expectedDate =
            CalendarDate(year = 2022, month = 1, dayOfMonth = 1, utcTimeMillis = January2022Millis)
        // Arabic locale with Arabic-Indic digits and symbols
        val parsedDate =
            model.parse("٠١/٠١/٢٠٢٢", "d/MM/yyyy", Locale.forLanguageTag("ar-u-nu-arab"))
        assertThat(parsedDate).isEqualTo(expectedDate)
    }

    @Test
    fun formatDate() {
        val date =
            CalendarDate(year = 2022, month = 1, dayOfMonth = 1, utcTimeMillis = January2022Millis)
        assertThat(model.formatWithSkeleton(date, "yMMMd")).isEqualTo("Jan 1, 2022")
        assertThat(model.formatWithSkeleton(date, "dMMMy")).isEqualTo("Jan 1, 2022")
        assertThat(model.formatWithSkeleton(date, "yMMMMEEEEd"))
            .isEqualTo("Saturday, January 1, 2022")
        // Check that the direct formatting is equal to the one the model does.
        assertThat(model.formatWithSkeleton(date, "yMMMd")).isEqualTo(date.format(model, "yMMMd"))
    }

    @Test
    fun formatMonth() {
        val month = model.getMonth(year = 2022, month = 3)
        assertThat(model.formatWithSkeleton(month, "yMMMM")).isEqualTo("March 2022")
        assertThat(model.formatWithSkeleton(month, "MMMMy")).isEqualTo("March 2022")
        // Check that the direct formatting is equal to the one the model does.
        assertThat(model.formatWithSkeleton(month, "yMMMM")).isEqualTo(month.format(model, "yMMMM"))
    }

    @Test
    fun formatWithSkeleton() {
        // Format twice to check that the formatter is outputting the right values, and that it's
        // not failing due to cache issues.
        var formatted =
            formatWithSkeleton(
                January2022Millis,
                DatePickerDefaults.YearMonthWeekdayDaySkeleton,
                Locale.US,
                cache = mutableMapOf(),
            )
        assertThat(formatted).isEqualTo("Saturday, January 1, 2022")

        formatted =
            formatWithSkeleton(
                January2022Millis - 1000000000,
                DatePickerDefaults.YearMonthWeekdayDaySkeleton,
                Locale.US,
                cache = mutableMapOf(),
            )
        assertThat(formatted).isEqualTo("Monday, December 20, 2021")
    }

    @Test
    fun formatWithSkeletonProducingEqualPattern() {
        // Format twice to check that the formatter is outputting the right values, and that it's
        // not failing due to cache issues.
        var formatted =
            formatWithSkeleton(
                January2022Millis,
                skeleton = "YY",
                Locale.US,
                cache = mutableMapOf(),
            )
        assertThat(formatted).isEqualTo("22")

        formatted =
            formatWithSkeleton(
                January2022Millis - 1000000000,
                "YY",
                Locale.US,
                cache = mutableMapOf(),
            )
        assertThat(formatted).isEqualTo("21")
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.N)
    @Test
    fun formatWithSkeletonCapitalizationForLocale() {
        // English
        val usFormatted =
            formatWithSkeleton(
                January2022Millis,
                skeleton = "yMMMM",
                Locale.US,
                cache = mutableMapOf(),
            )
        // Spanish
        val esFormatted =
            formatWithSkeleton(
                January2022Millis,
                skeleton = "yMMMM",
                Locale("ES"),
                cache = mutableMapOf(),
            )
        // Danish
        val daFormatted =
            formatWithSkeleton(
                January2022Millis,
                skeleton = "yMMMM",
                Locale("DA"),
                cache = mutableMapOf(),
            )
        // Note: Expecting the month names to be capitalized in English and Spanish, but lowercase
        // in Danish.
        assertThat(usFormatted).isEqualTo("January 2022")
        assertThat(esFormatted).isEqualTo("Enero de 2022")
        assertThat(daFormatted).isEqualTo("januar 2022")
    }

    @Test
    fun weekdayNames() {
        // Ensure we are running on a US locale for this test.
        Locale.setDefault(Locale.US)
        val weekDays = model.weekdayNames
        assertThat(weekDays).hasSize(DaysInWeek)
        // Check that the first day is always "Monday", per ISO-8601 standard.
        assertThat(weekDays.first().first).ignoringCase().contains("Monday")
        weekDays.forEach {
            assertThat(it.second.first().lowercaseChar())
                .isEqualTo(it.first.first().lowercaseChar())
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun dateInputFormat() {
        // Note: This test ignores the parameterized model, as we need to create a new model per
        // Locale.
        var newModel = CalendarModelImpl(Locale.US)
        var legacyModel = LegacyCalendarModelImpl(Locale.US)

        assertThat(newModel.getDateInputFormat().patternWithDelimiters).isEqualTo("MM/dd/yyyy")
        assertThat(newModel.getDateInputFormat().patternWithoutDelimiters).isEqualTo("MMddyyyy")
        assertThat(newModel.getDateInputFormat().delimiter).isEqualTo('/')
        assertThat(legacyModel.getDateInputFormat().patternWithDelimiters).isEqualTo("MM/dd/yyyy")
        assertThat(legacyModel.getDateInputFormat().patternWithoutDelimiters).isEqualTo("MMddyyyy")
        assertThat(legacyModel.getDateInputFormat().delimiter).isEqualTo('/')

        newModel = CalendarModelImpl(Locale.CHINA)
        legacyModel = LegacyCalendarModelImpl(Locale.CHINA)
        assertThat(newModel.getDateInputFormat().patternWithDelimiters).isEqualTo("yyyy/MM/dd")
        assertThat(newModel.getDateInputFormat().patternWithoutDelimiters).isEqualTo("yyyyMMdd")
        assertThat(newModel.getDateInputFormat().delimiter).isEqualTo('/')
        assertThat(legacyModel.getDateInputFormat().patternWithDelimiters).isEqualTo("yyyy/MM/dd")
        assertThat(legacyModel.getDateInputFormat().patternWithoutDelimiters).isEqualTo("yyyyMMdd")
        assertThat(legacyModel.getDateInputFormat().delimiter).isEqualTo('/')

        newModel = CalendarModelImpl(Locale.UK)
        legacyModel = LegacyCalendarModelImpl(Locale.UK)
        assertThat(newModel.getDateInputFormat().patternWithDelimiters).isEqualTo("dd/MM/yyyy")
        assertThat(newModel.getDateInputFormat().patternWithoutDelimiters).isEqualTo("ddMMyyyy")
        assertThat(newModel.getDateInputFormat().delimiter).isEqualTo('/')
        assertThat(legacyModel.getDateInputFormat().patternWithDelimiters).isEqualTo("dd/MM/yyyy")
        assertThat(legacyModel.getDateInputFormat().patternWithoutDelimiters).isEqualTo("ddMMyyyy")
        assertThat(legacyModel.getDateInputFormat().delimiter).isEqualTo('/')

        newModel = CalendarModelImpl(Locale.KOREA)
        legacyModel = LegacyCalendarModelImpl(Locale.KOREA)
        assertThat(newModel.getDateInputFormat().patternWithDelimiters).isEqualTo("yyyy.MM.dd")
        assertThat(newModel.getDateInputFormat().patternWithoutDelimiters).isEqualTo("yyyyMMdd")
        assertThat(newModel.getDateInputFormat().delimiter).isEqualTo('.')
        assertThat(legacyModel.getDateInputFormat().patternWithDelimiters).isEqualTo("yyyy.MM.dd")
        assertThat(legacyModel.getDateInputFormat().patternWithoutDelimiters).isEqualTo("yyyyMMdd")
        assertThat(legacyModel.getDateInputFormat().delimiter).isEqualTo('.')

        newModel = CalendarModelImpl(Locale("es", "CL"))
        legacyModel = LegacyCalendarModelImpl(Locale("es", "CL"))
        assertThat(newModel.getDateInputFormat().patternWithDelimiters).isEqualTo("dd-MM-yyyy")
        assertThat(newModel.getDateInputFormat().patternWithoutDelimiters).isEqualTo("ddMMyyyy")
        assertThat(newModel.getDateInputFormat().delimiter).isEqualTo('-')
        assertThat(legacyModel.getDateInputFormat().patternWithDelimiters).isEqualTo("dd-MM-yyyy")
        assertThat(legacyModel.getDateInputFormat().patternWithoutDelimiters).isEqualTo("ddMMyyyy")
        assertThat(legacyModel.getDateInputFormat().delimiter).isEqualTo('-')
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun equalModelsOutput() {
        // Note: This test ignores the parameters and just runs a few equality tests for the output.
        // It will execute twice, but that should to tolerable :)
        // Using the JVM Locale.getDefault() for testing purposes only.
        val newModel = CalendarModelImpl(Locale.getDefault())
        val legacyModel = LegacyCalendarModelImpl(Locale.getDefault())

        val date = newModel.getCanonicalDate(January2022Millis) // 1/1/2022
        val legacyDate = legacyModel.getCanonicalDate(January2022Millis)
        val month = newModel.getMonth(date)
        val legacyMonth = legacyModel.getMonth(date)

        assertThat(newModel.today).isEqualTo(legacyModel.today)
        assertThat(month).isEqualTo(legacyMonth)
        assertThat(newModel.getDateInputFormat()).isEqualTo(legacyModel.getDateInputFormat())
        assertThat(newModel.plusMonths(month, 3)).isEqualTo(legacyModel.plusMonths(month, 3))
        assertThat(date).isEqualTo(legacyDate)
        assertThat(newModel.getDayOfWeek(date)).isEqualTo(legacyModel.getDayOfWeek(date))
        assertThat(newModel.formatWithSkeleton(date, "dMMMy"))
            .isEqualTo(legacyModel.formatWithSkeleton(date, "dMMMy"))
        assertThat(newModel.formatWithSkeleton(month, "MMMy"))
            .isEqualTo(legacyModel.formatWithSkeleton(month, "MMMy"))
    }

    @Test
    fun toLocalStringFormat() {
        assertThat(2.toLocalString(locale = Locale.forLanguageTag("en"))).isEqualTo("2")
        assertThat(2.toLocalString(locale = Locale.forLanguageTag("ar-u-nu-arab"))).isEqualTo("٢")
        assertThat(2.toLocalString(locale = Locale.forLanguageTag("ar-u-nu-latn"))).isEqualTo("2")
    }

    internal companion object {
        @Parameterized.Parameters(name = "{0}")
        @JvmStatic
        fun parameters() =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Using the JVM Locale.getDefault() for testing purposes only.
                arrayOf(
                    CalendarModelImpl(Locale.getDefault()),
                    LegacyCalendarModelImpl(Locale.getDefault()),
                )
            } else {
                arrayOf(LegacyCalendarModelImpl(Locale.getDefault()))
            }
    }
}

private const val January2022Millis = 1640995200000
