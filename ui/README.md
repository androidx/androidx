# Jetpack Compose
## Intro
Jetpack Compose is a suite of libraries within the AndroidX ecosystem. For more information, see our [project page](https://developer.android.com/jetpackcompose)

## Syntax
Jetpack Compose uses composable functions instead of XML layouts to define UI components. You can see this in action in the demos, like `androidx.ui.material.demos.ButtonDemo.kt`. More information can be found in the [compiler README](https://android.googlesource.com/platform/frameworks/support/+/androidx-master-dev/compose/README.md).

You may notice some parts of the codebase use an XML-like syntax. This was an exploration done early on in the project, and we have since decided to move away from it. Usages of this syntax will eventually be converted to use the standard kotlin DSL instead.

## Compiler
Composable functions are built using a custom Kotlin compiler plugin. More information about the compiler plugin is available in [this README](https://android.googlesource.com/platform/frameworks/support/+/androidx-master-dev/compose/README.md).

## Getting started
To try out Jetpack Compose you need to set up the toolchain for AndroidX development. Follow the process [here](https://android.googlesource.com/platform/frameworks/support/+/androidx-master-dev/README.md) to check out the code.

To start the required version of Android Studio, you need to run the studiow command from the `/ui` subfolder

    cd path/to/checkout/frameworks/support/ui/
    ./studiow

Also if you would like to build from the command line, all gradle commands need to be run from the `/ui` subfolder.  E.g. to build the demo app, run:

    cd path/to/checkout/frameworks/support/ui/
    ./gradlew ui:integration-tests:demos:installDebug

## Currently available components
Jetpack Compose is in very early stages of development. Developers wanting to build sample apps will probably want to include the material, layout and framework modules. You can see how to setup your dependencies in `material/integration-tests/material-studies/build.gradle`.

Run the `demos` app to see examples of individual components.

A sample implementation of the [Material Rally app](https://material.io/design/material-studies/rally.html) is under `material/integration-tests/material-studies`.

To build the Material Rally app via the command line run:

    cd path/to/checkout/frameworks/support/ui/
    ./gradlew :ui-material:integration-tests:ui-material-studies:assembleDebug


## Structure
Library code for Jetpack Compose lives under the `frameworks/support/ui` directory. Additionally, sample code can be found within each module in the `integration-tests` subdirectories and the compiler and runtime code can be found in `frameworks/support/compose`.

The modules within UI are structured as follows:
* `ui-android-text/`

   Android specific text stack dependent implementations
* `ui-android-view/`

   Wrappers and adapters for existing Android Views
* `ui-animation/`

   Animation components
* `ui-animation-core/`

   Internal declarations for the animations system
* `ui-core/`

   Base classes used across the system covering primitives, graphics and drawing
* `integration-tests/demos/`

   Module that collects all demos across ui and packages them into one demo APK
* `ui-framework/`

   Base components exposed by the system as building blocks. This includes Draw, Layout, Text, etc.
* `ui-layout/`

   Basic layout components
* `ui-material/`

   Set of UI components built according to the Material spec
* `ui-platform/`

   Internal implementation that allows separation of android implementation from host-side tests
* `ui-test/`

   Testing framework
* `ui-text/`

   Text engine

## Feedback
To provide feedback or report bugs, please refer to the main [AndroidX contribution guide](https://android.googlesource.com/platform/frameworks/support/+/androidx-master-dev/README.md) and report your bugs [here](https://issuetracker.google.com/issues/new?component=612128)
