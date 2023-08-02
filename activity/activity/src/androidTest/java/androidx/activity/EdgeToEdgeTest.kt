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
import androidx.core.view.WindowInsetsControllerCompat
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.testutils.withActivity
import androidx.testutils.withUse
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class EdgeToEdgeTest {

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
                } else if (Build.VERSION.SDK_INT >= 23) {
                    assertThat(window.statusBarColor).isEqualTo(Color.TRANSPARENT)
                    assertThat(window.navigationBarColor).isEqualTo(DefaultDarkScrim)
                    WindowInsetsControllerCompat(window, view).run {
                        assertThat(isAppearanceLightStatusBars).isTrue()
                    }
                }
            }
        }
    }

    @Test
    fun enableCustom() {
        withUse(ActivityScenario.launch(ComponentActivity::class.java)) {
            withActivity {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(Color.CYAN, Color.DKGRAY) { _ -> false },
                    navigationBarStyle = SystemBarStyle
                        .auto(Color.CYAN, Color.DKGRAY) { _ -> false }
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
                } else if (Build.VERSION.SDK_INT >= 23) {
                    assertThat(window.statusBarColor).isEqualTo(Color.CYAN)
                    assertThat(window.navigationBarColor).isEqualTo(Color.DKGRAY)
                    WindowInsetsControllerCompat(window, view).run {
                        assertThat(isAppearanceLightStatusBars).isTrue()
                    }
                }
            }
        }
    }

    @Test
    fun enableDark() {
        withUse(ActivityScenario.launch(ComponentActivity::class.java)) {
            withActivity {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.dark(Color.DKGRAY),
                    navigationBarStyle = SystemBarStyle.dark(Color.DKGRAY)
                )
                val view = window.decorView
                if (Build.VERSION.SDK_INT >= 26) {
                    assertThat(window.statusBarColor).isEqualTo(Color.DKGRAY)
                    assertThat(window.navigationBarColor).isEqualTo(Color.DKGRAY)
                    WindowInsetsControllerCompat(window, view).run {
                        assertThat(isAppearanceLightStatusBars).isFalse()
                        assertThat(isAppearanceLightNavigationBars).isFalse()
                    }
                } else if (Build.VERSION.SDK_INT >= 23) {
                    assertThat(window.statusBarColor).isEqualTo(Color.DKGRAY)
                    assertThat(window.navigationBarColor).isEqualTo(Color.DKGRAY)
                    WindowInsetsControllerCompat(window, view).run {
                        assertThat(isAppearanceLightStatusBars).isFalse()
                    }
                }
            }
        }
    }

    @Test
    fun enableLight() {
        withUse(ActivityScenario.launch(ComponentActivity::class.java)) {
            withActivity {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.light(Color.CYAN, Color.DKGRAY),
                    navigationBarStyle = SystemBarStyle.light(Color.CYAN, Color.DKGRAY),
                )
                val view = window.decorView
                if (Build.VERSION.SDK_INT >= 26) {
                    assertThat(window.statusBarColor).isEqualTo(Color.CYAN)
                    assertThat(window.navigationBarColor).isEqualTo(Color.CYAN)
                    WindowInsetsControllerCompat(window, view).run {
                        assertThat(isAppearanceLightStatusBars).isTrue()
                        assertThat(isAppearanceLightNavigationBars).isTrue()
                    }
                } else if (Build.VERSION.SDK_INT >= 23) {
                    assertThat(window.statusBarColor).isEqualTo(Color.CYAN)
                    assertThat(window.navigationBarColor).isEqualTo(Color.DKGRAY)
                    WindowInsetsControllerCompat(window, view).run {
                        assertThat(isAppearanceLightStatusBars).isTrue()
                    }
                }
            }
        }
    }
}
