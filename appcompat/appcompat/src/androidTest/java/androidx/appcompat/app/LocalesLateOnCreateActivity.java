/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.appcompat.app;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.os.LocaleList;

import androidx.core.os.LocaleListCompat;

import java.util.Locale;

/**
 * An activity with systemLocales replaced with some customised locales for testing.
 */
public class LocalesLateOnCreateActivity extends LocalesUpdateActivity {

    public static LocaleListCompat DEFAULT_LOCALE_LIST;
    public static LocaleListCompat TEST_LOCALE_LIST;
    public static LocaleListCompat EXPECTED_LOCALE_LIST;

    @Override
    public void onCreate(Bundle bundle) {
        // Override locales so that AppCompat attempts to re-apply during onCreate().

        if (Build.VERSION.SDK_INT >= 21) {
            DEFAULT_LOCALE_LIST = LocaleListCompat.forLanguageTags(
                    Locale.US.toLanguageTag() + "," + Locale.CHINESE.toLanguageTag());
            TEST_LOCALE_LIST = LocaleListCompat.forLanguageTags(
                    Locale.CANADA_FRENCH.toLanguageTag() + ","
                            + Locale.US.toLanguageTag());
            EXPECTED_LOCALE_LIST = LocaleListCompat.forLanguageTags(
                    Locale.CANADA_FRENCH.toLanguageTag() + ","
                            + Locale.US.toLanguageTag() + "," + Locale.CHINESE.toLanguageTag());
        } else {
            DEFAULT_LOCALE_LIST = LocaleListCompat.create(Locale.US);
            TEST_LOCALE_LIST = LocaleListCompat.create(Locale.CANADA_FRENCH);
            EXPECTED_LOCALE_LIST = LocaleListCompat.create(Locale.CANADA_FRENCH);
        }
        disableAutomaticLocales(getApplicationContext());

        super.onCreate(bundle);
    }

    private static void setLocales(LocaleListCompat locales, Context context) {
        Configuration conf = context.getResources().getConfiguration();
        if (Build.VERSION.SDK_INT >= 24) {
            conf.setLocales(LocaleList.forLanguageTags(locales.toLanguageTags()));
        } else if (Build.VERSION.SDK_INT >= 17) {
            conf.setLocale(locales.get(0));
        } else {
            conf.locale = locales.get(0);
        }
        // updateConfiguration is required to make the configuration change stick.
        // updateConfiguration must be called before any use of the actual Resources.
        context.getResources().updateConfiguration(conf,
                context.getResources().getDisplayMetrics());
    }

    /**
     * Ensures the context does not use system locales, instead uses the DEFAULT_LOCALE_LIST
     *
     * <p>This must be called before a Context's Resources are used for the first time. {@code
     * Activity.onCreate} is a great place to call {@code disableAutomaticLocales(this)}
     */
    public static void disableAutomaticLocales(Context context) {
        setLocales(DEFAULT_LOCALE_LIST, context);
    }
}
