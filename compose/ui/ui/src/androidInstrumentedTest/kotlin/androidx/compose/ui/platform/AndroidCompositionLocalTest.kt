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

package androidx.compose.ui.platform

import android.content.res.Configuration
import android.view.View
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.compose.ui.window.Popup
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class AndroidCompositionLocalTest {
    @get:Rule
    val rule = createComposeRule()
    private val context = InstrumentationRegistry.getInstrumentation().context

    @Test
    fun fontFamilyResolver_isPassedToPopup() {
        val expected = createFontFamilyResolver(context)
        var actual: FontFamily.Resolver? = null
        rule.setContent {
            CompositionLocalProvider(
                LocalFontFamilyResolver provides expected
            ) {
                Popup {
                    val popupResolver = LocalFontFamilyResolver.current
                    SideEffect {
                        actual = popupResolver
                    }
                }
            }
        }
        rule.runOnIdle {
            assertThat(actual).isSameInstanceAs(expected)
        }
    }

    @Test
    fun localConfigurationComparesForEquality_sameInstance() {
        lateinit var view: View
        var compositionCount = 0
        rule.setContent {
            view = LocalView.current
            with(LocalConfiguration.current) {
                compositionCount++
            }
        }
        rule.runOnIdle {
            assertThat(compositionCount).isEqualTo(1)
        }
        val configuration = view.context.resources.configuration
        // Dispatch the same configuration: same instance and compares equal so we shouldn't
        // invalidate LocalConfiguration
        view.dispatchConfigurationChanged(configuration)
        rule.runOnIdle {
            assertThat(compositionCount).isEqualTo(1)
        }
        configuration.densityDpi *= 2
        // Same instance but different fields, so we should invalidate LocalConfiguration
        view.dispatchConfigurationChanged(configuration)
        rule.runOnIdle {
            assertThat(compositionCount).isEqualTo(2)
        }
        configuration.screenHeightDp *= 2
        // Same instance but different fields, so we should invalidate LocalConfiguration
        view.dispatchConfigurationChanged(configuration)
        rule.runOnIdle {
            assertThat(compositionCount).isEqualTo(3)
        }
    }

    @Test
    fun localConfigurationComparesForEquality_newInstance() {
        lateinit var view: View
        var compositionCount = 0
        rule.setContent {
            view = LocalView.current
            with(LocalConfiguration.current) {
                compositionCount++
            }
        }
        rule.runOnIdle {
            assertThat(compositionCount).isEqualTo(1)
        }
        val configuration = view.context.resources.configuration
        // Make a deep copy
        val configurationCopy = Configuration(configuration)
        // New instance, but compares equal, so we shouldn't invalidate LocalConfiguration
        view.dispatchConfigurationChanged(configurationCopy)
        rule.runOnIdle {
            assertThat(compositionCount).isEqualTo(1)
        }
        // Make another deep copy and mutate its fields
        val configurationCopy2 = Configuration(configuration).apply { densityDpi *= 2 }
        // New instance and different fields, so we should invalidate LocalConfiguration
        view.dispatchConfigurationChanged(configurationCopy2)
        rule.runOnIdle {
            assertThat(compositionCount).isEqualTo(2)
        }
        // Make another deep copy and mutate its fields
        val configurationCopy3 = Configuration(configurationCopy2).apply { screenHeightDp *= 2 }
        // New instance and different fields, so we should invalidate LocalConfiguration
        view.dispatchConfigurationChanged(configurationCopy3)
        rule.runOnIdle {
            assertThat(compositionCount).isEqualTo(3)
        }
    }
}
