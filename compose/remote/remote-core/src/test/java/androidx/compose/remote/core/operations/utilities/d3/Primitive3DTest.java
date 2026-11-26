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
package androidx.compose.remote.core.operations.utilities.d3;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Validates the procedural mesh builders in {@link Primitive3D}. These are not exhaustive geometric
 * proofs — each test confirms a capability produces a structurally valid mesh (well-formed
 * vertex/normal/index arrays, in-range indices, finite values, unit normals) plus a few invariants
 * that are specific to the shape (bounding box, on-surface points, cap placement). The
 * {@code EXTRUDE_PATH} primitive (arbitrary 2D outline → 3D) gets extra coverage because it carries
 * the only non-trivial logic: nonzero-winding cap triangulation that must handle holes, overlapping
 * contours and concave/sloped outlines.
 */
public class Primitive3DTest {

    private static final float EPS = 1e-4f;
    private static final float PI = (float) Math.PI;

    // ------------------------------------------------------------------------------------------
    // Solids — dispatched through Primitive3D.build(type, segments, flags, params)
    // ------------------------------------------------------------------------------------------

    @Test
    public void sphere() {
        MeshData m = Primitive3D.build(Primitive3D.SPHERE, 16, 0, new float[]{2f, 0f, 0f, 0f});
        assertValidMesh(m);
        assertUnitNormals(m);
        // Every vertex sits on the sphere of radius 2 about the origin.
        for (int i = 0; i < vertCount(m); i++) {
            assertEquals("vertex on sphere surface", 2f, len(m.verts, i), 1e-3f);
        }
    }

    @Test
    public void torus() {
        MeshData m =
                Primitive3D.build(Primitive3D.TORUS, 20, 0, new float[]{1f, 0.25f, 0f, 0f, 0f});
        assertValidMesh(m);
        assertUnitNormals(m);
        float[] b = bounds(m);
        // Outer radius majorR+minorR in the XZ plane: a ring vertex lands exactly there.
        assertEquals(1.25f, b[3], 1e-3f); // maxX
        // Tube half-height ≈ minorR in Y (discrete tube segments may not sample the very top, so
        // the
        // extent approaches but doesn't exceed minorR).
        assertTrue("|Y| within tube radius", b[4] > 0f && b[4] <= 0.25f + EPS);
        assertTrue("|Y| near tube radius", b[4] >= 0.25f * 0.9f);
    }

    @Test
    public void plane() {
        MeshData m = Primitive3D.build(Primitive3D.PLANE, 0, 0, new float[]{2f, 1f, 0f, 0f, 0f});
        assertValidMesh(m);
        assertUnitNormals(m);
        assertEquals("a quad is 2 triangles", 6, m.indices.length);
        for (int i = 0; i < vertCount(m); i++) {
            assertEquals("plane is flat in Z", 0f, m.verts[i * 3 + 2], EPS);
            assertEquals("normal faces +Z", 1f, m.normals[i * 3 + 2], EPS);
        }
    }

    @Test
    public void planeUv() {
        MeshData m =
                Primitive3D.build(
                        Primitive3D.PLANE,
                        0,
                        Primitive3D.UV << Primitive3D.FLAG_UV_SHIFT,
                        new float[]{2f, 2f, 0f, 0f, 0f});
        assertValidMesh(m);
        assertTrue("uvMode produces UV coords", m.uv != null && m.uv.length == vertCount(m) * 2);
    }

    @Test
    public void cube() {
        MeshData m =
                Primitive3D.build(Primitive3D.CUBE, 0, 0, new float[]{2f, 2f, 2f, 0f, 0f, 0f});
        assertValidMesh(m);
        assertUnitNormals(m);
        assertEquals("6 faces × 4 verts", 24, vertCount(m));
        assertEquals("6 faces × 2 triangles", 36, m.indices.length);
        float[] b = bounds(m);
        assertEquals(-1f, b[0], EPS);
        assertEquals(-1f, b[1], EPS);
        assertEquals(-1f, b[2], EPS);
        assertEquals(1f, b[3], EPS);
        assertEquals(1f, b[4], EPS);
        assertEquals(1f, b[5], EPS);
    }

    @Test
    public void roundedCube() {
        MeshData m =
                Primitive3D.build(
                        Primitive3D.ROUNDED_CUBE, 3, 0, new float[]{2f, 2f, 2f, 0f, 0f, 0f, 0.3f});
        assertValidMesh(m);
        assertUnitNormals(m);
        float[] b = bounds(m);
        // Rounded corners stay within the nominal box half-extents.
        assertTrue("within box X", b[0] >= -1f - EPS && b[3] <= 1f + EPS);
        assertTrue("within box Y", b[1] >= -1f - EPS && b[4] <= 1f + EPS);
    }

