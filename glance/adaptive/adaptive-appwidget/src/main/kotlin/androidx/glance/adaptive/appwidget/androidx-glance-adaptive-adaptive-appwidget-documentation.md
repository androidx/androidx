# Module root: androidx.glance.adaptive adaptive-appwidget

# Package androidx.glance.adaptive.appwidget

Provides AppWidget compilation and receiver implementations for Glance Adaptive widgets.

# Package androidx.glance.adaptive.appwidget.renderer

Provides AppWidget template registry and Compose UI orchestration.

# Package androidx.glance.adaptive.appwidget.selection

Provides AppWidget-specific surfaces, detectors, and size tiers.

# Package androidx.glance.adaptive.appwidget.components

Provides template-agnostic building blocks shared across templates.

A composable belongs here only once a second template needs it. Anything used by a single template
lives in that template's package instead, and is promoted here in a change that introduces the
second caller.

# Package androidx.glance.adaptive.appwidget.templates.track

Provides the layout plan, size selector, Glance renderer, and blocks for the Track template.


