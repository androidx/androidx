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

package androidx.compose.material3.a2ui

import androidx.a2ui.compose.ui.A2uiCatalog
import androidx.a2ui.compose.ui.testing.A2uiTestController
import androidx.a2ui.compose.ui.testing.A2uiTestSurface
import androidx.a2ui.compose.ui.testing.getData
import androidx.a2ui.model.protocol.A2uiComponentPayload
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isPopup
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MaterialChoicePickerComponentTest {

    private val testCatalog =
        A2uiCatalog(catalogId = "test_catalog", components = listOf(MaterialChoicePickerComponent))

    // =======================================================
    // Category 1: Checkbox & Dropdown Selection Interactions
    // =======================================================

    @Test
    fun defaultProps_mutuallyExclusiveCheckbox_rendersDropdownAndToggles() = runComposeUiTest {
        val optionsList =
            listOf(
                mapOf("label" to "Option A", "value" to "opt_a"),
                mapOf("label" to "Option B", "value" to "opt_b"),
            )
        val choicePickerPayload =
            A2uiComponentPayload(
                id = "root",
                type = "ChoicePicker",
                properties =
                    mapOf("options" to optionsList, "value" to mapOf("path" to "/selection")),
            )
        val initialData = mapOf("selection" to listOf("opt_a"))

        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents = listOf(choicePickerPayload),
                initialData = initialData,
            )
        val surface = controller.start()

        setContent { MaterialTheme { A2uiTestSurface(surface) } }

        // Selected label is displayed in the anchor text field
        onNodeWithText("Option A").assertIsDisplayed()

        // Click anchor to open dropdown
        onNodeWithText("Option A").performClick()
        controller.waitForIdle()

        // Dropdown menu items are visible -> select Option B
        onNodeWithText("Option B").assertIsDisplayed().performClick()
        controller.waitForIdle()

        // Data model updated to opt_b and anchor displays Option B
        assertThat(controller.getData<List<String>>("/selection")).containsExactly("opt_b")
        onNodeWithText("Option B").assertIsDisplayed()
    }

    @Test
    fun multipleSelection_checkbox_rendersDropdownAndTogglesMultiple() = runComposeUiTest {
        val optionsList =
            listOf(
                mapOf("label" to "Pizza", "value" to "pizza"),
                mapOf("label" to "Burger", "value" to "burger"),
                mapOf("label" to "Salad", "value" to "salad"),
            )
        val choicePickerPayload =
            A2uiComponentPayload(
                id = "root",
                type = "ChoicePicker",
                properties =
                    mapOf(
                        "variant" to "multipleSelection",
                        "displayStyle" to "checkbox",
                        "options" to optionsList,
                        "value" to mapOf("path" to "/selectedFoods"),
                    ),
            )
        val initialData = mapOf("selectedFoods" to listOf("pizza"))

        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents = listOf(choicePickerPayload),
                initialData = initialData,
            )
        val surface = controller.start()

        setContent { MaterialTheme { A2uiTestSurface(surface) } }

        onNodeWithText("Pizza").assertIsDisplayed()

        // Click anchor text field to expand dropdown
        onNodeWithText("Select options").performClick()
        controller.waitForIdle()

        // Select Burger in dropdown -> both Pizza and Burger selected as chips
        onNodeWithText("Burger").performClick()
        controller.waitForIdle()

        assertThat(controller.getData<List<String>>("/selectedFoods"))
            .containsExactly("pizza", "burger")
    }

    @Test
    fun multipleSelection_checkbox_deselectsItemInsideDropdown() = runComposeUiTest {
        val optionsList =
            listOf(
                mapOf("label" to "Pizza", "value" to "pizza"),
                mapOf("label" to "Burger", "value" to "burger"),
            )
        val choicePickerPayload =
            A2uiComponentPayload(
                id = "root",
                type = "ChoicePicker",
                properties =
                    mapOf(
                        "variant" to "multipleSelection",
                        "displayStyle" to "checkbox",
                        "options" to optionsList,
                        "value" to mapOf("path" to "/selectedFoods"),
                    ),
            )
        val initialData = mapOf("selectedFoods" to listOf("pizza", "burger"))

        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents = listOf(choicePickerPayload),
                initialData = initialData,
            )
        val surface = controller.start()

        setContent { MaterialTheme { A2uiTestSurface(surface) } }

        // Expand dropdown menu
        onNodeWithText("Select options").performClick()
        controller.waitForIdle()

        // Deselect "Burger" inside the open dropdown menu
        onNode(hasText("Burger") and hasAnyAncestor(isPopup())).performClick()
        controller.waitForIdle()

        assertThat(controller.getData<List<String>>("/selectedFoods")).containsExactly("pizza")
    }

    @Test
    fun mutuallyExclusive_dropdown_reclickSelected_keepsSelection() = runComposeUiTest {
        val optionsList =
            listOf(
                mapOf("label" to "Option A", "value" to "opt_a"),
                mapOf("label" to "Option B", "value" to "opt_b"),
            )
        val choicePickerPayload =
            A2uiComponentPayload(
                id = "root",
                type = "ChoicePicker",
                properties =
                    mapOf("options" to optionsList, "value" to mapOf("path" to "/selection")),
            )
        val initialData = mapOf("selection" to listOf("opt_a"))

        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents = listOf(choicePickerPayload),
                initialData = initialData,
            )
        val surface = controller.start()

        setContent { MaterialTheme { A2uiTestSurface(surface) } }

        onNode(hasText("Option A") and hasClickAction()).performClick()
        controller.waitForIdle()

        // Re-clicking Option A in the open dropdown menu
        onNode(hasText("Option A") and hasAnyAncestor(isPopup())).assertIsSelected().performClick()
        controller.waitForIdle()

        // Keeps opt_a selected
        assertThat(controller.getData<List<String>>("/selection")).containsExactly("opt_a")
        onNodeWithText("Option A").assertIsDisplayed()
    }

    @Test
    fun headerLabel_switchesPlaceholderToShowOptions() = runComposeUiTest {
        val optionsList =
            listOf(
                mapOf("label" to "Choice 1", "value" to "1"),
                mapOf("label" to "Choice 2", "value" to "2"),
            )
        val choicePickerPayload =
            A2uiComponentPayload(
                id = "root",
                type = "ChoicePicker",
                properties =
                    mapOf(
                        "label" to "Header Title",
                        "options" to optionsList,
                        "value" to mapOf("path" to "/emptySelection"),
                    ),
            )
        val initialData = mapOf("emptySelection" to listOf<String>())

        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents = listOf(choicePickerPayload),
                initialData = initialData,
            )
        val surface = controller.start()

        setContent { MaterialTheme { A2uiTestSurface(surface) } }

        // Header label is displayed prominently
        onNodeWithText("Header Title").assertIsDisplayed()

        // Placeholder "Select options" must NOT be rendered when header exists
        onNodeWithText("Select options").assertDoesNotExist()

        // Placeholder "Show options" must be rendered instead
        onNodeWithText("Show options").assertIsDisplayed()
    }

    @Test
    fun filterable_mutuallyExclusive_rendersChipWithoutDismissIcon() = runComposeUiTest {
        val optionsList =
            listOf(
                mapOf("label" to "Apple", "value" to "apple"),
                mapOf("label" to "Banana", "value" to "banana"),
            )
        val choicePickerPayload =
            A2uiComponentPayload(
                id = "root",
                type = "ChoicePicker",
                properties =
                    mapOf(
                        "variant" to "mutuallyExclusive",
                        "filterable" to true,
                        "options" to optionsList,
                        "value" to mapOf("path" to "/fruit"),
                    ),
            )
        val initialData = mapOf("fruit" to listOf("apple"))

        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents = listOf(choicePickerPayload),
                initialData = initialData,
            )
        val surface = controller.start()

        setContent { MaterialTheme { A2uiTestSurface(surface) } }

        // Filterable mutually exclusive shows selected item as chip inside trailing icon slot
        onNodeWithText("Apple").assertIsDisplayed()
        // No remove icon description for non-dismissible chip
        onNodeWithContentDescription("Remove Apple").assertDoesNotExist()
    }

    @Test
    fun filterable_mutuallyExclusive_clearsFilterQueryOnSelection() = runComposeUiTest {
        val optionsList =
            listOf(
                mapOf("label" to "Apple", "value" to "apple"),
                mapOf("label" to "Pineapple", "value" to "pineapple"),
            )
        val choicePickerPayload =
            A2uiComponentPayload(
                id = "root",
                type = "ChoicePicker",
                properties =
                    mapOf(
                        "variant" to "mutuallyExclusive",
                        "filterable" to true,
                        "options" to optionsList,
                        "value" to mapOf("path" to "/fruitSelection"),
                    ),
            )
        val initialData = mapOf("fruitSelection" to listOf<String>())

        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents = listOf(choicePickerPayload),
                initialData = initialData,
            )
        val surface = controller.start()

        setContent { MaterialTheme { A2uiTestSurface(surface) } }

        // Enter filter query "pine"
        onNode(hasSetTextAction()).performTextInput("pine")
        controller.waitForIdle()

        // Select Pineapple from menu -> selection updates and filter query clears
        onNodeWithText("Pineapple").performClick()
        controller.waitForIdle()

        assertThat(controller.getData<List<String>>("/fruitSelection")).containsExactly("pineapple")

        // Input text has been reset (query "pine" no longer exists in input)
        onNode(hasSetTextAction() and hasText("pine")).assertDoesNotExist()
    }

    @Test
    fun isReady_missingOptionLabel_remainsLoading() = runComposeUiTest {
        // Option 1 has value but missing "label" (label path is unpopulated)
        val optionsList =
            listOf(
                mapOf(
                    "label" to mapOf("path" to "/unpopulated/label"),
                    "value" to "opt_without_label",
                )
            )
        val choicePickerPayload =
            A2uiComponentPayload(
                id = "root",
                type = "ChoicePicker",
                properties =
                    mapOf("options" to optionsList, "value" to mapOf("path" to "/selection")),
            )
        val initialData = mapOf("selection" to listOf<String>())

        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents = listOf(choicePickerPayload),
                initialData = initialData,
            )
        val surface = controller.start()

        setContent {
            MaterialTheme {
                A2uiTestSurface(
                    surface = surface,
                    onLoading = { modifier ->
                        Text("Loading...", modifier = modifier.testTag("custom_loader"))
                    },
                )
            }
        }

        // Component should remain loading because option label is unpopulated
        onNodeWithTag("custom_loader").assertIsDisplayed()
    }

    // =======================================================
    // Category 2: Chips Variant & Display Styles
    // =======================================================

    @Test
    fun displayStyle_chips_rendersFilterChips() = runComposeUiTest {
        val optionsList =
            listOf(
                mapOf("label" to "Android", "value" to "android"),
                mapOf("label" to "Kotlin", "value" to "kotlin"),
            )
        val choicePickerPayload =
            A2uiComponentPayload(
                id = "root",
                type = "ChoicePicker",
                properties =
                    mapOf(
                        "displayStyle" to "chips",
                        "variant" to "multipleSelection",
                        "options" to optionsList,
                        "value" to mapOf("path" to "/tags"),
                    ),
            )
        val initialData = mapOf("tags" to listOf("kotlin"))

        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents = listOf(choicePickerPayload),
                initialData = initialData,
            )
        val surface = controller.start()

        setContent { MaterialTheme { A2uiTestSurface(surface) } }

        onNodeWithText("Android").assertIsDisplayed().assertIsNotSelected()
        onNodeWithText("Kotlin").assertIsDisplayed().assertIsSelected()

        // Click Android chip to select it
        onNodeWithText("Android").performClick()
        controller.waitForIdle()

        onNodeWithText("Android").assertIsSelected()
        onNodeWithText("Kotlin").assertIsSelected()
        assertThat(controller.getData<List<String>>("/tags")).containsExactly("kotlin", "android")
    }

    @Test
    fun displayStyle_chips_multipleSelection_deselectsChip() = runComposeUiTest {
        val optionsList =
            listOf(
                mapOf("label" to "Android", "value" to "android"),
                mapOf("label" to "Kotlin", "value" to "kotlin"),
            )
        val choicePickerPayload =
            A2uiComponentPayload(
                id = "root",
                type = "ChoicePicker",
                properties =
                    mapOf(
                        "displayStyle" to "chips",
                        "variant" to "multipleSelection",
                        "options" to optionsList,
                        "value" to mapOf("path" to "/tags"),
                    ),
            )
        val initialData = mapOf("tags" to listOf("kotlin", "android"))

        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents = listOf(choicePickerPayload),
                initialData = initialData,
            )
        val surface = controller.start()

        setContent { MaterialTheme { A2uiTestSurface(surface) } }

        onNodeWithText("Kotlin").assertIsSelected()

        // Click selected Kotlin chip to deselect it
        onNodeWithText("Kotlin").performClick()
        controller.waitForIdle()

        onNodeWithText("Kotlin").assertIsNotSelected()
        assertThat(controller.getData<List<String>>("/tags")).containsExactly("android")
    }

    @Test
    fun displayStyle_chips_mutuallyExclusive_togglesSingleSelection() = runComposeUiTest {
        val optionsList =
            listOf(
                mapOf("label" to "Small", "value" to "s"),
                mapOf("label" to "Medium", "value" to "m"),
                mapOf("label" to "Large", "value" to "l"),
            )
        val choicePickerPayload =
            A2uiComponentPayload(
                id = "root",
                type = "ChoicePicker",
                properties =
                    mapOf(
                        "displayStyle" to "chips",
                        "variant" to "mutuallyExclusive",
                        "options" to optionsList,
                        "value" to mapOf("path" to "/selectedSize"),
                    ),
            )
        val initialData = mapOf("selectedSize" to listOf("s"))

        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents = listOf(choicePickerPayload),
                initialData = initialData,
            )
        val surface = controller.start()

        setContent { MaterialTheme { A2uiTestSurface(surface) } }

        onNodeWithText("Small").assertIsSelected()
        onNodeWithText("Medium").assertIsNotSelected()

        // Click Medium -> Small is deselected, Medium selected
        onNodeWithText("Medium").performClick()
        controller.waitForIdle()

        onNodeWithText("Small").assertIsNotSelected()
        onNodeWithText("Medium").assertIsSelected()
        assertThat(controller.getData<List<String>>("/selectedSize")).containsExactly("m")

        // Click Medium again -> keeps Medium selected (mutually exclusive maintains selection)
        onNodeWithText("Medium").performClick()
        controller.waitForIdle()

        onNodeWithText("Medium").assertIsSelected()
        assertThat(controller.getData<List<String>>("/selectedSize")).containsExactly("m")
    }

    @Test
    fun displayStyle_chips_filterable_filtersChipsBySearchInput() = runComposeUiTest {
        val optionsList =
            listOf(
                mapOf("label" to "Red", "value" to "red"),
                mapOf("label" to "Green", "value" to "green"),
                mapOf("label" to "Blue", "value" to "blue"),
            )
        val choicePickerPayload =
            A2uiComponentPayload(
                id = "root",
                type = "ChoicePicker",
                properties =
                    mapOf(
                        "displayStyle" to "chips",
                        "filterable" to true,
                        "options" to optionsList,
                        "value" to mapOf("path" to "/selectedColor"),
                    ),
            )
        val initialData = mapOf("selectedColor" to listOf<String>())

        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents = listOf(choicePickerPayload),
                initialData = initialData,
            )
        val surface = controller.start()

        setContent { MaterialTheme { A2uiTestSurface(surface) } }

        onNodeWithText("Red").assertIsDisplayed()
        onNodeWithText("Green").assertIsDisplayed()
        onNodeWithText("Blue").assertIsDisplayed()

        // Type filter query "re"
        onNode(hasSetTextAction()).performTextInput("re")
        controller.waitForIdle()

        onNodeWithText("Red").assertIsDisplayed()
        onNodeWithText("Green").assertIsDisplayed()
        onNodeWithText("Blue").assertDoesNotExist()

        // Click Green chip
        onNodeWithText("Green").performClick()
        controller.waitForIdle()

        assertThat(controller.getData<List<String>>("/selectedColor")).containsExactly("green")
    }

    // =======================================================
    // Category 3: Group Label & Filtering
    // =======================================================

    @Test
    fun label_groupLabel_rendersTextAndUpdatesDynamically() = runComposeUiTest {
        val optionsList =
            listOf(
                mapOf("label" to "Option 1", "value" to "1"),
                mapOf("label" to "Option 2", "value" to "2"),
            )
        val choicePickerPayload =
            A2uiComponentPayload(
                id = "root",
                type = "ChoicePicker",
                properties =
                    mapOf(
                        "label" to mapOf("path" to "/form/groupTitle"),
                        "options" to optionsList,
                        "value" to listOf("1"),
                    ),
            )
        val initialData = mapOf("form" to mapOf("groupTitle" to "Select Your Plan"))

        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents = listOf(choicePickerPayload),
                initialData = initialData,
            )
        val surface = controller.start()

        setContent { MaterialTheme { A2uiTestSurface(surface) } }

        onNodeWithText("Select Your Plan").assertIsDisplayed()

        controller.updateData("/form/groupTitle", "Choose a Subscription")
        controller.waitForIdle()

        onNodeWithText("Select Your Plan").assertDoesNotExist()
        onNodeWithText("Choose a Subscription").assertIsDisplayed()
    }

    @Test
    fun filterable_true_filtersOptionsCaseInsensitively() = runComposeUiTest {
        val optionsList =
            listOf(
                mapOf("label" to "Apple", "value" to "apple"),
                mapOf("label" to "Banana", "value" to "banana"),
                mapOf("label" to "Pineapple", "value" to "pineapple"),
                mapOf("label" to "Orange", "value" to "orange"),
            )
        val choicePickerPayload =
            A2uiComponentPayload(
                id = "root",
                type = "ChoicePicker",
                properties =
                    mapOf(
                        "filterable" to true,
                        "options" to optionsList,
                        "value" to mapOf("path" to "/fruitSelection"),
                    ),
            )
        val initialData = mapOf("fruitSelection" to listOf<String>())

        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents = listOf(choicePickerPayload),
                initialData = initialData,
            )
        val surface = controller.start()

        setContent { MaterialTheme { A2uiTestSurface(surface) } }

        // Click anchor to open dropdown
        onNode(hasSetTextAction()).performClick()
        controller.waitForIdle()

        // All options visible initially
        onNodeWithText("Apple").assertIsDisplayed()
        onNodeWithText("Banana").assertIsDisplayed()
        onNodeWithText("Pineapple").assertIsDisplayed()
        onNodeWithText("Orange").assertIsDisplayed()

        // Type filter query "app"
        onNode(hasSetTextAction()).performTextInput("app")
        controller.waitForIdle()

        onNodeWithText("Apple").assertIsDisplayed()
        onNodeWithText("Pineapple").assertIsDisplayed()
        onNodeWithText("Banana").assertDoesNotExist()
        onNodeWithText("Orange").assertDoesNotExist()

        // Select Pineapple while filtered
        onNodeWithText("Pineapple").performClick()
        controller.waitForIdle()

        assertThat(controller.getData<List<String>>("/fruitSelection")).containsExactly("pineapple")

        onNode(hasSetTextAction()).performClick()
        controller.waitForIdle()

        // Clear filter query (onValueChange expands dropdown and restores all options)
        onNode(hasSetTextAction()).performTextReplacement("")
        controller.waitForIdle()

        onNodeWithText("Apple").assertIsDisplayed()
        onNodeWithText("Banana").assertIsDisplayed()
        onNode(hasText("Pineapple") and hasAnyAncestor(isPopup()))
            .assertIsDisplayed()
            .assertIsSelected()
        onNodeWithText("Orange").assertIsDisplayed()
    }

    @Test
    fun filterable_true_queryWithNoMatches_showsNoOptions() = runComposeUiTest {
        val optionsList =
            listOf(
                mapOf("label" to "Alpha", "value" to "a"),
                mapOf("label" to "Beta", "value" to "b"),
            )
        val choicePickerPayload =
            A2uiComponentPayload(
                id = "root",
                type = "ChoicePicker",
                properties =
                    mapOf(
                        "displayStyle" to "chips",
                        "filterable" to true,
                        "options" to optionsList,
                        "value" to mapOf("path" to "/selection"),
                    ),
            )
        val initialData = mapOf("selection" to listOf("a"))

        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents = listOf(choicePickerPayload),
                initialData = initialData,
            )
        val surface = controller.start()

        setContent { MaterialTheme { A2uiTestSurface(surface) } }

        onNodeWithText("Alpha").assertIsDisplayed()
        onNodeWithText("Beta").assertIsDisplayed()

        // Filter with non-matching query
        onNode(hasSetTextAction()).performTextInput("xyz")
        controller.waitForIdle()

        onNodeWithText("Alpha").assertDoesNotExist()
        onNodeWithText("Beta").assertDoesNotExist()
    }

    @Test
    fun filterable_clearButton_clearsFilterQuery() = runComposeUiTest {
        val optionsList =
            listOf(
                mapOf("label" to "Alpha", "value" to "a"),
                mapOf("label" to "Beta", "value" to "b"),
            )
        val choicePickerPayload =
            A2uiComponentPayload(
                id = "root",
                type = "ChoicePicker",
                properties =
                    mapOf(
                        "displayStyle" to "chips",
                        "filterable" to true,
                        "options" to optionsList,
                        "value" to mapOf("path" to "/selection"),
                    ),
            )
        val initialData = mapOf("selection" to listOf("a"))

        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents = listOf(choicePickerPayload),
                initialData = initialData,
            )
        val surface = controller.start()

        setContent { MaterialTheme { A2uiTestSurface(surface) } }

        // Type filter query "xyz" -> no options visible
        onNode(hasSetTextAction()).performTextInput("xyz")
        controller.waitForIdle()

        onNodeWithText("Alpha").assertDoesNotExist()

        // Click clear search icon button
        onNodeWithContentDescription("Clear search filter").performClick()
        controller.waitForIdle()

        // Filter cleared -> options visible again
        onNodeWithText("Alpha").assertIsDisplayed()
        onNodeWithText("Beta").assertIsDisplayed()
    }

    @Test
    fun filterable_multipleSelection_retainsQueryAndLeavesDropdownOpenOnSelection() =
        runComposeUiTest {
            val optionsList =
                listOf(
                    mapOf("label" to "Apple", "value" to "apple"),
                    mapOf("label" to "Pineapple", "value" to "pineapple"),
                )
            val choicePickerPayload =
                A2uiComponentPayload(
                    id = "root",
                    type = "ChoicePicker",
                    properties =
                        mapOf(
                            "variant" to "multipleSelection",
                            "filterable" to true,
                            "options" to optionsList,
                            "value" to mapOf("path" to "/fruits"),
                        ),
                )
            val initialData = mapOf("fruits" to listOf<String>())

            val controller =
                A2uiTestController(
                    catalog = testCatalog,
                    initialComponents = listOf(choicePickerPayload),
                    initialData = initialData,
                )
            val surface = controller.start()

            setContent { MaterialTheme { A2uiTestSurface(surface) } }

            // Type filter query "app"
            onNode(hasSetTextAction()).performTextInput("app")
            controller.waitForIdle()

            // Select Apple from menu
            onNodeWithText("Apple").performClick()
            controller.waitForIdle()

            assertThat(controller.getData<List<String>>("/fruits")).containsExactly("apple")

            // Filter query "app" remains in text field and Pineapple is still visible in dropdown
            onNode(hasSetTextAction() and hasText("app")).assertIsDisplayed()
            onNodeWithText("Pineapple").assertIsDisplayed()
        }

    @Test
    fun emptyOptionsList_rendersNoMatchingOptionsText() = runComposeUiTest {
        val optionsList = emptyList<Map<String, String>>()
        val choicePickerPayload =
            A2uiComponentPayload(
                id = "root",
                type = "ChoicePicker",
                properties =
                    mapOf(
                        "displayStyle" to "chips",
                        "options" to optionsList,
                        "value" to mapOf("path" to "/selection"),
                    ),
            )
        val initialData = mapOf("selection" to listOf<String>())

        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents = listOf(choicePickerPayload),
                initialData = initialData,
            )
        val surface = controller.start()

        setContent { MaterialTheme { A2uiTestSurface(surface) } }

        onNodeWithText("No matching options").assertIsDisplayed()
    }

    @Test
    fun missingVariantAndStyle_fallbackToDefaults() = runComposeUiTest {
        val optionsList =
            listOf(
                mapOf("label" to "Choice 1", "value" to "c1"),
                mapOf("label" to "Choice 2", "value" to "c2"),
            )
        val choicePickerPayload =
            A2uiComponentPayload(
                id = "root",
                type = "ChoicePicker",
                properties =
                    mapOf(
                        "options" to optionsList,
                        "value" to mapOf("path" to "/selection"),
                    ),
            )
        val initialData = mapOf("selection" to listOf("c1"))

        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents = listOf(choicePickerPayload),
                initialData = initialData,
            )
        val surface = controller.start()

        setContent { MaterialTheme { A2uiTestSurface(surface) } }

        // Omitted displayStyle falls back to Checkbox (dropdown) and mutuallyExclusive
        onNodeWithText("Choice 1").assertIsDisplayed()
    }

    @Test
    fun dynamicUpdate_toggleFilterableTrue_displaysPlaceholderText() = runComposeUiTest {
        val optionsList =
            listOf(
                mapOf("label" to "Choice A", "value" to "a"),
                mapOf("label" to "Choice B", "value" to "b"),
            )
        val choicePickerPayload =
            A2uiComponentPayload(
                id = "root",
                type = "ChoicePicker",
                properties =
                    mapOf(
                        "filterable" to false,
                        "options" to optionsList,
                        "value" to mapOf("path" to "/selection"),
                    ),
            )
        val initialData = mapOf("selection" to listOf<String>())

        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents = listOf(choicePickerPayload),
                initialData = initialData,
            )
        val surface = controller.start()

        setContent { MaterialTheme { A2uiTestSurface(surface) } }

        // Filterable is false initially -> filter placeholder does not exist
        onNodeWithText("Start typing to filter…").assertDoesNotExist()

        // Dynamically update component payload to set filterable = true
        controller.updateComponent(
            id = "root",
            properties =
                mapOf(
                    "filterable" to true,
                    "options" to optionsList,
                    "value" to mapOf("path" to "/selection"),
                ),
        )
        controller.waitForIdle()

        // Filter placeholder is now immediately rendered
        onNodeWithText("Start typing to filter…").assertIsDisplayed()
    }

    // =======================================================
    // Category 4: Static Bindings & External Data Updates
    // =======================================================

    @Test
    fun staticValue_readOnly_preventsToggling() = runComposeUiTest {
        val optionsList =
            listOf(
                mapOf("label" to "Read Only 1", "value" to "1"),
                mapOf("label" to "Read Only 2", "value" to "2"),
            )
        val choicePickerPayload =
            A2uiComponentPayload(
                id = "root",
                type = "ChoicePicker",
                properties = mapOf("options" to optionsList, "value" to listOf("1")),
            )

        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents = listOf(choicePickerPayload),
            )
        val surface = controller.start()

        setContent { MaterialTheme { A2uiTestSurface(surface) } }

        // Static read-only -> anchor is disabled and cannot open dropdown
        onNodeWithText("Read Only 1").assertIsDisplayed().assertIsNotEnabled()
    }

    @Test
    fun staticValue_chips_readOnly_disablesChips() = runComposeUiTest {
        val optionsList =
            listOf(
                mapOf("label" to "Chip A", "value" to "a"),
                mapOf("label" to "Chip B", "value" to "b"),
            )
        val choicePickerPayload =
            A2uiComponentPayload(
                id = "root",
                type = "ChoicePicker",
                properties =
                    mapOf(
                        "displayStyle" to "chips",
                        "options" to optionsList,
                        "value" to listOf("a"),
                    ),
            )

        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents = listOf(choicePickerPayload),
            )
        val surface = controller.start()

        setContent { MaterialTheme { A2uiTestSurface(surface) } }

        // Static read-only value -> filter chips are disabled
        onNodeWithText("Chip A").assertIsDisplayed().assertIsNotEnabled()
        onNodeWithText("Chip B").assertIsDisplayed().assertIsNotEnabled()
    }

    @Test
    fun externalDataChange_updatesSelectedOption() = runComposeUiTest {
        val optionsList =
            listOf(
                mapOf("label" to "Item A", "value" to "a"),
                mapOf("label" to "Item B", "value" to "b"),
            )
        val choicePickerPayload =
            A2uiComponentPayload(
                id = "root",
                type = "ChoicePicker",
                properties =
                    mapOf("options" to optionsList, "value" to mapOf("path" to "/selectedItem")),
            )
        val initialData = mapOf("selectedItem" to listOf("a"))

        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents = listOf(choicePickerPayload),
                initialData = initialData,
            )
        val surface = controller.start()

        setContent { MaterialTheme { A2uiTestSurface(surface) } }

        onNodeWithText("Item A").assertIsDisplayed()

        controller.updateData("/selectedItem", listOf("b"))
        controller.waitForIdle()

        onNodeWithText("Item A").assertDoesNotExist()
        onNodeWithText("Item B").assertIsDisplayed()
    }

    @Test
    fun componentPayloadUpdate_updatesOptionsAndValues() = runComposeUiTest {
        val initialOptions =
            listOf(
                mapOf("label" to "Initial 1", "value" to "1"),
                mapOf("label" to "Initial 2", "value" to "2"),
            )
        val choicePickerPayload =
            A2uiComponentPayload(
                id = "root",
                type = "ChoicePicker",
                properties = mapOf("options" to initialOptions, "value" to listOf("1")),
            )

        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents = listOf(choicePickerPayload),
            )
        val surface = controller.start()

        setContent { MaterialTheme { A2uiTestSurface(surface) } }

        onNodeWithText("Initial 1").assertIsDisplayed()

        val updatedOptions =
            listOf(
                mapOf("label" to "Updated 1", "value" to "1"),
                mapOf("label" to "Updated 2", "value" to "2"),
            )
        controller.updateComponent(
            id = "root",
            properties = mapOf("options" to updatedOptions, "value" to listOf("2")),
        )
        controller.waitForIdle()

        onNodeWithText("Updated 2").assertIsDisplayed()
        onNodeWithText("Initial 1").assertDoesNotExist()
    }

    // =======================================================
    // Category 5: Progressive Loading & Readiness
    // =======================================================

    @Test
    fun isReady_unresolvedProperties_remainsInLoadingStateUntilDataArrives() = runComposeUiTest {
        val optionsList =
            listOf(
                mapOf("label" to mapOf("path" to "/opt1/label"), "value" to "1"),
                mapOf("label" to mapOf("path" to "/opt2/label"), "value" to "2"),
            )
        val choicePickerPayload =
            A2uiComponentPayload(
                id = "root",
                type = "ChoicePicker",
                properties =
                    mapOf("options" to optionsList, "value" to mapOf("path" to "/form/selection")),
            )

        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents = listOf(choicePickerPayload),
            )
        val surface = controller.start()

        setContent {
            MaterialTheme {
                A2uiTestSurface(
                    surface = surface,
                    onLoading = { modifier ->
                        Text(
                            "Loading ChoicePicker...",
                            modifier = modifier.testTag("custom_loader"),
                        )
                    },
                )
            }
        }

        // Selection and option labels missing -> loading
        onNodeWithTag("custom_loader").assertIsDisplayed()

        // Selection arrives -> still loading because option labels missing
        controller.updateData("/form/selection", listOf("1"))
        controller.waitForIdle()
        onNodeWithTag("custom_loader").assertIsDisplayed()

        // Option 1 label arrives -> still loading
        controller.updateData("/opt1/label", "First Option")
        controller.waitForIdle()
        onNodeWithTag("custom_loader").assertIsDisplayed()

        // Option 2 label arrives -> success
        controller.updateData("/opt2/label", "Second Option")
        controller.waitForIdle()

        onNodeWithTag("custom_loader").assertDoesNotExist()
        onNodeWithText("First Option").assertIsDisplayed()
    }

    @Test
    fun duplicateOptionValues_reportsErrorAndRendersErrorFallback() = runComposeUiTest {
        val optionsList =
            listOf(
                mapOf("label" to "Apple 1", "value" to "apple"),
                mapOf("label" to "Apple 2", "value" to "apple"),
            )
        val choicePickerPayload =
            A2uiComponentPayload(
                id = "root",
                type = "ChoicePicker",
                properties = mapOf("options" to optionsList, "value" to listOf("apple")),
            )

        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents = listOf(choicePickerPayload),
            )
        val surface = controller.start()

        setContent {
            MaterialTheme {
                A2uiTestSurface(
                    surface = surface,
                    onError = { exception, _ -> Text("Error: ${exception.message}") },
                )
            }
        }

        onNodeWithText("Error: Duplicate option values ['apple'] found in ChoicePicker options.")
            .assertIsDisplayed()
    }

    // =======================================================
    // Category 6: Modifiers
    // =======================================================

    @Test
    fun modifier_parentModifier_appliesToRoot() = runComposeUiTest {
        val optionsList = listOf(mapOf("label" to "Option 1", "value" to "1"))
        val choicePickerPayload =
            A2uiComponentPayload(
                id = "root",
                type = "ChoicePicker",
                properties =
                    mapOf("label" to "Choices", "options" to optionsList, "value" to listOf("1")),
            )

        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents = listOf(choicePickerPayload),
            )
        val surface = controller.start()

        setContent {
            MaterialTheme {
                A2uiTestSurface(surface = surface, modifier = Modifier.testTag("choice_picker_tag"))
            }
        }

        onNode(hasText("Choices")).assertIsDisplayed()
        onNodeWithTag("choice_picker_tag").assertIsDisplayed()
    }

    @Test
    fun modifier_parameterChanges_updatesRenderedModifier() = runComposeUiTest {
        val optionsList = listOf(mapOf("label" to "Option 1", "value" to "1"))
        val choicePickerPayload =
            A2uiComponentPayload(
                id = "root",
                type = "ChoicePicker",
                properties = mapOf("options" to optionsList, "value" to listOf("1")),
            )

        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents = listOf(choicePickerPayload),
            )
        val surface = controller.start()

        var modifier by mutableStateOf(Modifier.testTag("initial_tag"))

        setContent { MaterialTheme { A2uiTestSurface(surface = surface, modifier = modifier) } }

        onNodeWithTag("initial_tag").assertIsDisplayed()

        modifier = Modifier.testTag("updated_tag")
        waitForIdle()

        onNodeWithTag("initial_tag").assertDoesNotExist()
        onNodeWithTag("updated_tag").assertIsDisplayed()
    }
}
