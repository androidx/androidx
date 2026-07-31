# Module root

Wear Compose Foundation

# Package androidx.wear.compose.foundation

This package provides the lazy list containers
[ScalingLazyColumn][androidx.wear.compose.foundation.lazy.ScalingLazyColumn] (used with Material),
[TransformingLazyColumn][androidx.wear.compose.foundation.lazy.TransformingLazyColumn]
(used with Material 3), support for building curved content, pager components and handling for
rotary input from hardware components like a rotating side button (RSB) or a physical bezel on Wear
OS devices.

As a rule, the prefix `Basic` in Foundation indicates that this is a design-system agnostic component, and that a more tailored component is available in the Material or Material 3 library.

### Components

|                | **APIs**                                                                                        | **Description**                                                                                                                                                                                                                                                                         |
|----------------|-------------------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Lists          | [ScalingLazyColumn][androidx.wear.compose.foundation.lazy.ScalingLazyColumn]                    | Lazy list that provides the scaling/fisheye scrolling effect that forms a key part of the Material design language, and should be used with the Wear Compose Material library. Content items are only materialized and composed when needed.                                            |
|                | [TransformingLazyColumn][androidx.wear.compose.foundation.lazy.TransformingLazyColumn]          | Lazy list that provides both scaling and morphing behavior for scrolling lists, forming signature functionality for the Material 3 design language, for use with Wear Compose Material 3. Similarly, content items are only materialized and composed when needed.                      |
| Curved content | [CurvedLayout]                                                                                  | Layout composable that denotes curved content, typically built from foundational components such as [curvedBox][androidx.wear.compose.foundation.curvedBox], [curvedRow][androidx.wear.compose.foundation.curvedRow] and [curvedColumn][androidx.wear.compose.foundation.curvedColumn]. |
|                | [curvedBox][androidx.wear.compose.foundation.curvedBox]                                         | Places its children on top of each other and on an arc, similar to a [Box][androidx.compose.foundation.layout.Box] layout, but curved into a segment of an annulus.                                                                                                                     |
|                | [curvedRow][androidx.wear.compose.foundation.curvedRow]                                         | Places its children in an arc, rotated as needed, similar to a [Row][androidx.compose.foundation.layout.Row] layout, but curved into a segment of an annulus.                                                                                                                           |
|                | [curvedColumn][androidx.wear.compose.foundation.curvedColumn]                                   | Places its children stacked as part of an arc, similar to a [Column][androidx.compose.foundation.layout.Column] layout, but curved into a segment of an annulus.                                                                                                                        |
|                | [curvedComposable][androidx.wear.compose.foundation.curvedComposable]                           | Allows non-curved composables to be part of a [CurvedLayout].                                                                                                                                                                                                                           |
|                | [basicCurvedText][androidx.wear.compose.foundation.basicCurvedText]                             | Writes curved text following the curvature of a circle (usually at the edge of a circular screen).                                                                                                                                                                                      |
| Pagers         | [HorizontalPager][androidx.wear.compose.foundation.pager.HorizontalPager]                       | Horizontally scrolling Pager, optimized for Wear OS devices. Used with Material3 [HorizontalPagerScaffold][androidx.wear.compose.material3.HorizontalPagerScaffold] and [AnimatedPage][androidx.wear.compose.material3.AnimatedPage]                                                    |
|                | [VerticalPager][androidx.wear.compose.foundation.pager.VerticalPager]                           | Vertically scrolling Pager, optimized for Wear OS devices. Used with Material3 [VerticalPagerScaffold][androidx.wear.compose.material3.VerticalPagerScaffold] and [AnimatedPage][androidx.wear.compose.material3.AnimatedPage]                                                          |
| Focus          | [hierarchicalFocusGroup][androidx.wear.compose.foundation.hierarchicalFocusGroup]               | Annotates composables to track the active part of the composition and coordinate focus declaratively, requesting focus when needed.                                                                                                                                                     |
|                | [requestFocusOnHierarchyActive][androidx.wear.compose.foundation.requestFocusOnHierarchyActive] | Used with [hierarchicalFocusGroup][androidx.wear.compose.foundation.hierarchicalFocusGroup] to request focus on the following focusable element when needed.                                                                                                                            |
|                | [LocalScreenIsActive]                                                                           | CompositionLocal used to determine when a screen is active, as specified by each component (such as whilst paging or swiping to dismiss) .                                                                                                                                              |
| Rotary         | [rotaryScrollable][androidx.wear.compose.foundation.rotary.rotaryScrollable]                    | Modifier connecting rotary events from the rotating side button or physical bezel on Wear OS devices to scrollable containers, such as [TransformingLazyColumn][androidx.wear.compose.foundation.lazy.TransformingLazyColumn] (which supports rotary input by default).                 |
| Ambient        | [LocalAmbientModeManager]                                                                       | CompositionLocal used to obtain the current [AmbientModeManager] to track the ambient (low-power) mode state.                                                                                                                                                                           |
| Accessibility  | [LocalReduceMotion]                                                                             | CompositionLocal used to track the reduce-motion setting.                                                                                                                                                                                                                               |
| Swiping        | [BasicSwipeToDismissBox]                                                                        | Basic handling for the swipe-to-dismiss gesture, used as a building block for [SwipeToDismissBox][androidx.wear.compose.material.SwipeToDismissBox] in Material and [SwipeToDismissBox][androidx.wear.compose.material3.SwipeToDismissBox] in Material3.                                |
| Expanding      | [rememberExpandableState] and [ExpandableState]                                                 | Building blocks for expandable content in Material with [ScalingLazyColumn][androidx.wear.compose.foundation.lazy.ScalingLazyColumn]                                                                                                                                                    |

