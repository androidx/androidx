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

package androidx.compose.remote.core.operations.d3;

import static androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression.COS;
import static androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression.MUL;
import static androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression.SIN;
import static androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression.VAR1;
import static androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression.VAR2;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import androidx.compose.remote.core.Operation;
import androidx.compose.remote.core.Operations;
import androidx.compose.remote.core.Paint3DContext;
import androidx.compose.remote.core.PaintOperation;
import androidx.compose.remote.core.VariableSupport;
import androidx.compose.remote.core.WireBuffer;
import androidx.compose.remote.core.operations.Utils;
import androidx.compose.remote.core.operations.utilities.d3.Primitive3D;
import androidx.compose.remote.core.testing.HeadlessRemoteContext;
import androidx.compose.remote.core.testing.SoftwareRasterPaintContext;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;

/**
 * Tests the wire format rules of the 3D operations: readers refuse the advanced bit, ignore extra
 * trailing params, and turn unknown sub-ops, types and light kinds or too few params into a no-op
 * instead of a throw; only negative or absurd lengths fail.
 */
public class Operations3DWireFormatTest {

    private static final int VARIABLE_ID = 100;

    private final HeadlessRemoteContext mContext = new HeadlessRemoteContext();
    private final Recorder mRecorder = new Recorder(mContext);

    // ---------------------------------------------------------------------------------------------
    // The advanced bit

    @Test
    public void drawMesh3D_advancedBit_isRefused() {
        WireBuffer buffer = new WireBuffer();
        DrawMesh3D.apply(buffer, 7, Paint3DContext.MODE_SOFTWARE_SMOOTH | Advanced3D.ADVANCED_BIT);
        buffer.setIndex(1);
        RuntimeException e =
                assertThrows(
                        RuntimeException.class,
                        () -> DrawMesh3D.read(buffer, new ArrayList<>()));
        assertTrue(e.getMessage(), e.getMessage().contains("advanced"));
    }

    @Test
    public void drawMesh3D_withoutAdvancedBit_roundTrips() {
        int mode =
                Paint3DContext.MODE_SOFTWARE_SMOOTH
                        | Paint3DContext.MODE_DOUBLE_SIDED
                        | Paint3DContext.MODE_FRONT_CW;
        WireBuffer buffer = new WireBuffer();
        DrawMesh3D.apply(buffer, 4, mode);
        assertEquals("opcode + meshId + mode", 9, buffer.getSize());

        Operation op = readOne(buffer, Operations.DRAW_MESH_3D, DrawMesh3D::read);
        assertWritesBack(buffer, op);
        paint(op);
        assertEquals(4, mRecorder.mMeshId);
        assertEquals(mode, mRecorder.mMode);
    }

    @Test
    public void defineMesh3D_advancedBit_isRefused() {
        int[] indices = {0, 1, 2};
        float[] verts = {0, 0, 0, 1, 0, 0, 0, 1, 0};
        WireBuffer buffer = new WireBuffer();
        DefineMesh3D.apply(buffer, 5 | Advanced3D.ADVANCED_BIT, indices, verts, null);
        buffer.setIndex(1);
        assertThrows(RuntimeException.class, () -> DefineMesh3D.read(buffer, new ArrayList<>()));
    }

    @Test
    public void defineMesh3D_withoutAdvancedBit_roundTrips() {
        int[] indices = {0, 1, 2};
        float[] verts = {0, 0, 0, 1, 0, 0, 0, 1, 0};
        float[] normals = {0, 0, 1, 0, 0, 1, 0, 0, 1};
        float[] uv = {0, 0, 1, 0, 0, 1};
        WireBuffer buffer = new WireBuffer();
        DefineMesh3D.apply(buffer, 5, indices, verts, normals, uv);

        Operation op = readOne(buffer, Operations.DEFINE_MESH_3D, DefineMesh3D::read);
        assertWritesBack(buffer, op);

        paint(op);
        assertEquals(5, mRecorder.mDefinedId);
        assertArrayEquals(indices, mRecorder.mIndices);
        assertArrayEquals(verts, mRecorder.mVerts, 0f);
        assertArrayEquals(normals, mRecorder.mNormals, 0f);
        assertArrayEquals(uv, mRecorder.mUv, 0f);
    }

    @Test
    public void setTexture3D_advancedBit_isRefused() {
        WireBuffer buffer = new WireBuffer();
        SetTexture3D.apply(buffer, 42 | Advanced3D.ADVANCED_BIT);
        buffer.setIndex(1);
        assertThrows(RuntimeException.class, () -> SetTexture3D.read(buffer, new ArrayList<>()));
    }

    @Test
    public void setTexture3D_withoutAdvancedBit_roundTrips() {
        WireBuffer buffer = new WireBuffer();
        SetTexture3D.apply(buffer, 42);

        Operation op = readOne(buffer, Operations.SET_TEXTURE_3D, SetTexture3D::read);
        assertWritesBack(buffer, op);

        paint(op);
        assertEquals(42, mRecorder.mBitmapId);
    }

    // ---------------------------------------------------------------------------------------------
    // Matrix3DOp

