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

package androidx.wear.complications;

import static com.google.common.truth.Truth.assertThat;

import android.text.format.DateFormat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.annotation.internal.DoNotInstrument;

import java.util.Locale;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = ComplicationTextUtilsTest.ShadowDateFormat.class)
@DoNotInstrument
public class ComplicationTextUtilsTest {

    @Test
    public void time24HourUsesCustomFormatIfNotTooLong() {
        // In ShadowDateFormat, best time format for Norwegian is set to return with a dot instead
        // of
        // a colon (which matches the actual return value for that language).
        assertThat(ComplicationTextUtils.shortTextTimeFormat(new Locale("no"), true))
                .isEqualTo("HH.mm");
    }

    @Test
    public void time24HourUsesStandardExpectedFormatIfNotTooLong() {
        assertThat(ComplicationTextUtils.shortTextTimeFormat(new Locale("en", "GB"), true))
                .isEqualTo("HH:mm");
    }

    @Test
    public void time12HourRemovesSpaceBeforeAmPmIfNecessary() {
        // Default is "h:mm a" but that can be 8 characters
        assertThat(ComplicationTextUtils.shortTextTimeFormat(new Locale("en", "US"), false))
                .isEqualTo("h:mma");
    }

    @Test
    public void time12HourStripsAmPmIfTooLong() {
        // Czech am/pm is "odp./dop." which are too long to fit after the time.
        assertThat(ComplicationTextUtils.shortTextTimeFormat(new Locale("cs"), false))
                .isEqualTo("h:mm");
    }

    @Test
    public void standardDayMonthFormat() {
        assertThat(ComplicationTextUtils.shortTextDayMonthFormat(new Locale("en", "GB")))
                .isEqualTo("d MMM");
    }

    @Test
    public void usDayMonthFormat() {
        assertThat(ComplicationTextUtils.shortTextDayMonthFormat(new Locale("en", "US")))
                .isEqualTo("MMM d");
    }

    @Test
    public void tooLongTextDayMonthFormat() {
        // In ShadowDateFormat, best date format for Brazilian Portuguese is set to return with
        // extra
        // characters that mean it cannot fit within the short text limit. This matches the actual
        // return value for that language.
        assertThat(ComplicationTextUtils.shortTextDayMonthFormat(new Locale("pt", "BR")))
                .isEqualTo("d/MM");
    }

    @Test
    public void vietnameseTextDayMonthFormat() {
        // In Vietnamese, an MMM month can be e.g. "thg 10", which means "d MMM" is too long even
        // though
        // the same format pattern is acceptable in other languages.
        assertThat(ComplicationTextUtils.shortTextDayMonthFormat(new Locale("vi")))
                .isEqualTo("d/MM");
    }

    @Test
    public void monthFallback() {
        // In ShadowDateFormat, best date format for this language returns a too-long pattern for
        // any
        // input. Result should be the specified fallback.
        assertThat(ComplicationTextUtils.shortTextMonthFormat(new Locale("mfe"))).isEqualTo("MM");
    }

    @Test
    public void standardShortMonthFormat() {
        assertThat(ComplicationTextUtils.shortTextMonthFormat(new Locale("en", "GB")))
                .isEqualTo("LLL");
    }

    @Test
    public void germanShortMonthFormat() {
        // In German, the desired output is e.g. "Jan." instead of "Jan". To get this result, the
        // pattern "MMM" should be used instead of "LLL".
        assertThat(ComplicationTextUtils.shortTextMonthFormat(new Locale("de"))).isEqualTo("MMM");
    }

    @Test
    public void standardDayOfMonthFormat() {
        assertThat(ComplicationTextUtils.shortTextDayOfMonthFormat(new Locale("en", "GB")))
                .isEqualTo("dd");
    }

    @Test
    public void finnishDayOfMonthFormat() {
        assertThat(ComplicationTextUtils.shortTextDayOfMonthFormat(new Locale("fi")))
                .isEqualTo("dd.");
    }

    @Test
    public void germanDayOfMonthFormat() {
        assertThat(ComplicationTextUtils.shortTextDayOfMonthFormat(new Locale("de")))
                .isEqualTo("dd.");
    }

    @Test
    public void standardDayOfWeekFormat() {
        assertThat(ComplicationTextUtils.shortTextDayOfWeekFormat(new Locale("en", "GB")))
                .isEqualTo("EEE");
    }

    @Test
    public void fallbackDayOfWeekFormat() {
        assertThat(ComplicationTextUtils.shortTextDayOfWeekFormat(new Locale("mfe")))
                .isEqualTo("EEEEE");
    }

    /** Robolectric shadow for Android DateFormat. */
    @Implements(value = DateFormat.class)
    public static class ShadowDateFormat {
        @Implementation
        public static String getBestDateTimePattern(Locale locale, String skeleton) {
            // Special case returning too long pattern for any skeleton, to test fallback.
            if (locale.getLanguage().equals("mfe")) {
                return "'too long'MMMd";
            }

            switch (skeleton) {
                case "HHmm":
                    switch (locale.getLanguage()) {
                        case "no":
                            return "HH.mm";
                        default:
                            return "HH:mm";
                    }
                case "hhmm":
                    switch (locale.getLanguage()) {
                        case "no":
                            return "h.mm a";
                        default:
                            return "hh:mm a";
                    }
                case "hmm":
                    switch (locale.getLanguage()) {
                        case "no":
                            return "h.mm a";
                        default:
                            return "h:mm a";
                    }
                case "MMMd":
                    switch (locale.getCountry()) {
                        case "BR":
                            return "d 'de' MMM";
                        case "US":
                            return "MMM d";
                        default:
                            return "d MMM";
                    }
                case "MMd":
                    switch (locale.getCountry()) {
                        case "US":
                            return "MM/d";
                        default:
                            return "d/MM";
                    }
                case "MMM":
                    return "LLL";
                default:
                    return skeleton;
            }
        }
    }
}
