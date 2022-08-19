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

import androidx.annotation.RequiresApi;

/**
 * An activity that has a customized fontScale, set before onCreate().
 */
@RequiresApi(17)
public class LocalesCustomApplyOverrideConfigurationActivity extends LocalesUpdateActivity {
    public static final float CUSTOM_FONT_SCALE = 4.24f;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(newBase);

        Configuration config = new Configuration();
        config.fontScale = CUSTOM_FONT_SCALE;
        super.applyOverrideConfiguration(config);
    }
}
