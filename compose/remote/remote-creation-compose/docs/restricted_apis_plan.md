# Plan to review and clean up Restricted APIs in remote-creation-compose

This plan outlines the strategy to review and clean up all restricted APIs in the `remote-creation-compose` module.

## Guidelines

- **Small & Focused**: API changes should be kept small and focused. Sequence these logically building on what is already public at that point.
- **Refactor First**: Make refactoring changes in a previous CL, then stack the API request. Ensure refactoring tasks (like redesigning interfaces to avoid leaky restricted APIs) are tracked as first tasks.
- **Compose Standards**: APIs should be reviewed and match Compose standards and naming. Analysis should be included in the CL body.
- **Downstream Churn**: Consider churn for downstream users. Avoid breaking everything multiple times; do it once or use temporary deprecations.
- **No Public Java Classes**: Avoid exposing public APIs from remote-creation Java classes directly due to lack of named parameters.
- **Internal/Private Visibility**: Prefer making APIs `internal` or `private` if they are only needed within the module or file, rather than making them public or deleting them if they are still useful for implementation.

## Batches (TODO List)

We will tackle the cleanup in batches, grouped by package.

### In-Progress API Cleanups & CLs

The following API reviews and cleanups are currently in progress:

- **[CL 4003513](https://android-review.googlesource.com/c/platform/frameworks/support/+/4003513)**: Removed `RemoteInt/Float.toRemoteString(before, after, flags)` to clean up leaky APIs.
- **[CL 4003413](https://android-review.googlesource.com/c/platform/frameworks/support/+/4003413)**: Exposed `RemoteEnum` and `RemoteStateLayout` as public APIs.
- **[CL 4034886](https://android-review.googlesource.com/c/platform/frameworks/support/+/4034886)**: Exposed `fontVariationSettings` in `RemotePaint`.
- **[CL 4076652](https://android-review.googlesource.com/c/platform/frameworks/support/+/4076652)**: Exposed the `RemotePath.Builder` API.
- **[CL 4092671](https://android-review.googlesource.com/c/platform/frameworks/support/+/4092671)**: Exposed the capture `Flow` API and made the non-deprecated `captureSingleRemoteDocument` public.
- **[CL 4094960](https://android-review.googlesource.com/c/platform/frameworks/support/+/4094960)**: Refactored logical and bitwise operator parameters in `RemoteState`.
- **[CL 4097154](https://android-review.googlesource.com/c/platform/frameworks/support/+/4097154)**: Renamed `RemoteBoolean` comparison operators.
- **[CL 4097153](https://android-review.googlesource.com/c/platform/frameworks/support/+/4097153)**: Renamed `RemoteFloat` comparison operators.
- **[CL 4097233](https://android-review.googlesource.com/c/platform/frameworks/support/+/4097233)**: Renamed `RemoteBoolean` logical operators.

---


- [x] **Batch 1: `androidx.compose.remote.creation.compose.action`**
  - **Focus**: Actions and input events.
  - **Key APIs to Review**:
    - `Action.toRemoteAction()`: Core method to convert to remote operation. Redesigned to use `RemoteAction` base class so `Action` interface is clean public API.
    - `HostAction` class and `HostAction.Type` enum: Made internal, public `hostAction` factory functions exposed.
    - `ValueChange`: Made internal, public `valueChange` factory function exposed.
  - **Proposed CL Split**:
    - [x] **CL 1**: Redesign `Action` to avoid `toRemoteAction()` on public interface.
    - [x] **CL 2**: Review `HostAction` and related types.
    - [x] **CL 3**: Review `ValueChange` factory functions and overloads.

- [x] **Batch 2: `androidx.compose.remote.creation.compose.layout` (Part 1: Arrangements & Alignments)**
  - [x] Review `RemoteArrangement` (15 occurrences) and `RemoteAlignment` (8 occurrences). Public interfaces and standard arrangement/alignment presets exposed; internal wire conversion `toRemote()` methods restricted.
  - [x] Investigate inconsistencies with Compose and fix naming.
  - [x] Decided to keep interfaces and factory presets public while restricting implementation classes.

- [ ] **Batch 3: `androidx.compose.remote.creation.compose.layout` (Part 2: Drawing)**
  - [ ] Review `RemoteDrawScope` (24 occurrences), `DrawHelpers` (7 occurrences), `RemoteAccess` (1 occurrence), and `RemoteContentDrawScope` (1 occurrence).
  - [x] Standard draw methods (`drawLine`, `drawRect`, `drawCircle`, `drawOval`, `drawArc`, `drawRoundRect`, `drawTextOnCircle`) exposed as public APIs.
  - [ ] Review remaining restricted transformation, path, text, and offscreen drawing methods.
  - [ ] Compare with Compose `DrawScope` and match standards.

- [x] **Batch 4: `androidx.compose.remote.creation.compose.layout` (Part 3: Components)**
  - [x] Review `RemoteBox` and `RemoteBoxScope` (`RemoteBox` standard layout exposed).
  - [x] Review `RemoteRow` (specifically `weight(RemoteFloat)` in `RemoteRowScope` exposed as public).
  - [x] Review `RemoteColumn` (specifically `weight(RemoteFloat)` in `RemoteColumnScope` exposed as public).
  - [x] Ensure standard layout components are correctly exposed.

- [x] **Batch 5: `RemoteText`**
  - [x] Review `RemoteText` (1 restricted overload remaining; primary `RemoteText` composable exposed with full Compose styling parameters).
  - [x] Review `RemoteTextStyle` (1 restricted constructor remaining; public class and `fromTextStyle` factory exposed).

- [x] **Batch 6: `androidx.compose.remote.creation.compose.state` (Part 1: Primitives)**
  - [x] Review `RemoteBoolean` ([CL 4097154](https://android-review.googlesource.com/c/platform/frameworks/support/+/4097154) and [CL 4097233](https://android-review.googlesource.com/c/platform/frameworks/support/+/4097233))
  - [x] Review `RemoteInt` ([CL 4003513](https://android-review.googlesource.com/c/platform/frameworks/support/+/4003513) and [CL 4094960](https://android-review.googlesource.com/c/platform/frameworks/support/+/4094960))
  - [x] Review `RemoteFloat` ([CL 4003513](https://android-review.googlesource.com/c/platform/frameworks/support/+/4003513) and [CL 4097153](https://android-review.googlesource.com/c/platform/frameworks/support/+/4097153))
  - [x] Review `RemoteLong` (`rememberNamedRemoteLong`, arithmetic operators, and `toRemoteInt()` exposed).
  - [x] Review `RemoteString` (`.rs` extension, string operations `lowercase`, `uppercase`, `trim`, `isEmpty`, `isNotEmpty`, `plus`, `rememberNamedRemoteString`, `rememberMutableRemoteString` exposed).
  - [x] Cluster operations into related groups (arithmetic, comparison, conversion, etc.)

- [x] **Batch 7: `androidx.compose.remote.creation.compose.state` (Part 2: Complex Types)**
  - [x] Review `RemoteColor` (11 occurrences) - companion invoke, `rememberNamedRemoteColor`, `.rc` extensions exposed.
  - [x] Review `RemotePaint` - Expose `fontVariationSettings` ([CL 4034886](https://android-review.googlesource.com/c/platform/frameworks/support/+/4034886)).
  - [x] Review `RemoteImageBitmap` (2 occurrences) - `rememberNamedRemoteImageBitmap`, `rememberMutableRemoteImageBitmap` exposed.
  - [x] Review `RemoteBitmapFont` (3 occurrences) - `rememberRemoteBitmapFont` exposed.
  - [x] Review `RemoteColorFilter` (3 occurrences) - `tint`, `colorMatrix` exposed.
  - [x] Review `RemoteTextUnit` (2 occurrences) - `toPx()`, `.rsp`, `.toRsp()`, `asRemoteTextUnit()` exposed.
  - [x] Review `RemoteDp` (4 occurrences) - `toPx()`, `.rdp`, `asRdp()`, `asRemoteDp()`, `toRemoteDp()`, arithmetic/comparison operators, `rememberNamedRemoteDp`, `min`, `max` exposed.
  - [x] Compare and fix mapping to Compose naming for graphics types.

- [x] **Batch 8: `androidx.compose.remote.creation.compose.state` (Part 3: Helpers)**
  - [x] Review `RemoteFloatOperations` / logical and bitwise operators ([CL 4094960](https://android-review.googlesource.com/c/platform/frameworks/support/+/4094960)).
  - [x] Review `RemoteEnum` and `RemoteStateLayout` ([CL 4003413](https://android-review.googlesource.com/c/platform/frameworks/support/+/4003413)).
  - [x] Review `RemoteMatrix3x3` (2 occurrences) - `times` operator and `constantValueOrNull` exposed.
  - [x] Review `RemoteStateScope` - public interface with `parentScope`, `remoteDensity`, `layoutDirection`, `remotePath` exposed; internal document access restricted.

- [x] **Batch 9: `androidx.compose.remote.creation.compose.vector`**
  - [x] Review `RemoteVectorPainter` (`rememberRemoteVectorPainter` exposed)
  - [x] Review `RemotePathNode` / `RemotePath.Builder` ([CL 4076652](https://android-review.googlesource.com/c/platform/frameworks/support/+/4076652))
  - [x] `RemoteVector` (1 occurrence) - Not planned (kept internal/restricted)

- [x] **Batch 10: `androidx.compose.remote.creation.compose.shapes`**
  - [x] Review `RemoteCornerSize` (1 occurrence) - `RemoteCornerSize` factory functions exposed.
  - [x] Review `RemoteOutline` (3 occurrences) - public sealed class exposed.
  - [x] Review `RemoteCornerBasedShape` (6 occurrences) - public abstract class exposed.
  - [x] Review `RemoteRoundedCornerShape` (10 occurrences) - public class & factory functions exposed.
  - [x] Focus on shapes support.

- [x] **Batch 11: `androidx.compose.remote.creation.compose.modifier`**
  - [x] Make Modifier implementation classes `internal` (17 modifier implementation classes converted to `internal`).
  - [x] Separate internal implementation from public modifier factory functions (35+ public modifier factory functions exposed in `api/current.txt`).

- [x] **Batch 12: `androidx.compose.remote.creation.compose.painter`**
  - [x] Review `RemoteColorPainter` (`painterRemoteColor` exposed).
  - [x] Review `RemoteBitmapPainter` / `RemoteImageBitmapPainter` (`painterRemoteImageBitmap` exposed).
  - [x] Review `RemotePainter` base class exposed.

- [x] **Batch 13: Others**
  - [x] Review `ExperimentalRemoteCreationComposeApi` (opt-in annotation restricted to library group).
  - [x] Review `RemoteComposeCreationComposeFlags` (public flags object exposed).

- [x] **Batch 14: `androidx.compose.remote.creation.compose.capture`**
  - [x] Address `Flow<>` capture and make `captureSingleRemoteDocument` public ([CL 4092671](https://android-review.googlesource.com/c/platform/frameworks/support/+/4092671))
  - [x] Review `RemoteComposeCreationState` & `LocalRemoteComposeCreationState`
  - [x] `RecordingCanvas` (2 occurrences) - Not planned (kept internal/restricted)

## Out of Scope

The following APIs are considered out of scope for the current cleanup effort:

- **`androidx.compose.remote.creation.compose.widgets`**
  - Review `WidgetsModifiers`
  - Review `RemoteComposeWidget`
  - Review `AbstractRCWidget`
  - Review `RCWidget`
  - Review `ProceduralRCWidget`
  - Review `WidgetLambdaAction`
  - Review `WidgetInformation`
- **Other Not-Planned Restricted APIs**
  - `RemoteVector` (vector model internals)
  - `RecordingCanvas` (internal canvas recording implementation)
