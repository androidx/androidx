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
import android.os.Parcel
import android.support.wearable.complications.ComplicationText.TimeDifferenceBuilder
import android.support.wearable.complications.ComplicationText.TimeFormatBuilder
import androidx.test.core.app.ApplicationProvider
import androidx.wear.watchface.complications.SharedRobolectricTestRunner
import com.google.common.truth.Truth
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import java.util.GregorianCalendar
import java.util.TimeZone
import java.util.concurrent.TimeUnit

@RunWith(SharedRobolectricTestRunner::class)
public class ComplicationTextTest {
    private val mResources = ApplicationProvider.getApplicationContext<Context>().resources

    @Test
    public fun testPlainText() {
        // GIVEN ComplicationText of the plain string type
        val complicationText =
            ComplicationText.plainText("hello")

        // WHEN getText is called
        // THEN the plain string is returned.
        Assert.assertEquals(
            "hello",
            complicationText.getTextAt(mResources, 132456789).toString()
        )
    }

    @Test
    public fun testTimeDifferenceShortSingleUnitOnly() {
        // GIVEN ComplicationText with short single unit time difference text only
        val refTime: Long = 10000000
        val complicationText =
            TimeDifferenceBuilder()
                .setReferencePeriodStartMillis(refTime - 156561)
                .setReferencePeriodEndMillis(refTime)
                .setStyle(ComplicationText.DIFFERENCE_STYLE_SHORT_SINGLE_UNIT)
                .build()

        // WHEN getText is called for a time after the ref period
        // THEN the time difference text is returned and "2h 35m" should be rounded to "3h".
        var testTime = refTime +
            TimeUnit.MINUTES.toMillis(35) +
            TimeUnit.HOURS.toMillis(2)
        Assert.assertEquals(
            "3h",
            complicationText.getTextAt(mResources, testTime).toString()
        )

        // WHEN getText is called for a time after the ref period
        // THEN the time difference text is returned and "23h 59m 10s" should be rounded to "1d".
        testTime = refTime +
            TimeUnit.SECONDS.toMillis(10) +
            TimeUnit.MINUTES.toMillis(59) +
            TimeUnit.HOURS.toMillis(23)
        Assert.assertEquals(
            "1d",
            complicationText.getTextAt(mResources, testTime).toString()
        )

        // THEN the time difference text is returned, and "10m 10s" should be rounded to "11m".
        testTime = refTime +
            TimeUnit.SECONDS.toMillis(10) +
            TimeUnit.MINUTES.toMillis(10)
        Assert.assertEquals(
            "11m",
            complicationText.getTextAt(mResources, testTime).toString()
        )

        // WHEN getText is called for a time after the ref period
        // THEN the time difference text is returned and "23h 15m" should be rounded to "1d".
        testTime = refTime +
            TimeUnit.MINUTES.toMillis(15) +
            TimeUnit.HOURS.toMillis(23)
        Assert.assertEquals(
            "1d",
            complicationText.getTextAt(mResources, testTime).toString()
        )

        // WHEN getText is called for a time after the ref period
        // THEN the time difference text is returned and "23h 0m" should be round to "23h".
        testTime = refTime + TimeUnit.HOURS.toMillis(23)
        Assert.assertEquals(
            "23h",
            complicationText.getTextAt(mResources, testTime).toString()
        )

        // WHEN getText is called for a time after the ref period
        // THEN the time difference text is returned and "23h 10m" should be round to "1d".
        testTime = refTime +
            TimeUnit.MINUTES.toMillis(10) +
            TimeUnit.HOURS.toMillis(23)
        Assert.assertEquals(
            "1d",
            complicationText.getTextAt(mResources, testTime).toString()
        )

        // WHEN getText is called for a time after the ref period
        // THEN the time difference text is returned and "22h 10m" should be round to "23h".
        testTime = refTime +
            TimeUnit.MINUTES.toMillis(10) +
            TimeUnit.HOURS.toMillis(22)
        Assert.assertEquals(
            "23h",
            complicationText.getTextAt(mResources, testTime).toString()
        )

        // WHEN getText is called for a time after the ref period
        // THEN the time difference text is returned and "1d 10h" should be round to "2d".
        testTime = refTime +
            TimeUnit.HOURS.toMillis(10) +
            TimeUnit.DAYS.toMillis(1)
        Assert.assertEquals(
            "2d",
            complicationText.getTextAt(mResources, testTime).toString()
        )

        // WHEN getText is called for a time after the ref period
        // THEN the time difference text is returned and "22h 10m" should be round to "23h".
        testTime = refTime +
            TimeUnit.MINUTES.toMillis(10) +
            TimeUnit.HOURS.toMillis(22)
        Assert.assertEquals(
            "23h",
            complicationText.getTextAt(mResources, testTime).toString()
        )

        // WHEN getText is called for a time after the ref period
        // THEN the time difference text is returned and "59m 30s" should be round to "1h".
        testTime = refTime +
            TimeUnit.SECONDS.toMillis(30) +
            TimeUnit.MINUTES.toMillis(59)
        Assert.assertEquals(
            "1h",
            complicationText.getTextAt(mResources, testTime).toString()
        )

        // WHEN getText is called for a time after the ref period
        // THEN the time difference text is returned and "59m 00s" should be displayed as "59m".
        testTime = refTime + TimeUnit.MINUTES.toMillis(59)
        Assert.assertEquals(
            "59m",
            complicationText.getTextAt(mResources, testTime).toString()
        )
    }

