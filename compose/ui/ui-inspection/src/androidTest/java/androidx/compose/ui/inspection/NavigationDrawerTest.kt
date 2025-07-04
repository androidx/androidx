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

package androidx.compose.ui.inspection

import androidx.compose.ui.inspection.rules.ComposeInspectionRule
import androidx.compose.ui.inspection.rules.sendCommand
import androidx.compose.ui.inspection.testdata.NavigationDrawerTestActivity
import androidx.compose.ui.inspection.util.AllParametersChecks
import androidx.compose.ui.inspection.util.GetAllParametersCommand
import androidx.compose.ui.inspection.util.GetComposablesCommand
import androidx.compose.ui.inspection.util.nodes
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import layoutinspector.compose.inspection.LayoutInspectorComposeProtocol.ComposableNode.Flags
import org.junit.Rule
import org.junit.Test

@LargeTest
class NavigationDrawerTest {
    @get:Rule val rule = ComposeInspectionRule(NavigationDrawerTestActivity::class)

    @Test
    fun testNavigationDrawerDrawableElements() = runBlocking {
        val composableResponse =
            rule.inspectorTester
                .sendCommand(
                    GetComposablesCommand(
                        rootViewId = rule.rootId,
                        skipSystemComposables = false,
                        extractAllParameters = true,
                    )
                )
                .getComposablesResponse

        val parametersResponse =
            rule.inspectorTester
                .sendCommand(
                    GetAllParametersCommand(rootViewId = rule.rootId, skipSystemComposables = false)
                )
                .getAllParametersResponse
        val checks = AllParametersChecks(composableResponse, parametersResponse)

        // Exactly 9 composables are contributing to drawing in this test app.
        // All other composables should be filtered out by the HAS_DRAW_MODIFIER flag.
        // ModalNavigationDrawer must be filtered out.
        val withChildDrawModifiers =
            composableResponse
                .nodes()
                .filter { (it.flags and Flags.SYSTEM_CREATED_VALUE) == 0 }
                .filter { (it.flags and Flags.HAS_CHILD_DRAW_MODIFIER_VALUE) != 0 }

        // If this fails check for changes to NavigationDrawer.
        // We want to skip the draw modifier flag for Scrim used to show the navigation drawer.
        checks.assertNoNode(withChildDrawModifiers, "ModalNavigationDrawer")
        checks.assertNoNode(withChildDrawModifiers, "Scrim")

        // Drawable nodes of the Scrim should have the draw modifier flag.
        assertThat(withChildDrawModifiers).hasSize(9)
        checks.assertIconNode(withChildDrawModifiers[0])
        checks.assertTextNode(withChildDrawModifiers[1], "Navigation Drawer Test App")
        checks.assertTextNode(withChildDrawModifiers[2], "Hello world")
        checks.assertNode(withChildDrawModifiers[3], "ModalDrawerSheet")
        checks.assertTextNode(withChildDrawModifiers[4], "Drawer Title")
        checks.assertNode(withChildDrawModifiers[5], "HorizontalDivider")
        checks.assertTextNode(withChildDrawModifiers[6], "Section 1")
        checks.assertNode(withChildDrawModifiers[7], "NavigationDrawerItem")
        checks.assertTextNode(withChildDrawModifiers[8], "Item 1")
    }
}
