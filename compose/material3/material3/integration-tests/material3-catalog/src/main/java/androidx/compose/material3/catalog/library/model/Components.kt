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

package androidx.compose.material3.catalog.library.model

import androidx.annotation.DrawableRes
import androidx.compose.material3.catalog.library.R
import androidx.compose.material3.catalog.library.util.AdaptiveGuidelinesUrl
import androidx.compose.material3.catalog.library.util.AdaptiveNavigationSuiteMaterial3SourceUrl
import androidx.compose.material3.catalog.library.util.AdaptiveSourceUrl
import androidx.compose.material3.catalog.library.util.ComponentGuidelinesUrl
import androidx.compose.material3.catalog.library.util.DocsUrl
import androidx.compose.material3.catalog.library.util.Material3SourceUrl
import androidx.compose.material3.catalog.library.util.PackageSummaryUrl
import androidx.compose.material3.catalog.library.util.StyleGuidelinesUrl

data class Component(
    val id: Int,
    val name: String,
    val description: String,
    @DrawableRes val icon: Int = R.drawable.ic_component,
    val tintIcon: Boolean = true,
    val guidelinesUrl: String,
    val docsUrl: String,
    val sourceUrl: String,
    val examples: List<Example>
)

private var nextId: Int = 1

private fun nextId(): Int = nextId.also { nextId += 1 }

// Components are ordered alphabetically by name.

private val Adaptive =
    Component(
        id = nextId(),
        name = "Adaptive",
        description =
            "Adaptive scaffolds provides automatic layout adjustment on different window size " +
                "classes and postures.\n\n" +
                "Note: this sample is better experienced in a resizable emulator or foldable device.",
        // No adaptive icon
        guidelinesUrl = "$AdaptiveGuidelinesUrl/understanding-layout/overview",
        docsUrl = "$DocsUrl/adaptive",
        sourceUrl = "$AdaptiveSourceUrl/ThreePaneScaffold.kt",
        examples = AdaptiveExamples
    )

private val Badge =
    Component(
        id = nextId(),
        name = "Badge",
        description =
            "A badge can contain dynamic information, such as the presence of a new " +
                "notification or a number of pending requests. Badges can be icon only or contain " +
                "a short text.",
        // No badge icon
        guidelinesUrl = "$ComponentGuidelinesUrl/badge",
        docsUrl = "$DocsUrl#badge",
        sourceUrl = "$Material3SourceUrl/Badge.kt",
        examples = BadgeExamples
    )

private val BottomAppBars =
    Component(
        id = nextId(),
        name = "Bottom App Bar",
        description =
            "A bottom app bar displays navigation and key actions at the bottom of mobile " +
                "screens.",
        // No bottom app bar icon
        guidelinesUrl = "$ComponentGuidelinesUrl/bottom-app-bars",
        docsUrl = "$DocsUrl#bottomappbar",
        sourceUrl = "$Material3SourceUrl/AppBar.kt",
        examples = BottomAppBarsExamples
    )

private val BottomSheets =
    Component(
        id = nextId(),
        name = "Bottom Sheet",
        description =
            "Bottom sheets are surfaces containing supplementary content, anchored to the " +
                "bottom of the screen.",
        // No bottom sheet icon
        guidelinesUrl = "$ComponentGuidelinesUrl/bottom-sheets",
        docsUrl = "$DocsUrl#bottomsheet",
        sourceUrl = "$Material3SourceUrl/ModalBottomSheet.android.kt",
        examples = BottomSheetExamples
    )

private val Buttons =
    Component(
        id = nextId(),
        name = "Buttons",
        description =
            "Buttons help people initiate actions, from sending an email, to sharing a " +
                "document, to liking a post.",
        // No buttons icon
        guidelinesUrl = "$ComponentGuidelinesUrl/buttons",
        docsUrl = "$PackageSummaryUrl#button",
        sourceUrl = "$Material3SourceUrl/Button.kt",
        examples = ButtonsExamples,
    )

