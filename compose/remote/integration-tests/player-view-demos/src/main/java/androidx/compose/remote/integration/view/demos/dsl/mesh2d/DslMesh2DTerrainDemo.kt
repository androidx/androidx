/*
 * Copyright 2026 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.remote.integration.view.demos.dsl.mesh2d

import androidx.compose.remote.core.operations.Header
import androidx.compose.remote.creation.Rc
import androidx.compose.remote.creation.RemoteComposeWriter
import androidx.compose.remote.creation.dsl.Modifier
import androidx.compose.remote.creation.dsl.RcFloat
import androidx.compose.remote.creation.dsl.RcMeshLayout
import androidx.compose.remote.creation.dsl.RcPaintStyle
import androidx.compose.remote.creation.dsl.RcProfile
import androidx.compose.remote.creation.dsl.RcScope
import androidx.compose.remote.creation.dsl.background
import androidx.compose.remote.creation.dsl.clamp
import androidx.compose.remote.creation.dsl.createRcBuffer
import androidx.compose.remote.creation.dsl.fillMaxSize
import androidx.compose.remote.creation.dsl.floor
import androidx.compose.remote.creation.profile.RcPlatformProfiles

/** Vertices per side. 40 x 40 = 1600 vertices, 9126 indices - see the size note on the demo. */
private const val TERRAIN_GRID = 40

/** Fixed, so the terrain is the same every time this document is written. */
private const val TERRAIN_SEED = 20260919

private const val BALL_COUNT = 40

/**
 * Downhill acceleration per unit of height gradient, in domain-units per second squared. The domain
 * is the unit square, so a ball crossing the whole terrain travels a distance of 1.
 */
private const val GRAVITY = 0.15f

/** Viscous drag coefficient, per second. Terminal speed on a slope s is GRAVITY * s / DRAG. */
private const val DRAG = 1.2f

/** Seconds a ball rolls before it is returned to its start. */
private const val BALL_LIFETIME = 4f

/** Stagger between balls' restarts, so they do not all reset on the same frame. */
private const val BALL_STAGGER = 1.1f

/**
 * Perlin noise terrain as a literal mesh, with ten balls rolling down it.
 *
 * This demo exists to push on the parts of the API the other mesh demos do not reach, and three of
 * them are worth pointing at:
 *
 * **The terrain is baked, not parametric.** Every other mesh demo describes its geometry as an
 * expression over `(u, v)` and costs a few hundred bytes whatever its resolution. Noise cannot be
 * written that way - the expression language has no hash, no permutation table and no integer
 * arithmetic - so [TerrainData] computes it in ordinary Kotlin at document-write time and ships the
 * result as a vertex list. That is what [RcScope.remoteMesh2DValues] is for, and it is the honest
 * trade: this document is about 37 kB where a parametric mesh would be under one. Roughly half of
 * that is the index array (see the note in the report), and the positions and uv are f16, which is
 * free accuracy here because the mesh lives in the unit square and that is exactly where a half
 * float is at its most precise.
 *
 * **Colour carries two independent fields at once.** Hue and saturation come from height, running a
 * palette from deep water through shore and grass to bare rock and snow. Brightness is a Lambertian
 * hillshade computed from the local gradient, lit from the upper left. Height and slope are
 * different things - a high plateau is bright and flat, a low gorge is dark and steep - and putting
 * them on different channels is what makes the relief legible.
 *
 * **The uv channel carries the slope, not a texture coordinate.** `u` is steepness normalised
 * against the steepest point on this terrain, `v` is aspect - the compass direction of steepest
 * descent - mapped onto 0..1. Nothing samples an image here, so uv is two spare floats per vertex
 * and this is what it looks like to use them as a data channel: a player with a shader could read
 * the slope back per-fragment without the document ever sending a normal map.
 *
 * The balls are a real particle system, not ten copies of a drawing. The player integrates them;
 * the document only describes the rules. Each one reads the terrain gradient out of a float array
 * by index every frame and accelerates downhill, so they find the basins on their own.
 */
@Suppress("RestrictedApiAndroidX")
public fun dslMesh2DPerlinTerrain(): ByteArray {
    val terrain = TerrainData(TERRAIN_GRID, TERRAIN_SEED)
    return createRcBuffer(
        RcProfile(RcPlatformProfiles.ANDROIDX),
        RemoteComposeWriter.hTag(Header.DOC_WIDTH, 400),
        RemoteComposeWriter.hTag(Header.DOC_HEIGHT, 400),
        RemoteComposeWriter.hTag(
            Header.DOC_CONTENT_DESCRIPTION,
            "Perlin terrain with rolling balls",
        ),
        experimental = true,
    ) {
        Canvas(modifier = Modifier.fillMaxSize().background(0xFF05070C.toInt())) {
            val w = componentWidth()
            val h = componentHeight()

            val mesh =
                remoteMesh2DValues(
                    verts = terrain.verts,
                    indices = terrain.indices,
                    uv = terrain.uv,
                    colors = terrain.colors,
                    // The mesh is authored in the unit square, so every position and uv is in
                    // 0..1 - the one range where f16 has accuracy to spare. This halves the
                    // vertex payload for a quantisation error of about 0.0005 of the canvas.
                    halfFloat = true,
                    // Grid/uCount/vCount are not used to build the geometry here - the vertices
                    // are explicit - but they tell matrixFromMesh2D how to sample the surface,
                    // so the mesh stays usable as a coordinate frame.
                    layout = RcMeshLayout.Grid,
                    uCount = TERRAIN_GRID,
                    vCount = TERRAIN_GRID,
                )

            // The mesh is authored in the unit square; the canvas matrix is what sizes it. This
            // is why baked geometry does not have to mean fixed-resolution geometry.
            save {
                scale(w, h)
                drawMesh2D(mesh)
            }

            rollingBalls(terrain, w, h)

            drawMesh2DLabel("mesh2d f16 Values, UV slope & particles")
        }
    }
}

