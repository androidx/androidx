# RemoteCompose System Architecture

RemoteCompose is a system for serializing a UI tree and drawing commands into a binary format that can be played back on a remote device (the "Player"). It supports dynamic expressions, animations, and interactive elements without requiring a full re-serialization of the document.

## Core Modules

### 1. `remote-core` (The Protocol & Engine)
This is the heart of the system. It defines the binary protocol and the logic for decoding and maintaining the state of a remote document.
- **`Operations.java`**: Defines the opcode map (e.g., `DRAW_RECT`, `LAYOUT_BOX`).
- **`CoreDocument.java`**: Represents the state of the document on the player side. It holds the decoded operations, variables, and the layout tree.
- **`RemoteComposeBuffer.java`**: Low-level buffer for writing/reading the binary stream.
- **`RemoteContext.java`**: Maintains the runtime state during playback, including variable values, clock time, and transformation matrices.
- **`operations/`**: Individual classes for each operation (e.g., `DrawRect.java`). These handle their own serialization and playback logic.

### 2. `remote-creation-core` & `remote-creation` (The Creation API)
These modules provide the developer-facing API to build the remote document.
- **`RemoteComposeWriter.java`**: High-level API to emit commands into the buffer.
- **`RemoteComposeContext.kt`**: A Kotlin DSL wrapper around the writer, providing a more idiomatic way to build layouts.
- **`RecordingModifier.java`**: Collects layout modifiers (padding, size, etc.) to be serialized alongside components.
- **`Painter.java`**: A bridge to `android.graphics.Paint` that tracks changes and serializes them as `PAINT_VALUES` operations.

### 3. `remote-player-view` (The Renderer)
The Android-specific implementation for playing back the remote document.
- **`RemoteComposePlayer.java`**: Orchestrates the playback, handling timing, input events, and rendering to a `Canvas`.
- **`RemoteComposeView.java`**: A standard Android `View` that hosts a `RemoteComposePlayer`.
- **`accessibility/`**: Bridges the remote semantic operations to the Android Accessibility system.

## Data Flow & Lifecycle

1.  **Creation**: A developer uses `RemoteComposeContext` (DSL) or `RemoteComposeWriter` to define the UI.
2.  **Serialization**: The writer emits binary opcodes and data into a `WireBuffer`.
3.  **Transmission**: The resulting byte array is sent to the Player.
4.  **Decoding**: The Player's `CoreDocument` reads the buffer and reconstructs the operation list and layout tree.
5.  **Playback (Continuous)**:
    - **Update**: The `RemoteContext` updates time-based variables (`ANIMATION_TIME`, etc.).
    - **Layout**: If needed, `LayoutCompute` calculates component positions and sizes.
    - **Draw**: The player iterates through drawing operations, using the `RemoteContext` to resolve dynamic expressions (e.g., an animated color or position).
    - **Interaction**: Touch events are captured by the Player and may trigger `Actions` (like updating a variable) or be sent back to the host.

## Key Concepts

### Dynamic Expressions (`RFloat`, `FloatExpression`)
Instead of fixed values, many operations accept "Remote IDs" that point to expressions. These expressions can be:
- **Constants**: Fixed values.
- **Animated**: Values that depend on time or state.
- **Math**: Combinations of other expressions (ADD, MUL, SIN, etc.).
- **System**: Values provided by the player (e.g., `WINDOW_WIDTH`, `TOUCH_X`).

### Layout Tree
RemoteCompose doesn't just draw; it understands layout.
- **Components**: `Box`, `Row`, `Column`, `Text`, `Image`.
- **Modifiers**: Applied to components to change their behavior or appearance.
- **Measure Pass**: The player performs a layout pass similar to Compose or standard Android views.

### The Painter & Commit
The `Painter` (or `RemotePaint`) is stateful during creation. You set properties (color, stroke) and must call `commit()` to emit a `PAINT_VALUES` operation. All subsequent drawing operations will use that paint state until the next commit or a `save/restore` block.

## How to Describe a Command
When describing a command (operation):
1.  **Identify the Opcode**: Find it in `Operations.java`.
2.  **Locate the Implementation**: Find the class in `remote-core/src/main/java/androidx/compose/remote/core/operations/`.
3.  **Check the `write` method**: See how it's serialized (which fields, what order).
4.  **Check the `paint` / `apply` method**: See how it's rendered or how it affects the `RemoteContext`.
