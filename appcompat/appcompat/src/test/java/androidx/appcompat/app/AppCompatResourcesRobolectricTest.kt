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

package androidx.appcompat.app

import android.app.Application
import androidx.appcompat.R
import androidx.test.core.app.ApplicationProvider
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(maxSdk = 28)
class AppCompatResourcesRobolectricTest {

    /**
     * This is a test only for Robolectric. Currently Robolectric allows tests to assume that the
     * application theme will be propagated to Activities. There are a number of tests in the
     * wild which assume that the following works:
     *
     * ```
     * ApplicationProvider.getApplicationContext<Application>()
     *     .setTheme(R.style.Theme_MyApp)
     *
     * val activity = buildActivity(AppCompatActivity::class.java).get()
     * // activity also has it's theme set to R.style.Theme_MyApp
     * ```
     *
     * This has never been the case on Android devices. We have a workaround in AppCompat to
     * enable this to continue to work in Robolectric, and this test ensures that the workaround
     * works. If this behavior is fixed in Robolectric, this test and the workaround can be removed.
     */
    @Test
    fun testApplicationThemeIsCopiedToActivity() {
        val application = ApplicationProvider.getApplicationContext<Application>()
        application.setTheme(R.style.Theme_AppCompat_Light)

        // If the theme is not propagated to the Activity, AppCompatActivity will throw a:
        // 'IllegalStateException: You need to use a Theme.AppCompat theme'
        Robolectric.buildActivity(AppCompatActivity::class.java)
            .create().start().resume().get()
    }
}
