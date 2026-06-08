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

package androidx.compose.remote.creation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import androidx.compose.remote.core.Paint3DContext;
import androidx.compose.remote.core.RcPlatformServices;
import androidx.compose.remote.core.RcProfiles;
import androidx.compose.remote.core.WireBuffer;
import androidx.compose.remote.core.operations.Header;
import androidx.compose.remote.core.operations.Utils;
import androidx.compose.remote.core.operations.d3.DrawMesh3D;
import androidx.compose.remote.core.operations.d3.Matrix3DOp;
import androidx.compose.remote.core.operations.d3.MeshExpression3D;
import androidx.compose.remote.core.operations.d3.MeshPrimitive;
import androidx.compose.remote.core.operations.d3.SetLights3D;
import androidx.compose.remote.core.operations.utilities.d3.Primitive3D;

import org.jspecify.annotations.NonNull;
import org.junit.Test;

import java.util.function.Consumer;

/**
 * Tests {@link RemoteComposeWriter3D}: valid 3D operations write the expected wire bytes, and
 * values no player understands are refused.
 */
public class RemoteComposeWriter3DTest {

    private static final int[] TRI = {0, 1, 2};
    private static final float[] VERTS = {0, 0, 0, 1, 0, 0, 0, 1, 0};
    private static final float[] VIEW = {0, 0, 5, 0, 0, 0, 0, 1, 0};
    private static final float[] SPHERE = {1, 0, 0, 0};
    private static final float[] PLANE = {0, 0, 0, 0, 1, 1, 0.5f, 0.5f, 0.5f};

    /** The header of {@link #writer3D1()}, as written before the 3D2 format existed. */
    private static final String HEADER_3D1 =
            "00048c000100000001000000000000000300050004000000640006000400000032000e000400000201";

