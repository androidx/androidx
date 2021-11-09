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

package android.support.wearable.complications

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.wear.watchface.complications.data.SharedRobolectricTestRunner
import com.google.common.truth.Truth
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Calendar
import java.util.GregorianCalendar
import java.util.TimeZone
import java.util.concurrent.TimeUnit

@RunWith(SharedRobolectricTestRunner::class)
public class TimeFormatTextTest {
    private val mResources = ApplicationProvider.getApplicationContext<Context>().resources

    @Test
    public fun testDefaultStyleFormat() {
        // GIVEN a TimeFormatText object using the default style
        val tft = TimeFormatText(
            "E 'in' LLL",
            ComplicationText.FORMAT_STYLE_DEFAULT,
            null
        )

        // WHEN getText is called for a given date
        val result =
            tft.getTextAt(mResources, GregorianCalendar(2016, 2, 4).timeInMillis)

        // THEN the results are in the default case.
        Assert.assertEquals("Fri in Mar", result)
    }

    @Test
    public fun testUpperCaseStyleFormat() {
        // GIVEN a TimeFormatText object using the upper case style
        val tft = TimeFormatText(
            "E 'in' LLL",
            ComplicationText.FORMAT_STYLE_UPPER_CASE,
            null
        )

        // WHEN getText is called for a given date
        val result =
            tft.getTextAt(mResources, GregorianCalendar(2016, 2, 4).timeInMillis)

        // THEN the results are in upper case.
        Assert.assertEquals("FRI IN MAR", result)
    }

    @Test
    public fun testLowerCaseStyleFormat() {
        // GIVEN a TimeFormatText object using the lower case style
        val tft = TimeFormatText(
            "E 'in' LLL",
            ComplicationText.FORMAT_STYLE_LOWER_CASE,
            null
        )

        // WHEN getText is called for a given date
        val result =
            tft.getTextAt(mResources, GregorianCalendar(2016, 2, 4).timeInMillis)

        // THEN the results are in lower case.
        Assert.assertEquals("fri in mar", result)
    }

    @Test
    public fun testFormatWithTimeZone() {
        // GIVEN a TimeFormatText object using the default style and a time zone specified.
        val tft = TimeFormatText(
            "HH:mm",
            ComplicationText.FORMAT_STYLE_DEFAULT,
            TimeZone.getTimeZone("America/Los_Angeles")
        )
        val calendar =
            GregorianCalendar(TimeZone.getTimeZone("GMT+0"))
        calendar[2016, 2, 4, 15, 12] = 58

        // WHEN getText is called for a given date
        val result = tft.getTextAt(mResources, calendar.timeInMillis)

        // THEN the results are adjusted for the time zone.
        Assert.assertEquals("07:12", result)
    }

    @Test
    public fun testReturnSameTime() {
        // GIVEN a TimeFormatText object using the lower case style
        var tft = TimeFormatText(
            "HH:mm",
            ComplicationText.FORMAT_STYLE_LOWER_CASE,
            null
        )

        // WHEN returnsSameText is called for two time in the different period.
        // THEN the result of getText should be different.
        var testTime1 =
            TimeUnit.HOURS.toMillis(23) + TimeUnit.MINUTES.toMillis(10)
        var testTime2 = testTime1 + TimeUnit.MINUTES.toMillis(30)
        Assert.assertFalse(tft.returnsSameText(testTime1, testTime2))

        // WHEN returnsSameText is called for two time in the same period.
        // THEN the result of getText should be the same.
        testTime1 =
            TimeUnit.HOURS.toMillis(23) + TimeUnit.MINUTES.toMillis(10)
        testTime2 = testTime1 + TimeUnit.SECONDS.toMillis(30)
        Assert.assertTrue(tft.returnsSameText(testTime1, testTime2))

        // GIVEN a TimeFormatText object using the lower case style and a specify time zone.
        val tz = TimeZone.getTimeZone("America/Anchorage")
        tft = TimeFormatText("d", ComplicationText.FORMAT_STYLE_LOWER_CASE, tz)
        val cal = Calendar.getInstance()

        // WHEN returnsSameText is called for two time in different date.
        // THEN the result of getText should be the same.
        cal.timeZone = tz
        cal[2016, 7, 7, 23, 59] = 59
        val time1 = cal.timeInMillis
        cal[2016, 7, 8, 0, 0] = 0
        val time2 = cal.timeInMillis
        Assert.assertFalse(tft.returnsSameText(time1, time2))
    }

