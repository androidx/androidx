# Remote Compose Code Generator & Architecture

## Overview
The Remote Compose Code Generator (`:compose:remote:remote-generator`) is the authoritative tool
and single source of truth for the Remote Compose wire protocol format, schema definitions, and
multi-language AST/serialization code generation.

It generates typed ASTs, parsers, and serializers for:
- **Kotlin / Java**: [`:compose:remote:remote-document`](../remote-document) (read-only AST) and
  [`:compose:remote:remote-document-writer`](../remote-document-writer) (serialization)
- **C++**: Header-only embedded player definitions (`remote_compose_operations.h`)
- **Rust**: Embedded/Wasm `no_std` operations module (`remote_compose_operations.rs`)
- **Swift**: Apple platform client models (`RemoteComposeOperations.swift`)
- **TypeScript**: Web and Node player/editor bindings (`remoteComposeOperations.ts`)
- **Python**: Python tooling and pipeline bindings (`remote_compose.py`)

---

## Schema Source of Truth (`operations.json`)
The canonical schema defining all Remote Compose operations is located at:
[`src/main/resources/schema/operations.json`](src/main/resources/schema/operations.json)

Each operation entry specifies:
- **`id`**: Unique wire protocol opcode (1 byte: 0–255), strictly aligned with
  `androidx.compose.remote.core.Operations`.
- **`name`**: PascalCase operation name (e.g., `DrawCircle`, `BoxLayout`).
- **`category`**: Semantic classification (e.g., `Canvas Operations`, `Layout Operations`,
  `Document Structure`, `Data Operations`, `Modifiers`).
- **`description`**: Human-readable documentation for generated classes and methods.
- **`profiles`**: Array of profile tags indicating which capability profiles support the operation
  (e.g., `["baseline"]`, `["androidx", "widgets"]`, `["androidx", "widgets", "experimental"]`,
  `["deprecated"]`).
- **`fields`**: Ordered list of operation payload fields, including:
  - `name`: Property name (camelCase).
  - `type`: Wire type (`int`, `float`, `string`, `color`, `bytes`, `short`, `byte`, `long`,
    `boolean`, `float_or_id`).
  - `optional`: Whether the field is optional.
  - `default`: Default value when omitted.
  - `description`: Field documentation.
- **`isContainer` / `closedBy`**: Defines hierarchical container scopes (e.g., `BoxLayout` closed
  by `ContainerEnd`).

### Synchronization with `Operations.java`
The test `RemoteComposeGeneratorTest.testSchemaSynchronization` automatically validates that all
opcodes and definitions in `operations.json` stay in lock-step with `Operations.java` in
`:compose:remote:remote-core`.

If Java operations are added or modified, update the schema by running:
```bash
UPDATE_REMOTE_COMPOSE_SCHEMA=true PROJECT_PREFIX=:compose:remote: \
  ./gradlew :compose:remote:remote-generator:test --tests "*RemoteComposeGeneratorTest*"
```

---

## Read vs. Write Library Separation
In client architectures, players and viewers (such as embedded renderers, watchfaces, or display
controllers) only need to decode and evaluate binary documents. Including serialization and
document-writing logic incurs unnecessary binary size, dependency footprint, and memory overhead.

Conversely, authoring tools, visual editors, and compilers require full read-write capabilities.

The code generators support this separation cleanly:

### Kotlin
- **`:compose:remote:remote-document` (Read-Only AST)**:
  - Contains immutable data structures for documents (`RemoteDocument`), headers
    (`DocumentHeader`), and typed operation nodes (`OperationNode`, `DrawCircleNode`, etc.).
  - Contains decoding logic only (`RemoteDocument.load(bytes)`, `Node.read(buffer)`).
  - Contains no serializer or buffer-writing methods.
  - Minimal dependencies: depends only on `:compose:remote:remote-core` and standard annotations.
- **`:compose:remote:remote-document-writer` (Serialization)**:
  - Depends on `:compose:remote:remote-document`.
  - Provides serialization extension methods:
    - `RemoteDocument.save(): ByteArray`
    - `RemoteDocument.save(buffer: WireBuffer)`
    - `DocumentNode.write(buffer: WireBuffer)`
    - `OperationNode.write(buffer: WireBuffer)`
    - Concrete node extension methods (e.g., `DrawCircleNode.write(buffer)`).
  - Provides `RemoteDocumentSerializer` helper object.

