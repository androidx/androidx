# Project: XR SceneCore (`xr/scenecore`)

This directory contains the Jetpack SceneCore libraries (`androidx.xr.scenecore`), which provide 3D/XR spatial scene management, activity panels, 3D glTF model rendering, custom mesh geometry, PBR materials, spatial audio, and spatial component building blocks for Android XR.

---

## Canonical Build & Development Commands

Run all commands from the repository root (`frameworks/support`). Always scope Gradle invocations with `PROJECT_PREFIX` to avoid repo-wide configuration overhead:

- **Build SceneCore module**:
  ```bash
  PROJECT_PREFIX=:xr:scenecore ./gradlew :xr:scenecore:scenecore:assemble
  ```
- **Compile all SceneCore subprojects**:
  ```bash
  PROJECT_PREFIX=:xr:scenecore ./gradlew :xr:scenecore:scenecore-spatial-core:assemble :xr:scenecore:scenecore-spatial-rendering:assemble :xr:scenecore:scenecore-openxr:assemble
  ```
- **Run Unit Tests (Robolectric / JVM)**:
  ```bash
  PROJECT_PREFIX=:xr:scenecore ./gradlew :xr:scenecore:scenecore-spatial-core:testReleaseUnitTest
  PROJECT_PREFIX=:xr:scenecore ./gradlew :xr:scenecore:scenecore-spatial-rendering:testReleaseUnitTest
  PROJECT_PREFIX=:xr:scenecore ./gradlew :xr:scenecore:scenecore-openxr:testReleaseUnitTest
  ```
- **Update Public API Tracks**:
  ```bash
  ./gradlew :xr:scenecore:scenecore:updateApi
  ```
- **Check Public API Compatibility**:
  ```bash
  ./gradlew :xr:scenecore:scenecore:checkApi
  ```
- **Format Kotlin Files (`ktfmt`)**:
  ```bash
  ./gradlew :ktCheckFile --format --file xr/scenecore/scenecore/src/main/java/androidx/xr/scenecore/Scene.kt
  ```

---

## 3-Tier SceneCore Modular Architecture

SceneCore is structured into three distinct architectural tiers to enforce separation between developer-facing APIs, runtime abstractions, platform OS extensions, split rendering engines, and the OpenXR backend:

```
+-----------------------------------------------------------------------------------------+
| Tier 1: Consumer SDK Layer (androidx.xr.scenecore / :xr:scenecore:scenecore)            |
| -> Public classes: Scene, Entity, MainPanelEntity, GltfModelEntity, MovableComponent...  |
+-----------------------------------------------------------------------------------------+
                                          |
                                    Delegates via
                                          |
+-----------------------------------------------------------------------------------------+
| Tier 2: Runtime Abstraction (androidx.xr.scenecore.runtime / :scenecore-runtime)        |
| -> Interfaces: SceneRuntime, RenderingRuntime, Entity, Component & Audio wrappers        |
+-----------------------------------------------------------------------------------------+
                     /                    |                      \
         Implemented by             Implemented by             Implemented by
                   /                      |                        \
+-------------------------+   +---------------------------+   +---------------------------+
| On-Device Spatial Core  |   | On-Device Spatial Render  |   | OpenXR Backend (WIP)      |
| :scenecore-spatial-core |   | :scenecore-spatial-rendering| | :scenecore-openxr         |
|                         |   |                           |   |                           |
| Interfaces directly with|   | Integrates native         |   | Native OpenXR calls via   |
| platform XrExtensions   |   | SplitEngine (ImpressApi)  |   | SceneCoreOpenXrNative JNI |
| and Android OS APIs     |   | inside an extension       |   | bridge. Snapshot-only;    |
|                         |   | SubspaceNode              |   | not on public devices yet |
+-------------------------+   +---------------------------+   +---------------------------+
```

### 1. Consumer SDK Layer (`:xr:scenecore:scenecore`)
- **Package**: `androidx.xr.scenecore.*`
- **Role**: Main developer entry point (`Scene`, `Entity`, `PanelEntity`, `ActivityPanelEntity`, `GltfModelEntity`, `AnchorEntity`, `SpatialEnvironment`, `SpatialCapabilities`, `ScenePose`).
- **Rule**: Public classes wrap runtime delegates obtained via `scenecore-runtime`. Never call `com.android.extensions.xr.*` directly in this layer.

### 2. Runtime Interface Abstraction (`:xr:scenecore:scenecore-runtime`)
- **Package**: `androidx.xr.scenecore.runtime.*`
- **Role**: Defines `SceneRuntime` (spatial scene graph, entities, components, audio, capability listeners) and `RenderingRuntime` (glTF models, custom meshes, PBR/water materials, textures, EXR IBL). Decouples hardware runtimes from public APIs.
- **Rule**: Annotated with `@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)`.