    @Test
    public void cone() {
        MeshData m = Primitive3D.build(Primitive3D.CONE, 16, 0, new float[]{1f, 2f, 0f, 0f, 0f});
        assertValidMesh(m);
        assertUnitNormals(m);
        float[] b = bounds(m);
        assertEquals("base at y=0", 0f, b[1], 1e-3f);
        assertEquals("apex at y=height", 2f, b[4], 1e-3f);
        assertEquals("base radius in X", 1f, b[3], 1e-3f);
    }

    @Test
    public void cylinder() {
        MeshData m =
                Primitive3D.build(
                        Primitive3D.CYLINDER,
                        16,
                        0,
                        new float[]{0.5f, 0f, -1f, 0f, 0f, 1f, 0f});
        assertValidMesh(m);
        assertUnitNormals(m);
        float[] b = bounds(m);
        assertEquals("spans the axis from y=-1", -1f, b[1], 1e-3f);
        assertEquals("to y=+1", 1f, b[4], 1e-3f);
        assertEquals("radius in X", 0.5f, b[3], 1e-3f);
    }

    @Test
    public void sphericalSector() {
        MeshData m =
                Primitive3D.build(
                        Primitive3D.SPHERICAL_SECTOR, 16, 0, new float[]{1f, PI / 2f, 0f, 0f, 0f});
        assertValidMesh(m);
    }

    @Test
    public void sphericalDome() {
        MeshData m =
                Primitive3D.build(
                        Primitive3D.SPHERICAL_DOME, 16, 0, new float[]{1f, PI / 2f, 0f, 0f, 0f});
        assertValidMesh(m);
    }

    @Test
    public void tube() {
        // radius + 3 control points (x,y,z each).
        MeshData m =
                Primitive3D.build(
                        Primitive3D.TUBE,
                        12,
                        0,
                        new float[]{0.2f, 0f, 0f, 0f, 0f, 1f, 0f, 1f, 1f, 0f});
        assertValidMesh(m);
    }

    @Test
    public void capTube() {
        MeshData m =
                Primitive3D.build(
                        Primitive3D.CAP_TUBE,
                        12,
                        0,
                        new float[]{0.2f, 0f, 0f, 0f, 0f, 1f, 0f, 1f, 1f, 0f});
        assertValidMesh(m);
    }

    @Test
    public void profileTube() {
        // [nPoints, x0,y0,z0, x1,y1,z1, r0, r1] — a 2-point tapering tube.
        MeshData m =
                Primitive3D.build(
                        Primitive3D.PROFILE_TUBE,
                        12,
                        Primitive3D.FLAG_TUBE_CAP,
                        new float[]{2f, 0f, 0f, 0f, 0f, 1f, 0f, 0.4f, 0.1f});
        assertValidMesh(m);
    }

    // ------------------------------------------------------------------------------------------
    // Extruded 2D shapes — flat profile in XY swept along Z by `depth`
    // ------------------------------------------------------------------------------------------

    @Test
    public void extrudeCircle() {
        MeshData m =
                Primitive3D.build(Primitive3D.EXTRUDE_CIRCLE, 24, 0,
                        new float[]{1f, 0.4f, 0, 0, 0});
        assertValidMesh(m);
        assertUnitNormals(m);
        assertCapDepth(m, 0.4f, 0f);
        float[] b = bounds(m);
        assertEquals("disk radius in X", 1f, b[3], 1e-3f);
    }

    @Test
    public void extrudeSector() {
        MeshData m =
                Primitive3D.build(
                        Primitive3D.EXTRUDE_SECTOR, 24, 0,
                        new float[]{1f, 0f, PI / 2f, 0.4f, 0, 0, 0});
        assertValidMesh(m);
        assertUnitNormals(m);
        assertCapDepth(m, 0.4f, 0f);
    }

    @Test
    public void extrudeSegment() {
        MeshData m =
                Primitive3D.build(
                        Primitive3D.EXTRUDE_SEGMENT, 24, 0,
                        new float[]{1f, 0f, PI / 2f, 0.4f, 0, 0, 0});
        assertValidMesh(m);
        assertUnitNormals(m);
        assertCapDepth(m, 0.4f, 0f);
    }

    @Test
    public void extrudeArc() {
        MeshData m =
                Primitive3D.build(
                        Primitive3D.EXTRUDE_ARC, 24, 0,
                        new float[]{0.5f, 1f, 0f, PI / 2f, 0.4f, 0, 0, 0});
        assertValidMesh(m);
        assertUnitNormals(m);
        assertCapDepth(m, 0.4f, 0f);
    }

    @Test
    public void extrudeRoundedRect() {
        MeshData m =
                Primitive3D.build(
                        Primitive3D.EXTRUDE_ROUNDED_RECT, 6, 0,
                        new float[]{2f, 1f, 0.2f, 0.4f, 0, 0, 0});
        assertValidMesh(m);
        assertUnitNormals(m);
        assertCapDepth(m, 0.4f, 0f);
        float[] b = bounds(m);
        assertEquals("half width", 1f, b[3], 1e-3f);
        assertEquals("half height", 0.5f, b[4], 1e-3f);
    }