private val ButtonGroups =
    Component(
        id = nextId(),
        name = "Button Groups",
        description =
            "button groups is a container for material components that adds an animation on press",
        guidelinesUrl = "$ComponentGuidelinesUrl/button-groups",
        docsUrl = "$PackageSummaryUrl#buttongroups",
        sourceUrl = "$Material3SourceUrl/ButtonGroup.kt",
        examples = ButtonGroupsExamples,
    )

private val Card =
    Component(
        id = nextId(),
        name = "Card",
        description = "Cards contain content and actions that relate information about a subject.",
        // No card icon
        guidelinesUrl = "$StyleGuidelinesUrl/cards",
        docsUrl = "$PackageSummaryUrl#card",
        sourceUrl = "$Material3SourceUrl/Card.kt",
        examples = CardExamples
    )

private val Carousel =
    Component(
        id = nextId(),
        name = "Carousel",
        description =
            "Carousels are stylized versions of lists that provide a unique viewing and " +
                "behavior that suit large imagery and other visually rich content.",
        // No carousel icon
        guidelinesUrl = "$StyleGuidelinesUrl/carousel",
        docsUrl = "$PackageSummaryUrl#carousel",
        sourceUrl = "$Material3SourceUrl/Carousel.kt",
        examples = CarouselExamples
    )

private val Checkboxes =
    Component(
        id = nextId(),
        name = "Checkboxes",
        description =
            "Checkboxes allow the user to select one or more items from a set or turn an " +
                "option on or off.",
        // No checkbox icon
        guidelinesUrl = "$ComponentGuidelinesUrl/checkboxes",
        docsUrl = "$DocsUrl#checkbox",
        sourceUrl = "$Material3SourceUrl/Checkbox.kt",
        examples = CheckboxesExamples
    )

private val Chips =
    Component(
        id = nextId(),
        name = "Chips",
        description =
            "Chips allow users to enter information, make selections, filter content, or" +
                " trigger actions.",
        // No chip icon
        guidelinesUrl = "$ComponentGuidelinesUrl/chips",
        docsUrl = "$DocsUrl#chips",
        sourceUrl = "$Material3SourceUrl/Chip.kt",
        examples = ChipsExamples
    )

private val DatePickers =
    Component(
        id = nextId(),
        name = "Date pickers",
        description = "Date pickers let users select a date or range of dates.",
        // No date picker icon
        guidelinesUrl = "$ComponentGuidelinesUrl/datepicker",
        docsUrl = "$PackageSummaryUrl#datepicker",
        sourceUrl = "$Material3SourceUrl/DatePicker.kt",
        examples = DatePickerExamples
    )

private val Dialogs =
    Component(
        id = nextId(),
        name = "Dialogs",
        description =
            "Dialogs provide important prompts in a user flow. They can require an action, " +
                "communicate information, or help users accomplish a task.",
        // No dialogs icon
        guidelinesUrl = "$ComponentGuidelinesUrl/dialogs",
        docsUrl = "$PackageSummaryUrl#alertdialog",
        sourceUrl = "$Material3SourceUrl/AlertDialog.kt",
        examples = DialogExamples
    )

private val ExtendedFloatingActionButton =
    Component(
        id = nextId(),
        name = "Extended FAB",
        description =
            "Extended FABs help people take primary actions. They're wider than FABs to " +
                "accommodate a text label and larger target area.",
        // No extended FAB icon
        guidelinesUrl = "$ComponentGuidelinesUrl/extended-fab",
        docsUrl = "$PackageSummaryUrl#extendedfloatingactionbutton",
        sourceUrl = "$Material3SourceUrl/FloatingActionButton.kt",
        examples = ExtendedFABExamples,
    )

private val FloatingActionButtons =
    Component(
        id = nextId(),
        name = "Floating action buttons",
        description =
            "The FAB represents the most important action on a screen. It puts key actions " +
                "within reach.",
        // No FABs icon
        guidelinesUrl = "$ComponentGuidelinesUrl/floating-action-button",
        docsUrl = "$PackageSummaryUrl#floatingactionbutton",
        sourceUrl = "$Material3SourceUrl/FloatingActionButton.kt",
        examples = FloatingActionButtonsExamples,
    )

