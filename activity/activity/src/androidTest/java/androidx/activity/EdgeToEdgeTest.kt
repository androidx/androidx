/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.activity

import android.graphics.Color
import android.os.Build
import android.view.WindowManager
import androidx.core.view.WindowInsetsControllerCompat
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.testutils.withActivity
import androidx.testutils.withUse
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class EdgeToEdgeTest {

    @Suppress("DEPRECATION")
    @Test
    fun enableAuto() {
        withUse(ActivityScenario.launch(ComponentActivity::class.java)) {
            withActivity {
                enableEdgeToEdge()
                val view = window.decorView
                if (Build.VERSION.SDK_INT >= 29) {
                    assertThat(window.statusBarColor).isEqualTo(Color.TRANSPARENT)
                    assertThat(window.navigationBarColor).isEqualTo(Color.TRANSPARENT)
                    WindowInsetsControllerCompat(window, view).run {
                        assertThat(isAppearanceLightStatusBars).isTrue()
                        assertThat(isAppearanceLightNavigationBars).isTrue()
                    }
                } else if (Build.VERSION.SDK_INT >= 26) {
                    assertThat(window.statusBarColor).isEqualTo(Color.TRANSPARENT)
                    assertThat(window.navigationBarColor).isEqualTo(DefaultLightScrim)
                    WindowInsetsControllerCompat(window, view).run {
                        assertThat(isAppearanceLightStatusBars).isTrue()
                        assertThat(isAppearanceLightNavigationBars).isTrue()
                    }
                } else {
                    assertThat(window.statusBarColor).isEqualTo(Color.TRANSPARENT)
                    assertThat(window.navigationBarColor).isEqualTo(DefaultDarkScrim)
                    WindowInsetsControllerCompat(window, view).run {
                        assertThat(isAppearanceLightStatusBars).isTrue()
                    }
                }
                if (Build.VERSION.SDK_INT >= 30) {
                    assertThat(window.attributes.layoutInDisplayCutoutMode)
                        .isEqualTo(WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS)
                } else if (Build.VERSION.SDK_INT >= 28) {
                    assertThat(window.attributes.layoutInDisplayCutoutMode)
                        .isEqualTo(
                            WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                        )
                }
            }
        }
    }

    @Suppress("DEPRECATION")
    @Test
    fun enableCustom() {
        withUse(ActivityScenario.launch(ComponentActivity::class.java)) {
            withActivity {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(Color.CYAN, Color.DKGRAY) { _ -> false },
                    navigationBarStyle =
                        SystemBarStyle.auto(Color.CYAN, Color.DKGRAY) { _ -> false },
                )
                val view = window.decorView
                if (Build.VERSION.SDK_INT >= 29) {
                    assertThat(window.statusBarColor).isEqualTo(Color.TRANSPARENT)
                    assertThat(window.navigationBarColor).isEqualTo(Color.TRANSPARENT)
                    WindowInsetsControllerCompat(window, view).run {
                        assertThat(isAppearanceLightStatusBars).isTrue()
                        assertThat(isAppearanceLightNavigationBars).isTrue()
                    }
                } else if (Build.VERSION.SDK_INT >= 26) {
                    assertThat(window.statusBarColor).isEqualTo(Color.CYAN)
                    assertThat(window.navigationBarColor).isEqualTo(Color.CYAN)
                    WindowInsetsControllerCompat(window, view).run {
                        assertThat(isAppearanceLightStatusBars).isTrue()
                        assertThat(isAppearanceLightNavigationBars).isTrue()
                    }
                } else {
                    assertThat(window.statusBarColor).isEqualTo(Color.CYAN)
                    assertThat(window.navigationBarColor).isEqualTo(Color.DKGRAY)
                    WindowInsetsControllerCompat(window, view).run {
                        assertThat(isAppearanceLightStatusBars).isTrue()
                    }
                }
                if (Build.VERSION.SDK_INT >= 30) {
                    assertThat(window.attributes.layoutInDisplayCutoutMode)
                        .isEqualTo(WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS)
                } else if (Build.VERSION.SDK_INT >= 28) {
                    assertThat(window.attributes.layoutInDisplayCutoutMode)
                        .isEqualTo(
                            WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                        )
                }
            }
        }
    }

    @SdkSuppress(maxSdkVersion = 34) // b/427246798
    @Suppress("DEPRECATION")
    @Test
    fun enableDark() {
        withUse(ActivityScenario.launch(ComponentActivity::class.java)) {
            withActivity {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.dark(Color.DKGRAY),
                    navigationBarStyle = SystemBarStyle.dark(Color.DKGRAY),
                )
                val view = window.decorView
                if (Build.VERSION.SDK_INT >= 35) {
                    assertThat(window.statusBarColor).isEqualTo(Color.TRANSPARENT)
                    assertThat(window.navigationBarColor).isEqualTo(Color.TRANSPARENT)
                    WindowInsetsControllerCompat(window, view).run {
                        assertThat(isAppearanceLightStatusBars).isFalse()
                        assertThat(isAppearanceLightNavigationBars).isFalse()
                    }
                } else if (Build.VERSION.SDK_INT >= 26) {
                    assertThat(window.statusBarColor).isEqualTo(Color.DKGRAY)
                    assertThat(window.navigationBarColor).isEqualTo(Color.DKGRAY)
                    WindowInsetsControllerCompat(window, view).run {
                        assertThat(isAppearanceLightStatusBars).isFalse()
                        assertThat(isAppearanceLightNavigationBars).isFalse()
                    }
                } else {
                    assertThat(window.statusBarColor).isEqualTo(Color.DKGRAY)
                    assertThat(window.navigationBarColor).isEqualTo(Color.DKGRAY)
                    WindowInsetsControllerCompat(window, view).run {
                        assertThat(isAppearanceLightStatusBars).isFalse()
                    }
                }
                if (Build.VERSION.SDK_INT >= 30) {
                    assertThat(window.attributes.layoutInDisplayCutoutMode)
                        .isEqualTo(WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS)
                } else if (Build.VERSION.SDK_INT >= 28) {
                    assertThat(window.attributes.layoutInDisplayCutoutMode)
                        .isEqualTo(
                            WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                        )
                }
            }
        }
    }

    @SdkSuppress(maxSdkVersion = 34) // b/427246798
    @Suppress("DEPRECATION")
    @Test
    fun enableLight() {
        withUse(ActivityScenario.launch(ComponentActivity::class.java)) {
            withActivity {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.light(Color.CYAN, Color.DKGRAY),
                    navigationBarStyle = SystemBarStyle.light(Color.CYAN, Color.DKGRAY),
                )
                val view = window.decorView
                if (Build.VERSION.SDK_INT >= 35) {
                    assertThat(window.statusBarColor).isEqualTo(Color.TRANSPARENT)
                    assertThat(window.navigationBarColor).isEqualTo(Color.TRANSPARENT)
                    WindowInsetsControllerCompat(window, view).run {
                        assertThat(isAppearanceLightStatusBars).isTrue()
                        assertThat(isAppearanceLightNavigationBars).isTrue()
                    }
                } else if (Build.VERSION.SDK_INT >= 26) {
                    assertThat(window.statusBarColor).isEqualTo(Color.CYAN)
                    assertThat(window.navigationBarColor).isEqualTo(Color.CYAN)
                    WindowInsetsControllerCompat(window, view).run {
                        assertThat(isAppearanceLightStatusBars).isTrue()
                        assertThat(isAppearanceLightNavigationBars).isTrue()
                    }
                } else {
                    assertThat(window.statusBarColor).isEqualTo(Color.CYAN)
                    assertThat(window.navigationBarColor).isEqualTo(Color.DKGRAY)
                    WindowInsetsControllerCompat(window, view).run {
                        assertThat(isAppearanceLightStatusBars).isTrue()
                    }
                }
                if (Build.VERSION.SDK_INT >= 30) {
                    assertThat(window.attributes.layoutInDisplayCutoutMode)
                        .isEqualTo(WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS)
                } else if (Build.VERSION.SDK_INT >= 28) {
                    assertThat(window.attributes.layoutInDisplayCutoutMode)
                        .isEqualTo(
                            WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                        )
                }
            }
        }
    }
}
