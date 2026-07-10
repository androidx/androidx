# Project: Activity

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

- activity: Contains the core Activity APIs.
- activity-compose: Contains the Compose integration for Activity.
- activity-ktx: Contains the Kotlin extensions for Activity.
- activity-lint: Contains the lint checks for Activity.

## Documentation links:

- https://developer.android.com/guide/components/activities/intro-activities
- https://developer.android.com/jetpack/androidx/releases/activity