### Java (`:compose:remote:remote-core`)
- Kotlin is banned in `:compose:remote:remote-core` to maintain zero Kotlin runtime overhead on
  minimal Java environments.
- `JavaGenerator` produces:
  - **Abstract Base Classes (`<OpName>Base.java`)**: Implements mechanical wire serialization
    (`apply`, `write`), deserialization (`read`), metadata (`documentation`), and JSON serialization
    (`serialize`). Hand-written operation classes subclass the base class to provide only custom
    layout and canvas painting logic (`paint(PaintContext)`), eliminating boilerplate with zero
    caller churn.
  - **Complete Data Operations**: Generates full classes for operations without custom execution
    behavior.
  - **Central Registration Tables (`GeneratedOperations.java`)**: OpCode constants and profile
    capability lookup methods.

### C++
Controlled via the preprocessor macro `RC_ENABLE_WRITER` (or legacy `RC_NO_WRITER`):
- When building reader-only runtimes (such as microcontrollers or display engines), pass
  `-DRC_ENABLE_WRITER=0` (or `-DRC_NO_WRITER`).
- All `write(WireBuffer&)` methods and `RemoteDocument::save(...)` are conditionally compiled using
  `#if defined(RC_ENABLE_WRITER) && RC_ENABLE_WRITER`.
- Default: When neither `RC_ENABLE_WRITER` nor `RC_NO_WRITER` is specified, `RC_ENABLE_WRITER`
  defaults to `1` for backwards compatibility.

### Rust
Controlled via Cargo features and `rustc --cfg` flags:
- Cargo feature: `writer` (included in `default` features) vs `reader_only`.
- Direct compiler flag: `--cfg rc_no_writer`.
- When writing is disabled, `Operation::write(...)` and `RemoteDocument::save(...)` are excluded
  from compilation via `#[cfg(...)]`.
- **Rust Player Support (`origami_core/remote_compose_core`)**:
  - `RustGenerator.generateOpCodesModule()` produces `op_codes.rs` with `OpId` in
    `SCREAMING_SNAKE_CASE` (e.g. `DRAW_CIRCLE = 46`, `LAYOUT_BOX = 202`), the
    `SUPPORTED_OPERATIONS` table, and profile bitmasks matching `origami_core` conventions directly.

### TypeScript / Swift / Python
- **TypeScript**: Web and Node player/editor bindings with `Uint8Array` / `DataView` `WireBuffer`.
- **Swift**: Apple platform client models adhering to `RemoteOperation` protocol with big-endian `WireBuffer`.
- **Python**: Idiomatic Python 3 dataclasses, `OpCode` `IntEnum`, big-endian `WireBuffer`, and `RemoteDocument` (`load`/`save`) for automation, tooling, and test pipelines.

---

## Profile-Based Operation Filtering (Binary Size Optimization)
Remote Compose defines capability profiles (see `Profile.java`, `SupportedOperationsProvider`, and
`Operations.getOperations(...)`):
- **`baseline`**: Core primitives (canvas drawing, basic math, container layouts, transforms).
  Required across all players.
- **`widgets`**: Extended text styling (`TextStyle`, `TextTransform`), dynamic lists, and
  widget-oriented operations.
- **`androidx`**: AndroidX-specific data operations (e.g., `ShaderData`, `FontData`).
- **`experimental`**: Experimental operations undergoing active development (e.g., `FlowLayout`,
  2D mesh drawing).
- **`deprecated`**: Deprecated operations maintained for backward compatibility.

Embedded platforms (e.g. MCUs, Wear OS watchfaces) often run with stringent flash (ROM) constraints
where stripping unused operations is essential.

### C++ Profile Macros
By default, all operations are enabled (`RC_PROFILE_ALL = 1`) when no profile macro is set to `1`.
Each profile macro is normalized to `0`/`1`, so passing `-DRC_PROFILE_EXPERIMENTAL=0` explicitly
disables that profile. To compile for a specific profile, pass one or more defines:
- `-DRC_PROFILE_BASELINE`: Compiles only baseline operations.
- `-DRC_PROFILE_WIDGETS`: Compiles baseline + widgets operations.
- `-DRC_PROFILE_ANDROIDX`: Compiles baseline + widgets + androidx operations.
- `-DRC_PROFILE_OEM`: Compiles baseline + OEM operations.
- `-DRC_PROFILE_EXPERIMENTAL`: Modifier enabling experimental operations for the active platform
  profile(s) (e.g., `-DRC_PROFILE_WIDGETS -DRC_PROFILE_EXPERIMENTAL`).