    @Test
    public void extrudeSquircle() {
        MeshData m =
                Primitive3D.build(
                        Primitive3D.EXTRUDE_SQUIRCLE, 48, 0, new float[]{1f, 4f, 0.4f, 0, 0, 0});
        assertValidMesh(m);
        assertUnitNormals(m);
        assertCapDepth(m, 0.4f, 0f);
    }

    // ------------------------------------------------------------------------------------------
    // EXTRUDE_PATH — arbitrary 2D outline (the new capability)
    // ------------------------------------------------------------------------------------------

    @Test
    public void extrudePathSquare() {
        // A unit square (side 2) CCW, extruded by depth 0.4.
        List<float[]> contours = new ArrayList<>();
        contours.add(rect(-1f, -1f, 1f, 1f, false));
        MeshData m = Primitive3D.build(Primitive3D.EXTRUDE_PATH, 0, 0,
                packPath(0.4f, 0, 0, 0, contours));

        assertValidMesh(m);
        assertUnitNormals(m);
        assertCapDepth(m, 0.4f, 0f);
        // The front cap must tile exactly the 2×2 = 4 area, no gaps and no overlaps.
        assertEquals("front cap area = square area", 4f, frontCapArea(m, 0.2f), 1e-3f);
        assertEquals("back cap area = front cap area", 4f, capArea(m, -0.2f), 1e-3f);
        assertTrue("center is filled", pointInCap(m, 0.2f, 0f, 0f));
    }

    @Test
    public void extrudePathTriangleSlopedEdges() {
        // A triangle has sloped edges — this is what the trapezoid (not rectangle) cap fill must
        // tile exactly. Vertices CCW; analytic area = 2.
        List<float[]> contours = new ArrayList<>();
        contours.add(new float[]{0f, 0f, 2f, 0f, 1f, 2f});
        MeshData m = Primitive3D.build(Primitive3D.EXTRUDE_PATH, 0, 0,
                packPath(0.3f, 0, 0, 0, contours));

        assertValidMesh(m);
        assertEquals("front cap tiles the triangle", 2f, frontCapArea(m, 0.15f), 1e-3f);
        assertTrue("interior point filled", pointInCap(m, 0.15f, 1f, 0.5f));
    }

    @Test
    public void extrudePathConcave() {
        // A plus/cross (concave) — area = 5 unit cells. Validates multi-span tiling.
        List<float[]> contours = new ArrayList<>();
        contours.add(
                new float[]{
                        -1f, -3f, 1f, -3f, 1f, -1f, 3f, -1f, 3f, 1f, 1f, 1f,
                        1f, 3f, -1f, 3f, -1f, 1f, -3f, 1f, -3f, -1f, -1f, -1f
                });
        MeshData m = Primitive3D.build(Primitive3D.EXTRUDE_PATH, 0, 0,
                packPath(0.2f, 0, 0, 0, contours));

        assertValidMesh(m);
        assertEquals("front cap tiles the cross", 20f, frontCapArea(m, 0.1f), 1e-3f);
        assertTrue("arm filled", pointInCap(m, 0.1f, 2f, 0f));
        assertTrue("center filled", pointInCap(m, 0.1f, 0f, 0f));
        assertTrue("notch is empty", !pointInCap(m, 0.1f, 2f, 2f));
    }

    @Test
    public void extrudePathHoleUsesNonzeroWinding() {
        // Outer CCW square (area 16) with an inner CW square (area 4): opposite winding → a hole.
        List<float[]> contours = new ArrayList<>();
        contours.add(rect(-2f, -2f, 2f, 2f, false)); // CCW
        contours.add(rect(-1f, -1f, 1f, 1f, true)); // CW (reversed) → hole
        MeshData m = Primitive3D.build(Primitive3D.EXTRUDE_PATH, 0, 0,
                packPath(0.2f, 0, 0, 0, contours));

        assertValidMesh(m);
        assertEquals("area = outer - hole", 12f, frontCapArea(m, 0.1f), 1e-3f);
        assertTrue("ring is filled", pointInCap(m, 0.1f, 1.5f, 0f));
        assertTrue("hole is empty", !pointInCap(m, 0.1f, 0f, 0f));
    }

    @Test
    public void extrudePathSameWindingNoHole() {
        // Same two squares but the inner is ALSO CCW: nonzero winding keeps the inside filled
        // (winding count 2, not 0). Even-odd would wrongly punch a hole here — this guards that.
        List<float[]> contours = new ArrayList<>();
        contours.add(rect(-2f, -2f, 2f, 2f, false)); // CCW
        contours.add(rect(-1f, -1f, 1f, 1f, false)); // CCW too
        MeshData m = Primitive3D.build(Primitive3D.EXTRUDE_PATH, 0, 0,
                packPath(0.2f, 0, 0, 0, contours));

        assertValidMesh(m);
        assertEquals("solid, no hole", 16f, frontCapArea(m, 0.1f), 1e-3f);
        assertTrue("center is filled", pointInCap(m, 0.1f, 0f, 0f));
    }

