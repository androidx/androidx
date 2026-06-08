# `DRAW_MESH_3D` mode word: the flag contract

---

## 1. Problem

`DRAW_MESH_3D(meshId, mode)` is the one 3D operation whose `mode` word has to serve every engine,
from a software rasterizer to Filament. Today the word mixes three different kinds of information:

| Kind | Bits | Portable across players? |
| :--- | :--- | :--- |
| *What the picture must look like* (`SMOOTH`, `WIREFRAME`, `TRANSPARENT`, `DOUBLE_SIDED`, `FRONT_CW`, `PBR`) | 0, 8–12, 14–16 | Yes |
| *Which Android engine should draw it* (software / canvas / drawMesh / gl / z-buffer variants, `GPU`) | 1–3, 13 | No: an Android implementation detail in a cross-platform format |
| *Versioning* (reserved bits, the advanced bit) | 17–31 | Policy |

## 2. Principles

1. **The software renderer is the reference.** A flag is defined by what the reference draws, or,
   for a feature the reference doesn't implement, by the *fallback* the reference draws instead.
   An engine conforms if its output matches the reference within the tolerance of its class (§9).
2. **Documents state intent, never engines.** A document says what the picture must be
   (semantic flags), what it *requires* a player to implement, and what it would *prefer* if
   available. Engine choice is player policy: the best engine the player has that satisfies the
   document's requirements.
3. **A wrong picture is worse than no picture.** A player that cannot honour a *required* flag
   MUST refuse the document, exactly as it refuses an unknown opcode. It MUST NOT guess.
4. **The class of a flag is fixed by its bit position.** Whether an unknown flag is ignorable or
   required is decided by which range it lives in, so every player enforces the rule with one
   mask, without knowing the flag. New flags never need a coordinated player release just to be
   *safe*; they need one only to be *supported*.
5. **Additive growth only.** Nothing in bits 0–16 changes. Reserved bits that no shipped
   document sets (the writers have always refused 17–31) may be given new rules now; this gets
   less true every month, so the rules in §5 should land soon.

## 3. The mode word

```
 31 30 29 ........ 24 23 ........ 17 16 15 14 13 12 11 10  9  8  7  6  5  4  3  2  1  0
 ┌──┬──┬─────────────┬─────────────┬──┬──┬──┬──┬──┬──┬──┬──┬──┬───────────┬────────┬──┐
 │R │A │  REQUIRED   │  OPTIONAL   │CW│DS│PB│GP│TR│E2│E1│E0│WF│ reserved  │ engine │SM│
 └──┴──┴─────────────┴─────────────┴──┴──┴──┴──┴──┴──┴──┴──┴──┴───────────┴────────┴──┘
```

| Bits | Class | Name | Reader rule when the player doesn't implement it |
| :--- | :--- | :--- | :--- |
| 0 | Semantic | `SMOOTH` | n/a: every player implements the semantic flags (§4) |
| 1–3 | Legacy hint | engine hint | Ignore (§6) |
| 4–7 | Reserved | | Writers write 0. Readers MUST ignore. |
| 8 | Semantic | `WIREFRAME` | n/a |
| 9–11 | Semantic | `WIRE_EDGE0..2` | n/a |
| 12 | Semantic | `TRANSPARENT` | n/a |
| 13 | Legacy hint | `GPU` | Ignore (§6) |
| 14 | Semantic | `PBR` | n/a (has a defined fallback, §4) |
| 15 | Semantic | `DOUBLE_SIDED` | n/a |
| 16 | Semantic | `FRONT_CW` | n/a |
| 17–23 | **Optional** | assigned in §7 | **Ignore.** Result may be less refined, never wrong. |
| 24–29 | **Required** | assigned in §7 | **Refuse the document.** |
| 30 | Advanced | `ADVANCED` | **Refuse the document.** Layout of the operation differs (PROTOCOL_3D.md §7.1). |
| 31 | Reserved | | Writers write 0. Readers MUST refuse if set. |

Constants, in every player:

```
MODE_OPTIONAL_MASK  = 0x00FE0000   // bits 17–23
MODE_REQUIRED_MASK  = 0x3F000000   // bits 24–29
MODE_ADVANCED       = 0x40000000   // bit 30
MODE_RESERVED_MASK  = 0x800000F0   // bits 4–7 and 31
```

## 4. Semantic flags (Normative)

These are the flags every conformant player MUST implement. Each has a reference definition and,
where the reference can't do the "real" thing, a fallback that *is* the definition for the
reference class. All of these keep their current meaning; this section only restates them in
engine-neutral terms. Section numbers refer to PROTOCOL_3D.md.

