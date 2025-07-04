/*
 * Copyright 2025 The Android Open Source Project
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
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class LocalResourcesTest {
    @get:Rule val rule = createComposeRule()

    @Test
    fun localResourcesInvalidatesOnConfigurationChange() {
        lateinit var view: View
        var compositionCount = 0
        rule.setContent {
            view = LocalView.current
            with(LocalResources.current) { compositionCount++ }
        }
        rule.runOnIdle { assertThat(compositionCount).isEqualTo(1) }
        val configuration = view.context.resources.configuration
        // Make a deep copy - mutating the original configuration will affect other tests
        val configurationCopy = Configuration(configuration)
        // Dispatch the new configuration: new instance that compares equal so we shouldn't
        // invalidate LocalConfiguration, and hence shouldn't invalidate LocalResources
        view.dispatchConfigurationChanged(configurationCopy)
        rule.runOnIdle { assertThat(compositionCount).isEqualTo(1) }
        configurationCopy.densityDpi *= 2
        // Same instance but different fields, so we should invalidate LocalConfiguration and
        // hence LocalResources
        view.dispatchConfigurationChanged(configurationCopy)
        rule.runOnIdle { assertThat(compositionCount).isEqualTo(2) }
    }
}