- `-DRC_PROFILE_DEPRECATED`: Modifier enabling deprecated operations for the active platform
  profile(s).

Each non-baseline operation class, factory case in `createOperation()`, and name in
`getOpCodeName()` is conditionally guarded:
```cpp
#if RC_PROFILE_ALL || (RC_PROFILE_ANDROIDX || RC_PROFILE_WIDGETS)
class TextStyle : public Operation { ... };
#endif
```

### Rust Profile Cfg Flags
By default, all operations compile when no profile flags or Cargo profile features are passed.
To compile for a specific profile, use Cargo features (`profile_baseline`, `profile_widgets`,
`profile_androidx`, `profile_oem`, `profile_experimental`, `profile_deprecated`, `profile_all`) or
direct `rustc --cfg` flags (`rc_profile_baseline`, `rc_profile_widgets`, `rc_profile_androidx`,
`rc_profile_oem`, `rc_profile_experimental`, `rc_profile_deprecated`, `rc_profile_all`):
- Cargo: `cargo build --no-default-features --features "writer,profile_widgets"`
- Rustc: `rustc --cfg rc_profile_widgets`
- Experimental operations combine a platform profile and modifier (matching `RcProfiles`), e.g.
  `--cfg rc_profile_widgets --cfg rc_profile_experimental`.

Generated structs, enum variants, and deserialization match arms are guarded via:
```rust
#[cfg(any(feature = "profile_all", rc_profile_all, not(any(feature = "profile_baseline", feature = "profile_widgets", feature = "profile_androidx", feature = "profile_oem", feature = "profile_experimental", feature = "profile_deprecated", rc_profile_baseline, rc_profile_widgets, rc_profile_androidx, rc_profile_oem, rc_profile_experimental, rc_profile_deprecated)), any(feature = "profile_androidx", rc_profile_androidx, feature = "profile_widgets", rc_profile_widgets)))]
pub struct TextStyle { ... }
```

---

## C++ Zero-Copy Memory Architecture (`BufferView` & `std::string_view`)
Remote Compose documents frequently transport non-trivial assets such as embedded bitmap payloads
(`BitmapData`), raw font assets (`FontData`), audio clips (`SoundData`), and localized text strings
(`TextData`, `TextMerge`, etc.).

In performance-sensitive and memory-constrained C++ environments (such as embedded watchfaces,
display controllers, and high-frequency UI rendering pipelines), copying large byte arrays or
constructing heap-allocated `std::string` objects during wire decoding causes substantial heap
fragmentation, cache thrashing, and decode latency.

To guarantee zero heap allocations and $O(1)$ decoding for payload-heavy operations, the C++
generator implements a non-owning zero-copy view model.

### Non-Owning View Types
1. **`std::string_view` for Strings**:
   All string fields in generated operations (e.g., `TextData::text`) are represented as
   `std::string_view`. During decoding, `buffer.readStringView()` returns a view pointing directly
   into the contiguous UTF-8 byte payload inside the `WireBuffer`, without allocating heap storage.

2. **`BufferView` for Binary Payloads**:
   All binary fields (`"type": "bytes"` in `operations.json`, such as `BitmapData::bitmap`,
   `FontData::fontData`, and `SoundData::data`) are typed as `BufferView`.

   `BufferView` is a lightweight, non-owning span:
   ```cpp
   class BufferView {
   public:
       constexpr BufferView() = default;
       constexpr BufferView(const uint8_t* data, size_t size);

       constexpr const uint8_t* data() const noexcept;
       constexpr size_t size() const noexcept;
       constexpr bool empty() const noexcept;
       constexpr const uint8_t* begin() const noexcept;
       constexpr const uint8_t* end() const noexcept;
       constexpr const uint8_t& operator[](size_t index) const;

       bool operator==(const BufferView& other) const;
       bool operator!=(const BufferView& other) const;

       // Explicit conversion to owning heap vector if persistence is needed
       std::vector<uint8_t> toVector() const;
   };
   ```