### 3. Concrete Implementations
- **`scenecore-spatial-core` (`SpatialSceneRuntime`)**: Foundational on-device spatial engine (`AndroidXrEntity`, `ActivitySpaceImpl`, `PanelEntityImpl`, `MainPanelEntityImpl`, `MovableComponentImpl`, `ResizableComponentImpl`). Interacts directly with platform `com.android.extensions.xr.*` APIs and `SurfaceControlViewHost`.
- **`scenecore-spatial-rendering` (`SpatialRenderingRuntime`)**: Hardware-accelerated 3D split-rendering engine backed by the native Impress/SplitEngine library (`ImpressApi`). Encapsulates glTF loading, PBR materials, custom mesh geometry, stereoscopic surfaces, and EXR IBL lighting inside extension `SubspaceNode` instances.
- **`scenecore-openxr` (`OpenXrSceneRuntime`)**: Experimental cross-vendor OpenXR implementation using native JNI calls (`SceneCoreOpenXrNative`). Snapshot-only (not published to Maven) while OpenXR runtime pathways are finalized on commercial devices. `OpenXrSceneRuntimeFactory` only requires `Feature.FULLSTACK`, so when it is on the classpath it is selected wherever `SpatialSceneRuntimeFactory`'s requirements are not met.
- **`scenecore-testing` (`FakeSceneRuntime`)**: Fakes and test doubles (`FakeSceneRuntime`, `FakeRenderingRuntime`, `FakeEntity`, `FakeImpressApiImpl`, `EntityTester`) for Robolectric/JVM testing without physical XR displays or native graphics hardware.
- **`integration-tests:testapp`**: Integration test applications (`testapp`, `videoplayerdrmtest`, `TestAppActivityLeakTest`).

---

## Ecosystem & System Dependencies Map

| Component | Package / Artifact | Architectural Role & Details |
| :--- | :--- | :--- |
| **Android XR Extensions** | `com.android.extensions.xr.*` | Platform sidecar library defining `XrExtensions`, `Node`, `NodeTransaction`, `PassthroughState`, spatial audio, `ReformOptions`, and `AvatarManager`. Prebuilt `com.android.extensions.xr.host.test.jar` is used in host JVM unit tests. |
| **Impress 3D Engine** | `com.google.ar.imp.*` (`com.google.ar:impress`) | High-performance cross-platform 3D/XR engine using a component-based architecture (`imp::Node`, `imp::Component`, `imp::View`, `Registry`, `Dispatcher`, `imp::Future`, `.isf` Impress Scene Files). Wrapped in Kotlin/Java via `ImpressApi` and `ImpressApiImpl`. |
| **SplitEngine** | `com.google.ar.imp.view.splitengine.*` | Zero-copy split-rendering IPC engine separating the frontend application from the backend renderer using shared memory and FlatBuffers (`SplitEngineSubspaceManager`, `ImpSplitEngineRenderer`, `SplitEngineInputEvent`). |
| **Jetpack XR Natives** | `androidx.xr.scenecore.impl.perception.*` / JNI | Native C++/JNI bridges connecting `androidx.xr.scenecore` and `androidx.xr.arcore` to OpenXR drivers, native Impress, and SplitEngine. |

---

## Canonical SDK-to-Extensions Mapping Table

When tracing or modifying behavioral bugs across layers, reference this mapping to see which runtime and extension APIs underpin each SDK feature:

| SceneCore SDK API (`androidx.xr.scenecore`) | Runtime Impl (`scenecore-spatial-core` / `rendering`) | Underlying Extensions / OS System APIs (`com.android.extensions.xr.*`, `android.*`, `ImpressApi`) |
| :--- | :--- | :--- |
| **`Session.scene` / Root Nodes** | `SpatialSceneRuntime` / `SpatialRenderingRuntime` | • `XrExtensions.attachSpatialScene(...)`<br>• Root scene and task window leash `Node` instances created via `XrExtensions.createNode()` |
| **`Scene.activitySpace`** | `ActivitySpaceImpl` (`SystemSpaceEntityImpl`) | • Root scene `Node`<br>• Subscribes to `Node.subscribeToTransform(...)`<br>• Tracks `SpatialState.bounds` via `XrExtensions.setSpatialStateCallback(...)` |
| **`Scene.keyEntity`** | `SpatialSceneRuntime.keyEntity = ...` | • **Requires Spatial API >= 2**<br>• Subscribes to targeted `Node.subscribeToTransform(...)`<br>• Calls `XrExtensions.underlyingObject.setSpatialContinuityHint(activity, pos, rot)` |
| **`Scene.isBoundaryConsentGranted`** | `SpatialSceneRuntime` boundary consent observers | • Queries `Settings.Secure` (`guardian_consent_granted`) & `Settings.System` (`toggle_guardian`) via `ContentResolver`<br>• Registers `ContentObserver` for live updates |
| **`MainPanelEntity`** | `MainPanelEntityImpl` (`BasePanelEntity`) | • Task Window Leash Node (`MainPanelAndTaskWindowLeashNode`)<br>• Reads window dimensions via `Activity.windowManager.currentWindowMetrics.bounds` *(MinAPI 30)*<br>• Resizing triggers asynchronous IPC `XrExtensions.setMainWindowSize(activity, w, h, executor, cb)` |
| **`PanelEntity.create(...)`** | `PanelEntityImpl` | • Requires **MinAPI 30**<br>• Creates offscreen target `android.view.SurfaceControlViewHost(context, display, Binder())`<br>• Invokes `NodeTransaction.setSurfacePackage(node, surfacePackage)` and `.setWindowBounds(...)` |
| **`ActivityPanelEntity.create(...)`** | `ActivityPanelEntityImpl` | • Calls `XrExtensions.createActivityPanel(activity, ActivityPanelLaunchParameters(rect))` (`ActivityPanel`)<br>• `ActivityPanel.launchActivity` / `moveActivity` / `setWindowBounds` |
| **`AnchorEntity.create(...)`** | `AnchorEntityImpl` | • Extracts raw tracking token from `androidx.xr.arcore.Anchor.anchorToken`<br>• `NodeTransaction.setParent(node, activitySpace.node).setAnchorId(node, anchorToken).apply()` |
| **`SpatialEnvironment`** *(Passthrough)* | `SpatialEnvironmentImpl` | • Dedicated environment `Node` (`EnvironmentPassthroughNode`)<br>• `NodeTransaction.setPassthroughState(node, opacity, PASSTHROUGH_MODE_MAX / PASSTHROUGH_MODE_OFF)` |
| **`SpatialEnvironment`** *(Skybox/Geometry)* | `SpatialEnvironmentFeatureImpl` via `scenecore-spatial-rendering` | • `XrExtensions.attachSpatialEnvironment` / `detachSpatialEnvironment`<br>• **Skybox:** `ImpressApi.setPreferredEnvironmentLight(exrImage.nativeHandle)`<br>• **3D Geometry:** `SplitEngineSubspaceManager.createSubspace` + `ImpressApi.instanceGltfModel(...)` |
| **`GltfModelEntity`** | `GltfEntityImpl` & `GltfFeatureImpl` | • `SplitEngineSubspaceManager.createSubspace` creates outer extension `SubspaceNode`<br>• `ImpressApi.loadGltfAsset` & `instanceGltfModel`<br>• Animations controlled via `ImpressApi.animateGltfModel` (`GltfAnimation`) |
| **`MeshEntity`** | `MeshEntityImpl` & `MeshFeatureImpl` | • Enclosed in extension `SubspaceNode`<br>• `ImpressApi` handles `CustomMesh`, `MeshBuffer`, and `KhronosPbrMaterial` setups |
| **`SurfaceEntity`** | `SurfaceEntityImpl` & `SurfaceFeatureImpl` | • Creates `SubspaceNode` boundary via `SplitEngineSubspaceManager`<br>• `ImpressApi` renders stereoscopic split view surfaces (`StereoMode.TOP_BOTTOM`, `SIDE_BY_SIDE`, `MONO`) with custom color transfer/range parameters and alpha mask textures |
| **`MovableComponent`** | `MovableComponentImpl` | • Calls `XrExtensions.createReformOptions(executor, consumer)` (`ReformOptions`)<br>• Configures flags (`ReformOptions.ALLOW_MOVE` & `FLAG_ALLOW_SYSTEM_MOVEMENT`, `FLAG_POSE_RELATIVE_TO_PARENT`)<br>• Attaches to entity via `NodeTransaction.enableReform(node, reformOptions)` |
| **`ResizableComponent`** | `ResizableComponentImpl` | • Uses `ReformOptions` configured with `ReformOptions.ALLOW_RESIZE`, `FLAG_POSE_RELATIVE_TO_PARENT`, min/max bounds, and aspect ratio locks<br>• Intercepts `ReformEvent.proposedSize` to dispatch `ResizeEvent` |
| **`InteractableComponent`** | `InteractableComponentImpl` | • Subscribes directly to `com.android.extensions.xr.node.Node.listenForInput(executor, listener)` |
| **`PointerCaptureComponent`** | `PointerCaptureComponentImpl` | • Invokes `Node.requestPointerCapture(...)` and `Node.stopPointerCapture()` across exclusive pointer sessions |
| **`SpatialPointerComponent`** | `SpatialPointerComponentImpl` | • Customizes ray/pointer icons via `NodeTransaction.setPointerIcon(node, convertSpatialPointerIconType(type))` |
| **`PositionalAudioComponent`** | `PositionalAudioComponentImpl` | • Wraps `com.android.extensions.xr.media.AudioTrackExtensions`<br>• Updates audio source positions in space via `AudioTrackExtensions.setPointSourceParams(...)` (`PointSourceParams`) |
| **`SoundFieldAudioComponent`** | `SoundFieldAudioComponentImpl` | • Calls `AudioTrackExtensions.setSoundFieldAttributes(track, soundFieldAttributes)` for ambisonics parameters |
| **`SoundEffectPoolComponent`** | `SoundEffectPoolComponentImpl` & `SoundEffectPoolImpl` | • Wraps `com.android.extensions.xr.media.SoundPoolExtensions`<br>• Attaches sound sources to node boundaries using `SoundPoolExtensions.setSpatialSource(...)` |
| **`FollowingSpace`** | `FollowingSpaceImpl` | • **Requires Spatial API >= 2**<br>• Applies `NodeTransaction.enableHeadTransform(node, translateMode, rotateMode, translationAmount, rotationAmount)` synchronously in `init` before parenting under `activitySpace.node`<br>• Driven by periodic `NodeTransaction.ping(node).apply()` at 500 ms intervals while keepalive is active (`setFollowerActive`); unparented via `ReferenceCleaner` on GC |
| **`BillboardEntity`** | `BillboardEntityImpl` | • **Requires Spatial API >= 2**<br>• Applies `NodeTransaction.enableBillboard(node, rotateMode, ...)` synchronously in `init` before parenting/positioning<br>• Driven by periodic `NodeTransaction.ping(node).apply()` while attached to a parent (`parent != null`), paused when unparented (`parent = null`), and stopped on `dispose()` |

