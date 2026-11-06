# Plan to review and clean up Restricted APIs in remote-creation-compose

This plan outlines the strategy to review and clean up the remaining restricted APIs in `remote-creation-compose` and `remote-creation-expression`.

## Guidelines

- **Small & Focused**: API changes should be kept small and focused. Sequence these logically building on what is already public at that point.
- **Refactor First**: Make refactoring changes in a previous CL, then stack the API request. Ensure refactoring tasks (like redesigning interfaces to avoid leaky restricted APIs) are tracked as first tasks.
- **Compose Standards**: APIs should be reviewed and match Compose standards and naming. Analysis should be included in the CL body.
- **Downstream Churn**: Consider churn for downstream users. Avoid breaking everything multiple times; do it once or use temporary deprecations.
- **No Public Java Classes**: Avoid exposing public APIs from `remote-creation` Java classes directly due to lack of named parameters.
- **Internal/Private Visibility**: Prefer making APIs `internal` or `private` if they are only needed within the module or file, rather than making them public or deleting them if they are still useful for implementation.

## What's Left

The following restricted APIs remain across `remote-creation-compose` and `remote-creation-expression` to be reviewed (exposed as public APIs, deleted, or kept restricted across module boundaries):

### 1. Layout & Drawing (`androidx.compose.remote.creation.compose.layout`)

- **`RemoteDrawScope`**:
  - Path drawing & clipping (pending Kotlin `RemotePath` API): `drawPath`, `drawTweenPath`, `drawTextOnPath`, `clipPath`, and `RemoteStateScope.remotePath`.
  - Bitmap & anchored text: `drawScaledBitmap`, `drawAnchoredText`.
  - Control flow & offscreen: `drawConditionally`, `drawToOffscreenBitmap`, `loop`.
  - Remote utilities: `remote` (`RemoteAccess`).
- **`RemoteTextMeasurer` & `RemoteTextLayoutResult`**:
  - Split single-line layout measurement, font metrics (`ascent`, `descent`), tight glyph bounds (`glyphLeft`, `glyphTop`, `glyphRight`, `glyphBottom`), and `monospaceWidth` into dedicated `RemoteTextMeasurer` methods and result types.
- **Layout Components & Types**:
  - `RemoteFlowRow`: `@Composable RemoteFlowRow` layout component.
  - `RemoteArrangement.Absolute`: `Left`, `Center`, `Right`, `SpaceBetween`, `SpaceEvenly`, `SpaceAround`, `aligned`, `spacedBy`.
  - `RemoteContentScale` & `RemoteImage(..., RemoteContentScale, ...)`: Remote-typed content scale enum and `RemoteImage` overload.
  - `RemoteText`: `RemoteText(..., RemoteFontVariation.Settings?, ...)` overload and `RemoteTextOverflow`.
  - `RemoteCustomComponent` & `RemoteCustomPropertiesScope`: Custom component DSL.

### 2. Built-in Context Variables & Time

- **Reference-Epoch Time Helpers**:
  - Review remaining restricted reference-time helpers (`deltaFromReferenceInMinutes`, `deltaFromReferenceInHours`, `timeOfReferenceInSeconds`, `timeOfReferenceInMinutes`, `timeOfReferenceInHours`, `dayOfMonthForReference`, `monthOfYearForReference`, `dayOfWeekForReference`, `yearForReference`) and retire or internalize legacy `RemoteTime`.
- **Guard `RemoteFloat(Float)` / `Float.rf`**:
  - Prevent public `RemoteFloat(Float)` and `Float.rf` from acting as a backdoor for raw `RemoteContext.FLOAT_*` NaN-encoded IDs once typed context accessors cover all public use cases.

### 3. Modifiers & Transitions (`androidx.compose.remote.creation.compose.modifier`)

- **Graphics Layer & Effects**:
  - `RemoteBlurEffect`, `RemoteCompositingStrategy`, and the `RemoteModifier.graphicsLayer` overload accepting `RemoteShape`, `RemoteBlurEffect`, and `RemoteCompositingStrategy`.
- **Visibility & Motion**:
  - `RemoteEnterTransition.Custom`, `RemoteExitTransition.Custom`, `RemoteModifier.animationSpec` (with custom enter/exit transitions), and `RemoteModifier.includeMacro` / `RemoteMacro`.

