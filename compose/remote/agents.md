# Agent Instructions

When investigating this codebase or performing tasks, you MUST recursively search for and read all `.md` files in the project directories. These files contain critical architectural overviews, API conventions, and specialized guides that are essential for maintaining consistency and safety.

## Documentation Map

### Core Engine (`remote-core/doc/`)
- `REMOTE_COMPOSE_ARCHITECTURE.md`: High-level system design and data flow.
- `PROTOCOL_SPEC.md`: Binary format, opcodes, and field layouts.
- `DATA_FLOW.md`: Lifecycle from server creation to client playback.
- `EXPRESSION_ENGINE.md`: RPN logic, variable scoping, and math operations.

### Creation API (`remote-creation/doc/`)
- `MODIFIER_REGISTRY.md`: Mapping of DSL methods to underlying operations.
- `KOTLIN_DSL_PATTERNS.md`: Common snippets and best practices for Kotlin.
- `JAVA_PROCEDURAL_PATTERNS.md`: Usage guide for the Java `RemoteComposeWriter`.

### Specialized Subsystems (`remote-creation/doc/guides/`)
- `PARTICLE_SYSTEM_GUIDE.md`: Initializing and evolving high-performance particle systems.
- `PATH_EXPRESSION_GUIDE.md`: Algorithmic path generation (Cartesian & Polar).
- `LOOP_GUIDE.md`: Player-side repetition and dynamic indexing.
- `TOUCH_GUIDE.md`: Defining interactive variables and haptic feedback.
- `COMPONENTS_GUIDE.md`: Overview of layout systems.
- `PROCEDURAL_COMPONENTS_GUIDE.md`: Layout with Procedural components and Modifiers.
- `COMPOSE_COMPONENTS_GUIDE.md`: Layout with Compose-like components and Modifiers.
- `DRAW_TEXT_ANCHORED_GUIDE.md`: Precise text alignment and justification.
- `CREATION_DEMO_GUIDE.md`: How to write and register new procedural demos.

Always prioritize the patterns and mandates described in these documents over general assumptions.