    @Test
    public fun testTimeDifferenceShortDualUnitOnly() {
        // GIVEN ComplicationText with short dual unit time difference text only
        val refTime: Long = 10000000
        val complicationText =
            TimeDifferenceBuilder()
                .setReferencePeriodStartMillis(refTime - 156561)
                .setReferencePeriodEndMillis(refTime)
                .setStyle(ComplicationText.DIFFERENCE_STYLE_SHORT_DUAL_UNIT)
                .build()

        // WHEN getText is called for a time after the ref period
        // THEN the time difference text is returned and "2h 35m 10s" should be rounded to "2h 36m".
        var testTime = refTime +
            TimeUnit.SECONDS.toMillis(10) +
            TimeUnit.MINUTES.toMillis(35) +
            TimeUnit.HOURS.toMillis(
                2
            )
        Assert.assertEquals(
            "2h 36m",
            complicationText.getTextAt(mResources, testTime).toString()
        )

        // WHEN getText is called for a time after the ref period
        // THEN the time difference text is returned and "2h 35m" should be rounded to "2h 35m".
        testTime = refTime +
            TimeUnit.MINUTES.toMillis(35) +
            TimeUnit.HOURS.toMillis(2)
        Assert.assertEquals(
            "2h 35m",
            complicationText.getTextAt(mResources, testTime).toString()
        )

        // THEN the time difference text is returned
        // and "9d 23h 58m 10s" should be rounded to "10d".
        testTime = refTime +
            TimeUnit.SECONDS.toMillis(10) +
            TimeUnit.MINUTES.toMillis(58) +
            TimeUnit.HOURS.toMillis(23) +
            TimeUnit.DAYS.toMillis(9)
        Assert.assertEquals(
            "10d",
            complicationText.getTextAt(mResources, testTime).toString()
        )

        // WHEN getText is called for a time after the ref period
        // THEN the time difference text is returned and "23h 59m 10s" should be rounded to "1d".
        testTime = refTime +
            TimeUnit.SECONDS.toMillis(10) +
            TimeUnit.MINUTES.toMillis(59) +
            TimeUnit.HOURS.toMillis(
                23
            )
        Assert.assertEquals(
            "1d",
            complicationText.getTextAt(mResources, testTime).toString()
        )

        // THEN the time difference text is returned
        // and "23h 58m 10s" should be rounded to "23h 59m".
        testTime = refTime +
            TimeUnit.SECONDS.toMillis(10) +
            TimeUnit.MINUTES.toMillis(58) +
            TimeUnit.HOURS.toMillis(
                23
            )
        Assert.assertEquals(
            "23h 59m",
            complicationText.getTextAt(mResources, testTime).toString()
        )
    }

