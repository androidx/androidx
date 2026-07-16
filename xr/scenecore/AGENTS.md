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
  PROJECT_PREFIX=:xr:scenecore ./gradlew :xr:scenecore:scenecore-spatial-core:assemble :xr:scenecore:scenecore-spatial-rendering:assemble :xr:scenecore:scenecore-projected:assemble
  ```
- **Run Unit Tests (Robolectric / JVM)**:
  ```bash
  PROJECT_PREFIX=:xr:scenecore ./gradlew :xr:scenecore:scenecore-spatial-core:testReleaseUnitTest
  PROJECT_PREFIX=:xr:scenecore ./gradlew :xr:scenecore:scenecore-spatial-rendering:testReleaseUnitTest
  PROJECT_PREFIX=:xr:scenecore ./gradlew :xr:scenecore:scenecore-projected:testReleaseUnitTest
  ```
- **Update Public API Tracks**:
  ```bash
  ./gradlew :xr:scenecore:scenecore:updateApi
  ```
- **Format Kotlin Files (`ktfmt`)**:
  ```bash
  ./gradlew :ktCheckFile --format --file xr/scenecore/scenecore/src/main/java/androidx/xr/scenecore/Scene.kt
  ```

---

## 3-Tier SceneCore Modular Architecture

SceneCore is structured into three distinct architectural tiers to enforce separation between developer-facing APIs, runtime abstractions, platform OS extensions, split rendering engines, and IPC projection:

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
| On-Device Spatial Core  |   | On-Device Spatial Render  |   | Projected & Tethered Mode |
| :scenecore-spatial-core |   | :scenecore-spatial-rendering| | :scenecore-projected      |
|                         |   |                           |   |                           |
| Interfaces directly with|   | Integrates Google's       |   | Jetpack AIDL IPC services |
| Vendor XrExtensions and |   | native SplitEngine        |   | (IProjectedSceneCoreService)|
| Android Platform APIs   |   | (ImpressApi) inside an    |   | across process/device     |
|                         |   | extension SubspaceNode    |   | boundaries                |
+-------------------------+   +---------------------------+   +---------------------------+
```

### 1. Consumer SDK Layer (`:xr:scenecore:scenecore`)
- **Package**: `androidx.xr.scenecore.*`
- **Role**: Main developer entry point (`Scene`, `Entity`, `PanelEntity`, `ActivityPanelEntity`, `GltfModelEntity`, `AnchorEntity`, `SpatialEnvironment`, `SpatialCapabilities`).
- **Rule**: Public classes wrap runtime delegates obtained via `scenecore-runtime`. Never call `com.android.extensions.xr.*` directly in this layer.

### 2. Runtime Interface Abstraction (`:xr:scenecore:scenecore-runtime`)
- **Package**: `androidx.xr.scenecore.runtime.*`
- **Role**: Defines `SceneRuntime` (spatial scene graph, entities, components, audio, capability listeners) and `RenderingRuntime` (glTF models, custom meshes, PBR/water materials, textures, EXR IBL). Decouples hardware runtimes from public APIs.

### 3. Concrete Implementations
- **`scenecore-spatial-core` (`SpatialSceneRuntime`)**: Foundational on-device spatial engine (`AndroidXrEntity`, `ActivitySpaceImpl`, `PanelEntityImpl`, `MovableComponentImpl`). Interacts directly with platform `com.android.extensions.xr.*` APIs and `SurfaceControlViewHost`.
- **`scenecore-spatial-rendering` (`SpatialRenderingRuntime`)**: 3D spatial rendering engine. Operates inside extension `SubspaceNode` (`SplitEngineSubspaceManager`) to offload heavy rendering tasks (glTF models, materials, stereoscopic surfaces, IBL skybox) to native **SplitEngine (`ImpressApi`)**.
- **`scenecore-projected` (`ProjectedSceneRuntime`)**: Supports projected/remote displays via AIDL services (`IProjectedSceneCoreService.aidl`, `IProjectedNode.aidl`).
- **`scenecore-testing` (`FakeSceneRuntime`)**: Fakes and test doubles (`FakeSceneRuntime`, `FakeRenderingRuntime`, `FakeEntity`, `FakeImpressApiImpl`, `EntityTester`) for Robolectric/JVM testing without physical XR displays or native graphics hardware.
- **`integration-tests`**: Integration test applications (`testapp`, `videoplayerdrmtest`).