private val FloatingActionButtonMenu =
    Component(
        id = nextId(),
        name = "FAB Menu",
        description = "The FAB Menu displays additional key actions on click of a FAB.",
        // No FAB Menu icon
        guidelinesUrl = "$ComponentGuidelinesUrl/fab-menu",
        docsUrl = "$PackageSummaryUrl#floatingactionbuttonmenu",
        sourceUrl = "$Material3SourceUrl/FloatingActionButtonMenu.kt",
        examples = FloatingActionButtonMenuExamples,
    )

private val FloatingAppBars =
    Component(
        id = nextId(),
        name = "Floating App Bar",
        description = "A floating app bar displays key actions above the content.",
        // No floating app bar icon
        guidelinesUrl = "$ComponentGuidelinesUrl/floating-app-bars",
        docsUrl = "$DocsUrl#floatingappbar",
        sourceUrl = "$Material3SourceUrl/FloatingAppBar.kt",
        examples = FloatingAppBarsExamples
    )

private val IconButtons =
    Component(
        id = nextId(),
        name = "Icon buttons",
        description =
            "Icon buttons allow users to take actions and make choices with a single tap.",
        // No icon-button icon
        guidelinesUrl = "$ComponentGuidelinesUrl/icon-button",
        docsUrl = "$PackageSummaryUrl#iconbutton",
        sourceUrl = "$Material3SourceUrl/IconButton.kt",
        examples = IconButtonExamples,
    )

private val Lists =
    Component(
        id = nextId(),
        name = "Lists",
        description = "Lists are continuous, vertical indexes of text or images.",
        // No ListItem icon
        tintIcon = true,
        guidelinesUrl = "$ComponentGuidelinesUrl/list-item",
        docsUrl = "$PackageSummaryUrl#listitem",
        sourceUrl = "$Material3SourceUrl/ListItem.kt",
        examples = ListsExamples,
    )

private val LoadingIndicators =
    Component(
        id = nextId(),
        name = "Loading indicators",
        description =
            "Loading indicators express an unspecified wait time or display the length of " +
                "a loading process.",
        // No loading indicator icon
        guidelinesUrl = "$ComponentGuidelinesUrl/loading-indicators",
        tintIcon = true,
        docsUrl = "$PackageSummaryUrl#loadingindicator",
        sourceUrl = "$Material3SourceUrl/LoadingIndicator.kt",
        examples = LoadingIndicatorsExamples,
    )

private val Menus =
    Component(
        id = nextId(),
        name = "Menus",
        description = "Menus display a list of choices on temporary surfaces.",
        // No menu icon
        guidelinesUrl = "$ComponentGuidelinesUrl/menus",
        docsUrl = "$PackageSummaryUrl#dropdownmenu",
        sourceUrl = "$Material3SourceUrl/Menu.kt",
        examples = MenusExamples
    )

private val NavigationBar =
    Component(
        id = nextId(),
        name = "Navigation bar",
        description =
            "Navigation bars offer a persistent and convenient way to switch between " +
                "primary destinations in an app.",
        // No navigation bar icon
        guidelinesUrl = "$ComponentGuidelinesUrl/navigation-bar",
        docsUrl = "$PackageSummaryUrl#navigationbar",
        sourceUrl = "$Material3SourceUrl/NavigationBar.kt",
        examples = NavigationBarExamples
    )

private val NavigationDrawer =
    Component(
        id = nextId(),
        name = "Navigation drawer",
        description = "Navigation drawers provide ergonomic access to destinations in an app.",
        // No navigation drawer icon
        guidelinesUrl = "$ComponentGuidelinesUrl/navigation-drawer",
        docsUrl = "$PackageSummaryUrl#navigationdrawer",
        sourceUrl = "$Material3SourceUrl/NavigationDrawer.kt",
        examples = NavigationDrawerExamples
    )

private val NavigationRail =
    Component(
        id = nextId(),
        name = "Navigation rail",
        description =
            "Navigation rails provide access to primary destinations in apps when using " +
                "tablet and desktop screens.",
        // No navigation rail icon
        guidelinesUrl = "$ComponentGuidelinesUrl/navigation-rail",
        docsUrl = "$PackageSummaryUrl#navigationrail",
        sourceUrl = "$Material3SourceUrl/NavigationRail.kt",
        examples = NavigationRailExamples
    )

