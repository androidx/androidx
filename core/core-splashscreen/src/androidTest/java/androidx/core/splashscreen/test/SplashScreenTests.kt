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

package androidx.core.splashscreen.test

import androidx.test.filters.LargeTest
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.google.common.truth.Truth
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4ClassRunner::class)
class SplashScreenTests {

    private lateinit var device: UiDevice

    @Before
    fun setUp() {
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    }

    @Test
    fun iconBackgroundSetForIconTheme() {
        val splashScreen =
            startActivityWithSplashScreen(SplashScreenWithIconBgTestActivity::class, device) {
                it.putExtra(EXTRA_ANIMATION_LISTENER, true)
            }
        Truth.assertThat(splashScreen.splashScreenIconViewBackground).isNotNull()
    }

    @Test
    fun noIconBackgroundOnDefaultTheme() {
        val splashScreen =
            startActivityWithSplashScreen(SplashScreenTestActivity::class, device) {
                it.putExtra(EXTRA_ANIMATION_LISTENER, true)
            }
        Truth.assertThat(splashScreen.splashScreenIconViewBackground).isNull()
    }
}