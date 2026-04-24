# Project: CameraPipe (camera-camera2-pipe)

CameraPipe is a low-level, high-performance Camera2 abstraction layer built from the ground up to be
idiomatic to Kotlin. It provides a flexible and efficient shim to power high-performance camera
applications and serves as the backend for CameraX's `camera-camera2` implementation.

## High-Level Architecture

CameraPipe is organized around several key abstractions:

- **CameraPipe**: The top-level entry point for all interactions with Android cameras. It provides
  APIs to query and control available cameras (`CameraPipe.cameras`), create capture sessions
  (`CameraPipe.createCameraGraph`, `CameraPipe.createFrameGraph`) and manage global resources.
- **CameraDevices**: Provides fine-grained access and control over available cameras on the device.
  An instance of this class can be accessed via `CameraPipe.cameras`. Underneath the hood, the
  camera devices are managed by `Camera2DeviceCache` (for camera ID list management) and
  `Camera2DeviceManager` (for camera prewarming, opening and closing).
- **CameraGraph**: Represents the combined configuration and state of a single camera session. It
  is conceptually synonymous with a `CameraCaptureSession`, except that the user would specify both
  the camera ID and session configurations at once in `CameraGraph.Config` when a `CameraGraph` is
  created, removing the need for the user to manage the cameras separately. `CameraGraph` provides
  APIs for querying streams, setting the `Surface`s of streams, setting parameters and most
  importantly, acquiring an exclusive `CameraGraph` session (`CameraGraph.Session`) through
  `CameraGraph.acquireSession` to submit capture requests and control 3A. Note that
  `CameraGraph.Session` differs from a camera capture session - it is a `CameraGraph` concept that
  grants exclusive access to `CameraGraph` for the single user that acquired the session.
- **FrameGraph**: Extends `CameraGraph` to provide capabilities for advanced stream controls.
  Output images and metadata (capture result) from a `FrameGraph` are grouped into `Frame` objects,
  allowing each frame to contain the output image and the corresponding capture result metadata.
  `FrameGraph`'s capture APIs (`capture`, `captureWith`) can only be used on streams configured with
  `ImageSourceConfig`. Using the `FrameGraph.captureWith` API, the user would receive a
  `FrameBuffer`, a ring-buffer of `Frame` objects that allows for continuous frame processing.
- **StreamGraph**: Provides information around the list of streams (`CameraStream`, `OutputStream`
  and `InputStream`) configured in a `CameraGraph`. An instance of this class can be accessed via
  `CameraGraph.streams`.

### Data Objects

- **CameraPipe.Config**: Application level configuration for `CameraPipe`, notably including
  application `Context`, thread configuration and camera backend configuration. If thread
  configuration is not supplied, CameraPipe will creates its own threads.
- **CameraGraph.Config**: Defines the configurations for a `CameraGraph`, including the camera ID,
  and various session and stream configurations such as the list of streams (`CameraStream.Config`),
  session parameters, persistent parameter settings and request listeners.
- **CameraStream**: Represents a single output pipeline from the camera, associated with one or more
  `OutputStream`s. Defined via `CameraStream.Config`.
- **OutputStream**: Represents a single output destination for a `CameraStream`, including size,
  format, and other properties. Defined via `OutputStream.Config`. For a multi-resolution stream
  (backed by `MultiResolutionImageReader`), one `CameraStream` would contain multiple
  `OutputStream`s.
- **Request**: An immutable set of parameters, target streams, and listeners for a single camera
  capture operation. Conceptually synonymous with a `CaptureRequest`.
- **FrameInfo**: The capture result for a specific frame. It contains mainly the result metadata,
  frame number and the metadata of the request from which the result is generated. Conceptually
  synonymous with a `TotalCaptureResult`.

## Top-level Directories

- **core** (@core/): Contains the basic building blocks for internal classes. It includes core
  classes and utilities used for threading, logging, queueing, synchronization and more.
- **compat** (@compat/): Contains Camera2-specific implementations of CameraPipe's abstractions.
  These include the dispatching of differing API usages across API levels and logic for handling
  platform-specific quirks (issues).
- **internal** (@internal/): Contains core internal logic for camera control, frame distribution,
  output grouping, and frame state management.
- **config** (@config/): Manages dependency injection using Dagger. Contains Dagger components for
  various CameraPipe components.
- **graph** (@graph/): Contains implementations and internal tools and abstractions for a
  `CameraGraph`, including `CameraGraph` and `CameraGraph.Session` implementations, stream and
  surface management utilities, graph state management classes, 3A control and management facilities
  and robust capture request management.
- **framegraph** (@framegraph/): Contains the core logic and utilities that powers a `FrameGraph`.
- **media** (@media/): Contains wrappers and classes to handle Android media class such as `Image`s
  and `ImageReader`s.

## General Instructions