    @Test
    public fun testTimeFormatWithTimeZone() {
        val complicationText = TimeFormatText(
            "HH:mm",
            ComplicationText.FORMAT_STYLE_DEFAULT,
            TimeZone.getTimeZone("Asia/Seoul")
        )
        val calendar =
            GregorianCalendar(TimeZone.getTimeZone("GMT+0"))
        calendar[2016, 2, 4, 18, 52] = 58
        val result =
            complicationText.getTextAt(mResources, calendar.timeInMillis)
        Assert.assertEquals("03:52", result.toString())
    }

    @Test
    public fun testParcelTimeFormatTextWithoutTimeZone() {
        // GIVEN TimeFormatText with no time zone
        val originalText = TimeFormatText(
            "EEE 'the' d LLL",
            ComplicationText.FORMAT_STYLE_LOWER_CASE,
            null
        )

        // WHEN the object is parcelled and unparcelled
        val newText = originalText.roundTripParcelable()!!

        // THEN the object behaves as expected.
        val result =
            newText.getTextAt(mResources, GregorianCalendar(2016, 2, 4).timeInMillis)
        Assert.assertEquals("fri the 4 mar", result.toString())
    }

    @Test
    public fun testParcelTimeFormatTextWithTimeZone() {
        // GIVEN TimeFormatText with a time zone specified
        val originalText = TimeFormatText(
            "EEE 'the' d LLL HH:mm",
            ComplicationText.FORMAT_STYLE_LOWER_CASE,
            TimeZone.getTimeZone("GMT+5")
        )

        // WHEN the object is parcelled and unparcelled
        val newText = originalText.roundTripParcelable()!!

        // THEN the object behaves as expected.
        val calendar = GregorianCalendar(TimeZone.getTimeZone("GMT+0"))
        calendar[2016, 2, 4, 18, 52] = 58
        val result = newText.getTextAt(mResources, calendar.timeInMillis)
        Assert.assertEquals("fri the 4 mar 23:52", result.toString())
    }

    @Test
    public fun nextChangeTimeSeconds() {
        val timeFormatText = TimeFormatText(
            "HH:mm:ss",
            ComplicationText.FORMAT_STYLE_DEFAULT,
            TimeZone.getDefault()
        )

        // Next change is at the next second boundary.
        Truth.assertThat(timeFormatText.getNextChangeTime(1000035789L))
            .isEqualTo(1000036000L)
        Truth.assertThat(timeFormatText.getNextChangeTime(7000060000L))
            .isEqualTo(7000061000L)
        Truth.assertThat(timeFormatText.getNextChangeTime(1900000001L))
            .isEqualTo(1900001000L)
        Truth.assertThat(timeFormatText.getNextChangeTime(1111100999L))
            .isEqualTo(1111101000L)
    }

    @Test
    public fun nextChangeTimeMinutely() {
        val timeFormatText = TimeFormatText(
            "HH:mm",
            ComplicationText.FORMAT_STYLE_DEFAULT,
            TimeZone.getTimeZone("Asia/Seoul")
        )

        // Next change is at the next minute boundary.
        Truth.assertThat(timeFormatText.getNextChangeTime(6000035789L))
            .isEqualTo(6000060000L)
        Truth.assertThat(timeFormatText.getNextChangeTime(6000060000L))
            .isEqualTo(6000120000L)
        Truth.assertThat(timeFormatText.getNextChangeTime(6000060001L))
            .isEqualTo(6000120000L)
        Truth.assertThat(timeFormatText.getNextChangeTime(6000059999L))
            .isEqualTo(6000060000L)
    }

    @Test
    public fun nextChangeTimeHourly() {
        val timeFormatText = TimeFormatText(
            "d LLL HH",
            ComplicationText.FORMAT_STYLE_DEFAULT,
            TimeZone.getDefault()
        )

        // Next change is at the next hour boundary.
        Truth.assertThat(timeFormatText.getNextChangeTime(36000035789L))
            .isEqualTo(36003600000L)
        Truth.assertThat(timeFormatText.getNextChangeTime(36000600000L))
            .isEqualTo(36003600000L)
        Truth.assertThat(timeFormatText.getNextChangeTime(36003600000L))
            .isEqualTo(36007200000L)
        Truth.assertThat(timeFormatText.getNextChangeTime(36007199999L))
            .isEqualTo(36007200000L)
    }