    @Test
    public void matrix3DOp_extraArgs_areIgnored() {
        WireBuffer buffer = new WireBuffer();
        Matrix3DOp.apply(buffer, Paint3DContext.M3_TRANSLATE, new float[] {1, 2, 3, 4, 5});

        Operation op = readOne(buffer, Operations.MATRIX_3D_OP, Matrix3DOp::read);
        assertWritesBack(buffer, op);

        paint(op);
        assertEquals(Paint3DContext.M3_TRANSLATE, mRecorder.mSub);
        assertArrayEquals(new float[] {1, 2, 3}, mRecorder.mArgs, 0f);
    }

    @Test
    public void matrix3DOp_tooFewArgsOrUnknownSubOp_isSkipped() {
        float[][] cases = {
            {Paint3DContext.M3_TRANSLATE, 1, 2},
            {Paint3DContext.M3_ROTATE_AXIS, 1, 0, 0},
            {Paint3DContext.M3_MULTIPLY, 1, 0, 0, 0},
            {7},
            {-1, 1, 2, 3},
            {1000},
        };
        for (float[] c : cases) {
            float[] args = new float[c.length - 1];
            System.arraycopy(c, 1, args, 0, args.length);
            WireBuffer buffer = new WireBuffer();
            Matrix3DOp.apply(buffer, (int) c[0], args);
            Operation op = readOne(buffer, Operations.MATRIX_3D_OP, Matrix3DOp::read);
            assertWritesBack(buffer, op);
            paint(op);
        }
        assertTrue(mRecorder.mCalls.toString(), mRecorder.mCalls.isEmpty());
    }

    @Test
    public void matrix3DOp_pushAndPop_reachTheRenderer() {
        for (int sub : new int[] {Paint3DContext.M3_PUSH, Paint3DContext.M3_POP}) {
            WireBuffer buffer = new WireBuffer();
            Matrix3DOp.apply(buffer, sub, new float[0]);
            assertEquals("opcode + sub + argsLen", 9, buffer.getSize());
            Operation op = readOne(buffer, Operations.MATRIX_3D_OP, Matrix3DOp::read);
            paint(op);
            assertEquals(sub, mRecorder.mSub);
            assertEquals(0, mRecorder.mArgs.length);
        }
    }

    @Test
    public void matrix3DOp_trimmedArgs_followVariables() {
        Matrix3DOp op =
                new Matrix3DOp(
                        Paint3DContext.M3_TRANSLATE,
                        new float[] {Utils.asNan(VARIABLE_ID), 2, 3, 99});
        mContext.loadFloat(VARIABLE_ID, 7f);
        op.updateVariables(mContext);
        paint(op);
        assertArrayEquals(new float[] {7, 2, 3}, mRecorder.mArgs, 0f);

        mContext.loadFloat(VARIABLE_ID, -1f);
        op.updateVariables(mContext);
        paint(op);
        assertArrayEquals(new float[] {-1, 2, 3}, mRecorder.mArgs, 0f);
    }

    @Test
    public void matrix3DOp_corruptLength_throws() {
        for (int len : new int[] {-1, Matrix3DOp.MAX_ARGS + 1}) {
            WireBuffer buffer = new WireBuffer();
            buffer.start(Operations.MATRIX_3D_OP);
            buffer.writeInt(Paint3DContext.M3_TRANSLATE);
            buffer.writeInt(len);
            buffer.setIndex(1);
            assertThrows(
                    RuntimeException.class, () -> Matrix3DOp.read(buffer, new ArrayList<>()));
        }
    }

    // ---------------------------------------------------------------------------------------------
    // SetCamera3D

    private static final float[] VIEW = {0, 0, 5, 0, 0, 0, 0, 1, 0};

    @Test
    public void setCamera3D_extraParams_areIgnored() {
        WireBuffer buffer = new WireBuffer();
        SetCamera3D.apply(
                buffer,
                Paint3DContext.PROJECTION_PERSPECTIVE,
                new float[] {1, 1.5f, 0.1f, 100, 42, 43},
                new float[] {0, 0, 5, 0, 0, 0, 0, 1, 0, 44});

        Operation op = readOne(buffer, Operations.SET_CAMERA_3D, SetCamera3D::read);
        assertWritesBack(buffer, op);

        paint(op);
        assertEquals(Paint3DContext.PROJECTION_PERSPECTIVE, mRecorder.mProjection);
        assertArrayEquals(new float[] {1, 1.5f, 0.1f, 100}, mRecorder.mProj, 0f);
        assertArrayEquals(VIEW, mRecorder.mView, 0f);
    }

    @Test
    public void setCamera3D_tooFewParams_isSkipped() {
        paint(new SetCamera3D(Paint3DContext.PROJECTION_PERSPECTIVE, new float[3], VIEW));
        paint(new SetCamera3D(Paint3DContext.PROJECTION_ORTHO, new float[5], VIEW));
        paint(new SetCamera3D(Paint3DContext.PROJECTION_ORTHO, new float[6], new float[8]));
        assertTrue(mRecorder.mCalls.toString(), mRecorder.mCalls.isEmpty());
    }