CameraPipe serves as the core library for the camera packages. Designing and making changes should
be done with utmost care, while considering and verifying all CameraPipe and associated projects.

- **CameraPipe Projects**: `:camera:camera-camera2-pipe`, `:camera:camera-camera2-pipe-testing`,
  `:camera:integration-tests:camera-testapp-camera2-pipe`.
- **Associated Projects**: `:camera:camera-common`, `:camera:camera-camera2`,
  `:camera:integration-tests:camera-testapp-core`.
- **Language**: Use Kotlin for all new files. Prefer modern Kotlin idioms for readability.
- **Formatting**: Format all `.kt` files using `ktfmt` before submitting. Use the following command:
  `./gradlew :camera:camera-camera2-pipe:ktCheckFile --format --file <file>`
- **Public API**: CameraPipe's public APIs are restricted to `LIBRARY_GROUP`, but CameraPipe does
  follow the standard [AndroidX API guidelines](https://android.googlesource.com/platform/frameworks/support/+/androidx-main/docs/api_guidelines/).
- **Git Commits**: Do not make a git commit unless specifically requested. Use `git mv` when moving
  files to preserve history.

## Development Workflow

When making CameraPipe changes, you must verify the changes by building CameraPipe
(`camera-camera2-pipe`) and running CameraPipe unit tests. When the changes are ready for review,
make sure to also run CameraX Camera2 (`camera-camera2`) unit tests to verify API and functional
compatibility before uploading the changes.

When `./gradlew` is mentioned, it refers to the script located under `frameworks/support` (@../../).

- **Building CameraPipe**: Use the following command to build CameraPipe:
  ```
  ./gradlew --info \
  :camera:camera-camera2-pipe:compileReleaseSources
  :camera:camera-camera2-pipe-testing:compileReleaseSources
  ```
- **Running CameraPipe Unit Tests**: Use the following command to run CameraPipe unit tests:
  ```
  ./gradlew --info --strict \
  :camera:camera-camera2-pipe:testReleaseUnitTest \
  :camera:camera-camera2-pipe:compileReleaseUnitTestKotlin \
  :camera:camera-camera2-pipe-testing:testReleaseUnitTest \
  :camera:camera-camera2-pipe-testing:compileReleaseUnitTestKotlin \
  :camera:integration-tests:camera-testapp-camera2-pipe:testDebugUnitTest \
  :camera:integration-tests:camera-testapp-camera2-pipe:compileDebugUnitTestKotlin
  ```
- **Running CameraX Camera2 Unit Tests**: Use the following command to run CameraX Camera2 unit
  tests:
  ```
  ./gradlew --info --strict \
  :camera:camera-camera2:testReleaseUnitTest \
  :camera:camera-camera2:compileReleaseUnitTestKotlin
  ```
- **Dependency Injection**: CameraPipe uses **Dagger** for dependency injection. Most core
  components are wired together in the `config` package.
- **Concurrency**: Use **Kotlin Coroutines** and `Flow` for asynchronous operations. For low-level
  thread-safe state, prefer `AtomicFu` or synchronized blocks where appropriate.
- **API Design**: Follow AndroidX API guidelines. Ensure new APIs are easy to use from Java even
  though the library is Kotlin-first. Mark new public APIs with
  `@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)` if it isn't part of the top-level CameraPipe
  directory.

## Testing

CameraPipe relies on robust tests for performance and correctness in hardware interactions.

- **Assertion Library**: Use Google **Truth** for all assertions. Avoid traditional JUnit `assert*`
  or Hamcrest.
- **Fakes over Mocks**: Prioritize using the fakes provided in the `testing` package or
  `:camera:camera-camera2-pipe-testing` module over mocking frameworks like Mockito.
- **Unit Tests**: Most tests are JVM-based Robolectric tests. Run them using:
  `./gradlew :camera:camera-camera2-pipe:test`
- **Robolectric Configuration**: Use `@Config(sdk = [Config.TARGET_SDK])` for standard tests unless
  specific SDK-level behavior needs verification.
- **Functional Tests**: `CameraPipeSimulator`, `CameraGraphSimulator` and `FrameGraphSimulator`
  should be used instead of Robolectric camera when functional testing or end-to-end verification
  is needed.

## Git Commit Messages

Use the standard CameraX/AndroidX commit message format. Each section should be separated by a
blank line. The commit title should not exceed 50 characters, and body lines should not exceed 72
characters.

```
<Commit Title>

<Detailed description of the change.>

Bug: <Bug id>
Test: <Test instructions or command>
```

- **Commit Title**: Short, descriptive summary in the imperative mood (e.g., "Add feature" not
  "Added feature").
- **Bug**: List each bug ID on a new line prefixed with `Bug:`.
- **Change-Id**: When amending commits (e.g., `git commit --amend`), **do not modify or remove the
  `Change-Id:` line**. Never use `-m` alone when amending, as it strips the `Change-Id` and
  creates a duplicate CL.