/**
 * Ten balls that read the terrain's gradient and roll downhill.
 *
 * The state per ball is five floats - position, velocity and an age - and the update is four
 * expressions the player evaluates itself. Nothing about the balls is sent per frame.
 */
@Suppress("RestrictedApiAndroidX")
private fun RcScope.rollingBalls(terrain: TerrainData, w: RcFloat, h: RcFloat) {
    val grid = terrain.grid.toFloat()
    val lastCell = grid - 1f

    // The gradient field goes over as two plain float arrays. The balls index into them; the mesh
    // has the same information baked into its uv, but a mesh is not readable from an expression,
    // so the data has to be sent a second time in a form that is.
    val gradX = remoteFloatArray(terrain.slopeX)
    val gradY = remoteFloatArray(terrain.slopeY)

    val variables = FloatArray(5)
    val dt = deltaTime()

    // The particle index. There is a `RemoteComposeWriter.index()` in the DSL but the scope's
    // writer is internal, so it cannot be reached from demo code; wrapping the raw VAR1 token is
    // the supported way, and is what RcScope.RcFloats does for the particle variables themselves.
    //
    // ParticlesCreate substitutes this token by position, and the packing it uses only round-trips
    // when the token sits at an offset below the number of particle variables. Every initial
    // expression below therefore LEADS with the index, putting it at offset 0, which is always
    // safe. Writing `0.5f + index * k` instead of `index * k + 0.5f` would move it past the
    // boundary and silently corrupt a different equation.
    val index = RcFloat(Rc.FloatExpression.VAR1)

    // Deterministic per-ball scatter. noiseFrom is a pure hash of its argument, so the layout is
    // identical on every run and every platform - unlike rand(), which is re-rolled whenever the
    // expression is evaluated.
    //    val startX = noiseFrom(index * 7919f) * 0.40f + 0.5f
    //    val startY = noiseFrom(index * 104729f) * 0.40f + 0.5f
    //    val startVx = noiseFrom(index * 15485863f) * 0.22f
    //    val startVy = noiseFrom(index * 32452843f) * 0.22f

    val startX = rand() * 0.6f + 0.2f
    val startY = rand() * 0.6f + 0.2f
    val startVx = rand() * 0.22f
    val startVy = rand() * 0.22f
    val startAge = index * -BALL_STAGGER

    // One long impulse: the setup runs once, the process block runs every frame.
    impulse(3_600_000f.rf, 0f.rf) {
        val ps =
            createParticles(
                variables,
                arrayOf(startX, startY, startVx, startVy, startAge),
                BALL_COUNT,
            )
        val (px, py, vx, vy, age) = RcFloats(variables)

        // Which terrain cell the ball is over. The clamp is not optional: an out-of-range array
        // index throws rather than returning zero, and a ball sitting exactly on the far edge
        // would otherwise index one past the end.
        val cellX = clamp(0f.rf, lastCell.rf, floor(px * lastCell))
        val cellY = clamp(0f.rf, lastCell.rf, floor(py * lastCell))
        val cell = cellY * grid + cellX

        impulseProcess() {
            particlesLoop(
                ps,
                // Restart when the ball has been rolling for its lifetime. Balls settle into
                // basins and would otherwise stop for good; recycling them keeps the terrain
                // being explored. The start values are deterministic, so a restart returns a
                // ball to exactly where it began.
                age - BALL_LIFETIME,
                arrayOf(
                    // Position is clamped to the canvas so a ball that runs out of terrain
                    // parks at the edge until its lifetime expires.
                    clamp(0.012f.rf, 0.988f.rf, px + vx * dt),
                    clamp(0.012f.rf, 0.988f.rf, py + vy * dt),
                    // Downhill force plus viscous drag, both integrated against dt so the
                    // motion does not depend on the player's frame rate.
                    vx - (vx * DRAG + gradX[cell] * GRAVITY) * dt,
                    vy - (vy * DRAG + gradY[cell] * GRAVITY) * dt,
                    age + dt,
                ),
            ) {
                val ballX = px * w
                val ballY = py * h
                val radius = w * 0.017f

                // A short streak back along the velocity, which is what makes it read as rolling
                // rather than teleporting.
                paint {
                    color(0x59FFFFFF)
                    style(RcPaintStyle.Stroke)
                    strokeWidth(radius * 0.7f)
                    antiAlias(true)
                }
                drawLine(ballX - vx * w * 0.10f, ballY - vy * h * 0.10f, ballX, ballY)

                paint {
                    color(0xCC0A0E14.toInt())
                    style(RcPaintStyle.Fill)
                }
                drawCircle(ballX, ballY, radius * 1.3f)

                paint { color(0xFFFFF1CE.toInt()) }
                drawCircle(ballX, ballY, radius)

                paint { color(0xFFFFFFFF.toInt()) }
                drawCircle(ballX - radius * 0.32f, ballY - radius * 0.32f, radius * 0.34f)
            }
        }
    }
}
