# Project: XR Runtime (`xr/runtime`)

This directory contains the core session management, runtime lifecycle orchestration, feature configuration state machines, and mathematical primitives for Jetpack XR (`androidx.xr.runtime`).

---

## Canonical Build & Development Commands

Run all commands from the repository root (`frameworks/support`). Always scope Gradle invocations with `PROJECT_PREFIX` to avoid repository-wide configuration overhead:

- **Build XR Runtime**:
  ```bash
  PROJECT_PREFIX=:xr:runtime ./gradlew :xr:runtime:runtime:assemble
  ```
- **Compile all Runtime Subprojects**:
  ```bash
  PROJECT_PREFIX=:xr:runtime ./gradlew :xr:runtime:runtime:assemble :xr:runtime:runtime-interfaces:assemble :xr:runtime:runtime-openxr:assemble
  ```
- **Run Unit Tests (Host JVM)**:
  ```bash
  PROJECT_PREFIX=:xr:runtime ./gradlew :xr:runtime:runtime:testReleaseUnitTest
  ```
- **Update Public API Track**:
  ```bash
  PROJECT_PREFIX=:xr:runtime ./gradlew :xr:runtime:runtime:updateApi
  ```
- **Format Kotlin Files (`ktfmt`)**:
  ```bash
  ./gradlew :ktCheckFile --format --file xr/runtime/runtime/src/main/kotlin/androidx/xr/runtime/Session.kt
  ```

---

## Architecture & Sub-projects Map

The `xr/runtime` module acts as the central control plane for all XR subsystems (Perception/ARCore, SceneCore, and Rendering):

- **`runtime/` (`androidx.xr.runtime`)**: Primary developer API module. Defines `Session`, `Config` (feature toggles), `CoreState` (time-stamped state flows), `SpatialApi` / `SpatialApiVersionHelper`, `XrCapabilities`, `XrDevice`, `XrServiceAvailability`, `AugmentedImageDatabase`, and spatial math primitives (`Pose`, `Vector2`, `Vector3`, `Vector4`, `Matrix3`, `Matrix4`, `Quaternion`, `BoundingBox`, `Ray`, `FieldOfView`, `FloatSize2d`, `FloatSize3d`, `GeospatialPose`).
- **`runtime-interfaces/` (`androidx.xr.runtime.internal`)**: Core service loader contracts and internal lifecycle abstractions (`JxrRuntime`, `PerceptionRuntimeFactory`, `RenderingRuntimeFactory`, `SceneRuntimeFactory`, `SpatialApiVersionProvider`, `SessionResultProvider`, `SessionResultProviderFactory`, `XrInstanceManager`).
- **`runtime-openxr/` (`androidx.xr.runtime.openxr`)**: OpenXR driver and loader integration for native OpenXR platform sessions (`OpenXrRuntimeFactory`).
- **`runtime-manifest/` (`androidx.xr.runtime.manifest`)**: Manifest resources, XML attributes, and permission declarations for XR applications.
- **`runtime-projected/` (`androidx.xr.runtime.projected`)**: Projected / remote display runtime implementations.
- **`runtime-testing/` (`androidx.xr.runtime.testing`)**: Testing fakes and doubles (`FakeSession`, `FakeJxrRuntime`, `FakeSessionConnector`, `FakeStateExtender`) for host JVM unit tests.

---

## Session Architecture & Dynamic Provider Discovery

The `Session` class (`Session.kt`) orchestrates all underlying XR subsystems via dynamic `ServiceLoader` reflection lookups. When an application calls `Session.create(context, coroutineContext, lifecycleOwner)`:

```
+----------------------------------------------------------------------------------------------------+
|                                    Session.create(...) Factory                                     |
+----------------------------------------------------------------------------------------------------+
                                                  |
                         Loads Reflection Providers (ServiceLoader)
                                                  |
           +--------------------------------------+--------------------------------------+
           |                                      |                                      |
           v                                      v                                      v
+-----------------------+              +-----------------------+              +-----------------------+
| Perception Factories  |              |    Scene Factories    |              |  Rendering Factories  |
| - OpenXrRuntime       |              | - SpatialSceneRuntime |              | - SpatialRendering    |
| - ArCoreRuntime       |              | - ProjectedScene      |              |   Runtime             |
| - FakePerception      |              | - FakeScene           |              | - FakeRendering       |
+-----------------------+              +-----------------------+              +-----------------------+
           |                                      |                                      |
           +--------------------------------------+--------------------------------------+
                                                  |
                                       Instantiates & Owns
                                                  |
                                                  v
+----------------------------------------------------------------------------------------------------+
|                             Session Instance (JxrRuntime list)                                     |
|  - Manages StateFlow<CoreState> via updateLoop()                                                   |
|  - Listens to LifecycleOwner (RESUME -> pause() / resume() / destroy())                             |
+----------------------------------------------------------------------------------------------------+
```

### Dynamic ServiceLoader Providers Table

