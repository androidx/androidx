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

package android.support.wearable.complications;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertEquals;

import static java.util.concurrent.TimeUnit.HOURS;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;

import android.content.res.Resources;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.internal.DoNotInstrument;

import java.util.GregorianCalendar;

@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
public class ComplicationTextTemplateTest {

    private static final String TEST_TEXT1 = "Hello";
    private static final String TEST_TEXT2 = "darkness";
    private static final String TEST_TEXT3 = "old friend";
    private static final String TEST_TEXT_LONG = "Lovely weather we're having :)";

    private Resources mResources = ApplicationProvider.getApplicationContext().getResources();

    @Test
    public void testPlainTextConcatenation() {
        ComplicationText hello = ComplicationText.plainText(TEST_TEXT1);
        ComplicationText there = ComplicationText.plainText(TEST_TEXT2);
        ComplicationText friend = ComplicationText.plainText(TEST_TEXT3);

        ComplicationTextTemplate complicationTextTemplate =
                new ComplicationTextTemplate.Builder()
                        .addComplicationText(hello)
                        .addComplicationText(there)
                        .addComplicationText(friend)
                        .build();

        assertEquals(
                TEST_TEXT1 + " " + TEST_TEXT2 + " " + TEST_TEXT3,
                complicationTextTemplate.getText(mResources, 132456789).toString());
    }

    @Test
    public void testPlainTextTemplate() {
        ComplicationText hello = ComplicationText.plainText(TEST_TEXT1);
        ComplicationText there = ComplicationText.plainText(TEST_TEXT2);
        ComplicationText friend = ComplicationText.plainText(TEST_TEXT3);

        ComplicationTextTemplate complicationTextTemplate =
                new ComplicationTextTemplate.Builder()
                        .addComplicationText(hello)
                        .addComplicationText(there)
                        .addComplicationText(friend)
                        .setSurroundingText("^1, ^2 my ^3.")
                        .build();

        assertEquals(
                TEST_TEXT1 + ", " + TEST_TEXT2 + " my " + TEST_TEXT3 + ".",
                complicationTextTemplate.getText(mResources, 132456789).toString());
    }

    @Test
    public void testTemplateEscapeCharacter() {
        ComplicationText hello = ComplicationText.plainText(TEST_TEXT1);
        ComplicationText there = ComplicationText.plainText(TEST_TEXT2);

        ComplicationTextTemplate complicationTextTemplate =
                new ComplicationTextTemplate.Builder()
                        .addComplicationText(hello)
                        .addComplicationText(there)
                        .setSurroundingText("^1 ^^2 ^2")
                        .build();

        assertEquals(
                TEST_TEXT1 + " ^2 " + TEST_TEXT2,
                complicationTextTemplate.getText(mResources, 132456789).toString());
    }

    @Test
    public void testTimeDifferenceTemplate() {
        long refTime = 10000000;
        ComplicationText complicationText =
                new ComplicationText.TimeDifferenceBuilder()
                        .setReferencePeriodStartMillis(refTime - 156561)
                        .setReferencePeriodEndMillis(refTime)
                        .setStyle(ComplicationText.DIFFERENCE_STYLE_SHORT_SINGLE_UNIT)
                        .build();

        ComplicationText complicationText2 =
                new ComplicationText.TimeDifferenceBuilder()
                        .setReferencePeriodStartMillis(refTime - 156561)
                        .setReferencePeriodEndMillis(refTime)
                        .setStyle(ComplicationText.DIFFERENCE_STYLE_SHORT_DUAL_UNIT)
                        .build();

        ComplicationTextTemplate template =
                new ComplicationTextTemplate.Builder()
                        .addComplicationText(complicationText)
                        .addComplicationText(complicationText2)
                        .setSurroundingText("^1 : ^2")
                        .build();

        // "2h 35m" should be rounded to "3h".
        long testTime = refTime + MINUTES.toMillis(35) + HOURS.toMillis(2);
        assertEquals("3h : 2h 35m", template.getText(mResources, testTime).toString());

        // "23h 59m" should be rounded to "1d".
        testTime = refTime + MINUTES.toMillis(59) + HOURS.toMillis(23);
        assertEquals("1d : 23h 59m", template.getText(mResources, testTime).toString());

        // "10m 10s" should be rounded to "11m".
        testTime = refTime + SECONDS.toMillis(10) + MINUTES.toMillis(10);
        assertEquals("11m : 11m", template.getText(mResources, testTime).toString());

        // "23h 15m" should be rounded to "1d".
        testTime = refTime + MINUTES.toMillis(15) + HOURS.toMillis(23);
        assertEquals("1d : 23h 15m", template.getText(mResources, testTime).toString());
    }

    @Test
    public void testTimeFormatUpperCase() {
        ComplicationText complicationText =
                new ComplicationText.TimeFormatBuilder()
                        .setFormat("EEE 'the' d LLL")
                        .setStyle(ComplicationText.FORMAT_STYLE_UPPER_CASE)
                        .build();

        ComplicationText longText = ComplicationText.plainText(TEST_TEXT_LONG);

        ComplicationTextTemplate template =
                new ComplicationTextTemplate.Builder()
                        .addComplicationText(complicationText)
                        .addComplicationText(longText)
                        .setSurroundingText("^1 *** ^2")
                        .build();

        CharSequence result =
                template.getText(mResources, new GregorianCalendar(2016, 2, 4).getTimeInMillis());
        assertEquals("FRI THE 4 MAR *** " + TEST_TEXT_LONG, result.toString());
    }