    @Test
    public void setCamera3D_unknownProjection_passesItsParams() {
        float[] proj = {1, 2, 3, 4, 5};
        paint(new SetCamera3D(9, proj, VIEW));
        assertEquals(9, mRecorder.mProjection);
        assertArrayEquals(proj, mRecorder.mProj, 0f);
    }

    @Test
    public void setCamera3D_corruptLength_throws() {
        WireBuffer buffer = new WireBuffer();
        buffer.start(Operations.SET_CAMERA_3D);
        buffer.writeInt(Paint3DContext.PROJECTION_PERSPECTIVE);
        buffer.writeInt(SetCamera3D.MAX_PARAMS + 1);
        buffer.setIndex(1);
        assertThrows(RuntimeException.class, () -> SetCamera3D.read(buffer, new ArrayList<>()));
    }

    // ---------------------------------------------------------------------------------------------
    // SetLights3D

    @Test
    public void setLights3D_plainSet_reachesTheRendererUnchanged() {
        int[] types = {
            Paint3DContext.LIGHT_DIRECTIONAL, Paint3DContext.LIGHT_POINT,
            Paint3DContext.LIGHT_AMBIENT
        };
        int[] colors = {0xFFFFFFFF, 0xFF808080, 0xFF102030};
        float[] params = {0, -1, 0, 0.8f, 1, 2, 3, 0.5f, 0, 0, 0, 0.3f};
        WireBuffer buffer = new WireBuffer();
        SetLights3D.apply(buffer, types, colors, params);

        Operation op = readOne(buffer, Operations.SET_LIGHTS_3D, SetLights3D::read);
        assertWritesBack(buffer, op);

        paint(op);
        assertArrayEquals(types, mRecorder.mTypes);
        assertArrayEquals(colors, mRecorder.mColors);
        assertArrayEquals(params, mRecorder.mParams, 0f);
    }

    @Test
    public void setLights3D_extraParamsAndUnknownKinds_areNormalized() {
        int[] types = {
            Paint3DContext.LIGHT_POINT | (2 << Paint3DContext.LIGHT_EXTRA_SHIFT),
            7, // unknown kind
            Paint3DContext.LIGHT_DIRECTIONAL | (1 << Paint3DContext.LIGHT_EXTRA_SHIFT),
            Paint3DContext.LIGHT_AMBIENT,
        };
        int[] colors = {0xFF000001, 0xFF000002, 0xFF000003, 0xFF000004};
        float[] params = {
            1, 2, 3, 0.5f, 90, 91, // point + 2 extra
            4, 5, 6, 0.6f, // unknown kind
            0, -1, 0, 0.7f, 92, // directional + 1 extra
            0, 0, 0, 0.2f, // ambient
        };
        WireBuffer buffer = new WireBuffer();
        SetLights3D.apply(buffer, types, colors, params);

        Operation op = readOne(buffer, Operations.SET_LIGHTS_3D, SetLights3D::read);
        assertWritesBack(buffer, op);

        paint(op);
        assertArrayEquals(
                new int[] {
                    Paint3DContext.LIGHT_POINT, Paint3DContext.LIGHT_DIRECTIONAL,
                    Paint3DContext.LIGHT_AMBIENT
                },
                mRecorder.mTypes);
        assertArrayEquals(new int[] {0xFF000001, 0xFF000003, 0xFF000004}, mRecorder.mColors);
        assertArrayEquals(
                new float[] {1, 2, 3, 0.5f, 0, -1, 0, 0.7f, 0, 0, 0, 0.2f},
                mRecorder.mParams,
                0f);
    }

    @Test
    public void setLights3D_normalizedParams_followVariables() {
        SetLights3D op =
                new SetLights3D(
                        new int[] {
                            Paint3DContext.LIGHT_POINT | (1 << Paint3DContext.LIGHT_EXTRA_SHIFT)
                        },
                        new int[] {0xFFFFFFFF},
                        new float[] {1, 2, 3, Utils.asNan(VARIABLE_ID), 50});
        mContext.loadFloat(VARIABLE_ID, 0.25f);
        op.updateVariables(mContext);
        paint(op);
        assertArrayEquals(new float[] {1, 2, 3, 0.25f}, mRecorder.mParams, 0f);

        mContext.loadFloat(VARIABLE_ID, 0.75f);
        op.updateVariables(mContext);
        paint(op);
        assertArrayEquals(new float[] {1, 2, 3, 0.75f}, mRecorder.mParams, 0f);
    }

    @Test
    public void setLights3D_paramsEndEarly_dropsTheLightsWithoutParams() {
        paint(
                new SetLights3D(
                        new int[] {Paint3DContext.LIGHT_POINT, Paint3DContext.LIGHT_POINT},
                        new int[] {0xFF000001, 0xFF000002},
                        new float[] {1, 2, 3, 0.5f, 4, 5}));
        assertArrayEquals(new int[] {Paint3DContext.LIGHT_POINT}, mRecorder.mTypes);
        assertArrayEquals(new int[] {0xFF000001}, mRecorder.mColors);
        assertArrayEquals(new float[] {1, 2, 3, 0.5f}, mRecorder.mParams, 0f);
    }