### 4. State, Paint & Expressions (`androidx.compose.remote.creation.compose.state`)

- **`RemotePaint`**:
  - `isAntiAlias`, `filterQuality` (`RemoteFilterQuality`), `shader` (`RemoteShader`), and `colorFilter` (`RemoteColorFilter` / `RemoteBlendModeColorFilter`).
- **`RemoteColor`**:
  - `fromAHSV`, `rgb(Float, Float, Float, Float)`, `lerp(@ColorInt Int, @ColorInt Int, RemoteFloat)`, `compositeOver`, `times`, and `RemoteBoolean.select(@ColorInt Int, @ColorInt Int)`.
- **`RemoteImageBitmap` & `RemoteBitmapFont`**:
  - `createOffscreenRemoteBitmap`, URL `width`/`height` `createNamedRemoteImageBitmap` / `rememberNamedRemoteImageBitmap` overloads, and `RemoteBitmapFont` class/glyph constructors.
- **Primitives & Math Operations**:
  - `RemoteString`: `RemoteString(String)` constructor, `length`, `substring` overloads, `plus(String)`, and `selectIf*` helpers.
  - `RemoteFloatOperations`: `cubicEasing` (and `CUBIC_*` / `SPLINE_CUSTOM` / `EASE_OUT_*` constants), `evalSpline`, `interpolateRemoteFloat`, `animateRemoteFloat`, `rand`, `randRange`, and mixed `Float` overloads (`min`, `max`, `lerp`, `clamp`, `atan2`).
  - `RemoteFloat`, `RemoteInt`, & `RemoteLong`: Mixed primitive `Float`/`Int` operators, `RemoteInt` math/bitwise helpers, `toRemoteStringWithPadding` / `toRemoteStringOptions`, `RemoteLong(Long)` constructor & `fromLowHigh`, `createReference`, `Hoist`, and `selectIf*` helpers.
- **Array & Scope Helpers**:
  - `RemoteFloatArray`, `RemoteMutableFloatArray`, `RemoteIntArray`, `RemoteStringArray`, and `RemoteState.withGlobalScope`.

### 5. Shaders, Vectors, Capture & Actions

- **`androidx.compose.remote.creation.compose.shaders`**:
  - `colorStops` overloads on `RemoteLinearGradient` (`linearGradient`, `horizontalGradient`, `verticalGradient`), `RemoteTileMode` overloads on `RemoteLinearGradient`, `RemoteRadialGradient`, `RemoteSweepGradient`, and `RemoteBrush.image`, plus `RemoteShader`.
- **`androidx.compose.remote.creation.compose.vector`**:
  - Animated vector APIs (`RemoteAnimatedVector`, `painterRemoteAnimatedVector`, `animatedVectorResource`) and `RemoteImageVector.Builder` `group` / `addPath`.
- **`androidx.compose.remote.creation.compose.capture`**:
  - `createProfile`, `CaptureUpdateThrottle`, and `DocumentStats`.
- **`androidx.compose.remote.creation.compose.action`**:
  - `ScrollAction` (`scrollBy`, `scrollTo`) and `LambdaAction`.

### 6. Keep Restricted (Cross-Module Boundary)

- Wire-serialization, capture-state, and cache-key APIs shared across RemoteCompose modules (`:compose:remote:remote-creation-expression`, `:compose:remote:remote-creation-compose`, `:compose:remote:remote-player-compose`, `:compose:remote:remote-testing`, `:wear:compose:remote:remote-material3`, and `:xr:glimmer:glimmer-remote`), including `RemoteComposeCreationState`, `RemoteComposeCaptureState`, `LocalRemoteComposeCreationState`, `RemoteStateScope` ID accessors (`id`, `floatId`, `longId`, `document`, `creationState`), `BaseRemoteState` ID methods, `RemoteStateCacheKey`, `RemoteStateVisitor`, `RemoteCanvas`, `RemoteAction.toRemoteAction()`, `RemoteModifier.toRecordingModifier`, and prebuilt binary bridges (`CapturedDocument`, `WriterEvents`).

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