| Flag | Reference behaviour | Engine notes |
| :--- | :--- | :--- |
| `SMOOTH` (bit 0) | 1: Gouraud, lit per vertex with the mesh normals. 0: flat, one colour per triangle from the face normal. A mesh without normals is flat in either case (§4.3). | A per-pixel-lit engine (WebGL, Filament) MAY light per pixel for `SMOOTH`; that is within tolerance (§9). It MUST still render flat as flat. |
| `WIREFRAME` (bit 8) | Hidden-line wireframe: faces write depth without colour, then edges are drawn as depth-tested unlit lines in the paint colour (§6.6). | An engine without hidden-line support MUST hand the draw to an engine that has it (in practice the software reference). "Forces the software engine" in §6.6 becomes this rule; the document doesn't name the engine. |
| `WIRE_EDGE0..2` (bits 9–11) | Which edges of each triangle to draw (v0→v1, v1→v2, v2→v0). None set = all. Meaningful only with `WIREFRAME`; otherwise ignored. | |
| `TRANSPARENT` (bit 12) | Depth test on, depth write off, src-over blend; alpha = paint alpha × texel alpha. The document orders transparent draws back to front (§6.7). | Order-independent transparency is NOT required and, if used, MUST produce the same result for correctly ordered documents. |
| `PBR` (bit 14) | The material is PBR. The reference has no PBR: it renders the Blinn-Phong base material and this flag changes nothing (§6.2 rule 2). | A PBR engine applies §6.2 rule 3. A PBR engine MUST still render a non-`PBR` draw with Blinn-Phong-equivalent shading. |
| `DOUBLE_SIDED` (bit 15) | No back-face culling; back faces lit with the reversed normal (§6.7). | |
| `FRONT_CW` (bit 16) | Clockwise triangles are front-facing (`glFrontFace(GL_CW)`); culling, the `DOUBLE_SIDED` reversal and flat face normals follow. Infinite planes ignore it. | |

`PBR` is a semantic flag with an explicit fallback: that is why it is *not* a required flag. The
general rule is in §7: a flag belongs in the semantic/optional space only if its fallback is
defined and acceptable; otherwise it is required.

## 5. Flag classes and the reader algorithm (Normative)

### 5.1 Reader

On reading `DRAW_MESH_3D`, before anything else:

```
if (mode & MODE_ADVANCED) != 0:                   refuse(document, "advanced form not supported")
if (mode & MODE_RESERVED_MASK & 0x80000000) != 0: refuse(document, "reserved bit 31 set")
unknownRequired = mode & MODE_REQUIRED_MASK & ~SUPPORTED_REQUIRED
if unknownRequired != 0:                           refuse(document, "requires 3D feature(s) " + hex(unknownRequired))
// bits 4–7 and unknown optional bits: ignore
```

`SUPPORTED_REQUIRED` is the set of required flags this player implements. It is a compile-time
constant of the player (or of the engine the player will use, see §8).

"Refuse" means: the document fails to load, with an error that names the operation and the
flags, through the same path as an unknown opcode. No operation of the document is painted. A
player MUST NOT draw the rest of the document and skip the offending draw.

The check happens at read (inflate) time, not at paint time, so the failure is deterministic and
happens once.

### 5.2 Writer

A writer MUST refuse a mode that sets:
- `ADVANCED` or bit 31 (no advanced form is defined);
- any bit in 4–7;
- any *unassigned* bit in 17–29 (§7 lists the assigned ones).

A writer MAY write any assigned optional or required bit. It is the document author's choice to
require a feature; the writer doesn't second-guess it.

### 5.3 Optional versus required: the one question

When a new flag is proposed, ask: **if a player silently drops this flag, is the result *wrong*,
or merely *less refined*?**