    @Test
    public fun testStopwatchTextUnitOnly() {
        // GIVEN ComplicationText with stop watch time difference text only
        val refTime: Long = 10000000
        val complicationText =
            TimeDifferenceBuilder()
                .setReferencePeriodStartMillis(refTime - 156561)
                .setReferencePeriodEndMillis(refTime)
                .setStyle(ComplicationText.DIFFERENCE_STYLE_STOPWATCH)
                .build()

        // WHEN getText is called for a time after the ref period
        // THEN the time difference text is returned and "23h 59m 10s" should be rounded to "1d".
        var testTime = refTime +
            TimeUnit.SECONDS.toMillis(10) +
            TimeUnit.MINUTES.toMillis(59) +
            TimeUnit.HOURS.toMillis(
                23
            )
        Assert.assertEquals(
            "1d",
            complicationText.getTextAt(mResources, testTime).toString()
        )

        // WHEN getText is called for a time after the ref period
        // THEN the time difference text is returned in stopwatch style with no rounding.
        testTime = refTime +
            TimeUnit.MINUTES.toMillis(59) +
            TimeUnit.HOURS.toMillis(23)
        Assert.assertEquals(
            "23:59",
            complicationText.getTextAt(mResources, testTime).toString()
        )

        // WHEN getText is called for another time after the ref period
        // THEN the time difference text is returned in stopwatch style with no rounding.
        testTime = refTime +
            TimeUnit.SECONDS.toMillis(59) +
            TimeUnit.MINUTES.toMillis(1)
        Assert.assertEquals(
            "01:59",
            complicationText.getTextAt(mResources, testTime).toString()
        )
    }

    @Test
    public fun testWordsSingleUnitWithSurroundingString() {
        // GIVEN ComplicationText with stop watch time difference text only
        val refTime: Long = 10000000
        val complicationText =
            TimeDifferenceBuilder()
                .setReferencePeriodStartMillis(refTime - 156561)
                .setReferencePeriodEndMillis(refTime)
                .setStyle(ComplicationText.DIFFERENCE_STYLE_WORDS_SINGLE_UNIT)
                .setSurroundingText("just ^1 left")
                .build()

        // WHEN getText is called for a time after the ref period
        // THEN the time difference text is returned and "23h 59m 10s" should be rounded to one day.
        var testTime = refTime +
            TimeUnit.HOURS.toMillis(23) +
            TimeUnit.MINUTES.toMillis(59) +
            TimeUnit.SECONDS.toMillis(
                10
            )
        Assert.assertEquals(
            "just 1 day left",
            complicationText.getTextAt(mResources, testTime).toString()
        )

        // WHEN getText is called for a time after the ref period
        // THEN the time difference text is returned in words, showing the bigger unit only, and
        // rounding up.
        testTime = refTime +
            TimeUnit.HOURS.toMillis(12) +
            TimeUnit.MINUTES.toMillis(35)
        Assert.assertEquals(
            "just 13 hours left",
            complicationText.getTextAt(mResources, testTime).toString()
        )

        // WHEN getText is called for another time after the ref period
        // THEN the time difference text is returned in words, showing the bigger unit only, and
        // rounding up.
        testTime = refTime +
            TimeUnit.MINUTES.toMillis(35) +
            TimeUnit.SECONDS.toMillis(59)
        Assert.assertEquals(
            "just 36 mins left",
            complicationText.getTextAt(mResources, testTime).toString()
        )
    }

    @Test
    public fun testSurroundingTextWithIgnoredPlaceholders() {
        // WHEN ComplicationText with surrounding text that contains one of ^2..9 is created
        val refTime: Long = 10000000
        val complicationText =
            TimeDifferenceBuilder()
                .setReferencePeriodStartMillis(refTime - 156561)
                .setReferencePeriodEndMillis(refTime)
                .setStyle(ComplicationText.DIFFERENCE_STYLE_WORDS_SINGLE_UNIT)
                .setSurroundingText("^1 ^^ ^ ^^1 ^2 ^3 ^4 ^5 ^6 ^7 ^8 ^9")
                .build()

        // THEN placeholders other than ^1 and ^^ are ignored
        Assert.assertEquals(
            "Now ^ ^ ^1 ^2 ^3 ^4 ^5 ^6 ^7 ^8 ^9",
            complicationText.getTextAt(mResources, refTime).toString()
        )
    }

