# scene3D API — strengths, weaknesses, and round-2 wishlist

Notes gathered while building eight new 3D demos against the `scene3D { }` creation DSL
(`RcScene3DScope`). The goal was to stress the API across very different shapes of problem —
wall-clock animation, instanced grids, procedural geometry, recursive structure, discrete state
machines, and physics/agent simulation — and record where it sang and where it fought back.

## The demos and what each one exercised

| Demo | File | What it stresses | Verdict |
|------|------|------------------|---------|
| 3D Clock | `DslClock3D.kt` | wall-clock time → reactive `rotate` | **Ideal fit** |
| Newton's Cradle | `DslNewtonsCradle3D.kt` | closed-form pendulum, `min`/`max` to split motion | **Great fit** |
| Cube Wave | `DslCubeWave3D.kt` | `loop` instancing + reactive per-index `translate` | **Good fit, hit limits** |
| Rubik's Cube | `DslRubiksCube3D.kt` | discrete move schedule via reactive conditionals | **Possible, awkward** |
| Growing Tree | `DslGrowingTree3D.kt` | recursive author-time geometry + timed reveal | **Good, one real gap** |
| Voxel Terrain | `DslVoxelTerrain3D.kt` | author-time noise, per-instance color + size | **Good (but unrollable only)** |
| Hydrogen Orbitals | `DslHydrogenOrbitals3D.kt` | custom `defineMesh` parametric isosurfaces | **Ideal fit** |
| Flocking Boids | `DslBoids3D.kt` | per-frame agent simulation | **Fakeable only** |

All eight build, register (DemosCreation `0061`–`0076`), and were verified on device via
`DocCaptureActivity` (shapes: `clock`, `cradle`, `cubewave`, `rubiks`, `tree`, `voxel`, `orbitals`,
`boids`).

---

## Strengths

1. **Reactive transforms are the sweet spot.** `rotate(RcFloat, axis)` and `translate(RcFloat³)`
   driven by `continuousSeconds()` / `hour()` / `minutes()` make the clock, cradle, and all the
   slow turntables fall out in a few lines, and the document stays tiny because the motion is
   re-evaluated on the player every frame — no re-upload. The clock is ~10 lines of actual motion.

2. **`loop` is real player-side instancing.** The cube-wave grid is 196 cubes from one
   `loop(0, 1, N*N)` with the index decoded into row/col and feeding a reactive `translate`. One
   cube mesh + one loop on the wire, N² animated cubes on screen. This is the highlight feature.

3. **A rich, closed-form math kernel.** `sin/cos/sqrt/abs/floor/fraction/atan2/min/pow/...` plus
   `clamp/lerp/smoothStep/pingPong/ifElse` cover essentially all per-frame procedural needs. The
   cradle's "two balls take turns" is just `max(base,0)` / `min(base,0)`; boid heading is
   `atan2(vx,vz)` of an analytic derivative. Trig-on-time is the workhorse.