| Provider Type | Service Class String / Class | Concrete Implementations Discovered |
| :--- | :--- | :--- |
| **Perception Runtime** | `PerceptionRuntimeFactory` | • `androidx.xr.arcore.openxr.OpenXrRuntimeFactory`<br>• `androidx.xr.arcore.playservices.ArCoreRuntimeFactory`<br>• `androidx.xr.arcore.testing.FakePerceptionRuntimeFactory`<br>• `androidx.xr.runtime.StubPerceptionRuntimeFactory` |
| **Scene Runtime** | `SceneRuntimeFactory` | • `androidx.xr.scenecore.spatial.core.SpatialSceneRuntimeFactory`<br>• `androidx.xr.scenecore.projected.ProjectedSceneRuntimeFactory`<br>• `androidx.xr.scenecore.testing.FakeSceneRuntimeFactory` |
| **Rendering Runtime** | `RenderingRuntimeFactory` | • `androidx.xr.scenecore.spatial.rendering.SpatialRenderingRuntimeFactory`<br>• `androidx.xr.scenecore.testing.FakeRenderingRuntimeFactory` |
| **State Extender** | `StateExtender` | • `androidx.xr.arcore.PerceptionStateExtender`<br>• `androidx.xr.arcore.playservices.CameraStateExtender`<br>• `androidx.xr.arcore.testing.internal.FakeStateExtender` |
| **Session Connector** | `SessionConnector` | • `androidx.xr.scenecore.Scene`<br>• `androidx.xr.runtime.testing.FakeSessionConnector` |

---

## Configuration State Machine (`Config`)

System features are managed dynamically via `session.configure(config)`. The `Config` object is immutable and built via `Config.Builder`:

- **Plane Tracking** (`PlaneTrackingMode`): Controls horizontal/vertical plane detection.
- **Hand Tracking** (`HandTrackingMode`): Enables 26-joint hand skeleton tracking.
- **Device Tracking** (`DeviceTrackingMode`): Controls 6DOF head and device pose tracking.
- **Depth Estimation** (`DepthEstimationMode`): Toggles depth maps and real-world mesh generation.
- **Anchor Persistence** (`AnchorPersistenceMode`): Enables persisting world anchors across sessions.
- **Face Tracking** (`FaceTrackingMode`): Enables blendshape and mesh face tracking.
- **Geospatial** (`GeospatialMode`): Integrates Google Earth 3D geospatial localization.
- **Eye Tracking** (`EyeTrackingMode`): Enables gaze vector and eye tracking.
- **QR Code Tracking** (`QrCodeTrackingMode`): Controls detection and size estimation of QR codes.
- **Augmented Image Database** (`AugmentedImageDatabase`): Image target detection database.

### Configuration Transaction Rules & Rollback Protocol
- Calling `session.configure(newConfig)` invokes `runtime.configure(newConfig)` sequentially on all active `JxrRuntime` instances inside a `configurationMutex.withLock` block.
- **Transactional Rollback:** If any runtime throws an exception during configuration (e.g. `FaceTrackingNotCalibratedException` or `LibraryNotLinkedException`), `Session` automatically rolls back configuration on all previously mutated runtimes by re-applying `this.config`.

---

## Threading, Performance & StrictMode Rules

1. **Session Creation Thread Safety**:
   - `Session.create` performs disk I/O (loading native libraries via `System.loadLibrary`) and ServiceLoader reflection searches.
   - **StrictMode Compliance:** `Session.create` MUST be called off the Main/UI thread (e.g. using `withContext(Dispatchers.IO)`). Calling it on the Main thread will trigger a StrictMode `DiskReadViolation`.
2. **Lifecycle Observer Cleanup**:
   - `Session` registers a `LifecycleEventObserver` on `lifecycleOwner.lifecycle`.
   - On `DESTROYED`, `session.destroy()` is executed. It immediately unregisters observers from the Main thread to prevent leaks, cancels the `coroutineScope`, and invokes `destroy()` in reverse order on all `runtimes`, `stateExtenders`, and `sessionConnectors`.
3. **Synchronous vs Asynchronous Teardown**:
   - When `destroy()` is called, `Session` uses `configurationMutex.tryLock()`.
   - If the lock is acquired, teardown runs synchronously to ensure native GPU/EGL handles are released before `onDestroy()` returns.
   - If the lock is held asynchronously, a fallback Main-thread coroutine is launched (`configurationMutex.withLock`) to avoid deadlocks in single-threaded test runners.

---

## Guidelines for AI Agents Developing XR Runtime Code

1. **Extending Session State**:
   - To expose new subsystem state in `CoreState`, implement `StateExtender` and register it via ServiceLoader or `stateExtenders` constructor injection.
   - Ensure `extend(state)` is non-blocking and safe for per-frame execution inside `updateLoop()`.

2. **Testing Standards**:
   - Use `runtime-testing` doubles (`FakeSession`, `FakeJxrRuntime`, `FakeSessionConnector`).
   - Use Google Truth (`assertThat(...)`) for assertions. Avoid standard JUnit assertions.

3. **API Contracts & RestrictTo**:
   - Restrict internal contracts (`JxrRuntime`, factories, `StateExtender`) with `@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)`.
   - Always run `PROJECT_PREFIX=:xr:runtime ./gradlew :xr:runtime:runtime:updateApi` whenever public API surfaces in `androidx.xr.runtime` change.
