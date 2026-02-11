# RemoteCompose Data Flow & Lifecycle

This document describes how a RemoteCompose document moves from initial creation to final rendering on a device.

## 1. Creation (Server/Host)
The developer uses the `RemoteComposeWriter` (Java) or `RemoteComposeContext` (Kotlin DSL) to emit operations.
- **Buffering**: Operations are serialized into a `WireBuffer` (a growable byte array).
- **ID Management**: The `RemoteComposeState` tracks unique IDs for strings, bitmaps, paths, and dynamic variables.
- **Painter State**: Setting paint properties (color, stroke) accumulates in a `PaintBundle` and is only emitted when `commit()` is called.

## 2. Transmission
The contents of the `WireBuffer` are sent over the network or IPC as a simple byte array (`byte[]`).

## 3. Decoding (Client/Player)
The Player receives the bytes and initializes a `CoreDocument`.
- **Parsing**: `Operations.read()` iterates through the buffer, looking up the OpCode in the `DefaultVersionMap` and instantiating the corresponding `Operation` class.
- **Layout Tree**: Component operations (`COMPONENT_START`, `CONTAINER_END`) reconstruct the UI hierarchy.
- **Variable Registration**: Operations that depend on dynamic values register themselves as listeners in the `RemoteContext`.

## 4. Playback Loop
The Player runs a continuous loop (often driven by vsync):
1.  **Time Update**: `RemoteClock` updates global time variables (`ANIMATION_TIME`, `CONTINUOUS_SEC`).
2.  **Variable Resolution**: The `RemoteContext` evaluates dynamic expressions (RPN) whose dependencies have changed.
3.  **Layout Pass**: If components or variables are dirty, `LayoutCompute` calculates the new geometry.
4.  **Paint Pass**: The Player iterates through the `Operation` list.
    - Each operation calls `apply(RemoteContext)`.
    - Drawing operations use the resolved coordinates and the current `Paint` state.
5.  **Interaction**: Touch events are captured by `RemoteComposeView`, mapped back to components, and may trigger host-side callbacks or local variable updates.