    @Test
    public fun testWordsSingleUnitWithSurroundingStringAndDayMinimumUnit() {
        // GIVEN ComplicationText with stop watch time difference text only
        val refTime: Long = 10000000
        val complicationText =
            TimeDifferenceBuilder()
                .setReferencePeriodStartMillis(refTime - 156561)
                .setReferencePeriodEndMillis(refTime)
                .setStyle(ComplicationText.DIFFERENCE_STYLE_WORDS_SINGLE_UNIT)
                .setSurroundingText("just ^1 left")
                .setMinimumUnit(TimeUnit.DAYS)
                .build()

        // WHEN getText is called for a time after the ref period
        // THEN the time difference text is returned with the time rounded up to the next day.
        var testTime = refTime +
            TimeUnit.DAYS.toMillis(7) +
            TimeUnit.HOURS.toMillis(23) +
            TimeUnit.MINUTES.toMillis(59)
        Assert.assertEquals(
            "just 8 days left",
            complicationText.getTextAt(mResources, testTime).toString()
        )

        // WHEN getText is called for a time after the ref period
        // THEN the time difference text is returned in words with the time rounded up to the next
        // day.
        testTime = refTime +
            TimeUnit.HOURS.toMillis(12) +
            TimeUnit.MINUTES.toMillis(35)
        Assert.assertEquals(
            "just 1 day left",
            complicationText.getTextAt(mResources, testTime).toString()
        )

        // WHEN getText is called for another time after the ref period
        // THEN the time difference text is returned in words with the time rounded up to the next
        // day.
        testTime = refTime +
            TimeUnit.MINUTES.toMillis(35) +
            TimeUnit.SECONDS.toMillis(59)
        Assert.assertEquals(
            "just 1 day left",
            complicationText.getTextAt(mResources, testTime).toString()
        )
    }

    @Test
    public fun testShortWordsSingleUnit() {
        // GIVEN ComplicationText with stop watch time difference text only
        val refTime: Long = 10000000
        val complicationText =
            TimeDifferenceBuilder()
                .setReferencePeriodStartMillis(refTime - 156561)
                .setReferencePeriodEndMillis(refTime)
                .setStyle(ComplicationText.DIFFERENCE_STYLE_SHORT_WORDS_SINGLE_UNIT)
                .build()

        // WHEN getText is called for a time after the ref period
        // THEN the time difference text is returned and "23h 59m 10s" should be rounded to one day.
        var testTime = refTime +
            TimeUnit.HOURS.toMillis(23) +
            TimeUnit.MINUTES.toMillis(59) +
            TimeUnit.SECONDS.toMillis(10)
        Assert.assertEquals(
            "1 day",
            complicationText.getTextAt(mResources, testTime).toString()
        )

        // WHEN getText is called for a time after the ref period
        // THEN the time difference text is returned in words, showing the bigger unit only, and
        // rounding up.
        testTime = refTime +
            TimeUnit.HOURS.toMillis(1) +
            TimeUnit.MINUTES.toMillis(35)
        Assert.assertEquals(
            "2 hours",
            complicationText.getTextAt(mResources, testTime).toString()
        )

        // WHEN getText is called for a double-digit number of hours after the ref period
        // THEN the time difference text is returned using the short style.
        testTime = refTime +
            TimeUnit.HOURS.toMillis(12) +
            TimeUnit.MINUTES.toMillis(35)
        Assert.assertEquals(
            "13h",
            complicationText.getTextAt(mResources, testTime).toString()
        )

        // WHEN getText is called for another time many days the ref period, such that more than 7
        // characters would be used if the unit was shown as a word
        // THEN the time difference text is returned using the short style.
        testTime = refTime + TimeUnit.DAYS.toMillis(120)
        Assert.assertEquals(
            "120d",
            complicationText.getTextAt(mResources, testTime).toString()
        )
    }

    @Test
    public fun testStopwatchWithNowText() {
        // GIVEN ComplicationText with stop watch time difference text only
        val refTime: Long = 10000000
        val complicationText =
            TimeDifferenceBuilder()
                .setReferencePeriodStartMillis(refTime - 156561)
                .setReferencePeriodEndMillis(refTime)
                .setStyle(ComplicationText.DIFFERENCE_STYLE_STOPWATCH)
                .setShowNowText(true)
                .build()

        // WHEN getText is called for a time within the ref period
        // THEN the time difference text is returned and "Now" is shown.
        Assert.assertEquals(
            "Now",
            complicationText.getTextAt(mResources, refTime - 100).toString()
        )

        // WHEN getText is called for a time after the ref period
        // THEN the time difference text is returned and the time difference is shown in stopwatch
        // style.
        val testTime = refTime + TimeUnit.SECONDS.toMillis(35)
        Assert.assertEquals(
            "00:35",
            complicationText.getTextAt(mResources, testTime).toString()
        )
    }