private val NavigationSuiteScaffold =
    Component(
        id = nextId(),
        name = "Navigation Suite Scaffold",
        description =
            "The Navigation Suite Scaffold wraps the provided content and places the " +
                "adequate provided navigation component on the screen according to the current " +
                "NavigationSuiteType. \n\n" +
                "Note: this sample is better experienced in a resizable emulator or foldable device.",
        // No navigation suite scaffold icon
        guidelinesUrl = "", // TODO: Add guidelines url when available
        docsUrl = "", // TODO: Add docs url when available
        sourceUrl = "$AdaptiveNavigationSuiteMaterial3SourceUrl/NavigationSuiteScaffold.kt",
        examples = NavigationSuiteScaffoldExamples
    )

private val ProgressIndicators =
    Component(
        id = nextId(),
        name = "Progress indicators",
        description =
            "Progress indicators express an unspecified wait time or display the length of " +
                "a process.",
        // No progress indicator icon
        guidelinesUrl = "$ComponentGuidelinesUrl/progress-indicators",
        docsUrl = "$DocsUrl#circularprogressindicator",
        sourceUrl = "$Material3SourceUrl/ProgressIndicator.kt",
        examples = ProgressIndicatorsExamples
    )

private val PullToRefreshIndicators =
    Component(
        id = nextId(),
        name = "Pull-to-Refresh Indicator",
        description =
            "Pull to refresh is a swipe gesture available at the beginning of lists, grid " +
                "lists, and card collections where the most recent content appears ",
        // No pull-to-refresh icon
        // TODO: Request component guidelines documentation from design.
        guidelinesUrl = "",
        docsUrl = "$DocsUrl#pulltorefreshcontainer",
        sourceUrl = "$Material3SourceUrl/PullToRefresh.kt",
        examples = PullToRefreshExamples
    )

private val RadioButtons =
    Component(
        id = nextId(),
        name = "Radio buttons",
        description = "Radio buttons allow the user to select one option from a set.",
        // No radio-button icon
        guidelinesUrl = "$ComponentGuidelinesUrl/radio-buttons",
        docsUrl = "$DocsUrl#radiobutton",
        sourceUrl = "$Material3SourceUrl/RadioButton.kt",
        examples = RadioButtonsExamples
    )

private val SearchBars =
    Component(
        id = nextId(),
        name = "Search bars",
        description =
            "Search bars allow users to enter a keyword or phrase and get relevant " +
                "information.",
        // No search bar icon
        guidelinesUrl = "", // No guidelines yet
        docsUrl = "", // No docs yet
        sourceUrl = "$Material3SourceUrl/SearchBar.kt",
        examples = SearchBarExamples
    )

private val SegmentedButtons =
    Component(
        id = nextId(),
        name = "Segmented Button",
        description =
            "Segmented buttons help people select options, switch views, or sort elements.",
        guidelinesUrl = "", // No guidelines yet
        docsUrl = "", // No docs yet
        sourceUrl = "$Material3SourceUrl/SegmentedButton.kt",
        examples = SegmentedButtonExamples
    )

private val ToggleButtons =
    Component(
        id = nextId(),
        name = "ToggleButtons",
        description = "Toggle buttons provide a selectable button that animates on press.",
        guidelinesUrl = "", // No guidelines yet
        docsUrl = "", // No docs yet
        sourceUrl = "$Material3SourceUrl/ToggleButton.kt",
        examples = ToggleButtonsExamples
    )

private val Sliders =
    Component(
        id = nextId(),
        name = "Sliders",
        description = "Sliders allow users to make selections from a range of values.",
        // No slider icon
        guidelinesUrl = "", // No guidelines yet
        docsUrl = "", // No docs yet
        sourceUrl = "$Material3SourceUrl/Slider.kt",
        examples = SlidersExamples
    )