---

## Technical Deep-Dive, Version-Gating & On-Device Probing

### 1. SplitEngine Subspace Coordination (Outer vs. Inner Transforms)
When rendering complex 3D structures (`GltfModelEntity`, `MeshEntity`, `SurfaceEntity`) alongside standard windowing panels (`PanelEntity`) inside a uniform spatial scene graph without conflicts:
- **Outer Enclosure:** `scenecore-spatial-rendering` encloses 3D rendering inside a hardware boundary created by `SplitEngineSubspaceManager.createSubspace(subspaceName, handle)`, yielding a standard `com.android.extensions.xr.node.Node` of type Subspace.
- **Outer Transforms:** External entity changes (parent hierarchy, world pose, scaling) execute on the enclosing extension `Node` via `NodeTransaction.setPosition / setOrientation / setScale`.
- **Inner Render Graph:** Within that subspace boundary, `ImpressApi` manages a native C++ scene graph (`ImpressNode`). Shaders, skeletal animation ticks (`animateGltfModel`), custom vertex buffers, and PBR/IBL lighting passes run inside SplitEngine without per-frame OS transaction overhead.

### 2. Spatial Continuity Hints (`Spatial API >= 2`)
When assigning a `keyEntity` on `Scene`, `SpatialSceneRuntime` checks the underlying platform capabilities:
- On devices reporting `spatialApiVersion >= 2`, assigning a `keyEntity` registers `Node.subscribeToTransform` on the targeted entity.
- Each live coordinate broadcast updates the system compositor via `xrExtensions.underlyingObject.setSpatialContinuityHint(activity, position, rotation)`.
- When reassigned or cleared, `clearSpatialContinuityHint(activity)` immediately revokes the hint.

### 3. Boundary Consent System Lookups (`ContentResolver`)
To reliably track user boundary/guardian consent across extension revisions, `SpatialSceneRuntime.calculateBoundaryConsentState()` queries Android settings:
- Reads `Settings.Secure.getInt(resolver, "guardian_consent_granted", 0)` along with `Settings.System.getInt(resolver, "toggle_guardian", 1)`.
- Uses a `ContentObserver` registered against both settings URIs to dispatch real-time events through `addOnBoundaryConsentChangedListener`.

### 4. Phase 0 On-Device Empirical Probing Workflow
Before writing complex SDK workarounds for pose, bounds, parenting, or occlusion bugs:
1. Inspect live compositor scene-graph state on the device:
   - True world pose computed by the compositor (`ImpNodeWorldPosition`).
   - Actual parenting hierarchy (`Parent`).
   - Accumulated transform scale (`Scale`).
   - Raycast and input hit-test state (`Collider Enabled` / `Layer Mask`).
2. Enable verbose logging:
   ```bash
   adb shell setprop log.tag.SceneCore VERBOSE
   adb logcat -s SceneCore:V Impress:V XrExtensions:V
   ```
3. If the platform compositor computes incorrect transforms or bounds, determine whether the bug originates in the platform layer before applying workarounds in SceneCore.

---

## Resource Lifecycle & Teardown Invariants

Memory leaks and native crashes in XR applications frequently stem from out-of-order teardown or dangling listeners. The following invariants MUST be preserved:

### 1. Strict Reverse Teardown Order
Entities, runtimes, and native subsystems must be disposed in the strict reverse order of creation:
```
1. mEntityManager.clear()          (Dispose all user-created Entity instances)
2. mSpatialEnvironment.dispose()   (Clear skybox/passthrough listeners & Impress nodes)
3. mSceneNodeRegistry.clear()      (Detach extension nodes from scene graph)
4. extensions.CleanupTransaction   (Apply final empty transform transaction)
5. impressApi.dispose()            (Tear down SplitEngine native render session)
6. renderer.dispose()              (Release EGL/Vulkan surfaces and graphics contexts)
```

### 2. `SpatialEnvironmentImpl.dispose()` Listener & Node Clearing
- Always unregister skybox and passthrough change listeners on disposal.
- Explicitly destroy underlying Impress environment nodes (`destroySkybox()`, `destroyPassthrough()`) to release native texture handles and HDR maps immediately.
- Null out callback references so registered lambdas do not retain the host `Activity`.

### 3. `ImpressApiImpl` Post-Destroy Guards
- Every public entry point in `ImpressApiImpl` and native JNI bindings MUST guard against calls after disposal:
  ```kotlin
  if (isDestroyed) {
      throw IllegalStateException("Attempted to call ImpressApi after disposal.")
  }
  ```
  *(Note: `android.util.Log` is forbidden in AndroidX library runtime code; do NOT use `Log.w` here).*
- Calling Impress JNI after `dispose()` triggers `SIGSEGV` or `SIGABRT` in native code.

### 4. `BindingsResourceManager` Daemon Thread Cleanup
- Native binding release queues and asynchronous resource disposal threads must terminate their looper/executor when the session is destroyed.
- Prevent thread pool leaks across Activity recreations.

### 5. Component Listener Detachment on Entity Dispose
- `ResizableComponentImpl` and `MovableComponentImpl` MUST call `removeReformEventConsumer` or unregister touch/reform listeners when their host entity is disposed.
- Failing to unregister leaves dangling listeners that process input for dead entities.

### 6. `TestAppActivityLeakTest` Main-Looper Idling
- Activity leak tests must idle the main looper before checking references:
  ```kotlin
  InstrumentationRegistry.getInstrumentation().waitForIdleSync()
  ```
- Always drain the main looper queue before taking weak reference snapshots or asserting zero retained activities.

### 7. `ReferenceCleaner` & Indirect `ScheduledFuture` Retention
- Entities without a public `dispose()` method rely on `ReferenceCleaner` (`registerCleanup(executor, CleanupAction)`).
- `CleanupAction` must be a `private class` that never captures `this`, **and** nothing referenced by `CleanupAction` (such as a shared state lock holding a `ScheduledFuture<*>`) may capture `this` inside a scheduled runnable. Use companion/static task factories so an active `ScheduledFuture` in `scheduledExecutor`'s work queue does not keep the entity strongly reachable.
- Verify GC eligibility while scheduled tasks are still pending using `MemoryUtils.assertGarbageCollected`.

### 8. Unparenting Raw-Transaction Root Nodes on Cleanup
- When a root space's extension node is parented under `activitySpace.node` directly via `NodeTransaction.setParent` rather than the Kotlin `parent` property (so `parent == null`), `AndroidXrEntity.dispose()` will not detach it.
- Its `CleanupAction` MUST explicitly execute `transaction.setParent(node, null).apply()` so the parent node does not keep the detached node alive for the lifetime of `ActivitySpace`.

### 9. Synchronous `NodeTransaction` Model & Initial Config Ordering
- `SpatialSceneRuntime` and `AndroidXrEntity` execute `NodeTransaction.apply()` synchronously on the caller's thread, not on `scheduledExecutor`.
- Any node configuration that gates privacy or rendering behavior on the underlying node MUST be applied **synchronously in `init`** before the node is parented (`setParent`) or positioned (`setPose`).

### 10. Shared `ReformOptions.flags` Bitmask Preservation
- `AndroidXrEntity.getReformOptions()` returns a single shared `ReformOptions` instance across `MovableComponentImpl` and `ResizableComponentImpl`.
- Never overwrite `reformOptions.flags = ...` with a bare assignment when toggling movement flags (e.g., on `onParentChanged`); preserve orthogonal bits via bitmasking (`(reformOptions.flags and ReformOptions.FLAG_SCALE_WITH_DISTANCE) or ...`).

### 11. Mid-Gesture Scene-Graph Transition Invariants (`MovableComponent` / `MovableComponentImpl`)
- Never simply `return null` and drop `MOVE`/`UP`/`CANCEL` events when an entity is reparented into a disallowed subtree while `isMoving == true`—doing so leaves `isMoving` stuck `true`, leaks `entityShadowRenderer` or `setPlanePoseForMoveUpdatePose` plane shadows, and starves `MoveEventListener`s of `MOVE_STATE_END`.
- **In `MovableComponentImpl`**: when `isMoving` is true and movement becomes disallowed on the new parent, reset `isMoving = false`, call `entityShadowRenderer?.hideShadow()`, and emit a single terminal `MoveEvent.MOVE_STATE_END` with the last delivered pose, scale, and ray; reset `lastRenderedPose = null` and `lastInputRay = null` in `onDetach`.
- **In `MovableComponent`**: track `isPlaneShadowShown = anchorablePlanePose != null && moveEvent.moveState != MoveEvent.MOVE_STATE_END`, clear it via `setPlanePoseForMoveUpdatePose(null, Pose.Identity)` if anchoring is disabled mid-move, and reset `isPlaneShadowShown = false` in `onDetach`.