    @Test
    public void setLights3D_paramsWithoutLights_areReadAsNoLights() {
        // The original writer accepts params without lights; such a document must still load.
        WireBuffer buffer = new WireBuffer();
        SetLights3D.apply(buffer, new int[0], new int[0], new float[] {0, -1, 0, 1});

        Operation op = readOne(buffer, Operations.SET_LIGHTS_3D, SetLights3D::read);
        assertWritesBack(buffer, op);
        paint(op);
        assertEquals(List.of("setLights3D"), mRecorder.mCalls);
        assertEquals(0, mRecorder.mTypes.length);
        assertEquals(0, mRecorder.mColors.length);
        assertEquals(0, mRecorder.mParams.length);
    }

    @Test
    public void setLights3D_corruptLength_throws() {
        WireBuffer buffer = new WireBuffer();
        buffer.start(Operations.SET_LIGHTS_3D);
        buffer.writeInt(-1);
        buffer.setIndex(1);
        assertThrows(RuntimeException.class, () -> SetLights3D.read(buffer, new ArrayList<>()));

        WireBuffer absurd = new WireBuffer();
        absurd.start(Operations.SET_LIGHTS_3D);
        absurd.writeInt(0); // No lights.
        absurd.writeInt(1 << 20); // Params length.
        absurd.setIndex(1);
        assertThrows(RuntimeException.class, () -> SetLights3D.read(absurd, new ArrayList<>()));
    }

    // ---------------------------------------------------------------------------------------------
    // Paint3DState

    @Test
    public void paint3DState_materialExtraParams_areKeptAndIgnored() {
        WireBuffer buffer = new WireBuffer();
        Paint3DState.apply(buffer, Paint3DState.MATERIAL, new float[] {0.5f, 32, 1, 2, 3});

        Operation op = readOne(buffer, Operations.PAINT_3D_STATE, Paint3DState::read);
        assertWritesBack(buffer, op);

        paint(op);
        assertEquals(0.5f, mRecorder.mSpecular, 0f);
        assertEquals(32f, mRecorder.mShininess, 0f);
    }

    @Test
    public void paint3DState_unknownSubOrTooFewParams_isNoOp() {
        paint(new Paint3DState(9, new float[] {1, 2}));
        paint(new Paint3DState(Paint3DState.MATERIAL, new float[] {1}));
        paint(new Paint3DState(Paint3DState.DEPTH_BIAS, new float[0]));
        assertTrue(mRecorder.mCalls.toString(), mRecorder.mCalls.isEmpty());
    }

    // ---------------------------------------------------------------------------------------------
    // MeshPrimitive

    @Test
    public void meshPrimitive_infinitePlane_definesAnInfinitePlane() {
        float[] params = {1, 2, 3, 0.5f, 2, 1, 0.1f, 0.2f, 0.3f};
        int flags = Primitive3D.AXIS_Z << Primitive3D.FLAG_AXIS_SHIFT;
        WireBuffer buffer = new WireBuffer();
        MeshPrimitive.apply(buffer, 11, Primitive3D.INFINITE_PLANE, 0f, flags, params);

        Operation op = readOne(buffer, Operations.MESH_PRIMITIVE_3D, MeshPrimitive::read);
        assertWritesBack(buffer, op);

        paint(op);
        assertEquals(List.of("defineInfinitePlane3D"), mRecorder.mCalls);
        assertEquals(11, mRecorder.mPlaneId);
        assertEquals(flags, mRecorder.mPlaneFlags);
        assertArrayEquals(params, mRecorder.mPlaneParams, 0f);
    }

    @Test
    public void meshPrimitive_infinitePlane_padsAndTrimsParams() {
        assertArrayEquals(
                new float[] {1, 2, 3, 0, 1, 1, 0.5f, 0.5f, 0.5f},
                MeshPrimitive.infinitePlaneParams(new float[][] {{1, 2, 3}}),
                0f);
        assertArrayEquals(
                new float[] {0, 0, 0, 0, 1, 1, 0.5f, 0.5f, 0.5f},
                MeshPrimitive.infinitePlaneParams(new float[0][]),
                0f);
        assertArrayEquals(
                new float[] {1, 2, 3, 4, 5, 6, 7, 8, 9},
                MeshPrimitive.infinitePlaneParams(
                        new float[][] {{1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11}, {12}}),
                0f);
    }

    @Test
    public void meshPrimitive_unknownTypeOrTooFewParams_definesNothing() {
        paint(new MeshPrimitive(1, 24, 8f, 0, new float[] {1, 2, 3, 4}));
        paint(new MeshPrimitive(1, -1, 8f, 0, new float[] {1, 2, 3, 4}));
        paint(new MeshPrimitive(1, Primitive3D.SPHERE, 8f, 0, new float[] {1, 2}));
        paint(new MeshPrimitive(1, Primitive3D.CUBE, 8f, 0, new float[0][]));
        assertTrue(mRecorder.mCalls.toString(), mRecorder.mCalls.isEmpty());

        paint(new MeshPrimitive(1, Primitive3D.SPHERE, 8f, 0, new float[] {1, 0, 0, 0, 99}));
        assertEquals("extra params are ignored", List.of("defineMesh3D"), mRecorder.mCalls);
    }