private val Snackbars =
    Component(
        id = nextId(),
        name = "Snackbars",
        description =
            "Snackbars provide brief messages about app processes at the bottom of the " +
                "screen.",
        // No snackbar icon
        guidelinesUrl = "$ComponentGuidelinesUrl/snackbars",
        docsUrl = "$DocsUrl#snackbar",
        sourceUrl = "$Material3SourceUrl/Snackbar.kt",
        examples = SnackbarsExamples
    )

private val SplitButtons =
    Component(
        id = nextId(),
        name = "Split Button",
        description = "Split buttons let user perform additional actions besides the main action",
        guidelinesUrl = "", // No guidelines yet
        docsUrl = "", // No docs yet
        sourceUrl = "$Material3SourceUrl/SplitButton.kt",
        examples = SplitButtonExamples
    )

private val Switches =
    Component(
        id = nextId(),
        name = "Switches",
        description = "Switches toggle the state of a single setting on or off.",
        // No switch icon
        // No guidelines yet
        guidelinesUrl = "",
        docsUrl = "",
        sourceUrl = "$Material3SourceUrl/Switch.kt",
        examples = SwitchExamples
    )

private val Tabs =
    Component(
        id = nextId(),
        name = "Tabs",
        description =
            "Tabs organize content across different screens, data sets, and other " +
                "interactions.",
        // No tabs icon
        guidelinesUrl = "$ComponentGuidelinesUrl/tabs",
        docsUrl = "$DocsUrl#tab",
        sourceUrl = "$Material3SourceUrl/Tab.kt",
        examples = TabsExamples
    )

private val TextFields =
    Component(
        id = nextId(),
        name = "Text fields",
        description = "Text fields let users enter and edit text.",
        // No text fields icon
        guidelinesUrl = "$ComponentGuidelinesUrl/text-fields",
        docsUrl = "$DocsUrl#textfield",
        sourceUrl = "$Material3SourceUrl/TextField.kt",
        examples = TextFieldsExamples
    )

private val Tooltips =
    Component(
        id = nextId(),
        name = "Tooltips",
        description = "Tooltips call user attention to an anchor component.",
        // no tooltips icon
        guidelinesUrl = "$ComponentGuidelinesUrl/tooltips",
        docsUrl = "$PackageSummaryUrl#tooltip",
        sourceUrl = "$Material3SourceUrl/Tooltip.kt",
        examples = TooltipsExamples
    )

private val TimePickers =
    Component(
        id = nextId(),
        name = "Time Picker",
        description = "Time picker allows the user to choose time of day.",
        // No time picker icon
        guidelinesUrl = "$ComponentGuidelinesUrl/time-picker",
        docsUrl = "$DocsUrl#time-pickers",
        sourceUrl = "$Material3SourceUrl/TimePicker.kt",
        examples = TimePickerExamples
    )

private val TopAppBar =
    Component(
        id = nextId(),
        name = "Top app bar",
        description = "Top app bars display information and actions at the top of a screen.",
        // No top app bar icon
        guidelinesUrl = "$ComponentGuidelinesUrl/top-app-bar",
        docsUrl = "$PackageSummaryUrl#smalltopappbar",
        sourceUrl = "$Material3SourceUrl/AppBar.kt",
        examples = TopAppBarExamples
    )

/** Components for the catalog, ordered alphabetically by name. */
val Components =
    listOf(
        Adaptive,
        Badge,
        BottomAppBars,
        BottomSheets,
        Buttons,
        ButtonGroups,
        Card,
        Carousel,
        Checkboxes,
        Chips,
        DatePickers,
        Dialogs,
        ExtendedFloatingActionButton,
        FloatingActionButtons,
        FloatingActionButtonMenu,
        FloatingAppBars,
        IconButtons,
        Lists,
        LoadingIndicators,
        Menus,
        NavigationBar,
        NavigationDrawer,
        NavigationRail,
        NavigationSuiteScaffold,
        ProgressIndicators,
        PullToRefreshIndicators,
        RadioButtons,
        SearchBars,
        SegmentedButtons,
        Sliders,
        Snackbars,
        SplitButtons,
        Switches,
        Tabs,
        TextFields,
        TimePickers,
        ToggleButtons,
        Tooltips,
        TopAppBar
    )
