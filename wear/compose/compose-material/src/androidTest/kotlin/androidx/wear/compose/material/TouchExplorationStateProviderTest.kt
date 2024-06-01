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

package androidx.wear.compose.material

import android.content.Context
import android.view.accessibility.AccessibilityManager
import androidx.compose.runtime.State
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class TouchExplorationStateProviderTest {

    @get:Rule val rule = createComposeRule()

    @Test
    fun returns_correct_values() {
        lateinit var accessibilityManager: AccessibilityManager
        lateinit var accessibilityStatus: State<Boolean>
        rule.setContent {
            val context = LocalContext.current
            accessibilityManager =
                context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager

            val touchExplorationStateProvider = DefaultTouchExplorationStateProvider()
            accessibilityStatus = touchExplorationStateProvider.touchExplorationState()
        }
        assertEquals(
            accessibilityStatus.value,
            accessibilityManager.isEnabled && accessibilityManager.isTouchExplorationEnabled
        )
    }
}