    @Test
    public void extrudePathOverlapUnion() {
        // Two overlapping CCW squares (the synthetic-bold glyph case). Nonzero winding fills their
        // union; even-odd would empty the doubly-covered overlap. Union = 4 + 4 - 2 = 6.
        List<float[]> contours = new ArrayList<>();
        contours.add(rect(-1f, -1f, 1f, 1f, false));
        contours.add(rect(0f, -1f, 2f, 1f, false));
        MeshData m = Primitive3D.build(Primitive3D.EXTRUDE_PATH, 0, 0,
                packPath(0.2f, 0, 0, 0, contours));

        assertValidMesh(m);
        assertEquals("union area", 6f, frontCapArea(m, 0.1f), 1e-3f);
        assertTrue("overlap region is filled", pointInCap(m, 0.1f, 0.5f, 0f));
    }

    @Test
    public void extrudePathTranslatedCenter() {
        // The (cx,cy,cz) offset translates the whole prism.
        List<float[]> contours = new ArrayList<>();
        contours.add(rect(-1f, -1f, 1f, 1f, false));
        MeshData m =
                Primitive3D.build(
                        Primitive3D.EXTRUDE_PATH, 0, 0, packPath(0.4f, 5f, 6f, 7f, contours));
        assertValidMesh(m);
        float[] b = bounds(m);
        assertEquals(4f, b[0], EPS); // minX = 5 - 1
        assertEquals(6f, b[3], EPS); // maxX = 5 + 1
        assertEquals(5f, b[1], EPS); // minY = 6 - 1
        assertEquals(6.8f, b[2], 1e-3f); // minZ = 7 - depth/2
        assertEquals(7.2f, b[5], 1e-3f); // maxZ = 7 + depth/2
    }

    // ------------------------------------------------------------------------------------------
    // LATHE — revolve a 2D profile around the Y axis (multi-channel input)
    // ------------------------------------------------------------------------------------------

    @Test
    public void latheCylinder() {
        // Constant-radius profile (r=0.5, y=-1..1) → a cylindrical wall; every vertex sits on the
        // radius-0.5 circle about the Y axis with a purely radial normal.
        float[] center = {0f, 0f, 0f};
        float[] profile = {0.5f, -1f, 0.5f, 1f};
        MeshData m =
                Primitive3D.build(Primitive3D.LATHE, 24, 0, new float[][]{center, profile});
        assertValidMesh(m);
        assertUnitNormals(m);
        for (int i = 0; i < vertCount(m); i++) {
            float x = m.verts[i * 3];
            float y = m.verts[i * 3 + 1];
            float z = m.verts[i * 3 + 2];
            assertEquals("radius from Y axis", 0.5f, (float) Math.sqrt(x * x + z * z), 1e-3f);
            assertTrue("y in profile range", y >= -1f - EPS && y <= 1f + EPS);
            assertEquals("normal is radial (ny=0)", 0f, m.normals[i * 3 + 1], 1e-3f);
        }
    }

    @Test
    public void latheProfileBounds() {
        // A vase-ish profile (base, bulge, neck): bounds = max radius × profile height span.
        float[] center = {0f, 0f, 0f};
        float[] profile = {0.2f, 0f, 0.6f, 0.5f, 0.35f, 1.2f};
        MeshData m =
                Primitive3D.build(Primitive3D.LATHE, 16, 0, new float[][]{center, profile});
        assertValidMesh(m);
        assertUnitNormals(m);
        float[] b = bounds(m);
        assertEquals("max radius in X", 0.6f, b[3], 1e-3f);
        assertEquals("base y", 0f, b[1], 1e-3f);
        assertEquals("top y", 1.2f, b[4], 1e-3f);
    }

    @Test
    public void latheApproximatesSphere() {
        // A semicircle profile revolved → a sphere of that radius; every vertex ~radius from
        // center.
        int n = 24;
        float[] profile = new float[(n + 1) * 2];
        for (int i = 0; i <= n; i++) {
            double t = Math.PI * i / n; // 0..π, pole to pole
            profile[i * 2] = (float) Math.sin(t); // r
            profile[i * 2 + 1] = (float) -Math.cos(t); // y (-1..1)
        }
        MeshData m = Primitive3D.build(Primitive3D.LATHE, 24, 0,
                new float[][]{{0f, 0f, 0f}, profile});
        assertValidMesh(m);
        for (int i = 0; i < vertCount(m); i++) {
            assertEquals("on unit sphere", 1f, len(m.verts, i), 2e-2f);
        }
    }

