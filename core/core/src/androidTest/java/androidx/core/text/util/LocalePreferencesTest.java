/*
 * Copyright (C) 2022 The Android Open Source Project
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

package androidx.core.text.util;

import static org.junit.Assert.assertEquals;

import android.os.Build.VERSION_CODES;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SdkSuppress;
import androidx.test.filters.SmallTest;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Locale;

@SmallTest
@SdkSuppress(minSdkVersion = VERSION_CODES.N)
@RunWith(AndroidJUnit4.class)
public class LocalePreferencesTest {
    private static Locale sLocale;

    @BeforeClass
    public static void setUpClass() throws Exception {
        sLocale = Locale.getDefault(Locale.Category.FORMAT);
    }

    @After
    public void tearDown() {
        Locale.setDefault(sLocale);
    }

    // Hour cycle
    @Test
    public void getHourCycle_hasSubTags_resultIsH24() throws Exception {
        Locale.setDefault(Locale.forLanguageTag("en-US-u-ca-chinese-hc-h24-mu-celsius-fw-wed"));

        String result = LocalePreferences.getHourCycle();

        assertEquals(LocalePreferences.HourCycle.H24, result);
    }

    @Test
    public void getHourCycle_hasSubTagsWithoutHourCycleTag_resultIsH12() throws Exception {
        Locale.setDefault(Locale.forLanguageTag("en-US-u-ca-chinese-mu-celsius-fw-wed"));

        String result = LocalePreferences.getHourCycle();

        assertEquals(LocalePreferences.HourCycle.H12, result);
    }

    @Test
    public void getHourCycle_hasSubTagsAndDisableResolved_resultIsH24() throws Exception {
        Locale.setDefault(Locale.forLanguageTag("en-US-u-ca-chinese-hc-h24-mu-celsius-fw-wed"));

        String result = LocalePreferences.getHourCycle(false);

        assertEquals(LocalePreferences.HourCycle.H24, result);
    }

    @Test
    public void getHourCycle_hasSubTagsWithoutHourCycleTagAndDisableResolved_resultIsEmpty()
            throws Exception {
        Locale.setDefault(Locale.forLanguageTag("en-US-u-ca-chinese-mu-celsius-fw-wed"));

        String result = LocalePreferences.getHourCycle(false);

        assertEquals(LocalePreferences.HourCycle.DEFAULT, result);
    }

    @Test
    public void getHourCycle_inputLocaleWithHourCycleTag_resultIsH12() throws Exception {
        String result = LocalePreferences.getHourCycle(Locale.forLanguageTag("en-US-u-hc-h12"));

        assertEquals(LocalePreferences.HourCycle.H12, result);
    }

    @Test
    public void getHourCycle_inputLocaleWithoutHourCycleTag_resultIsH12() throws Exception {
        String result = LocalePreferences.getHourCycle(Locale.forLanguageTag("en-US"));

        assertEquals(LocalePreferences.HourCycle.H12, result);
    }

    @Test
    public void getHourCycle_inputH23Locale_resultIsH23() throws Exception {
        String result = LocalePreferences.getHourCycle(Locale.forLanguageTag("fr-FR"));

        assertEquals(LocalePreferences.HourCycle.H23, result);
    }

    @Test
    public void getHourCycle_inputH23LocaleWithHourCycleTag_resultIsH12() throws Exception {
        String result = LocalePreferences.getHourCycle(Locale.forLanguageTag("fr-FR-u-hc-h12"));

        assertEquals(LocalePreferences.HourCycle.H12, result);
    }

    @Test
    public void getHourCycle_inputLocaleWithoutHourCycleTagAndDisableResolved_resultIsEmpty()
            throws Exception {
        String result = LocalePreferences.getHourCycle(Locale.forLanguageTag("en-US"), false);

        assertEquals(LocalePreferences.HourCycle.DEFAULT, result);
    }

    @Test
    public void getHourCycle_compareHasResolvedValueIsTrueAndWithoutResolvedValue_sameResult()
            throws Exception {
        Locale.setDefault(Locale.forLanguageTag("zh-TW-u-ca-chinese-hc-h24-mu-celsius-fw-wed"));

        // Has Hour Cycle subtag
        String resultWithoutResolvedValue = LocalePreferences.getHourCycle();
        String resultResolvedIsTrue = LocalePreferences.getHourCycle(true);
        assertEquals(resultWithoutResolvedValue, resultResolvedIsTrue);

        // Does not have HourCycle subtag
        Locale.setDefault(Locale.forLanguageTag("zh-TW-u-ca-chinese-mu-celsius-fw-wed"));

        resultWithoutResolvedValue = LocalePreferences.getHourCycle();
        resultResolvedIsTrue = LocalePreferences.getHourCycle(true);
        assertEquals(resultWithoutResolvedValue, resultResolvedIsTrue);
    }

    // Calendar
    @Test
    public void getCalendarType_hasSubTags_resultIsChinese() throws Exception {
        Locale.setDefault(Locale.forLanguageTag("en-US-u-ca-chinese-hc-h24-mu-celsius-fw-wed"));

        String result = LocalePreferences.getCalendarType();

        assertEquals(LocalePreferences.CalendarType.CHINESE, result);
    }

    @Test
    public void getCalendarType_hasSubTagsWithoutCalendarTag_resultIsGregorian() throws Exception {
        Locale.setDefault(Locale.forLanguageTag("en-US-u-hc-h24-mu-celsius-fw-wed"));

        String result = LocalePreferences.getCalendarType();

        assertEquals(LocalePreferences.CalendarType.GREGORIAN, result);
    }

    @Test
    public void getCalendarType_hasSubTagsAndDisableResolved_resultIsChinese() throws Exception {
        Locale.setDefault(Locale.forLanguageTag("en-US-u-ca-chinese-hc-h24-mu-celsius-fw-wed"));

        String result = LocalePreferences.getCalendarType(false);

        assertEquals(LocalePreferences.CalendarType.CHINESE, result);
    }

    @Test
    public void getCalendarType_hasSubTagsWithoutCalendarTagAndDisableResolved_resultIsEmpty()
            throws Exception {
        Locale.setDefault(Locale.forLanguageTag("en-US-u-mu-celsius-fw-wed"));

        String result = LocalePreferences.getCalendarType(false);

        assertEquals(LocalePreferences.CalendarType.DEFAULT, result);
    }

    @Test
    public void getCalendarType_inputLocaleWithCalendarTag_resultIsChinese() throws Exception {
        String result =
                LocalePreferences.getCalendarType(Locale.forLanguageTag("en-US-u-ca-chinese"));

        assertEquals(LocalePreferences.CalendarType.CHINESE, result);
    }

    @Test
    public void getCalendarType_inputLocaleWithoutCalendarTag_resultIsGregorian() throws Exception {
        String result = LocalePreferences.getCalendarType(Locale.forLanguageTag("en-US"));

        assertEquals(LocalePreferences.CalendarType.GREGORIAN, result);
    }

    @Test
    public void getCalendarType_inputLocaleWithoutCalendarTagAndDisableResolved_resultIsEmpty()
            throws Exception {
        String result = LocalePreferences.getCalendarType(Locale.forLanguageTag("en-US"), false);

        assertEquals(LocalePreferences.CalendarType.DEFAULT, result);
    }

    // Temperature unit
    @Test
    public void getTemperatureUnit_hasSubTags_resultIsCelsius() throws Exception {
        Locale.setDefault(Locale.forLanguageTag("en-US-u-ca-chinese-hc-h24-mu-celsius-fw-wed"));

        String result = LocalePreferences.getTemperatureUnit();

        assertEquals(LocalePreferences.TemperatureUnit.CELSIUS, result);
    }

    @Test
    public void getTemperatureUnit_hasSubTagsWithoutUnitTag_resultIsFahrenheit() throws Exception {
        Locale.setDefault(Locale.forLanguageTag("en-US-u-hc-h24-fw-wed"));

        String result = LocalePreferences.getTemperatureUnit();

        assertEquals(LocalePreferences.TemperatureUnit.FAHRENHEIT, result);
    }

    @Test
    public void getTemperatureUnit_hasSubTagsAndDisableResolved_resultIsCelsius() throws Exception {
        Locale.setDefault(Locale.forLanguageTag("en-US-u-ca-chinese-hc-h24-mu-celsius-fw-wed"));

        String result = LocalePreferences.getTemperatureUnit(false);

        assertEquals(LocalePreferences.TemperatureUnit.CELSIUS, result);
    }

    @Test
    public void getTemperatureUnit_hasSubTagsAndDisableResolved_resultIsFahrenheit()
            throws Exception {
        Locale.setDefault(Locale.forLanguageTag("zh-TW-u-ca-chinese-hc-h24-mu-fahrenhe-fw-wed"));

        String result = LocalePreferences.getTemperatureUnit(false);

        assertEquals(LocalePreferences.TemperatureUnit.FAHRENHEIT, result);
    }

    @Test
    public void getTemperatureUnit_hasSubTagsWithoutUnitTagAndDisableResolved_resultIsEmpty()
            throws Exception {
        Locale.setDefault(Locale.forLanguageTag("en-US-u-fw-wed"));

        String result = LocalePreferences.getTemperatureUnit(false);

        assertEquals(LocalePreferences.TemperatureUnit.DEFAULT, result);
    }

    @Test
    public void getTemperatureUnit_inputLocaleWithUnitTag_resultIsCelsius() throws Exception {
        String result = LocalePreferences
                .getTemperatureUnit(Locale.forLanguageTag("en-US-u-mu-celsius"));

        assertEquals(LocalePreferences.TemperatureUnit.CELSIUS, result);
    }

    @Test
    public void getTemperatureUnit_inputLocaleWithoutUnitTag_resultIsFahrenheit() throws Exception {
        String result = LocalePreferences.getTemperatureUnit(Locale.forLanguageTag("en-US"));

        assertEquals(LocalePreferences.TemperatureUnit.FAHRENHEIT, result);
    }

    @Test
    public void getTemperatureUnit_inputLocaleWithoutUnitTagAndDisableResolved_resultIsEmpty()
            throws Exception {
        String result = LocalePreferences
                .getTemperatureUnit(Locale.forLanguageTag("en-US"), false);

        assertEquals(LocalePreferences.TemperatureUnit.DEFAULT, result);
    }

    // First day of week
    @Test
    public void getFirstDayOfWeek_hasSubTags_resultIsCelsius() throws Exception {
        Locale.setDefault(Locale.forLanguageTag("en-US-u-ca-chinese-hc-h24-mu-celsius-fw-wed"));

        String result = LocalePreferences.getFirstDayOfWeek();

        assertEquals(LocalePreferences.FirstDayOfWeek.WEDNESDAY, result);
    }

    @Test
    public void getFirstDayOfWeek_hasSubTagsWithoutFwTag_resultIsSun() throws Exception {
        Locale.setDefault(Locale.forLanguageTag("en-US-u-hc-h24"));

        String result = LocalePreferences.getFirstDayOfWeek();

        assertEquals(LocalePreferences.FirstDayOfWeek.SUNDAY, result);

    }

    @Test
    public void getFirstDayOfWeek_hasSubTagsAndDisableResolved_resultIsWed() throws Exception {
        Locale.setDefault(Locale.forLanguageTag("en-US-u-ca-chinese-hc-h24-mu-celsius-fw-wed"));

        String result = LocalePreferences.getFirstDayOfWeek(false);

        assertEquals(LocalePreferences.FirstDayOfWeek.WEDNESDAY, result);
    }

    @Test
    public void getFirstDayOfWeek_hasSubTagsWithoutFwTagAndDisableResolved_resultIsEmpty()
            throws Exception {
        Locale.setDefault(Locale.forLanguageTag("en-US-u-ca-chinese"));

        String result = LocalePreferences.getFirstDayOfWeek(false);

        assertEquals(LocalePreferences.FirstDayOfWeek.DEFAULT, result);
    }

    @Test
    public void getFirstDayOfWeek_inputLocaleWithFwTag_resultIsWed() throws Exception {
        String result = LocalePreferences
                .getFirstDayOfWeek(Locale.forLanguageTag("en-US-u-fw-wed"));

        assertEquals(LocalePreferences.FirstDayOfWeek.WEDNESDAY, result);
    }

    @Test
    public void getFirstDayOfWeek_inputLocaleWithoutFwTag_resultIsSun() throws Exception {
        String result = LocalePreferences.getFirstDayOfWeek(Locale.forLanguageTag("en-US"));

        assertEquals(LocalePreferences.FirstDayOfWeek.SUNDAY, result);
    }

    @Test
    public void getFirstDayOfWeek_inputLocaleWithoutFwTagAndDisableResolved_resultIsEmpty()
            throws Exception {
        String result = LocalePreferences
                .getFirstDayOfWeek(Locale.forLanguageTag("en-US"), false);

        assertEquals(LocalePreferences.FirstDayOfWeek.DEFAULT, result);
    }
}
