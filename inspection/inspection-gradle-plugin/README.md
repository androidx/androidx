# Inspection Gradle Plugin

The `inspection-gradle-plugin` is a Gradle plugin that packages special-purpose inspection libraries
(`inspector.jar`) inside of Android libraries (`.aar` files). These inspector JARs are used by
Android Studio to provide live, detailed insights into how a library is behaving inside a running
application (e.g., inspecting WorkManager's jobs or Compose's layout).

## Overview

The plugin automates the process of creating a self-contained inspector and bundling it into a
target library's release artifact. This involves two main projects:

* **Inspector Project:** An Android library module containing the inspection logic (e.g.,
  `:work:work-inspection` or `:compose:ui:ui-inspection`).
* **Target Library Project:** The library to be inspected at runtime, which will host the
  inspector (e.g., `:work:work-runtime` or `:compose:ui:ui`).

## How it Works

The process can be broken down into two parts:

### Part 1: Building the `inspector.jar`

This happens within the **inspector project**.

1. **Dependency Shadowing (`ShadowDependenciesTask`):** The plugin finds all of the inspector
   project's transitive dependencies. It bundles their compiled code directly into a single "fat
   JAR" along with the inspector's own code. Crucially, it renames the package paths of these
   bundled dependencies to prevent version conflicts with the app that will eventually use the
   target library. *Note*, some dependencies are dropped in the process (e.g. kotlin-stdlib),
   because they are expected to exist at runtime.
2. **Dexing (`DexInspectorTask`):** The resulting "fat JAR" is converted from Java bytecode into the
   Android DEX format, creating the final `inspector.jar`.

### Part 2: Packaging the `inspector.jar` into the AAR

This happens within the **target library project**.

1. **Configuration:** The `packageInspector()` function is called in the target library's
   `build.gradle` to link it to the inspector project.
2. **AAR Modification (`AddInspectorJarToAarTask`):** When the target library's `release` variant is
   built, this task injects the `inspector.jar` (created in Part 1) into the `/libs` directory
   inside the final `.aar` file.
3. **Verification (`VerifyInspectorJarPresent`):** A final check ensures the `inspector.jar` was
   packaged correctly.

## Inputs and Outputs

* **Input:** An inspector project and a target library project.
* **Output:** A modified Android Archive (`.aar`) file for the target library. This AAR is standard,
  but contains the `inspector.jar` inside its `libs/` folder.

## Contents of `inspector.jar`

The `inspector.jar` is a self-contained, DEX-formatted file containing:

1. **The Inspector's Compiled Code:** The logic from the inspector module.
2. **Bundled and "Shadowed" Dependencies:** All of the inspector's dependencies are included
   directly in the JAR. Their package names are relocated (e.g., `org.jetbrains.kotlin` becomes
   `androidx.inspection.shadow.org.jetbrains.kotlin`) to guarantee that the inspector runs
   independently and does not create dependency conflicts within the host application.

This allows Android Studio to safely load and run the inspector to provide rich debugging features
for the target library.