    @Test
    public fun testSingleUnitWithoutNowText() {
        // GIVEN ComplicationText with stop watch time difference text only
        val refTime: Long = 10000000
        val complicationText =
            TimeDifferenceBuilder()
                .setReferencePeriodStartMillis(refTime - 156561)
                .setReferencePeriodEndMillis(refTime + 100)
                .setStyle(ComplicationText.DIFFERENCE_STYLE_SHORT_SINGLE_UNIT)
                .setShowNowText(false)
                .build()

        // WHEN getText is called for a time within the ref period
        // THEN the time difference text is returned and "0m" is shown.
        Assert.assertEquals(
            "0m",
            complicationText.getTextAt(mResources, refTime).toString()
        )
    }

    @Test
    public fun testTimeDifferenceShortSingleUnitAndFormatString() {
        // GIVEN ComplicationText with short single unit time difference text and a format string
        val refTime: Long = 10000000
        val complicationText =
            TimeDifferenceBuilder()
                .setReferencePeriodStartMillis(refTime - 156561)
                .setReferencePeriodEndMillis(refTime)
                .setStyle(ComplicationText.DIFFERENCE_STYLE_SHORT_SINGLE_UNIT)
                .setSurroundingText("hello ^1 time")
                .build()

        // WHEN getText is called for a time after the ref period
        // THEN the time difference text is returned within the format string
        val testTime = refTime +
            TimeUnit.MINUTES.toMillis(35) +
            TimeUnit.HOURS.toMillis(2)
        Assert.assertEquals(
            "hello 3h time",
            complicationText.getTextAt(mResources, testTime).toString()
        )
    }

    @Test
    public fun testTimeDifferenceShortDualUnitAndFormatString() {
        // GIVEN ComplicationText with short dual unit time difference text and a format string
        val refTime: Long = 10000000
        val complicationText =
            TimeDifferenceBuilder()
                .setReferencePeriodStartMillis(refTime - 156561)
                .setReferencePeriodEndMillis(refTime)
                .setStyle(ComplicationText.DIFFERENCE_STYLE_SHORT_DUAL_UNIT)
                .setSurroundingText("sometext^1somemoretext")
                .build()

        // WHEN getText is called for a time after the ref period
        // THEN the time difference text is returned within the format string
        val testTime = refTime +
            TimeUnit.MINUTES.toMillis(35) +
            TimeUnit.HOURS.toMillis(2)
        Assert.assertEquals(
            "sometext2h 35msomemoretext",
            complicationText.getTextAt(mResources, testTime).toString()
        )
    }

    @Test
    public fun testTimeFormatUpperCase() {
        val complicationText =
            TimeFormatBuilder()
                .setFormat("EEE 'the' d LLL")
                .setStyle(ComplicationText.FORMAT_STYLE_UPPER_CASE)
                .build()
        val result = complicationText.getTextAt(
            mResources, GregorianCalendar(2016, 2, 4).timeInMillis
        )
        Assert.assertEquals("FRI THE 4 MAR", result.toString())
    }

    @Test
    public fun testTimeFormatLowerCase() {
        val complicationText =
            TimeFormatBuilder()
                .setFormat("EEE 'the' d LLL")
                .setStyle(ComplicationText.FORMAT_STYLE_LOWER_CASE)
                .build()
        val result = complicationText.getTextAt(
            mResources, GregorianCalendar(2016, 2, 4).timeInMillis
        )
        Assert.assertEquals("fri the 4 mar", result.toString())
    }

    @Test
    public fun testTimeFormatNoStyle() {
        val complicationText =
            TimeFormatBuilder().setFormat("EEE 'the' d LLL").build()
        val result = complicationText.getTextAt(
            mResources, GregorianCalendar(2016, 2, 4).timeInMillis
        )
        Assert.assertEquals("Fri the 4 Mar", result.toString())
    }

