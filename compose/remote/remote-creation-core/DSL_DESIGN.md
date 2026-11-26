# RemoteCompose DSL — Design Principles

This document captures the *why* behind the Kotlin DSL in `androidx.compose.remote.creation.dsl`. The DSL is a typed Kotlin facade over the legacy `RemoteComposeWriter` / `RcPaint` / `RemoteComposeShader` Java builders. Every choice here exists because of something the legacy API made too easy to get wrong.

The DSL package is currently `@RestrictTo(LIBRARY_GROUP)` and may evolve.

---

## 1. Type safety over raw primitives

The legacy Java API uses raw `Int` for almost everything: color values, resource IDs, paint opcodes, flag combinations, skip-block offsets, component IDs. Two unrelated ints are interchangeable at the call site, which makes accidental swaps invisible.

The DSL replaces these with **value classes** (`@JvmInline`) that carry the same wire-level int but cannot be mixed:

- `RcText`, `RcImage`, `RcShader`, `RcTextList` — resource handles (`RcTypes.kt:34-84`)
- `RcColor`, `RcColorValue` — color references
- `RcBool` — boolean reference, *deliberately not* `RcInteger`, "so that `boolean → numeric` arithmetic doesn't compile silently" (`RcTypes.kt:864`)
- `RcSkipToken` — opaque handle returned by `beginSkip` and consumed by `endSkip`, "preventing accidentally swapping skip-block boundaries" (`RcTypes.kt:877-883`)
- `RcComponentId` — "prevents arbitrary ints from being used as component IDs and keeps the IDs traceable" (`RcTypes.kt:885-891`)
- `RcWeight` — typed font weight added alongside the legacy `RcFontWeight` float constants

Value-class wrapping has zero heap cost in most cases and the constructors are `internal`, so callers cannot fabricate a handle — they must go through factory functions on `RcScope`.

**Trade-off acknowledged in code:** `RcColor.id` and `RcColorValue.id` are `public val` rather than `internal`, because the legacy Java `RcPaint.setColor(...)` (one package up) reads them via `getId()` and that API is add-only. See the KDoc at `RcTypes.kt:41-53` — the workaround attempts and the JetBrains issue link are documented inline. These are the only "B4 holdouts."

## 2. Idiomatic Kotlin naming, not Java translation

Where the underlying Java API uses `setX(...)` and raw opcodes, the DSL uses property-like names and typed enums:

> *"Each method takes typed value classes / enums (`RcColor`, `RcPaintStyle`, `RcStrokeCap`, `RcBlendMode`, etc.) instead of raw `Int` opcodes. Method names drop the Java-style `set` prefix for a more idiomatic Kotlin DSL feel."* — `RcPaintScope.kt:27-30`

Compose users expect `color(...)`, `strokeWidth(...)`, `style(...)` — they get exactly that, with full overload resolution for `Int`, `Long`, `RcColor`, and `RcColorValue` variants (`RcPaintScope.kt:52-62`).

Extension property suffixes provide unit-safe lightweight conversions: `16.rsp`, `8f.rdp`, `true.rb`, `0xFF112233.rcColor()`. These read naturally inside a builder and disappear at compile time.

## 3. Reactive value types as first-class citizens

A RemoteCompose document is replayed and animated on the player side; many values are not numbers but *expressions* that the player evaluates per frame. The DSL exposes four reactive types:

- **`RcFloat`** — full arithmetic (`+`, `-`, `*`, `/`, `%`), transcendental (`sin`, `cos`, `sqrt`, `pow`, …), interpolation (`lerp`, `smoothStep`, `pingPong`), array reductions. ~880 lines of operator overloads in `RcFloat.kt`.
- **`RcInteger`** — arithmetic plus bitwise (`RcTypes.kt:126-186`). Carries an optional `RemoteComposeWriter` reference so derived expressions can route back through the writer; constructed-without-writer ints fail fast (`RcTypes.kt:104-120`).
- **`RcBool`** — boolean handle (`RcTypes.kt:864-871`).
- **`RcColor` / `RcColorValue`** — color expressions.

The reactive nature is invisible at the call site: writing `position + velocity * deltaTime` builds a wire expression rather than evaluating eagerly. NaN-encoding is used internally to ferry IDs alongside float-shaped values (`RcTypes.kt:37`, `RcFloat.kt:37`); callers do not need to know.

## 4. Compose-shaped composition model

The DSL mirrors `@Composable` syntax so Compose developers can pattern-match it:

- **Scoped builders** with `@DslMarker` (`RcDslMarker`, `RcScope.kt:30`) — Box / Column / Row / Flow / Text / Image / Canvas all take a trailing-lambda content block typed to a child scope.
- **Per-scope helpers** — `RcColumnScope.weight(...)`, `RcRowScope.weight(...)`, `RcFlowScope`, `RcCollapsibleColumnScope`, etc. Only valid in the parent layout.
- **Modifier chains** — `Modifier.padding(8.rdp).size(120.rdp).onClick { … }` with the same `.then(...)` algebra and `companion object Modifier` empty element as Jetpack Compose (`Modifier.kt:33-58`).
- **Positioning** as a sealed-interface hierarchy (`RcPositioning.kt`) — `RcHorizontalPositioning.Start`, `RcRowHorizontalPositioning.SpaceBetween`, etc. — so only positionings valid in a given layout type are accepted.