### Zero-Copy `WireBuffer` Decoding
When reading documents from external memory (such as memory-mapped files, read-only flash, or
network packet buffers), `WireBuffer` can be constructed in non-owning mode:
```cpp
// Non-owning constructor: borrows external memory without copying
WireBuffer buffer(rawMemoryPointer, rawLength, /* copy = */ false);
assert(!buffer.isOwning());

// Loads document; all string and buffer fields point directly into rawMemoryPointer
auto document = RemoteDocument::load(buffer);
```

### Lifetime Contract & Caller Responsibilities
> [!WARNING]
> Operations containing `BufferView` or `std::string_view` do **not** own their underlying memory.
> The memory slice is borrowed directly from the backing `WireBuffer` (or the caller's memory
> block).

- **Validity Requirement**: The backing buffer (or memory-mapped region) **MUST** remain allocated,
  valid, and unmodified for the entire lifetime of the `RemoteDocument` and its operations.
- **Cache Persistence**: If an operation or document must outlive the ephemeral decode buffer (for
  example, when caching an image texture or font across multiple render frames), callers must
  explicitly promote the non-owning views to owned memory:
  ```cpp
  auto bitmapOp = std::dynamic_pointer_cast<BitmapData>(op);
  if (bitmapOp) {
      // Create persistent copy if required
      std::vector<uint8_t> persistentBytes = bitmapOp->bitmap.toVector();
  }
  ```

### `BitmapData` Decoding Helpers
`BitmapData` packages dimensions and formats into packed 32-bit fields to minimize wire overhead.
The generated C++ class provides convenient inline accessors:
- `bitmapOp->getWidth()`: Returns low 16 bits of `widthAndType`.
- `bitmapOp->getType()`: Returns high 16 bits of `widthAndType`.
- `bitmapOp->getHeight()`: Returns low 16 bits of `heightAndEncoding`.
- `bitmapOp->getEncoding()`: Returns high 16 bits of `heightAndEncoding`.

---

## Public Generator API (`RemoteSerializationCodeGenerator`)
External build tools and host tasks can invoke `RemoteSerializationCodeGenerator.generate(language, outputDirectory)` directly from Java or Kotlin:

```java
RemoteSerializationCodeGenerator.generate(RemoteSerializationCodeGenerator.LANGUAGE_CPP, outputDir);
```

Supported target constants:
- `RemoteSerializationCodeGenerator.LANGUAGE_CPP`: emits `WireBuffer.h`, `WireBuffer.cpp`, `RemoteComposeOps.h`, and `RemoteComposeOps.cpp`.
- `RemoteSerializationCodeGenerator.LANGUAGE_RUST`: emits `remote_compose_operations.rs` and `op_codes.rs`.
- `RemoteSerializationCodeGenerator.LANGUAGE_PYTHON`: emits `remote_compose.py`.
- `RemoteSerializationCodeGenerator.LANGUAGE_SWIFT`: emits `RemoteComposeOperations.swift`.
- `RemoteSerializationCodeGenerator.LANGUAGE_TYPESCRIPT`: emits `remoteComposeOperations.ts`.

---

## Regenerating AndroidX Checked-In Code
After making edits to `operations.json` or generator templates:

1. **Regenerate AndroidX Kotlin & Java Sources**:
   ```bash
   PROJECT_PREFIX=:compose:remote: ./gradlew :compose:remote:remote-generator:updateGeneratedCode
   ```

2. **Run Tests & Verify Build**:
   ```bash
   PROJECT_PREFIX=:compose:remote: ./gradlew :compose:remote:remote-document:test \
     :compose:remote:remote-document-writer:test :compose:remote:remote-generator:test \
     :compose:remote:remote-core:test
   ```

3. **Format Code**:
   ```bash
   PROJECT_PREFIX=:compose:remote: ./gradlew :compose:remote:remote-core:javaFormat
   ```

4. **Update API Signatures** (if public APIs changed):
   ```bash
   PROJECT_PREFIX=:compose:remote: ./gradlew :compose:remote:remote-generator:updateApi \
     :compose:remote:remote-document:updateApi :compose:remote:remote-document-writer:updateApi
   ```
