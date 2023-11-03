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

package androidx.appcompat.testutils

import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate

@Suppress("DEPRECATION")
class NightModeActivityTestRule<T : AppCompatActivity>(
    activityClazz: Class<T>,
    initialTouchMode: Boolean = false,
    launchActivity: Boolean = true
) : androidx.test.rule.ActivityTestRule<T>(activityClazz, initialTouchMode, launchActivity) {
    override fun beforeActivityLaunched() {
        // By default we'll set the night mode to NO, which allows us to make better
        // assumptions in the test below
        runOnUiThread {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
    }

    override fun afterActivityFinished() {
        // Reset the default night mode
        runOnUiThread {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
    }
}