For more information, check out the [Wear OS Compose guides](https://developer.android.com/training/wearables/compose).

# Package androidx.wear.compose.foundation.lazy

This package is for components built on top of Compose Foundation [LazyColumn][androidx.compose.foundation.lazy.LazyColumn] ([ScalingLazyColumn]) or [LazyLayout][androidx.compose.foundation.lazy.layout.LazyLayout] ([TransformingLazyColumn]).

[ScalingLazyColumn] provides the scaling/fisheye scrolling list that forms a key part of the Material design language, and should be used with the Wear Compose Material library. Content items are only materialized and composed when needed.

[TransformingLazyColumn] provides both scaling and morphing behavior for scrolling lists, forming signature functionality for the Material 3 design language, for use with Wear Compose Material 3. Similarly, content items are only materialized and composed when needed.

# Package androidx.wear.compose.foundation.pager

This package provides components for creating multi-page layouts where users can navigate between pages
using swipe gestures. Key components include [HorizontalPager] and [VerticalPager], and the
[rememberPagerState] function for managing their state.

These components are recommended for use with Wear Compose Material 3, which also provides [HorizontalPagerScaffold][androidx.wear.compose.material3.HorizontalPagerScaffold], [VerticalPagerScaffold][androidx.wear.compose.material3.VerticalPagerScaffold], [HorizontalPageIndicator][androidx.wear.compose.material3.HorizontalPageIndicator], [VerticalPageIndicator][androidx.wear.compose.material3.VerticalPageIndicator] and [AnimatedPage][androidx.wear.compose.material3.AnimatedPage] components to achieve the Material 3 design system.

# Package androidx.wear.compose.foundation.rotary

This package offers APIs for handling rotary input from hardware components like a rotating side button
(RSB) or a physical bezel on Wear OS devices. The primary component is the [rotaryScrollable] modifier,
which allows you to connect rotary input to scrollable composables.

Scrolling lists such as [ScalingLazyColumn][androidx.wear.compose.foundation.lazy.ScalingLazyColumn] and [TransformingLazyColumn][androidx.wear.compose.foundation.lazy.TransformingLazyColumn] support rotary input by default, as does the [VerticalPager][androidx.wear.compose.foundation.pager.VerticalPager]. The [rotaryScrollable] modifier is provided so that custom components can be extended for rotary support too.