- *Less refined* (anti-aliasing, mipmapping, anisotropic filtering, "receive shadows if you have
  them", level-of-detail hints): **optional**, bits 17–23.
- *Wrong* (a feature whose absence changes what the picture depicts: shadows the layout depends
  on, environment lighting that defines the look, clipping planes, instancing, per-vertex colours
  that carry data): **required**, bits 24–29.

A flag whose fallback can be *defined* precisely enough to be the reference behaviour (like
`PBR`) is better specified as a semantic flag with that fallback, in the optional range; the
reference then implements the fallback and the flag is never "unknown".

## 6. Legacy engine hint (bits 1–3 and 13) (Normative)

These bits exist in shipped documents and keep their encoding. Their *meaning* is narrowed:

1. The engine hint `(mode >> 1) & 7` and `GPU` (bit 13) are **preferences from the Android
   reference player's history**: 0 software, 1 canvas, 2 drawmesh, 3 gl, 4 drawmesh-zbuf,
   5 canvas-zbuf, 6–7 reserved. They name Android implementation strategies.
2. Any player MAY ignore them. A non-Android player SHOULD ignore them. The Android player MAY
   honour them when the named engine satisfies every flag of the draw and the rules of
   PROTOCOL_3D.md, and MUST NOT honour them otherwise (this is §4.2 rule 6 today).
3. New documents SHOULD write 0 for bits 1–3 and express a performance preference, if any, as
   `GPU` only. Writers keep accepting all values for compatibility.
4. No new engine value will be assigned. Values 6 and 7 stay reserved forever. A player MUST
   treat them as 0.
5. The "one engine per scene" rule (§4.2 rule 5) becomes a rule about the *player's* choice: a
   player SHOULD draw all the draws of a scene with one engine so they share a depth buffer. The
   document can no longer violate it.

Engine selection is otherwise **player policy** (§8).

## 7. Flag registry (Normative)

The only place a flag in 17–29 gets its meaning. Players implement a flag by its number from this
table; the name is for humans. Adding a row is a change to this document, reviewed by the owners
of all three players. A row is never removed or renumbered; a withdrawn flag is marked
*deprecated* and its bit stays reserved.

### 7.1 Optional flags (17–23): ignorable

| Bit | Name | Meaning when implemented | Fallback when ignored | Status |
| :--- | :--- | :--- | :--- | :--- |
| 17 | | | | unassigned |
| 18 | | | | unassigned |
| 19 | | | | unassigned |
| 20 | | | | unassigned |
| 21 | | | | unassigned |
| 22 | | | | unassigned |
| 23 | | | | unassigned |

### 7.2 Required flags (24–29): refuse if unimplemented

| Bit | Name | Meaning | Conformance test | Status |
| :--- | :--- | :--- | :--- | :--- |
| 24 | | | | unassigned |
| 25 | | | | unassigned |
| 26 | | | | unassigned |
| 27 | | | | unassigned |
| 28 | | | | unassigned |
| 29 | | | | unassigned |

### 7.3 Candidates (Informative, not assigned)

Listed so that implementers can see the intended shape; none of these is a flag until it has a
row above with a full definition and a conformance test.

| Candidate | Likely class | Why |
| :--- | :--- | :--- |
| Anti-aliased edges / MSAA preference | optional | Aliased output is less refined, not wrong. |
| Texture mipmapping / trilinear | optional | Same. |
| Receive shadows if available | optional | A document that merely looks nicer with shadows. |
| Cast + receive shadows, required | required | A document whose composition depends on the shadow (a sundial, a drop shadow that is the only ground cue). |
| Image-based (environment) lighting | required | Defines the look of a PBR material; a Blinn-Phong fallback is a different picture. |
| Instancing (draw N copies from a buffer) | required, or `ADVANCED` | Drawing one copy instead of N is wrong. If it needs extra fields, it is an advanced form (bit 30), not a flag. |
| Per-vertex colours | `ADVANCED` on `DEFINE_MESH_3D`, not a draw flag | It changes the mesh layout. |

A feature that needs **more data in the operation** is never a flag: it is an advanced form (bit
30, PROTOCOL_3D.md §7.1) of the operation and belongs to a later format version.

## 8. Document-level requirements: the `FEATURE_3D_REQUIRED` header tag (Normative, proposed)

Per-draw bits are the truth, but a host wants to choose an engine, or a player, *before* loading.

- Tag: `Header.FEATURE_3D_REQUIRED`, type INT. Id **to be assigned** when implemented (the next
  free header property; 34 at the time of writing).
- Value: the bitwise OR of `mode & (MODE_REQUIRED_MASK | MODE_ADVANCED)` over every
  `DRAW_MESH_3D` in the document, plus bit 31 if any `DEFINE_MESH_3D` or `SET_TEXTURE_3D`
  uses its advanced bit. 0 means "the reference renderer suffices".
- A writer that emits any required or advanced bit MUST write the tag. A writer MAY write a 0 tag.
- A reader that supports the tag:
  1. reads it from the header without inflating the operations;
  2. if `(value & ~SUPPORTED_REQUIRED) != 0` (with bit 30 and 31 never in `SUPPORTED_REQUIRED`
     for this version), refuses the document before parsing any operation;
  3. still performs the per-draw check of §5.1, because old documents don't carry the tag and a
     malformed document could lie. The tag is an index, not a substitute.
- A host MAY use the tag to route: documents with a non-zero tag to a player/engine that
  implements those features; others to the reference.

A document whose tag claims less than its draws use is malformed; the per-draw check catches it.
A tag claiming more than the draws use is allowed (a writer may be conservative).

## 9. Conformance (Normative)

1. **Reference.** The software rasterizer of the reference player (`remote-player-core`,
   `JavaPaint3DContext`) defines the expected output for the semantic flags and for the fallback
   of every optional flag.
2. **Classes and tolerance.** An engine declares a class:

   | Class | Examples | Tolerance against the reference |
   | :--- | :--- | :--- |
   | A: reference-equivalent | software rasterizers (Java, C++, TS) | Pixel-exact on the corpus, modulo documented differences in line rasterization. |
   | B: hardware rasterizer, same lighting model | Android canvas engines, WebGL with the reference shading | Per-pixel difference within the corpus thresholds; silhouettes and depth ordering exact. |
   | C: physically based | Filament, WebGL PBR | Semantic flags exact (ordering, culling, transparency, wireframe); shading judged by the PBR contract (§6.2 of PROTOCOL_3D.md), not by pixel difference. |

3. **Corpus.** The checked-in 3D documents (`integration-tests/player-view-demos` corpus and the
   image-diff harness) are the conformance suite. Every assigned flag in §7 MUST add at least one
   document that exercises it and a reference image. A player claims support for a required flag
   only if it passes that flag's documents within its class tolerance.
4. **Refusal tests.** Every player MUST pass: a document with an unassigned required bit set is
   refused at load; a document with an unassigned optional bit set renders identically to the
   same document without it; a document with `ADVANCED` or bit 31 set is refused.

## 10. Compatibility (Normative)

1. Bits 0–16 do not change meaning. Every shipped document renders as before on every player.
2. Writers have always refused bits 17–31, so no shipped document sets them. Changing 24–29 and
   31 from "ignore" to "refuse" therefore breaks nothing in the field. This window closes the day
   a writer starts emitting such bits; land §5 first.
3. A player that implements §5 can read every existing document. An existing player given a
   document that uses a required flag will ignore it (old rule) and may draw it wrong; that is
   the situation this proposal ends, and the reason hosts should use the header tag (§8) to keep
   such documents away from old players.
4. PROTOCOL_3D.md §9 stays true: new players read every existing document; the format grows
   additively; no new 3D opcode.

## 11. Related operations (Informative, follow-up)

The same silent-degrade hazard exists elsewhere and should get the same treatment in a later
revision:

| Where | Today | Proposed |
| :--- | :--- | :--- |
| `PAINT_3D_STATE` `MATERIAL` extras (PBR fields) | kept and ignored | Define extras by index; indices 0–N optional, N+1 and up required, or add a required-features word as the first extra. |
| `SET_LIGHTS_3D` type-word bits 16–31, unknown light kinds | ignored / dropped | A dropped light can be "wrong" (the only light). Split kinds into fallback-defined and required. |
| `MESH_PRIMITIVE_3D` flag bits 9–31, `MESH_EXPRESSION_3D` flag bits 2–31 | ignored | Same optional/required split by range. |
| `MATRIX_3D_OP` unknown sub-ops | no-op | A missing transform is always wrong; should refuse. |

## 12. Implementation checklist (per player)

- [ ] Constants `MODE_OPTIONAL_MASK`, `MODE_REQUIRED_MASK`, `MODE_ADVANCED`, `MODE_RESERVED_MASK`
      and `SUPPORTED_REQUIRED` (0 for all players today).
- [ ] `DRAW_MESH_3D.read`: the §5.1 check; refusal goes through the unknown-opcode error path.
- [ ] Engine hint: treated as a preference the player may ignore; values 6–7 as 0; wireframe draws
      routed to an engine with hidden-line support regardless of the hint.
- [ ] Header: read `FEATURE_3D_REQUIRED` when assigned; refuse early if unsupported.
- [ ] Writer: refuse `ADVANCED`, bit 31, bits 4–7 and unassigned 17–29.
- [ ] Tests from §9.4.
- [ ] JSON: `"mode"` names unchanged; new flags appear as `"flags": ["name", …]` resolved from §7.

## 13. Examples

| Intent | mode | Notes |
| :--- | :--- | :--- |
| Smooth-shaded opaque mesh, no preference | `0x00000001` | The common case. |
| Same, prefer a GPU if present | `0x00002001` | `GPU` is a preference; the reference draws it identically. |
| Transparent, double-sided, clockwise winding | `0x00019001` | bits 0, 12, 15, 16. |
| Hidden-line wireframe, edges 0 and 1 only | `0x00000700` | bits 8, 9, 10. Any player routes this to hidden-line capable rendering. |
| PBR material, smooth | `0x00004001` | Reference draws Blinn-Phong; a class-C engine draws PBR. Both conform. |
| Future: smooth + optional bit 17 | `0x00020001` | Unknown to this player: ignored, drawn as `0x1`. |
| Future: smooth + required bit 24 | `0x01000001` | Unknown to this player: **document refused**. |
| Legacy: `drawmesh-zbuf-smooth` | `0x00000009` | Engine hint 4. Honoured by the Android player if able; ignored elsewhere; renders the same picture. |

---

*Change log*
- Draft 1 (2026-11-24): initial proposal, following the `EXT_BIT` → `ADVANCED_BIT` change.