---

## Coordinate-Space & Transform Math Invariants

### 1. Single-Snapshot `ActivitySpace` Conversion
- When processing asynchronous spatial events (`InputEvent`, `HitInfo`, spatial pointer motion), capture `activitySpace.transformPoseTo(PerceptionSpace)` (or `OriginCompensation`) in a **single snapshot** at the beginning of the event.
- **NEVER** re-query `entity.transformPoseTo()` across separate asynchronous steps in the same event. Head tracking or origin updates between reads cause frame-to-frame shear and matrix drift.

### 2. `ActivitySpace.hitTest` Scene-Parent Compensation
- When unprojecting hit test queries and results into `ActivitySpace`:
  - Compensate outgoing rays and incoming hit positions/distances for uniform `sceneParentScale`.
  - Apply gravity-aligned orientation compensation before computing local intersection points and surface normals.

### 3. Gravity-Aligned Pose Heading at Vertical Poles
- When calculating gravity-aligned poses (`gravityAlignedYawRotation` / `getGravityAlignedPose`), do not discard yaw when pitch approaches `±90°` (gimbal lock).
- **North vs. South Pole Sign Asymmetry**: When pitched straight up (`+90°`, `forward.y > 0`), the device's top (`+Y`) tilts backward across the pole opposite to the facing direction (`upSign = -1f` when measuring against `-Z`); when pitched straight down (`-90°`, `forward.y < 0`), `+Y` points forward along the horizontal facing direction (`upSign = +1f`). Using the same sign at both poles flips heading by `180°`.
- **Smooth Polar Blend & Inverse Guards**: Blend smoothly between the horizontal forward projection and the signed up-vector fallback across the polar threshold window (`r < polarBlendThreshold`), and always guard `Matrix4.inverse` calls with `isTrs` / `isInvertible` before inverting.

### 4. `AnchorEntityImpl.worldSpaceScale == Vector3.One`
- AR anchors in Android XR must maintain unit scale in world space (`1.0, 1.0, 1.0`).
- Therefore, `AnchorEntityImpl.getScale(Space.ACTIVITY)` MUST return:
  ```kotlin
  Vector3(
      1f / activitySpace.worldSpaceScale.x,
      1f / activitySpace.worldSpaceScale.y,
      1f / activitySpace.worldSpaceScale.z
  )
  ```

### 5. `ReformOptions.FLAG_POSE_RELATIVE_TO_PARENT` Requirement
- When configuring `MovableComponentImpl` or `ResizableComponentImpl` on entities, always pass `ReformOptions.FLAG_POSE_RELATIVE_TO_PARENT`.
- This ensures reform transforms are calculated relative to the parent entity rather than the `ActivitySpace` root, and is required for reforms on nodes under head-transform protection.

### 6. `MainPanelEntityImpl` Live `WindowManager` Bounds
- In full-space or panel resize events, always fall back to `WindowManager.currentWindowMetrics.bounds` if spatial panel bounds have not yet received an initial layout pass.

### 7. Head-Following Privacy & Input Sanitization
- When an entity is in a head-following subtree (`SceneGraphRules.isInFollowerSubtree(inputEntity)`):
  - Node transform subscriptions (`Node.subscribeToTransform`) are paused by the platform while under head-transform protection; do not build redundant listener-detachment state machines in SceneCore.
  - Platform `InputEvent.HitInfo` events still carry world-space hit transforms and positions, so SceneCore MUST sanitize `InputEvent` synchronously upon arrival (before posting to listener executors): set `hitPosition = null` and compute `transform` from SceneCore's parent-relative poses up to the nearest follower ancestor (returning `Matrix4.Identity` if `!entityToAncestor.isTrs`, e.g., under a zero-scale ancestor).
  - Keep scene-graph rules (`isFollower`, `withholdsWorldTransform`, `SceneGraphRules`) in `scenecore-runtime` and `scenecore-spatial-core` rather than scattering subtype checks across public `scenecore` classes.

---

## Entity and Component Integration Checklist (Mandatory for Every New Type)

Evaluate every item below when designing or reviewing a new SceneCore entity or component type:

### 1. Scene-Graph Queries and Setters
- `getPose` / `setPose` / `getScale` / `setScale` in `Space.PARENT`, `ACTIVITY` and `REAL_WORLD`, on the type itself and on its descendants.
- `ScenePose.transformPoseTo` / `transformPositionTo` / `transformVectorTo` / `transformDirectionTo`, `poseInActivitySpace`, `hitTest` (all overloads).
- World pose readers: `PanelEntity.getPerceivedResolution` and `SurfaceEntity.getPerceivedResolution`.
- Documented `@throws` on every affected existing method in KDoc.

### 2. Scene and Lifecycle
- `Scene.keyEntity` and spatial continuity: can the type or its descendants be the key entity? What happens when the current key entity moves under it?
- Main panel behavior under the type across Home Space ↔ Full Space.
- Parenting: can the type be parented, re-parented or unparented? Can it nest with other entities of the same family?
- Lifecycle: GC reclamation with no explicit dispose, session close, activity recreate, and ordering of queued runtime tasks after disposal.
- Recenter behavior while active.

### 3. Components
- `MovableComponent`: system, custom, anchorable, and on glTF/mesh entities.
- `ResizableComponent` and reform flags (`FLAG_POSE_RELATIVE_TO_PARENT` under head-following ancestors).
- `InteractableComponent` and `InputEvent.HitInfo` fields (`hitPosition`, `transform`, `inputEntity`).

### 4. Privacy and Permissions
- What pose data reaches the app through queries, hit info, reform events, or node transform subscriptions.
- Which permissions the feature must work without, enforced by the test app manifest.
- Residual leaks the SDK cannot close, filed with platform bug references.

### 5. Version and Runtime Gating
- `spatialApiVersion` gating, and behavior on runtimes that cannot support the feature (e.g., OpenXR): exception type and KDoc.
- Fakes and testers in `scenecore-testing`, including simulating unsupported runtimes.

---

## Jetpack XR API Council Directives & 3-Tier Severity Matrix

The Android XR / Jetpack XR API Council enforces zero-debt engineering, Kotlin/Compose consistency, and ABI stability across `androidx.xr`:

* **Level 1 (Hard Blockers)**:
  - ABI breaks or breaking changes on published signatures.
  - Memory or `Context` leaks (retaining `Activity` in static callbacks, singletons, or pending `ScheduledFuture` tasks).
  - Leaking C++ / native engine types (`Impress`, `SplitEngine`, native pointer handles, or `com.android.extensions.xr.*` types) into public `androidx.xr.*` APIs.
  - Calling `android.util.Log` in library runtime code.
  - Undefined alpha math (`PanelBlendMode` premultiplied vs. straight alpha).
  - Wrong spatial units (meters vs. millimeters vs. pixels; always encode units in parameter/property names or use typed `Duration` / `FloatSize2d` / `FloatSize3d`).
  - Middle default parameters in `@JvmOverloads` (default parameters must always be at the end).
  - Diverging from 2D Compose layout conventions.
  - Silent disposal state (use `Entity.isDisposed` and throw `DisposedException` or `IllegalStateException`).
  - Boolean `isDynamic` or frontend CPU mesh copies (use zero-copy `MeshBuffer.MeshUsage`).
  - Unbundled `Parcelable` removals.
* **Level 2 (Major Objections)**:
  - Conflating creation-time `*Attributes` with runtime-mutable `*Params`.
  - Visual corner radius clipping desynchronized from hit-test colliders (`CurvedRect`).
  - Async event-bus pose-readiness notifications instead of polling accuracy fields.
  - Non-cancelling async coroutines across lifecycle events.
* **Level 3 (Advisory / Ergonomics)**:
  - KDoc clarity (including `@throws` and unit specifications), `Vector3` transform helpers avoiding `Pose` allocations, and Kotlin `val`/`var` properties vs. Java `get*()`/`set*()`.

---

## Multi-CL Feature Stack Planning, Shaping & Review Discipline

### 1. 5-Layer Stack Dependency Order
Structure multi-CL feature stacks following the canonical dependency hierarchy:
```
[Layer 1: scenecore-runtime]           Internal interfaces (@RestrictTo LIBRARY_GROUP_PREFIX)
           │
           ├───► [Layer 2a: scenecore-spatial-core]       XrExtensions & NodeTransaction impls
           └───► [Layer 2b: scenecore-spatial-rendering]  Impress C++ JNI & SplitEngine impls
                           │
                           ▼
                 [Layer 3: scenecore]                     Public developer Kotlin API
                           │
                           ▼
                 [Layer 4: scenecore-testing]             High-fidelity Fake* test doubles
                           │
                           ▼
                 [Layer 5: integration-tests:testapp]     Interactive scenarios & *RulesTest
```

### 2. Strict Wave Separation
- **Wave 1 (Core Bug Fixes)** must contain *only* the minimal, complete set of changes needed to fix the target bugs across all affected consumers (`hitTest`, `InputEvent`, `HitInfo`, `MovableComponent` rays) so Wave 1 leaves all callers in a consistent coordinate space if merged standalone.
- **Wave 2 (Edge-Case Hardening & Lifecycle Cleanup)** sits on top of Wave 1 so it can be reviewed or abandoned without blocking Wave 1. Never trap a P1/P2 bug fix inside a wide refactor.