    /**
     * The 3D operations of the original format and the bytes each one appended before the 3D2
     * format existed (recorded with the writer of the change that added the 3D operations).
     */
    private static final Step[] STEPS_3D1 = {
        new Step(
                "6e0000000100000003000000000000000100000002000000090000000000000000000000003f80"
                        + "00000000000000000000000000003f800000000000000000000000000000",
                w -> w.defineMesh3D(1, TRI, VERTS, null)),
        new Step(
                "6e0000000200000003000000000000000100000002000000090000000000000000000000"
                        + "003f8000000000000000000000000000003f800000000000000000000900000000000000"
                        + "003f80000000000000000000003f80000000000000000000003f80000000000006000000"
                        + "00000000003f80000000000000000000003f800000",
                w -> w.defineMesh3D(
                        2, TRI, VERTS, new float[] {0, 0, 1, 0, 0, 1, 0, 0, 1},
                        new float[] {0, 0, 1, 0, 0, 1})),
        new Step(
                "6f00000000000000043f8000003fc000003dcccccd42c8000000000009000000000000000040a0"
                        + "0000000000000000000000000000000000003f80000000000000",
                w -> w.setCamera3D(
                        Paint3DContext.PROJECTION_PERSPECTIVE,
                        new float[] {1f, 1.5f, 0.1f, 100f}, VIEW)),
        new Step(
                "6f0000000100000006bf8000003f800000bf8000003f8000003dcccccd41200000000000090000"
                        + "00000000000040a00000000000000000000000000000000000003f80000000000000",
                w -> w.setCamera3D(
                        Paint3DContext.PROJECTION_ORTHO, new float[] {-1, 1, -1, 1, 0.1f, 10},
                        VIEW)),
        new Step(
                "700000000000000000",
                w -> w.matrix3DOp(Paint3DContext.M3_IDENTITY, new float[0])),
        new Step(
                "700000000100000003ff80002a4000000040400000",
                w -> w.matrix3DOp(
                        Paint3DContext.M3_TRANSLATE, new float[] {Utils.asNan(42), 2, 3})),
        new Step(
                "700000000200000003400000004000000040000000",
                w -> w.matrix3DOp(Paint3DContext.M3_SCALE, new float[] {2, 2, 2})),
        new Step(
                "7000000003000000043f000000000000003f80000000000000",
                w -> w.matrix3DOp(Paint3DContext.M3_ROTATE_AXIS, new float[] {0.5f, 0, 1, 0})),
        new Step(
                "7000000004000000103f800000000000000000000000000000000000003f800000000000000000"
                        + "000000000000000000003f80000000000000000000000000000000000000"
                        + "3f800000",
                w -> w.matrix3DOp(
                        Paint3DContext.M3_MULTIPLY,
                        new float[] {1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1})),
        new Step(
                "710000000100000001",
                w -> w.drawMesh3D(1, Paint3DContext.MODE_SOFTWARE_SMOOTH)),
        new Step(
                "710000000200000700",
                w -> w.drawMesh3D(2, Paint3DContext.MODE_SOFTWARE_WIREFRAME3)),
        new Step(
                "710000000300000009",
                w -> w.drawMesh3D(3, Paint3DContext.MODE_DRAWMESH_ZBUF_SMOOTH)),
        new Step("720000000000000000", RemoteComposeWriter3D::clearDepth3D),
        new Step(
                "730000000200000000ffffffff00000001ff8080800000000800000000bf800000000000003f4c"
                        + "cccd3f80000040000000404000003f000000",
                w -> w.setLights3D(
                        new int[] {0, 1}, new int[] {0xFFFFFFFF, 0xFF808080},
                        new float[] {0, -1, 0, 0.8f, 1, 2, 3, 0.5f})),
        new Step(
                "730000000000000000",
                w -> w.setLights3D(new int[0], new int[0], new float[0])),
        new Step("760000002a", w -> w.setTexture3D(42)),
        new Step("7600000000", w -> w.setTexture3D(0)),
        new Step("7200000001000000023f00000042000000", w -> w.setMaterial3D(0.5f, 32f)),
        new Step("7200000002000000023f80000040000000", w -> w.setDepthBias3D(1f, 2f)),
        new Step(
                "780000000300000000410000000000000000000001000000043f80000000000000000000000000"
                        + "0000",
                w -> w.meshPrimitive(3, Primitive3D.SPHERE, 8f, 0, SPHERE)),
        new Step(
                "7800000004000000033f8000000000001000000001000000063f8000003f8000003f8000000000"
                        + "00000000000000000000",
                w -> w.meshPrimitive(
                        4, Primitive3D.CUBE, 1f, Primitive3D.UV << Primitive3D.FLAG_UV_SHIFT,
                        new float[] {1, 1, 1, 0, 0, 0})),
        new Step(
                "780000000500000013414000000000003f0000000200000003000000000000000000000000000000"
                        + "043f800000000000003f8000003f800000",
                w -> w.meshPrimitive(
                        5, Primitive3D.LATHE, 12f, 0x3F,
                        new float[][] {{0, 0, 0}, {1, 0, 1, 1}})),
        new Step(
                "75000000060000000200000000000000033f8000004100000040c00000000000010000000100000000"
                        + "0000000000000000",
                w -> w.meshExpression(
                        6, MeshExpression3D.SURFACE_SPHERE, 0, new float[] {1, 8, 6},
                        new float[][] {{0}}, new float[0][], new float[0][])),
        new Step(
                "75000000070000000300000001000000043f8000004000000041000000408000000000000100"
                        + "000001000000000000000000000000",
                w -> w.meshExpression(
                        7, MeshExpression3D.SURFACE_CYLINDER, MeshExpression3D.FLAG_FLIP_WINDING,
                        new float[] {1, 2, 8, 4}, new float[][] {{0}}, new float[0][],
                        new float[0][])),
    };

    // ---------------------------------------------------------------------------------------------
    // 3D1: unchanged bytes

    @Test
    public void documentIsWrittenAsBefore() {
        RemoteComposeWriter writer = writer3D1();
        WireBuffer buffer = writer.getBuffer().getBuffer();
        assertEquals(HEADER_3D1, hex(buffer, 0, buffer.getSize()));
        for (Step step : STEPS_3D1) {
            assertEquals(step.mHex, appended(writer, step.mCall));
        }
    }

    // ---------------------------------------------------------------------------------------------
    // 3D2 additions