    @Test
    public fun testTimeFormatUpperCaseSurroundingString() {
        val complicationText =
            TimeFormatBuilder()
                .setFormat("EEE 'the' d LLL")
                .setStyle(ComplicationText.FORMAT_STYLE_UPPER_CASE)
                .setSurroundingText("sometext^1somemoretext")
                .build()
        val result = complicationText.getTextAt(
            mResources, GregorianCalendar(2016, 2, 4).timeInMillis
        )
        Assert.assertEquals("sometextFRI THE 4 MARsomemoretext", result.toString())
    }

    @Test
    public fun testTimeFormatWithTimeZone() {
        val complicationText =
            TimeFormatBuilder()
                .setFormat("HH:mm")
                .setTimeZone(TimeZone.getTimeZone("Asia/Seoul"))
                .build()
        val calendar =
            GregorianCalendar(TimeZone.getTimeZone("GMT+0"))
        calendar[2016, 2, 4, 18, 52] = 58
        val result =
            complicationText.getTextAt(mResources, calendar.timeInMillis)
        Assert.assertEquals("03:52", result.toString())
    }

    @Test
    public fun testParcelPlainText() {
        // GIVEN ComplicationText containing plain text
        val originalText =
            ComplicationText.plainText("hello how are you")

        // WHEN the object is parcelled and unparcelled
        val newText = originalText.roundTripParcelable()!!

        // THEN the object behaves as expected.
        Assert.assertEquals(
            "hello how are you",
            newText.getTextAt(mResources, 100000).toString()
        )
    }

    @Test
    public fun testParcelTimeDifferenceTextWithoutMinUnit() {
        // GIVEN ComplicationText containing TimeDifferenceText
        val refTime: Long = 10000000
        val originalText =
            TimeDifferenceBuilder()
                .setReferencePeriodStartMillis(refTime - 156561)
                .setReferencePeriodEndMillis(refTime)
                .setStyle(ComplicationText.DIFFERENCE_STYLE_SHORT_SINGLE_UNIT)
                .setSurroundingText("hello ^1 time")
                .setShowNowText(false)
                .build()

        // WHEN the object is parcelled and unparcelled
        val newText = originalText.roundTripParcelable()!!

        // THEN the object behaves as expected.
        val testTime = refTime +
            TimeUnit.HOURS.toMillis(2) +
            TimeUnit.MINUTES.toMillis(35)
        Assert.assertEquals(
            "hello 3h time",
            newText.getTextAt(mResources, testTime).toString()
        )
        Assert.assertEquals(
            "hello 0m time",
            newText.getTextAt(mResources, refTime).toString()
        )
    }

    @Test
    public fun testParcelTimeDifferenceTextWithMinUnit() {
        // GIVEN ComplicationText containing TimeDifferenceText with a minimum unit specified
        val refTime: Long = 10000000
        val originalText =
            TimeDifferenceBuilder()
                .setReferencePeriodStartMillis(refTime - 156561)
                .setReferencePeriodEndMillis(refTime)
                .setStyle(ComplicationText.DIFFERENCE_STYLE_SHORT_DUAL_UNIT)
                .setMinimumUnit(TimeUnit.HOURS)
                .build()

        // WHEN the object is parcelled and unparcelled
        val newText = originalText.roundTripParcelable()!!

        // THEN the object behaves as expected.
        val testTime = refTime +
            TimeUnit.HOURS.toMillis(2) +
            TimeUnit.MINUTES.toMillis(35)
        Assert.assertEquals("3h", newText.getTextAt(mResources, testTime).toString())
    }