4. **Two clean geometry on-ramps.** Parametric primitives (`primitiveSphere/Cube/Cylinder/Torus/
   RoundedCube/ExtrudedRoundedRect/…`) cover the common shapes, and `defineMesh(indices, verts,
   normals, uv)` is a no-ceremony escape hatch for fully custom geometry — the hydrogen orbitals are
   just a (θ,φ) grid with `r = |Yₗₘ|` and finite-difference normals. Recursion at author time (the
   tree's L-system) composes fine with it.

5. **Lighting/material/texture are enough for "looks 3D".** Two directional lights +
   `material(spec, shininess)` gave the cradle its steel and the orbitals their sheen with no fuss.

---

## Weaknesses / friction (each hit while building, with the workaround used)

1. **`MAX_EXPRESSION_SIZE = 32` RPN tokens, and expressions are *inlined*, not CSE'd.**
   This was the single biggest sharp edge. An `RcFloat` is an expression *tree*; every time you
   reuse a variable it is **re-expanded** into the consuming expression. The cube-wave's first cut
   used `sqrt(dx²+dz²)` for a radial ripple — `dx`/`dz` each expand `row`/`col`, the square doubles
   them, and the whole thing blew past 32 tokens and threw `"… to long"` at document build. Same in
   Rubik's: `clamp01(x)` alone duplicates `x`, and `x` contained the move-phase chain, so the
   per-cubie angle overflowed.
   - **Workarounds:** (a) simplify the math (cube-wave switched to Manhattan distance `|dx|+|dz|`),
     and (b) call `RcFloat.flush()` on shared sub-expressions, which commits them and returns a
     **1-token id reference** — manual common-subexpression elimination. Rubik's flushes `ph`, `slot`,
     `turn`, `moveAngle[s]`; boids flushes each bird's `ang`.
   - **Ask:** automatic CSE (or a documented `bind()`/`let`-style helper), a *much* higher cap, and a
     **build-time error that names the offending `RcFloat` variable and its token count** instead of
     dumping the RPN string and saying "to long". Today you discover the limit only at runtime.

2. **No reactive per-axis scale.** `scale(RcFloat)` is **uniform only**; per-axis `scale` is
   `Float`-only. The tree wants to *telescope* — extend along its length while keeping its girth —
   which is a reactive non-uniform scale. The workaround is `scale(rad, len, rad)` (static, final
   shape) **×** `scale(grow)` (reactive uniform), so each branch grows as a shrunken proportional
   copy from its joint. Reads fine, but it's not what "growing" should require. The cube-wave would
   also have liked reactive height-scaling instead of translate-bobbing.
   - **Ask:** `scale(x: RcFloat, y: RcFloat, z: RcFloat)`.

3. **`color()` is not reactive / not per-instance.** Color is fixed per `drawMesh`, so anything that
   wants per-element color must be **unrolled** — it can't ride a `loop`. This forced the cube-wave
   to be single-color (the wave reads through lighting only) and the voxel terrain to be 324
   unrolled draws *because* each column needs its own biome color (and its own non-uniform height).
   Cube-wave and voxel are the same problem mirrored: instancing buys you geometry but not color or
   per-axis size.
   - **Ask:** a reactive `color(RcFloat/RcColor)` and/or a per-loop-index color-array lookup
     (parallel to how `remoteFloatArray[index]` already feeds `translate`).

4. **`rotate` axis must be static.** The angle can be reactive but the axis can't, so you can't aim
   geometry along a *reactive* direction with one call — you must decompose into yaw/pitch about
   fixed axes. Boids dodge this (yaw-about-Y only); the tree dodges it by baking axis-angle at author
   time. Anything wanting to orient a mesh down a reactive vector (a boid's full 3D velocity, a
   needle pointing at a moving target) has to hand-build the yaw/pitch decomposition.
   - **Ask:** a `lookAlong(dirX, dirY, dirZ)` / `rotateToward` helper taking reactive components, or
     a reactive quaternion/`multiply(RcFloat matrix)`.

5. **No cross-frame state in the *scene* DSL → no real simulation.** The reactive model is pure
   closed-form `f(time)`. There's no way to feed last frame's positions back in, so true boids
   (neighbor separation/alignment/cohesion integrated each frame) can't be expressed in `scene3D`;
   the demo is a *scripted* flock (each path baked as sin/cos). Same reason Rubik's uses
   turn-and-return: a real cube *permutes* stickers, which needs retained state.
   - **Nuance discovered:** a stateful particle system **does exist** lower down —
     `RemoteComposeWriter.createParticles` / `particlesLoop` (per-particle update equations +
     restart) and even `particlesComparison` ("2 particles collision detection"). But it is **not
     surfaced in the typed DSL** and **not integrated with `scene3D`/`drawMesh`** (it's a 2D paint
     loop), and its equations are still bound by the 32-token cap. So the primitive for emergent
     simulation is *present in the engine but unreachable from 3D*.
   - **Ask:** expose particles in the typed DSL and let a particle drive a `drawMesh` (instanced 3D
     particles with retained per-particle state) — this single addition unlocks boids, fireworks,
     cloth, real Rubik's scrambles, fluid-ish effects.

6. **Minor ergonomics.**
   - Top-level math fns (`sin`, `cos`, `min`, `max`, `abs`) are **shadowed inside `scene3D {}`** by
     same-named scope members, so `sin(x)` fails with "cannot be called with an implicit receiver".
     The member forms `x.sin()`, `a.min(b)` work everywhere; `max(a,b)` has *no* member form so you
     write `a - a.min(0f)` for `max(a,0)`. Worth either removing the conflicting scope members or
     adding RcFloat `.max()`/`.clamp01()` members.
   - Mixing `Float` and `RcFloat` in one `translate`/`rotate` call isn't allowed (all-one-or-the-
     other overloads); constant coords in a reactive call need `RcFloat(0f)`.
   - `Float - RcFloat` doesn't exist (only `RcFloat - Float`), so `1f - expr` must be written
     `expr * -1f + 1f`. Easy to trip on.
   - Per-instance/per-face data: a Rubik's cubie can't show six sticker colors (no per-face color in
     one `drawMesh`); orbitals can't two-tone lobes by sign without splitting the mesh. A per-vertex
     color channel on `defineMesh` would fix both.

---

## Round-2 priorities (ranked by how often they bit)

1. **Solve the 32-token wall:** automatic CSE, a higher cap, and a *named* build-time error.
   (Hit in 2 of 8 demos as hard crashes; lurks in any non-trivial per-index math.)
2. **`color(RcFloat)` + per-loop-index color** so instancing covers color, not just geometry.
3. **Reactive per-axis `scale(RcFloat³)`** (telescoping growth, height-scaled grids).
4. **Particles in the typed DSL, driving `drawMesh`** (retained per-particle state in 3D).
5. **Reactive orientation** (`lookAlong`/reactive matrix) so meshes can aim down a live vector.
6. **Per-vertex color on `defineMesh`** (sign-colored lobes, six-color cubies, gradient meshes).
7. **Ergonomic cleanup:** unshadow math in `scene3D`, add `RcFloat.max/.clamp`, allow Float/RcFloat
   mixing in transforms, add `Float - RcFloat`.

## One-line summary

The DSL is excellent for *parametric, closed-form, instanced* 3D — clocks, orbiting systems, wave
fields, procedurally-built static geometry that animates by transform. It gets awkward exactly when
a scene needs **per-instance variety beyond position** (color, non-uniform size) or **state that
persists across frames** (simulation, permutation). The engine already has the missing piece for the
latter (particles); the highest-leverage round-2 work is lifting the 32-token expression ceiling and
surfacing per-instance color + stateful particles into the typed 3D DSL.