    @Test
    public void latheTranslated() {
        float[] center = {3f, 4f, 5f};
        float[] profile = {1f, -1f, 1f, 1f};
        MeshData m =
                Primitive3D.build(Primitive3D.LATHE, 12, 0, new float[][]{center, profile});
        assertValidMesh(m);
        float[] b = bounds(m);
        assertEquals(2f, b[0], 1e-3f); // 3 - 1
        assertEquals(4f, b[3], 1e-3f); // 3 + 1
        assertEquals(3f, b[1], 1e-3f); // 4 - 1
        assertEquals(5f, b[4], 1e-3f); // 4 + 1
    }

    @Test
    public void extrudePathBevelZeroIsSharp() {
        // bevel = 0 must produce exactly the sharp extrusion (1 trapezoid cap + 4 wall quads): the
        // bevel code path is skipped, so counts/geometry are unchanged from the un-bevelled square.
        List<float[]> contours = new ArrayList<>();
        contours.add(rect(-1f, -1f, 1f, 1f, false));
        MeshData m = Primitive3D.build(
                Primitive3D.EXTRUDE_PATH, 0, 0, packPath(0.4f, 0, 0, 0, 0f, contours));
        assertValidMesh(m);
        assertEquals("sharp: front+back cap (8) + 4 wall quads (16)", 8 + 16, vertCount(m));
        assertEquals("front cap still tiles the full square", 4f, frontCapArea(m, 0.2f), 1e-3f);
    }

    @Test
    public void extrudePathBevelInsetsCapAndAddsRings() {
        // With a bevel the flat front cap shrinks (inset by the bevel) while the silhouette/wall
        // stays full size, and extra chamfer rings appear → more vertices, smaller cap area.
        List<float[]> contours = new ArrayList<>();
        contours.add(rect(-1f, -1f, 1f, 1f, false));
        MeshData m = Primitive3D.build(
                Primitive3D.EXTRUDE_PATH, 0, 0, packPath(0.4f, 0, 0, 0, 0.1f, contours));
        assertValidMesh(m);
        assertUnitNormals(m);
        // Flat top is inset by 0.1 each side → a 1.8×1.8 square ≈ 3.24, well under the full 4.
        assertEquals("front cap inset by bevel", 1.8f * 1.8f, frontCapArea(m, 0.2f), 1e-2f);
        // Wall still reaches the full ±1 silhouette.
        float[] b = bounds(m);
        assertEquals("silhouette stays full width", 1f, b[3], 1e-3f);
        assertTrue("more geometry than the sharp case", vertCount(m) > 8 + 16);
    }

    // ------------------------------------------------------------------------------------------
    // SWEEP — sweep a 2D cross-section along a 3D path (multi-channel input)
    // ------------------------------------------------------------------------------------------

    @Test
    public void sweepCircleAlongLineIsTube() {
        // A circle (radius 0.3) swept along the Z axis → a tube; every wall vertex sits at radius
        // 0.3 from that axis, with a radial (z=0) normal.
        float[] scalars = {1f, 0f, 0f, 0f, 0f, 0f}; // closed, no caps
        float[] xsec = circleXY(0.3f, 16);
        float[] path = {0f, 0f, -1f, 0f, 0f, 0f, 0f, 0f, 1f};
        MeshData m = Primitive3D.build(Primitive3D.SWEEP, 0, 0, new float[][]{scalars, xsec, path});
        assertValidMesh(m);
        assertUnitNormals(m);
        for (int i = 0; i < vertCount(m); i++) {
            float x = m.verts[i * 3];
            float y = m.verts[i * 3 + 1];
            float z = m.verts[i * 3 + 2];
            assertEquals("radius from Z axis", 0.3f, (float) Math.sqrt(x * x + y * y), 1e-3f);
            assertTrue("z within path", z >= -1f - EPS && z <= 1f + EPS);
            assertEquals("normal is radial (nz=0)", 0f, m.normals[i * 3 + 2], 1e-3f);
        }
    }

    @Test
    public void sweepScaleTapers() {
        // Per-station scale 1 → 0 tapers the swept circle from radius 0.3 down to a point.
        float[] scalars = {1f, 0f, 0f, 0f, 0f, 0f};
        float[] xsec = circleXY(0.3f, 12);
        float[] path = {0f, 0f, 0f, 0f, 0f, 1f, 0f, 0f, 2f};
        float[] scales = {1f, 0.5f, 0f};
        MeshData m =
                Primitive3D.build(Primitive3D.SWEEP, 0, 0,
                        new float[][]{scalars, xsec, path, scales});
        assertValidMesh(m);
        float maxR = 0f;
        float minR = Float.MAX_VALUE;
        for (int i = 0; i < vertCount(m); i++) {
            float r = (float) Math.sqrt(m.verts[i * 3] * m.verts[i * 3]
                    + m.verts[i * 3 + 1] * m.verts[i * 3 + 1]);
            maxR = Math.max(maxR, r);
            minR = Math.min(minR, r);
        }
        assertEquals("widest ring = full radius", 0.3f, maxR, 1e-3f);
        assertEquals("tapered ring collapses to the axis", 0f, minR, 1e-3f);
    }