    @Test
    public void additions3D2_writeExpectedBytes() {
        assertWrites("matrix3DOp PUSH", RemoteComposeWriter3D::matrix3DPush,
                bytes(b -> Matrix3DOp.apply(b, Paint3DContext.M3_PUSH, new float[0])));
        assertWrites("matrix3DOp POP", RemoteComposeWriter3D::matrix3DPop,
                bytes(b -> Matrix3DOp.apply(b, Paint3DContext.M3_POP, new float[0])));
        assertWrites("matrix3DOp PUSH",
                w -> w.matrix3DOp(Paint3DContext.M3_PUSH, new float[0]),
                bytes(b -> Matrix3DOp.apply(b, Paint3DContext.M3_PUSH, new float[0])));

        int smooth = Paint3DContext.MODE_SOFTWARE_SMOOTH;
        int[] flags = {
            Paint3DContext.MODE_TRANSPARENT, Paint3DContext.MODE_GPU, Paint3DContext.MODE_PBR,
            Paint3DContext.MODE_DOUBLE_SIDED, Paint3DContext.MODE_FRONT_CW
        };
        String[] names = {"TRANSPARENT", "GPU", "PBR", "DOUBLE_SIDED", "FRONT_CW"};
        for (int i = 0; i < flags.length; i++) {
            int mode = smooth | flags[i];
            assertWrites("drawMesh3D flag " + names[i], w -> w.drawMesh3D(1, mode),
                    bytes(b -> DrawMesh3D.apply(b, 1, mode)));
        }
        int two = smooth | Paint3DContext.MODE_TRANSPARENT | Paint3DContext.MODE_PBR;
        assertWrites("drawMesh3D flags TRANSPARENT|PBR", w -> w.drawMesh3D(1, two),
                bytes(b -> DrawMesh3D.apply(b, 1, two)));
        int cw = Paint3DContext.MODE_CANVAS_FLAT | Paint3DContext.MODE_DOUBLE_SIDED
                | Paint3DContext.MODE_FRONT_CW;
        assertWrites("drawMesh3D flags DOUBLE_SIDED|FRONT_CW", w -> w.drawMesh3D(2, cw),
                bytes(b -> DrawMesh3D.apply(b, 2, cw)));

        float[] sphere = {1, 8, 6};
        float[][] pos = {{0}};
        float[][] none = new float[0][];
        for (int flip : new int[] {
                MeshExpression3D.FLAG_FLIP_NORMALS,
                MeshExpression3D.FLAG_FLIP_NORMALS | MeshExpression3D.FLAG_FLIP_WINDING}) {
            assertWrites("meshExpression flag FLIP_NORMALS",
                    w -> w.meshExpression(
                            1, MeshExpression3D.SURFACE_SPHERE, flip, sphere, pos, none, none),
                    bytes(b -> MeshExpression3D.apply(
                            b, 1, MeshExpression3D.SURFACE_SPHERE, flip, sphere, pos, none, none)));
        }

        int[] ambient = {Paint3DContext.LIGHT_AMBIENT};
        int[] white = {0xFFFFFFFF};
        float[] ambientParams = {0, 0, 0, 0.3f};
        assertWrites("setLights3D ambient light",
                w -> w.setLights3D(ambient, white, ambientParams),
                bytes(b -> SetLights3D.apply(b, ambient, white, ambientParams)));
        int[] extra = {Paint3DContext.LIGHT_POINT | (2 << Paint3DContext.LIGHT_EXTRA_SHIFT)};
        float[] extraParams = {1, 2, 3, 0.5f, 9, 9};
        assertWrites("setLights3D light extra params",
                w -> w.setLights3D(extra, white, extraParams),
                bytes(b -> SetLights3D.apply(b, extra, white, extraParams)));

        assertWrites("meshPrimitive type INFINITE_PLANE",
                w -> w.meshPrimitive(1, Primitive3D.INFINITE_PLANE, 0f, 0, PLANE),
                bytes(b -> MeshPrimitive.apply(b, 1, Primitive3D.INFINITE_PLANE, 0f, 0, PLANE)));
        assertWrites("meshPrimitive type INFINITE_PLANE",
                w -> w.meshPrimitive(1, Primitive3D.INFINITE_PLANE, 0f, 0, new float[][] {PLANE}),
                bytes(b -> MeshPrimitive.apply(b, 1, Primitive3D.INFINITE_PLANE, 0f, 0, PLANE)));
        int axis = Primitive3D.AXIS_Z << Primitive3D.FLAG_AXIS_SHIFT;
        assertWrites("meshPrimitive flag AXIS",
                w -> w.meshPrimitive(1, Primitive3D.SPHERE, 8f, axis, SPHERE),
                bytes(b -> MeshPrimitive.apply(b, 1, Primitive3D.SPHERE, 8f, axis, SPHERE)));
        int curvature = Primitive3D.FLAG_TUBE_CURVATURE_DENSITY;
        assertWrites("meshPrimitive flag TUBE_CURVATURE_DENSITY",
                w -> w.meshPrimitive(1, Primitive3D.SPHERE, 8f, curvature, SPHERE),
                bytes(b -> MeshPrimitive.apply(b, 1, Primitive3D.SPHERE, 8f, curvature, SPHERE)));
        int both = axis | curvature;
        assertWrites("meshPrimitive flags AXIS|TUBE_CURVATURE_DENSITY",
                w -> w.meshPrimitive(1, Primitive3D.SPHERE, 8f, both, SPHERE),
                bytes(b -> MeshPrimitive.apply(b, 1, Primitive3D.SPHERE, 8f, both, SPHERE)));
    }

