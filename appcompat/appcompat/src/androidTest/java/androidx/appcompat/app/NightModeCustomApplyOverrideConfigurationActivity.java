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

package androidx.appcompat.app;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Build;

import androidx.annotation.RequiresApi;

import java.util.Locale;

@RequiresApi(17)
public class NightModeCustomApplyOverrideConfigurationActivity extends NightModeActivity {
    public static final float CUSTOM_FONT_SCALE = 4.24f;
    public static final Locale CUSTOM_LOCALE = Locale.CANADA_FRENCH;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(newBase);

        Configuration config = new Configuration();
        config.fontScale = CUSTOM_FONT_SCALE;
        applyOverrideConfiguration(config);
    }

    @Override
    public void applyOverrideConfiguration(Configuration newConfig) {
        super.applyOverrideConfiguration(updateConfigurationIfSupported(newConfig));
    }

    private Configuration updateConfigurationIfSupported(Configuration config) {
        // Configuration.getLocales is added after 24 and Configuration.locale is deprecated in 24
        if (Build.VERSION.SDK_INT >= 24) {
            if (!config.getLocales().isEmpty()) {
                return config;
            }
        } else {
            if (config.locale != null) {
                return config;
            }
        }

        Locale locale = CUSTOM_LOCALE;
        if (locale != null) {
            // Configuration.setLocale is added after 17 and Configuration.locale is deprecated
            // after 24
            if (Build.VERSION.SDK_INT >= 17) {
                config.setLocale(locale);
            } else {
                config.locale = locale;
            }
        }
        return config;
    }
}
