# Android XR CPM (`dumpsys spf_cpm`) Architecture & Specification

## 1. Overview & Purpose

In Android XR, the **Client Presentation Manager (CPM)** and **SpaceFlinger** form the spatial compositor and window presentation subsystem. `dumpsys spf_cpm` outputs a point-in-time snapshot of the entire spatial scene graph, compositor state, and interactive input target registry.

This document defines the structure, properties, and system-defined node hierarchies of `dumpsys spf_cpm` based strictly on CPM and Window Manager guarantees.

---

## 2. Output Structure

A complete `dumpsys spf_cpm` dump consists of three primary sections:

```
SPF Nodes:
Node Root (stateIndex=0, z=0) id=1:
  ... [Scene Graph Hierarchy] ...

InputTargetManager state:
  Active panel collider count: <count>
  <collider_name_or_id>
  ...

Stats since reset:
  Compositor Frames: <count>, Compositor FPS: <fps>, ...
  Compositor CPU Time (µs): ...
  Compositor GPU Time (µs): ...
Number of living nodes: <count>
Total size of buffers in scene: <bytes>
```

### 2.1 The Spatial Scene Graph (`SPF Nodes:`)
* Nodes are formatted hierarchically using **2 spaces of leading indentation per depth level**.
* Each node begins with a **Header Line**:
  ```text
  Node <name> (stateIndex=<index>, z=<zOrder>) id=<nodeId>:
  ```
* Immediately following the header line are one or more lines containing **Properties** (`key=value` or `key=<composite>`) and **Vector Attributes** (`key=(x y z)`).

### 2.2 Input Target Manager State
* Summarizes active 2D/3D raycast and touch input targets tracked by the spatial input subsystem.
* Lists active input target identifiers matching the `input-id` values found in the scene graph.

### 2.3 Compositor Statistics
* Reports real-time frame rates, frame latch latency, CPU/GPU compositor timings, total living node count, and total GPU buffer memory footprint.

---

## 3. Node Line Format & System Identifiers

### 3.1 Node Header Format
```text
Node <name> (stateIndex=<int>, z=<int>) id=<int>:
```
* **`<name>`**: Identifier assigned to the node. May be system-generated or client-specified.
* **`stateIndex`**: Internal CPM transaction state revision.
* **`z`**: Local composite ordering / sub-layering depth index.
* **`id`**: Unique global integer ID assigned to every living node by CPM.

### 3.2 System-Defined Structural Node Names
CPM, Window Manager, and System UI create structural nodes with predictable naming conventions:

| Node Name Pattern | Origin | Definition |
| :--- | :--- | :--- |
| `Root` | CPM | The single root node of the entire compositor scene graph. |
| `space-root-task-<taskId>` | Window Manager | Root reference space container for Android task `<taskId>`. All surfaces, transforms, and entities belonging to `<taskId>` are descendants of this node. |
| `space-transform-task-<taskId>` | Window Manager | System transform node used for recentering and reforming the task's primary 2D window. |
| `space-transform-spatial-<id>` | Window Manager | Root spatial reference space for the task's 3D spatial content. |
| `window-task-<taskId>` | Window Manager | Wrapper node hosting the task's primary 2D activity window surface. |
| `task-<taskId>-window` | Window Manager | Wrapper node hosting an embedded Activity task window surface. |
| `window-<systemWindowName>` | System UI / CPM | System overlay window leashes (e.g. `window-MIRROR_SYSTEM_ALERT`, `window-VIRTUAL_KEYBOARD`, `window-NOTIFICATION_PANEL`). |
| `<packageName>/<activityName>#<surfaceId>` | SurfaceFlinger / WM | 2D surface quad layer for an Activity's View hierarchy (`isAndroid2d=true`). |
| `#<layerId>` | SpaceFlinger / CPM | Anonymous 2D surface quad layer created underneath an embedded window leash (`isAppSpatialEmbeddedWindowLeash=true`). Holds the raster graphics buffer (`buffer=WxH`) and hit-test region (`touchableRegion`). |
| `SurfaceTracking-VRI[<title>]` | Compositor | `ViewRootImpl` buffer tracking node used by CPM to track buffer latching and damage regions. |
| `DragBar` | System UI | System UI 6DoF move affordance node (`isSysUi=true`). |
| `Resize*` | System UI | System UI corner/edge resize affordance nodes (`isSysUi=true`, e.g. `ResizeTopLeft`, `ResizeBottomRight`). |
| `Caption`, `TaskBar` | System UI | System UI window caption bar node (`isSysUi=true`), distinct from move/resize affordances. |
| `PASSTHROUGH_WINDOW_ACTIONS`, `OVERVIEW`, `GNAV`, `PERSISTENT_INDICATOR`, `LAUNCHER`, `NOTIFICATION_PANEL`, `TOOLTIP`, `SECONDARY_MENU`, `QUICK_SETTINGS`, `DWELL_PANEL` | System UI | System UI spatial panels and overlay windows (`isSysUi=true`). |
| `AudioTrack.Node` | AudioFlinger | Spatial audio emitter node anchored in the scene graph. |
| `jni node` | CPM JNI Bridge | Internal wrapper node created when bridging native C++ SpaceFlinger nodes. |