    @Test
    public void meshPrimitive_corruptCounts_defineNothing() {
        // A profile tube claiming a billion points, and a path with an absurd contour size.
        paint(new MeshPrimitive(1, Primitive3D.PROFILE_TUBE, 8f, 0, new float[] {1e9f, 0, 0}));
        paint(
                new MeshPrimitive(
                        1, Primitive3D.EXTRUDE_PATH, 8f, 0, new float[] {1, 0, 0, 0, 0, 1, 1e9f}));
        assertTrue(mRecorder.mCalls.toString(), mRecorder.mCalls.isEmpty());
    }

    @Test
    public void meshPrimitive_corruptLengths_throw() {
        for (int[] counts : new int[][] {{-1}, {65}, {1, -1}, {1, (1 << 20) + 1}}) {
            WireBuffer buffer = new WireBuffer();
            buffer.start(Operations.MESH_PRIMITIVE_3D);
            buffer.writeInt(1);
            buffer.writeInt(Primitive3D.SPHERE);
            buffer.writeFloat(8f);
            buffer.writeInt(0);
            for (int count : counts) {
                buffer.writeInt(count);
            }
            buffer.setIndex(1);
            assertThrows(
                    RuntimeException.class, () -> MeshPrimitive.read(buffer, new ArrayList<>()));
        }
    }

    // ---------------------------------------------------------------------------------------------
    // MeshExpression3D

    private static final float[][] NONE = new float[0][];

    @Test
    public void meshExpression_validSphere_definesTheGrid() {
        MeshExpression3D op =
                new MeshExpression3D(
                        3,
                        MeshExpression3D.SURFACE_SPHERE,
                        0,
                        new float[] {1, 4, 3},
                        new float[][] {{0}},
                        NONE,
                        NONE);
        op.updateVariables(mContext);
        paint(op);
        assertEquals(List.of("defineMesh3D"), mRecorder.mCalls);
        assertEquals(3, mRecorder.mDefinedId);
        assertEquals(4 * 3 * 3, mRecorder.mVerts.length);
        assertEquals(3 * 2 * 6, mRecorder.mIndices.length);
    }

    @Test
    public void meshExpression_invalid_definesNothing() {
        float[][] one = {{0}};
        MeshExpression3D[] ops = {
            // Unknown surface types.
            new MeshExpression3D(1, 4, 0, new float[] {1, 4, 3, 0, 1, 3}, one, NONE, NONE),
            new MeshExpression3D(1, -1, 0, new float[] {1, 4, 3, 0, 1, 3}, one, NONE, NONE),
            // Too few params.
            new MeshExpression3D(
                    1, MeshExpression3D.SURFACE_SPHERE, 0, new float[] {1, 4}, one, NONE, NONE),
            new MeshExpression3D(
                    1,
                    MeshExpression3D.SURFACE_HEIGHT_FIELD,
                    0,
                    new float[] {0, 1, 4, 0, 1, 4, 0},
                    one,
                    NONE,
                    NONE),
            // GENERAL needs three position expressions.
            new MeshExpression3D(
                    1,
                    MeshExpression3D.SURFACE_GENERAL,
                    0,
                    new float[] {0, 1, 4, 0, 1, 4},
                    new float[][] {{0}, {0}},
                    NONE,
                    NONE),
            // A grid far too large to generate.
            new MeshExpression3D(
                    1,
                    MeshExpression3D.SURFACE_SPHERE,
                    0,
                    new float[] {1, 1e9f, 1e9f},
                    one,
                    NONE,
                    NONE),
            // An expression that can't be evaluated.
            new MeshExpression3D(
                    1,
                    MeshExpression3D.SURFACE_SPHERE,
                    0,
                    new float[] {1, 4, 3},
                    new float[][] {{}},
                    NONE,
                    NONE),
        };
        for (MeshExpression3D op : ops) {
            op.updateVariables(mContext);
            paint(op);
        }
        assertTrue(mRecorder.mCalls.toString(), mRecorder.mCalls.isEmpty());
    }

    @Test
    public void meshExpression_roundTrips() {
        WireBuffer buffer = new WireBuffer();
        MeshExpression3D.apply(
                buffer,
                3,
                MeshExpression3D.SURFACE_CYLINDER,
                MeshExpression3D.FLAG_FLIP_WINDING,
                new float[] {1, 2, 4, 3, 77},
                new float[][] {{0}, {1}},
                NONE,
                new float[][] {{0}, {1}});
        Operation op = readOne(buffer, Operations.MESH_EXPRESSION_3D, MeshExpression3D::read);
        assertWritesBack(buffer, op);
    }

    // Finite-difference normals point to the front side of the triangles, the side their
    // counter-clockwise winding faces; FLAG_FLIP_WINDING reverses both.

    private static final int[] FLAGS = {0, MeshExpression3D.FLAG_FLIP_WINDING};
    private static final float[][] UNDISPLACED = {{0}};

