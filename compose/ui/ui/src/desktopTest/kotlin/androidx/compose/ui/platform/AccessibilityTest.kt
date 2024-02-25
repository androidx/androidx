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

package androidx.compose.ui.platform

import androidx.compose.foundation.layout.Box
import androidx.compose.material.Button
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.Tab
import androidx.compose.material.TabRow
import androidx.compose.material.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.assertThat
import androidx.compose.ui.isEqualTo
import androidx.compose.ui.platform.a11y.AccessibilityController
import androidx.compose.ui.platform.a11y.ComposeAccessible
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.isContainer
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import java.awt.Point
import javax.accessibility.AccessibleContext
import javax.accessibility.AccessibleRole
import javax.accessibility.AccessibleText
import kotlin.test.fail
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class AccessibilityTest {

    @get:Rule
    val rule = createComposeRule()

    private lateinit var testAccessibleController: AccessibilityController

    @Before
    fun initTestController() {
        testAccessibleController = AccessibilityController(
            owner = rule.onRoot().fetchSemanticsNode().root!!.semanticsOwner,
            desktopComponent = PlatformComponent.Empty,
            onFocusReceived = { }
        )
    }

    private fun SemanticsNodeInteraction.accessibleContext(): AccessibleContext =
        ComposeAccessible(
            semanticsNode = fetchSemanticsNode(),
            controller = testAccessibleController
        ).accessibleContext!!

    private fun SemanticsNodeInteraction.assertHasAccessibleRole(role: AccessibleRole) {
        assertThat(accessibleContext().accessibleRole).isEqualTo(role)
    }

    @Test
    fun accessibleText() {
        rule.setContent {
            Text("Hello world. Hi world.", modifier = Modifier.testTag("text"))
        }

        val accessibleContext = rule.onNodeWithTag("text").accessibleContext()
        val accessibleText = accessibleContext.accessibleText!!
        assertEquals(22, accessibleText.charCount)

        assertEquals("H", accessibleText.getAtIndex(AccessibleText.CHARACTER, 0))
        assertEquals("Hello", accessibleText.getAtIndex(AccessibleText.WORD, 0))
        assertEquals("Hello world. ", accessibleText.getAtIndex(AccessibleText.SENTENCE, 0))

        assertEquals("e", accessibleText.getAfterIndex(AccessibleText.CHARACTER, 0))
        assertEquals("world", accessibleText.getAfterIndex(AccessibleText.WORD, 0))
        assertEquals("Hi world.", accessibleText.getAfterIndex(AccessibleText.SENTENCE, 0))

        assertEquals("d", accessibleText.getBeforeIndex(AccessibleText.CHARACTER, 21))
        assertEquals("world", accessibleText.getBeforeIndex(AccessibleText.WORD, 21))
        assertEquals("Hi world", accessibleText.getBeforeIndex(AccessibleText.SENTENCE, 21))

        assertEquals(0, accessibleText.getIndexAtPoint(Point(0, 0)))
        assertEquals("Hello world. Hi world.".length, accessibleText.getIndexAtPoint(Point(10000, 10000)))
    }

    @Test
    fun tabHasPageTabAccessibleRole() {
        rule.setContent {
            TabRow(selectedTabIndex = 0) {
                Tab(
                    selected = true,
                    onClick = { },
                    modifier = Modifier.testTag("tab"),
                    text = { Text("Tab") }
                )
            }
        }

        rule.onNodeWithTag("tab").assertHasAccessibleRole(AccessibleRole.PAGE_TAB)
    }

    @Test
    fun dropDownListRoleTranslatesToComboBoxAccessibleRole() {
        rule.setContent {
            Button(
                modifier = Modifier
                    .semantics { role = Role.DropdownList }
                    .testTag("button"),
                onClick = { }
            ) {
                Text("Button")
            }
        }

        rule.onNodeWithTag("button").assertHasAccessibleRole(AccessibleRole.COMBO_BOX)
    }

    @Test
    fun progressBarHasCorrectRoleAndValues() {
        rule.setContent {
            LinearProgressIndicator(
                progress = 0.2f,
                modifier = Modifier.testTag("progressbar")
            )
        }

        rule.onNodeWithTag("progressbar").apply {
            val context = accessibleContext()
            val value = context.accessibleValue
                ?: fail("No accessibleValue on LinearProgressIndicator")

            assertThat(context.accessibleRole).isEqualTo(AccessibleRole.PROGRESS_BAR)
            assertThat(value.minimumAccessibleValue).isEqualTo(0f)
            assertThat(value.maximumAccessibleValue).isEqualTo(1f)
            assertThat(value.currentAccessibleValue).isEqualTo(0.2f)
        }
    }

    @Test
    fun boxHasUnknownRole() {
        rule.setContent {
            Box(Modifier.testTag("box"))
        }

        rule.onNodeWithTag("box").assertHasAccessibleRole(AccessibleRole.UNKNOWN)
    }

    @Suppress("DEPRECATION")
    @Test
    fun containerHasGroupRole() {
        rule.setContent {
            Box(Modifier.testTag("box").semantics {
                isContainer = true
            })
        }

        rule.onNodeWithTag("box").assertHasAccessibleRole(AccessibleRole.GROUP_BOX)
    }

    @Test
    fun traversalGroupHasGroupRole() {
        rule.setContent {
            Box(Modifier.testTag("box").semantics {
                isTraversalGroup = true
            })
        }

        rule.onNodeWithTag("box").assertHasAccessibleRole(AccessibleRole.GROUP_BOX)
    }
}