    // ---------------------------------------------------------------------------------------------
    // Always refused

    @Test
    public void valuesNoPlayerUnderstands_areAlwaysRefused() {
        RemoteComposeWriter writer = writer3D1();
        assertRefused(writer, "MODE_ADVANCED", w -> w.drawMesh3D(1, Paint3DContext.MODE_ADVANCED));
        for (int bit : new int[] {4, 7, 17, 29, 31}) {
            assertRefused(writer, "(bits 4-7, 17-29 and 31 are reserved)",
                    w -> w.drawMesh3D(1, 1 << bit));
        }
        assertRefused(writer, "drawMesh3D mode 0x30000 sets reserved bits 0x20000",
                w -> w.drawMesh3D(1, Paint3DContext.MODE_FRONT_CW | (1 << 17)));
        assertRefused(writer, "bit 30",
                w -> w.defineMesh3D(1 | (1 << 30), TRI, VERTS, null));
        assertRefused(writer, "bit 30", w -> w.setTexture3D(5 | (1 << 30)));
        assertRefused(writer, "Unknown matrix3DOp sub-op 7",
                w -> w.matrix3DOp(7, new float[0]));
        assertRefused(writer, "Unknown matrix3DOp sub-op -1",
                w -> w.matrix3DOp(-1, new float[0]));
        assertRefused(writer, "takes no arguments",
                w -> w.matrix3DOp(Paint3DContext.M3_PUSH, new float[] {1}));
        assertRefused(writer, "Unknown setCamera3D projection 2",
                w -> w.setCamera3D(2, new float[] {1f, 1.5f, 0.1f, 100f}, VIEW));
        assertRefused(writer, "Unknown setCamera3D projection -1",
                w -> w.setCamera3D(-1, new float[] {1f, 1.5f, 0.1f, 100f}, VIEW));
        assertRefused(writer, "Unknown setLights3D light kind 3",
                w -> w.setLights3D(new int[] {3}, new int[] {0}, new float[4]));
        assertRefused(writer, "reserved bits 16-31",
                w -> w.setLights3D(new int[] {1 << 16}, new int[] {0}, new float[4]));
        assertRefused(writer, "Unknown meshPrimitive type 24",
                w -> w.meshPrimitive(1, 24, 8f, 0, SPHERE));
        assertRefused(writer, "Unknown meshPrimitive type -1",
                w -> w.meshPrimitive(1, -1, 8f, 0, new float[][] {SPHERE}));
        assertRefused(writer,
                "meshPrimitive flags 0x200 set reserved bits 0x200 (bits 9-31 are reserved)",
                w -> w.meshPrimitive(1, Primitive3D.SPHERE, 8f, 1 << 9, SPHERE));
        int axisAndBit31 = (Primitive3D.AXIS_Z << Primitive3D.FLAG_AXIS_SHIFT) | (1 << 31);
        assertRefused(writer, "meshPrimitive flags 0x800000c0 set reserved bits 0x80000000",
                w -> w.meshPrimitive(
                        1, Primitive3D.SPHERE, 8f, axisAndBit31, new float[][] {SPHERE}));
        assertRefused(writer, "Unknown meshExpression surface type 4",
                w -> w.meshExpression(1, 4, 0, new float[] {1, 8, 6}, new float[][] {{0}},
                        new float[0][], new float[0][]));
        int[] flags = {1 << 2, 1 << 31, MeshExpression3D.FLAG_FLIP_NORMALS | (1 << 3)};
        String[] messages = {
            "meshExpression flags 0x4 set reserved bits 0x4 (bits 2-31 are reserved)",
            "meshExpression flags 0x80000000 set reserved bits 0x80000000",
            "meshExpression flags 0xa set reserved bits 0x8"
        };
        for (int i = 0; i < flags.length; i++) {
            int f = flags[i];
            assertRefused(writer, messages[i],
                    w -> w.meshExpression(1, MeshExpression3D.SURFACE_SPHERE, f,
                            new float[] {1, 8, 6}, new float[][] {{0}}, new float[0][],
                            new float[0][]));
        }
    }