    @Test
    public void meshExpression_finiteDifferenceNormals_pointToTheFrontOfEveryTriangle() {
        for (int flags : FLAGS) {
            for (MeshExpression3D op : curvedSurfaces(flags)) {
                paint(op);
                String what = op + ", flags " + flags;
                float[] v = mRecorder.mVerts;
                float[] n = mRecorder.mNormals;
                int[] t = mRecorder.mIndices;
                int checked = 0;
                for (int k = 0; k < t.length; k += 3) {
                    float[] face = faceNormal(v, t[k], t[k + 1], t[k + 2]);
                    if (face == null) {
                        continue; // Two corners on a sphere's pole.
                    }
                    for (int c = k; c < k + 3; c++) {
                        int p = t[c] * 3;
                        float dot = n[p] * face[0] + n[p + 1] * face[1] + n[p + 2] * face[2];
                        assertTrue(what + ": vertex " + t[c] + " of triangle " + k / 3, dot > 0);
                    }
                    checked++;
                }
                assertTrue(what + ": " + checked + " triangles checked", checked >= t.length / 6);
            }
        }
    }

    @Test
    public void meshExpression_finiteDifferenceNormals_pointToTheDocumentedFrontSide() {
        for (int flags : FLAGS) {
            // Without the flag the front side is +Y for a height field, the inside of a sphere,
            // the outside of a cylinder and -Z for (u, v, 0); with it, the other side.
            float sign = flags == 0 ? 1 : -1;

            paintSurface(
                    MeshExpression3D.SURFACE_HEIGHT_FIELD,
                    flags,
                    new float[] {-1, 1, 4, -1, 1, 4, -1, 1},
                    UNDISPLACED);
            assertFrontSide("height field", sign, p -> new float[] {0, 1, 0});
            float[] n = mRecorder.mNormals;
            for (int p = 0; p < n.length; p += 3) {
                assertEquals("height field normal", sign, n[p + 1], 1e-6f);
            }

            paintSurface(MeshExpression3D.SURFACE_SPHERE,
                    flags, new float[]{1, 9, 7}, UNDISPLACED);
            assertFrontSide("sphere", sign, p -> new float[]{-p[0], -p[1], -p[2]});
            n = mRecorder.mNormals;
            for (int i = 0; i < 9; i++) {
                assertEquals("north pole normal", -sign, n[i * 7 * 3 + 1], 1e-6f);
                assertEquals("south pole normal", sign, n[(i * 7 + 6) * 3 + 1], 1e-6f);
            }

            paintSurface(
                    MeshExpression3D.SURFACE_CYLINDER,
                    flags, new float[]{1, 2, 9, 4}, UNDISPLACED);
            assertFrontSide("cylinder", sign, p -> new float[]{p[0], 0, p[2]});

            paintSurface(
                    MeshExpression3D.SURFACE_GENERAL,
                    flags,
                    new float[] {-1, 1, 3, -1, 1, 3},
                    new float[][] {{VAR1}, {VAR2}, {0}});
            assertFrontSide("(u, v, 0)", sign, p -> new float[] {0, 0, -1});
        }
    }

    // FLAG_FLIP_NORMALS reverses every normal and keeps the triangles and positions.

    @Test
    public void meshExpression_flipNormals_reversesTheFiniteDifferenceNormals() {
        for (int winding : FLAGS) {
            MeshExpression3D[] plain = curvedSurfaces(winding);
            MeshExpression3D[] flipped = curvedSurfaces(winding
                    | MeshExpression3D.FLAG_FLIP_NORMALS);
            for (int s = 0; s < plain.length; s++) {
                assertSameMesh(plain[s] + ", flags " + winding, plain[s], flipped[s], -1);
            }
        }
    }

    @Test
    public void meshExpression_flipNormals_reversesTheNormalExpressions() {
        // Without FLAG_FLIP_NORMALS the authored normals are used whatever the winding: the
        // outward normal of a unit sphere, which equals the position.
        for (int flags : FLAGS) {
            paint(outwardSphere(flags));
            float[] v = mRecorder.mVerts;
            float[] n = mRecorder.mNormals;
            for (int p = 0; p < v.length; p++) {
                assertEquals("flags " + flags + ": normal " + p / 3, v[p], n[p], 1e-6f);
            }
        }
        int winding = MeshExpression3D.FLAG_FLIP_WINDING;
        int flip = MeshExpression3D.FLAG_FLIP_NORMALS;
        assertSameMesh("flip normals", outwardSphere(0), outwardSphere(flip), -1);
        assertSameMesh("flip both", outwardSphere(winding), outwardSphere(winding | flip), -1);
    }

    @Test
    public void meshExpression_unknownFlagBits_areIgnored() {
        int unknown = 0xFFFFFFFC; // Bits 2-31.
        for (int known = 0; known < 4; known++) {
            MeshExpression3D[] plain = curvedSurfaces(known);
            MeshExpression3D[] extra = curvedSurfaces(known | unknown);
            for (int s = 0; s < plain.length; s++) {
                assertSameMesh(plain[s] + ", flags " + known, plain[s], extra[s], 1);
            }
            String what = "outward sphere, flags " + known;
            assertSameMesh(what, outwardSphere(known), outwardSphere(known | unknown), 1);
        }
    }