---

## Ecosystem & System Dependencies Map

Understanding the underlying platforms and native graphics stack is vital for debugging cross-layer issues:

| Component | Source Reference / Location | Architectural Role & Details |
| :--- | :--- | :--- |
| **Android XR Extensions** | `http://go/mhcs1` (`frameworks/base/libs/xr/Jetpack`) | Platform sidecar library (`com.android.extensions.xr.*` / `android.extensions.xr.*`) defining `XrExtensions`, `Node`, `NodeTransaction`, `PassthroughState`, spatial audio, `AvatarManager`. Prebuilt `com.android.extensions.xr.host.test.jar` is used in host JVM unit tests. |
| **Impress 3D Engine** | `http://go/g3` (`third_party/impress`) | High-performance cross-platform 3D/XR engine using Component-Based Architecture (`imp::Node`, `imp::Component`, `imp::View`, `Registry`, `Dispatcher`, `imp::Future`, `.isf` Impress Scene Files). Wrapped in Java via `com.google.ar:impress` (`ImpressApi` / `ImpressApiImpl`). |
| **SplitEngine** | `http://go/g3` (`third_party/split_engine`) | Zero-copy split-rendering IPC engine separating Frontend app from Backend renderer using Ashmem shared memory and FlatBuffers (`SplitEngineSubspaceManager`, `ImpSplitEngineRenderer`, `SplitEngineInputEvent`). |
| **Jetpack XR Natives** | `http://go/g3` (`third_party/jetpack_xr_natives`) | Native C++/JNI bridge connecting Kotlin/Java APIs (`androidx.xr.scenecore`, `androidx.xr.arcore`) to OpenXR drivers, native Impress, and SplitEngine. |

---

## Canonical SDK-to-Extensions Mapping Table (Agent Quick-Lookup)

When tracing or modifying behavioral bugs across layers, reference this mapping to know exactly what system or extension components underpin each SDK feature:

| SceneCore SDK API (`androidx.xr.scenecore`) | Runtime Impl (`scenecore-spatial-core` / `rendering`) | Underlying Extensions / OS System APIs (`com.android.extensions.xr.*`, `android.*`, `ImpressApi`) |
| :--- | :--- | :--- |
| **`Session.scene` / Root Nodes** | `SpatialSceneRuntime` / `SpatialRenderingRuntime` | • `XrExtensions.attachSpatialScene(...)`<br>• Root scene and task window leash CPM `Node` instances created via `XrExtensions.createNode()` |
| **`Scene.activitySpace`** | `ActivitySpaceImpl` (`SystemSpaceEntityImpl`) | • Root CPM `Node`<br>• Subscribes to `Node.subscribeToTransform(...)`<br>• Tracks `SpatialState.bounds` via `XrExtensions.setSpatialStateCallback(...)` |
| **`Scene.keyEntity`** | `SpatialSceneRuntime.keyEntity = ...` | • **Requires Spatial API >= 2**<br>• Subscribes to targeted `Node.subscribeToTransform(...)`<br>• Calls `XrExtensions.underlyingObject.setSpatialContinuityHint(activity, pos, rot)` |
| **`Scene.isBoundaryConsentGranted`** | `SpatialSceneRuntime` boundary consent observers | • Queries `Settings.Secure` (`guardian_consent_granted`) & `Settings.System` (`toggle_guardian`) via `ContentResolver`<br>• Registers `ContentObserver` for live updates |
| **`MainPanelEntity`** | `MainPanelEntityImpl` (`BasePanelEntity`) | • Task Window Leash CPM Node (`MainPanelAndTaskWindowLeashNode`)<br>• Reads window dimensions via `Activity.windowManager.currentWindowMetrics.bounds` *(MinAPI 30)*<br>• Resizing triggers asynchronous IPC `XrExtensions.setMainWindowSize(activity, w, h, executor, cb)` |
| **`PanelEntity.create(...)`** | `PanelEntityImpl` | • Requires **MinAPI 30**<br>• Creates offscreen target `android.view.SurfaceControlViewHost(context, display, Binder())`<br>• Invokes `NodeTransaction.setSurfacePackage(node, surfacePackage)` and `.setWindowBounds(...)` |
| **`ActivityPanelEntity.create(...)`** | `ActivityPanelEntityImpl` | • Calls `XrExtensions.createActivityPanel(activity, ActivityPanelLaunchParameters(rect))` (`ActivityPanel`)<br>• `ActivityPanel.launchActivity` / `moveActivity` / `setWindowBounds` |
| **`AnchorEntity.create(...)`** | `AnchorEntityImpl` | • Extracts raw tracking token from `androidx.xr.arcore.Anchor.anchorToken`<br>• `NodeTransaction.setParent(node, activitySpace.node).setAnchorId(node, anchorToken).apply()` |
| **`SpatialEnvironment`** *(Passthrough)* | `SpatialEnvironmentImpl` | • Dedicated CPM Node (`EnvironmentPassthroughNode`)<br>• `NodeTransaction.setPassthroughState(node, opacity, PASSTHROUGH_MODE_MAX / PASSTHROUGH_MODE_OFF)` |
| **`SpatialEnvironment`** *(Skybox/Geometry)* | `SpatialEnvironmentFeatureImpl` via `scenecore-spatial-rendering` | • `XrExtensions.attachSpatialEnvironment` / `detachSpatialEnvironment`<br>• **Skybox:** `ImpressApi.setPreferredEnvironmentLight(exrImage.nativeHandle)`<br>• **3D Geometry:** `SplitEngineSubspaceManager.createSubspace` + `ImpressApi.instanceGltfModel(...)` |
| **`GltfModelEntity`** | `GltfEntityImpl` & `GltfFeatureImpl` | • `SplitEngineSubspaceManager.createSubspace` creates outer extension `SubspaceNode`<br>• `ImpressApi.loadGltfAsset` & `instanceGltfModel`<br>• Animations controlled via `ImpressApi.animateGltfModel` (`GltfAnimation`) |
| **`MeshEntity`** | `MeshEntityImpl` & `MeshFeatureImpl` | • Enclosed in extension `SubspaceNode`<br>• `ImpressApi` handles `CustomMesh`, `MeshBuffer`, and `KhronosPbrMaterial` setups |
| **`SurfaceEntity`** | `SurfaceEntityImpl` & `SurfaceFeatureImpl` | • Creates `SubspaceNode` boundary via `SplitEngineSubspaceManager`<br>• `ImpressApi` renders stereoscopic split view surfaces (`StereoMode.TOP_BOTTOM`, `SIDE_BY_SIDE`, `MONO`) with custom color transfer/range parameters and alpha mask textures |
| **`MovableComponent`** | `MovableComponentImpl` | • Calls `XrExtensions.createReformOptions(executor, consumer)` (`ReformOptions`)<br>• Configures flags (`ReformOptions.ALLOW_MOVE` & `FLAG_ALLOW_SYSTEM_MOVEMENT`)<br>• Attaches to entity via `NodeTransaction.enableReform(node, reformOptions)` |
| **`ResizableComponent`** | `ResizableComponentImpl` | • Uses `ReformOptions` configured with `ReformOptions.ALLOW_RESIZE`, min/max bounds, and aspect ratio locks<br>• Intercepts `ReformEvent.proposedSize` to dispatch `ResizeEvent` |
| **`InteractableComponent`** | `InteractableComponentImpl` | • Subscribes directly to `com.android.extensions.xr.node.Node.listenForInput(executor, listener)` |
| **`PointerCaptureComponent`** | `PointerCaptureComponentImpl` | • Invokes `Node.requestPointerCapture(...)` and `Node.stopPointerCapture()` across exclusive pointer sessions |
| **`SpatialPointerComponent`** | `SpatialPointerComponentImpl` | • Customizes ray/pointer icons via `NodeTransaction.setPointerIcon(node, convertSpatialPointerIconType(type))` |
| **`PositionalAudioComponent`** | `PositionalAudioComponentImpl` | • Wraps `com.android.extensions.xr.media.AudioTrackExtensions`<br>• Updates audio source positions in space via `AudioTrackExtensions.setPointSourceParams(...)` (`PointSourceParams`) |
| **`SoundFieldAudioComponent`** | `SoundFieldAudioComponentImpl` | • Calls `AudioTrackExtensions.setSoundFieldAttributes(track, soundFieldAttributes)` for ambisonics parameters |
| **`SoundEffectPoolComponent`** | `SoundEffectPoolComponentImpl` & `SoundEffectPoolImpl` | • Wraps `com.android.extensions.xr.media.SoundPoolExtensions`<br>• Attaches sound sources to node boundaries using `SoundPoolExtensions.setSpatialSource(...)` |

