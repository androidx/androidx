# Module root

Wear Compose Material 3

# Package androidx.wear.compose.material3

Build Jetpack Compose UIs with Wear Material 3, the latest evolution of Material Design for wearables. Wear Material 3 includes updates to several areas, including theming, components, motion, and typography — all designed to help you make engaging and desirable experiences on the wrist.

This package provides the foundational UI components and visual theming specifications required to build applications that adhere to the Material 3 design system on Wear OS.

For more information, check out the [Wear OS Compose guides](https://developer.android.com/training/wearables/compose).

## Overview

### Theming

|      | **APIs** | **Description** |
| ---- | -------- | --------------- |
| **Material Theming** | [MaterialTheme] | The overall theme for Wear Material 3, providing colors, typography, shapes, and motion. |
| **Color scheme** | [ColorScheme] | Defines the colors used across the Wear Material 3 UI. |
|  | [dynamicColorScheme] | Creates a color scheme based on dynamic color extraction. |
| **Typography** | [Typography] | Defines the text styles used in Wear Material 3. |
| **Shape** | [Shapes] | Defines the shapes for Wear Material 3 components. |
| **Motion** | [MotionScheme] | Defines the motion characteristics for Wear Material 3 components. |

### Scaffolds

|               | **APIs**                  | **Description**                                                                                                      |
|---------------|---------------------------|----------------------------------------------------------------------------------------------------------------------|
| **Scaffolds** | [AppScaffold]             | Lays out the structure of an app and animates [TimeText] content.                                                    |
|               | [ScreenScaffold]          | Lays out the structure of a screen and animates [ScrollIndicator] and [EdgeButton] where there is scrolling content. |
|               | [HorizontalPagerScaffold] | Lays out the structure of a horizontal pager and animates [HorizontalPageIndicator].                                 |
|               | [VerticalPagerScaffold]   | Lays out the structure of a vertical pager and animates [VerticalPageIndicator].                                     |

### Components

|                         | **APIs**                                                 | **Description**                                                                                                                                                    |
|-------------------------|----------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Buttons**             | [Button]                                                 | Standard, stadium-shaped button for icon, label and secondary label content.                                                                                       |
|                         | [IconButton]                                             | Circular, icon-only button.                                                                                                                                        |
|                         | [TextButton]                                             | Circular, text-only button.                                                                                                                                        |
|                         | [EdgeButton]                                             | Signature button that follows the screen's curvature.                                                                                                              |
|                         | [ButtonGroup]                                            | Expressive, animated row of buttons.                                                                                                                               |
| **Cards**               | [Card]                                                   | Base level container Card with rounded corners and optional image background.                                                                                      |
|                         | [AppCard]                                                | A card with an opinionated 5-slot layout for title, time, name, icon and body content.                                                                             |
|                         | [TitleCard]                                              | A card with an opinionated 4-slot layout for title, time, subtitle and body content, with optional image background.                                               |
| **Dialogs**             | [Dialog]                                                 | Base full-screen, animated dialog.                                                                                                                                 |
|                         | [AlertDialog]                                            | Alert dialog for user input, with variations for confirm/dismiss or EdgeButton and optional additional content.                                                    |
|                         | [ConfirmationDialog]                                     | Transient confirmation with variations for short [curvedText][androidx.wear.compose.material3.curvedText] or longer body text.                                     |
|                         | [SuccessConfirmationDialog]                              | An animated confirmation dialog with a success icon.                                                                                                               |
|                         | [FailureConfirmationDialog]                              | An animated confirmation dialog with a failure icon.                                                                                                               |
|                         | [OpenOnPhoneDialog]                                      | Dialog showing animated "Open on phone" icon.                                                                                                                      |
| **Icons**               | [Icon]                                                   | Icon component built from ImageVector, ImageBitmap or Painter.                                                                                                     |
| **Pickers**             | [Picker]                                                 | Basic, scrollable picker for item selection.                                                                                                                       |
|                         | [PickerGroup]                                            | Builder for a screen of [Picker] components.                                                                                                                       |
|                         | [DatePicker]                                             | Full-screen date picker for day, month, year selection.                                                                                                            |
|                         | [TimePicker]                                             | Full-screen time picker for hours, minutes, seconds selection.                                                                                                     |
| **Progress indicators** | [CircularProgressIndicator]                              | Animated circular progress indicator for defined or indeterminate progress.                                                                                        |
|                         | [LinearProgressIndicator]                                | Horizontal progress indicator.                                                                                                                                     |
|                         | [ArcProgressIndicator]                                   | Indeterminate arc progress indicator.                                                                                                                              |
|                         | [SegmentedCircularProgressIndicator]                     | Segmented variation of the [CircularProgressIndicator].                                                                                                            |
| **Selectable buttons**  | [RadioButton]                                            | Stadium-shaped toggle button for icon, label and secondary label content with a radio button.                                                                      |
|                         | [SplitRadioButton]                                       | Split variation of the [RadioButton].                                                                                                                              |
| **Sliders & Steppers**  | [Slider]                                                 | Stadium-shaped selector for a range of values.                                                                                                                     |
|                         | [Stepper]                                                | Full-screen component for selecting from a range of values.                                                                                                        |
| **Status indicators**   | [LevelIndicator]                                         | Visual indicator for a setting level such as volume.                                                                                                               |
|                         | [ScrollIndicator]                                        | Visual indicator for scroll position within a scrollable container such as [TransformingLazyColumn][androidx.wear.compose.foundation.lazy.TransformingLazyColumn]. |
|                         | [HorizontalPageIndicator]                                | Indicates the currently active page and approximate number of pages for a [HorizontalPager][androidx.wear.compose.foundation.pager.HorizontalPager].               |
|                         | [VerticalPageIndicator]                                  | Indicates the currently active page and approximate number of pages for a [VerticalPager][androidx.wear.compose.foundation.pager.VerticalPager].                   |
| **Swipe behavior**      | [SwipeToDismissBox]                                      | Swipe to dismiss handling for content within a [Box][androidx.compose.foundation.layout.Box].                                                                      |
|                         | [SwipeToReveal]                                          | Reveal up to two additional actions behind a UI component when swiped.                                                                                             |
| **Text**                | [curvedText][androidx.wear.compose.material3.curvedText] | Curved text within a [CurvedLayout][androidx.wear.compose.foundation.CurvedLayout].                                                                                |
|                         | [Text]                                                   | Linear Text                                                                                                                                                        |
|                         | [TimeText]                                               | Curved time text                                                                                                                                                   |
| **Toggle buttons**      | [CheckboxButton]                                         | Stadium-shaped toggle button for icon, label and secondary label content with a checkbox.                                                                          |
|                         | [SplitCheckboxButton]                                    | Split variation of the [CheckboxButton].                                                                                                                           |
|                         | [SwitchButton]                                           | Stadium-shaped toggle button for icon, label and secondary label content with a switch.                                                                            |
|                         | [SplitSwitchButton]                                      | Split variation of the [SwitchButton].                                                                                                                             |
|                         | [IconToggleButton]                                       | Circular, icon-only toggle button with shape-morphing variations.                                                                                                  |
|                         | [TextToggleButton]                                       | Circular, text-only toggle button with shape-morphing variations.                                                                                                  |

### One-handed gestures

|                      | **APIs**                                                                                                                            | **Description**                                                                                                                                                                                                          |
|----------------------|-------------------------------------------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Gesture Modifier** | [oneHandedGesture][androidx.wear.compose.material3.onehandedgesture.oneHandedGesture]                                               | Modifier to enable one-handed gesture support on a UI element.                                                                                                                                                           |
| **Config**           | [rememberOneHandedGestureConfiguration][androidx.wear.compose.material3.onehandedgesture.rememberOneHandedGestureConfiguration]     | Creates and remembers a configuration for one-handed gestures.                                                                                                                                                           |
| **Indicators**       | [OneHandedGestureClickIndicator][androidx.wear.compose.material3.onehandedgesture.OneHandedGestureClickIndicator]                   | Indicator for click gesture availability, such as on Buttons and Cards. Transitions between the content and a gesture indicator.                                                                                         |
|                      | [OneHandedGestureScrollIndicator][androidx.wear.compose.material3.onehandedgesture.OneHandedGestureScrollIndicator]                 | Indicator for scroll gesture availability, displays a standard [ScrollIndicator][androidx.wear.compose.material3.ScrollIndicator] with temporary transitions to a gesture indicator animation.                           |
|                      | [OneHandedGestureHorizontalPageIndicator][androidx.wear.compose.material3.onehandedgesture.OneHandedGestureHorizontalPageIndicator] | Indicator for horizontal pager gesture availability, displays a standard [HorizontalPageIndicator][androidx.wear.compose.material3.HorizontalPageIndicator] with temporary transitions to a gesture indicator animation. |
|                      | [OneHandedGestureVerticalPageIndicator][androidx.wear.compose.material3.onehandedgesture.OneHandedGestureVerticalPageIndicator]     | Indicator for vertical pager gesture availability, displays a standard [VerticalPageIndicator][androidx.wear.compose.material3.VerticalPageIndicator] with temporary transitions to a gesture indicator animation.       |

# Package androidx.wear.compose.material3.onehandedgesture

This package offers APIs providing one-handed gesture support, consisting of gesture handlers for primary action (e.g. double pinch) and dismiss action (e.g. wrist turn).
A gesture configuration should be created for each component that supports a gesture, such as a button, card or scrollable container.
Animated indicators are provided that show when the gestures are available on clickable UI elements (like buttons and cards), scrollable  lists or pagers.

