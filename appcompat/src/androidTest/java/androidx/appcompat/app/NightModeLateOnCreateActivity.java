/*
 * Copyright 2019 The Android Open Source Project
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
import android.os.Bundle;

public class NightModeLateOnCreateActivity extends NightModeActivity {

    @Override
    public void onCreate(Bundle bundle) {
        // Disable night mode so that AppCompat attempts to re-apply during onCreate().
        disableAutomaticNightMode(getApplicationContext());

        super.onCreate(bundle);
    }

    private static void setNightMode(boolean enabled, Context context) {
        Configuration conf = context.getResources().getConfiguration();
        int mode = enabled ? Configuration.UI_MODE_NIGHT_YES : Configuration.UI_MODE_NIGHT_NO;
        conf.uiMode = mode | (conf.uiMode & ~Configuration.UI_MODE_NIGHT_MASK);

        // updateConfiguration is required to make the configuration change stick.
        // updateConfiguration must be called before any use of the actual Resources.
        context.getResources().updateConfiguration(conf,
                context.getResources().getDisplayMetrics());
    }

    /**
     * Ensure a context does not use night mode if the system setting enables night mode.
     *
     * <p>This must be called before a Context's Resources are used for the first time. {@code
     * Activity.onCreate} is a great place to call {@code disableAutomaticNightMode(this)}
     */
    public static void disableAutomaticNightMode(Context context) {
        setNightMode(false, context);
    }
}
