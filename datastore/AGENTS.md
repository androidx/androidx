# Project: DataStore

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

- **Multiplatform Support:** DataStore is a Kotlin Multiplatform library. Ensure changes are compatible with non-Android platforms where applicable.
- **Assertion Library:** When writing new tests or updating existing ones, replace Hamcrest or Truth assertions with Kruth.

## Description of sub-projects:

- datastore: Contains the underlying store used by each serialization method along with components that require an Android dependency.
- datastore-core: Contains the underlying store used by each serialization method (multiplatform).
- datastore-preferences: Android Preferences DataStore.
- datastore-preferences-core: Preferences DataStore without Android Dependencies.
- datastore-core-okio: Contains APIs to use datastore-core in multiplatform via okio.
- datastore-guava: Contains wrappers for using DataStore using ListenableFuture.
- datastore-rxjava2: Contains wrappers for using DataStore using RxJava2.
- datastore-rxjava3: Contains wrappers for using DataStore using RxJava2 (RxJava3 wrappers are also present).
- datastore-preferences-rxjava2: Contains wrappers for using Preferences DataStore with RxJava2.
- datastore-preferences-rxjava3: Contains wrappers for using Preferences DataStore with RxJava3.
- datastore-tink: Encryption support for datastore by integrating with the Tink library.
- datastore-proto: Protos used for internal testing or benchmarks.
- datastore-benchmark: Benchmarks for DataStore.
- datastore-sampleapp: Sample application for DataStore.
- datastore-compose-samples: Samples for using DataStore with Compose.

## Documentation links:

- https://developer.android.com/topic/libraries/architecture/datastore
- https://developer.android.com/kotlin/multiplatform/datastore