    @Test
    public void sweepWithCapsIsValid() {
        // Caps add a centroid-fan disk at each end; the mesh stays well-formed with unit normals.
        float[] scalars = {1f, 1f, 1f, 0f, 0f, 0f}; // closed + both caps
        float[] xsec = circleXY(0.25f, 10);
        float[] path = {0f, 0f, 0f, 0f, 1f, 0f};
        MeshData m = Primitive3D.build(Primitive3D.SWEEP, 0, 0, new float[][]{scalars, xsec, path});
        assertValidMesh(m);
        assertUnitNormals(m);
        // 2 stations × (10+1 seam) wall verts + two caps of (10+1) verts each.
        assertEquals(2 * 11 + 2 * 11, vertCount(m));
    }

    @Test
    public void sweepOpenSectionIsRibbon() {
        // An open cross-section (a straight segment) swept → a flat ribbon, no seam wrap.
        float[] scalars = {0f, 0f, 0f, 0f, 0f, 0f}; // open
        float[] xsec = {-0.3f, 0f, 0f, 0f, 0.3f, 0f};
        float[] path = {0f, 0f, 0f, 0f, 0f, 1f};
        MeshData m = Primitive3D.build(Primitive3D.SWEEP, 0, 0, new float[][]{scalars, xsec, path});
        assertValidMesh(m);
        assertEquals("2 stations × 3 open columns", 2 * 3, vertCount(m));
    }

    // ------------------------------------------------------------------------------------------
    // HELIX — a coil/spring (a tube swept along a generated helix)
    // ------------------------------------------------------------------------------------------

    @Test
    public void helixBoundsAndValidity() {
        // coilR=1, tubeR=0.15, pitch=0.5, 3 turns. The tube center traces radius 1 about Y, so the
        // mesh spans ±(coilR+tubeR) in X/Z and ~pitch·turns tall.
        MeshData m =
                Primitive3D.build(
                        Primitive3D.HELIX, 10, 0, new float[]{1f, 0.15f, 0.5f, 3f, 0f, 0f, 0f});
        assertValidMesh(m);
        assertUnitNormals(m);
        float[] b = bounds(m);
        assertEquals("outer X extent = coilR + tubeR", 1.15f, b[3], 2e-2f);
        assertEquals("outer Z extent = coilR + tubeR", 1.15f, b[5], 2e-2f);
        // Height = the centerline rise (pitch·turns) plus the tube poking ~±tubeR past each end.
        float height = b[4] - b[1];
        assertEquals("height ≈ pitch·turns + 2·tubeR", 0.5f * 3f + 2f * 0.15f, height, 0.1f);
    }

    @Test
    public void helixTubeRadius() {
        // Every wall vertex sits ~tubeR from the helical centerline (radius coilR about Y).
        float coilR = 1f;
        float tubeR = 0.12f;
        MeshData m =
                Primitive3D.build(
                        Primitive3D.HELIX, 12, 0,
                        new float[]{coilR, tubeR, 0.4f, 2f, 0f, 0f, 0f});
        assertValidMesh(m);
        float[] b = bounds(m);
        // Inner gap of the coil tube in the XZ plane = coilR - tubeR.
        assertTrue("has an inner hole", b[3] > coilR && (coilR - tubeR) > 0f);
    }

    // ------------------------------------------------------------------------------------------
    // ICOSPHERE — geodesic sphere (subdivided icosahedron)
    // ------------------------------------------------------------------------------------------

    @Test
    public void icosphereOnSphere() {
        MeshData m = Primitive3D.build(Primitive3D.ICOSPHERE, 2, 0, new float[]{1.5f, 0f, 0f, 0f});
        assertValidMesh(m);
        assertUnitNormals(m);
        for (int i = 0; i < vertCount(m); i++) {
            assertEquals("vertex on the sphere", 1.5f, len(m.verts, i), 1e-4f);
            // Normal equals the normalized position direction.
            assertEquals("normal.x = dir.x", m.verts[i * 3] / 1.5f, m.normals[i * 3], 1e-4f);
        }
    }

    @Test
    public void icosphereSubdivisionGrows() {
        // Each subdivision level quadruples the triangle count (20·4ⁿ faces). segments <= 0 means
        // "default", so request explicit levels 1..3.
        MeshData l1 = Primitive3D.build(Primitive3D.ICOSPHERE, 1, 0, new float[]{1f, 0f, 0f, 0f});
        MeshData l2 = Primitive3D.build(Primitive3D.ICOSPHERE, 2, 0, new float[]{1f, 0f, 0f, 0f});
        MeshData l3 = Primitive3D.build(Primitive3D.ICOSPHERE, 3, 0, new float[]{1f, 0f, 0f, 0f});
        assertEquals("level-1 faces = 20·4", 20 * 4 * 3, l1.indices.length);
        assertEquals("level-2 faces = 20·4²", 20 * 16 * 3, l2.indices.length);
        assertEquals("level-3 faces = 20·4³", 20 * 64 * 3, l3.indices.length);
    }