If you can write Compose UI, you can read RemoteCompose DSL.

## 5. Typed flag sets and packed specs

Bitfields are the worst case for raw-int APIs: a typo is a silent corruption. The DSL distinguishes two cases:

- **Independent bits** (OR-able) — `RcTextAnchorFlags`, `RcMatrixFromPathFlags`. Each provides an `infix or` and `operator contains` (`RcTypes.kt:701-735`), with named companions for every flag.
- **Mutually-exclusive sub-fields packed into one int** — `RcTextFromFloatSpec`. Raw `or` of arbitrary constants "would silently corrupt the sub-field encoding" (`RcTypes.kt:758`), so the public surface is **only** the `of(padAfter=..., padPre=..., grouping=..., ...)` factory with typed sub-enums (`RcTypes.kt:836-852`). Wrong combinations cannot be expressed.

The principle: if a bit pattern has structure, the structure goes in the type system, not in a comment.

## 6. Wrapped builders + `raw` escape hatch

Each scope (`RcPaintScope`, `RcShaderScope`) wraps an underlying mutable Java builder and exposes it via a `public val raw: ...` property. The KDocs explicitly call this out:

> *"For paint methods not yet wrapped (gradients, axes, path effects, etc.) use the [raw] escape hatch to access the underlying [RcPaint] builder directly."* — `RcPaintScope.kt:40-41`

This is deliberate: the DSL prioritizes covering common cases idiomatically and lets advanced users reach through for anything exotic. The escape hatch keeps the DSL viable while it grows — a missing wrapper is never a blocker.

The dispatch happens at the type, not via reflection or `Any`. `RcShaderScope.uniform(name, value)` picks `setIntUniform` / `setFloatUniform` / `setBitmapUniform` by overload resolution (`RcShaderScope.kt:30`). "No reflection, no `Any`."

## 7. Add-only legacy boundary

The Java API in the packages above `/dsl/` is not intended to be used by AndroidX's add-only API contract. The DSL is built **alongside** it, not as a replacement. Concrete consequences visible in the code:

- The DSL never deletes a legacy class; it wraps or supersedes.
- Backward-compatible doubles exist where the new type-safe form is preferred but the old form remains valid: `remoteBoolean(Boolean): RcInteger` (legacy) coexists with `remoteBool(Boolean): RcBool` (typed). KDoc says "Prefer this" rather than `@Deprecated`, because the legacy form is still public API (`RcScope.kt:902-907`).
- The "B4 holdouts" mentioned in `RcTypes.kt:46-53` document specific points where the add-only constraint blocks an otherwise-cleaner design, with a JetBrains issue link explaining the language limitation that prevents a private-back-end workaround.

When the legacy boundary opens, the holdouts close. Until then, they are documented in place.

## 8. Document-as-binary builder

The DSL produces a serialized byte buffer, not a live object tree. The top-level entry points (`RcDocCreator.kt`):

```kotlin
val bytes: ByteArray = createRcBuffer(profile, *tags, experimental = ...) {
    // RcScope receiver — full DSL available here
    Column { Text("hello") }
}
```

`createRcBuffer` wraps the content in a root layout; `createRawRcBuffer` does not. Both produce a `ByteArray` ready for the player. There is no other shared mutable state — every builder operation drives the writer forward and immediately commits bytes.

This shape is why `RcInteger` carries an optional writer reference and why `RcFloat` arithmetic is allowed to be lazy: the document is the source of truth, not a Kotlin object graph.

## 9. Fail-fast on misuse, not on use

Type safety catches most mistakes at compile time. Where it cannot, the DSL prefers a loud runtime error over silent wrong behavior:

- `RcInteger` arithmetic without a writer throws `"RcInteger arithmetic requires a writer; use remoteInteger(...) inside an RcScope."` (`RcTypes.kt:115-120`).
- `RcSkipToken` cannot be constructed externally, so `endSkip(token)` is structurally well-formed.
- `RcPoint` literal extraction returns `NaN` for non-literal expressions, and the limitation is documented inline (`RcShaderScope.kt:115-120`) rather than hidden.

The principle: nothing should be quietly tolerated that the player will later interpret as garbage.

---

## What's intentionally **not** in scope

- **Replacing the legacy Java builders.** They remain the supported wire-encoder. The DSL is the recommended *creation* surface; the writer remains the runtime contract.
- **Hiding the underlying buffer model.** `raw` access is a feature, not a leak.
- **Reaching parity with every legacy operation in one pass.** Common cases first; the escape hatch covers the long tail until wrappers catch up.
- **Mirroring Jetpack Compose's `@Composable` runtime.** The DSL borrows Compose's *call shapes* but not its recomposition model — the document is built once and replayed by the player.

---

## Test-as-spec

The `src/test/.../dsl/` suite is structured as a specification of the DSL's surface, not just regression coverage:

- `RcTypedOverloadsTest` — proves the typed enum / value-class overloads resolve to the correct legacy opcodes (the contract).
- `RcFlagSetsTest` — proves `or` / `contains` semantics on flag sets.
- `RcIntegerArithmeticTest`, `RcFloatTest` — prove operator overloads produce the right expression trees.
- `RcPaintScopeTest`, `RcShaderScopeTest`, `RcActionScopeTest` — prove each scoped builder dispatches to its underlying mutable builder.

If you change the DSL surface, the tests should change first.