    @Test
    public void testParcelPlainText() {
        ComplicationText text1 = ComplicationText.plainText(TEST_TEXT1);
        ComplicationText longText = ComplicationText.plainText(TEST_TEXT_LONG);

        ComplicationTextTemplate template =
                new ComplicationTextTemplate.Builder()
                        .addComplicationText(text1)
                        .addComplicationText(longText)
                        .setSurroundingText("^1 : ^2")
                        .build();

        ComplicationTextTemplate newText = roundTripParcelable(template);

        assertEquals(
                TEST_TEXT1 + " : " + TEST_TEXT_LONG,
                newText.getText(mResources, 100000).toString());
    }

    @Test
    public void testParcelTimeDifferenceTextWithPlainTextAndTemplate() {
        long refTime = 10000000;
        ComplicationText originalText =
                new ComplicationText.TimeDifferenceBuilder()
                        .setReferencePeriodStartMillis(refTime - 156561)
                        .setReferencePeriodEndMillis(refTime)
                        .setStyle(ComplicationText.DIFFERENCE_STYLE_SHORT_SINGLE_UNIT)
                        .setSurroundingText("hello ^1 time")
                        .setShowNowText(false)
                        .build();

        ComplicationText longText = ComplicationText.plainText(TEST_TEXT_LONG);

        ComplicationTextTemplate template =
                new ComplicationTextTemplate.Builder()
                        .addComplicationText(originalText)
                        .addComplicationText(longText)
                        .setSurroundingText("^1 : ^2")
                        .build();

        ComplicationTextTemplate newText = roundTripParcelable(template);

        long testTime = refTime + HOURS.toMillis(2) + MINUTES.toMillis(35);
        assertEquals(
                "hello 3h time : " + TEST_TEXT_LONG,
                newText.getText(mResources, testTime).toString());
        assertEquals(
                "hello 0m time : " + TEST_TEXT_LONG,
                newText.getText(mResources, refTime).toString());
    }

    @Test
    public void nextChangeTimeDifferenceAndFormat() {
        ComplicationText diffText =
                new ComplicationText.TimeDifferenceBuilder()
                        .setReferencePeriodStartMillis(0)
                        .setReferencePeriodEndMillis(0)
                        .setStyle(ComplicationText.DIFFERENCE_STYLE_SHORT_SINGLE_UNIT)
                        .build();

        ComplicationText formatText =
                new ComplicationText.TimeFormatBuilder()
                        .setFormat("EEE 'the' d LLL HH")
                        .setStyle(ComplicationText.FORMAT_STYLE_LOWER_CASE)
                        .build();

        ComplicationTextTemplate template =
                new ComplicationTextTemplate.Builder()
                        .addComplicationText(diffText)
                        .addComplicationText(formatText)
                        .setSurroundingText("^1 : ^2")
                        .build();

        // Next change comes from the time difference, so is next minute boundary + 1ms.
        assertThat(template.getNextChangeTime(60000000123L)).isEqualTo(60000060001L);
    }

    @Test
    public void nextChangeTimeDifferenceAndPlain() {
        ComplicationText diffText =
                new ComplicationText.TimeDifferenceBuilder()
                        .setReferencePeriodStartMillis(0)
                        .setReferencePeriodEndMillis(0)
                        .setStyle(ComplicationText.DIFFERENCE_STYLE_SHORT_SINGLE_UNIT)
                        .build();

        ComplicationText plainText = ComplicationText.plainText("hello");

        ComplicationTextTemplate template =
                new ComplicationTextTemplate.Builder()
                        .addComplicationText(diffText)
                        .addComplicationText(plainText)
                        .setSurroundingText("^1 : ^2")
                        .build();

        assertThat(template.getNextChangeTime(60000000123L)).isEqualTo(60000060001L);
    }

    @Test
    public void nextChangeTimeFormatAndPlain() {
        ComplicationText formatText =
                new ComplicationText.TimeFormatBuilder()
                        .setFormat("EEE 'the' d LLL HH:mm")
                        .setStyle(ComplicationText.FORMAT_STYLE_LOWER_CASE)
                        .build();

        ComplicationText plainText = ComplicationText.plainText("hello");

        ComplicationTextTemplate template =
                new ComplicationTextTemplate.Builder()
                        .addComplicationText(plainText)
                        .addComplicationText(formatText)
                        .setSurroundingText("^1 : ^2")
                        .build();

        assertThat(template.getNextChangeTime(60000000123L)).isEqualTo(60000060000L);
    }

    /** Writes {@code in} to a {@link Parcel} and reads it back, returning the result. */
    @SuppressWarnings("unchecked")
    private static <T extends Parcelable> T roundTripParcelable(T in) {
        Parcel parcel = Parcel.obtain();
        try {
            parcel.writeValue(in);
            parcel.setDataPosition(0);
            return (T) parcel.readValue(in.getClass().getClassLoader());
        } finally {
            parcel.recycle();
        }
    }
}
