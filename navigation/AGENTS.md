# Project: Navigation

## General Instructions:

- When modifying any .kt file, format it via the following command:
  `./gradlew :ktCheckFile --format --file <file>`. If more than one file needs formatting, continue
  adding `--file <next-file>` to the command.
- When a public API is changed or when asked to update the public API files, execute:
  `./gradlew <project>:updateApi`, the projects and their root paths can be found in
  `settings.gradle`.
- When moving files, use `git mv` to keep version control history.
- Do not make a git commit unless specifically requested.

## Development Workflow & Refactoring:

- **Language Conversion:** When migrating files, convert them from Java to idiomatic Kotlin.
- **Assertion Library:** When migrating or writing new tests, replace Hamcrest assertions with
  Kruth.
- **Kotlin Idioms:** Prefer modern Kotlin idioms for readability.

## Description of sub-projects:

- navigation-common: Contains the common Navigation APIs.
- navigation-runtime: Contains the runtime Navigation APIs.
- navigation-compose: Contains the Compose integration for Navigation.
- navigation-fragment: Contains the Fragment integration for Navigation.
- navigation-ui: Contains the UI integration for Navigation.
- navigation-testing: Contains the testing helpers for Navigation.
- navigation-safe-args-gradle-plugin: Contains the Safe Args Gradle Plugin.

## Documentation links:

- https://developer.android.com/guide/navigation
- https://developer.android.com/jetpack/androidx/releases/navigation