    @Test
    public fun testParcelTimeDifferenceTextWithUnrecognizedMinUnit() {
        // GIVEN a parcelled ComplicationText object containing TimeDifferenceText with an unknown
        // minimum unit specified
        val refTime: Long = 10000000
        val originalText =
            TimeDifferenceBuilder()
                .setReferencePeriodStartMillis(refTime - 156561)
                .setReferencePeriodEndMillis(refTime)
                .setStyle(ComplicationText.DIFFERENCE_STYLE_SHORT_DUAL_UNIT)
                .build()
        val parcel = Parcel.obtain()
        originalText.writeToParcel(parcel, 0)
        parcel.setDataPosition(0)
        val bundle = parcel.readBundle(javaClass.classLoader)
        bundle!!.putString("minimum_unit", "XYZ")
        val parcelWithBadMinUnit = Parcel.obtain()
        parcelWithBadMinUnit.writeBundle(bundle)
        parcelWithBadMinUnit.setDataPosition(0)

        // WHEN the object is unparcelled
        val newText =
            ComplicationText.CREATOR.createFromParcel(
                parcelWithBadMinUnit
            )

        // THEN the object is unparcelled successfully, and behaves as if no min unit was specified.
        val testTime = refTime +
            TimeUnit.HOURS.toMillis(2) +
            TimeUnit.MINUTES.toMillis(35)
        Assert.assertEquals("2h 35m", newText.getTextAt(mResources, testTime).toString())
    }

    @Test
    public fun testParcelTimeFormatTextWithoutTimeZone() {
        // GIVEN ComplicationText containing TimeFormatText with no time zone
        val originalText =
            TimeFormatBuilder()
                .setFormat("EEE 'the' d LLL")
                .setStyle(ComplicationText.FORMAT_STYLE_LOWER_CASE)
                .build()

        // WHEN the object is parcelled and unparcelled
        val newText = originalText.roundTripParcelable()!!

        // THEN the object behaves as expected.
        val result =
            newText.getTextAt(mResources, GregorianCalendar(2016, 2, 4).timeInMillis)
        Assert.assertEquals("fri the 4 mar", result.toString())
    }

    @Test
    public fun testParcelTimeFormatTextWithTimeZone() {
        // GIVEN ComplicationText containing TimeFormatText with a time zone specified
        val originalText =
            TimeFormatBuilder()
                .setFormat("EEE 'the' d LLL HH:mm")
                .setStyle(ComplicationText.FORMAT_STYLE_LOWER_CASE)
                .setTimeZone(TimeZone.getTimeZone("GMT+5"))
                .build()

        // WHEN the object is parcelled and unparcelled
        val newText = originalText.roundTripParcelable()!!

        // THEN the object behaves as expected.
        val calendar =
            GregorianCalendar(TimeZone.getTimeZone("GMT+0"))
        calendar[2016, 2, 4, 18, 52] = 58
        val result = newText.getTextAt(mResources, calendar.timeInMillis)
        Assert.assertEquals("fri the 4 mar 23:52", result.toString())
    }

    @Test
    public fun zeroWorksWhenNoRefPeriodStartTime() {
        // GIVEN ComplicationText with time difference text with no ref period start time
        val refTime: Long = 10000000
        val complicationText =
            TimeDifferenceBuilder()
                .setReferencePeriodStartMillis(0)
                .setReferencePeriodEndMillis(refTime + 100)
                .setStyle(ComplicationText.DIFFERENCE_STYLE_SHORT_SINGLE_UNIT)
                .build()

        // WHEN getText is called for a zero time
        // THEN the time difference text is returned and "Now" is shown, with no NPE
        Assert.assertEquals("Now", complicationText.getTextAt(mResources, 0).toString())
    }

    @Test
    public fun nextChangeTimeNotTimeDependent() {
        val text =
            ComplicationText.plainText("hello")
        Truth.assertThat(text.getNextChangeTime(1000000))
            .isEqualTo(Long.MAX_VALUE)
    }

    @Test
    public fun nextChangeTimeTimeDifference() {
        val text = TimeDifferenceBuilder()
            .setReferencePeriodStartMillis(0)
            .setReferencePeriodEndMillis(0)
            .setStyle(ComplicationText.DIFFERENCE_STYLE_SHORT_SINGLE_UNIT)
            .build()

        // Time difference rounds up, so the next change is 1ms after the next minute boundary.
        Truth.assertThat(text.getNextChangeTime(600000123))
            .isEqualTo(600060001)
    }

    @Test
    public fun nextChangeTimeTimeFormat() {
        val text = TimeFormatBuilder()
            .setFormat("EEE 'the' d LLL HH:mm")
            .setStyle(ComplicationText.FORMAT_STYLE_LOWER_CASE)
            .build()

        // Time format rounds down, so the next change is at the next minute boundary.
        Truth.assertThat(text.getNextChangeTime(600000123))
            .isEqualTo(600060000)
    }
}