---

## 4. System Properties & Flags Glossary

### 4.1 Stereoscopic Visibility
* **`visible on both`**: Node is rendered for both the left and right eyes.
* **`hidden on both`**: Node is not rendered for either eye.
* **`requested-vis=show|hide`**: Client-requested visibility state.
* **`stereo-override=no override`**: Active stereoscopic override configuration.

### 4.2 Surface & Buffer Geometry
* **`buffer=<width>x<height>`** (e.g. `buffer=1600x2000`): Native pixel dimensions of the graphics surface buffer. When `buffer=null`, the node does not own a raster buffer.
* **`bufferOrigin=TopLeft|BottomLeft`**: UV coordinate origin convention.
* **`bufferRotation=0|90|180|270`**: Buffer display rotation.
* **`sourceCrop=(left top right bottom)`**: Clipping rectangle applied to the buffer.
* **`root2DSurfaceBoundsInMeters=(width height)`**: Physical metric dimensions of the 2D surface in world space (e.g. `(1.000 1.250)` = 1.0m wide by 1.25m tall).
* **`root2DSurfaceBoundsInPixels=(width height)`**: Pixel resolution corresponding to the metric surface bounds.
* **`root2DSurfaceOffsetInMeters=(x y)`**: Metric displacement of the surface relative to its transform anchor.
* **`touchableRegion=(left top right bottom)`**: 2D hit-testing bounding box in pixels for spatial raycasting and direct touch.
* **`screenBounds=(left top right bottom)`**: 2D display screen bounding box.
* **`cornerRadius=(rx ry)`**: Curvature radii in meters applied to panel corners.
* **`panelBackgroundColor=(r g b a)`**: Fallback background fill color.

### 4.3 Classification Flags
* **`isAndroid2d=true|false`**: True if the node is a 2D Android SurfaceFlinger buffer layer.
* **`isAppSpatialEmbeddedWindowLeash=true|false`**: True if the node is a spatial container leash for an embedded window within a spatial scene.
* **`isSysUi=true|false`**: True if the node is created and owned by System UI.
* **`isImmersiveActivity=true|false`**: True if the activity is running in Full Space (unbounded 3D mode).
* **`hasSubspace=true|false`**: True if the node anchors an embedded 3D entity/glTF subspace subtree.
* **`isSecure=true|false`**: True if the surface has `FLAG_SECURE` set (DRM content).
* **`worldLocked=true|false`**: True if the node is world-locked (anchored to physical space tracking), false if head-locked or soft-headlocked.
* **`canReceiveInput=true|false`**: True if the node is eligible to receive pointer, raycast, or key events.
* **`focusable=true|false`**: True if the surface can acquire input focus.
* **`isOverlayUi=true|false`**: True for floating system overlays.
* **`skipCapture=true|false`**: True if the surface is excluded from screenshots and screen captures.
* **`hasPassthroughControl=true|false`**: True if the node manages camera passthrough opacity/state.

