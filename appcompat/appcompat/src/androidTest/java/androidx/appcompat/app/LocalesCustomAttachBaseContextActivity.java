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
import android.content.res.Resources;
import android.os.Build;

/**
 * An activity with customized configuration.
 */
public class LocalesCustomAttachBaseContextActivity extends LocalesUpdateActivity {
    public static final float CUSTOM_FONT_SCALE = 4.24f;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(useCustomConfig(newBase));
    }

    private Context useCustomConfig(Context context) {
        if (Build.VERSION.SDK_INT >= 24) {
            Configuration config = new Configuration();
            config.fontScale = CUSTOM_FONT_SCALE;
            return context.createConfigurationContext(config);
        } else {
            Resources res = context.getResources();
            Configuration config = new Configuration(res.getConfiguration());
            config.fontScale = CUSTOM_FONT_SCALE;
            res.updateConfiguration(config, res.getDisplayMetrics());
            return context;
        }
    }
}
