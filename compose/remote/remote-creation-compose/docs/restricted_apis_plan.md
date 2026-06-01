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


- [ ] **Batch 1: `androidx.compose.remote.creation.compose.action`**
  - **Focus**: Actions and input events.
  - **Key APIs to Review**:
    - `Action.toRemoteAction()`: Core method to convert to remote operation. Needs redesign to avoid exposing it on a public interface (e.g., use a well-known base class to check for instead).
    - `HostAction` class and `HostAction.Type` enum: Used for triggering named actions on the host.
    - `ValueChange` overloads for `RemoteDp`: Restricted factory functions.
  - **Proposed CL Split**:
    - [ ] **CL 1**: Redesign `Action` to avoid `toRemoteAction()` on public interface.
    - [ ] **CL 2**: Review `HostAction` and related types.
    - [ ] **CL 3**: Review `ValueChange` factory functions and overloads.

- [ ] **Batch 2: `androidx.compose.remote.creation.compose.layout` (Part 1: Arrangements & Alignments)**
  - [ ] Review `RemoteArrangement` (25 occurrences) and `RemoteAlignment` (restricted implementation classes and conversion methods).
  - [ ] Investigate inconsistencies with Compose and fix naming.
  - [ ] Decide if these should be made public to support custom arrangements/alignments.

- [ ] **Batch 3: `androidx.compose.remote.creation.compose.layout` (Part 2: Drawing)**
  - [ ] Review `RemoteDrawScope` (24 occurrences), `DrawHelpers` (7 occurrences), and `RemoteAccess` (3 occurrences).
  - [ ] Compare with Compose `DrawScope` and match standards.

- [ ] **Batch 4: `androidx.compose.remote.creation.compose.layout` (Part 3: Components)**
  - [ ] Review `RemoteBox` and `RemoteBoxScope`
  - [ ] Review `RemoteRow` (specifically `weight(Float)` in `RemoteRowScope`)
  - [ ] Review `RemoteColumn` (specifically `weight(Float)` in `RemoteColumnScope`)
  - [ ] Ensure standard layout components are correctly exposed.

- [ ] **Batch 5: `RemoteText`**
  - [ ] Review `RemoteText` (4 occurrences)
  - [ ] Review `RemoteTextStyle` (3 occurrences)

- [ ] **Batch 6: `androidx.compose.remote.creation.compose.state` (Part 1: Primitives)**
  - [x] Review `RemoteBoolean` ([CL 4097154](https://android-review.googlesource.com/c/platform/frameworks/support/+/4097154) and [CL 4097233](https://android-review.googlesource.com/c/platform/frameworks/support/+/4097233))
  - [x] Review `RemoteInt` ([CL 4003513](https://android-review.googlesource.com/c/platform/frameworks/support/+/4003513) and [CL 4094960](https://android-review.googlesource.com/c/platform/frameworks/support/+/4094960))
  - [x] Review `RemoteFloat` ([CL 4003513](https://android-review.googlesource.com/c/platform/frameworks/support/+/4003513) and [CL 4097153](https://android-review.googlesource.com/c/platform/frameworks/support/+/4097153))
  - [ ] Review `RemoteLong`
  - [ ] Review `RemoteString`
  - [x] Cluster operations into related groups (arithmetic, comparison, conversion, etc.)

- [ ] **Batch 7: `androidx.compose.remote.creation.compose.state` (Part 2: Complex Types)**
  - [ ] Review `RemoteColor` (10 occurrences)
  - [x] Review `RemotePaint` - Expose `fontVariationSettings` ([CL 4034886](https://android-review.googlesource.com/c/platform/frameworks/support/+/4034886))
  - [ ] Review `RemoteBitmap` (7 occurrences)
  - [ ] Review `RemoteBitmapFont` (2 occurrences)
  - [ ] Review `RemoteColorFilter` (3 occurrences)
  - [ ] Review `RemoteTextUnit` (5 occurrences)
  - [ ] Review `RemoteDp` (5 occurrences)
  - [ ] Compare and fix mapping to Compose naming for graphics types.

- [ ] **Batch 8: `androidx.compose.remote.creation.compose.state` (Part 3: Helpers)**
  - [x] Review `RemoteFloatOperations` / logical and bitwise operators ([CL 4094960](https://android-review.googlesource.com/c/platform/frameworks/support/+/4094960))
  - [x] Review `RemoteEnum` and `RemoteStateLayout` ([CL 4003413](https://android-review.googlesource.com/c/platform/frameworks/support/+/4003413))
  - [ ] Review `RemoteMatrix3x3` (3 occurrences)
  - [ ] Review `RemoteStateScope` and investigate if it should be `internal`.

- [ ] **Batch 9: `androidx.compose.remote.creation.compose.vector`**
  - [ ] Review `RemoteVector` (1 occurrence)
  - [ ] Review `RemoteVectorPainter` (1 occurrence)
  - [x] Review `RemotePathNode` / `RemotePath.Builder` ([CL 4076652](https://android-review.googlesource.com/c/platform/frameworks/support/+/4076652))
  - [ ] Focus on vector graphics support.

- [ ] **Batch 10: `androidx.compose.remote.creation.compose.shapes`**
  - [ ] Review `RemoteCornerSize` (1 occurrence)
  - [ ] Review `RemoteOutline` (3 occurrences)
  - [ ] Review `RemoteCornerBasedShape` (4 occurrences)
  - [ ] Review `RemoteRoundedCornerShape` (9 occurrences)
  - [ ] Focus on shapes support.

- [ ] **Batch 11: `androidx.compose.remote.creation.compose.modifier`**
  - [ ] Make all Modifier implementation classes `internal`.
  - [ ] Separate internal implementation from public modifier factory functions if needed.
  - *Note*: Since making implementation classes internal doesn't require public API changes, this can be done in a single CL.

- [ ] **Batch 12: `androidx.compose.remote.creation.compose.painter`**
  - [ ] Review `RemoteColorPainter`
  - [ ] Review `RemoteBitmapPainter`

- [ ] **Batch 13: Others**
  - [ ] Review `ExperimentalRemoteCreationComposeApi`
  - [ ] Review `RemoteComposeCreationComposeFlags`
  - [ ] Review `DocumentStats`

- [ ] **Batch 14: `androidx.compose.remote.creation.compose.capture`**
  - [ ] Review `RemoteComposeCreationState`
  - [ ] Review `LocalRemoteComposeCreationState`
  - [ ] Review `RecordingCanvas`
  - [x] Address `Flow<>` capture and make `captureSingleRemoteDocument` public ([CL 4092671](https://android-review.googlesource.com/c/platform/frameworks/support/+/4092671))

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
