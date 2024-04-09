# Module root

Compose Material 3

# Package androidx.compose.material3

Build Jetpack Compose UIs with <a href="https://m3.material.io" class="external" target="_blank">Material Design 3</a>, the next evolution of Material Design. Material 3 includes updated theming and components and Material You personalization features like dynamic color, and is designed to be cohesive with the new Android 12 visual style and system UI.

![Material You image](https://developer.android.com/images/reference/androidx/compose/material3/material-you.png)

In this page, you'll find documentation for types, properties, and functions available in the `androidx.compose.material3` package.

For more information, check out <a href="https://developer.android.com/jetpack/compose/designsystems/material3" class="external" target="_blank">Material Design 3 in Compose</a>.

## Overview

### Theming

|      | **APIs** | **Description** |
| ---- | -------- | --------------- |
| **Material Theming** | [MaterialTheme] | M3 theme |
| **Color scheme** | [ColorScheme] | M3 color scheme |
|  | [lightColorScheme] | M3 light color scheme |
|  | [darkColorScheme] | M3 dark color scheme |
| **Dynamic color** | [dynamicLightColorScheme] | M3 dynamic light color scheme |
|  | [dynamicDarkColorScheme] | M3 dynamic dark color scheme |
| **Typography** | [Typography] | M3 typography |
| **Shape** | [Shapes] | M3 shape |

### Components

|                         | **APIs**                       | **Description**                           |
|-------------------------|--------------------------------|-------------------------------------------|
| **Badge**               | [Badge]                        | M3 badge                                  |
|                         | [BadgedBox]                    | M3 badged box                             |
| **Bottom app bar**      | [BottomAppBar]                 | M3 bottom app bar                         |
| **Bottom sheet**        | [BottomSheetScaffold]          | M3 bottom sheet                           |
|                         | [ModalBottomSheet]             | M3 modal bottom sheet                     |
| **Buttons**             | [Button]                       | M3 filled button                          |
|                         | [ElevatedButton]               | M3 elevated button                        |
|                         | [FilledTonalButton]            | M3 filled tonal button                    |
|                         | [OutlinedButton]               | M3 outlined button                        |
|                         | [TextButton]                   | M3 text button                            |
| **Cards**               | [Card]                         | M3 filled card                            |
|                         | [ElevatedCard]                 | M3 elevated card                          |
|                         | [OutlinedCard]                 | M3 outlined card                          |
| **Checkbox**            | [Checkbox]                     | M3 checkbox                               |
|                         | [TriStateCheckbox]             | M3 indeterminate checkbox                 |
| **Chips**               | [AssistChip]                   | M3 assist chip                            |
|                         | [ElevatedAssistChip]           | M3 elevated assist chip                   |
|                         | [FilterChip]                   | M3 filter chip                            |
|                         | [ElevatedFilterChip]           | M3 elevated filter chip                   |
|                         | [InputChip]                    | M3 input chip                             |
|                         | [SuggestionChip]               | M3 suggestion chip                        |
|                         | [ElevatedSuggestionChip]       | M3 elevated suggestion chip               |
| **Date Picker**         | [DatePicker]                   | M3 date picker                            |
|                         | [DatePickerDialog]             | M3 date picker embeeded in dialog         |
|                         | [DateRangePicker]              | M3 date range picker                      |
| **Dialogs**             | [AlertDialog]                  | M3 basic dialog                           |
| **Dividers**            | [HorizontalDivider]            | M3 horizontal divider                     |
|                         | [VerticalDivider]              | M3 vertical divider                       |
| **Extended FAB**        | [ExtendedFloatingActionButton] | M3 extended FAB                           |
| **FAB**                 | [FloatingActionButton]         | M3 FAB                                    |
|                         | [SmallFloatingActionButton]    | M3 small FAB                              |
|                         | [LargeFloatingActionButton]    | M3 large FAB                              |
| **Icon button**         | [IconButton]                   | M3 standard icon button                   |
|                         | [IconToggleButton]             | M3 standard icon toggle button            |
|                         | [FilledIconButton]             | M3 filled icon button                     |
|                         | [FilledIconToggleButton]       | M3 filled icon toggle button              |
|                         | [FilledTonalIconButton]        | M3 filled tonal icon button               |
|                         | [FilledTonalIconToggleButton]  | M3 filled tonal icon toggle button        |
|                         | [OutlinedIconButton]           | M3 outlined icon button                   |
|                         | [OutlinedIconToggleButton]     | M3 outlined icon toggle button            |
| **Lists**               | [ListItem]                     | M3 list item                              |
| **Menus**               | [DropdownMenu]                 | M3 menu                                   |
|                         | [DropdownMenuItem]             | M3 menu item                              |
|                         | [ExposedDropdownMenuBox]       | M3 exposed dropdown menu                  |
| **Navigation bar**      | [NavigationBar]                | M3 navigation bar                         |
|                         | [NavigationBarItem]            | M3 navigation bar item                    |
| **Navigation drawer**   | [ModalNavigationDrawer]        | M3 modal navigation drawer                |
|                         | [ModalDrawerSheet]             | M3 modal drawer sheet                     |
|                         | [PermanentNavigationDrawer]    | M3 permanent standard navigation drawer   |
|                         | [PermanentDrawerSheet]         | M3 permanent standard drawer sheet        |
|                         | [DismissibleNavigationDrawer]  | M3 dismissible standard navigation drawer |
|                         | [DismissibleDrawerSheet]       | M3 dismissible standard drawer sheet      |
|                         | [NavigationDrawerItem]         | M3 navigation drawer item                 |
| **Navigation rail**     | [NavigationRail]               | M3 navigation rail                        |
|                         | [NavigationRailItem]           | M3 navigation rail item                   |
| **Progress indicators** | [LinearProgressIndicator]      | M3 linear progress indicator              |
|                         | [CircularProgressIndicator]    | M3 circular progress indicator            |
| **Radio button**        | [RadioButton]                  | M3 radio button                           |
| **Search Bar**          | [SearchBar]                    | M3 search bar                             |
|                         | [DockedSearchBar]              | M3 docked search bar                      |
| **Segmented Button**    | [SegmentedButton]              | M3 segmented button                       |
|                         | [SingleChoiceSegmentedButtonRow] | M3 single choice segmented button row |
|                         | [MultiChoiceSegmentedButtonRow] | M3 multiple choice segmented button row |
| **Sliders**             | [Slider]                       | M3 slider                                 |
|                         | [RangeSlider]                  | M3 range slider                           |
| **Snackbars**           | [Snackbar]                     | M3 snackbar                               |
| **Swipe to Dismiss**    | [SwipeToDismiss]               | M3 swipe to dismiss                       |
| **Switch**              | [Switch]                       | M3 switch                                 |
| **Tabs**                | [Tab]                          | M3 tab                                    |
|                         | [LeadingIconTab]               | M3 leading icon tab                       |
|                         | [PrimaryIndicator]             | M3 primary tab indicator                  |
|                         | [PrimaryTabRow]                | M3 primary tab row                        |
|                         | [SecondaryIndicator]           | M3 secondary tab indicator                |
|                         | [SecondaryTabRow]              | M3 secondary tab row                      |
|                         | [TabRow]                       | M3 fixed tab row                          |
|                         | [ScrollableTabRow]             | M3 scrollable tab row                     |
| **Text fields**         | [TextField]                    | M3 filled text field                      |
|                         | [OutlinedTextField]            | M3 outlined text field                    |
| **Time Picker**         | [TimePicker]                   | M3 time picker                            |
|                         | [TimeInput]                    | M3 time input                             |
| **Tool tip**            | [PlainTooltipBox]              | M3 plain tool tip                         |
|                         | [RichTooltipBox]               | M3 rich tool tip                          |
| **Top app bar**         | [TopAppBar]                    | M3 small top app bar                      |
|                         | [CenterAlignedTopAppBar]       | M3 center-aligned top app bar             |
|                         | [MediumTopAppBar]              | M3 medium top app bar                     |
|                         | [LargeTopAppBar]               | M3 large top app bar                      |

### Surfaces and layout

|      | **APIs** | **Description** |
| ---- | -------- | --------------- |
| **Surfaces** | [Surface] | M3 surface |
| **Scaffold** | [Scaffold] | M3 layout |

### Icons and text

|      | **APIs** | **Description** |
| ---- | -------- | --------------- |
| **Icon** | [Icon] | M3 icon |
| **Text** | [Text] | M3 text |

Also check out the <a href="https://developer.android.com/reference/kotlin/androidx/compose/material/icons/package-summary" class="external" target="_blank">androidx.compose.material.icons package</a>.
