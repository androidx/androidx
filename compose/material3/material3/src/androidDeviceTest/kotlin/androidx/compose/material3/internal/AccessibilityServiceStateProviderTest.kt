/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.material3.internal

import androidx.compose.runtime.SideEffect
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class AccessibilityServiceStateProviderTest {

    @get:Rule val rule = createComposeRule()

    @Test
    fun firstComposition_doesNotRecompose() {
        // Connecting UiAutomation enables accessibility without enabling touch exploration.
        InstrumentationRegistry.getInstrumentation().uiAutomation

        var compositions = 0
        rule.setContent {
            rememberAccessibilityServiceState().value
            SideEffect { compositions++ }
        }

        rule.runOnIdle { assertThat(compositions).isEqualTo(1) }
    }
}
