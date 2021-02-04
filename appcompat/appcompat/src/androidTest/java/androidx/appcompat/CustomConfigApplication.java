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

package androidx.appcompat;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.os.LocaleList;

import androidx.appcompat.app.NightModeCustomAttachBaseContextActivity;

import java.util.Locale;

/**
 * Application that mimics Opera Browser's application-level locale customization.
 */
public class CustomConfigApplication extends androidx.multidex.MultiDexApplication {
    private static final Locale CUSTOM_LOCALE =
            NightModeCustomAttachBaseContextActivity.CUSTOM_LOCALE;

    public void onCreate() {
        applyLanguage(this, CUSTOM_LOCALE);
    }

    private void applyLanguage(Context context, Locale locale) {
        Locale.setDefault(locale);
        applyLanguageToConfiguration(Resources.getSystem(), locale);
        applyLanguageToConfiguration(context.getResources(), locale);
    }

    private void applyLanguageToConfiguration(Resources res, Locale locale) {
        setLocale(res.getConfiguration(), locale);
        res.updateConfiguration(res.getConfiguration(), res.getDisplayMetrics());
    }

    private void setLocale(Configuration configuration, Locale locale) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            configuration.setLocales(new LocaleList(locale));
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            configuration.setLocale(locale);
        } else {
            configuration.locale = locale;
        }
    }
}