    @Test
    public void lightSetThatDoesNotMatchItsTypeWords_isRefused() {
        RemoteComposeWriter writer = writer3D1();
        assertRefused(writer, "1 lights need 1 colors and 4 params, got 1 and 3",
                w -> w.setLights3D(new int[] {Paint3DContext.LIGHT_AMBIENT}, new int[] {0},
                        new float[3]));
        assertRefused(writer, "1 lights need 1 colors and 5 params, got 2 and 5",
                w -> w.setLights3D(
                        new int[] {Paint3DContext.LIGHT_POINT
                                | (1 << Paint3DContext.LIGHT_EXTRA_SHIFT)},
                        new int[] {0, 0}, new float[5]));
    }

    // ---------------------------------------------------------------------------------------------
    // Helpers

    /** A writer call and the bytes it appends. */
    private static final class Step {
        final String mHex;
        final Consumer<RemoteComposeWriter3D> mCall;

        Step(@NonNull String hex, @NonNull Consumer<RemoteComposeWriter3D> call) {
            mHex = hex;
            mCall = call;
        }
    }

    private static @NonNull RemoteComposeWriter writer3D1() {
        return new RemoteComposeWriter(
                RcPlatformServices.None,
                7,
                RemoteComposeWriter.hTag(Header.DOC_WIDTH, 100),
                RemoteComposeWriter.hTag(Header.DOC_HEIGHT, 50),
                RemoteComposeWriter.hTag(Header.DOC_PROFILES, profiles()));
    }

    /** 3D operations are registered in the experimental profile. */
    private static int profiles() {
        return RcProfiles.PROFILE_ANDROIDX | RcProfiles.PROFILE_EXPERIMENTAL;
    }

    /** Checks that {@code call} appends {@code expected}. */
    private static void assertWrites(
            @NonNull String feature,
            @NonNull Consumer<RemoteComposeWriter3D> call,
            @NonNull String expected) {
        assertEquals(feature, expected, appended(writer3D1(), call));
    }

    /**
     * Checks that {@code call} is refused with an {@link IllegalArgumentException} whose message
     * contains {@code text}, leaving the document unchanged.
     */
    private static void assertRefused(
            @NonNull RemoteComposeWriter writer,
            @NonNull String text,
            @NonNull Consumer<RemoteComposeWriter3D> call) {
        int size = writer.getBuffer().getBuffer().getSize();
        IllegalArgumentException e =
                assertThrows(
                        text,
                        IllegalArgumentException.class,
                        () -> call.accept(writer.getRc3DWriter()));
        assertTrue(e.getMessage(), e.getMessage().contains(text));
        assertEquals(text, size, writer.getBuffer().getBuffer().getSize());
    }

    /** Returns the hex of the bytes {@code call} appends to {@code writer}'s document. */
    private static @NonNull String appended(
            @NonNull RemoteComposeWriter writer, @NonNull Consumer<RemoteComposeWriter3D> call) {
        WireBuffer buffer = writer.getBuffer().getBuffer();
        int before = buffer.getSize();
        call.accept(writer.getRc3DWriter());
        return hex(buffer, before, buffer.getSize());
    }

    /** Returns the hex of the bytes {@code write} puts in a new buffer. */
    private static @NonNull String bytes(@NonNull Consumer<WireBuffer> write) {
        WireBuffer buffer = new WireBuffer();
        write.accept(buffer);
        return hex(buffer, 0, buffer.getSize());
    }

    private static @NonNull String hex(@NonNull WireBuffer buffer, int from, int to) {
        byte[] bytes = buffer.getBuffer();
        StringBuilder sb = new StringBuilder();
        for (int i = from; i < to; i++) {
            sb.append(String.format("%02x", bytes[i] & 0xFF));
        }
        return sb.toString();
    }
}
