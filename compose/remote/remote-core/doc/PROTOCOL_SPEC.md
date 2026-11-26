# RemoteCompose Protocol Specification

This document defines some of the binary protocol format for RemoteCompose.
Each operation consists of an OpCode followed by its specific data fields.
This is not authoritative but is provided to give a feel for the protocol.

## OpCode Reference

| OpCode | Name | Purpose | Fields |
| :--- | :--- | :--- | :--- |
| 0 | `HEADER` | Document metadata | Version, Width, Height, Density, Profiles, Compression, Scroll directions |
| 38 | `CLIP_PATH` | Set clip path | Path ID |
| 39 | `CLIP_RECT` | Set clip rectangle | L, T, R, B |
| 40 | `PAINT_VALUES` | Update paint state | Serialized `PaintBundle` |
| 42 | `DRAW_RECT` | Draw rectangle | L, T, R, B |
| 43 | `DRAW_TEXT_RUN` | Draw text run | Text ID, start, end, x, y, RTL |
| 44 | `DRAW_BITMAP` | Draw image | Bitmap ID, L, T, R, B |
| 46 | `DRAW_CIRCLE` | Draw circle | cx, cy, radius |
| 47 | `DRAW_LINE` | Draw line | x1, y1, x2, y2 |
| 51 | `DRAW_ROUND_RECT` | Draw rounded rect | L, T, R, B, rx, ry |
| 52 | `DRAW_SECTOR` | Draw pie slice | L, T, R, B, start, sweep |
| 53 | `DRAW_TEXT_ON_PATH`| Text along path | Text ID, Path ID, hOffset, vOffset |
| 63 | `THEME` | Set active theme | Theme ID (Light/Dark/Any) |
| 80 | `DATA_FLOAT` | Define constant | Float ID, value |
| 81 | `ANIMATED_FLOAT` | Dynamic expression | Float ID, RPN operations |
| 101 | `DATA_BITMAP` | Serialized image | Bitmap ID, width, height, pixels |
| 102 | `DATA_TEXT` | Serialized string | Text ID, string content |
| 109 | `MODIFIER_CLIP_PATH` | Clip component to path (experimental) | Path ID |
| 123 | `DATA_PATH` | Serialized path | Path ID, points/commands |
| 124 | `DRAW_PATH` | Draw vector path | Path ID |
| 126-129 | `MATRIX_*` | Transformations | Scale, Translate, Skew, Rotate |
| 130-131 | `MATRIX_SAVE/RESTORE`| Pushes/Pops matrix | - |
| 133 | `DRAW_TEXT_ANCHOR` | Aligned text | Text ID, x, y, panX, panY, flags |
| 135 | `TEXT_FROM_FLOAT` | Float to String | Text ID, value ID, formatting |
| 139 | `DRAW_CONTENT` | Component content | Used in Canvas/Draw Content |
| 144 | `INTEGER_EXPRESSION`| Math on Ints | Int ID, mask, operations |
| 157 | `TOUCH_EXPRESSION` | Input mapping | Def, min, max, mode, easing, RPN |
| 161 | `PARTICLE_DEFINE` | Particle system | ID, var IDs, count, init equations |
| 163 | `PARTICLE_LOOP` | Particle update | ID, restart eq, update equations |
| 178 | `CONDITIONAL_OPS` | If block | Condition type, a, b, children... |
| 193 | `PATH_EXPRESSION` | Algorithmic path | Path ID, range, X eq, Y eq |
| 201 | `COMPONENT_START` | UI Node | Component ID, layout type, flags |
| 230 | `CONTAINER_END` | Closes block | - |

## Data Types
- **Int**: 32-bit signed (Big Endian).
- **Float**: 32-bit IEEE 754.
- **Short**: 16-bit signed.
- **Byte**: 8-bit unsigned.
- **String**: UTF-8 encoded, prefixed by length (Int).
- **Float ID**: Encoded as a `NaN` where the significand contains the 24-bit ID.

## Compression

A document with a properties header (API level 7+) can set the `COMPRESS`
property (key 31, Int). With the value `1`, the header is followed by the
marker byte `0x77` (reserved opcode 119) and then a single zlib stream
(DEFLATE, RFC 1950) holding the operations. Other non-zero values are
reserved and rejected.

- The header itself is never compressed, so tools can peek at it (version,
  size, profiles, compression) without inflating anything.
- Players inflate the stream, capped at `Limits.MAX_DECOMPRESSED_SIZE` bytes,
  and parse the result as the operations following the header. The in-memory
  document drops the `COMPRESS` property, so it matches the uncompressed
  original byte for byte.
- Writers compress API level 8+ documents when their `COMPRESS` header tag asks
  for it. `Header.compressDocument` and `Header.decompressDocument` convert
  existing documents.

See [COMPRESSION.md](COMPRESSION.md) for the full guide.

## Scroll directions

A document with a properties header (API level 7+) can declare the directions
it scrolls in with the `DOC_SCROLL` property (key 32, Int): `1`
(`SCROLL_HORIZONTAL`), `2` (`SCROLL_VERTICAL`), or `3` for both. Other bits
are reserved: writers reject them and players ignore them.

- `CoreDocument.hasHorizontalScroll()` and `hasVerticalScroll()`, which back
  `RemoteComposePlayer.isHorizontallyScrollable()` and
  `isVerticallyScrollable()`, report the declared directions on top of the ones
  they find in the document: the `RootContentBehavior` scroll mode, scrollable
  components and scroll modifiers.
- The property can only add directions, for scrolling those checks can't find,
  such as a scroll driven by a touch expression. A document with a vertical
  scroll container reports vertical scrolling whatever the property says.
- Hosts can peek at the property in the header, even in a compressed document,
  without inflating the operations. Players that predate it ignore it.
- Writers take a `DOC_SCROLL` header tag. JSON documents take
  `"scroll": "horizontal"`, `"vertical"` or `"both"`.

## Unified pointer input

A document with a properties header (API level 7+) can turn on unified pointer
input with the `FEATURE_UNIFIED_POINTER_INPUT` property (key 33, Int): `0` is
off (the default) and `1` is on.

- The wire format of the operations does not change. The property only chooses
  which classes the player creates for the operations that handle touches, and
  how the player sends touch events to them.
- It is experimental. Players that predate it ignore it.
- Writers take a `FEATURE_UNIFIED_POINTER_INPUT` header tag. JSON documents
  take `"unifiedPointerInput": 1`.

See [POINTER_INPUT.md](POINTER_INPUT.md) for the full guide.

## 3D

The 3D operations (opcodes 110–118 and 120, in the experimental profiles) are specified in
[PROTOCOL_3D.md](PROTOCOL_3D.md).