### 3. Symbol-in-Same-Commit Rule & Pruning Speculative Runtime Surface
- Never add helper methods, properties, or parameters in commit $N$ whose first production caller is in commit $N+1$ or later.
- Every symbol added in commit $N$ must have a production caller and unit test in commit $N$.
- When public API review narrows a feature surface to fixed presets, prune unused public constructors/constants from internal `scenecore-runtime` tunable classes in the same stack rather than leaving dead runtime surface.

### 4. Per-Commit Invariant Gate Before Upload
- Run `git rebase <base> --exec "<gradle test>"` to verify that every commit in the stack compiles, passes unit tests independently, and never drops or weakens tests introduced in earlier commits.
- Fix lint findings; don't add `lint-baseline.xml` or `.ignore` API files to get past them.

### 5. Reviewer Precedence Hierarchy & Review Evidence Bar
- **Precedence Hierarchy**:
  1. Subsystem Domain Leads (Architecture, threading, IPC contracts)
  2. Android API Council (Public API design, naming, compatibility)
  3. Kotlin Readability Mentors (Language idioms, styling)
  4. Automated AI Review Bots (Linting, static analysis)
- **4-Pillar Evidence Bar** for code reviews:
  1. Exact `file:line` anchor on added/modified lines.
  2. Concrete execution trace or contract violation.
  3. Concrete failure mode and impact.
  4. Actionable fix snippet.
- **High-Signal Skip List**:
  - Never enforce DRY in unit tests. Unit tests must be readable, isolated, and DAMP.
  - Skip purely subjective formatting nits handled by `ktfmt`.
- **Single-Leader Topic Presubmit**:
  - Always use a single Gerrit topic (`repo start <topic> .` / `git push ... -o topic=<topic>`).
  - Vote `Presubmit-Ready+1` on **exactly ONE leader change per topic** (the bottom-most/leaf change). Never vote on each change individually.

### 6. Headset Test-App UX Standards
- Opening a scenario should enter Full Space. Put the control panel to one side, follower content in the center, and world content on the other side.
- Snaps and placements triggered from the control panel land where the user is looking (the panel). Offer a **3-second countdown** shown in a head-following panel so the user can look away first.
- Use touch targets of at least 56 dp and text of at least 16 sp. Size movable/resizable affordances from a layout listener, never once at startup.

### 7. `scenecore-testing` Dual-Fake Hierarchy Parity
- In `:xr:scenecore:scenecore-testing`, `FakeSceneRuntime.createEntity` and `createPanelEntity` return outer `testing.FakeEntity` wrappers (wrapping `testing.internal.FakeEntity`), whereas other factory methods may return `testing.internal.Fake*` instances directly (unwrapped).
- Consequently:
  - `testing.FakeEntity.parent` MUST call `addChildInternal` / `removeChildInternal` on both `(_parent as? FakeEntity)` and `(_parent as? InternalFakeEntity)`.
  - Outer `testing.FakeEntity` methods (`getPose`, `setPose`, `getScale`, `setScale`, `transformPoseTo`, `hitTest`) MUST evaluate `SceneGraphRules` against `this` (which walks the outer wrapper's `parent` chain) rather than delegating directly to `rtEntity` (whose internal `parent` is only set when the parent is also an outer `FakeEntity`).

---

## General Guidelines for Developing SceneCore Code

1. **Maintain Clean Layer Abstractions**:
   - Keep public `androidx.xr.scenecore` classes decoupled from implementation details. Public entities/components wrap `SceneRuntime` / `RenderingRuntime` interfaces.
   - Never expose Impress types, `com.android.extensions.xr` platform internals, or native memory handles directly in `androidx.xr.scenecore`.
2. **Testing Rules & Best Practices**:
   - Write host unit tests using `scenecore-testing` fakes (`FakeSceneRuntime`, `FakeRenderingRuntime`, `FakeEntity`, `FakeImpressApiImpl`).
   - Never require real OpenXR sessions or GPU hardware in host unit tests (`test/`).
   - Use Google Truth (`assertThat(...)`) for assertions. Avoid standard JUnit assertions.
3. **Public API & Versioning**:
   - Use `@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)` for internal runtime APIs.
   - Mark unstable/experimental APIs with appropriate annotations (e.g., `@ExperimentalGltfAnimationApi`).
   - Always run `./gradlew :xr:scenecore:scenecore:updateApi` whenever public API signatures change.
4. **Threading & Lifecycle Rules**:
   - Scene graph modifications must run on the UI/Main thread.
   - Heavy asset loading operations (glTF models, textures, audio resources) must be asynchronous and tied to proper Lifecycles.
