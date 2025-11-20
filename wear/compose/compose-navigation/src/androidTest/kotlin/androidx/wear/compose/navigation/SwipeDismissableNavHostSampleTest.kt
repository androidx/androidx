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
package androidx.wear.compose.navigation

import android.os.Build
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeRight
import androidx.test.filters.SdkSuppress
import androidx.wear.compose.navigation.samples.NavHostWithNamedArgument
import androidx.wear.compose.navigation.samples.SimpleNavHost
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Rule
import org.junit.Test

class SwipeDismissableNavHostSampleTest {
    @get:Rule val rule = createComposeRule(effectContext = StandardTestDispatcher())

    @Test
    fun toggles_between_destinations_in_simplenavhost() {
        rule.setContent { SimpleNavHost() }

        rule.onNodeWithText("On").performClick()
        rule.onNodeWithText("Off").performClick()

        rule.onNodeWithText("On").assertExists()
    }

    @Test
    fun navigates_to_named_arguments() {
        rule.setContent { NavHostWithNamedArgument() }

        rule.onNodeWithText("Item 1").performClick()

        rule.onNodeWithText("Details Screen").assertExists()
    }

    @Test
    @SdkSuppress(maxSdkVersion = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    fun swipes_back_from_named_arguments() {

        rule.setContent { NavHostWithNamedArgument() }

        rule.onNodeWithText("Item 1").performClick()
        rule.onRoot().performTouchInput { swipeRight() }

        rule.onNodeWithText("List Screen").assertExists()
    }
}
