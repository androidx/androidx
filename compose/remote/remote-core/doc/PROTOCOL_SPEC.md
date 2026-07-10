# RemoteCompose Protocol Specification

This document defines some of the binary protocol format for RemoteCompose.
Each operation consists of an OpCode followed by its specific data fields. 
This is not authoritative but is provided to give a feel for the protocol.

## OpCode Reference

| OpCode | Name | Purpose | Fields |
| :--- | :--- | :--- | :--- |
| 0 | `HEADER` | Document metadata | Version, Width, Height, Density, Profiles |
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
- **Int**: 32-bit signed (Little Endian).
- **Float**: 32-bit IEEE 754.
- **Short**: 16-bit signed.
- **Byte**: 8-bit unsigned.
- **String**: UTF-8 encoded, prefixed by length (Int).
- **Float ID**: Encoded as a `NaN` where the significand contains the 24-bit ID.