    /** Curved surfaces of every type, without normal expressions. */
    private static MeshExpression3D @NonNull [] curvedSurfaces(int flags) {
        return new MeshExpression3D[] {
            // Saddle (u, v, uv).
            surface(
                    MeshExpression3D.SURFACE_GENERAL,
                    flags,
                    new float[] {-1, 1, 7, -1, 1, 6},
                    new float[][] {{VAR1}, {VAR2}, {VAR1, VAR2, MUL}}),
            // y = sin(x) cos(z) / 2.
            surface(
                    MeshExpression3D.SURFACE_HEIGHT_FIELD,
                    flags,
                    new float[] {-2, 2, 9, -2, 2, 8, -10, 10},
                    new float[][] {{VAR1, SIN, VAR2, COS, MUL, 0.5f, MUL}}),
            // Bumpy sphere of radius 1 + sin(3u) sin(v) / 10; the poles stay points.
            surface(
                    MeshExpression3D.SURFACE_SPHERE,
                    flags,
                    new float[] {1, 9, 7},
                    new float[][] {{VAR1, 3, MUL, SIN, VAR2, SIN, MUL, 0.1f, MUL}}),
            // Wavy cylinder of radius 1 + sin(2y) / 10.
            surface(
                    MeshExpression3D.SURFACE_CYLINDER,
                    flags,
                    new float[] {1, 2, 9, 5},
                    new float[][] {{VAR2, 2, MUL, SIN, 0.1f, MUL}}),
        };
    }

    /** A {@link MeshExpression3D} without normal or UV expressions. */
    private static @NonNull MeshExpression3D surface(
            int type, int flags, float @NonNull [] params, float @NonNull [] @NonNull [] pos) {
        return new MeshExpression3D(1, type, flags, params, pos, NONE, NONE);
    }

    private void paintSurface(
            int type, int flags, float @NonNull [] params, float @NonNull [] @NonNull [] pos) {
        paint(surface(type, flags, params, pos));
    }

    /** A unit sphere whose normal expressions give its outward normal. */
    private static @NonNull MeshExpression3D outwardSphere(int flags) {
        float[][] outward = {
            {VAR2, SIN, VAR1, COS, MUL}, {VAR2, COS}, {VAR2, SIN, VAR1, SIN, MUL},
        };
        return new MeshExpression3D(
                1,
                MeshExpression3D.SURFACE_SPHERE,
                flags,
                new float[] {1, 9, 7},
                UNDISPLACED,
                outward,
                NONE);
    }

    /** Returns {@code (v1 - v0) x (v2 - v0)} of a triangle, or null if it is degenerate. */
    private static float @Nullable [] faceNormal(float @NonNull [] v, int i0, int i1, int i2) {
        int p0 = i0 * 3;
        int p1 = i1 * 3;
        int p2 = i2 * 3;
        float ax = v[p1] - v[p0];
        float ay = v[p1 + 1] - v[p0 + 1];
        float az = v[p1 + 2] - v[p0 + 2];
        float bx = v[p2] - v[p0];
        float by = v[p2 + 1] - v[p0 + 1];
        float bz = v[p2 + 2] - v[p0 + 2];
        float[] f = {ay * bz - az * by, az * bx - ax * bz, ax * by - ay * bx};
        return f[0] * f[0] + f[1] * f[1] + f[2] * f[2] < 1e-12f ? null : f;
    }

    /**
     * Asserts that every normal and every non-degenerate triangle of the last mesh points to the
     * side {@code side} gives for its position ({@code sign} 1), or to the other side ({@code -1}).
     */
    private void assertFrontSide(
            @NonNull String what, float sign, @NonNull UnaryOperator<float[]> side) {
        float[] v = mRecorder.mVerts;
        float[] n = mRecorder.mNormals;
        for (int p = 0; p < v.length; p += 3) {
            float[] s = side.apply(new float[] {v[p], v[p + 1], v[p + 2]});
            float dot = n[p] * s[0] + n[p + 1] * s[1] + n[p + 2] * s[2];
            assertTrue(what + ": normal " + p / 3, dot * sign > 0);
        }
        int[] t = mRecorder.mIndices;
        int checked = 0;
        for (int k = 0; k < t.length; k += 3) {
            float[] face = faceNormal(v, t[k], t[k + 1], t[k + 2]);
            if (face == null) {
                continue;
            }
            float[] c = new float[3];
            for (int m = 0; m < 3; m++) {
                c[m] = (v[t[k] * 3 + m] + v[t[k + 1] * 3 + m] + v[t[k + 2] * 3 + m]) / 3;
            }
            float[] s = side.apply(c);
            float dot = face[0] * s[0] + face[1] * s[1] + face[2] * s[2];
            assertTrue(what + ": triangle " + k / 3, dot * sign > 0);
            checked++;
        }
        assertTrue(what + ": every triangle is degenerate", checked > 0);
    }

