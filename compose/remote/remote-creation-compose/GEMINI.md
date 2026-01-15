# RemoteCompose Creation API Conventions

This document outlines the conventions and patterns for building and using public APIs in `remote-creation-compose`.

## Core Principle: Remote-First API

All public drawing APIs should prioritize "Remote" types over standard platform or Compose types. This ensures that the APIs are compatible with dynamic expressions and remote evaluation.

### Remote Types Mapping

| Standard Type | Remote Type | Description |
| :--- | :--- | :--- |
| `Float` | `RemoteFloat` | Used for coordinates, sizes, alpha, stroke width, etc. |
| `String` | `RemoteString` | Used for text content. |
| `Color` | `RemoteColor` | Used for solid colors. |
| `Brush` | `RemoteBrush` | Used for gradients and shaders. |
| `Offset` | `RemoteOffset` | Pair of `RemoteFloat` (x, y). |
| `Size` | `RemoteSize` | Pair of `RemoteFloat` (width, height). |
| `ImageBitmap` | `RemoteBitmap` | Used for drawing images. |
| `Paint` | `RemotePaint` | Extension of `android.graphics.Paint` capable of carrying remote IDs. |

## Key Patterns and Learnings

### 1. Handling `RemoteFloat`
`RemoteFloat` can represent either a constant value or a dynamic expression (identified by an ID).
- **NEVER** use `internalAsFloat()` for arithmetic or logic. It returns the remote ID encoded as a NaN, which will corrupt any calculation.
- **DO** use `getFloatIdForCreationState(creationState)` when you need to serialize the value/ID into a `RecordingCanvas` command. If this is required put a TODO in RecordingCanvas to add a Remote overload.

### 2. `RemotePaint` usage
`RemotePaint` is a critical bridge. It allows standard `android.graphics.Paint` properties to be associated with remote IDs.
- `RecordingCanvas.usePaint` intercepts these properties and serializes their IDs via `PaintBundle`.

### 3. Bridging Compose Types
When bridging standard Compose types (like `BlendMode` or `ColorFilter`) to the remote implementation:
- Prefer using established conversion methods like `asAndroidColorFilter()`.
- If a conversion is `internal` (like `BlendMode.toAndroidBlendMode()`), implement a reusable local helper in the UI layer to maintain connectivity while respecting visibility. But check for an existing one in `compose/remote` first.

### 4. Incremental Public API Design
Always start with the minimum set of methods required by downstream consumers (like `remote-material3`). This reduces the maintenance surface and serialization overhead. Implementing unused methods will introduce a lot of transitive complexity and untested code.

### 5. V1 Compatibility in RecordingCanvas
While `RemoteCanvas2` and `RemoteDrawScope2` are pruned and use remote-first types, `RecordingCanvas` must maintain overloads for standard platform types (`Float`, `Rect`, etc.) to support the existing V1 `RemoteDrawScope` during the migration period.
- **CAUTION**: When adding new functionality to V2, consider if a V1 bridge is needed in `RecordingCanvas` to prevent build regressions.

### 6. Specialized Drawing Signatures
Some drawing operations, like `drawTextOnCircle`, have complex signatures in the underlying `RemoteComposeWriter`. Ensure these signatures are correctly mirrored in `RemoteCanvas2` and `RemoteDrawScope2`, matching the writer's expectations for optional parameters like warp radius, alignment, and placement.
