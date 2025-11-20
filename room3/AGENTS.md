# Project: Room

## General Instructions:

- When modifying any .kt file, format it via the following command:
  `./gradlew :ktCheckFile --format --file <file>`. If more than one file needs formatting, continue
  adding `--file <next-file>` to the command.
- When a public API is changed or when asked to update the public API files, execute:
  `./gradlew <project>:updateApi`, the projects and their root paths can be found in
  `settings.gradle`.
- When moving files, use `git mv` to keep version control history.

## Development Workflow & Refactoring:

- **Test Migration:** There is an ongoing effort to migrate tests from the Java-based
  `integration-tests/testapp` to the Kotlin-based `integration-tests/kotlintestapp`.
- **Language Conversion:** When migrating files, convert them from Java to idiomatic Kotlin.
- **Assertion Library:** When migrating or writing new tests, replace Hamcrest assertions with
  Kruth.
- **Kotlin Idioms:** Prefer modern Kotlin idioms for readability. For example, convert concatenated
  strings in annotations like `@DatabaseView` to use multi-line strings.

## Description of sub-projects:

- room3-common: Contains the annotation APIs from the library.
- room3-runtime: Contains the runtime part of the library.
- room3-compiler: Contains the KSP processor part of the library.
- room3-compiler-processing: Contains the XProcessing library used for KSP processing and code
  generation.
- room-migration: Contains the migration serialization structure for reading and storing schemas.
- room-testing: Contains the testing helper library for validating migrations.
- room-gradle-plugin: Contains the Room Gradle Plugin used to configure schema directories.

## Documentation links:

- https://developer.android.com/training/data-storage/room
- https://developer.android.com/kotlin/multiplatform/room
- https://developer.android.com/training/data-storage/room/defining-data
- https://developer.android.com/training/data-storage/room/accessing-data
- https://developer.android.com/training/data-storage/room/relationships
- https://developer.android.com/training/data-storage/room/async-queries
- https://developer.android.com/training/data-storage/room/creating-views
- https://developer.android.com/training/data-storage/room/prepopulate
- https://developer.android.com/training/data-storage/room/migrating-db-versions
- https://developer.android.com/training/data-storage/room/testing-db
- https://developer.android.com/training/data-storage/room/referencing-data
- https://developer.android.com/training/data-storage/room/sqlite-room-migration
