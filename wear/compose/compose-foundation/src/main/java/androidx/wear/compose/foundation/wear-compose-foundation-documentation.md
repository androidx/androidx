# Module root

Wear Compose Foundation

# Package androidx.wear.compose.foundation

This package provides the lazy list containers [ScalingLazyColumn] (used with Material) and [TransformingLazyColumn] (used with Material 3), and support for building curved content. As a rule, the prefix `Basic` in Foundation indicates that this is a design-system agnostic component, and that a more tailored component is available in the Material or Material 3 library.

For example:
- Curved layout APIs such as [CurvedLayout], [BasicCurvedText], [CurvedRow], and [CurvedColumn].
- Swipe building blocks such as [BasicSwipeToDismissBox]. See also [androidx.wear.compose.material.SwipeToReveal] for use with Material only (Material 3 has a dedicated [androidx.wear.compose.material3.SwipeToReveal] component).
- Expandable APIs for use as building blocks with Material Design, such as [rememberExpandableState] and [ExpandableState].
- Focus APIs such as [HierarchicalFocusCoordinator] and [LocalScreenIsActive].
- Ambient control using [LocalAmbientModeManager].
- Accessibility APIs such as [LocalReduceMotion].

For more information, check out the [Wear OS Compose guides](https://developer.android.com/training/wearables/compose).

# Package androidx.wear.compose.foundation.lazy

This package is for components built on top of Compose Foundation [LazyColumn] ([ScalingLazyColumn]) or [LazyLayout] ([TransformingLazyColumn]).

[ScalingLazyColumn] provides the scaling/fisheye scrolling list that forms a key part of the Material design language, and should be used with the Wear Compose Material library. Content items are only materialized and composed when needed.

[TransformingLazyColumn] provides both scaling and morphing behavior for scrolling lists, forming signature functionality for the Material 3 design language, for use with Wear Compose Material 3. Similarly, content items are only materialized and composed when needed.

# Package androidx.wear.compose.foundation.pager

This package provides components for creating multi-page layouts where users can navigate between pages
using swipe gestures. Key components include [HorizontalPager] and [VerticalPager], and the
[rememberPagerState] function for managing their state.

These components are recommended for use with Wear Compose Material 3, which also provides [PagerScaffold], [HorizontalPageIndicator], [VerticalPageIndicator] and [AnimatedPage] components to achieve the Material 3 design system.

# Package androidx.wear.compose.foundation.rotary

This package offers APIs for handling rotary input from hardware components like a rotating side button
(RSB) or a physical bezel on Wear OS devices. The primary component is the [rotaryScrollable] modifier,
which allows you to connect rotary input to scrollable composables.

Scrolling lists such as [ScalingLazyColumn] and [TransformingLazyColumn] support rotary input by default, as does the [VerticalPager]. The [rotaryScrollable] modifier is provided so that custom components can be extended for rotary support too.