    /**
     * Paints {@code a} and then {@code b} and asserts that they define the same triangles and
     * positions, and that the normals of {@code b} are exactly those of {@code a} times {@code
     * normalSign}.
     */
    private void assertSameMesh(
            @NonNull String what,
            @NonNull MeshExpression3D a,
            @NonNull MeshExpression3D b,
            float normalSign) {
        int calls = mRecorder.mCalls.size();
        paint(a);
        int[] indices = mRecorder.mIndices.clone();
        float[] verts = mRecorder.mVerts.clone();
        float[] normals = mRecorder.mNormals.clone();
        for (int k = 0; k < normals.length; k++) {
            normals[k] *= normalSign;
        }
        paint(b);
        assertEquals(what + ": meshes defined", calls + 2, mRecorder.mCalls.size());
        assertArrayEquals(what + ": indices", indices, mRecorder.mIndices);
        assertArrayEquals(what + ": positions", verts, mRecorder.mVerts, 0f);
        assertArrayEquals(what + ": normals", normals, mRecorder.mNormals, 0f);
    }

    // ---------------------------------------------------------------------------------------------
    // Helpers

    /** Reads an operation's static {@code read} into a list. */
    private interface Reader {
        void read(@NonNull WireBuffer buffer, @NonNull List<Operation> operations);
    }

    /** Reads the single operation in {@code buffer}, checking that all its bytes are consumed. */
    private static @NonNull Operation readOne(
            @NonNull WireBuffer buffer, int opcode, @NonNull Reader reader) {
        buffer.setIndex(0);
        assertEquals(opcode, buffer.readOperationType());
        List<Operation> operations = new ArrayList<>();
        reader.read(buffer, operations);
        assertEquals("bytes consumed", buffer.getSize(), buffer.getIndex());
        assertEquals(1, operations.size());
        return operations.get(0);
    }

    /** Checks that {@code op} writes exactly the bytes it was read from. */
    private static void assertWritesBack(@NonNull WireBuffer original, @NonNull Operation op) {
        WireBuffer out = new WireBuffer();
        op.write(out);
        assertArrayEquals(original.cloneBytes(), out.cloneBytes());
    }

    private void paint(@NonNull Operation op) {
        if (op instanceof VariableSupport) {
            ((VariableSupport) op).updateVariables(mContext);
        }
        ((PaintOperation) op).paint(mRecorder);
    }

    /** Records what the operations hand a {@link Paint3DContext}. */
    private static final class Recorder extends SoftwareRasterPaintContext
            implements Paint3DContext {
        final List<String> mCalls = new ArrayList<>();
        int mSub;
        float[] mArgs;
        int mProjection;
        float[] mProj;
        float[] mView;
        int mMeshId;
        int mMode;
        int[] mTypes;
        int[] mColors;
        float[] mParams;
        int mBitmapId;
        int mDefinedId;
        int[] mIndices;
        float[] mVerts;
        float[] mNormals;
        float[] mUv;
        int mPlaneId;
        int mPlaneFlags;
        float[] mPlaneParams;
        float mSpecular;
        float mShininess;

        Recorder(@NonNull HeadlessRemoteContext context) {
            super(context, 4, 4);
        }

        @Override
        public void defineMesh3D(
                int id,
                int @NonNull [] indices,
                float @NonNull [] verts,
                float @Nullable [] normals) {
            defineMesh3D(id, indices, verts, normals, null);
        }

        @Override
        public void defineMesh3D(
                int id,
                int @NonNull [] indices,
                float @NonNull [] verts,
                float @Nullable [] normals,
                float @Nullable [] uv) {
            mCalls.add("defineMesh3D");
            mDefinedId = id;
            mIndices = indices;
            mVerts = verts;
            mNormals = normals;
            mUv = uv;
        }

        @Override
        public void defineInfinitePlane3D(int id, int flags, float @NonNull [] params) {
            mCalls.add("defineInfinitePlane3D");
            mPlaneId = id;
            mPlaneFlags = flags;
            mPlaneParams = params;
        }

        @Override
        public void setCamera3D(
                int projection, float @NonNull [] projParams, float @NonNull [] viewParams) {
            mCalls.add("setCamera3D");
            mProjection = projection;
            mProj = projParams;
            mView = viewParams;
        }

        @Override
        public void matrix3DOp(int sub, float @NonNull [] args) {
            mCalls.add("matrix3DOp");
            mSub = sub;
            mArgs = args;
        }

        @Override
        public void drawMesh3D(int meshId, int mode) {
            mCalls.add("drawMesh3D");
            mMeshId = meshId;
            mMode = mode;
        }

        @Override
        public void clearDepth3D() {
            mCalls.add("clearDepth3D");
        }

        @Override
        public void setLights3D(
                int @NonNull [] types, int @NonNull [] colors, float @NonNull [] params) {
            mCalls.add("setLights3D");
            mTypes = types;
            mColors = colors;
            mParams = params;
        }

        @Override
        public void setTexture3D(int bitmapId) {
            mCalls.add("setTexture3D");
            mBitmapId = bitmapId;
        }

        @Override
        public void setMaterial3D(float specStrength, float shininess) {
            mCalls.add("setMaterial3D");
            mSpecular = specStrength;
            mShininess = shininess;
        }

        @Override
        public void setDepthBias3D(float constant, float slope) {
            mCalls.add("setDepthBias3D");
        }
    }
}