---

## Technical Deep-Dive & Version-Gating Quirks

### 1. SplitEngine Subspace Coordination (Outer vs. Inner Transforms)
When rendering complex 3D structures (`GltfModelEntity`, `MeshEntity`, `SurfaceEntity`) alongside standard windowing panels (`PanelEntity`) inside a uniform spatial scene graph without conflicts:
- **Outer Enclosure:** `scenecore-spatial-rendering` encloses 3D rendering inside a hardware boundary created by `SplitEngineSubspaceManager.createSubspace(subspaceName, handle)`, yielding a standard `com.android.extensions.xr.node.Node` of type Subspace.
- **Outer Transforms:** External entity changes (parent hierarchy, world pose, scaling) execute on the enclosing **CPM Node** via `NodeTransaction.setPosition / setOrientation / setScale`.
- **Inner Render Graph:** Within that subspace boundary, `ImpressApi` manages a complete native C++ scene graph (`ImpressNode`). Shaders, skeletal animation ticks (`animateGltfModel`), custom vertex buffers, and PBR/IBL lighting passes run entirely inside SplitEngine without per-frame OS transaction overhead.

### 2. Spatial Continuity Hints (`Spatial API >= 2`)
When assigning a `keyEntity` on `Scene`, `SpatialSceneRuntime` checks the underlying platform capabilities:
- On devices reporting `spatialApiVersion >= 2`, assigning a `keyEntity` registers `Node.subscribeToTransform` on the targeted entity.
- Each live coordinate broadcast updates the hardware compositor via `xrExtensions.underlyingObject.setSpatialContinuityHint(activity, position, rotation)`.
- When reassigned or cleared, `clearSpatialContinuityHint(activity)` immediately revokes the hint.

### 3. Boundary Consent System Lookups (`ContentResolver`)
To reliably track user boundary/guardian consent across varied extension revisions, `SpatialSceneRuntime.calculateBoundaryConsentState()` directly queries standard Android settings:
- Reads `Settings.Secure.getInt(resolver, "guardian_consent_granted", 0)` along with `Settings.System.getInt(resolver, "toggle_guardian", 1)`.
- Uses a `ContentObserver` registered against both settings URIs to dispatch real-time events through `addOnBoundaryConsentChangedListener`.
- *(Note: Ongoing migrations such as `b/464401298` target transitioning this lookup to dedicated XR Extension endpoints in future Spatial API revisions).*

---

## Guidelines for AI Agents Developing SceneCore Code

1. **Maintain Clean Layer Abstractions**:
   - Keep public `androidx.xr.scenecore` classes decoupled from implementation details. Public entities/components wrap `SceneRuntime` / `RenderingRuntime` interfaces.
   - Never expose Impress types, `com.android.extensions.xr` platform internals, or native memory handles directly in `androidx.xr.scenecore`.

2. **Testing Rules & Best Practices**:
   - Write host unit tests using `scenecore-testing` fakes (`FakeSceneRuntime`, `FakeRenderingRuntime`, `FakeEntity`, `FakeImpressApiImpl`).
   - Never require real OpenXR sessions or GPU hardware in host unit tests (`test/`).
   - Use Google Truth (`assertThat(...)`) for assertions. Avoid standard JUnit assertions.

3. **Public API & Versioning**:
   - Use `@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)` for internal runtime APIs.
   - Mark unstable/experimental APIs with appropriate annotations (e.g., `@ExperimentalGltfAnimationApi`).
   - Always run `./gradlew :xr:scenecore:scenecore:updateApi` whenever public API signatures change.

4. **Threading & Lifecycle Rules**:
   - Scene graph modifications must run on the UI/Main thread.
   - Heavy asset loading operations (glTF models, textures, audio resources) must be asynchronous and tied to proper Lifecycles.
