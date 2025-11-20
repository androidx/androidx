/*
 * Copyright (C) 2020 The Android Open Source Project
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
package androidx.appcompat.widget;

import android.content.Context;
import android.content.res.Configuration;
import android.view.ContextThemeWrapper;

import java.util.Locale;

public class AppCompatSpinnerRtlActivity extends AppCompatSpinnerActivity {
    protected void attachBaseContext(Context newBase) {
        Configuration overrideConfig = new Configuration();
        overrideConfig.fontScale = 0;
        // Mark activity to use RTL language / locale
        overrideConfig.locale = new Locale("ar", "sa");

        ContextThemeWrapper wrappedBase = new ContextThemeWrapper(
                newBase, androidx.appcompat.R.style.Theme_AppCompat_Empty);
        wrappedBase.applyOverrideConfiguration(overrideConfig);

        newBase = wrappedBase;

        super.attachBaseContext(newBase);
    }
}
