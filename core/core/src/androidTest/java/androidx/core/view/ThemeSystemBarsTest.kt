/*
 * Copyright (C) 2022 The Android Open Source Project
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

package androidx.core.view

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.testutils.withActivity
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class ThemeSystemBarsTest {

    @SdkSuppress(maxSdkVersion = 22)
    @Test
    fun statusBar_dark_before_supported() {
        val scenario = ActivityScenario.launch(LightSystemBarsActivity::class.java)

        val insetsController = scenario.withActivity {
            WindowCompat.getInsetsController(window, window.decorView)
        }

        assertThat(insetsController.isAppearanceLightStatusBars).isFalse()

        scenario.close()
    }

    @SdkSuppress(minSdkVersion = 23)
    @Test
    fun statusBar_light() {
        val scenario = ActivityScenario.launch(LightSystemBarsActivity::class.java)

        val insetsController = scenario.withActivity {
            WindowCompat.getInsetsController(window, window.decorView)
        }

        assertThat(insetsController.isAppearanceLightStatusBars).isTrue()

        scenario.close()
    }

    @SdkSuppress(minSdkVersion = 23)
    @Test
    fun statusBar_dark() {
        val scenario = ActivityScenario.launch(DarkSystemBarsActivity::class.java)

        val insetsController = scenario.withActivity {
            WindowCompat.getInsetsController(window, window.decorView)
        }

        assertThat(insetsController.isAppearanceLightStatusBars).isFalse()

        scenario.close()
    }

    @SdkSuppress(maxSdkVersion = 26)
    @Test
    fun navigationBar_dark_before_supported() {
        val scenario = ActivityScenario.launch(LightSystemBarsActivity::class.java)

        val insetsController = scenario.withActivity {
            WindowCompat.getInsetsController(window, window.decorView)
        }

        assertThat(insetsController.isAppearanceLightNavigationBars).isFalse()

        scenario.close()
    }

    @SdkSuppress(minSdkVersion = 27)
    @Test
    fun navigationBar_light() {
        val scenario = ActivityScenario.launch(LightSystemBarsActivity::class.java)

        val insetsController = scenario.withActivity {
            WindowCompat.getInsetsController(window, window.decorView)
        }

        assertThat(insetsController.isAppearanceLightNavigationBars).isTrue()

        scenario.close()
    }

    @SdkSuppress(minSdkVersion = 27)
    @Test
    fun navigationBar_dark() {
        val scenario = ActivityScenario.launch(DarkSystemBarsActivity::class.java)

        val insetsController = scenario.withActivity {
            WindowCompat.getInsetsController(window, window.decorView)
        }

        assertThat(insetsController.isAppearanceLightNavigationBars).isFalse()

        scenario.close()
    }
}