    @Test
    public void icosphereTranslated() {
        MeshData m = Primitive3D.build(Primitive3D.ICOSPHERE, 1, 0, new float[]{1f, 5f, 6f, 7f});
        assertValidMesh(m);
        float[] b = bounds(m);
        assertEquals(4f, b[0], 1e-3f);
        assertEquals(6f, b[3], 1e-3f);
        assertEquals(8f, b[5], 1e-3f);
    }

    // ------------------------------------------------------------------------------------------
    // Dispatch
    // ------------------------------------------------------------------------------------------

    @Test
    public void buildRejectsUnknownType() {
        assertThrows(
                IllegalArgumentException.class,
                () -> Primitive3D.build(9999, 8, 0, new float[]{1f}));
    }

    @Test
    public void allTypesProduceValidMeshes() {
        // One more pass through every type at default tessellation (segments <= 0) so the default
        // paths are exercised too.
        assertValidMesh(Primitive3D.build(Primitive3D.SPHERE, 0, 0, new float[]{1, 0, 0, 0}));
        assertValidMesh(Primitive3D.build(Primitive3D.TORUS, 0, 0, new float[]{1, 0.3f, 0, 0, 0}));
        assertValidMesh(Primitive3D.build(Primitive3D.PLANE, 0, 0, new float[]{1, 1, 0, 0, 0}));
        assertValidMesh(Primitive3D.build(Primitive3D.CUBE, 0, 0, new float[]{1, 1, 1, 0, 0, 0}));
        assertValidMesh(
                Primitive3D.build(Primitive3D.ROUNDED_CUBE, 0, 0,
                        new float[]{1, 1, 1, 0, 0, 0, 0.2f}));
        assertValidMesh(Primitive3D.build(Primitive3D.CONE, 0, 0, new float[]{1, 1, 0, 0, 0}));
        assertValidMesh(
                Primitive3D.build(Primitive3D.CYLINDER, 0, 0,
                        new float[]{0.5f, 0, -1, 0, 0, 1, 0}));
        assertValidMesh(
                Primitive3D.build(Primitive3D.EXTRUDE_CIRCLE, 0, 0, new float[]{1, 0.3f, 0, 0, 0}));
        assertValidMesh(
                Primitive3D.build(Primitive3D.EXTRUDE_SQUIRCLE, 0, 0,
                        new float[]{1, 4, 0.3f, 0, 0, 0}));
    }

    // ------------------------------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------------------------------

    private static int vertCount(MeshData m) {
        return m.verts.length / 3;
    }

    /** Distance of vertex {@code i} from the origin. */
    private static float len(float[] verts, int i) {
        float x = verts[i * 3];
        float y = verts[i * 3 + 1];
        float z = verts[i * 3 + 2];
        return (float) Math.sqrt(x * x + y * y + z * z);
    }

    /** Asserts the mesh is structurally well-formed (the baseline every builder must satisfy). */
    private static void assertValidMesh(MeshData m) {
        assertTrue("mesh non-null", m != null);
        assertTrue("has vertices", m.verts.length > 0);
        assertEquals("verts are xyz triples", 0, m.verts.length % 3);
        assertEquals("one normal per vertex", m.verts.length, m.normals.length);
        assertTrue("has triangles", m.indices.length > 0);
        assertEquals("indices are triangles", 0, m.indices.length % 3);
        int vc = vertCount(m);
        for (int idx : m.indices) {
            assertTrue("index in range: " + idx, idx >= 0 && idx < vc);
        }
        for (float v : m.verts) {
            assertTrue("vertex finite", !Float.isNaN(v) && !Float.isInfinite(v));
        }
        for (float n : m.normals) {
            assertTrue("normal finite", !Float.isNaN(n) && !Float.isInfinite(n));
        }
        if (m.uv != null) {
            assertEquals("two uv per vertex", vc * 2, m.uv.length);
        }
    }

    /** Asserts every normal is unit length (within tolerance). */
    private static void assertUnitNormals(MeshData m) {
        for (int i = 0; i < vertCount(m); i++) {
            float l = len(m.normals, i);
            assertEquals("normal " + i + " is unit length", 1f, l, 1e-2f);
        }
    }

    /** Axis-aligned bounds: {minX,minY,minZ,maxX,maxY,maxZ}. */
    private static float[] bounds(MeshData m) {
        float[] b = {
                Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE,
                -Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE
        };
        for (int i = 0; i < vertCount(m); i++) {
            for (int a = 0; a < 3; a++) {
                float c = m.verts[i * 3 + a];
                if (c < b[a]) b[a] = c;
                if (c > b[a + 3]) b[a + 3] = c;
            }
        }
        return b;
    }

