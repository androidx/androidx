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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Mesh-quality and compatibility tests for {@link Primitive3D}.
 *
 * <ul>
 *   <li><b>Compatibility.</b> Builds with the default flags define what existing documents render,
 *       so they must not change. Each golden build below carries a fingerprint (counts, an index
 *       hash, and order-sensitive sums of the positions, normals and UVs) recorded from the
 *       builders before the mesh-quality work. The only intended changes are the degenerate {@code
 *       EXTRUDE_PATH} caps, which are tested for quality instead.
 *   <li><b>{@code EXTRUDE_PATH} caps.</b> No zero-area or sliver triangles, winding that agrees
 *       with the normals, and a cap area equal to the filled (nonzero-winding) area.
 *   <li><b>Contour hygiene.</b> Duplicate and non-finite points are dropped, the bevel inset is
 *       clamped per vertex, and odd input still gives a finite, well-formed mesh.
 *   <li><b>Opt-in features.</b> Curvature-spaced tube rings ({@link
 *       Primitive3D#FLAG_TUBE_CURVATURE_DENSITY}), UV for every type and the axis remap ({@link
 *       Primitive3D#axisMode}).
 * </ul>
 */
public class Primitive3DQualityTest {

    private static final int UV_FLAG = Primitive3D.UV << Primitive3D.FLAG_UV_SHIFT;
    private static final int TYPE_COUNT = 23;

    // ------------------------------------------------------------------------------------------
    // Compatibility: default-flag builds are unchanged
    // ------------------------------------------------------------------------------------------

    @Test
    public void defaultFlagBuildsAreUnchanged() {
        StringBuilder failures = new StringBuilder();
        for (Golden g : goldens()) {
            String diff = g.check(g.build(g.mFlags));
            if (diff != null) {
                failures.append('\n').append(g.mName).append(": ").append(diff);
            }
        }
        assertTrue("meshes changed:" + failures, failures.length() == 0);
    }

    @Test
    public void goldensCoverEveryType() {
        boolean[] seen = new boolean[TYPE_COUNT];
        for (Golden g : goldens()) {
            seen[g.mType] = true;
        }
        for (int type = 0; type < TYPE_COUNT; type++) {
            assertTrue("no golden for type " + type, seen[type]);
        }
    }

    // ------------------------------------------------------------------------------------------
    // EXTRUDE_PATH caps
    // ------------------------------------------------------------------------------------------

    @Test
    public void extrudePathTrianglesAreWellShaped() {
        for (Map.Entry<String, float[][]> shape : capShapes().entrySet()) {
            for (float bevel : new float[] {0f, 0.05f, 0.3f}) {
                for (float depth : new float[] {0.4f, 0.85f}) {
                    String what = shape.getKey() + " bevel " + bevel + " depth " + depth;
                    MeshData m = buildPath(depth, bevel, shape.getValue());
                    assertValidMesh(what, m);
                    assertUnitNormals(what, m);
                    assertWellShaped(what, m, 1e4, true);
                }
            }
        }
    }

    @Test
    public void kidneyRoadCapsHaveNoSlivers() {
        // The concave annulus of the review: its caps used to carry zero-area triangles and
        // dozens of triangles more than 5000 times longer than high.
        float[][] road = {kidney(96, 1f, 0f), reversed(kidney(96, 1f, -0.12f))};
        float[][] wideRoad = {kidney(200, 2f, 0f), reversed(kidney(200, 2f, -0.1f))};
        for (float bevel : new float[] {0f, 0.05f}) {
            for (float depth : new float[] {0.4f, 0.85f}) {
                assertWellShaped("road", buildPath(depth, bevel, road), 5000, true);
                assertWellShaped("wide road", buildPath(depth, bevel, wideRoad), 5000, true);
            }
        }
    }

    @Test
    public void extrudePathCapAreaIsTheFilledArea() {
        Map<String, float[][]> shapes = capShapes();
        Map<String, Double> expected = new LinkedHashMap<>();
        expected.put("square", 4.0);
        expected.put("triangle", 2.0);
        expected.put("cross", 20.0);
        expected.put("star", signedArea(shapes.get("star")[0]));
        expected.put("squareWithHole", 15.0);
        expected.put("overlap", 6.0);
        float[] a = shapes.get("overlapRotated")[0];
        float[] b = shapes.get("overlapRotated")[1];
        expected.put(
                "overlapRotated", signedArea(a) + signedArea(b) - convexIntersectionArea(a, b));
        expected.put("circle", signedArea(shapes.get("circle")[0]));
        expected.put("circleWithHole", totalSignedArea(shapes.get("circleWithHole")));
        expected.put("kidneyRoad", totalSignedArea(shapes.get("kidneyRoad")));
        expected.put("kidneyRoad200", totalSignedArea(shapes.get("kidneyRoad200")));
        expected.put("duplicateClosingPoint", 4.0);
        expected.put("duplicatePoint", 4.0);
        expected.put("tinyEdge", 4.0);
        assertEquals("every shape has an expected area", shapes.keySet(), expected.keySet());
        for (Map.Entry<String, Double> e : expected.entrySet()) {
            MeshData m = buildPath(0.4f, 0f, shapes.get(e.getKey()));
            double tol = 1e-5 * e.getValue() + 1e-6;
            assertEquals(e.getKey() + " front cap", e.getValue(), capArea(m, 0.2f, true), tol);
            assertEquals(e.getKey() + " back cap", e.getValue(), capArea(m, -0.2f, false), tol);
        }
    }

    @Test
    public void extrudePathBevelShrinksOutlineAndGrowsHole() {
        // A 4×4 square with a 2×2 hole: the bevel insets both boundaries into the material, so
        // the flat cap covers (4 - 2b)² - (2 + 2b)².
        for (float bevel : new float[] {0.1f, 0.3f}) {
            MeshData m =
                    buildPath(
                            1f, bevel, rect(-2f, -2f, 2f, 2f, false), rect(-1f, -1f, 1f, 1f, true));
            assertValidMesh("hole bevel " + bevel, m);
            assertWellShaped("hole bevel " + bevel, m, 1e4, true);
            double outer = 4.0 - 2.0 * bevel;
            double hole = 2.0 + 2.0 * bevel;
            double expected = outer * outer - hole * hole;
            assertEquals("front cap", expected, capArea(m, 0.5f, true), 1e-4);
            assertEquals("back cap", expected, capArea(m, -0.5f, false), 1e-4);
        }
    }

    // ------------------------------------------------------------------------------------------
    // Contour hygiene and the bevel clamp
    // ------------------------------------------------------------------------------------------

    @Test
    public void extrudePathDropsDuplicateAndNonFinitePoints() {
        MeshData clean = buildPath(0.4f, 0.1f, square());
        float[][] noisy = {
            {-1f, -1f, 1f, -1f, 1f, 1f, -1f, 1f, -1f, -1f},
            {-1f, -1f, 1f, -1f, 1f, -1f, 1f, 1f, -1f, 1f},
            {-1f, -1f, 1f, -1f, 1f, -1f + 1e-6f, 1f, 1f, -1f, 1f},
            {-1f, -1f, 1f, -1f, Float.NaN, 0f, 1f, 1f, -1f, 1f},
        };
        for (float[] ring : noisy) {
            assertSameMesh(Arrays.toString(ring), clean, buildPath(0.4f, 0.1f, ring));
        }
    }

    @Test
    public void extrudePathWithoutAreaIsEmpty() {
        float[][][] inputs = {
            {}, {{0f, 0f, 1f, 1f}}, {{0.5f, 0.5f, 0.5f, 0.5f, 0.5f, 0.5f}},
        };
        for (float[][] contours : inputs) {
            MeshData m = buildPath(0.4f, 0.1f, contours);
            assertEquals(Arrays.deepToString(contours), 0, m.indices.length);
            assertFinite(Arrays.deepToString(contours), m);
        }
    }

    @Test
    public void extrudePathBevelIsClampedOnShortEdges() {
        // The review's apron: offsetting a dimpled outline outward compresses it at the dimples,
        // here into edges under 0.009 long, shorter than most of these bevels.
        float[][] apron = {kidney(200, 2f, 0.9f), reversed(kidney(200, 2f, 0.6f))};
        // A neck far narrower than twice the bevel: unclamped, its inset folds through itself
        // and the chamfer turns inside out.
        float[][] neck = {rect(-1f, -0.05f, 1f, 0.05f, false)};
        for (float bevel : new float[] {0.004f, 0.008f, 0.01f, 0.014f, 0.02f, 0.05f}) {
            String what = "apron bevel " + bevel;
            MeshData m = buildPath(0.2f, bevel, apron);
            assertValidMesh(what, m);
            assertUnitNormals(what, m);
            assertWellShaped(what, m, Double.MAX_VALUE, true);
        }
        MeshData m = buildPath(1f, 0.3f, neck);
        assertValidMesh("neck", m);
        assertUnitNormals("neck", m);
        assertWellShaped("neck", m, Double.MAX_VALUE, true);
        float[] box = bounds(m);
        assertTrue(
                "neck stays inside its outline",
                box[0] >= -1f && box[3] <= 1f && box[1] >= -0.05f && box[4] <= 0.05f);
    }

    @Test
    public void extrudePathRandomOutlinesStayWellFormed() {
        Random random = new Random(20261007L);
        float[] bevels = {0f, 0.001f, 0.02f, 0.15f, 2f};
        for (int t = 0; t < 400; t++) {
            int kind = t % 4;
            float[][] contours;
            boolean simple = kind < 2;
            if (kind == 0) {
                contours = new float[][] {starShaped(random, 4 + random.nextInt(40), 0.3f, 1.5f)};
            } else if (kind == 1) {
                contours =
                        new float[][] {
                            starShaped(random, 6 + random.nextInt(40), 1.2f, 2f),
                            reversed(starShaped(random, 4 + random.nextInt(20), 0.2f, 0.6f)),
                        };
            } else if (kind == 2) {
                contours = new float[][] {randomPolygon(random, 3 + random.nextInt(12), 2f)};
            } else {
                // Grid-snapped: repeated ys, horizontal edges, collinear and touching points.
                contours =
                        new float[][] {
                            snapped(starShaped(random, 4 + random.nextInt(30), 0.5f, 2f), 0.25f),
                            snapped(randomPolygon(random, 3 + random.nextInt(8), 1.5f), 0.25f),
                        };
            }
            float bevel = bevels[(t / 4) % bevels.length];
            float depth = (t / 20) % 2 == 0 ? 0.4f : 1f;
            String what = "case " + t + " bevel " + bevel + ": " + Arrays.deepToString(contours);
            MeshData m = buildPath(depth, bevel, contours);
            if (m.indices.length == 0) {
                continue; // nothing left after hygiene
            }
            assertValidMesh(what, m);
            assertUnitNormals(what, m);
            assertWellShaped(what, m, Double.MAX_VALUE, false);
            if (simple && bevel == 0f) {
                double area = totalSignedArea(contours);
                assertEquals(what, area, capArea(m, depth / 2f, true), 1e-5 * area);
                assertEquals(what, area, capArea(m, -depth / 2f, false), 1e-5 * area);
            }
        }
    }

    @Test
    public void hasSelfIntersectionFindsFolds() {
        assertFalse(Primitive3D.hasSelfIntersection(square()));
        assertFalse(Primitive3D.hasSelfIntersection(cross()));
        assertFalse(Primitive3D.hasSelfIntersection(kidney(96, 1f, -0.12f)));
        float[] bowTie = {-1f, -1f, 1f, 1f, 1f, -1f, -1f, 1f};
        assertTrue(Primitive3D.hasSelfIntersection(bowTie));
    }

    // ------------------------------------------------------------------------------------------
    // FLAG_TUBE_CURVATURE_DENSITY
    // ------------------------------------------------------------------------------------------

    @Test
    public void curvatureDensityIsIndependentOfTheRadius() {
        float[] path = {0f, 0f, 0f, 1f, 0f, 0f, 1f, 1f, 0f, 2f, 1f, 1f};
        int curvature = Primitive3D.FLAG_TUBE_CURVATURE_DENSITY;
        for (int type : new int[] {Primitive3D.TUBE, Primitive3D.CAP_TUBE}) {
            int expected = -1;
            for (float radius : new float[] {0.01f, 0.1f, 1f}) {
                MeshData m = Primitive3D.build(type, 8, curvature, tubeParams(radius, path));
                assertValidMesh("radius " + radius, m);
                assertUnitNormals("radius " + radius, m);
                if (expected < 0) {
                    expected = vertCount(m);
                } else {
                    assertEquals("type " + type + " radius " + radius, expected, vertCount(m));
                }
            }
            // By default the rings are spaced by the facet width, so thin tubes get many more.
            MeshData thin = Primitive3D.build(type, 8, 0, tubeParams(0.01f, path));
            assertTrue("default spacing follows the radius", vertCount(thin) > 4 * expected);
        }
    }

    @Test
    public void curvatureDensityPutsOneRingPerStraightSpan() {
        float[] straight = {0f, 0f, 0f, 1f, 0f, 0f, 3f, 0f, 0f};
        for (float radius : new float[] {0.01f, 1f}) {
            MeshData m =
                    Primitive3D.build(
                            Primitive3D.TUBE,
                            8,
                            Primitive3D.FLAG_TUBE_CURVATURE_DENSITY,
                            tubeParams(radius, straight));
            assertEquals("three rings of 8 sides plus the seam", 3 * 9, vertCount(m));
        }
    }

    @Test
    public void curvatureDensityYieldsToExplicitDensityAndLegacyMode() {
        float[] path = {0f, 0f, 0f, 1f, 0f, 0f, 1f, 1f, 0f, 2f, 1f, 1f};
        int curvature = Primitive3D.FLAG_TUBE_CURVATURE_DENSITY;
        int explicit = Primitive3D.FLAG_TUBE_PATH_DENSITY;
        float[] withDensity = new float[path.length + 2];
        withDensity[0] = 0.1f;
        withDensity[1] = 3f;
        System.arraycopy(path, 0, withDensity, 2, path.length);
        assertSameMesh(
                "explicit density wins",
                Primitive3D.build(Primitive3D.TUBE, 8, explicit, withDensity),
                Primitive3D.build(Primitive3D.TUBE, 8, explicit | curvature, withDensity));
        int legacy = Primitive3D.FLAG_TUBE_LEGACY;
        assertSameMesh(
                "legacy sweep ignores the flag",
                Primitive3D.build(Primitive3D.TUBE, 8, legacy, tubeParams(0.1f, path)),
                Primitive3D.build(Primitive3D.TUBE, 8, legacy | curvature, tubeParams(0.1f, path)));
        MeshData profile =
                Primitive3D.build(
                        Primitive3D.PROFILE_TUBE,
                        8,
                        curvature | Primitive3D.FLAG_TUBE_CAP,
                        new float[] {
                            4f, 0f, 0f, 0f, 1f, 0f, 0f, 1f, 1f, 0f, 2f, 1f, 1f, 0.1f, 0.3f, 0.1f
                        });
        assertValidMesh("profile tube", profile);
        assertUnitNormals("profile tube", profile);
    }

    // ------------------------------------------------------------------------------------------
    // UV
    // ------------------------------------------------------------------------------------------

    @Test
    public void everyTypeGeneratesUvWhenRequested() {
        for (Golden g : goldens()) {
            int flags = g.mFlags & ~Primitive3D.FLAG_UV_MASK;
            MeshData without = g.build(flags);
            MeshData with = g.build(flags | UV_FLAG);
            assertNull(g.mName + " has no UV unless requested", without.uv);
            assertNotNull(g.mName + " has UV when requested", with.uv);
            assertEquals(g.mName, 2 * vertCount(with), with.uv.length);
            for (float f : with.uv) {
                assertTrue(g.mName + " uv in [0,1]: " + f, f >= 0f && f <= 1f);
            }
            if (g.mType != Primitive3D.ICOSPHERE) {
                // UV leaves the geometry alone (only the icosphere splits its seam vertices).
                assertArrayEquals(g.mName, without.verts, with.verts, 0f);
                assertArrayEquals(g.mName, without.normals, with.normals, 0f);
                assertArrayEquals(g.mName, without.indices, with.indices);
            }
            for (int axis = Primitive3D.AXIS_X; axis <= Primitive3D.AXIS_Z; axis++) {
                MeshData turned = g.build(flags | UV_FLAG | axis << Primitive3D.FLAG_AXIS_SHIFT);
                assertArrayEquals(g.mName + " uv ignores the axis", with.uv, turned.uv, 0f);
            }
        }
    }

    @Test
    public void icosphereUvDoesNotWrapAcrossTheSeam() {
        for (int level = 1; level <= 4; level++) {
            MeshData m =
                    Primitive3D.build(
                            Primitive3D.ICOSPHERE, level, UV_FLAG, new float[] {1f, 0f, 0f, 0f});
            assertValidMesh("level " + level, m);
            for (int t = 0; t < m.indices.length; t += 3) {
                float u0 = m.uv[m.indices[t] * 2];
                float u1 = m.uv[m.indices[t + 1] * 2];
                float u2 = m.uv[m.indices[t + 2] * 2];
                float span = Math.max(u0, Math.max(u1, u2)) - Math.min(u0, Math.min(u1, u2));
                assertTrue(
                        "level " + level + " triangle " + t / 3 + " spans u " + span, span <= 0.5f);
            }
        }
    }

    // ------------------------------------------------------------------------------------------
    // Axis remap
    // ------------------------------------------------------------------------------------------

    @Test
    public void flagFieldsDecodeIndependently() {
        int flags =
                Primitive3D.AXIS_Z << Primitive3D.FLAG_AXIS_SHIFT
                        | Primitive3D.UV2 << Primitive3D.FLAG_UV_SHIFT
                        | Primitive3D.FLAG_TUBE_CURVATURE_DENSITY
                        | Primitive3D.FLAG_TUBE_CAP;
        assertEquals(Primitive3D.AXIS_Z, Primitive3D.axisMode(flags));
        assertEquals(Primitive3D.UV2, Primitive3D.uvMode(flags));
        assertEquals(Primitive3D.AXIS_NATIVE, Primitive3D.axisMode(UV_FLAG));
        int sampling =
                Primitive3D.FLAG_SPLINE
                        | Primitive3D.FLAG_TUBE_LEGACY
                        | Primitive3D.FLAG_TUBE_PATH_DENSITY
                        | Primitive3D.FLAG_TUBE_CAP
                        | Primitive3D.FLAG_TUBE_CURVATURE_DENSITY;
        assertEquals(0, Primitive3D.FLAG_AXIS_MASK & Primitive3D.FLAG_UV_MASK);
        assertEquals(0, (Primitive3D.FLAG_AXIS_MASK | Primitive3D.FLAG_UV_MASK) & sampling);
    }

    @Test
    public void axisRemapIsANoOpForNativeAndExplicitlyPlacedTypes() {
        for (Golden g : goldens()) {
            int from = Primitive3D.nativeAxis(g.mType);
            MeshData mesh = g.build(g.mFlags);
            for (int axis = Primitive3D.AXIS_NATIVE; axis <= Primitive3D.AXIS_Z; axis++) {
                if (axis == Primitive3D.AXIS_NATIVE
                        || from == Primitive3D.AXIS_NATIVE
                        || axis == from) {
                    assertSameMesh(
                            g.mName + " axis " + axis,
                            mesh,
                            g.build(g.mFlags | axis << Primitive3D.FLAG_AXIS_SHIFT));
                }
            }
        }
    }

    @Test
    public void axisRemapIsARigidQuarterTurnAboutThePivot() {
        // Every input is centered at the pivot (1,2,3) (the cone at its base center).
        int remapped = 0;
        for (Golden g : centeredInputs()) {
            int from = Primitive3D.nativeAxis(g.mType);
            assertTrue(g.mName + " has a native axis", from != Primitive3D.AXIS_NATIVE);
            MeshData mesh = g.build(g.mFlags);
            for (int to = Primitive3D.AXIS_X; to <= Primitive3D.AXIS_Z; to++) {
                if (to == from) {
                    continue;
                }
                MeshData turned = g.build(g.mFlags | to << Primitive3D.FLAG_AXIS_SHIFT);
                String what = g.mName + " axis " + from + "->" + to;
                assertArrayEquals(what, mesh.indices, turned.indices);
                assertQuarterTurn(what, mesh.verts, turned.verts, from, to, PIVOT);
                assertQuarterTurn(what, mesh.normals, turned.normals, from, to, ORIGIN);
                remapped++;
            }
        }
        assertEquals("two remaps per type with a native axis", 2 * 18, remapped);
    }

    // ------------------------------------------------------------------------------------------
    // Inputs
    // ------------------------------------------------------------------------------------------

    private static final float[] PIVOT = {1f, 2f, 3f};
    private static final float[] ORIGIN = {0f, 0f, 0f};

    /** A build input and, for the compatibility goldens, the fingerprint it must reproduce. */
    static final class Golden {
        final String mName;
        final int mType;
        final float mSegments;
        final int mFlags;
        final float[][] mData;
        int mVerts;
        int mIndices;
        int mIndexHash;
        double mPositions;
        double mNormals;
        double mUvs;

        Golden(String name, int type, float segments, int flags, float[]... data) {
            mName = name;
            mType = type;
            mSegments = segments;
            mFlags = flags;
            mData = data;
        }

        /** Sets the recorded fingerprint; {@code uvs} is NaN for a build without UV. */
        Golden expect(
                int verts,
                int indices,
                int indexHash,
                double positions,
                double normals,
                double uvs) {
            mVerts = verts;
            mIndices = indices;
            mIndexHash = indexHash;
            mPositions = positions;
            mNormals = normals;
            mUvs = uvs;
            return this;
        }

        /** Builds this input with {@code flags} (on a copy of the params). */
        MeshData build(int flags) {
            float[][] data = new float[mData.length][];
            for (int i = 0; i < data.length; i++) {
                data[i] = mData[i].clone();
            }
            return Primitive3D.build(mType, mSegments, flags, data);
        }

        /** Null if {@code m} matches the recorded fingerprint, else what differs. */
        String check(MeshData m) {
            if (m.verts.length / 3 != mVerts || m.indices.length != mIndices) {
                return "counts "
                        + m.verts.length / 3
                        + "/"
                        + m.indices.length
                        + ", expected "
                        + mVerts
                        + "/"
                        + mIndices;
            }
            if (Arrays.hashCode(m.indices) != mIndexHash) {
                return "indices";
            }
            if (Math.abs(moment(m.verts) - mPositions) > tolerance(m.verts)) {
                return "positions";
            }
            if (Math.abs(moment(m.normals) - mNormals) > tolerance(m.normals)) {
                return "normals";
            }
            if ((m.uv == null) != Double.isNaN(mUvs)) {
                return "uv presence";
            }
            if (m.uv != null && Math.abs(moment(m.uv) - mUvs) > tolerance(m.uv)) {
                return "uvs";
            }
            return null;
        }
    }

    /** Order-sensitive fingerprint of a mesh array: {@code Σ a[i]·(1 + i mod 13)}. */
    static double moment(float[] a) {
        double s = 0;
        for (int i = 0; i < a.length; i++) {
            s += a[i] * (double) (1 + i % 13);
        }
        return s;
    }

    /** Tolerance for {@link #moment}: far above float noise, far below any real change. */
    static double tolerance(float[] a) {
        double s = 0;
        for (int i = 0; i < a.length; i++) {
            s += Math.abs(a[i]) * (double) (1 + i % 13);
        }
        return 1e-6 * s + 1e-6;
    }

    /**
     * One or more inputs per type, with the fingerprints the builders produced before the
     * mesh-quality work (default flags, plus UV for the types that already had it).
     */
    static List<Golden> goldens() {
        List<Golden> g = new ArrayList<>();
        g.add(
                new Golden("sphere", Primitive3D.SPHERE, 12, 0, ch(0.5f, 0.1f, -0.2f, 0.3f))
                        .expect(91, 432, 0x57d433f1, 136.913134, 19.026255, Double.NaN));
        g.add(
                new Golden("sphereUv", Primitive3D.SPHERE, 0, UV_FLAG, ch(1f, 0f, 0f, 0f))
                        .expect(325, 1728, 0xc1487141, 34.106775, 34.106775, 2267.416667));
        g.add(
                new Golden(
                                "cylinder",
                                Primitive3D.CYLINDER,
                                10,
                                0,
                                ch(0.25f, 0f, -1f, 0f, 0f, 1f, 0f))
                        .expect(44, 120, 0x93f57a89, 25.714068, 6.169526, Double.NaN));
        g.add(
                new Golden(
                                "cylinderTilted",
                                Primitive3D.CYLINDER,
                                0,
                                0,
                                ch(0.3f, -1f, 0f, 0.5f, 1f, 1f, -0.5f))
                        .expect(100, 288, 0x363a8191, 361.589503, 8.343054, Double.NaN));
        g.add(
                new Golden("cone", Primitive3D.CONE, 16, 0, ch(0.5f, 1.2f, 0f, 0f, 0f))
                        .expect(51, 96, 0xa7c95cf9, 136.136910, -25.153012, Double.NaN));
        g.add(
                new Golden("cube", Primitive3D.CUBE, 0, 0, ch(2f, 1f, 0.5f, 0.1f, 0.2f, 0.3f))
                        .expect(24, 36, 0xa9a42d01, 117.450004, -1.000000, Double.NaN));
        g.add(
                new Golden("cubeUv", Primitive3D.CUBE, 0, UV_FLAG, ch(1f, 1f, 1f, 0f, 0f, 0f))
                        .expect(24, 36, 0xa9a42d01, 10.500000, -1.000000, 157.000000));
        g.add(
                new Golden(
                                "roundedCube",
                                Primitive3D.ROUNDED_CUBE,
                                4,
                                0,
                                ch(2f, 1f, 0.5f, 0f, 0f, 0f, 0.2f))
                        .expect(150, 576, 0x75a53b81, 30.804423, 9.022113, Double.NaN));
        g.add(
                new Golden(
                                "sphericalSector",
                                Primitive3D.SPHERICAL_SECTOR,
                                12,
                                0,
                                ch(1f, 0.8f, 0f, 0f, 0f))
                        .expect(91, 324, 0x6143fca7, 479.099751, 288.587224, Double.NaN));
        g.add(
                new Golden(
                                "sphericalDome",
                                Primitive3D.SPHERICAL_DOME,
                                12,
                                0,
                                ch(1f, 1.5707964f, 0f, 0f, 0f))
                        .expect(78, 324, 0x49afc455, 283.705929, 198.607857, Double.NaN));
        g.add(
                new Golden("tube", Primitive3D.TUBE, 8, 0, tubeParams(0.1f, BENT_PATH))
                        .expect(405, 2112, 0x3ade3641, 5052.231229, 176.585788, Double.NaN));
        g.add(
                new Golden(
                                "tubeLegacy",
                                Primitive3D.TUBE,
                                8,
                                Primitive3D.FLAG_TUBE_LEGACY,
                                tubeParams(0.1f, BENT_PATH))
                        .expect(36, 144, 0x2b5f9b91, 437.310111, 13.101125, Double.NaN));
        g.add(
                new Golden(
                                "tubeLegacySpline",
                                Primitive3D.TUBE,
                                8,
                                Primitive3D.FLAG_TUBE_LEGACY | Primitive3D.FLAG_SPLINE,
                                tubeParams(0.1f, BENT_PATH))
                        .expect(225, 1152, 0xbb427081, 2628.233655, -50.261151, Double.NaN));
        g.add(
                new Golden(
                                "tubePathDensity",
                                Primitive3D.TUBE,
                                8,
                                Primitive3D.FLAG_TUBE_PATH_DENSITY,
                                ch(0.1f, 3f, 0f, 0f, 0f, 1f, 0f, 0f, 1f, 1f, 0f, 2f, 1f, 1f))
                        .expect(90, 432, 0x853d6bb1, 1062.258165, 41.470561, Double.NaN));
        g.add(
                new Golden(
                                "capTube",
                                Primitive3D.CAP_TUBE,
                                10,
                                0,
                                ch(0.2f, 0f, 0f, 0f, 0f, 1f, 0f, 1f, 2f, 0f))
                        .expect(253, 1260, 0x6f885b01, 2416.951701, -112.187065, Double.NaN));
        g.add(
                new Golden(
                                "profileTube",
                                Primitive3D.PROFILE_TUBE,
                                12,
                                0,
                                ch(3f, 0f, 0f, 0f, 0f, 1f, 0f, 0.5f, 2f, 0f, 0.2f, 0.4f, 0.1f))
                        .expect(195, 1008, 0x6cb65cd1, 1547.264204, -316.922219, Double.NaN));
        g.add(
                new Golden(
                                "profileTubeCapped",
                                Primitive3D.PROFILE_TUBE,
                                12,
                                Primitive3D.FLAG_TUBE_CAP,
                                ch(3f, 0f, 0f, 0f, 0f, 1f, 0f, 0.5f, 2f, 0f, 0.2f, 0.4f, 0.1f))
                        .expect(221, 1080, 0x526bae91, 1771.979796, -285.832884, Double.NaN));
        g.add(
                new Golden("torus", Primitive3D.TORUS, 20, 0, ch(1f, 0.25f, 0f, 0f, 0f))
                        .expect(126, 600, 0xb852bc3d, 48.151056, 12.404289, Double.NaN));
        g.add(
                new Golden("torusUv", Primitive3D.TORUS, 0, UV_FLAG, ch(1f, 0.4f, 0.5f, 0f, 0f))
                        .expect(275, 1440, 0x49507d41, 1029.483478, 7.893853, 1914.366671));
        g.add(
                new Golden("plane", Primitive3D.PLANE, 0, 0, ch(2f, 1f, 0f, 0f, 0f))
                        .expect(4, 6, 0x34f53bc1, 6.000000, 30.000000, Double.NaN));
        g.add(
                new Golden("planeUv", Primitive3D.PLANE, 0, UV_FLAG, ch(2f, 1f, 0f, 0f, 0.5f))
                        .expect(4, 6, 0x34f53bc1, 21.000000, 30.000000, 22.000000));
        g.add(
                new Golden(
                                "extrudeCircle",
                                Primitive3D.EXTRUDE_CIRCLE,
                                0,
                                0,
                                ch(1f, 0.3f, 0f, 0f, 0f))
                        .expect(146, 288, 0xf5fb4539, -5.787787, -8.745973, Double.NaN));
        g.add(
                new Golden(
                                "extrudeSector",
                                Primitive3D.EXTRUDE_SECTOR,
                                12,
                                0,
                                ch(1f, 0.2f, 1.5f, 0.3f, 0f, 0f, 0f))
                        .expect(84, 156, 0xa0821897, 685.714012, 370.323655, Double.NaN));
        g.add(
                new Golden(
                                "extrudeSegment",
                                Primitive3D.EXTRUDE_SEGMENT,
                                12,
                                0,
                                ch(1f, 0.2f, 2.5f, 0.3f, 0f, 0f, 0f))
                        .expect(78, 144, 0xe8cf5265, 423.162496, 240.174138, Double.NaN));
        g.add(
                new Golden(
                                "extrudeArc",
                                Primitive3D.EXTRUDE_ARC,
                                16,
                                0,
                                ch(0.5f, 1f, 0.3f, 4f, 0.2f, 0f, 0f, 0f))
                        .expect(204, 396, 0x772c0bdb, 50.131027, 13.815786, Double.NaN));
        g.add(
                new Golden(
                                "extrudeRoundedRect",
                                Primitive3D.EXTRUDE_ROUNDED_RECT,
                                6,
                                0,
                                ch(2f, 1f, 0.25f, 0.3f, 0f, 0f, 0f))
                        .expect(170, 336, 0xd56cfa6d, 2.265191, -7.692887, Double.NaN));
        g.add(
                new Golden(
                                "extrudeSquircle",
                                Primitive3D.EXTRUDE_SQUIRCLE,
                                0,
                                0,
                                ch(1f, 4f, 0.3f, 0f, 0f, 0f))
                        .expect(290, 576, 0x46791271, 4.944480, -13.113781, Double.NaN));
        g.add(
                new Golden(
                                "extrudePathSquare",
                                Primitive3D.EXTRUDE_PATH,
                                0,
                                0,
                                path(0.4f, 0f, square()))
                        .expect(24, 36, 0x220de5b5, -77.000000, -9.000000, Double.NaN));
        g.add(
                new Golden(
                                "extrudePathSquareBevel",
                                Primitive3D.EXTRUDE_PATH,
                                0,
                                0,
                                path(0.4f, 0.1f, square()))
                        .expect(56, 84, 0x8365ac9d, -21.699999, -0.949747, Double.NaN));
        g.add(
                new Golden(
                                "extrudePathCross",
                                Primitive3D.EXTRUDE_PATH,
                                0,
                                0,
                                path(0.4f, 0f, cross()))
                        .expect(72, 108, 0x35a47f1d, -12.800000, -7.000000, Double.NaN));
        g.add(
                new Golden(
                                "extrudePathHole",
                                Primitive3D.EXTRUDE_PATH,
                                0,
                                0,
                                path(
                                        0.4f,
                                        0f,
                                        rect(-2f, -2f, 2f, 2f, false),
                                        rect(-0.5f, -0.5f, 0.5f, 0.5f, true)))
                        .expect(64, 96, 0x4ee42be9, -93.400000, -14.000000, Double.NaN));
        g.add(
                new Golden(
                                "lathe",
                                Primitive3D.LATHE,
                                16,
                                0,
                                ch(0f, 0f, 0f),
                                ch(0.2f, 0f, 0.6f, 0.5f, 0.35f, 1.2f))
                        .expect(51, 192, 0x08eeadc1, 214.485375, -28.681850, Double.NaN));
        g.add(
                new Golden(
                                "latheClosed",
                                Primitive3D.LATHE,
                                12,
                                0,
                                ch(0f, 0f, 0f),
                                ch(0f, -1f, 0.8f, -0.5f, 0.8f, 0.5f, 0f, 1f))
                        .expect(52, 216, 0x743a63c9, 8.156922, 14.321938, Double.NaN));
        g.add(
                new Golden(
                                "sweep",
                                Primitive3D.SWEEP,
                                0,
                                0,
                                ch(1f, 1f, 1f, 0f, 0f, 0f),
                                ch(-0.2f, -0.2f, 0.2f, -0.2f, 0.2f, 0.2f, -0.2f, 0.2f),
                                ch(0f, 0f, 0f, 0f, 1f, 0f, 1f, 2f, 0f, 2f, 2f, 1f),
                                ch(1f, 0.8f, 0.6f, 0.4f),
                                ch(0f, 0.2f, 0.4f, 0.6f))
                        .expect(30, 96, 0x90175db9, 492.008973, -12.675579, Double.NaN));
        g.add(
                new Golden(
                                "sweepOpen",
                                Primitive3D.SWEEP,
                                0,
                                0,
                                ch(0f, 0f, 0f, 0f, 0f, 0f),
                                ch(-0.3f, 0f, 0f, 0.1f, 0.3f, 0f),
                                ch(0f, 0f, 0f, 0f, 0f, 1f, 0.5f, 0f, 2f))
                        .expect(9, 24, 0x3fd0e3f1, 72.074272, 44.773868, Double.NaN));
        g.add(
                new Golden("helix", Primitive3D.HELIX, 10, 0, ch(1f, 0.15f, 0.5f, 3f, 0f, 0f, 0f))
                        .expect(693, 3660, 0x05253ac1, 188.738997, -305.524358, Double.NaN));
        g.add(
                new Golden("icosphere", Primitive3D.ICOSPHERE, 0, 0, ch(1.5f, 0f, 0f, 0f))
                        .expect(162, 960, 0x16c0dbc7, -68.395275, -45.596850, Double.NaN));
        g.add(
                new Golden("icosphereLevel3", Primitive3D.ICOSPHERE, 3, 0, ch(1f, 0.5f, 0f, 0f))
                        .expect(642, 3840, 0xee6b5f45, 2188.057406, -58.942593, Double.NaN));
        return g;
    }

    /** One input per type with a native axis, each centered (or, for the cone, based) at PIVOT. */
    private static List<Golden> centeredInputs() {
        List<Golden> g = new ArrayList<>();
        g.add(new Golden("sphere", Primitive3D.SPHERE, 12, UV_FLAG, ch(0.5f, 1f, 2f, 3f)));
        g.add(new Golden("cone", Primitive3D.CONE, 16, 0, ch(0.5f, 1.2f, 1f, 2f, 3f)));
        g.add(new Golden("cube", Primitive3D.CUBE, 0, 0, ch(2f, 1f, 0.5f, 1f, 2f, 3f)));
        g.add(
                new Golden(
                        "roundedCube",
                        Primitive3D.ROUNDED_CUBE,
                        3,
                        0,
                        ch(2f, 1f, 0.5f, 1f, 2f, 3f, 0.2f)));
        g.add(
                new Golden(
                        "sphericalSector",
                        Primitive3D.SPHERICAL_SECTOR,
                        12,
                        0,
                        ch(1f, 0.8f, 1f, 2f, 3f)));
        g.add(
                new Golden(
                        "sphericalDome",
                        Primitive3D.SPHERICAL_DOME,
                        12,
                        0,
                        ch(1f, 1.2f, 1f, 2f, 3f)));
        g.add(new Golden("torus", Primitive3D.TORUS, 16, 0, ch(1f, 0.25f, 1f, 2f, 3f)));
        g.add(
                new Golden(
                        "lathe",
                        Primitive3D.LATHE,
                        12,
                        0,
                        ch(1f, 2f, 3f),
                        ch(0.2f, 0f, 0.6f, 0.5f, 0.35f, 1.2f)));
        g.add(new Golden("helix", Primitive3D.HELIX, 8, 0, ch(1f, 0.15f, 0.5f, 2f, 1f, 2f, 3f)));
        g.add(new Golden("icosphere", Primitive3D.ICOSPHERE, 1, 0, ch(1f, 1f, 2f, 3f)));
        g.add(new Golden("plane", Primitive3D.PLANE, 0, 0, ch(2f, 1f, 1f, 2f, 3f)));
        g.add(
                new Golden(
                        "extrudeCircle",
                        Primitive3D.EXTRUDE_CIRCLE,
                        12,
                        0,
                        ch(1f, 0.3f, 1f, 2f, 3f)));
        g.add(
                new Golden(
                        "extrudeSector",
                        Primitive3D.EXTRUDE_SECTOR,
                        8,
                        0,
                        ch(1f, 0.2f, 1.5f, 0.3f, 1f, 2f, 3f)));
        g.add(
                new Golden(
                        "extrudeSegment",
                        Primitive3D.EXTRUDE_SEGMENT,
                        8,
                        0,
                        ch(1f, 0.2f, 2.5f, 0.3f, 1f, 2f, 3f)));
        g.add(
                new Golden(
                        "extrudeArc",
                        Primitive3D.EXTRUDE_ARC,
                        8,
                        0,
                        ch(0.5f, 1f, 0.3f, 4f, 0.2f, 1f, 2f, 3f)));
        g.add(
                new Golden(
                        "extrudeRoundedRect",
                        Primitive3D.EXTRUDE_ROUNDED_RECT,
                        4,
                        0,
                        ch(2f, 1f, 0.25f, 0.3f, 1f, 2f, 3f)));
        g.add(
                new Golden(
                        "extrudeSquircle",
                        Primitive3D.EXTRUDE_SQUIRCLE,
                        16,
                        0,
                        ch(1f, 4f, 0.3f, 1f, 2f, 3f)));
        float[] p = path(0.4f, 0.1f, cross());
        p[1] = 1f;
        p[2] = 2f;
        p[3] = 3f;
        g.add(new Golden("extrudePath", Primitive3D.EXTRUDE_PATH, 0, 0, p));
        return g;
    }

    private static final float[] BENT_PATH = {0f, 0f, 0f, 1f, 0f, 0f, 1f, 1f, 0f, 2f, 1f, 1f};

    /** The {@code EXTRUDE_PATH} outlines the cap tests run over. */
    private static Map<String, float[][]> capShapes() {
        Map<String, float[][]> s = new LinkedHashMap<>();
        s.put("square", new float[][] {square()});
        s.put("triangle", new float[][] {{0f, 0f, 2f, 0f, 1f, 2f}});
        s.put("cross", new float[][] {cross()});
        s.put("star", new float[][] {star(5, 1f, 0.4f)});
        s.put(
                "squareWithHole",
                new float[][] {
                    rect(-2f, -2f, 2f, 2f, false), rect(-0.5f, -0.5f, 0.5f, 0.5f, true)
                });
        s.put("overlap", new float[][] {square(), rect(0f, -1f, 2f, 1f, false)});
        float[] turned = square();
        for (int i = 0; i < turned.length; i += 2) {
            float x = turned[i];
            float y = turned[i + 1];
            turned[i] = x * 0.8f - y * 0.6f + 0.5f;
            turned[i + 1] = x * 0.6f + y * 0.8f;
        }
        s.put("overlapRotated", new float[][] {square(), turned});
        s.put("circle", new float[][] {circle(1f, 64)});
        s.put("circleWithHole", new float[][] {circle(1f, 64), reversed(circle(0.5f, 48))});
        s.put("kidneyRoad", new float[][] {kidney(96, 1f, 0f), reversed(kidney(96, 1f, -0.12f))});
        s.put(
                "kidneyRoad200",
                new float[][] {kidney(200, 2f, 0f), reversed(kidney(200, 2f, -0.1f))});
        s.put(
                "duplicateClosingPoint",
                new float[][] {{-1f, -1f, 1f, -1f, 1f, 1f, -1f, 1f, -1f, -1f}});
        s.put("duplicatePoint", new float[][] {{-1f, -1f, 1f, -1f, 1f, -1f, 1f, 1f, -1f, 1f}});
        s.put("tinyEdge", new float[][] {{-1f, -1f, 1f, -1f, 1f, -1f + 1e-6f, 1f, 1f, -1f, 1f}});
        return s;
    }

    private static float[] ch(float... values) {
        return values;
    }

    /** {@code TUBE}/{@code CAP_TUBE} params: the radius, then the path points. */
    private static float[] tubeParams(float radius, float[] points) {
        float[] p = new float[points.length + 1];
        p[0] = radius;
        System.arraycopy(points, 0, p, 1, points.length);
        return p;
    }

    /** {@code EXTRUDE_PATH} params centered at the origin. */
    private static float[] path(float depth, float bevel, float[]... contours) {
        int n = 6;
        for (float[] c : contours) {
            n += 1 + c.length;
        }
        float[] p = new float[n];
        p[0] = depth;
        p[4] = bevel;
        p[5] = contours.length;
        int k = 6;
        for (float[] c : contours) {
            p[k++] = c.length / 2;
            for (float v : c) {
                p[k++] = v;
            }
        }
        return p;
    }

    private static MeshData buildPath(float depth, float bevel, float[]... contours) {
        return Primitive3D.build(Primitive3D.EXTRUDE_PATH, 0, 0, path(depth, bevel, contours));
    }

    private static float[] square() {
        return new float[] {-1f, -1f, 1f, -1f, 1f, 1f, -1f, 1f};
    }

    private static float[] cross() {
        return new float[] {
            -1f, -3f, 1f, -3f, 1f, -1f, 3f, -1f, 3f, 1f, 1f, 1f,
            1f, 3f, -1f, 3f, -1f, 1f, -3f, 1f, -3f, -1f, -1f, -1f
        };
    }

    /** Axis-aligned rectangle, CCW, or CW (a hole) when {@code cw}. */
    private static float[] rect(float x0, float y0, float x1, float y1, boolean cw) {
        return cw
                ? new float[] {x0, y0, x0, y1, x1, y1, x1, y0}
                : new float[] {x0, y0, x1, y0, x1, y1, x0, y1};
    }

    /** CCW star with {@code points} tips, the first at the top. */
    private static float[] star(int points, float outer, float inner) {
        float[] s = new float[points * 4];
        for (int i = 0; i < points * 2; i++) {
            double a = Math.PI / 2 + Math.PI * i / points;
            double r = i % 2 == 0 ? outer : inner;
            s[i * 2] = (float) (r * StrictMath.cos(a));
            s[i * 2 + 1] = (float) (r * StrictMath.sin(a));
        }
        return s;
    }

    /** CCW regular polygon of {@code n} points on a circle of radius {@code r}. */
    private static float[] circle(float r, int n) {
        float[] c = new float[n * 2];
        for (int i = 0; i < n; i++) {
            double a = 2 * Math.PI * i / n;
            c[i * 2] = (float) (r * StrictMath.cos(a));
            c[i * 2 + 1] = (float) (r * StrictMath.sin(a));
        }
        return c;
    }

    /**
     * The concave kidney outline of the review's road ({@code n} CCW points), offset by {@code
     * offset} along its outward normal and then scaled by {@code scale}.
     */
    private static float[] kidney(int n, float scale, float offset) {
        double[] x = new double[n];
        double[] y = new double[n];
        for (int i = 0; i < n; i++) {
            double t = 2 * Math.PI * i / n;
            double r = 1 + 0.35 * StrictMath.cos(2 * t) - 0.25 * StrictMath.cos(t);
            x[i] = 1.3 * r * StrictMath.cos(t);
            y[i] = 0.8 * r * StrictMath.sin(t);
        }
        float[] out = new float[n * 2];
        for (int i = 0; i < n; i++) {
            double dx = x[(i + 1) % n] - x[(i + n - 1) % n];
            double dy = y[(i + 1) % n] - y[(i + n - 1) % n];
            double len = StrictMath.hypot(dx, dy);
            out[i * 2] = (float) ((x[i] + offset * dy / len) * scale);
            out[i * 2 + 1] = (float) ((y[i] - offset * dx / len) * scale);
        }
        return out;
    }

    /** A star-shaped (hence simple) CCW polygon around the origin. */
    private static float[] starShaped(Random random, int n, float rMin, float rMax) {
        float[] c = new float[n * 2];
        for (int i = 0; i < n; i++) {
            // Jitter below one step keeps the angles increasing and every gap under π (n >= 4).
            double a = 2 * Math.PI * (i + 0.8 * random.nextDouble()) / n;
            double r = rMin + (rMax - rMin) * random.nextDouble();
            c[i * 2] = (float) (r * StrictMath.cos(a));
            c[i * 2 + 1] = (float) (r * StrictMath.sin(a));
        }
        return c;
    }

    /** Random points: usually a self-intersecting outline. */
    private static float[] randomPolygon(Random random, int n, float size) {
        float[] c = new float[n * 2];
        for (int i = 0; i < c.length; i++) {
            c[i] = (float) ((random.nextDouble() * 2 - 1) * size);
        }
        return c;
    }

    private static float[] snapped(float[] ring, float grid) {
        float[] out = ring.clone();
        for (int i = 0; i < out.length; i++) {
            out[i] = Math.round(out[i] / grid) * grid;
        }
        return out;
    }

    private static float[] reversed(float[] ring) {
        int n = ring.length / 2;
        float[] out = new float[ring.length];
        for (int i = 0; i < n; i++) {
            out[i * 2] = ring[(n - 1 - i) * 2];
            out[i * 2 + 1] = ring[(n - 1 - i) * 2 + 1];
        }
        return out;
    }

    // ------------------------------------------------------------------------------------------
    // Geometry and assertions
    // ------------------------------------------------------------------------------------------

    /** Shoelace area of a ring: positive for CCW. */
    private static double signedArea(float[] ring) {
        int n = ring.length / 2;
        double a = 0;
        for (int i = 0; i < n; i++) {
            int j = (i + 1) % n;
            a += (double) ring[i * 2] * ring[j * 2 + 1] - (double) ring[j * 2] * ring[i * 2 + 1];
        }
        return 0.5 * a;
    }

    /** The filled area of nested, non-overlapping contours (holes wound CW). */
    private static double totalSignedArea(float[][] contours) {
        double a = 0;
        for (float[] ring : contours) {
            a += signedArea(ring);
        }
        return a;
    }

    /** Area of the intersection of two convex CCW polygons (Sutherland–Hodgman clipping). */
    private static double convexIntersectionArea(float[] subject, float[] clip) {
        List<double[]> poly = new ArrayList<>();
        for (int i = 0; i < subject.length; i += 2) {
            poly.add(new double[] {subject[i], subject[i + 1]});
        }
        int n = clip.length / 2;
        for (int i = 0; i < n && !poly.isEmpty(); i++) {
            double ax = clip[i * 2];
            double ay = clip[i * 2 + 1];
            double ex = clip[(i + 1) % n * 2] - ax;
            double ey = clip[(i + 1) % n * 2 + 1] - ay;
            List<double[]> in = poly;
            poly = new ArrayList<>();
            for (int j = 0; j < in.size(); j++) {
                double[] p = in.get(j);
                double[] q = in.get((j + 1) % in.size());
                double sp = ex * (p[1] - ay) - ey * (p[0] - ax);
                double sq = ex * (q[1] - ay) - ey * (q[0] - ax);
                if (sp >= 0) {
                    poly.add(p);
                }
                if ((sp >= 0) != (sq >= 0)) {
                    double t = sp / (sp - sq);
                    poly.add(new double[] {p[0] + t * (q[0] - p[0]), p[1] + t * (q[1] - p[1])});
                }
            }
        }
        double a = 0;
        for (int i = 0; i < poly.size(); i++) {
            double[] p = poly.get(i);
            double[] q = poly.get((i + 1) % poly.size());
            a += p[0] * q[1] - q[0] * p[1];
        }
        return 0.5 * a;
    }

    /**
     * Signed area of the cap on the plane {@code z == zPlane}, seen from the side it faces: the
     * triangles whose vertices all lie on the plane with normals along +Z ({@code front}) or -Z. A
     * triangle wound against its normal subtracts.
     */
    private static double capArea(MeshData m, float zPlane, boolean front) {
        double area = 0;
        for (int t = 0; t < m.indices.length; t += 3) {
            int a = m.indices[t] * 3;
            int b = m.indices[t + 1] * 3;
            int c = m.indices[t + 2] * 3;
            boolean onCap = true;
            for (int v : new int[] {a, b, c}) {
                onCap &=
                        Math.abs(m.verts[v + 2] - zPlane) < 1e-5f
                                && (front ? m.normals[v + 2] > 0.999f : m.normals[v + 2] < -0.999f);
            }
            if (onCap) {
                double cross =
                        (m.verts[b] - (double) m.verts[a])
                                        * (m.verts[c + 1] - (double) m.verts[a + 1])
                                - (m.verts[c] - (double) m.verts[a])
                                        * (m.verts[b + 1] - (double) m.verts[a + 1]);
                area += front ? 0.5 * cross : -0.5 * cross;
            }
        }
        return area;
    }

    /**
     * Asserts every triangle has a clearly non-zero area, an aspect (longest edge over height) of
     * at most {@code maxAspect}, and, for all triangles or only flat cap triangles, a winding that
     * agrees with its vertex normals.
     */
    private static void assertWellShaped(
            String what, MeshData m, double maxAspect, boolean allWindings) {
        float[] v = m.verts;
        float[] n = m.normals;
        for (int t = 0; t < m.indices.length; t += 3) {
            int a = m.indices[t] * 3;
            int b = m.indices[t + 1] * 3;
            int c = m.indices[t + 2] * 3;
            double e1x = v[b] - (double) v[a];
            double e1y = v[b + 1] - (double) v[a + 1];
            double e1z = v[b + 2] - (double) v[a + 2];
            double e2x = v[c] - (double) v[a];
            double e2y = v[c + 1] - (double) v[a + 1];
            double e2z = v[c + 2] - (double) v[a + 2];
            double e3x = e2x - e1x;
            double e3y = e2y - e1y;
            double e3z = e2z - e1z;
            double cx = e1y * e2z - e1z * e2y;
            double cy = e1z * e2x - e1x * e2z;
            double cz = e1x * e2y - e1y * e2x;
            double area2 = Math.sqrt(cx * cx + cy * cy + cz * cz);
            double lmax =
                    Math.max(
                            e1x * e1x + e1y * e1y + e1z * e1z,
                            Math.max(
                                    e2x * e2x + e2y * e2y + e2z * e2z,
                                    e3x * e3x + e3y * e3y + e3z * e3z));
            String tri = what + ": triangle " + t / 3;
            assertTrue(tri + " has zero area", area2 > 1e-12 * lmax && area2 > 0);
            assertTrue(tri + " is a sliver, aspect " + lmax / area2, lmax / area2 <= maxAspect);
            boolean flatCap =
                    v[a + 2] == v[b + 2] && v[b + 2] == v[c + 2] && Math.abs(n[a + 2]) > 0.99f;
            if (allWindings || flatCap) {
                double nx = n[a] + n[b] + n[c];
                double ny = n[a + 1] + n[b + 1] + n[c + 1];
                double nz = n[a + 2] + n[b + 2] + n[c + 2];
                assertTrue(tri + " winds against its normals", cx * nx + cy * ny + cz * nz > 0);
            }
        }
    }

    /**
     * Asserts {@code turned} is {@code original} rotated a quarter turn about {@code pivot} that
     * takes +{@code from} to +{@code to}: the native-axis offset moves to the target axis, the
     * target-axis offset comes back negated, and the third component is untouched.
     */
    private static void assertQuarterTurn(
            String what, float[] original, float[] turned, int from, int to, float[] pivot) {
        assertEquals(what, original.length, turned.length);
        int f = from - 1;
        int t = to - 1;
        int k = 3 - f - t;
        for (int i = 0; i < original.length; i += 3) {
            assertEquals(what, original[i + f] - pivot[f], turned[i + t] - pivot[t], 1e-5f);
            assertEquals(what, -(original[i + t] - pivot[t]), turned[i + f] - pivot[f], 1e-5f);
            assertEquals(what, original[i + k], turned[i + k], 0f);
        }
    }

    private static void assertSameMesh(String what, MeshData expected, MeshData actual) {
        assertArrayEquals(what + " verts", expected.verts, actual.verts, 0f);
        assertArrayEquals(what + " normals", expected.normals, actual.normals, 0f);
        assertArrayEquals(what + " indices", expected.indices, actual.indices);
        if (expected.uv == null) {
            assertNull(what + " uv", actual.uv);
        } else {
            assertNotNull(what + " uv", actual.uv);
            assertArrayEquals(what + " uv", expected.uv, actual.uv, 0f);
        }
    }

    /** Asserts the mesh arrays are consistent, finite and index in range. */
    private static void assertValidMesh(String what, MeshData m) {
        assertEquals(what + ": xyz triples", 0, m.verts.length % 3);
        assertEquals(what + ": one normal per vertex", m.verts.length, m.normals.length);
        assertEquals(what + ": triangles", 0, m.indices.length % 3);
        assertTrue(what + ": has triangles", m.indices.length > 0);
        int vc = vertCount(m);
        for (int idx : m.indices) {
            assertTrue(what + ": index " + idx + " in range", idx >= 0 && idx < vc);
        }
        if (m.uv != null) {
            assertEquals(what + ": two uv per vertex", 2 * vc, m.uv.length);
        }
        assertFinite(what, m);
    }

    private static void assertFinite(String what, MeshData m) {
        for (float f : m.verts) {
            assertTrue(what + ": finite vertex", Float.isFinite(f));
        }
        for (float f : m.normals) {
            assertTrue(what + ": finite normal", Float.isFinite(f));
        }
        if (m.uv != null) {
            for (float f : m.uv) {
                assertTrue(what + ": finite uv", Float.isFinite(f));
            }
        }
    }

    private static void assertUnitNormals(String what, MeshData m) {
        for (int i = 0; i < m.normals.length; i += 3) {
            double len =
                    Math.sqrt(
                            m.normals[i] * m.normals[i]
                                    + m.normals[i + 1] * m.normals[i + 1]
                                    + m.normals[i + 2] * m.normals[i + 2]);
            assertEquals(what + ": unit normal " + i / 3, 1.0, len, 1e-3);
        }
    }

    /** Axis-aligned bounds {@code {minX, minY, minZ, maxX, maxY, maxZ}}. */
    private static float[] bounds(MeshData m) {
        float[] b = {
            Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE,
            -Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE
        };
        for (int i = 0; i < m.verts.length; i += 3) {
            for (int a = 0; a < 3; a++) {
                b[a] = Math.min(b[a], m.verts[i + a]);
                b[a + 3] = Math.max(b[a + 3], m.verts[i + a]);
            }
        }
        return b;
    }

    private static int vertCount(MeshData m) {
        return m.verts.length / 3;
    }
}