    @Test
    public fun nextChangeTimeDailyUtc() {
        val timeFormatText = TimeFormatText(
            "d LLL",
            ComplicationText.FORMAT_STYLE_DEFAULT,
            TimeZone.getTimeZone("UTC")
        )

        // Next change is at the next day boundary.
        Truth.assertThat(timeFormatText.getNextChangeTime(8640000035789L))
            .isEqualTo(8640086400000L)
        Truth.assertThat(timeFormatText.getNextChangeTime(8640086399999L))
            .isEqualTo(8640086400000L)
        Truth.assertThat(timeFormatText.getNextChangeTime(8640086400000L))
            .isEqualTo(8640172800000L)
    }

    @Test
    public fun nextChangeTimeDailyTimeZone() {
        val timeFormatText = TimeFormatText(
            "d LLL",
            ComplicationText.FORMAT_STYLE_DEFAULT,
            TimeZone.getTimeZone("GMT-5")
        )
        val calendar: Calendar =
            GregorianCalendar(TimeZone.getTimeZone("UTC"))
        calendar[2018, Calendar.APRIL, 17, 8, 0] = 0
        calendar[Calendar.MILLISECOND] = 0
        val inputTime = calendar.timeInMillis

        // Next change is at midnight in the specified time zone.
        calendar[2018, Calendar.APRIL, 18, 5, 0] = 0
        val expectedTime = calendar.timeInMillis
        Truth.assertThat(timeFormatText.getNextChangeTime(inputTime))
            .isEqualTo(expectedTime)
    }

    @Test
    public fun nextChangeTimeDailyTimeZoneNextDay() {
        val timeFormatText = TimeFormatText(
            "d LLL",
            ComplicationText.FORMAT_STYLE_DEFAULT,
            TimeZone.getTimeZone("GMT+5")
        )
        val calendar: Calendar =
            GregorianCalendar(TimeZone.getTimeZone("UTC"))
        calendar[2018, Calendar.APRIL, 17, 23, 0] = 0
        calendar[Calendar.MILLISECOND] = 0
        val inputTime = calendar.timeInMillis

        // Next change is at midnight in the specified time zone, the day after the start time
        // (which
        // is after midnight in GMT+5).
        calendar[2018, Calendar.APRIL, 18, 19, 0] = 0
        val expectedTime = calendar.timeInMillis
        Truth.assertThat(timeFormatText.getNextChangeTime(inputTime))
            .isEqualTo(expectedTime)
    }

    @Test
    public fun testTimeFormatTextGetPrecision() {
        val dateFormats = arrayOf(
            "S",
            "s",
            "d",
            "m",
            "h",
            "D",
            "W",
            "y",
            "E",
            "w",
            "EEE 'the' d LLL HH:mm",
            "'now time is' HH 'o''clock'",
            "yyyy-MM-dd",
            "yyyy-MM-dd HH:mm",
            "yyyy-MM-dd HH:mmZ",
            "yyyy-MM-dd HH:mm:ss.SSSZ",
            "yyyy-MM-dd'T'HH:mm:ss.SSSZ",
            "yyyy ''' hello'",
            "yyyy ''''' hello' s",
            "LLL",
            "LLL a"
        )
        val dateFormatPrecision = longArrayOf(
            TimeUnit.SECONDS.toMillis(1),
            TimeUnit.SECONDS.toMillis(1),
            TimeUnit.DAYS.toMillis(1),
            TimeUnit.MINUTES.toMillis(1),
            TimeUnit.HOURS.toMillis(1),
            TimeUnit.DAYS.toMillis(1),
            TimeUnit.DAYS.toMillis(1),
            TimeUnit.DAYS.toMillis(1),
            TimeUnit.DAYS.toMillis(1),
            TimeUnit.DAYS.toMillis(1),
            TimeUnit.MINUTES.toMillis(1),
            TimeUnit.HOURS.toMillis(1),
            TimeUnit.DAYS.toMillis(1),
            TimeUnit.MINUTES.toMillis(1),
            TimeUnit.MINUTES.toMillis(1),
            TimeUnit.SECONDS.toMillis(1),
            TimeUnit.SECONDS.toMillis(1),
            TimeUnit.DAYS.toMillis(1),
            TimeUnit.SECONDS.toMillis(1),
            TimeUnit.DAYS.toMillis(1),
            TimeUnit.HOURS.toMillis(12)
        )
        for (i in dateFormats.indices) {
            val text = TimeFormatText(
                dateFormats[i],
                ComplicationText.FORMAT_STYLE_DEFAULT,
                null
            )
            Assert.assertEquals(dateFormatPrecision[i], text.precision)
        }
    }
}