### 4.4 Spatial Interaction & Capabilities
* **`input-id=<int>`**: Global input target identifier assigned by CPM's `InputTargetManager`. When `input-id >= 0` and `canReceiveInput=true`, the node is an active raycast/touch input target.
* **`spaceTransformId=<int>`**: Identifier of the linked space transform.
* **`XrClientId=<int>`**: Process/session client ID.
* **`reformOptions=<enable=<bitmask>, flags=<bitmask>>`**:
  - `enable & 1`: **Movable** (can be translated via 6DoF hand/controller rays).
  - `enable & 2`: **Resizable** (can be resized via edge/corner drag affordances).
  - `enable == 3`: Both movable and resizable.
* **`sysUIMetadata=<specialNode=<int>, ...>`**: System UI metadata attached to special SysUI nodes.

### 4.5 3D Transforms (`ImpNode...` Attributes)
All transforms are computed by SpaceFlinger and output in SI meters and Hamilton quaternions:
* **`ImpNodeWorldPosition=(x y z)`**: World position coordinates in meters.
* **`ImpNodeWorldRotation=(w x y z)`**: World orientation quaternion in `[w, x, y, z]` order.
* **`ImpNodeWorldScale=(sx sy sz)`**: World scale multipliers along X, Y, Z.
* **`ImpNodeWorldForward=(fx fy fz)`**: World forward direction unit vector.
* **`ImpNodeLocalPosition=(x y z)`**: Local position relative to parent in meters.
* **`ImpNodeLocalRotation=(w x y z)`**: Local orientation quaternion in `[w, x, y, z]` order.
* **`ImpNodeLocalScale=(sx sy sz)`**: Local scale multipliers.
* **`ImpNodeLocalForward=(fx fy fz)`**: Local forward direction vector.
* **`ImpNodeIsActive=true|false`**: Transform hierarchy active status.
* **`ImpNodeIsEnabled=true|false`**: Transform node enabled status.

---

## 5. CPM Structural Hierarchies

### 5.1 Task Root Container (`space-root-task-<taskId>`)
Every Android task is rooted at `space-root-task-<taskId>`, which is attached directly under the compositor `Root` node. All surfaces, window leashes, and 3D entities belonging to the task exist within this subtree.

### 5.2 Window Leash Containers & 2D Surface Layers
CPM separates spatial window positioning (leash container nodes) from raster buffer rendering (2D surface nodes):

1. **Task Window Leash**:
   - The container node that directly hosts `window-task-<taskId>`.
   - `window-task-<taskId>` directly hosts the Activity's 2D surface node (`<packageName>/<activityName>#<surfaceId>`, `isAndroid2d=true`).
2. **Activity Window Leash**:
   - The container node that directly hosts `task-<taskId>-window`.
   - `task-<taskId>-window` directly hosts the embedded Activity's 2D surface node (`isAndroid2d=true`).
3. **Embedded Window Leash (`isAppSpatialEmbeddedWindowLeash=true`)**:
   - A spatial container node that directly hosts an anonymous 2D surface quad layer (`#<layerId>`, `isAndroid2d=true`, `buffer=WxH`).
   - Embedded window leashes may be nested inside another window leash's subtree when satellite windows are attached to a parent window.

### 5.3 System UI Affordance Attachment
When a node enables move or resize capabilities (`reformOptions`), System UI attaches interactive affordance nodes (`isSysUi=true`, such as `DragBar` and `Resize*`) as direct children of:
- The target container node itself (e.g., a movable layout container),
- Its task window wrapper (`window-task-<taskId>` or `task-<taskId>-window`), or
- Its 2D surface node (`isAndroid2d=true`).

### 5.4 3D Subspace Roots (`hasSubspace=true`)
When a node hosts an embedded 3D scene or glTF entity hierarchy, CPM marks the root of that 3D subtree with `hasSubspace=true`.