    /** Asserts an extruded prism's caps sit at cz ± depth/2 (the Z extent equals the thickness). */
    private static void assertCapDepth(MeshData m, float depth, float cz) {
        float[] b = bounds(m);
        assertEquals("front cap Z", cz + depth / 2f, b[5], 1e-3f);
        assertEquals("back cap Z", cz - depth / 2f, b[2], 1e-3f);
    }

    /** Total area of triangles whose three vertices all lie on the plane z == zPlane. */
    private static float capArea(MeshData m, float zPlane) {
        float area = 0f;
        for (int t = 0; t < m.indices.length; t += 3) {
            int a = m.indices[t];
            int b = m.indices[t + 1];
            int c = m.indices[t + 2];
            if (onZ(m, a, zPlane) && onZ(m, b, zPlane) && onZ(m, c, zPlane)) {
                area += triArea2D(m, a, b, c);
            }
        }
        return area;
    }

    private static float frontCapArea(MeshData m, float zPlane) {
        return capArea(m, zPlane);
    }

    /** True if any cap triangle on plane z == zPlane contains the 2D point (px,py). */
    private static boolean pointInCap(MeshData m, float zPlane, float px, float py) {
        for (int t = 0; t < m.indices.length; t += 3) {
            int a = m.indices[t];
            int b = m.indices[t + 1];
            int c = m.indices[t + 2];
            if (onZ(m, a, zPlane) && onZ(m, b, zPlane) && onZ(m, c, zPlane)
                    && pointInTriangle(m, a, b, c, px, py)) {
                return true;
            }
        }
        return false;
    }

    private static boolean onZ(MeshData m, int v, float zPlane) {
        return Math.abs(m.verts[v * 3 + 2] - zPlane) < 1e-4f;
    }

    private static float triArea2D(MeshData m, int a, int b, int c) {
        float ax = m.verts[a * 3];
        float ay = m.verts[a * 3 + 1];
        float bx = m.verts[b * 3];
        float by = m.verts[b * 3 + 1];
        float cx = m.verts[c * 3];
        float cy = m.verts[c * 3 + 1];
        return Math.abs((bx - ax) * (cy - ay) - (cx - ax) * (by - ay)) * 0.5f;
    }

    private static boolean pointInTriangle(MeshData m, int a, int b, int c, float px, float py) {
        float ax = m.verts[a * 3];
        float ay = m.verts[a * 3 + 1];
        float bx = m.verts[b * 3];
        float by = m.verts[b * 3 + 1];
        float cx = m.verts[c * 3];
        float cy = m.verts[c * 3 + 1];
        float d1 = sign(px, py, ax, ay, bx, by);
        float d2 = sign(px, py, bx, by, cx, cy);
        float d3 = sign(px, py, cx, cy, ax, ay);
        boolean hasNeg = (d1 < 0) || (d2 < 0) || (d3 < 0);
        boolean hasPos = (d1 > 0) || (d2 > 0) || (d3 > 0);
        return !(hasNeg && hasPos); // same sign (or on an edge) → inside
    }

    private static float sign(float px, float py, float ax, float ay, float bx, float by) {
        return (px - bx) * (ay - by) - (ax - bx) * (py - by);
    }

    /** A CCW circle of radius {@code r} with {@code k} points, as x,y pairs (a sweep
     * cross-section). */
    private static float[] circleXY(float r, int k) {
        float[] c = new float[k * 2];
        for (int i = 0; i < k; i++) {
            double a = 2.0 * Math.PI * i / k;
            c[i * 2] = r * (float) Math.cos(a);
            c[i * 2 + 1] = r * (float) Math.sin(a);
        }
        return c;
    }

    /** Axis-aligned rectangle contour as x,y pairs; {@code cw} reverses to clockwise winding. */
    private static float[] rect(float x0, float y0, float x1, float y1, boolean cw) {
        if (!cw) {
            return new float[]{x0, y0, x1, y0, x1, y1, x0, y1}; // CCW (y-up)
        }
        return new float[]{x0, y0, x0, y1, x1, y1, x1, y0}; // CW
    }

    private static float[] packPath(
            float depth, float cx, float cy, float cz, List<float[]> contours) {
        return packPath(depth, cx, cy, cz, 0f, contours);
    }

    /** Pack EXTRUDE_PATH params: [depth, cx, cy, cz, bevel, contourCount, (count, x0,y0,…)*]. */
    private static float[] packPath(
            float depth, float cx, float cy, float cz, float bevel, List<float[]> contours) {
        int total = 6;
        for (float[] c : contours) {
            total += 1 + c.length;
        }
        float[] p = new float[total];
        p[0] = depth;
        p[1] = cx;
        p[2] = cy;
        p[3] = cz;
        p[4] = bevel;
        p[5] = contours.size();
        int w = 6;
        for (float[] c : contours) {
            p[w++] = c.length / 2f;
            for (float v : c) {
                p[w++] = v;
            }
        }
        return p;
    }
}
