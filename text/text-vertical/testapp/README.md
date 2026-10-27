# Vertical Text Demo App

This module (`:text:text-vertical-testapp`) shows the demos for `:text:text-vertical` and
`:text:text-vertical-compose`. It follows the layout of the Compose demo app: `Demos.kt` lists
each demo in a `DemoCategory` tree, and `DemoApp` shows a category as a list and a demo as full
screen content.

## Build and install

```bash
PROJECT_PREFIX=:text,:annotation ./gradlew :text:text-vertical-testapp:installDebug
adb shell am start -n androidx.text.vertical.testapp/.DemoActivity
```

## Open one demo directly

`DemoActivity` reads the string extra `demo_name` and opens that demo with its parent categories
on the back stack. The name is a slash path, a breadcrumb path, a title that is unique in the
tree, or a substring that matches one title:

```bash
adb shell am start -n androidx.text.vertical.testapp/.DemoActivity --es demo_name "HorizontalText/CJK font"
adb shell am start -n androidx.text.vertical.testapp/.DemoActivity --es demo_name "HorizontalText > CJK font"
adb shell am start -n androidx.text.vertical.testapp/.DemoActivity --es demo_name "Subtitle cue"
adb shell am start -n androidx.text.vertical.testapp/.DemoActivity --es demo_name "CJK"
```

Titles such as `Long text` repeat under `VerticalText` and `HorizontalText`, so they need a path.
An unknown or ambiguous name stops the activity with an `IllegalArgumentException`.

## Add a demo

1. Write a `@Composable fun MyDemo()` in `TextDemos.kt`. Wrap it in `StyleColorsToggle` if it
   should offer the style colors button.
2. Add `ComposableDemo("My demo") { MyDemo() }` to a category in `Demos.kt`. Keep sibling titles
   unique.

## Test

```bash
PROJECT_PREFIX=:text,:annotation ./gradlew :text:text-vertical-testapp:connectedCheck
```
